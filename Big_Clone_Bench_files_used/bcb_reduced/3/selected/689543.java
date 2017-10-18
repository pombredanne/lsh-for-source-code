package net.sf.eqemutils.utils;

import java.io.*;
import java.security.*;
import java.util.*;

/** Collection of utility functions for MD5 computation.
 */
public class MD5Utils {

    /** Computes the MD5 hash for 'ThisString'.
   */
    public static byte[] ComputeForBinary(String ThisString) throws Exception {
        byte[] Result;
        MessageDigest MD5Hasher;
        MD5Hasher = MessageDigest.getInstance("MD5");
        MD5Hasher.update(ThisString.getBytes("iso-8859-1"));
        Result = MD5Hasher.digest();
        return Result;
    }

    /** Computes the MD5 hash for 'ThisString'.
   */
    public static byte[] ComputeForText(String ThisString) throws Exception {
        byte[] Result;
        MessageDigest MD5Hasher;
        MD5Hasher = MessageDigest.getInstance("MD5");
        MD5Hasher.update(ThisString.replaceAll("\r", "").getBytes("iso-8859-1"));
        Result = MD5Hasher.digest();
        return Result;
    }

    /** Returns a readable (hexa) version of the hash 'ThisHash'.
   */
    public static String HashToString(byte[] ThisHash) {
        String Result;
        StringBuffer recResult;
        recResult = new StringBuffer();
        for (byte ThisByte : ThisHash) recResult.append(Integer.toHexString(ThisByte & 0xFF));
        Result = "" + recResult;
        return Result;
    }

    /** Returns the hash symbolized by 'ThisHashString'.
   */
    public static byte[] StringToHash(String ThisHashString) throws Exception {
        byte[] Result;
        int j, jEnd;
        if (ThisHashString.length() % 2 == 1) throw new Exception("error : this hash does not contain an even amount of characters '" + ThisHashString + "'");
        Result = new byte[ThisHashString.length() / 2];
        for (j = 1, jEnd = Result.length; j <= jEnd; j++) Result[j - 1] = (byte) Integer.parseInt(ThisHashString.substring(2 * (j - 1), 2 * (j - 1) + 2), 16);
        return Result;
    }

    /** Tells whether 'Hash_1' and 'Hash_2' contain exactly the same bytes.
   */
    public static boolean Equals(byte[] Hash_1, byte[] Hash_2) {
        boolean Result;
        int j, jEnd;
        Result = (Hash_1.length == Hash_2.length);
        for (j = 1, jEnd = Hash_1.length; Result && j <= jEnd; j++) Result = (Hash_1[j - 1] == Hash_2[j - 1]);
        return Result;
    }

    public static void main(String Arguments[]) {
        String ThisFileContents;
        int j, jEnd;
        if (Arguments.length < 1) {
            System.err.println("usage: MD5Utils <file>...");
            System.err.println("  Prints the MD5 hash of each <file> along with its name");
            System.exit(1);
        }
        try {
            for (j = 1, jEnd = Arguments.length; j <= jEnd; j++) {
                ThisFileContents = ReaderUtils.ReadAll(new File(Arguments[j - 1]));
                System.out.println("" + HashToString(ComputeForText(ThisFileContents)) + "  " + Arguments[j - 1]);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
