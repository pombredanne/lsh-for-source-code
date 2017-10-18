package org.sourceforge.vlibrary.util;

import java.security.MessageDigest;
import org.apache.log4j.Logger;

public class CryptoImpl implements Crypto {

    /** log4j Logger */
    private static Logger logger = Logger.getLogger(Crypto.class.getName());

    private String algorithm = "MD5";

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public final String encrypt(String input) throws Exception {
        try {
            MessageDigest messageDigest = (MessageDigest) MessageDigest.getInstance(algorithm).clone();
            messageDigest.reset();
            messageDigest.update(input.getBytes());
            String output = convert(messageDigest.digest());
            return output;
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fatal Error while digesting input string", ex);
            }
        }
        return input;
    }

    /**
     * From org.apache.catalina.util.HexUtils (c)Apache Software Foundation ASL 2.0
     *
     * Convert a byte array into a printable format containing a
     * String of hexadecimal digit characters (two per byte).
     *
     * @param bytes Byte array representation
     */
    public String convert(byte bytes[]) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(convertDigit((int) (bytes[i] >> 4)));
            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
        }
        return (sb.toString());
    }

    /**
     * From org.apache.catalina.util.HexUtils (c)Apache Software Foundation ASL 2.0
     *
     * [Private] Convert the specified value (0 .. 15) to the corresponding
     * hexadecimal digit.
     *
     * @param value Value to be converted
     */
    private static char convertDigit(int value) {
        value &= 0x0f;
        if (value >= 10) {
            return ((char) (value - 10 + 'a'));
        } else {
            return ((char) (value + '0'));
        }
    }

    public static void main(String[] args) throws Exception {
        CryptoImpl crypto = new CryptoImpl();
        if (args.length < 1) {
            System.out.println("Insufficient number of parameters.  Usage> CryptoImpl inputString [algorithm]");
            System.exit(-1);
        }
        if (args.length < 2) {
            crypto.setAlgorithm("MD5");
        }
        System.out.println("InputString=" + args[0]);
        System.out.println("Digested string=" + crypto.encrypt(args[0]));
    }
}
