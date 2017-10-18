package sf2.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class KeyWrap {

    protected byte[] key;

    public KeyWrap() {
        this(null);
    }

    public KeyWrap(byte[] key) {
        this.key = key;
    }

    public KeyWrap wrap(byte[] key) {
        this.key = key;
        return this;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof KeyWrap) {
            return Arrays.equals(key, ((KeyWrap) another).key);
        } else return false;
    }

    public static byte[] parse(String str) {
        try {
            str = str.trim();
            byte[] key = new byte[str.length() / 2];
            for (int i = 0; i < key.length; i++) {
                String head = str.substring(0, 2);
                str = str.substring(2);
                key[i] = (byte) Integer.parseInt(head, 16);
            }
            return key;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] hash(byte[] array) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(array);
            return sha1.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String format(byte[] key) {
        String str = "";
        if (key == null) return str;
        for (int i = 0; i < key.length; i++) {
            str += Integer.toHexString(0xFF & key[i]);
        }
        return str;
    }
}
