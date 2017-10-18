package at.rc.tacos.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

/**
 * Provides methods to generate a hash value out of a given string. The original
 * password cannot be recovered. This is a implementation of a one-way hash
 * algorithm. Code from: Devarticles.com -> Password Encryption
 * 
 * @author Michael
 */
public class PasswordEncryption {

    private static PasswordEncryption instance;

    /**
	 * Default class constructor.
	 */
    private PasswordEncryption() {
    }

    /**
	 * Generates a hash value out of a given string and returns it. The SHA-1
	 * algorithm will be used to encrypt the password.
	 * 
	 * @param password
	 *            the plain text password to generate the hash
	 * @return the hash value for the given input
	 */
    public synchronized String encrypt(String password) {
        try {
            MessageDigest md = null;
            md = MessageDigest.getInstance("SHA-1");
            md.update(password.getBytes("UTF-8"));
            byte raw[] = md.digest();
            byte[] hash = (new Base64()).encode(raw);
            return new String(hash);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algorithm SHA-1 is not supported");
            return null;
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 encoding is not supported");
            return null;
        }
    }

    /**
	 * Creates a new instance of this class or returns the previousely used
	 * instance.
	 * 
	 * @return a instance of the <code>SecurePassword</code> class.
	 */
    public static synchronized PasswordEncryption getInstance() {
        if (instance == null) return new PasswordEncryption(); else return instance;
    }
}
