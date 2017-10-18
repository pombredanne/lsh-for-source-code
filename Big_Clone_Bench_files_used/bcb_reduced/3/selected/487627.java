package com.troyhigh.njrotc.admintrackerold;

import javolution.xml.XMLObjectWriter;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLBinding;
import javolution.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Provides I/O operations for XML library files
 * <p>Unless explictly stated otherwise, parameters cannot be null.</p>
 * 
 * @author Albert Ou
 * @version 1.0 2008-September-07
 */
public class ATXLibraryDriver {

    private static final File SAVE_DIR = new File("libraries");

    private static File customDir;

    public static final String STUDENT_TYPE = "students";

    public static final String EVENT_TYPE = "events";

    public static final String ROSTER_TYPE = "rosters";

    private static final String LIBRARY_EXT = ".atx";

    private static final String DIGEST_ALGORITHM = "MD5";

    private static final String ENCODING = "UTF-8";

    private static final String DATE_FORMAT = "yyyyMMddhhmmss";

    private static final String S = File.separator;

    private static final File CUSTOM_SETTINGS = new File("settings" + LIBRARY_EXT);

    /**
     * Creates a library file system in the default save directory, if not existing already
     * 
     * <p>It will use the custom save directory if defined.
     * If both the default and custom are invalid, it will ask the user to specify a new valid custom directory.</p>
     */
    public static void init() {
        System.out.println("Attempting to initialize library file system");
        try {
            if (CUSTOM_SETTINGS.canRead() && CUSTOM_SETTINGS.isFile()) customDir = new File((String) readLibrary(CUSTOM_SETTINGS, String.class)); else System.out.println("No custom library settings found");
        } catch (Exception e) {
            e.printStackTrace();
        }
        File root;
        if (customDir == null) root = SAVE_DIR; else root = customDir;
        String[] types = { STUDENT_TYPE, EVENT_TYPE, ROSTER_TYPE };
        boolean isValid;
        do {
            isValid = isValidDirectory(root);
            if (!isValid) {
                selectCustomDirectory();
                root = customDir;
            }
            for (String name : types) {
                isValid = isValidDirectory(new File(root, name));
                if (!isValid) break;
            }
        } while (!isValid);
        saveCustomDirectory();
        try {
            System.out.println("Library file system successfully created in " + root.getCanonicalPath());
        } catch (IOException ioe) {
        }
    }

    /**
     * Writes an XML representation of a given Object as well as the associated checksums and backups
     * 
     * @param   obj     The Object to save
     * @param   output  The output file path
     * @returns True if and only if every write operation is correctly completed; false otherwise
     * 
     * @see {@link #makeLibraryFile(String, String) makeLibraryFile}
     */
    public static boolean writeLibrary(Object obj, File output) {
        XMLObjectWriter writer = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            writer = XMLObjectWriter.newInstance(new BufferedOutputStream(new DigestOutputStream(new FileOutputStream(output), md)), ENCODING);
            writer.setIndentation("\t");
            XMLBinding binding = new XMLBinding();
            binding.setAlias(Student.class, "Student");
            binding.setAlias(Event.class, "Event");
            writer.setBinding(binding);
            writer.write(obj);
        } catch (Exception e) {
            try {
                System.out.println("Write operation for " + output.getCanonicalPath() + " failed");
            } catch (IOException ioe) {
            }
            e.printStackTrace();
            return false;
        } finally {
            close(writer);
        }
        File checksumFile = makeChecksumFile(output);
        FileOutputStream mdStream = null;
        try {
            mdStream = new FileOutputStream(checksumFile);
            mdStream.write(md.digest());
        } catch (Exception e) {
            System.out.println("Write operation for associated checksum file failed");
            e.printStackTrace();
            return false;
        } finally {
            close(mdStream);
        }
        File backupLibrary = makeLibraryBackupFile(output);
        try {
            File backupDirectory = backupLibrary.getParentFile();
            if (!backupDirectory.exists()) backupDirectory.mkdir();
            copyFile(output, backupLibrary);
            copyFile(checksumFile, makeChecksumFile(backupLibrary));
        } catch (Exception e) {
            System.out.println("Write operation for backup files failed");
            e.printStackTrace();
            return false;
        }
        try {
            System.out.println("Write operation for " + output.getCanonicalPath() + " succeeded");
        } catch (IOException ioe) {
        }
        return true;
    }

    /**
     * Prepares a File object for saving a library; it attempts to choose an appropriate location based on the type of library specified.
     * 
     * <p>Conditions such as access privileges are checked. The custom save directory is used if defined; otherwise it will use the default directory.
     * If both the default and custom are invalid, it will ask the user to specify a new valid custom directory.
     * Note that this will not create an actual file on the disk, only an object for later use with the {@link #writeLibrary(Object, File) writeLibrary} method.</p>
     * 
     * @param   basename    The basename of the library without the file extension
     * @param   type        The type of library (Valid arguments: {@link #STUDENT_TYPE STUDENT_TYPE}, {@link #EVENT_TYPE EVENT_TYPE}, {@link #ROSTER_TYPE ROSTER_TYPE}
     * @throws  IllegalArgumentException    If parameter <code>type</code> is not one of the values listed
     * 
     * @return  A valid File object
     */
    public static File makeLibraryFile(String basename, String type) {
        if (type == null || (!type.equals(STUDENT_TYPE) && !type.equals(EVENT_TYPE) && !type.equals(ROSTER_TYPE))) throw new IllegalArgumentException("Invalid library type");
        File dir;
        boolean isValid;
        do {
            if (customDir != null) dir = new File(customDir, type); else dir = new File(SAVE_DIR, type);
            isValid = isValidDirectory(dir);
            if (!isValid) selectCustomDirectory();
        } while (!isValid);
        init();
        saveCustomDirectory();
        return new File(dir, basename + LIBRARY_EXT);
    }

    /**
     * Checks access conditions to see whether the given File object is a valid directory for writing library files
     * 
     * @param   dir     The directory to check
     * @return  True if and only if the directory is valid for write operations
     */
    private static boolean isValidDirectory(File dir) {
        boolean isValid = true;
        try {
            if (!dir.exists()) isValid = dir.mkdirs();
            if (!dir.isDirectory()) {
                System.out.println(dir.getPath() + " is not a directory");
                isValid = false;
            } else if (!dir.canRead()) {
                System.out.println("Denied read access to " + dir.getPath());
                isValid = false;
            } else if (!dir.canWrite()) {
                System.out.println("Denied write access to " + dir.getPath());
                isValid = false;
            }
        } catch (SecurityException e) {
            System.out.println("A Java security manager is denying access rights.");
            e.printStackTrace();
            return false;
        } finally {
            return isValid;
        }
    }

    /**
     * Sets the custom save directory to a user-specified path (i.e., such as if the default path is invalid)
     */
    private static void selectCustomDirectory() {
        ATXFileChooser.selectSaveDirectory();
    }

    /**
     * Reads and instantiates an Object using its XML representation from a library file
     * 
     * <p>Conditions such as access rights and data integrity (through checksum comparisons) are considered.
     * If the main library file is found to be invalid, backup versions are searched and tested, beginning with the most recent, until a prospective candidate is discovered.
     * If none is available, the process aborts.</p>
     *
     * @param   input   The main library file to read
     * @param   cls     The class that the client expects the library's Object to belong
     * @return  The Object the library contains, with the same runtime type of the original Object; otherwise null if the input File does not exist or cannot be read, or if the expect class does not match the runtime class of the library's Object
     */
    public static <T extends Object> T readLibrary(File input, Class cls) {
        if (input == null) {
            System.out.println("Selected file is a null argument (programming error); now aborting");
            return null;
        }
        try {
            System.out.println("Attempting to load " + input.getCanonicalPath());
        } catch (IOException e) {
        }
        boolean searchForBackup = false;
        byte[] originalDigest;
        byte[] newDigest;
        try {
            if (!input.exists()) {
                System.out.println(input.getName() + " does not exist");
                searchForBackup = true;
            } else if (!input.isFile()) {
                System.out.println(input.getName() + " is not a file; now aborting");
                return null;
            } else if (!input.canRead()) {
                System.out.println(input.getName() + " cannot be read");
                searchForBackup = true;
            } else {
                originalDigest = readChecksumFile(input);
                newDigest = calculateChecksum(input);
                if (originalDigest == null || newDigest == null || !MessageDigest.isEqual(originalDigest, newDigest)) {
                    System.out.println("Integrity of main file " + input.getName() + " may be compromised: checksums do not match");
                    searchForBackup = true;
                }
            }
        } catch (SecurityException e) {
            System.out.println("A Java security manager is denying access rights");
            searchForBackup = true;
        }
        File main;
        if (searchForBackup) {
            System.out.println("Attempting to search for backup versions");
            File backupDir = makeBackupDirectory(input);
            File[] list = listLibraries(backupDir);
            if (list == null) {
                System.out.println("No valid backup directory found; now aborting");
                return null;
            }
            int index = list.length - 1;
            do {
                main = list[index];
                originalDigest = readChecksumFile(main);
                newDigest = calculateChecksum(main);
                index--;
            } while (index >= 0 && (originalDigest == null || newDigest == null || !MessageDigest.isEqual(originalDigest, newDigest)));
            if (index < 0) {
                try {
                    System.out.println("Backup directory " + backupDir.getCanonicalPath() + " found but no valid backup files; now aborting");
                } catch (IOException e) {
                }
                return null;
            }
            try {
                System.out.println("Prospective backup file found: " + main.getCanonicalPath());
            } catch (IOException e) {
            }
            System.out.println(main.getName() + " originates from revision " + (index + 2) + " (of " + list.length + " found in total)");
        } else {
            main = input;
            System.out.println(main.getName() + " successfully passed integrity check");
        }
        XMLObjectReader reader = null;
        try {
            reader = XMLObjectReader.newInstance(new BufferedInputStream(new FileInputStream(main)));
            Object obj = reader.read();
            if (cls != obj.getClass()) {
                System.out.println("Class of the library's Object does not match the expected class (programming error); now aborting");
                return null;
            }
            System.out.println("Library " + main.getName() + " successfully loaded");
            return (T) obj;
        } catch (Exception e) {
            System.out.println("Error loading " + main.getName() + "; now aborting");
            e.printStackTrace();
            return null;
        } finally {
            close(reader);
        }
    }

    /**
     * Lists prospective library files in a given directory
     * 
     * @param   dir     The directory to search
     * @return  An array of prospective library files, sorted lexicographically; otherwise null if directory is nonexistent or read/write access is denied
     */
    public static File[] listLibraries(File dir) {
        try {
            if (!dir.isDirectory() || !dir.canRead()) return null;
            File[] all = dir.listFiles();
            if (all == null) return null;
            ArrayList<File> filter = new ArrayList<File>(all.length / 2);
            for (File f : all) {
                if (f.isFile() && f.canRead() && f.getName().endsWith(LIBRARY_EXT)) filter.add(f);
            }
            File[] libraries = new File[filter.size()];
            libraries = filter.toArray(libraries);
            Arrays.sort(libraries);
            return libraries;
        } catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lists all prospective library files of a given type, searching the default save directory if a custom directory is not specified
     * 
     * @param   type    The type of library (Valid arguments: {@link #STUDENT_TYPE STUDENT_TYPE}, {@link #EVENT_TYPE EVENT_TYPE}, {@link #ROSTER_TYPE ROSTER_TYPE}
     * @throws  IllegalArgumentException    If parameter <code>type</code> is not one of the values listed
     * 
     * @return  An array of prospective library files, sorted lexicographically; otherwise null if directory is nonexistent or read/write access is denied
     */
    public static File[] listLibraries(String type) {
        if (type == null || (!type.equals(STUDENT_TYPE) && !type.equals(EVENT_TYPE) && !type.equals(ROSTER_TYPE))) throw new IllegalArgumentException("Invalid library type");
        if (customDir == null) return listLibraries(new File(SAVE_DIR, type)); else return listLibraries(new File(customDir, type));
    }

    /**
     * Generates a checksum File object that corresponds to the given library File
     *
     * @param   f   The library file with which to calculate the checksum
     * @return  The File representing the checksum
     */
    private static File makeChecksumFile(File f) {
        File mdFile;
        String mdFileName = f.getName() + '.' + DIGEST_ALGORITHM;
        if (f.getParent() == null) mdFile = new File(mdFileName); else mdFile = new File(f.getParentFile(), mdFileName);
        return mdFile;
    }

    /**
     * Reads an existing checksum file associated with the given library File
     * 
     * @param   f   The library file
     * @return  The message digest in binary, otherwise null if the checksum File does not exist, is invalid, or read access is denied
     */
    private static byte[] readChecksumFile(File f) {
        File checksumFile = makeChecksumFile(f);
        FileInputStream in = null;
        byte[] digest;
        try {
            if (!checksumFile.isFile() || !checksumFile.canRead()) return null;
            in = new FileInputStream(checksumFile);
            int length = in.available();
            if (length != MessageDigest.getInstance(DIGEST_ALGORITHM).getDigestLength()) return null;
            digest = new byte[length];
            in.read(digest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(in);
        }
        return digest;
    }

    /**
     * Reads a File and returns its checksum using the default algorithm
     * 
     * @param   f   The File with which to calculate a checksum
     * @return  The message digest in binary, otherwise null if the file does not exist
     */
    private static byte[] calculateChecksum(File f) {
        DigestInputStream in = null;
        byte[] digest;
        try {
            if (!f.isFile() || !f.canRead()) return null;
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            in = new DigestInputStream(new FileInputStream(f), md);
            byte[] buffer = new byte[2048];
            int read;
            do {
                read = in.read(buffer);
            } while (read > 0);
            digest = md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(in);
        }
        return digest;
    }

    /**
     * Generates a timestamped backup File object that corresponds to the given library File
     *
     * @param   f   The library file to backup
     * @return  The File representing the backup
     */
    private static File makeLibraryBackupFile(File f) {
        String bkFileName = f.getName();
        String extension = bkFileName.substring(bkFileName.lastIndexOf('.'));
        bkFileName = bkFileName.substring(0, bkFileName.length() - extension.length());
        bkFileName += S + bkFileName + "_";
        bkFileName += (new SimpleDateFormat(DATE_FORMAT)).format(Calendar.getInstance().getTime());
        bkFileName += extension;
        File bkFile;
        bkFile = new File(f.getParentFile(), bkFileName);
        return bkFile;
    }

    /**
     * Generates a File object that corresponds to the backup directory of the given library File
     * 
     * @param   f   The library file
     * @return  The File representing the backup directory
     */
    private static File makeBackupDirectory(File f) {
        String bkDirName = f.getName();
        String extension = bkDirName.substring(bkDirName.lastIndexOf('.'));
        bkDirName = bkDirName.substring(0, bkDirName.length() - extension.length());
        File bkDir;
        if (f.getParent() == null) bkDir = new File(bkDirName); else bkDir = new File(f.getParentFile(), bkDirName);
        return bkDir;
    }

    /**
     * Emergency method to close an InputStream; useful in case an exception occurs
     * @param   in      The InputStream to close
     */
    private static void close(InputStream in) {
        if (in == null) return;
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Emergency method to close an OutputStream; useful in case an exception occurs
     * @param   out     The OutputStream to close
     */
    private static void close(OutputStream out) {
        if (out == null) return;
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Emergency method to close an XMLObjectWriter; useful in case an exception occurs
     * @param   writer  The XMLObjectWriter to close
     */
    private static void close(XMLObjectWriter writer) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    /**
     * Emergency method to close an XMLObjectReader; useful in case an exception occurs
     * @param   writer  The XMLObjectReader to close
     */
    private static void close(XMLObjectReader reader) {
        if (reader == null) return;
        try {
            reader.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic file copy operation
     * 
     * @param   source      Source file
     * @param   destination Destination file
     */
    private static void copyFile(File source, File destination) throws IOException, SecurityException {
        if (!destination.exists()) destination.createNewFile();
        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destinationChannel = new FileOutputStream(destination).getChannel();
            long count = 0;
            long size = sourceChannel.size();
            while ((count += destinationChannel.transferFrom(sourceChannel, 0, size - count)) < size) ;
        } finally {
            if (sourceChannel != null) sourceChannel.close();
            if (destinationChannel != null) destinationChannel.close();
        }
    }

    /**
     * Saves the custom directory path for future use
     */
    private static void saveCustomDirectory() {
        try {
            if (customDir != null) writeLibrary(customDir.getPath(), CUSTOM_SETTINGS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows FileChooser to set the custom save directory
     * @param   dir     The new custom save directory
     */
    protected static void setCustomDirectory(File dir) {
        customDir = dir;
    }

    public static void test() {
        Student stu = new Student(10, "NGUYEN", "QUAN", 8472);
        ArrayList<Student> list = new ArrayList<Student>();
        for (int i = 0; i < 200; i++) list.add(stu);
        writeLibrary(list, makeLibraryFile("student_list", STUDENT_TYPE));
    }

    public static void test2() {
        ArrayList<Student> list = readLibrary(new File(SAVE_DIR + S + STUDENT_TYPE + S + "student_list.atx"), ArrayList.class);
    }
}
