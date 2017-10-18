package fi.hiit.cutehip.utils;

import java.math.BigInteger;
import fi.hiit.framework.crypto.SHA1Digest;

public class KeyMaterialGenerator {

    public static final int ENCRYPTION_GL_KEY_OUTGOING = 1;

    public static final int AUTH_GL_KEY_OUTGOING = 2;

    public static final int ENCRYPTION_LG_KEY_OUTGOING = 3;

    public static final int AUTH_LG_KEY_OUTGOING = 4;

    public static final int KEY_INDEX_LENGTH = 1;

    public static final int KEY_MATERIAL_LENGTH = 10 * SHA1Digest.HASH_SIZE;

    public static final int KEYS = 10;

    public KeyMaterialGenerator(byte[] kij, byte[] i, byte[] j, byte[] hitI, byte[] hitR) {
        byte[] lhs;
        byte[] rhs;
        int offset = 0;
        byte[] buff = new byte[kij.length + i.length + j.length + hitI.length + hitR.length + KEY_INDEX_LENGTH];
        if (__sort(hitI, hitR) < 0) {
            lhs = hitI;
            rhs = hitR;
        } else {
            lhs = hitR;
            rhs = hitI;
        }
        __kij = new byte[kij.length];
        System.arraycopy(kij, 0, __kij, 0, kij.length);
        System.arraycopy(kij, 0, buff, offset, kij.length);
        offset += kij.length;
        System.arraycopy(lhs, 0, buff, offset, lhs.length);
        offset += lhs.length;
        System.arraycopy(rhs, 0, buff, offset, rhs.length);
        offset += rhs.length;
        System.arraycopy(i, 0, buff, offset, i.length);
        offset += i.length;
        System.arraycopy(j, 0, buff, offset, j.length);
        offset += j.length;
        buff[offset] = 0x1;
        SHA1Digest md = new SHA1Digest();
        __key = md.digest(buff);
        for (int k = 0; k < KEYS; k++) {
            byte[] key = getKey((byte) (k + 1));
            System.arraycopy(key, 0, __keyMat, k * SHA1Digest.HASH_SIZE, SHA1Digest.HASH_SIZE);
        }
    }

    public byte[] drawKey(int offset, int length) {
        if (offset + length > __keyMat.length) return null;
        byte[] key = new byte[length];
        System.arraycopy(__keyMat, offset, key, 0, length);
        return key;
    }

    private byte[] getKey(byte index) {
        if (index <= 1) return __key;
        byte[] buff = new byte[__kij.length + SHA1Digest.HASH_SIZE + KEY_INDEX_LENGTH];
        byte[] hash = null;
        int i = 2;
        SHA1Digest md = new SHA1Digest();
        hash = __key;
        while (i <= index) {
            System.arraycopy(__kij, 0, buff, 0, __kij.length);
            System.arraycopy(hash, 0, buff, __kij.length, SHA1Digest.HASH_SIZE);
            buff[buff.length - 1] = (byte) i;
            hash = md.digest(buff);
            i++;
        }
        return hash;
    }

    private int __sort(byte[] hitI, byte[] hitR) {
        BigInteger lhs = new BigInteger(hitI);
        BigInteger rhs = new BigInteger(hitR);
        return lhs.compareTo(rhs);
    }

    public byte[] __kij;

    public byte[] __key;

    public byte[] __keyMat = new byte[KEY_MATERIAL_LENGTH];
}
