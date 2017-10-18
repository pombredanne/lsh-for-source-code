package com.gever.util.crypto;

import java.security.*;

/**
 * ��ϢժҪ��
 * @author Hu.Walker
 * @version 0.9
 */
public class Digest {

    public Digest() {
    }

    public static final String MD5 = "MD5";

    public static final String SHA1 = "SHA-1";

    /**
   * <p>��ϢժҪ</p>
   * һ����ϢժҪ����һ����ݿ������ָ�ơ�����һ�����ⳤ�ȵ�һ����ݿ���м��㣬����һ��Ψһָӡ��
   * @param message Ҫ���в���ժҪ���ַ���Ϣ
   * @param algorithm ժҪ�㷨,֧��MD5��SHA-1
   * @return ��ϢժҪ�ֽ�����
   * @throws NoSuchAlgorithmException ָ����ժҪ�㷨����
   */
    public static byte[] getMessageDigest(String message, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(algorithm);
        alg.update(message.getBytes());
        return alg.digest();
    }

    /**
   * <p>��ϢժҪ</p>
   * һ����ϢժҪ����һ����ݿ������ָ�ơ�����һ�����ⳤ�ȵ�һ����ݿ���м��㣬����һ��Ψһָӡ��
   * @param message Ҫ���в���ժҪ���ֽ�������Ϣ
   * @param algorithm ժҪ�㷨,֧��MD5��SHA-1
   * @return ��ϢժҪ�ֽ�����
   * @throws NoSuchAlgorithmException ָ����ժҪ�㷨����
   */
    public static byte[] getMessageDigest(byte[] message, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(algorithm);
        alg.update(message);
        return alg.digest();
    }

    /**
   * ��ָ���㷨��ժҪ����Ϣ����У��
   * @param message ԭ��Ϣ
   * @param algorithm ժҪ�㷨
   * @param digest Ҫ��֤��ժҪ
   * @return У����,true=У����ȷ,false=У�����
   * @throws NoSuchAlgorithmException
   */
    public static boolean verifyMessageDigest(String message, String algorithm, byte[] digest) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(algorithm);
        alg.update(message.getBytes());
        return MessageDigest.isEqual(digest, alg.digest());
    }

    /**
   * ��ָ���㷨��ժҪ����Ϣ����У��
   * @param message ԭ�ֽ�������Ϣ
   * @param algorithm ժҪ�㷨
   * @param digest Ҫ��֤��ժҪ
   * @return У����,true=У����ȷ,false=У�����
   * @throws NoSuchAlgorithmException
   */
    public static boolean verifyMessageDigest(byte[] message, String algorithm, byte[] digest) throws NoSuchAlgorithmException {
        MessageDigest alg = MessageDigest.getInstance(algorithm);
        alg.update(message);
        return MessageDigest.isEqual(digest, alg.digest());
    }
}
