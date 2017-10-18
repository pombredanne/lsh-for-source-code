package net.sourceforge.ikms.util.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加密解密相关公共方法
 * 
 * @author <b>oxidy</b>, Copyright &#169; 2003
 * @since 05 August 2011
 * 
 */
public class CryptUtils {

    private static Logger logger = LoggerFactory.getLogger(CryptUtils.class);

    /**
	 * 获取MD5加密后的字符串
	 * @param password
	 *            需要加密的字符串
	 * @return 加密后的字符串
	 */
    public static String getMD5(String password) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            String pwd = new BigInteger(1, md5.digest()).toString(16);
            return pwd;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return password;
    }
}
