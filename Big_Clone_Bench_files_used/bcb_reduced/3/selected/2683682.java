package ru.nnov.kino.web.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author: Mikhail Sedov [06.03.2009]
 */
public class SecurityUtils {

    private static final String ZEROS = "00000000000000000000000000000000";

    private static MessageDigest m;

    static {
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public static String md5(String source) {
        m.update(source.getBytes(), 0, source.length());
        String md5 = new BigInteger(1, m.digest()).toString(16);
        return ZEROS.substring(0, 32 - md5.length()) + md5;
    }
}
