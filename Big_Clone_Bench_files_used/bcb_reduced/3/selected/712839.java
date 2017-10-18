package org.nightlabs.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * This is a collection of utility functions. All methods
 * of this class are static.
 * @author Alex Bieber, Marc Klinger, Marco Schulze, Niklas Schiffler
 * @version 1.3 ;-)
 */
public abstract class Utils {

    /**
	 * UTF-8 caracter set name.
	 */
    public static final String CHARSET_NAME_UTF_8 = "UTF-8";

    /**
	 * UTF-8 caracter set.
	 */
    public static final Charset CHARSET_UTF_8 = Charset.forName(CHARSET_NAME_UTF_8);

    /**
	 * 1 GB in bytes.
	 * This holds the result of the calculation 1 * 1024 * 1024 * 1024
	 */
    public static final long GIGABYTE = 1 * 1024 * 1024 * 1024;

    /**
	 * Add a trailing file separator character to the
	 * given directory name if it does not already
	 * end with one.
	 * @see File#separator
	 * @param directory A directory name
	 * @return the directory name anding with a file seperator
	 */
    public static String addFinalSlash(String directory) {
        if (directory.endsWith(File.separator)) return directory; else return directory + File.separator;
    }

    /**
	 * @deprecated Use String.format("%0{minDigits}d", i) instead
	 */
    public static String int2StringMinDigits(int i, int minDigits) {
        String s = Integer.toString(i);
        if (s.length() < minDigits) {
            StringBuffer sBuf = new StringBuffer(s);
            while (sBuf.length() < minDigits) sBuf.insert(0, '0');
            s = sBuf.toString();
        }
        return s;
    }

    /**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * See {@link #getRelativePath(File, String)} for examples.
	 * 
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
    public static String getRelativePath(String baseDir, String file) throws IOException {
        return getRelativePath(new File(baseDir), file);
    }

    /**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * See {@link #getRelativePath(File, String)} for examples.
	 * 
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
    public static String getRelativePath(File baseDir, File file) throws IOException {
        return getRelativePath(baseDir, file.getPath());
    }

    /**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>
	 *   <code>baseDir="/home/marco"</code><br/>
	 *   <code>file="temp/jfire/jboss/bin/run.sh"</code><br/>
	 *     or<br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="temp/jfire/jboss/bin/run.sh"</code>
	 * </li>
	 * <li>
	 *   <code>baseDir="/home/marco/workspace.jfire/JFireBase"</code><br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *     or<br/>
	 *   <code>file="../../temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="../../temp/jfire/jboss/bin/run.sh"</code>
	 * </li>
	 * <li>
	 *   <code>baseDir="/tmp/workspace.jfire/JFireBase"</code><br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="/home/marco/temp/jfire/jboss/bin/run.sh"</code> (absolute, because relative is not possible)
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
    public static String getRelativePath(File baseDir, String file) throws IOException {
        File absFile;
        File tmpF = new File(file);
        if (tmpF.isAbsolute()) absFile = tmpF; else absFile = new File(baseDir, file);
        File dest = absFile;
        File b = baseDir;
        String up = "";
        while (b.getParentFile() != null) {
            String res = _getRelativePath(b, dest.getAbsolutePath());
            if (res != null) return up + res;
            up = "../" + up;
            b = b.getParentFile();
        }
        return absFile.getAbsolutePath();
    }

    /**
	 * This method is a helper method used by {@link #getRelativePath(File, String) }. It will
	 * only return a relative path, if <code>file</code> is a child node of <code>baseDir</code>.
	 * Otherwise it returns <code>null</code>.
	 *
	 * @param baseDir The directory to which the resulting path will be relative.
	 * @param file The file to which the resulting path will point.
	 */
    private static String _getRelativePath(File baseDir, String file) throws IOException {
        if (!baseDir.isAbsolute()) throw new IllegalArgumentException("baseDir \"" + baseDir.getPath() + "\" is not absolute!");
        File absFile;
        File tmpF = new File(file);
        if (tmpF.isAbsolute()) absFile = tmpF; else absFile = new File(baseDir, file);
        String absFileStr = null;
        String baseDirStr = null;
        for (int mode_base = 0; mode_base < 2; ++mode_base) {
            switch(mode_base) {
                case 0:
                    baseDirStr = simplifyPath(baseDir);
                    break;
                case 1:
                    baseDirStr = baseDir.getCanonicalPath();
                    break;
                default:
                    throw new IllegalStateException("this should never happen!");
            }
            for (int mode_abs = 0; mode_abs < 2; ++mode_abs) {
                baseDirStr = addFinalSlash(baseDirStr);
                switch(mode_abs) {
                    case 0:
                        absFileStr = simplifyPath(absFile);
                        break;
                    case 1:
                        absFileStr = absFile.getCanonicalPath();
                        break;
                    default:
                        throw new IllegalStateException("this should never happen!");
                }
                if (!absFileStr.startsWith(baseDirStr)) {
                    if (mode_base >= 1 && mode_abs >= 1) return null;
                } else break;
            }
        }
        if (baseDirStr == null) throw new NullPointerException("baseDirStr == null");
        if (absFileStr == null) throw new NullPointerException("absFileStr == null");
        return absFileStr.substring(baseDirStr.length(), absFileStr.length());
    }

    /**
	 * This method removes double slashes, single-dot-directories and double-dot-directories
	 * from a path. This means, it does nearly the same as <code>File.getCanonicalPath</code>, but
	 * it does not resolve symlinks. This is essential for the method <code>getRelativePath</code>,
	 * because this method first tries it with a simplified path before using the canonical path
	 * (prevents problems with iteration through directories, where there are symlinks).
	 * <p>
	 * Please note that this method makes the given path absolute!
	 *
	 * @param path A path to simplify, e.g. "/../opt/java/jboss/../jboss/./bin/././../server/default/lib/."
	 * @return the simplified string (absolute path), e.g. "/opt/java/jboss/server/default/lib"
	 */
    public static String simplifyPath(File path) {
        LinkedList<String> dirs = new LinkedList<String>();
        String pathStr = path.getAbsolutePath();
        boolean startWithSeparator = pathStr.startsWith(File.separator);
        StringTokenizer tk = new StringTokenizer(pathStr, File.separator, false);
        while (tk.hasMoreTokens()) {
            String dir = tk.nextToken();
            if (".".equals(dir)) ; else if ("..".equals(dir)) {
                if (!dirs.isEmpty()) dirs.removeLast();
            } else dirs.addLast(dir);
        }
        StringBuffer sb = new StringBuffer();
        for (String dir : dirs) {
            if (startWithSeparator || sb.length() > 0) sb.append(File.separator);
            sb.append(dir);
        }
        return sb.toString();
    }

    /**
	 * Transfer all available data from an {@link InputStream} to an {@link OutputStream}.
	 * <p>
	 * This is a convenience method for <code>transferStreamData(in, out, 0, -1)</code>
	 * @param in The stream to read from
	 * @param out The stream to write to
	 * @return The number of bytes transferred
	 * @throws IOException In case of an error
	 */
    public static long transferStreamData(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException {
        return transferStreamData(in, out, 0, -1);
    }

    /**
	 * Check two objects for equality.
	 * <p>
	 * This method is a convenience reducing code:
	 * <code>obj0 == obj1 || (obj0 != null && obj0.equals(obj1))</code>
	 * will be replaced by <code>Utils.equals(obj0, obj1)</code>
	 * and you do not need to worry about <code>null</code> anymore.
	 * <p>
	 * Additionally if you pass two arrays to this method 
	 * (whose equals method only checks for equality)
	 * this method will consult {@link Arrays#equals(Object[], Object[])}
	 * for equality of the parameters instead, of course after a <code>null</code> check.
	 * 
	 * @param obj0 One object to check for equality
	 * @param obj1 The other object to check for equality
	 * @return <code>true</code> if both objects are <code>null</code> or
	 * 		if they are equal or if both objects are Object arrays
	 *    and equal according to {@link Arrays#equals(Object[], Object[])} - 
	 *    <code>false</code> otherwise 
	 */
    public static boolean equals(Object obj0, Object obj1) {
        if (obj0 instanceof Object[] && obj1 instanceof Object[]) return obj0 == obj1 || Arrays.equals((Object[]) obj0, (Object[]) obj1);
        return obj0 == obj1 || (obj0 != null && obj0.equals(obj1));
    }

    /**
	 * @param l The long number for which to calculate the hashcode.
	 * @return the same as new Long(l).hashCode() would do, but 
	 * 		without the overhead of creating a Long instance.
	 */
    public static int hashCode(long l) {
        return (int) (l ^ (l >>> 32));
    }

    /**
	 * Get a hash code for an object. This method also handles 
	 * <code>null</code>-Objects.
	 * @param obj An object or <code>null</code>
	 * @return <code>0</code> if <code>obj == null</code> - 
	 * 		<code>obj.hashCode()</code> otherwise 
	 */
    public static int hashCode(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    /**
	 * This method deletes the given directory recursively. If the given parameter
	 * specifies a file and no directory, it will be deleted anyway. If one or more
	 * files or subdirectories cannot be deleted, the method still continues and tries
	 * to delete as many files/subdirectories as possible.
	 * 
	 * @param dir The directory or file to delete
	 * @return True, if the file or directory does not exist anymore. This means it either
	 * was not existing already before or it has been successfully deleted. False, if the
	 * directory could not be deleted.
	 */
    public static boolean deleteDirectoryRecursively(String dir) {
        File dirF = new File(dir);
        return deleteDirectoryRecursively(dirF);
    }

    /**
	 * This method deletes the given directory recursively. If the given parameter
	 * specifies a file and no directory, it will be deleted anyway. If one or more
	 * files or subdirectories cannot be deleted, the method still continues and tries
	 * to delete as many files/subdirectories as possible.
	 *
	 * @param dir The directory or file to delete
	 * @return <code>true</code> if the file or directory does not exist anymore. 
	 * 		This means it either was not existing already before or it has been 
	 * 		successfully deleted. <code>false</code> if the directory could not be 
	 * 		deleted.
	 */
    public static boolean deleteDirectoryRecursively(File dir) {
        if (!dir.exists()) return true;
        if (dir.isDirectory()) {
            File[] content = dir.listFiles();
            for (int i = 0; i < content.length; ++i) {
                File f = content[i];
                if (f.isDirectory()) deleteDirectoryRecursively(f); else try {
                    f.delete();
                } catch (SecurityException e) {
                }
            }
        }
        try {
            return dir.delete();
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
	 * Tries to find a unique, not existing folder name under the given root folder with a random
	 * number (in hex format) added to the given prefix. When found, the directory will be created.
	 * <p>
	 * This is a convenience method for {@link #createUniqueRandomFolder(File, String, long, long)}
	 * and calls it with 10000 as maxIterations and 10000 as number range.
	 * <p>
	 * Note that this method might throw a {@link IOException}
	 * if it will not find a unique name within 10000 iterations.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.  
	 * 
	 * @param rootFolder The rootFolder to find a unique subfolder for
	 * @param prefix A prefix for the folder that has to be found.
	 * @return A File pointing to an unique (non-existing) Folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
    public static File createUniqueRandomFolder(File rootFolder, final String prefix) throws IOException {
        return createUniqueRandomFolder(rootFolder, prefix, 10000, 10000);
    }

    /**
	 * Tries to find a unique, not existing folder name under the given root folder with a random
	 * number (in hex format) added to the given prefix. When found, the directory will be created.
	 * <p>
	 * The method will try to find a name for <code>maxIterations</code> number
	 * of itarations and use random numbers from 0 to <code>uniqueOutOf</code>.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.  
	 *  
	 * @param rootFolder The rootFolder to find a unique subfolder for 
	 * @param prefix A prefix for the folder that has to be found.
	 * @param maxIterations The maximum number of itarations this method shoud do. 
	 * 		If after them still no unique folder could be found, a {@link IOException}
	 * 		is thrown. 
	 * @param uniqueOutOf The range of random numbers to apply (0 - given value)
	 * 
	 * @return A File pointing to an unique folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
    public static synchronized File createUniqueRandomFolder(File rootFolder, final String prefix, long maxIterations, long uniqueOutOf) throws IOException {
        long count = 0;
        while (++count <= maxIterations) {
            File f = new File(rootFolder, String.format("%s%x", prefix, (long) (Math.random() * uniqueOutOf)));
            if (!f.exists()) {
                if (!f.mkdirs()) throw new IOException("The directory " + f.getAbsolutePath() + " could not be created");
                return f;
            }
        }
        throw new IOException("Reached end of maxIteration(" + maxIterations + "), but could not acquire a unique fileName for folder " + rootFolder);
    }

    /**
	 * Tries to find a unique, not existing folder name under the given root folder 
	 * suffixed with a number. When found, the directory will be created.
	 * It will start with 0 and make Integer.MAX_VALUE number
	 * of iterations maximum. The first not existing foldername will be returned.
	 * If no foldername could be found after the maximum iterations a {@link IOException}
	 * will be thrown.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.  
	 * 
	 * @param rootFolder The rootFolder to find a unique subfolder for
	 * @param prefix A prefix for the folder that has to be found.
	 * @return A File pointing to an unique (not existing) Folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
    public static synchronized File createUniqueIncrementalFolder(File rootFolder, final String prefix) throws IOException {
        for (int n = 0; n <= Integer.MAX_VALUE; n++) {
            File f = new File(rootFolder, String.format("%s%x", prefix, n));
            if (!f.exists()) {
                if (!f.mkdirs()) throw new IOException("The directory " + f.getAbsolutePath() + " could not be created");
                return f;
            }
        }
        throw new IOException("Iterated to Integer.MAX_VALUE and could not find a unique folder!");
    }

    /**
	 * Transfer data between streams
	 * @param in The input stream
	 * @param out The output stream
	 * @param inputOffset How many bytes to skip before transferring
	 * @param inputLen How many bytes to transfer. -1 = all
	 * @return The number of bytes transferred
	 * @throws IOException if an error occurs.
	 */
    public static long transferStreamData(java.io.InputStream in, java.io.OutputStream out, long inputOffset, long inputLen) throws java.io.IOException {
        int bytesRead;
        int transferred = 0;
        byte[] buf = new byte[4096];
        if (inputOffset > 0) if (in.skip(inputOffset) != inputOffset) throw new IOException("Input skip failed (offset " + inputOffset + ")");
        while (true) {
            if (inputLen >= 0) bytesRead = in.read(buf, 0, (int) Math.min(buf.length, inputLen - transferred)); else bytesRead = in.read(buf);
            if (bytesRead <= 0) break;
            out.write(buf, 0, bytesRead);
            transferred += bytesRead;
            if (inputLen >= 0 && transferred >= inputLen) break;
        }
        out.flush();
        return transferred;
    }

    /**
	 * Copy a resource loaded by the class loader of a given class to a file.
	 * <p>
	 * This is a convenience method for <code>copyResource(sourceResClass, sourceResName, new File(destinationFilename))</code>.
	 * @param sourceResClass The class whose class loader to use. If the class 
	 * 		was loaded using the bootstrap class loaderClassloader.getSystemResourceAsStream
	 * 		will be used. See {@link Class#getResourceAsStream(String)} for details.
	 * @param sourceResName The name of the resource
	 * @param destinationFilename Where to copy the contents of the resource
	 * @throws IOException in case of an error
	 */
    public static void copyResource(Class<?> sourceResClass, String sourceResName, String destinationFilename) throws IOException {
        copyResource(sourceResClass, sourceResName, new File(destinationFilename));
    }

    /**
	 * Copy a resource loaded by the class loader of a given class to a file.
	 * @param sourceResClass The class whose class loader to use. If the class 
	 * 		was loaded using the bootstrap class loaderClassloader.getSystemResourceAsStream
	 * 		will be used. See {@link Class#getResourceAsStream(String)} for details.
	 * @param sourceResName The name of the resource
	 * @param destinationFile Where to copy the contents of the resource
	 * @throws IOException in case of an error
	 */
    public static void copyResource(Class<?> sourceResClass, String sourceResName, File destinationFile) throws IOException {
        InputStream source = null;
        FileOutputStream destination = null;
        try {
            source = sourceResClass.getResourceAsStream(sourceResName);
            if (source == null) throw new FileNotFoundException("Class " + sourceResClass.getName() + " could not find resource " + sourceResName);
            if (destinationFile.exists()) {
                if (destinationFile.isFile()) {
                    if (!destinationFile.canWrite()) throw new IOException("FileCopy: destination file is unwriteable: " + destinationFile.getCanonicalPath());
                } else throw new IOException("FileCopy: destination is not a file: " + destinationFile.getCanonicalPath());
            } else {
                File parentdir = destinationFile.getParentFile();
                if (parentdir == null || !parentdir.exists()) throw new IOException("FileCopy: destination directory doesn't exist: " + destinationFile.getCanonicalPath());
                if (!parentdir.canWrite()) throw new IOException("FileCopy: destination directory is unwriteable: " + destinationFile.getCanonicalPath());
            }
            destination = new FileOutputStream(destinationFile);
            transferStreamData(source, destination);
        } finally {
            if (source != null) try {
                source.close();
            } catch (IOException e) {
                ;
            }
            if (destination != null) try {
                destination.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    /**
	 * Copy a file.
	 * <p>
	 * This is a convenience method for <code>copyFile(new File(sourceFilename), new File(destinationFilename))</code>
	 * @param sourceFilename The source file to copy
	 * @param destinationFilename To which file to copy the source
	 * @throws IOException in case of an error
	 */
    public static void copyFile(String sourceFilename, String destinationFilename) throws IOException {
        copyFile(new File(sourceFilename), new File(destinationFilename));
    }

    /**
	 * Copy a file.
	 * @param sourceFile The source file to copy
	 * @param destinationFile To which file to copy the source
	 * @throws IOException in case of an error
	 */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        FileInputStream source = null;
        FileOutputStream destination = null;
        try {
            if (!sourceFile.exists() || !sourceFile.isFile()) throw new IOException("FileCopy: no such source file: " + sourceFile.getCanonicalPath());
            if (!sourceFile.canRead()) throw new IOException("FileCopy: source file is unreadable: " + sourceFile.getCanonicalPath());
            if (destinationFile.exists()) {
                if (destinationFile.isFile()) {
                    if (!destinationFile.canWrite()) throw new IOException("FileCopy: destination file is unwriteable: " + destinationFile.getCanonicalPath());
                } else throw new IOException("FileCopy: destination is not a file: " + destinationFile.getCanonicalPath());
            } else {
                File parentdir = destinationFile.getParentFile();
                if (parentdir == null || !parentdir.exists()) throw new IOException("FileCopy: destination directory doesn't exist: " + destinationFile.getCanonicalPath());
                if (!parentdir.canWrite()) throw new IOException("FileCopy: destination directory is unwriteable: " + destinationFile.getCanonicalPath());
            }
            source = new FileInputStream(sourceFile);
            destination = new FileOutputStream(destinationFile);
            transferStreamData(source, destination);
        } finally {
            if (source != null) try {
                source.close();
            } catch (IOException e) {
                ;
            }
            if (destination != null) try {
                destination.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    /**
	 * Copy a directory recursively.
	 * @param sourceDirectory The source directory
	 * @param destinationDirectory The destination directory
	 * @throws IOException in case of an error
	 */
    public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) throw new IOException("No such source directory: " + sourceDirectory.getAbsolutePath());
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory()) throw new IOException("Destination exists but is not a directory: " + sourceDirectory.getAbsolutePath());
        } else destinationDirectory.mkdirs();
        File[] files = sourceDirectory.listFiles();
        for (File file : files) {
            File destinationFile = new File(destinationDirectory, file.getName());
            if (file.isDirectory()) copyDirectory(file, destinationFile); else copyFile(file, destinationFile);
        }
    }

    /**
	 * Get a file object from a base directory and a list of subdirectories or files.
	 * @param file The base directory
	 * @param subDirs The subdirectories or files
	 * @return The new file instance
	 */
    public static File getFile(File file, String... subDirs) {
        File f = file;
        for (String subDir : subDirs) f = new File(f, subDir);
        return f;
    }

    /**
	 * Returns a String with zeros prefixing
	 * the given base. The returned String will
	 * have at least a length of the given strLength.
	 * <p>
	 * This method calls  {@link #addLeadingChars(String, int, char)} with '0' as
	 * <code>fillChar</code>.
	 * </p>
	 *
	 * @param base The base String to prefix.
	 * @param strLength The length of the resulting String
	 * @return A string with zeros prefixing the given base.
	 */
    public static String addLeadingZeros(String base, int strLength) {
        return addLeadingChars(base, strLength, '0');
    }

    /**
	 * This method adds the character passed as <code>fillChar</code>
	 * to the front of the string <code>s</code> until the total length
	 * of the string reaches <code>length</code>. If the given string
	 * <code>s</code> is longer or exactly as long as defined by
	 * <code>length</code>, no characters will be added.
	 *
	 * @param s The string to which characters are appended (before).
	 * @param length The minimum length of the result.
	 * @param fillChar The character that will be added.
	 * @return the resulting string with as many <code>fillChar</code> characters added to it as necessary.
	 */
    public static String addLeadingChars(String s, int length, char fillChar) {
        if (s != null && s.length() >= length) return s;
        StringBuffer sb = new StringBuffer(length);
        int l = s == null ? length : length - s.length();
        while (sb.length() < l) sb.append(fillChar);
        if (s != null) sb.append(s);
        return sb.toString();
    }

    /**
	 * This method adds the character passed as <code>fillChar</code>
	 * to the end of the string <code>s</code> until the total length
	 * of the string reaches <code>length</code>. If the given string
	 * <code>s</code> is longer or exactly as long as defined by
	 * <code>length</code>, no characters will be added.
	 *
	 * @param s The string to which characters are appended (after).
	 * @param length The minimum length of the result.
	 * @param fillChar The character that will be added.
	 * @return the resulting string with as many <code>fillChar</code> characters added to it as necessary.
	 */
    public static String addTrailingChars(String s, int length, char fillChar) {
        if (s != null && s.length() >= length) return s;
        StringBuffer sb = new StringBuffer(length);
        if (s != null) sb.append(s);
        while (sb.length() < length) sb.append(fillChar);
        return sb.toString();
    }

    /**
	 * Returns a String with spaces prefixing
	 * the given base. The returned String will
	 * have at least a length of the given strLength.
	 * <p>
	 * This method calls {@link #addLeadingChars(String, int, char)} with spaces (' ') as
	 * <code>fillChar</code>.
	 * </p>
	 *  
	 * @param base The base String to prefix.
	 * @param strLength The length of the resulting String
	 * @return A string with zeros prefixing the given base.
	 */
    public static String addLeadingSpaces(String base, int strLength) {
        return addLeadingChars(base, strLength, ' ');
    }

    /**
	 * This method encodes a byte array into a human readable hex string. For each byte,
	 * two hex digits are produced. They are concatted without any separators.
	 * <p>
	 * This is a convenience method for <code>encodeHexStr(buf, 0, buf.length)</code>
	 *
	 * @param buf The byte array to translate into human readable text.
	 * @return a human readable string like "fa3d70" for a byte array with 3 bytes and these values.
	 */
    public static String encodeHexStr(byte[] buf) {
        return encodeHexStr(buf, 0, buf.length);
    }

    /**
	 * Encode a byte array into a human readable hex string. For each byte,
	 * two hex digits are produced. They are concatted without any separators.
	 *
	 * @param buf The byte array to translate into human readable text.
	 * @param pos The start position (0-based).
	 * @param len The number of bytes that shall be processed beginning at the position specified by <code>pos</code>.
	 * @return a human readable string like "fa3d70" for a byte array with 3 bytes and these values.
	 * @see #decodeHexStr(byte[])
	 */
    public static String encodeHexStr(byte[] buf, int pos, int len) {
        StringBuffer hex = new StringBuffer();
        while (len-- > 0) {
            byte ch = buf[pos++];
            int d = (ch >> 4) & 0xf;
            hex.append((char) (d >= 10 ? 'a' - 10 + d : '0' + d));
            d = ch & 0xf;
            hex.append((char) (d >= 10 ? 'a' - 10 + d : '0' + d));
        }
        return hex.toString();
    }

    /**
	 * @deprecated FIXME: This method has the wrong name. It does in fact encode - not decode. Use
	 *		{@link #encodeHexStr(byte[], int, int)} or {@link #encodeHexStr(byte[])}.
	 */
    public static String decodeHexStr(byte[] buf, int pos, int len) {
        return encodeHexStr(buf, pos, len);
    }

    /**
	 * Decode a string containing two hex digits for each byte.
	 * @param hex The hex encoded string
	 * @return The byte array represented by the given hex string
	 */
    public static byte[] decodeHexStr(String hex) {
        if (hex.length() % 2 != 0) throw new IllegalArgumentException("The hex string must have an even number of characters!");
        byte[] res = new byte[hex.length() / 2];
        int m = 0;
        for (int i = 0; i < hex.length(); i += 2) {
            res[m++] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return res;
    }

    /**
	 * Get a 32 character MD5 hash in hex notation for the given input string.
	 * @param clear The input string to build the hash on
	 * @return The MD5 encoded hex string.
	 * @throws NoSuchAlgorithmException
	 */
    public static String getMD5HexString(String clear) throws NoSuchAlgorithmException {
        byte[] enc = MessageDigest.getInstance("MD5").digest(clear.getBytes());
        System.out.println(new String(enc));
        return encodeHexStr(enc, 0, enc.length);
    }

    /**
	 * @deprecated FIXME: this method has a misleading name.
	 * 		It is a mixture of something like htmlEntities
	 * 		(but not complete), nl2br (but nox XHTML conform)
	 * 		and tab2spaces. (Marc)
	 */
    public static String htmlEncode(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n') sb.append("<br>"); else if (ch == '<') sb.append("&lt;"); else if (ch == '>') sb.append("&gt;"); else if (ch == '\t') sb.append("&nbsp;&nbsp;&nbsp;&nbsp;"); else sb.append(ch);
        }
        return sb.toString();
    }

    /**
	 * Read a UTF-8 encoded text file and return the contents as string.
	 * <p>For other encodings, use {@link #readTextFile(File, String)}.
	 * @param f The file to read, maximum size 1 GB
	 * @throws FileNotFoundException if the file was not found
	 * @throws IOException in case of an error
	 * @return The contents of the text file
	 */
    public static String readTextFile(File f) throws FileNotFoundException, IOException {
        return readTextFile(f, CHARSET_NAME_UTF_8);
    }

    /**
	 * Read a text file and return the contents as string.
	 * <p>For other encodings, use {@link #readTextFile(File, String)}.
	 * @param f The file to read, maximum size 1 GB
	 * @param encoding The file encoding, e.g. "UTF-8"
	 * @throws FileNotFoundException if the file was not found
	 * @throws IOException in case of an error
	 * @return The contents of the text file
	 */
    public static String readTextFile(File f, String encoding) throws FileNotFoundException, IOException {
        if (f.length() > GIGABYTE) throw new IllegalArgumentException("File exceeds " + GIGABYTE + " bytes: " + f.getAbsolutePath());
        StringBuffer sb = new StringBuffer();
        FileInputStream fin = new FileInputStream(f);
        try {
            InputStreamReader reader = new InputStreamReader(fin, encoding);
            try {
                char[] cbuf = new char[1024];
                int bytesRead;
                while (true) {
                    bytesRead = reader.read(cbuf);
                    if (bytesRead <= 0) break; else sb.append(cbuf, 0, bytesRead);
                }
            } finally {
                reader.close();
            }
        } finally {
            fin.close();
        }
        return sb.toString();
    }

    /**
	 * Read a text file from an {@link InputStream} using
	 * UTF-8 encoding.
	 * <p>
	 * This method does NOT close the input stream!
	 * @param in The stream to read from. It will not be closed by this operation.
	 * @return The contents of the input stream file
	 */
    public static String readTextFile(InputStream in) throws FileNotFoundException, IOException {
        return readTextFile(in, CHARSET_NAME_UTF_8);
    }

    /**
	 * Read a text file from an {@link InputStream} using
	 * the given encoding.
	 * <p>
	 * This method does NOT close the input stream!
	 * @param in The stream to read from. It will not be closed by this operation.
	 * @param encoding The charset used for decoding, e.g. "UTF-8"
	 * @return The contents of the input stream file
	 */
    public static String readTextFile(InputStream in, String encoding) throws FileNotFoundException, IOException {
        StringBuffer sb = new StringBuffer();
        InputStreamReader reader = new InputStreamReader(in, encoding);
        char[] cbuf = new char[1024];
        int bytesRead;
        while (true) {
            bytesRead = reader.read(cbuf);
            if (bytesRead <= 0) break; else sb.append(cbuf, 0, bytesRead);
            if (sb.length() > GIGABYTE) throw new IllegalArgumentException("Text exceeds " + GIGABYTE + " bytes!");
        }
        return sb.toString();
    }

    /**
	 * Get the extension of a filename.
	 * @param fileName A file name (might contain the full path) or <code>null</code>.
	 * @return <code>null</code>, if the given <code>fileName</code> doesn't contain
	 *		a dot (".") or if the given <code>fileName</code> is <code>null</code>. Otherwise, 
	 *		returns all AFTER the last dot.
	 */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex < 0) return null;
        return fileName.substring(lastIndex + 1);
    }

    /**
	 * Get a filename without extension.
	 * @param fileName A file name (might contain the full path) or <code>null</code>.
	 * @return all before the last dot (".") or the full <code>fileName</code> if no dot exists. 
	 * 		Returns <code>null</code>, if the given <code>fileName</code> is <code>null</code>.   
	 */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return null;
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex < 0) return fileName;
        return fileName.substring(0, lastIndex);
    }

    private static File tempDir = null;

    /**
	 * Get the temporary directory.
	 * 
	 * FIXME: it seems not to be a good practise to create
	 * 		a temp file just to get the directory. accessing
	 * 		the system temp dir property would work without
	 * 		hd access and without throwing an IOException. 
	 * 		See File.getTempDir() (private) (Marc)
	 * 
	 * @return The temporary directory.
	 * @throws IOException In case of an error
	 */
    public static File getTempDir() throws IOException {
        if (tempDir == null) {
            File tmp = File.createTempFile("tmp.", ".tmp");
            File res = tmp.getParentFile();
            if (!res.isAbsolute()) res = res.getAbsoluteFile();
            tempDir = res;
            tmp.delete();
        }
        return tempDir;
    }

    /**
	 * @deprecated This method has a wrong name. It does not create a
	 * 		unique key, but hashes the input value. Use {@link #hash(byte[], String)}
	 * 		instead.
	 * 
	 * Hash a byte array with the given algorithm.
	 * 
	 * @param imageData the data to generate a unique key for
	 * @param algorithm the name of the alogorithm (e.g. MD5, SHA)
	 * @return a unique key for the given byte[]
	 */
    public static byte[] generateUniqueKey(byte[] data, String algorithm) {
        try {
            return hash(data, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Specifies usage of the MD5 algorithm in {@link #hash(byte[], String)} (or one of the other hash methods).
	 */
    public static final String HASH_ALGORITHM_MD5 = "MD5";

    /**
	 * Specifies usage of the SHA algorithm in {@link #hash(byte[], String)} (or one of the other hash methods).
	 */
    public static final String HASH_ALGORITHM_SHA = "SHA";

    /**
	 * Hash a byte array with the given algorithm.
	 *
	 * @param data The data to hash
	 * @param algorithm The name of the hash alogorithm (e.g. MD5, SHA) as supported by {@link MessageDigest}. If using one of these
	 *		algorithms, you should use the appropriate constant: {@link #HASH_ALGORITHM_MD5} or {@link #HASH_ALGORITHM_SHA}.
	 * @return the array of bytes for the resulting hash value.
	 * @throws NoSuchAlgorithmException if the algorithm is not available in the caller's environment.
	 *
	 * @see #hash(File, String)
	 * @see #hash(InputStream, long, String)
	 */
    public static byte[] hash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return md.digest();
    }

    /**
	 * Hash an {@link InputStream} with the given algorithm.
	 *
	 * @param in The {@link InputStream} which will be read. This stream is not closed, but read until its end or until the number of bytes specified
	 *		in <code>bytesToRead</code> has been read.
	 * @param bytesToRead If -1, the {@link InputStream} <code>in</code> will be read till its end is reached. Otherwise, the amount of bytes specified by
	 *		this parameter is read. If the {@link InputStream} ends before having read the specified amount of bytes, an {@link IOException} will be thrown.
	 * @param algorithm The name of the hash alogorithm (e.g. MD5, SHA) as supported by {@link MessageDigest}. If using one of these
	 *		algorithms, you should use the appropriate constant: {@link #HASH_ALGORITHM_MD5} or {@link #HASH_ALGORITHM_SHA}.
	 * @return the array of bytes for the resulting hash value.
	 * @throws NoSuchAlgorithmException if the algorithm is not available in the caller's environment.
	 * @throws IOException if reading from the {@link InputStream} fails.
	 *
	 * @see #hash(byte[], String)
	 */
    public static byte[] hash(InputStream in, long bytesToRead, String algorithm) throws NoSuchAlgorithmException, IOException {
        if (bytesToRead < 0 && bytesToRead != -1) throw new IllegalArgumentException("bytesToRead < 0 && bytesToRead != -1");
        long bytesReadTotal = 0;
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] data = new byte[10240];
        while (true) {
            int len;
            if (bytesToRead < 0) len = data.length; else {
                len = (int) Math.min((long) data.length, bytesToRead - bytesReadTotal);
                if (len < 1) break;
            }
            int bytesRead = in.read(data, 0, len);
            if (bytesRead < 0) {
                if (bytesToRead >= 0) throw new IOException("Unexpected EndOfStream! bytesToRead==" + bytesToRead + " but only " + bytesReadTotal + " bytes could be read from InputStream!");
                break;
            }
            bytesReadTotal += bytesRead;
            if (bytesRead > 0) md.update(data, 0, bytesRead);
        }
        return md.digest();
    }

    /**
	 * This methods reads a given file and calls {@link #hash(InputStream, long, String)}.
	 *
	 * @param file The file to read an hash.
	 * @param algorithm The algorithm to use.
	 * @return The result of {@link #hash(InputStream, long, String)}.
	 * @throws NoSuchAlgorithmException if the algorithm is not available in the caller's environment.
	 * @throws IOException if reading the <code>file</code> fails.
	 *
	 * @see #hash(byte[], String)
	 */
    public static byte[] hash(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            return hash(in, -1, algorithm);
        } finally {
            in.close();
        }
    }

    /**
	 * @deprecated Use {@link Arrays#equals(byte[], byte[])}.
	 * 
	 * compares two byte[]s
	 * 
	 * @param b1 the first byte[]
	 * @param b2 the second byte[]
	 * @return true if both byte[]s contain the identical data or false if not 
	 */
    public static boolean compareByteArrays(byte[] b1, byte[] b2) {
        return Arrays.equals(b1, b2);
    }

    /**
	 * Compares two InputStreams.
	 * 
	 * @param in1 the first InputStream
	 * @param in2 the second InputStream
	 * @param length the length to read from each InputStream
	 * @return true if both InputStreams contain the identical data or false if not
	 * @throws IOException if an I/O error occurs while reading <code>length</code> bytes
	 * 		from one of the input streams.  
	 */
    public static boolean compareInputStreams(InputStream in1, InputStream in2, int length) throws IOException {
        boolean identical = true;
        int read = 0;
        while (read < length) {
            int int1 = in1.read();
            int int2 = in2.read();
            read++;
            if (int1 != int2) {
                identical = false;
                break;
            }
        }
        if (read < length) {
            in1.skip(length - read);
            in2.skip(length - read);
        }
        return identical;
    }

    /**
	 * Recursively zips all entries of the given zipInputFolder to
	 * a zipFile defined by zipOutputFile.
	 * 
	 * @param zipOutputFile The file to write to (will be deleted if existent).
	 * @param zipInputFolder The inputFolder to zip.
	 */
    public static void zipFolder(File zipOutputFile, File zipInputFolder) throws IOException {
        FileOutputStream fout = new FileOutputStream(zipOutputFile);
        ZipOutputStream out = new ZipOutputStream(fout);
        try {
            File[] files = zipInputFolder.listFiles();
            zipFilesRecursively(out, zipOutputFile, files, zipInputFolder.getAbsoluteFile());
        } finally {
            out.close();
        }
    }

    /**
	 * Recursively writes all found files as entries into the given ZipOutputStream.
	 * 
	 * @param out The ZipOutputStream to write to.
	 * @param zipOutputFile the output zipFile. optional. if it is null, this method cannot check whether
	 *		your current output file is located within the zipped directory tree. You must not locate
	 *		your zip-output file within the source directory, if you leave this <code>null</code>.
	 * @param files The files to zip (optional, defaults to all files recursively). It must not be <code>null</code>,
	 *		if <code>entryRoot</code> is <code>null</code>.
	 * @param entryRoot The root folder of all entries. Entries in subfolders will be
	 *		added relative to this. If <code>entryRoot==null</code>, all given files will be
	 *		added without any path (directly into the zip's root). <code>entryRoot</code> and <code>files</code> must not
	 *		both be <code>null</code> at the same time.
	 * @throws IOException in case of an I/O error.
	 */
    public static void zipFilesRecursively(ZipOutputStream out, File zipOutputFile, File[] files, File entryRoot) throws IOException {
        if (entryRoot == null && files == null) throw new IllegalArgumentException("entryRoot and files must not both be null!");
        if (entryRoot != null && !entryRoot.isDirectory()) throw new IllegalArgumentException("entryRoot is not a directory: " + entryRoot.getAbsolutePath());
        if (files == null) {
            files = new File[] { entryRoot };
        }
        byte[] buf = new byte[1024 * 5];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (zipOutputFile != null) if (file.equals(zipOutputFile)) continue;
            if (file.isDirectory()) {
                File[] dirFiles = file.listFiles();
                zipFilesRecursively(out, zipOutputFile, dirFiles, entryRoot);
            } else {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                String relativePath = entryRoot == null ? file.getName() : getRelativePath(entryRoot, file.getAbsoluteFile());
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified());
                out.putNextEntry(entry);
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
        }
    }

    /**
	 * Unzip the given archive into the given folder.
	 * 
	 * @param zipArchive The zip file to unzip.
	 * @param unzipRootFolder The folder to unzip to.
	 * @throws IOException in case of an I/O error.
	 */
    public static void unzipArchive(File zipArchive, File unzipRootFolder) throws IOException {
        ZipFile zipFile = new ZipFile(zipArchive);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                File dir = new File(unzipRootFolder, entry.getName());
                if (!dir.mkdirs()) throw new IllegalStateException("Could not create directory entry, possibly permission issues.");
            } else {
                InputStream in = zipFile.getInputStream(entry);
                File file = new File(unzipRootFolder, entry.getName());
                File dir = new File(file.getParent());
                if (dir.exists()) {
                    assert (dir.isDirectory());
                } else {
                    dir.mkdirs();
                }
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                int len;
                byte[] buf = new byte[1024 * 5];
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
        zipFile.close();
    }

    /**
	 * Truncates a double value at the given decimal position.
	 * E.g. <code>d</code> = 9.45935436363463 and <code>numDigits</code> = 2 
	 * will return 9.45.
	 * 
	 * @param d the double to shorten
	 * @param numDigits the number of decimal places after the decimal separator.
	 * @return the shorted double
	 */
    public static double truncateDouble(double d, int numDigits) {
        double multiplier = Math.pow(10, numDigits);
        return ((int) (d * multiplier)) / multiplier;
    }

    /**
	 * Makes a Double out of Integer. The parameter <code>numDigits</code> 
	 * determines where the decimal separator should be, seen from the end.
	 * E.g. <code>value</code> = 135 and <code>numDigits</code> = 2 will 
	 * return 1.35.
	 * 
	 * @param value the integer to transform into a double
	 * @param numDigits determines where the decimal separator should be, 
	 * 		seen from the end
	 * @return the transformed double
	 */
    public static double getDouble(int value, int numDigits) {
        double multiplier = Math.pow(10, numDigits);
        return ((double) value) / multiplier;
    }

    /**
	 * @deprecated Use {@link #getStackTraceAsString(Throwable)} instead
	 */
    public static String getStacktraceAsString(Throwable t) {
        return getStackTraceAsString(t);
    }

    /**
	 * Get the stack trace representation of a <code>Throwable</code>
	 * as string. 
	 * @param t The <code>Throwable</code>
	 * @return The stack trace as string.
	 */
    public static String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
	 * Append a field string representation for an Oject to
	 * a {@link StringBuffer}.
	 * @param o The Object to represent
	 * @param s A {@link StringBuffer} to append the string.
	 */
    public static void toFieldString(Object o, StringBuffer s) {
        final Field fields[] = o.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; ++i) {
            s.append(",");
            s.append(fields[i].getName());
            s.append("=");
            try {
                fields[i].setAccessible(true);
                s.append(fields[i].get(o));
            } catch (IllegalAccessException ex) {
                s.append("*private*");
            }
        }
    }

    /**
	 * Get a field string representation for an Oject.
	 * @param o The Object to represent
	 * @return The field string representation.
	 */
    public static String toFieldString(Object o) {
        StringBuffer s = new StringBuffer();
        toFieldString(o, s);
        return s.toString();
    }

    /**
	 * A generic reflection-based toString method. 
	 * @param o The Object to represent
	 * @return The string representation of the object.
	 */
    public static String toString(Object o) {
        StringBuffer s = new StringBuffer();
        s.append(o.getClass().getName());
        s.append("@");
        s.append(Integer.toHexString(o.hashCode()));
        s.append("[");
        toFieldString(o, s);
        s.append("]");
        return s.toString();
    }

    /**
	 * Get a byte array as hex string.
	 * <p>
	 * This method used upper-case "A"..."F" till version 1.2 of this class. From version 1.3 of this class on,
	 * it uses "a"..."f" because it now delegates to {@link #encodeHexStr(byte[])}.
	 * </p>
	 * @param in The source byte array
	 * @return the hex string.
	 * @deprecated Use {@link #encodeHexStr(byte[])} instead.
	 */
    public static String byteArrayToHexString(byte in[]) {
        return encodeHexStr(in);
    }

    /**
	 * Convert an URL to an URI.
	 * @param url The URL to cenvert
	 * @return The URI
	 * @throws MalformedURLException if the given URL is malformed
	 */
    public static URI urlToUri(URL url) throws MalformedURLException {
        if (url == null) return null;
        try {
            return new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException e) {
            MalformedURLException newEx = new MalformedURLException("URL " + url + " was malformed");
            newEx.initCause(e);
            throw newEx;
        }
    }
}
