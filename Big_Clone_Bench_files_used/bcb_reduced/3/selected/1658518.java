package de.objectcode.time4u.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import de.objectcode.time4u.StorePlugin;

public class PasswordCipher {

    private static String CIPHER = "AES";

    private static String DIGEST = "SHA1";

    private static String RANDOM = "SHA1PRNG";

    private static int ITERATIONS = 20;

    private static int SALT_SIZE = 20;

    public static String encode(String cryptPassword, Credentials credentials) {
        byte[] salt = new byte[SALT_SIZE];
        try {
            SecureRandom random = SecureRandom.getInstance(RANDOM);
            random.nextBytes(salt);
        } catch (Exception e) {
            StorePlugin.getDefault().log(e);
        }
        return new String(Base64.encode(encode(cryptPassword, credentials.tobyteArray(), salt)));
    }

    public static byte[] encode(String cryptPassword, byte[] credentials, byte[] salt) {
        try {
            MessageDigest digester = MessageDigest.getInstance(DIGEST);
            SecureRandom random = SecureRandom.getInstance(RANDOM);
            digester.reset();
            for (int i = 0; i < ITERATIONS; i++) {
                digester.update(salt);
                digester.update(cryptPassword.getBytes("UTF-8"));
            }
            byte[] hash = digester.digest();
            random.setSeed(hash);
            int maxKeySize = Cipher.getMaxAllowedKeyLength(CIPHER);
            KeyGenerator generator = KeyGenerator.getInstance(CIPHER);
            generator.init(maxKeySize, random);
            SecretKey key = generator.generateKey();
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherOut = cipher.doFinal(credentials);
            byte[] ret = new byte[salt.length + cipherOut.length];
            System.arraycopy(salt, 0, ret, 0, salt.length);
            System.arraycopy(cipherOut, 0, ret, salt.length, cipherOut.length);
            return ret;
        } catch (Exception e) {
            StorePlugin.getDefault().log(e);
        }
        return new byte[0];
    }

    public static Credentials decode(String cryptPassword, String encoded) {
        byte[] data = Base64.decode(encoded.toCharArray());
        byte[] salt = new byte[SALT_SIZE];
        byte[] cipherIn = new byte[data.length - salt.length];
        System.arraycopy(data, 0, salt, 0, salt.length);
        System.arraycopy(data, salt.length, cipherIn, 0, cipherIn.length);
        return new Credentials(decode(cryptPassword, cipherIn, salt));
    }

    public static byte[] decode(String cryptPassword, byte[] encoded, byte[] salt) {
        try {
            MessageDigest digester = MessageDigest.getInstance(DIGEST);
            SecureRandom random = SecureRandom.getInstance(RANDOM);
            digester.reset();
            for (int i = 0; i < ITERATIONS; i++) {
                digester.update(salt);
                digester.update(cryptPassword.getBytes("UTF-8"));
            }
            byte[] hash = digester.digest();
            random.setSeed(hash);
            int maxKeySize = Cipher.getMaxAllowedKeyLength(CIPHER);
            KeyGenerator generator = KeyGenerator.getInstance(CIPHER);
            generator.init(maxKeySize, random);
            SecretKey key = generator.generateKey();
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = cipher.doFinal(encoded);
            return decoded;
        } catch (Exception e) {
            StorePlugin.getDefault().log(e);
        }
        return new byte[0];
    }

    public static class Credentials {

        private String m_userId;

        private String m_password;

        private String m_url;

        public Credentials(String userId, String password, String url) {
            m_userId = userId;
            m_password = password;
            m_url = url;
        }

        public Credentials(byte[] data) {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            DataInputStream dataInput = new DataInputStream(is);
            try {
                m_userId = dataInput.readUTF();
                m_password = dataInput.readUTF();
                m_url = dataInput.readUTF();
            } catch (Exception e) {
                StorePlugin.getDefault().log(e);
            }
        }

        public String getPassword() {
            return m_password;
        }

        public String getUrl() {
            return m_url;
        }

        public String getUserId() {
            return m_userId;
        }

        public byte[] tobyteArray() {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(os);
            try {
                dataOutput.writeUTF(m_userId);
                dataOutput.writeUTF(m_password);
                dataOutput.writeUTF(m_url);
                dataOutput.flush();
                dataOutput.close();
            } catch (Exception e) {
                StorePlugin.getDefault().log(e);
            }
            return os.toByteArray();
        }
    }
}
