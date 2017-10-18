package org.furthurnet.md5;

import java.io.*;

class MD5TestSuite {

    private static void printHex(byte b) {
        if (((int) b & 0xff) < 0x10) System.out.print("0");
        System.out.print(Long.toString(((int) b) & 0xff, 16));
    }

    private static void teststring(String s, String real) {
        byte hash[];
        MD5 md5 = new MD5();
        int i;
        String hex;
        System.out.print("MD5(\"" + s + "\"): ");
        md5.update(s);
        hex = md5.asHex();
        if (hex.equals(real)) System.out.println("ok"); else System.out.println("***FAILED***");
    }

    public static void testsuite() {
        teststring("", "d41d8cd98f00b204e9800998ecf8427e");
        teststring("a", "0cc175b9c0f1b6a831c399e269772661");
        teststring("abc", "900150983cd24fb0d6963f7d28e17f72");
        teststring("message digest", "f96b697d7cb7938d525a2f31aaf161d0");
        teststring("abcdefghijklmnopqrstuvwxyz", "c3fcd3d76192e4007dfb496cca67e13b");
        teststring("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", "d174ab98d277d9f5a5611c2c9f419d9f");
        teststring("12345678901234567890123456789012345678901234567890123456789012345678901234567890", "57edf4a22be3c955ac49da2e2107b67a");
    }

    public static void teststream(String file) throws IOException {
        if (file != null) System.out.print(file + ": ");
        System.out.println(MD5.getMD5(new File(file)));
    }

    public static void testfile(String file) {
        long before = 0, after = 0;
        try {
            before = System.currentTimeMillis();
            teststream(file);
            after = System.currentTimeMillis();
        } catch (IOException e) {
            System.out.println("IOException in \"" + file + "\"");
        }
        System.out.println("FileTestDone: " + (after - before) + " milliseconds");
    }

    public static void testtimetrial() {
        int test_block_len = 1000, test_block_count = 10000;
        byte block[] = new byte[test_block_len], hash[];
        int i;
        long start, end;
        MD5 md5;
        System.out.print("MD5 time trial. Digesting " + test_block_count + " " + test_block_len + "-byte blocks ...");
        for (i = 0; i < block.length; i++) {
            block[i] = (byte) (i & 0xff);
        }
        md5 = new MD5();
        start = System.currentTimeMillis();
        for (i = 0; i < test_block_count; i++) md5.update(block, test_block_len);
        hash = md5.digest();
        end = System.currentTimeMillis();
        System.out.print(" done\nDigest = " + md5.asHex() + "\nTime = " + (end - start) + " milliseconds\n");
    }

    public static void main(String args[]) {
        int l;
        int i;
        byte[] arr = new byte[256];
        testtimetrial();
        testsuite();
        for (i = 0; i < args.length; i++) {
            testfile(args[i]);
        }
    }
}
