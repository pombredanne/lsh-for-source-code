package org.sdmxapi.restlet.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;
import junit.framework.TestCase;

/**
 * @author david.o.evans
 *
 */
public class SecurityTest extends TestCase {

    private static final Logger logger = Logger.getLogger(SecurityTest.class);

    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
	 * @throws Exception
	 */
    public void testEncrypt() throws Exception {
        logger.info(encrypt("david.evans@modjava.com"));
    }

    /**
	 * @param plaintext
	 * @return
	 */
    public synchronized String encrypt(String plaintext) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }
}
