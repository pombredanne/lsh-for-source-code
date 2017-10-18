package org.jwebsocket.util;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

/**
 * Provides some convenience methods to support the web socket
 * development.
 * @author aschulze
 */
public class Tools {

    /**
	 * Returns the MD5 sum of the given string. The output always has 32 digits.
	 * @param aMsg String the string to calculate the MD5 sum for.
	 * @return MD5 sum of the given string.
	 */
    public static String getMD5(String aMsg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] lBufSource = aMsg.getBytes("UTF-8");
            byte[] lBufTarget = md.digest(lBufSource);
            Formatter formatter = new Formatter();
            for (byte b : lBufTarget) {
                formatter.format("%02x", b);
            }
            return (formatter.toString());
        } catch (Exception ex) {
            System.out.println("getMD5: " + ex.getMessage());
        }
        return null;
    }

    /**
	 * Returns the hex value of the given int as a string. If {@code aLen} is
	 * greater than zero the output is cut or filled to the given length
	 * otherwise the exact number of digits is returned.
	 * @param aInt Integer to be converted into a hex-string.
	 * @param aLen Number of hex digits (optionally filled or cut if needed)
	 * @return Hex-string of the given integer.
	 */
    public static String intToHex(int aInt, int aLen) {
        String lRes = Integer.toHexString(aInt);
        if (aLen > 0 && lRes.length() > aLen) {
            lRes = lRes.substring(0, aLen);
        } else {
            while (lRes.length() < aLen) {
                lRes = "0" + lRes.substring(0, aLen);
            }
        }
        return lRes;
    }

    /**
	 * Returns the hex value of the given int as a string. If {@code aLen} is
	 * greater than zero the output is cut or filled to the given length
	 * otherwise the exact number of digits is returned.
	 * @param aInt Integer to be converted into a string.
	 * @param aLen Number of digits (optionally filled or cut if needed)
	 * @return String of the given integer.
	 */
    public static String intToString(int aInt, int aLen) {
        String lRes = Integer.toString(aInt);
        if (aLen > 0 && lRes.length() > aLen) {
            lRes = lRes.substring(0, aLen);
        } else {
            while (lRes.length() < aLen) {
                lRes = "0" + lRes;
            }
        }
        return lRes;
    }

    /**
	 * Converts a string into an integer value and automatically sets it to
	 * a given default value if the string could not be parsed.
	 * @param aString string to be converted into an integer.
	 * @param aDefault default value assigned to the result in case of an exception.
	 * @return integer value of string or given default value in case of exception.
	 */
    public static int stringToInt(String aString, int aDefault) {
        int lRes;
        try {
            lRes = Integer.parseInt(aString);
        } catch (Exception lEx) {
            lRes = aDefault;
        }
        return lRes;
    }

    /**
	 * 
	 * @param aISO8601Date
	 * @return
	 */
    public static Date ISO8601ToDate(String aISO8601Date) {
        SimpleDateFormat lSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            return lSDF.parse(aISO8601Date);
        } catch (ParseException lEx) {
            return null;
        }
    }

    /**
	 *
	 * @param aDate
	 * @return
	 */
    public static String DateToISO8601(Date aDate) {
        SimpleDateFormat lSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return lSDF.format(aDate);
    }

    /**
	 *
	 * @param aDate
	 * @return
	 */
    public static String DateToISO8601WithMillis(Date aDate) {
        SimpleDateFormat lSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return lSDF.format(aDate);
    }
}
