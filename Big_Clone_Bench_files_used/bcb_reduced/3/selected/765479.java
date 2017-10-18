package org.ikasan.common.util.checksum;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple DigestChecksum implementation utilising an MD5 Digest
 * 
 * Public interface is similar to the java.util.zip.Checksum
 * interface, except that rather than implementing the 
 * getValue method returning a long, we are instead interested
 * in the String value of the resultant hash on digest.
 * 
 * Also note that calling any of the digest methods resets 
 * the digest itself to its initial state
 * 
 * @author Ikasan Development Team
 *
 */
public class Md5Checksum implements DigestChecksum {

    /**
	 * String representation of the Algorithm itself
	 */
    private static final String MD5_ALGORITHM = "MD5";

    /**
	 * Message Digest
	 */
    private MessageDigest messageDigest;

    /**
	 * Constructor
	 */
    public Md5Checksum() {
        super();
        reset();
    }

    public String digestToString() {
        byte[] byteDigest = messageDigest.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteDigest.length; i++) {
            String hex = Integer.toHexString(0xff & byteDigest[i]);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    public BigInteger digestToBigInteger() {
        return new BigInteger(digestToString(), 16);
    }

    public void reset() {
        try {
            messageDigest = MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void update(byte b) {
        messageDigest.update(b);
    }

    public void update(byte[] b, int off, int len) {
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        messageDigest.update(b, off, len);
    }

    public void update(byte[] bytes) {
        messageDigest.update(bytes);
    }
}
