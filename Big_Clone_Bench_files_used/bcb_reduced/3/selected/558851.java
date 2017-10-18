package helper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public final class EncryptionService {

    private static EncryptionService instance;

    private EncryptionService() {
    }

    public synchronized String encrypt(String text) throws Exception {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new Exception(e.getMessage());
        }
        md.update(text.getBytes());
        byte raw[] = md.digest();
        String hash = "";
        for (int i = 0; i < raw.length; i++) {
            byte temp = raw[i];
            String s = Integer.toHexString(new Byte(temp));
            while (s.length() < 2) {
                s = "0" + s;
            }
            s = s.substring(s.length() - 2);
            hash += s;
        }
        return hash;
    }

    public static synchronized EncryptionService getInstance() {
        if (instance == null) {
            return new EncryptionService();
        } else {
            return instance;
        }
    }
}
