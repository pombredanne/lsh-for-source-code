package org.osmius.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.zip.Deflater;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * This and others classes of the web console are based on <a href="http://www.appfuse.org">Appfuse</a> (v1.9.4),
 * a tool from <a href="http://raibledesigns.com">Matt Raible</a> to facilitate the web application development.
 * Thanks for his great effort.
 * <p/>
 * String Utility Class This is used to encode passwords programmatically
 */
public class StringUtil {

    private static final Log log = LogFactory.getLog(StringUtil.class);

    public static final int START = 1;

    public static final int END = 2;

    public static final int ANYWHERE = 3;

    /**
    * Encode a string using algorithm specified in web.xml and return the
    * resulting encrypted password. If exception, the plain credentials
    * string is returned
    *
    * @param password  Password or other credentials to use in authenticating
    *                  this username
    * @param algorithm Algorithm used to do the digest
    * @return encypted password based on the algorithm.
    */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
    * Encode a string using Base64 encoding. Used when storing passwords
    * as cookies.
    * <p/>
    * This is weak encoding in that anyone can use the decodeString
    * routine to reverse the encoding.
    *
    * @param str
    * @return String
    */
    public static String encodeString(String str) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        return encoder.encodeBuffer(str.getBytes()).trim();
    }

    /**
    * Decode a string using Base64 encoding.
    *
    * @param str
    * @return String
    */
    public static String decodeString(String str) {
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        try {
            return new String(dec.decodeBuffer(str));
        } catch (IOException io) {
            throw new RuntimeException(io.getMessage(), io.getCause());
        }
    }

    /**
    * Appends '%' characters to a string
    *
    * @param str  String to add characters
    * @param mode START, END, ANYWARE
    * @return
    */
    public static String MatchMode(String str, int mode) {
        String matchedMode = "";
        if (mode == START) matchedMode = "%" + str; else if (mode == END) matchedMode = str + "%"; else if (mode == ANYWHERE) matchedMode = "%" + str + "%";
        return matchedMode;
    }

    public static String subString(String str, int begin, int end) {
        String data = "";
        if (str != null) {
            if (str.length() >= begin && str.length() >= end) {
                data = str.substring(begin, end);
            } else {
                if (begin < str.length()) {
                    if (str.length() < end) {
                        data = str.substring(begin, str.length());
                    }
                }
            }
        }
        return data;
    }

    public static String formatDateTime(DateTime prevDet, String milis) {
        return new StringBuilder().append(prevDet.getYear()).append("-").append(prevDet.getMonthOfYear() < 10 ? new StringBuilder().append("0").append(prevDet.getMonthOfYear()).toString() : prevDet.getMonthOfYear()).append("-").append(prevDet.getDayOfMonth() < 10 ? new StringBuilder().append("0").append(prevDet.getDayOfMonth()).toString() : prevDet.getDayOfMonth()).append(" ").append(prevDet.getHourOfDay() < 10 ? new StringBuilder().append("0").append(prevDet.getHourOfDay()).toString() : prevDet.getHourOfDay()).append(":").append(prevDet.getMinuteOfHour() < 10 ? new StringBuilder().append("0").append(prevDet.getMinuteOfHour()).toString() : prevDet.getMinuteOfHour()).append(":").append(prevDet.getSecondOfMinute() < 10 ? new StringBuilder().append("0").append(prevDet.getSecondOfMinute()).toString() : prevDet.getSecondOfMinute()).append(".").append(milis).append(",").toString();
    }

    public static String formatDateTime(DateTime prevDet) {
        return new StringBuilder().append(prevDet.getYear()).append("-").append(prevDet.getMonthOfYear() < 10 ? new StringBuilder().append("0").append(prevDet.getMonthOfYear()).toString() : prevDet.getMonthOfYear()).append("-").append(prevDet.getDayOfMonth() < 10 ? new StringBuilder().append("0").append(prevDet.getDayOfMonth()).toString() : prevDet.getDayOfMonth()).append(" ").append(prevDet.getHourOfDay() < 10 ? new StringBuilder().append("0").append(prevDet.getHourOfDay()).toString() : prevDet.getHourOfDay()).append(":").append(prevDet.getMinuteOfHour() < 10 ? new StringBuilder().append("0").append(prevDet.getMinuteOfHour()).toString() : prevDet.getMinuteOfHour()).append(":").append(prevDet.getSecondOfMinute() < 10 ? new StringBuilder().append("0").append(prevDet.getSecondOfMinute()).toString() : prevDet.getSecondOfMinute()).append(".").append(prevDet.getMillisOfSecond()).append(",").toString();
    }

    /**
    * compress Stromg.
    *
    * @param str is the string to compress.
    * @return a compressed byte array.
    * @throws java.io.IOException
    */
    public static byte[] compress(String str) throws IOException {
        byte[] bytesToCompress = str.getBytes();
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION);
        compressor.setInput(bytesToCompress);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytesToCompress.length);
        byte[] buf = new byte[bytesToCompress.length + 100];
        while (!compressor.finished()) {
            bos.write(buf, 0, compressor.deflate(buf));
        }
        bos.close();
        return bos.toByteArray();
    }

    /**
    * decompress String.
    *
    * @param compressedBytes is the compressed byte array.
    * @return decompressed Strring.
    * @throws java.io.IOException
    * @throws java.util.zip.DataFormatException
    *
    */
    public static String decompress(byte[] compressedBytes) throws IOException, DataFormatException {
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressedBytes);
        decompressor.finished();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedBytes.length);
        byte[] buf = new byte[compressedBytes.length + 100];
        while (!decompressor.finished()) {
            bos.write(buf, 0, decompressor.inflate(buf));
        }
        bos.close();
        return new String(bos.toByteArray());
    }
}
