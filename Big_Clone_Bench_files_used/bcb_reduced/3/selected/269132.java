package meconsea.webcoll.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComUtil {

    public static String md5(String src) {
        char hexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] bytes = src.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            bytes = md.digest();
            int j = bytes.length;
            char[] chars = new char[j * 2];
            int k = 0;
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                chars[k++] = hexChars[b >>> 4 & 0xf];
                chars[k++] = hexChars[b & 0xf];
            }
            return new String(chars);
        } catch (Exception e) {
            return null;
        }
    }

    private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();

    private static final char[] AMP_ENCODE = "&amp;".toCharArray();

    private static final char[] LT_ENCODE = "&lt;".toCharArray();

    private static final char[] GT_ENCODE = "&gt;".toCharArray();

    private static MessageDigest digest = null;

    public static boolean validateUserName(String username) {
        Pattern p = Pattern.compile("^\\w+$");
        Matcher m = p.matcher(username);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static String Array2String(String[] values) {
        String result = "";
        if (values == null) {
            return result;
        }
        int len = values.length;
        for (int i = 0; i < len; i++) {
            result += values[i] + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(result.length() - 1);
        }
        return result;
    }

    public static String Array2String(Object[] values) {
        String result = "";
        if (values == null) {
            return result;
        }
        int len = values.length;
        for (int i = 0; i < len; i++) {
            result += values[i].toString() + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(result.length() - 1);
        }
        return result;
    }

    public static String Array2String(List values) {
        String result = "";
        if (values == null) {
            return result;
        }
        int len = values.size();
        for (int i = 0; i < len; i++) {
            result += values.get(i).toString() + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(result.length() - 1);
        }
        return result;
    }

    public static String base64Encode(String txt) {
        if (txt != null && txt.length() > 0) {
            txt = new sun.misc.BASE64Encoder().encode(txt.getBytes());
        }
        return txt;
    }

    public static String base64Encode(byte[] txt) {
        String encodeTxt = "";
        if (txt != null && txt.length > 0) {
            encodeTxt = new sun.misc.BASE64Encoder().encode(txt);
        }
        return encodeTxt;
    }

    public static String base64decode(String txt) {
        if (txt != null && txt.length() > 0) {
            byte[] buf;
            try {
                buf = new sun.misc.BASE64Decoder().decodeBuffer(txt);
                txt = new String(buf);
            } catch (IOException ex) {
            }
        }
        return txt;
    }

    public static byte[] base64decodebyte(String txt) {
        byte[] buf = null;
        if (txt != null && txt.length() > 0) {
            try {
                buf = new sun.misc.BASE64Decoder().decodeBuffer(txt);
            } catch (IOException ex) {
            }
        }
        return buf;
    }

    /**
	 * Replaces all instances of oldString with newString in line.
	 *
	 * @param line
	 *            the String to search to perform replacements on
	 * @param oldString
	 *            the String that should be replaced by newString
	 * @param newString
	 *            the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replace(String line, String oldString, String newString) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
	 * Replaces all instances of oldString with newString in line with the added
	 * feature that matches of newString in oldString ignore case.
	 *
	 * @param line
	 *            the String to search to perform replacements on
	 * @param oldString
	 *            the String that should be replaced by newString
	 * @param newString
	 *            the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replaceIgnoreCase(String line, String oldString, String newString) {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
	 * Replaces all instances of oldString with newString in line with the added
	 * feature that matches of newString in oldString ignore case. The count
	 * paramater is set to the number of replaces performed.
	 *
	 * @param line
	 *            the String to search to perform replacements on
	 * @param oldString
	 *            the String that should be replaced by newString
	 * @param newString
	 *            the String that will replace all instances of oldString
	 * @param count
	 *            a value that will be updated with the number of replaces
	 *            performed.
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replaceIgnoreCase(String line, String oldString, String newString, int[] count) {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            int counter = 0;
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**
	 * Replaces all instances of oldString with newString in line. The count
	 * Integer is updated with number of replaces.
	 *
	 * @param line
	 *            the String to search to perform replacements on
	 * @param oldString
	 *            the String that should be replaced by newString
	 * @param newString
	 *            the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replace(String line, String oldString, String newString, int[] count) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            int counter = 0;
            counter++;
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**
	 * This method takes a string which may contain HTML tags (ie, &lt;b&gt;,
	 * &lt;table&gt;, etc) and converts the '&lt'' and '&gt;' characters to
	 * their HTML escape sequences.
	 *
	 * @param in
	 *            the text to be converted.
	 * @return the input string with the characters '&lt;' and '&gt;' replaced
	 *         with their HTML escape sequences.
	 */
    public static final String escapeHTMLTags(String in) {
        if (in == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = in.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
            }
        }
        if (last == 0) {
            return in;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
	 * Hashes a String using the Md5 algorithm and returns the result as a
	 * String of hexadecimal numbers. This method is synchronized to avoid
	 * excessive MessageDigest object creation. If calling this method becomes a
	 * bottleneck in your code, you may wish to maintain a pool of MessageDigest
	 * objects instead of using this method.
	 * <p>
	 * A hash is a one-way function -- that is, given an input, an output is
	 * easily computed. However, given the output, the input is almost
	 * impossible to compute. This is useful for passwords since we can store
	 * the hash and a hacker will then have a very hard time determining the
	 * original password.
	 * <p>
	 * In Jive, every time a user logs in, we simply take their plain text
	 * password, compute the hash, and compare the generated hash to the stored
	 * hash. Since it is almost impossible that two passwords will generate the
	 * same hash, we know if the user gave us the correct password or not. The
	 * only negative to this system is that password recovery is basically
	 * impossible. Therefore, a reset password method is used instead.
	 *
	 * @param data
	 *            the String to compute the hash of.
	 * @return a hashed version of the passed-in String
	 */
    public static final synchronized String hash(String data) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("Failed to load the MD5 MessageDigest. " + "We will be unable to function normally.");
                nsae.printStackTrace();
            }
        }
        digest.update(data.getBytes());
        return encodeHex(digest.digest());
    }

    /**
	 * Turns an array of bytes into a String representing each byte as an
	 * unsigned hex number.
	 * <p>
	 * Method by Santeri Paavolainen, Helsinki Finland 1996<br>
	 * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
	 * Distributed under LGPL.
	 *
	 * @param bytes
	 *            an array of bytes to convert to a hex-string
	 * @return generated hex string
	 */
    public static final String encodeHex(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        int i;
        for (i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString().toUpperCase();
    }

    /**
	 * Turns a hex encoded string into a byte array. It is specifically meant to
	 * "reverse" the toHex(byte[]) method.
	 *
	 * @param hex
	 *            a hex encoded String to transform into a byte array.
	 * @return a byte array representing the hex String[
	 */
    public static final byte[] decodeHex(String hex) {
        char[] chars = hex.toCharArray();
        byte[] bytes = new byte[chars.length / 2];
        int byteCount = 0;
        for (int i = 0; i < chars.length; i += 2) {
            byte newByte = 0x00;
            newByte |= hexCharToByte(chars[i]);
            newByte <<= 4;
            newByte |= hexCharToByte(chars[i + 1]);
            bytes[byteCount] = newByte;
            byteCount++;
        }
        return bytes;
    }

    /**
	 * Returns the the byte value of a hexadecmical char (0-f). It's assumed
	 * that the hexidecimal chars are lower case as appropriate.
	 *
	 * @param ch
	 *            a hexedicmal character (0-f)
	 * @return the byte value of the character (0x00-0x0F)
	 */
    private static final byte hexCharToByte(char ch) {
        switch(ch) {
            case '0':
                return 0x00;
            case '1':
                return 0x01;
            case '2':
                return 0x02;
            case '3':
                return 0x03;
            case '4':
                return 0x04;
            case '5':
                return 0x05;
            case '6':
                return 0x06;
            case '7':
                return 0x07;
            case '8':
                return 0x08;
            case '9':
                return 0x09;
            case 'a':
                return 0x0A;
            case 'b':
                return 0x0B;
            case 'c':
                return 0x0C;
            case 'd':
                return 0x0D;
            case 'e':
                return 0x0E;
            case 'f':
                return 0x0F;
        }
        return 0x00;
    }

    /**
	 * Converts a line of text into an array of lower case words using a
	 * BreakIterator.wordInstance().
	 * <p>
	 *
	 * This method is under the Jive Open Source Software License and was
	 * written by Mark Imbriaco.
	 *
	 * @param text
	 *            a String of text to convert into an array of words
	 * @return text broken up into an array of words.
	 */
    public static final String[] toLowerCaseWordArray(String text) {
        if (text == null || text.length() == 0) {
            return new String[0];
        }
        ArrayList<String> wordList = new ArrayList<String>();
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(text);
        int start = 0;
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            String tmp = text.substring(start, end).trim();
            tmp = replace(tmp, "+", "");
            tmp = replace(tmp, "/", "");
            tmp = replace(tmp, "\\", "");
            tmp = replace(tmp, "#", "");
            tmp = replace(tmp, "*", "");
            tmp = replace(tmp, ")", "");
            tmp = replace(tmp, "(", "");
            tmp = replace(tmp, "&", "");
            if (tmp.length() > 0) {
                wordList.add(tmp);
            }
        }
        return (String[]) wordList.toArray(new String[wordList.size()]);
    }

    /**
	 * Pseudo-random number generator object for use with randomString(). The
	 * Random class is not considered to be cryptographically secure, so only
	 * use these random Strings for low to medium security applications.
	 */
    private static Random randGen = new Random();

    /**
	 * Array of numbers and letters of mixed case. Numbers appear in the list
	 * twice so that there is a more equal chance that a number will be picked.
	 * We can use the array to get a random number or letter by picking a random
	 * array index.
	 */
    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
	 * Returns a random String of numbers and letters (lower and upper case) of
	 * the specified length. The method uses the Random class that is built-in
	 * to Java which is suitable for low to medium grade security uses. This
	 * means that the output is only pseudo random, i.e., each number is
	 * mathematically generated so is not truly random.
	 * <p>
	 *
	 * The specified length must be at least one. If not, the method will return
	 * null.
	 *
	 * @param length
	 *            the desired length of the random String to return.
	 * @return a random String of numbers and letters of the specified length.
	 */
    public static final String randomString(int length) {
        if (length < 1) {
            return null;
        }
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randGen.nextInt(71)];
        }
        return new String(randBuffer);
    }

    /**
	 * Intelligently chops a String at a word boundary (whitespace) that occurs
	 * at the specified index in the argument or before. However, if there is a
	 * newline character before <code>length</code>, the String will be
	 * chopped there. If no newline or whitespace is found in
	 * <code>string</code> up to the index <code>length</code>, the String
	 * will chopped at <code>length</code>.
	 * <p>
	 * For example, chopAtWord("This is a nice String", 10) will return "This is
	 * a" which is the first word boundary less than or equal to 10 characters
	 * into the original String.
	 *
	 * @param string
	 *            the String to chop.
	 * @param length
	 *            the index in <code>string</code> to start looking for a
	 *            whitespace boundary at.
	 * @return a substring of <code>string</code> whose length is less than or
	 *         equal to <code>length</code>, and that is chopped at
	 *         whitespace.
	 */
    public static final String chopAtWord(String string, int length) {
        if (string == null) {
            return string;
        }
        char[] charArray = string.toCharArray();
        int sLength = string.length();
        if (length < sLength) {
            sLength = length;
        }
        for (int i = 0; i < sLength - 1; i++) {
            if (charArray[i] == '\r' && charArray[i + 1] == '\n') {
                return string.substring(0, i + 1);
            } else if (charArray[i] == '\n') {
                return string.substring(0, i);
            }
        }
        if (charArray[sLength - 1] == '\n') {
            return string.substring(0, sLength - 1);
        }
        if (string.length() < length) {
            return string;
        }
        for (int i = length - 1; i > 0; i--) {
            if (charArray[i] == ' ') {
                return string.substring(0, i).trim();
            }
        }
        return string.substring(0, length);
    }

    /**
	 * Escapes all necessary characters in the String so that it can be used in
	 * an XML doc.
	 *
	 * @param string
	 *            the string to escape.
	 * @return the string with appropriate characters escaped.
	 */
    public static final String escapeForXML(String string) {
        if (string == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = string.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '&') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(AMP_ENCODE);
            } else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
            }
        }
        if (last == 0) {
            return string;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    public static final String escapeForSpecial(String string) {
        if (string == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = string.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '&') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(AMP_ENCODE);
            } else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
            } else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
            }
        }
        if (last == 0) {
            return string;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
	 * Unescapes the String by converting XML escape sequences back into normal
	 * characters.
	 *
	 * @param string
	 *            the string to unescape.
	 * @return the string with appropriate characters unescaped.
	 */
    public static final String unescapeFromXML(String string) {
        string = replace(string, "&lt;", "<");
        string = replace(string, "&gt;", ">");
        string = replace(string, "&quot;", "\"");
        return replace(string, "&amp;", "&");
    }

    private static final char[] zeroArray = "0000000000000000".toCharArray();

    /**
	 * Pads the supplied String with 0's to the specified length and returns the
	 * result as a new String. For example, if the initial String is "9999" and
	 * the desired length is 8, the result would be "00009999". This type of
	 * padding is useful for creating numerical values that need to be stored
	 * and sorted as character data. Note: the current implementation of this
	 * method allows for a maximum <tt>length</tt> of 16.
	 *
	 * @param string
	 *            the original String to pad.
	 * @param length
	 *            the desired length of the new padded String.
	 * @return a new String padded with the required number of 0's.
	 */
    public static final String zeroPadString(String string, int length) {
        if (string == null || string.length() > length) {
            return string;
        }
        StringBuffer buf = new StringBuffer(length);
        buf.append(zeroArray, 0, length - string.length()).append(string);
        return buf.toString();
    }

    /**
	 * Formats a Date as a fifteen character long String made up of the Date's
	 * padded millisecond value.
	 *
	 * @return a Date encoded as a String.
	 */
    public static final String dateToMillis(Date date) {
        return zeroPadString(Long.toString(date.getTime()), 15);
    }

    /**
	 * Formats a Date as a fifteen character long String made up of the Date's
	 * padded millisecond value.
	 *
	 * @return a Date encoded as a String.
	 */
    @SuppressWarnings("unchecked")
    public static final String collectionToString(Collection c, String spilt) {
        if (c == null) {
            return null;
        }
        if (spilt == null) {
            return null;
        }
        String ret = "";
        ArrayList a = new ArrayList(c);
        try {
            for (int i = 0; i < a.size(); i++) {
                String t = (String) a.get(i);
                if (i == a.size() - 1) {
                    ret = ret + t;
                } else {
                    ret = ret + t + spilt;
                }
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String genPassword(int length) {
        if (length < 1) {
            return null;
        }
        String[] strChars = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "a" };
        StringBuffer strPassword = new StringBuffer();
        int nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
        for (int i = 0; i < length; i++) {
            nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
            strPassword.append(strChars[nRand % (strChars.length - 1)]);
        }
        return strPassword.toString();
    }

    public static String genNumPassword(int length) {
        if (length < 1) {
            return null;
        }
        String[] strChars = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        StringBuffer strPassword = new StringBuffer();
        int nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
        for (int i = 0; i < length; i++) {
            nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
            strPassword.append(strChars[nRand % (strChars.length - 1)]);
        }
        return strPassword.toString();
    }

    public static String genEmptyString(int length) {
        if (length < 1) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String getAsciiString(int digit) {
        byte ret[] = new byte[1];
        ret[0] = (byte) digit;
        return new String(ret);
    }

    public static int getAsciiNum(String s) {
        if (s.length() < 1) {
            return 0;
        }
        byte b = s.getBytes()[0];
        return b;
    }

    public static String getCurrTime() {
        Date now = new Date();
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String s = outFormat.format(now);
        return s;
    }

    public static String formatDate(Date date, String format) {
        SimpleDateFormat outFormat = new SimpleDateFormat(format);
        return outFormat.format(date);
    }

    /**
	 * Formats a Date object to return a date using the global locale.
	 */
    public static String formatDate(Date date) {
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd");
        return outFormat.format(date);
    }

    /**
	 * Formats a Date object to return a date and time using the global locale.
	 */
    public static String formatDateTime(Date date) {
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return outFormat.format(date);
    }

    public static String formatDate2(Date myDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        String strDate = formatter.format(myDate);
        return strDate;
    }

    public static String formatDate3(Date myDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        String strDate = formatter.format(myDate);
        return strDate;
    }

    public static String formatDate4(Date myDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String strDate = formatter.format(myDate);
        return strDate;
    }

    public static String formatDate5(Date myDate) {
        String strDate = getYear(myDate) + "-" + getMonth(myDate) + "-" + getDay(myDate);
        return strDate;
    }

    public static String formatDate6(Date myDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String strDate = formatter.format(myDate);
        return strDate;
    }

    public static long Date2Long(int year, int month, int date) {
        Calendar cld = Calendar.getInstance();
        month = month - 1;
        cld.set(year, month, date);
        return cld.getTime().getTime();
    }

    public static long Time2Long(int year, int month, int date, int hour, int minute, int second) {
        Calendar cld = Calendar.getInstance();
        month = month - 1;
        cld.set(year, month, date, hour, minute, second);
        return cld.getTime().getTime();
    }

    public static int getYear(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.YEAR);
    }

    public static int getMonth(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.MONTH) + 1;
    }

    public static int getDay(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.DAY_OF_MONTH);
    }

    public static int getHour(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.MINUTE);
    }

    public static int getSecond(long t) {
        Calendar cld = Calendar.getInstance();
        if (t > 0) {
            cld.setTime(new java.util.Date(t));
        }
        return cld.get(Calendar.SECOND);
    }

    public static int getYear(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.YEAR);
    }

    public static int getMonth(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.MONTH) + 1;
    }

    public static int getDay(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.DAY_OF_MONTH);
    }

    public static int getHour(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.MINUTE);
    }

    public static int getSecond(Date date) {
        Calendar cld = Calendar.getInstance();
        cld.setTime(date);
        return cld.get(Calendar.SECOND);
    }

    public static int getYear() {
        Calendar cld = Calendar.getInstance();
        cld.setTime(new java.util.Date());
        return cld.get(Calendar.YEAR);
    }

    public static int getMonth() {
        Calendar cld = Calendar.getInstance();
        cld.setTime(new java.util.Date());
        return cld.get(Calendar.MONTH) + 1;
    }

    public static int getDay() {
        Calendar cld = Calendar.getInstance();
        cld.setTime(new java.util.Date());
        return cld.get(Calendar.DAY_OF_MONTH);
    }

    public static String replaceComma(String text) {
        if (text != null) {
            text = text.replaceAll("�??", ",");
        }
        return text;
    }

    public static String replaceBr(String text) {
        if (text != null) {
            text = text.replaceAll("\n", "<BR>");
        }
        return text;
    }

    public static long getLongTime() {
        return System.currentTimeMillis();
    }

    /**
	 * Check a string null or blank.
	 *
	 * @param param
	 *            string to check
	 * @return boolean
	 */
    public static boolean nullOrBlank(String param) {
        return (param == null || param.length() == 0 || param.trim().equals("")) ? true : false;
    }

    public static String notNull(String param) {
        return param == null ? "" : param.trim();
    }

    /**
	 * Parse a string to boolean.
	 *
	 * @param param
	 *            string to parse
	 * @return boolean value, if param begin with(1,y,Y,t,T) return true, on
	 *         exception return false.
	 */
    public static boolean parseBoolean(String param) {
        if (nullOrBlank(param)) {
            return false;
        }
        switch(param.charAt(0)) {
            case '1':
            case 'y':
            case 'Y':
            case 't':
            case 'T':
                return true;
        }
        return false;
    }

    public static String parseHtml(String srcStr) {
        System.out.println("转换之前" + srcStr);
        if (srcStr == null) {
            return "";
        }
        String dis = srcStr.replace("&lt;", "<").replace("&gt;", ">");
        return dis;
    }

    public static void main(String[] args) {
        System.out.println(ComUtil.md5("administrator" + "0-=zhehai)_+" + "371424198204251818"));
    }
}
