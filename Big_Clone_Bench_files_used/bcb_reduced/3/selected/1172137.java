package es.auto.cipher;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class CipherManager {

    public static enum Algorithms {

        PBEWithMD5AndDES
    }

    ;

    private Cipher c;

    public CipherManager(Algorithms algotithm) {
        try {
            c = Cipher.getInstance(algotithm.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public byte[] encript(Key key, String text) {
        try {
            c.init(Cipher.ENCRYPT_MODE, key);
            return c.doFinal(text.getBytes());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decript(Key key, byte[] encodedText) {
        try {
            c.init(Cipher.DECRYPT_MODE, key, c.getParameters());
            return new String(c.doFinal(encodedText));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Key getSecretKey(String key) {
        return new SecretKeySpec(key.getBytes(), c.getAlgorithm());
    }

    public static String getEncodedWithMD5(String texto) {
        StringBuffer output = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(texto.getBytes());
            int size = b.length;
            output = new StringBuffer(size);
            for (int i = 0; i < size; i++) {
                int u = b[i] & 255;
                if (u < 16) {
                    output.append("0" + Integer.toHexString(u));
                } else {
                    output.append(Integer.toHexString(u));
                }
            }
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return output.toString();
    }
}
