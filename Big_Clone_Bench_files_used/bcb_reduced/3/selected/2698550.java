package com.bird.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * ������ƹ�����
 * @author ��־ǿ
 * 2009-11-30
 */
public class TokenUtil {

    /**
	 * struts   ���ƻ���
	 * @param   request
	 * @return  �������
	 */
    public static String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        try {
            byte id[] = session.getId().getBytes();
            byte now[] = new Long(System.currentTimeMillis()).toString().getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(id);
            md.update(now);
            return (toHex(md.digest()));
        } catch (IllegalStateException e) {
            return (null);
        } catch (NoSuchAlgorithmException e) {
            return (null);
        }
    }

    /**
	 * @param  buffer
	 * @return 16���Ƶ��ַ��ʾ
	 */
    protected static String toHex(byte buffer[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buffer.length; i++) sb.append(Integer.toHexString((int) buffer[i] & 0xff));
        return (sb.toString());
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }
}
