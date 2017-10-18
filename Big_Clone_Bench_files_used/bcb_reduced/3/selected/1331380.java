package gndzh.som.summer.encode;

import java.security.MessageDigest;

/**
 * Ӧ��md5�ļ���������㷨
 * @author gan
 *
 */
public class MD5andKL {

    public static String MD5(String inStr) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String KL(String inStr) {
        char[] a = inStr.toCharArray();
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (a[i] ^ 't');
        }
        String s = new String(a);
        return s;
    }

    public static String JM(String inStr) {
        char[] a = inStr.toCharArray();
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (a[i] ^ 't');
        }
        String k = new String(a);
        return k;
    }

    public static void main(String args[]) {
        String s = new String("tiger");
        System.out.println("ԭʼ��" + s);
        System.out.println("MD5��" + MD5(s));
        System.out.println("MD5��" + MD5(s));
        System.out.println("MD5���ټ��ܣ�" + KL(MD5(s)));
        System.out.println("����ΪMD5��ģ�" + JM(KL(MD5(s))));
    }
}
