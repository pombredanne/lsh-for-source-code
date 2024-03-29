package webmoney.cryptography.examination;

import org.junit.Test;
import java.util.Arrays;
import webmoney.cryptography.MD4;
import static org.junit.Assert.assertTrue;

public class MD4Test {

    private static final int HASH_SIZE = 16;

    static byte[] argument1 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };

    static byte[] etalon1 = { (byte) 0xFE, (byte) 0x86, (byte) 0xEB, (byte) 0xA3, 0x4E, (byte) 0xB9, (byte) 0xDA, 0x3B, 0x76, 0x55, 0x5B, 0x12, (byte) 0x88, 0x08, (byte) 0xAA, 0x36 };

    static final byte[] argument2 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38 };

    static final byte[] etalon2 = { 0x01, 0x15, 0x0F, (byte) 0xF5, 0x10, (byte) 0xD3, 0x59, (byte) 0x95, 0x2E, (byte) 0xA0, (byte) 0xF5, 0x17, 0x59, 0x21, (byte) 0xA7, 0x31 };

    static final byte[] argument3 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C };

    static byte[] etalon3 = new byte[] { (byte) 0x99, (byte) 0xA0, 0x4D, (byte) 0xD3, 0x6A, (byte) 0xB0, (byte) 0xC8, (byte) 0x85, (byte) 0xE8, 0x42, (byte) 0xF4, (byte) 0xE4, (byte) 0xB2, (byte) 0x88, 0x3A, 0x2D };

    static byte[] argument4 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40 };

    static byte[] etalon4 = new byte[] { 0x39, 0x34, (byte) 0xD5, 0x1C, 0x7F, (byte) 0xF4, (byte) 0x97, 0x0F, (byte) 0x9F, 0x0A, (byte) 0xC3, 0x14, (byte) 0xCE, (byte) 0xDE, (byte) 0xEB, (byte) 0xFE };

    static byte[] argument5 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46 };

    static byte[] etalon5 = new byte[] { 0x40, 0x71, (byte) 0x98, (byte) 0xCC, (byte) 0xE7, (byte) 0xE6, 0x70, (byte) 0xFF, (byte) 0xCC, 0x4A, (byte) 0x85, 0x7C, (byte) 0xAB, 0x76, (byte) 0xFA, (byte) 0xBA };

    @Test
    public void L56() {
        assertTrue(test(argument1, etalon1));
    }

    @Test
    public void E56() {
        assertTrue(test(argument2, etalon2));
    }

    @Test
    public void M56L64() {
        assertTrue(test(argument3, etalon3));
    }

    @Test
    public void E64() {
        assertTrue(test(argument4, etalon4));
    }

    @Test
    public void M64() {
        assertTrue(test(argument5, etalon5));
    }

    private boolean test(byte[] argument, byte[] etalon) {
        MD4 messageDigest = new MD4();
        messageDigest.engineReset();
        byte[] hash = messageDigest.digest(argument);
        return Arrays.equals(hash, etalon);
    }
}
