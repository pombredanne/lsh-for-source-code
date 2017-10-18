package pub.utils;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.regexp.RE;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

/** A set of utility functions that deal with Strings */
public class StringUtils {

    /** internal counter for the gensym() static function.
     */
    private static long __gensym_counter;

    static {
        __gensym_counter = 0;
    }

    /** Creates a unique symbolic identifier, prefixed with the prefix
     * 'g'.  Useful for macro-writing code.  */
    public static String gensym() {
        return gensym("g");
    }

    /** Given a java.io.InputStream, pulls all the content out and
     * returns it as a String. */
    public static String readInputStream(java.io.InputStream is) throws java.io.IOException {
        return readReader(new java.io.InputStreamReader(is));
    }

    public static String readReader(java.io.Reader reader) throws java.io.IOException {
        StringBuffer buffer = new StringBuffer();
        java.io.BufferedReader r = new java.io.BufferedReader(reader);
        String nextLine;
        while ((nextLine = r.readLine()) != null) {
            buffer.append(nextLine);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /** Creates a unique symbolic identifier, prefixed with 'prefix'.
     * Useful for macro-writing code.  */
    public static String gensym(String prefix) {
        __gensym_counter++;
        return prefix + __gensym_counter;
    }

    /**
     * Unlike normal String.substring(), this will bound stop to the
     * length of the string.
     */
    public static String safeSubstring(String s, int start, int len) {
        if (s == null) {
            s = "";
        }
        return s.substring(start, Math.min(len, s.length() - start));
    }

    /** Given exception e, tries to extract a string of its stack trace. */
    public static String getStackTrace(Throwable e) {
        try {
            java.io.StringWriter swriter = new java.io.StringWriter();
            java.io.PrintWriter pwriter = new java.io.PrintWriter(swriter);
            e.printStackTrace(pwriter);
            pwriter.close();
            swriter.close();
            return swriter.toString();
        } catch (java.io.IOException e2) {
            Log.getLogger(pub.utils.StringUtils.class).warn(e2);
            return e.toString();
        }
    }

    public static boolean looksLikeAgiName(String s) {
        if (s == null) {
            return false;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^at.g\\d{5}\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        return p.matcher(s).find();
    }

    /**
     * Joins a list of strings together using a delimiter 'delim'.
     */
    public static String join(String delim, java.util.List v) {
        if (v.size() == 0) {
            return "";
        }
        if (v.size() == 1) {
            return "" + v.get(0);
        }
        StringBuffer result = new StringBuffer("" + v.get(0));
        for (int i = 1; i < v.size(); i++) {
            result.append(delim);
            result.append("" + v.get(i));
        }
        return result.toString();
    }

    /**
     * Joins an array of strings together using a delimiter 'delim'.
     */
    public static String join(String delim, Object[] v) {
        return join(delim, java.util.Arrays.asList(v));
    }

    /**
     *  Returns true if the string looks like an integer.  Done by
     *  using the "It's easier to ask forgiveness than permission"
     *  idiom, although we should probably use regular expressions
     *  instead.
     */
    public static boolean looksLikeInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /** Capitalizes a word: first character is uppercased, and the
     * rest is lowercased.
     */
    public static String capitalize(String s) {
        if (s.equals("")) {
            return "";
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /** Uppercases the first letter in s */
    public static String uppercaseFirst(String s) {
        if (s.equals("")) {
            return "";
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * returns true if s1 is a substring of s2
     */
    public static boolean isSubstringOf(String s1, String s2) {
        return s2.indexOf(s1) != -1;
    }

    /**
     * Removes all leading and trailing whitespace from a string, and
     * also removes all newlines within the string. */
    public static String superStrip(String s) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch == '\n') || (ch == '\r')) {
                b.append(" ");
            } else {
                b.append(ch);
            }
        }
        return b.toString().trim();
    }

    /** Given a delimiter delim and string s, return a list of Strings,
     * where we break between delims.
     */
    public static String[] split(char delim, String s) {
        if (s.length() == 0) {
            return new String[0];
        }
        s = s + delim;
        java.util.ArrayList l = new java.util.ArrayList();
        int i = 0;
        int j = 0;
        while (j < s.length()) {
            if (s.charAt(j) == delim) {
                l.add(s.substring(i, j));
                i = j + 1;
            }
            j++;
        }
        Object[] items = l.toArray();
        String[] results = new String[items.length];
        for (int k = 0; k < items.length; k++) {
            results[k] = (String) items[k];
        }
        return results;
    }

    /**
     * Given a delimiter delim and string s, return a list of Strings,
     * where we split across adjacent whitespace.
     */
    public static java.util.List split(String s) {
        if (s.length() == 0) {
            return new java.util.ArrayList();
        }
        s = s + ' ';
        java.util.List l = new java.util.ArrayList();
        int i = 0;
        int j = 0;
        while (j < s.length()) {
            if ((s.charAt(j) == ' ') || (s.charAt(j) == '\n') || (s.charAt(j) == '\r') || (s.charAt(j) == '\t')) {
                if (i != j) {
                    l.add(s.substring(i, j));
                }
                i = j + 1;
            }
            j++;
        }
        return l;
    }

    public static String indent(String s, int level) {
        String[] lines = split('\n', s);
        java.util.ArrayList indented_lines = new java.util.ArrayList();
        String indent_str = "";
        for (int i = 0; i < level; i++) {
            indent_str += " ";
        }
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 0) {
                indented_lines.add(indent_str + lines[i]);
            } else {
                indented_lines.add(lines[i]);
            }
        }
        return join("\n", indented_lines.toArray());
    }

    /**
     * Transforms html-sensitive characters into harmless entities.
     */
    public static String htmlEntityQuote(String str) {
        StringBuffer result = new StringBuffer();
        char next_char;
        for (int i = 0; i < str.length(); i++) {
            next_char = str.charAt(i);
            if (next_char == '&') {
                result.append("&amp;");
            } else if (next_char == '<') {
                result.append("&lt;");
            } else if (next_char == '"') {
                result.append("&quot;");
            } else if (next_char == '\'') {
                result.append("&#39;");
            } else {
                result.append(next_char);
            }
        }
        return result.toString();
    }

    /**
     * A filter to transform the empty string to the null string, and
     * otherwise pass other strings through.
     */
    public static String emptyStringToNull(String s) {
        if ((s != null) && s.trim().equals("")) {
            return null;
        } else {
            return s;
        }
    }

    /**
     * A filter to transform null to the empty string, and otherwise
     * pass other strings through.
     */
    public static String nullToEmptyString(String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    /** Reads all the text from a java.io.Reader and returns it as a String
     * Warning: possibly memory intensive, if 'r' contains a lot of text.
     */
    public static String read(java.io.Reader r) throws java.io.IOException {
        java.io.BufferedReader bufferedReader = new java.io.BufferedReader(r);
        String line;
        StringBuffer stringBuffer = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line);
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }

    /**
     * Pulls a parameter from a servlet request.  If the result is set
     * to null, return the empty string instead.
     */
    public static String safeGetParameter(String parameter, HttpServletRequest request) {
        return safeGetParameter(parameter, request, "");
    }

    /**
     * Pulls a parameter from a servlet request.  If the result is set
     * to null or is otherwise empty, return the default parameter
     * instead.
     */
    public static String safeGetParameter(String parameter, HttpServletRequest request, String default_value) {
        if (isEmpty(request.getParameter(parameter))) {
            return default_value;
        } else {
            String returnString = request.getParameter(parameter);
            return superStrip(returnString);
        }
    }

    /**
     * Pulls a list of parameters from a servlet request, replacing
     * null values with empty strings.
     */
    public static String[] safeGetParameters(String parameter, HttpServletRequest request) {
        String[] values = request.getParameterValues(parameter);
        if (values == null) {
            return new String[0];
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                values[i] = "";
            }
            values[i] = superStrip(values[i]);
        }
        return values;
    }

    /**
     * Older versions of Java (1.3) did not have the getRequestURL()
     * function, so we use this instead.
     *
     * This method is accessible in our JSP classes through the
     * "tair:contextPath" tairtag.
     */
    public static String getBaseContextPath(HttpServletRequest request) {
        StringBuffer path = new StringBuffer();
        path.append(request.getScheme());
        path.append("://");
        path.append(request.getServerName());
        path.append(":");
        path.append(request.getServerPort());
        path.append(request.getContextPath());
        return path.toString();
    }

    public static String replace(String text, String from, String to) {
        try {
            RE re = new RE(quotemeta(from));
            return re.subst(text, to);
        } catch (org.apache.regexp.RESyntaxException e) {
            return from;
        }
    }

    /**
     * Returns the query-string portion of a string.
     */
    public static String getQueryString(String url) {
        int start = url.indexOf('?') + 1;
        return url.substring(start);
    }

    /**
     * Return the portion of the url up to where the query string begins.
     */
    public static String getURIString(String url) {
        int end = url.indexOf('?');
        return url.substring(0, end);
    }

    public static String substituteQueryParameter(String query, String name, String value) {
        String[] keyvalues = split('&', query);
        for (int i = 0; i < keyvalues.length; i++) {
            if (name.equals(keyvalues[i].substring(0, name.length()))) {
                keyvalues[i] = name + "=" + value;
            }
        }
        return join("&", keyvalues);
    }

    public static String removeQueryParameter(String query, String name) {
        String[] keyvalues = split('&', query);
        java.util.ArrayList new_keyvalues = new java.util.ArrayList();
        for (int i = 0; i < keyvalues.length; i++) {
            String query_name = keyvalues[i].substring(0, keyvalues[i].indexOf('='));
            if (name.equals(query_name)) {
                ;
            } else {
                new_keyvalues.add(keyvalues[i]);
            }
        }
        return join("&", new_keyvalues);
    }

    /** Construct a new URL where the unused parameter names are set to the empty string,
	unless they are used by the url.

	Example: obliterateUnusedParameters("http://tesuque/pub/Display?acc=42", 
	                                    Arrays.asList(new String[] {"acc", "type"}))
        should evaluate to "http://tesuque/pub/Display?acc=42&type="
    */
    public static String obliterateUnusedParameters(String url, List unusedParamNames) {
        String location = split('?', url)[0];
        Set paramsToKeep = new HashSet(collectParameterNames(url));
        StringBuffer buffer = new StringBuffer(url);
        boolean isFirstParameterSeen = (paramsToKeep.size() > 0);
        for (int i = 0; i < unusedParamNames.size(); i++) {
            if (paramsToKeep.contains(unusedParamNames.get(i))) {
                continue;
            }
            if (isFirstParameterSeen == false) {
                isFirstParameterSeen = true;
                buffer.append('?');
            } else {
                buffer.append('&');
            }
            buffer.append(unusedParamNames.get(i));
            buffer.append('=');
        }
        return buffer.toString();
    }

    private static List collectParameterNames(String url) {
        List collectedNames = new ArrayList();
        if (split('?', url).length > 1) {
            String query = split('?', url)[1];
            String[] keyvalues = split('&', query);
            for (int i = 0; i < keyvalues.length; i++) {
                collectedNames.add(keyvalues[i].substring(0, keyvalues[i].indexOf("=")));
            }
        }
        return collectedNames;
    }

    /**
     * Returns true if the given string is either "1", or "y", or "Y".
     */
    public static boolean isTrue(String s) {
        return ("1".equals(s) || "Y".equals(s) || "y".equals(s) || "t".equals(s) || "T".equals(s));
    }

    /**
     * Returns the URL used to initiate the request.
     */
    public static String getSelfUrl2(HttpServletRequest request) {
        StringBuffer url = javax.servlet.http.HttpUtils.getRequestURL(request);
        String query = request.getQueryString();
        if (query != null) {
            url.append("?");
            url.append(query);
        }
        return url.toString();
    }

    public static String[] parametersAsHiddenInputs(HttpServletRequest request) {
        List results = new ArrayList();
        Iterator keyIter = request.getParameterMap().keySet().iterator();
        while (keyIter.hasNext()) {
            String name = (String) keyIter.next();
            String values[] = request.getParameterValues(name);
            for (int i = 0; i < values.length; i++) {
                results.add("<input type=\"hidden\" name=\"" + protectFormValue(name) + "\" value=\"" + protectFormValue(values[i]) + "\">");
            }
        }
        return (String[]) results.toArray(new String[] {});
    }

    /**
     * Applies a limited form of SQL quotation on the term, and adds a
     * string prefix based on the 'method'.  Used for generating dynamic
     * SQL search queries.
     */
    public static String sqlQualify(String term, String method) {
        method = method.toUpperCase();
        if (method.equals("CONTAINS")) {
            return " like '%" + sqlQuote(term) + "%'";
        } else if (method.equals("STARTS")) {
            return " like '" + sqlQuote(term) + "%'";
        } else if (method.equals("ENDS")) {
            return " like '%" + sqlQuote(term) + "'";
        } else if (method.equals("EXCLUDES")) {
            return " not like '%" + sqlQuote(term) + "%'";
        } else {
            return " like '" + sqlQuote(term) + "'";
        }
    }

    /** Quotes a string for inserting into the database, escaping any
     * single quotes.
     */
    public static String sqlQuote(String s) {
        return StringUtils.replace(s, "'", "''");
    }

    /** Given a list of objects, returns a Javascript string
     * representation of that list.
     */
    public static String emitJavascriptArray(List l) {
        StringBuffer buffer = new StringBuffer();
        if (l.size() == 0) {
            return "[]";
        }
        buffer.append("[");
        for (int i = 0; i < l.size(); i++) {
            String next = "" + l.get(i);
            buffer.append("\"");
            buffer.append(replace(next, "\"", "\\\""));
            buffer.append("\"");
            if (i != l.size() - 1) {
                buffer.append(",");
            }
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Encodes a value to be put into a HTML parameter value field.
     */
    public static String cgiParamQuote(String s) {
        try {
            return java.net.URLEncoder.encode(s, "utf-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if s is null or if s is the word "null".
     */
    public static boolean isNull(String s) {
        return ((s == null) || s.equalsIgnoreCase("NULL"));
    }

    /**
     * Returns true if the string is empty, either by being the empty
     * string or by being null.
     */
    public static boolean isEmpty(String s) {
        return ((s == null) || s.equals("") || s.equalsIgnoreCase("null"));
    }

    /**
     * Returns true if the string is not empty.
     */
    public static boolean isNotEmpty(String s) {
        return (!isEmpty(s));
    }

    /**
     * Protect all potential regular expression characters in s.  This
     * mimics Perl's quotemeta() function.
     */
    public static String quotemeta(String s) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((('A' <= ch) && (ch <= 'Z')) || (('a' <= ch) && (ch <= 'z')) || (('0' <= ch) && (ch <= '9'))) {
                buffer.append(ch);
            } else {
                buffer.append("\\");
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    public static String parseAccessionGetType(String accession) {
        int index = accession.indexOf(":");
        if (index != -1) {
            return accession.substring(0, index);
        } else {
            return accession;
        }
    }

    public static String parseAccessionGetKey(String accession) {
        int index = accession.indexOf(":");
        if (index != -1) {
            return accession.substring(index + 1);
        } else {
            return "";
        }
    }

    /** Given a value that we expect within the "value" attribute of a
     * form input element, we replace any quote symbols with the
     * appropriate character entities. **/
    public static String protectFormValue(String value) {
        return replace(replace(value, "\"", "&quot;"), "'", "&#39;");
    }

    /**
     * trim white space from a given string
     * @return a new String without space
     */
    public static String trimWhitespace(String old) {
        StringBuffer returnval = new StringBuffer();
        for (int i = 0; i < old.length(); i++) {
            if (!Character.isWhitespace(old.charAt(i))) {
                returnval.append(old.charAt(i));
            }
        }
        return new String(returnval);
    }

    public static String trimNonLetter(String old) {
        StringBuffer returnval = new StringBuffer();
        for (int i = 0; i < old.length(); i++) {
            if (Character.isLetter(old.charAt(i))) {
                returnval.append(old.charAt(i));
            }
        }
        return new String(returnval);
    }

    /** Returns true if the string appears to look like a TIGR gene identifier, like AT1G01010. */
    public static boolean looksLikeTigrModelName(String s) {
        if (s == null) {
            return false;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("AT\\dG\\d{5}(\\.\\d)?", java.util.regex.Pattern.CASE_INSENSITIVE);
        return pattern.matcher(s).matches();
    }

    /** Truncate a string, and place '...' if a string is too long
     */
    public static String truncateWithDots(String s) {
        return truncateWithDots(s, 25);
    }

    public static String truncateWithDots(String s, int length) {
        if (isEmpty(s)) {
            return s;
        }
        if (s.length() > length) {
            return s.substring(0, length - "...".length()) + "...";
        }
        return s;
    }

    /**
     * convert a boolean value to string 
     * @param the boolean value 
     * @param char what format we want this boolean value convert to
     * acceptable char are : T Y t y
     * @return T/F Y/N t/f y/n 
     */
    public static char booleanToChar(boolean value, char returnChar) {
        if (!value) {
            switch(returnChar) {
                case 'T':
                    return 'F';
                case 'Y':
                    return 'N';
                case 't':
                    return 'f';
                case 'y':
                    return 'n';
            }
        }
        return returnChar;
    }

    public static String leftAlignedPaddedString(int input, int length, char padded) {
        String s = Integer.toString(input);
        if (s.length() > length) return s.substring(0, length); else if (s.length() < length) {
            char data[] = new char[length];
            for (int i = 0; i < length; i++) {
                data[i] = padded;
            }
            return (new String(data)).substring(0, length - s.length()) + s;
        } else return s;
    }

    public static String removeTrailingNonWordChars(String s) {
        int i = s.length() - 1;
        while (i >= 0) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                break;
            }
            i--;
        }
        i++;
        return s.substring(0, i);
    }

    public static String removeTrailingNonTitleChars(String s) {
        int i = s.length() - 1;
        while (i >= 0) {
            if (Character.isLetterOrDigit(s.charAt(i)) || Character.getType(s.charAt(i)) == Character.END_PUNCTUATION) {
                break;
            }
            i--;
        }
        i++;
        return s.substring(0, i);
    }

    /** Computes an md5 checksum for use in passwords.  The string is
     * guaranteed to be 32 characters long.
     @param passwd a String password to hash
     @return a 32-character wide string, the hexadecimal
     representation of the md5 sum, in lowercase.

     Copied-and-pasted from hibernate-pubsearch implementation.
     */
    public static String computeMd5Hex(String passwd) {
        if (passwd == null) throw new IllegalArgumentException();
        final int GUARANTEED_LENGTH = 32;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(passwd.getBytes());
            String output = new java.math.BigInteger(1, bytes).toString(16);
            while (output.length() < GUARANTEED_LENGTH) output = "0" + output;
            assert (output.length() == GUARANTEED_LENGTH);
            return output;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
