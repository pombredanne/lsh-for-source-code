package au.gov.naa.digipres.dpr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import au.gov.naa.digipres.dpr.core.Constants;

public class FileUtils {

    public static interface FileCopierListener {

        public void fileCopied(String filename);

        public void dirCopied(String dirName);
    }

    /**
	 * Constructor!
	 *
	 */
    public FileUtils() {
    }

    private List<FileCopierListener> listeners = new ArrayList<FileCopierListener>();

    /**
	 * Add a listener to the file Utils. The listener will recieve notification of copy events.
	 * @param listener
	 */
    public void addListener(FileCopierListener listener) {
        listeners.add(listener);
    }

    /**
	 * Remove a listener.
	 * @param listener
	 */
    public void removeListener(FileCopierListener listener) {
        listeners.remove(listener);
    }

    /**
	 * TreeCopy
	 * Copy directory tree from src dir to dst dir.
	 * Overwrites files that already exist,
	 * any extra files already in destination are left there
	 * 
	 * Also notify any listeners listening :)
	 * 
	 * @param File src - source dir to copy.
	 * @param File dst - destination dir
	 * @throws IOException - thrown while trying to make dirs or read dirs / files
	 * @throws FileCopierException - thrown if there is some other error during the tree copy.
	 */
    public void performCopy(File sourceFolder, File destinationFolder) throws IOException, FileCopierException {
        File[] folderEntries;
        File destination;
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            throw new FileCopierException("Source directory is not valid: " + sourceFolder.getAbsolutePath(), FileCopierException.INVALID_SOURCE_DIR);
        }
        if (destinationFolder.isFile()) {
            throw new FileCopierException("Destination is a file", FileCopierException.DESTINATION_IS_FILE);
        }
        if (!destinationFolder.exists()) {
            if (!destinationFolder.mkdirs()) {
                throw new FileCopierException("Unable to make destination folder!", FileCopierException.UNABLE_TO_CREATE_DESTINATION);
            }
        }
        folderEntries = sourceFolder.listFiles();
        for (File folderEntry : folderEntries) {
            destination = new File(destinationFolder.getAbsolutePath() + File.separator + folderEntry.getName());
            if (folderEntry.isFile()) {
                fileCopy(folderEntry, destination);
                for (FileCopierListener listener : listeners) {
                    listener.fileCopied(destinationFolder.getName());
                }
            } else {
                performCopy(folderEntry, destination);
            }
        }
        for (FileCopierListener listener : listeners) {
            listener.dirCopied(destinationFolder.getName());
        }
    }

    /**
	 * TreeCount
	 * Count files in directory tree
	 * DO NOT COUNT DIRECTORIES!
	 * @param File dir - base dir to count files of.
	 */
    public static int TreeCount(File dir) {
        int count = 0;
        File[] aDirs;
        if (dir.isFile()) {
            return 1;
        }
        aDirs = dir.listFiles();
        for (File dir2 : aDirs) {
            count = count + TreeCount(dir2);
        }
        return count;
    }

    /**
	 * TreeDirs
	 * Count sub directories in directory tree
	 * @param File dir - base dir to count sub folders of.
	 */
    public static int TreeDirs(File dir) {
        int count = 0;
        File[] aDirs;
        if (dir.isFile()) {
            return 0;
        }
        aDirs = dir.listFiles();
        for (File dir2 : aDirs) {
            if (dir2.isDirectory()) {
                count++;
                count = count + TreeDirs(dir2);
            }
        }
        return count;
    }

    /**
	 * TreeSize
	 * Get size of directory tree
	 * @param File dir - base dir to get size of.
	 */
    public static long TreeSize(File dir) {
        long size = 0;
        File[] aDirs;
        if (dir.isFile()) {
            return dir.length();
        }
        aDirs = dir.listFiles();
        for (File dir2 : aDirs) {
            size = size + TreeSize(dir2);
        }
        return size;
    }

    /**
	 * TreeDel
	 * Delete the directory tree
	 * @param File dir - dir to remove.
	 */
    public static int TreeDel(File dir) {
        int count = 0;
        File[] aDirs;
        if (dir.isFile()) {
            count = 1;
        } else {
            aDirs = dir.listFiles();
            for (File dir2 : aDirs) {
                count = count + TreeDel(dir2);
            }
        }
        dir.delete();
        return count;
    }

    /**
	 * Copy sourceFile to destFile. Throws exceptions if there are problems.
	 * 
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 * @throws FileCopierException
	 */
    public static void fileCopy(File sourceFile, File destFile) throws IOException {
        destFile.getParentFile().mkdirs();
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destFile);
            byte[] buf = new byte[64 * 1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        destFile.setLastModified(sourceFile.lastModified());
    }

    /**
	 * Delete directory and its contents.
	 * @param directory
	 */
    public static void deleteDirAndContents(File directory) {
        if (directory != null && directory.exists()) {
            if (directory.isDirectory()) {
                deleteContentsOfDir(directory);
            }
            directory.delete();
        }
    }

    public static void deleteContentsOfDir(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] dirFiles = directory.listFiles();
            for (File dirFile : dirFiles) {
                if (dirFile.isDirectory()) {
                    deleteDirAndContents(dirFile);
                } else {
                    dirFile.delete();
                }
            }
        }
    }

    /**
	 * Returns the full, recursive list of filenames contained within
	 * the given directory
	 * @param directory
	 * @return
	 */
    public static List<String> getDirectoryContents(File directory) {
        List<String> filenameList = new ArrayList<String>();
        addChildrenToList(directory, filenameList);
        return filenameList;
    }

    private static void addChildrenToList(File baseFile, List<String> filenameList) {
        if (baseFile.isDirectory()) {
            File[] childArr = baseFile.listFiles();
            if (childArr != null) {
                for (File element : childArr) {
                    if (element.isFile()) {
                        filenameList.add(element.getAbsolutePath());
                    } else {
                        addChildrenToList(element, filenameList);
                    }
                }
            }
        }
    }

    /**
	 * Get the checksum for this file using the algorithm provided. 
	 * Checksum is returned as a hex String. 
	 * @param file
	 * @param algorithm
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
    public static String getChecksum(File file, String algorithm) throws IOException {
        if (algorithm.equals(Constants.DUMMY_CHECKSUM_ALGORITHM)) {
            if (file.exists() && file.isFile()) {
                return Constants.DUMMY_CHECKSUM_VALUE;
            }
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        FileInputStream stream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        MessageDigest checksum;
        try {
            checksum = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Given algorithm is not currently supported.", e);
        }
        int numRead;
        do {
            numRead = stream.read(buffer);
            if (numRead > 0) {
                checksum.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        stream.close();
        return convertToHex(checksum.digest());
    }

    /**
	 * 
	 * Converts byte array to printable hexadecimal string.
	 *  eg convert created checksum to file form.
	 * @param byteArray
	 * @return String representing byte array.
	 */
    private static String convertToHex(byte[] byteArray) {
        String s;
        String hexString = "";
        for (byte element : byteArray) {
            s = Integer.toHexString(element & 0xFF);
            if (s.length() == 1) {
                s = "0" + s;
            }
            hexString = hexString + s;
        }
        return hexString;
    }

    /**
	 * Modify the given path so that it is a valid relative path on the current file system.
	 * The path may need a leading slash removed, and may need to have the path separator changed.
	 * @param path
	 * @return
	 */
    public static String fixRelativePath(String path) {
        String modifiedPath = path;
        if (modifiedPath.startsWith("/") || modifiedPath.startsWith("\\")) {
            modifiedPath = modifiedPath.substring(1);
        }
        if (File.separatorChar == '\\') {
            modifiedPath = modifiedPath.replace('/', '\\');
        } else {
            modifiedPath = modifiedPath.replace('\\', '/');
        }
        return modifiedPath;
    }
}
