package org.japano.util;

import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.japano.Buffer;

/**
 * Contains various static utility methods usefull for webapplications.
 *
 * @author Sven Helmberger ( sven dot helmberger at gmx dot de )
 * @version $Id: Util.java,v 1.27 2005/12/14 06:34:10 fforw Exp $
 * #SFLOGO#
 */
public class Util {

    private static Logger _log = null;

    /**
   * formatter for the detailedDate
   */
    private static SimpleDateFormat detailedDateFormat = new SimpleDateFormat("MM.dd.yyyy - HH:mm:ss.SSS");

    /** alphabet for {@link #encode(byte[])} */
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-.".toCharArray();

    /** Creates a new instance of Util */
    private Util() {
    }

    /** Converts illegal Characters to HTML entities.
   * The method tries to use named entities where possible. otherwise a numerical
   * entity is used.
   * @param text Text to encode
   * @param convertTags if <CODE>true</CODE>, HTML tags are readable as text ( &quot;&lt;b&gt;&quot;
   * becomes &quot;&amp;lt;b&amp;gt;&quot; ).
   *
   * @return encoded text
   */
    public static String htmlEncode(String text, boolean convertTags) {
        if (text == null) return "";
        StringBuffer out = new StringBuffer(text.length() * 2);
        char c;
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            if (c >= 128 || (convertTags && (c == '<' || c == '>' || c == '&' || c == '"') || c == '\'')) {
                switch(c) {
                    case 8222:
                    case 8220:
                        out.append('\"');
                        break;
                    case '\'':
                    case 8217:
                        out.append("&apos;");
                        break;
                    case '"':
                        out.append("&quot;");
                        break;
                    case '&':
                        out.append("&amp;");
                        break;
                    case '<':
                        out.append("&lt;");
                        break;
                    case '>':
                        out.append("&gt;");
                        break;
                    case ' ':
                        out.append("&nbsp;");
                        break;
                    case '¡':
                        out.append("&iexcl;");
                        break;
                    case '¢':
                        out.append("&cent;");
                        break;
                    case '£':
                        out.append("&pound;");
                        break;
                    case '¤':
                        out.append("&curren;");
                        break;
                    case '¥':
                        out.append("&yen;");
                        break;
                    case '¦':
                        out.append("&brvbar;");
                        break;
                    case '§':
                        out.append("&sect;");
                        break;
                    case '¨':
                        out.append("&uml;");
                        break;
                    case '©':
                        out.append("&copy;");
                        break;
                    case 'ª':
                        out.append("&ordf;");
                        break;
                    case '«':
                        out.append("&laquo;");
                        break;
                    case '¬':
                        out.append("&not;");
                        break;
                    case '­':
                        out.append("&shy;");
                        break;
                    case '®':
                        out.append("&reg;");
                        break;
                    case '¯':
                        out.append("&macr;");
                        break;
                    case '°':
                        out.append("&deg;");
                        break;
                    case '±':
                        out.append("&plusmn;");
                        break;
                    case '²':
                        out.append("&sup2;");
                        break;
                    case '³':
                        out.append("&sup3;");
                        break;
                    case '´':
                        out.append("&acute;");
                        break;
                    case 'µ':
                        out.append("&micro;");
                        break;
                    case '¶':
                        out.append("&para;");
                        break;
                    case '·':
                        out.append("&middot;");
                        break;
                    case '¸':
                        out.append("&cedil;");
                        break;
                    case '¹':
                        out.append("&sup1;");
                        break;
                    case 'º':
                        out.append("&ordm;");
                        break;
                    case '»':
                        out.append("&raquo;");
                        break;
                    case '¼':
                        out.append("&frac14;");
                        break;
                    case '½':
                        out.append("&frac12;");
                        break;
                    case '¾':
                        out.append("&frac34;");
                        break;
                    case '¿':
                        out.append("&iquest;");
                        break;
                    case 'À':
                        out.append("&Agrave;");
                        break;
                    case 'Á':
                        out.append("&Aacute;");
                        break;
                    case 'Â':
                        out.append("&Acirc;");
                        break;
                    case 'Ã':
                        out.append("&Atilde;");
                        break;
                    case 'Ä':
                        out.append("&Auml;");
                        break;
                    case 'Å':
                        out.append("&Aring;");
                        break;
                    case 'Æ':
                        out.append("&AElig;");
                        break;
                    case 'Ç':
                        out.append("&Ccedil;");
                        break;
                    case 'È':
                        out.append("&Egrave;");
                        break;
                    case 'É':
                        out.append("&Eacute;");
                        break;
                    case 'Ê':
                        out.append("&Ecirc;");
                        break;
                    case 'Ë':
                        out.append("&Euml;");
                        break;
                    case 'Ì':
                        out.append("&Igrave;");
                        break;
                    case 'Í':
                        out.append("&Iacute;");
                        break;
                    case 'Î':
                        out.append("&Icirc;");
                        break;
                    case 'Ï':
                        out.append("&Iuml;");
                        break;
                    case 'Ð':
                        out.append("&ETH;");
                        break;
                    case 'Ñ':
                        out.append("&Ntilde;");
                        break;
                    case 'Ò':
                        out.append("&Ograve;");
                        break;
                    case 'Ó':
                        out.append("&Oacute;");
                        break;
                    case 'Ô':
                        out.append("&Ocirc;");
                        break;
                    case 'Õ':
                        out.append("&Otilde;");
                        break;
                    case 'Ö':
                        out.append("&Ouml;");
                        break;
                    case '×':
                        out.append("&times;");
                        break;
                    case 'Ø':
                        out.append("&Oslash;");
                        break;
                    case 'Ù':
                        out.append("&Ugrave;");
                        break;
                    case 'Ú':
                        out.append("&Uacute;");
                        break;
                    case 'Û':
                        out.append("&Ucirc;");
                        break;
                    case 'Ü':
                        out.append("&Uuml;");
                        break;
                    case 'Ý':
                        out.append("&Yacute;");
                        break;
                    case 'Þ':
                        out.append("&THORN;");
                        break;
                    case 'ß':
                        out.append("&szlig;");
                        break;
                    case 'à':
                        out.append("&agrave;");
                        break;
                    case 'á':
                        out.append("&aacute;");
                        break;
                    case 'â':
                        out.append("&acirc;");
                        break;
                    case 'ã':
                        out.append("&atilde;");
                        break;
                    case 'ä':
                        out.append("&auml;");
                        break;
                    case 'å':
                        out.append("&aring;");
                        break;
                    case 'æ':
                        out.append("&aelig;");
                        break;
                    case 'ç':
                        out.append("&ccedil;");
                        break;
                    case 'è':
                        out.append("&egrave;");
                        break;
                    case 'é':
                        out.append("&eacute;");
                        break;
                    case 'ê':
                        out.append("&ecirc;");
                        break;
                    case 'ë':
                        out.append("&euml;");
                        break;
                    case 'ì':
                        out.append("&igrave;");
                        break;
                    case 'í':
                        out.append("&iacute;");
                        break;
                    case 'î':
                        out.append("&icirc;");
                        break;
                    case 'ï':
                        out.append("&iuml;");
                        break;
                    case 'ð':
                        out.append("&eth;");
                        break;
                    case 'ñ':
                        out.append("&ntilde;");
                        break;
                    case 'ò':
                        out.append("&ograve;");
                        break;
                    case 'ó':
                        out.append("&oacute;");
                        break;
                    case 'ô':
                        out.append("&ocirc;");
                        break;
                    case 'õ':
                        out.append("&otilde;");
                        break;
                    case 'ö':
                        out.append("&ouml;");
                        break;
                    case '÷':
                        out.append("&divide;");
                        break;
                    case 'ø':
                        out.append("&oslash;");
                        break;
                    case 'ù':
                        out.append("&ugrave;");
                        break;
                    case 'ú':
                        out.append("&uacute;");
                        break;
                    case 'û':
                        out.append("&ucirc;");
                        break;
                    case 'ü':
                        out.append("&uuml;");
                        break;
                    case 'ý':
                        out.append("&yacute;");
                        break;
                    case 'þ':
                        out.append("&thorn;");
                        break;
                    case 'ÿ':
                        out.append("&yuml;");
                        break;
                    case 8364:
                    case 128:
                        out.append("&euro;");
                        break;
                    default:
                        out.append("&#").append((int) c).append(';');
                        break;
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Returns the hex encoded SHA-digest for a given input.
   * This is usefull for secure password storage.
   * Passwords are not stored as clear text but as their SHA-digested equivalent.
   * If the password is entered it also encoded as SHA-digest. Then that digest is
   * compared to the orginial digest. if they match, the password was correct.
   * @param input Text input
   * @return hex encoded SHA-digest
   */
    public static String getSHADigest(String input) {
        if (input == null) return null;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (java.security.NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
        if (sha == null) throw new RuntimeException("No message digest");
        sha.update(input.getBytes());
        byte[] data = sha.digest();
        StringBuffer buf = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xff;
            buf.append(hexDigit(value >> 4));
            buf.append(hexDigit(value));
        }
        return buf.toString();
    }

    private static char hexDigit(int i) {
        i &= 15;
        i = '0' + i;
        if (i > '9') i += 'a' - '9' - 1;
        return (char) i;
    }

    /** secure random number generator */
    private static SecureRandom random;

    /** Generates a secure random word with the given length.
   * @param len Amount of random characters to generate
   * @return random Word containing letters and numbers.
   */
    public static String randomWord(int len) {
        return randomWord(len, null);
    }

    /** Generates a secure random word with the given length.
   * @param len Amount of random characters to generate
   * @param alphabet Alphabet to generate from.
   * @return random Word containing letters and numbers.
   */
    public static String randomWord(int len, char[] alphabet) {
        if (random == null) {
            random = createSecureRandom();
        }
        if (alphabet == null) alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
        StringBuffer out = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            out.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return out.toString();
    }

    /**
   * Recursively deletes a directory and all sub directories and files.
   * @param dir path of the directory to delete.
   */
    public static void deleteRecursively(String dir) {
        try {
            File f = new File(dir);
            if (f.exists()) {
                File[] children = f.listFiles();
                for (int i = 0; i < children.length; i++) {
                    File child = children[i];
                    if (child.isDirectory()) deleteRecursively(child.getPath()); else child.delete();
                }
                f.delete();
            }
        } catch (Exception e) {
            getLogger().error(e);
        }
    }

    /**
   * Returns the utility class logger.
   * @return log4j logger
   */
    public static Logger getLogger() {
        if (_log == null) {
            _log = org.apache.log4j.Logger.getLogger(Util.class);
        }
        return _log;
    }

    /**
   * Returns a textual representation of the given time stamp (Format: MM.dd.yyyy - HH:mm:ss.SSS).
   */
    public static String getDate(long stamp) {
        synchronized (detailedDateFormat) {
            return detailedDateFormat.format(new Date(stamp));
        }
    }

    private static final List ALLOWED_NOCOLOR = Collections.unmodifiableList(Arrays.asList(new String[] { "B", "I", "P", "A", "LI", "OL", "UL", "EM", "BR", "TT", "STRONG", "BLOCKQUOTE", "DIV", "CODE", "DL", "DT", "DD", "SUP", "SUB" }));

    public static final List ALLOWED_STRICT = Collections.unmodifiableList(Arrays.asList(new String[] { "B", "I", "EM", "TT", "STRONG", "CODE", "SUP", "SUB" }));

    private static Pattern tagPattern = Pattern.compile("</?([a-zA-Z\\-]+)(\\s*(href)?\\s*(?:=\\s*\"[A-Za-z0-9\\-_ ]+\")?)*/?>");

    /**
   * Makes safe html out of a given html snippet.
   * @param allowedTags string list of allowed tags.
   * @param value HTML snippet
   * @return safe HTML snippet
   */
    public static String safeHTML(String value, List allowedTags) {
        synchronized (tagPattern) {
            StringBuffer out = new StringBuffer(value.length());
            Matcher m = tagPattern.matcher(value);
            int lastPost = 0;
            while (m.find()) {
                String tagName = m.group(1);
                boolean legal = false;
                for (Iterator i = allowedTags.iterator(); i.hasNext(); ) {
                    String tag = (String) i.next();
                    if (tagName.equalsIgnoreCase(tag)) {
                        legal = true;
                        break;
                    }
                }
                if (legal) {
                    out.append(htmlEncode(value.substring(lastPost, m.start()), true));
                    out.append(m.group());
                } else {
                    out.append(htmlEncode(value.substring(lastPost, m.end()), true));
                }
                lastPost = m.end();
            }
            if (lastPost < value.length()) out.append(htmlEncode(value.substring(lastPost), true));
            return out.toString();
        }
    }

    public static String dumpAsHTML(Object o) {
        StringBuffer buf = new StringBuffer();
        buf.append(o.getClass().getName()).append('@').append(Integer.toHexString(o.hashCode())).append("<br><dl>");
        Method[] methods = o.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            String name = m.getName();
            if ((name.startsWith("get") || name.startsWith("is")) && m.getParameterTypes().length == 0 && (m.getReturnType().isPrimitive() || m.getReturnType().equals(String.class))) {
                if (name.startsWith("get")) name = name.substring(3); else name = name.substring(2);
                name = Introspector.decapitalize(name);
                Object value;
                try {
                    value = m.invoke(o, (Object[]) null);
                    if (value == null) value = "null";
                    buf.append("<dt>").append(name).append("</dt><dd>").append(value).append("</dd>");
                } catch (Exception e) {
                    getLogger().warn("error dumping " + o, e);
                }
            }
        }
        buf.append("</dl>");
        return buf.toString();
    }

    /**
   * Returns an unmodifiable map containing the keys and values from the given Object array.
   * The key at index n is mapped to the value at index n+1.
   * @throws IllegalArgumentException if the length of the given array is not an even number.
   */
    public static Map asMap(Object[] keyValue) throws IllegalArgumentException {
        if ((keyValue.length & 1) != 0) {
            throw new IllegalArgumentException(keyValue.length + " is not even.");
        }
        Map map = new HashMap(keyValue.length / 2);
        for (int i = 0; i < keyValue.length; i += 2) {
            map.put(keyValue[i], keyValue[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    private static String[] secureRndNames = new String[] { System.getProperty("japano.securerandom"), "SHA1PRNG", "IBMSecureRandom" };

    /**
   * Creates a new secure random number generator.
   * The following secure random algorithm names are tried:
   * <ul><li>
   * The value of system property "japano.securerandom", if set.
   * </li><li>
   * "SHA1PRNG"
   * </li><li>
   * "IBMSecureRandom" (available if running in the IBM JRE)
   * </li></ul>
   */
    public static SecureRandom createSecureRandom() {
        SecureRandom secureRnd = null;
        try {
            for (int i = 0; i < secureRndNames.length; i++) {
                try {
                    if (secureRndNames[i] != null) {
                        secureRnd = SecureRandom.getInstance(secureRndNames[i]);
                        break;
                    }
                } catch (NoSuchAlgorithmException nsae) {
                    getLogger().debug("no secure random algorithm named \"" + secureRndNames[i] + "\"", nsae);
                }
            }
            if (secureRnd == null) {
                throw new IllegalStateException("no secure random algorithm found. (tried " + Arrays.asList(secureRndNames) + ")");
            }
            secureRnd.setSeed(System.currentTimeMillis());
        } catch (Exception e) {
            getLogger().fatal("error initializing secure random", e);
        }
        return secureRnd;
    }

    public static void printThrowable(Buffer out, Throwable t) {
        boolean first = true;
        while (t != null) {
            if (!first) out.println("caused by:");
            out.println(Util.htmlEncode(t.toString(), true));
            out.print("<pre>");
            StackTraceElement[] ste = t.getStackTrace();
            for (int i = 0; i < ste.length; i++) {
                if (i == 0) out.print("at "); else out.print("   ");
                out.println(ste[i]);
            }
            out.print("</pre>");
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            } else {
                t = t.getCause();
            }
        }
    }

    public static boolean isAbsoluteURL(String url) {
        if (url == null) return false;
        int cpos = url.indexOf(':');
        if (cpos >= 0) {
            for (int i = 0; i < cpos; i++) {
                char c = url.charAt(i);
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+')) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static String replace(String in, String what, String with) {
        int pos = 0, lp = 0;
        StringBuffer sb = new StringBuffer();
        while ((pos = in.indexOf(what, lp)) > -1) {
            sb.append(in.substring(lp, pos));
            sb.append(with);
            lp = pos + what.length();
        }
        if (lp < in.length()) {
            sb.append(in.substring(lp));
        }
        return sb.toString();
    }

    /**
   * Writes a collection to a text file. the objects in the collection are seperated
   * by a line separator. The comment is written with a leading <code>'#'</code>.
   *
   */
    public static void writeCollection(Collection c, String path, String comment) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(path));
            String lineSep = System.getProperty("line.separator");
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                String line = (String) i.next();
                bw.write(line);
                bw.write(lineSep);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (bw != null) bw.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing japano tag library definition file", e);
            }
        }
    }

    /**
   * Writes a Map to a text file. The textfile contains key value pairs seperated
   * by a space.
   * The comment is written with a leading <code>'#'</code>.
   */
    public static void writeMap(Map m, String path, String comment) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(path));
            String lineSep = System.getProperty("line.separator");
            if (comment != null && comment.length() > 0) {
                bw.write("# ");
                bw.write(comment);
                bw.write(lineSep);
            }
            for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                bw.write(key);
                bw.write(" ");
                bw.write((String) m.get(key));
                bw.write(lineSep);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (bw != null) bw.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing japano tag library definition file", e);
            }
        }
    }

    /**
   * Reads the map from the a given path.  It expects space seperated key value pairs.
   */
    public static Map readMap(String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                Map m = readMap(new FileInputStream(f));
                return m;
            } else {
                return null;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map readMap(InputStream is) {
        if (is == null) return null;
        Map class2lib = new HashMap();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            int nr = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int dividerPos = line.indexOf(" ");
                if (dividerPos < 0) throw new RuntimeException("Error parsing class map file line " + nr);
                String klass = line.substring(0, dividerPos);
                String tldName = line.substring(dividerPos + 1);
                class2lib.put(klass, tldName);
                nr++;
            }
            return class2lib;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ioe) {
                System.err.println("Error closing class map file" + ioe);
            }
        }
    }

    /**
   * Reads a list of strings from a text file. the list contains the lines of the text file as strings. lines beginning with <code>'#'</code>
   * are not contained in this list.
   */
    private List readList(String resource) {
        BufferedReader br = null;
        List list = new ArrayList();
        try {
            InputStream is = getClass().getResourceAsStream(resource);
            if (is == null) throw new RuntimeException("Resource \"" + resource + "\" not found");
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) list.add(line);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing japano tag library definition file", e);
            }
        }
    }

    /**
   * Deserializes an object from a file.
   *
   * @param path
   * @return deserialized object
   */
    public static Object deserialized(String path) {
        ObjectInputStream ois = null;
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            ois = new ObjectInputStream(new FileInputStream(f));
            return ois.readObject();
        } catch (Exception e) {
            getLogger().error("Error reading object", e);
            return null;
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (IOException e) {
                getLogger().error("Error closing object input stream", e);
                return null;
            }
        }
    }

    /**
   * Serializes the given object into a file.
   *
   * @param path full path of the file to write to
   * @param o object to write
   */
    public static void serialize(String path, Object o) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(path));
            oos.writeObject(o);
        } catch (Exception e) {
            getLogger().error("Error serializing object", e);
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
                getLogger().error("Error closing object input stream", e);
            }
        }
    }

    public static String encode(byte[] data) {
        int cnt = data.length / 3;
        StringBuffer buf = new StringBuffer(data.length + 2);
        int off = 0;
        for (int i = 0; i < cnt; i++) {
            int v0 = data[off];
            int v1 = data[off + 1];
            int v2 = data[off + 2];
            insertTriple(buf, v0, v1, v2);
            off += 3;
        }
        int v0 = 0;
        int v1 = 0;
        switch(data.length - cnt * 3) {
            case 2:
                v1 = data[off + 1];
            case 1:
                v0 = data[off];
                insertTriple(buf, v0, v1, 0);
                break;
            case 0:
                break;
        }
        return buf.toString();
    }

    private static void insertTriple(StringBuffer buf, int v0, int v1, int v2) {
        int b0 = (v0 & 0xfc) >> 2;
        int b1 = ((v0 & 0x03) << 4) + ((v1 & 0xf0) >> 4);
        int b2 = ((v1 & 0x0f) << 2) + ((v2 & 0xc0) >> 6);
        int b3 = (v2 & 0x3f);
        buf.append(ALPHABET[b0]).append(ALPHABET[b1]).append(ALPHABET[b2]).append(ALPHABET[b3]);
    }

    /**
   * Makes the given String safe to be enclosed in quotes.
   */
    public static String quoteEscape(String s) {
        if (s == null) return "";
        StringBuffer buf = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '\"':
                    buf.append("\\\"");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

    /** Test methods for Util */
    public static class TEST {

        public static void testReplace() {
            String result = replace("ABCDE", "CD", "XXX");
            if (!result.equals("ABXXXE")) throw new RuntimeException("test failed. result = " + result);
            result = replace("ABCDE", "XX", "CD");
            if (!result.equals("ABCDE")) throw new RuntimeException("test2 failed. result = " + result);
        }

        public static void testAbsoluteUrl() {
            if (!isAbsoluteURL("http://www.google.com")) throw new RuntimeException();
            if (!isAbsoluteURL("HttP://WWW.google.Com")) throw new RuntimeException();
            if (isAbsoluteURL("/test/index.html")) throw new RuntimeException();
            if (isAbsoluteURL("index.html")) throw new RuntimeException();
        }

        public static void testCreateRandom() {
            if (createSecureRandom() == null) throw new RuntimeException();
        }

        public static void testAsMap() {
            Map m = asMap(new Object[] { "1", "one", "2", "two" });
            if (!m.get("1").equals("one")) throw new RuntimeException();
            if (!m.get("2").equals("two")) throw new RuntimeException();
            try {
                m = asMap(new Object[] { "1", "one", "2" });
                throw new RuntimeException();
            } catch (IllegalArgumentException e) {
            }
        }

        public static void testSHADigest() {
            String digest = getSHADigest("japano");
            if (!digest.equals("0b15cb1bfbe27be390584a14beaf19712bbdb955")) throw new RuntimeException("digest does not match. is : " + digest);
        }

        public static void testHTMLEncode() {
            String encode = htmlEncode("<b>\"Hällo!\"</b>", true);
            if (!encode.equals("&lt;b&gt;&quot;H&auml;llo!&quot;&lt;/b&gt;")) throw new RuntimeException("tst1 failed");
            encode = htmlEncode("<b>\"Hällo!\"</b>", false);
            if (!encode.equals("<b>\"H&auml;llo!\"</b>")) throw new RuntimeException("tst2 failed");
        }
    }

    private static final Pattern JAVADOC_PATTERN = Pattern.compile("@[a-zA-Z0-9\\.]+.*$", Pattern.MULTILINE);

    public static String removeJavadocTags(String text) {
        if (text == null) return null;
        Matcher m = JAVADOC_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(randomWord(16, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789".toCharArray()));
    }
}
