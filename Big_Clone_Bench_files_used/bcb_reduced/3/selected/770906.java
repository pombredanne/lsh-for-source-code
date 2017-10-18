package cn.lzh.common.encrypt;

import java.security.MessageDigest;
import cn.lzh.common.string.HEXEncoder;

public class MD5Util {

    public static String MD5Encode(String origin) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = HEXEncoder.encode(md.digest(resultString.getBytes()));
        } catch (Exception ex) {
        }
        return resultString;
    }
}
