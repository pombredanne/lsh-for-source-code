package sas.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import static java.lang.Math.*;

/**
 * @author Viedt
 */
public final class FileToolkit implements FileConfiguration {

    private static final Logger log = Logger.getLogger(FileToolkit.class);

    private static final int HASHBUFFER_SIZE = 50 * BUFFERLENGTH;

    private static FileToolkit instance;

    private static MessageDigest digest;

    /**
     * Private contructor, which initializes the message digest
     */
    private FileToolkit() {
        try {
            digest = MessageDigest.getInstance(HASHALG);
        } catch (NoSuchAlgorithmException ex) {
            log.fatal(ex.getMessage());
        }
    }

    /**
	 * Thread-safe method for returning a singleton object of this class
	 * @return Instance of this class
	 */
    public static synchronized FileToolkit getToolkit() {
        if (instance == null) {
            instance = new FileToolkit();
        }
        return instance;
    }

    /**
     * @param size
     * @return a String representing the filesize
     */
    public String formatFileSize(long size) {
        String outputsize;
        double tempsize;
        if (size > 1024 * 1024) {
            tempsize = round(size * 10 / 1024 / 1024);
            tempsize = tempsize / 10;
            outputsize = String.valueOf(tempsize) + " MB";
        } else {
            tempsize = round(size * 10 / 1024);
            tempsize = tempsize / 10;
            outputsize = String.valueOf(tempsize) + " KB";
        }
        return outputsize;
    }

    /**
	 * compresses an file. not recommended for already compressed files like jpg or zip
	 * @param file the file, which should be zipped
	 * @return a byte array containing the zipped file
	 * @throws FileNotFoundException if file does not exist 
	 */
    public byte[] packData(File file) throws FileNotFoundException {
        byte[] data;
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        try {
            byte[] buffer = new byte[BUFFERLENGTH];
            FileInputStream fileIn = new FileInputStream(file);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            GZIPOutputStream zipOut = new GZIPOutputStream(byteOut);
            int len = 0;
            while ((len = fileIn.read(buffer, 0, BUFFERLENGTH)) >= 0) {
                zipOut.write(buffer, 0, len);
            }
            fileIn.close();
            zipOut.close();
            byteOut.close();
            data = byteOut.toByteArray();
        } catch (IOException ex) {
            data = new byte[0];
            log.error("Error during compression: " + ex.getMessage());
        }
        return data;
    }

    /**
	 * unzips data and stores it to an file
	 * @param data the zipped data
	 * @param file the result file
	 */
    public void unpackData(byte[] data, File file) {
        try {
            byte[] buffer = new byte[BUFFERLENGTH];
            GZIPInputStream zipIn = new GZIPInputStream(new ByteArrayInputStream(data));
            FileOutputStream fileOut = new FileOutputStream(file);
            int len = 0;
            while ((len = zipIn.read(buffer, 0, BUFFERLENGTH)) >= 0) {
                fileOut.write(buffer, 0, len);
            }
            zipIn.close();
            fileOut.close();
        } catch (IOException ex) {
            log.error("Error during decompression: " + ex.getMessage());
        }
    }

    /**
     * calculates a hash value using the algorithm configured in {@link FileConfiguration#HASHALG}
     * @param data a byte array, of which the hash value should be calculated
     * @return a string containing the hash value or null, if the hash could not be calculated
     */
    public String calcHash(byte[] data) {
        String hash = null;
        if (digest == null) {
            log.error("Hash cannot be calculated. The MessageDigest has not been initialised probably.");
            hash = "0";
        } else {
            digest.update(data);
            hash = new BigInteger(digest.digest()).toString(16);
        }
        return hash;
    }

    /**
     * calculates a hash value using the algorithm configured in {@link FileConfiguration#HASHALG}
     * @param file a file, the hash value shall represent
     * @return a string containing the hash value or null, if the hash could not be calculated
     */
    public String calcHash(File file) {
        String hash = null;
        if (digest == null) {
            log.error("Hash cannot be calculated. The MessageDigest has not been initialised probably.");
            hash = "0";
        } else {
            try {
                byte[] buffer = new byte[HASHBUFFER_SIZE];
                int len;
                FileInputStream fileIn;
                fileIn = new FileInputStream(file);
                while ((len = fileIn.read(buffer)) >= 0) {
                    digest.update(buffer, 0, len);
                }
                fileIn.read(buffer);
                fileIn.close();
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            hash = new BigInteger(digest.digest()).toString(16);
        }
        return hash;
    }
}
