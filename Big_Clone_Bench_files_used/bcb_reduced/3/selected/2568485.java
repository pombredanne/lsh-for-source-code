package org.jpos.ee;

import org.jpos.iso.ISOUtil;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Alejandro P. Revilla
 * @version $Revision: 1.5 $ $Date: 2004/12/09 19:45:33 $
 *
 * Assorted helpers
 */
public class EEUtil {

    public static String getHash(String userName, String pass) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(userName.getBytes());
            hash = ISOUtil.hexString(md.digest(pass.getBytes())).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
        }
        return hash;
    }

    public static String getHash(String s) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            hash = ISOUtil.hexString(md.digest(s.getBytes())).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
        }
        return hash;
    }

    public static String getRandomHash() {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            return ISOUtil.hexString(bytes).toLowerCase();
        } catch (java.security.NoSuchAlgorithmException e) {
            return getHash(Double.toString(Math.random()), Double.toString(Math.random()));
        }
    }
}
