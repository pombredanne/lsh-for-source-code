package com.sitescape.team.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.sitescape.util.PasswordEncryptor;

public class EncryptUtil {

    private static final String PASSWORD_ENCRYPTION_ALGORITHM = SPropsUtil.getString("user.password.encryption.algorithm", "MD5");

    public static String encryptSHA1(String... input) {
        return encrypt("SHA-1", input);
    }

    public static String encrypt(String algorithm, String[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) md.update(input[i].getBytes("UTF-8"));
            }
            byte[] messageDigest = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString((0xf0 & messageDigest[i]) >> 4));
                hexString.append(Integer.toHexString(0x0f & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return new StringBuffer().toString();
        }
    }

    public static String encryptPassword(String password) {
        return PasswordEncryptor.encrypt(PASSWORD_ENCRYPTION_ALGORITHM, password);
    }
}
