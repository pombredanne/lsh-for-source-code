package org.internna.ossmoney.util;

import java.util.Locale;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.springframework.util.Assert;
import org.apache.commons.codec.binary.Hex;
import org.internna.ossmoney.model.Subcategory;
import org.internna.ossmoney.model.security.UserDetails;
import static org.springframework.util.StringUtils.hasText;

public final class StringUtils {

    public static final String EMPTY = "";

    private static final Pattern emailPattern = Pattern.compile(UserDetails.EMAIL_REGEXP);

    private static final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);

    static {
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
    }

    /**
     * Default private constructor for a Utility class.
     */
    private StringUtils() {
        throw new AssertionError("Do not try to instantiate utility class");
    }

    /**
     * Try to build a hexadecimal string from an array of bytes.
     *
     * @param uuid a byte[]
     * @return A hexadecimal string or null
     */
    public static String toHex(byte[] uuid) {
        return uuid != null && uuid.length > 0 ? new String(Hex.encodeHex(uuid)) : null;
    }

    /**
     * Calculates SHA1 message digest.
     *
     * @param toEncode a non null non empty string
     * @return the calculated hash
     */
    public static String SHA1(String toEncode) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return encode(MessageDigest.getInstance("SHA-1"), null, toEncode);
    }

    /**
     * Calculates SHA1 message digest applying a salt.
     *
     * @param salt any 
     * @param toEncode a non null non empty string
     * @return the calculated hash
     */
    public static String SHA1(String salt, String toEncode) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return encode(MessageDigest.getInstance("SHA-1"), salt, toEncode);
    }

    private static String encode(MessageDigest digest, String salt, String toEncode) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Assert.hasText(toEncode);
        digest.reset();
        return toHex(digest.digest((hasText(salt) ? toEncode + "{" + salt + "}" : toEncode).getBytes("UTF-8")));
    }

    /**
     * Checks if a string has characters other than spaces.
     *
     * @param any any string
     * @return true if the string has no content
     */
    public static boolean isNullOrEmpty(String any) {
        return !hasText(any);
    }

    /**
     * Easy way to obtain an empty string when using the constant is not desirable.
     *
     * @return a non null empty string
     */
    public static String emptyString() {
        return EMPTY;
    }

    /**
     * Formats a number like 1,517.85
     *
     * @param number any
     * @return the formatted string
     */
    public static String format(BigDecimal number) {
        return number != null ? format(number.doubleValue()) : format((Double) null);
    }

    /**
     * Formats a number like 1,517.85
     *
     * @param number
     * @return
     */
    public static String format(Double number) {
        return number != null ? formatter.format(number) : "0.00";
    }

    public static InputStream asStream(String origin) {
        String value = hasText(origin) ? origin : EMPTY;
        try {
            return new ByteArrayInputStream(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * Tests the input is a valid email address
     *
     * @param email any
     * @return true if the input is a valid email address
     */
    public static boolean isEmailAddress(String email) {
        return hasText(email) && emailPattern.matcher(email).matches();
    }

    public static String getOperation(Subcategory subcategory) {
        return "category.investment.buy".equals(subcategory.getCategory()) ? "Buy" : "category.investment.sell".equals(subcategory.getCategory()) ? "Sell" : "IntInc";
    }
}
