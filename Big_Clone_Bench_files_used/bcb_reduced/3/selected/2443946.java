package gcrypt;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Gencrypter {

    static final String InternalKey = "925cdb19c7b3e11bfe5a55ace651c4d9";

    String keyFilePath;

    byte[] theKey;

    Cipher cipher;

    SecretKeySpec skeySpec;

    public Gencrypter(String keyDirName, String keyFile) throws Exception {
        byte[] KeyData = new byte[1];
        keyFilePath = keyDirName + java.io.File.separator + keyFile;
        try {
            File keyfile = new File(keyFilePath);
            int Infilelen = (int) keyfile.length();
            FileInputStream fileInputStream = new FileInputStream(keyfile);
            BufferedInputStream in = new BufferedInputStream(fileInputStream);
            KeyData = new byte[Infilelen];
            int i = in.read(KeyData, 0, Infilelen);
            in.close();
        } catch (IOException ioe) {
            System.out.println("\n** Request PropertiesFile io error");
            ioe.printStackTrace();
        }
        cipher = Cipher.getInstance("AES");
        theKey = makeKey(KeyData);
        skeySpec = new SecretKeySpec(theKey, "AES");
    }

    public Gencrypter() {
        try {
            cipher = Cipher.getInstance("AES");
            theKey = BytearrayFromStringAsHex(InternalKey);
            skeySpec = new SecretKeySpec(theKey, "AES");
        } catch (Exception e) {
            System.out.println("\n** Encrypter initialisation error");
            e.printStackTrace();
        }
    }

    public String encryptstring(String inString) {
        byte[] encrypted = new byte[1];
        try {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            encrypted = cipher.doFinal(inString.getBytes());
        } catch (Exception e) {
            System.out.println("\n** Encryption error");
            e.printStackTrace();
            return null;
        }
        String encryptedAsHex = asHex(encrypted);
        return encryptedAsHex;
    }

    public String decryptstring(String inString) {
        byte[] todecrypt = new byte[1];
        byte[] decrypted = new byte[1];
        try {
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            todecrypt = BytearrayFromStringAsHex(inString);
            decrypted = cipher.doFinal(todecrypt);
        } catch (Exception e) {
            System.out.println("\n** Decryption error");
            e.printStackTrace();
            return null;
        }
        String decryptedString = new String(decrypted);
        return decryptedString;
    }

    private byte[] BytearrayFromStringAsHex(String hexString) {
        int len = hexString.length();
        byte[] bytearray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            String nstring = "0x" + hexString.substring(i, i + 2);
            int ascii = Integer.decode(nstring);
            bytearray[i / 2] = (byte) ascii;
        }
        return bytearray;
    }

    private String asHex(byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;
        for (i = 0; i < buf.length; i++) {
            if (((int) buf[i] & 0xff) < 0x10) strbuf.append("0");
            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }
        return strbuf.toString();
    }

    private byte[] makeKey(byte[] data) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] theTextToDigestAsBytes = data;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(theTextToDigestAsBytes);
        byte[] digest = md.digest();
        return digest;
    }
}
