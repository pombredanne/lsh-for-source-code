package jworktime;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collection;

public class MD5Sum {

    public MD5Sum(ResultCallback cb) {
        callback = cb;
    }

    public static final String MD5_DIGEST = "MD5";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
        } else if (args.length == 1) {
            processFile(args[0]);
        } else if (args.length == 2 && args[0].equals("-s")) {
            processString(args[1]);
        } else {
            processFiles(args);
        }
    }

    public static void printUsage() {
        System.out.println("\nMD5 Summe\n");
    }

    public static void processFile(String fName) throws Exception {
        processFile(new File(fName));
    }

    public static void processFile(File file) throws Exception {
        if (file.isDirectory()) {
            processFiles(file.listFiles());
        } else {
            MessageDigest md5 = getMD5ForFile(file);
            displayResult(file, md5.digest());
        }
    }

    public static void processString(String text) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(MD5_DIGEST);
        md5.reset();
        md5.update(text.getBytes());
        displayResult(null, md5.digest());
    }

    public static void processFiles(String[] fNames) throws Exception {
        for (String fName : fNames) {
            processFile(fName);
        }
    }

    public static void processFiles(File[] files) throws Exception {
        for (File file : files) {
            processFile(file);
        }
    }

    public static String digestToHexString(byte[] digest) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            String str = Integer.toHexString(0xFF & digest[i]);
            if (str.length() == 1) {
                hexString.append("0");
                hexString.append(str);
            } else {
                hexString.append(str);
            }
        }
        return hexString.toString();
    }

    public static void displayResult(File f, byte[] digest) {
        if (f == null) {
            System.out.println("MD5: " + digestToHexString(digest));
        } else {
            System.out.println(f + ": " + digestToHexString(digest));
        }
    }

    public static MessageDigest getMD5ForFile(File f) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(MD5_DIGEST);
        md5.reset();
        DigestInputStream dis = new DigestInputStream(new FileInputStream(f), md5);
        dis.on(true);
        byte[] bbuffer = new byte[1024];
        while (dis.read(bbuffer) != -1) ;
        return md5;
    }

    public void process(Collection<File> fs, boolean recursion) {
        for (File f : fs) {
            process(f, recursion);
        }
    }

    public void process(File[] fs, boolean recursion) {
        for (File f : fs) {
            process(f, recursion);
        }
    }

    private void process(File[] fs, int depth, boolean recursion) {
        for (File f : fs) {
            process(f, depth, recursion);
        }
    }

    public void process(File f, boolean recursion) {
        process(f, 0, recursion);
    }

    private void process(File f, int depth, boolean recursion) {
        if (f.isDirectory()) {
            if (depth == 0 || recursion) {
                process(f.listFiles(), ++depth, recursion);
            }
            return;
        }
        try {
            MessageDigest md5 = getMD5ForFile(f);
            callback.display(f, digestToHexString(md5.digest()));
        } catch (Exception ex) {
            callback.display(f, "[failed]");
        }
    }

    public void process(String t) {
        try {
            MessageDigest md5 = MessageDigest.getInstance(MD5_DIGEST);
            md5.reset();
            md5.update(t.getBytes());
            callback.display(null, digestToHexString(md5.digest()));
        } catch (Exception ex) {
            callback.display(null, "[failed]");
        }
    }

    public static interface ResultCallback {

        void display(File f, String md5sum);
    }

    private ResultCallback callback;
}
