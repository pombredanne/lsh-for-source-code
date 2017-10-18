package net.codesmarts.log4j;

import java.security.*;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Methods for creating semiunique hash values based on logging incidents
 * @author Fred McCann
 */
public class HashUtility {

    private static Logger log = Logger.getLogger(HashUtility.class);

    public static final int LOCATION = 0;

    public static final int CONTENT = 1;

    public static final int THROWABLE = 2;

    /**
     * Create hash values based on incidents
     * @param eventList
     * @return
     */
    protected static String hash(List eventList, int method) {
        if (eventList == null || eventList.size() == 0) return null;
        StringBuffer buffer = new StringBuffer();
        Iterator i = eventList.iterator();
        while (i.hasNext()) {
            LoggingEvent event = (LoggingEvent) i.next();
            String[] throwd = event.getThrowableStrRep();
            switch(method) {
                case LOCATION:
                    LocationInfo info = event.getLocationInformation();
                    buffer.append(info.getClassName() + info.getMethodName() + event.getLevel().toString());
                    if (throwd != null && throwd.length > 1) for (int x = 1; x < throwd.length; x++) buffer.append(throwd[x]);
                    break;
                case CONTENT:
                    buffer.append(event.getMessage());
                    break;
                case THROWABLE:
                    if (throwd != null && throwd.length > 1) for (int x = 1; x < throwd.length; x++) buffer.append(throwd[x]); else buffer.append(event.getMessage());
                    break;
            }
        }
        return md5(buffer.toString());
    }

    /**
     * Returns the MD5 hash of a string
     *
     * @param plaintext The string to be encrypted
     * @return the hash of the input string
     */
    private static String md5(String plaintext) {
        return hash(plaintext, "MD5");
    }

    /**
     * Private function to turn md5 result to 32 hex-digit string
     *
     * @param hash byte array hash
     * @return the string representation of that hash
     */
    private static String byteArrayToString(byte hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        int i;
        for (i = 0; i < hash.length; i++) {
            if (((int) hash[i] & 0xff) < 0x10) buf.append("0");
            buf.append(Long.toString((int) hash[i] & 0xff, 16));
        }
        return buf.toString().toUpperCase();
    }

    /**
     * Converts a string into an array of bytes
     *
     * @param string The string to convert
     * @return a string of bytes that represents the string
     */
    private static byte[] stringToByteArray(String string) {
        return string.getBytes();
    }

    /**
     * Takes a string and returns its hash
     *
     * @param convert the input string, plaintext
     * @param algorithm the algorithm to use to create the hash
     * @return the string representation of the hash created by the input string
     */
    private static String hash(String convert, String algorithm) {
        byte[] barray = stringToByteArray(convert);
        String restring = "";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(barray);
            byte[] result = md.digest();
            restring = byteArrayToString(result);
        } catch (NoSuchAlgorithmException nsa) {
            log.error("Can't find algorithm for " + algorithm + " hash.");
        }
        return restring;
    }
}
