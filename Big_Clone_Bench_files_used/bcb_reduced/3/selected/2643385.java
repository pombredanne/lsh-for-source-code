package backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class computes various hashsums of files
 * 
 * @author Rajmund Witt
 * @version 0.5
 * 
 * @see LogWriter
 */
public abstract class Hasher {

    private static LogWriter log_ = LogWriter.getInstance();

    private static MessageDigest digest_;

    private static int length_;

    /**
	 * 
	 * @param path to the file to be hashed
	 * @return String representation of the MD5 sum of the file provided
	 */
    public static String computeMD5ofFile(String path) {
        String md5 = null;
        try {
            digest_ = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        }
        length_ = 32;
        md5 = hashIt(path);
        return md5;
    }

    /**
	 * 
	 * @param path to the file to be hashed
	 * @return String representation of the SHA1 sum of the file provided
	 */
    public static String computeSHA1ofFile(String path) {
        String sha1 = null;
        try {
            digest_ = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        }
        length_ = 40;
        sha1 = hashIt(path);
        return sha1;
    }

    /**
	 * Performs the acual computation of the hashsum based on the settings
	 * @param path to the file
	 * @return the hashsum based on the settings
	 */
    private static String hashIt(String path) {
        String hash = "";
        InputStream input = null;
        try {
            File theFile = new File(path);
            input = new FileInputStream(theFile);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = input.read(buffer)) > 0) {
                digest_.update(buffer, 0, read);
            }
            byte[] hashValues = digest_.digest();
            BigInteger bigInt = new BigInteger(1, hashValues);
            hash = bigInt.toString(16);
        } catch (NullPointerException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        } catch (FileNotFoundException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        } catch (SecurityException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        } catch (NumberFormatException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        } catch (IOException exc) {
            exc.printStackTrace();
            log_.appendString("Problem encountered hashing file: " + path);
        } finally {
            try {
                input.close();
            } catch (IOException exc) {
                exc.printStackTrace();
                log_.appendString("Problem encountered hashing file: " + path);
            }
        }
        while (hash.length() < length_) {
            hash = "0" + hash;
        }
        return hash;
    }
}
