package net.sf.clearwork.core.utils.secret;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * MD5��ȫ���� Message-Digest Algorithm 5
 * <p>
 * ��90������MIT�ļ�����ѧʵ���Һ�RSA Data Security Inc��������MD2��MD3��MD4��չ������<br>
 * MD5�����ⳤ�ȵ�"�ֽڴ�"�任��һ��128bit�Ĵ�����������һ����������ַ�任�㷨<br>
 * ���ü����㷨�ǵ�����ܣ����ܵ���ݲ�����ͨ����ܻ�ԭ��
 *
 * @author <a href="mailto:huqiyes@gmail.com">huqi</a>
 * @serialData 2007
 */
public final class MD5Utils {

    private static final String MD5_ALGORITHM = "MD5";

    private MD5Utils() {
    }

    private static MessageDigest getMD5DigestAlgorithm() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(MD5_ALGORITHM);
    }

    public static byte[] getMD5Digest(byte source[]) throws NoSuchAlgorithmException {
        return getMD5DigestAlgorithm().digest(source);
    }

    public static byte[] getMD5Digest(String source) throws NoSuchAlgorithmException {
        return getMD5Digest(source.getBytes());
    }

    public static String getMD5DigestHex(byte source[]) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getMD5Digest(source)));
    }

    public static String getMD5DigestHex(String source) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getMD5Digest(source)));
    }

    public static String getMD5DigestBase64(byte source[]) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getMD5Digest(source)));
    }

    public static String getMD5DigestBase64(String source) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getMD5Digest(source)));
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println(getMD5DigestHex("abc"));
        System.out.println(getMD5DigestBase64("abc"));
    }
}
