package tool.dtf4j.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Various utilities mostly related to I/O
 */
public class Utils {

    /** The logger */
    private static Logger logger_ = Logger.getLogger("DTF4JLogger");

    /** Used to generate a unique identification */
    private static SecureRandom prng;

    /** Used to generate a unique identification */
    private static MessageDigest sha;

    /** The file separator */
    private static String mySep_ = System.getProperty("file.separator");

    private static String otherSep_ = mySep_.equals("/") ? "\\\\" : "/";

    static {
        try {
            prng = SecureRandom.getInstance("SHA1PRNG");
            sha = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new RuntimeException("Could not created components needed for unique id generation!");
        }
    }

    /**
	 * Localizes the provided path and returns a localized version. Here
	 * localize means that it conforms to the caller's operating system.
	 * @param path the path to be localized
	 * @return a localized version of the provided path
	 */
    public static String localize(String path) {
        if (path.indexOf("://") != -1) {
            return path;
        }
        try {
            return path.replaceAll(otherSep_, mySep_);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("localize: got a StringIndexOutOfBoundsException: " + e);
            System.err.println("What's that all about?");
            System.err.println("path=" + path);
            System.err.println("otherSep_=" + otherSep_);
            System.err.println("mySep_=" + mySep_);
            System.err.println("Returning path instead");
            return path;
        }
    }

    /**
     * This methods will unzip all the files contained in zipFileName, placing them in a position
     * offset with directory outputDirectory.
     * @param zipFileName the name of the zip file we want to extract.
     * @param outputDirectory the name of the directory in which to extract the files.
     * @throws IOException thrown if the file could not be unzipped, or we cannot write in the output directory.
     */
    public static synchronized void unzipFiles(String zipFileName, String outputDirectory) throws IOException {
        ZipInputStream zipFile = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFileName)));
        ZipEntry entry;
        while ((entry = zipFile.getNextEntry()) != null) {
            try {
                createFile(entry, zipFile, outputDirectory);
                zipFile.closeEntry();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        zipFile.close();
    }

    /**
     * This method deletes all the files and subdirectories as created when 
	 * the zipFileName was unzipped in the outputDirectory.
     */
    public static void cleanUnzippedFiles(String zipFileName, String outputDirectory) throws IOException {
        FileInputStream inStream;
        ZipInputStream zipFile = new ZipInputStream(inStream = new FileInputStream(zipFileName));
        ZipEntry entry;
        File f;
        while ((entry = zipFile.getNextEntry()) != null) {
            f = new File(outputDirectory + File.separator + entry.getName());
            if (f.exists()) {
                Utils.delete(f);
            }
            zipFile.closeEntry();
        }
        zipFile.close();
        inStream.close();
    }

    /**
     * This method creates a file specified by fileName, if it doesn't 
	 * already exist. If the parent directories don't exist, they will 
	 * be created automatically.
     * @param fileName the name of the file to create.
     * @throws IOException thrown if any IOException occur due to permission problems etc.
     */
    public static void touch(String fileName) throws IOException {
        checkSubDirsExist(fileName);
        RandomAccessFile f = new RandomAccessFile(new File(fileName), "rw");
        f.close();
    }

    /**
     * This method creates a dir specified by dirName, if it doesn't 
	 * already exist. If the parent directories don't exist, they will 
	 * be created automatically.
     * @param dirName the name of the dir to create.
     * @throws IOException thrown if any IOException occur due to permission problems etc.
     */
    public static void touchDir(String dirName) throws IOException {
        final File file = new File(dirName);
        if (file.exists() && file.isDirectory()) {
            return;
        } else if (file.exists()) throw new IOException("Could not create directory as file with that name already exists");
        if (!(new File(dirName).mkdirs())) throw new IOException("Could not create directory.");
    }

    /**
     * This method deletes the file specified by fileName.
     * If it is a directory, all subdirectories are also deleted.
     * @param fileName the name of the file or directory to be deleted.
     * @throws IOException thrown if the file or directory could not be deleted.
     */
    public static void delete(String fileName) throws IOException {
        delete(new File(fileName));
    }

    /**
     * This method deletes the file specified by fileName.
     * If it is a directory, all subdirectories are also deleted.
     * @param node the File identifier of the file or directory to be deleted.
     * @throws IOException thrown if the file or directory could not be deleted.
     */
    public static void delete(File node) throws IOException {
        if (!node.exists()) return;
        if (node.isDirectory()) {
            String[] files = node.list();
            for (int i = 0; i < files.length; i++) {
                delete(new File(node.getCanonicalPath() + File.separator + files[i]));
            }
            if (!node.delete()) {
                throw new IOException("Could not delete the directory :" + node + "- write protected or not empty");
            }
            System.gc();
        } else {
            if (!node.delete()) throw new IOException("Could not delete the file :" + node + " - may be write protected");
            System.gc();
        }
    }

    /**
     * Creates all necessary parent directories contained in this path.
     * Not to be confused with  checkSubDirsExist(String rootPath, String path) which
     * is used exclusively by the unzip routines, since java zip files always
     * store directories separated by the '/' instead of File.separator char.
     * @param path the directory path to be created.
     * @throws IOException if the directory path could not be created.
     */
    public static synchronized void checkSubDirsExist(String path) throws IOException {
        StringTokenizer token = new StringTokenizer(path, File.separator);
        String dirComponents[] = new String[token.countTokens()];
        String root = (path.charAt(0) != File.separator.charAt(0) ? token.nextToken() : File.separator + token.nextToken());
        File f;
        for (int i = 1; i < dirComponents.length - 1; i++) {
            root = root + File.separator + token.nextToken();
            f = new File(root);
            if (!f.exists()) if (!f.mkdir()) {
                logger_.log(Level.SEVERE, "Error creating directory:" + root);
                throw new IOException("Error creating directory:" + root);
            }
        }
    }

    /**
     * Creates all necessary parent directories contained in this path.
     * This method is used exclusively by the unzip routines, since java 
     * zip files always store directories separated by the '/' instead 
     * of File.separator char.
     * @param rootPath
     * @param path
     * @throws IOException if the directory path could not be created.
     */
    private static synchronized void checkSubDirsExist(String rootPath, String path) throws IOException {
        StringTokenizer token = new StringTokenizer(path, "/");
        String dirComponents[] = new String[token.countTokens()];
        String root = rootPath;
        File f;
        for (int i = 0; i < dirComponents.length - 1; i++) {
            root = root + File.separator + token.nextToken();
            f = new File(root);
            if (!f.exists()) if (!f.mkdir()) {
                logger_.log(Level.SEVERE, "Error creating directory:" + root);
                throw new IOException("Error creating directory:" + root);
            }
        }
    }

    /**
     * @param file
     * @param source
     * @param outputDir
     * @throws IOException if the file could not be created
     */
    private static synchronized void createFile(ZipEntry file, ZipInputStream source, String outputDir) throws IOException {
        if ((file.isDirectory()) || (file.getName().charAt(file.getName().length() - 1) == '/')) {
            checkSubDirsExist(outputDir, file.getName());
            (new File(outputDir + File.separator + file.getName())).mkdir();
        } else {
            checkSubDirsExist(outputDir, file.getName());
            final File destFile = new File(outputDir, file.getName());
            Utils.touch(destFile.getAbsolutePath());
            int bytesRead;
            byte buff[] = new byte[4096];
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(destFile));
            while ((bytesRead = source.read(buff, 0, buff.length)) >= 0) {
                output.write(buff, 0, bytesRead);
            }
            output.close();
        }
    }

    /**
     * Returns a unique identification as a <code>String</code> object.
     */
    public static String getUniqueID() {
        String randomNum = new Integer(prng.nextInt()).toString();
        byte[] result = sha.digest(randomNum.getBytes());
        return new String(result);
    }

    /**
     * Causes for a temporary file to be created.
     * @param  prefix the prefix string to be used in generating the file's
     * name; must be at least three characters long
     * @param  suffix the suffix string to be used in generating the file's
     * name; may be <code>null</code>, in which case the
     * suffix <code>".tmp"</code> will be used 
     * @return the temporary file created.
     * @throws IOException if the temporary file could not be created.
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        File result = null;
        try {
            result = File.createTempFile(prefix, suffix);
            result.deleteOnExit();
        } catch (IOException ex) {
            logger_.log(Level.WARNING, "Error creating temporary file", ex);
            throw ex;
        }
        return result;
    }
}
