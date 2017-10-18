package uk.gov.dti.og.fox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import uk.gov.dti.og.fox.dom.DOM;
import uk.gov.dti.og.fox.ex.ExInternal;
import uk.gov.dti.og.fox.ex.ExRoot;
import uk.gov.dti.og.fox.ex.ExRuntimeRoot;
import uk.gov.dti.og.fox.ex.ExTooFew;
import uk.gov.dti.og.fox.io.IOUtil;
import uk.gov.dti.og.fox.track.Track;

public abstract class XFUtil extends Track {

    public static final int SANITISE_ALERTS = 1;

    public static final int SANITISE_HINTS = 2;

    public static final int SANITISE_HTMLENTITIES = 3;

    public static final String ALPHA_UPPER_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String ALPHA_LOWER_MAP = "abcdefghijklmnopqrstuvwxyz";

    public static final String ALPHA_SPACE_MAP = "                          ";

    public static final String XML_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String XML_DATE_FORMAT = "yyyy-MM-dd";

    public static final Integer INTEGER_MAX_VALUE = new Integer(Integer.MAX_VALUE);

    private static final int MAX_ALPHA_RADIX = 62;

    private static final long UNIQUE_CACHE_SIZE = 1000;

    static final long UNIQUE_RESET_BOUNDARY = Long.MAX_VALUE - Integer.MAX_VALUE;

    static char _gUniquePrefix;

    static String _gUniqueSuffix;

    static long _gUniqueCount = UNIQUE_RESET_BOUNDARY + 1;

    private static final Iterator _gObsoleteIterator = getUniqueIterator(10000);

    /**
   * All possible chars for representing a number as a String
   */
    static final char[] ALPHA_DIGETS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    static final String[] mons = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };

    public static String gHostName = null;

    static {
        gHostName = getHostName();
        _gUniquePrefix = gHostName.charAt(0);
    }

    public static void init() {
    }

    public static final Object _gUniqueSyncObject = new Object();

    /** @depricate This should be obsoleted in preference for using instances from getUniqueIterator */
    public static final String unique() {
        String lUnique = (String) _gObsoleteIterator.next();
        int lFirstChar = (int) lUnique.charAt(0);
        if (!((lFirstChar >= 'a' && lFirstChar <= 'z') || (lFirstChar >= 'A' && lFirstChar <= 'Z'))) {
            throw new ExInternal("Hostname (" + gHostName + ") must start with [a-zA-Z], generating uniques would make for invalid XML names");
        }
        return lUnique;
    }

    public static final Iterator getUniqueIterator() {
        return new UniqueIterator(UNIQUE_CACHE_SIZE);
    }

    public static final Iterator getUniqueIterator(long pCacheSize) {
        return new UniqueIterator(pCacheSize);
    }

    /** (OVERLOADED) SQL like Null value replace utility */
    public static String nvl(String pValue, String pReplaceNullsWith) {
        if (pValue != null && pValue.length() != 0) {
            return pValue;
        }
        return pReplaceNullsWith;
    }

    /** (OVERLOADED) SQL like Null value replace utility */
    public static final int nvl(Integer pValue, Integer pReplaceNullsWith) {
        if (pValue != null) {
            return pValue.intValue();
        }
        return pReplaceNullsWith.intValue();
    }

    /** (OVERLOADED) SQL like Null value replace utility */
    public static final int nvl(Integer pValue, int pReplaceNullsWith) {
        if (pValue != null) {
            return pValue.intValue();
        }
        return pReplaceNullsWith;
    }

    /** (OVERLOADED) SQL like Null value replace utility */
    public static final int nvl(String pValue, int pReplaceNullsWith) {
        if (pValue != null && pValue.length() != 0) {
            return Integer.valueOf(pValue).intValue();
        }
        return pReplaceNullsWith;
    }

    /** (OVERLOADED) SQL like Null value replace utility converts to "" */
    public static String nvl(String pValue) {
        return nvl(pValue, "");
    }

    /** This initialise all the first letters to uppercase and the rest to lowercase, WARNING also replaces _ with spaces */
    public static String initCap(String oldStr) {
        oldStr = replace("_", " ", oldStr);
        StringBuffer finalStr = new StringBuffer();
        int strLen = oldStr.length();
        if (strLen > 0) {
            oldStr = oldStr.toLowerCase();
            int next = 1;
            int count = 0;
            finalStr.append(oldStr.substring(count, next).toUpperCase());
            while ((count = oldStr.indexOf(' ', next - 1)) != -1 && count < strLen - 1) {
                count++;
                String begin = oldStr.substring(next, count);
                next = count + 1;
                String letter = oldStr.substring(count, next).toUpperCase();
                finalStr.append(begin + letter);
            }
            finalStr.append(oldStr.substring(next));
        }
        return finalStr.toString();
    }

    /** Global replace all occurances of string */
    public static final String replace(String find, String replace, String str) {
        if (str.indexOf(find) == -1) {
            return str;
        }
        StringBuffer sBuf = new StringBuffer();
        sBuf.append(str);
        replace(find, replace, sBuf);
        return sBuf.toString();
    }

    /** Global replace all occurances of string */
    public static final int replace(String find, String replace, StringBuffer buf) {
        int c = 0;
        int l = find.length();
        int r = replace.length();
        int p = 0;
        int i;
        while ((i = buf.indexOf(find, p)) != -1) {
            buf.replace(i, i + l, replace);
            c++;
            p = i + r;
        }
        return c;
    }

    /** Global replace all occurances of string */
    public static final int replaceRegexpExhaustive(String pRegexp, String pReplacement, StringBuffer pBuf) {
        Pattern p = Pattern.compile(pRegexp);
        Matcher m = p.matcher(pBuf.toString());
        int count = 0;
        boolean result = m.find();
        if (result) {
            pBuf.delete(0, pBuf.capacity());
        }
        while (result) {
            count++;
            m.appendReplacement(pBuf, pReplacement);
            result = m.find();
        }
        m.appendTail(pBuf);
        return count;
    }

    /** Global replace of strings in string buffer ignoring case
   *    This is done passing in the find string in a specific case (say lower case)
   *    then and a second processed string buffer in the same case. The processed buffer is 
   *    used for searches and any replacements processed into both buffers to keep them aligned
   */
    public static final int replaceIgnoreCase(String pFindFixedCaseString, String pReplaceString, StringBuffer pRealBufferAnyCase, StringBuffer pProcessedBufferFixedCase) {
        int c = 0;
        int l = pFindFixedCaseString.length();
        int r = pReplaceString.length();
        int p = 0;
        int i;
        while ((i = pProcessedBufferFixedCase.indexOf(pFindFixedCaseString, p)) != -1) {
            pRealBufferAnyCase.replace(i, i + l, pReplaceString);
            pProcessedBufferFixedCase.replace(i, i + l, pReplaceString);
            c++;
            p = i + r;
        }
        return c;
    }

    /** Global replace all occurances of string iteratively until all occurances exhausted */
    public static final int replaceExhaustive(String find, String replace, StringBuffer buf) {
        int c = 0;
        int l = find.length();
        int p = 0;
        int i;
        while ((i = buf.indexOf(find, p)) != -1) {
            buf.replace(i, i + l, replace);
            c++;
            p = i;
        }
        return c;
    }

    public static final String pathStripLeadSlashesString(String pString) {
        StringBuffer buf = new StringBuffer(pString);
        while (buf.length() > 0 && buf.charAt(0) == '/') {
            buf.deleteCharAt(0);
        }
        return buf.toString();
    }

    public static final String pathPopHead(StringBuffer buf, boolean skip_lead_slashes) {
        if (skip_lead_slashes) {
            while (buf.length() > 0 && buf.charAt(0) == '/') {
                buf.deleteCharAt(0);
            }
        }
        if (buf.length() == 0) return "";
        int p = buf.indexOf("/");
        if (p == -1) p = buf.length();
        String str = buf.substring(0, p);
        buf.delete(0, p);
        while (buf.length() > 0 && buf.charAt(0) == '/') {
            buf.deleteCharAt(0);
        }
        return str;
    }

    public static final String pathPopTail(StringBuffer buf) {
        if (buf.length() == 0) return "";
        int p = buf.lastIndexOf("/");
        String str;
        if (p == -1) {
            str = buf.toString();
            buf.delete(0, buf.length());
        } else if (p + 1 == buf.length()) {
            str = "";
            buf.delete(p, buf.length());
        } else {
            str = buf.substring(p + 1, buf.length());
            buf.delete(p, buf.length());
        }
        return str;
    }

    public static final StringBuffer pathPushTail(StringBuffer poPathStringBuffer, String pTailWordString) {
        if (pTailWordString != null && pTailWordString.length() != 0) {
            if (poPathStringBuffer.length() > 0) {
                poPathStringBuffer.append('/');
            }
            poPathStringBuffer.append(pTailWordString);
        }
        return poPathStringBuffer;
    }

    public static final StringBuffer pathPushHead(StringBuffer poPathStringBuffer, String pHeadWordString) {
        if (pHeadWordString != null && pHeadWordString.length() != 0) {
            if (poPathStringBuffer.length() > 0) {
                poPathStringBuffer.insert(0, '/');
            }
            poPathStringBuffer.insert(0, pHeadWordString);
        }
        return poPathStringBuffer;
    }

    public static String tagPopNS(StringBuffer buf) {
        if (buf.length() == 0) return "";
        int p = buf.lastIndexOf(":");
        if (p == -1) return "";
        String str = buf.substring(0, p);
        buf.delete(0, p + 1);
        return str;
    }

    /** Evaluate Fox String Boolean */
    public static boolean stringBoolean(String pString) {
        if (pString.length() == 0 || pString.equals(".") || pString.equalsIgnoreCase("y") || pString.equalsIgnoreCase("yes") || pString.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    public static String getJavaStackTraceInfo(Throwable th) {
        String stackTraceInfo;
        if (th instanceof ExRoot) {
            stackTraceInfo = ((ExRoot) th).getMessageStack();
        } else if (th instanceof ExRuntimeRoot) {
            stackTraceInfo = ((ExRuntimeRoot) th).getMessageStack();
        } else {
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw));
            stackTraceInfo = sw.toString();
        }
        return stackTraceInfo;
    }

    public static boolean isNull(Object pStringOrObject) {
        if (pStringOrObject == null || pStringOrObject.equals("")) {
            return true;
        }
        return false;
    }

    public static String[] toStringArray(ArrayList pArrayList) {
        String[] prototype = {};
        return (String[]) pArrayList.toArray(prototype);
    }

    public static final List toArrayList(Object pObject) {
        List array;
        if (pObject == null) {
            array = new ArrayList(1);
        } else if (pObject instanceof String) {
            array = new ArrayList(1);
            array.add(pObject);
        } else {
            String[] lObjectArray = (String[]) pObject;
            array = new ArrayList(lObjectArray.length);
            for (int i = 0; i < lObjectArray.length; i++) {
                array.add(lObjectArray[i]);
            }
        }
        return array;
    }

    /** Reads an Input Stream into a new String Buffer */
    public static StringBuffer toStringBuffer(InputStream pInputStream, int pChunkSize, int pMaxSize) {
        return toStringBuffer(new InputStreamReader(pInputStream), pChunkSize, pMaxSize);
    }

    /** Reads a Reader into a new String Buffer */
    public static StringBuffer toStringBuffer(Reader pReader, int pChunkSize, long pMaxSize) {
        if (pMaxSize < 0) {
            pMaxSize = Integer.MAX_VALUE;
        }
        StringBuffer lTargetBuffer = new StringBuffer();
        char lReadBuffer[] = new char[pChunkSize];
        int lReadSize;
        int lReadMax = pChunkSize;
        READ_LOOP: while (lTargetBuffer.length() < pMaxSize) {
            if (lTargetBuffer.length() + pChunkSize >= pMaxSize) {
                lReadMax = (int) (pMaxSize - lTargetBuffer.length());
                if (lReadMax < 1) {
                    throw new ExInternal("Read size compute error");
                }
            }
            try {
                lReadSize = pReader.read(lReadBuffer, 0, lReadMax);
                if (lReadSize == -1) {
                    break READ_LOOP;
                } else if (lReadSize == 0) {
                    throw new ExInternal("Unexpected zero read size");
                }
            } catch (IOException e) {
                throw new ExInternal("Error converting InputStream into ByteArrayr", e);
            }
            lTargetBuffer.append(lReadBuffer, 0, lReadSize);
        }
        return lTargetBuffer;
    }

    /** Reads a InputStream into a new byteArray */
    public static byte[] toByteArray(InputStream pInputStream, int pChunkSize, long pMaxSize) {
        if (pMaxSize < 0) {
            pMaxSize = Long.MAX_VALUE;
        }
        byte[] lTargetArray = new byte[0];
        byte lReadBuffer[] = new byte[pChunkSize];
        int lReadSize;
        int lReadMax = pChunkSize;
        READ_LOOP: while (lTargetArray.length < pMaxSize) {
            if (lTargetArray.length + pChunkSize >= pMaxSize) {
                lReadMax = (int) (pMaxSize - lTargetArray.length);
                if (lReadMax < 1) {
                    throw new ExInternal("Read size compute error");
                }
            }
            try {
                lReadSize = pInputStream.read(lReadBuffer, 0, lReadMax);
                if (lReadSize == -1) {
                    break READ_LOOP;
                } else if (lReadSize == 0) {
                    throw new ExInternal("Unexpected zero read size");
                }
            } catch (IOException e) {
                throw new ExInternal("Error converting InputStream into ByteArrayr", e);
            }
            byte[] lNewArray = new byte[lTargetArray.length + lReadSize];
            System.arraycopy(lTargetArray, 0, lNewArray, 0, lTargetArray.length);
            System.arraycopy(lReadBuffer, 0, lNewArray, lTargetArray.length, lReadSize);
            lTargetArray = lNewArray;
        }
        return lTargetArray;
    }

    /** Interigates java call stack returning method names as an array - not efficient use only for diagnostic code */
    public static final String[] getJavaStackMethods(int pMaxArraySize) {
        Exception x = new Exception();
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));
        StringTokenizer st = new StringTokenizer(sw.toString(), "\n");
        ArrayList al = new ArrayList();
        st.nextToken();
        st.nextToken();
        st.nextToken();
        int p1;
        String call;
        while (al.size() < pMaxArraySize && st.hasMoreTokens()) {
            call = st.nextToken().trim();
            st.nextToken();
            p1 = call.indexOf(' ') + 1;
            al.add(call.substring(p1, call.indexOf('(', p1)));
        }
        return (String[]) al.toArray(new String[0]);
    }

    /** Interigates java call stack returning class names as an array - not efficient use only for diagnostic code */
    public static final String[] getJavaStackClasses(int pMaxArraySize) {
        Exception x = new Exception();
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));
        StringTokenizer st = new StringTokenizer(sw.toString(), "\n");
        ArrayList al = new ArrayList();
        st.nextToken();
        st.nextToken();
        st.nextToken();
        int p1;
        String call;
        while (al.size() < pMaxArraySize && st.hasMoreTokens()) {
            call = st.nextToken().trim();
            st.nextToken();
            p1 = call.indexOf(' ') + 1;
            call = call.substring(p1, call.indexOf('(', p1));
            p1 = call.lastIndexOf('.');
            al.add(call.substring(0, p1));
        }
        return (String[]) al.toArray(new String[0]);
    }

    public static final Integer toInt(String pString) {
        if (isNull(pString)) {
            return null;
        }
        if (pString.equals("unbounded")) {
            return INTEGER_MAX_VALUE;
        }
        return Integer.valueOf(pString);
    }

    public static final Integer nvlInteger(String pString, Integer pInteger) {
        if (isNull(pString)) {
            return pInteger;
        }
        if (pString.equals("unbounded")) {
            return INTEGER_MAX_VALUE;
        }
        return Integer.valueOf(pString);
    }

    public static final String toStr(int pInt) {
        return Integer.toString(pInt);
    }

    public static final boolean isCharacters(String pSourceString, char[] pCharSet, int pSubStringFrom, int pSubStringTo) {
        return isCharacters(pSourceString.toCharArray(), pCharSet, pSubStringFrom, pSubStringTo);
    }

    public static final boolean isCharacters(char[] pSourceChars, char[] pCharSet, int pSubStringFrom, int pSubStringTo) throws ExInternal {
        if (pSourceChars == null || pCharSet == null) {
            throw new ExInternal("null value passed to isCharacters");
        }
        int i, j;
        CHAR_LOOP: for (i = pSubStringFrom; i < pSubStringTo; i++) {
            MAP_LOOP: for (j = 0; j < pCharSet.length; j++) {
                if (pSourceChars[i] == pCharSet[j]) {
                    continue CHAR_LOOP;
                }
            }
            return false;
        }
        return true;
    }

    public String removeSpaces(String s) {
        StringTokenizer st = new StringTokenizer(s, " ", false);
        String t = "";
        while (st.hasMoreElements()) {
            t += st.nextElement();
        }
        return t;
    }

    /** Tests whether the passed string is a valid value
   * @param pTest the string to be tested
   * @return true if the string passed is not null or an empty string
   */
    public static boolean exists(String pTest) {
        if (pTest == null || pTest.length() == 0) {
            return false;
        }
        return true;
    }

    /**
  * Creates a string representation of the first argument in the
  * radix specified by the second argument.
  * <p>
  * If the radix is smaller than <code>Character.MIN_RADIX</code> or
  * larger than <code>Character.MAX_RADIX</code>, then the radix
  * <code>10</code> is used instead.
  * <p>
  * If the first argument is negative, the first element of the 
  * result is the ASCII minus sign <code>'-'</code> 
  * (<code>'&#92;u002d'</code>. If the first argument is not negative, 
  * no sign character appears in the result. 
  * <p>
  * The remaining characters of the result represent the magnitude of 
  * the first argument. If the magnitude is zero, it is represented by 
  * a single zero character <code>'0'</code> 
  * (<code>'&#92;u0030'</code>); otherwise, the first character of the
  * representation of the magnitude will not be the zero character.
  * The following ASCII characters are used as digits: 
  * <blockquote><pre>
  *   0123456789abcdefghijklmnopqrstuvwxyz
  * </pre></blockquote>
  * These are <tt>'&#92;u0030'</tt> through <tt>'&#92;u0039'</tt> 
  * and <tt>'&#92;u0061'</tt> through <tt>'&#92;u007a'</tt>. If the 
  * radix is <var>N</var>, then the first <var>N</var> of these 
  * characters are used as radix-<var>N</var> digits in the order 
  * shown. Thus, the digits for hexadecimal (radix 16) are 
  * <tt>0123456789abcdef</tt>. If uppercase letters
  * are desired, the {@link java.lang.String#toUpperCase()} method 
  * may be called on the result: 
  * <blockquote><pre>
  * Long.toString(n, 16).toUpperCase()
  * </pre></blockquote>
  * 
  * @param   i       a long.
  * @param   radix   the radix.
  * @return  a string representation of the argument in the specified radix.
  * @see     java.lang.Character#MAX_RADIX
  * @see     java.lang.Character#MIN_RADIX
  */
    public static String toAlphaString(long i) {
        final int radix = MAX_ALPHA_RADIX;
        char[] buf = new char[65];
        int charPos = 64;
        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            try {
                buf[charPos--] = ALPHA_DIGETS[(int) (-(i % radix))];
            } catch (IndexOutOfBoundsException x) {
                System.out.println("= " + (int) (-i));
                throw x;
            }
            i = i / radix;
        }
        buf[charPos] = ALPHA_DIGETS[(int) (-i)];
        if (negative) {
            buf[--charPos] = '-';
        }
        return new String(buf, charPos, (65 - charPos));
    }

    /**
    * Returns the value of the specified parameter from the <code>Map</code>.
    * 
    * <p>The map has been obtained via an <code>HttpServletRequest</code> and
    * should contain <code>String[]</code> values. However, the OC4J (9.0.3)
    * implementation has a bug and may return <code>String</code> for single
    * value parameters. - GAW
    * 
    * @param paramsMap the map of request parameters
    * @param name the name of the parameter whose value is to be obtained.
    * @return the value of the parameter.
    */
    public static String getParamMapValue(Map paramsMap, String name) {
        Object valueObj = paramsMap.get(name);
        if (valueObj instanceof String[]) return ((String[]) valueObj)[0]; else return (String) valueObj;
    }

    public static final boolean isWhiteSpace(String pString) {
        char[] val = pString.toCharArray();
        int len = val.length;
        int st = 0;
        while (st < len) {
            if (val[st] > ' ') {
                return false;
            }
            st++;
        }
        return true;
    }

    public static final List stringPlaceHolderQuoteRemove(String pOriginalString, StringBuffer pStringBuffer, String pQuote, String pPlaceHolderStart, String pPlaceHolderEnd) throws ExTooFew {
        List lRestoreList = new ArrayList();
        RestoreEntry lRestoreEntry;
        String lSeekString;
        int lStartIndex, lEndIndex;
        int lOffsetIndex = 0;
        int lCount = 0;
        END_LOOP: for (; ; ) {
            lSeekString = pStringBuffer.toString();
            lStartIndex = lSeekString.indexOf(pQuote, lOffsetIndex);
            if (lStartIndex == -1) {
                break END_LOOP;
            }
            lEndIndex = lSeekString.indexOf(pQuote, lStartIndex + 1);
            if (lEndIndex == -1) {
                throw new ExTooFew("Quote(" + pQuote + ") mismatch in: " + pOriginalString);
            }
            lRestoreEntry = new RestoreEntry();
            lRestoreEntry.mHolder = pPlaceHolderStart + String.valueOf(lCount++) + pPlaceHolderEnd;
            lRestoreEntry.mValue = pStringBuffer.substring(lStartIndex, lEndIndex + 1);
            lRestoreList.add(lRestoreEntry);
            pStringBuffer.replace(lStartIndex, lEndIndex + 1, lRestoreEntry.mHolder);
            lOffsetIndex = lStartIndex + 1;
        }
        return lRestoreList;
    }

    public static final List stringPlaceHolderDeepRemove(String pOriginalString, StringBuffer pStringBuffer, String pStartBrace, String pEndBrace, String pPlaceHolderStart, String pPlaceHolderEnd) throws ExTooFew {
        List lRestoreList = new ArrayList();
        RestoreEntry lRestoreEntry;
        int lStartIndex, lEndIndex, lSeekIndex;
        int lCount = 0;
        END_LOOP: for (; ; ) {
            String lSeekString = pStringBuffer.toString();
            lEndIndex = lSeekString.indexOf(pEndBrace);
            if (lEndIndex == -1) {
                break END_LOOP;
            }
            lStartIndex = -1;
            START_LOOP: while ((lSeekIndex = lSeekString.indexOf(pStartBrace, lStartIndex + 1)) != -1 && lSeekIndex < lEndIndex) {
                lStartIndex = lSeekIndex;
            }
            if (lStartIndex == -1) {
                throw new ExTooFew("Mismatch of " + pStartBrace + "" + pEndBrace + " in: " + pOriginalString);
            }
            lRestoreEntry = new RestoreEntry();
            lRestoreEntry.mHolder = pPlaceHolderStart + String.valueOf(lCount++) + pPlaceHolderEnd;
            lRestoreEntry.mValue = pStringBuffer.substring(lStartIndex, lEndIndex + 1);
            lRestoreList.add(lRestoreEntry);
            pStringBuffer.replace(lStartIndex, lEndIndex + 1, lRestoreEntry.mHolder);
        }
        return lRestoreList;
    }

    public static final void stringPlaceHolderRestore(StringBuffer pStringBuffer, List pRestoreList) {
        RestoreEntry lRestoreEntry;
        int lStartIndex;
        for (int i = pRestoreList.size() - 1; i >= 0; i--) {
            lRestoreEntry = (RestoreEntry) pRestoreList.get(i);
            lStartIndex = pStringBuffer.indexOf(lRestoreEntry.mHolder);
            if (lStartIndex == -1) {
                throw new ExInternal("stringPlaceHolderDeepRestore index error on :" + lRestoreEntry.mHolder);
            }
            pStringBuffer.replace(lStartIndex, lStartIndex + lRestoreEntry.mHolder.length(), lRestoreEntry.mValue);
        }
    }

    public static String sanitiseStringForOutput(String pString, int sanitiseType) {
        String lSanitisedString = pString;
        if (sanitiseType == SANITISE_ALERTS) {
            lSanitisedString = lSanitisedString.replaceAll("(\r|\n){1,2}", "\\\\n");
            lSanitisedString = lSanitisedString.replaceAll("'", "\\\\'");
            lSanitisedString = lSanitisedString.replaceAll("\"", "\\\\\"");
            lSanitisedString = lSanitisedString.replaceAll("\\\\r", "\\\\n");
        } else if (sanitiseType == SANITISE_HINTS) {
            lSanitisedString = lSanitisedString.replaceAll("\\\\", "\\\\\\\\");
            lSanitisedString = lSanitisedString.replaceAll("(\r|\n){1,2}", "<br />");
            lSanitisedString = lSanitisedString.replaceAll("'", "\\\\'");
            lSanitisedString = lSanitisedString.replaceAll("\"", "\\\\\"");
        } else if (sanitiseType == SANITISE_HTMLENTITIES) {
            lSanitisedString = lSanitisedString.replaceAll("&", "&amp;");
            lSanitisedString = lSanitisedString.replaceAll("<", "&lt;");
            lSanitisedString = lSanitisedString.replaceAll(">", "&gt;");
            lSanitisedString = lSanitisedString.replaceAll("'", "&apos;");
            lSanitisedString = lSanitisedString.replaceAll("\"", "&quot;");
        }
        return lSanitisedString;
    }

    public static boolean isValidChar(char[] chars, int[] positions, int min, int max) {
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] + 1 > chars.length) {
                return false;
            }
            if (!(chars[positions[i]] > min && chars[positions[i]] < max)) {
                return false;
            }
        }
        return true;
    }

    public static String formatDate(Date lDate, String lFormatMask) {
        SimpleDateFormat lDateFormat = new SimpleDateFormat(lFormatMask);
        return lDateFormat.format(lDate);
    }

    public static String formatDateXML(Date lDate) {
        return formatDate(lDate, XML_DATE_FORMAT);
    }

    public static String formatDatetimeXML(Date lDate) {
        return formatDate(lDate, XML_DATETIME_FORMAT);
    }

    public static String dateTimeReader(String pDate, String pDateFormat, String pXsType) {
        if (pXsType.equals("xs:time")) {
            return pDate;
        } else if (pXsType.equals("xs:dateTime")) {
            char[] date = pDate.toCharArray();
            if (date.length < 12 && date.length > 20) {
                return pDate;
            }
            char[] day = new char[2];
            char[] month = new char[2];
            char[] year = new char[4];
            int diff = 0;
            if (isValidChar(date, new int[] { 0, 1 }, 47, 58)) {
                day[0] = date[0];
                day[1] = date[1];
            } else {
                if (isValidChar(date, new int[] { 0 }, 47, 58) && !(isValidChar(date, new int[] { 1 }, 64, 91) || isValidChar(date, new int[] { 1 }, 96, 123))) {
                    day[0] = '0';
                    day[1] = date[0];
                    diff--;
                } else {
                    return pDate;
                }
            }
            if (isValidChar(date, new int[] { 3 + diff }, 47, 58)) {
                if (isValidChar(date, new int[] { 4 + diff }, 47, 58)) {
                    month[0] = date[3 + diff];
                    month[1] = date[4 + diff];
                } else {
                    month[0] = '0';
                    month[1] = date[3 + diff];
                    diff--;
                }
                diff--;
            } else if ((isValidChar(date, new int[] { 3 + diff }, 64, 91) || isValidChar(date, new int[] { 3 + diff }, 96, 123)) && (isValidChar(date, new int[] { 4 + diff }, 64, 91) || isValidChar(date, new int[] { 4 + diff }, 96, 123)) && (isValidChar(date, new int[] { 5 + diff }, 64, 91) || isValidChar(date, new int[] { 5 + diff }, 96, 123))) {
                String sb = new String(("" + date[3 + diff] + date[4 + diff] + date[5 + diff]).toUpperCase());
                int count;
                boolean found = true;
                for (count = 0; count < mons.length; count++) {
                    String m = mons[count];
                    if (sb.equals(m)) {
                        int p = (count + 1) % 12;
                        if (p == 0) {
                            p = 12;
                        }
                        if (p < 10) {
                            month[0] = '0';
                            month[1] = (char) (p + 48);
                        } else {
                            month[0] = '1';
                            p = p - 10;
                            month[1] = (char) (p + 48);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return pDate;
                }
            } else {
                return pDate;
            }
            if (isValidChar(date, new int[] { 7 + diff, 8 + diff, 9 + diff, 10 + diff }, 47, 58)) {
                year[0] = date[7 + diff];
                year[1] = date[8 + diff];
                year[2] = date[9 + diff];
                year[3] = date[10 + diff];
            } else {
                return pDate;
            }
            int dateSize = date.length;
            char[] hour = new char[] { '0', '0' };
            char[] min = new char[] { '0', '0' };
            char[] sec = new char[] { '0', '0' };
            if (dateSize > 12) {
                if (isValidChar(date, new int[] { 12 + diff, 13 + diff }, 47, 58)) {
                    hour[0] = date[12 + diff];
                    hour[1] = date[13 + diff];
                }
                if (dateSize > 14) {
                    if (isValidChar(date, new int[] { 15 + diff, 16 + diff }, 47, 58)) {
                        min[0] = date[15 + diff];
                        min[1] = date[16 + diff];
                    }
                    if (dateSize > 16) {
                        if (isValidChar(date, new int[] { 18 + diff, 19 + diff }, 47, 58)) {
                            sec[0] = date[18 + diff];
                            sec[1] = date[19 + diff];
                        }
                    }
                }
            }
            return "" + year[0] + year[1] + year[2] + year[3] + '-' + month[0] + month[1] + '-' + day[0] + day[1] + 'T' + hour[0] + hour[1] + ':' + min[0] + min[1] + ':' + sec[0] + sec[1];
        } else if (pXsType.equals("xs:date")) {
            char[] date = pDate.toCharArray();
            char[] day = new char[2];
            char[] month = new char[2];
            char[] year = new char[4];
            int upos = 1;
            int apos = 0;
            int cpos = 0;
            int sep = 0;
            StringBuffer monthSB = new StringBuffer();
            boolean valid = true;
            if (date.length < 6) {
                valid = false;
            }
            char monthFormat = 'N';
            for (int i = 0; i < date.length && valid; i++) {
                char l = date[i];
                switch(upos) {
                    case 1:
                        if (l > 47 && l < 58) {
                            day[apos] = (char) l;
                            apos++;
                            cpos++;
                        } else {
                            if (cpos > 0) {
                                day[1] = day[0];
                                day[0] = '0';
                                upos++;
                                i--;
                                apos = 0;
                                cpos = 0;
                            } else {
                                valid = false;
                                break;
                            }
                        }
                        if (apos >= 2) {
                            upos++;
                            apos = 0;
                            cpos = 0;
                        }
                        break;
                    case 2:
                        if (l > 47 && l < 58) {
                            if (monthFormat == 'C') {
                                upos++;
                                i--;
                                break;
                            } else {
                                monthFormat = 'D';
                                month[cpos] = (char) l;
                                apos++;
                                cpos++;
                            }
                        } else if ((l > 64 && l < 91) || (l > 96 && l < 123)) {
                            if (monthFormat == 'D') {
                                valid = false;
                                break;
                            } else {
                                monthFormat = 'C';
                                monthSB.append((char) l);
                                apos++;
                            }
                        } else {
                            sep++;
                            apos++;
                        }
                        if (sep > 1 || cpos >= 2) {
                            upos++;
                        }
                        break;
                    case 3:
                        if (monthFormat == 'N') {
                            valid = false;
                            break;
                        } else if (monthFormat == 'C') {
                            String sb = new String(monthSB.toString().toUpperCase());
                            int count;
                            for (count = 0; count < mons.length; count++) {
                                String m = mons[count];
                                if (sb.equals(m)) {
                                    int p = (count + 1) % 12;
                                    if (p == 0) {
                                        p = 12;
                                    }
                                    if (p < 10) {
                                        month[0] = '0';
                                        month[1] = (char) (p + 48);
                                    } else {
                                        month[0] = '1';
                                        p = p - 10;
                                        month[1] = (char) (p + 48);
                                    }
                                    break;
                                }
                            }
                            if (count == mons.length) {
                                valid = false;
                                break;
                            }
                        } else if (monthFormat == 'D' && cpos == 1) {
                            month[1] = month[0];
                            month[0] = '0';
                        }
                        upos++;
                        i--;
                        apos = 0;
                        cpos = 0;
                        break;
                    case 4:
                        if (l > 47 && l < 58) {
                            year[cpos] = (char) l;
                            apos++;
                            cpos++;
                        } else if ((l > 64 && l < 91) || (l > 96 && l < 123)) {
                            valid = false;
                            break;
                        } else {
                            apos++;
                        }
                        if (cpos >= 4) {
                            upos++;
                        }
                        break;
                }
            }
            if (valid) {
                if (cpos == 2) {
                    int y = Integer.parseInt("" + year[0] + year[1]);
                    year[2] = year[0];
                    year[3] = year[1];
                    if (y > 50) {
                        year[0] = '1';
                        year[1] = '9';
                    } else {
                        year[0] = '2';
                        year[1] = '0';
                    }
                } else if (cpos == 1) {
                    return pDate;
                }
                return "" + year[0] + year[1] + year[2] + year[3] + "-" + month[0] + month[1] + "-" + day[0] + day[1];
            } else {
                return pDate;
            }
        } else {
            return pDate;
        }
    }

    public static void testDateTime(String[] dates, String type, String format) {
        System.out.print(type + " processing\n---------------\n");
        int errors = 0;
        for (int j = 0; j < dates.length / 2; j++) {
            String date = dateTimeReader(dates[j * 2], format, type);
            String orgDate = dates[j * 2];
            String resDate = dates[j * 2 + 1];
            boolean datesEqual = date.equals(resDate);
            System.out.println("[" + orgDate + "]" + ((orgDate.length() > 8) ? "\t" : "\t\t") + "[" + date + "]" + ((date.length() > 8) ? "\t" : "\t\t") + datesEqual);
            if (!datesEqual) {
                errors++;
            }
        }
        System.out.print("----------------\nNumber of errors: " + errors + "\n");
    }

    public static int createBoxTextOutput(StringBuffer output, int pWidth) {
        StringTokenizer tokens = new StringTokenizer(output.toString(), "\n ", true);
        output.delete(0, output.length());
        int lWidth = 0;
        int rows = 1;
        while (tokens.hasMoreTokens()) {
            String next = tokens.nextToken();
            if (next.equals("\n")) {
                lWidth = 0;
                rows++;
                output.append('\n');
                continue;
            }
            int tokenLength = next.length();
            if (tokenLength + lWidth > pWidth) {
                lWidth = 0;
                rows++;
                output.append('\n');
                while (tokenLength > pWidth) {
                    String part = next.substring(0, pWidth);
                    output.append(part + '\n');
                    next = next.substring(pWidth, tokenLength);
                    tokenLength -= pWidth;
                    rows++;
                }
                if (tokenLength != 0 && !next.equals(" ")) {
                    lWidth += tokenLength;
                    output.append(next);
                }
            } else {
                if (!next.equals(" ") || (next.equals(" ") && lWidth > 0)) {
                    lWidth += tokenLength;
                    output.append(next);
                }
            }
        }
        return rows;
    }

    public static void replaceNewlineWithBreak(String textValue, DOM newAttachPoint) {
        StringTokenizer tokens = new StringTokenizer(textValue, "\n\r", true);
        boolean brokeLast = false;
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken();
            if (token.equals("\n")) {
                newAttachPoint.addElem("br");
                brokeLast = true;
            } else if (token.equals("\r")) {
                if (brokeLast != true) {
                    newAttachPoint.addElem("br");
                } else {
                    brokeLast = false;
                }
            } else {
                newAttachPoint.createUnconnectedText(token).moveToParent(newAttachPoint);
                brokeLast = false;
            }
        }
    }

    /**
   * Determines if the address host address specified is a valid IP4 IP address, of
   * the form:
   * 
   *     d.d.d.d	
   *     d.d.d	
   *     d.d
   *     d
   *     
   * @param inetAddress the host address to be checked as an IP address
   * @return true if the host address specified is a valid IP address, false otherwise.
   */
    public static boolean isInet4IPAddress(String inetAddress) {
        StringTokenizer sTok = new StringTokenizer(inetAddress, ".");
        boolean isIPAddress = inetAddress.length() > 0;
        while (sTok.hasMoreTokens() && isIPAddress) {
            String addressPart = sTok.nextToken();
            for (int c = 0; c < addressPart.length() && isIPAddress; c++) {
                char addressChar = addressPart.charAt(c);
                isIPAddress = addressChar >= '0' && addressChar <= '9';
            }
        }
        return isIPAddress;
    }

    public static final String getHostName() {
        if (gHostName == null) {
            InetAddress addr;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException x) {
                throw new ExInternal("Cannot get getLocalHost");
            }
            gHostName = addr.getHostName();
        }
        return gHostName;
    }

    private static String gHostIP = null;

    public static final String getHostIP() {
        if (gHostIP == null) {
            InetAddress addr;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException x) {
                throw new ExInternal("Cannot get getLocalHost");
            }
            gHostIP = addr.getHostAddress();
        }
        return gHostIP;
    }

    public static final String stringNormaliseEndOfLine(String pInputString, String pOutputEOLDelimiter) {
        return stringNormaliseEndOfLineStringBuffer(pInputString, pOutputEOLDelimiter).toString();
    }

    public static final StringBuffer stringNormaliseEndOfLineStringBuffer(String pInputString, String pOutputEOLDelimiter) {
        StringBuffer lStringBuffer = new StringBuffer(pInputString.length());
        StringTokenizer lStringTokenizer = new StringTokenizer(pInputString, "\r\n", true);
        boolean lCR = false;
        while (lStringTokenizer.hasMoreTokens()) {
            String lString = lStringTokenizer.nextToken();
            if ("\r".equals(lString)) {
                if (lCR) {
                    lStringBuffer.append(pOutputEOLDelimiter);
                }
                lCR = true;
            } else if ("\n".equals(lString)) {
                lStringBuffer.append(pOutputEOLDelimiter);
                lCR = false;
            } else {
                if (lCR) {
                    lStringBuffer.append(pOutputEOLDelimiter);
                }
                lCR = false;
                lStringBuffer.append(lString);
            }
        }
        if (lCR) {
            lStringBuffer.append(pOutputEOLDelimiter);
        }
        return lStringBuffer;
    }

    private static final Pattern regexpParse(String pRegExp, int pFlags) throws ExInternal {
        try {
            return Pattern.compile(pRegExp, pFlags);
        } catch (PatternSyntaxException e) {
            throw new ExInternal("Regexp syntax error", e);
        } catch (IllegalArgumentException e) {
            throw new ExInternal("Illegal flags value passed to Pattern.compile");
        }
    }

    public static final Pattern regexpParse(String pRegExp) {
        return regexpParse(pRegExp, 0);
    }

    public static final Pattern regexpParseIgnoreCase(String pRegExp) {
        return regexpParse(pRegExp, Pattern.CASE_INSENSITIVE);
    }

    public static final String[] regexpMatch(String pRegExp, String pValue) {
        Pattern lPattern = regexpParse(pRegExp);
        Matcher lMatcher = lPattern.matcher(pValue);
        if (!lMatcher.find()) {
            return null;
        }
        int lGroupCount = lMatcher.groupCount() + 1;
        String[] lResults = new String[lGroupCount];
        for (int g = 0; g < lGroupCount; g++) {
            lResults[g] = lMatcher.group(g);
        }
        return lResults;
    }

    public static final boolean regexpMatches(String pRegExp, String pValue) {
        return Pattern.matches(pRegExp, pValue);
    }

    public static final boolean isInteger(String pString) {
        try {
            Integer.parseInt(pString);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static final double roundDouble(double pDouble, int pPlaces) {
        return Math.round(pDouble * Math.pow(10, (double) pPlaces)) / Math.pow(10, (double) pPlaces);
    }

    public static final int toInteger(String pString) {
        try {
            return Integer.parseInt(pString);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public static final String obfuscateValue(String str) {
        return str.replaceAll(".", String.valueOf((char) 160));
    }

    public static final String resolveLink(String pFoxServletMnem, String pLocation) {
        if (pLocation.startsWith("http://") || pLocation.startsWith("https://") || pLocation.startsWith("/") || pLocation.startsWith("./") || pLocation.startsWith("../")) {
            return pLocation;
        } else {
            return pFoxServletMnem + pLocation;
        }
    }

    public static final String getFoxServletMnem(FoxRequest pFoxRequest, Mod pModule) {
        return getFoxServletMnem(pFoxRequest, pModule.getApp());
    }

    public static final String getFoxServletMnem(FoxRequest pFoxRequest, App pApp) {
        return pFoxRequest.getURLContextServletPath() + "/" + pApp.getMnemonicName() + "/";
    }

    public static String md5(String pString) {
        try {
            MessageDigest lMessageDigest = MessageDigest.getInstance("MD5");
            byte[] lByteArray = lMessageDigest.digest(pString.getBytes());
            return new BigInteger(1, lByteArray).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new ExInternal("MD5 not implemented", ex);
        }
    }

    public static Key generateHardwareAESKey(int pSize) {
        String lOS = System.getProperty("os.name");
        boolean lIsWindows = false;
        if (lOS.toLowerCase().indexOf("windows") != -1) {
            lIsWindows = true;
        }
        Pattern lMacPattern = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2})", Pattern.MULTILINE);
        StringBuffer lMacs = new StringBuffer(pSize);
        TreeSet lMacSet = new TreeSet();
        Process ps;
        try {
            if (lIsWindows) {
                ps = Runtime.getRuntime().exec("ipconfig /all");
            } else {
                ps = Runtime.getRuntime().exec("/sbin/ifconfig");
            }
            InputStream in = ps.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String lLine = null;
            Matcher lMatcher;
            while ((lLine = reader.readLine()) != null) {
                lMatcher = lMacPattern.matcher(lLine);
                if (lMatcher.find() && lMacs.length() < pSize && !"000000000000".equals(lMatcher.group(0).replaceAll("(-|:)", ""))) {
                    lMacSet.add(lMatcher.group(0).replaceAll("(-|:)", ""));
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new ExInternal("Could not generate hardware key!");
        }
        Iterator lMacIter = lMacSet.iterator();
        while (lMacIter.hasNext()) {
            lMacs.append((String) lMacIter.next());
        }
        lMacs.setLength(pSize);
        int x;
        for (x = (pSize - 1); x > 1; --x) {
            if (lMacs.charAt(x) != 0) {
                break;
            }
        }
        lMacs.setLength(x + 1);
        if (x < (pSize - 1) && x > 0) {
            for (int i = x; i < (pSize - 1); ++i) {
                lMacs.append(lMacs.charAt(i % x));
            }
        } else if (x <= 1) {
            throw new ExInternal("Not enough data gathered to generate hardware key");
        }
        SecretKeySpec lKeySpec = new SecretKeySpec(lMacs.toString().getBytes(), "AES");
        return lKeySpec;
    }

    public static KeyPair generateRSAKeys() {
        KeyPairGenerator lKeyGen;
        try {
            lKeyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ExInternal(e.getMessage());
        }
        lKeyGen.initialize(2048);
        KeyPair lKeys = lKeyGen.generateKeyPair();
        return lKeys;
    }

    public static boolean checkRSAKeyPair(PrivateKey pPrivate, PublicKey pPublic) {
        byte[] lTestData = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
        byte[] lEncrypted, lDecrypted;
        try {
            lEncrypted = encryptBytes(lTestData, pPublic, "RSA/ECB/PKCS1Padding");
            lDecrypted = decryptBytes(lEncrypted, pPrivate, "RSA/ECB/PKCS1Padding");
            if (Arrays.equals(lDecrypted, lTestData)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static PrivateKey decodePrivateKey(String pKey, String pAlgorithm) {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(pAlgorithm);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodeBASE64(pKey));
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ExInternal(e.getMessage());
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new ExInternal(e.getMessage());
        }
    }

    public static PublicKey decodePublicKey(String pKey, String pAlgorithm) {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(pAlgorithm);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodeBASE64(pKey));
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ExInternal(e.getMessage());
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new ExInternal(e.getMessage());
        }
    }

    public static String encodeBASE64(byte[] pBytes) {
        BASE64Encoder b64 = new BASE64Encoder();
        return b64.encode(pBytes);
    }

    public static byte[] decodeBASE64(String pEncoded) {
        BASE64Decoder b64 = new BASE64Decoder();
        try {
            return b64.decodeBuffer(pEncoded);
        } catch (IOException e) {
            return null;
        }
    }

    public static void encryptRSAStream(InputStream pIn, OutputStream pOut, PublicKey pKey) throws Exception {
        byte[] lBuffer = new byte[200];
        int numRead = 0;
        while ((numRead = pIn.read(lBuffer)) != -1) {
            pOut.write(encryptBytes(trimBytes(lBuffer, numRead), pKey, "RSA/ECB/PKCS1Padding"));
        }
        if (pOut != null) {
            pOut.close();
        }
    }

    public static void decryptRSAStream(InputStream pIn, OutputStream pOut, PrivateKey pKey) throws Exception {
        byte[] lBuffer = new byte[256];
        int numRead = 0;
        while ((numRead = pIn.read(lBuffer)) != -1) {
            pOut.write(decryptBytes(trimBytes(lBuffer, numRead), pKey, "RSA/ECB/PKCS1Padding"));
        }
        if (pOut != null) {
            pOut.close();
        }
    }

    public static void encryptAESStream(InputStream pIn, OutputStream pOut, Key pKey, AlgorithmParameterSpec pParamSpec) throws Exception {
        byte[] lBuffer = new byte[11];
        int numRead = 0;
        while ((numRead = pIn.read(lBuffer)) != -1) {
            pOut.write(encryptBytes(trimBytes(lBuffer, numRead), pKey, "AES/CBC/PKCS5Padding", pParamSpec));
        }
        if (pOut != null) {
            pOut.close();
        }
    }

    public static void decryptAESStream(InputStream pIn, OutputStream pOut, Key pKey, AlgorithmParameterSpec pParamSpec) throws Exception {
        byte[] lBuffer = new byte[16];
        int numRead = 0;
        while ((numRead = pIn.read(lBuffer)) != -1) {
            pOut.write(decryptBytes(trimBytes(lBuffer, numRead), pKey, "AES/CBC/PKCS5Padding", pParamSpec));
        }
        if (pOut != null) {
            pOut.close();
        }
    }

    public static byte[] encryptBytes(byte[] pBytes, Key key, String pAlgorithm) throws Exception {
        byte[] cipherText = null;
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance(pAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipherText = cipher.doFinal(pBytes);
        return cipherText;
    }

    public static byte[] decryptBytes(byte[] pBytes, Key key, String pAlgorithm) throws Exception {
        byte[] dectyptedText = null;
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance(pAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, key);
        dectyptedText = cipher.doFinal(pBytes);
        return dectyptedText;
    }

    public static byte[] encryptBytes(byte[] pBytes, Key key, String pAlgorithm, AlgorithmParameterSpec pParamSpec) throws Exception {
        byte[] cipherText = null;
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance(pAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, pParamSpec);
        cipherText = cipher.doFinal(pBytes);
        return cipherText;
    }

    public static byte[] decryptBytes(byte[] pBytes, Key key, String pAlgorithm, AlgorithmParameterSpec pParamSpec) throws Exception {
        byte[] dectyptedText = null;
        Security.addProvider(new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance(pAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, pParamSpec);
        dectyptedText = cipher.doFinal(pBytes);
        return dectyptedText;
    }

    public static byte[] trimBytes(byte[] pBytes, int pLength) {
        byte[] lNewBuffer = null;
        if (pBytes.length == pLength) {
            lNewBuffer = pBytes;
        } else {
            lNewBuffer = new byte[pLength];
            System.arraycopy(pBytes, 0, lNewBuffer, 0, pLength);
        }
        return lNewBuffer;
    }

    private static String upperFirstchar(String pString) {
        return pString.substring(0, 1).toUpperCase() + pString.substring(1);
    }

    public static Object getPrivateMembers(String pMemberPath, Object pStartingObject) throws ExInternal {
        if (pStartingObject == null) {
            return null;
        }
        Object lParentObject = null;
        Object lReturnObject = null;
        String lCutPath = "";
        String lMemberName = "";
        if (pMemberPath.indexOf('.') >= 0) {
            lMemberName = pMemberPath.substring(pMemberPath.lastIndexOf('.') + 1);
            lCutPath = pMemberPath.substring(0, pMemberPath.lastIndexOf('.'));
            lParentObject = getPrivateMembers(lCutPath, pStartingObject);
            if (lParentObject == null) {
                return null;
            }
            try {
                Method lMethod = lParentObject.getClass().getMethod("get" + upperFirstchar(lMemberName), null);
                return lMethod.invoke(lParentObject, null);
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            } catch (NoSuchMethodException ex) {
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
            if (lReturnObject == null) {
                try {
                    Field memberField = lParentObject.getClass().getDeclaredField(lMemberName);
                    memberField.setAccessible(true);
                    lReturnObject = memberField.get(lParentObject);
                } catch (IllegalAccessException e) {
                    throw new ExInternal("Couldn't read field", e);
                } catch (NoSuchFieldException e) {
                }
            }
        } else {
            if (lReturnObject == null) {
                try {
                    Method lMethod = pStartingObject.getClass().getMethod("get" + upperFirstchar(pMemberPath), null);
                    if (lMethod.invoke(pStartingObject, null) instanceof Object) {
                        return lMethod.invoke(pStartingObject, null);
                    } else {
                        return (Integer) lMethod.invoke(pStartingObject, null);
                    }
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                try {
                    Field memberField = pStartingObject.getClass().getDeclaredField(pMemberPath);
                    memberField.setAccessible(true);
                    return memberField.get(pStartingObject);
                } catch (IllegalAccessException e) {
                    throw new ExInternal("Couldn't read field", e);
                } catch (NoSuchFieldException e) {
                }
            }
        }
        return lReturnObject;
    }

    public static void writeFile(File pFile, InputStream pInputStream) throws IOException {
        FileOutputStream destination = new FileOutputStream(pFile);
        try {
            IOUtil.transfer(pInputStream, destination);
        } finally {
            destination.close();
        }
    }

    public static void writeFile(File pFile, Reader pReader) throws IOException {
        FileWriter lWriter = null;
        try {
            lWriter = new FileWriter(pFile);
            int c;
            while ((c = pReader.read()) != -1) {
                lWriter.write(c);
            }
        } finally {
            lWriter.close();
        }
    }

    public static void writeFile(File pFile, String pData) throws IOException {
        LineNumberReader lLineNumberReader = new LineNumberReader(new StringReader(pData));
        FileWriter lWriter = null;
        PrintWriter lPrintWriter;
        String lLineBuffer;
        try {
            lWriter = new FileWriter(pFile);
            lPrintWriter = new PrintWriter(lWriter);
            lLineBuffer = lLineNumberReader.readLine();
            while (lLineBuffer != null) {
                lPrintWriter.println(lLineBuffer);
                lLineBuffer = lLineNumberReader.readLine();
            }
        } finally {
            IOUtil.close(lWriter);
        }
    }

    public static File createDir(String pDirName, File pParentFolder) throws IOException {
        File lNewDir = new File(pParentFolder, pDirName);
        lNewDir.delete();
        lNewDir.mkdir();
        return lNewDir;
    }

    public static File createTempDir(String pDirName, String pTempDirSuffix) throws IOException {
        File lNewDir = File.createTempFile(pDirName, pTempDirSuffix);
        lNewDir.delete();
        lNewDir.mkdir();
        return lNewDir;
    }

    public static void copyDir(File pSourceDir, File pDestinationDir) {
        if (pSourceDir.isDirectory()) {
            if (!pDestinationDir.exists()) {
                pDestinationDir.mkdir();
            }
            String[] children = pSourceDir.list();
            for (int i = 0; i < children.length; i++) {
                copyDir(new File(pSourceDir, children[i]), new File(pDestinationDir, children[i]));
            }
        } else {
            copyFile(pSourceDir, pDestinationDir);
        }
    }

    public static boolean copyFile(File pSource, File pDestination) {
        InputStream in;
        OutputStream out;
        try {
            in = new FileInputStream(pSource);
            out = new FileOutputStream(pDestination);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
   * Gets the index in a string of the closing parenthesis.
   * e.g. if you have a string  "test( the ( quick ( brown ( fox ) jumped ) over ) the ) lazy dog"
   *      and you want only the text inside test() you would remove "test(" and pass the string
   *      as argument. The index returned is then of the matching ). You can then use this in
   *      a substring (but remember to add the offset of the length of "test(" to the result
   * @param pStr a String with the open parenthesis you wish to match removed
   * @return the index of the closing parenthesis
   * @throws ExInternal
   */
    public static int getCloseParenthesisIndex(String pStr) throws ExInternal {
        return getCloseParenthesisIndex(pStr, 1);
    }

    public static int getCloseParenthesisIndex(String pStr, int pDepth) throws ExInternal {
        int openIndex = pStr.indexOf("(");
        int closeIndex = pStr.indexOf(")");
        if (closeIndex == -1) {
            throw new RuntimeException("mismatched parenthesis");
        } else if (openIndex != -1 && openIndex < closeIndex) {
            return openIndex + 1 + getCloseParenthesisIndex(pStr.substring(openIndex + 1), pDepth + 1);
        } else if (pDepth == 1) {
            return closeIndex;
        } else {
            return closeIndex + 1 + getCloseParenthesisIndex(pStr.substring(closeIndex + 1), pDepth - 1);
        }
    }

    public static boolean checkURLStatus(String pURL) {
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(pURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            conn.disconnect();
            conn = null;
            url = null;
        }
    }
}
