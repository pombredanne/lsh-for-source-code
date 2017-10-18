package org.ourgrid.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.apache.commons.io.FileSystemUtils;
import org.ourgrid.common.exception.UnableToDigestFileException;
import org.ourgrid.common.executor.Win32Executor;
import org.ourgrid.worker.WorkerConfiguration;
import sun.misc.BASE64Encoder;

/**
 * This is a java file utils class, it has method to manipulate java file names.
 */
public class JavaFileUtil {

    /** This represents the class file name extension */
    private static final String CLASS_SUFFIX = ".class";

    /** This represents the java source file name extension */
    private static final String JAVA_SUFFIX = ".java";

    public static void writeToFile(String string, String path) throws IOException {
        File f = new File(path);
        FileWriter writer = new FileWriter(f, true);
        writer.write(string + System.getProperty("newLine"));
        writer.close();
    }

    /**
	 * This method extract the extension of the Java Source file name
	 * 
	 * @param namePlusSufix The coplete name to the Java Source File
	 * @param suffix The substring that should be extracted
	 * @return The file name without suffix parameter.
	 */
    public static String extractJavaSuffix(String namePlusSufix, String suffix) {
        int sufixo_inicio = namePlusSufix.indexOf(suffix);
        if (sufixo_inicio != -1) {
            namePlusSufix = namePlusSufix.substring(0, sufixo_inicio);
        }
        return namePlusSufix;
    }

    /**
	 * This method determine the complete name of a class
	 * 
	 * @param file The file abstraction that denotes a java class file
	 * @param root The root directory where the class is located
	 * @return The complete name of a class
	 */
    public static String getFullClassName(File file, String root) {
        String path = file.getAbsolutePath();
        int inicial = path.indexOf(root);
        inicial += root.length();
        inicial += 1;
        String pathPlusClassName = path.substring(inicial, path.length());
        String classFullName = JavaFileUtil.extractJavaSuffix(pathPlusClassName, JavaFileUtil.CLASS_SUFFIX);
        classFullName = JavaFileUtil.extractJavaSuffix(classFullName, JavaFileUtil.JAVA_SUFFIX);
        classFullName = classFullName.replace(File.separatorChar, '.');
        return classFullName;
    }

    /**
	 * That utility method get a File object in applying a Message Digest
	 * Filter, the result is a digest string representation of the file contents
	 * 
	 * @param fileToDigest The File object abstraction that denotes a file to be
	 *        digested
	 * @return The digest string representation of the file contents. Or null if
	 *         some exception occurs,
	 * @throws UnableToDigestFileException If there is any problem on the digest
	 *         generation, like the file is not found, I/O errors or the digest
	 *         algorithm is not valid.
	 */
    public static String getDigestRepresentation(File fileToDigest) throws UnableToDigestFileException {
        MessageDigest messageDigest;
        FileInputStream inputStream = null;
        byte[] buffer = new byte[8129];
        int numberOfBytes;
        byte[] digestValue;
        BASE64Encoder encoder;
        String fileHash = new String();
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            inputStream = new FileInputStream(fileToDigest.getAbsoluteFile());
            numberOfBytes = inputStream.read(buffer);
            while (numberOfBytes != -1) {
                messageDigest.update(buffer, 0, numberOfBytes);
                numberOfBytes = inputStream.read(buffer);
            }
            digestValue = messageDigest.digest();
            encoder = new BASE64Encoder();
            fileHash = encoder.encode(digestValue);
        } catch (IOException exception) {
            throw new UnableToDigestFileException(fileToDigest.getAbsolutePath(), exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new UnableToDigestFileException(fileToDigest.getAbsolutePath(), exception);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return fileHash;
    }

    /**
	 * Tells if a given filepath represents an absolute path or not.
	 * 
	 * @param filepath The file path.
	 * @return True if the given filepath represents an absolute file path,
	 *         false otherwise.
	 */
    public static boolean isAbsolutePath(String filepath) {
        if ((filepath.indexOf(":\\") != -1) || filepath.charAt(0) == '\\') {
            return true;
        }
        return (new File(filepath)).isAbsolute();
    }

    /**
	 * Makes translations that depend on the operating system being used.
	 * 
	 * @param filepath The file path.
	 * @param gumAttributesMap A map of attributes from where we retrieve the
	 *        ATT_OS attribute. This attribute represents a constant indicating
	 *        the Operating System according to <code>GuMSpec</code>
	 *        constants.
	 * @return The translated filepath.
	 */
    public static String getTranslatedFilePath(String filepath, Map gumAttributesMap) {
        String os = (String) gumAttributesMap.get(WorkerConfiguration.ATT_OS);
        if (os != null && os.equals(WorkerConfiguration.OS_WINDOWS)) {
            return (Win32Executor.convert2WinStyle(filepath));
        }
        return filepath;
    }

    /**
	 * Deletes a directory and all of its contents recursively.
	 * 
	 * @param dir Directory to delete.
	 * @return True if directory was deleted.
	 */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory() && dir.exists()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
	 * Deletes a directory and all of its contents recursively.
	 * 
	 * @param mainDir Directory to delete.
	 * @return True if directory was deleted.
	 */
    public static boolean deleteDir(String string) {
        return deleteDir(new File(string));
    }

    /**
	 * Algorithm used to copy files.
	 * 
	 * @param sourceFile Source file.
	 * @param destFile Destination file.
	 * @throws IOException In case an exception occurs while copying.
	 */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
	 * @param srcPath
	 * @param dstPath
	 * @throws IOException
	 */
    public static void copyDirectory(String srcPath, String dstPath) throws IOException {
        copyDirectory(new File(srcPath), new File(dstPath));
    }

    /**
	 * TODO: REFACTORING TO N.I.O
	 * @param srcPath
	 * @param dstPath
	 * @throws IOException
	 */
    public static void copyDirectory(File srcPath, File dstPath) throws IOException {
        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {
            if (!srcPath.exists()) {
                throw new IllegalArgumentException("File or directory does not exist.");
            } else {
                InputStream in = new FileInputStream(srcPath);
                OutputStream out = new FileOutputStream(dstPath);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
    }

    public static boolean setWritable(File file) {
        try {
            new FilePermission(file.getAbsolutePath(), "write");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setReadAndWrite(File file) {
        return file.setReadable(true, false) && file.setWritable(true, false);
    }

    public static boolean setNonReadable(File file) {
        return file.setReadable(false, false);
    }

    public static boolean setReadable(File file) {
        try {
            new FilePermission(file.getAbsolutePath(), "read");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setExecutable(File file) {
        try {
            new FilePermission(file.getAbsolutePath(), "execute");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Algorithm used to copy files.
	 * 
	 * @param sourceFile Source file.
	 * @param destFile Destination file.
	 * @throws IOException In case an exception occurs while copying.
	 */
    public static void copyFile(String sourceFile, String destFile) throws IOException {
        copyFile(new File(sourceFile), new File(destFile));
    }
}
