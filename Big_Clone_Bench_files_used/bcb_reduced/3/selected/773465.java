package android.bluebox.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Crypto31 {

    private static Cipher enCipher;

    private static Cipher deCipher;

    private static String algorithm = "AES";

    private static byte[] salt = new byte[] { 0x7d, 0x60, 0x43, 0x5f, 0x02, (byte) 0xe9, (byte) 0xe0, (byte) 0xae };

    private static String sSalt = "wtf";

    private static int iterationCount = 2020;

    public static void createInstance(String str) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(str.toCharArray());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            PBEParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
            enCipher = Cipher.getInstance(algorithm);
            enCipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
            deCipher = Cipher.getInstance(algorithm);
            deCipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
        } catch (Exception e) {
        }
    }

    public static String encrypt(String str) {
        try {
            byte[] tmp = new byte[1024];
            int pos = 0;
            byte[] input = str.getBytes();
            byte[] output = enCipher.update(input, 0, input.length);
            if (output != null) {
                pos = output.length;
                for (int i = 0; i < pos; i++) {
                    tmp[i] = output[i];
                }
            }
            output = enCipher.doFinal();
            if (output != null) {
                for (int i = 0; i < output.length; i++) {
                    tmp[i + pos] = output[i];
                }
                pos += output.length;
            }
            byte[] result = new byte[pos];
            for (int i = 0; i < pos; i++) {
                result[i] = tmp[i];
            }
            String strBase64 = Base64.encodeToString(result, Base64.DEFAULT);
            return strBase64;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String str) {
        String result = "";
        try {
            byte[] input = Base64.decode(str, Base64.DEFAULT);
            byte[] output = deCipher.update(input, 0, input.length);
            if (output != null) {
                result += new String(output);
            }
            output = deCipher.doFinal();
            if (output != null) {
                result += new String(output);
            }
            return result;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String createMD5(String str) {
        String sig = null;
        String strSalt = str + sSalt;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(strSalt.getBytes(), 0, strSalt.length());
            byte byteData[] = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            sig = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Can not use md5 algorithm");
        }
        return sig;
    }
}
