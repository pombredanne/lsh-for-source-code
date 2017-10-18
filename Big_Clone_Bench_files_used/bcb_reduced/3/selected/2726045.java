package utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;

/**
 * class for miscallenious String utils
 *
 * @author Kimmo Saarinen, Cidercone
 */
public class StringUtil {

    private static String NULL = "null";

    private static SimpleDateFormat formatReadable = new SimpleDateFormat("dd.MM.yyyy 'klo' HH:mm");

    private static SimpleDateFormat formatDDMMYYYY = new SimpleDateFormat("dd.MM.yyyy");

    private static SimpleDateFormat formatSQLTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat formatSQLDate = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * allowed alphanumeric characters for validateInput
     */
    private static final String allowedCharacters = "abcdefghijklmnopqrstuvwxyz���ABCDEFGHIJKLMNOPQRSTUVWXYZ���0123456789 \n-:;+.,?)(&%\"!@*><";

    /**
     * allowed alphanumeric characters for validateInputLM
     */
    private static final String alphanumericCharacters = "abcdefghijklmnopqrstuvwxyz���ABCDEFGHIJKLMNOPQRSTUVWXYZ���0123456789";

    /**
     * allowed alphanumeric characters for validateOnlyNumbers
     */
    private static final String numericCharacters = "0123456789-";

    /**
     * allowed characters in email address
     */
    private static final String VALID_EMAIL_CHARS = "abcdefghijklmnopqrstuyvwxzABCDEFGHIJKLMNOPQRSTUYVWXYZ0123456789@._-";

    /**
     * allowed characters in file name and application name
     */
    private static final String VALID_APPLICATION_FILE_NAME_CHARS = "abcdefghijklmnopqrstuyvwxzABCDEFGHIJKLMNOPQRSTUYVWXYZ0123456789 _.!#$'(),-;@";

    /**
     * allowed characters in file name and application name
     */
    private static final String VALID_FILE_NAME_CHARS = "abcdefghijklmnopqrstuyvwxzABCDEFGHIJKLMNOPQRSTUYVWXYZ0123456789_.!#$'(),-;@";

    /**
     * allowed characters in first name, last name and organization name
     */
    private static final String VALID_NAME_CHARS = "abcdefghijklmnopqrstuyvwxzABCDEFGHIJKLMNOPQRSTUYVWXYZ0123456789_.!#$'(),-;@";

    /**
     * allowed characters in phone number
     */
    private static final String VALID_PHONE_CHARS = "0123456789-+ ";

    /**
     * for testing purposes
     *
     * @author Kimmo Saarinen, Cidercone
     */
    public static void main(String[] args) {
        String command = args[0];
        String input = args[1];
        if (command.equalsIgnoreCase("md5")) {
            String output = hexEncode(md5(input));
            System.out.println(output + " (length = " + output.length() + ")");
        } else if (command.equalsIgnoreCase("validate")) {
            String output = validateInput(input);
            System.out.println(output);
        } else {
            System.out.println("syntax:");
            System.out.println("  java StringUtil <command> <input>");
            System.out.println("  <command>: md5, validate");
        }
    }

    /**
     * Encode String with MD5 algorithm
     *
     * @param input input String to be encoded
     * @return encoded String
     * @author Kimmo Saarinen, Cidercone
     */
    public static String md5(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] output = md.digest(input.getBytes());
            return new String(output, "ISO-8859-1");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encode String to hexadecimal format
     *
     * @param input String to be encoded
     * @return encoded String
     * @author Kimmo Saarinen, Cidercone
     */
    public static String hexEncode(String input) {
        if (input == null) return null;
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < input.length(); i++) {
            String hex = Integer.toHexString(input.charAt(i));
            output.append(hex);
        }
        return output.toString();
    }

    /**
     * formats timestamp into string (DD.MM.YYYY)
     *
     * @param param timestamp to be changed
     */
    public static String timestampToDDMMYYYY(Timestamp param) {
        if (param == null) return "Invalid Date";
        try {
            return formatDDMMYYYY.format(param);
        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    /**
     * formats timestamp into sql date string (YYYY-MM-DD)
     *
     * @param param timestamp to be changed
     */
    public static String timestampToSQLDate(Timestamp param) {
        if (param == null) return null;
        try {
            return formatSQLDate.format(param);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * formats calendar into sql date string (YYYY-MM-DD)
     *
     * @param param timestamp to be changed
     */
    public static String calendarToSQLDate(GregorianCalendar cal) {
        if (cal == null) return null;
        try {
            return StringUtil.timestampToSQLDate(new Timestamp(cal.getTime().getTime()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * formats timestamp into string (DD.MM.YYYY)
     *
     * @param param timestamp to be changed
     */
    public static String timestampToDDMMYYYY(GregorianCalendar cal) {
        if (cal == null) return null;
        try {
            return formatDDMMYYYY.format(cal.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * converts String object to int, and if String cannot be parsed (or is
     * null), returns a default int value.
     *
     * @param input        input String to be parsed
     * @param defaultValue default int value to return on error
     * @return int representation on input String, or default value on error
     */
    public static int stringToInt(String input, int defaultValue) {
        if (input == null) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * converts String object to long, and if String cannot be parsed (or is
     * null), returns a default long value.
     *
     * @param input        input String to be parsed
     * @param defaultValue default long value to return on error
     * @return long representation on input String, or default value on error
     */
    public static long stringToLong(String input, long defaultValue) {
        if (input == null) return defaultValue;
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * removes any non-alphanumeric character from input. this method can be
     * used eg. validating user login / password form input parameters. eg.
     * validateInput("./=q..-w&&ert--Y!!1-23") returns "qwertY123"
     *
     * @param input input String to validate
     * @return validated output String
     * @author Kimmo Saarinen, Cidercone
     */
    public static String validateInputLN(String input) {
        if (input == null) return null;
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < output.length(); i++) {
            boolean found = false;
            for (int j = 0; j < alphanumericCharacters.length(); j++) {
                if (output.charAt(i) == alphanumericCharacters.charAt(j)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                output.deleteCharAt(i);
                i--;
            }
        }
        return output.toString();
    }

    /**
     * validates the application file name.
     *
     * @param input input String to validate
     * @return true if the input is valid
     */
    public static boolean validateFileName(String input) {
        boolean ok = true;
        if (input == null) {
            return false;
        }
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < output.length(); i++) {
            boolean found = false;
            for (int j = 0; j < VALID_FILE_NAME_CHARS.length(); j++) {
                if (output.charAt(i) == VALID_FILE_NAME_CHARS.charAt(j)) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                ok = false;
                break;
            }
        }
        return ok;
    }

    /**
     * validates the application name.
     *
     * @param input input String to validate
     * @return true if the input is valid
     */
    public static boolean validateApplicationFileName(String input) {
        boolean ok = true;
        if (input == null) {
            return false;
        }
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < output.length(); i++) {
            boolean found = false;
            for (int j = 0; j < VALID_APPLICATION_FILE_NAME_CHARS.length(); j++) {
                if (output.charAt(i) == VALID_APPLICATION_FILE_NAME_CHARS.charAt(j)) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                ok = false;
                break;
            }
        }
        return ok;
    }

    /**
     * removes any non-alphanumeric character from input. this method can be
     * used eg. validating user login / password form input parameters. eg.
     * validateInput("./=q..-w&&ert--Y!!1-23") returns "qwertY123"
     *
     * @param input input String to validate
     * @return validated output String
     * @author Kimmo Saarinen, Cidercone
     */
    public static String validateInput(String input) {
        if (input == null) return null;
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < output.length(); i++) {
            boolean found = false;
            for (int j = 0; j < allowedCharacters.length(); j++) {
                if (output.charAt(i) == allowedCharacters.charAt(j)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                output.deleteCharAt(i);
                i--;
            }
        }
        return output.toString();
    }

    /**
     * removes any non-numeric character from input. this method can be used eg.
     * validating phone numbers input parameters.
     *
     * @param input input String to validate
     * @return validated output String
     */
    public static String validateOnlyNumbers(String input) {
        if (input == null) return null;
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < output.length(); i++) {
            boolean found = false;
            for (int j = 0; j < numericCharacters.length(); j++) {
                if (output.charAt(i) == numericCharacters.charAt(j)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                output.deleteCharAt(i);
                i--;
            }
        }
        return output.toString();
    }

    /**
     * returns filename to be without prefix directories, e.g.
     * "c:\windows\foo.txt" returns "foo.txt" e.g. "/opt/bar/foo.txt" returns
     * "foo.txt"
     */
    public static String getFilenameFromPath(String filename) {
        if (filename == null) return null;
        int k = filename.lastIndexOf("\\");
        if (k == -1) k = filename.lastIndexOf("/");
        if (k != -1) filename = filename.substring(k + 1, filename.length());
        return filename;
    }

    public static String simpleEncode(String data, int maxSize) {
        if (data == null) {
            return null;
        }
        if (data.length() > 15) {
            data = StringUtils.substring(data, 0, 15) + "...";
        }
        return simpleEncode(data);
    }

    public static String simpleEncode(String data) {
        if (data == null) {
            return null;
        }
        final StringBuffer buf = new StringBuffer();
        final char[] chars = data.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            buf.append("&#" + (int) chars[i]);
        }
        return buf.toString();
    }

    public static String formatString(String value) {
        if (StringUtils.isBlank(value)) {
            return "-";
        }
        return simpleEncode(value);
    }

    /**
     * removes unwished characters from the text
     *
     * @param source text
     */
    public static String encodeHtmlAndSQL(String source) {
        if (source == null) return null;
        String text = source;
        text = replaceString(text, "<", "&lt;");
        text = replaceString(text, ">", "&gt;");
        text = replaceString(text, "\\", "/");
        text = replaceString(text, "\"", "&quot;");
        text = replaceString(text, "\'", "&quot;");
        text = replaceString(text, "\n", "<br>");
        return text;
    }

    /**
     * removes html tags
     *
     * @param source text
     */
    public static String decodeHtmlAndSQL(String source) {
        if (source == null) return null;
        String text = source;
        text = replaceString(text, "&lt;", "<");
        text = replaceString(text, "&gt;", ">");
        text = replaceString(text, "&quot;", "\"");
        text = replaceString(text, "&quot;", "\'");
        text = replaceString(text, "<br>", "\n");
        text = replaceString(text, "</a>", "");
        return text;
    }

    /**
     * Replaces match with newData and returns new string
     *
     * @param source  input string
     * @param match   string to be searched
     * @param newData string to be replaced for 'match'
     * @return the new string
     */
    public static String replaceString(String source, String match, String newData) {
        if (source == null) return null;
        if (match == null) return source;
        if (newData == null) newData = "";
        while (source.indexOf(match) >= 0) {
            int begin = source.indexOf(match);
            int end = match.length() + begin;
            if (begin >= 0 && end >= 0) {
                source = source.substring(0, begin) + newData + source.substring(end);
            }
        }
        return source;
    }

    /**
     * Replaces match with newData and returns new StringBuffer
     *
     * @param source  input StringBuffer
     * @param match   string to be searched
     * @param newData string to be replaced for 'match'
     * @return the new StringBuffer
     */
    public static StringBuffer replaceString(StringBuffer source, String match, String newData) {
        if (source == null) return null;
        if (match == null) return source;
        if (newData == null) newData = "";
        while (source.indexOf(match) >= 0) {
            int begin = source.indexOf(match);
            int end = match.length() + begin;
            if (begin >= 0 && end >= 0) {
                source.replace(begin, end, newData);
            }
        }
        return source;
    }

    /**
     * converts String to String for sql insert/updates. e.g. "abc'def" ->
     * "'abcdef'" e.g. null -> "null"
     */
    public static String sqlWrapper(String s) {
        if (s == null) return NULL;
        s = replaceString(s, "\'", "");
        return '\'' + s + '\'';
    }

    /**
     * converts Timestamp to String for sql insert/updates.
     */
    public static String sqlWrapper(Timestamp param) {
        if (param == null) return NULL;
        try {
            return '\'' + formatSQLTimestamp.format(param) + '\'';
        } catch (Exception e) {
            return NULL;
        }
    }

    public static String removeNull(String s) {
        return (s == null || s.equalsIgnoreCase("null")) ? "" : s.trim();
    }

    /**
     * validate email address
     *
     * @param email address
     * @return true/false
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.length() < 6) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [null or too short]");
            return false;
        }
        int len = email.length();
        int at = email.indexOf('@');
        if (at < 1 || at > (len - 5)) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [@ at wrong place or missing]");
            return false;
        }
        int dot = email.lastIndexOf('.');
        if (dot == -1 || dot < (at + 2)) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [. at wrong place or missing]");
            return false;
        }
        if (dot > (len - 2)) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [. at the end]");
            return false;
        }
        if (email.indexOf('@', at + 1) != -1) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [too many @ signs]");
            return false;
        }
        for (int i = 0; i < email.length(); i++) if (VALID_EMAIL_CHARS.indexOf(email.charAt(i)) == -1) {
            Log.debug(Log.GENERAL, "validateEmail(" + email + ") returns false [invalid character " + email.charAt(i) + "]");
            return false;
        }
        return true;
    }

    /**
     * Validates parameter for csv
     *
     * @param s
     * @return validated string
     */
    public static String validateForCsv(String s) {
        if (s == null) return "";
        if (s.indexOf(';') == -1 && s.indexOf(',') == -1 && s.indexOf('"') == -1 && s.indexOf('\n') == -1 && s.indexOf('\r') == -1) return s;
        StringBuffer b = new StringBuffer();
        b.append('"');
        if (s.indexOf('"') != -1 || s.indexOf('\n') != -1 || s.indexOf('\r') != -1) {
            char[] c = s.toCharArray();
            for (int i = 0; i < c.length; i++) {
                if (c[i] == '"') {
                    b.append('"');
                    b.append(c[i]);
                } else if (c[i] == '\n' || c[i] == '\r') {
                    b.append(" ");
                } else {
                    b.append(c[i]);
                }
            }
        } else b.append(s);
        b.append('"');
        return b.toString();
    }

    /**
     * parse String with StringTokenizer to create a ArrayList of tokens
     * delimetered by one of the characters specified with the delim parameter.
     * <p/>
     * example: makeArrayList("foo, bar, rock,,cool,fun") generates a ArrayList
     * with objects "foo", "bar", "rock", "cool" and "fun".
     *
     * @param input input string to be tokenized
     * @param delim delimeter characters
     * @return ArrayList of String objects
     */
    public static ArrayList makeArrayList(String input, String delim) {
        ArrayList al = new ArrayList();
        if (input == null || delim == null) return al;
        StringTokenizer tokens = new StringTokenizer(input, delim);
        while (tokens.hasMoreTokens()) {
            al.add(tokens.nextToken().trim());
        }
        return al;
    }

    /**
     * Formats number, where locale is US, number of desimal digits are exactly
     * 2 and no grouping is used Excamples: 1500 => 1500.00 2.555 => 2.56
     *
     * @param strNumber number to format as String
     * @return formatted number as String
     */
    public static String formatNumber(String strNumber) {
        return formatNumber(Double.parseDouble(strNumber), Locale.US, 2, 2, false);
    }

    /**
     * Checks is string Null or empty. Note whitespaces are removed before
     * checking
     *
     * @param str
     * @return
     */
    public static boolean isNullOrEmpty(String str) {
        boolean reply = false;
        if (str == null || str.trim().length() == 0) {
            reply = true;
        }
        return reply;
    }

    /**
     * Formats number, where locale is US, number of desimal digits are exactly
     * 2 and no grouping is used Excamples: 1500 => 1500.00 2.555 => 2.56
     *
     * @param double number to format
     * @return formatted number as String
     */
    public static String formatNumber(double value) {
        return formatNumber(value, Locale.US, 2, 2, false);
    }

    /**
     * Formats number
     *
     * @param double                number to format
     * @param locale                locale to use
     * @param maximumFractionDigits maximum number of fraction digits
     * @param minimumFractionDigits minimum number of fraction digits
     * @param bGrouping             true if grouping is used (ie. thousand separator)
     * @return formatted number as String
     */
    public static String formatNumber(double value, Locale locale, int maximumFractionDigits, int minimumFractionDigits, boolean bGrouping) {
        NumberFormat nf = NumberFormat.getInstance(locale);
        nf.setMaximumFractionDigits(maximumFractionDigits);
        nf.setMinimumFractionDigits(minimumFractionDigits);
        nf.setGroupingUsed(bGrouping);
        return nf.format(value);
    }

    /**
     * Formats file size to human readable format (kB or MB) Example: 1250500 =>
     * 1.2MB
     *
     * @param size file size in bytes
     * @return formatted file size as String
     */
    public static String formatFileSize(int size) {
        double value = 0.0;
        String s = "";
        if (size < 10240) {
            s = formatNumber(size / 1024.D, Locale.US, 1, 1, false) + " kB";
        } else if (size >= 10240 && size < 1048576) {
            s = formatNumber(size / 1024.0D, Locale.US, 0, 0, false) + " kB";
        } else {
            s = formatNumber(size / 1048576.0D, Locale.US, 1, 1, false) + " MB";
        }
        return s;
    }

    public static String toHexString(int value) {
        String vStr = Integer.toHexString(value).toUpperCase();
        if (vStr != null) {
            String prefix = "0x";
            for (int i = vStr.length(); i < 8; i++) {
                prefix += "0";
            }
            return prefix + vStr;
        }
        return vStr;
    }

    public static String toHexString(long value) {
        String vStr = Long.toHexString(value).toUpperCase();
        if (vStr != null) {
            String prefix = "0x";
            for (int i = vStr.length(); i < 8; i++) {
                prefix += "0";
            }
            return prefix + vStr;
        }
        return vStr;
    }

    /**
     * Adds the items of the String list to a string separated with ","
     *
     * @param items A list of strings
     * @return A string containing the items
     */
    public static String itemsToString(List items) {
        String result = "";
        for (int i = 0; items != null && i < items.size(); i++) {
            if (i != 0) result += ", ";
            result += items.get(i);
        }
        return result;
    }

    /**
     * Validates password Strings.
     *
     * @param password Password to be validated
     * @return Returns error String in the case of error; null otherwise
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() == 0) {
            return "Password was empty";
        }
        if (password.length() < 8) {
            return "Password was too short. It must be at least eight (8) characters long.";
        }
        if (password.length() > 50) {
            return "Password was too long. It's maximum length is 50 characters";
        }
        int letterCount = 0;
        int digitCount = 0;
        int otherCount = 0;
        for (int i = 0; i < password.length(); i++) {
            char chr = password.charAt(i);
            if (Character.isLetter(chr)) {
                letterCount++;
            } else if (Character.isDigit(chr)) {
                digitCount++;
            } else {
                otherCount++;
            }
        }
        if (letterCount == 0) {
            return "Password did not contain normal characters";
        }
        if (digitCount == 0 && otherCount == 0) {
            return "Password did not contain digits/special characters";
        }
        for (int i = 1; i < (password.length() - 1); i++) {
            char chr = password.charAt(i);
            if (password.charAt(i - 1) == chr && password.charAt(i + 1) == chr) {
                return "Password can not contain more than two (2) sequential characters";
            }
        }
        return null;
    }

    /**
     * Utility method for Strings displayed on html tables, etc. Null values are
     * removed Input string is spawned on multiple rows if its length exceeds
     * param cellMaxLength
     *
     * @param str           the input string
     * @param cellMaxLength the maximum length for the cell (area) the string is
     *                      displayed on
     * @return A string spawned on multiple lines
     */
    public static String displayStringOnList(String str, int cellMaxLength) {
        str = removeNull(str);
        String returnStr = "";
        int index = 0;
        if (str.length() > 0 && str.length() > cellMaxLength) {
            while (index < str.length()) {
                if (index < cellMaxLength) {
                    returnStr += str.substring(index, cellMaxLength) + " \n ";
                    index += cellMaxLength;
                } else {
                    returnStr += str.substring(index, str.length());
                    index += str.length();
                }
            }
        } else returnStr = str;
        return returnStr;
    }

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * This method accepts a string and removes any html tags in it.
     * added by: Phesto Enock  02.04.2008
     * @param anyString
     * @return returns a string with removed html tags if any
     */
    public static String removeHtmlTags(String anyString) {
        String noHTMLString = anyString;
        try {
            if (!noHTMLString.equalsIgnoreCase("") && noHTMLString != null) {
                noHTMLString = anyString.replaceAll("[\\p{Punct}&&[^@]&&[^-]&&[^.]&&[^_]]", "");
            }
        } catch (Exception e) {
            Log.debug(Log.GENERAL, "RemoveHTMLTags(" + anyString + ") produces " + e);
        }
        return noHTMLString;
    }
}
