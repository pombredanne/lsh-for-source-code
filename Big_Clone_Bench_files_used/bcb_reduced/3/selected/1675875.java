package org.aha.mf4j.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 *   Utility method to compute MD5 hash.
 * </p>
 * @author Arne Halvorsen (aha42).
 */
public final class MD5 {

    private MD5() {
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
   * <p>
   *   Computes MD5 hash.
   * @param text Text to compute hash for.
   * @return Hash.
   * @throws IOException If fails.
   */
    public static String compute(String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            md.update(text.getBytes("UTF-8"), 0, text.length());
            md5hash = md.digest();
            return convertToHex(md5hash);
        } catch (NoSuchAlgorithmException nax) {
            RuntimeException rx = new IllegalStateException();
            rx.initCause(rx);
            throw rx;
        } catch (UnsupportedEncodingException uex) {
            RuntimeException rx = new IllegalStateException();
            rx.initCause(uex);
            throw rx;
        }
    }
}
