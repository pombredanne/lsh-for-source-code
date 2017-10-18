package no.eirikb.utils.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 *
 * @author Eirik Brandtzæg <eirikdb@gmail.com>
 */
public class MD5File {

    public static String MD5File(File file) {
        try {
            if (file.isFile()) {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                InputStream is = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int read = 0;
                try {
                    while ((read = is.read(buffer)) > 0) {
                        digest.update(buffer, 0, read);
                    }
                    byte[] md5sum = digest.digest();
                    BigInteger bigInt = new BigInteger(1, md5sum);
                    String md5 = bigInt.toString(16);
                    if (md5.length() == 31) {
                        md5 = "0" + md5;
                    }
                    return md5;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Note; is recursive - will fail at large files.
     * @param file
     * @return
     */
    public static String MD5Directory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files);
            String total = "";
            for (File f : files) {
                total += MD5Directory(f);
            }
            return total;
        } else if (file.isFile()) {
            return file.getName() + ":" + MD5File(file);
        }
        return null;
    }
}
