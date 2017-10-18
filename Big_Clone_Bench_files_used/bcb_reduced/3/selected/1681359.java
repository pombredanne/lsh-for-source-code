package eu.wc.snippets.hashes;

import eu.wc.snippets.conversion.ByteArrayToAny;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Perform hashing operations on a File.<br>
 * Get a hash,compare a hash are supported.
 * @author Francisco Dominguez Santos
 */
public class FileHashCheck {

    /**
     * Constant indicating a possible hashing algorithm
     */
    public static final String SHA256 = "SHA-256";

    /**
     * Constant indicating a possible hashing algorithm
     */
    public static final String SHA512 = "SHA-512";

    private MessageDigest md = null;

    private File f;

    /**
     * Creates a new instance of FileHashCheck
     * @param f The file on which operations can be performed
     * @param algorithm The algorithm to use for the hash generation and comparison
     * @throws java.security.NoSuchAlgorithmException If the given algorithm is not supported by the installed provider
     */
    public FileHashCheck(File f, String algorithm) throws NoSuchAlgorithmException {
        this.f = f;
        md = MessageDigest.getInstance(algorithm);
    }

    /**
     * Returns a byte array representing the hash made from the file
     * @throws java.io.FileNotFoundException if the file is not found
     * @throws java.io.IOException if a read/write error occurs during the hash generation
     * @return hash as a byte array
     */
    public byte[] getHashFromFile() throws FileNotFoundException, IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(f);
            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    md.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
        } finally {
            fis.close();
        }
        return md.digest();
    }

    /**
     * Returns a hexadecimal string representing the hash made from the file
     * @throws java.io.FileNotFoundException if the file is not found
     * @throws java.io.IOException if a read/write error occurs during the hash generation
     * @return A hexadecimal string representing the hash
     */
    public String getHashFromFileAsString() throws FileNotFoundException, IOException {
        byte[] output = getHashFromFile();
        return ByteArrayToAny.byteArrayToHex(output);
    }

    /**
     * Compares the given hash to the hash generated from this file
     * @param hash The hash to be used for comparison
     * @throws java.io.FileNotFoundException if the file is not found
     * @throws java.io.IOException if a read/write error occurs during the hash generation
     * @return true if the hashes are equal, false otherwise
     */
    public boolean isSameHash(byte[] hash) throws FileNotFoundException, IOException {
        byte[] currentHash = this.getHashFromFile();
        return Arrays.equals(hash, currentHash);
    }

    /**
     * Compares the given hash to the hash generated from this file
     * @param hexHash A hexadecimal representation of the hash to be compared
     * @throws java.io.FileNotFoundException if the file is not found
     * @throws java.io.IOException if a read/write error occurs during the hash generation
     * @return true if the hashes are equal, false otherwise
     */
    public boolean isSameHash(String hexHash) throws FileNotFoundException, IOException {
        String currentHash = this.getHashFromFileAsString();
        return hexHash.equalsIgnoreCase(currentHash);
    }
}
