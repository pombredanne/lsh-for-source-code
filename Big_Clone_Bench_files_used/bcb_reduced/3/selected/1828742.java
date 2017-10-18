package org.verus.ngl.utilities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;

/**
 *
 * @author root
 */
public class EncryptionUtility {

    private static EncryptionUtility encryptionUtility = null;

    /** Creates a new instance of EncryptionUtility */
    private EncryptionUtility() {
    }

    public static EncryptionUtility getInstance() {
        if (encryptionUtility == null) {
            encryptionUtility = new EncryptionUtility();
        }
        return encryptionUtility;
    }

    public synchronized String encrypt(String plaintext) throws Exception {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (Exception e) {
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }
}
