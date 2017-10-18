package org.wdcode.common.codec;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.wdcode.common.constants.EncryptConstants;
import org.wdcode.common.exception.CustomRuntimeException;
import org.wdcode.common.nio.StreamChannel;
import org.wdcode.common.params.WdCommonParams;
import org.wdcode.common.util.StringUtil;

/**
 * 信息摘要类
 * @author WD
 * @since JDK6
 * @version 1.0 2010-06-22
 */
public final class Digest {

    /**
	 * 获得字符串摘要
	 * @param text 要获得摘要的字符串
	 * @return 获得摘要后的字节数组
	 */
    public static final byte[] digest(String text) {
        return digest(StringUtil.toBytes(text));
    }

    /**
	 * 获得字符串摘要
	 * @param in 要获得摘要的输入流
	 * @return 获得摘要后的字节数组
	 */
    public static final byte[] digest(InputStream in) {
        return digest(StreamChannel.read(in));
    }

    /**
	 * 获得字符串摘要
	 * @param b 要获得摘要的字节数组
	 * @return 获得摘要后的字节数组
	 */
    public static final byte[] digest(byte[] b) {
        return getMessageDigest(b, WdCommonParams.getEncryptDigest());
    }

    /**
	 * 获得字符串摘要
	 * @param text 要获得摘要的字符串
	 * @return 获得摘要后的hex字符串
	 */
    public static final String digestHex(String text) {
        return digestHex(StringUtil.toBytes(text));
    }

    /**
	 * 获得字符串摘要
	 * @param in 要获得摘要的输入流
	 * @return 获得摘要后的hex字符串
	 */
    public static final String digestHex(InputStream in) {
        return digestHex(StreamChannel.read(in));
    }

    /**
	 * 获得字符串摘要
	 * @param b 要获得摘要的字节数组
	 * @return 获得摘要后的hex字符串
	 */
    public static final String digestHex(byte[] b) {
        return Encode.hex(digest(b));
    }

    /**
	 * 获得字符串摘要
	 * @param text 要获得摘要的字符串
	 * @return 获得摘要后的base64字符串
	 */
    public static final String digestBase64(String text) {
        return digestBase64(StringUtil.toBytes(text));
    }

    /**
	 * 获得字符串摘要
	 * @param in 要获得摘要的输入流
	 * @return 获得摘要后的base64字符串
	 */
    public static final String digestBase64(InputStream in) {
        return digestBase64(StreamChannel.read(in));
    }

    /**
	 * 获得字符串摘要
	 * @param b 要获得摘要的字节数组
	 * @return 获得摘要后的base64字符串
	 */
    public static final String digestBase64(byte[] b) {
        return Encode.base64(digest(b));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param text 要MD2的字符串
	 * @return MD2后的字节数组
	 */
    public static final byte[] md2(String text) {
        return md2(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param in 要MD2的输入流
	 * @return MD2后的字节数组
	 */
    public static final byte[] md2(InputStream in) {
        return md2(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param b 要MD2的z字节数组
	 * @return MD2后的字节数组
	 */
    public static final byte[] md2(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_MD2);
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param text 要MD2的字符串
	 * @return MD2后的字节数组hex后的字符串
	 */
    public static final String md2Hex(String text) {
        return md2Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param in 要MD2的输入流
	 * @return MD2后的字节数组hex后的字符串
	 */
    public static final String md2Hex(InputStream in) {
        return md2Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param in 要MD2的输入流
	 * @return MD2后的字节数组hex后的字符串
	 */
    public static final String md2Hex(byte[] b) {
        return Encode.hex(md2(b));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param text 要MD2的字符串
	 * @return MD2后的字节数组base64后的字符串
	 */
    public static final String md2Base64(String text) {
        return md2Base64(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param in 要MD2的输入流
	 * @return MD2后的字节数组base64后的字符串
	 */
    public static final String md2Base64(InputStream in) {
        return md2Base64(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD2(信息-摘要算法)码
	 * @param in 要MD2的输入流
	 * @return MD2后的字节数组base64后的字符串
	 */
    public static final String md2Base64(byte[] b) {
        return Encode.base64(md2(b));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param text 要MD5的字符串
	 * @return MD5后的字节数组
	 */
    public static final byte[] md5(String text) {
        return md5(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param in 要MD5的输入流
	 * @return MD5后的字节数组
	 */
    public static final byte[] md5(InputStream in) {
        return md5(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param b 要MD5的字节数组
	 * @return MD5后的字节数组
	 */
    public static final byte[] md5(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_MD5);
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param text 要MD5的字符串
	 * @return MD5后的字节数组的hex字符串
	 */
    public static final String md5Hex(String text) {
        return md5Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param in 要MD5的输入流
	 * @return MD5后的字节数组的hex字符串
	 */
    public static final String md5Hex(InputStream in) {
        return md5Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param b 要MD5的字节数组
	 * @return MD5后的字节数组的hex字符串
	 */
    public static final String md5Hex(byte[] b) {
        return Encode.hex(md5(b));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param text 要MD5的字符串
	 * @return MD5后的字节数组的base64字符串
	 */
    public static final String md5Base64(String text) {
        return md5Base64(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param in 要MD5的输入流
	 * @return MD5后的字节数组的base64字符串
	 */
    public static final String md5Base64(InputStream in) {
        return md5Base64(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param b 要MD5的字节数组
	 * @return MD5后的字节数组的base64字符串
	 */
    public static final String md5Base64(byte[] b) {
        return Encode.base64(md5(b));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param text 要SHA-256的字符串
	 * @return SHA-256后的字节数组
	 */
    public static final byte[] sha256(String text) {
        return sha256(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param in 要SHA-256的输入流
	 * @return SHA-256后的字节数组
	 */
    public static final byte[] sha256(InputStream in) {
        return sha256(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param b 要SHA-256的字节数组
	 * @return SHA-256后的字节数组
	 */
    public static final byte[] sha256(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_SHA_256);
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param text 要SHA-256的字符串
	 * @return SHA-256后的字节数组的hex后字符串
	 */
    public static final String sha256Hex(String text) {
        return sha256Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param in 要SHA-256的输入流
	 * @return SHA-256后的字节数组的hex后字符串
	 */
    public static final String sha256Hex(InputStream in) {
        return sha256Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param b 要SHA-256的字节数组
	 * @return SHA-256后的字节数组的hex后字符串
	 */
    public static final String sha256Hex(byte[] b) {
        return Encode.hex(sha256(b));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param text 要SHA-256的字符串
	 * @return SHA-256后的字节数组的base64后字符串
	 */
    public static final String sha256Base64(String text) {
        return sha256Base64(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param text 要SHA-256的字符串
	 * @return SHA-256后的字节数组的base64后字符串
	 */
    public static final String sha256Base64(InputStream in) {
        return sha256Base64(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-256(信息-摘要算法)码
	 * @param b 要SHA-256的字节数组
	 * @return SHA-256后的字节数组的base64后字符串
	 */
    public static final String sha256Base64(byte[] b) {
        return Encode.base64(sha256(b));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param text 要SHA-384的字符串
	 * @return SHA-384后的字节数组
	 */
    public static final byte[] sha384(String text) {
        return sha384(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param in 要SHA-384的输入流
	 * @return SHA-384后的字节数组
	 */
    public static final byte[] sha384(InputStream in) {
        return sha384(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param b 要SHA-384的字节数组
	 * @return SHA-384后的字节数组
	 */
    public static final byte[] sha384(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_SHA_384);
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param text 要SHA-384的字符串
	 * @return SHA-384后的字节数组的hex后字符串
	 */
    public static final String sha384Hex(String text) {
        return sha384Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param in 要SHA-384的输入流
	 * @return SHA-384后的字节数组的hex后字符串
	 */
    public static final String sha384Hex(InputStream in) {
        return sha384Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param b 要SHA-384的字节数组
	 * @return SHA-384后的字节数组的hex后字符串
	 */
    public static final String sha384Hex(byte[] b) {
        return Encode.hex(sha384(b));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param text 要SHA-384的字符串
	 * @return SHA-384后的字节数组的base64后字符串
	 */
    public static final String sha384Base64(String text) {
        return sha384Base64(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param in 要SHA-384的输入流
	 * @return SHA-384后的字节数组的base64后字符串
	 */
    public static final String sha384Base64(InputStream in) {
        return sha384Base64(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-384(信息-摘要算法)码
	 * @param b 要SHA-384的字节数组
	 * @return SHA-384后的字节数组的base64后字符串
	 */
    public static final String sha384Base64(byte[] b) {
        return Encode.base64(sha384(b));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param text 要SHA-512的字符串
	 * @return SHA-512后的字节数组
	 */
    public static final byte[] sha512(String text) {
        return sha512(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param in 要SHA-512的输入流
	 * @return SHA-512后的字节数组
	 */
    public static final byte[] sha512(InputStream in) {
        return sha512(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param b 要SHA-512的字节数组
	 * @return SHA-512后的字节数组
	 */
    public static final byte[] sha512(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_SHA_512);
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param text 要SHA-512的字符串
	 * @return SHA-512后的字节数组的hex后字符串
	 */
    public static final String sha512Hex(String text) {
        return sha512Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param in 要SHA-512的输入流
	 * @return SHA-512后的字节数组的hex后字符串
	 */
    public static final String sha512Hex(InputStream in) {
        return sha512Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param b 要SHA-512的字节数组
	 * @return SHA-512后的字节数组的hex后字符串
	 */
    public static final String sha512Hex(byte[] b) {
        return Encode.hex(sha512(b));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param text 要SHA-512的字符串
	 * @return SHA-512后的字节数组的base64后字符串
	 */
    public static final String sha512Base64(String text) {
        return sha512Base64(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param in 要SHA-512的输入流
	 * @return SHA-512后的字节数组的base64后字符串
	 */
    public static final String sha512Base64(InputStream in) {
        return sha512Base64(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-512(信息-摘要算法)码
	 * @param b 要SHA-512的字节数组
	 * @return SHA-512后的字节数组的base64后字符串
	 */
    public static final String sha512Base64(byte[] b) {
        return Encode.base64(sha512(b));
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组
	 */
    public static final byte[] sha1(String text) {
        return sha1(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组
	 */
    public static final byte[] sha1(InputStream in) {
        return sha1(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组
	 */
    public static final byte[] sha1(byte[] b) {
        return getMessageDigest(b, EncryptConstants.ALGO_SHA_1);
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组的hex后字符串
	 */
    public static final String sha1Hex(String text) {
        return sha1Hex(StringUtil.toBytes(text));
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组的hex后字符串
	 */
    public static final String sha1Hex(InputStream in) {
        return sha1Hex(StreamChannel.read(in));
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字节数组的hex后字符串
	 */
    public static final String sha1Hex(byte[] b) {
        return Encode.hex(sha1(b));
    }

    /**
	 * 获得信息摘要
	 * @param b 要加密的字节数组
	 * @param algorithm 摘要算法
	 * @return 加密后的字节数组
	 */
    public static final byte[] getMessageDigest(byte[] b, String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm).digest(b);
        } catch (NoSuchAlgorithmException e) {
            throw new CustomRuntimeException(e);
        }
    }
}
