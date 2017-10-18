package net.sf.portecle.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import net.sf.portecle.FPortecle;
import net.sf.portecle.StringUtil;

/**
 * Provides utility methods for the creation of message digests.
 */
public final class DigestUtil {

    /**
	 * Private to prevent construction.
	 */
    private DigestUtil() {
    }

    /**
	 * Get the digest of a message as a formatted String.
	 * 
	 * @param bMessage The message to digest
	 * @param digestType The message digest algorithm
	 * @return The message digest
	 * @throws CryptoException If there was a problem generating the message digest
	 */
    public static String getMessageDigest(byte[] bMessage, DigestType digestType) throws CryptoException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(digestType.name());
        } catch (NoSuchAlgorithmException ex) {
            throw new CryptoException(MessageFormat.format(FPortecle.RB.getString("NoCreateDigest.exception.message"), digestType), ex);
        }
        byte[] bFingerPrint = messageDigest.digest(bMessage);
        StringBuilder sb = StringUtil.toHex(bFingerPrint, 2, ":");
        return sb.toString();
    }
}
