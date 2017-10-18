package com.googlecode.inutils4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class MyStringUtils {

    public static final int APPEND = 1;

    public static final int FILL = 2;

    protected static final NumberFormat _ffmt = NumberFormat.getInstance();

    protected static final String XLATE = "0123456789abcdef";

    protected static String LINE_SEPARATOR;

    static {
        _ffmt.setMinimumIntegerDigits(1);
        _ffmt.setMinimumFractionDigits(1);
        _ffmt.setMaximumFractionDigits(2);
        LINE_SEPARATOR = "\n";
        try {
            LINE_SEPARATOR = System.getProperty("line.separator");
        } catch (Exception localException) {
        }
    }

    public static List<String> regexTest(Pattern pattern, String str) {
        Matcher match = pattern.matcher(str);
        List retorno = new ArrayList();
        while (match.find()) {
            retorno.add(match.group());
        }
        return retorno;
    }

    public static String regexFindFirst(String pattern, String str) {
        return regexFindFirst(Pattern.compile(pattern), str, 1);
    }

    public static String regexFindFirst(String pattern, String str, int group) {
        return regexFindFirst(Pattern.compile(pattern), str, group);
    }

    public static String regexFindFirst(Pattern pattern, String str, int group) {
        Matcher match = pattern.matcher(str);
        if (match.find()) {
            return match.group(group);
        }
        return null;
    }

    public static List<String> deleteIfContains(List<String> a, String str) {
        List<String> retorno = new ArrayList<String>();
        for (String line : a) {
            if (!(line.contains(str))) {
                retorno.add(line);
            }
        }
        return retorno;
    }

    public static List<String> deleteIfNotContains(List<String> a, String str) {
        List<String> retorno = new ArrayList<String>();
        for (String line : a) {
            if (line.contains(str)) {
                retorno.add(line);
            }
        }
        return retorno;
    }

    public static List<String> mixStringLists(List<String> a, List<String> b) {
        String str;
        List<String> retorno = new ArrayList<String>();
        for (Iterator<String> localIterator = a.iterator(); localIterator.hasNext(); ) {
            str = localIterator.next();
            retorno.add(str);
        }
        for (Iterator<String> localIterator = b.iterator(); localIterator.hasNext(); ) {
            str = localIterator.next();
            retorno.add(str);
        }
        return retorno;
    }

    public static List mixLists(List a, List b) {
        Object str;
        List retorno = new ArrayList();
        for (Iterator localIterator = a.iterator(); localIterator.hasNext(); ) {
            str = localIterator.next();
            retorno.add(str);
        }
        for (Iterator localIterator = b.iterator(); localIterator.hasNext(); ) {
            str = localIterator.next();
            retorno.add(str);
        }
        return retorno;
    }

    public static String removerAcentos(String s) {
        s = s.replaceAll("[��]", "a");
        s = s.replaceAll("[����]", "e");
        s = s.replaceAll("[��]", "i");
        s = s.replaceAll("[����]", "o");
        s = s.replaceAll("[��]", "u");
        s = s.replaceAll("�", "C");
        s = s.replaceAll("�", "c");
        s = s.replaceAll("[��]", "A");
        s = s.replaceAll("[����]", "E");
        s = s.replaceAll("[��]", "I");
        s = s.replaceAll("[��]", "O");
        s = s.replaceAll("[��]", "U");
        return s;
    }

    public static List<List<String>> split(List<String> list, int count) {
        List retorno = new ArrayList();
        return retorno;
    }

    public static Set<String> fixList(FixType type, Collection<String> list) {
        Comparator comparator = null;
        if (type == FixType.DELETEREPEATED) {
            comparator = new Unique();
        }
        if (type == FixType.ALPHABETICAL) {
            comparator = new Sort();
        }
        if (type == FixType.ALPHABETICALDELETEREPEATED) {
            comparator = new UniqueSort();
        }
        Set retorno = new TreeSet(comparator);
        for (String str : list) {
            retorno.add(str);
        }
        return retorno;
    }

    public static boolean saveToFile(int type, Collection<String> list, String file) {
        BufferedWriter out = null;
        try {
            if (type == 1) out = new BufferedWriter(new FileWriter(file, true)); else {
                out = new BufferedWriter(new FileWriter(file));
            }
            for (String str : list) {
                out.write(str + "\r\n");
            }
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getContentListSplit(File file, String delimiter) {
        List retorno = new ArrayList();
        String content = getContent(file);
        for (String str : content.split(delimiter)) {
            retorno.add(str);
        }
        return retorno;
    }

    public static List<String> asListLines(String file) {
        List retorno = new ArrayList();
        file = file.replace("\r\n", "\n");
        file = file.replace("\n", "\r\n");
        for (String str : file.split("\r\n")) {
            retorno.add(str);
        }
        return retorno;
    }

    public static String getContent(File file) {
        StringBuffer buffer = new StringBuffer();
        if (file.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(file));
                while (in.ready()) {
                    String linha = in.readLine();
                    buffer.append(linha + "\r\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    public static String getContent(HttpClient client, String url) {
        String retorno = "";
        try {
            GetMethod httpget = new GetMethod(url);
            client.executeMethod(httpget);
            try {
                String linha;
                InputStream is = httpget.getResponseBodyAsStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                StringBuffer out = new StringBuffer();
                while ((linha = in.readLine()) != null) {
                    out.append(linha).append("\r\n");
                }
                retorno = out.toString();
            } finally {
                httpget.releaseConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retorno;
    }

    public static String getContent(String url) {
        return getContent(new HttpClient(), url);
    }

    @Deprecated
    public static boolean blank(String value) {
        return isBlank(value);
    }

    public static boolean isBlank(String value) {
        int ii = 0;
        for (int ll = (value == null) ? 0 : value.length(); ii < ll; ++ii) {
            if (!(Character.isWhitespace(value.charAt(ii)))) {
                return false;
            }
        }
        return true;
    }

    public static String trim(String value) {
        return ((value == null) ? null : value.trim());
    }

    public static String deNull(String value) {
        return ((value == null) ? "" : value);
    }

    public static String truncate(String s, int maxLength) {
        return truncate(s, maxLength, "");
    }

    public static String truncate(String s, int maxLength, String append) {
        if ((s == null) || (s.length() <= maxLength)) {
            return s;
        }
        return s.substring(0, maxLength - append.length()) + append;
    }

    public static String capitalize(String s) {
        if (isBlank(s)) {
            return s;
        }
        char c = s.charAt(0);
        if (Character.isUpperCase(c)) {
            return s;
        }
        return String.valueOf(Character.toUpperCase(c)) + s.substring(1);
    }

    public static String toUSLowerCase(String s) {
        return ((isBlank(s)) ? s : s.toLowerCase(Locale.US));
    }

    public static String toUSUpperCase(String s) {
        return ((isBlank(s)) ? s : s.toUpperCase(Locale.US));
    }

    public static String sanitize(String source, CharacterValidator validator) {
        if (source == null) {
            return null;
        }
        int nn = source.length();
        StringBuilder buf = new StringBuilder(nn);
        for (int ii = 0; ii < nn; ++ii) {
            char c = source.charAt(ii);
            if (validator.isValid(c)) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static String replace(String source, String before, String after) {
        int pos = source.indexOf(before);
        if (pos == -1) {
            return source;
        }
        StringBuilder sb = new StringBuilder(source.length() + 32);
        int blength = before.length();
        int start = 0;
        while (pos != -1) {
            sb.append(source.substring(start, pos));
            sb.append(after);
            start = pos + blength;
            pos = source.indexOf(before, start);
        }
        sb.append(source.substring(start));
        return sb.toString();
    }

    public static String pad(String value, int width) {
        if (width <= 0) throw new IllegalArgumentException("Pad width must be greater than zero.");
        if (value.length() >= width) {
            return value;
        }
        return value + spaces(width - value.length());
    }

    public static String prepad(String value, int width) {
        if (width <= 0) throw new IllegalArgumentException("Pad width must be greater than zero.");
        if (value.length() >= width) {
            return value;
        }
        return spaces(width - value.length()) + value;
    }

    public static String spaces(int count) {
        return fill(' ', count);
    }

    public static String fill(char c, int count) {
        char[] sameChars = new char[count];
        Arrays.fill(sameChars, c);
        return new String(sameChars);
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException localNumberFormatException) {
        }
        return false;
    }

    public static String format(float value) {
        return _ffmt.format(value);
    }

    public static String format(double value) {
        return _ffmt.format(value);
    }

    public static String coordsToString(int x, int y) {
        StringBuilder buf = new StringBuilder();
        coordsToString(buf, x, y);
        return buf.toString();
    }

    public static void coordsToString(StringBuilder buf, int x, int y) {
        if (x >= 0) {
            buf.append("+");
        }
        buf.append(x);
        if (y >= 0) {
            buf.append("+");
        }
        buf.append(y);
    }

    public static String encode(String s) {
        try {
            return ((s != null) ? URLEncoder.encode(s, "UTF-8") : null);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("UTF-8 is unknown in this Java.");
        }
    }

    public static String decode(String s) {
        try {
            return ((s != null) ? URLDecoder.decode(s, "UTF-8") : null);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("UTF-8 is unknown in this Java.");
        }
    }

    public static String hexlate(byte[] bytes, int count) {
        if (bytes == null) {
            return "";
        }
        count = Math.min(count, bytes.length);
        char[] chars = new char[count * 2];
        for (int i = 0; i < count; ++i) {
            int val = bytes[i];
            if (val < 0) {
                val += 256;
            }
            chars[(2 * i)] = "0123456789abcdef".charAt(val / 16);
            chars[(2 * i + 1)] = "0123456789abcdef".charAt(val % 16);
        }
        return new String(chars);
    }

    public static String hexlate(byte[] bytes) {
        return ((bytes == null) ? "" : hexlate(bytes, bytes.length));
    }

    public static byte[] unhexlate(String hex) {
        if ((hex == null) || (hex.length() % 2 != 0)) {
            return null;
        }
        hex = hex.toLowerCase();
        byte[] data = new byte[hex.length() / 2];
        for (int ii = 0; ii < hex.length(); ii += 2) {
            int value = (byte) ("0123456789abcdef".indexOf(hex.charAt(ii)) << 4);
            value += "0123456789abcdef".indexOf(hex.charAt(ii + 1));
            data[(ii / 2)] = (byte) value;
        }
        return data;
    }

    public static String md5hex(String source) {
        return digest("MD5", source);
    }

    public static String sha1hex(String source) {
        return digest("SHA-1", source);
    }

    public static byte[] parseByteArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        byte[] vals = new byte[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Byte.parseByte(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static short[] parseShortArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        short[] vals = new short[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Short.parseShort(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static int[] parseIntArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        int[] vals = new int[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Integer.parseInt(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static long[] parseLongArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        long[] vals = new long[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Long.parseLong(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static float[] parseFloatArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        float[] vals = new float[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Float.parseFloat(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static double[] parseDoubleArray(String source) {
        StringTokenizer tok = new StringTokenizer(source, ",");
        double[] vals = new double[tok.countTokens()];
        for (int i = 0; tok.hasMoreTokens(); ++i) {
            try {
                vals[i] = Double.parseDouble(tok.nextToken().trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return vals;
    }

    public static String[] parseStringArray(String source) {
        return parseStringArray(source, false);
    }

    public static String[] parseStringArray(String source, boolean intern) {
        int tcount = 0;
        int tpos = -1;
        int tstart = 0;
        if (source.length() == 0) {
            return new String[0];
        }
        source = replace(source, ",,", "%COMMA%");
        while ((tpos = source.indexOf(",", tpos + 1)) != -1) {
            ++tcount;
        }
        String[] tokens = new String[tcount + 1];
        tpos = -1;
        tcount = 0;
        while ((tpos = source.indexOf(",", tpos + 1)) != -1) {
            tokens[tcount] = source.substring(tstart, tpos);
            tokens[tcount] = replace(tokens[tcount].trim(), "%COMMA%", ",");
            if (intern) {
                tokens[tcount] = tokens[tcount].intern();
            }
            tstart = tpos + 1;
            ++tcount;
        }
        tokens[tcount] = source.substring(tstart);
        tokens[tcount] = replace(tokens[tcount].trim(), "%COMMA%", ",");
        return tokens;
    }

    public static String join(Object[] values) {
        return join(values, false);
    }

    public static String join(Object[] values, boolean escape) {
        return join(values, ", ", escape);
    }

    public static String join(Object[] values, String separator) {
        return join(values, separator, false);
    }

    public static String joinEscaped(String[] values) {
        return join(values, true);
    }

    public static String[] split(String source, String sep) {
        if (isBlank(source)) {
            return new String[0];
        }
        int tcount = 0;
        int tpos = -1;
        int tstart = 0;
        while ((tpos = source.indexOf(sep, tpos + 1)) != -1) {
            ++tcount;
        }
        String[] tokens = new String[tcount + 1];
        tpos = -1;
        tcount = 0;
        while ((tpos = source.indexOf(sep, tpos + 1)) != -1) {
            tokens[tcount] = source.substring(tstart, tpos);
            tstart = tpos + 1;
            ++tcount;
        }
        tokens[tcount] = source.substring(tstart);
        return tokens;
    }

    public static String toMatrixString(int[] values, int colCount, int fieldWidth) {
        StringBuilder buf = new StringBuilder();
        StringBuilder valbuf = new StringBuilder();
        for (int i = 0; i < values.length; ++i) {
            valbuf.setLength(0);
            valbuf.append(values[i]);
            int spaces = fieldWidth - valbuf.length();
            for (int s = 0; s < spaces; ++s) {
                buf.append(" ");
            }
            buf.append(valbuf);
            if ((i % colCount != colCount - 1) || (i == values.length - 1)) continue;
            buf.append(LINE_SEPARATOR);
        }
        return buf.toString();
    }

    public static String intervalToString(long millis) {
        StringBuilder buf = new StringBuilder();
        boolean started = false;
        long days = millis / 86400000L;
        if (days != 0L) {
            buf.append(days).append("d ");
            started = true;
        }
        long hours = millis / 3600000L % 24L;
        if ((started) || (hours != 0L)) {
            buf.append(hours).append("h ");
        }
        long minutes = millis / 60000L % 60L;
        if ((started) || (minutes != 0L)) {
            buf.append(minutes).append("m ");
        }
        long seconds = millis / 1000L % 60L;
        if ((started) || (seconds != 0L)) {
            buf.append(seconds).append("s ");
        }
        buf.append(millis % 1000L).append("m");
        return buf.toString();
    }

    public static String shortClassName(Object object) {
        return ((object == null) ? "null" : shortClassName(object.getClass()));
    }

    public static String shortClassName(Class<?> clazz) {
        return shortClassName(clazz.getName());
    }

    public static String shortClassName(String name) {
        int didx = name.lastIndexOf(".");
        if (didx == -1) {
            return name;
        }
        didx = name.lastIndexOf(".", didx - 1);
        if (didx == -1) {
            return name;
        }
        return name.substring(didx + 1);
    }

    public static String unStudlyName(String name) {
        boolean seenLower = false;
        StringBuilder nname = new StringBuilder();
        int nlen = name.length();
        for (int i = 0; i < nlen; ++i) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (seenLower) {
                    nname.append("_");
                }
                seenLower = false;
                nname.append(c);
            } else {
                seenLower = true;
                nname.append(Character.toUpperCase(c));
            }
        }
        return nname.toString();
    }

    public static String wordWrap(String str, int width) {
        int size = str.length();
        StringBuilder buf = new StringBuilder(size + size / width);
        int lastidx = 0;
        while (lastidx < size) {
            if (lastidx + width >= size) {
                buf.append(str.substring(lastidx));
                break;
            }
            int lastws = lastidx;
            int ii = lastidx;
            for (int ll = lastidx + width; ii < ll; ++ii) {
                char c = str.charAt(ii);
                if (c == '\n') {
                    buf.append(str.substring(lastidx, ii + 1));
                    lastidx = ii + 1;
                    break;
                }
                if (Character.isWhitespace(c)) {
                    lastws = ii;
                }
            }
            if (lastws == lastidx) {
                buf.append(str.substring(lastidx, lastidx + width)).append(LINE_SEPARATOR);
                lastidx += width;
            } else if (lastws > lastidx) {
                buf.append(str.substring(lastidx, lastws)).append(LINE_SEPARATOR);
                lastidx = lastws + 1;
            }
        }
        return buf.toString();
    }

    protected static String join(Object[] values, String separator, boolean escape) {
        StringBuilder buf = new StringBuilder();
        int vlength = values.length;
        for (int i = 0; i < vlength; ++i) {
            if (i > 0) {
                buf.append(separator);
            }
            String value = (values[i] == null) ? "" : values[i].toString();
            buf.append((escape) ? replace(value, ",", ",,") : value);
        }
        return buf.toString();
    }

    protected static String digest(String codec, String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance(codec);
            return hexlate(digest.digest(source.getBytes()));
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(codec + " codec not available");
        }
    }

    public abstract static interface CharacterValidator {

        public abstract boolean isValid(char paramChar);
    }

    public static class Formatter {

        public String toString(Object object) {
            return ((object == null) ? "null" : object.toString());
        }

        public String getOpenBox() {
            return "(";
        }

        public String getCloseBox() {
            return ")";
        }
    }

    public static class Sort implements Comparator<String> {

        public int compare(String arg0, String arg1) {
            if (arg0.equals(arg1)) {
                return 1;
            }
            return arg0.compareTo(arg1);
        }
    }

    static class Unique implements Comparator<String> {

        public int compare(String arg0, String arg1) {
            if (arg0.equals(arg1)) {
                return 0;
            }
            return 1;
        }
    }

    public static class UniqueSort implements Comparator<String> {

        public int compare(String arg0, String arg1) {
            return arg0.compareTo(arg1);
        }
    }

    public static String cleanCodeText(String text) {
        text = text.trim();
        text = text.replace("\t", " ");
        text = text.replace("       ", " ").trim();
        text = text.replace("      ", " ").trim();
        text = text.replace("     ", " ").trim();
        text = text.replace("    ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("    ", " ").trim();
        text = text.replace("    ", " ").trim();
        text = text.replace("    ", " ").trim();
        text = text.replace("  ", " ").trim();
        text = text.replace("    ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("  ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("   ", " ").trim();
        text = text.replace("  ", " ").trim();
        text = text.replace("  ", " ").trim();
        text = text.replace("   ", " ").trim();
        return text.trim();
    }

    public static String fileHash(File f) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String digest = getDigest(new FileInputStream(f), md, 2048);
        return digest;
    }

    public static String getDigest(InputStream is, MessageDigest md, int byteArraySize) throws NoSuchAlgorithmException, IOException {
        md.reset();
        byte[] bytes = new byte[byteArraySize];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            md.update(bytes, 0, numBytes);
        }
        byte[] digest = md.digest();
        String result = new String(Hex.encode(digest));
        is.close();
        return result;
    }
}
