package au.edu.educationau.opensource.dsm.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Standalone URI Encoder class
 */
public class URIEncoder {

    /**
	 * A list of invalid characters that can't exist within filenames. If they
	 * appear then the DiskCache will escape them. The current list is in part
	 * based on Microsoft Knowledge Base article Q177506 (because DOS
	 * filesystems are more generally limited than UNIX filesystems).
	 * 
	 * SGP: Windows NT refuses to take "?", so I add it to the list.
	 * Additionally, if we encode "?", the jdk runtime logic decodes it twice
	 * for "file:" urls, giving a filename with a space in it. I have fixed it
	 * in JetspeedDiskCacheEntry.java, avoiding the creation of a new URL when
	 * getFile() is not null.
	 */
    public static final String[] INVALID_CHARACTERS = { "\\", "/", ":", "*", "\"", "<", ">", "|", "+", "?" };

    /***/
    public static final String[] CODED_CHARACTERS = { "#" + (int) '\\' + ";", "#" + (int) '/' + ";", "#" + (int) ':' + ";", "#" + (int) '*' + ";", "#" + (int) '"' + ";", "#" + (int) '<' + ";", "#" + (int) '>' + ";", "#" + (int) '|' + ";", "#" + (int) '+' + ";", "#" + (int) '?' + ";" };

    /**
	 * Encode the given URI
	 * 
	 * @param uri
	 */
    public static String encode(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI may not be null. ");
        }
        StringBuffer buffer = new StringBuffer(uri);
        StringUtils.replaceAll(buffer, "_", "__");
        StringUtils.replaceAll(buffer, "://", "_");
        StringUtils.replaceAll(buffer, "/", "_");
        StringUtils.replaceAll(buffer, ":", "___");
        encodeQueryData(buffer);
        return buffer.toString();
    }

    /**
	 * Hash the given URL
	 * 
	 * @param url
	 */
    public static String hashURL(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL may not be null. ");
        }
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            if (md != null) {
                md.reset();
                md.update(url.getBytes());
                BigInteger hash = new BigInteger(1, md.digest());
                result = hash.toString(16);
            }
            md = null;
        } catch (NoSuchAlgorithmException ex) {
            result = null;
        }
        return result;
    }

    /**
	 * Decode the given URI.
	 * 
	 * @param uri
	 */
    public static String decode(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI may not be null. ");
        }
        String newURI = "";
        int start = uri.indexOf("_");
        String protocol = null;
        if (uri.charAt(start + 1) == '_') {
            start = -1;
        }
        if (start > -1) {
            protocol = uri.substring(0, start);
        }
        newURI = uri.substring(start + 1, uri.length());
        StringBuffer buffer = new StringBuffer(newURI);
        StringUtils.replaceAll(buffer, "___", ":");
        StringUtils.replaceAll(buffer, "_", "/");
        StringUtils.replaceAll(buffer, "_", "/");
        StringUtils.replaceAll(buffer, "//", "_");
        if (protocol != null) {
            buffer.replace(0, 0, "://");
            buffer.replace(0, 0, protocol);
        }
        decodeQueryData(buffer);
        return buffer.toString();
    }

    /**
	 * <p>
	 * If this data contains any INVALID_CHARACTERS encode the data into a
	 * target String.
	 * </p>
	 * 
	 * <p>
	 * NOTE: the algorithm between encode and decode is shared, if you modify
	 * one you should modify the other.
	 * </p>
	 * 
	 * @see decode(String data)
	 */
    private static StringBuffer encodeQueryData(StringBuffer data) {
        for (int i = 0; i < INVALID_CHARACTERS.length; ++i) {
            String source = INVALID_CHARACTERS[i];
            String coded = CODED_CHARACTERS[i];
            data = StringUtils.replaceAll(data, source, coded);
        }
        return data;
    }

    /**
	 * <p>
	 * If this data contains any encoded INVALID_CHARACTERS, decode the data
	 * back into the source string
	 * </p>
	 * 
	 * <p>
	 * NOTE: the algorithm between encode and decode is shared, if you modify
	 * one you should modify the other.
	 * </p>
	 * 
	 * @see encode(String data)
	 */
    private static StringBuffer decodeQueryData(StringBuffer data) {
        for (int i = 0; i < INVALID_CHARACTERS.length; ++i) {
            String source = INVALID_CHARACTERS[i];
            String coded = CODED_CHARACTERS[i];
            data = StringUtils.replaceAll(data, coded, source);
        }
        return data;
    }
}
