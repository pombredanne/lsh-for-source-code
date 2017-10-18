package xpusp.model;

import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Category;

/**
 * This is a utility class that holds convenience methods used in many classes.<br>
 * @sf $Header: /cvsroot/xpusp/xpusp/sources/xpusp/model/Utility.java,v 1.10 2001/12/10 08:11:31 krico Exp $
 */
public class Utility {

    /**
   * One should avoid instantiating many utility since all methods are static,
   * but since the Java VM some times Garbage-collects it, keeping a reference for an instance could
   * assure that it is allways configured
   */
    public Utility() {
    }

    /**
   * Static instance for logging statements
   */
    static Category logger = Category.getInstance(Utility.class);

    /**
   * Name of the Algorithm used to create one way hash functions (ie: SHA, MD5)
   */
    private static final String PASSWORD_ENCRYPTION_ALGORITHM = "MD5";

    /**
   * The instance of the message digest algorithm used to encrypt one way functions
   */
    private static MessageDigest MESSAGE_DIGEST = null;

    /**
   * This static method instantiates the {@link #MESSAGE_DIGEST MESSAGE_DIGEST} used by encryption methods. 
   * This method <b>must</b> be called befor any calls to {@link #cryptPassword cryptPassword(byte[]password)} and 
   * {@link #checkPassword checkPassword(byte[]encrypted, String toCrypt)}.
   */
    public static void configure() throws NoSuchAlgorithmException {
        if (MESSAGE_DIGEST == null) MESSAGE_DIGEST = MessageDigest.getInstance(PASSWORD_ENCRYPTION_ALGORITHM); else logger.warn("Utility configured again");
    }

    /**
   * This method encrypts a password using a one way cryptographic function of 
   * the algorithm defined by {@link #PASSWORD_ENCRYPTION_ALGORITHM PASSWORD_ENCRYPTION_ALGORITHM}
   * <b>Before</b> using this method a call to {@link #configure configure()} must be made
   * @return a one way hash for the <code>password</code> parameter
   * @param password the bytes that should be encrypted and returned
   */
    public static byte[] cryptPassword(byte[] password) {
        if (MESSAGE_DIGEST == null) logger.fatal("You must call the configure method before calling cryptPassword.");
        byte ret[] = MESSAGE_DIGEST.digest(password);
        MESSAGE_DIGEST.reset();
        return ret;
    }

    /**
   * This method checks if the encryption of the String <code>toCrypt</code> matches the <u>already encrypted</u>
   * bytes <code>encrypted</code>
   * <b>Before</b> using this method a call to {@link #configure configure()} must be made
   * @param encrypted the encrypted password
   * @param toCrypt the string to be encrypted and compared with the encrypted pass
   * @return true if the passwords match, false otherwise
   */
    public static boolean checkPassword(byte[] encrypted, String toCrypt) {
        return equals(encrypted, cryptPassword(toCrypt.getBytes()));
    }

    /**
   * Compare two objects for equality.  The following logic is followed:
   * <ol>
   * <li>If both objects are null they are considered equal
   * <li>If one of the object is null and the other one is not they are considered <b>not</b> equal
   * <li>If both objects are arrays and of the same class, then their objects are compared for equality
   * <li>If both objects are equal acording to the o1.equals(o2) then they are considered equal
   * </ol>
   * @return true if both objects are equal false otherwise
   */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 == null || o2 == null) return false;
        if (o1.getClass().isArray() && o2.getClass().isArray() && o1.getClass() == o2.getClass()) {
            Class type = o1.getClass().getComponentType();
            if (type.isPrimitive()) {
                if (type == Boolean.TYPE) return Arrays.equals((boolean[]) o1, (boolean[]) o2);
                if (type == Character.TYPE) return Arrays.equals((char[]) o1, (char[]) o2);
                if (type == Byte.TYPE) return Arrays.equals((byte[]) o1, (byte[]) o2);
                if (type == Short.TYPE) return Arrays.equals((short[]) o1, (short[]) o2);
                if (type == Integer.TYPE) return Arrays.equals((int[]) o1, (int[]) o2);
                if (type == Long.TYPE) return Arrays.equals((long[]) o1, (long[]) o2);
                if (type == Float.TYPE) return Arrays.equals((float[]) o1, (float[]) o2);
                if (type == Double.TYPE) return Arrays.equals((double[]) o1, (double[]) o2);
            } else {
                return Arrays.equals((Object[]) o1, (Object[]) o2);
            }
        }
        return o1.equals(o2);
    }
}
