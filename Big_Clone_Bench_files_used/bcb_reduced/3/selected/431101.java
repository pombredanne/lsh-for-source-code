package net.sf.sql2java.generator.merger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public abstract class MergeHelper {

    public static String calculateMD5(File file) throws Exception {
        return calculateMD5(new FileInputStream(file));
    }

    public static String calculateMD5(String content) throws Exception {
        return calculateMD5(new ByteArrayInputStream(content.getBytes()));
    }

    public static String calculateMD5(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            return bigInt.toString(16);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }

    public static boolean leadsToFile(File file) {
        return file.getName().contains(".");
    }
}
