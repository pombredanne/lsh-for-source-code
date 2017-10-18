package org.nightlabs.jfire.installer.client.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.nightlabs.installer.util.Util;

/**
 * Should work as follows:
 * main [-a allplatformsdir] [-t targetdir] platformdirs...
 *
 * (1) Build full functional platforms if allplatforms is given by taking allplatforms content and copying specific platform over it
 * (2) Compare all platforms. All files that are the same in all platforms go to the new allplatforms folder
 * (3) All files that are not common go to their specific new platform folder.
 *
 *
 * Marc Klinger - marc[at]nightlabs[dot]de
 */
public class BuildPackage {

    private Map<String, String> allDigestsByRelativePath = new HashMap<String, String>();

    private Map<String, Integer> relativeFileOccurencies = new HashMap<String, Integer>();

    private Map<File, String> allPlatformFiles = new HashMap<File, String>();

    private void build(File targetDir, File... platformDirs) throws IOException {
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        File newAllplatformsDir = new File(targetDir, "allplatforms");
        if (newAllplatformsDir.exists()) throw new IOException("dir exists: " + newAllplatformsDir.getAbsolutePath());
        for (File platformDir : platformDirs) {
            File newPlatformDir = new File(targetDir, platformDir.getName());
            if (newPlatformDir.exists()) throw new IOException("dir exists: " + newPlatformDir.getAbsolutePath());
        }
        System.out.println("Copying files...");
        for (File platformDir : platformDirs) {
            File newPlatformDir = new File(targetDir, platformDir.getName());
            copyDirectory(null, platformDir, newPlatformDir);
        }
        md = null;
        System.out.println("Applying diff...");
        for (Map.Entry<String, Integer> entry : relativeFileOccurencies.entrySet()) {
            if (entry.getValue() == platformDirs.length) {
                boolean first = true;
                for (File platformDir : platformDirs) {
                    File newPlatformDir = new File(targetDir, platformDir.getName());
                    File sourceFile = new File(newPlatformDir, entry.getKey());
                    if (first) {
                        File targetFile = new File(newAllplatformsDir, entry.getKey());
                        targetFile.getParentFile().mkdirs();
                        copyFile(sourceFile, targetFile);
                        first = false;
                    }
                    sourceFile.delete();
                }
            }
        }
        System.out.println("Cleaning up...");
        for (File platformDir : platformDirs) {
            File newPlatformDir = new File(targetDir, platformDir.getName());
            File[] files = newPlatformDir.listFiles();
            for (File file : files) deleteDirectoryIfEmpty(file);
        }
    }

    private static void deleteDirectoryIfEmpty(File directory) {
        if (!directory.isDirectory()) return;
        File[] subFiles = directory.listFiles();
        for (File file : subFiles) deleteDirectoryIfEmpty(file);
        if (directory.listFiles().length == 0) {
            directory.delete();
        }
    }

    MessageDigest md;

    /**
	 * Transfer data between streams
	 * @param in The input stream
	 * @param out The output stream
	 * @throws IOException if an error occurs.
	 */
    public String transferStreamData(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException {
        try {
            long inputOffset = 0;
            long inputLen = -1;
            int bytesRead;
            int transferred = 0;
            byte[] buf = new byte[4096];
            if (inputOffset > 0) if (in.skip(inputOffset) != inputOffset) throw new IOException("Input skip failed (offset " + inputOffset + ")");
            while (true) {
                if (inputLen >= 0) bytesRead = in.read(buf, 0, (int) Math.min(buf.length, inputLen - transferred)); else bytesRead = in.read(buf);
                if (bytesRead <= 0) break;
                if (md != null) {
                    if (bytesRead == buf.length) md.update(buf); else md.update(Arrays.copyOf(buf, bytesRead));
                }
                out.write(buf, 0, bytesRead);
                transferred += bytesRead;
                if (inputLen >= 0 && transferred >= inputLen) break;
            }
            out.flush();
            return md != null ? Util.encodeHexStr(md.digest()) : null;
        } finally {
            if (md != null) md.reset();
        }
    }

    /**
	 * Copy a file.
	 * @param sourceFile The source file to copy
	 * @param destinationFile To which file to copy the source
	 * @throws IOException in case of an error
	 */
    public String copyFile(File sourceFile, File destinationFile) throws IOException {
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
            return transferStreamData(source, destination);
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
    public void copyDirectory(File topLevelDirectory, File sourceDirectory, File destinationDirectory) throws IOException {
        if (topLevelDirectory == null) topLevelDirectory = sourceDirectory;
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) throw new IOException("No such source directory: " + sourceDirectory.getAbsolutePath());
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory()) throw new IOException("Destination exists but is not a directory: " + sourceDirectory.getAbsolutePath());
        } else destinationDirectory.mkdirs();
        File[] files = sourceDirectory.listFiles();
        for (File file : files) {
            File destinationFile = new File(destinationDirectory, file.getName());
            if (file.isDirectory()) copyDirectory(topLevelDirectory, file, destinationFile); else {
                String digest = copyFile(file, destinationFile);
                if (digest != null) {
                    String relativePath = getRelativePath(topLevelDirectory, file);
                    allPlatformFiles.put(topLevelDirectory, relativePath);
                    if (!allDigestsByRelativePath.containsKey(relativePath)) allDigestsByRelativePath.put(relativePath, digest);
                    if (allDigestsByRelativePath.get(relativePath).equals(digest)) {
                        allDigestsByRelativePath.put(relativePath, digest);
                        if (!relativeFileOccurencies.containsKey(relativePath)) relativeFileOccurencies.put(relativePath, 1); else relativeFileOccurencies.put(relativePath, relativeFileOccurencies.get(relativePath) + 1);
                    }
                }
            }
        }
    }

    private static String getRelativePath(File dir, File subFile) throws IOException {
        return subFile.getCanonicalPath().substring(dir.getCanonicalPath().length() + 1);
    }

    public static void main(String[] args) throws IOException {
        List<File> dirs = new ArrayList<File>();
        File targetDir = null;
        boolean isTargetDir = false;
        for (String arg : args) {
            if (arg.equals("-t")) {
                isTargetDir = true;
            } else if (isTargetDir) {
                targetDir = new File(arg);
                isTargetDir = false;
            } else {
                File dir = new File(arg);
                dirs.add(dir);
                if (!dir.isDirectory()) throw new IOException("dir does not exist: " + dir.getAbsolutePath());
            }
        }
        if (dirs.size() < 2) throw new IllegalArgumentException("Must have at least 2 source directories");
        if (targetDir == null) targetDir = new File(".");
        BuildPackage buildPackage = new BuildPackage();
        buildPackage.build(targetDir, dirs.toArray(new File[dirs.size()]));
    }
}
