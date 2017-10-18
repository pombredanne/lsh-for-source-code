package org.pixory.pxfoundation;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * produces a security hashed, and then text encoded, representation of the
 * input
 */
public class PXCipher extends Object {

    private static final Log LOG = LogFactory.getLog(PXCipher.class);

    private static final String CHARACTER_ENCODING_NAME = "UTF-8";

    private static final String MESSAGE_DIGEST_NAME = "SHA-1";

    private PXCipher() {
    }

    /**
	 * @return returns null if null input or couldn't acquire a MessageDigest for
	 *         some reason (restrictions on the provider?). Return value is
	 *         Base64 encoded after encryption
	 */
    public static String getCipherText(String text) {
        String getCipherText = null;
        if ((text != null) && (text.length() > 0)) {
            try {
                MessageDigest aDigest = MessageDigest.getInstance(MESSAGE_DIGEST_NAME);
                if (aDigest != null) {
                    byte[] someBytes = text.getBytes(CHARACTER_ENCODING_NAME);
                    byte[] someCipherBytes = aDigest.digest(someBytes);
                    byte[] someCipherCharactersBytes = Base64.encodeBase64(someCipherBytes);
                    getCipherText = new String(someCipherCharactersBytes, "US-ASCII");
                }
            } catch (Exception anException) {
                LOG.warn(null, anException);
            }
        }
        return getCipherText;
    }
}
