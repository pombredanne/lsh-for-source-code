package gov.lanl.Utility;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import java.security.MessageDigest;

public class IdentitySHA {

    private boolean verbose = false;

    private MessageDigest sha = null;

    private static Logger cat = Logger.getLogger(IdentitySHA.class.getName());

    /**
     * public constructor
     */
    public IdentitySHA() {
        verbose = false;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (java.security.NoSuchAlgorithmException e) {
            cat.error("Constructor failed: " + e);
        }
    }

    /**
     * Create short version of a digest by taking the first {length} bytes of the hash.
     * @param salt Base for the encryption
     * @param identity Value to be encrypted
     * @param length Length of extract to keep
     * @return Digest string
     */
    public String createHL7Digest(byte[] salt, String identity, int length) {
        sha.reset();
        sha.update(identity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) cat.debug(toHex(pwhash) + " ");
        StringBuffer result = new StringBuffer(new String(Base64.encode(concatenate(pwhash, salt))));
        result.setLength(length);
        for (int i = 0; i < length; i++) {
            if (result.charAt(i) == '^') result.setCharAt(i, 'A');
        }
        return result.toString();
    }

    /**
     * Create short version of a digest by taking the first {length} bytes of the hash.
     * @param salt Base for the encryption
     * @param identity Value to be encrypted
     * @param length Length of extract to keep
     * @return Digest string
     */
    public String createShortDigest(byte[] salt, String identity, int length) {
        sha.reset();
        sha.update(identity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) cat.debug(toHex(pwhash) + " ");
        return new String(Base64.encode(concatenate(pwhash, salt))).substring(0, length);
    }

    /**
     * Create Digest for each input identity
     * @param salt to set the base for the encryption
     * @param identity to be encrypted
     */
    public String createDigest(byte[] salt, String identity) {
        String label = (salt.length > 0) ? "{SSHA}" : "{SHA}";
        sha.reset();
        sha.update(identity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) cat.debug(toHex(pwhash) + " ");
        return label + new String(Base64.encode(concatenate(pwhash, salt)));
    }

    public boolean checkShortDigest(String digest, byte[] salt, String identity) {
        int length = digest.length();
        sha.reset();
        sha.update(identity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) cat.debug(toHex(pwhash) + " ");
        String result = new String(Base64.encode(concatenate(pwhash, salt))).substring(0, length);
        return digest.equals(result);
    }

    /**
     * Check Digest against identity
     * @param digest is digest to be checked against
     * @param identity to be checked
     */
    public boolean checkDigest(String digest, String identity) {
        if (digest.regionMatches(true, 0, "{SHA}", 0, 5)) {
            digest = digest.substring(5);
        } else if (digest.regionMatches(true, 0, "{SSHA}", 0, 6)) {
            digest = digest.substring(6);
        }
        byte[][] hs = split(Base64.decode(digest), 20);
        byte[] hash = hs[0];
        byte[] salt = hs[1];
        if (verbose) cat.debug(toHex(hash) + " " + toHex(salt));
        sha.reset();
        sha.update(identity.getBytes());
        sha.update(salt);
        byte[] pwhash = sha.digest();
        if (verbose) cat.debug(toHex(pwhash));
        boolean valid = true;
        if (!MessageDigest.isEqual(hash, pwhash)) {
            valid = false;
            cat.warn("doesn't match: " + identity);
        }
        return valid;
    }

    /**
     * set the verbose flag
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Combine two byte arrays
     * @param l first byte array
     * @param r second byte array
     * @return byte[] combined byte array
     */
    private static byte[] concatenate(byte[] l, byte[] r) {
        byte[] b = new byte[l.length + r.length];
        System.arraycopy(l, 0, b, 0, l.length);
        System.arraycopy(r, 0, b, l.length, r.length);
        return b;
    }

    /**
     * split a byte array in two
     * @param src byte array to be split
     * @param n element at which to split the byte array
     * @return byte[][]  two byte arrays that have been split
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
        return new byte[][] { l, r };
    }

    private static String hexits = "0123456789abcdef";

    /**
     *  Convert byte array to hex character string
     *  @param block byte array to convert to hexString
     *  @return String representation of byte arrayf
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
     * Convert Hex String to byte array
     * @param s string to convert
     * @return byte array
     */
    private static byte[] fromHex(String s) {
        s = s.toLowerCase();
        byte[] b = new byte[(s.length() + 1) / 2];
        int j = 0;
        int h;
        int nybble = -1;
        for (int i = 0; i < s.length(); ++i) {
            h = hexits.indexOf(s.charAt(i));
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
     * Main program for command line use and testing
     */
    public static void main(String[] args) {
        IdentitySHA sh = new IdentitySHA();
        if (args.length < 1) {
            String myName = "IdentitySHA";
            System.out.print("usage: " + myName + " [-v] [-s salt] identity ...\n" + "   or: " + myName + " [-v] -c digest identity ...\n" + "       salt is in hexadecimal notation.\n" + "       digest contains SHA-1 hash and salt, base64 encoded.\n");
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
            for (; i < args.length; ++i) {
                sh.checkDigest(digest, args[i]);
            }
        } else {
            byte[] salt = {};
            if (args[i].equals("-s")) {
                ++i;
                salt = fromHex(args[i++]);
                if (sh.verbose) System.out.println(toHex(salt));
            }
            for (; i < args.length; ++i) {
                System.out.println(sh.createDigest(salt, args[i]));
            }
        }
    }
}
