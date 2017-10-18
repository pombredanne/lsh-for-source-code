package org.apache.harmony.security.tests.provider.crypto;

import java.io.UnsupportedEncodingException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.harmony.security.provider.crypto.SHA1Impl;
import java.security.MessageDigest;

/**
 * Tests against methods in SHA1Impl class.
 * The input data and results of computing are defined in Secure Hash Standard,
 * see http://www.itl.nist.gov/fipspubs/fip180-1.htm
 */
public class SHA1ImplTest extends TestCase {

    private static final int INDEX = SHA1Impl.BYTES_OFFSET;

    private static MessageDigest md;

    protected void setUp() throws Exception {
        super.setUp();
        md = MessageDigest.getInstance("SHA-1", "Crypto");
    }

    public final void testOneBlockMessage() {
        int[] words = new int[INDEX + 6];
        int[] hash1 = { 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };
        int[] hash = { 0xA9993E36, 0x4706816A, 0xBA3E2571, 0x7850C26C, 0x9CD0D89D };
        for (int i = 0; i < words.length; i++) {
            words[i] = 0;
        }
        words[0] = 0x61626380;
        words[15] = 0x00000018;
        alternateHash(words, hash1);
        md.update(new byte[] { 0x61, 0x62, 0x63 });
        byte[] dgst = md.digest();
        for (int k = 0; k < 5; k++) {
            int i = k * 4;
            int j = ((dgst[i] & 0xff) << 24) | ((dgst[i + 1] & 0xff) << 16) | ((dgst[i + 2] & 0xff) << 8) | (dgst[i + 3] & 0xff);
            assertTrue("false1: k=" + k + " hash1[k]=" + Integer.toHexString(hash1[k]), hash[k] == hash1[k]);
            assertTrue("false2: k=" + k + " j=" + Integer.toHexString(j), hash[k] == j);
        }
    }

    public final void testMultiBlockMessage() throws UnsupportedEncodingException {
        int[] hash = { 0x84983e44, 0x1c3bd26e, 0xbaae4aa1, 0xf95129e5, 0xe54670f1 };
        md.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes("UTF-8"));
        byte[] dgst = md.digest();
        for (int k = 0; k < 5; k++) {
            int i = k * 4;
            int j = ((dgst[i] & 0xff) << 24) | ((dgst[i + 1] & 0xff) << 16) | ((dgst[i + 2] & 0xff) << 8) | (dgst[i + 3] & 0xff);
            assertTrue("false: k=" + k + " j=" + Integer.toHexString(j), hash[k] == j);
        }
    }

    public final void testLongMessage() {
        int[] hash = { 0x34aa973c, 0xd4c4daa4, 0xf61eeb2b, 0xdbad2731, 0x6534016f };
        byte msgs[][] = new byte[][] { { 0x61 }, { 0x61, 0x61 }, { 0x61, 0x61, 0x61 }, { 0x61, 0x61, 0x61, 0x61 } };
        int lngs[] = new int[] { 1000000, 500000, 333333, 250000 };
        for (int n = 0; n < 4; n++) {
            for (int i = 0; i < lngs[n]; i++) {
                md.update(msgs[n]);
            }
            if (n == 2) {
                md.update(msgs[0]);
            }
            byte[] dgst = md.digest();
            for (int k = 0; k < 5; k++) {
                int i = k * 4;
                int j = ((dgst[i] & 0xff) << 24) | ((dgst[i + 1] & 0xff) << 16) | ((dgst[i + 2] & 0xff) << 8) | (dgst[i + 3] & 0xff);
                assertTrue("false: n =" + n + "  k=" + k + " j" + Integer.toHexString(j), hash[k] == j);
            }
        }
    }

    /**
     * implements alternative algorithm described in the SECURE HASH STANDARD
     */
    private void alternateHash(int[] bufW, int[] hash) {
        final int[] K = { 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6 };
        int a = hash[0];
        int b = hash[1];
        int c = hash[2];
        int d = hash[3];
        int e = hash[4];
        final int MASK = 0x0000000F;
        int temp;
        int s;
        int tmp;
        for (int t = 0; t < 80; t++) {
            s = t & MASK;
            if (t >= 16) {
                tmp = bufW[(s + 13) & MASK] ^ bufW[(s + 8) & MASK] ^ bufW[(s + 2) & MASK] ^ bufW[s];
                bufW[s] = (tmp << 1) | (tmp >>> 31);
            }
            temp = (a << 5) | (a >>> 27);
            if (t < 20) {
                temp += (b & c) | ((~b) & d);
            } else if (t < 40) {
                temp += b ^ c ^ d;
            } else if (t < 60) {
                temp += (b & c) | (b & d) | (c & d);
            } else {
                temp += b ^ c ^ d;
            }
            temp += e + bufW[s] + K[t];
            e = d;
            d = c;
            c = (b << 30) | (b >>> 2);
            b = a;
            a = temp;
        }
        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
    }

    public static Test suite() {
        return new TestSuite(SHA1ImplTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
