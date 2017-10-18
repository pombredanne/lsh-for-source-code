package org.lindenb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** public methods encrypting a string  */
public class Digest {

    private String algorithm;

    private String encoding;

    public static final Digest SHA1 = new Digest("SHA-1", "UTf-8");

    public static final Digest MD5 = new Digest("MD5", "UTf-8");

    private Digest(String algorithm, String encoding) {
        this.algorithm = algorithm;
        this.encoding = encoding;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isImplemented() {
        try {
            MessageDigest.getInstance(getAlgorithm());
            return true;
        } catch (NoSuchAlgorithmException err) {
            return false;
        }
    }

    public String encrypt(File file) throws IOException {
        byte[] hash = null;
        try {
            FileInputStream in = new FileInputStream(file);
            MessageDigest instance = MessageDigest.getInstance(getAlgorithm());
            byte array[] = new byte[2048];
            int len;
            while ((len = in.read(array)) != -1) {
                instance.update(array, 0, len);
            }
            hash = instance.digest();
            in.close();
        } catch (NoSuchAlgorithmException err) {
            throw new IOException(err);
        } catch (UnsupportedEncodingException err) {
            throw new IOException(err);
        }
        return getHashString(hash);
    }

    public String encrypt(String string) {
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance(getAlgorithm()).digest(string.getBytes(getEncoding()));
        } catch (NoSuchAlgorithmException err) {
            throw new Error("no " + getAlgorithm() + " support in this VM");
        } catch (UnsupportedEncodingException err) {
            throw new Error("no " + getAlgorithm() + " support in this VM");
        }
        return getHashString(hash);
    }

    private static String getHashString(byte[] hash) {
        StringBuilder hashString = new StringBuilder(42);
        for (int i = 0; i < hash.length; ++i) {
            String x = Integer.toHexString(hash[i] & 0xff);
            if (x.length() < 2) hashString.append("0");
            hashString.append(x);
        }
        return hashString.toString();
    }
}
