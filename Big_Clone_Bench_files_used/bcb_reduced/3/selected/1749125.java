package com.aratana.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;

public class BinaryUtilities {

    private static MessageDigest md2Digest;

    private static MessageDigest sha1Digest;

    private static MessageDigest sha256Digest;

    private static MessageDigest sha512Digest;

    public static void main(String[] args) {
        File file = FileUtilities.selectFile(null, null, null, null);
        try {
            System.err.println(new String(BinaryUtilities.byteToHexa(getMd5(new FileInputStream(file)))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] invert(byte[] source, int offset, int length) {
        final byte[] result = new byte[length];
        for (int i = length - 1, j = offset; i > 0; i--, j++) {
            result[i] = source[j];
        }
        return result;
    }

    public static BitSet byteToBitSet(final byte[] ah_byte) {
        final BitSet lo_return = new BitSet(ah_byte.length * 8);
        for (int ln = 0; ln < ah_byte.length; ln++) {
            for (int ln_2 = 8; ln_2 > 0; ln_2--) {
                if ((ah_byte[ln] << ln_2 & 0x80) == 0x08) {
                    lo_return.set((ln + 1) * ln_2);
                }
            }
        }
        return lo_return;
    }

    public static byte[] byteToHexa(final byte[] bytes) {
        final byte[] result = new byte[bytes.length * 2];
        int position = 0;
        for (int i = 0; i < bytes.length; i++) {
            position = i * 2;
            result[position] = (byte) ((bytes[i] & 0xF0) >> 4);
            result[position] += result[position] > 9 ? 0x37 : 0x30;
            position++;
            result[position] = (byte) (bytes[i] & 0x0F);
            result[position] += result[position] > 9 ? 0x37 : 0x30;
        }
        return result;
    }

    public static int byteToInt(final byte[] ah_byte) {
        return byteToInt(ah_byte, 0);
    }

    public static int byteToInt(final byte[] ah_byte, final int offset) {
        return (int) byteToNumber(ah_byte, offset, 4);
    }

    public static short byteToShort(final byte[] ah_byte) {
        return byteToShort(ah_byte, 0);
    }

    public static short byteToShort(final byte[] ah_byte, final int offset) {
        return (short) byteToNumber(ah_byte, offset, 2);
    }

    public static long byteToLong(final byte[] ah_byte) {
        return byteToNumber(ah_byte, 0, 8);
    }

    public static long byteToNumber(final byte[] ah_byte, final int offset, final int an_bytes) {
        long ln_return = 0l;
        for (int ln = offset, ln_1 = an_bytes - 1; ln < offset + an_bytes; ln++, ln_1--) {
            ln_return += ((long) ah_byte[ln] & 0xff) << ln_1 * 8;
        }
        return ln_return;
    }

    public static Integer[] getDecomposicao(int number) {
        Integer[] decomposicao = null;
        final ArrayList<Integer> buffer = new ArrayList<Integer>();
        final int log = (int) (Math.log(number) / Math.log(2));
        for (int i = log, j = (int) Math.pow(2, log); number > 0; i--, j /= 2) {
            if (number >= j) {
                number -= j;
                buffer.add(i);
            }
        }
        buffer.toArray(decomposicao = new Integer[buffer.size()]);
        return decomposicao;
    }

    public static byte[] getMd2(final byte[] data) {
        return getMd2Digest().digest(data);
    }

    private static MessageDigest getMd2Digest() {
        if (md2Digest == null) {
            try {
                md2Digest = MessageDigest.getInstance("MD2");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return md2Digest;
    }

    public static byte[] getMd5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    public static byte[] getMd5(final InputStream stream) throws IOException {
        try {
            MessageDigest md5DigestLocal = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int readed = -1;
            while ((readed = stream.read(buffer)) != -1) {
                md5DigestLocal.update(buffer, 0, readed);
            }
            return md5DigestLocal.digest();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageDigest getMd5Digest() {
        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md5Digest;
    }

    public static byte[] getSha1(final byte[] data) {
        return getSha1Digest().digest(data);
    }

    private static MessageDigest getSha1Digest() {
        if (sha1Digest == null) {
            try {
                sha1Digest = MessageDigest.getInstance("SHA-1");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return sha1Digest;
    }

    public static byte[] getSha256(final byte[] data) {
        return getSha256Digest().digest(data);
    }

    private static MessageDigest getSha256Digest() {
        if (sha256Digest == null) {
            try {
                sha256Digest = MessageDigest.getInstance("SHA-256");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return sha256Digest;
    }

    public static byte[] getSha512(final byte[] data) {
        return getSha512Digest().digest(data);
    }

    private static MessageDigest getSha512Digest() {
        if (sha512Digest == null) {
            try {
                sha512Digest = MessageDigest.getInstance("SHA-512");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return sha512Digest;
    }

    public static byte[] intToByte(final int an_number) {
        return numberToByte(an_number, 4);
    }

    public static byte[] longToByte(final long an_number) {
        return numberToByte(an_number, 8);
    }

    private static byte[] numberToByte(final long an_number, final int an_bytes) {
        final byte[] lh_return = new byte[an_bytes];
        for (int ln = 0, ln_1 = an_bytes - 1; ln < an_bytes; ln++, ln_1--) {
            lh_return[ln] = (byte) (an_number >> ln_1 * 8);
        }
        return lh_return;
    }

    public static byte[] shortToByte(final short an_number) {
        return numberToByte(an_number, 2);
    }
}
