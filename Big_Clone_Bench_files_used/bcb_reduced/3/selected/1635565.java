package dtec.string.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public class CommonString {

    /**
	 * ����ֵ��ʽ: yyyy-MM-dd hh:mm:ss
	 */
    public static String getCurrentTime() {
        Date date = new Date();
        return new String(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
    }

    /**
	 * 
	 * @param format
	 *            yyyy-MM-dd HH:mm:ss
	 * @return
	 */
    public static String getCurrentTime(String format) {
        Date date = new Date();
        return new String(new SimpleDateFormat(format).format(date));
    }

    /**
	 * 
	 * @param format
	 *            yyyy-MM-dd
	 * @return
	 */
    public static String getCurrentDate(String format) {
        Date date = new Date();
        return new String(new SimpleDateFormat(format).format(date));
    }

    /**
	 * ����ֵ��ʽ: yyyy-MM-dd
	 */
    public static String getCurrentDate() {
        Date date = new Date();
        return new String(new SimpleDateFormat("yyyy-MM-dd").format(date));
    }

    public static void main(String[] args) {
        System.out.println(CommonString.split(" ", "sd").size());
    }

    public static String MD5(String src) {
        byte[] source = src.getBytes();
        try {
            MessageDigest alg = java.security.MessageDigest.getInstance("MD5 ");
            source = alg.digest(source);
        } catch (NoSuchAlgorithmException ex) {
        }
        return source.toString();
    }

    public static String getTime(String year, String month, String day, String hour, String minute) {
        String result = "";
        result += year + "-" + month + "-" + day + " " + (hour.length() == 1 ? ("0" + hour) : hour) + ":" + (minute.length() == 1 ? ("0" + minute) : minute) + ":01";
        return result;
    }

    public static String toUTF_8(final String str) {
        String retVal = str;
        try {
            retVal = new String(str.getBytes("ISO8859_1"), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public static int[] split(String str, char sign) {
        int[] result = new int[3];
        int sum = 0;
        String buffer = "";
        for (int i = 0; i < str.length(); i++) {
            char temp = str.charAt(i);
            buffer += (temp == '|') ? "" : temp;
            if (temp == sign) {
                result[sum++] = Integer.parseInt(buffer);
                buffer = "";
            }
        }
        result[sum] = Integer.parseInt(buffer);
        return result;
    }

    public static ArrayList<String> split(String str, String sign) {
        ArrayList<String> result = new ArrayList<String>();
        if (!"".equals(str.trim())) {
            while (str.indexOf(sign) >= 0) {
                if (!str.substring(0, str.indexOf(sign)).equals("")) {
                    result.add(str.substring(0, str.indexOf(sign)));
                }
                str = str.substring(str.indexOf(sign) + 1, str.length());
            }
            result.add(str);
        }
        return result;
    }

    /**
	 * �ݹ�URLDecoder.decode(str,"UTF-8")ֱ��������%<br/>
	 * ���⣺���Ļ����г��֡�%����������ȷ����������Ҫ�ڿͻ��˴���%�ַ�
	 * 
	 * @param str
	 * @return decode������ֵ
	 */
    public static String decode(String str) {
        try {
            if (str.contains("%")) {
                str = URLDecoder.decode(str, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String getRequestString(HttpServletRequest request) throws IOException {
        StringBuffer buffer = new StringBuffer();
        String jsonString = "";
        while ((jsonString = request.getReader().readLine()) != null) {
            buffer.append(jsonString);
        }
        return buffer.toString();
    }
}
