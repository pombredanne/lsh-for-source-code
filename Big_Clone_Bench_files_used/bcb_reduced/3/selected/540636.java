package com.uro.common.util;

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
 * 공통적으로 필요한 데이터 변환기능 및 문자열 조작기능등이 구현되어 있음
 */
public class StringUtil {

    public static final int RIGHT = 1;

    public static final int LEFT = 2;

    /**
	 * 주어진 문자열을 이용하여 지정한 위치로부터 원하는 길이만큼의 문자열을 구함
	 * 
	 * @param str
	 *            원하는 문자열 가지고 있는 문자열
	 * @param offset
	 *            원하는 문자열 시작위치 (1부터 시작)
	 * @param leng
	 *            원하는 문자열 길이
	 * @return 원하는 문자열 객체
	 */
    public static String subString(String str, int offset, int leng) {
        return new String(str.getBytes(), (offset - 1), leng);
    }

    /**
	 * 주어진 문자열을 이용하여 지정한 위치로부터 끝까지의 문자열을 구함
	 * 
	 * @param str
	 *            원하는 문자열 가지고 있는 문자열
	 * @param offset
	 *            원하는 문자열 시작위치 (1부터 시작)
	 * @return 원하는 문자열 객체
	 */
    public static String subString(String str, int offset) {
        byte[] bytes = str.getBytes();
        int size = bytes.length - (offset - 1);
        return new String(bytes, (offset - 1), size);
    }

    /**
	 * 주어진 문자열을 대상으로하여 주어진 길이만큼의 문자열을 생성하여 리턴함.
	 * <p>
	 * 
	 * <pre>
	 *  (예)
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6));
	 * 	출력 = &quot;abcd  &quot;
	 * 
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,3));
	 * 	출력 = &quot;abc&quot;
	 * 
	 * 	String str = &quot;가나다라&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6));
	 * 	출력 = &quot;가나다&quot;
	 * 
	 * 	String str = &quot;가나다라&quot;;
	 * 	System.out.println(StringUtil.fitString(str,3));
	 * 	출력 = &quot;???&quot;
	 * </pre>
	 * 
	 * @param str
	 *            대상 문자열
	 * @param size
	 *            만들고자 하는 문자열의 길이
	 * @return 주어진 길이만큼의 문자
	 */
    public static String fitString(String str, int size) {
        return fitString(str, size, StringUtil.LEFT);
    }

    /**
	 * 주어진 문자열을 대상으로하여 주어진 길이만큼의 문자열을 생성하여 리턴함.
	 * <p>
	 * 
	 * <pre>
	 *  (예)
	 * 	String str = &quot;abcd&quot;;
	 * 	System.out.println(StringUtil.fitString(str,6,StringUtil.RIGHT));
	 * 	출력 = &quot;  abcd&quot;
	 * </pre>
	 * 
	 * @param str
	 *            대상 문자열
	 * @param size
	 *            만들고자 하는 문자열의 길이
	 * @param align
	 *            정열기준의 방향(RIGHT, LEFT)
	 * @return 주어진 길이만큼의 문자
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
	 * 문자열을 기본분리자(white space)로 분리하여 문자열배열을 생성함
	 * 
	 * @param str
	 * @return 문자열 배열
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
	 * Vector에 저장된 객체들을 이용하여 문자열 배열을 생성함
	 * 
	 * @param vt
	 * @return 문자열 배열
	 */
    public static String[] toStringArray(Vector vt) {
        String[] strings = new String[vt.size()];
        for (int idx = 0; idx < vt.size(); idx++) {
            strings[idx] = vt.elementAt(idx).toString();
        }
        return strings;
    }

    /**
	 * 주어진 문자열에서 지정한 문자열값을 지정한 문자열로 치환후 그결과 문자열을 리턴함.
	 * 
	 * @param src
	 * @param from
	 * @param to
	 * @return 문자열
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
	 * 주어진문자열이 지정한 길이를 초과하는 경우 짤라내고 '...'을 붙여 리턴함.
	 * 
	 * @param str
	 * @param limit
	 * @return 문자열
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
	 * 스트링에서 특정 문자를 시작으로 끝 문자열까지 삭제한다.
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
	 * 주어진 문자로 원하는 갯수만큼의 char[] 를 생성함.
	 * 
	 * @param c
	 *            생성할 문자
	 * @param cnt
	 *            생성할 갯수
	 * @return char array
	 */
    public static char[] makeCharArray(char c, int cnt) {
        char a[] = new char[cnt];
        Arrays.fill(a, c);
        return a;
    }

    /**
	 * 주어진 문자로 원하는 갯수만큼의 String 을 생성함.
	 * 
	 * @param c
	 *            생성할 문자
	 * @param cnt
	 *            생성할 갯수
	 * @return 원하는 갯수 많큼 생성된 문자열
	 */
    public static String getString(char c, int cnt) {
        return new String(makeCharArray(c, cnt));
    }

    /**
	 * String의 좌측 공백을 없앤다.
	 * 
	 * @param lstr
	 *            대상 String
	 * @return String 결과 String
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
	 * String의 우측 공백을 없앤다.
	 * 
	 * @param lstr
	 *            대상 String
	 * @return String 결과 String
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
	 * 좌측에서 특정 길이 만큼 취한다.
	 * 
	 * @param str
	 *            대상 String
	 * @param Len
	 *            길이
	 * @return 결과 String
	 */
    public static String getLeft(String str, int Len) {
        if (str.equals(null)) return "";
        return str.substring(0, Len);
    }

    /**
	 * 우측에서 특정 길이 만큼 취한다.
	 * 
	 * @param str
	 *            대상 String
	 * @param Len
	 *            길이
	 * @return 결과 String
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
	 * 입력된 값이 널이면, replace 값으로 대체한다.
	 * 
	 * @param str
	 *            입력
	 * @param replace
	 *            대체 값
	 * @return 문자
	 */
    public static String nvl(String str, String replace) {
        if (str == null) {
            return replace;
        } else {
            return str;
        }
    }

    /**
	 * Null 또는 공백이면 다른 값으로 대체한다.
	 * 
	 * @param str
	 *            입력
	 * @param replace
	 *            대체 값
	 * @return 문
	 */
    public static String checkEmpty(String str, String replace) {
        if (str == null || str.equals("")) {
            return replace;
        } else {
            return str;
        }
    }

    /**
	 * 문자를 합친다.
	 * 
	 * @param str
	 *            문자
	 * @return 합쳐진 문자
	 */
    public static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuffer(strLen).append(Character.toTitleCase(str.charAt(0))).append(str.substring(1)).toString();
    }

    /**
	 * Exception 정보를 String으로 변환한다.
	 * 
	 * @param e
	 *            Exception
	 * @return String 변환된 Exception
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
	 * XML에서 사요하는 특수 문자를 변환한다.
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
	 * token문자들을 ArrayList로 반환
	 * 
	 * @param s
	 *            구분자를 포함한 문자열
	 * @param token
	 *            구분자
	 * @return List 문자들이 저장된 List object
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
	 * 구분(token) 문자열(s) 중에서 문자의 카운트를 가져옴
	 * 
	 * @param s
	 *            구분자를 포함한 문자열
	 * @param token
	 *            구분자
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
	 * 구분(token) 문자중(s)에서 특정 index번째 문자를 가져옴
	 * 
	 * @param index
	 *            가져올 문자의 index
	 * @param s
	 *            구분자를 포함한 문자열
	 * @param token
	 *            구분자
	 * @return String index번째 문자
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
	 * 구분(token) 문자중(s)에서 특정 index번째 문자를 가져옴. <BR>
	 * 문자(s)가 null일시 nvl반환
	 * 
	 * @param index
	 *            가져올 문자의 index
	 * @param s
	 *            구분자를 포함한 문자열
	 * @param token
	 *            구분자
	 * @param nvl
	 *            null일째 반환될 nvl
	 * @return String index번째 문자
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
	 * 경고 메시지 HTML 문장을 만들어서 출력한다.
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
	 * 일반 메시지를 출력한다.
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
	 * 해당 문자열의 특정 character를 제거한 문자열을 return.
	 *
	 * <blockquote><pre>
	 * 예1) StringUtil.removeCharacter("33,111,000", ',') => 33111000
	 * 예2) StringUtil.removeCharacter("2002/02/02", '/') => 20020202
	 * </pre></blockquote>
	 *
	 * @param     sStr         원 문자열
	 * @param     Chr         삭제할 문자 캐릭터인경우
	 * @return    retr         charValue가 삭제된 문자열
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
	 * 해당 문자열의 특정 character를 제거한 문자열을 return.
	 *
	 * <blockquote><pre>
	 * 예1) StringUtil.removeCharacter("33,111,000", ",") => 33111000
	 * 예2) StringUtil.removeCharacter("2002/02/02", "/") => 20020202
	 * </pre></blockquote>
	 *
	 * @param     sStr         원 문자열
	 * @param     sChr         삭제할 문자 스트링인경우
	 * @return    retr         charValue가 삭제된 문자열
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
     * String의 크기가 주어진 크기보다 작으면 나머지는 "0"로 채운다.
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
     * 암호화 한다.
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
