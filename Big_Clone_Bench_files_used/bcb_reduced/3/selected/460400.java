package br.org.databasetools.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;

public class StringUtils {

    public static String brokenStrings(String value, String token, int count, String separator) {
        StringBuilder sb = new StringBuilder();
        String[] str = value.split(token);
        for (int i = 0; i < str.length; i++) {
            sb.append(str[i]);
            if ((i % count) == 0) {
                sb.append("\n");
            } else {
                if (i < str.length - 1) sb.append(separator);
            }
        }
        return sb.toString();
    }

    public static String brokenStrings(String value, int max) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (value.length() >= max) {
                String temp = value.substring(0, max);
                sb.append(temp);
                sb.append("\n");
                value = value.substring(max);
                continue;
            } else {
                sb.append(value);
            }
            break;
        }
        return sb.toString();
    }

    public static String getFirstWord(String word) {
        int pos = word.indexOf(" ");
        if (pos != -1) {
            return word.substring(0, pos);
        }
        return word;
    }

    public static boolean isContainsNumbers(String word) {
        if (word == null) return false;
        String numbers = "0123456789";
        for (int i = 0; i < word.length(); i++) {
            String str = word.substring(i, i + 1);
            if (numbers.contains(str) == true) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNumberOnly(String word) {
        if (word == null) return false;
        String numbers = "0123456789";
        for (int i = 0; i < word.length(); i++) {
            String str = word.substring(i, i + 1);
            if (numbers.contains(str) == false) {
                return false;
            }
        }
        return true;
    }

    public static String getNumberOnly(String word) {
        if (word == null) return null;
        String numbers = "0123456789";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < word.length(); i++) {
            String str = word.substring(i, i + 1);
            if (numbers.contains(str)) {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    public static String reverse(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.reverse();
        return sb.toString();
    }

    public static long getNumber(String word) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < word.length(); i++) {
            try {
                String str = word.substring(i, i + 1);
                Long.parseLong(str);
                sb.append(str);
            } catch (Exception ex) {
            }
        }
        Long result = new Long(sb.toString());
        return result;
    }

    public static String fillZeroLeft(String str, int count) {
        return fillLeft(str, '0', count);
    }

    public static String fillZeroRight(String str, int count) {
        return fillRigth(str, '0', count);
    }

    public static String encripty(String word) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        BigInteger hash = new BigInteger(1, md.digest(word.getBytes()));
        String s = hash.toString(16);
        if (s.length() % 2 != 0) s = "0" + s;
        return s;
    }

    public static String fillLeft(String str, char charFill, int length) {
        StringBuffer result = new StringBuffer();
        if (str.length() >= length) {
            return result.append(str.substring(0, length)).toString();
        } else {
            result.append(str);
            int qtde = length - str.length();
            for (int i = 0; i < qtde; i++) {
                result.insert(0, charFill);
            }
            return result.toString();
        }
    }

    public static String fillLeft(long str, char charFill, int length) {
        return fillLeft(str + "", charFill, length);
    }

    public static String fillLeft(int str, char charFill, int length) {
        return fillLeft(str + "", charFill, length);
    }

    public static String fillLeft(double str, char charFill, int length) {
        return fillLeft(str + "", charFill, length);
    }

    public static String fillRigth(String str, char charFill, int length) {
        StringBuffer result = new StringBuffer();
        if (str.length() >= length) {
            return result.append(str.substring(0, length)).toString();
        } else {
            result.append(str);
            for (int i = result.length(); i < length; i++) {
                result.insert(i, charFill);
            }
            return result.toString();
        }
    }

    public static String fillCenter(String str, char charFill, int length) {
        String newstr = new String(str);
        if (str.length() < length) {
            int left = ((length - newstr.length()) / 2);
            newstr = fillLeft(newstr, charFill, (length - left));
            newstr = fillRigth(newstr, charFill, length);
        }
        return newstr;
    }

    public static String centerText(String text, int length) {
        return fillCenter(text, ' ', length);
    }

    public static String tirarAcentos(String text) {
        String result = text;
        result = result.replaceAll("[����]", "e");
        result = result.replaceAll("[����]", "u");
        result = result.replaceAll("[����]", "i");
        result = result.replaceAll("[�����]", "a");
        result = result.replaceAll("[�����]", "o");
        result = result.replaceAll("[����]", "E");
        result = result.replaceAll("[����]", "U");
        result = result.replaceAll("[����]", "I");
        result = result.replaceAll("[�����]", "A");
        result = result.replaceAll("[�����]", "O");
        result = result.replaceAll("�", "c");
        result = result.replaceAll("�", "C");
        return result;
    }
}
