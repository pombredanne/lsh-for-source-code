package org.psystems.bpm.dashboard.server.sequrity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author dima_d
 * 
 */
public class PasswordUtil {

    public static MessageDigest sha = null;

    private static String hexits = "0123456789abcdef";

    static {
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae.toString());
        }
    }

    public static void main(String[] args) {
        String salt = "" + Math.random();
        String newhash = generateDigest("adminpwd", salt, "SHA-1");
        System.out.println("HASH:" + newhash);
        System.out.println("verify: " + verifyPassword(newhash, "adminpwd"));
    }

    private PasswordUtil() {
    }

    /**
	 * Concatentates to byte arrays.
	 * 
	 * @param byteArray1
	 *            first byte array
	 * @param byteArray2
	 *            second byte array
	 * 
	 * @return concatenated by array
	 */
    private static byte[] concatenate(byte[] byteArray1, byte[] byteArray2) {
        byte[] b = new byte[byteArray1.length + byteArray2.length];
        System.arraycopy(byteArray1, 0, b, 0, byteArray1.length);
        System.arraycopy(byteArray2, 0, b, byteArray1.length, byteArray2.length);
        return b;
    }

    /**
	 * Generates a byte array from hex string.
	 * 
	 * @param s
	 *            String to generated array from
	 * @return a byte array
	 */
    private static byte[] fromHex(String s) {
        s = s.toLowerCase();
        byte[] b = new byte[(s.length() + 1) / 2];
        int j = 0;
        int nybble = -1;
        for (int i = 0; i < s.length(); i++) {
            int h = hexits.indexOf(s.charAt(i));
            if (h >= 0) {
                if (nybble < 0) {
                    nybble = h;
                } else {
                    b[j++] = (byte) ((nybble << 4) + h);
                    nybble = -1;
                }
            }
        }
        if (nybble >= 0) {
            b[j++] = (byte) (nybble << 4);
        }
        if (j < b.length) {
            byte[] b2 = new byte[j];
            System.arraycopy(b, 0, b2, 0, j);
            b = b2;
        }
        return b;
    }

    /**
	 * Generates the hashed version of a given string.
	 * 
	 * @param password
	 *            the password to hash
	 * @param saltHex
	 *            the salt to use
	 * @param alg
	 *            the algorithm
	 */
    public static String generateDigest(String password, String saltHex, String alg) {
        try {
            MessageDigest sha = MessageDigest.getInstance(alg);
            byte[] salt = new byte[0];
            if (saltHex != null) {
                salt = fromHex(saltHex);
            }
            String label = null;
            if (alg.startsWith("SHA")) {
                label = (salt.length <= 0) ? "{SHA}" : "{SSHA}";
            } else if (alg.startsWith("MD5")) {
                label = (salt.length <= 0) ? "{MD5}" : "{SMD5}";
            }
            sha.reset();
            sha.update(password.getBytes());
            sha.update(salt);
            byte[] pwhash = sha.digest();
            StringBuffer digest = new StringBuffer(label);
            digest.append(Base64.encode(concatenate(pwhash, salt)).toCharArray());
            return digest.toString();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("failed to find " + "algorithm for password hashing.", nsae);
        }
    }

    /**
	 * Splits a byte array into two.
	 * 
	 * @param src
	 *            byte array to split
	 * @param n
	 *            location to split the array
	 * @return a two dimensional array of the split
	 */
    private static byte[][] split(byte[] src, int n) {
        byte[] l;
        byte[] r;
        if (src.length <= n) {
            l = src;
            r = new byte[0];
        } else {
            l = new byte[n];
            r = new byte[src.length - n];
            System.arraycopy(src, 0, l, 0, n);
            System.arraycopy(src, n, r, 0, r.length);
        }
        byte[][] lr = { l, r };
        return lr;
    }

    /**
	 * Converts a block of bytes into a hex string.
	 * 
	 * @param block
	 *            bytes to convert
	 * @return hex version of the bytes
	 */
    private static String toHex(byte[] block) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < block.length; i++) {
            buf.append(hexits.charAt(block[i] >>> 4 & 0xf));
            buf.append(hexits.charAt(block[i] & 0xf));
        }
        return String.valueOf(String.valueOf(buf));
    }

    /**
	 * Validates if a plaintext password matches a hashed version.
	 * 
	 * @param digest
	 *            digested version
	 * @param password
	 *            plaintext password
	 * @return if the two match
	 */
    public static boolean verifyPassword(String digest, String password) {
        String alg = null;
        int size = 0;
        if (digest.regionMatches(true, 0, "{SHA}", 0, 5)) {
            digest = digest.substring(5);
            alg = "SHA-1";
            size = 20;
        } else if (digest.regionMatches(true, 0, "{SSHA}", 0, 6)) {
            digest = digest.substring(6);
            alg = "SHA-1";
            size = 20;
        } else if (digest.regionMatches(true, 0, "{MD5}", 0, 5)) {
            digest = digest.substring(5);
            alg = "MD5";
            size = 16;
        } else if (digest.regionMatches(true, 0, "{SMD5}", 0, 6)) {
            digest = digest.substring(6);
            alg = "MD5";
            size = 16;
        }
        try {
            MessageDigest sha = MessageDigest.getInstance(alg);
            if (sha == null) {
                return false;
            }
            byte[][] hs = split(Base64.decode(digest), size);
            byte[] hash = hs[0];
            byte[] salt = hs[1];
            sha.reset();
            sha.update(password.getBytes());
            sha.update(salt);
            byte[] pwhash = sha.digest();
            return MessageDigest.isEqual(hash, pwhash);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("failed to find " + "algorithm for password hashing.", nsae);
        }
    }
}
