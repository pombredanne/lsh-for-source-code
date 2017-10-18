package ru.adv.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.util.StringUtils;
import ru.adv.io.CopyException;
import ru.adv.io.InputOutputException;
import ru.adv.logger.TLogger;

/**
 * Набор функций для манипулирования фйлами
 */
public class Files {

    private static final int FILE_BUFF_SIZE = 1024 * 50;

    private Files() {
    }

    public static byte[] md5Digest(File file) throws InputOutputException {
        try {
            InputStream input = null;
            MessageDigest md = MessageDigest.getInstance("MD5");
            try {
                input = new BufferedInputStream(new FileInputStream(file));
                byte[] buff = new byte[10240];
                int countBytes = 0;
                while ((countBytes = input.read(buff)) > -1) {
                    md.update(buff, 0, countBytes);
                }
                return md.digest();
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (Exception e) {
            throw new InputOutputException(e, file);
        }
    }

    /**
     * test if absolute file path
     * @return
     */
    public static boolean isAbsolute(String filePath) {
        return StringUtils.hasText(filePath) && new File(filePath).isAbsolute();
    }

    /**
     * Копирует файл <code>from</code> в файл <code>to</code>
     */
    public static void copy(File from, File to) throws InputOutputException {
        copy(from, to, false);
    }

    /**
     * Копирует файл <code>from</code> в файл <code>to</code>
     */
    public static void copy(File from, File to, boolean backupMode) throws InputOutputException {
        try {
            TLogger logger = new TLogger(Files.class);
            if (backupMode && isFileEquals(from, to)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("skip " + from);
                }
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("copy " + from);
            }
            InputStream in = new BufferedInputStream(new FileInputStream(from), FILE_BUFF_SIZE);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(to), FILE_BUFF_SIZE);
            Stream.readTo(in, out);
            in.close();
            out.close();
            if (backupMode) {
                to.setLastModified(from.lastModified());
                to.setExecutable(from.canExecute());
            }
        } catch (IOException e) {
            throw new CopyException(e, from.getPath(), to.getPath());
        }
    }

    private static boolean isFileEquals(File f1, File f2) {
        return f1.exists() == f2.exists() && f1.lastModified() == f2.lastModified() && f1.length() == f2.length() && f1.getName().equals(f2.getName());
    }

    /**
     * Копирует файл <code>from</code> в файл <code>to</code>
     */
    public static void copy(InputOutput from, File to) throws InputOutputException {
        try {
            InputStream in = from.getInputStream();
            FileOutputStream out = new FileOutputStream(to);
            Stream.readTo(in, out);
            in.close();
            out.close();
        } catch (IOException e) {
            throw new CopyException(e, from.getSystemId(), to.getAbsolutePath());
        }
    }

    public static void copy(String from, String to) throws InputOutputException {
        copy(getFile(from), getFile(to));
    }

    public static void copyDirectory(File from, File to) throws InputOutputException {
        _copyDirectory(from, to, false, true);
    }

    /**
     * @param from
     * @param to
     * @param backupMode copy files if it not exists or has another modification time
     * @throws InputOutputException
     */
    public static void copyDirectory(File from, File to, boolean backupMode) throws InputOutputException {
        _copyDirectory(from, to, backupMode, true);
    }

    private static void _copyDirectory(File from, File to, boolean backupMode, boolean firstLevel) throws InputOutputException {
        if (to.exists()) {
            if (!to.isDirectory()) {
                throw new IllegalArgumentException(to.getPath() + " must be directory");
            }
        } else {
            if (!to.mkdirs()) {
                throw new InputOutputException("Cannot create " + to.getPath(), to.getPath());
            }
        }
        if (!from.exists()) {
            final String message = "File " + from.getPath() + " does not exist";
            if (!backupMode) {
                throw new InputOutputException(message, from.getPath());
            } else {
                TLogger.warning(Files.class, message);
                return;
            }
        }
        if (from.isFile()) {
            copy(from, getFile(to.getPath() + "/" + from.getName()), backupMode);
        } else if (from.isDirectory()) {
            File targetDir;
            if (firstLevel) {
                targetDir = to;
            } else {
                targetDir = getFile(to.getPath() + "/" + from.getName());
                if (!to.canWrite()) {
                    throw new InputOutputException("Cannot create " + targetDir.getPath(), targetDir.getPath());
                }
                targetDir.mkdirs();
            }
            File[] list = from.listFiles();
            for (int i = 0; i < list.length; i++) {
                _copyDirectory(list[i], targetDir, backupMode, false);
            }
        } else {
            throw new UnreachableCodeReachedException("Files.copyDirectory(): " + from.getPath() + ": unknown file type");
        }
    }

    public static void copyDirectory(String from, String to) throws InputOutputException {
        copyDirectory(from, to, false);
    }

    public static void copyDirectory(String from, String to, boolean backupMode) throws InputOutputException {
        copyDirectory(getFile(from), getFile(to), backupMode);
    }

    public static File getFile(String name) {
        return new File(name);
    }

    /**
     * Удаляет файл или дирректорию, если таковая существует
     * 
     * @param file
     *            что удаляем
     * @param recursive
     *            в случае с дирректорией рекурсивно удаляет дирекотрию
     */
    public static boolean remove(File file, boolean recursive) {
        return remove(file, recursive, true);
    }

    /**
     * Удаляет файл или дирректорию, если таковая существует
     * 
     * @param file
     *            что удаляем
     * @param recursive
     *            в случае с дирректорией рекурсивно удаляет дирекотрию
     * @param deleteDir
     *            false - only delete content of directory, without delete
     *            direcory
     */
    private static boolean remove(File file, boolean recursive, boolean deleteDir) {
        if (file.exists()) {
            if (file.isDirectory() && recursive) {
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++) {
                    if (!remove(list[i], recursive)) {
                        return false;
                    }
                }
                if (!deleteDir) return true;
            }
            return file.delete();
        }
        return false;
    }

    public static boolean remove(String file, boolean recursive) {
        return remove(getFile(file), recursive);
    }

    /**
     * Remove all content of directory, without remove direcory
     * 
     * @return
     */
    public static boolean cleanDirectory(String dirPath) {
        File dir = getFile(dirPath);
        if (!dir.isDirectory()) return false;
        return remove(dir, true, false);
    }

    /**
     * Normalize a path. Eliminates "/../" and "/./" in a string. Returns
     * <code>null</code> if the ..'s went past the root. Eg:
     * 
     * <pre>
     *  /foo//               --&gt;     /foo/
     *  /foo/./              --&gt;     /foo/
     *  /foo/../bar          --&gt;     /bar
     *  /foo/../bar/         --&gt;     /bar/
     *  /foo/../bar/../baz   --&gt;     /baz
     *  //foo//./bar         --&gt;     /foo/bar
     *  /../                 --&gt;     null
     * </pre>
     * 
     * @param path
     *            the path to normalize
     * @return the normalized String, or <code>null</code> if too many ..'s.
     */
    public static String normalize(final String path) {
        return StringUtils.cleanPath(path);
    }

    public static void rename(File source, File dest) throws IOException {
        if (!source.renameTo(dest)) {
            throw new IOException("Cannot rename file " + source.getName());
        }
    }

    public static void rename(String source, String dest) throws InputOutputException {
        if (!new File(source).renameTo(new File(dest))) {
            throw new InputOutputException("Cannot rename file " + source + " to " + dest, source);
        }
    }

    public static void delete(File source) throws IOException {
        if (!source.delete()) {
            throw new IOException("Cannot delete file " + source.getName());
        }
    }

    /**
     * Returns the name of the file or directory
     * 
     * @param filePath
     * @return
     */
    public static String getName(String filePath) {
        List<String> items = Strings.split(filePath, "\\/:");
        String name = items.size() > 0 ? (String) items.get(items.size() - 1) : filePath;
        if (name.length() > 0) {
            return name;
        }
        return filePath;
    }

    public static File createTempFile(String prefix, String suffix, File journalDir) throws InputOutputException {
        try {
            return File.createTempFile(prefix, suffix, journalDir);
        } catch (IOException e) {
            throw new InputOutputException("Cannot create temporary file in " + journalDir.getPath(), journalDir);
        }
    }

    /**
     * compress file
     * 
     * @param from
     * @param to
     * @param callback
     * @return file size
     * @throws IOException
     */
    public static long compressFile(File from, File to, ProgressCallback callback) throws IOException {
        long result = 0;
        FileInputStream fis = new FileInputStream(from);
        try {
            GZIPOutputStream gos = new _GZIP_This(new FileOutputStream(to), true);
            try {
                Stream.readTo(fis, gos, callback);
                gos.flush();
            } finally {
                gos.close();
                result = to.length();
            }
        } finally {
            fis.close();
        }
        return result;
    }

    public static void createDir(final String workDir) {
        final File dir = new File(workDir);
        createDir(dir);
    }

    public static void createDir(final File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    static class _GZIP_This extends GZIPOutputStream {

        public _GZIP_This(OutputStream out, boolean isBestSpeed) throws IOException {
            super(out, 10240);
            if (isBestSpeed) {
                def.setLevel(Deflater.BEST_SPEED);
            }
        }
    }

    /**
     * uncompress file
     * 
     * @param from
     * @param to
     * @param callback
     * @return file size
     * @throws IOException
     */
    public static long uncompressFile(File from, File to, ProgressCallback callback) throws IOException {
        long result = 0;
        FileInputStream fis = new FileInputStream(from);
        try {
            GZIPInputStream gis = new GZIPInputStream(fis, 10240);
            FileOutputStream fos = new FileOutputStream(to);
            try {
                Stream.readTo(gis, fos, callback);
                fos.flush();
            } finally {
                fos.close();
                result = to.length();
            }
        } finally {
            fis.close();
        }
        return result;
    }

    public static boolean readable(File file) {
        return file != null && file.exists() && file.canRead();
    }
}
