package fi.arcusys.qnet.common.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities for manipulating passwords.
 * @author mikko
 * @version 1.0 $Rev: 66 $
 */
public class PasswordHelper {

    public static final String ALGORITHM_MD5 = "MD5";

    public static final String DEFAULT_ALGORITHM = ALGORITHM_MD5;

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Two arguments required: <user name> and <password>");
            System.exit(1);
        }
        String userName = args[0];
        String password = args[1];
        System.out.println(encodePassword(userName, password.toCharArray()));
    }

    /**
	 * Convert byte data to hexadecimal characters.
	 * @param in
	 * @return
	 */
    public static char[] toHex(byte[] in) {
        int len = in.length;
        char[] out = new char[len * 2];
        int j = 0;
        for (int i = 0; i < len; i++) {
            out[j++] = HEX_DIGITS[(240 & in[i]) >>> 4];
            out[j++] = HEX_DIGITS[15 & in[i]];
        }
        return out;
    }

    /**
	 * Digest input data using specified algorithm and return the digest
	 * output data using hexadecimal encoding.
	 * @param dt
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
    public static char[] toHexDigest(String algorithm, char[]... dt) throws NoSuchAlgorithmException {
        char[] out;
        MessageDigest md = MessageDigest.getInstance(algorithm);
        StringBuilder sb = new StringBuilder();
        for (char[] dtp : dt) {
            sb.append(dtp);
        }
        String s = sb.toString();
        sb.delete(0, sb.length());
        byte[] bytes = s.getBytes();
        s = null;
        try {
            byte[] md5bytes = md.digest(bytes);
            out = toHex(md5bytes);
        } finally {
            int l = bytes.length;
            for (int i = 0; i < l; i++) {
                bytes[i] = 0;
            }
        }
        return out;
    }

    /**
	 * Encode a plaintext password by digesting it with the
	 * specified algorithm and converting byte data to hexadecimal
	 * encoding.
	 * 
	 * <p>Digest is calculated from string <code>usernamepassword
	 * </code>, for example if user is "alice" and her password is
	 * "bob4ever", digest is calculated from string "alicebob4ever".</p>
	 * 
	 * <p>Syntax of password:<p>
	 * 
	 * <pre>
	 *   ["{" algorithm "}"] data
	 *   
	 *     algorithm = digest algorithm name (e.g. "MD5")
	 *     data = HEX-encoded bytes
	 *     
	 *  If algorithm is not specified, data contains plaintext password.
	 * </pre>
	 * 
	 * <p>Letters (A-F) in the hex data are always in upper-case.</p>
	 * 
	 * <p>Plase note that "MD5" is the only required message digesting 
	 * algorithm to be supported.</p>
	 * 
	 * @param algorithm the algorithm to be used
	 * @param plainPassword plaintext password data
	 * @return encoded password
	 */
    public static String encodePassword(String algorithm, String username, char[] plainPassword) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        if (null == algorithm) {
            algorithm = DEFAULT_ALGORITHM;
        }
        sb.append("{");
        sb.append(algorithm);
        sb.append("}");
        sb.append(toHexDigest(algorithm, username.toCharArray(), plainPassword));
        return sb.toString();
    }

    /**
	 * Encode a password with the default digest algorithm, which
	 * is MD5.
	 * @param plainPassword
	 * @return
	 */
    public static String encodePassword(String username, char[] plainPassword) {
        try {
            return encodePassword(DEFAULT_ALGORITHM, username, plainPassword);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class DecodedPassword {

        public String algorithm;

        public char[] hexData;
    }

    /**
	 * Decode a password, i.e. extract the digest algorithm part
	 * ("{algorithm}") and the password data. If the password is
	 * not digested, data is the password as plaintext.
	 * 
	 * <p>If the data contains hexadecimal encoding, it's converted
	 * to upper case.-/p>
	 * 
	 * @param encodedPassword
	 * @return
	 */
    public static DecodedPassword decodePassword(char[] encodedPassword) {
        DecodedPassword decoded = new DecodedPassword();
        if (encodedPassword[0] == '{') {
            int i = 1;
            int len = encodedPassword.length;
            while (i < len && encodedPassword[i] != '}') {
                i++;
            }
            if (i < len) {
                decoded.algorithm = new String(encodedPassword, 1, i - 1);
                int dataLen = len - i - 1;
                decoded.hexData = new char[dataLen];
                if (dataLen > 0) {
                    System.arraycopy(encodedPassword, i + 1, decoded.hexData, 0, dataLen);
                    for (int j = 0; j < dataLen; j++) {
                        char ch = decoded.hexData[j];
                        if (ch > '9') {
                            decoded.hexData[j] = Character.toUpperCase(ch);
                        }
                    }
                }
            } else {
                decoded.hexData = new char[0];
            }
        } else {
            decoded.algorithm = null;
            decoded.hexData = new char[encodedPassword.length];
            System.arraycopy(encodedPassword, 0, decoded.hexData, 0, encodedPassword.length);
        }
        return decoded;
    }

    /**
	 * Extract the "{algorithm}" part of a encoded password.
	 * 
	 * @param encodedPassword
	 * @return
	 */
    public static String getAlgorithm(char[] encodedPassword) {
        String alg = null;
        if (encodedPassword[0] == '{') {
            int i = 0;
            int len = encodedPassword.length;
            while (i < len && encodedPassword[i] != '{') {
                i++;
            }
            alg = new String(encodedPassword, 1, i - 1);
        }
        return alg;
    }
}
