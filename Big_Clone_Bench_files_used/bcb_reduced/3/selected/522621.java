package net.sf.lwdba.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Digest {

    public static String getDigest(String str, String privateKey) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte buf[] = str.getBytes();
        byte _key[] = privateKey.getBytes();
        md.update(buf, 0, buf.length);
        return toHexString(md.digest(_key));
    }

    public static String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            if (b[i] < 0) sb.append(Integer.toHexString(256 + b[i])); else {
                if (b[i] < 16) sb.append("0");
                sb.append(Integer.toHexString(b[i]));
            }
        }
        return sb.toString();
    }
}
