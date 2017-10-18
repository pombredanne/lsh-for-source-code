package cn.edu.dutir.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5 {

    private static final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static MessageDigest mMD5;

    private MD5() {
    }

    public static MessageDigest getInstance() {
        if (mMD5 == null) {
            try {
                mMD5 = MessageDigest.getInstance("md5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return mMD5;
    }

    private static String byteToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexDigits[(b[i] & 0xf0) >>> 4]);
            sb.append(hexDigits[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String digest(String text) {
        MessageDigest md5 = getInstance();
        byte bytes[] = text.getBytes();
        md5.update(bytes);
        return byteToHexString(md5.digest());
    }

    public static void main(String args[]) {
        String text = "http://wwhhss.blogchina.com/1274540.html";
        System.out.println(digest(text));
    }
}
