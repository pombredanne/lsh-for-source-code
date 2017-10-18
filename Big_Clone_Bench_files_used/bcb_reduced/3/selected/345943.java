package org.sf.jlaunchpad.util;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import java.text.ParseException;

/**
 * Used to create or verify file checksums.
 *
 * @ant.task category="control"
 * @since Ant 1.5
 */
public class Checksum {

    /**
   * File for which checksum is to be calculated.
   */
    private File file = null;

    /**
   * Root directory in which the checksum files will be written.
   * If not specified, the checksum files will be written
   * in the same directory as each file.
   */
    private File todir;

    /**
   * MessageDigest algorithm to be used.
   */
    private String algorithm = "MD5";

    /**
   * MessageDigest Algorithm provider
   */
    private String provider = null;

    /**
   * File Extension that is be to used to create or identify
   * destination file
   */
    private String fileext;

    /**
   * Holds generated checksum and gets set as a Project Property.
   */
    private String property;

    /**
   * Holds checksums for all files (both calculated and cached on disk).
   * Key:   java.util.File (source file)
   * Value: java.lang.String (digest)
   */
    private Map allDigests = new HashMap();

    /**
   * Holds relative file names for all files (always with a forward slash).
   * This is used to calculate the total hash.
   * Key:   java.util.File (source file)
   * Value: java.lang.String (relative file name)
   */
    private Map relativeFilePaths = new HashMap();

    /**
   * Property where totalChecksum gets set.
   */
    private String totalproperty;

    /**
   * Whether or not to create a new file.
   * Defaults to <code>false</code>.
   */
    private boolean forceOverwrite;

    /**
   * Contains the result of a checksum verification. ("true" or "false")
   */
    private String verifyProperty;

    /**
   * Stores SourceFile, DestFile pairs and SourceFile, Property String pairs.
   */
    private Hashtable includeFileMap = new Hashtable();

    /**
   * Message Digest instance
   */
    private MessageDigest messageDigest;

    /**
   * is this task being used as a nested condition element?
   */
    private boolean isCondition;

    /**
   * Size of the read buffer to use.
   */
    private int readBufferSize = 8 * 1024;

    /**
   * Formater for the checksum file.
   */
    private MessageFormat format = FormatElement.getDefault().getFormat();

    /**
   * Sets the file for which the checksum is to be calculated.
   *
   * @param file a <code>File</code> value
   */
    public void setFile(File file) {
        this.file = file;
    }

    /**
   * Sets the root directory where checksum files will be
   * written/read
   *
   * @param todir the directory to write to
   * @since Ant 1.6
   */
    public void setTodir(File todir) {
        this.todir = todir;
    }

    /**
   * Specifies the algorithm to be used to compute the checksum.
   * Defaults to "MD5". Other popular algorithms like "SHA" may be used as well.
   *
   * @param algorithm a <code>String</code> value
   */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
   * Sets the MessageDigest algorithm provider to be used
   * to calculate the checksum.
   *
   * @param provider a <code>String</code> value
   */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
   * Sets the file extension that is be to used to
   * create or identify destination file.
   *
   * @param fileext a <code>String</code> value
   */
    public void setFileext(String fileext) {
        this.fileext = fileext;
    }

    /**
   * Sets the property to hold the generated checksum.
   *
   * @param property a <code>String</code> value
   */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
   * Sets the property to hold the generated total checksum
   * for all files.
   *
   * @param totalproperty a <code>String</code> value
   * @since Ant 1.6
   */
    public void setTotalproperty(String totalproperty) {
        this.totalproperty = totalproperty;
    }

    /**
   * Sets the verify property.  This project property holds
   * the result of a checksum verification - "true" or "false"
   *
   * @param verifyProperty a <code>String</code> value
   */
    public void setVerifyproperty(String verifyProperty) {
        this.verifyProperty = verifyProperty;
    }

    /**
   * Whether or not to overwrite existing file irrespective of
   * whether it is newer than
   * the source file.  Defaults to false.
   *
   * @param forceOverwrite a <code>boolean</code> value
   */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
   * The size of the read buffer to use.
   *
   * @param size an <code>int</code> value
   */
    public void setReadBufferSize(int size) {
        this.readBufferSize = size;
    }

    /**
   * Select the in/output pattern via a well know format name.
   *
   * @param e an <code>enumerated</code> value
   * @since 1.7.0
   */
    public void setFormat(FormatElement e) {
        format = e.getFormat();
    }

    /**
   * Specify the pattern to use as a MessageFormat pattern.
   * <p/>
   * <p>{0} gets replaced by the checksum, {1} by the filename.</p>
   *
   * @param p a <code>String</code> value
   * @since 1.7.0
   */
    public void setPattern(String p) {
        format = new MessageFormat(p);
    }

    /**
   * Calculate the checksum(s).
   *
   */
    public void execute() {
        isCondition = false;
        boolean value = validateAndExecute();
    }

    /**
   * Validate attributes and get down to business.
   */
    private boolean validateAndExecute() {
        String savedFileExt = fileext;
        if (file != null && file.exists() && file.isDirectory()) {
            throw new RuntimeException("Checksum cannot be generated for directories");
        }
        if (file != null && totalproperty != null) {
            throw new RuntimeException("File and Totalproperty cannot co-exist.");
        }
        if (property != null && fileext != null) {
            throw new RuntimeException("Property and FileExt cannot co-exist.");
        }
        if (property != null) {
            if (forceOverwrite) {
                throw new RuntimeException("ForceOverwrite cannot be used when Property is specified");
            }
            int ct = 0;
            if (file != null) {
                ct++;
            }
            if (ct > 1) {
                throw new RuntimeException("Multiple files cannot be used when Property is specified");
            }
        }
        if (verifyProperty != null) {
            isCondition = true;
        }
        if (verifyProperty != null && forceOverwrite) {
            throw new RuntimeException("VerifyProperty and ForceOverwrite cannot co-exist.");
        }
        if (isCondition && forceOverwrite) {
            throw new RuntimeException("ForceOverwrite cannot be used when conditions are being used.");
        }
        messageDigest = null;
        if (provider != null) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm, provider);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new RuntimeException(noalgo);
            } catch (NoSuchProviderException noprovider) {
                throw new RuntimeException(noprovider);
            }
        } else {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new RuntimeException(noalgo);
            }
        }
        if (messageDigest == null) {
            throw new RuntimeException("Unable to create Message Digest");
        }
        if (fileext == null) {
            fileext = "." + algorithm;
        } else if (fileext.trim().length() == 0) {
            throw new RuntimeException("File extension when specified must not be an empty string");
        }
        try {
            if (file != null) {
                if (totalproperty != null || todir != null) {
                    relativeFilePaths.put(file, file.getName().replace(File.separatorChar, '/'));
                }
                addToIncludeFileMap(file);
            }
            return generateChecksums();
        } finally {
            fileext = savedFileExt;
            includeFileMap.clear();
        }
    }

    /**
   * Add key-value pair to the hashtable upon which
   * to later operate upon.
   */
    private void addToIncludeFileMap(File file) throws RuntimeException {
        if (file.exists()) {
            if (property == null) {
                File checksumFile = getChecksumFile(file);
                if (forceOverwrite || isCondition || (file.lastModified() > checksumFile.lastModified())) {
                    includeFileMap.put(file, checksumFile);
                } else {
                    if (totalproperty != null) {
                        String checksum = readChecksum(checksumFile);
                        byte[] digest = decodeHex(checksum.toCharArray());
                        allDigests.put(file, digest);
                    }
                }
            } else {
                includeFileMap.put(file, property);
            }
        } else {
            String message = "Could not find file " + file.getAbsolutePath() + " to generate checksum for.";
            throw new RuntimeException(message);
        }
    }

    private File getChecksumFile(File file) {
        File directory;
        if (todir != null) {
            String path = (String) relativeFilePaths.get(file);
            if (path == null) {
                throw new RuntimeException("Internal error: " + "relativeFilePaths could not match file" + file + "\n" + "please file a bug report on this");
            }
            directory = new File(todir, path).getParentFile();
            directory.mkdirs();
        } else {
            directory = file.getParentFile();
        }
        return new File(directory, file.getName() + fileext);
    }

    /**
   * Generate checksum(s) using the message digest created earlier.
   */
    private boolean generateChecksums() {
        boolean checksumMatches = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        byte[] buf = new byte[readBufferSize];
        try {
            for (Enumeration e = includeFileMap.keys(); e.hasMoreElements(); ) {
                messageDigest.reset();
                File src = (File) e.nextElement();
                if (!isCondition) {
                }
                fis = new FileInputStream(src);
                DigestInputStream dis = new DigestInputStream(fis, messageDigest);
                while (dis.read(buf, 0, readBufferSize) != -1) {
                }
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest();
                if (totalproperty != null) {
                    allDigests.put(src, fileDigest);
                }
                String checksum = createDigestString(fileDigest);
                Object destination = includeFileMap.get(src);
                if (destination instanceof java.lang.String) {
                    String prop = (String) destination;
                    if (isCondition) {
                        checksumMatches = checksumMatches && checksum.equals(property);
                    } else {
                    }
                } else if (destination instanceof java.io.File) {
                    if (isCondition) {
                        File existingFile = (File) destination;
                        if (existingFile.exists()) {
                            try {
                                String suppliedChecksum = readChecksum(existingFile);
                                checksumMatches = checksumMatches && checksum.equals(suppliedChecksum);
                            } catch (Exception be) {
                                checksumMatches = false;
                            }
                        } else {
                            checksumMatches = false;
                        }
                    } else {
                        File dest = (File) destination;
                        fos = new FileOutputStream(dest);
                        fos.write(format.format(new Object[] { checksum, src.getName() }).getBytes());
                        fos.write(System.getProperty("line.separator").getBytes());
                        fos.close();
                        fos = null;
                    }
                }
            }
            if (totalproperty != null) {
                Set keys = allDigests.keySet();
                Object[] keyArray = keys.toArray();
                Arrays.sort(keyArray);
                messageDigest.reset();
                for (int i = 0; i < keyArray.length; i++) {
                    File src = (File) keyArray[i];
                    byte[] digest = (byte[]) allDigests.get(src);
                    messageDigest.update(digest);
                    String fileName = (String) relativeFilePaths.get(src);
                    messageDigest.update(fileName.getBytes());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close(fis);
            close(fos);
        }
        return checksumMatches;
    }

    /**
   * Close a Writer without throwing any exception if something went wrong.
   * Do not attempt to close it if the argument is null.
   * @param device output writer, can be null.
   */
    public static void close(Writer device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }

    /**
   * Close a stream without throwing any exception if something went wrong.
   * Do not attempt to close it if the argument is null.
   *
   * @param device Reader, can be null.
   */
    public static void close(Reader device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }

    public static void close(OutputStream device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }

    /**
   * Close a stream without throwing any exception if something went wrong.
   * Do not attempt to close it if the argument is null.
   *
   * @param device stream, can be null.
   */
    public static void close(InputStream device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }

    private String createDigestString(byte[] fileDigest) {
        StringBuffer checksumSb = new StringBuffer();
        for (int i = 0; i < fileDigest.length; i++) {
            String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);
            if (hexStr.length() < 2) {
                checksumSb.append("0");
            }
            checksumSb.append(hexStr);
        }
        return checksumSb.toString();
    }

    /**
   * Converts an array of characters representing hexadecimal values into an
   * array of bytes of those same values. The returned array will be half the
   * length of the passed array, as it takes two characters to represent any
   * given byte. An exception is thrown if the passed char array has an odd
   * number of elements.
   * <p/>
   * NOTE: This code is copied from jakarta-commons codec.
   *
   * @param data an array of characters representing hexadecimal values
   * @return the converted array of bytes
   * @throws Exception on error
   */
    public static byte[] decodeHex(char[] data) {
        int l = data.length;
        if ((l & 0x01) != 0) {
            throw new RuntimeException("odd number of characters.");
        }
        byte[] out = new byte[l >> 1];
        for (int i = 0, j = 0; j < l; i++) {
            int f = Character.digit(data[j++], 16) << 4;
            f = f | Character.digit(data[j++], 16);
            out[i] = (byte) (f & 0xFF);
        }
        return out;
    }

    /**
   * reads the checksum from a file using the specified format.
   *
   * @since 1.7
   */
    private String readChecksum(File f) {
        BufferedReader diskChecksumReader = null;
        try {
            diskChecksumReader = new BufferedReader(new FileReader(f));
            Object[] result = format.parse(diskChecksumReader.readLine());
            if (result == null || result.length == 0 || result[0] == null) {
                throw new RuntimeException("failed to find a checksum");
            }
            return (String) result[0];
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read checksum file " + f, e);
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't read checksum file " + f, e);
        } finally {
            close(diskChecksumReader);
        }
    }
}
