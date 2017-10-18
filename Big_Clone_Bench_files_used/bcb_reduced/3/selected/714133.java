package bw.os;

import java.util.zip.*;
import java.io.*;
import java.math.*;
import java.security.*;

public class PersistentObjectHandler {

    public static final String FILE_PREFIX = "os-";

    public static final String FILE_EXT = ".obj";

    public PersistentObjectHandler() {
    }

    protected String makeFilename(String key) {
        String hashedKey = hashKey(key);
        return (FILE_PREFIX + hashedKey + FILE_EXT);
    }

    private String hashKey(String key) {
        String hashed = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashed = hash.toString(16);
        } catch (Exception ex) {
            ex.printStackTrace();
            hashed = String.valueOf(key.hashCode());
        }
        return hashed;
    }

    protected Object getObjectFromStream(InputStream in) throws Exception {
        ObjectInputStream ois = getObjectInputStream(in);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    protected ObjectInputStream getObjectInputStream(InputStream in) throws Exception {
        return new ObjectInputStream(new GZIPInputStream(in));
    }

    protected ObjectOutputStream getObjectOutputStream(OutputStream out) throws Exception {
        return new ObjectOutputStream(new GZIPOutputStream(out));
    }
}
