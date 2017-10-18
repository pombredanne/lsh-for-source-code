package erki.talk;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Crypto {

    private Cipher encCipher;

    private Cipher decCipher;

    public static void main(String[] args) {
        try {
            System.out.println(toSHA("test"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Crypto(SecretKey desKey) {
        try {
            encCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            encCipher.init(Cipher.ENCRYPT_MODE, desKey);
            decCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            decCipher.init(Cipher.DECRYPT_MODE, desKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String message) throws IllegalBlockSizeException, BadPaddingException {
        return DHKeyExchange.toHexString(encCipher.doFinal(message.getBytes()));
    }

    public String decrypt(String encrypted) throws IllegalBlockSizeException, BadPaddingException, IOException {
        return DHKeyExchange.toString(decCipher.doFinal(DHKeyExchange.toBytes(encrypted)));
    }

    public static String toSHA(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.update(message.getBytes());
        return DHKeyExchange.toHexString(digest.digest());
    }
}
