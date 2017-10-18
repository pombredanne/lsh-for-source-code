package org.eaasyst.eaa.security.impl;

import java.security.MessageDigest;
import org.eaasyst.eaa.security.EncrypterBase;
import org.eaasyst.eaa.security.EncryptionException;
import sun.misc.BASE64Encoder;

/**
* <p>This utility class provides a method to produce a one-way
* encryption for passwords.</p>
*
* @version 2.0
* @author Jeff Chilton
*/
public final class DefaultEncrypter extends EncrypterBase {

    /**
	 * <p>This method encrypts whatever text is passed to it. This
	 * implementation ignores the optional seed parameter and simply
	 * encrypts the text passed.</p>
	 *
	 * @param seed a security seed used to enhance the encryption
	 * @param text the String that you want encrypted
	 * @return the encrypted text
	 * @throws EncryptionException
	 * @since Eaasy Street 2.0
	 */
    protected synchronized String encryptThis(String seed, String text) throws EncryptionException {
        String encryptedValue = null;
        String textToEncrypt = text;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(textToEncrypt.getBytes("UTF-8"));
            encryptedValue = (new BASE64Encoder()).encode(md.digest());
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
        return encryptedValue;
    }
}
