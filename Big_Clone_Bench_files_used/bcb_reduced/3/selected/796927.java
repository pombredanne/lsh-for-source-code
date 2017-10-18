package moller.util.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import moller.util.string.StringUtil;

public class MD5 {

    public static String calculate(File theFileToCalculate) {
        String hashValue = "";
        if (theFileToCalculate.isFile()) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                InputStream is = new FileInputStream(theFileToCalculate);
                byte[] buffer = new byte[8192];
                int read = 0;
                try {
                    while ((read = is.read(buffer)) > 0) {
                        digest.update(buffer, 0, read);
                    }
                    hashValue = StringUtil.convertToHexString(digest.digest());
                } catch (IOException e) {
                    throw new RuntimeException("Unable to process file for MD5", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
                    }
                }
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        return hashValue;
    }
}
