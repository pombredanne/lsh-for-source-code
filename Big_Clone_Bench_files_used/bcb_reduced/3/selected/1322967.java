package edu.uga.galileo.voci.security;

import java.security.MessageDigest;

/**
 * Performs one-way encryption of a user's password for comparison against the
 * database's entry during authentication, or for entry of a new password via
 * the pubtool.
 */
public final class MD5Encrypt {

    /**
	 * Encrypt a <code>String</code>.
	 * 
	 * @param text
	 *            The <code>String</code> to encrypt.
	 * @return An encrypted <code>String</code>.
	 */
    public static String encrypt(String text) {
        char[] toEncrypt = text.toCharArray();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest dig = MessageDigest.getInstance("MD5");
            dig.reset();
            String pw = "";
            for (int i = 0; i < toEncrypt.length; i++) {
                pw += toEncrypt[i];
            }
            dig.update(pw.getBytes());
            byte[] digest = dig.digest();
            int digestLength = digest.length;
            for (int i = 0; i < digestLength; i++) {
                hexString.append(hexDigit(digest[i]));
            }
        } catch (java.security.NoSuchAlgorithmException ae) {
            ae.printStackTrace();
        }
        return hexString.toString();
    }

    /**
	 * <code>hexDigit</code> method supports the MD5 encryption algorithm.
	 * 
	 * @param x
	 *            a <code>byte</code> value to convert
	 * @return a <code>String</code> value representing the resulting string.
	 */
    private static String hexDigit(byte x) {
        StringBuffer sb = new StringBuffer();
        char c;
        c = (char) ((x >> 4) & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        c = (char) (x & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        return sb.toString();
    }
}
