package esa.herschel.randres.xmind.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHelper {

    public static String FILE_EXT = "xmind";

    public static String FILE_EXT_SEP = ".";

    public static String FILE_TOKEN_SEP = "_";

    private static MessageDigest m_digest = null;

    static {
        try {
            m_digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param name
	 * @param version
	 * @return the name (without extension) of the file
	 * e.g. phs_1
	 */
    public static String getFilename(String name, String version) {
        return name + FILE_TOKEN_SEP + version;
    }

    /**
	 * @param name
	 * @param version
	 * @return the complete name (with extension) of the file
	 * e.g. phs_1.xmind
	 */
    public static String getCompleteFilename(String name, String version) {
        return getFilename(name, version) + FILE_EXT_SEP + FILE_EXT;
    }

    /**
	 * @param array
	 * @return the MD5 hexadecimal string 
	 */
    public static String getMD5String(byte[] array) {
        m_digest.update(array, 0, array.length);
        BigInteger md5 = new BigInteger(1, m_digest.digest());
        String md5Str = md5.toString(16);
        return md5Str;
    }

    /**
	 * @param File path string
	 * @return the MD5 hexadecimal hash 
	 */
    public static String getMD5String(String filepath) {
        File f = new File(filepath);
        String md5Str = null;
        try {
            InputStream is = new FileInputStream(f);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                m_digest.update(buffer, 0, read);
            }
            byte[] md5sum = m_digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            md5Str = bigInt.toString(16);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        return md5Str;
    }
}
