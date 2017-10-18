package com.liferay.portal.util;

import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.Digester;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="DigesterImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class DigesterImpl implements Digester {

    public String digest(String text) {
        return digest(Digester.DIGEST_ALGORITHM, text);
    }

    public String digest(String algorithm, String text) {
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance(algorithm);
            digester.update(text.getBytes(Digester.ENCODING));
        } catch (NoSuchAlgorithmException nsae) {
            _log.error(nsae, nsae);
        } catch (UnsupportedEncodingException uee) {
            _log.error(uee, uee);
        }
        byte[] bytes = digester.digest();
        if (_BASE_64) {
            return Base64.encode(bytes);
        } else {
            return new String(Hex.encodeHex(bytes));
        }
    }

    private static final boolean _BASE_64 = PropsValues.PASSWORDS_DIGEST_ENCODING.equals("base64");

    private static Log _log = LogFactory.getLog(Digester.class);
}
