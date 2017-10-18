package org.garret.ptl.portlets.util;

import java.security.MessageDigest;
import org.garret.ptl.util.Base64OutputStream;
import org.garret.ptl.util.SystemException;

public class PasswordUtils {

    public static String encryptPasswd(String pass) {
        try {
            if (pass == null || pass.length() == 0) return pass;
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(pass.getBytes("UTF-8"));
            return Base64OutputStream.encode(sha.digest());
        } catch (Throwable t) {
            throw new SystemException(t);
        }
    }
}
