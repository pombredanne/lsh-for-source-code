package org.msb.finance.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@code PasswordHasher} class provides the ability to hash (translate into a practically irreversible form) a
 * password so that it can safely be stored in a file or in a database.
 * 
 * @author Marc Boudreau
 * 
 */
public class PasswordHasher {

    /**
	 * The singleton instance of this class.
	 */
    private static PasswordHasher instance = new PasswordHasher();

    /**
	 * This object implements the hashing function used by this class.
	 */
    private MessageDigest digest;

    /**
	 * Constructs an instance of this class. This constructor is made private so that only the singleton instance can be
	 * created.
	 */
    private PasswordHasher() {
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "The MD5 algorithm is not available on this system.", ex);
        }
    }

    /**
	 * Translates the provided password into a practically irreversible hash that is safe to store in files or
	 * databases. The result is a hexadecimal representation of the hash results. The hexadecimal digits {@code a}
	 * through {@code f} can be returned either as lower case or upper case.
	 * <p>
	 * The password is provided in an array of {@code char} elements instead of a {@link String} object, this allows the
	 * method to replace each element of the array with the '\0' character once it no longer needs them.
	 * 
	 * @param password
	 *            An array of {@code char} containing the password to be hashed.
	 * @param uppercase
	 *            A {@code boolean} value indicating whether the hexadecimal digits {@code a} through {@code f} should
	 *            be returned in upper case form.
	 * 
	 * @return A {@link String} object containing the result of the hash operation, represented using hexadecimal
	 *         notation.
	 */
    public static String hash(char[] password, boolean uppercase) {
        instance.digest.reset();
        if (null != password) {
            instance.digest.update(createBuffer(password));
            Arrays.fill(password, '\0');
        }
        byte[] result = instance.digest.digest();
        return translateToHex(result, uppercase);
    }

    /**
	 * Translates the provided password into a practically irreversible hash that is safe to store in files or
	 * databases. The result is a hexadecimal representation of the hash results. The hexadecimal digits {@code a}
	 * through {@code f} are returned as lower case.
	 * <p>
	 * The password is provided in an array of {@code char} elements instead of a {@link String} object, this allows the
	 * method to replace each element of the array with the '\0' character once it no longer needs them.
	 * 
	 * @param password
	 *            An array of {@code char} containing the password to be hashed.
	 * 
	 * @return A {@link String} object containing the result of the hash operation, represented using hexadecimal
	 *         notation.
	 */
    public static String hash(char[] password) {
        return hash(password, false);
    }

    /**
	 * Creates a {@link ByteBuffer} object that contains the provided password.
	 * 
	 * @param password
	 *            The array of characters which will be copied into the {@code ByteBuffer} object.
	 * 
	 * @return A {@code ByteBuffer} object containing the password.
	 */
    static ByteBuffer createBuffer(char[] password) {
        ByteBuffer buffer = ByteBuffer.allocate(password.length * 2);
        buffer.asCharBuffer().put(password);
        return buffer;
    }

    /**
	 * Translates an array of {@code byte} values into a {@link String} object that contains their hexadecimal
	 * representation. The hexadecimal digits {@code a}, {@code b}, {@code c}, {@code d}, {@code e}, and {@code f}
	 * can be returned in upper case if the {@code uppercase} parameter is {@code true}.
	 * 
	 * @param array
	 *            The array of {@code byte} values to translate.
	 * @param uppercase
	 *            A {@code boolean} value indicating if the hexadecimal digits {@code a} through {@code f} should be
	 *            returned in upper case.
	 * 
	 * @return A {@link String} object containing the hexadecimal representation.
	 */
    static String translateToHex(byte[] array, boolean uppercase) {
        StringBuilder buffer = new StringBuilder(array.length * 2);
        for (byte b : array) {
            long l = b | 256L;
            String s = Long.toHexString(l);
            if (uppercase) {
                s = s.toUpperCase();
            }
            buffer.append(s.substring(s.length() - 2));
        }
        return buffer.toString();
    }
}
