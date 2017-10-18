package src;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Various ID generators.
 */
public class GenerateID {

    static final Random rand = new Random();

    public static void p(Object o) {
        System.out.println((o == null ? o : o.toString()));
    }

    /**
	 * Correct md5 implementation
	 * @return Plain md5 sum (hexadecimal) of the input string.
	 */
    public static String md5sum(String s) {
        StringBuffer hash = new StringBuffer();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            byte[] b = s.getBytes();
            b = md5.digest(b);
            for (byte bb : b) {
                String hex = Integer.toHexString((int) bb & 0xff);
                while (hex.length() < 2) hex = '0' + hex;
                hash.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
        }
        return hash.toString();
    }

    /**
	 * @param s is the string to calculate the hash from
	 * @return Sort of an MD5 hash of the input string.
	 * @see #md5sum(String)
	 */
    public static String getMD5(String s) {
        StringBuffer out = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] b = s.getBytes();
            b = md.digest(b);
            for (int i = 0; i < b.length; i++) {
                out.append((int) b[i]);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    /**
	 * Get the hex value of an Integer.
	 * @see #toHexBi(BigInteger, boolean) for bigger integers
	 * @param input is the Integer to calculate the hex value from
	 * @return the input in hexadecimal description
	 */
    public static String toHex(long input, boolean lowercase) {
        StringBuffer hex = new StringBuffer();
        for (byte i = 1; i < 20; i++) {
            byte c = (byte) (input % 16);
            hex.append(getHexChar(c, lowercase));
            input -= c;
            input = (long) input / 16;
        }
        hex = hex.reverse();
        while (hex.length() > 0) if (hex.charAt(0) == '0') hex.deleteCharAt(0); else break;
        return hex.toString();
    }

    /**
	 * Get the hex value of a BigInteger.
	 * @param input is the BigInteger to calculate the hex value from
	 * @return the input in hexadecimal description
	 */
    public static String toHexBi(BigInteger input, boolean lowercase) {
        StringBuffer hex = new StringBuffer();
        for (byte i = 1; i < 20; i++) {
            byte c = Byte.parseByte(input.mod(new BigInteger("16")).toString());
            hex.append(getHexChar(c, lowercase));
            input = input.subtract(new BigInteger("" + c));
            input = input.divide(new BigInteger("16"));
        }
        hex = hex.reverse();
        while (hex.length() > 0) if (hex.charAt(0) == '0') hex.deleteCharAt(0); else break;
        return hex.toString();
    }

    /**
	 * This method gives you the hex char of an integer.
	 * @param i is the input integer
	 * @return "err" if i >= 16, otherwise the char (0-9A-F)
	 */
    public static String getHexChar(byte i, boolean lowercase) {
        if (i >= 16) return "err";
        if (i < 10) return new String("" + i); else {
            return new String((lowercase ? "abcdef" : "ABCDEF").charAt(i - 10) + "");
        }
    }

    public static String getHexMD5id(String s, String part, boolean lowercase) {
        return "id" + getMD5Hex(s, part, lowercase);
    }

    /**
	 * @param s is the input string to generate the hash from
	 * @param part is the character which is to be used to part the sections
	 * @return An MD5 hash from the String s in hex format.
	 */
    public static String getMD5Hex(String s, String part, boolean lowercase) {
        StringBuffer hmd5 = new StringBuffer();
        s = getMD5(s);
        String[] integers = s.split("-");
        for (String integer : integers) {
            try {
                hmd5.append(toHex(Integer.parseInt(integer), lowercase) + part);
            } catch (NumberFormatException e) {
                if (integer.length() == 0) ; else if (integer.length() < 10) {
                    e.printStackTrace();
                    System.err.println("Length < 10: >" + integer + "<");
                } else {
                    BigInteger l = new BigInteger(integer);
                    hmd5.append(toHexBi(l, lowercase) + part);
                }
            }
        }
        if (hmd5.length() > 0) {
            hmd5.deleteCharAt(hmd5.length() - 1);
        }
        return hmd5.toString();
    }
}
