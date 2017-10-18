package com.gittigidiyor.payment.garanti.api;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

/**
 * @author Hakan ERDOGAN
 * hakan.erdogan@gittigidiyor.com
 * http://www.jroller.com/hakan/
 * Created on 21.Kas.2007 23:03:33
 */
class StringUtil {

    /**
	 * Returns a trimmed value of the given string. 
	 *
	 * @param  str  given string
	 * @return      trimmed value of the given string
	 */
    public static String trim(String str) {
        if (str != null) {
            str = str.trim();
        }
        return str;
    }

    /**
	 * Returns a trimmed value of the given string or null if the trimmed value is empty. 
	 *
	 * @param  str  given string
	 * @return      trimmed value of the given string or null if the trimmed value is empty
	 */
    public static String trimAndSetNullIfBlank(String str) {
        if (str != null) {
            str = str.trim();
            if (str.length() == 0) {
                str = null;
            }
        }
        return str;
    }

    public static void trimAndSetNullIfBlank(String[] str) {
        if (str != null) {
            for (int i = 0; i < str.length; i++) {
                str[i] = trimAndSetNullIfBlank(str[i]);
            }
        }
    }

    public static boolean isNullOrZeroLength(String str) {
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return false;
    }

    /**
	 * Checks elements whether they are all null or zero length.
	 * 
	 * @param strArray
	 * @return true if all elements of specified array are null or zero length
	 * 			if strArray is null or zero length, returns true. 
	 */
    public static boolean areNullOrZeroLength(String... strArray) {
        if (strArray != null && strArray.length > 0) {
            for (String str : strArray) {
                if (!isNullOrZeroLength(str)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * Checks array whether it has null or zero length element.
	 * 
	 * @param strArray
	 * @return true if string array has null or zero length element 
	 * 			and returns false if strArray is null or zero length 
	 */
    public static boolean hasNullOrZeroLengthElement(String... strArray) {
        if (strArray != null && strArray.length > 0) {
            for (String str : strArray) {
                if (isNullOrZeroLength(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getSHA1Text(String text) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        String hash = (new BASE64Encoder()).encode(sha1.digest(text.getBytes()));
        return hash;
    }

    public static String toHtmlAscii(String str) {
        if (!isNullOrZeroLength(str)) {
            str = str.replaceAll("ğ", "&#287;");
            str = str.replaceAll("Ğ", "&#286;");
            str = str.replaceAll("ü", "&#252;");
            str = str.replaceAll("Ü", "&#220;");
            str = str.replaceAll("ş", "&#351;");
            str = str.replaceAll("Ş", "&#350;");
            str = str.replaceAll("ö", "&#246;");
            str = str.replaceAll("Ö", "&#214;");
            str = str.replaceAll("ç", "&#231;");
            str = str.replaceAll("Ç", "&#199;");
            str = str.replaceAll("ı", "&#305;");
            str = str.replaceAll("İ", "&#304;");
        }
        return str;
    }

    public static String toAscii(String str) {
        if (!isNullOrZeroLength(str)) {
            str = str.replaceAll("ğ", "g");
            str = str.replaceAll("Ğ", "G");
            str = str.replaceAll("ü", "u");
            str = str.replaceAll("Ü", "U");
            str = str.replaceAll("ş", "s");
            str = str.replaceAll("Ş", "S");
            str = str.replaceAll("ö", "o");
            str = str.replaceAll("Ö", "O");
            str = str.replaceAll("ç", "c");
            str = str.replaceAll("Ç", "C");
            str = str.replaceAll("ı", "i");
            str = str.replaceAll("İ", "I");
        }
        return str;
    }
}
