package com.qcs.eduquill.utilities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 *
 * @version      1.0 23 Jun 2009
 * @author       Sreekanthreddy Y
 */
public class EncryptionData {

    private static EncryptionData encryptionData = null;

    /** Creates a new instance of EncryptionUtility */
    private EncryptionData() {
    }

    public static EncryptionData getInstance() {
        if (encryptionData == null) {
            encryptionData = new EncryptionData();
        }
        return encryptionData;
    }

    public synchronized String encrypt(String plaintext) throws Exception {
        StringBuffer sb = new StringBuffer();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new Exception(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new Exception(e.getMessage());
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }
}
