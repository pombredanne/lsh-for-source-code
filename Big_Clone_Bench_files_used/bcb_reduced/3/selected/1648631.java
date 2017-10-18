package org.infoeng.icws.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.rmi.dgc.VMID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import sun.misc.HexDumpEncoder;

/**
 * Utility functions for ICWS (transplanted from DSpace).
 *
 * @author  Peter Breton
 * @version $Revision: 92 $
 */
public class Utils {

    private static int counter = 0;

    private static Random random = new Random();

    private static VMID vmid = new VMID();

    /** Private Constructor */
    private Utils() {
    }

    /**
     * Return an MD5 checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(String data) {
        return getMD5(data.getBytes());
    }

    /**
     * Return an MD5 checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(byte[] data) {
        return toHex(getMD5Bytes(data));
    }

    /**
     * Return an MD5 checksum for data as a byte array.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data as a byte array.
     */
    public static byte[] getMD5Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return null;
    }

    /**
     * Return a SHA checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return SHA checksum for the data in hex format.
     */
    public static String getSHA(String data) {
        return getSHA(data.getBytes());
    }

    /**
     * Return a SHA checksum for data in hex format.
     *
     * @param data The data to checksum.
     * @return SHA checksum for the data in hex format.
     */
    public static String getSHA(byte[] data) {
        return toHex(getSHABytes(data));
    }

    /**
     * Return a SHA checksum for data as a byte array.
     *
     * @param data The data to checksum.
     * @return MD5 checksum for the data as a byte array.
     */
    public static byte[] getSHABytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return null;
    }

    /**
     * Return a hex representation of the byte array
     *
     * @param data The data to transform.
     * @return A hex representation of the data.
     */
    public static String toHex(byte[] data) {
        if ((data == null) || (data.length == 0)) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int low = (int) (data[i] & 0x0F);
            int high = (int) (data[i] & 0xF0);
            result.append(Integer.toHexString(high).substring(0, 1));
            result.append(Integer.toHexString(low));
        }
        return result.toString();
    }

    /**
     * Generate a unique key.
     * The key is a long (length 38 to 40) sequence of digits.
     *
     * @return A unique key as a long sequence of base-10 digits.
     */
    public static String generateKey() {
        return new BigInteger(generateBytesKey()).abs().toString();
    }

    /**
     * Generate a unique key.
     * The key is a 32-character long sequence of hex digits.
     *
     * @return A unique key as a long sequence of hex digits.
     */
    public static String generateHexKey() {
        return toHex(generateBytesKey());
    }

    /**
     * Generate a unique key as a byte array.
     *
     * @return A unique key as a byte array.
     */
    public static synchronized byte[] generateBytesKey() {
        byte[] junk = new byte[16];
        random.nextBytes(junk);
        String input = new StringBuffer().append(vmid).append(new java.util.Date()).append(junk).append(counter++).toString();
        return getMD5Bytes(input.getBytes());
    }

    /**
     * Copy stream-data from source to destination. This method does not
     * buffer, flush or close the streams, as to do so would require making
     * non-portable assumptions about the streams' origin and further use. If
     * you wish to perform a buffered copy, use {@link #bufferedCopy}.
     *
     * @param input The InputStream to obtain data from.
     * @param output The OutputStream to copy data to.
     */
    public static void copy(final InputStream input, final OutputStream output) throws IOException {
        final int BUFFER_SIZE = 1024 * 4;
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            final int count = input.read(buffer, 0, BUFFER_SIZE);
            if (-1 == count) {
                break;
            }
            output.write(buffer, 0, count);
        }
    }

    /**
     * Copy stream-data from source to destination, with buffering.
     * This is equivalent to passing {@link #copy} a
     * <code>java.io.BufferedInputStream</code> and
     * <code>java.io.BufferedOuputStream</code> to {@link #copy}, and flushing
     * the output stream afterwards. The streams are not closed after the copy.
     *
     * @param source The InputStream to obtain data from.
     * @param destination The OutputStream to copy data to.
     */
    public static void bufferedCopy(final InputStream source, final OutputStream destination) throws IOException {
        final BufferedInputStream input = new BufferedInputStream(source);
        final BufferedOutputStream output = new BufferedOutputStream(destination);
        copy(input, output);
        output.flush();
    }

    /**
    * Replace characters that could be interpreted as HTML codes with symbolic
    * references (entities). This function should be called before displaying any metadata
    * fields that could contain the characters "<", ">", "&", "'", and
    * double quotation marks.
    * This will effectively disable HTML links in metadata.
    *
    * @param value     the metadata value to be scrubbed for display
    *
    * @return      the passed-in string, with html special characters replaced
    * with entities.
    */
    public static String addEntities(String oldValue) {
        String value = oldValue.replaceAll("&", "&amp;");
        value = value.replaceAll("\"", "&quot;");
        value = value.replaceAll("'", "&apos;");
        value = value.replaceAll("<", "&lt;");
        value = value.replaceAll(">", "&gt;");
        return value;
    }

    public static String convertEntities(String oldValue) {
        String value = oldValue.replaceAll("&amp;", "&");
        value = value.replaceAll("&quot;", "\"");
        value = value.replaceAll("&apos;", "'");
        value = value.replaceAll("&lt;", "<");
        value = value.replaceAll("&gt;", ">");
        return value;
    }

    /**
    * Method prettyPrintHex
    *
    * @param baToConvert
    * @return hexdump string
    */
    public static String prettyPrintHex(byte[] baToConvert) {
        HexDumpEncoder hde = new HexDumpEncoder();
        return hde.encodeBuffer(baToConvert);
    }

    /**
    * Method prettyPrintHex
    *
    * @param sToConvert
    * @return hexdump string
    */
    public static String prettyPrintHex(String sToConvert) {
        return prettyPrintHex(sToConvert.getBytes());
    }

    /** Field HEX_DIGITS */
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Field BIT_DIGIT */
    private static char[] BIT_DIGIT = { '0', '1' };

    /** Field COMPARE_BITS */
    private static final byte[] COMPARE_BITS = { (byte) 0x80, (byte) 0x40, (byte) 0x20, (byte) 0x10, (byte) 0x08, (byte) 0x04, (byte) 0x02, (byte) 0x01 };

    /** Field BYTE_SEPARATOR */
    private static char BYTE_SEPARATOR = ' ';

    /** Field WITH_BYTE_SEPARATOR */
    private static boolean WITH_BYTE_SEPARATOR = false;

    /**
    *  Sets the WithByteSeparator attribute of the Convert class
    *
    * @param  bs  The new WithByteSeparator value
    */
    public static void setWithByteSeparator(boolean bs) {
        WITH_BYTE_SEPARATOR = bs;
    }

    /**
    *  Sets the ByteSeparator attribute of the Convert class
    *
    * @param  bs  The new ByteSeparator value
    */
    public static void setByteSeparator(char bs) {
        BYTE_SEPARATOR = bs;
    }

    /**
    *  Sets the BitDigits attribute of the Convert class
    *
    * @param  bd             The new BitDigits value
    * @exception  Exception  Description of Exception
    */
    public static void setBitDigits(char[] bd) throws Exception {
        if (bd.length != 2) {
            throw new Exception("wrong number of characters!");
        }
        BIT_DIGIT = bd;
    }

    /**
    * Method setBitDigits
    *
    * @param zeroBit
    * @param oneBit
    */
    public static void setBitDigits(char zeroBit, char oneBit) {
        BIT_DIGIT[0] = zeroBit;
        BIT_DIGIT[1] = oneBit;
    }

    /**
    *  Description of the Method
    *
    * @param  block  Description of Parameter
    * @return        Description of the Returned Value
    */
    public static String byteArrayToBinaryString(byte[] block) {
        StringBuffer strBuf = new StringBuffer();
        int iLen = block.length;
        for (int i = 0; i < iLen; i++) {
            byte2bin(block[i], strBuf);
            if ((i < iLen - 1) & WITH_BYTE_SEPARATOR) {
                strBuf.append(BYTE_SEPARATOR);
            }
        }
        return strBuf.toString();
    }

    /**
    * Method toBinaryString
    *
    * @param ba
    * @return
    */
    public static String toBinaryString(byte[] ba) {
        return byteArrayToBinaryString(ba);
    }

    /**
    * Method toBinaryString
    *
    * @param b
    * @return
    */
    public static String toBinaryString(byte b) {
        byte[] ba = new byte[1];
        ba[0] = b;
        return byteArrayToBinaryString(ba);
    }

    /**
    * Method toBinaryString
    *
    * @param s
    * @return
    */
    public static String toBinaryString(short s) {
        return toBinaryString(toByteArray(s));
    }

    /**
    * Method toBinaryString
    *
    * @param i
    * @return
    */
    public static String toBinaryString(int i) {
        return toBinaryString(toByteArray(i));
    }

    /**
    * Method toBinaryString
    *
    * @param l
    * @return
    */
    public static String toBinaryString(long l) {
        return toBinaryString(toByteArray(l));
    }

    /**
    * Method toByteArray
    *
    * @param s
    * @return
    */
    public static final byte[] toByteArray(short s) {
        byte[] baTemp = new byte[2];
        baTemp[1] = (byte) (s);
        baTemp[0] = (byte) (s >> 8);
        return baTemp;
    }

    /**
    * Method toByteArray
    *
    * @param i
    * @return
    */
    public static final byte[] toByteArray(int i) {
        byte[] baTemp = new byte[4];
        baTemp[3] = (byte) i;
        baTemp[2] = (byte) (i >> 8);
        baTemp[1] = (byte) (i >> 16);
        baTemp[0] = (byte) (i >> 24);
        return baTemp;
    }

    /**
    * Method toByteArray
    *
    * @param l
    * @return
    */
    public static final byte[] toByteArray(long l) {
        byte[] baTemp = new byte[8];
        baTemp[7] = (byte) l;
        baTemp[6] = (byte) (l >> 8);
        baTemp[5] = (byte) (l >> 16);
        baTemp[4] = (byte) (l >> 24);
        baTemp[3] = (byte) (l >> 32);
        baTemp[2] = (byte) (l >> 40);
        baTemp[1] = (byte) (l >> 48);
        baTemp[0] = (byte) (l >> 56);
        return baTemp;
    }

    /**
    *  Description of the Method
    *
    * @param  block  Description of Parameter
    * @return        Description of the Returned Value
    */
    public static String byteArrayToHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if ((i < len - 1) & WITH_BYTE_SEPARATOR) {
                buf.append(BYTE_SEPARATOR);
            }
        }
        return buf.toString();
    }

    /**
    *  Description of the Method
    *
    * @param  in  string to be converted
    * @return     String in readable hex encoding
    */
    public static String stringToHexString(String in) {
        byte[] ba = in.getBytes();
        return toHexString(ba);
    }

    /**
    *  Description of the Method
    *
    * @param  block   Description of Parameter
    * @param  offset  Description of Parameter
    * @param  length  Description of Parameter
    * @return         Description of the Returned Value
    */
    public static String byteArrayToHexString(byte[] block, int offset, int length) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        length = length + offset;
        if ((len < length)) {
            length = len;
        }
        for (int i = 0 + offset; i < length; i++) {
            byte2hex(block[i], buf);
            if (i < length - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    /**
    *  Returns a string of hexadecimal digits from a byte array. Each byte is
    *  converted to 2 hex symbols.
    *
    * @param  ba  Description of Parameter
    * @return     Description of the Returned Value
    */
    public static String toHexString(byte[] ba) {
        return toHexString(ba, 0, ba.length);
    }

    /**
    * Method toHexString
    *
    * @param b
    * @return
    */
    public static String toHexString(byte b) {
        byte[] ba = new byte[1];
        ba[0] = b;
        return toHexString(ba, 0, ba.length);
    }

    /**
    *  Description of the Method
    *
    * @param s
    * @return               Description of the Returned Value
    */
    public static String toHexString(short s) {
        return toHexString(toByteArray(s));
    }

    /**
    * Method toHexString
    *
    * @param i
    * @return
    */
    public static String toHexString(int i) {
        return toHexString(toByteArray(i));
    }

    /**
    * Method toHexString
    *
    * @param l
    * @return
    */
    public static String toHexString(long l) {
        return toHexString(toByteArray(l));
    }

    /**
    * Method toString
    *
    * @param ba
    * @return
    */
    public static String toString(byte[] ba) {
        return new String(ba).toString();
    }

    /**
    * Method toString
    *
    * @param b
    * @return
    */
    public static String toString(byte b) {
        byte[] ba = new byte[1];
        ba[0] = b;
        return new String(ba).toString();
    }

    /**
    *  converts String to Hex String. Example: niko ->6E696B6F
    *
    * @param  ba      Description of Parameter
    * @param  offset  Description of Parameter
    * @param  length  Description of Parameter
    * @return         Description of the Returned Value
    */
    public static String toHexString(byte[] ba, int offset, int length) {
        char[] buf;
        if (WITH_BYTE_SEPARATOR) {
            buf = new char[length * 3];
        } else {
            buf = new char[length * 2];
        }
        for (int i = offset, j = 0, k; i < offset + length; ) {
            k = ba[i++];
            buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
            buf[j++] = HEX_DIGITS[k & 0x0F];
            if (WITH_BYTE_SEPARATOR) {
                buf[j++] = BYTE_SEPARATOR;
            }
        }
        return new String(buf);
    }

    /**
    * Converts readable hex-String to byteArray
    *
    * @param strA
    * @return
    */
    public static byte[] hexStringToByteArray(String strA) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte sum = (byte) 0x00;
        boolean nextCharIsUpper = true;
        for (int i = 0; i < strA.length(); i++) {
            char c = strA.charAt(i);
            switch(Character.toUpperCase(c)) {
                case '0':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x00;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x00;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '1':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x10;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x01;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '2':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x20;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x02;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '3':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x30;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x03;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '4':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x40;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x04;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '5':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x50;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x05;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '6':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x60;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x06;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '7':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x70;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x07;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '8':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x80;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x08;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case '9':
                    if (nextCharIsUpper) {
                        sum = (byte) 0x90;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x09;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'A':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xA0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0A;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'B':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xB0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0B;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'C':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xC0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0C;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'D':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xD0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0D;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'E':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xE0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0E;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
                case 'F':
                    if (nextCharIsUpper) {
                        sum = (byte) 0xF0;
                        nextCharIsUpper = false;
                    } else {
                        sum |= (byte) 0x0F;
                        result.write(sum);
                        nextCharIsUpper = true;
                    }
                    break;
            }
        }
        if (!nextCharIsUpper) {
            throw new RuntimeException("The String did not contain an equal number of hex digits");
        }
        return result.toByteArray();
    }

    /**
    *  Description of the Method
    *
    * @param  b    Description of Parameter
    * @param  buf  Description of Parameter
    */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /**
    *  Description of the Method
    *
    * @param  b    Description of Parameter
    * @param  buf  Description of Parameter
    */
    private static void byte2bin(byte b, StringBuffer buf) {
        for (int i = 0; i < 8; i++) {
            if ((b & COMPARE_BITS[i]) != 0) {
                buf.append(BIT_DIGIT[1]);
            } else {
                buf.append(BIT_DIGIT[0]);
            }
        }
    }

    /**
    *  Returns a string of 8 hexadecimal digits (most significant digit first)
    *  corresponding to the integer <i>n</i> , which is treated as unsigned.
    *
    * @param  n  Description of Parameter
    * @return    Description of the Returned Value
    */
    private static String intToHexString(int n) {
        char[] buf = new char[8];
        for (int i = 7; i >= 0; i--) {
            buf[i] = HEX_DIGITS[n & 0x0F];
            n >>>= 4;
        }
        return new String(buf);
    }
}
