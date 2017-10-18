package cn.lzh.common.string;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * �ַ���
 * @author <a href="mailto:sealinglip@gmail.com">Sealinglip</a>
 *
 */
public class StringUtil {

    /**
	 * ֮�����ṩ�����������Ϊ�˺�String.split��ֿ���
	 * ����û�в���������ʽ������ֱ�ӻ����趨�ַ�ķָ�
	 * @param sourceStr
	 * @param delim
	 * @return String[]
	 */
    public static String[] split(String sourceStr, String delim) {
        StringTokenizer st = new StringTokenizer(sourceStr, delim);
        List<String> subStrList = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String element = st.nextToken();
            subStrList.add(element);
        }
        return subStrList.toArray(new String[subStrList.size()]);
    }

    /** 
	 * ת���ֽ�����Ϊ16�����ִ� 
	 * @param b �ֽ����� 
	 * @return 16�����ִ� 
	 */
    public static String byteArrayToHexString(byte[] b) {
        return HEXEncoder.encode(b);
    }

    /**
	 * ת��16�����ַ�Ϊ�ֽ�����
	 * @param hexString
	 * @return
	 */
    public static byte[] hexStringTobyteArray(String hexString) {
        return HEXDecoder.decodeBuffer(hexString);
    }

    public static String join(String seperator, String[] strings) {
        int length = strings.length;
        if (length == 0) return "";
        StringBuffer buf = new StringBuffer(length * strings[0].length()).append(strings[0]);
        for (int i = 1; i < length; i++) {
            buf.append(seperator).append(strings[i]);
        }
        return buf.toString();
    }

    public static String replaceOnce(String template, String placeholder, String replacement) {
        int loc = template.indexOf(placeholder);
        if (loc < 0) {
            return template;
        } else {
            return new StringBuffer(template.substring(0, loc)).append(replacement).append(template.substring(loc + placeholder.length())).toString();
        }
    }

    public static String MD5Encode(String origin) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
        } catch (Exception ex) {
        }
        return resultString;
    }

    /**
	 * ���תȫ�ǣ�ȫ�ǿո�Ϊ12288����ǿո�Ϊ32,�����ַ���(33-126)��ȫ��(65281-65374)�Ķ�Ӧ��ϵ�ǣ������65248
	 * @author hqx
	 * @param arg �����ַ�
	 * @return ȫ���ַ�
	 */
    public static final String toSBC(String arg) {
        if (arg == null) return null;
        char[] c = arg.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 32) {
                c[i] = (char) 12288;
                continue;
            }
            if (c[i] < 127) c[i] = (char) (c[i] + 65248);
        }
        return new String(c);
    }

    /**
	 * ȫ��ת��ǣ�ȫ�ǿո�Ϊ12288����ǿո�Ϊ32,�����ַ���(33-126)��ȫ��(65281-65374)�Ķ�Ӧ��ϵ�ǣ������65248
	 * @author hqx
	 * @param arg �����ַ�
	 * @return ����ַ�
	 */
    public static final String toDBC(String arg) {
        if (arg == null) return null;
        char[] c = arg.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375) c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    /**
	 * ȥ���ַ��еĿո񡢻س������з��Ʊ��(��ͷ�ͽ�β)
	 * @param src	�����ַ�
	 * @return	û�пհ׵��ַ�
	 */
    public static final String trimBlank(String src) {
        Pattern p = Pattern.compile("\\s|\t|\r|\n|\f|| | |　");
        Matcher m = p.matcher(src);
        String after = m.replaceAll("");
        return after;
    }
}
