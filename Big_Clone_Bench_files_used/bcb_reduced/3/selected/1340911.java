package Logic;

import java.security.MessageDigest;

public class MD5 {

    private Splitter splitter;

    public MD5(Splitter splitter) {
        this.splitter = splitter;
    }

    public String md5(String plain) {
        String hash = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuffer sb = new StringBuffer();
            byte buf[] = plain.getBytes();
            byte md5[] = md.digest(buf);
            for (int i = 0; i < md5.length; i++) {
                String tmpStr = "0" + Integer.toHexString((0xff & md5[i]));
                sb.append(tmpStr.substring(tmpStr.length() - 2));
            }
            hash = sb.toString();
        } catch (Exception e) {
            splitter.log(this, 5, splitter.translate("md5.failed"));
        }
        return hash;
    }
}
