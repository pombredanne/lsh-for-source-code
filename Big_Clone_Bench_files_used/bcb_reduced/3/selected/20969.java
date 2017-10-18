package babylon;

import java.security.*;

public class PasswordEncryptor {

    public static String hashAlgorithm = "sha-512";

    private MessageDigest passwordEncryptor;

    protected boolean canEncrypt = true;

    private final String passwordSalt = "4188828493317328682062339";

    private final int encryptionRepeat = 8192;

    public PasswordEncryptor() {
        try {
            passwordEncryptor = MessageDigest.getInstance(hashAlgorithm);
        } catch (Exception e) {
            canEncrypt = false;
        }
    }

    public String encryptPassword(String original) {
        if (original.equals("") || !canEncrypt) return (original);
        byte[] enc = null;
        try {
            synchronized (passwordEncryptor) {
                passwordEncryptor.reset();
                String originalSalted = original + passwordSalt;
                enc = passwordEncryptor.digest(originalSalted.getBytes());
                for (int i = 1; i < encryptionRepeat; i++) {
                    enc = passwordEncryptor.digest(enc);
                }
            }
            return (new String(enc, "ISO-8859-1"));
        } catch (Exception e) {
            canEncrypt = false;
            return (original);
        }
    }
}
