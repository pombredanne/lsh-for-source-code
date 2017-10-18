package com.chungco.rest.evdb.service;

import java.io.UnsupportedEncodingException;
import com.chungco.rest.evdb.EvdbUtils;
import junit.framework.Assert;
import junit.framework.TestCase;

public class Md5MessageDigestTest extends TestCase {

    public void eg_testMD5() {
        final byte[] nonce = "0689559111".getBytes();
        final byte[] x = ":".getBytes();
        final byte[] md5pass = EvdbUtils.md5("H0gwart$".getBytes());
        final byte[] out = new byte[nonce.length + x.length + md5pass.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(x, 0, out, nonce.length, x.length);
        System.arraycopy(md5pass, 0, out, nonce.length + x.length, md5pass.length);
        System.out.print("Nonce: ");
        printBytes(nonce);
        System.out.print("\nX: ");
        printBytes(x);
        System.out.print("\nMD5: ");
        printBytes(md5pass);
        System.out.print("\nDone: ");
        printBytes(out);
        final byte[] response = EvdbUtils.md5(out);
        System.out.println("\nResponse: ");
        printBytes(response);
    }

    private void printBytes(final byte[] bytes) {
        for (byte b : bytes) {
            System.out.print(Integer.toHexString(b & 0xff) + " ");
        }
    }

    public void testEvdbMd5() {
        try {
            String done = EvdbUtils.digest("0689559111", "H0gwart$");
            final String knownResult = "ea230d9de20eaaa2d8ed286cc71a8442";
            Assert.assertEquals("The response did not match the known result", knownResult, done);
        } catch (UnsupportedEncodingException e) {
            Assert.fail("Encoding error: " + e.getMessage());
        }
    }
}
