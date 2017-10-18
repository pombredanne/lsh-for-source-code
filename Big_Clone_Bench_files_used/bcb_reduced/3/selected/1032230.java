package org.opentides.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.opentides.InvalidImplementationException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class StringUtil {

    private static String zeros = "0000000000";

    private static Random random = new Random((new Date()).getTime());

    private static Logger _log = Logger.getLogger(StringUtil.class);

    public static boolean isEmpty(String obj) {
        if ((obj == null) || (obj.trim().length() == 0)) return true; else return false;
    }

    /**
     * Escapes special characters for HQL.
     * @param str
     * @param escapeForLike
     */
    public static String escapeSql(String str, boolean escapeForLike) {
        if (str == null) {
            return null;
        } else {
            str = StringUtils.replace(str, "'", "''");
            if (escapeForLike) {
                str = StringUtils.replace(str, "\\", "\\\\\\\\");
                str = StringUtils.replace(str, "%", "\\%");
                str = StringUtils.replace(str, "_", "\\_");
            } else {
                str = StringUtils.replace(str, "\\", "\\\\");
            }
        }
        return str;
    }

    public static String toFixedString(int value, int length) {
        String val = Integer.toString(value);
        int diff = length - val.length();
        if (diff > 0) return (zeros.substring(10 - diff) + val); else return val;
    }

    public static String removeHTMLTags(String html) {
        return html.replaceAll("<(.*?)>", " ").replaceAll("\\s+", " ");
    }

    public static int convertToInt(String str, int defValue) {
        int value = defValue;
        try {
            value = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
        }
        return value;
    }

    public static long convertToLong(String str, long defValue) {
        long value = defValue;
        try {
            value = Long.parseLong(str);
        } catch (NumberFormatException nfe) {
        }
        return value;
    }

    public static Double convertToDouble(String str, double defValue) {
        Double doub = defValue;
        try {
            doub = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
        }
        return doub;
    }

    /**
     * generates an alphanumeric string based on specified length.
     * @param length # of characters to generate
     * @return random string
     */
    public static String generateRandomString(int length) {
        char[] values = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        String out = "";
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(values.length);
            out += values[idx];
        }
        return out;
    }

    /**
	 * Encrypt password by using SHA-256 algorithm, encryptedPassword length is 32 bits
	 * @param clearTextPassword
	 * @return
	 * @throws NoSuchAlgorithmException
	 * reference http://java.sun.com/j2se/1.4.2/docs/api/java/security/MessageDigest.html
	 */
    public static String getEncryptedPassword(String clearTextPassword) {
        if (StringUtil.isEmpty(clearTextPassword)) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clearTextPassword.getBytes());
            return new sun.misc.BASE64Encoder().encode(md.digest());
        } catch (NoSuchAlgorithmException e) {
            _log.error("Failed to encrypt password.", e);
        }
        return "";
    }

    /**
     * Generates a hash code for a given source code. 
     * This method ignores whitespace in generating the hash code.
     * @param source
     * @return
     */
    public static String hashSourceCode(String source) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            return new sun.misc.BASE64Encoder().encode(md.digest());
        } catch (NoSuchAlgorithmException e) {
            _log.error("Failed to generate hashcode.", e);
        }
        return null;
    }

    /**
     * Encrypts the string along with salt 
     * @param userId
     * @return
     * @throws Exception
     */
    public static String encrypt(String userId) {
        BASE64Encoder encoder = new BASE64Encoder();
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return encoder.encode(salt) + encoder.encode(userId.getBytes());
    }

    /**
	 * Decrypts the string and removes the salt
	 * @param encryptKey
	 * @return
	 * @throws Exception
	 */
    public static String decrypt(String encryptKey) {
        if (!StringUtil.isEmpty(encryptKey) && encryptKey.length() > 12) {
            String cipher = encryptKey.substring(12);
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                return new String(decoder.decodeBuffer(cipher));
            } catch (IOException e) {
                throw new InvalidImplementationException("Failed to perform decryption for key [" + encryptKey + "]", e);
            }
        } else return null;
    }

    /**
	 * Parse a line of text in standard CSV format and returns array of Strings
	 * @param csvLine
	 * @return
	 */
    public static List<String> parseCsvLine(String csvLine) {
        return StringUtil.parseCsvLine(csvLine, ',', '"', '\\', false);
    }

    /**
	 * Parse a line of text in CSV format and returns array of Strings
	 * Implementation of parsing is extracted from open-csv.
	 * http://opencsv.sourceforge.net/
	 * 
	 * @param csvLine
	 * @param separator
	 * @param quotechar
	 * @param escape
	 * @param strictQuotes
	 * @return
	 * @throws IOException
	 */
    public static List<String> parseCsvLine(String csvLine, char separator, char quotechar, char escape, boolean strictQuotes) {
        List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(50);
        boolean inQuotes = false;
        for (int i = 0; i < csvLine.length(); i++) {
            char c = csvLine.charAt(i);
            if (c == escape) {
                boolean isNextCharEscapable = inQuotes && csvLine.length() > (i + 1) && (csvLine.charAt(i + 1) == quotechar || csvLine.charAt(i + 1) == escape);
                if (isNextCharEscapable) {
                    sb.append(csvLine.charAt(i + 1));
                    i++;
                }
            } else if (c == quotechar) {
                boolean isNextCharEscapedQuote = inQuotes && csvLine.length() > (i + 1) && csvLine.charAt(i + 1) == quotechar;
                if (isNextCharEscapedQuote) {
                    sb.append(csvLine.charAt(i + 1));
                    i++;
                } else {
                    inQuotes = !inQuotes;
                    if (!strictQuotes) {
                        if (i > 2 && csvLine.charAt(i - 1) != separator && csvLine.length() > (i + 1) && csvLine.charAt(i + 1) != separator) {
                            sb.append(c);
                        }
                    }
                }
            } else if (c == separator && !inQuotes) {
                tokensOnThisLine.add(sb.toString());
                sb = new StringBuilder(50);
            } else {
                if (!strictQuotes || inQuotes) sb.append(c);
            }
        }
        if (inQuotes) {
            _log.warn("Un-terminated quoted field at end of CSV line. \n [" + csvLine + "]");
        }
        if (sb != null) {
            tokensOnThisLine.add(sb.toString());
        }
        return tokensOnThisLine;
    }

    /**
     * Combines an array of string into one string using the specified separator.
     * @param separator
     * @param input
     * @return
     */
    public static final String explode(char separator, String[] input) {
        if (input == null) return null;
        int count = 0;
        StringBuffer out = new StringBuffer();
        for (String word : input) {
            if (count++ > 0) out.append(separator);
            out.append(word);
        }
        return out.toString();
    }

    @Deprecated
    public static String dateToString(Date obj, String format) {
        if (obj == null) return "";
        SimpleDateFormat dtFormatter = new SimpleDateFormat(format);
        return dtFormatter.format(obj);
    }

    @Deprecated
    public static Date stringToDate(String strDate, String format) throws ParseException {
        SimpleDateFormat dtFormatter = new SimpleDateFormat(format);
        if (StringUtil.isEmpty(strDate)) throw new ParseException("Cannot convert empty string to Date.", 0);
        return dtFormatter.parse(strDate.trim());
    }

    @Deprecated
    public static Date convertFlexibleDate(String strDate, String[] formats) throws ParseException {
        if (StringUtil.isEmpty(strDate)) return null;
        for (int i = 0; i < formats.length; i++) {
            try {
                SimpleDateFormat dtFormatter = new SimpleDateFormat(formats[i]);
                dtFormatter.setLenient(false);
                return dtFormatter.parse(strDate.trim());
            } catch (ParseException e) {
            }
        }
        throw new ParseException("No matching date format for " + strDate, 0);
    }

    @Deprecated
    public static String convertShortDate(Date obj) {
        return dateToString(obj, "yyyyMMdd");
    }

    @Deprecated
    public static Date convertShortDate(String str) throws ParseException {
        return stringToDate(str, "yyyyMMdd");
    }

    @Deprecated
    public static Date convertShortDate(String str, Date defaultDate) {
        try {
            return stringToDate(str, "yyyyMMdd");
        } catch (ParseException pex) {
            return defaultDate;
        }
    }

    @Deprecated
    public static Date convertFlexibleDate(String strDate) throws ParseException {
        if (StringUtil.isEmpty(strDate)) throw new ParseException("Cannot convert empty string to Date.", 0);
        String[] formats = { "MM/dd/yyyy", "MM-dd-yyyy", "yyyyMMdd", "yyyy-MM-dd", "MMM dd yyyy", "MMM dd, yyyy", "MMM yyyy", "MM/yyyy", "MM-yyyy", "yyyy" };
        return convertFlexibleDate(strDate, formats);
    }

    @Deprecated
    public static boolean compareNullableDates(Date date1, Date date2) {
        if ((date1 == null) && (date2 == null)) return true;
        if (date1 != null) {
            if (date1.equals(date2)) return true; else return false;
        }
        return false;
    }
}
