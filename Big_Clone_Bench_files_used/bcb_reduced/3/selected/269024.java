package org.dea.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import junit.framework.TestCase;

public class CheckSummerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CheckSummerTest.class);
    }

    public void testGetDigestOutputStreams() throws Exception {
        File testFile = new File("test/FileCheckSummer");
        File copyTestFile = new File("test/FileCheckSummer2");
        testFile.delete();
        copyTestFile.delete();
        assertTrue(!testFile.exists());
        assertTrue(!copyTestFile.exists());
        for (long length = 1; length <= 512; length++) {
            FileOutputStream out = new FileOutputStream(testFile);
            DigestOutputStream dOut = CheckSummer.getDigestOutputStream(out);
            fill(dOut, length);
            byte[] digesfromStream = dOut.getMessageDigest().digest();
            byte[] digestExpected = CheckSummer.createChecksum(testFile.getAbsolutePath());
            assertEquals(digestExpected, digesfromStream);
            dOut.close();
            out.close();
            testFile.delete();
        }
    }

    private void assertEquals(byte[] digestExpected, byte[] fromStream) {
        assertEquals(digestExpected.length, fromStream.length);
        for (int i = 0; i < digestExpected.length; i++) {
            assertEquals(digestExpected[i], fromStream[i]);
        }
    }

    private void fill(OutputStream dOut, long length) throws IOException {
        for (int i = 0; i < length; i++) {
            dOut.write('a');
            dOut.flush();
        }
    }
}
