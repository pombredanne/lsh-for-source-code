package com.incendiaryblue.cmslite;

import java.security.*;
import java.io.Serializable;

/**
 * A command line utility to compute the SHA hash of a string.
 *
 * <p><pre>usage: java com.incendiaryblue.cmslite.SHATool STRING</pre></p>
 *
 * <p>This is useful because this is how cmslite passwords are 
 * stored in the database.</p>
 */
public class SHATool implements Serializable {

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("usage: java com.incendiaryblue.cmslite.SHATool STRING");
            System.exit(1);
        }
        try {
            String message = args[0];
            byte digest[] = makeDigest(message);
            String hexDigest = printHex(digest);
            System.out.println("SHA digest == " + hexDigest);
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }

    public static byte[] makeDigest(String message) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA");
        return sha.digest(message.getBytes());
    }

    public static String printHex(byte data[]) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; ++i) appendHex(buf, data[i]);
        return buf.toString();
    }

    public static void appendHex(StringBuffer buf, byte b) {
        int x = (b >>> 4) & 0xf;
        int y = b & 0xf;
        buf.append(hexChars[x]);
        buf.append(hexChars[y]);
    }

    public static char hexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
}
