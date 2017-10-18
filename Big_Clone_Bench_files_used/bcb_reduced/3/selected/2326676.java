package ca.ucalgary.cpsc.ebe.fitClipseCore.core;

import java.security.MessageDigest;

public class Sha1 {

    private static String convertToHex(final byte[] data) {
        final StringBuffer buf = new StringBuffer();
        for (final byte element : data) {
            int halfbyte = element >>> 4 & 0x0F;
            int two_halfs = 0;
            do {
                if (0 <= halfbyte && halfbyte <= 9) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + halfbyte - 10));
                }
                halfbyte = element & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String hash(final String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
            return Sha1.convertToHex(sha1hash);
        } catch (final Exception e) {
            return null;
        }
    }
}
