package net.sourceforge.jcv.util;

import com.salmonllc.util.MessageLog;
import com.salmonllc.util.Util;
import sun.misc.BASE64Encoder;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Used to manage text encryptions.
 */
public class EncryptionManager {

    private static EncryptionManager instance;

    private EncryptionManager() {
    }

    /**
     * Implements a one way encryption algorithm.
     *
     * @param plainText the text to encrypt.
     * @return the encrypted text.
     */
    public synchronized String encrypt(String plainText) {
        String hash = null;
        try {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                throw new NoSuchAlgorithmException();
            }
            try {
                if (plainText != null) md.update(plainText.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedEncodingException();
            }
            byte raw[] = md.digest();
            hash = (new BASE64Encoder()).encode(raw);
        } catch (NoSuchAlgorithmException e) {
            MessageLog.writeErrorMessage(e, this);
        } catch (UnsupportedEncodingException e) {
            MessageLog.writeErrorMessage(e, this);
        }
        return Util.stripChars(hash);
    }

    public static synchronized EncryptionManager getInstance() {
        if (instance == null) {
            instance = new EncryptionManager();
        }
        return instance;
    }

    public static void main(String[] args) {
        System.out.println(getInstance().encrypt("test"));
    }
}
