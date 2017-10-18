package persistence;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class Password implements Serializable {

    byte encrypted[];

    public Password(String password) {
        encrypted = crypt(password);
    }

    static char salt(byte pw[]) {
        return (char) ((pw[0] << 8) | pw[1]);
    }

    static byte[] crypt(String password) {
        Random r = new SecureRandom();
        byte b[] = new byte[2];
        r.nextBytes(b);
        return crypt(password, (char) ((b[0] << 8) | b[1]));
    }

    static byte[] crypt(String password, char salt) {
        byte b[] = password.getBytes();
        byte a[] = new byte[2 + b.length];
        a[0] = (byte) (salt >> 8);
        a[1] = (byte) (salt & 0xff);
        System.arraycopy(b, 0, a, 2, b.length);
        try {
            b = MessageDigest.getInstance("MD5").digest(a);
            byte c[] = new byte[2 + b.length];
            System.arraycopy(a, 0, c, 0, 2);
            System.arraycopy(b, 0, c, 2, b.length);
            return c;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    boolean match(String password) {
        return Arrays.equals(encrypted, crypt(password, salt(encrypted)));
    }

    boolean match(char pw[]) {
        return match(new String(pw));
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < encrypted.length; i++) buffer.append((i > 0 ? " " : "") + Integer.toHexString(0x100 | (encrypted[i] & 0xff)).substring(1));
        return buffer.toString();
    }
}
