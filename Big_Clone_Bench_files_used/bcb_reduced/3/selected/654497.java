package utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author Greg
 */
public class conv {

    private static final char[] HEXTAB = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String HexFromString(String ascii) {
        String st = "";
        for (int i = 0; i < ascii.length(); i++) {
            String tempo;
            int ch = (int) ascii.charAt(i);
            tempo = Integer.toHexString(ch);
            if (tempo.length() < 2) {
                tempo = "0" + tempo;
            }
            st += tempo;
        }
        return st;
    }

    public static String HexFromBytes(byte[] data, int ofs, int len) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.setLength(len << 1);
        int pos = 0;
        int c = ofs + len;
        while (ofs < c) {
            sbuf.setCharAt(pos++, HEXTAB[(data[ofs] >> 4) & 0x0f]);
            sbuf.setCharAt(pos++, HEXTAB[data[ofs++] & 0x0f]);
        }
        return sbuf.toString().toUpperCase();
    }

    public static byte[] fromHexString(String in) {
        byte[] bts = new byte[in.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(in.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    public static void ChangeEndian(byte[] array, int ofs) {
        byte b0 = array[ofs + 0];
        byte b1 = array[ofs + 1];
        array[ofs + 0] = array[ofs + 3];
        array[ofs + 1] = array[ofs + 2];
        array[ofs + 2] = b1;
        array[ofs + 3] = b0;
    }

    public static void BytesFromInt(int i, byte[] array, int ofs) {
        array[ofs + 0] = (byte) ((i >>> 24) & 0x000000FF);
        array[ofs + 1] = (byte) ((i >> 16) & 0x000000FF);
        array[ofs + 2] = (byte) ((i >> 8) & 0x000000FF);
        array[ofs + 3] = (byte) (i & 0x00FF);
    }

    public static long BytesToLong(byte[] arr, int ofs) {
        byte[] tmp = new byte[8];
        for (int i = 0; i < 8; i++) {
            tmp[i] = arr[ofs + i];
        }
        long accum = 0;
        int i = 0;
        for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return accum;
    }

    public static void arrayBytesCopy(byte[] src, byte[] dest, int ofs, int length) {
        for (int i = 0; i < length; i++) {
            dest[i] = src[ofs + i];
        }
    }

    public static void arrayBytesCopy(byte[] src, int ofs, int length, byte[] dest, int nbBytes) {
        for (int i = 0; i < nbBytes; i++) {
            if ((ofs + i) < src.length && i < dest.length && i < length) dest[i] = src[ofs + i]; else if (i < dest.length) dest[i] = (byte) 0x00;
        }
    }

    public static byte[] WordFromInteger(int i, boolean toLittleEndian) {
        byte[] Bint = new byte[4];
        BytesFromInt(i, Bint, 0);
        byte[] toReturn = new byte[2];
        if (toLittleEndian) {
            ChangeEndian(Bint, 0);
            arrayBytesCopy(Bint, toReturn, 0, 2);
        } else arrayBytesCopy(Bint, toReturn, 2, 2);
        return toReturn;
    }

    public static String longToHexStr(Long l, boolean toLittleEndian) throws IOException {
        java.io.ByteArrayOutputStream doubleByteArray = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream Data = new java.io.DataOutputStream(doubleByteArray);
        Data.writeLong(l);
        Data.flush();
        byte[] temp = doubleByteArray.toByteArray();
        Data.close();
        doubleByteArray.close();
        Data = null;
        doubleByteArray = null;
        if (toLittleEndian) {
            for (int i = 0, j = 7; i < 4; i++, j--) {
                byte tempo = temp[j];
                temp[j] = temp[i];
                temp[i] = tempo;
            }
        }
        return HexFromBytes(temp, 0, 8);
    }

    public static String HexByteFromInteger(int i) {
        String hx = Integer.toHexString(i);
        if (hx.length() < 2) hx = "0" + hx; else hx = hx.substring(0, 2);
        return hx;
    }

    public static String FloatToHexStr(float f, boolean toLittleEndian) {
        String toReturn = "";
        byte[] temp = new byte[4];
        BytesFromInt(Float.floatToRawIntBits(f), temp, 0);
        if (toLittleEndian) ChangeEndian(temp, 0);
        toReturn = HexFromBytes(temp, 0, 4);
        return toReturn;
    }

    public static int BytestoInt(byte[] data, int ofs) {
        byte[] temp = { data[ofs + 0], data[ofs + 1], data[ofs + 2], data[ofs + 3] };
        int bits = (temp[0] << 24) | ((temp[1] & 0x0ff) << 16) | ((temp[2] & 0x0ff) << 8) | (temp[3] & 0x0ff);
        return bits;
    }

    public static String md5(String key) {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else hashString.append(hex.substring(hex.length() - 2));
        }
        return hashString.toString();
    }

    public static long checkSum(byte[] data) {
        Checksum checksumEngine = new CRC32();
        checksumEngine.update(data, 0, data.length);
        return checksumEngine.getValue();
    }
}
