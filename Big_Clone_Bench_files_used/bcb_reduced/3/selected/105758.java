package com.rcreations.codec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;

/**
 * Utility class dealing with encoding and decoding of data.
 * Contains functions dealing with:
 * <ul>
 * <li> Escaping string with html character entities.
 * <li> Escaping string with xml character entities.
 * <li> Encoding and decoding a GET URL query parameter value.
 * <li> Encoding and decoding bytes with base 64 encoding.
 * <li> Hashing a string with MD 5 message digest followed by base 64 encoding.
 * <li> Encoding and decoding a value ready for use as a query parameter value while also disguising the data.
 * </ul>
 */
public class EncodingUtils {

    /**
    * Convert a text string to html character entities.
    * For example:
    * <ul>
    * <li> '&nbsp;' is converted to "&amp;nbsp;"
    * <li> '<' is converted to "&amp;lt;"
    * </ul>
    * 
    * An alternative is to call Apache Common StringEscapeUtils.escapeHtml() method.
    * 
    * @param strInput input to convert
    * @param bPreserveFirstSpaceOnly if true, then spaces get special treatment: where leading spaces are converted, but any spaces following a letter will be preserved. Useful in table data where you want to show any leading spaces but want to preserve other spaces so the table's auto wrap feature still works.
    */
    public static String htmlEscape(String strInput, boolean bPreserveFirstSpaceOnly) {
        if (strInput == null) {
            return null;
        }
        int inputLength = strInput.length();
        StringBuffer res = new StringBuffer(inputLength * 3);
        int i;
        char cLast = 0;
        boolean bFirstNonSpaceFound = false;
        for (i = 0; i < inputLength; i++) {
            char c = strInput.charAt(i);
            switch(c) {
                case ' ':
                    if (bPreserveFirstSpaceOnly) {
                        if (bFirstNonSpaceFound == false || cLast == ' ') {
                            res.append("&nbsp;");
                        } else {
                            res.append(' ');
                        }
                    } else {
                        res.append("&nbsp;");
                    }
                    break;
                case '\t':
                    res.append("&nbsp;&nbsp;&nbsp;");
                    bFirstNonSpaceFound = true;
                    break;
                case '\r':
                    res.append("<br>\r");
                    bFirstNonSpaceFound = true;
                    break;
                case '\n':
                    if (cLast != '\r') {
                        res.append("<br>\n");
                    } else {
                        res.append("\n");
                    }
                    bFirstNonSpaceFound = true;
                    break;
                case '\"':
                    res.append("&quot;");
                    bFirstNonSpaceFound = true;
                    break;
                case '&':
                    res.append("&amp;");
                    bFirstNonSpaceFound = true;
                    break;
                case '<':
                    res.append("&lt;");
                    bFirstNonSpaceFound = true;
                    break;
                case '>':
                    res.append("&gt;");
                    bFirstNonSpaceFound = true;
                    break;
                case '£':
                    res.append("&pound;");
                    bFirstNonSpaceFound = true;
                    break;
                case '©':
                    res.append("&copy;");
                    bFirstNonSpaceFound = true;
                    break;
                case '®':
                    res.append("&reg;");
                    bFirstNonSpaceFound = true;
                    break;
                case '«':
                    res.append("&laquo;");
                    bFirstNonSpaceFound = true;
                    break;
                case '°':
                    res.append("&deg;");
                    bFirstNonSpaceFound = true;
                    break;
                case '±':
                    res.append("&plusmn;");
                    bFirstNonSpaceFound = true;
                    break;
                case '²':
                    res.append("&sup2;");
                    bFirstNonSpaceFound = true;
                    break;
                case '³':
                    res.append("&sup3;");
                    bFirstNonSpaceFound = true;
                    break;
                case '´':
                    res.append("&acute;");
                    bFirstNonSpaceFound = true;
                    break;
                case 'µ':
                    res.append("&micro;");
                    bFirstNonSpaceFound = true;
                    break;
                case '¶':
                    res.append("&para;");
                    bFirstNonSpaceFound = true;
                    break;
                case '¹':
                    res.append("&sup1;");
                    bFirstNonSpaceFound = true;
                    break;
                case '»':
                    res.append("&raquo;");
                    bFirstNonSpaceFound = true;
                    break;
                case '¼':
                    res.append("&frac14;");
                    bFirstNonSpaceFound = true;
                    break;
                case '½':
                    res.append("&frac12;");
                    bFirstNonSpaceFound = true;
                    break;
                case '¾':
                    res.append("&frac34;");
                    bFirstNonSpaceFound = true;
                    break;
                default:
                    res.append(c);
                    bFirstNonSpaceFound = true;
                    break;
            }
            cLast = c;
        }
        return res.toString();
    }

    /**
    * Convert a text string to html character entities
    * where bPreserveFirstSpaceOnly = false
    * (see {@link #htmlEscape( String, boolean )}).
    * For example:
    * <ul>
    * <li> '&nbsp;' is converted to "&amp;nbsp;"
    * <li> '<' is converted to "&amp;lt;"
    * </ul>
    * 
    * An alternative is to call Apache Common StringEscapeUtils.escapeHtml() method.
    * 
    * @param strInput input to convert
    */
    public static String htmlEscape(String strInput) {
        return htmlEscape(strInput, false);
    }

    /**
    * Convert a text string to XML character entities.
    * For example:
    * <ul>
    * <li> '&' is converted to "&amp;amp;"
    * <li> '<' is converted to "&amp;lt;"
    * </ul>
    * 
    * An alternative is to call Apache Common StringEscapeUtils.escapeHtml() method.
    * 
    * @param strInput input to convert
    * @param bTags if false, then the '<' and '>' characters are preserved.
    */
    public static String xmlEscape(String strInput, boolean bTags) {
        int inputLength = strInput.length();
        StringBuffer res = new StringBuffer(inputLength * 3);
        int i;
        if (bTags) {
            for (i = 0; i < inputLength; i++) {
                char c = strInput.charAt(i);
                switch(c) {
                    case '&':
                        res.append("&amp;");
                        break;
                    case '<':
                        res.append("&lt;");
                        break;
                    case '>':
                        res.append("&gt;");
                        break;
                    default:
                        res.append(c);
                        break;
                }
            }
        } else {
            for (i = 0; i < inputLength; i++) {
                char c = strInput.charAt(i);
                switch(c) {
                    case '&':
                        res.append("&amp;");
                        break;
                    default:
                        res.append(c);
                        break;
                }
            }
        }
        return res.toString();
    }

    /**
    * Encodes a string for use as a GET URL query parameter value.
    * Convenience method for java.net.URLEncoder.encode().
    * 
    * An alternative is to call Apache Common URLCodec.encode() method.
    */
    public static String urlEncode(String val) {
        String output = null;
        if (val != null) {
            output = java.net.URLEncoder.encode(val);
        }
        return output;
    }

    /**
    * Decodes a GET URL query parameter value (opposite of urlEncode).
    * 
    * An alternative is to call Apache Common URLCodec.decode() method.
    * 
    * @see #urlEncode
    */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DLS")
    public static String urlDecode(String val) {
        StringBuffer buf = new StringBuffer(val.length());
        char c;
        for (int i = 0; i < val.length(); i++) {
            c = val.charAt(i);
            if (c == '%') {
                try {
                    buf.append((char) Integer.parseInt(val.substring(i + 1, i + 3), 16));
                    i += 2;
                    continue;
                } catch (NumberFormatException e) {
                }
            } else if (c == '+') {
                buf.append(' ');
                continue;
            }
            buf.append(c);
        }
        return buf.toString();
    }

    /**
    * Encodes a byte array into a string using base 64 encoding.
    * 
    * An alternative is to call Apache Common Base64.encode() method.
    */
    public static String base64Encode(byte[] val) {
        Base64Encoder encoder = new Base64Encoder(val);
        return encoder.processBytes();
    }

    /**
    * Decodes a string which was encoded with base 64 encoding
    * back into a byte array (opposite of base64Encode).
    * 
    * An alternative is to call Apache Common Base64.decode() method.
    * 
    * @see #base64Encode
    */
    public static byte[] base64Decode(String val) {
        Base64Decoder decoder = new Base64Decoder(val);
        return decoder.processStringToBytes();
    }

    /**
    * Process the given toXor array by XORing each byte with a
    * byte from the given withEncoder array.
    * @return byte array containing original toXor values XORed with the withEncoder array.
    */
    public static byte[] arrayXor(byte toXor[], byte withEncoder[]) {
        byte output[] = new byte[toXor.length];
        int iWith = 0;
        for (int i = 0; i < toXor.length; i++) {
            output[i] = (byte) (toXor[i] ^ withEncoder[iWith]);
            iWith = (iWith + 1) % withEncoder.length;
        }
        return output;
    }

    /**
    * Hashes the given string using MD5 message digest
    * and then returns the result as a base 64 encoded string.
    * 
    * An alternative is to call Apache Common MessageDigest.md5Hex() method.
    */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings({ "DLS", "REC" })
    public static String md5Encode(String val) {
        String output = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(val.getBytes());
            byte[] digest = md.digest();
            output = base64Encode(digest);
        } catch (Exception e) {
        }
        return output;
    }

    /**
    * Hashes the given string using MD5 message digest
    * and then returns the result as a base 64 encoded string.
    * 
    * An alternative is to call Apache Common MessageDigest.shaHex() method.
    */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings({ "DLS", "REC" })
    public static String shaEncode(String val) {
        String output = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(val.getBytes());
            byte[] digest = md.digest();
            output = base64Encode(digest);
        } catch (Exception e) {
        }
        return output;
    }

    static byte[] _gXorBytes = "gnutsp".getBytes();

    /**
    * Encodes the given string so that it's useable as a GET URL query parameter value
    * while also disguising the data.
    */
    public static String encodeVar(String strToEncode) {
        StringBuffer output = new StringBuffer("_Encoded");
        byte[] bytes = strToEncode.getBytes();
        bytes = EncodingUtils.arrayXor(bytes, _gXorBytes);
        output.append(base64Encode(bytes));
        return output.toString();
    }

    /**
    * Checks the given string for encoding by encodeVar and returns the decoded value.
    * @see #encodeVar
    */
    public static String decodeVar(String strToDecode) {
        String output;
        if (strToDecode.startsWith("_Encoded") == true) {
            output = strToDecode.substring(8);
            byte[] bytes = base64Decode(output);
            bytes = arrayXor(bytes, _gXorBytes);
            output = new String(bytes);
        } else {
            output = strToDecode;
        }
        return output;
    }

    /**
    * Testing the encoder.
    * Takes input from console and outputs encoded version.
    */
    public static void main(String args[]) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String strLine = null;
            while (true) {
                System.out.println("\nEnter text to encode: ");
                strLine = reader.readLine();
                if (strLine == null || strLine.length() == 0) {
                    break;
                }
                System.out.println(strLine + " --> " + encodeVar(strLine));
            }
        } catch (Exception e) {
        }
    }
}
