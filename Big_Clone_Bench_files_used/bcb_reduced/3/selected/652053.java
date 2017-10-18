package org.apache.harmony.security.tests.java.security;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import junit.framework.TestCase;
import org.apache.harmony.security.tests.support.MyMessageDigest1;

/**
 * Tests for <code>MessageDigest</code> constructor and methods
 */
public class MessageDigest1Test extends TestCase {

    /**
     * @tests java.security.MessageDigest#reset()
     */
    public void test_reset() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        md.reset();
        assertTrue(md.runEngineReset);
    }

    /**
     * @tests java.security.MessageDigest#update(byte)
     */
    public void test_updateLB() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        md.update((byte) 1);
        assertTrue(md.runEngineUpdate1);
    }

    /**
     * @tests java.security.MessageDigest#update(byte[], int, int)
     */
    public void test_updateLB$LILI() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        final byte[] bytes = { 1, 2, 3, 4, 5 };
        md.update(bytes, 1, 2);
        assertTrue(md.runEngineUpdate2);
        try {
            md.update(null, 0, 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            md.update(bytes, 0, bytes.length + 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            md.update(bytes, Integer.MAX_VALUE, 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        final int offset = -1;
        final int len = -1;
        md = new MyMessageDigest1("ABC") {

            @Override
            public void engineUpdate(byte[] arg0, int arg1, int arg2) {
                assertSame("buf", bytes, arg0);
                assertEquals("offset", offset, arg1);
                assertEquals("len", len, arg2);
                runEngineUpdate2 = true;
            }
        };
        md.update(bytes, offset, len);
        assertTrue(md.runEngineUpdate2);
    }

    /**
     * @tests java.security.MessageDigest#update(byte[])
     */
    public void test_updateLB$() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        byte[] b = { 1, 2, 3, 4, 5 };
        md.update(b);
        assertTrue(md.runEngineUpdate2);
    }

    /**
     * @tests java.security.MessageDigest#update(ByteBuffer)
     */
    public void test_updateLjava_nio_ByteBuffer() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        byte[] b = { 1, 2, 3, 4, 5 };
        ByteBuffer byteBuffer = ByteBuffer.wrap(b);
        int limit = byteBuffer.limit();
        md.update(byteBuffer);
        assertTrue(md.runEngineUpdate2);
        assertEquals(byteBuffer.limit(), byteBuffer.position());
        assertEquals(limit, byteBuffer.limit());
    }

    /**
     * @tests java.security.MessageDigest#digest()
     */
    public void test_digest() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        assertEquals("incorrect result", 0, md.digest().length);
        assertTrue(md.runEngineDigest);
    }

    /**
     * @tests java.security.MessageDigest#digest(byte[])
     */
    public void test_digestLB$() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        byte[] b = { 1, 2, 3, 4, 5 };
        assertEquals("incorrect result", 0, md.digest(b).length);
        assertTrue(md.runEngineDigest);
    }

    /**
     * @tests java.security.MessageDigest#digest(byte[], int, int)
     */
    public void test_digestLB$LILI() throws Exception {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        byte[] b = { 1, 2, 3, 4, 5 };
        assertEquals("incorrect result", 0, md.digest(b, 2, 3));
        assertTrue("digest failed", md.runEngineDigest);
        md = new MyMessageDigest1();
        final byte[] bytes = new byte[] { 2, 4, 1 };
        try {
            md.digest(null, 0, 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            md.digest(bytes, 0, bytes.length + 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            md.digest(bytes, Integer.MAX_VALUE, 1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        final int offset = -1;
        final int len = -1;
        final int status = 33;
        md = new MyMessageDigest1("ABC") {

            @Override
            public int engineDigest(byte[] arg0, int arg1, int arg2) {
                assertSame("buf", bytes, arg0);
                assertEquals("offset", offset, arg1);
                assertEquals("len", len, arg2);
                return status;
            }
        };
        assertEquals("returned status", status, md.digest(bytes, offset, len));
    }

    /**
     * @tests java.security.MessageDigest#isEqual(byte[],byte[])
     */
    public void test_isEqualLB$LB$() {
        byte[] b1 = { 1, 2, 3, 4 };
        byte[] b2 = { 1, 2, 3, 4, 5 };
        byte[] b3 = { 1, 3, 3, 4 };
        byte[] b4 = { 1, 2, 3, 4 };
        assertTrue(MessageDigest.isEqual(b1, b4));
        assertFalse(MessageDigest.isEqual(b1, b2));
        assertFalse(MessageDigest.isEqual(b1, b3));
    }

    /**
     * @tests java.security.MessageDigest#getAlgorithm()
     */
    public void test_getAlgorithm() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        assertEquals("ABC", md.getAlgorithm());
    }

    /**
     * @tests java.security.MessageDigest#getProvider()
     */
    public void test_getProvider() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        assertNull(md.getProvider());
    }

    /**
     * @tests java.security.MessageDigest#getDigestLength()
     */
    public void test_getDigestLength() {
        MyMessageDigest1 md = new MyMessageDigest1("ABC");
        assertEquals(0, md.getDigestLength());
    }

    /**
     * Tests SHA MessageDigest provider
     */
    public void testSHAProvider() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] bytes = new byte[] { 1, 1, 1, 1, 1 };
        try {
            md.update(bytes, -1, 1);
            fail("No expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        md.update(bytes, 1, -1);
        md = MessageDigest.getInstance("SHA");
        try {
            md.digest(bytes, 0, -1);
            fail("No expected DigestException");
        } catch (DigestException e) {
        }
        try {
            md.digest(bytes, -1, 0);
            fail("No expected DigestException");
        } catch (DigestException e) {
        }
    }
}
