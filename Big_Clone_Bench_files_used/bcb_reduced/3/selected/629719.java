package br.ufmg.saotome.arangi.commons;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 *
 */
public class Util {

    /**
	 * stringHasNumbers
	 * 
	 * This method check if string has digits
	 * 
	 * @param string
	 * @return true if has, false if not
	 */
    public static boolean stringHasNumbers(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
	 * 
	 * Method for check if string has characters non AlfaNumerical
	 * 
	 * @param string
	 * @return true if has, false if not
	 */
    public static boolean stringHasNonAlfaNumerical(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isLetterOrDigit(string.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Generate the random uid
	 * 
	 * @return uid
	 */
    public static String generateGUID() {
        double rand = Math.random();
        long randVal = Math.round(rand * Long.MAX_VALUE);
        Date dt = new Date();
        long tempo = dt.getTime();
        StringBuffer sb = new StringBuffer();
        long mask = 0xFF;
        for (int i = 0; i < 8; i++) {
            randVal >>= 8;
            int bt = (int) (randVal & mask);
            char ch = (char) ((bt % 26) + 65);
            sb.append(ch);
        }
        sb.append('-');
        for (int i = 0; i < 8; i++) {
            tempo >>= 8;
            int bt = (int) (tempo & mask);
            char ch = (char) ((bt % 26) + 65);
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String genetateSHA256(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes("UTF-8"));
        byte[] passWd = md.digest();
        String hex = toHex(passWd);
        return hex;
    }

    private static String toHex(byte[] bt) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < bt.length; i++) {
            if ((0xff & bt[i]) < 0x10) hexString.append("0" + Integer.toHexString((0xFF & bt[i]))); else hexString.append(Integer.toHexString(0xFF & bt[i]));
        }
        return hexString.toString();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        System.out.println(genetateSHA256("123"));
    }
}
