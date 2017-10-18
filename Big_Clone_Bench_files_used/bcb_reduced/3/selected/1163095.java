package hu.blomqvist.dbutil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public abstract class BenchTask implements Runnable {

    private String getHash(Class clazz) throws IOException, NoSuchAlgorithmException {
        String toLoad = "bin/" + clazz.getName().replace(".", "/") + ".class";
        FileInputStream in = new FileInputStream(toLoad);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[1024 * 32];
        while ((read = in.read(buffer)) != -1) {
            byteOut.write(buffer, 0, read);
        }
        byte[] data = byteOut.toByteArray();
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(data, 0, data.length);
        String hashAsHex = new BigInteger(1, m.digest()).toString(16);
        return hashAsHex;
    }

    private long startTime;

    private long endTime;

    private Class clazz;

    private String classHash = "";

    private String comment = "";

    public void init(Class clazz, String comment) throws Exception {
        this.clazz = clazz;
        this.comment = comment;
        this.classHash = getHash(clazz);
    }

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void endTime() throws IOException {
        endTime = System.currentTimeMillis();
        FileWriter fileOut = new FileWriter(new File(clazz.getSimpleName() + ".txt"), true);
        fileOut.write(clazz.getSimpleName() + ":" + comment + ":" + classHash + ":" + startTime + ":" + endTime + ":" + (endTime - startTime) + ":" + new Date(startTime) + " -> " + new Date(endTime) + ":" + ((int) ((endTime - startTime) / 1000)) + "sec\n");
        fileOut.flush();
        fileOut.close();
    }

    public abstract void run();
}
