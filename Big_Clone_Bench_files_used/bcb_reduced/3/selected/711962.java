package org.plugger.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class helps with Cryptography functionality.
 *
 * @author Antonio Begue
 * @version $Revision: 1.0 $
 */
public class Cryptography {

    /**
     * Returns the SHA1 of the input text.
     * @param text The string to be converted.
     * @return The converted string.
     */
    public String hashTextSHA1(String text) {
        String sha1hash = "";
        try {
            sha1hash = getHash("SHA-1", text, "");
        } catch (Exception ex) {
        }
        return sha1hash;
    }

    /**
     * Returns the MD5 of the input text.
     * @param text The string to be converted.
     * @return The converted string.
     */
    public String hashTextMD5(String text) {
        String md5hash = "";
        try {
            md5hash = getHash("MD5", text, "");
        } catch (Exception ex) {
        }
        return md5hash;
    }

    /**
     * Returns the type MessageDigest of the input text with the input salt.
     * @param type The type of the MessageDigest algorithm.
     * @param text The string to be converted.
     * @param salt The salt for the conversion.
     * @return The converted string.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String getHash(String type, String text, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance(type);
        byte[] hash = new byte[md.getDigestLength()];
        if (!salt.isEmpty()) {
            md.update(salt.getBytes("iso-8859-1"), 0, salt.length());
        }
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        hash = md.digest();
        return convertToHex(hash);
    }

    private String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
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
}
