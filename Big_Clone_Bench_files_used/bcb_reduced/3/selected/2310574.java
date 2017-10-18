package adv.tools;

import adv.language.Constants;
import adv.web.UserFilter;
import ognl.*;
import ognlscript.FileParser;
import ognlscript.Line;
import ognlscript.block.StringDigester;
import ognlscript.block.OgnlExpression;
import ognlscript.OgnlscriptCompileException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 30-sep-2006
 * Time: 13:17:11
 * To change this template use File | Settings | File Templates.
 */
public class TextTools implements Constants {

    private static final Pattern COMPRESSPIPES = Pattern.compile("\\s{0,}\\|\\s{0,}");

    private static final Pattern CRCONVERPATTERN = Pattern.compile(CRREGEXPATTERN);

    private static final Pattern TABCONVERPATTERN = Pattern.compile(TABREGEXPATTERN);

    public static void main(String[] args) throws OgnlException {
        String text = "00 0   0  0 0   1| 2 1 |2 1 | 2 | 3";
        System.out.println(text);
        text = COMPRESSPIPES.matcher(text).replaceAll("|");
        System.out.println(text);
        System.out.println(escapeSingleQuote("'"));
    }

    public static String renderHtmlLine(String line) {
        if (line == null) return "";
        line = CRCONVERPATTERN.matcher(line).replaceAll(CRTO);
        line = TABCONVERPATTERN.matcher(line).replaceAll(TABTO);
        return line;
    }

    public static String renderSystemLine(String line) {
        if (line == null) return "";
        line = CRCONVERPATTERN.matcher(line).replaceAll("\n");
        line = TABCONVERPATTERN.matcher(line).replaceAll("\t");
        return line;
    }

    public static String compressPipes(String line) {
        if (line == null) return "";
        return COMPRESSPIPES.matcher(line).replaceAll("|").trim();
    }

    public static int quotedIndexOf(String line, String searching) {
        return quotedIndexOf(line, searching, 0);
    }

    public static int quotedIndexOf(String line, String searching, int startPos) {
        return quotedIndexOf(line, searching, startPos, line.length());
    }

    public static int quotedIndexOf(String line, String searching, int startPos, int endPos) {
        boolean escaping = false;
        int matching = 0;
        char lastQuote = 0;
        for (int i = startPos; i < endPos; i++) {
            char c = line.charAt(i);
            if (escaping) {
                escaping = false;
            } else {
                if (lastQuote == 0 && c == searching.charAt(matching)) {
                    matching++;
                    if (matching == searching.length()) {
                        return i - searching.length() + 1;
                    }
                } else {
                    matching = 0;
                    if (c == '\\') {
                        escaping = true;
                    } else if (c == '"' && lastQuote == '"') {
                        lastQuote = 0;
                    } else if (c == '"' && lastQuote == 0) {
                        lastQuote = '"';
                    } else if (c == '\'' && lastQuote == '\'') {
                        lastQuote = 0;
                    } else if (c == '\'' && lastQuote == 0) {
                        lastQuote = '\'';
                    } else if (c == searching.charAt(matching)) {
                    }
                }
            }
        }
        return -1;
    }

    public static void validateName(String s) throws Exception {
        validateName(s, null);
    }

    public static String extraChars = "";

    public static void validateNumName(String s) throws Exception {
        validateNumName(s, "");
    }

    public static void validateNumName(String s, String more) throws Exception {
        validateName(s, "0123456789" + more);
        try {
            if (s.length() > 0) Integer.parseInt(s.substring(0, 1));
            throw new Exception("Detactado valor numerico. Se permiten numeros, pero en combinacion con letras.");
        } catch (NumberFormatException e) {
        }
    }

    public static boolean isUpper(int c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLower(int c) {
        return c >= 'a' && c <= 'z';
    }

    public static void validateName(String s, String more) throws Exception {
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (isUpper(c) || isLower(c) || (extraChars.indexOf(c) > -1) || (more != null && more.indexOf(c) > -1)) {
                ;
            } else {
                if (more == null) {
                    more = "";
                }
                throw new Exception("Caracter no valido: [" + ((char) c) + "]. Permitidos: [A-Z][a-z]" + ((extraChars + more).length() > 0 ? "[" + extraChars + more + "]" : ""));
            }
        }
    }

    public static String printCollection(Collection col) {
        return printCollection(col, ",");
    }

    public static String printCollection(Collection col, String delim) {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = col.iterator(); i.hasNext(); ) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static String printMap(Map m) {
        return printMap(m, ":", ",");
    }

    public static String printMap(Map m, String sepKeyFromValue, String sepPairs) {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry o = (Map.Entry) i.next();
            sb.append(o.getKey()).append(sepKeyFromValue).append(printValue(o.getValue()));
            if (i.hasNext()) {
                sb.append(sepPairs);
            }
        }
        return sb.toString();
    }

    public static String printValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return String.valueOf(value);
        }
    }

    public static final StringDigester CLEAN = new StringDigester() {

        public String digest(String line) {
            line = FileParser.trimSpaces(line);
            if (line.endsWith(";")) {
                line = line.substring(0, line.length() - 1);
            }
            return line;
        }
    };

    /**
     * Limpia el fichero
     *
     * @param lines
     * @throws ognlscript.OgnlscriptRuntimeException
     */
    public static void prepare(List<Line> lines) {
        FileParser.prepare(lines, JOINLINECHAR, STARTSINGLECOMMENTLINE, STARTMULTICOMMENT, ENDMULTICOMMENT, CLEAN);
    }

    public static String printStackTrace(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        BufferedReader br = new BufferedReader(new StringReader(stringWriter.toString()));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            boolean add = true;
            while ((line = br.readLine()) != null) {
                if (line.contains(UserFilter.class.getName() + ".doFilter")) {
                    add = false;
                    sb.append(line).append("\n");
                    continue;
                } else {
                    if (!add && !line.startsWith("\tat")) {
                        add = true;
                    }
                }
                if (add) {
                    sb.append(line).append("\n");
                } else {
                }
            }
        } catch (IOException e1) {
            ;
        }
        return sb.toString();
    }

    public static String replicate(String s, int i) {
        if (i == 0) {
            return "";
        }
        StringBuilder ss = new StringBuilder();
        for (int n = 0; n < i; n++) {
            ss.append(s);
        }
        return ss.toString();
    }

    public static void print(String text, File out, String encoding) throws IOException {
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), encoding), true);
        print(new StringReader(text), printer);
        printer.close();
    }

    public static void print(Reader r, PrintWriter out) throws IOException {
        BufferedReader br = new BufferedReader(r);
        try {
            String line = br.readLine();
            while (line != null) {
                out.println(line);
                out.flush();
                line = br.readLine();
            }
        } finally {
            if (br != null) br.close();
        }
    }

    private static final Pattern emailPattern = Pattern.compile("^(([A-Za-z0-9]+_+)|([A-Za-z0-9]+\\-+)|([A-Za-z0-9]+\\.+)|([A-Za-z0-9]+\\++))*[A-Za-z0-9]+@((\\w+\\-+)|(\\w+\\.))*\\w{1,63}\\.[a-zA-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validateEmail(String email) {
        Matcher matcher = emailPattern.matcher(email);
        return matcher.matches();
    }

    public static String getMD5(String s) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.toLowerCase().getBytes());
            return HexString.bufferToHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error grave al inicializar MD5");
            e.printStackTrace();
            return "!!";
        }
    }

    public static String getSHA1(String s) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(s.toLowerCase().getBytes());
            return HexString.bufferToHex(sha1.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error grave al inicializar SHA1");
            e.printStackTrace();
            return "!!";
        }
    }

    private static final Pattern escapeSingleQuotePattern = Pattern.compile("\\'");

    public static String escapeSingleQuote(String x) {
        return escapeSingleQuotePattern.matcher(x).replaceAll("\\\\'");
    }

    public static boolean validateUrl(String web) {
        if (!web.startsWith("http://")) {
            web = "http://" + web;
        }
        try {
            URL url = new URL(web);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static int findSeparator(String line) {
        int p = line.indexOf("=");
        return p;
    }

    public static List<Throwable> getStackList(Throwable e) {
        List<Throwable> causes = new ArrayList<Throwable>();
        causes.add(e);
        e = e.getCause();
        while (e != null) {
            causes.add(e);
            e = e.getCause();
        }
        return causes;
    }

    public static String read(Reader is) throws IOException {
        StringWriter sw = new StringWriter(512);
        TextTools.print(is, new PrintWriter(sw));
        sw.flush();
        return sw.getBuffer().toString();
    }

    public static Properties load(File propFile, Properties properties, String encoding) throws IOException {
        if (properties == null) {
            properties = new Properties();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(propFile), encoding));
        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            int pos = line.indexOf("=");
            if (!line.startsWith("#") && pos >= 1) {
                String key = line.substring(0, pos).trim();
                String value = line.substring(pos + 1).trim();
                properties.setProperty(key, value);
            }
            line = br.readLine();
        }
        br.close();
        return properties;
    }

    public static void store(File propFile, Properties properties, String encoding) throws IOException {
        List<Line> lines = FileParser.toList(new InputStreamReader(new FileInputStream(propFile), encoding));
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(propFile), encoding), true);
        Properties cached = (Properties) properties.clone();
        for (Line myLine : lines) {
            String line = myLine.getText().trim();
            int pos = line.indexOf("=");
            if (!line.startsWith("#") && pos >= 1) {
                String key = line.substring(0, pos).trim();
                if (cached.containsKey(key)) {
                    String newValue = (String) cached.remove(key);
                    line = key + " = " + newValue;
                }
            }
            printer.println(line);
        }
        for (Object key : cached.keySet()) {
            Object value = cached.get(key);
            printer.println(key + " = " + value);
        }
        printer.close();
    }

    public static final String[] SINGLE_TAGS = { "br", "p", "hr" };

    public static String replace(String source, String search, String replace) {
        int pos = source.indexOf(search);
        while (pos > -1) {
            source = source.substring(0, pos) + replace + source.substring(pos + search.length());
            pos = source.indexOf(search);
        }
        return source;
    }

    public static String replaceVars(String data, Map<String, Object> values) {
        try {
            ASTConst expr = new ASTConst(0);
            expr.setValue(data);
            String parsed2 = (String) expr.getValue(new OgnlContext(values), values);
            return parsed2 != null ? parsed2 : data;
        } catch (OgnlException e) {
            return e.getMessage() + " " + data;
        }
    }
}
