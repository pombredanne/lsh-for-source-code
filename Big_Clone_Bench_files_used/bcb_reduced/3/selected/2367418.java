package in.co.codedoc.encrypt;

import in.co.codedoc.util.HexUtil;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Iterator;

public class MD5 {

    public static String Hash(String text) {
        return Hash(text.getBytes());
    }

    public static String Hash(byte[] bytes) {
        return Hash(new byte[][] { bytes });
    }

    public static String Hash(long[] longs) {
        byte[] bytes = new byte[longs.length * 8];
        for (int i = 0; i < longs.length; i++) {
            for (int j = 0; j < 8; j++) {
                bytes[8 * i + j] = (byte) (longs[i] >> (j * 8));
            }
        }
        return Hash(bytes);
    }

    public static String Hash(Collection<Long> longs) {
        byte[] bytes = new byte[longs.size() * 8];
        Iterator<Long> iter = longs.iterator();
        int i = 0;
        while (iter.hasNext()) {
            long cv = iter.next();
            for (int j = 0; j < 8; j++) {
                bytes[8 * i + j] = (byte) (cv >> (j * 8));
            }
            i++;
        }
        return Hash(bytes);
    }

    public static String Hash(byte[][] bss) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            for (byte[] bs : bss) {
                md.update(bs);
            }
            byte[] digest = md.digest();
            String hash = HexUtil.PrintHexDigits(digest);
            return hash;
        } catch (Throwable th) {
            throw new RuntimeException("Failed to hash data:" + th.getMessage(), th);
        }
    }
}
