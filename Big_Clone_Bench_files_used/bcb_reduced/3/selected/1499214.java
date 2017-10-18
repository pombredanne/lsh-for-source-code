package com.kapil.framework.test.crypto;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.kapil.framework.crypto.DigesterFactory;
import com.kapil.framework.crypto.IDigester;
import com.kapil.framework.crypto.UniqueTokenGenerator;

/**
 * Test cases for Digester methods.
 */
public class CryptoTest {

    @Test
    public void testSecureDigest() {
        IDigester simpleDigester = DigesterFactory.getInstance().getSecureDigester();
        String secureString = simpleDigester.digest("kapil");
        Assert.assertEquals(secureString, "{SHA}/KtCPCInoz9L+EoRDHfHxvvubXk=");
    }

    @Test
    public void testSaltedSecureDigest() {
        IDigester secureDigester = DigesterFactory.getInstance().getSaltedSecureDigester();
        secureDigester.digest("kapil");
    }

    @Test
    public void testNullSecureDigest() {
        IDigester secureDigester = DigesterFactory.getInstance().getSecureDigester();
        String digest = secureDigester.digest(null);
        Assert.assertEquals(digest, null);
    }

    @Test
    public void testNullSaltedSecureDigest() {
        IDigester secureDigester = DigesterFactory.getInstance().getSaltedSecureDigester();
        String digest = secureDigester.digest(null);
        Assert.assertEquals(digest, null);
    }

    @Test
    public void testUniqueToken() {
        UniqueTokenGenerator.getUniqueToken();
    }

    @Test
    public void testDecoding() {
        String encodedString = Base64.encodeBase64String(new byte[] { 'k', 'a' });
        byte[] decodedString = Base64.decodeBase64(encodedString);
        Assert.assertEquals(decodedString, new byte[] { 'k', 'a' });
    }

    @Test
    public void testNullStringEncode() {
        String encodedString = Base64.encodeBase64String(null);
        Assert.assertEquals(encodedString, null);
    }
}
