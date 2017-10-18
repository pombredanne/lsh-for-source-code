package net.sourceforge.java_stratego.stratego.server.shared;

import java.security.MessageDigest;

public class Hash {

    public static String Sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = new byte[40];
            md.update(s.getBytes("iso-8859-1"), 0, s.length());
            hash = md.digest();
            return toHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String toHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
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
}
