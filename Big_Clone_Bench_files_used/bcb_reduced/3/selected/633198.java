package org.yoshiori.commons.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;

/**
 * @author yoshiori
 * 
 */
public class MessageDigest {

    private java.security.MessageDigest digest;

    private String encoding = "UTF-8";

    public MessageDigest(DigestAlgorithm algorithm) {
        try {
            digest = java.security.MessageDigest.getInstance(algorithm.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param data
     * @return
     */
    public byte[] getByts(byte[] data) {
        digest.update(data);
        return digest.digest();
    }

    /**
     * @param data
     * @return
     */
    public byte[] getByts(String data) {
        try {
            digest.update(data.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return digest.digest();
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public byte[] getByts(File file) throws IOException {
        DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), digest);
        while (digestInputStream.read() != -1) {
        }
        byte[] bytes = digestInputStream.getMessageDigest().digest();
        digestInputStream.close();
        return bytes;
    }

    /**
     * @param data
     * @return
     */
    public String getHexString(byte[] data) {
        return toHexString(getByts(data));
    }

    /**
     * @param data
     * @return
     */
    public String getHexString(String data) {
        return toHexString(getByts(data));
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public String getHexString(File file) throws IOException {
        return toHexString(getByts(file));
    }

    /**
     * @param bytes
     * @return
     */
    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int b : bytes) {
            b = b & 0xff;
            if (b < 0xf) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    /**
	 * @return the encoding
	 */
    public String getEncoding() {
        return encoding;
    }

    /**
	 * @param encoding the encoding to set
	 */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @author yoshiori
     * 
     */
    public enum DigestAlgorithm {

        MD2("MD2"), MD5("MD5"), SHA_1("SHA-1"), SHA_256("SHA-256"), SHA_384("SHA-384"), SHA_512("SHA-512");

        private String algorithmName;

        /**
         * @param algorithmName
         */
        private DigestAlgorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String toString() {
            return algorithmName;
        }
    }
}
