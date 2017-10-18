package org.alfresco.extension.authentication.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class HashEncoder {

    private static final String FORMAT = "%02x";

    private String algorithm = "SHA-1";

    private static final HashEncoder INSTANCE = new HashEncoder();

    private HashEncoder() {
    }

    public String createHash(String argument) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(argument.getBytes());
        return convertToHexValue(digest);
    }

    public String createHash(char[] argument) throws NoSuchAlgorithmException {
        String s = new String(argument);
        return this.createHash(s);
    }

    private String convertToHexValue(byte[] digest) {
        Formatter formatter = new Formatter();
        for (byte b : digest) {
            formatter.format(FORMAT, b);
        }
        return formatter.toString();
    }

    public static HashEncoder getInstance() {
        return INSTANCE;
    }
}
