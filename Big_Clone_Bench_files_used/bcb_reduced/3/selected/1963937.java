package org.mftech.dawn.server.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * <br>
 * <b>Project:</b> iaw SoSe 2007 <br>
 * <b>Package:</b> de.fhtw.sose07.iaw.security <br>
 * <b>last change:</b> 27.6.2007
 * 
 * @author Boettge, Hebeisen, Fluegge
 * @version 1.0 <br>
 *          <b>Description:</b><br>
 *          The security manager can map clean password to hashStrings or checks
 *          password for correctness <br>
 *          <b>History:</b><br>
 *          <table border=1>
 *          <tr>
 *          <td>who</td>
 *          <td>when</td>
 *          <td>what</td>
 *          </tr>
 *          <tr>
 *          <td>MF</td>
 *          <td>02.5.2007</td>
 *          <td>file created</td>
 *          </tr>
 *          <tr>
 *          <td>BHF</td>
 *          <td>27.6.2007</td>
 *          <td>added comments</td>
 *          </tr>
 *          </table>
 * 
 */
public class SecurityManager {

    /**
	 * generetes an SHA String
	 * 
	 * @param pwd password which should be changes
	 * @return teh SHA String
	 */
    public String generatedMD5String(String pwd) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("SHA");
            byte[] arr = messagedigest.digest(pwd.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arr.length; i++) {
                sb.append(Integer.toHexString(arr[i] & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * checks a given String and a HashString, whether they hav the same root
	 * 
	 * @param pwd
	 * @param md5
	 * @return
	 */
    public boolean checkString(String pwd, String md5) {
        if (md5.equals(this.generatedMD5String(pwd))) {
            return true;
        } else {
            return false;
        }
    }
}
