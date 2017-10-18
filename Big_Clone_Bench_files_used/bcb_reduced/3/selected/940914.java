package nl.burgerweeshuis.util;

import java.security.MessageDigest;

/**
 * Helper class for password handling based on Tagisch JAAS example
 * (http://free.tagish.net/jaas/doc.html)
 * 
 * @author Andy Armstrong
 * @author Eelco Hillenius
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "MD5";

    private static MessageDigest md = null;

    /**
	 * Turn a byte array into a char array containing a printable hex
	 * representation of the bytes. Each byte in the source array contributes a
	 * pair of hex digits to the output array.
	 * 
	 * @param src
	 *            the source array
	 * @return a char array containing a printable version of the source data
	 */
    private static char[] hexDump(byte src[]) {
        char buf[] = new char[src.length * 2];
        for (int b = 0; b < src.length; b++) {
            String byt = Integer.toHexString((int) src[b] & 0xFF);
            if (byt.length() < 2) {
                buf[b * 2 + 0] = '0';
                buf[b * 2 + 1] = byt.charAt(0);
            } else {
                buf[b * 2 + 0] = byt.charAt(0);
                buf[b * 2 + 1] = byt.charAt(1);
            }
        }
        return buf;
    }

    /**
	 * Zero the contents of the specified array. Typically used to erase
	 * temporary storage that has held plaintext passwords so that we don't
	 * leave them lying around in memory.
	 * 
	 * @param pwd
	 *            the array to zero
	 */
    public static void smudge(char pwd[]) {
        if (null != pwd) {
            for (int b = 0; b < pwd.length; b++) {
                pwd[b] = 0;
            }
        }
    }

    /**
	 * Zero the contents of the specified array.
	 * 
	 * @param pwd
	 *            the array to zero
	 */
    public static void smudge(byte pwd[]) {
        if (null != pwd) {
            for (int b = 0; b < pwd.length; b++) {
                pwd[b] = 0;
            }
        }
    }

    /**
	 * Perform MD5 hashing on the supplied password and return a char array
	 * containing the encrypted password as a printable string. The hash is
	 * computed on the low 8 bits of each character.
	 * 
	 * @param pwd
	 *            The password to encrypt
	 * @return a character array containing a 32 character long hex encoded MD5
	 *         hash of the password
	 * @throws Exception
	 */
    public static char[] cryptPassword(char pwd[]) throws Exception {
        if (null == md) {
            md = MessageDigest.getInstance(ALGORITHM);
        }
        md.reset();
        byte pwdb[] = new byte[pwd.length];
        for (int b = 0; b < pwd.length; b++) {
            pwdb[b] = (byte) pwd[b];
        }
        char crypt[] = hexDump(md.digest(pwdb));
        smudge(pwdb);
        return crypt;
    }
}
