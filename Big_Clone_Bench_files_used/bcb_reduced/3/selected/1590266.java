package net.sourceforge.jaulp.file.checksum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import net.sourceforge.jaulp.crypto.algorithm.Algorithm;
import net.sourceforge.jaulp.file.read.ReadFileUtils;

/**
 * The Class ChecksumUtils is a utility class for computing checksum from files
 * and byte arrays.
 *
 * @version 1.0
 * @author Asterios Raptis
 */
public class ChecksumUtils {

    /**
	 * The main method.
	 *
	 * @param args The args
	 * @throws FileNotFoundException Is thrown if the file is not found.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException Is thrown if the algorithm is not supported or does not
	 * exists.
	 */
    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (args.length != 1) {
            System.err.println("Usage: java ChecksumCRC32 filename");
        } else {
            File pom = new File(args[0]);
            checkWithFile(pom);
            long checksum = getChecksum(new File(args[0]), true);
            System.out.println("CRC32 checksum:" + checksum);
            checksum = getChecksum(new File(args[0]), false);
            System.out.println("Adler32 checksum:" + checksum);
            byte[] ba = ReadFileUtils.readFileToBytearray(pom);
            checkWithByteArray(ba);
        }
    }

    /**
	 * Gets the checksum from the given file. If the flag crc is true than the
	 * CheckedInputStream is constructed with an instance of
	 *
	 * @param file The file The file from what to get the checksum.
	 * @param crc The crc If the flag crc is true than the CheckedInputStream is
	 * constructed with an instance of {@link java.util.zip.CRC32}
	 * object otherwise it is constructed with an instance of
	 * @return The checksum from the given file as long.
	 * @throws FileNotFoundException Is thrown if the file is not found.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * {@link java.util.zip.CRC32} object otherwise it is constructed with an
	 * instance of {@link java.util.zip.Adler32} object.
	 * {@link java.util.zip.Adler32} object.
	 */
    public static long getChecksum(File file, boolean crc) throws FileNotFoundException, IOException {
        CheckedInputStream cis = null;
        if (crc) {
            cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
        } else {
            cis = new CheckedInputStream(new FileInputStream(file), new Adler32());
        }
        int length = (int) file.length();
        byte[] buffer = new byte[length];
        long checksum = 0;
        while (cis.read(buffer) >= 0) {
            checksum = cis.getChecksum().getValue();
        }
        checksum = cis.getChecksum().getValue();
        return checksum;
    }

    /**
	 * Gets the checksum from the given byte array with an instance of.
	 *
	 * @param bytes The byte array.
	 * @return The checksum from the byte array as long.
	 * {@link java.util.zip.Adler32} object.
	 */
    public static long getCheckSumAdler32(byte[] bytes) {
        Checksum checksum = new Adler32();
        checksum.update(bytes, 0, bytes.length);
        long cs = checksum.getValue();
        return cs;
    }

    /**
	 * Gets the checksum from the given file with an instance of.
	 *
	 * @param file The file.
	 * @return The checksum from the file as long.
	 * {@link java.util.zip.Adler32} object.
	 */
    public static long getCheckSumAdler32(File file) {
        return getCheckSumAdler32(ReadFileUtils.getFilecontentAsByteArray(file));
    }

    /**
	 * Gets the checksum from the given byte array with an instance of.
	 *
	 * @param bytes The byte array.
	 * @return The checksum from the byte array as long.
	 * {@link java.util.zip.CRC32} object.
	 */
    public static long getCheckSumCRC32(byte[] bytes) {
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        long cs = checksum.getValue();
        return cs;
    }

    /**
	 * Gets the checksum from the given file with an instance of.
	 *
	 * @param file The file.
	 * @return The checksum from the file as long.
	 * {@link java.util.zip.CRC32} object.
	 */
    public static long getCheckSumCRC32(File file) {
        return getCheckSumCRC32(ReadFileUtils.getFilecontentAsByteArray(file));
    }

    /**
	 * Gets the checksum from the given byte array with an instance of.
	 *
	 * @param bytes the byte array.
	 * @param algorithm the algorithm to get the checksum. This could be for instance
	 * "MD4", "MD5", "SHA-1", "SHA-256", "SHA-384" or "SHA-512".
	 * @return The checksum from the file as a String object.
	 * @throws NoSuchAlgorithmException Is thrown if the algorithm is not supported or does not
	 * exists.
	 * {@link java.security.MessageDigest} object.
	 */
    public static String getChecksum(byte[] bytes, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        messageDigest.update(bytes);
        byte digest[] = messageDigest.digest();
        StringBuilder hexView = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String intAsHex = Integer.toHexString(0xFF & digest[i]);
            if (intAsHex.length() == 1) {
                hexView.append('0');
            }
            hexView.append(intAsHex);
        }
        return hexView.toString();
    }

    /**
	 * Gets the checksum from the given file with an instance of.
	 *
	 * @param file the file.
	 * @param algorithm the algorithm to get the checksum. This could be for instance
	 * "MD4", "MD5", "SHA-1", "SHA-256", "SHA-384" or "SHA-512".
	 * @return The checksum from the file as a String object.
	 * @throws NoSuchAlgorithmException Is thrown if the algorithm is not supported or does not
	 * exists.
	 * {@link java.security.MessageDigest} object.
	 */
    public static String getChecksum(File file, String algorithm) throws NoSuchAlgorithmException {
        return getChecksum(ReadFileUtils.getFilecontentAsByteArray(file), algorithm);
    }

    /**
	 * Utility method for tests.
	 *
	 * @param pom
	 *            the pom
	 *
	 * @throws NoSuchAlgorithmException
	 *             Is thrown if the algorithm is not supported or does not
	 *             exists.
	 */
    private static void checkWithFile(File pom) throws NoSuchAlgorithmException {
        Algorithm[] algorithms = Algorithm.values();
        for (int i = 0; i < algorithms.length; i++) {
            try {
                String result = getChecksum(pom, algorithms[i].getAlgorithm());
                System.out.println("getChecksum from " + algorithms[i] + " algorithm:\t\t" + result);
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
	 * Utility method for tests.
	 *
	 * @param ba
	 *            the ba
	 *
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
    private static void checkWithByteArray(byte[] ba) throws NoSuchAlgorithmException {
        Algorithm[] algorithms = Algorithm.values();
        for (int i = 0; i < algorithms.length; i++) {
            try {
                String result = getChecksum(ba, algorithms[i].getAlgorithm());
                System.out.println("getChecksum from " + algorithms[i] + " algorithm:\t\t" + result);
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }
}
