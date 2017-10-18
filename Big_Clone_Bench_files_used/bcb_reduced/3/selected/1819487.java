package tools.util;

import java.util.*;
import java.io.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import com.sun.crypto.provider.SunJCE;
import javax.crypto.*;

public class Crypto {

    static byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };

    private static int count = 20;

    private static char[] password = null;

    private static boolean success = false;

    private static int tries = 0;

    public static void setPassword(String p) {
        tries++;
        if (tries > 5) {
            System.out.println("Unable to Set Crypto Password");
            return;
        }
        if (password == null || !success) password = p.toCharArray();
        SunJCE jce = new SunJCE();
        Security.addProvider(jce);
    }

    public static byte[] encrypt(byte[] ciphertext) throws Exception {
        return encrypt(ciphertext, password);
    }

    public static byte[] encrypt(byte[] cleartext, char[] pass) throws Exception {
        return encrypt(cleartext, pass, "DES");
    }

    public static byte[] encrypt(byte[] cleartext, String inp, String alg) throws Exception {
        FileInputStream fin = new FileInputStream(inp);
        byte[] ret = encrypt(cleartext, StreamUtil.readChars(fin), alg);
        fin.close();
        return ret;
    }

    public static Cipher getBlowfishCipher(String fn, boolean tf) throws Exception {
        return getBlowfishCipher(true, fn, tf);
    }

    public static final String digest(String credentials) {
        return digest(credentials, "SHA", null);
    }

    public static final String digest(String credentials, String algorithm) {
        return digest(credentials, algorithm, null);
    }

    public static final String digest(String credentials, String algorithm, String encoding) {
        try {
            MessageDigest md = (MessageDigest) MessageDigest.getInstance(algorithm).clone();
            if (encoding == null) {
                md.update(credentials.getBytes());
            } else {
                md.update(credentials.getBytes(encoding));
            }
            return (convert(md.digest()));
        } catch (Exception ex) {
            tools.util.LogMgr.err("Crypto.digest " + ex.toString());
            return credentials;
        }
    }

    public static final int[] DEC = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 00, 01, 02, 03, 04, 05, 06, 07, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

    public static byte[] convert(String digits) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < digits.length(); i += 2) {
            char c1 = digits.charAt(i);
            if ((i + 1) >= digits.length()) throw new IllegalArgumentException(("convert.odd"));
            char c2 = digits.charAt(i + 1);
            byte b = 0;
            if ((c1 >= '0') && (c1 <= '9')) b += ((c1 - '0') * 16); else if ((c1 >= 'a') && (c1 <= 'f')) b += ((c1 - 'a' + 10) * 16); else if ((c1 >= 'A') && (c1 <= 'F')) b += ((c1 - 'A' + 10) * 16); else throw new IllegalArgumentException(("convert.bad"));
            if ((c2 >= '0') && (c2 <= '9')) b += (c2 - '0'); else if ((c2 >= 'a') && (c2 <= 'f')) b += (c2 - 'a' + 10); else if ((c2 >= 'A') && (c2 <= 'F')) b += (c2 - 'A' + 10); else throw new IllegalArgumentException(("convert.bad"));
            baos.write(b);
        }
        return (baos.toByteArray());
    }

    /**
     * Convert a byte array into a printable format containing a
     * String of hexadecimal digit characters (two per byte).
     *
     * @param bytes Byte array representation
     */
    public static String convert(byte bytes[]) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(convertDigit((int) (bytes[i] >> 4)));
            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
        }
        return (sb.toString());
    }

    private static char convertDigit(int value) {
        value &= 0x0f;
        if (value >= 10) return ((char) (value - 10 + 'a')); else return ((char) (value + '0'));
    }

    public static Cipher getBlowfishCipher(boolean b64, String fn, boolean tf) throws Exception {
        byte[] raw = null;
        File f = new File(fn);
        if (f.exists() && f.length() > 0) raw = FileUtil.getBytesFromFile(f);
        if (raw == null || raw.length < 1) {
            KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
            String size = System.getProperty("Blowfish.Key.Size");
            if (size != null) kgen.init(Integer.parseInt(size));
            SecretKey skey = kgen.generateKey();
            raw = skey.getEncoded();
            byte[] rawb = raw;
            if (b64) rawb = Base642.encode(raw);
            FileUtil.writeBytesToFile(f, rawb);
        } else if (b64) raw = Base642.decode(raw);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "Blowfish");
        Cipher cipher = Cipher.getInstance("Blowfish");
        if (tf) cipher.init(Cipher.ENCRYPT_MODE, skeySpec); else cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher;
    }

    public static Cipher getCipher(String inp, boolean tf, String alg) throws Exception {
        FileInputStream fin = new FileInputStream(inp);
        char[] pass = StreamUtil.readChars(fin);
        fin.close();
        return getCipher(pass, tf, alg);
    }

    public static byte[] encrypt(byte[] cleartext, char[] pass, String alg) throws Exception {
        return encrypt(cleartext, pass, alg, salt);
    }

    public static String encryptString(String cleartext) throws Exception {
        String ts = tools.util.AlphaNumeric.generateRandomAlphNumeric(8);
        return "$1$" + ts + "$" + encryptString(cleartext, ts);
    }

    public static String encryptString(String cleartext, String ts) throws Exception {
        return new String(Base64Encoder.toBase64(encrypt(cleartext.getBytes(), cleartext.toCharArray(), "DES", ts.getBytes())));
    }

    public static byte[] encrypt(byte[] cleartext, char[] pass, String alg, byte[] tsalt) throws Exception {
        PBEKeySpec pbeKeySpec;
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac;
        pbeParamSpec = new PBEParameterSpec(tsalt, count);
        pbeKeySpec = new PBEKeySpec(pass);
        keyFac = SecretKeyFactory.getInstance("PBEWithMD5And" + alg);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5And" + alg);
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        return pbeCipher.doFinal(cleartext);
    }

    public static byte[] decrypt(byte[] ciphertext) throws Exception {
        return decrypt(ciphertext, password);
    }

    public static byte[] decrypt(byte[] ciphertext, char[] pass) throws Exception {
        PBEKeySpec pbeKeySpec;
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac;
        pbeParamSpec = new PBEParameterSpec(salt, count);
        pbeKeySpec = new PBEKeySpec(pass);
        keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        byte[] b = pbeCipher.doFinal(ciphertext);
        if (!success && b != null) {
            success = true;
        }
        return b;
    }

    public static Cipher getCipher() throws Exception {
        return getCipher(password, true);
    }

    public static Cipher getCipher(char[] pass) throws Exception {
        return getCipher(pass, true);
    }

    public static Cipher getCipher(char[] pass, boolean tf) throws Exception {
        return getCipher(pass, tf, "DES");
    }

    public static Cipher getCipher(char[] pass, boolean tf, String alg) throws Exception {
        if (pass == null) pass = password;
        PBEKeySpec pbeKeySpec;
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac;
        pbeParamSpec = new PBEParameterSpec(salt, count);
        pbeKeySpec = new PBEKeySpec(pass);
        keyFac = SecretKeyFactory.getInstance("PBEWithMD5And" + alg);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5And" + alg);
        if (tf) pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec); else pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        return pbeCipher;
    }
}
