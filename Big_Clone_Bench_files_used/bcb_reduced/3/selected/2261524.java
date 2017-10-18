package com.valueteam.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author maurizio
 *
 */
public class MD5Manager {

    private static Log log = LogFactory.getLog(MD5Manager.class);

    private static MD5Manager instance = null;

    private MD5Manager() {
    }

    public static MD5Manager getInstance() {
        if (instance == null) {
            instance = new MD5Manager();
        }
        return instance;
    }

    public String convertToMD5(String password) {
        String encodedPassword = "";
        byte[] defaultBytes = password.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            encodedPassword = hexString + "";
        } catch (NoSuchAlgorithmException nsae) {
            log.error("ERROR in encoding password. Please verify.");
            nsae.printStackTrace();
        }
        return encodedPassword;
    }
}
