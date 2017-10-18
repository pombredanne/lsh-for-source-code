package de.tum.in.elitese.wahlsys.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;
import sun.misc.BASE64Encoder;

/**
 * Helper Class for all digests.
 * 
 * @author Christoph Frenzel
 * 
 */
public class DigestHelper {

    private static final DigestHelper INSTANCE = new DigestHelper();

    /**
	 * @return Singleton
	 */
    public static DigestHelper getInstance() {
        return INSTANCE;
    }

    private BASE64Encoder fEncoder;

    private MessageDigest fMessageDigest;

    /**
	 * Constructor
	 */
    private DigestHelper() {
        try {
            fMessageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(null, "Security Error.", "SHA-256 could not be found.", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(-1);
        }
        fEncoder = new BASE64Encoder();
    }

    /**
	 * Computes the digest for the given String.
	 * 
	 * @param string
	 * @return the digest as a String
	 */
    public String getDigestForString(String string) {
        byte[] bytes = fMessageDigest.digest(string.getBytes());
        return fEncoder.encode(bytes);
    }
}
