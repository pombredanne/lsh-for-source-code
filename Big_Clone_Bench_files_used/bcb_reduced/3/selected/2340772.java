package zzz.crypto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

public class AES {

    /**
	 * Turns array of bytes into string
	 * 
	 * @param buf
	 *            Array of bytes to convert to hex string
	 * @return Generated hex string
	 */
    public static String asHex(byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;
        for (i = 0; i < buf.length; i++) {
            if (((int) buf[i] & 0xff) < 0x10) strbuf.append("0");
            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }
        return strbuf.toString();
    }

    public void testHex() {
        String s = "Th";
        System.out.println(Arrays.toString(s.getBytes()));
        System.out.println(asHex(s.getBytes()));
        String hex = Integer.toHexString(84);
        System.out.println(hex);
    }

    public void testString2Hex() {
        String s = "This is just an example";
        System.out.println(Arrays.toString(s.getBytes()));
    }

    public void test01() throws Exception {
        String message = "This is just an example";
        System.out.println(Arrays.toString(message.getBytes()));
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        SecretKey skey = kgen.generateKey();
        byte[] raw = { 67, 97, 108, 108, 32, 109, 101, 32, 73, 115, 104, 109, 97, 101, 108, 46 };
        System.out.println(raw.length);
        System.out.println(Arrays.toString(raw));
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skey);
        byte[] encrypted = cipher.doFinal(("This is just an example").getBytes());
        System.out.println("encrypted string: " + asHex(encrypted));
        System.out.println(Arrays.toString(encrypted));
        cipher.init(Cipher.DECRYPT_MODE, skey);
        byte[] original = cipher.doFinal(encrypted);
        String originalString = new String(original);
        System.out.println("Original string: " + originalString + " " + asHex(original));
    }

    public void testAES_CBC_NoPadding() throws Exception {
        byte[] key = { 67, 97, 108, 108, 32, 109, 101, 32, 73, 115, 104, 109, 97, 101, 108, 46 };
        System.out.println("-----------------");
        byte[] o_text = "This is not a block length.".getBytes();
        System.out.println(asHex(o_text));
        byte[] text = new byte[32];
        System.arraycopy(o_text, 0, text, 0, o_text.length);
        System.out.println(asHex(text));
        byte[] iv = { 12, 34, 96, 15, 12, 34, 96, 15, 12, 34, 96, 15, 12, 34, 96, 15 };
        runCipherSymmetric("AES/CBC/NoPadding", key, "AES", iv, text);
    }

    public void testAES_CBC_PKCS5Padding() throws Exception {
        byte[] key = { 84, -4, -14, 52, -44, 111, 72, -66, -75, -98, -43, 112, -116, 113, 38, 103, 52, 96, -89, 71, -105, -107, 120, -97, 23, -97, 127, -120, 20, 60, -68, -11 };
        System.out.println("testAES_CBC_PKCS5Padding-----------------");
        byte[] text = "This is not a block length.".getBytes();
        byte[] iv = { 12, 34, 96, 15, 12, 34, 96, 15, 12, 34, 96, 15, 12, 34, 96, 15 };
        runCipherSymmetric("AES/CBC/PKCS5Padding", key, "AES", iv, text);
    }

    public void testSHA256() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] b = md.digest("zhengrenqi".getBytes());
        System.out.println(b.length);
        System.out.println(Arrays.toString(b));
        System.out.println(asHex(b));
    }

    public void testAesSHA256Password() throws Exception {
        String password = "zhengrenqi";
        MessageDigest SHA_256 = MessageDigest.getInstance("SHA-256");
        byte[] key = SHA_256.digest(password.getBytes());
        MessageDigest MD5 = MessageDigest.getInstance("MD5");
        byte[] iv = MD5.digest(password.getBytes());
        byte[] text = "This is zhengrenqi's text.".getBytes();
        runCipherSymmetric("AES/CBC/PKCS5Padding", key, "AES", iv, text);
    }

    @Test
    public void testdeCipher() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("password:");
        String password = br.readLine();
        String ciphertext = "e85a2d4e15d0b90c273406e3f7c0188b8be89acc0172f170ce1cb0f86716c37a42cbd92d4d1e3c5fc956a92bb5084cf715f431460158b20da56d6af2d4b8ccdd";
        byte[] cipherbytes = hex2bytes(ciphertext);
        MessageDigest SHA_256 = MessageDigest.getInstance("SHA-256");
        byte[] key = SHA_256.digest(password.getBytes());
        MessageDigest MD5 = MessageDigest.getInstance("MD5");
        byte[] iv = MD5.digest(password.getBytes());
        System.out.println(Arrays.toString(cipherbytes));
        byte[] text = deCipher("AES/CBC/PKCS5Padding", key, "AES", iv, cipherbytes);
        System.out.println(new String(text));
    }

    byte[] nonBlockPlaintext = "This is not a block length.".getBytes();

    private static final byte[] kAESKey = { (byte) 0x2b, (byte) 0x7e, (byte) 0x15, (byte) 0x16, (byte) 0x28, (byte) 0xae, (byte) 0xd2, (byte) 0xa6, (byte) 0xab, (byte) 0xf7, (byte) 0x15, (byte) 0x88, (byte) 0x09, (byte) 0xcf, (byte) 0x4f, (byte) 0x3c };

    private static final byte[] kAESiv = { (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f, (byte) 0x10 };

    private byte[] enCipher(String algorithm, byte[] keyBits, String keyAlgorithm, byte[] ivBits, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        Key key = new SecretKeySpec(keyBits, 0, keyBits.length, keyAlgorithm);
        IvParameterSpec iv = null;
        if (ivBits != null) iv = new IvParameterSpec(ivBits, 0, ivBits.length);
        if (iv == null) cipher.init(Cipher.ENCRYPT_MODE, key); else cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ciphertext;
    }

    private byte[] deCipher(String algorithm, byte[] keyBits, String keyAlgorithm, byte[] ivBits, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        Key key = new SecretKeySpec(keyBits, 0, keyBits.length, keyAlgorithm);
        IvParameterSpec iv = null;
        if (ivBits != null) iv = new IvParameterSpec(ivBits, 0, ivBits.length);
        if (iv == null) cipher.init(Cipher.DECRYPT_MODE, key); else cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return decrypted;
    }

    private void runCipherSymmetric(String algorithm, byte[] keyBits, String keyAlgorithm, byte[] ivBits, byte[] plaintext) throws Exception {
        System.out.println("Arrays.toString(plaintext) is " + Arrays.toString(plaintext));
        Cipher cipher = Cipher.getInstance(algorithm);
        Key key = new SecretKeySpec(keyBits, 0, keyBits.length, keyAlgorithm);
        IvParameterSpec iv = null;
        if (ivBits != null) iv = new IvParameterSpec(ivBits, 0, ivBits.length);
        int blocksize = 16;
        int ciphertextLength = 0;
        int remainder = plaintext.length % blocksize;
        if (remainder == 0) ciphertextLength = plaintext.length; else ciphertextLength = plaintext.length - remainder + blocksize;
        if (iv == null) cipher.init(Cipher.ENCRYPT_MODE, key); else cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] ciphertext = new byte[ciphertextLength];
        cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, 0);
        System.out.println("encrypted string: " + asHex(ciphertext));
        System.out.println("the ciphertext is " + Arrays.toString(ciphertext));
        System.out.println("the toUnsignedShort ciphertext is " + Arrays.toString(toUnsignedShort(ciphertext)));
        if (iv == null) cipher.init(Cipher.DECRYPT_MODE, key); else cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = new byte[plaintext.length];
        cipher.doFinal(ciphertext, 0, ciphertext.length, decrypted, 0);
        System.out.println("Arrays.toString(decrypted) is " + Arrays.toString(decrypted));
        System.out.println(new String(decrypted));
    }

    public byte[] hex2bytes(String s) {
        byte[] barr = new byte[s.length() / 2];
        for (int i = 0; i < barr.length; i++) {
            barr[i] = hex2byte(s.substring(i * 2, i * 2 + 2));
        }
        return barr;
    }

    public byte hex2byte(String s) {
        if (s.length() != 2) {
            throw new RuntimeException();
        }
        byte b = 0;
        int i = Integer.parseInt(s, 16);
        b = (byte) i;
        return b;
    }

    public void testhex2byte() {
        String s = "a7";
        byte b = hex2byte(s);
        System.out.println(asHex(new byte[] { b }));
    }

    public short[] toUnsignedShort(byte[] buf) {
        short anUnsignedByte = 0;
        int firstByte = 0;
        short[] arr = new short[buf.length];
        for (int j = 0; j < buf.length; j++) {
            firstByte = (0x000000ff & ((int) buf[j]));
            anUnsignedByte = (short) firstByte;
            arr[j] = anUnsignedByte;
        }
        return arr;
    }
}
