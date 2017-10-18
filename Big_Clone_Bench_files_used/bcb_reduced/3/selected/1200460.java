package com.directi.qwick.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import com.directi.qwick.QwickException;

public class Password {

    private static int PASSWORD_MIN_LENGTH = 8;

    private static int PASSWORD_MAX_LENGTH = 32;

    private final String password;

    public Password(String password) {
        this.password = password;
    }

    public boolean isInvalid() {
        if (length() < PASSWORD_MIN_LENGTH && length() > PASSWORD_MAX_LENGTH) return true;
        if (containsLowerCase() && containsNumber() && (containsSpecialChar() || containsUpperCase())) return false;
        return true;
    }

    private boolean containsLowerCase() {
        return match(".??[a-z]");
    }

    private boolean containsNumber() {
        return match(".??[0-9]");
    }

    private boolean containsSpecialChar() {
        return match(".??[:,!,@,#,$,%,^,&,*,?,_,~]");
    }

    private boolean containsUpperCase() {
        return match(".??[A-Z]");
    }

    private int length() {
        if (password == null) return 0;
        return password.length();
    }

    private boolean match(final String pattern) {
        Pattern p = Pattern.compile(pattern);
        return p.matcher(password).find();
    }

    public static boolean compare(String plainTextPwd, String hashedPassword) {
        return hashedPassword.equals(hash(plainTextPwd));
    }

    public static String hash(String plainTextPwd) {
        MessageDigest hashAlgo;
        try {
            hashAlgo = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new QwickException(e);
        }
        hashAlgo.update(plainTextPwd.getBytes());
        return new String(hashAlgo.digest());
    }
}
