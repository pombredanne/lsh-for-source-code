package org.yaircc.torrent.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BList;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.BTypeException;
import org.yaircc.torrent.util.Validator;

/**
 * @author dante
 * 
 */
public class MIInfoSection {

    private static final String PIECE_LENGTH = "piece length";

    private static final String PIECES = "pieces";

    private static final String NAME = "name";

    private static final String LENGTH = "length";

    private static final String FILES = "files";

    private static final String PATH = "path";

    private static final String PRIVATE = "private";

    private final int pieceLength;

    private final List<Hash> hashList;

    private final List<MIFileInfo> files;

    private Hash infoHash;

    private final Boolean privTracker;

    private final boolean singleFileMode;

    private final String baseDir;

    private long dataSize;

    /**
	 * Creates an info section with only one file. No directory "hint" can be
	 * given here.
	 * 
	 * @param fileInfo
	 * @param hashes
	 * @param pieceLength
	 * @param priv
	 *            true, if the torrent should be marked private.
	 * @return
	 */
    public static MIInfoSection singleFile(MIFileInfo fileInfo, List<Hash> hashes, int pieceLength, Boolean priv) {
        Validator.nonNull(fileInfo, hashes);
        Validator.isTrue(pieceLength > 0, "Illegal piece length: " + pieceLength);
        MIInfoSection data = new MIInfoSection(Arrays.asList(new MIFileInfo[] { fileInfo }), hashes, pieceLength, priv, null, true);
        Map<String, Object> info = data.createBasicInfo();
        info.put(NAME, fileInfo.getFileName());
        info.put(LENGTH, fileInfo.getLength());
        data.infoHash = data.calculateInfoHash(info);
        return data;
    }

    /**
	 * Creates an info section for one or more files. A directory "hint" can be
	 * given in the <i>baseDir</i> argument.
	 * 
	 * @param baseDir
	 * @param files
	 * @param hashes
	 * @param pieceLength
	 * @param priv
	 *            true, if the torrent should be marked private.
	 * @return
	 */
    public static MIInfoSection multiFile(String baseDir, List<MIFileInfo> files, List<Hash> hashes, int pieceLength, Boolean priv) {
        Validator.nonNull(baseDir, files, hashes);
        Validator.isTrue(pieceLength > 0, "Illegal piece length: " + pieceLength);
        MIInfoSection data = new MIInfoSection(files, hashes, pieceLength, priv, baseDir, false);
        Map<String, Object> info = data.createBasicInfo();
        info.put(NAME, baseDir);
        List<Map<String, Object>> flst = new ArrayList<Map<String, Object>>(files.size());
        for (MIFileInfo file : files) {
            Map<String, Object> fmap = new HashMap<String, Object>();
            flst.add(fmap);
            fmap.put(LENGTH, file.getLength());
            List<String> path = fileToPath(file);
            fmap.put(PATH, path);
        }
        data.infoHash = data.calculateInfoHash(info);
        return data;
    }

    /**
	 * @param file
	 * @return
	 */
    private static List<String> fileToPath(MIFileInfo file) {
        List<String> path = new LinkedList<String>();
        StringTokenizer tok = new StringTokenizer(file.getFileName(), File.separator);
        while (tok.hasMoreTokens()) {
            path.add(tok.nextToken());
        }
        return path;
    }

    private Map<String, Object> createBasicInfo() {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put(PIECE_LENGTH, pieceLength);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(hashList.size() * 20);
        for (Hash h : hashList) {
            try {
                bout.write(h.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        info.put(PIECES, bout.toByteArray());
        if (privTracker != null) {
            info.put(PRIVATE, privTracker ? 1 : 0);
        }
        return info;
    }

    private MIInfoSection(List<MIFileInfo> files, List<Hash> hashes, int pieceLength, Boolean priv, String baseDir, boolean singleFileMode) {
        assert files.size() == 1 || !singleFileMode;
        assert baseDir != null || singleFileMode;
        this.pieceLength = pieceLength;
        privTracker = priv;
        this.singleFileMode = singleFileMode;
        this.files = new ArrayList<MIFileInfo>(files);
        hashList = new ArrayList<Hash>(hashes);
        this.baseDir = baseDir;
        calculateSize();
        int reqHashes = (int) ((dataSize + pieceLength - 1) / pieceLength);
        Validator.isTrue(reqHashes == hashList.size(), "No. of hashes required: " + reqHashes + " but got " + hashList.size());
    }

    private void calculateSize() {
        dataSize = 0;
        for (MIFileInfo fi : files) {
            dataSize += fi.getLength();
        }
    }

    /**
	 * Retrieves the info section from an existing info "dictionary" (bencoded).
	 * 
	 * @param info
	 * @throws BTypeException
	 */
    public MIInfoSection(BMap info) throws BTypeException {
        Validator.notNull(info, "Info dictionary is null!");
        pieceLength = ((BigInteger) info.get(PIECE_LENGTH)).intValue();
        byte[] hashes = (byte[]) info.get(PIECES);
        Validator.notNull(hashes, "Pieces entry in info dictionary is null!");
        Validator.isTrue(hashes.length % 20 == 0, "Pieces entry in info dictionary is invalid (length " + hashes.length + " not multiple of 20)!");
        hashList = new ArrayList<Hash>(hashes.length / 20);
        int off = 0;
        while (off < hashes.length) {
            hashList.add(new Hash(Arrays.copyOfRange(hashes, off, off + 20), Hash.Type.SHA1));
            off += 20;
        }
        files = new ArrayList<MIFileInfo>();
        String base = info.getString(NAME);
        Validator.notNull(base, "Missing non-optional entry " + NAME);
        if (info.containsKey(LENGTH)) {
            singleFileMode = true;
            baseDir = null;
            files.add(new MIFileInfo(base, ((BigInteger) info.get(LENGTH)).longValue()));
        } else {
            singleFileMode = false;
            baseDir = base;
            BList fList = info.getList(FILES);
            for (int i = 0; i < fList.size(); i++) {
                BMap file = fList.getMap(i);
                StringBuilder path = new StringBuilder();
                BList elements = file.getList(PATH);
                for (int j = 0; j < elements.size(); j++) {
                    String element = elements.getString(j);
                    if (path.length() > 0) {
                        path.append(File.separatorChar);
                    }
                    path.append(element);
                }
                files.add(new MIFileInfo(path.toString(), ((BigInteger) file.get(LENGTH)).longValue()));
            }
        }
        infoHash = calculateInfoHash(info);
        privTracker = Integer.valueOf(1).equals(info.getInteger(PRIVATE));
        calculateSize();
    }

    /**
	 * @param info
	 * @return
	 */
    private Hash calculateInfoHash(Map<String, ?> info) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("sha1");
            sha1.update(BEncodedOutputStream.bencode(info));
            return new Hash(sha1.digest(), Hash.Type.SHA1);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Returns the piece length configured.
	 * 
	 * @return
	 */
    public int getPieceLength() {
        return pieceLength;
    }

    /**
	 * Returns the hash values of the actual content described by this info
	 * section.
	 * 
	 * @return
	 */
    public List<Hash> getPieceHashes() {
        return Collections.unmodifiableList(hashList);
    }

    /**
	 * Returns the number of pieces the actual data was split up into.
	 * 
	 * @return
	 */
    public int getPiecesCount() {
        return hashList.size();
    }

    /**
	 * Returns meta data about the files in the torrent data.
	 * 
	 * @return
	 */
    public List<MIFileInfo> getFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
	 * Returns the hash of the info section. This is used as an identifier for a
	 * torrent file.
	 * 
	 * @return
	 */
    public Hash getInfoHash() {
        return infoHash;
    }

    /**
	 * Tests if the torrent is marked private.
	 * 
	 * @return
	 */
    public boolean isPrivate() {
        return privTracker != null && privTracker;
    }

    /**
	 * Determines if this info was created with a bencoded SingleFileMode
	 * structure.
	 * 
	 * @return
	 */
    public boolean isSingleFileMode() {
        return singleFileMode;
    }

    /**
	 * Returns the "hint" base directory for the actual files. This can be null
	 * if no hint was specified.
	 * 
	 * @return
	 */
    public String getBaseDir() {
        return baseDir;
    }

    /**
	 * Rebuilds the bencoded map for this info section. Note: The map is really
	 * rebuild, if you initialized this class with an existing info section it
	 * will only be equal if no custom or "newer" keys are used in a dictionary.
	 * 
	 * @return
	 */
    public Map<String, ?> asDictionary() {
        Map<String, Object> infoMap = createBasicInfo();
        if (singleFileMode) {
            infoMap.put(NAME, files.get(0).getFileName());
            infoMap.put(LENGTH, files.get(0).getLength());
        } else {
            infoMap.put(NAME, baseDir);
            List<Map<String, Object>> tfiles = new LinkedList<Map<String, Object>>();
            for (MIFileInfo file : files) {
                Map<String, Object> entry = new HashMap<String, Object>();
                entry.put(PATH, fileToPath(file));
                entry.put(LENGTH, file.getLength());
                tfiles.add(entry);
            }
            infoMap.put(FILES, tfiles);
        }
        return infoMap;
    }

    /**
	 * Returns the size of the data described by this info section.
	 * 
	 * @return
	 */
    public long getDataSize() {
        return dataSize;
    }
}
