package net.diet_rich.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.diet_rich.util.StringUtils;

/**
 * static io utility methods
 * 
 * @author Georg Dietrich
 */
public class IOUtils {

    /**
	 * generate a hash string for a file's contents
	 * 
	 * @param algorithm
	 *            the hash algorithm to use (e.g. MD2, MD5, SHA-1, SHA-256,
	 *            SHA-384, SHA-512)
	 * @return the lowercase hexadecimal hash string
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
    public static String getHash(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        FileInputStream filein = new FileInputStream(file);
        DigestInputStream digest = new DigestInputStream(filein, MessageDigest.getInstance(algorithm));
        byte[] buffer = new byte[8192];
        while (digest.read(buffer) != -1) ;
        digest.close();
        return StringUtils.toHex(digest.getMessageDigest().digest());
    }

    /**
	 * @return the directory from where this application runs
	 */
    public static String location() {
        File location;
        try {
            location = new File(new IOUtils().getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!location.isDirectory()) location = location.getParentFile();
        return location.getPath();
    }
}
