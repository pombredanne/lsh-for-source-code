package org.mooym;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Responsible for creating MD5 checksums.
 * 
 * @author roesslerj
 */
public class MD5Checker {

    private final MessageDigest digest;

    /**
   * Constructor.
   */
    public MD5Checker() {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exc) {
            throw new RuntimeException("MD5 algorithm not available!", exc);
        }
    }

    /**
   * Calculates the MD5 of a given file.
   * 
   * @param file
   *          The file to calculate the MD5 for.
   * @return The MD5 as a String value.
   * @throws IOException
   *           If problems reading the file occur.
   */
    public String getMD5(File file) throws IOException {
        BufferedInputStream inputStream = null;
        digest.reset();
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];
            for (int numberOfBytesRead = inputStream.read(buffer); numberOfBytesRead != -1; numberOfBytesRead = inputStream.read(buffer)) {
                digest.update(buffer);
            }
            return convertByteArrayToString(digest.digest());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
   * Calls {@link MessageDigest#digest()} and returns the md5 value of the file
   * in the form of a String.
   * 
   * @return A String containing the md5 value.
   */
    public String getValue() {
        return convertByteArrayToString(digest.digest());
    }

    /**
   * @see MessageDigest#reset()
   */
    public void reset() {
        digest.reset();
    }

    /**
   * @see MessageDigest#update(byte[])
   */
    public void update(byte[] arg0) {
        digest.update(arg0);
    }

    /**
   * Converts an array of bytes into an Hex String.
   * 
   * @param byteArray
   *          The array of bytes to convert.
   * @return The Hex String.
   */
    private String convertByteArrayToString(byte[] byteArray) {
        String md5Client;
        StringBuffer result = new StringBuffer();
        for (byte element : byteArray) {
            result.append(String.format("%x", element));
        }
        md5Client = result.toString().toUpperCase();
        return md5Client;
    }
}
