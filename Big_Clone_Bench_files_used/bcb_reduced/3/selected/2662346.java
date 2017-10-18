package gawky.crypt;

import java.security.MessageDigest;

public class HashCode {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println(getSHA1Hash("sumsumsum"));
    }

    public static final String addLZ(String s) {
        return "00".substring(0, 2 - s.length()) + s;
    }

    public static String getSHA1Hash(String value) {
        return getHash(value, "SHA-1");
    }

    public static String getMD5Hash(String value) {
        return getHash(value, "MD5");
    }

    public static String getHash(String value, String type) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance(type);
            byte d[] = sha1.digest(value.getBytes());
            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < d.length; i++) buff.append(addLZ(Integer.toHexString(d[i] & 0xFF)));
            return buff.toString();
        } catch (Exception e) {
        }
        return "";
    }
}
