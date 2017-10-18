package org.jma.lib.utils.multithreading;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author jesus
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UniqueIdentifier {

    public static String getID() {
        return (new java.rmi.dgc.VMID()).toString();
    }

    public static String calculatesMD5(String plainText) throws NoSuchAlgorithmException {
        MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
        mdAlgorithm.update(plainText.getBytes());
        byte[] digest = mdAlgorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            plainText = Integer.toHexString(0xFF & digest[i]);
            if (plainText.length() < 2) {
                plainText = "0" + plainText;
            }
            hexString.append(plainText);
        }
        return hexString.toString();
    }
}
