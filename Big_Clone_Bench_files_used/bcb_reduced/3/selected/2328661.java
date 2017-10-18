package org.elephantt.webby.security;

import org.apache.commons.codec.binary.Base64;
import org.elephantt.webby.StringUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

/**
 * Generates crypotgraphic hashes. Useful for storing passwords and such.
 * 
 * See http://en.wikipedia.org/wiki/Cryptographic_hash_function
 */
public class SecureHashingService {

    private static final String defaultDigestAlgo = "SHA-1";

    private final String digestAlgorithm;

    private final byte[] salt;

    public SecureHashingService(byte[] salt, String digestAlgorithm) throws NoSuchAlgorithmException {
        this.salt = salt;
        String effectiveDigestAlgo = digestAlgorithm;
        if (effectiveDigestAlgo == null) effectiveDigestAlgo = defaultDigestAlgo;
        this.digestAlgorithm = effectiveDigestAlgo;
    }

    public SecureHashingService(byte[] salt) throws NoSuchAlgorithmException {
        this(salt, null);
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] hash(byte[] value) {
        try {
            MessageDigest digester = MessageDigest.getInstance(digestAlgorithm);
            digester.update(salt);
            digester.update(value);
            return digester.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
   * Hashes the string value and returns the base64-encoded result. Same as the byte form but uses the default
   * Webby string encoding.
   */
    public String hash(String value) {
        return StringUtil.bytesToString(Base64.encodeBase64(hash(StringUtil.stringToBytes(value))));
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecureHashingService svc = new SecureHashingService("ZpFGeS35@$".getBytes(), "SHA-1");
        String value = "hash me baby";
        System.out.println("value: " + value);
        System.out.println("hash: " + new String(Base64.encodeBase64(svc.hash(value.getBytes("UTF-8")))));
    }
}
