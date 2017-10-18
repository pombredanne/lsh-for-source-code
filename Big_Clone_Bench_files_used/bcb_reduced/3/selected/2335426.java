package com.ericdaugherty.mail;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import com.ericdaugherty.mail.server.utils.FileUtils;

/**
 *
 * @author mfg8876
 */
public class Utils {

    public static void ensureAllAcountMailDirsExist(File mailDir) {
        File dir = new File(mailDir, "1");
        if (!dir.exists()) dir.mkdir();
        dir = new File(mailDir, "temp");
        if (!dir.exists()) dir.mkdir();
        dir = new File(mailDir, "pop3server");
        if (!dir.exists()) dir.mkdir();
        createPop3Dirs(dir, "1");
        createPop3Dirs(dir, "2");
        createPop3Dirs(dir, "3");
        createPop3Dirs(dir, "4");
        createPop3Dirs(dir, "5");
    }

    private static void createPop3Dirs(File pop3Dir, String subDir) {
        File dir = new File(pop3Dir, subDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir = new File(pop3Dir, subDir + File.separator + "pop3" + subDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static void copyFiles(File directory, File target) throws IOException {
        File[] allTestFiles = directory.listFiles();
        for (int i = 0; i < allTestFiles.length; i++) {
            if (allTestFiles[i].isDirectory()) {
                String newSubDir = allTestFiles[i].getPath();
                newSubDir = newSubDir.substring(newSubDir.lastIndexOf(File.separator) + 1);
                File targetDir = new File(target, newSubDir);
                targetDir.mkdir();
                copyFiles(allTestFiles[i], targetDir);
            } else {
                FileUtils.copyFile(allTestFiles[i], new File(target, allTestFiles[i].getName()));
            }
        }
    }

    public static void deleteFiles(File directory) throws IOException {
        File[] allTestFiles = directory.listFiles();
        for (int i = 0; i < allTestFiles.length; i++) {
            if (allTestFiles[i].isDirectory()) {
                deleteFiles(allTestFiles[i]);
                allTestFiles[i].delete();
            } else {
                allTestFiles[i].delete();
            }
        }
    }

    public static byte[] getOriginalMD5(File input) throws IOException {
        InputStream is = new FileInputStream(input);
        byte[] result = new byte[16];
        int count = 0;
        try {
            int nextByte;
            for (; count < 16; ) {
                nextByte = is.read();
                result[count++] = (byte) (nextByte & 0xff);
            }
            return result;
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static byte[] getDerivedMD5(File input) throws IOException, GeneralSecurityException {
        InputStream is = new FileInputStream(input);
        MessageDigest md = MessageDigest.getInstance("MD5");
        try {
            int nextByte;
            outer: while ((nextByte = is.read()) != -1) {
                if (nextByte == 0x0a || nextByte == 0x0d) continue;
                for (int i = 0; i < initialField.length; i++) {
                    if (nextByte != initialField[i]) continue outer;
                    if ((nextByte = is.read()) == -1) {
                        throw new IOException("Reached end of file before discovering initial field");
                    }
                }
                for (int i = 0; i < initialField.length; i++) {
                    md.update((byte) (initialField[i] & 0xff));
                }
                break;
            }
            while ((nextByte = is.read()) != -1) {
                if (nextByte == 0x0a || nextByte == 0x0d) continue;
                md.update((byte) (nextByte & 0xff));
            }
            return md.digest();
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
    }

    private static final byte[] initialField = "X-Priority: Normal".getBytes();
}
