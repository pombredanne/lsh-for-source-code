package com.flyox.game.militarychess.util;

import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * @author crazy 孫偉
 * @version 1.0
 */
public class Secret {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String res = Secret.genDigest("1111");
        System.out.println(res.length() + "   " + res);
    }

    public static String genDigest(String info) {
        MessageDigest alga;
        byte[] digesta = null;
        try {
            alga = MessageDigest.getInstance("SHA-1");
            alga.update(info.getBytes());
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byte2hex(digesta);
    }

    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs = hs + "0" + stmp; else hs = hs + stmp;
        }
        return hs.toUpperCase();
    }

    /**
	 * 加密
	 * @param msg
	 * @param sk
	 * @return
	 */
    public static byte[] encryptMsg(byte[] msg, SecretKey key) {
        Cipher cipher;
        byte[] ciphertext;
        try {
            cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            ciphertext = cipher.doFinal(msg);
            return ciphertext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 解密
	 * @param player
	 * @param request
	 * @return
	 */
    public static byte[] decipherMsg(byte[] content, SecretKey key) {
        try {
            Cipher aliceCipher = Cipher.getInstance("DES");
            aliceCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] recovered = aliceCipher.doFinal(content);
            return recovered;
        } catch (KeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
