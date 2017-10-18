package de.burlov.amazon.s3.dirsync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import de.burlov.amazon.s3.RegExpUtil;
import de.burlov.amazon.s3.S3Utils;
import de.burlov.amazon.s3.dirsync.datamodel.v1.FileInfo;
import de.burlov.amazon.s3.dirsync.datamodel.v1.Folder;
import de.burlov.amazon.s3.dirsync.datamodel.v1.MainIndex;
import de.burlov.bouncycastle.io.CryptInputStream;
import de.burlov.bouncycastle.io.CryptOutputStream;

/**
 * Primaere s3dirsync Klasse mit High Level API
 * 
 * @author paul
 * 
 */
public class DirSync {

    private static final byte[] salt = new byte[] { (byte) 89, (byte) 43, (byte) 94, (byte) 02, (byte) 20, (byte) 45, (byte) 123, (byte) 1, (byte) 0, (byte) 204 };

    private static final int ITERATION_COUNT = 30000;

    private static final String SYS_DATA_PREFIX = "system";

    private static final String FILES_PREFIX = "data";

    private static final String MAIN_INDEX_KEY = "main-index";

    private static final String DELIMITER = "/";

    private String bucket;

    private String location;

    private S3Service s3Service;

    private byte[] pbeKey;

    private BlockCipher cipher;

    private MainIndex mainIndex;

    private Log log = LogFactory.getLog(DirSync.class);

    private Map<String, Folder> folderCache = new HashMap<String, Folder>();

    private int deletedFiles;

    private int transferredFiles;

    private long transferredData;

    private String accessKey;

    private String secretKey;

    private MessageDigest shaDigest;

    private MessageDigest md5Digest;

    private List<Pattern> excludePatterns = new LinkedList<Pattern>();

    private List<Pattern> includePatterns = new LinkedList<Pattern>();

    public DirSync(String accessKey, String secretKey, String bucket, String location, char[] encPassword) throws DirSyncException {
        super();
        try {
            shaDigest = MessageDigest.getInstance("SHA-1");
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new DirSyncException(e.getMessage());
        }
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        if (encPassword == null) {
            throw new IllegalArgumentException("encryption password is null");
        }
        this.bucket = bucket;
        this.location = location;
        if (StringUtils.equalsIgnoreCase(location, S3Bucket.LOCATION_EUROPE)) {
            this.location = S3Bucket.LOCATION_EUROPE;
        } else {
            this.location = null;
        }
        if (StringUtils.isBlank(this.bucket)) {
            this.bucket = accessKey + ".dirsync";
        }
        cipher = new SerpentEngine();
        pbeKey = generatePbeKey(encPassword);
        try {
            s3Service = new RestS3Service(new AWSCredentials(accessKey, secretKey));
        } catch (S3ServiceException e1) {
            throw new DirSyncException("Connecting to S3 service failed", e1);
        }
    }

    /**
	 * 
	 * @param autocreate
	 *        'true' wenn fehlende Index/bucket automatisch erstellt werden sollen
	 * @throws DirSyncException
	 */
    private void connect(boolean autocreate) throws DirSyncException {
        if (mainIndex != null) {
            return;
        }
        boolean bucketExists = false;
        try {
            bucketExists = s3Service.isBucketAccessible(bucket);
        } catch (S3ServiceException e1) {
            throw new DirSyncException("Internal error: " + e1.getMessage());
        }
        if (!bucketExists) {
            if (autocreate) {
                try {
                    s3Service.createBucket(bucket, location);
                } catch (S3ServiceException e2) {
                    throw new DirSyncException("Creating bucket '" + bucket + "' failed: " + e2.getLocalizedMessage());
                }
            } else {
                throw new DirSyncException("Bucket not found: " + bucket);
            }
        }
        try {
            mainIndex = (MainIndex) downloadObject(getMainIndexKey(), pbeKey);
        } catch (IOException e) {
            throw new DirSyncException("Reading main index failed. Are password and S3 login data valid?", e);
        }
        if (mainIndex == null) {
            if (autocreate) {
                mainIndex = new MainIndex();
                SecureRandom srnd = new SecureRandom();
                srnd.setSeed(pbeKey);
                byte[] dataKey = new byte[32];
                srnd.nextBytes(dataKey);
                mainIndex.setEncryptionKey(dataKey);
            } else {
                throw new DirSyncException("No data found");
            }
        }
    }

    /**
	 * Sensible Daten in Hauptspeicher explizit ueberschreiben
	 */
    public void close() {
        if (pbeKey != null) {
            Arrays.fill(pbeKey, (byte) 0);
        }
        if (mainIndex != null) {
            Arrays.fill(mainIndex.getEncryptionKey(), (byte) 0);
        }
        accessKey = null;
        secretKey = null;
    }

    /**
	 * Synchronisiert Daten im lokalen Verzeichnis und auf dem Server
	 * 
	 * @param baseDir
	 * @param folderName
	 * @param up
	 *        Synchronisationsrichtung. Wenn 'true' dann werden Daten von lokalen Verzeichnis auf
	 *        den Server geschrieben. Wenn 'false' Dann werden Daten von Server auf lokalen
	 *        Verzeichnis geschrieben
	 * @param snapShot
	 *        Wenn 'true' dann wird Synchronisierung in 'Abbild' Modus duchgefuehrt. D.h.
	 *        Zielverzeichnis wird auf den gleichen Stand wie Quelle gebracht: neue Dateien werde
	 *        geloescht, geloeschte wiederherstellt und modifiezierte upgedated. Wenn 'false' dann
	 *        werden neue Dateien hinzugefuegt und modifizierte upgedated aber auf keinen Fall
	 *        irgendetwas geloescht.
	 * @throws DirSyncException
	 */
    public void syncFolder(File baseDir, String folderName, boolean up, boolean snapShot) throws DirSyncException {
        connect(up);
        deletedFiles = 0;
        transferredFiles = 0;
        transferredData = 0;
        if (!baseDir.isDirectory()) {
            throw new DirSyncException("Invalid directory: " + baseDir.getAbsolutePath());
        }
        Folder folder = getFolder(folderName);
        if (folder == null) {
            if (!up) {
                log.warn("No such folder " + folderName);
                return;
            }
            folder = new Folder(folderName, Long.toHexString(mainIndex.getNextId()));
        }
        Collection<LocalFile> files;
        try {
            files = generateChangedFileList(baseDir, folder);
        } catch (Exception e) {
            throw new DirSyncException(e);
        }
        if (up) {
            syncUp(folder, files, snapShot);
        } else {
            syncDown(folder, baseDir, files, snapShot);
        }
        log.info("Transferred data: " + FileUtils.byteCountToDisplaySize(transferredData));
        log.info("Transferred files: " + transferredFiles);
        log.info("Deleted/Removed files: " + deletedFiles);
    }

    public void deleteFolder(String folderName) throws DirSyncException {
        connect(false);
        Folder folder = getFolder(folderName);
        if (folder == null) {
            return;
        }
        for (Map.Entry<String, FileInfo> info : folder.getIndexData().entrySet()) {
            try {
                deleteRemoteFile(info.getValue().getStorageId());
            } catch (S3ServiceException e) {
                log.error("Unable to delete file " + info.getKey(), e);
            }
        }
        mainIndex.getFolders().remove(folderName);
        try {
            saveMainIndex();
        } catch (S3ServiceException e) {
            throw new DirSyncException("Unable to delete main index entry", e);
        }
        folderCache.remove(folderName);
        try {
            s3Service.deleteObject(bucket, getFolderKey(folder.getStorageId()));
        } catch (S3ServiceException e) {
            throw new DirSyncException("Unable to delete folder entry", e);
        }
    }

    /**
	 * Methode setzt Passwort auf neuen Wert. Beim naechsten connet() Aufruf muss schon neues
	 * Passwort mitgegeben werden.
	 * 
	 * @param newPassword
	 * @throws DirSyncException
	 */
    public void changePassword(char[] newPassword) throws DirSyncException {
        connect(false);
        pbeKey = generatePbeKey(newPassword);
        try {
            saveMainIndex();
        } catch (S3ServiceException e) {
            throw new DirSyncException(e);
        }
    }

    private byte[] generatePbeKey(char[] password) {
        PKCS5S2ParametersGenerator pgen = new PKCS5S2ParametersGenerator();
        pgen.init(PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password), salt, ITERATION_COUNT);
        CipherParameters params = pgen.generateDerivedParameters(256);
        byte[] ret = ((KeyParameter) params).getKey();
        return ret;
    }

    private void saveFolder(Folder folder) throws DirSyncException {
        try {
            Folder newFolder = new Folder(folder.getName(), Long.toHexString(mainIndex.getNextId()));
            newFolder.setLastModified(System.currentTimeMillis());
            newFolder.setIndexData(folder.getIndexData());
            uploadObject(SYS_DATA_PREFIX + "/" + newFolder.getStorageId(), getDataEncryptionKey(), newFolder);
            mainIndex.getFolders().put(newFolder.getName(), newFolder.getStorageId());
            saveMainIndex();
            folderCache.put(newFolder.getName(), newFolder);
            s3Service.deleteObject(bucket, getFolderKey(folder.getStorageId()));
        } catch (Exception e) {
            throw new DirSyncException("Save folder description failed", e);
        }
    }

    private void saveMainIndex() throws S3ServiceException {
        uploadObject(getMainIndexKey(), pbeKey, mainIndex);
    }

    /**
	 * Methode laedt lokale Dateien die als geaendert erkannt wurden auf den Server hoch
	 * 
	 * @param folder
	 * @param files
	 * @param snapShot
	 * @throws DirSyncException
	 */
    private void syncUp(Folder folder, Collection<LocalFile> files, boolean snapShot) throws DirSyncException {
        boolean dirty = false;
        long uploadedBytes = 0;
        for (LocalFile item : files) {
            File file = item.getLocalFile();
            if (file.exists()) {
                byte[] hash;
                try {
                    hash = digestFile(file, shaDigest);
                } catch (IOException e1) {
                    throw new DirSyncException("Unable to hash file: " + file.getAbsolutePath(), e1);
                }
                FileInfo info = folder.getFileInfo(hash);
                if (info != null) {
                    folder.getIndexData().put(item.getRelativeName(), info);
                    log.info("Link file with uploaded data: " + item.getRelativeName());
                    info.setLastModified(file.lastModified());
                    dirty = true;
                } else {
                    info = new FileInfo(file.lastModified(), file.length(), Long.toHexString(mainIndex.getNextId()));
                    try {
                        log.info("Uploading " + file.getAbsolutePath());
                        uploadFile(file, getFileKey(info.getStorageId()));
                        dirty = true;
                        uploadedBytes += file.length();
                        transferredData += file.length();
                        transferredFiles++;
                        info.setHash(hash);
                    } catch (Exception e) {
                        throw new DirSyncException("File upload '" + file.getAbsolutePath() + "' failed. " + e.getLocalizedMessage());
                    }
                    folder.getIndexData().put(item.getRelativeName(), info);
                    if (uploadedBytes > 10000000) {
                        saveFolder(folder);
                        dirty = false;
                        uploadedBytes = 0;
                    }
                }
            } else if (snapShot) {
                if (folder.getIndexData().remove(item.getRelativeName()) != null) {
                    log.info("Remove file " + item.getRelativeName());
                    deletedFiles++;
                    dirty = true;
                }
            }
        }
        Set<String> idsToDelete = folder.syncFileHashIndex();
        if (dirty) {
            saveFolder(folder);
        }
        try {
            for (String id : idsToDelete) {
                deleteRemoteFile(id);
            }
        } catch (S3ServiceException e) {
            log.error("Unable to delete remote file. " + e.getLocalizedMessage(), e);
        }
    }

    private void syncDown(Folder folder, File baseDir, Collection<LocalFile> files, boolean snapShot) {
        for (LocalFile item : files) {
            FileInfo info = folder.getIndexData().get(item.getRelativeName());
            File file = item.getLocalFile();
            if (info != null) {
                try {
                    byte[] hash = null;
                    if (file.exists()) {
                        hash = digestFile(file, shaDigest);
                    }
                    if (info.getHash() == null || !Arrays.equals(info.getHash(), hash)) {
                        log.info("Downloading " + item.getRelativeName());
                        downloadFile(file, getFileKey(info.getStorageId()));
                        transferredFiles++;
                        transferredData += file.length();
                    }
                    file.setLastModified(info.getLastModified());
                } catch (Exception e) {
                    log.error("File download '" + item.getRelativeName() + "' failed. " + e.getLocalizedMessage());
                }
            } else if (snapShot) {
                log.info("Delete " + file.getAbsolutePath());
                if (!file.delete()) {
                    log.warn("Unable to delete file: " + file.getAbsolutePath());
                } else {
                    deletedFiles++;
                }
                deleteEmptyFolder(baseDir, file.getParentFile());
            }
        }
    }

    /**
	 * Loescht rekursiv leere Verzeichnisse
	 * 
	 * @param lowerDir
	 *        unterste Verzeichnis, der nicht geloscht werden darf
	 * @param dir
	 *        Verzeichnis zum loeschen
	 */
    private void deleteEmptyFolder(final File lowerDir, File dir) {
        if (dir != null && dir.exists() && dir.isDirectory() && !lowerDir.equals(dir)) {
            File[] children = dir.listFiles();
            if (children == null || children.length == 0) {
                dir.delete();
                dir = dir.getParentFile();
                deleteEmptyFolder(lowerDir, dir);
            }
        }
    }

    private void uploadFile(File file, String key) throws IOException, S3ServiceException {
        file = prepareFileForUpload(file, key);
        byte[] digest = digestFile(file, md5Digest);
        try {
            S3Object obj = new S3Object(key);
            obj.setDataInputFile(file);
            obj.setMd5Hash(digest);
            obj.setContentLength(file.length());
            s3Service.putObject(bucket, obj);
        } finally {
            file.delete();
        }
    }

    /**
	 * Liefert Schluessel fuer primaere Datenverschluesselung
	 * 
	 * @return
	 */
    private byte[] getDataEncryptionKey() {
        assert mainIndex != null;
        assert mainIndex.getEncryptionKey() != null && mainIndex.getEncryptionKey().length >= 32;
        return mainIndex.getEncryptionKey();
    }

    /**
	 * Prepariert eine Datei zum Hochladen. Sie wird komprimiert und verschluesselt
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
    private File prepareFileForUpload(File source, String s3key) throws IOException {
        File tmp = File.createTempFile("dirsync", ".tmp");
        tmp.deleteOnExit();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new DeflaterOutputStream(new CryptOutputStream(new FileOutputStream(tmp), cipher, getDataEncryptionKey()));
            IOUtils.copy(in, out);
            in.close();
            out.close();
            return tmp;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private void downloadFile(File target, String s3key) throws IOException, S3ServiceException {
        InputStream in = downloadData(s3key);
        if (in == null) {
            throw new IOException("No data found");
        }
        in = new InflaterInputStream(new CryptInputStream(in, cipher, getDataEncryptionKey()));
        File temp = File.createTempFile("dirsync", null);
        FileOutputStream fout = new FileOutputStream(temp);
        try {
            IOUtils.copy(in, fout);
            if (target.exists()) {
                target.delete();
            }
            IOUtils.closeQuietly(fout);
            IOUtils.closeQuietly(in);
            FileUtils.moveFile(temp, target);
        } catch (IOException e) {
            fetchStream(in);
            throw e;
        } finally {
            IOUtils.closeQuietly(fout);
            IOUtils.closeQuietly(in);
        }
    }

    private void fetchStream(InputStream in) {
        byte[] buf = new byte[1024];
        try {
            while (in.read(buf) > 0) {
            }
        } catch (IOException e) {
            return;
        }
    }

    private void upload(InputStream in, String s3key, long length) throws S3ServiceException {
        S3Object so = new S3Object(s3key);
        so.setDataInputStream(in);
        so.setContentLength(length);
        s3Service.putObject(bucket, so);
    }

    private void deleteRemoteFile(String id) throws S3ServiceException {
        s3Service.deleteObject(bucket, getFileKey(id));
    }

    /**
	 * Generiert Liste mit geanderten Dateien. Fehlende oder neue Dateien werden auch hinzugefuegt.
	 * List wird unter berucksichtigung der 'exclude' und/oder 'include' Patterns erstellt
	 * 
	 * @param baseDir
	 * @param folder
	 * @return
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    private List<LocalFile> generateChangedFileList(File baseDir, Folder folder) throws Exception {
        HashSet<String> localFiles = new HashSet<String>();
        List<LocalFile> ret = new LinkedList<LocalFile>();
        for (File file : (Collection<File>) FileUtils.listFiles(baseDir, FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter())) {
            if (!file.isFile()) {
                continue;
            }
            String filename = computeRelativeName(baseDir, file);
            if (!shouldIncludeFile(filename)) {
                continue;
            }
            localFiles.add(filename);
            FileInfo info = folder.getFileInfo(filename);
            if (info == null) {
                ret.add(new LocalFile(file, filename));
            } else {
                if (isFileChanged(file, info)) {
                    ret.add(new LocalFile(file, filename));
                }
            }
        }
        for (String filename : folder.getIndexData().keySet()) {
            if (!shouldIncludeFile(filename)) {
                continue;
            }
            if (!localFiles.contains(filename)) {
                ret.add(new LocalFile(new File(baseDir, filename), filename));
            }
        }
        return ret;
    }

    /**
	 * Methode testet ob gegebene Dateiename laut vorhandenen exclude/include Regeln in Liste der zu
	 * bearbeitenden Datein eingeschlossen werden soll
	 * 
	 * @param filename
	 * @return
	 */
    private boolean shouldIncludeFile(String filename) {
        if (!includePatterns.isEmpty()) {
            boolean include = false;
            for (Pattern p : includePatterns) {
                if (p.matcher(filename).matches()) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                return false;
            }
        }
        for (Pattern p : excludePatterns) {
            if (p.matcher(filename).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Testet ob eine Datei sich geaendert hat.
	 * 
	 * @param file
	 * @param info
	 * @return
	 * @throws Exception
	 */
    private boolean isFileChanged(File file, FileInfo info) throws Exception {
        if (file.length() != info.getLength()) {
            return true;
        }
        if (file.lastModified() != info.getLastModified()) {
            return true;
        }
        return false;
    }

    private byte[] digestFile(File file, MessageDigest digest) throws IOException {
        DigestInputStream in = new DigestInputStream(new FileInputStream(file), digest);
        IOUtils.copy(in, new NullOutputStream());
        in.close();
        return in.getMessageDigest().digest();
    }

    /**
	 * Methode berechnet relative Pfad einer Datei bezueglich eines Basisordners
	 * 
	 * @param baseDir
	 * @param file
	 * @return
	 */
    private String computeRelativeName(File baseDir, File file) {
        URI relUri = baseDir.toURI().relativize(file.toURI());
        return relUri.getPath();
    }

    /**
	 * Liefert Folder OBjekt mit dem gegebenen Namen. Wenn Folder Objekt schon runtergeladen wurde,
	 * dann wird die lokale Version zuruckgegeben. Anderfalls wird zuerst Folder Objekt vom Server
	 * runtergeladen
	 * 
	 * @param name
	 * @return
	 * @throws DirSyncException
	 */
    public Folder getFolder(String name) throws DirSyncException {
        connect(false);
        String id = mainIndex.getFolders().get(name);
        if (id == null) {
            return null;
        }
        try {
            return getFolderIntern(id);
        } catch (Exception e) {
            throw new DirSyncException("Reading folder description failed", e);
        }
    }

    private Folder getFolderIntern(String id) throws IOException, S3ServiceException {
        Folder ret = folderCache.get(id);
        if (ret == null) {
            ret = downloadFolder(id);
            if (ret != null) {
                folderCache.put(ret.getName(), ret);
            }
        }
        return ret;
    }

    private Folder downloadFolder(String id) throws IOException, S3ServiceException {
        Folder folder = (Folder) downloadObject(getFolderKey(id), getDataEncryptionKey());
        folder.initFileHashIndex();
        return folder;
    }

    private void uploadObject(String s3key, byte[] encKey, Serializable obj) throws S3ServiceException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oout = new ObjectOutputStream(new DeflaterOutputStream(new CryptOutputStream(bout, cipher, encKey)));
            oout.writeObject(obj);
            oout.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] data = bout.toByteArray();
        upload(new ByteArrayInputStream(data), s3key, data.length);
        transferredData += data.length;
    }

    private Object downloadObject(String s3key, byte[] encKey) throws IOException {
        InputStream in = downloadData(s3key);
        if (in == null) {
            return null;
        }
        CountingInputStream cin = new CountingInputStream(new InflaterInputStream(new CryptInputStream(in, cipher, encKey)));
        ObjectInputStream oin = new ObjectInputStream(cin);
        try {
            Object o = oin.readObject();
            transferredData += cin.getByteCount();
            return o;
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private InputStream downloadData(String key) {
        try {
            S3Object obj = null;
            obj = s3Service.getObject(new S3Bucket(bucket), key);
            if (obj == null || obj.getDataInputStream() == null) {
                return null;
            }
            return obj.getDataInputStream();
        } catch (S3ServiceException e) {
        }
        return null;
    }

    /**
	 * Methode loescht alle Objekte aus einem Bucket
	 * 
	 * @throws DirSyncException
	 */
    public void cleanBucket() throws DirSyncException {
        try {
            for (S3Object so : s3Service.listObjects(new S3Bucket(bucket))) {
                try {
                    s3Service.deleteObject(bucket, so.getKey());
                } catch (S3ServiceException e) {
                    log.error("Deleting of object '" + so.getKey() + "' failed: " + e.getLocalizedMessage());
                }
            }
        } catch (S3ServiceException e) {
            throw new DirSyncException("Cleaning bucket failed: " + e.getLocalizedMessage());
        }
    }

    /**
	 * Methode findet nicht mehr referenzierte Objekte in S3 und loescht sie
	 * 
	 * @throws DirSyncException
	 */
    public void cleanUp() throws DirSyncException {
        connect(false);
        Set<String> referencedKeys = getAllUsedObjects();
        int removedCount = 0;
        try {
            for (String key : S3Utils.listObjects(accessKey, secretKey, bucket)) {
                if (!referencedKeys.remove(key)) {
                    removedCount++;
                    s3Service.deleteObject(bucket, key);
                }
            }
        } catch (Exception e) {
            throw new DirSyncException(e);
        }
        log.info("Objects deleted: " + removedCount);
    }

    /**
	 * Methode liefrt Auflistung mit S3 Keys aller aktuell vom Programm benutzten und gespeicherten
	 * Objekte (Daten als auch Verwaltungsinformationen)
	 * 
	 * @return
	 * @throws DirSyncException
	 */
    private Set<String> getAllUsedObjects() throws DirSyncException {
        HashSet<String> referencedKeys = new HashSet<String>();
        referencedKeys.add(getMainIndexKey());
        for (String id : mainIndex.getFolders().values()) {
            referencedKeys.add(getFolderKey(id));
        }
        for (String name : mainIndex.getFolders().keySet()) {
            Folder folder = getFolder(name);
            if (folder == null) {
                continue;
            }
            for (FileInfo info : folder.getIndexData().values()) {
                referencedKeys.add(getFileKey(info.getStorageId()));
            }
        }
        return referencedKeys;
    }

    /**
	 * Methode gibt Zusammenfassung der auf dem Server liegenenden Daten
	 * 
	 * @throws DirSyncException
	 */
    public void printStorageSummary() throws DirSyncException {
        int storedObjects = 0;
        try {
            if (!S3Utils.bucketExists(accessKey, secretKey, bucket)) {
                System.out.println("No such bucket");
                return;
            }
            System.out.println("Summary for bucket " + bucket);
            for (String str : S3Utils.listObjects(accessKey, secretKey, bucket)) {
                storedObjects++;
            }
        } catch (Exception e) {
            throw new DirSyncException(e.getMessage());
        }
        connect(false);
        int usedObjects = getAllUsedObjects().size();
        System.out.println("Total objects in use: " + usedObjects);
        System.out.println("Total stored objects: " + storedObjects);
        if (usedObjects < storedObjects) {
            System.out.println((storedObjects - usedObjects) + " orphaned objects found. '-cleanup' command recommended");
        }
        System.out.println("---------------------------------------------------------------------------------");
        long totalSize = 0;
        for (Map.Entry<String, String> entry : mainIndex.getFolders().entrySet()) {
            System.out.println("Folder: " + entry.getKey());
            Folder folder = getFolder(entry.getKey());
            long folderSize = 0;
            if (folder != null) {
                for (FileInfo fi : folder.getIndexData().values()) {
                    folderSize += fi.getLength();
                }
                System.out.println("Files: " + folder.getIndexData().size());
                System.out.println("Size: " + FileUtils.byteCountToDisplaySize(folderSize));
                System.out.println();
            }
            totalSize += folderSize;
        }
        System.out.println("---------------------------------------------------------------------------------");
        System.out.println("Total folders: " + mainIndex.getFolders().size());
        System.out.println("Total size: " + FileUtils.byteCountToDisplaySize(totalSize));
    }

    /**
	 * Methode setzt Filterregel fuer Dateien die aus Prozess ausgeschlossen werden sollen. Exclude
	 * Regeln haben hoehere Prioritaet als Include Regel.
	 * 
	 * @param excludes
	 *        Filterregel mit * und ?
	 */
    public void setExcludePatterns(Collection<String> excludes) {
        excludePatterns = new ArrayList<Pattern>(excludes.size());
        for (String str : excludes) {
            String exp = RegExpUtil.convertSimpleRegexpToJava(str);
            excludePatterns.add(Pattern.compile(exp));
        }
    }

    /**
	 * Methode setzt Filterregel fuer Dateien die in das Prozess eingeschlossen werden sollen.
	 * Exclude Regeln haben hoehere Prioritaet als Include Regel.
	 * 
	 * @param includes
	 *        Filterregel mit * und ?
	 */
    public void setIncludePatterns(Collection<String> includes) {
        includePatterns = new ArrayList<Pattern>(includes.size());
        for (String str : includes) {
            String exp = RegExpUtil.convertSimpleRegexpToJava(str);
            includePatterns.add(Pattern.compile(exp));
        }
    }

    private static String getMainIndexKey() {
        return SYS_DATA_PREFIX + DELIMITER + MAIN_INDEX_KEY;
    }

    private static String getFolderKey(String id) {
        return SYS_DATA_PREFIX + DELIMITER + id;
    }

    private static String getFileKey(String id) {
        return FILES_PREFIX + DELIMITER + id;
    }
}
