package com.uro.common.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ���������� �ʿ��� ������ ��ȯ��� �� ���ڿ� ���۱�ɵ��� �����Ǿ� ����
 */
public class StringUtil {

    public static final int RIGHT = 1;

    public static final int LEFT = 2;

    /**
	 * �־��� ���ڿ��� �̿��Ͽ� ������ ��ġ�κ��� ���ϴ� ���̸�ŭ�� ���ڿ��� ����
	 * 
	 * @param str
	 *            ���ϴ� ���ڿ� ������ �ִ� ���ڿ�
	 * @param offset
	 *            ���ϴ� ���ڿ� ������ġ (1���� ����)
	 * @param leng
	 *            ���ϴ� ���ڿ� ����
	 * @return ���ϴ� ���ڿ� ��ü
	 */
    public static String subString(String str, int offset, int leng) {
        return new String(str.getBytes(), (offset - 1), leng);
    }

    /**
	 * �־��� ���ڿ��� �̿��Ͽ� ������ ��ġ�κ��� �������� ���ڿ��� ����
	 * 
	 * @param str
	 *            ���ϴ� ���ڿ� ������ �ִ� ���ڿ�
	 * @param offset
	 *            ���ϴ� ���ڿ� ������ġ (1���� ����)
	 * @return ���ϴ� ���ڿ� ��ü
	 */
    public static String subString(String str, int offset) {
        byte[] bytes = str.getBytes();
        int size = bytes.length - (offset - 1);
        return new String(bytes, (offset - 1), size);
    }

    /**
	 * �־��� ���ڿ��� ��������Ͽ� �־��� ���̸�ŭ�� ���ڿ��� ���Ͽ� ������.
	 * <p>
	 * 
	 * <pre>
	 *  (��)
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6));
	 * 	��� = &quot;abcd  &quot;
	 * 
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,3));
	 * 	��� = &quot;abc&quot;
	 * 
	 * 	String str = &quot;�����ٶ�&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6));
	 * 	��� = &quot;������&quot;
	 * 
	 * 	String str = &quot;�����ٶ�&quot;;
	 * 	System.out.println(StringUtil.fitString(str,3));
	 * 	��� = &quot;???&quot;
	 * </pre>
	 * 
	 * @param str
	 *            ��� ���ڿ�
	 * @param size
	 *            ������� �ϴ� ���ڿ��� ����
	 * @return �־��� ���̸�ŭ�� ����
	 */
    public static String fitString(String str, int size) {
        return fitString(str, size, StringUtil.LEFT);
    }

    /**
	 * �־��� ���ڿ��� ��������Ͽ� �־��� ���̸�ŭ�� ���ڿ��� ���Ͽ� ������.
	 * <p>
	 * 
	 * <pre>
	 *  (��)
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6,StringUtil.RIGHT));
	 * 	��� = &quot;  abcd&quot;
	 * </pre>
	 * 
	 * @param str
	 *            ��� ���ڿ�
	 * @param size
	 *            ������� �ϴ� ���ڿ��� ����
	 * @param align
	 *            ���������� ����(RIGHT, LEFT)
	 * @return �־��� ���̸�ŭ�� ����
	 */
    public static String fitString(String str, int size, int align) {
        byte[] bts = str.getBytes();
        int len = bts.length;
        if (len == size) {
            return str;
        }
        if (len > size) {
            String s = new String(bts, 0, size);
            if (s.length() == 0) {
                StringBuffer sb = new StringBuffer(size);
                for (int idx = 0; idx < size; idx++) {
                    sb.append("?");
                }
                s = sb.toString();
            }
            return s;
        }
        if (len < size) {
            int cnt = size - len;
            char[] values = new char[cnt];
            for (int idx = 0; idx < cnt; idx++) {
                values[idx] = ' ';
            }
            if (align == StringUtil.RIGHT) {
                return String.copyValueOf(values) + str;
            } else {
                return str + String.copyValueOf(values);
            }
        }
        return str;
    }

    /**
	 * ���ڿ��� �⺻�и���(white space)�� �и��Ͽ� ���ڿ��迭�� ����
	 * 
	 * @param str
	 * @return ���ڿ� �迭
	 */
    public static String[] toStringArray(String str) {
        Vector vt = new Vector();
        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            vt.add(st.nextToken());
        }
        return toStringArray(vt);
    }

    /**
	 * Vector�� ����� ��ü���� �̿��Ͽ� ���ڿ� �迭�� ����
	 * 
	 * @param vt
	 * @return ���ڿ� �迭
	 */
    public static String[] toStringArray(Vector vt) {
        String[] strings = new String[vt.size()];
        for (int idx = 0; idx < vt.size(); idx++) {
            strings[idx] = vt.elementAt(idx).toString();
        }
        return strings;
    }

    /**
	 * �־��� ���ڿ����� ������ ���ڿ����� ������ ���ڿ��� ġȯ�� �װ�� ���ڿ��� ������.
	 * 
	 * @param src
	 * @param from
	 * @param to
	 * @return ���ڿ�
	 */
    public static String replace(String src, String from, String to) {
        if (src == null) return null;
        if (from == null) return src;
        if (to == null) to = "";
        StringBuffer buf = new StringBuffer();
        for (int pos; (pos = src.indexOf(from)) >= 0; ) {
            buf.append(src.substring(0, pos));
            buf.append(to);
            src = src.substring(pos + from.length());
        }
        buf.append(src);
        return buf.toString();
    }

    /**
	 * �־����ڿ��� ������ ���̸� �ʰ��ϴ� ��� ©�󳻰� '...'�� �ٿ� ������.
	 * 
	 * @param str
	 * @param limit
	 * @return ���ڿ�
	 */
    public static String cutString(String str, int limit) {
        if (str == null || limit < 4) return str;
        int len = str.length();
        int cnt = 0, index = 0;
        while (index < len && cnt < limit) {
            if (str.charAt(index++) < 256) cnt++; else cnt += 2;
        }
        if (index < len) str = str.substring(0, index - 1) + "...";
        return str;
    }

    /**
	 * ��Ʈ������ Ư�� ���ڸ� �������� �� ���ڿ����� �����Ѵ�.
	 * 
	 * @param src
	 * @param end
	 * @return
	 */
    public static String cutEndString(String src, String end) {
        if (src == null) return null;
        if (end == null) return src;
        int pos = src.indexOf(end);
        if (pos >= 0) {
            src = src.substring(0, pos);
        }
        return src;
    }

    /**
	 * �־��� ���ڷ� ���ϴ� ������ŭ�� char[] �� ����.
	 * 
	 * @param c
	 *            ���� ����
	 * @param cnt
	 *            ���� ����
	 * @return char array
	 */
    public static char[] makeCharArray(char c, int cnt) {
        char a[] = new char[cnt];
        Arrays.fill(a, c);
        return a;
    }

    /**
	 * �־��� ���ڷ� ���ϴ� ������ŭ�� String �� ����.
	 * 
	 * @param c
	 *            ���� ����
	 * @param cnt
	 *            ���� ����
	 * @return ���ϴ� ���� ��ŭ ��� ���ڿ�
	 */
    public static String getString(char c, int cnt) {
        return new String(makeCharArray(c, cnt));
    }

    /**
	 * String�� ���� ����� ��ش�.
	 * 
	 * @param lstr
	 *            ��� String
	 * @return String ��� String
	 */
    public static String getLeftTrim(String lstr) {
        if (!lstr.equals("")) {
            int strlen = 0;
            int cptr = 0;
            boolean lpflag = true;
            char chk;
            strlen = lstr.length();
            cptr = 0;
            lpflag = true;
            do {
                chk = lstr.charAt(cptr);
                if (chk != ' ') {
                    lpflag = false;
                } else {
                    if (cptr == strlen) {
                        lpflag = false;
                    } else {
                        cptr++;
                    }
                }
            } while (lpflag);
            if (cptr > 0) {
                lstr = lstr.substring(cptr, strlen);
            }
        }
        return lstr;
    }

    /**
	 * String�� ���� ����� ��ش�.
	 * 
	 * @param lstr
	 *            ��� String
	 * @return String ��� String
	 */
    public static String getRightTrim(String lstr) {
        if (!lstr.equals("")) {
            int strlen = 0;
            int cptr = 0;
            boolean lpflag = true;
            char chk;
            strlen = lstr.length();
            cptr = strlen;
            lpflag = true;
            do {
                chk = lstr.charAt(cptr - 1);
                if (chk != ' ') {
                    lpflag = false;
                } else {
                    if (cptr == 0) {
                        lpflag = false;
                    } else {
                        cptr--;
                    }
                }
            } while (lpflag);
            if (cptr < strlen) {
                lstr = lstr.substring(0, cptr);
            }
        }
        return lstr;
    }

    /**
	 * ���� Ư�� ���� ��ŭ ���Ѵ�.
	 * 
	 * @param str
	 *            ��� String
	 * @param Len
	 *            ����
	 * @return ��� String
	 */
    public static String getLeft(String str, int Len) {
        if (str.equals(null)) return "";
        return str.substring(0, Len);
    }

    /**
	 * ���� Ư�� ���� ��ŭ ���Ѵ�.
	 * 
	 * @param str
	 *            ��� String
	 * @param Len
	 *            ����
	 * @return ��� String
	 */
    public static String getRight(String str, int Len) {
        if (str.equals(null)) return "";
        String dest = "";
        for (int i = (str.length() - 1); i >= 0; i--) dest = dest + str.charAt(i);
        str = dest;
        str = str.substring(0, Len);
        dest = "";
        for (int i = (str.length() - 1); i >= 0; i--) dest = dest + str.charAt(i);
        return dest;
    }

    /**
	 * �Էµ� ���� ���̸�, replace ������ ��ü�Ѵ�.
	 * 
	 * @param str
	 *            �Է�
	 * @param replace
	 *            ��ü ��
	 * @return ����
	 */
    public static String nvl(String str, String replace) {
        if (str == null) {
            return replace;
        } else {
            return str;
        }
    }

    /**
	 * Null �Ǵ� ����̸� �ٸ� ������ ��ü�Ѵ�.
	 * 
	 * @param str
	 *            �Է�
	 * @param replace
	 *            ��ü ��
	 * @return ��
	 */
    public static String checkEmpty(String str, String replace) {
        if (str == null || str.equals("")) {
            return replace;
        } else {
            return str;
        }
    }

    /**
	 * ���ڸ� ��ģ��.
	 * 
	 * @param str
	 *            ����
	 * @return ������ ����
	 */
    public static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuffer(strLen).append(Character.toTitleCase(str.charAt(0))).append(str.substring(1)).toString();
    }

    /**
	 * Exception ������ String���� ��ȯ�Ѵ�.
	 * 
	 * @param e
	 *            Exception
	 * @return String ��ȯ�� Exception
	 */
    public static String getErrorTrace(Exception e) {
        if (e == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String errTrace = sw.toString();
        return errTrace;
    }

    /**
	 * XML���� ����ϴ� Ư�� ���ڸ� ��ȯ�Ѵ�.
	 * 
	 * @param s
	 * @return
	 */
    public static String escapeXml(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
	 * token���ڵ��� ArrayList�� ��ȯ
	 * 
	 * @param s
	 *            �����ڸ� ������ ���ڿ�
	 * @param token
	 *            ������
	 * @return List ���ڵ��� ����� List object
	 */
    public static List getTokenList(String s, String token) {
        List tokenList = new ArrayList();
        if (s != null && !"".equals(s)) {
            StringTokenizer st = new StringTokenizer(s, token);
            for (int i = 0; st.hasMoreTokens(); i++) {
                tokenList.add(st.nextToken().trim());
            }
        }
        return tokenList;
    }

    /**
	 * ����(token) ���ڿ�(s) �߿��� ������ ī��Ʈ�� ������
	 * 
	 * @param s
	 *            �����ڸ� ������ ���ڿ�
	 * @param token
	 *            ������
	 * @return int word count
	 */
    public static int getTokenLength(String s, String token) {
        if (s == null) return 0;
        int len = 0;
        StringTokenizer st = new StringTokenizer(s, token);
        while (st.hasMoreTokens()) {
            len++;
        }
        return len;
    }

    /**
	 * ����(token) ������(s)���� Ư�� index��° ���ڸ� ������
	 * 
	 * @param index
	 *            ������ ������ index
	 * @param s
	 *            �����ڸ� ������ ���ڿ�
	 * @param token
	 *            ������
	 * @return String index��° ����
	 */
    public static String getToken(int index, String s, String token) {
        if (s == null) return "";
        StringTokenizer st = new StringTokenizer(s, token);
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; st.hasMoreTokens(); i++) {
            if (index == i) {
                sb.append(st.nextToken());
                break;
            }
            st.nextToken();
        }
        if (sb.toString().length() > 0) return sb.toString().trim(); else return "";
    }

    /**
	 * ����(token) ������(s)���� Ư�� index��° ���ڸ� ������. <BR>
	 * ����(s)�� null�Ͻ� nvl��ȯ
	 * 
	 * @param index
	 *            ������ ������ index
	 * @param s
	 *            �����ڸ� ������ ���ڿ�
	 * @param token
	 *            ������
	 * @param nvl
	 *            null��° ��ȯ�� nvl
	 * @return String index��° ����
	 */
    public static String getToken(int index, String s, String token, String nvl) {
        if (s == null) return nvl;
        StringTokenizer st = new StringTokenizer(s, token);
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; st.hasMoreTokens(); i++) {
            if (index == i) {
                sb.append(st.nextToken());
                break;
            }
            st.nextToken();
        }
        if (sb.toString().length() > 0) return sb.toString().trim(); else return nvl;
    }

    /**
	 * ��� �޽��� HTML ������ ���� ����Ѵ�.
	 * @param res
	 * @param message
	 * @throws IOException
	 */
    public static void htmlPrint(HttpServletResponse res, String content) throws IOException {
        res.setContentType("text/html; charset=euc-kr");
        PrintWriter out = res.getWriter();
        out.println(content);
    }

    /**
	 * �Ϲ� �޽����� ����Ѵ�.
	 * @param res
	 * @param message
	 * @throws IOException
	 */
    public static void toMessage(HttpServletResponse res, String message) throws IOException {
        res.setContentType("text/html; charset=euc-kr");
        PrintWriter out = res.getWriter();
        out.println(message);
    }

    public static String getParam(HttpServletRequest req, String pName, String defaultValue) {
        String value = req.getParameter(pName.trim());
        String retr = (value == null ? defaultValue : value.trim());
        return retr;
    }

    /**
	 * �ش� ���ڿ��� Ư�� character�� ������ ���ڿ��� return.
	 *
	 * <blockquote><pre>
	 * ��1) StringUtil.removeCharacter("33,111,000", ',') => 33111000
	 * ��2) StringUtil.removeCharacter("2002/02/02", '/') => 20020202
	 * </pre></blockquote>
	 *
	 * @param     sStr         �� ���ڿ�
	 * @param     Chr         ������ ���� ĳ�����ΰ��
	 * @return    retr         charValue�� ������ ���ڿ�
	 *
	 */
    public static String removeCharacter(String sStr, char sChr) {
        if (sStr == null) {
            return "";
        }
        char[] fromchars = sStr.toCharArray();
        StringBuffer tochars = new StringBuffer(fromchars.length);
        for (int i = 0, p = fromchars.length; i < p; i++) {
            if (sChr != fromchars[i]) tochars.append(fromchars[i]);
        }
        return tochars.toString();
    }

    /**
	 * �ش� ���ڿ��� Ư�� character�� ������ ���ڿ��� return.
	 *
	 * <blockquote><pre>
	 * ��1) StringUtil.removeCharacter("33,111,000", ",") => 33111000
	 * ��2) StringUtil.removeCharacter("2002/02/02", "/") => 20020202
	 * </pre></blockquote>
	 *
	 * @param     sStr         �� ���ڿ�
	 * @param     sChr         ������ ���� ��Ʈ���ΰ��
	 * @return    retr         charValue�� ������ ���ڿ�
	 *
	 */
    public static String removeCharacter(String sStr, String sChr) {
        if (sStr == null) return "";
        if (sChr == null) return "";
        if (sChr.length() == 1) {
            return removeCharacter(sStr, sChr.charAt(0));
        } else {
            return removeCharacter2(sStr, sChr);
        }
    }

    public static String removeCharacter2(String sStr, String sChr) {
        String retr = "";
        StringTokenizer st = new StringTokenizer(sStr, sChr);
        while (st.hasMoreTokens()) {
            retr += st.nextToken();
        }
        return retr;
    }

    /**
     * String�� ũ�Ⱑ �־��� ũ�⺸�� ������ �������� "0"�� ä���.
     **/
    public static String padString(String param, int size) {
        String ret = param;
        try {
            Integer.parseInt(ret);
        } catch (NumberFormatException nfe) {
            return ret;
        }
        int length = ret.length();
        for (int i = 0; i < size - length; i++) {
            ret = "0" + ret;
        }
        return ret;
    }

    public static String capCase(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        str = str.toLowerCase();
        char[] chr = str.toCharArray();
        chr[0] = Character.toTitleCase(chr[0]);
        for (int i = 0; i < strLen; i++) {
            if (chr[i] == '_') {
                i++;
                chr[i] = Character.toTitleCase(chr[i]);
                i--;
            }
        }
        return new String(chr);
    }

    public static String camelCase(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        str = str.toLowerCase();
        char[] chr = str.toCharArray();
        int p = 0;
        for (int i = 0; i < strLen; i++) {
            if (chr[i] == '_') {
                i++;
                chr[p] = Character.toTitleCase(chr[i]);
            } else {
                chr[p] = chr[i];
            }
            p++;
        }
        return new String(chr, 0, p);
    }

    /**
     * ��ȣȭ �Ѵ�.
     *
     */
    public static String encryptString(String str) {
        StringBuffer sb = new StringBuffer();
        int i;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            byte[] md5Bytes = md5.digest();
            for (i = 0; i < md5Bytes.length; i++) {
                sb.append(md5Bytes[i]);
            }
        } catch (Exception e) {
        }
        return sb.toString();
    }
}
