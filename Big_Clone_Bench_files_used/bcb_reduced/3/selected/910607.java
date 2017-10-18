package webwatcher;

import java.io.*;
import java.security.*;

/**
 * This is the old class. The software now uses SmarterChecksum but
 * this class remains because it does some useful stuff.
 *
 * @author   Sverre H. Huseby
 *           &lt;<A HREF="mailto:shh@thathost.com">shh@thathost.com</A>&gt;
 * @version  $Id: Checksum.java 15 2010-08-22 16:09:10Z miraculix0815 $
 */
public class Checksum {

    private static final String hex = "0123456789ABCDEF";

    private static final String bytesToString(byte[] b) {
        StringBuffer sb;
        int len;
        len = b.length;
        sb = new StringBuffer(2 * b.length);
        for (int q = 0; q < len; q++) {
            sb.append((char) hex.charAt((b[q] >> 4) & 15));
            sb.append((char) hex.charAt(b[q] & 15));
        }
        return sb.toString();
    }

    private static final int hexToDec(char hex) throws NumberFormatException {
        if (hex >= '0' && hex <= '9') return hex - '0';
        hex = Character.toUpperCase(hex);
        if (hex < 'A' || hex > 'F') throw new NumberFormatException();
        return 10 + hex - 'A';
    }

    protected static final String notInitialized = "[not initialized]";

    protected byte[] checksum;

    protected static final byte[] stringToBytes(String s) throws NumberFormatException {
        byte[] ret;
        s = s.trim();
        if (s.length() != 32) throw new NumberFormatException();
        ret = new byte[16];
        for (int q = 0; q < ret.length; q++) ret[q] = (byte) ((hexToDec(s.charAt(2 * q)) << 4) | hexToDec(s.charAt(2 * q + 1)));
        return ret;
    }

    protected static final byte[] digest(String s) {
        byte[] ret = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            ret = md.digest();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("no message digest algorithm available!");
            System.exit(1);
        }
        return ret;
    }

    public Checksum() {
        checksum = null;
    }

    public Checksum(String s) throws NumberFormatException {
        this();
        if (s.equals(notInitialized)) checksum = null; else checksum = stringToBytes(s);
    }

    public Checksum(byte[] b) throws NumberFormatException {
        this();
        if (b.length != 16) throw new NumberFormatException();
        checksum = (byte[]) b.clone();
    }

    public Checksum(Checksum cs) {
        this();
        checksum = (byte[]) cs.checksum.clone();
    }

    public boolean isInitialized() {
        return checksum != null;
    }

    public void calculate(String s) {
        checksum = digest(s);
    }

    public String toString() {
        if (checksum == null) return notInitialized;
        return bytesToString(checksum);
    }

    public boolean equals(Checksum cs) {
        if (cs == null || checksum == null) return false;
        return toString().equalsIgnoreCase(cs.toString());
    }
}
