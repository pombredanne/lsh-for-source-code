package com.acv.dao.security;

import java.security.MessageDigest;
import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.apache.log4j.Logger;

/**
 * A class for check and encode a password sha.
 */
public class ShaPasswordChecker {

    /** The hexits. */
    private static String hexits = "0123456789abcdef";

    /** The Constant log. */
    private static final Logger log = Logger.getLogger(ShaPasswordChecker.class);

    /** The sha. */
    private MessageDigest sha = null;

    /**
	 * Encode password sha.
	 *
	 * @param passwordToEncode the password to encode
	 *
	 * @return the string
	 */
    public static String encodePasswordSha(String passwordToEncode) {
        ShaPasswordEncoder encoder = new ShaPasswordEncoder();
        return (encoder.encodePassword(passwordToEncode, null));
    }

    /**
	 * Instantiates a new sha password checker.
	 */
    public ShaPasswordChecker() {
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (java.security.NoSuchAlgorithmException e) {
            log.warn("Construction failed", e);
        }
    }

    /**
	 * Check password with no salt.
	 *
	 * @param encryptedPassWord the encrypted pass word
	 * @param passwordToTest the password to test
	 *
	 * @return true, if successful
	 */
    public boolean checkPasswordWithNoSalt(String encryptedPassWord, String passwordToTest) {
        if (encryptedPassWord == null || passwordToTest == null) {
            return false;
        }
        return encryptedPassWord.equalsIgnoreCase(encodePasswordSha(passwordToTest));
    }

    /**
	 * Check Digest against entity
	 * exemple :{SSHA}72uhy5xc1AWOLwmNcXALHBSzp8xt4giL testing123
	 * {SSHA}72uhy5xc1AWOLwmNcXALHBSzp8xt4giL is the password in ldap server. testing123 the password to compare
	 *
	 * @param digest is digest to be checked against
	 * @param entity entity (string) to be checked
	 *
	 * @return TRUE if there is a match, FALSE otherwise
	 */
    public boolean checkDigest(String digest, String entity) {
        boolean valid = true;
        digest = digest.substring(6);
        byte[][] hs = split(org.apache.commons.codec.binary.Base64.decodeBase64(digest.getBytes()), 20);
        byte[] hash = hs[0];
        byte[] salt = hs[1];
        sha.reset();
        sha.update(entity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        log.debug("Salted Hash extracted (in hex): " + toHex(hash) + " " + "nSalt extracted (in hex): " + toHex(salt));
        log.debug("Hash length is: " + hash.length + " Salt length is: " + salt.length);
        log.debug("Salted Hash presented in hex: " + toHex(pwhash));
        if (!MessageDigest.isEqual(hash, pwhash)) {
            valid = false;
            log.debug("Hashes DON'T match: " + entity);
        }
        if (MessageDigest.isEqual(hash, pwhash)) {
            valid = true;
            log.debug("Hashes match: " + entity);
        }
        return valid;
    }

    /**
	 * split a byte array in two.
	 *
	 * @param src byte array to be split
	 * @param n element at which to split the byte array
	 *
	 * @return byte[][] two byte arrays that have been split
	 */
    private static byte[][] split(byte[] src, int n) {
        byte[] l, r;
        if (src == null || src.length <= n) {
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
	 * Convert a byte array to a hex encoded string.
	 *
	 * @param block byte array to convert to hexString
	 *
	 * @return String representation of byte array
	 */
    private static String toHex(byte[] block) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < block.length; ++i) {
            buf.append(hexits.charAt((block[i] >>> 4) & 0xf));
            buf.append(hexits.charAt(block[i] & 0xf));
        }
        return buf + "";
    }
}
