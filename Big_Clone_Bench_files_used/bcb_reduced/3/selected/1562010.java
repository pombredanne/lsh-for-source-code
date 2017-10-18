package org.ikasan.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * A utility class for Checksum activities
 * 
 * @author Ikasan Development Team
 */
public class ChecksumUtils {

    /** The logger instance. */
    private static Logger logger = Logger.getLogger(ChecksumUtils.class);

    /** Default algorithm is MD5 */
    private static final String defaultAlgorithm = "MD5";

    /**
     * Get the checksum given an ImputStream and an algorithm
     * 
     * @param stream The stream for which to calculate the checksum
     * @param algorithm 
     * @return <code>String</code> representing the checksum
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    public static String getChecksum(InputStream stream, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest checksum = null;
        checksum = MessageDigest.getInstance(algorithm);
        byte[] data = new byte[128];
        while (true) {
            int bytesRead = stream.read(data);
            if (bytesRead < 0) {
                break;
            }
            checksum.update(data, 0, bytesRead);
        }
        byte[] byteDigest = checksum.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteDigest.length; i++) {
            String hex = Integer.toHexString(0xff & byteDigest[i]);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return new String(sb.toString());
    }

    /**
     * Get the checksum
     * @param stream
     * @return checksum
     * @throws NoSuchAlgorithmException
     */
    public static String getChecksum(ByteArrayOutputStream stream) throws NoSuchAlgorithmException {
        return getChecksum(stream, defaultAlgorithm);
    }

    /**
     * Get the checksum
     * 
     * @param stream
     * @param algorithm
     * @return checksum
     * @throws NoSuchAlgorithmException
     */
    public static String getChecksum(ByteArrayOutputStream stream, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest checksum = null;
        checksum = MessageDigest.getInstance(algorithm);
        byte[] buf = stream.toByteArray();
        checksum.update(buf);
        byte[] byteDigest = checksum.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteDigest.length; i++) {
            String hex = Integer.toHexString(0xff & byteDigest[i]);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return new String(sb.toString());
    }

    /**
     * Get the checksum
     * 
     * @param stream
     * @return checksum
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getChecksum(InputStream stream) throws NoSuchAlgorithmException, IOException {
        return getChecksum(stream, defaultAlgorithm);
    }

    /**
     * Get checksum given a file
     * 
     * @param file
     * @return checksum
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String getChecksum(File file) throws IOException, NoSuchAlgorithmException {
        return getChecksum(file, defaultAlgorithm);
    }

    /**
     * Get checksum given a file and an algorithm
     *  
     * @param file
     * @param algorithm
     * @return checksum
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String getChecksum(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        return getChecksum(stream, algorithm);
    }

    /**
     * Compare the two checksums as a straight String comparison throw a 
     * SFTPChecksumFailedException if they don't match, else do nothing
     * 
     * @param checksum1 The first checksum
     * @param checksum2 The second checksum
     * @return true if checksums match, else false
     */
    public static boolean checksumMatch(String checksum1, String checksum2) {
        logger.debug("Checksum 1: [" + checksum1 + "]");
        logger.debug("Checksum 2: [" + checksum2 + "]");
        if (checksum1.equals(checksum2)) {
            logger.info("Checksums match.");
            return true;
        }
        logger.warn("Checksums do not match.");
        return false;
    }
}
