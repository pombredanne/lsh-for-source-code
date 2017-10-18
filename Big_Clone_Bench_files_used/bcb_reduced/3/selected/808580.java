package com.dcivision.framework;

import java.security.MessageDigest;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import sun.misc.BASE64Encoder;

public class MD5Utility {

    public static final String REVISION = "$Revision: 1.3 $";

    private static MessageDigest dig = null;

    private static BASE64Encoder enc = new BASE64Encoder();

    static {
        try {
            if (dig == null) {
                dig = MessageDigest.getInstance("MD5");
            }
        } catch (java.security.NoSuchAlgorithmException nsae) {
            System.out.println("MD5Util Error: " + nsae.getMessage());
        }
    }

    public static String getRandomDigest() throws ApplicationException {
        return getDigest(String.valueOf(System.currentTimeMillis()));
    }

    /**
   * Pass in a string and return the MD5 digest of it.
   * @param inputString String to gen digest
   * @return String - Digest in BASE64 form
   */
    public static String getDigest(String s) throws ApplicationException {
        String s1 = null;
        try {
            s1 = enc.encode(dig.digest(s.getBytes()));
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
        return s1;
    }

    /**
   * Recursively search in the src string to find a match of the input string
   * @param inString Input String to find the match
   * @param src source string which might contain the target string.
   * @return boolean - true if found.
   */
    public static boolean findMatch(String in, String src) {
        return findMatch(in, src, " ");
    }

    /**
   * @param inString Target string which might be found in the source string
   * @param src Source string which might contain the target string
   * @param dlm String - Delimiter
   * @return boolean - true if inString found in src
   */
    public static boolean findMatch(String in, String src, String dlm) {
        in = in.trim();
        if (src.equals("")) {
            return false;
        }
        if (src.indexOf(dlm) == -1) {
            return false;
        }
        String s1 = src.substring(0, src.indexOf(dlm)).trim();
        if (s1.equals(in)) return true; else return findMatch(in, src.substring(src.indexOf(dlm) + 1), dlm);
    }

    protected static Category log = Category.getInstance("com.dci.common.MD5Util");

    public static void main(String argc[]) {
        log.log(Priority.ERROR, "test...");
    }
}
