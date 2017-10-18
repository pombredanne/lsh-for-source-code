package com.cosmos.acacia.crm.gui.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.apache.log4j.Logger;
import com.cosmos.acacia.crm.bl.users.UsersRemote;

public class UserUtils {

    private static Logger log = Logger.getLogger(UserUtils.class);

    private static Locale locale;

    public static void updateUserLocale(UsersRemote bean) {
        bean.setLocale(locale);
    }

    public static String getHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(password.getBytes());
            return new String(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm not found");
            return password;
        }
    }

    public static String getHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            hexString.append(Integer.toHexString(0xFF & array[i]));
        }
        return hexString.toString();
    }
}
