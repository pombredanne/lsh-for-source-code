package com.kwoksys.biz.auth;

import com.kwoksys.framework.session.CookieManager;
import org.apache.commons.codec.binary.Base64;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auth common.
 */
public class AuthUtils {

    private static final Logger logger = Logger.getLogger(AuthUtils.class.getName());

    public static String generateRandomChars(int numCharacters) {
        String characterList = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.";
        Random r = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numCharacters; i++) {
            result.append(characterList.charAt(r.nextInt(characterList.length())));
        }
        return result.toString();
    }

    /**
     * Gets the hash value of an input password.
     *
     * @param plaintext
     * @return ..
     */
    public static String hashPassword(String plaintext) {
        if (plaintext == null) {
            return "";
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
            md.update(plaintext.getBytes("UTF-8"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem hashing password.", e);
        }
        return new String(Base64.encodeBase64(md.digest()));
    }

    /**
     * Empties all auth cookies.
     *
     * @param response
     */
    public static void resetAuthCookies(HttpServletResponse response) {
        CookieManager.setUserId(response, "");
        CookieManager.setSessionToken(response, "");
    }
}
