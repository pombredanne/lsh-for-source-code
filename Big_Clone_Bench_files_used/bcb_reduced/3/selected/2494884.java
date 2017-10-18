package cn.ac.ntarl.umt.utils;

import java.security.MessageDigest;
import org.apache.log4j.Logger;

/**
 * ��������м��ܺ���֤�ĳ���
 * 
 *
 */
public class Password {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /** ��inputString����     */
    public static String createPassword(String inputString) {
        return encodeByMD5(inputString);
    }

    /**
     * ��֤����������Ƿ���ȷ
     * @param password    ��������루���ܺ�������룩
     * @param inputString    ������ַ�
     * @return    ��֤���boolean����
     */
    public static boolean authenticatePassword(String password, String inputString) {
        if (password.equals(encodeByMD5(inputString))) {
            return true;
        } else {
            return false;
        }
    }

    /** ���ַ����MD5����     */
    private static String encodeByMD5(String originString) {
        if (originString != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] results = md.digest(originString.getBytes());
                String resultString = byteArrayToHexString(results);
                return resultString.toUpperCase();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * ת���ֽ�����Ϊʮ������ַ�
     * @param b    �ֽ�����
     * @return    ʮ������ַ�
     */
    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    /** ��һ���ֽ�ת����ʮ�������ʽ���ַ�     */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static void main(String[] args) {
        String password = Password.createPassword("yujj123");
        System.out.println("��yujj123��MD5ժҪ����ַ�" + password);
        String inputString = "yujj123";
        System.out.println("yujj123������ƥ�䣿" + Password.authenticatePassword(password, inputString));
        inputString = "yujj";
        System.out.println("yujj������ƥ�䣿" + Password.authenticatePassword(password, inputString));
        log.warn("thi is a test");
    }

    private static Logger log;

    static {
        log = Logger.getLogger(Password.class);
    }
}
