package BlowfishJ;

import java.util.*;
import java.security.*;

/**
  * Support class for easy string encryption with the Blowfish algorithm, now 
  * in CBC mode with a SHA-1 key setup and correct padding - the purposes of
  * this module is mainly to show a possible implementation with Blowfish.
  *
  * @author Markus Hahn <markus_hahn@gmx.net>
  * @version April 14, 2003
  */
public class BlowfishEasy {

    BlowfishCBC m_bfish;

    static SecureRandom m_secRnd;

    static {
        m_secRnd = new SecureRandom();
    }

    /**
	  * Constructor to set up a string as the key. 
	  * @param sPassword the password 
	  */
    public BlowfishEasy(String sPassword) {
        MessageDigest mds = null;
        byte[] hash;
        try {
            mds = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
        }
        for (int nI = 0, nC = sPassword.length(); nI < nC; nI++) {
            mds.update((byte) (sPassword.charAt(nI) & 0x0ff));
        }
        hash = mds.digest();
        m_bfish = new BlowfishCBC(hash, 0, hash.length, 0);
    }

    /**
	  * Encrypts a string (treated in Unicode) using the
	  * internal random generator.
	  * @param sPlainText string to encrypt
	  * @return encrypted string in binhex format
	  */
    public String encryptString(String sPlainText) {
        long lCBCIV;
        synchronized (m_secRnd) {
            lCBCIV = m_secRnd.nextLong();
        }
        return encStr(sPlainText, lCBCIV);
    }

    /**
	  * Encrypts a string (treated in Unicode).
	  * @param sPlainText string to encrypt
	  * @param rndGen random generator (usually a java.security.SecureRandom
	  * instance)
	  * @return encrypted string in binhex format
	  */
    public String encryptString(String sPlainText, Random rndGen) {
        long lCBCIV = rndGen.nextLong();
        return encStr(sPlainText, lCBCIV);
    }

    private String encStr(String sPlainText, long lNewCBCIV) {
        int nStrLen = sPlainText.length();
        byte[] buf = new byte[((nStrLen << 1) & 0xfffffff8) + 8];
        int nI;
        int nPos = 0;
        for (nI = 0; nI < nStrLen; nI++) {
            char cActChar = sPlainText.charAt(nI);
            buf[nPos++] = (byte) ((cActChar >> 8) & 0x0ff);
            buf[nPos++] = (byte) (cActChar & 0x0ff);
        }
        byte bPadVal = (byte) (buf.length - (nStrLen << 1));
        while (nPos < buf.length) {
            buf[nPos++] = bPadVal;
        }
        m_bfish.setCBCIV(lNewCBCIV);
        m_bfish.encrypt(buf, 0, buf, 0, buf.length);
        byte[] newCBCIV = new byte[BlowfishCBC.BLOCKSIZE];
        BinConverter.longToByteArray(lNewCBCIV, newCBCIV, 0);
        return BinConverter.bytesToBinHex(newCBCIV, 0, BlowfishCBC.BLOCKSIZE) + BinConverter.bytesToBinHex(buf, 0, buf.length);
    }

    /**
	  * Decrypts a hexbin string (handling is case sensitive).
	  * @param sCipherText hexbin string to decrypt
	  * @return decrypted string (null equals an error)
	  */
    public String decryptString(String sCipherText) {
        int nLen = (sCipherText.length() >> 1) & ~7;
        if (nLen < BlowfishECB.BLOCKSIZE) return null;
        byte[] cbciv = new byte[BlowfishCBC.BLOCKSIZE];
        int nNumOfBytes = BinConverter.binHexToBytes(sCipherText, cbciv, 0, 0, BlowfishCBC.BLOCKSIZE);
        if (nNumOfBytes < BlowfishCBC.BLOCKSIZE) return null;
        m_bfish.setCBCIV(cbciv, 0);
        nLen -= BlowfishCBC.BLOCKSIZE;
        if (nLen == 0) {
            return "";
        }
        byte[] buf = new byte[nLen];
        nNumOfBytes = BinConverter.binHexToBytes(sCipherText, buf, BlowfishCBC.BLOCKSIZE * 2, 0, nLen);
        if (nNumOfBytes < nLen) {
            return null;
        }
        m_bfish.decrypt(buf, 0, buf, 0, buf.length);
        int nPadByte = (int) buf[buf.length - 1] & 0x0ff;
        if ((nPadByte > 8) || (nPadByte < 0)) {
            nPadByte = 0;
        }
        nNumOfBytes -= nPadByte;
        if (nNumOfBytes < 0) {
            return "";
        }
        return BinConverter.byteArrayToUNCString(buf, 0, nNumOfBytes);
    }

    /**
	  * Destroys (clears) the encryption engine, after that the instance is not 
	  * valid anymore.
	  */
    public void destroy() {
        m_bfish.cleanUp();
    }
}
