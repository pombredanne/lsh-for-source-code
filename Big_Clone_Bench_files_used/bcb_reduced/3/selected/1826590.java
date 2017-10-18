package krico.arara.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.security.*;
import org.apache.log4j.Category;

/**
 * Utility class for the model
 * @author @arara-author@
 * @version @arara-version@
 * @sf $Header: /cvsroot/arara/arara/sources/krico/arara/model/ModelUtils.java,v 1.10 2002/01/11 01:37:30 krico Exp $
 */
public class ModelUtils {

    static Category logger = Category.getInstance(ModelUtils.class.getName());

    /**
   * Algorithm used to create one way hash functions (ie: SHA, MD5)
   */
    private static final String PASSWORD_ENCRYPTION_ALGORITHM = "MD5";

    /**
   * The message digest algorithm used to crypt Passwords
   */
    private static MessageDigest MESSAGE_DIGEST = null;

    /**
   * Check if all members in both collections 
   * implement personInterface and have the same id
   */
    public static final boolean equalsIds(ArrayList personInterface1, ArrayList personInterface2) {
        if (personInterface1 == null && personInterface2 == null) return true;
        if (personInterface1 == null || personInterface2 == null) return false;
        try {
            PersonInterface[] p1 = new PersonInterface[personInterface1.size()];
            personInterface1.toArray(p1);
            PersonInterface[] p2 = new PersonInterface[personInterface2.size()];
            personInterface2.toArray(p2);
            if (p1.length != p2.length) return false;
            for (int i = 0; i < p1.length; i++) if (p1[i].getId() != p2[i].getId()) return false;
            return true;
        } catch (Exception e) {
            logger.warn("Exception in equalsIds", e);
            return false;
        }
    }

    /**
   * Convenience method to compare objects even if they are null
   * @return true if both objects are null or if <code>one.equals(two)</code> returns true otherwise return false
   */
    public static final boolean equals(Object one, Object two) {
        if (one == null && two == null) return true;
        if (one == null || two == null) return false;
        if (one.getClass().isArray() && two.getClass().isArray() && one.getClass() == two.getClass()) {
            Class type = one.getClass().getComponentType();
            if (type.isPrimitive()) {
                if (type == Boolean.TYPE) return Arrays.equals((boolean[]) one, (boolean[]) two);
                if (type == Character.TYPE) return Arrays.equals((char[]) one, (char[]) two);
                if (type == Byte.TYPE) return Arrays.equals((byte[]) one, (byte[]) two);
                if (type == Short.TYPE) return Arrays.equals((short[]) one, (short[]) two);
                if (type == Integer.TYPE) return Arrays.equals((int[]) one, (int[]) two);
                if (type == Long.TYPE) return Arrays.equals((long[]) one, (long[]) two);
                if (type == Float.TYPE) return Arrays.equals((float[]) one, (float[]) two);
                if (type == Double.TYPE) return Arrays.equals((double[]) one, (double[]) two);
            } else {
                return Arrays.equals((Object[]) one, (Object[]) two);
            }
        }
        return one.equals(two);
    }

    /**
   * This method encrypts a password using a one way cryptographic function of 
   * the algorithm defined by {@link #PASSWORD_ENCRYPTION_ALGORITHM PASSWORD_ENCRYPTION_ALGORITHM}
   * @return a one way hash for the <code>password</code> parameter
  */
    public static byte[] cryptPassword(byte[] password) throws NoSuchAlgorithmException {
        if (MESSAGE_DIGEST == null) MESSAGE_DIGEST = MessageDigest.getInstance(PASSWORD_ENCRYPTION_ALGORITHM);
        byte ret[] = MESSAGE_DIGEST.digest(password);
        MESSAGE_DIGEST.reset();
        return ret;
    }

    public static boolean checkPassword(byte[] encrypted, String toCrypt) {
        if (toCrypt == null) return false;
        try {
            return equals(encrypted, cryptPassword(toCrypt.getBytes()));
        } catch (NoSuchAlgorithmException nsae) {
            logger.warn("Exception checking passwords", nsae);
            return false;
        }
    }
}
