package ssmith.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ssmith.lang.Functions;

public class IOFunctions {

    public static void WaitForData(InputStream is, int time) throws IOException {
        Functions.delay(100);
        long now = System.currentTimeMillis();
        while (is.available() <= 0) {
            Functions.delay(100);
            if (System.currentTimeMillis() - now > time) {
                throw new IOException("Timeout waiting for data");
            }
        }
    }

    public static String GetMD5(String filename) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(filename);
        try {
            is = new DigestInputStream(is, md);
            byte b[] = new byte[1024];
            while (is.available() >= 0) {
                int len = is.read(b);
                if (len < 0) {
                    break;
                }
            }
        } finally {
            is.close();
        }
        return new BigInteger(1, md.digest()).toString(16);
    }

    public static void CreateDirsForFile(String f) {
        String path = f.replaceAll("\\\\", "/");
        int pos = path.lastIndexOf("/");
        path = path.substring(0, pos);
        File f2 = new File(path);
        f2.mkdirs();
    }

    /**
	 * This moves one dir to another, but if there is already a dir with that name, it renames it
	 * with the preifix.
	 * 
	 * @param move_from
	 * @param move_to
	 * @param add_suffix
	 * @throws IOException
	 */
    public static void moveDirectory(String move_from, String move_to, String add_suffix) throws IOException {
        File from = new File(move_from);
        File to = new File(move_to);
        if (from.canRead()) {
            if (to.canRead()) {
                if (to.renameTo(new File(move_to + add_suffix)) == false) {
                    throw new IOException("Could not rename '" + move_to + "' to '" + move_to + add_suffix + "' as there is a dir in the way that cannot be moved.");
                }
            }
            boolean success = from.renameTo(new File(move_to));
            if (!success) {
                throw new IOException("Could not move '" + move_from + "' to '" + move_to + "'");
            }
        }
    }

    public static void CopyFile(String srFile, String dtFile, boolean create_dirs) throws IOException {
        File f1 = new File(srFile);
        File f2 = new File(dtFile);
        if (create_dirs) {
            CreateDirsForFile(dtFile);
        }
        InputStream in = new FileInputStream(f1);
        OutputStream out = new FileOutputStream(f2);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void CopyDirectory(File sourceLocation, File targetLocation, boolean ignore_dot_files) throws IOException {
        if (ignore_dot_files) {
            if (sourceLocation.getName().startsWith(".")) {
                return;
            }
        }
        System.out.println("Copying " + sourceLocation);
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                CopyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]), ignore_dot_files);
            }
        } else {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static String GetUniqueFilenameAndPath(String path, String ext) {
        while (true) {
            int i = Functions.rnd(1000000, 999999999);
            File tmp = new File(path + i + "." + ext);
            if (tmp.canRead() == false) {
                return tmp.getAbsolutePath();
            }
            i++;
        }
    }

    public static String GetFilenameExtension(String s) {
        int pos = s.lastIndexOf(".");
        if (pos > 0) {
            return s.substring(pos + 1);
        } else {
            return "";
        }
    }
}
