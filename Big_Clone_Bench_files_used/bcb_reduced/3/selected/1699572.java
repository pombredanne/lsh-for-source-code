package com.acv.dao.security;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.apache.log4j.Logger;

/**
 * A class to check if a password is correct. Use the main function checkDigest
 */
public class SshaPasswordChecker {

    /** The Constant log. */
    private static final Logger log = Logger.getLogger(SshaPasswordChecker.class);

    /** The verbose. */
    private transient boolean verbose = false;

    /** The sha. */
    private transient MessageDigest sha;

    /**
	 * public constructor.
	 */
    public SshaPasswordChecker() {
        super();
        verbose = false;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (java.security.NoSuchAlgorithmException e) {
            log.warn("Construction failed", e);
        }
    }

    /**
	 * Create Digest for each entity values passed in.
	 *
	 * @param salt byte array to set the base for the encryption
	 * @param entity string to be encrypted
	 *
	 * @return string representing the salted hash output of the encryption
	 * operation
	 */
    public String createDigest(byte[] salt, String entity) {
        String label = "{SSHA}";
        sha.reset();
        sha.update(entity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) {
            log.debug("pwhash, binary represented as hex: " + toHex(pwhash) + " n");
            log.debug("Putting it all together: ");
            log.debug("binary digest of password plus binary salt: " + Arrays.toString(pwhash) + Arrays.toString(salt));
            log.debug("Now we base64 encode what is respresented above this line ...");
        }
        return label + new String(org.apache.commons.codec.binary.Base64.encodeBase64(concatenate(pwhash, salt)));
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
        if (verbose) {
            log.debug("Salted Hash extracted (in hex): " + toHex(hash) + " " + "nSalt extracted (in hex): " + toHex(salt));
            log.debug("Hash length is: " + hash.length + " Salt length is: " + salt.length);
            log.debug("Salted Hash presented in hex: " + toHex(pwhash));
        }
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
	 * set the verbose flag.
	 *
	 * @param verbose the verbose
	 */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
	 * Combine two byte arrays.
	 *
	 * @param l first byte array
	 * @param r second byte array
	 *
	 * @return byte[] combined byte array
	 */
    private static byte[] concatenate(byte[] l, byte[] r) {
        byte[] b = new byte[l.length + r.length];
        System.arraycopy(l, 0, b, 0, l.length);
        System.arraycopy(r, 0, b, l.length, r.length);
        return b;
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

    /** The hexits. */
    private static String hexits = "0123456789abcdef";

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

    /**
	 * Convert a String hex notation to a byte array.
	 *
	 * @param s string to convert
	 *
	 * @return byte array
	 */
    private static byte[] fromHex(String s) {
        s = s.toLowerCase(Locale.getDefault());
        byte[] b = new byte[(s.length() + 1) / 2];
        int j = 0;
        int h;
        int nibble = -1;
        for (int i = 0; i < s.length(); ++i) {
            h = hexits.indexOf(s.charAt(i));
            if (h >= 0) {
                if (nibble < 0) {
                    nibble = h;
                } else {
                    b[j++] = (byte) ((nibble << 4) + h);
                    nibble = -1;
                }
            }
        }
        if (nibble >= 0) {
            b[j++] = (byte) (nibble << 4);
        }
        if (j < b.length) {
            byte[] b2 = new byte[j];
            System.arraycopy(b, 0, b2, 0, j);
            b = b2;
        }
        return b;
    }

    /**
	 * Usage.
	 *
	 * @param className the class name
	 */
    private static void usage(String className) {
        System.out.print("usage: " + className + " [-v] -s salt SourceString ...n" + " or: " + className + " [-v] -c EncodedDigest SourceString ...n" + "    salt must be in hex.n" + "    digest contains SHA-1 hash or salted hash, base64 encoded.n");
    }

    /**
	 * Main program for command line use and testing.
	 *
	 * @param args the args
	 */
    public static void main(String[] args) {
        SshaPasswordChecker sh = new SshaPasswordChecker();
        String className = "TestSSHA";
        if (args.length <= 1) {
            usage(className);
            return;
        }
        int i = 0;
        if (args[i].equals("-v")) {
            ++i;
            sh.setVerbose(true);
        }
        if (args[i].equals("-c")) {
            ++i;
            String digest = args[i++];
            sh.checkDigest(digest, args[i]);
        } else if (args[i].equals("-s")) {
            byte[] salt = {};
            ++i;
            salt = fromHex(args[i++]);
        } else {
            usage(className);
        }
    }

    /**
	 * Encode password sha.
	 *
	 * @param passwordToEncode the password to encode
	 *
	 * @return the string
	 */
    public static String encodePasswordSha(String passwordToEncode) {
        ShaPasswordEncoder encoder = new ShaPasswordEncoder();
        String res = encoder.encodePassword(passwordToEncode, "777");
        return res;
    }
}
