package esub;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import org.bouncycastle.crypto.generators.*;

/**
 * Esub implementation using BouncyCastle provider.
 *
 * Copyright (c) 2004 Michael Schierl
 *
 * This file is free software according to the GNU GPL v2, or
 * any later version.
 */
public class Esub {

    private static Esub instance;

    public static Esub getInstance() {
        if (instance == null) instance = new Esub();
        return instance;
    }

    private Esub() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public boolean verify(String subject, String password, String esub) {
        return verify(hash(subject), hash(password), unhex(esub));
    }

    public boolean verify(byte[] subject, byte[] password, byte[] esub) {
        if (esub.length != 24) return false;
        try {
            Key key = new javax.crypto.spec.SecretKeySpec(password, "IDEA");
            Cipher cip = Cipher.getInstance("IDEA/CFB/NoPadding", "BC");
            cip.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.IvParameterSpec(esub, 0, 8));
            byte[] res = cip.doFinal(esub, 8, 16);
            return Arrays.equals(res, subject);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public String encode(String subject, String password) {
        return hex(encode(hash(subject), hash(password)));
    }

    public byte[] encode(byte[] subject, byte[] password) {
        try {
            Key key = new javax.crypto.spec.SecretKeySpec(password, "IDEA");
            Cipher cip = Cipher.getInstance("IDEA/CFB/NoPadding", "BC");
            cip.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cip.getIV();
            byte[] enc = cip.doFinal(subject);
            byte[] res = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, res, 0, iv.length);
            System.arraycopy(enc, 0, res, iv.length, enc.length);
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private byte[] hash(String toHash) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5", "BC");
            md5.update(toHash.getBytes("ISO-8859-1"));
            return md5.digest();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private byte[] unhex(String s) {
        try {
            byte[] res = new byte[s.length() / 2];
            for (int i = 0; i < res.length; i++) {
                res[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            }
            return res;
        } catch (NumberFormatException ex) {
            return new byte[0];
        }
    }

    private String hex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int bb = b[i] & 0xff;
            sb.append(bb < 0x10 ? "0" : "").append(Integer.toHexString(bb));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String pw = "Passwort";
        String sub = "Test-Subject";
        Esub e = Esub.getInstance();
        boolean res;
        res = e.verify(sub, pw, "61a6289b5a0ce7d6d94fff755e8a5ad4b0d196bae1455b7c");
        System.out.println("Correct: " + res);
        res = e.verify(sub, pw, "588edfef32a0fdb4eb6a39721058cf602704371a5753d8bf");
        System.out.println("Correct: " + res);
        res = e.verify(sub, pw, "588edfef32a0fdb4eb6a39721058cf602704371a5753d8bf");
        System.out.println("Incorrect: " + res);
        String esub1 = e.encode(sub, pw);
        String esub2 = e.encode(sub, pw);
        System.out.println(esub1);
        System.out.println(esub2);
        System.out.println("Equal: " + esub1.equals(esub2));
        System.out.println("Verify: " + e.verify(sub, pw, esub1));
        System.out.println("Verify: " + e.verify(sub, pw, esub2));
    }
}
