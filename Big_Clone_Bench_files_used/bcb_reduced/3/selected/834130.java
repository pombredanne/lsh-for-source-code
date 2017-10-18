package com.sitechasia.webx.core.utils.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * SHA算法工具类
 * <pre>
 *    //待计算散列值的数据
 *    String sourceString = ...;
 *    byte[] sourceBytes = ...;
 *
 *    //计算SHA散列值，返回二进制数组
 *    byte[] resultArray1 = SHAUtils.getSHADigest(sourceString);
 *    或者
 *    byte[] resultArray2 = SHAUtils.getSHADigest(sourceBytes);
 *
 *    //计算SHA散列值，返回Hex字符串
 *    String hexString1 = SHAUtils.getSHADigestHex(sourceString);
 *    或者
 *    String hexString2 = SHAUtils.getSHADigestHex(sourceBytes);
 *
 *    //计算SHA散列值，返回Base64编码格式字符串
 *    String base64String1 = SHAUtils.getSHADigestBase64(sourceString);
 *    或者
 *    String base64String2 = SHAUtils.getSHADigestBase64(sourceBytes);
 * </pre>
 *
 * @author zane
 * @author todd
 * @version 1.2 , 2008/5/7
 * @since JDK1.5
 */
public final class SHAUtils {

    private SHAUtils() {
    }

    private static final String SHA_ALGORITHM = "SHA-1";

    /**
	 * 返回SHA算法实现
	 *
	 * @return 返回SHA算法实现
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    private static MessageDigest getSHADigestAlgorithm() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(SHA_ALGORITHM);
    }

    /**
	 * 计算SHA散列值，返回<code>byte[]</code>
	 *
	 * @param source
	 * @return 返回<code>byte[]</code>
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static byte[] getSHADigest(byte[] source) throws NoSuchAlgorithmException {
        return getSHADigestAlgorithm().digest(source);
    }

    /**
	 * 计算SHA散列值，返回<code>byte[]</code>
	 *
	 * @param source
	 * @return 返回<code>byte[]</code>
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static byte[] getSHADigest(String source) throws NoSuchAlgorithmException {
        return getSHADigest(source.getBytes());
    }

    /**
	 * 计算SHA散列值，返回Hex字符串
	 *
	 * @param source
	 * @return 返回Hex字符串
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static String getSHADigestHex(byte[] source) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getSHADigest(source)));
    }

    /**
	 * 计算SHA散列值，返回Hex字符串
	 *
	 * @param source
	 * @return 返回Hex字符串
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static String getSHADigestHex(String source) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getSHADigest(source.getBytes())));
    }

    /**
	 * 计算SHA散列值，返回Base64编码格式字符串
	 *
	 * @param source
	 * @return 返回Base64编码格式字符串
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static String getSHADigestBase64(byte[] source) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getSHADigest(source)));
    }

    /**
	 * 计算SHA散列值，返回Base64编码格式字符串
	 *
	 * @param source
	 * @return 返回Base64编码格式字符串
	 * @throws NoSuchAlgorithmException
	 * @since 1.0
	 */
    public static String getSHADigestBase64(String source) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getSHADigest(source.getBytes())));
    }
}
