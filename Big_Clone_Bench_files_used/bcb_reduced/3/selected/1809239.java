package org.openXpertya.util;

import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Descripción de Clase
 *
 *
 * @version    2.2, 12.10.07
 * @author     Equipo de Desarrollo de openXpertya
 */
public class Secure {

    /** Descripción de Campos */
    private static Cipher s_cipher = null;

    /** Descripción de Campos */
    private static SecretKey s_key = null;

    /** Descripción de Campos */
    private static Logger log = Logger.getLogger(Secure.class.getName());

    /** Descripción de Campos */
    public static final String CLEARTEXT = "xyz";

    /**
     * Descripción de Método
     *
     *
     * @param hexString
     *
     * @return
     */
    public static byte[] convertHexString(String hexString) {
        if ((hexString == null) || (hexString.length() == 0)) {
            return null;
        }
        int size = hexString.length() / 2;
        byte[] retValue = new byte[size];
        String inString = hexString.toLowerCase();
        try {
            for (int i = 0; i < size; i++) {
                int index = i * 2;
                int ii = Integer.parseInt(inString.substring(index, index + 2), 16);
                retValue[i] = (byte) ii;
            }
            return retValue;
        } catch (Exception e) {
            log.finest(hexString + " - " + e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Descripción de Método
     *
     *
     * @param bytes
     *
     * @return
     */
    public static String convertToHexString(byte[] bytes) {
        int size = bytes.length;
        StringBuffer buffer = new StringBuffer(size * 2);
        for (int i = 0; i < size; i++) {
            int x = bytes[i];
            if (x < 0) {
                x += 256;
            }
            String tmp = Integer.toHexString(x);
            if (tmp.length() == 1) {
                buffer.append("0");
            }
            buffer.append(tmp);
        }
        return buffer.toString();
    }

    /**
     * Descripción de Método
     *
     *
     * @param value
     *
     * @return
     */
    public static String decrypt(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return value;
        }
        if (value.startsWith(CLEARTEXT)) {
            return value.substring(3);
        }
        byte[] data = convertHexString(value);
        if (data == null) {
            return null;
        }
        if (s_cipher == null) {
            initCipher();
        }
        if ((s_cipher != null) && (value != null) && (value.length() > 0)) {
            try {
                AlgorithmParameters ap = s_cipher.getParameters();
                s_cipher.init(Cipher.DECRYPT_MODE, s_key, ap);
                byte[] out = s_cipher.doFinal(data);
                String retValue = new String(out);
                log.finest(value + " => " + retValue);
                return retValue;
            } catch (Exception ex) {
                log.log(Level.SEVERE, value, ex);
            }
        }
        return value;
    }

    /**
     * Descripción de Método
     *
     *
     * @param value
     *
     * @return
     */
    public static String encrypt(String value) {
        String clearText = value;
        if (clearText == null) {
            clearText = "";
        }
        if (s_cipher == null) {
            initCipher();
        }
        if (s_cipher != null) {
            try {
                s_cipher.init(Cipher.ENCRYPT_MODE, s_key);
                byte[] encBytes = s_cipher.doFinal(clearText.getBytes());
                String encString = convertToHexString(encBytes);
                log.finest(value + " => " + encString);
                return encString;
            } catch (Exception ex) {
                log.log(Level.SEVERE, value, ex);
            }
        }
        return CLEARTEXT + value;
    }

    /**
     * Descripción de Método
     *
     *
     * @param key
     *
     * @return
     */
    public static int hash(String key) {
        long tableSize = 2147483647;
        long hashValue = 0;
        for (int i = 0; i < key.length(); i++) {
            hashValue = (37 * hashValue) + (key.charAt(i) - 31);
        }
        hashValue %= tableSize;
        if (hashValue < 0) {
            hashValue += tableSize;
        }
        int retValue = (int) hashValue;
        return retValue;
    }

    /**
     * Descripción de Método
     *
     */
    private static synchronized void initCipher() {
        try {
            s_cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            if (false) {
                KeyGenerator keygen = KeyGenerator.getInstance("DES");
                s_key = keygen.generateKey();
                byte[] key = s_key.getEncoded();
                StringBuffer sb = new StringBuffer("Key ").append(s_key.getAlgorithm()).append("(").append(key.length).append(")= ");
                for (int i = 0; i < key.length; i++) {
                    sb.append(key[i]).append(",");
                }
                log.info(sb.toString());
            } else {
                s_key = new javax.crypto.spec.SecretKeySpec(new byte[] { 100, 25, 28, -122, -26, 94, -3, -26 }, "DES");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "cipher", ex);
        }
    }

    /**
     * Descripción de Método
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        String[] testString = new String[] { "This is a test!", "", "This is a verly long test string 1624$%" };
        String[] digestResult = new String[] { "702edca0b2181c15d457eacac39de39b", "d41d8cd98f00b204e9800998ecf8427e", "934e7c5c6f5508ff50bc425770a10f45" };
        for (int i = 0; i < testString.length; i++) {
            String digestString = getDigest(testString[i]);
            if (digestResult[i].equals(digestString)) {
                log.info("OK - digest");
            } else {
                log.severe("Digest=" + digestString + " <> " + digestResult[i]);
            }
        }
        log.info("IsDigest true=" + isDigest(digestResult[0]));
        log.info("IsDigest false=" + isDigest("702edca0b2181c15d457eacac39DE39J"));
        log.info("IsDigest false=" + isDigest("702e"));
        String in = "4115da655707807F00FF";
        byte[] bb = convertHexString(in);
        String out = convertToHexString(bb);
        if (in.equalsIgnoreCase(out)) {
            log.info("OK - conversion");
        } else {
            log.severe("Conversion Error " + in + " <> " + out);
        }
        String test = "This is a test!!";
        String result = "28bd14203bcefba1c5eaef976e44f1746dc2facaa9e0623c";
        String test_1 = decrypt(result);
        if (test.equals(test_1)) {
            log.info("OK - dec_1");
        } else {
            log.info("TestDec=" + test_1 + " <> " + test);
        }
        String testEnc = encrypt(test);
        if (result.equals(testEnc)) {
            log.info("OK - enc");
        } else {
            log.severe("TestEnc=" + testEnc + " <> " + result);
        }
        String testDec = decrypt(testEnc);
        if (test.equals(testDec)) {
            log.info("OK - dec");
        } else {
            log.info("TestDec=" + testDec + " <> " + test);
        }
    }

    /**
     * Descripción de Método
     *
     *
     * @param message
     *
     * @return
     */
    public static String getDigest(String message) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        md.reset();
        byte[] input = message.getBytes();
        md.update(input);
        byte[] output = md.digest();
        md.reset();
        return convertToHexString(output);
    }

    /**
     * Descripción de Método
     *
     *
     * @param value
     *
     * @return
     */
    public static boolean isDigest(String value) {
        if ((value == null) || (value.length() != 32)) {
            return false;
        }
        return (convertHexString(value) != null);
    }
}
