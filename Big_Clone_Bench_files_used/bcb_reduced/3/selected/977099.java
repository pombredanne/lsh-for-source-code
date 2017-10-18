package org.apache.harmony.security.tests.java.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestInputStream2Test extends junit.framework.TestCase {

    ByteArrayInputStream inStream;

    ByteArrayInputStream inStream1;

    MessageDigest digest;

    /**
	 * @tests java.security.DigestInputStream#DigestInputStream(java.io.InputStream,
	 *        java.security.MessageDigest)
	 */
    public void test_ConstructorLjava_io_InputStreamLjava_security_MessageDigest() {
        DigestInputStream dis = new DigestInputStream(inStream, digest);
        assertNotNull("Constructor returned null instance", dis);
    }

    /**
	 * @tests java.security.DigestInputStream#getMessageDigest()
	 */
    public void test_getMessageDigest() {
        DigestInputStream dis = new DigestInputStream(inStream, digest);
        assertEquals("getMessageDigest returned a bogus result", digest, dis.getMessageDigest());
    }

    /**
	 * @tests java.security.DigestInputStream#on(boolean)
	 */
    public void test_onZ() throws Exception {
        MessageDigest originalDigest = (MessageDigest) (digest.clone());
        MessageDigest noChangeDigest = (MessageDigest) (digest.clone());
        DigestInputStream dis = new DigestInputStream(inStream, noChangeDigest);
        dis.on(false);
        int c = dis.read();
        assertEquals('T', c);
        assertTrue("MessageDigest changed even though processing was off", MessageDigest.isEqual(noChangeDigest.digest(), originalDigest.digest()));
        MessageDigest changeDigest = (MessageDigest) (digest.clone());
        dis = new DigestInputStream(inStream, digest);
        dis.on(true);
        c = dis.read();
        assertEquals('h', c);
        assertTrue("MessageDigest did not change with processing on", !MessageDigest.isEqual(digest.digest(), changeDigest.digest()));
    }

    /**
	 * @tests java.security.DigestInputStream#read()
	 */
    public void test_read() throws IOException {
        DigestInputStream dis = new DigestInputStream(inStream, digest);
        int c;
        while ((c = dis.read()) > -1) {
            int d = inStream1.read();
            assertEquals(d, c);
        }
    }

    /**
	 * @tests java.security.DigestInputStream#read(byte[], int, int)
	 */
    public void test_read$BII() throws IOException {
        DigestInputStream dis = new DigestInputStream(inStream, digest);
        int bytesToRead = inStream.available();
        byte buf1[] = new byte[bytesToRead + 5];
        byte buf2[] = new byte[bytesToRead + 5];
        assertTrue("No data to read for this test", bytesToRead > 0);
        int bytesRead1 = dis.read(buf1, 5, bytesToRead);
        int bytesRead2 = inStream1.read(buf2, 5, bytesToRead);
        assertEquals("Didn't read the same from each stream", bytesRead1, bytesRead2);
        assertEquals("Didn't read the entire", bytesRead1, bytesToRead);
        boolean same = true;
        for (int i = 0; i < bytesToRead + 5; i++) {
            if (buf1[i] != buf2[i]) {
                same = false;
            }
        }
        assertTrue("Didn't get the same data", same);
    }

    /**
	 * @tests java.security.DigestInputStream#setMessageDigest(java.security.MessageDigest)
	 */
    public void test_setMessageDigestLjava_security_MessageDigest() {
        DigestInputStream dis = new DigestInputStream(inStream, null);
        assertNull("Uninitialised MessageDigest should have been returned as null", dis.getMessageDigest());
        dis.setMessageDigest(digest);
        assertEquals("Wrong MessageDigest was returned.", digest, dis.getMessageDigest());
    }

    /**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 * @throws UnsupportedEncodingException 
	 */
    protected void setUp() throws UnsupportedEncodingException {
        inStream = new ByteArrayInputStream("This is a test string for digesting".getBytes("UTF-8"));
        inStream1 = new ByteArrayInputStream("This is a test string for digesting".getBytes("UTF-8"));
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            fail("Unable to find SHA-1 algorithm");
        }
    }
}
