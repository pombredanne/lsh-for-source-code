package jnewsgate.auth;

import jnewsgate.*;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.security.*;

/**
 * A generic hash implementation that uses a
 * <code>java.util.MessageDigest</code> object and hex-encodes the
 * result.
 */
public abstract class MessageDigestHexHash implements Hash {

    private static Logger l = Log.get();

    private MessageDigest md;

    private String encoding;

    protected MessageDigestHexHash(String algorithm, String encoding) {
        this.encoding = encoding;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            l.log(Level.SEVERE, "Could not find algorithm " + algorithm, ex);
        }
    }

    public String hashData(String data, String hashTemplate) {
        try {
            md.reset();
            md.update(data.getBytes(encoding));
            return bytesToHex(md.digest());
        } catch (IOException ex) {
            l.log(Level.SEVERE, "Could not hash data", ex);
            return "<error>";
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xff;
            sb.append(c < 0x10 ? "0" : "").append(Integer.toHexString(c));
        }
        return sb.toString();
    }
}
