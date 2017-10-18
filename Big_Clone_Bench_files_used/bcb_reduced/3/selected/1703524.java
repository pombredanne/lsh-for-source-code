package Plan58.provider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
	 * The Class md5pw.
	 * 
	 * @author Sören Haag - 660553
	 * Die Klasse md5pw dient zur Verschlüsselung eingegebener Passwörter. Dabei wird Passwort als String übergeben
	 * und gibt die md5-Summe als STring zurück
	 */
public class md5pw {

    /**
	 * Plain string to md5.
	 * 
	 * @param input the input
	 * 
	 * @return the string
	 */
    public String plainStringToMD5(String input) {
        MessageDigest md = null;
        byte[] byteHash = null;
        StringBuffer resultString = new StringBuffer();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.exit(-1);
        }
        md.reset();
        md.update(input.getBytes());
        byteHash = md.digest();
        for (int i = 0; i < byteHash.length; i++) {
            resultString.append(Integer.toHexString(0xFF & byteHash[i]));
        }
        return (resultString.toString());
    }
}
