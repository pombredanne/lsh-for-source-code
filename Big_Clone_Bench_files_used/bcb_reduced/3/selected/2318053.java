package wr3.util;

import java.io.*;
import java.security.*;
import java.text.*;
import java.util.*;

/**
 * usage:
 * <pre>
	Stringx.left 		("abc__cdf__fgh", "__"); 	// return "abc"
  	Stringx.leftback 	("abc__cdf__fgh", "__"); 	// return "abc__cdf"
	Stringx.right 		("abc__cdf__fgh", "__"); 	// return "cdf__fgh"
	Stringx.rightback 	("abc__cdf__fgh", "__"); 	// return "fgh"
	Stringx.between 	(); 
	Stringx.replace 	("hello world", "world", "web-report"); 				// return "hello web-report"
	Stringx.replace 	("hello world", {"hello","world"}, {"hi", "money"}); 	// return "hi money"
	Stringx.replaceAll 	();
	Stringx.replaceFirst(); 						// == replace ()
	Stringx.replaceBetween ();
	Stringx.trim ();								// " ab   cde  " --> "abcde"
	Stringx.split 		("this--is--good", "--");	// return new String[]{"this", "is", "good"}
	Stringx.split		("1,'ab,cd',2",",");		// {1, 'ab, cd', 2}
	Stringx.split		("1,'ab,cd',2",",","\'");	// {1, 'ab,cd', 2}
	Stringx.split2ints  ("3,2,6,4");				// return {3,2,6,4}
	Stringx.join ({"aa","bb","cc"}, ",", "(", ")")	// return "(aa),(bb),(cc)"
	Stringx.join		({"aa","bb","cc"}, " | ")	// return "aa | bb | cc"
	Stringx.join		({100,300,200}, ",")		// return "100,300,200"
	Stringx.join		({false,true,false}, ",")	// return "false,true,false"
	Stringx.unique		({"aa", "aa", "cc", "aa"})	// return "aa", "cc"
	Stringx.set			({"aa", "aa", "cc", "aa"})	// return "aa", "cc"
	Stringx.isSet		({"aa", "aa", "cc", "aa"})	// return false
	Stringx.sort		({"bb", "aa", "cc"})		// return "aa", "bb", "cc"
	Stringx.group		({"aa", "aa", "cc", "aa"});	// return {{"aa","cc"}, {3,1}}
	Stringx.include 	("aa", {"bb", "cc", "aa"})	// return true	
	Stringx.in 			("aa", {"bb", "cc", "aa"})	// return true	
	Stringx.isSubset 	({"aa", "bb"}, {"bb", "cc", "aa"})	// return true		
	Stringx.indexOf     ("aa", {"bb", "cc", "aa"}); // return 2
	Stringx.insert		("aa.bb.xml", ".", "_out");	// return aa_out.bb.xml
	Stringx.insertBack	("aa.bb.xml", ".", "_out");	// return aa.bb_out.xml
	Stringx.fill        ("+-",10);					// return "+-+-+-+-+-"
	Stringx.padLeft     ("abc", 10, ".");			// return ".......abc"
	Stringx.padRight	("abc", 10, "."); 			// return "abc......."
	Stringx.padLeft		({"a","abc","ab"}, " ");	// {"  a", "abc", " ab"}
	Stringx.padRight	({"a","abc","ab"}, ".");	// {"a..", "abc", "ab."}
	-------------- following methods is mainly for jsp --------------
	Stringx.base64enc   ("abc");					// return base64string
	Stringx.base64dec   ("base64string");			// return "abc"
	Stringx.md5 ("a");								// return string of 32 length
	Stringx.check		("", "default");		   	// return "default"
	Stringx.nullity		("");						// return true;
	Stringx.isTrue		("True");					// return true;
	-------------- following methods is for expression -------
	Stringx.getExpVars ("A * (B - C/2)");			// return {"A","B","C"}
	-------------- 
	Stringx.map2array (Map);						// return String[2][]
	Stringx.collection2array (c);				// return String[]
	Stringx.list2array (List);						// return String[]
	--------------
	Stringx.format ("{0}'s age is {1}", new Object[]{...})	// return "James's age is 100"
 *
 * </pre>
 * @see Numberx, Datetime
 * @see TestStringx
 */
public class Stringx {

    /**
	 * @author jamesqiu 2006-11-21
	 * capitalize a string (uppercase first char)
	 */
    public static String capitalize(String src) {
        if (src == null || src.equals("")) return src;
        char[] cs = src.toCharArray();
        cs[0] = Character.toUpperCase(cs[0]);
        return new String(cs);
    }

    /**
	 * @author jamesqiu 2006-11-21
	 * like capitalize but uppercase first char AND lowercase other chars
	 * @param more set as true/false are both ok 
	 */
    public static String capitalize(String src, boolean more) {
        if (src == null || src.equals("")) return src;
        return capitalize(src.toLowerCase());
    }

    /**
	 * Searches a string from left to right and returns the leftmost characters of the string.
	 * left ("111 222 333", " ") --> "111"
	 */
    public static String left(String src, String substr) {
        int idx = src.indexOf(substr);
        if (idx == -1) {
            return "";
        } else {
            return src.substring(0, idx);
        }
    }

    /**
	 * Searches a string from right to left and returns a substring.
	 * leftback ("111 222 333", " ") --> "111 222"
	 * notice:
	 * leftback ("a,0,0,0", ",0,0") --> "a", NOT "a,0"
	 */
    public static String leftback(String src, String substr) {
        int idx = src.indexOf(substr);
        if (idx == -1) {
            return "";
        }
        int len = substr.length();
        int tmp;
        while (true) {
            tmp = src.indexOf(substr, idx + len);
            if (tmp == -1) {
                break;
            } else {
                idx = tmp;
            }
        }
        return src.substring(0, idx);
    }

    /**
	 * Returns the rightmost characters in the string.
	 * right ("111 222 333", " ") --> "222 333"
	 */
    public static String right(String src, String substr) {
        int idx = src.indexOf(substr);
        if (idx == -1) {
            return "";
        } else {
            return src.substring(idx + substr.length());
        }
    }

    /**
	 * Returns the rightmost characters in a string.
	 * rightback ("111 222 333", " ") --> "333"
	 */
    public static String rightback(String src, String substr) {
        int idx = src.indexOf(substr);
        if (idx == -1) {
            return "";
        }
        int len = substr.length();
        int tmp;
        while (true) {
            tmp = src.indexOf(substr, idx + len);
            if (tmp == -1) {
                break;
            } else {
                idx = tmp;
            }
        }
        return src.substring(idx + len);
    }

    /**
	 * return the substring between other 2 substrings
	 */
    public static String between(String src, String str1, String str2) {
        String rt = src;
        rt = right(rt, str1);
        rt = left(rt, str2);
        return rt;
    }

    /**
	 * Replaces first specific substring in a string with 
	 * new substring. Case sensitive.
	 */
    public static String replaceFirst(String src, String from, String to) {
        StringBuffer sb = new StringBuffer(src);
        int idx = src.indexOf(from);
        int len = from.length();
        if (idx != -1) {
            sb = sb.replace(idx, idx + len, to);
        }
        return new String(sb);
    }

    /**
	 * Replaces all specific substring in a string with 
	 * new substring. Case sensitive.
	 */
    public static String replaceAll(String src, String from, String to) {
        StringBuffer sb = new StringBuffer(src);
        int i1, i2, tail;
        int len = from.length();
        int fromIndex = 0;
        while ((i1 = src.indexOf(from, fromIndex)) != -1) {
            i2 = i1 + len;
            tail = src.length() - i2;
            sb = sb.replace(i1, i2, to);
            src = new String(sb);
            fromIndex = src.length() - tail;
        }
        return new String(sb);
    }

    /**
	 * replaceFirst () wrapper
	 */
    public static String replace(String src, String from, String to) {
        return replaceFirst(src, from, to);
    }

    /**
	 * replace old strings list to new strings list
	 */
    public static String replace(String src, String[] from, String[] to) {
        String rt = src;
        int len = Math.min(from.length, to.length);
        for (int i = 0; i < len; i++) {
            rt = replace(rt, from[i], to[i]);
        }
        return rt;
    }

    /**
	 * replace all old strings in list to new strings. 
	 */
    public static String replaceAll(String src, String[] from, String[] to) {
        String rt = src;
        int len = Math.min(from.length, to.length);
        for (int i = 0; i < len; i++) {
            rt = replaceAll(rt, from[i], to[i]);
        }
        return rt;
    }

    /**
	 * replace a string between two sub string to a new string
	 * replaceBetween ("aaa bbb ccc", "aaa", "ccc", " zzz ") --> aaa zzz ccc
	 */
    public static String replaceBetween(String src, String before, String after, String to) {
        String from = between(src, before, after);
        return replaceFirst(src, from, to);
    }

    /**
	 * trim all " ", "\t","��" in string (include head, middle, end);
	 * ע���������ȫ�ǿո�
	 */
    public static String trim(String src) {
        if (nullity(src)) return "";
        String rt = src.trim();
        rt = Stringx.replaceAll(rt, new String[] { " ", "\t", "��" }, new String[] { "", "", "" });
        return rt;
    }

    /**
	 * Returns an Array of Strings that are the substrings of the specified String.
	 */
    public static String[] split(String src, String sep) {
        if (src == null || src.equals("") || sep == null || sep.equals("")) return new String[0];
        List<String> v = new ArrayList<String>();
        int idx;
        int len = sep.length();
        while ((idx = src.indexOf(sep)) != -1) {
            v.add(src.substring(0, idx));
            idx += len;
            src = src.substring(idx);
        }
        v.add(src);
        return (String[]) v.toArray(new String[0]);
    }

    /**
	 * <pre>
	 * @author jamesqiu 2006-11-18 ���Ф�����ETL��������
	 * Returns an Array of Strings that are the substrings of the specified String.
	 * 1111,abc,"hello, abc",11.11,20050101 ->
	 * [1111, abc, "hello, abc", 11.11, 20050101]
	 * </pre>
	 * 
	 * @param quo \", \' ���ַ����
	 */
    public static String[] split(String src, String sep, String quo) {
        if (src == null || src.equals("") || sep == null || sep.equals("") || quo == null || (!quo.equals("\"") && !quo.equals("\'"))) return new String[0];
        List<String> v = new ArrayList<String>();
        int i0 = 0;
        int i1 = 1;
        int len = sep.length();
        boolean has_quo = false;
        while (true) {
            if (has_quo = src.startsWith(quo, i0)) {
                i1 = src.indexOf(quo + sep, i0 + 1);
            } else {
                i1 = src.indexOf(sep, i0);
            }
            if (i1 != -1) {
                if (has_quo) {
                    v.add(src.substring(i0, i1 + 1));
                    i0 = i1 + (1 + len);
                } else {
                    v.add(src.substring(i0, i1));
                    i0 = i1 + len;
                }
            } else {
                v.add(src.substring(i0));
                break;
            }
        }
        return (String[]) v.toArray(new String[0]);
    }

    /**
	 * @author jamesqiu 2006-12-8
	 * 1,2;3,2;6,7 --> {{1,2}, {3,2}, {6,7}}
	 * @see TextAdaptor#getRawDataFromText(String, boolean)
	 */
    public static String[][] split2table(String express, String lineSep, String colSep) {
        if (Stringx.nullity(express) || Stringx.nullity(lineSep) || Stringx.nullity(colSep)) return new String[0][0];
        String[] rows = Stringx.split(express, lineSep);
        String[][] table = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            table[i] = Stringx.split(rows[i], colSep);
        }
        return table;
    }

    /**
	 * split string contains integer sequence to int[].
	 */
    public static int[] split2ints(String src, String sep) {
        String[] strs = split(src, sep);
        int[] ints0 = new int[strs.length];
        int nn = 0;
        for (int i = 0, n = strs.length; i < n; i++) {
            if (!strs[i].equals("")) {
                try {
                    ints0[nn] = Integer.parseInt(strs[i]);
                } catch (Exception e) {
                    ints0[nn] = 0;
                }
                nn++;
            }
        }
        int[] ints1 = new int[nn];
        System.arraycopy(ints0, 0, ints1, 0, nn);
        return ints1;
    }

    /**
	 * split string contains boolean sequence to boolean[].
	 * add by puning 2003-07-07
	 */
    public static boolean[] split2bools(String src, String sep) {
        String[] strs = split(src, sep);
        boolean[] booleans0 = new boolean[strs.length];
        int nn = 0;
        for (int i = 0, n = strs.length; i < n; i++) {
            if (!strs[i].equals("")) {
                try {
                    booleans0[nn] = new Boolean(strs[i]).booleanValue();
                } catch (Exception e) {
                    booleans0[nn] = false;
                }
                nn++;
            }
        }
        boolean[] booleans1 = new boolean[nn];
        System.arraycopy(booleans0, 0, booleans1, 0, nn);
        return booleans1;
    }

    /**
	 * join a string array to a single string with given join string.
	 */
    public static String join(String[] array, String sep) {
        if (array == null || array.length == 0) {
            return "";
        }
        int len = array.length;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < (len - 1); i++) {
            sb.append(array[i]).append(sep);
        }
        sb.append(array[len - 1]);
        return sb.toString();
    }

    public static String join(String[] array) {
        return join(array, ", ");
    }

    /**
	 * join a string array to a single string with given join string.
	 * @param array strings need to join
	 * @param sep seperate string
	 * @param pre prefix before every string like "("
	 * @param post postfix after every string like ")"
	 */
    public static String join(String[] array, String sep, String pre, String post) {
        if (array == null || array.length == 0) {
            return "";
        }
        int len = array.length;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < (len - 1); i++) {
            sb.append(pre).append(array[i]).append(post).append(sep);
        }
        sb.append(pre).append(array[len - 1]).append(post);
        return sb.toString();
    }

    /**
	 * join a int array to a single string with given join string.
	 */
    public static String join(int[] array, String sep) {
        if (array == null || array.length == 0) {
            return "";
        }
        int len = array.length;
        StringBuffer sb = new StringBuffer();
        String item;
        for (int i = 0; i < (len - 1); i++) {
            item = (new Integer(array[i])).toString();
            sb.append(item).append(sep);
        }
        sb.append(array[len - 1]);
        return sb.toString();
    }

    /**
	 * join a boolean array to a single string with given join string.
	 * add by puning 2003-07-07
	 */
    public static String join(boolean[] array, String sep) {
        if (array == null || array.length == 0) {
            return "";
        }
        int len = array.length;
        StringBuffer sb = new StringBuffer();
        String item;
        for (int i = 0; i < (len - 1); i++) {
            item = (new Boolean(array[i])).toString();
            sb.append(item).append(sep);
        }
        sb.append(array[len - 1]);
        return sb.toString();
    }

    /**
	 * @author jamesqiu 2006-12-8
	 * join a string[][] with line seperator and column seperator
	 */
    public static String join(String[][] table, String lineSep, String colSep) {
        if (table == null || table.length == 0) return "";
        StringBuffer rt = new StringBuffer("");
        for (int i = 0; i < table.length; i++) {
            if (i > 0) rt.append(lineSep);
            rt.append(join(table[i], colSep));
        }
        return rt.toString();
    }

    /**
	 * Make elements in a String Array unique. 
	 */
    public static String[] unique(String[] strArray) {
        List<String> unique = new ArrayList<String>();
        for (int i = 0; i < strArray.length; i++) {
            if (unique.contains(strArray[i])) continue;
            unique.add(strArray[i]);
        }
        return (String[]) unique.toArray(new String[0]);
    }

    /**
	 * Make elements in a String Array unique (like set). 
	 * alias of unique (string[])
	 */
    public static String[] set(String[] strArray) {
        return unique(strArray);
    }

    /**
	 * @return if all string in string[] are different (like set), return true;
	 * 	else, return false 
	 */
    public static boolean isSet(String[] strArray) {
        if (strArray == null) return false;
        int n = strArray.length;
        String s1 = "";
        String s2 = "";
        for (int i = 0; i < n; i++) {
            s1 = strArray[i];
            for (int j = 0; j < n; j++) {
                if (i != j) s2 = strArray[j];
                if (s1.equals(s2)) return false;
            }
        }
        return true;
    }

    /**
	 * sort a String Array in natural order.
	 */
    public static void sort(String[] strArray) {
        Arrays.sort(strArray);
    }

    /**
	 * group string array. 
	 * @param src_list String array grouped with asc/desc order. 
	 * @return String[2][n], String[0][n] is String, String[1][n] is length.
	 * String[0][n]:	[a][b][c][d]
	 * String[1][n]:	[3][2][6][0]
	 */
    public static String[][] group(String[] src_list) {
        if (src_list == null || src_list.length == 0) {
            return new String[][] { new String[0], new String[0] };
        }
        List<String> value = new ArrayList<String>();
        List<String> num = new ArrayList<String>();
        String cur_str = src_list[0];
        value.add(cur_str);
        int n = 1;
        String tmp = null;
        for (int i = 1; i < src_list.length; i++) {
            tmp = src_list[i];
            if (tmp.equals(cur_str)) {
                n++;
            } else {
                num.add("" + n);
                value.add(tmp);
                n = 1;
                cur_str = tmp;
            }
        }
        num.add("" + n);
        String[] rt_v = (String[]) value.toArray(new String[0]);
        String[] rt_n = (String[]) num.toArray(new String[0]);
        return new String[][] { rt_v, rt_n };
    }

    /**
	 * alias of in (string, string[])
	 * @return If str is an element of strArray, return true, else return false.
	 */
    public static boolean include(String str, String[] strArray) {
        return in(str, strArray);
    }

    /**
	 * @return If str is an element of strArray, return true, else return false.
	 */
    public static boolean in(String str, String[] strArray) {
        if (str == null || strArray == null) return false;
        for (int i = 0, n = strArray.length; i < n; i++) {
            if (str.equals(strArray[i])) return true;
        }
        return false;
    }

    /**
	 * @return if sub string array is subset of all string array
	 */
    public static boolean isSubset(String[] sub, String[] all) {
        if (sub == null || all == null) return false;
        for (int i = 0; i < sub.length; i++) {
            if (!in(sub[i], all)) return false;
        }
        return true;
    }

    /**
	 * remove these elements of source array which in remove array.
	 * @return source[] - remove[]
	 * @author jamesqiu 2006-12-20
	 */
    public static String[] remove(String[] source, String[] remove) {
        if (source == null || remove == null) return new String[0];
        List<String> rt = new ArrayList<String>();
        for (int i = 0; i < source.length; i++) {
            if (!in(source[i], remove)) {
                rt.add(source[i]);
            }
        }
        return (String[]) rt.toArray(new String[rt.size()]);
    }

    /**
	 * @return If str is an element of strArray, return Index, else return -1.
	 */
    public static int index(String str, String[] strArray) {
        if (str == null || strArray == null) return -1;
        for (int i = 0; i < strArray.length; i++) {
            if (str.equals(strArray[i])) return i;
        }
        return -1;
    }

    /**
	 * plus version of indexOf (String, String[]), 
	 * @param ignoreCase Ignore case if true;  
	 */
    public static int index(String str, String[] strArray, boolean ignoreCase) {
        if (str == null || strArray == null) return -1;
        boolean equals;
        for (int i = 0; i < strArray.length; i++) {
            equals = ignoreCase ? str.equalsIgnoreCase(strArray[i]) : str.equals(strArray[i]);
            if (equals) return i;
        }
        return -1;
    }

    /**
	 * insert new string before given substring
	 * @param str original string to insert
	 * @param substr given substring of str before which to insert
	 * @param newstr new string to insert befroe substring
	 */
    public static String insert(String str, String substr, String newstr) {
        if (str == null || substr == null || newstr == null) return "";
        String rt = left(str, substr) + newstr + substr + right(str, substr);
        return rt;
    }

    /**
	 * insert new string before given substring, find substring from back.
	 * @param str original string to insert
	 * @param substr given substring of str before which to insert
	 * @param newstr new string to insert befroe substring
	 */
    public static String insertback(String str, String substr, String newstr) {
        if (str == null || substr == null || newstr == null) return "";
        String rt = leftback(str, substr) + newstr + substr + rightback(str, substr);
        return rt;
    }

    /**
	 * ��ָ���ַ�s�����ָ������len��
	 * 1) ��������?����ԭ�ַ�
	 * 2) ���ָ������len�����ַ�s�����������s��ǰ�����ַ���䣻
	 * @param s		ָ���ַ��������
	 * @param len	ָ������䳤��
	 * @return ������ַ�
	 */
    public static String fill(String s, int len) {
        if (s == null || s.equals("") || (len < 1)) return s;
        int len1 = s.length();
        if (len1 >= len) return s;
        int n = len / len1;
        int m = len % len1;
        StringBuffer sb = new StringBuffer(len);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        if (m != 0) sb.append(s.substring(0, m));
        return sb.toString();
    }

    /**
	 * ����䣨�Ҷ��룩
	 * @param str	������ַ�
	 * @param len	��䳤��
	 * @param s		�������
	 * @return	������Ҷ����ַ�
	 */
    public static String padLeft(String str, int len, String s) {
        if (str == null || str.equals("") || len < str.length() || s == null || s.equals("")) return str;
        String rt = fill(s, len);
        int n = len - str.length();
        return rt.substring(0, n) + str;
    }

    /**
	 * ����䣨����룩
	 * @param str	������ַ�
	 * @param len	��䳤��
	 * @param s		�������
	 * @return	������Ҷ����ַ�
	 */
    public static String padRight(String str, int len, String s) {
        if (str == null || str.equals("") || len < str.length() || s == null || s.equals("")) return str;
        String rt = fill(s, len);
        int n = len - str.length();
        return str + rt.substring(0, n);
    }

    /**
	 * ���ַ������������䣨�Ҷ��룩����䳤��Ϊ�ַ�����������ַ�
	 * @param ss	���������
	 * @param s		�������
	 * @return
	 */
    public static String[] padLeft(String[] ss, String s) {
        if (ss == null || s == null || s.equals("")) return ss;
        int max = -1;
        int n = ss.length;
        for (int i = 0; i < n; i++) {
            if (ss[i] == null) continue;
            int m = ss[i].length();
            if (m > max) max = m;
        }
        String[] rt = new String[n];
        for (int i = 0; i < n; i++) {
            rt[i] = padLeft(ss[i], max, s);
        }
        return rt;
    }

    /**
	 * ���ַ������������䣨����룩����䳤��Ϊ�ַ�����������ַ�
	 * @param ss	���������
	 * @param s		�������
	 * @return
	 */
    public static String[] padRight(String[] ss, String s) {
        if (ss == null || s == null || s.equals("")) return ss;
        int max = -1;
        int n = ss.length;
        for (int i = 0; i < n; i++) {
            if (ss[i] == null) continue;
            int m = ss[i].length();
            if (m > max) max = m;
        }
        String[] rt = new String[n];
        for (int i = 0; i < n; i++) {
            rt[i] = padRight(ss[i], max, s);
        }
        return rt;
    }

    /**
	 * @param str to be encoded
	 * @return base 64 string
	 */
    public static String base64enc(String str) {
        if (str == null) {
            System.err.println("Stringx.base64encode (String) : null string.");
            return "";
        }
        String rt = null;
        try {
            rt = new sun.misc.BASE64Encoder().encode(str.getBytes("utf-8"));
        } catch (Exception e) {
            System.err.println("Stringx.base64encode (String) : encode error - " + e);
            return "";
        }
        return rt;
    }

    /**
	 * @param str base 64 string 
	 * @return decoded string
	 */
    public static String base64dec(String str) {
        if (str == null) {
            System.err.println("Stringx.base64encode (String) : null string.");
            return "";
        }
        String rt = null;
        try {
            byte[] bt = new sun.misc.BASE64Decoder().decodeBuffer(str);
            rt = new String(bt, "utf-8");
        } catch (Exception e) {
            System.err.println("Stringx.base64encode (String) : encode error - " + e);
            return "";
        }
        return rt;
    }

    /**
	 * @param str string to be encode
	 * @param md5 string
		md5("") = 							d41d8cd98f00b204e9800998ecf8427e
		md5("a") = 							0cc175b9c0f1b6a831c399e269772661
		md5("abc") = 						900150983cd24fb0d6963f7d28e17f72
		md5("message digest") = 			f96b697d7cb7938d525a2f31aaf161d0
		md5("abcdefghijklmnopqrstuvwxyz")=	c3fcd3d76192e4007dfb496cca67e13b
	 */
    public static String md5(String str) {
        if (str == null) {
            System.err.println("Stringx.md5 (String) : null string.");
            return "";
        }
        String rt = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes("gb2312"));
            byte[] bt = md5.digest();
            String s = null;
            int l = 0;
            for (int i = 0; i < bt.length; i++) {
                s = Integer.toHexString(bt[i]);
                l = s.length();
                if (l > 2) s = s.substring(l - 2, l); else if (l == 1) s = "0" + s;
                rt += s;
            }
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rt;
    }

    /**
	 * Check jsp parameter, if parameter is null or is "", set to default value.
	 */
    public static String check(String param, String src_default) {
        if (param == null || param.trim().equals("")) {
            return src_default;
        } else {
            return param;
        }
    }

    /**
	 * Check jsp parameter, if parameter is null or "" or "  ", return true.
	 */
    public static boolean nullity(String param) {
        return (param == null || param.trim().equals(""));
    }

    /**
	 * Check is given string is "true" 
	 */
    public static boolean isTrue(String param) {
        return "true".equalsIgnoreCase(param);
    }

    /**
	 * translate map's key as string array, map's value as string array.
	 * @return String[2][] rt, rt[0] = key string array, rt[1] = value string array.  
	 */
    public static String[][] map2array(Map<String, String> map) {
        if (map == null) return new String[2][0];
        int n = map.size();
        String[][] rt = new String[2][n];
        rt[0] = (String[]) map.keySet().toArray(new String[n]);
        rt[1] = (String[]) map.values().toArray(new String[n]);
        return rt;
    }

    /**
	 * translate List's elements as string array.
	 * condition: all elements of list is string (or null). 
	 * @return String[] rt.  
	 */
    public static String[] list2array(List<String> list) {
        return collect2array(list);
    }

    /**
	 * translate Collection's elements as string array.
	 * condition: all elements of collection is string (or null). 
	 * @return String[] rt.  
	 */
    public static String[] collect2array(Collection<String> c) {
        if (c == null) return new String[0];
        int n = c.size();
        String[] rt = new String[n];
        rt = (String[]) c.toArray(new String[n]);
        return rt;
    }

    /**
	 * @param formatPattern ��ʽ���ַ�����<pre>��
	 * {0}''s and {1}''s
	 * {0,number}
	 * {0,number,#.#}
	 * {0,number,currency}
	 * {0,number,percent}
	 * 
	 * {0,time}   {0,time,HH-mm-ss}
	 * {0,time,short}   {0,time,medium}   {0,time,long}   {0,time,full}
	 *  
	 * {0,date}   {0,date,yyyy-MM-dd}
	 * {0,date,short}    {0,date,medium}   {0,date,long}   {0,date,full}
	 * </pre>
	 * 
	 * @param param ������String, Number, Date (yyyy/MM/dd HH:mm:ss a)
	 */
    public static String format(String formatPattern, Object[] param) {
        String rt = MessageFormat.format(formatPattern, param);
        return rt;
    }

    /**
	 * ��ʽ�����������C��printf����Object[]ģ��ɱ����.\n
	 * ���磺"[{0}]: {1} {2,number,#.##} {3,date,yyyy/MM/dd} {3,time,aHH:mm:ss}\n"
	 * @see format ()
	 */
    public static void printf(String formatPattern, Object[] param) {
        System.out.print(format(formatPattern, param));
    }

    public static void main(String[] args) {
        String[] sa = new String[] { "", "a", "abc", "message digest", "abcdefghijklmnopqrstuvwxyz", "1" };
        String s = null;
        for (int i = 0; i < sa.length; i++) {
            s = md5(sa[i]);
            System.out.println(sa[i] + "==" + s);
        }
        System.out.println("test ---------- list2array ----------");
        List<String> l = null;
        String[] rt = list2array(l);
        System.out.println(rt.length);
        l = new ArrayList<String>();
        rt = list2array(l);
        System.out.println(rt.length);
        l.add("hello");
        l.add("����");
        l.add(null);
        rt = list2array(l);
        System.out.println(rt[1]);
        System.out.println(join(rt, ", ", "\"", "\""));
        System.out.println("test ----- format -----");
        printf("[{0}]: {1} {2,number,#.##} {3,date,yyyy/MM/dd} {3,time,aHH:mm:ss}\n", new Object[] { "James", new Integer(100), new Double(2003.14159265), new Date() });
    }
}
