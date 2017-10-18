package org.infoeng.ofbiz.ltans.test.ltap;

import junit.framework.TestCase;
import java.util.Random;
import java.util.Arrays;
import java.security.MessageDigest;
import org.infoeng.ofbiz.ltans.ltap.LTAPMessageDigest;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;

public class LTAPMessageDigestTest extends TestCase {

    private Random rnd;

    public LTAPMessageDigestTest() {
        rnd = new Random();
    }

    public void testMessageDigest() {
        byte[] rndBytes = new byte[1024];
        rnd.nextBytes(rndBytes);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
        }
        byte[] digestBytes = md.digest(rndBytes);
    }
}
