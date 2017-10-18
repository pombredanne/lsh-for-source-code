package org.openxdm.xcap.common.etag;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ETagGenerator {

    public static String HASH_ALGORITHM = "MD5";

    private static final char[] HEXCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(HEXCHARS[(bytes[i] >> 4) & 0x0f]).append(HEXCHARS[bytes[i] & 0x0f]);
        }
        return sb.toString();
    }

    /**
	 * To generate a safe etag the document selector
	 * and current time can be used as input for a digest.
	 * This method creates such etag by converting each
	 * digest byte to hex chars.
	 * @param documentSelector the document selector part of
	 * a xcap uri 
	 * @return a String with the document etag.
	 */
    public static String generate(String documentSelector) {
        if (documentSelector == null) {
            return null;
        }
        String date = Long.toString(System.currentTimeMillis());
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(documentSelector.getBytes());
            md.update(date.getBytes());
            byte[] digest = md.digest();
            return toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
