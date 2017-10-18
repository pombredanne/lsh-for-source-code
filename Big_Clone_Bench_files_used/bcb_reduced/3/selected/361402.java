package west.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 *
 * @author armnant
 */
public class Utils {

    public static String readFile(String filename) throws FileNotFoundException, IOException {
        return readFile(new File(filename));
    }

    public static String readFile(File file) throws FileNotFoundException, IOException {
        String s = null;
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = br.readLine()) != null) {
            if (s == null) {
                s = line;
            } else {
                s = s + "\n" + line;
            }
        }
        return s;
    }

    public static void makeDir(String path) throws IOException {
        makeDir(new File(path));
    }

    public static void makeDir(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create directory" + dir.getAbsolutePath());
        }
    }

    protected static String checksum(InputStream is) throws FileNotFoundException {
        try {
            byte[] buffer = new byte[1024];
            java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
            int read;
            do {
                read = is.read(buffer);
                if (read > 0) md5er.update(buffer, 0, read);
            } while (read != -1);
            byte[] digest = md5er.digest();
            if (digest == null) return null;
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        } catch (Exception e) {
            return null;
        }
    }

    public static void copyStreamToFileWithCheck(InputStream is, File out) throws FileNotFoundException, IOException, DirectoryDetectedException {
        copyStreamToFileWithCheck(is, out, true, true, false);
    }

    public static void copyStreamToFileWithCheck(InputStream is, File out, boolean read, boolean write, boolean execute) throws FileNotFoundException, IOException, DirectoryDetectedException {
        if (is == null) throw new NullPointerException("is is null");
        if (out == null) throw new NullPointerException("out is null");
        if (!out.getParentFile().isDirectory()) if (!out.getParentFile().mkdirs()) throw new IOException("Can't create directories");
        if (!out.exists()) {
            copyStreamToFile(is, out, read, write, execute);
        } else {
            String sumIs = null;
            String sumTmp = null;
            File tmpFile = File.createTempFile("tmp", ".tmp");
            copyStreamToFile(is, tmpFile);
            sumIs = checksum(is);
            sumTmp = checksum(new FileInputStream(tmpFile));
            if (sumIs.equals(sumTmp)) {
                if (!tmpFile.delete()) throw new IOException("cannot delete file \"" + tmpFile.getAbsolutePath() + "\"");
            } else {
                if (!out.delete()) throw new IOException("cannot delete file \"" + out.getAbsolutePath() + "\"");
                copyFile(tmpFile, out, read, write, execute);
            }
        }
    }

    public static void copyStreamToFile(InputStream is, File out) throws FileNotFoundException, IOException, DirectoryDetectedException {
        copyStreamToFile(is, out, true, true, false);
    }

    public static void copyStreamToFile(InputStream is, File out, boolean read, boolean write, boolean execute) throws IOException, DirectoryDetectedException {
        File outFile = null;
        if (out.isDirectory()) {
            throw new DirectoryDetectedException();
        } else {
            outFile = out;
        }
        OutputStream os = new FileOutputStream(outFile);
        int c;
        byte block[] = new byte[65536];
        while ((c = is.read(block, 0, 65536)) != -1) {
            os.write(block, 0, c);
        }
        if (c != -1) {
            os.write(block, 0, c);
        }
        os.close();
        outFile.setReadable(read);
        outFile.setWritable(write);
        outFile.setExecutable(execute);
    }

    public static void copyFile(String in, String out) throws FileNotFoundException, IOException {
        copyFile(new File(in), new File(out));
    }

    public static void copyFile(String in, File out) throws FileNotFoundException, IOException {
        copyFile(new File(in), out);
    }

    public static void copyFile(File in, String out) throws FileNotFoundException, IOException {
        copyFile(in, new File(out));
    }

    public static void copyFile(File in, File out) throws FileNotFoundException, IOException {
        copyFile(in, out, true, true, false);
    }

    public static void copyFile(String in, String out, boolean read, boolean write, boolean execute) throws FileNotFoundException, IOException {
        copyFile(new File(in), new File(out), read, write, execute);
    }

    public static void copyFile(String in, File out, boolean read, boolean write, boolean execute) throws FileNotFoundException, IOException {
        copyFile(new File(in), out, read, write, execute);
    }

    public static void copyFile(File in, String out, boolean read, boolean write, boolean execute) throws FileNotFoundException, IOException {
        copyFile(in, new File(out), read, write, execute);
    }

    public static void copyFile(File in, File out, boolean read, boolean write, boolean execute) throws FileNotFoundException, IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        File outFile = null;
        if (out.isDirectory()) {
            outFile = new File(out.getAbsolutePath() + File.separator + in.getName());
        } else {
            outFile = out;
        }
        FileChannel outChannel = new FileOutputStream(outFile).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
            outFile.setReadable(read);
            outFile.setWritable(write);
            outFile.setExecutable(execute);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static String getAppTmpDirname(String... subdirs) {
        String rslt = System.getProperty("java.io.tmpdir") + File.separator + "." + getAppName();
        for (int i = 0; i < subdirs.length; i++) rslt += File.separator + subdirs[i];
        return rslt;
    }

    public static String getAppConfigFilename() {
        String configFilename = "." + getAppName() + "rc";
        return System.getProperty("user.home") + File.separator + configFilename;
    }

    public static String getAppName() {
        return "west";
    }
}
