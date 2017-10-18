package net.sf.clearwork.core.utils.secret;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * ��ȫɢ���㷨 SHA (Secure Hash Algorithm)
 * <p>
 * �������ұ�׼�ͼ����ַ����Ĺ�ұ�׼FIPS PUB 180-1��һ���ΪSHA-1��<br>
 * ��Գ��Ȳ�����264������λ����Ϣ����160λ����ϢժҪ�����<br>
 * SHA��һ����ݼ����㷨�����㷨�������ר�Ҷ������ķ�չ�͸Ľ����������ƣ�<br>
 * �����ѳ�Ϊ���ϵ��ȫ��ɢ���㷨֮һ�������㷺ʹ�á�<br>
 * �ü����㷨�ǵ�����ܣ������ܵ���ݲ�����ͨ����ܻ�ԭ��
 *
 *
 * @author <a href="mailto:huqiyes@gmail.com">huqi</a>
 * @serialData 2007
 */
public final class SHAUtils {

    private static final String SHA_ALGORITHM = "SHA-1";

    private SHAUtils() {
    }

    private static MessageDigest getSHADigestAlgorithm() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(SHA_ALGORITHM);
    }

    public static byte[] getSHADigest(byte source[]) throws NoSuchAlgorithmException {
        return getSHADigestAlgorithm().digest(source);
    }

    public static byte[] getSHADigest(String source) throws NoSuchAlgorithmException {
        return getSHADigest(source.getBytes());
    }

    public static String getSHADigestHex(byte source[]) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getSHADigest(source)));
    }

    public static String getSHADigestHex(String source) throws NoSuchAlgorithmException {
        return new String(Hex.encodeHex(getSHADigest(source.getBytes())));
    }

    public static String getSHADigestBase64(byte source[]) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getSHADigest(source)));
    }

    public static String getSHADigestBase64(String source) throws NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getSHADigest(source.getBytes())));
    }
}
