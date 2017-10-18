package main;

import java.security.*;

public class PasswordEncryptor {

    public static String m_strHashAlgorithm = "sha-512";

    private MessageDigest m_passwordEncryptor;

    protected boolean m_bCanEncrypt = true;

    private final String m_strPasswordSalt = "4188828493317328682062339";

    private final int m_fnEncryptionRepeat = 8192;

    public PasswordEncryptor() {
        try {
            m_passwordEncryptor = MessageDigest.getInstance(m_strHashAlgorithm);
        } catch (Exception e) {
            m_bCanEncrypt = false;
        }
    }

    public String encryptPassword(String strOriginal) {
        if (strOriginal.isEmpty() || !m_bCanEncrypt) return (strOriginal);
        byte[] nEnc = null;
        try {
            synchronized (m_passwordEncryptor) {
                m_passwordEncryptor.reset();
                String originalSalted = strOriginal + m_strPasswordSalt;
                nEnc = m_passwordEncryptor.digest(originalSalted.getBytes());
                for (int i = 1; i < m_fnEncryptionRepeat; i++) {
                    nEnc = m_passwordEncryptor.digest(nEnc);
                }
            }
            return (new String(nEnc, "ISO-8859-1"));
        } catch (Exception e) {
            m_bCanEncrypt = false;
            return (strOriginal);
        }
    }
}
