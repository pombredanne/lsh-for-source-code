package org.dueam.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class MD5Utils {

    public static void main(String[] args) {
        System.out.println(MD5Utils.MD5Encoder("888888"));
    }

    private static MessageDigest md5 = null;

    private static final String DEFAULT_ARI = "MD5";

    /**
	 * ��ȡժҪĬ���㷨ΪMD5
	 * @param msg �ַ�
	 * @return
	 */
    public static String MD5Encoder(String msg) {
        init();
        return StringUtils.byteArrayToHexString(md5.digest(msg.getBytes()));
    }

    /**
	 * ��ȡժҪĬ���㷨ΪMD5
	 * @param f �ļ�
	 * @return
	 * @throws FileNotFoundException
	 */
    public static String MD5Encoder(File f) throws FileNotFoundException {
        InputStream fis = new FileInputStream(f);
        return MD5Encoder(fis);
    }

    /**
	 * ��ȡժҪĬ���㷨ΪMD5
	 * @param is ������
	 * @return
	 */
    public static String MD5Encoder(InputStream is) {
        if (null == is) return null;
        init();
        byte[] rb = new byte[4096];
        try {
            int size = is.read(rb);
            while (0 < size) {
                md5.update(rb, 0, size);
                size = is.read(rb);
            }
            return StringUtils.byteArrayToHexString(md5.digest());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * ��ʼ��Ĭ��ΪMD5
	 *
	 */
    private static void init() {
        if (md5 == null) {
            MessageDigestInit("MD5");
        }
    }

    /**
	 * ��ʼ���㷨
	 * @param aril �㷨��MD5 MD2 SHA-1 SHA-256 SHA-384 SHA-512��
	 * @return
	 */
    public static boolean MessageDigestInit(String aril) {
        if (null == aril || 1 > aril.length()) {
            aril = DEFAULT_ARI;
        }
        try {
            md5 = MessageDigest.getInstance(aril);
            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }
}
