package collabreate.server;

import java.security.*;

/**
 * Utils
 * This class offers various utility functions used by the server
 * @author Tim Vidas
 * @author Chris Eagle
 * @version 0.1.0, August 2008
 */
public class Utils {

    private static SecureRandom srand = new SecureRandom();

    /**
    * toHexString - generate a hex string representation of the specified
    *               portion of the given array
    * @param data The array to be converted
    * @param start The starting index within the array
    * @param length The number of bytes to represent
    * @return The string representation of the given array
    */
    protected static String toHexString(byte[] data, int start, int length) {
        String hex = "";
        int end = start + length;
        for (int i = start; i < end; i++) {
            String val = "0" + Integer.toHexString(data[i]);
            hex += val.substring(val.length() - 2);
        }
        return hex;
    }

    /**
    * toHexString - generate a hex string representation of the given array
    * @param data The array to be converted
    * @return The string representation of the given array
    */
    protected static String toHexString(byte[] data) {
        return toHexString(data, 0, data.length);
    }

    /**
    * toHexString - generate a byte array representation of the specified
    *               string
    * @param hexString The string to convert
    * @return The byte array representation of the given string
    */
    protected static byte[] toByteArray(String hexString) {
        if ((hexString.length() % 2) == 1) {
            return null;
        }
        try {
            int idx = 0;
            byte result[] = new byte[hexString.length() / 2];
            for (int i = 0; i < hexString.length(); i += 2) {
                String val = hexString.substring(i, i + 2);
                int b = Integer.parseInt(val, 16);
                result[idx++] = (byte) b;
            }
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
    * getMD5 - calculate the md5sum of a string 
    * @param tohash The string to hash 
    * @return The md5sum of the input string 
    */
    protected static String getMD5(String tohash) {
        byte[] defaultBytes = tohash.getBytes();
        String hashString = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(defaultBytes);
            byte hash[] = md5.digest();
            hashString = Utils.toHexString(hash);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashString;
    }

    /**
    * getRandom Return an array of random bytes
    * @param len The number of bytes to return
    */
    protected static byte[] getRandom(int len) {
        byte result[] = new byte[len];
        srand.nextBytes(result);
        return result;
    }

    /**
    * tests if the provided string contains digits only
    * @param s string to test
    */
    protected static boolean isNumeric(String s) {
        boolean rval = true;
        if (s == null || s.length() == 0) {
            rval = false;
        } else {
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    rval = false;
                }
            }
        }
        return rval;
    }

    /**
    * tests if the provided string contains hex characters only
    * @param s string to test
    */
    protected static boolean isHex(String s) {
        boolean rval = true;
        if (s == null || s.length() == 0) {
            rval = false;
        } else {
            final String abcdef = "abcdef";
            for (int i = 0; i < s.length(); i++) {
                char c = Character.toLowerCase(s.charAt(i));
                if (!(Character.isDigit(c) || (abcdef.indexOf(c) > -1))) {
                    System.out.println("case 2" + c);
                    rval = false;
                }
            }
        }
        return rval;
    }

    /**
    * tests if the provided string contains letters and digits only
    * @param s string to test
    */
    protected static boolean isAlphaNumeric(String s) {
        boolean rval = true;
        if (s == null || s.length() == 0) {
            rval = false;
        } else {
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isLetterOrDigit(s.charAt(i))) {
                    rval = false;
                }
            }
        }
        return rval;
    }
}
