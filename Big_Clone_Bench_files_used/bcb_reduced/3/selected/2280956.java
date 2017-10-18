package rath.msnm.util;

import org.apache.log4j.Logger;
import java.security.MessageDigest;

/**
 * String 조작??�?�� Utility???�적메소?�들??�??�??�는 ?�래?�이??
 * 
 * @author Jang-Ho Hwang, rath@linuxkorea.co.kr
 * @version $Id: StringUtil.java,v 1.1 2006/08/01 05:27:18 kenwudi Exp $
 */
public class StringUtil {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(StringUtil.class);

    /**
     * 주어�?origin 문자?�에??src 문자?�을 모두 찾아 dest 문자?�로 �?��?��???
     */
    public static String replaceString(String origin, String src, String dest) {
        if (logger.isDebugEnabled()) {
            logger.debug("replaceString(String, String, String) - start");
        }
        if (origin == null) return null;
        StringBuffer sb = new StringBuffer(origin.length());
        int srcLength = src.length();
        int destLength = dest.length();
        int preOffset = 0;
        int offset = 0;
        while ((offset = origin.indexOf(src, preOffset)) != -1) {
            sb.append(origin.substring(preOffset, offset));
            sb.append(dest);
            preOffset = offset + srcLength;
        }
        sb.append(origin.substring(preOffset, origin.length()));
        String returnString = sb.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("replaceString(String, String, String) - end");
        }
        return returnString;
    }

    /**
     * 주어�?문자�?MD5�?digest????HEXA?�태�?�?��?��???
     */
    public static String md5(String str) {
        if (logger.isDebugEnabled()) {
            logger.debug("md5(String) - start");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] b = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                int v = (int) b[i];
                v = v < 0 ? 0x100 + v : v;
                String cc = Integer.toHexString(v);
                if (cc.length() == 1) sb.append('0');
                sb.append(cc);
            }
            String returnString = sb.toString();
            if (logger.isDebugEnabled()) {
                logger.debug("md5(String) - end");
            }
            return returnString;
        } catch (Exception e) {
            logger.warn("md5(String) - exception ignored", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("md5(String) - end");
        }
        return "";
    }
}
