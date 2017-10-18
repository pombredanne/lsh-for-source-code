package org.chartsy.main.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 *
 * @author Viorel
 */
public final class StringUtil {

    public static final UppercaseDocumentFilter UPPERCASE_DOCUMENT_FILTER = new UppercaseDocumentFilter();

    private StringUtil() {
    }

    public static String stringBetween(String input, String start, String end) {
        int beginIndex = input.indexOf(start);
        int endIndex = input.indexOf(end, beginIndex);
        return input.substring(beginIndex + start.length(), endIndex);
    }

    public static String md5(String password) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            return hash.toString(16);
        } catch (NoSuchAlgorithmException ex) {
            return password;
        }
    }

    public static class UppercaseDocumentFilter extends DocumentFilter {

        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            fb.insertString(offset, text.toUpperCase(), attr);
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            fb.replace(offset, length, text.toUpperCase(), attrs);
        }
    }
}
