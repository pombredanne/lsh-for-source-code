package sunsite.tools;

import java.security.*;
import java.util.Random;
import java.net.*;

/**
 * 使用时必需创建一个新的GUID变量，最简单的获得GUID的方法是调用该变量的getNewGuid()
 * @author Administrator
 */
public class GUID {

    private StringBuffer sbBeforeMd5 = new StringBuffer();

    private String rawGUID = "";

    private String seed = "";

    private boolean bSecure = false;

    public static final int BeforeMD5 = 1;

    public static final int AfterMD5 = 2;

    public static final int FormatString = 3;

    /**
     * 构造器执行了初始化工作，产生md5码种子
     */
    public GUID() {
        newGuidSeed(false);
    }

    /**
     * 定制获取 GUID 信息，不建议使用该函数
     * @param nFormatType
     * @param secure
     * @return
     */
    public String getNewGuid(int nFormatType, boolean secure) {
        newGuidSeed(secure);
        String sGuid = "";
        if (BeforeMD5 == nFormatType) {
            sGuid = this.seed;
        } else if (AfterMD5 == nFormatType) {
            sGuid = this.rawGUID;
        } else {
            sGuid = this.toString();
        }
        return sGuid;
    }

    /**
     * 定制获取 GUID 信息，不建议使用该函数
     * @param nFormatType
     * @return
     */
    public String getNewGuid(int nFormatType) {
        return this.getNewGuid(nFormatType, this.bSecure);
    }

    /**
     * 简单的获得GUID的方法
     * @return
     */
    public String getNewGuid() {
        return this.getNewGuid(GUID.AfterMD5, this.bSecure);
    }

    /**
     * 获得GUID，可选是否使用安全策略
     * @param secure
     * @return
     */
    public String getNewGuid(boolean secure) {
        return this.getNewGuid(GUID.AfterMD5, secure);
    }

    @Override
    public String toString() {
        String raw = rawGUID.toUpperCase();
        StringBuffer sb = new StringBuffer();
        sb.append(raw.substring(0, 8));
        sb.append("-");
        sb.append(raw.substring(8, 12));
        sb.append("-");
        sb.append(raw.substring(12, 16));
        sb.append("-");
        sb.append(raw.substring(16, 20));
        sb.append("-");
        sb.append(raw.substring(20));
        return sb.toString();
    }

    /**
     * 重新产生种子
     */
    public void newGuidSeed() {
        this.newGuidSeed(false);
    }

    /**
     * 重新产生种子
     * @param secure 是否使用安全策略
     */
    public void newGuidSeed(boolean secure) {
        SecureRandom sr = new SecureRandom();
        long secureInitializer = sr.nextLong();
        Random rand = new Random(secureInitializer);
        String host_ip = "";
        try {
            host_ip = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException err) {
            err.printStackTrace();
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException err) {
            err.printStackTrace();
        }
        try {
            long time = System.currentTimeMillis();
            long randNumber = 0;
            if (secure) {
                randNumber = sr.nextLong();
            } else {
                randNumber = rand.nextLong();
            }
            sbBeforeMd5.append(host_ip);
            sbBeforeMd5.append(":");
            sbBeforeMd5.append(Long.toString(time));
            sbBeforeMd5.append(":");
            sbBeforeMd5.append(Long.toString(randNumber));
            seed = sbBeforeMd5.toString();
            md5.update(seed.getBytes());
            byte[] array = md5.digest();
            StringBuffer temp_sb = new StringBuffer();
            for (int i = 0; i < array.length; i++) {
                int b = array[i] & 0xFF;
                if (b < 0x10) temp_sb.append('0');
                temp_sb.append(Integer.toHexString(b));
            }
            rawGUID = temp_sb.toString();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
