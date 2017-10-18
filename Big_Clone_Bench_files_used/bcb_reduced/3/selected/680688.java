package cz.cvut.phone.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encoding text.
 * @author Frantisek Hradil
 */
public class SHA {

    private static final String TYPE_SHA = "SHA-1";

    private static final String ENCODE = "iso-8859-1";

    private static String convertToHexFormat(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * 
     * @param text Text, which will be encoding.
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     */
    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (text == null || text.length() < 1) {
            return null;
        }
        MessageDigest md = MessageDigest.getInstance(TYPE_SHA);
        md.update(text.getBytes(ENCODE), 0, text.length());
        byte[] sha1hash = new byte[40];
        sha1hash = md.digest();
        return convertToHexFormat(sha1hash);
    }
}
