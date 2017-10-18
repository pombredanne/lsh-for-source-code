package it.xargon.util;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * I metodi contenuti in questa classe sono utilizzati principalmente nel package XMP per
 * consentire una traduzione consistente di elementi Java di uso comune in flussi di byte
 * portabili. Ciascun metodo documenta il modo e la sequenza in cui i dati vengono serializzati e 
 * deserializzati.
 * Non � stato utilizzato il supporto alla serializzazione/deserializzazione di Java per
 * consentire lo sviluppo di gestori di protocollo XMP in linguaggi diversi da Java.
 * @author Francesco Muccilli
 *
 */
public class Bitwise {

    public static final byte[] BZERO = new byte[0];

    private static final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final char intToBase64[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    private static final char intToAltBase64[] = { '!', '"', '#', '$', '%', '&', '\'', '(', ')', ',', '-', '.', ':', ';', '<', '>', '@', '[', ']', '^', '`', '_', '{', '|', '}', '~', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '?' };

    private static final byte base64ToInt[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };

    private static final byte altBase64ToInt[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, 62, 9, 10, 11, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 12, 13, 14, -1, 15, 63, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, 18, 19, 21, 20, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 22, 23, 24, 25 };

    private Bitwise() {
    }

    public static byte[] floatToByteArray(float value) {
        return floatToByteArray(value, null);
    }

    public static byte[] floatToByteArray(float value, byte[] res) {
        return intToByteArray(Float.floatToIntBits(value), res);
    }

    public static float byteArrayToFloat(byte[] vl) {
        return Float.intBitsToFloat(byteArrayToInt(vl));
    }

    public static float sequenceToFloat(byte a, byte b, byte c, byte d) {
        return Float.intBitsToFloat(sequenceToInt(a, b, c, d));
    }

    public static float readFloat(InputStream is) throws IOException {
        byte[] cache = new byte[4];
        Tools.forceRead(is, cache, false);
        return byteArrayToFloat(cache);
    }

    public static byte[] doubleToByteArray(double value) {
        return doubleToByteArray(value, null);
    }

    public static byte[] doubleToByteArray(double value, byte[] res) {
        return longToByteArray(Double.doubleToLongBits(value), res);
    }

    public static double byteArrayToDouble(byte[] vl) {
        return Double.longBitsToDouble(byteArrayToLong(vl));
    }

    public static double sequenceToDouble(byte a, byte b, byte c, byte d, byte e, byte f, byte g, byte h) {
        return Double.longBitsToDouble(sequenceToLong(a, b, c, d, e, f, g, h));
    }

    public static double readDouble(InputStream is) throws IOException {
        byte[] cache = new byte[8];
        Tools.forceRead(is, cache, false);
        return byteArrayToDouble(cache);
    }

    public static byte[] shortToByteArray(short value) {
        return shortToByteArray(value, null);
    }

    public static byte[] shortToByteArray(short value, byte[] res) {
        byte[] vl = null;
        if ((res == null) || (res.length != 2)) vl = new byte[2]; else vl = res;
        vl[0] = (byte) (0xff & (value >> 8));
        vl[1] = (byte) (0xff & value);
        return vl;
    }

    public static short byteArrayToShort(byte[] vl) {
        if ((vl == null) || (vl.length != 2)) throw new IllegalArgumentException("Wrong source array length: expected 2 bytes");
        return sequenceToShort(vl[0], vl[1]);
    }

    public static short sequenceToShort(byte a, byte b) {
        return (short) ((a << 8) | (b & 0xff));
    }

    public static short readShort(InputStream is) throws IOException {
        byte[] cache = new byte[2];
        Tools.forceRead(is, cache, false);
        return byteArrayToShort(cache);
    }

    public static byte[] longToByteArray(long value) {
        return longToByteArray(value, null);
    }

    public static byte[] longToByteArray(long value, byte[] res) {
        byte[] vl = null;
        if ((res == null) || (res.length != 8)) vl = new byte[8]; else vl = res;
        vl[0] = (byte) (0xff & (value >> 56));
        vl[1] = (byte) (0xff & (value >> 48));
        vl[2] = (byte) (0xff & (value >> 40));
        vl[3] = (byte) (0xff & (value >> 32));
        vl[4] = (byte) (0xff & (value >> 24));
        vl[5] = (byte) (0xff & (value >> 16));
        vl[6] = (byte) (0xff & (value >> 8));
        vl[7] = (byte) (0xff & value);
        return vl;
    }

    public static long byteArrayToLong(byte[] vl) {
        if ((vl == null) || (vl.length != 8)) throw new IllegalArgumentException("Wrong source array length: expected 8 bytes");
        return sequenceToLong(vl[0], vl[1], vl[2], vl[3], vl[4], vl[5], vl[6], vl[7]);
    }

    public static long sequenceToLong(byte a, byte b, byte c, byte d, byte e, byte f, byte g, byte h) {
        return (((long) (a & 0xff) << 56) | ((long) (b & 0xff) << 48) | ((long) (c & 0xff) << 40) | ((long) (d & 0xff) << 32) | ((long) (e & 0xff) << 24) | ((long) (f & 0xff) << 16) | ((long) (g & 0xff) << 8) | ((long) (h & 0xff)));
    }

    public static long readLong(InputStream is) throws IOException {
        byte[] cache = new byte[8];
        Tools.forceRead(is, cache, false);
        return byteArrayToLong(cache);
    }

    public static byte[] intToByteArray(int value) {
        return intToByteArray(value, null);
    }

    public static byte[] intToByteArray(int value, byte[] res) {
        byte[] vl = null;
        if ((res == null) || (res.length != 4)) vl = new byte[4]; else vl = res;
        vl[0] = (byte) (0xff & (value >> 24));
        vl[1] = (byte) (0xff & (value >> 16));
        vl[2] = (byte) (0xff & (value >> 8));
        vl[3] = (byte) (0xff & value);
        return vl;
    }

    public static int byteArrayToInt(byte[] vl) {
        if ((vl == null) || (vl.length != 4)) throw new IllegalArgumentException("Wrong source array length: expected 4 bytes");
        return sequenceToInt(vl[0], vl[1], vl[2], vl[3]);
    }

    public static int sequenceToInt(byte a, byte b, byte c, byte d) {
        return (((int) (a & 0xff) << 24) | ((int) (b & 0xff) << 16) | ((int) (c & 0xff) << 8) | ((int) (d & 0xff)));
    }

    public static int readInt(InputStream is) throws IOException {
        byte[] cache = new byte[4];
        Tools.forceRead(is, cache, false);
        return byteArrayToInt(cache);
    }

    public static byte[] charToByteArray(char value) {
        return charToByteArray(value, null);
    }

    public static byte[] charToByteArray(char value, byte[] res) {
        byte[] vl = null;
        if ((res == null) || (res.length != 2)) vl = new byte[2]; else vl = res;
        vl[0] = (byte) (0xff & (value >> 8));
        vl[1] = (byte) (0xff & value);
        return vl;
    }

    public static char byteArrayToChar(byte[] vl) {
        if ((vl == null) || (vl.length != 2)) throw new IllegalArgumentException("Wrong source array length: expected 2 bytes");
        return sequenceToChar(vl[0], vl[1]);
    }

    public static char sequenceToChar(byte a, byte b) {
        return (char) ((a << 8) | (b & 0xff));
    }

    public static float readChar(InputStream is) throws IOException {
        byte[] cache = new byte[2];
        Tools.forceRead(is, cache, false);
        return byteArrayToChar(cache);
    }

    public static byte[] asciiStringToByteArray(String text, byte[] res) {
        byte[] vl = null;
        if (text == null) return BZERO;
        if ((res == null) || (res.length != text.length())) vl = new byte[text.length()]; else vl = res;
        for (int cnt = 0; cnt < text.length(); cnt++) vl[cnt] = asByte(text.charAt(cnt));
        return vl;
    }

    public static byte[] asciiStringToByteArray(String text) {
        return asciiStringToByteArray(text, null);
    }

    public static String byteArrayToAsciiString(byte[] vl) {
        if (vl == null) return "";
        char[] cont = new char[vl.length];
        for (int cnt = 0; cnt < cont.length; cnt++) cont[cnt] = asChar(vl[cnt]);
        return new String(cont);
    }

    public static String readAsciiString(int chunksize, InputStream is) throws IOException {
        byte[] cache = new byte[chunksize];
        Tools.forceRead(is, cache, false);
        return byteArrayToAsciiString(cache);
    }

    public static byte[] stringToByteArray(String text, byte[] res) {
        byte[] vl = null;
        if (text == null) return BZERO;
        if ((res == null) || (res.length != (text.length() * 2))) vl = new byte[text.length() * 2]; else vl = res;
        for (int cnt = 0; cnt < text.length(); cnt++) {
            char ch = text.charAt(cnt);
            vl[cnt * 2] = (byte) (0xff & (ch >> 8));
            vl[(cnt * 2) + 1] = (byte) (0xff & ch);
        }
        return vl;
    }

    public static byte[] stringToByteArray(String text) {
        return stringToByteArray(text, null);
    }

    public static String byteArrayToString(byte[] vl) {
        if (vl == null) return "";
        if (vl.length % 2 != 0) throw new IllegalArgumentException("Wrong source array length: expected an even number of bytes");
        char[] cont = new char[vl.length / 2];
        for (int cnt = 0; cnt < cont.length; cnt++) cont[cnt] = sequenceToChar(vl[cnt * 2], vl[(cnt * 2) + 1]);
        return new String(cont);
    }

    public static String readString(int chunksize, InputStream is) throws IOException {
        byte[] cache = new byte[chunksize];
        Tools.forceRead(is, cache, false);
        return byteArrayToString(cache);
    }

    public static String readAutoString(InputStream is) throws IOException {
        int chunksize = readInt(is);
        return readString(chunksize, is);
    }

    public static String byteArrayToHex(byte... buffer) {
        StringBuilder result = new StringBuilder();
        for (byte b : buffer) {
            result.append(hexdigits[(b >> 4) & 0x0F]);
            result.append(hexdigits[b & 0x0F]);
        }
        return result.toString();
    }

    public static byte[] MD5(String input) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md5.update(input.getBytes());
        return md5.digest();
    }

    public static byte[] MD5(byte[] input) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md5.update(input);
        return md5.digest();
    }

    public static String urlEncode(String text) {
        return urlEncode(text, null);
    }

    public static String urlEncode(String text, String enc) {
        if (text == null) return null;
        String ienc = enc == null ? "ISO8859_1" : enc;
        try {
            return URLEncoder.encode(text, ienc);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String urlDecode(String text) {
        return urlEncode(text, null);
    }

    public static String urlDecode(String text, String enc) {
        if (text == null) return null;
        String ienc = enc == null ? "ISO8859_1" : enc;
        try {
            return URLDecoder.decode(text, ienc);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String byteArrayToBase64(byte[] a) {
        return byteArrayToBase64(a, false);
    }

    public static String byteArrayToAltBase64(byte[] a) {
        return byteArrayToBase64(a, true);
    }

    private static String byteArrayToBase64(byte[] a, boolean alternate) {
        int aLen = a.length;
        int numFullGroups = aLen / 3;
        int numBytesInPartialGroup = aLen - 3 * numFullGroups;
        int resultLen = 4 * ((aLen + 2) / 3);
        StringBuffer result = new StringBuffer(resultLen);
        char[] intToAlpha = (alternate ? intToAltBase64 : intToBase64);
        int inCursor = 0;
        for (int i = 0; i < numFullGroups; i++) {
            int byte0 = a[inCursor++] & 0xff;
            int byte1 = a[inCursor++] & 0xff;
            int byte2 = a[inCursor++] & 0xff;
            result.append(intToAlpha[byte0 >> 2]);
            result.append(intToAlpha[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
            result.append(intToAlpha[(byte1 << 2) & 0x3f | (byte2 >> 6)]);
            result.append(intToAlpha[byte2 & 0x3f]);
        }
        if (numBytesInPartialGroup != 0) {
            int byte0 = a[inCursor++] & 0xff;
            result.append(intToAlpha[byte0 >> 2]);
            if (numBytesInPartialGroup == 1) {
                result.append(intToAlpha[(byte0 << 4) & 0x3f]);
                result.append("==");
            } else {
                int byte1 = a[inCursor++] & 0xff;
                result.append(intToAlpha[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
                result.append(intToAlpha[(byte1 << 2) & 0x3f]);
                result.append('=');
            }
        }
        return result.toString();
    }

    public static byte[] base64ToByteArray(String s) {
        return base64ToByteArray(s, false);
    }

    public static int base64toInt(char c, byte[] alphaToInt) {
        int result = alphaToInt[c];
        if (result < 0) throw new IllegalArgumentException("Illegal character " + c);
        return result;
    }

    public static byte[] altBase64ToByteArray(String s) {
        return base64ToByteArray(s, true);
    }

    private static byte[] base64ToByteArray(String s, boolean alternate) {
        byte[] alphaToInt = (alternate ? altBase64ToInt : base64ToInt);
        int sLen = s.length();
        int numGroups = sLen / 4;
        if (4 * numGroups != sLen) throw new IllegalArgumentException("Wrong string length: expected an multiple of 4.");
        int missingBytesInLastGroup = 0;
        int numFullGroups = numGroups;
        if (sLen != 0) {
            if (s.charAt(sLen - 1) == '=') {
                missingBytesInLastGroup++;
                numFullGroups--;
            }
            if (s.charAt(sLen - 2) == '=') missingBytesInLastGroup++;
        }
        byte[] result = new byte[3 * numGroups - missingBytesInLastGroup];
        int inCursor = 0, outCursor = 0;
        for (int i = 0; i < numFullGroups; i++) {
            int ch0 = base64toInt(s.charAt(inCursor++), alphaToInt);
            int ch1 = base64toInt(s.charAt(inCursor++), alphaToInt);
            int ch2 = base64toInt(s.charAt(inCursor++), alphaToInt);
            int ch3 = base64toInt(s.charAt(inCursor++), alphaToInt);
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));
            result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            result[outCursor++] = (byte) ((ch2 << 6) | ch3);
        }
        if (missingBytesInLastGroup != 0) {
            int ch0 = base64toInt(s.charAt(inCursor++), alphaToInt);
            int ch1 = base64toInt(s.charAt(inCursor++), alphaToInt);
            result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));
            if (missingBytesInLastGroup == 1) {
                int ch2 = base64toInt(s.charAt(inCursor++), alphaToInt);
                result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
            }
        }
        return result;
    }

    public static byte[] serializeObject(Object obj) {
        if (obj == null) return BZERO;
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Not serializable object");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            bos.flush();
            bos.close();
        } catch (IOException ex) {
            throw new IllegalStateException("Serialization failed", ex);
        }
        return bos.toByteArray();
    }

    public static Object deserializeObject(byte[] vl) {
        Object obj = null;
        if ((vl == null) || (vl.length == 0)) return null;
        ByteArrayInputStream bis = new ByteArrayInputStream(vl);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException ignored) {
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Buffer contains an unknown class", ex);
        }
        return obj;
    }

    public static <T> T deserializeObject(Class<T> clazz, byte[] vl) throws ClassNotFoundException {
        return clazz.cast(deserializeObject(vl));
    }

    public static byte asByte(char value) {
        return (byte) (value & 0x00FF);
    }

    public static byte asByte(short value) {
        return (byte) (value & 0x00FF);
    }

    public static byte asByte(int value) {
        return (byte) (value & 0x00FF);
    }

    public static byte asByte(long value) {
        return (byte) (value & 0x00FF);
    }

    public static char asChar(byte value) {
        return (char) (value & 0x00FF);
    }

    public static char asChar(short value) {
        return (char) (value & 0x00FFFF);
    }

    public static char asChar(int value) {
        return (char) (value & 0x00FFFF);
    }

    public static char asChar(long value) {
        return (char) (value & 0x00FFFF);
    }

    public static short asShort(byte value) {
        return (short) (value & 0x00FF);
    }

    public static short asShort(char value) {
        return (short) (value & 0x00FFFF);
    }

    public static short asShort(int value) {
        return (short) (value & 0x00FFFF);
    }

    public static short asShort(long value) {
        return (short) (value & 0x00FFFF);
    }

    public static int asInt(byte value) {
        return (int) (value & 0x00FF);
    }

    public static int asInt(char value) {
        return (int) (value & 0x00FFFF);
    }

    public static int asInt(short value) {
        return (int) (value & 0x00FFFF);
    }

    public static int asInt(long value) {
        return (int) (value & 0x00FFFFFFFF);
    }

    public static long asLong(byte value) {
        return (long) (value & 0x00FF);
    }

    public static long asLong(char value) {
        return (long) (value & 0x00FFFF);
    }

    public static long asLong(short value) {
        return (long) (value & 0x00FFFF);
    }

    public static long asLong(int value) {
        return (long) (value & 0x00FFFFFFFF);
    }
}
