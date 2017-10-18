package uk.ac.ebi.rhea.webapp.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordDigester {

    private MessageDigest md;

    private int digestLength;

    private PasswordDigester() {
    }

    private PasswordDigester(String algorithm) {
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public static PasswordDigester MD5() {
        PasswordDigester d = new PasswordDigester("MD5");
        d.digestLength = 32;
        return d;
    }

    public byte[] getDigest(String username, String password) {
        md.reset();
        md.update(username.toUpperCase().getBytes());
        md.update(password.getBytes());
        return md.digest();
    }

    public String getHexDigest(String username, String password) {
        byte[] digestBytes = getDigest(username, password);
        StringBuilder digesterSb = new StringBuilder(digestLength);
        for (int i = 0; i < digestBytes.length; i++) {
            int intValue = digestBytes[i] & 0xFF;
            if (intValue < 0x10) digesterSb.append('0');
            digesterSb.append(Integer.toHexString(intValue));
        }
        return digesterSb.toString();
    }

    /**
	 * Generates a hexadecimal string representing the digest of a
	 * username/password combination.
	 * @param args Only the 2 first will be considered:
	 * <ol><li>username</li><li>password</li></ol>
	 */
    public static void main(String[] args) {
        System.out.println(PasswordDigester.MD5().getHexDigest(args[0], args[1]));
    }
}
