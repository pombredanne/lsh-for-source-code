package org.dcm4chee.web.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.dcm4che2.base64.Base64Provider;

public class SecurityUtils {

    public static String encodePassword(String password) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes());
            String encodedPassword = new String(md.digest(), new Base64Provider().charsetForName("x-base64"));
            return encodedPassword;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
