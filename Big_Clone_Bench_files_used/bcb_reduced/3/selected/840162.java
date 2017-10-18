package pl.edu.pw.DVDManiac.util;

import java.security.MessageDigest;

/**
 * Klasa do obsługi MD5
 * 
 * @author Robert Duda
 */
public class MD5Tools {

    /**
	 * Wylicza skrót md5 danego ciągu znaków
	 * 
	 * @param input ciąg znaków, którego skrót ma zostać wyliczony
	 * @return 32 znakowy ciąg będący skrótem md5 danego ciągu
	 */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuffer sb = new StringBuffer();
            byte[] md5 = md.digest(input.getBytes());
            for (int i = 0; i < md5.length; i++) {
                String tmpStr = "0" + Integer.toHexString((0xff & md5[i]));
                sb.append(tmpStr.substring(tmpStr.length() - 2));
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}
