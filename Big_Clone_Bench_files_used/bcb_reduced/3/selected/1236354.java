package org.bims.bimswebaccess.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Support class to provide hash functionalities.
 * @author Oscar Mora Perez
 */
public final class BIMSHashUtils {

    /**
     * Constant to represent 32.
     */
    private static final int FOUR_BYTES = 32;

    /**
     * Constant to represent 4.
     */
    private static final int FOUR = 4;

    /**
     * Constant to represent 9.
     */
    private static final int NINE = 9;

    /**
     * Constant to represent 10.
     */
    private static final int TEN = 10;

    /**
     * Constant to represent 0x0F.
     */
    private static final int ZERO_HEX = 0x0F;

    /**
     * Private constructor due this is a support class, and their methods
     * are static.
     */
    private BIMSHashUtils() {
    }

    /**
     * Generates MD5 of inputted String.
     * @param text Base text to generate MD5
     * @return Hash of the input
     * @throws java.security.NoSuchAlgorithmException No algorithm found
     * @throws java.io.UnsupportedEncodingException Codding error
     */
    public static String md5(final String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[FOUR_BYTES];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }

    /**
     * Simple function to convert a byte to Hexadecimal.
     * @param data byto to convert to Hex
     * @return the byte converter to Hex
     */
    private static String convertToHex(final byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> FOUR) & ZERO_HEX;
            int twoHalfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= NINE)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - TEN)));
                }
                halfbyte = data[i] & ZERO_HEX;
            } while (twoHalfs++ < 1);
        }
        return buf.toString();
    }
}
