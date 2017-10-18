package simplespider.simplespider.dao.db4o;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class MD5 {

    private static String convertToHex(final byte[] data) {
        final StringBuffer buf = new StringBuffer();
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

    static String buildMD5(String text) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            {
                final byte[] bytes = text.getBytes("UTF-8");
                text = null;
                md.update(bytes, 0, bytes.length);
            }
            final byte[] md5hash = md.digest();
            return convertToHex(md5hash);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is missing", e);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 is missing", e);
        }
    }
}
