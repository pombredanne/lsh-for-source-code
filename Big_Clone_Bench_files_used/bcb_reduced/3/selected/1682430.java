package com.googlecode.quillen.service;

import com.googlecode.quillen.domain.*;
import static com.googlecode.quillen.util.Utils.logDebug;
import com.googlecode.quillen.util.Pair;
import com.googlecode.quillen.util.ResultConsumer;
import com.googlecode.quillen.repository.ObjectStorage;
import com.googlecode.quillen.repository.AttributeStorage;
import com.googlecode.quillen.repository.AttributeStorageQuery;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Created by IntelliJ IDEA.
 * User: greg
 * Date: Dec 6, 2008
 * Time: 3:28:07 PM
 */
public class ShadowFileServiceImpl implements ShadowFileService {

    private static final Log LOG = LogFactory.getLog(ShadowFileServiceImpl.class);

    private static final String SNAPSHOT_DATE_DELIM = "#";

    private final AttributeStorage attributeStorage;

    private final ObjectStorage objectStorage;

    private final CompressionService compressionService;

    public ShadowFileServiceImpl(CompressionService compressionService, AttributeStorage attributeStorage, ObjectStorage objectStorage) {
        this.attributeStorage = attributeStorage;
        this.objectStorage = objectStorage;
        this.compressionService = compressionService;
    }

    public void createStorage() throws AttributeStorageException, ObjectStorageException {
        attributeStorage.createDatabase(AttributeDatabase.shadow_files.toString());
        objectStorage.createBucket(ObjectBucket.shadow_files.toString());
        logDebug(LOG, "created shadow file storage");
    }

    public void deleteStorage() throws ObjectStorageException, AttributeStorageException {
        attributeStorage.deleteDatabase(AttributeDatabase.shadow_files.toString());
        objectStorage.deleteBucket(ObjectBucket.shadow_files.toString());
        logDebug(LOG, "deleted shadow file storage");
    }

    public ShadowFile get(String shadowKey) throws ObjectStorageException, IOException {
        ShadowFile result = ShadowFile.fromBytes(compressionService.decompress(objectStorage.get(ObjectBucket.shadow_files.toString(), shadowKey)));
        logDebug(LOG, "got shadow file %s of size %d", result.getKey(), result.getSize());
        return result;
    }

    public ShadowFile getStub(String shadowKey) throws AttributeStorageException {
        Attributes attrs = coalesceBundles(shadowKey, new ArrayList<String>(Arrays.asList("fn"))).first;
        if (attrs != null) {
            ShadowFile result = new ShadowFile(shadowKey, Long.valueOf(attrs.getSingle("size")));
            result.setOriginalFilenames(attrs.get("fn"));
            logDebug(LOG, "got stub for shadow file %s of size %d bytes", result.getKey(), result.getSize());
            return result;
        } else {
            return null;
        }
    }

    private Attributes putShadowFileToObjectStorageAndGetAttributes(ShadowFile shadowFile, Collection<String> snapshots) throws ObjectStorageException, IOException {
        Attributes attrs = new Attributes();
        if (shadowFile.getOriginalFilenames() == null) {
            objectStorage.put(ObjectBucket.shadow_files.toString(), shadowFile.getKey(), compressionService.compress(shadowFile.getBytes()));
            shadowFile.setOriginalFilenames(shadowFile.getFilenames());
            attrs.add("fn", shadowFile.getOriginalFilenames());
        }
        for (String snapshot : snapshots) {
            attrs.add("snapshot", snapshot + SNAPSHOT_DATE_DELIM + DateFormatUtils.format(shadowFile.getDate(), "yyyyMMddHHmmss"));
            if (!shadowFile.getOriginalFilenames().equals(shadowFile.getFilenames())) {
                attrs.add(snapshot + "-fn", shadowFile.getFilenames());
            }
        }
        return attrs;
    }

    public void batchPut(Collection<ShadowFile> shadowFiles, Collection<String> snapshots) throws ObjectStorageException, AttributeStorageException, IOException {
        Map<ShadowFile, Attributes> attributes = new HashMap<ShadowFile, Attributes>();
        for (ShadowFile shadowFile : shadowFiles) {
            attributes.put(shadowFile, putShadowFileToObjectStorageAndGetAttributes(shadowFile, snapshots));
        }
        batchPutBundles(attributes);
    }

    public void put(ShadowFile shadowFile, Collection<String> snapshots) throws ObjectStorageException, IOException, AttributeStorageException {
        putBundles(shadowFile, putShadowFileToObjectStorageAndGetAttributes(shadowFile, snapshots));
        logDebug(LOG, "put shadow file %s in snapshot %s", shadowFile.getKey(), StringUtils.join(snapshots, ", "));
    }

    public ShadowFile delete(String shadowKey, Collection<String> snapshots) throws ObjectStorageException, AttributeStorageException, IOException {
        List<String> attributesToGet = new ArrayList<String>();
        attributesToGet.add("snapshot");
        for (String snapshot : snapshots) {
            attributesToGet.add(snapshot + "-fn");
        }
        Pair<Attributes, Integer> pair = coalesceBundles(shadowKey, attributesToGet);
        final Attributes attrs = pair.first;
        final int bundleCount = pair.second;
        if (attrs != null) {
            Set<String> snapshotSet = new HashSet<String>(snapshots);
            Attributes toDelete = new Attributes();
            for (String v : new ArrayList<String>(attrs.get("snapshot"))) {
                final String snapshot = StringUtils.substringBeforeLast(v, SNAPSHOT_DATE_DELIM);
                if (snapshotSet.isEmpty() || snapshotSet.contains(snapshot)) {
                    toDelete.add("snapshot", attrs.remove("snapshot", v));
                    for (Map.Entry<String, Set<String>> entry : attrs.removeWithPrefix(snapshot).entrySet()) {
                        toDelete.add(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (attrs.get("snapshot") != null) {
                for (int i = 0; i < bundleCount; i++) {
                    attributeStorage.delete(AttributeDatabase.shadow_files.toString(), shadowKey + "-" + i, toDelete);
                }
                logDebug(LOG, "removed shadow file %s from snapshot %s", shadowKey, StringUtils.join(snapshots, ", "));
                return null;
            } else {
                ShadowFile shadowFile = get(shadowKey);
                for (int i = 0; i < bundleCount; i++) {
                    attributeStorage.delete(AttributeDatabase.shadow_files.toString(), shadowKey + "-" + i);
                }
                objectStorage.delete(ObjectBucket.shadow_files.toString(), shadowKey);
                logDebug(LOG, "deleted shadow file %s", shadowKey);
                return shadowFile;
            }
        } else {
            return null;
        }
    }

    public String createShadowKey(InputStream in) throws NoSuchAlgorithmException, IOException {
        String result;
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[1000000];
        int totalBytes = 0;
        try {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) >= 0) {
                sha.update(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            result = new String(Hex.encodeHex(sha.digest())) + "-" + totalBytes;
        } finally {
            in.close();
        }
        return result;
    }

    public void findStubs(final String snapshot, final String filenamePrefix, final ResultConsumer<ShadowFile> consumer) throws AttributeStorageException, ParseException {
        AttributeStorageQuery.Condition condition = new AttributeStorageQuery.LeafCondition("snapshot", AttributeStorageQuery.Operator.like, snapshot + SNAPSHOT_DATE_DELIM + "%");
        if (StringUtils.isNotEmpty(filenamePrefix)) {
            condition = new AttributeStorageQuery.ComplexCondition(condition, AttributeStorageQuery.Conjunction.and, new AttributeStorageQuery.ComplexCondition(new AttributeStorageQuery.LeafCondition(snapshot + "-fn", AttributeStorageQuery.Operator.like, filenamePrefix + "%"), AttributeStorageQuery.Conjunction.or, new AttributeStorageQuery.ComplexCondition(new AttributeStorageQuery.NullCondition(snapshot + "-fn"), AttributeStorageQuery.Conjunction.and, new AttributeStorageQuery.LeafCondition("fn", AttributeStorageQuery.Operator.like, filenamePrefix + "%"))));
        }
        attributeStorage.find(AttributeDatabase.shadow_files.toString(), new AttributeStorageQuery(condition), new ResultConsumer<Pair<String, Attributes>>() {

            public void newResult(Pair<String, Attributes> result) throws Exception {
                String key = StringUtils.substringBeforeLast(result.first, "-");
                if (!StringUtils.endsWith(result.first, "-0") || result.second.size() >= 200) {
                    logDebug(LOG, "coalescing possible multi-bundle item: %s", key);
                    result.second = coalesceBundles(key, null).first;
                }
                ShadowFile stub = toStub(snapshot, filenamePrefix, key, result.second);
                if (consumer != null && stub != null && !stub.getFilenames().isEmpty()) {
                    consumer.newResult(stub);
                }
            }
        });
    }

    private ShadowFile toStub(String snapshot, String filenamePrefix, String shadowKey, Attributes attrs) throws ParseException {
        Date date = null;
        List<String> snapshots = new ArrayList<String>(attrs.get("snapshot"));
        Collections.sort(snapshots, Collections.reverseOrder());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        for (String v : snapshots) {
            if (v.startsWith(snapshot + SNAPSHOT_DATE_DELIM)) {
                date = sdf.parse(StringUtils.substringAfterLast(v, SNAPSHOT_DATE_DELIM));
                break;
            }
        }
        if (date == null) {
            return null;
        }
        final ShadowFile result = new ShadowFile(shadowKey, Long.valueOf(attrs.getSingle("size")), date);
        Set<String> filenames = attrs.get(snapshot + "-fn");
        if (filenames == null || filenames.isEmpty()) {
            filenames = attrs.get("fn");
        }
        for (String filename : filenames) {
            if (StringUtils.isEmpty(filenamePrefix) || StringUtils.startsWith(filename, filenamePrefix)) {
                result.addFile(filename);
            }
        }
        return result;
    }

    private void batchPutBundles(Map<ShadowFile, Attributes> attributes) throws AttributeStorageException {
        for (int i = 0; !attributes.isEmpty(); i++) {
            Map<String, Attributes> batch = new HashMap<String, Attributes>();
            for (Map.Entry<ShadowFile, Attributes> entry : attributes.entrySet()) {
                ShadowFile shadowFile = entry.getKey();
                Attributes attrs = entry.getValue();
                attrs.add("size", Long.toString(shadowFile.getSize()));
                attrs.setPriority("size", 1);
                batch.put(shadowFile.getKey() + "-" + i, attrs);
            }
            attributeStorage.batchPut(AttributeDatabase.shadow_files.toString(), batch);
            for (ShadowFile shadowFile : new ArrayList<ShadowFile>(attributes.keySet())) {
                if (!batch.containsKey(shadowFile.getKey() + "-" + i)) {
                    attributes.remove(shadowFile);
                }
            }
        }
    }

    private void putBundles(ShadowFile shadowFile, Attributes attributes) throws AttributeStorageException {
        for (int i = 0; !attributes.isEmpty(); i++) {
            attributes.add("size", Long.toString(shadowFile.getSize()));
            attributes.setPriority("size", 1);
            attributeStorage.put(AttributeDatabase.shadow_files.toString(), shadowFile.getKey() + "-" + i, attributes);
        }
    }

    private Pair<Attributes, Integer> coalesceBundles(String key, List<String> attributesToGet) throws AttributeStorageException {
        String[] attributesToGetArr = null;
        if (attributesToGet != null && !attributesToGet.isEmpty() && attributesToGet.size() < 128) {
            attributesToGet.add("size");
            attributesToGetArr = new String[attributesToGet.size()];
            for (int i = 0; i < attributesToGet.size(); i++) {
                attributesToGetArr[i] = attributesToGet.get(i);
            }
        }
        Attributes result = new Attributes();
        int i = 0;
        Attributes bundle;
        do {
            bundle = attributeStorage.get(AttributeDatabase.shadow_files.toString(), key + "-" + i++, attributesToGetArr);
            result.merge(bundle);
        } while (bundle != null);
        return Pair.get(result.isEmpty() ? null : result, i);
    }
}
