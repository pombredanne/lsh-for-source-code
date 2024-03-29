package org.dspace.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.rmi.dgc.VMID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for DSpace.
 * 
 * @author Peter Breton
 * @version $Revision: 1696 $
 */
public class Utils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");

    private static final long MS_IN_SECOND = 1000L;

    private static final long MS_IN_MINUTE = 60000L;

    private static final long MS_IN_HOUR = 3600000L;

    private static final long MS_IN_DAY = 86400000L;

    private static final long MS_IN_WEEK = 604800000L;

    private static final long MS_IN_YEAR = 31536000000L;

    private static int counter = 0;

    private static Random random = new Random();

    private static VMID vmid = new VMID();

    /** Private Constructor */
    private Utils() {
    }

    /**
     * Return an MD5 checksum for data in hex format.
     * 
     * @param data
     *            The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(String data) {
        return getMD5(data.getBytes());
    }

    /**
     * Return an MD5 checksum for data in hex format.
     * 
     * @param data
     *            The data to checksum.
     * @return MD5 checksum for the data in hex format.
     */
    public static String getMD5(byte[] data) {
        return toHex(getMD5Bytes(data));
    }

    /**
     * Return an MD5 checksum for data as a byte array.
     * 
     * @param data
     *            The data to checksum.
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
     * Return a hex representation of the byte array
     * 
     * @param data
     *            The data to transform.
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
     * Generate a unique key. The key is a long (length 38 to 40) sequence of
     * digits.
     * 
     * @return A unique key as a long sequence of base-10 digits.
     */
    public static String generateKey() {
        return new BigInteger(generateBytesKey()).abs().toString();
    }

    /**
     * Generate a unique key. The key is a 32-character long sequence of hex
     * digits.
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
     * Copy stream-data from source to destination. This method does not buffer,
     * flush or close the streams, as to do so would require making non-portable
     * assumptions about the streams' origin and further use. If you wish to
     * perform a buffered copy, use {@link #bufferedCopy}.
     * 
     * @param input
     *            The InputStream to obtain data from.
     * @param output
     *            The OutputStream to copy data to.
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
     * Copy stream-data from source to destination, with buffering. This is
     * equivalent to passing {@link #copy}a
     * <code>java.io.BufferedInputStream</code> and
     * <code>java.io.BufferedOuputStream</code> to {@link #copy}, and
     * flushing the output stream afterwards. The streams are not closed after
     * the copy.
     * 
     * @param source
     *            The InputStream to obtain data from.
     * @param destination
     *            The OutputStream to copy data to.
     */
    public static void bufferedCopy(final InputStream source, final OutputStream destination) throws IOException {
        final BufferedInputStream input = new BufferedInputStream(source);
        final BufferedOutputStream output = new BufferedOutputStream(destination);
        copy(input, output);
        output.flush();
    }

    /**
     * Replace characters that could be interpreted as HTML codes with symbolic
     * references (entities). This function should be called before displaying
     * any metadata fields that could contain the characters " <", ">", "&",
     * "'", and double quotation marks. This will effectively disable HTML links
     * in metadata.
     * 
     * @param value
     *            the metadata value to be scrubbed for display
     * 
     * @return the passed-in string, with html special characters replaced with
     *         entities.
     */
    public static String addEntities(String value) {
        if (value == null || value.length() == 0) return value;
        value = value.replaceAll("&", "&amp;");
        value = value.replaceAll("\"", "&quot;");
        value = value.replaceAll("<", "&lt;");
        value = value.replaceAll(">", "&gt;");
        return value;
    }

    /**
     * Utility method to parse durations defined as \d+[smhdwy] (seconds,
     * minutes, hours, days, weeks, years)
     * 
     * @param duration
     *            specified duration
     * 
     * @return number of milliseconds equivalent to duration.
     * 
     * @throws ParseException
     *             if the duration is of incorrect format
     */
    public static long parseDuration(String duration) throws ParseException {
        Matcher m = DURATION_PATTERN.matcher(duration.trim());
        if (!m.matches()) {
            throw new ParseException("'" + duration + "' is not a valid duration definition", 0);
        }
        String units = m.group(2);
        long multiplier = MS_IN_SECOND;
        if ("s".equals(units)) {
            multiplier = MS_IN_SECOND;
        } else if ("m".equals(units)) {
            multiplier = MS_IN_MINUTE;
        } else if ("h".equals(units)) {
            multiplier = MS_IN_HOUR;
        } else if ("d".equals(units)) {
            multiplier = MS_IN_DAY;
        } else if ("w".equals(units)) {
            multiplier = MS_IN_WEEK;
        } else if ("y".equals(units)) {
            multiplier = MS_IN_YEAR;
        } else {
            throw new ParseException(units + " is not a valid time unit (must be 'y', " + "'w', 'd', 'h', 'm' or 's')", duration.indexOf(units));
        }
        long qint = Long.parseLong(m.group(1));
        return qint * multiplier;
    }
}
