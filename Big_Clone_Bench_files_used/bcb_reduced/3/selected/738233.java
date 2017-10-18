package net.sf.immc.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 静态类： 公共操作方法
 * 
 * @author <b>oxidy</b>, Copyright &#169; 2007-2010
 * @version 0.1,2009/12/21
 * @version 0.2,2010/12/13
 */
public class Utils {

    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * 默认构造方法
     */
    public Utils() {
    }

    /**
     * 生成UUID，返回字符串
     * 
     * @Description GUID是一个128位长的数字，一般用16进制表示
     * @return 生成的惟一的标识字符串
     * @since jdk 1.5
     */
    public static String getUuid() {
        UUID uid = UUID.randomUUID();
        return uid.toString();
    }

    /**
     * 获取MD5加密后的字符串
     * 
     * @param str
     *            需要加密的字符串
     * @return 加密后的字符串
     */
    public static String getMD5(String str) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            String pwd = new BigInteger(1, md5.digest()).toString(16);
            return pwd;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return str;
    }

    /**
     * Replaces all instances of oldString with newString in line.
     *
     * @param line the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     *
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replace(String line, String oldString, String newString) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
     * Unescapes the String by converting XML escape sequences back into normal
     * characters.
     *
     * @param string the string to unescape.
     * @return the string with appropriate characters unescaped.
     */
    public static final String unescapeFromXML(String string) {
        string = replace(string, "&lt;", "<");
        string = replace(string, "&gt;", ">");
        string = replace(string, "&quot;", "\"");
        return replace(string, "&amp;", "&");
    }

    /**
     * Pseudo-random number generator object for use with randomString().
     * The Random class is not considered to be cryptographically secure, so
     * only use these random Strings for low to medium security applications.
     */
    private static Random randGen = new Random();

    /**
     * Array of numbers and letters of mixed case. Numbers appear in the list
     * twice so that there is a more equal chance that a number will be picked.
     * We can use the array to get a random number or letter by picking a random
     * array index.
     */
    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
     * Returns a random String of numbers and letters (lower and upper case)
     * of the specified length. The method uses the Random class that is
     * built-in to Java which is suitable for low to medium grade security uses.
     * This means that the output is only pseudo random, i.e., each number is
     * mathematically generated so is not truly random.<p>
     *
     * The specified length must be at least one. If not, the method will return
     * null.
     *
     * @param length the desired length of the random String to return.
     * @return a random String of numbers and letters of the specified length.
     */
    public static final String randomString(int length) {
        if (length < 1) {
            return null;
        }
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randGen.nextInt(71)];
        }
        return new String(randBuffer);
    }

    /**
     * 随机生成长度为length的字符串，排除0,o,1和I，以免误解
     * 
     * @param length
     * @return
     */
    public static String genPassword(int length) {
        if (length < 1) {
            return null;
        }
        String[] strChars = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "a" };
        StringBuffer strPassword = new StringBuffer();
        int nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
        for (int i = 0; i < length; i++) {
            nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
            strPassword.append(strChars[nRand % (strChars.length - 1)]);
        }
        return strPassword.toString();
    }

    /**
     * 随机生成长度为length的数字字符串
     * 
     * @param length
     * @return
     */
    public static String genNumPassword(int length) {
        if (length < 1) {
            return null;
        }
        String[] strChars = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        StringBuffer strPassword = new StringBuffer();
        int nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
        for (int i = 0; i < length; i++) {
            nRand = (int) java.lang.Math.round(java.lang.Math.random() * 100);
            strPassword.append(strChars[nRand % (strChars.length - 1)]);
        }
        return strPassword.toString();
    }
}
