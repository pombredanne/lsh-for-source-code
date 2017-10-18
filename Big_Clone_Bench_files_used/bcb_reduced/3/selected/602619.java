package mykeynote.misc;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

    public static final String MD5 = "MD5", SHA1 = "SHA1", SHA256 = "SHA256", SHA384 = "SHA384", SHA512 = "SHA512";

    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    /**
	 * Builds a checksum for a given file
	 * @param file The file for which the checksum should be build
	 * @param algorithm What algorithm should be used, all algorithms 
	 * are listen in Cryptographics
	 * @return Returns the checksum as a byte Array, to convert it to
	 * Hex-String use toHexString provided in this Class
	 * @throws FileNotFoundException Reports that the file for which checksum should be build was not found
	 * @throws NoSuchAlgorithmException Reports that the algorithm does not exist
	 * @throws IOException Reports that there was an io error during the operation
	 */
    public String checksum(File file, String algorithm) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        InputStream filein = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        int i;
        do {
            i = filein.read(buffer);
            if (i > 0) {
                digest.update(buffer, 0, i);
            }
        } while (i != -1);
        filein.close();
        return toHexString(digest.digest());
    }

    /** Takes a byte array and returns it as a Hex String
	 * @param bytes The byte array that should be converted
	 * @return the hex string to the given array 
	 * @throws UnsupportedEncodingException Reports that the used in method ASCII 
	 * encoding is not supported... which schould not come out 
	 */
    public String toHexString(byte[] bytes) throws UnsupportedEncodingException {
        byte[] hex = new byte[2 * bytes.length];
        int index = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }

    /**
	 * Compares if a file has a given checksum, saved in a checksum file
	 * @param filename The file for which we should check if it has a specific checksum
	 * @param checksumFile The checksum file, where the checksum was saved
	 * @param algorithm The Algorithm to use for building the checksum
	 * @return Returns <b>true</b> if the checksum from the file is the same as the ckecksum
	 * saved in the checksum file, otherwise returns <b>false</b>
	 * @throws FileNotFoundException Reports that a file was not found
	 * @throws NoSuchAlgorithmException Reports that the given algorithm is not supported
	 * @throws IOException Reports that an IO exception of some sort has occured
	 */
    public boolean compareChecksum(File filename, File checksumFile, String algorithm) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        byte[] checksum1 = checksum(filename, algorithm).getBytes();
        byte[] checksum2 = new byte[checksum1.length];
        InputStream is = new FileInputStream(checksumFile);
        is.read(checksum2);
        if (new String(checksum2).equals(new String(checksum1))) return true;
        return false;
    }

    /**
	 * Builds a checksum for a given String
	 * @param string The String for which the checksum should be build
	 * @param algorithm What algorithm should be used, all algorithms 
	 * are listen in the Cryptographics package
	 * @return Returns the checksum as a byte Array, to convert it to
	 * Hex-String use toHexString provided in this Class
	 * @throws NoSuchAlgorithmException Reports that the algorithm does not exist
	 * @throws IOException Reports that there was an io error during the operation
	 */
    public String checksum(String string, String algorithm) throws NoSuchAlgorithmException, IOException {
        InputStream strin = new ByteArrayInputStream(string.getBytes());
        byte[] buffer = new byte[128];
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        int i;
        do {
            i = strin.read(buffer);
            if (i > 0) {
                digest.update(buffer, 0, i);
            }
        } while (i != -1);
        return toHexString(digest.digest());
    }
}
