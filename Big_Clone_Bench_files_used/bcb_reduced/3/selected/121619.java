package base.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import java.util.zip.CRC32;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 * This class provides some basic methods to manage files and folders.
 * 
 * @author Guido Angelo Ingenito
 */
public class FileUtil {

    private static final transient Logger logger = Logger.getLogger(FileUtil.class.getName());

    /**
	 * This method computes the specified file occupied space on the HD. If the
	 * input file is a simple file will be returned its size otherwise if the
	 * file is a directory on local HD will be recursivelly computed the
	 * occupied space of subfolders and contained files.
	 * 
	 * @param file
	 *            The file or directory whose size must be computed.
	 * @return A Byte rappresentation of the file'size.
	 */
    public static long fileSizeInByte(File file) {
        long size = 0;
        if (file.isDirectory()) {
            File[] filelist = file.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                if (filelist[i].isDirectory()) {
                    size += fileSizeInByte(filelist[i]);
                } else {
                    size += filelist[i].length();
                }
            }
        } else size += file.length();
        return size;
    }

    /**
	 * This method computes the specified file occupied space on the HD. If the
	 * input file is a simple file will be returned its size otherwise if the
	 * file is a directory on local HD will be recursivelly computed the
	 * occupied space of subfolders and contained files.
	 * 
	 * @param file
	 *            The file or directory whose size must be computed.
	 * @return A Kylo Byte rappresentation of the file'size.
	 */
    public static long fileSizeInKB(File file) {
        return fileSizeInByte(file) / 1024;
    }

    /**
	 * This method computes the specified file occupied space on the HD. If the
	 * input file is a simple file will be returned its size otherwise if the
	 * file is a directory on local HD will be recursivelly computed the
	 * occupied space of subfolders and contained files.
	 * 
	 * @param file
	 *            The file or directory whose size must be computed.
	 * @return A Mega Byte rappresentation of the file'size.
	 */
    public static long fileSizeInMB(File file) {
        return fileSizeInByte(file) / 1048576;
    }

    /**
	 * This method computes the specified file occupied space on the HD. If the
	 * input file is a simple file will be returned its size otherwise if the
	 * file is a directory on local HD will be recursivelly computed the
	 * occupied space of subfolders and contained files.
	 * 
	 * @param file
	 *            The file or directory whose size must be computed.
	 * @return A Giga Byte rappresentation of the file'size.
	 */
    public static long fileSizeInGB(File file) {
        return fileSizeInByte(file) / 1073741824;
    }

    /**
	 * This method creates a CRC32 Checksum relative to the file passed as
	 * formal parameter.
	 * 
	 * @param file
	 *            The file whose checksum must be created.
	 * @return The value of the CRC32 checksum-
	 * @throws IOException
	 *             If can't be created the InputStream associated to the input
	 *             File.
	 */
    public static Long createCRC32Checksum(File file) throws IOException {
        logger.debug("createCRC32Checksum(" + file.toString() + ")");
        InputStream inputStream = new FileInputStream(file);
        CRC32 checksum = new CRC32();
        checksum.reset();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) >= 0) {
            checksum.update(buffer, 0, bytesRead);
        }
        inputStream.close();
        logger.debug("CRC32 Checksum value: " + checksum.getValue());
        return new Long(checksum.getValue());
    }

    /**
	 * This method checks if the computed CRC32 Checksum of the input file is
	 * equals to the provided crc32Checksum passed as formal argument.
	 * 
	 * @param file
	 *            The file whose checksum must be compared with the input
	 *            crc32Checksum.
	 * @param crc32Checksum
	 *            The comparation parameter.
	 * @return True if the computed checksum is queals to the crc32Checksum,
	 *         false otherwise.
	 * @throws IOException
	 *             If can't be created the InputStream associated to the input
	 *             File.
	 */
    public static boolean checkCRC32Checksum(File file, Long crc32Checksum) throws IOException {
        logger.debug("checkCRC32Checksum(" + file.toString() + "," + crc32Checksum + ")");
        Long checksum = createCRC32Checksum(file);
        return checksum.equals(crc32Checksum);
    }

    /**
	 * This method builds an array of files. In the case the passed formal
	 * parameter named "file" is a simple file it is returned otherwise if it is
	 * a directory all sub-files and sub-directories are navigated recursivelly
	 * and are added to the resultant array.
	 * 
	 * @param file
	 *            The file or folder from which the resultant array is built.
	 * @return An array of one file if the formal parameter named "file" is a
	 *         simple file a list of sub-files or sub-directories built
	 *         recursivelly if the formal parameter is a directory.
	 */
    public static File[] allFileContent(File file) {
        Vector<File> result = new Vector<File>();
        if (file.isDirectory()) {
            File[] filelist = file.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                if (filelist[i].isDirectory()) {
                    File[] content = allFileContent(filelist[i]);
                    for (int k = 0; k < content.length; k++) result.add(content[k]);
                } else {
                    result.add(filelist[i]);
                }
            }
        } else result.add(file);
        return result.toArray(new File[0]);
    }

    /**
	 * Deletes all files and subdirectories under directory.
	 * 
	 * @param directory
	 *            The directory that must be deleted.
	 * @return True if all deletions were successful. If a deletion fails, the
	 *         method stops attempting to delete and returns false.
	 */
    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++) {
                File toBeDeleted = new File(directory, children[i]);
                logger.debug("Deleting : " + toBeDeleted);
                boolean success = deleteDirectory(toBeDeleted);
                if (!success) {
                    return false;
                }
            }
        }
        return directory.delete();
    }

    /**
	 * This method builds an MD5 Digest of a provided input file.
	 * 
	 * @param file
	 *            The file whose MD5 Digest must be build.
	 * @return The files'MD5 Digest.
	 * @throws IOException
	 *             If the method can't use the filesystem or the provided file
	 *             doesn't exists.
	 */
    public static String createMD5Digest(File file) throws IOException {
        String result = "";
        InputStream inputStream = new FileInputStream(file);
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            inputStream.close();
            result = new String(Hex.encode(messageDigest.digest()));
            logger.debug("The MD5 Digest for: " + file + " is: " + result);
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /**
	 * This method checks if the computed MD5 Digest of the input file is equals
	 * to the provided md5Digest passed as formal argument.
	 * 
	 * @param file
	 *            The file whose md5 digest must be compared with the input
	 *            md5Digest.
	 * @param md5Digest
	 *            The comparation parameter.
	 * @return True if the computed md5 digest is queals to the md5Digest, false
	 *         otherwise.
	 * @throws IOException
	 *             If can't be created the InputStream associated to the input
	 *             File.
	 */
    public static boolean checkMD5Digest(File file, String md5Digest) throws IOException {
        logger.debug("checkMD5Digest(" + file.toString() + "," + md5Digest + ")");
        String digest = createMD5Digest(file);
        return digest.equals(md5Digest);
    }

    /**
	 * This method reads all the lines of the input file returning its content
	 * in form of a String.
	 * 
	 * @param file
	 *            The file whose content must be retrieved.
	 * @return The content of the input file.
	 * @throws IOException
	 *             If can't work on the filesystem.
	 */
    public static String fileContent(File file) throws IOException {
        StringBuffer result = new StringBuffer();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int numRead = 0;
        while ((numRead = inputStream.read(buffer)) >= 0) {
            result.append(new String(buffer).toCharArray(), 0, numRead);
        }
        return result.toString();
    }

    public static void copyFile(File input, File output) throws IOException {
        FileInputStream inputStream = new FileInputStream(input);
        FileOutputStream outputStream = new FileOutputStream(output);
        byte[] buffer = new byte[1024];
        int numRead = 0;
        while ((numRead = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, numRead);
        }
        inputStream.close();
        outputStream.close();
    }
}
