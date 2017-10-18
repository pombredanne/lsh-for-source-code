package exabase.data;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class DataStore {

    private static Logger logger = Logger.getLogger(DataStore.class.getName());

    protected static DataStore instance = null;

    protected HashMap<Hash, String> hashToFileMap;

    protected DataStore() {
        hashToFileMap = new HashMap<Hash, String>();
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Set<Data> getDataSet() {
        Set<Data> datas = new HashSet<Data>();
        for (Entry<Hash, String> entry : hashToFileMap.entrySet()) {
            datas.add(new Data(entry.getValue(), entry.getKey()));
        }
        return datas;
    }

    public void addData(String fileURL, Hash hash) {
        logger.finer("adding data: " + fileURL + ", " + hash);
        hashToFileMap.put(hash, fileURL);
    }

    public Hash addFile(String fileName) {
        String md5Checksum = null;
        try {
            md5Checksum = getMD5Checksum(fileName);
        } catch (Exception e) {
            logger.warning("md5 checksum calculation failed for file " + fileName);
            return null;
        }
        Hash hash = new Hash(md5Checksum);
        addData(fileName, hash);
        return hash;
    }

    protected static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    protected static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void reset() {
        instance = null;
    }

    public Data getData(Hash hash) {
        String fileName = hashToFileMap.get(hash);
        return new Data(fileName, hash);
    }
}
