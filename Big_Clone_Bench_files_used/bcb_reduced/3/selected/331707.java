package net.videgro.oma.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Password {

    protected static final Log logger = LogFactory.getLog("Password");

    public static String generatePassword(int n) {
        char[] pw = new char[n];
        int c = 'A';
        int r1 = 0;
        for (int i = 0; i < n; i++) {
            r1 = (int) (Math.random() * 3);
            switch(r1) {
                case 0:
                    c = '0' + (int) (Math.random() * 10);
                    break;
                case 1:
                    c = 'a' + (int) (Math.random() * 26);
                    break;
                case 2:
                    c = 'A' + (int) (Math.random() * 26);
                    break;
            }
            pw[i] = (char) c;
        }
        return new String(pw);
    }

    public static String md5sum(String input) {
        byte[] defaultBytes = input.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xF0 & messageDigest[i]).charAt(0));
                hexString.append(Integer.toHexString(0x0F & messageDigest[i]).charAt(0));
            }
        } catch (NoSuchAlgorithmException nsae) {
            logger.info("md5sum: " + nsae.getMessage());
        }
        return hexString.toString();
    }
}
