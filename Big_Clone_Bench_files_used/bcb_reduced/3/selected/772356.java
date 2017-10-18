package com.wolfbell.araas.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Compute the message digest for a string.
 * @version $Revision: 1.3 $
 */
public final class DigestProducer {

    private static final String DEFAULT_ALGORITHM = "MD5";

    private MessageDigest md;

    public DigestProducer(String algorithm) throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance(algorithm);
    }

    public DigestProducer() throws NoSuchAlgorithmException {
        this(DEFAULT_ALGORITHM);
    }

    /**
     * Compute a digest from the message and the secret key and prepend
     * it to the message. The digest is generated as a hexasimal string.
     * @returns digest:message
     */
    public String addDigest(String message, String secretkey) {
        return addDigest(message, secretkey, null);
    }

    /**
     * Compute a digest from the message, the secret key and the remote IP address and prepend
     * it to the message. The digest is generated as a hexasimal string.
     * @returns digest:message
     */
    public String addDigest(String message, String secretkey, String remoteAddr) {
        md.update(message.getBytes());
        if (remoteAddr != null) md.update(remoteAddr.getBytes());
        byte digest[] = md.digest(secretkey.getBytes());
        return byte2hexstring(digest) + ":" + message;
    }

    public boolean isDigestValid(String messagewithdigest, String secretkey, String remoteAddr) {
        String message = messagewithdigest.substring(messagewithdigest.indexOf(":") + 1);
        return this.addDigest(message, secretkey, remoteAddr).equals(messagewithdigest);
    }

    private static final String HEXDIGIT = "0123456789abcdef";

    private final String byte2hexstring(byte[] c) {
        int len = c.length;
        StringBuffer out = new StringBuffer();
        int k;
        for (int i = 0; i < len; i++) {
            k = c[i];
            out.append(HEXDIGIT.charAt((k >> 4) & 0x0f));
            out.append(HEXDIGIT.charAt(k & 0x0f));
        }
        return out.toString();
    }
}
