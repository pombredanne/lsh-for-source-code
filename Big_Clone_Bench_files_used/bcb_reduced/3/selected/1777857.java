package org.hlj.commons.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.hlj.commons.conversion.ConversionUtil;
import org.hlj.commons.exception.CustomRuntimeException;
import org.hlj.commons.string.StringUtil;
import org.hlj.param.constants.EncryptConstants;
import org.hlj.param.constants.StringConstants;
import org.hlj.param.params.CommonParams;

/**
 * 字符串加密解密类
 * @author WD
 * @since JDK5
 * @version 1.0 2009-09-28
 */
public final class EncryptUtil {

    private static final byte[] AES_KEY;

    private static final byte[] DES_KEY;

    private static final int AES_LENGTH;

    private static final int DES_LENGTH;

    static {
        byte[] key = StringUtil.toBytes(CommonParams.getEncryptKey());
        int num = key.length - 1;
        AES_LENGTH = 16;
        AES_KEY = new byte[AES_LENGTH];
        for (int i = 0; i < AES_LENGTH; i++) {
            AES_KEY[i] = ConversionUtil.toByte(key[i] + key[num - i]);
        }
        num = AES_KEY.length - 1;
        DES_LENGTH = 8;
        DES_KEY = new byte[DES_LENGTH];
        for (int i = 0; i < DES_LENGTH; i++) {
            DES_KEY[i] = ConversionUtil.toByte(AES_KEY[i] + AES_KEY[num - i]);
        }
    }

    /**
	 * 加密密码用 先普通加密 在获得摘要
	 * @param password 要加密的密码
	 * @return 加密后的密码
	 */
    public static final String getPassword(String password) {
        return absoluteEncrypt(password);
    }

    /**
	 * 先普通加密 在获得摘要 无法解密
	 * @param text 要加密的文本
	 * @return 加密后的文本
	 */
    public static final String absoluteEncrypt(String text) {
        return digest(encrypt(text));
    }

    /**
	 * 获得字符串摘要
	 * @param text 要获得摘要的字符串
	 * @return 获得摘要后的字符串
	 */
    public static final String digest(String text) {
        return getMessageDigest(text, CommonParams.getEncryptDigest());
    }

    /**
	 * 返回字符串的MD5(信息-摘要算法)码
	 * @param text 要MD5的字符串
	 * @return MD5后的字符串 text为空或发生异常返回原串
	 */
    public static final String getMD5(String text) {
        return getMessageDigest(text, EncryptConstants.ALGO_MD5);
    }

    /**
	 * 返回字符串的SHA-1(信息-摘要算法)码
	 * @param text 要SHA-1的字符串
	 * @return SHA-1后的字符串 text为空或发生异常返回原串
	 */
    public static final String getSHA1(String text) {
        return getMessageDigest(text, EncryptConstants.ALGO_SHA_1);
    }

    /**
	 * 获得信息摘要
	 * @param text 要加密的字符串
	 * @param algorithm 摘要算法
	 * @return 加密后的字符串 text,algorithm为空或发生异常返回原串
	 */
    public static final String getMessageDigest(String text, String algorithm) {
        try {
            byte[] digesta = MessageDigest.getInstance(algorithm).digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            String str = null;
            for (int i = 0; i < digesta.length; i++) {
                str = Integer.toHexString(0xFF & digesta[i]);
                sb.append((str.length() == 1) ? StringConstants.ZERO + str : str);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 加密字符串
	 * @param text 要加密的字符串
	 * @return 加密后的字符串
	 */
    public static final String encrypt(String text) {
        return EncryptConstants.ALGO_AES.equals(CommonParams.getEncryptAlgo()) ? encryptAES(text) : encryptDES(text);
    }

    /**
	 * 解密字符串
	 * @param text 要解密的字符串
	 * @return 解密后的字符串
	 */
    public static final String decrypt(String text) {
        return EncryptConstants.ALGO_AES.equals(CommonParams.getEncryptAlgo()) ? decryptAES(text) : decryptDES(text);
    }

    /**
	 * 可逆的加密算法 DES算法
	 * @param text 需要加密的字符串
	 * @return 返回加密后的字符串 text为空或发生异常返回原串
	 */
    public static final String encryptDES(String text) {
        return encrypt(text, DES_KEY, 0, DES_LENGTH, EncryptConstants.ALGO_DES);
    }

    /**
	 * 针对encode方法的解密 DES算法
	 * @param text 需要加密的字符串
	 * @return 返回加密后的字符串 text为空或发生异常返回原串
	 */
    public static final String decryptDES(String text) {
        return decrypt(text, DES_KEY, 0, DES_LENGTH, EncryptConstants.ALGO_DES);
    }

    /**
	 * 可逆的加密算法 AES算法
	 * @param text 需要加密的字符串
	 * @return 返回加密后的字符串 text为空或发生异常返回原串
	 */
    public static final String encryptAES(String text) {
        return encrypt(text, AES_KEY, 0, AES_LENGTH, EncryptConstants.ALGO_AES);
    }

    /**
	 * 针对encrypt方法的解密 AES算法
	 * @param text 需要加密的字符串
	 * @return 返回加密后的字符串 text为空或发生异常返回原串
	 */
    public static final String decryptAES(String text) {
        return decrypt(text, AES_KEY, 0, AES_LENGTH, EncryptConstants.ALGO_AES);
    }

    /**
	 * 加密字符串
	 * @param text 要加密的字符串
	 * @param key 加密密钥Key 长度有限制 DSE 为8位 ASE 为16位
	 * @param offset 偏移从第几位开始
	 * @param len 长度一共几位
	 * @param algorithm 算法
	 * @return 加密后的字符串
	 */
    private static String encrypt(String text, byte[] key, int offset, int len, String algorithm) {
        return StringUtil.byteToHex(doFinal(StringUtil.toBytes(text), key, offset, len, algorithm, Cipher.ENCRYPT_MODE));
    }

    /**
	 * 解密字符串
	 * @param text 要解密的字符串
	 * @param key 解密密钥Key 长度有限制 DSE 为8位 ASE 为16位
	 * @param offset 偏移从第几位开始
	 * @param len 长度一共几位
	 * @param algorithm 算法
	 * @return 解密后的字符串
	 */
    private static String decrypt(String text, byte[] key, int offset, int len, String algorithm) {
        return StringUtil.toString(doFinal(StringUtil.hexToByte(text), key, offset, len, algorithm, Cipher.DECRYPT_MODE));
    }

    /**
	 * 计算密文
	 * @param text 要计算的字符串
	 * @param key 计算密钥Key 长度有限制 DSE 为8位 ASE 为16位
	 * @param offset 偏移从第几位开始
	 * @param len 长度一共几位
	 * @param algorithm 算法
	 * @param mode 计算模式 加密和解密
	 * @return 字节数组
	 */
    private static byte[] doFinal(byte[] text, byte[] key, int offset, int len, String algorithm, int mode) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, offset, len, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(mode, skeySpec);
            return cipher.doFinal(text);
        } catch (Exception e) {
            throw new CustomRuntimeException(e);
        }
    }

    private EncryptUtil() {
    }
}
