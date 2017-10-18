package org.charry.xupdater.configgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.*;

/**
 * 
 * @author WangHui
 *
 */
public class MD5Util {

    public static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static final String MD5(File file) {
        try {
            InputStream fis = new FileInputStream(file.getPath());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, numRead);
            }
            fis.close();
            return toHexString(md5.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexDigits[(b[i] & 0xf0) >>> 4]);
            sb.append(hexDigits[b[i] & 0x0f]);
        }
        return sb.toString();
    }
}
