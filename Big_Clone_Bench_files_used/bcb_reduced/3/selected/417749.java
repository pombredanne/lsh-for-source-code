package com.notmacchallenge;

import javax.crypto.*;
import java.security.MessageDigest;
import javax.crypto.spec.DESKeySpec;
import org.apache.commons.codec.binary.Base64;

public class DesEncrypter {

    Cipher ecipher;

    Cipher dcipher;

    public DesEncrypter(String key, boolean base64) {
        try {
            key = getHash(key, base64);
            doInit(key);
        } catch (Exception e) {
        }
    }

    public DesEncrypter(String key) {
        try {
            key = getHash(key, false);
            doInit(key);
        } catch (Exception e) {
        }
    }

    public void doInit(String key) throws Exception {
        while (((float) key.length() / 8) != key.length() / 8) key += "Z";
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        ecipher = Cipher.getInstance("DES");
        dcipher = Cipher.getInstance("DES");
        ecipher.init(Cipher.ENCRYPT_MODE, secretKey);
        dcipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    public String getHash(String key, boolean base64) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(key.getBytes());
        if (base64) return new String(new Base64().encode(md.digest()), "UTF8"); else return new String(md.digest(), "UTF8");
    }

    public String encrypt(String str, String method) {
        return encrypt(str, method, false);
    }

    public String encrypt(String str, String method, boolean base64) {
        if (method.equals("SHA") || str.startsWith("SHA:")) {
            try {
                if (str.startsWith("SHA:")) return str; else {
                    if (base64) return "SHA:" + getHash(str, base64).trim(); else return "SHA:" + new String(new Base64().encode(getHash(str, base64).getBytes()), "UTF8").trim();
                }
            } catch (Exception e) {
            }
            return null;
        } else if (method.equals("CRYPT3") || str.startsWith("CRYPT3:")) {
            try {
                if (str.startsWith("CRYPT3:")) return str; else {
                    if (base64) return "SHA:" + getHash(str, base64).trim(); else return "SHA:" + new String(new Base64().encode(getHash(str, base64).getBytes()), "UTF8").trim();
                }
            } catch (Exception e) {
            }
            return null;
        }
        try {
            byte[] utf8 = str.getBytes("UTF8");
            byte[] enc = ecipher.doFinal(utf8);
            return new String(new Base64().encode(enc), "UTF8");
        } catch (javax.crypto.BadPaddingException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (java.io.IOException e) {
        }
        return null;
    }

    public String decrypt(String str) {
        if (str.startsWith("SHA:")) return str;
        if (str.startsWith("CRYPT3:")) return str;
        try {
            byte[] dec = new Base64().decode(str.getBytes());
            byte[] utf8 = dcipher.doFinal(dec);
            return new String(utf8, "UTF8");
        } catch (javax.crypto.BadPaddingException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (java.io.IOException e) {
        }
        return null;
    }
}
