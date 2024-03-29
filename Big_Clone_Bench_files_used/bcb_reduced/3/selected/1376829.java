package com.limegroup.gnutella.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.limewire.io.IOUtils;
import org.limewire.util.I18NConvert;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains utility methods
 * @author  asingla
 */
public class LimeXMLUtils {

    private static final double MATCHING_RATE = .9;

    private static final String C_HEADER_BEGIN = "{";

    private static final String C_HEADER_END = "}";

    private static final String C_HEADER_NONE_VAL = "plaintext";

    private static final String C_HEADER_ZLIB_VAL = "deflate";

    private static final String C_HEADER_GZIP_VAL = "gzip";

    private static final String COMPRESS_HEADER_ZLIB = C_HEADER_BEGIN + C_HEADER_ZLIB_VAL + C_HEADER_END;

    private static final String COMPRESS_HEADER_GZIP = C_HEADER_BEGIN + C_HEADER_GZIP_VAL + C_HEADER_END;

    private static final String COMPRESS_HEADER_NONE = C_HEADER_BEGIN + C_HEADER_END;

    private static final int NONE = 0;

    private static final int GZIP = 1;

    private static final int ZLIB = 2;

    /**
     * Gets the text content of the child nodes.
     * This is the same as Node.getTextContent(), but exists on all
     * JDKs.
     */
    public static String getTextContent(Node node) {
        return getText(node.getChildNodes());
    }

    /**
     * Collapses a list of CDATASection, Text, and predefined EntityReference
     * nodes into a single string.  If the list contains other types of nodes,
     * those other nodes are ignored.
     */
    public static String getText(NodeList nodeList) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            switch(node.getNodeType()) {
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    buffer.append(node.getNodeValue());
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    if (node.getNodeName().equals("amp")) buffer.append('&'); else if (node.getNodeName().equals("lt")) buffer.append('<'); else if (node.getNodeName().equals("gt")) buffer.append('>'); else if (node.getNodeName().equals("apos")) buffer.append('\''); else if (node.getNodeName().equals("quot")) buffer.append('"');
                    break;
                default:
            }
        }
        return buffer.toString();
    }

    /**
     * Writes <CODE>string</CODE> into writer, escaping &, ', ", <, and >
     * with the XML excape strings.
     */
    public static void writeEscapedString(Writer writer, String string) throws IOException {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '<') writer.write("&lt;"); else if (c == '>') writer.write("&gt;"); else if (c == '&') writer.write("&amp;"); else if (c == '\'') writer.write("&apos;"); else if (c == '"') writer.write("&quot;"); else writer.write(c);
        }
    }

    /**
     * Reads all the bytes from the passed input stream till end of stream
     * reached.
     * @param in The input stream to read from
     * @return array of bytes read
     * @exception IOException If any I/O exception occurs while reading data
     */
    public static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            byteArray.write(buffer, 0, bytesRead);
        }
        return byteArray.toByteArray();
    }

    /**
     * Compares the queryDoc with the replyDoc and finds out if the
     * replyDoc is a match for the queryDoc
     * @param queryDoc The query Document
     * @param replyDoc potential reply Document
     * @return true if the replyDoc is a match for the queryDoc, false
     * otherwise
     */
    public static boolean match(LimeXMLDocument replyDoc, LimeXMLDocument queryDoc, boolean allowAllNulls) {
        if (queryDoc == null || replyDoc == null) throw new NullPointerException("querying with null doc.");
        Set<Map.Entry<String, String>> queryNameValues = queryDoc.getNameValueSet();
        int size = queryNameValues.size();
        int matchCount = 0;
        int nullCount = 0;
        boolean matchedBitrate = false;
        for (Map.Entry<String, String> entry : queryNameValues) {
            String currFieldName = entry.getKey();
            String queryValue = entry.getValue();
            assert queryValue != null : "null value";
            if (queryValue.equals("")) continue;
            String replyDocValue = replyDoc.getValue(currFieldName);
            if (currFieldName.endsWith("license_type__") && queryValue.length() > 0) {
                if (replyDocValue == null || !replyDocValue.startsWith(queryValue)) return false;
            }
            if (replyDocValue == null || replyDocValue.equals("")) nullCount++; else {
                try {
                    double rDVD = (new Double(replyDocValue)).doubleValue();
                    double qVD = (new Double(queryValue)).doubleValue();
                    if (rDVD == qVD) {
                        matchCount++;
                        if (currFieldName.equals(LimeXMLNames.AUDIO_BITRATE)) matchedBitrate = true;
                    }
                    continue;
                } catch (NumberFormatException nfe) {
                }
                String queryValueLC = queryValue.toLowerCase(Locale.US);
                String replyDocValueLC = I18NConvert.instance().getNorm(replyDocValue);
                if (replyDocValueLC.startsWith(queryValueLC) || replyDocValueLC.indexOf(" " + queryValueLC) >= 0) matchCount++;
            }
        }
        double sizeD = size;
        double matchCountD = matchCount;
        double nullCountD = nullCount;
        if (size > 1) {
            if (matchedBitrate) {
                sizeD--;
                matchCountD--;
                matchCount--;
            }
            if (((nullCountD + matchCountD) / sizeD) < MATCHING_RATE) return false;
            if (allowAllNulls || matchCount > 0) return true; else return false;
        } else if (size == 1) {
            if (allowAllNulls && nullCount == 1) return true;
            if (matchCountD / sizeD < 1) return false;
            return true;
        }
        return false;
    }

    public static boolean isMP3File(File in) {
        return isMP3File(in.getName());
    }

    public static boolean isMP3File(String in) {
        return in.toLowerCase(Locale.US).endsWith(".mp3");
    }

    public static boolean isRIFFFile(File f) {
        return isRIFFFile(f.getName());
    }

    public static boolean isRIFFFile(String in) {
        return in.toLowerCase(Locale.US).endsWith(".avi");
    }

    public static boolean isOGMFile(File f) {
        return isOGMFile(f.getName());
    }

    public static boolean isOGMFile(String in) {
        return in.toLowerCase(Locale.US).endsWith(".ogm");
    }

    public static boolean isOGGFile(File in) {
        return isOGGFile(in.getName());
    }

    public static boolean isOGGFile(String in) {
        return in.toLowerCase(Locale.US).endsWith(".ogg");
    }

    public static boolean isFLACFile(File in) {
        return isFLACFile(in.getName());
    }

    public static boolean isFLACFile(String in) {
        in = in.toLowerCase(Locale.US);
        return in.endsWith(".flac") || in.endsWith(".fla");
    }

    public static boolean isM4AFile(File in) {
        return isM4AFile(in.getName());
    }

    public static boolean isM4AFile(String in) {
        in = in.toLowerCase(Locale.US);
        return in.endsWith(".m4a") || in.endsWith(".m4p");
    }

    public static boolean isWMAFile(File f) {
        return isWMAFile(f.getName());
    }

    public static boolean isWMAFile(String in) {
        return in.toLowerCase(Locale.US).endsWith(".wma");
    }

    public static boolean isWMVFile(File f) {
        return isWMVFile(f.getName());
    }

    public static boolean isWMVFile(String in) {
        return in.toLowerCase(Locale.US).endsWith(".wmv");
    }

    public static boolean isASFFile(File f) {
        return isASFFile(f.getName());
    }

    public static boolean isASFFile(String in) {
        in = in.toLowerCase(Locale.US);
        return in.endsWith(".asf") || in.endsWith(".wm");
    }

    public static boolean isMPEGFile(File f) {
        return isMPEGFile(f.getName());
    }

    public static boolean isMPEGFile(String in) {
        in = in.toLowerCase(Locale.US);
        return in.endsWith(".mpg") || in.endsWith(".mpeg");
    }

    public static boolean isQuickTimeFile(File f) {
        return isQuickTimeFile(f.getName());
    }

    public static boolean isQuickTimeFile(String in) {
        in = in.toLowerCase(Locale.US);
        return in.endsWith(".mov") || in.endsWith(".m4v") || in.endsWith(".mp4") || in.endsWith(".3gp");
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio file (for example ID3 tags).
     */
    public static boolean isSupportedAudioFormat(File file) {
        return isSupportedAudioFormat(file.getName());
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio file (for example ID3 tags).
     */
    public static boolean isSupportedAudioFormat(String file) {
        return isMP3File(file) || isOGGFile(file) || isM4AFile(file) || isWMAFile(file) || isFLACFile(file);
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Video file.
     */
    public static boolean isSupportedVideoFormat(File file) {
        return isSupportedVideoFormat(file.getName());
    }

    /** 
     * Returns true if LimeWire can edit the meta data of this Audio file,
     * false otherwise
     */
    public static boolean isSupportedAudioEditableFormat(String file) {
        return isMP3File(file) || isOGGFile(file) || isM4AFile(file) || isFLACFile(file);
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Video file.
     */
    public static boolean isSupportedVideoFormat(String file) {
        return isRIFFFile(file) || isOGMFile(file) || isWMVFile(file) || isMPEGFile(file);
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio/Video file.
     */
    public static boolean isSupportedMultipleFormat(File file) {
        return isSupportedMultipleFormat(file.getName());
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio/Video file.
     */
    public static boolean isSupportedMultipleFormat(String file) {
        return isASFFile(file);
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio, Video, whatsoever file.
     */
    public static boolean isSupportedFormat(File file) {
        return isSupportedFormat(file.getName());
    }

    /** 
     * Returns true if LimeWire might be able to read the Meta Data of 
     * this Audio, Video, whatsoever file.
     */
    public static boolean isSupportedFormat(String file) {
        return isSupportedAudioFormat(file) || isSupportedVideoFormat(file) || isSupportedMultipleFormat(file);
    }

    /**
     * Returns true if LimeWire can edit the meta data of this file,
     * false otherwise
     */
    public static boolean isSupportedEditableFormat(String file) {
        return isSupportedAudioEditableFormat(file);
    }

    /**
     * @return whether LimeWire supports writing metadata into the file of specific type.
     * (we may be able to parse the metadata, but not annotate it)
     */
    public static boolean isEditableFormat(File file) {
        return isEditableFormat(file.getName());
    }

    public static boolean isEditableFormat(String file) {
        return isMP3File(file) || isOGGFile(file);
    }

    public static boolean isSupportedFormatForSchema(File file, String schemaURI) {
        if (isSupportedMultipleFormat(file)) return true; else if (LimeXMLNames.AUDIO_SCHEMA.equals(schemaURI)) return isSupportedAudioFormat(file); else if (LimeXMLNames.VIDEO_SCHEMA.equals(schemaURI)) return isSupportedVideoFormat(file); else return false;
    }

    public static boolean isFilePublishable(String file) {
        return isMP3File(file);
    }

    /**
     * Scans over the given String and returns a new String that contains
     * no invalid whitespace XML characters if any exist.  If none exist
     * the original string is returned.
     * 
     * This DOES NOT CONVERT entities such as & or <, it will only remove
     * invalid characters such as , , etc...
     */
    public static String scanForBadCharacters(String input) {
        if (input == null) return null;
        int length = input.length();
        StringBuilder buffer = null;
        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            if (Character.getType(c) == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
                if (buffer == null) buffer = createBuffer(input, i);
                buffer.append(' ');
            } else {
                if (buffer != null) buffer.appendCodePoint(c);
            }
            i += Character.charCount(c);
        }
        if (buffer == null) return input; else return buffer.toString();
    }

    /**
     * Attempts to unencode any leftover encoded entities in the XML.
     * This is generally caused by poor ID3 writers that write "&amp;" instead of "&".
     */
    public static String unencodeXML(String input) {
        if (input == null) return null;
        int length = input.length();
        StringBuilder buffer = null;
        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            if (c == '&') {
                if (input.regionMatches(i + 1, "amp;", 0, 4)) {
                    if (buffer == null) buffer = createBuffer(input, i);
                    buffer.append("&");
                    i += 4;
                } else if (input.regionMatches(i + 1, "lt;", 0, 3)) {
                    if (buffer == null) buffer = createBuffer(input, i);
                    buffer.append("<");
                    i += 3;
                } else if (input.regionMatches(i + 1, "gt;", 0, 3)) {
                    if (buffer == null) buffer = createBuffer(input, i);
                    buffer.append(">");
                    i += 3;
                } else if (input.regionMatches(i + 1, "quot;", 0, 5)) {
                    if (buffer == null) buffer = createBuffer(input, i);
                    buffer.append("\"");
                    i += 5;
                } else if (input.regionMatches(i + 1, "apos;", 0, 5)) {
                    if (buffer == null) buffer = createBuffer(input, i);
                    buffer.append("'");
                    i += 5;
                } else {
                    if (buffer != null) buffer.appendCodePoint(c);
                }
            } else {
                if (buffer != null) buffer.appendCodePoint(c);
            }
            i += Character.charCount(c);
        }
        if (buffer == null) return input; else return buffer.toString();
    }

    /**
     * Parses the passed string, and encodes the special characters (used in
     * xml for special purposes) with the appropriate codes.
     * e.g. '<' is changed to '&lt;'
     * @return the encoded string. Returns null, if null is passed as argument
     */
    public static String encodeXML(String input) {
        if (input == null) return null;
        int length = input.length();
        StringBuilder buffer = null;
        for (int i = 0; i < length; ) {
            int c = input.codePointAt(i);
            if (Character.getType(c) == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
                if (buffer == null) buffer = createBuffer(input, i);
                buffer.append(' ');
            } else {
                switch(c) {
                    case '&':
                        if (buffer == null) buffer = createBuffer(input, i);
                        buffer.append("&amp;");
                        break;
                    case '<':
                        if (buffer == null) buffer = createBuffer(input, i);
                        buffer.append("&lt;");
                        break;
                    case '>':
                        if (buffer == null) buffer = createBuffer(input, i);
                        buffer.append("&gt;");
                        break;
                    case '\"':
                        if (buffer == null) buffer = createBuffer(input, i);
                        buffer.append("&quot;");
                        break;
                    case '\'':
                        if (buffer == null) buffer = createBuffer(input, i);
                        buffer.append("&apos;");
                        break;
                    default:
                        if (buffer != null) buffer.appendCodePoint(c);
                }
            }
            i += Character.charCount(c);
        }
        if (buffer == null) return input; else return buffer.toString();
    }

    /** Creates a StringBuilder from the given data, up to the right length. */
    private static StringBuilder createBuffer(String data, int upTo) {
        StringBuilder sb = new StringBuilder(data.length() * 2);
        sb.append(data, 0, upTo);
        return sb;
    }

    /** @return A properly formatted version of the input data.
     */
    public static byte[] compress(byte[] data) {
        byte[] compressedData = null;
        if (shouldCompress(data)) compressedData = compressZLIB(data);
        byte[] retBytes = null;
        if (compressedData != null) {
            retBytes = new byte[COMPRESS_HEADER_ZLIB.length() + compressedData.length];
            System.arraycopy(COMPRESS_HEADER_ZLIB.getBytes(), 0, retBytes, 0, COMPRESS_HEADER_ZLIB.length());
            System.arraycopy(compressedData, 0, retBytes, COMPRESS_HEADER_ZLIB.length(), compressedData.length);
        } else {
            retBytes = new byte[COMPRESS_HEADER_NONE.length() + data.length];
            System.arraycopy(COMPRESS_HEADER_NONE.getBytes(), 0, retBytes, 0, COMPRESS_HEADER_NONE.length());
            System.arraycopy(data, 0, retBytes, COMPRESS_HEADER_NONE.length(), data.length);
        }
        return retBytes;
    }

    /** Currently, all data is compressed.  In the future, this will handle
     *  heuristics about whether data should be compressed or not.
     */
    private static boolean shouldCompress(byte[] data) {
        if (data.length >= 1000) return true; else return false;
    }

    /** Returns a ZLIB'ed version of data. */
    private static byte[] compressZLIB(byte[] data) {
        DeflaterOutputStream gos = null;
        Deflater def = null;
        try {
            def = new Deflater();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            gos = new DeflaterOutputStream(baos, def);
            gos.write(data, 0, data.length);
            gos.flush();
            gos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            assert false : "Couldn't write to byte stream";
            return null;
        } finally {
            IOUtils.close(gos);
            IOUtils.close(def);
        }
    }

    /** @return Correctly uncompressed data (according to Content-Type header) 
     *  May return a byte[] of length 0 if something bad happens. 
     */
    public static byte[] uncompress(byte[] data) throws IOException {
        byte[] retBytes = new byte[0];
        String headerFragment = new String(data, 0, C_HEADER_BEGIN.length());
        if (headerFragment.equals(C_HEADER_BEGIN)) {
            boolean found = false;
            int i = 0;
            for (; i < data.length && !found; i++) if (data[i] == (byte) 125) found = true;
            headerFragment = new String(data, 1, i - 1 - 1);
            int comp = getCompressionType(headerFragment);
            if (comp == NONE) {
                retBytes = new byte[data.length - (headerFragment.length() + 2)];
                System.arraycopy(data, i, retBytes, 0, data.length - (headerFragment.length() + 2));
            } else if (comp == GZIP) {
                retBytes = new byte[data.length - COMPRESS_HEADER_GZIP.length()];
                System.arraycopy(data, COMPRESS_HEADER_GZIP.length(), retBytes, 0, data.length - COMPRESS_HEADER_GZIP.length());
                retBytes = uncompressGZIP(retBytes);
            } else if (comp == ZLIB) {
                retBytes = new byte[data.length - COMPRESS_HEADER_ZLIB.length()];
                System.arraycopy(data, COMPRESS_HEADER_ZLIB.length(), retBytes, 0, data.length - COMPRESS_HEADER_ZLIB.length());
                retBytes = uncompressZLIB(retBytes);
            } else ;
        } else return data;
        return retBytes;
    }

    private static int getCompressionType(String header) {
        String s = header.trim();
        if (s.equals("") || s.equalsIgnoreCase(C_HEADER_NONE_VAL)) return NONE; else if (s.equalsIgnoreCase(C_HEADER_GZIP_VAL)) return GZIP; else if (s.equalsIgnoreCase(C_HEADER_ZLIB_VAL)) return ZLIB; else return -1;
    }

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressGZIP(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        InflaterInputStream gis = null;
        try {
            gis = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int b = gis.read();
                if (b == -1) break;
                baos.write(b);
            }
            return baos.toByteArray();
        } finally {
            IOUtils.close(gis);
        }
    }

    /** Returns the uncompressed version of the given ZLIB'ed bytes.  Throws
     *  IOException if the data is corrupt. */
    private static byte[] uncompressZLIB(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        InflaterInputStream gis = null;
        Inflater inf = null;
        try {
            inf = new Inflater();
            gis = new InflaterInputStream(bais, inf);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int b = gis.read();
                if (b == -1) break;
                baos.write(b);
            }
            return baos.toByteArray();
        } finally {
            IOUtils.close(gis);
            IOUtils.close(inf);
        }
    }

    private static final int NUM_BYTES_TO_HASH = 100;

    private static final int NUM_TOTAL_HASH = NUM_BYTES_TO_HASH * 3;

    private static void clearHashBytes(byte[] hashBytes) {
        for (int i = 0; i < NUM_BYTES_TO_HASH; i++) hashBytes[i] = (byte) 0;
    }

    /**
     * Hashes the file using bits and pieces of the file.
     * 
     * @return The SHA hash bytes of the input bytes.
     * @throws IOException if hashing failed for any reason.
     */
    public static byte[] hashFile(File toHash) throws IOException {
        byte[] retBytes = null;
        FileInputStream fis = null;
        byte[] hashBytes = new byte[NUM_BYTES_TO_HASH];
        try {
            fis = new FileInputStream(toHash);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsae) {
                throw new IllegalStateException(nsae);
            }
            long fileLength = toHash.length();
            if (fileLength < NUM_TOTAL_HASH) {
                int numRead = 0;
                do {
                    clearHashBytes(hashBytes);
                    numRead = fis.read(hashBytes);
                    md.update(hashBytes);
                    if (toHash.length() != fileLength) throw new IOException("invalid length");
                } while (numRead == NUM_BYTES_TO_HASH);
            } else {
                long thirds = fileLength / 3;
                clearHashBytes(hashBytes);
                fis.read(hashBytes);
                md.update(hashBytes);
                if (toHash.length() != fileLength) throw new IOException("invalid length");
                clearHashBytes(hashBytes);
                fis.skip(thirds - NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);
                if (toHash.length() != fileLength) throw new IOException("invalid length");
                clearHashBytes(hashBytes);
                fis.skip(toHash.length() - (thirds + NUM_BYTES_TO_HASH) - NUM_BYTES_TO_HASH);
                fis.read(hashBytes);
                md.update(hashBytes);
                if (toHash.length() != fileLength) throw new IOException("invalid length");
            }
            retBytes = md.digest();
        } finally {
            if (fis != null) fis.close();
        }
        return retBytes;
    }
}
