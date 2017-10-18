package org.compiere.util;

import java.math.*;
import java.security.*;
import java.sql.Timestamp;
import java.util.logging.*;
import javax.crypto.*;

/**
 * Security Services.
 * <p>
 * Change log:
 * <ul>
 * <li>2007-01-27 - teo_sarca - [ 1598095 ] class Secure is not working with UTF8
 * </ul>
 *  
 *  @author     Jorg Janke
 *  @version    $Id: Secure.java,v 1.2 2006/07/30 00:52:23 jjanke Exp $
 */
public class Secure implements SecureInterface {

    /**************************************************************************
	 *	Hash checksum number
	 *  @param key key
	 *  @return checksum number
	 */
    public static int hash(String key) {
        long tableSize = 2147483647;
        long hashValue = 0;
        for (int i = 0; i < key.length(); i++) hashValue = (37 * hashValue) + (key.charAt(i) - 31);
        hashValue %= tableSize;
        if (hashValue < 0) hashValue += tableSize;
        int retValue = (int) hashValue;
        return retValue;
    }

    /**************************************************************************
	 *  Convert Byte Array to Hex String
	 *  @param bytes bytes
	 *  @return HexString
	 */
    public static String convertToHexString(byte[] bytes) {
        int size = bytes.length;
        StringBuffer buffer = new StringBuffer(size * 2);
        for (int i = 0; i < size; i++) {
            int x = bytes[i];
            if (x < 0) x += 256;
            String tmp = Integer.toHexString(x);
            if (tmp.length() == 1) buffer.append("0");
            buffer.append(tmp);
        }
        return buffer.toString();
    }

    /**
	 *  Convert Hex String to Byte Array
	 *  @param hexString hex string
	 *  @return byte array
	 */
    public static byte[] convertHexString(String hexString) {
        if (hexString == null || hexString.length() == 0) return null;
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

    /**************************************************************************
	 * 	Adempiere Security
	 */
    public Secure() {
        initCipher();
    }

    /** Adempiere Cipher				*/
    private Cipher m_cipher = null;

    /** Adempiere Key				*/
    private SecretKey m_key = null;

    /** Message Digest				*/
    private MessageDigest m_md = null;

    /**	Logger						*/
    private static Logger log = Logger.getLogger(Secure.class.getName());

    /**
	 * 	Initialize Cipher & Key
	 */
    private synchronized void initCipher() {
        if (m_cipher != null) return;
        Cipher cc = null;
        try {
            cc = Cipher.getInstance("DES/ECB/PKCS5Padding");
            if (false) {
                KeyGenerator keygen = KeyGenerator.getInstance("DES");
                m_key = keygen.generateKey();
                byte[] key = m_key.getEncoded();
                StringBuffer sb = new StringBuffer("Key ").append(m_key.getAlgorithm()).append("(").append(key.length).append(")= ");
                for (int i = 0; i < key.length; i++) sb.append(key[i]).append(",");
                log.info(sb.toString());
            } else m_key = new javax.crypto.spec.SecretKeySpec(new byte[] { 100, 25, 28, -122, -26, 94, -3, -26 }, "DES");
        } catch (Exception ex) {
            log.log(Level.SEVERE, "", ex);
        }
        m_cipher = cc;
    }

    /**
	 *	Encryption.
	 *  @param value clear value
	 *  @return encrypted String
	 */
    public String encrypt(String value) {
        String clearText = value;
        if (clearText == null) clearText = "";
        if (m_cipher == null) initCipher();
        if (m_cipher != null) {
            try {
                m_cipher.init(Cipher.ENCRYPT_MODE, m_key);
                byte[] encBytes = m_cipher.doFinal(clearText.getBytes("UTF8"));
                String encString = convertToHexString(encBytes);
                return encString;
            } catch (Exception ex) {
                log.log(Level.INFO, "Problem encrypting string", ex);
            }
        }
        return CLEARVALUE_START + value + CLEARVALUE_END;
    }

    /**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
    public String decrypt(String value) {
        if (value == null || value.length() == 0) return value;
        boolean isEncrypted = value.startsWith(ENCRYPTEDVALUE_START) && value.endsWith(ENCRYPTEDVALUE_END);
        if (isEncrypted) value = value.substring(ENCRYPTEDVALUE_START.length(), value.length() - ENCRYPTEDVALUE_END.length());
        byte[] data = convertHexString(value);
        if (data == null) {
            if (isEncrypted) {
                log.info("Failed");
                return null;
            }
            return value;
        }
        if (m_cipher == null) initCipher();
        if (m_cipher != null && value != null && value.length() > 0) {
            try {
                AlgorithmParameters ap = m_cipher.getParameters();
                m_cipher.init(Cipher.DECRYPT_MODE, m_key, ap);
                byte[] out = m_cipher.doFinal(data);
                String retValue = new String(out, "UTF8");
                return retValue;
            } catch (Exception ex) {
                log.info("Failed decrypting " + ex.toString());
            }
        }
        return null;
    }

    /**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
    public Integer encrypt(Integer value) {
        return value;
    }

    /**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
    public Integer decrypt(Integer value) {
        return value;
    }

    /**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
    public BigDecimal encrypt(BigDecimal value) {
        return value;
    }

    /**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
    public BigDecimal decrypt(BigDecimal value) {
        return value;
    }

    /**
	 *	Encryption.
	 * 	The methods must recognize clear text values
	 *  @param value clear value
	 *  @return encrypted String
	 */
    public Timestamp encrypt(Timestamp value) {
        return value;
    }

    /**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
    public Timestamp decrypt(Timestamp value) {
        return value;
    }

    /**
	 *  Convert String to Digest.
	 *  JavaScript version see - http://pajhome.org.uk/crypt/md5/index.html
	 *
	 *  @param value message
	 *  @return HexString of message (length = 32 characters)
	 */
    public String getDigest(String value) {
        if (m_md == null) {
            try {
                m_md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }
        }
        m_md.reset();
        byte[] input = value.getBytes();
        m_md.update(input);
        byte[] output = m_md.digest();
        m_md.reset();
        return convertToHexString(output);
    }

    /**
	 * 	Checks, if value is a valid digest
	 *  @param value digest string
	 *  @return true if valid digest
	 */
    public boolean isDigest(String value) {
        if (value == null || value.length() != 32) return false;
        return (convertHexString(value) != null);
    }

    /**
	 * 	String Representation
	 *	@return info
	 */
    public String toString() {
        StringBuffer sb = new StringBuffer("Secure[");
        sb.append(m_cipher).append("]");
        return sb.toString();
    }
}
