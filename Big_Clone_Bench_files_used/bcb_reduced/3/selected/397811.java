package com.amazon.merchants.util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class ReversibleCipher {

    private static byte[] SEED_BYTES = null;

    private SecretKeySpec skspec = null;

    static {
        try {
            SEED_BYTES = "We use this string for keying the encoding".getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    public byte[] encode(String strToEnc) {
        try {
            byte[] bytesToEnc = strToEnc.getBytes("UTF-8");
            byte[] someCipherText = cipher().doFinal(bytesToEnc);
            return someCipherText;
        } catch (Exception ex) {
            return strToEnc == null ? null : strToEnc.getBytes();
        }
    }

    public String decode(byte[] bytesToDec) {
        try {
            byte[] somePlainText = decipher().doFinal(bytesToDec);
            return new String(somePlainText, "UTF-8");
        } catch (Exception ex) {
            return new String(bytesToDec);
        }
    }

    private Cipher cipher() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, DigestException {
        SecretKeySpec skspec = keySpec();
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skspec);
        return cipher;
    }

    private Cipher decipher() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, DigestException {
        SecretKeySpec skspec = keySpec();
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skspec);
        return cipher;
    }

    private SecretKeySpec keySpec() throws NoSuchAlgorithmException, UnsupportedEncodingException, DigestException, InvalidKeyException {
        if (skspec == null) {
            skspec = new SecretKeySpec(getBytes(24), "DESede");
        }
        return skspec;
    }

    private byte[] getBytes(int length) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(SEED_BYTES);
        byte[] bytes = new byte[length];
        byte[] sourceBytes = md.digest();
        for (int i = 0; i < length; i++) {
            bytes[i] = sourceBytes[i % sourceBytes.length];
        }
        return bytes;
    }
}
