package ch.busyboxes.agoo.dao.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import ch.busyboxes.agoo.dao.FileSystemDAO;

/**
 * Implementation of the file system dao
 * 
 * @author julien@busyboxes.ch
 */
public class FileSystemDAOImpl implements FileSystemDAO {

    /** Logger for this file */
    private Logger logger = Logger.getLogger(FileSystemDAOImpl.class);

    private static final int BUFFER_SIZE = 1024;

    /**
	 * @see FileSystemDAO#getFilesInPath(String)
	 */
    public List<String> getFilesInPath(String path) {
        List<String> result = new ArrayList<String>();
        if (logger.isDebugEnabled()) {
            logger.debug("Scanning: " + path + ", for files");
        }
        File folder = new File(path);
        if (folder.isDirectory()) {
            result = getNewFilesInPath(path, folder, result);
        } else {
            throw new RuntimeException("Path: " + path + ", is not a directory");
        }
        File tempFile;
        List<String> folders = new ArrayList<String>();
        for (String file : result) {
            tempFile = new File(file);
            if (tempFile.isDirectory()) {
                folders.add(file);
            }
        }
        result.removeAll(folders);
        if (logger.isDebugEnabled()) {
            logger.debug("Found: " + result.size() + " files in folder: " + path);
        }
        return result;
    }

    /**
	 * Looks for new files or folders in the give File
	 * 
	 * @param initialPath the prefix present in all paths
	 * @param file the file/folder to look at
	 * @param result the list of already found results
	 * @return the list of new file and folder found
	 */
    private List<String> getNewFilesInPath(String initialPath, File file, List<String> result) {
        if (!result.contains(file.getAbsolutePath())) {
            if (file.isDirectory()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found folder: " + file.toString() + ", scanning");
                }
                File[] subFiles = file.listFiles();
                for (File subFile : subFiles) {
                    result.addAll(getNewFilesInPath(initialPath, subFile, result));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found file: " + file.toString());
                }
                String absolutePath = file.getAbsolutePath();
                if (!result.contains(absolutePath)) {
                    if (absolutePath.startsWith(initialPath)) {
                        result.add(absolutePath.substring(initialPath.length(), absolutePath.length()));
                    } else {
                        result.add(absolutePath);
                    }
                }
            }
        }
        return result;
    }

    /**
	 * @see FileSystemDAO#computeMd5Hash(String)
	 */
    @Override
    public String computeMd5Hash(String path) {
        if (logger.isDebugEnabled()) {
            logger.debug("Computing md5 for file: " + path);
        }
        File file = new File(path);
        String result = null;
        InputStream fin = null;
        try {
            fin = new FileInputStream(file);
            MessageDigest md5er = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0) {
                    md5er.update(buffer, 0, read);
                }
            } while (read != -1);
            byte[] digest = md5er.digest();
            if (digest != null) {
                StringBuffer strDigest = new StringBuffer();
                for (int i = 0; i < digest.length; i++) {
                    strDigest.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
                }
                result = strDigest.toString();
            }
        } catch (Exception e) {
            logger.error("Error while computing md5 on file: " + path, e);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException ioe) {
                    logger.error("Error while closing stream on file: " + path, ioe);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("md5 hash is: " + result);
        }
        return result;
    }
}
