package net.rptools.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents the MD5 key for a certain set of data.
 * Can be used in maps as keys.
 */
public class MD5Key implements Serializable {

    private static MessageDigest md5Digest;

    String id;

    static {
        try {
            md5Digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public MD5Key() {
    }

    public MD5Key(String id) {
        this.id = id;
    }

    public MD5Key(byte[] data) {
        id = encodeToHex(digestData(data));
    }

    public MD5Key(InputStream data) {
        id = encodeToHex(digestData(data));
    }

    public String toString() {
        return id;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MD5Key)) {
            return false;
        }
        return id.equals(((MD5Key) obj).id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    private static synchronized byte[] digestData(byte[] data) {
        md5Digest.reset();
        md5Digest.update(data);
        return md5Digest.digest();
    }

    private static synchronized byte[] digestData(InputStream data) {
        md5Digest.reset();
        int b;
        try {
            while (((b = data.read()) >= 0)) {
                md5Digest.update((byte) b);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return md5Digest.digest();
    }

    private static String encodeToHex(byte[] data) {
        StringBuilder strbuild = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() < 2) {
                strbuild.append("0");
            }
            if (hex.length() > 2) {
                hex = hex.substring(hex.length() - 2);
            }
            strbuild.append(hex);
        }
        return strbuild.toString();
    }
}
