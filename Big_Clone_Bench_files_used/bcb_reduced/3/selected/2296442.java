package jxl.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * <description>
 *
 * @author epr
 */
public final class FileUtils {

    private FileUtils() {
    }

    public static byte[] digest(File f) throws IOException, NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA").digest(load(f));
    }

    /** Get all of the files (not dirs) under <CODE>dir</CODE>
     * @param dir Directory to search.
     * @return all the files under <CODE>dir</CODE>
     */
    public static Set<File> getRecursiveFiles(File dir) throws IOException {
        if (!dir.isDirectory()) {
            HashSet<File> one = new HashSet<File>();
            one.add(dir);
            return one;
        } else {
            Set<File> ret = recurseDir(dir);
            return ret;
        }
    }

    private static Set<File> recurseDir(File dir) throws IOException {
        HashSet<File> c = new HashSet<File>();
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                c.addAll(recurseDir(files[i]));
            } else {
                c.add(files[i]);
            }
        }
        return c;
    }

    public static boolean deleteRecursively(File dir) {
        if (dir.isDirectory()) {
            boolean success = true;
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) success |= deleteRecursively(file); else success |= file.delete();
            }
            return success;
        } else {
            return dir.delete();
        }
    }

    /**
     * Copy dest.length bytes from the inputstream into the dest bytearray.
     * @param is
     * @param dest
     * @throws IOException
     */
    public static void copy(InputStream is, byte[] dest) throws IOException {
        int len = dest.length;
        int ofs = 0;
        while (len > 0) {
            int size = is.read(dest, ofs, len);
            ofs += size;
            len -= size;
        }
    }

    /** Manually copy bytes from <CODE>source</CODE> to <CODE>target</CODE>
     * @param source
     * @param target
     * @throws IOException
     */
    public static void copy(File source, File target) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(target);
        fis.getChannel().transferTo(0, source.length(), fos.getChannel());
        fis.close();
        fos.flush();
        fos.close();
    }

    public static boolean copy(Set<File> sources, File toDir, boolean printout) throws IOException {
        boolean completeSuccess = true;
        int index = 0;
        int total = sources.size();
        TextTools tools = new TextTools();
        if (printout) {
            tools.startReprint("Copy " + total + " files to dir " + toDir + "\n");
        }
        for (File source : sources) {
            File target = new File(toDir, source.getName());
            if (printout) {
                tools.reprint(index + " : " + MathUtils.intPercent(index, total) + "% : " + source.getName());
            }
            try {
                copy(source, target);
            } catch (IOException ioe) {
                completeSuccess = false;
                ioe.printStackTrace();
            }
            index++;
        }
        if (printout) {
            tools.endReprint("Finished");
        }
        return completeSuccess;
    }

    /**
     * Copy the contents of is to os.
     * @param is
     * @param os
     * @param buf Can be null
     * @param close If true, is is closed after the copy.
     * @throws IOException
     */
    public static final void copy(InputStream is, OutputStream os, byte[] buf, boolean close) throws IOException {
        int len;
        if (buf == null) {
            buf = new byte[4096];
        }
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        os.flush();
        if (close) {
            is.close();
        }
    }

    public static void flush(byte[] data, File toFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(toFile);
        fos.write(data);
        fos.flush();
        fos.close();
    }

    /** Read <CODE>f</CODE> and return as byte[]
     * @param f
     * @throws IOException
     * @return bytes from <CODE>f</CODE>
     */
    public static final byte[] load(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        return load(fis, true);
    }

    /**
     * Copy the contents of is to the returned byte array.
     * @param is
     * @param close If true, is is closed after the copy.
     * @throws IOException
     */
    public static final byte[] load(InputStream is, boolean close) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(is, os, null, close);
        return os.toByteArray();
    }
}
