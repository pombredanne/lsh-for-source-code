package net.sf.catchup.common.crypto;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import net.sf.catchup.common.logging.LoggerFactory;
import net.sf.catchup.server.crypto.CryptoConstants;
import com.thoughtworks.xstream.core.util.Base64Encoder;

public class CryptoHelper {

    private static KeyPair keyPair = null;

    private CryptoHelper() {
    }

    /**
	 * Creates a SHA digest from the given <code>input</code> string
	 * 
	 * @param input
	 *            String to be digested
	 * @return input SHA digest of the input
	 */
    public static byte[] digest(final String input) {
        byte[] digestBytes = null;
        try {
            final MessageDigest md = MessageDigest.getInstance(CryptoConstants.SHA);
            digestBytes = md.digest(input.getBytes());
        } catch (final Exception e) {
            LoggerFactory.getAsyncLogger().error("An error occured while creating the digest", e);
        }
        return digestBytes;
    }

    public static String getEncoded(final byte[] bytes) {
        return new Base64Encoder().encode(bytes);
    }

    public static byte[] getDecoded(final String str) {
        return new Base64Encoder().decode(str);
    }

    /**
	 * Gets the public key of the corresponsing keypair which would've been
	 * created when the class loads
	 * 
	 * @return Public key for encrypting data, which the private key in the
	 *         generated keypair can decrypt
	 */
    public static PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
