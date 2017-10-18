package CryptLibME.Blowfish;

import CryptLibME.Utils.BinConverter;
import java.security.*;
import java.util.*;

/**
 * Support class for easy string encryption with the Blowfish algorithm. Works
 * in CBC mode using SHA-1 for key setup and applies padding.
 */
public class BlowfishEasy {

    BlowfishCBC bfc;

    /**
     * Constructor to use a password as the key. The password is passed as a
     * byte array and can be obtained using the "string".GetBytes() method.
     * @param passw The password as a byte array.
     */
    public BlowfishEasy(byte[] passw) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nse) {
            throw new UnsupportedOperationException();
        }
        md.update(passw, 0, passw.length);
        byte[] bufOut = new byte[passw.length * 10];
        try {
            md.digest(bufOut, 0, bufOut.length);
        } catch (DigestException ex) {
            ex.printStackTrace();
        }
        this.bfc = new BlowfishCBC(bufOut, 0, bufOut.length, 0);
    }

    /**
     * Encrypts a string, using the internal random generator.
     * @param plainText The string to encrypt.
     * @return The encrypted string.
     */
    public String encryptString(String plainText) {
        return encStr(plainText, new Random().nextLong());
    }

    /**
     * Encrypts a string with a provided random number generator instance.
     * @param plaintext The string to encrypt.
     * @param rndgen Random number generator to use. Usually an instance of
     * java.security.SecureRandom.
     * @return The encrypted string.
     */
    public String encryptString(String plaintext, Random rndgen) {
        return encStr(plaintext, rndgen.nextLong());
    }

    /** 
     * Internal routine for string encryption.
     */
    private String encStr(String plainText, long newCBCIV) {
        int strlen = plainText.length();
        byte[] buf = new byte[((strlen << 1) & ~7) + 8];
        int pos = 0;
        for (int i = 0; i < strlen; i++) {
            char achar = plainText.charAt(i);
            buf[pos++] = (byte) ((achar >> 8) & 0x0ff);
            buf[pos++] = (byte) (achar & 0x0ff);
        }
        byte padval = (byte) (buf.length - (strlen << 1));
        while (pos < buf.length) {
            buf[pos++] = padval;
        }
        this.bfc.setCBCIV(newCBCIV);
        this.bfc.encrypt(buf, 0, buf, 0, buf.length);
        byte[] newIV = new byte[Blowfish.BLOCKSIZE];
        BinConverter.longToByteArray(newCBCIV, newIV, 0);
        return BinConverter.bytesToHexStr(newIV, 0, Blowfish.BLOCKSIZE) + BinConverter.bytesToHexStr(buf, 0, buf.length);
    }

    /**
     * Decrypts a string formerly encrypted with the encryptString() method.
     * @param cipherText The string to decrypt.
     * @return The decrypted string or null if the input data is invalid.
     */
    public String decryptString(String cipherText) {
        int len = (cipherText.length() >> 1) & ~7;
        if (Blowfish.BLOCKSIZE > len) {
            return null;
        }
        byte[] cbciv = new byte[Blowfish.BLOCKSIZE];
        int numOfBytes = BinConverter.hexStrToBytes(cipherText, cbciv, 0, 0, Blowfish.BLOCKSIZE);
        if (numOfBytes < Blowfish.BLOCKSIZE) {
            return null;
        }
        this.bfc.setCBCIV(cbciv, 0);
        len -= Blowfish.BLOCKSIZE;
        if (len == 0) {
            return "";
        }
        byte[] buf = new byte[len];
        numOfBytes = BinConverter.hexStrToBytes(cipherText, buf, Blowfish.BLOCKSIZE << 1, 0, len);
        if (numOfBytes < len) {
            return null;
        }
        this.bfc.decrypt(buf, 0, buf, 0, buf.length);
        int padbyte = buf[buf.length - 1] & 0x0ff;
        if (Blowfish.BLOCKSIZE < padbyte) {
            padbyte = 0;
        }
        numOfBytes -= padbyte;
        if (numOfBytes < 0) {
            return "";
        }
        return BinConverter.byteArrayToStr(buf, 0, numOfBytes);
    }

    /**
     * Destroys the encryption stage, so no sensitive data is left in memory.
     * Notice that after that the instance is <b>not</b> valid anymore and
     * should be abandoned!
     */
    public void destroy() {
        this.bfc.cleanUp();
    }
}
