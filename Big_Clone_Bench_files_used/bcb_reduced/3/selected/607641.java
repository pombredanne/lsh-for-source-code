package omg.ligong.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * �ַ�����
 * 
 * @author Administrator
 * 
 */
public class StrFunc {

    /**
	 * MD5�����㷨����������
	 * 
	 * @param str
	 * @return
	 */
    public static String toMd5(String str) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
            System.exit(-1);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] byteArray = messageDigest.digest();
        StringBuffer md5StrBuff = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i])); else md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return md5StrBuff.toString();
    }

    /**
	 * ����ǰ����תΪ2010-1-9����ʽ
	 * 
	 * @return
	 */
    public static String getCurrentDateStr() {
        Calendar cal = Calendar.getInstance();
        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR)).append("-").append(cal.get(Calendar.MONTH)).append("-").append(cal.get(Calendar.DATE));
        return sb.toString();
    }

    public static String getDateStr(Calendar cal) {
        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR)).append("-").append(cal.get(Calendar.MONTH)).append("-").append(cal.get(Calendar.DATE));
        return sb.toString();
    }

    public static boolean isNull(String s) {
        return s == null || s.length() == 0;
    }

    public static void main(String args[]) {
        System.out.println(toMd5("admin"));
    }
}
