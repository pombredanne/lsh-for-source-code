package net.sf.jmp3renamer.plugins.DoubleFinder;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import net.sf.jmp3renamer.I18N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MD5 {

    private static transient Logger logger = LoggerFactory.getLogger(MD5.class);

    public static String calculate(File file) {
        String digest = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer)) >= 0) {
                md.update(buffer, 0, length);
            }
            byte[] md5Bytes = md.digest();
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0; i < md5Bytes.length; i++) {
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16) hexValue.append("0");
                hexValue.append(Integer.toHexString(val));
            }
            digest = hexValue.toString();
        } catch (Exception e) {
            logger.error(I18N.translate("error.calculate"), e);
        }
        return digest;
    }
}
