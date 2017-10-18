package com.vmix.simplemq.daemon;

import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.*;
import org.apache.log4j.Logger;

public class Guid {

    private static Logger logger = Logger.getLogger(Guid.class);

    ;

    private static final Random random = new Random(System.currentTimeMillis());

    private static final String patternString = "^[0-9ABCEDF]{8}-[0-9ABCEDF]{4}-[0-9ABCEDF]{4}-[0-9ABCEDF]{4}-[0-9ABCEDF]{12}$";

    private static final Pattern p = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

    private static String systemId = "foo";

    static {
        try {
            systemId = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            logger.error("Unable to get our Host Address for use in Guid", e);
        }
    }

    public static Guid getInstance(String salt) {
        Guid guid = new Guid();
        guid.generate(salt);
        return guid;
    }

    public static Guid fromGuidString(String guidString) {
        Guid guid = new Guid();
        guid.set(guidString);
        return guid;
    }

    private String guidString;

    private Guid() {
        logger = Logger.getLogger(getClass());
    }

    public boolean isValid() {
        if (guidString == null) {
            return false;
        }
        Matcher m = p.matcher(guidString);
        if (m.find()) {
            return true;
        } else {
            return false;
        }
    }

    private void generate(String salt) {
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("No MD5", e);
        }
        long time = System.currentTimeMillis();
        long rand = random.nextLong();
        sbValueBeforeMD5.append(systemId);
        sbValueBeforeMD5.append(salt);
        sbValueBeforeMD5.append(Long.toString(time));
        sbValueBeforeMD5.append(Long.toString(rand));
        md5.update(sbValueBeforeMD5.toString().getBytes());
        byte[] array = md5.digest();
        StringBuffer sb = new StringBuffer();
        int position = 0;
        for (int j = 0; j < array.length; ++j) {
            if (position == 4 || position == 6 || position == 8 || position == 10) {
                sb.append('-');
            }
            position++;
            int b = array[j] & 0xFF;
            if (b < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b).toUpperCase());
        }
        guidString = sb.toString().toUpperCase();
    }

    private void set(String guidString) {
        this.guidString = guidString.toUpperCase();
    }

    public String toString() {
        return guidString;
    }

    @Override
    public boolean equals(Object arg0) {
        if (this.getClass().isInstance(arg0)) {
            Guid g2 = (Guid) arg0;
            return this.toString().equals(g2.toString());
        }
        return super.equals(arg0);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
