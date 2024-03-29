package edu.rice.cs.util;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.security.*;

/** Create a property file with MD5 checksums
  *  @version $Id: MD5ChecksumProperties.java 5236 2010-04-27 01:43:36Z mgricken $
  */
public class MD5ChecksumProperties extends Properties {

    public static final int BUFFER_SIZE = 10 * 1024;

    public MD5ChecksumProperties() {
        super();
    }

    public MD5ChecksumProperties(Properties p) {
        super(p);
    }

    /**
   * Return the MD5 checksum for the data in the stream, while copying the data
   * into the output stream. The output stream is not closed.
   * @param is input stream
   * @param os output stream (or null if no copying desired)
   * @return MD5 checksum
   */
    public static byte[] getMD5(InputStream is, OutputStream os) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(new BufferedInputStream(is), digest);
            BufferedOutputStream bos = null;
            if (os != null) {
                bos = new BufferedOutputStream(os);
            }
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            while ((bytesRead = dis.read(buf, 0, BUFFER_SIZE)) != -1) {
                if (os != null) {
                    bos.write(buf, 0, bytesRead);
                }
            }
            if (os != null) {
                bos.flush();
            }
            dis.close();
            is.close();
            return digest.digest();
        } catch (NoSuchAlgorithmException nsae) {
            throw new UnexpectedException(nsae, "MD5 algorithm not available");
        }
    }

    /**
   * Return the MD5 checksum for the data in the stream
   * @param is input stream
   * @return MD5 checksum
   */
    public static byte[] getMD5(InputStream is) throws IOException {
        return getMD5(is, null);
    }

    /**
   * Return the MD5 checksum as string for the data in the stream, while
   * copying the data into the output stream. The output stream is not closed.
   * @param is input stream
   * @param os output stream (or null if no copying desired)
   * @return MD5 checksum string
   */
    public static String getMD5String(InputStream is, OutputStream os) throws IOException {
        byte[] messageDigest = getMD5(is, os);
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < messageDigest.length; i++) {
            String oneByte = "0" + Integer.toHexString(0xFF & messageDigest[i]);
            hexString.append(oneByte.substring(oneByte.length() - 2, oneByte.length()));
        }
        return hexString.toString();
    }

    /**
   * Return the MD5 checksum as string for the data in the stream.
   * @param is input stream
   * @return MD5 checksum string
   */
    public static String getMD5String(InputStream is) throws IOException {
        return getMD5String(is, null);
    }

    public static byte[] getMD5(File f) throws IOException {
        return getMD5(new FileInputStream(f));
    }

    public static String getMD5String(File f) throws IOException {
        return getMD5String(new FileInputStream(f));
    }

    public static byte[] getMD5(byte[] b) throws IOException {
        return getMD5(new ByteArrayInputStream(b));
    }

    public static String getMD5String(byte[] b) throws IOException {
        return getMD5String(new ByteArrayInputStream(b));
    }

    /** Add the MD5 checksum for the data in the input stream to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param is input stream with the data
    * @param os output stream to copy to (or null if not wanted)
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, InputStream is, OutputStream os) throws IOException {
        String md5 = getMD5String(is, os);
        Object prev = setProperty(key, md5);
        return ((prev == null) || (prev.equals(md5)));
    }

    /** Add the MD5 checksum for the data in the input stream to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param is input stream with the data
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, InputStream is) throws IOException {
        return addMD5(key, is, null);
    }

    /** Add the MD5 checksum for the data in the file to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param f file with the data
    * @param os output stream to copy to (or null if not wanted)
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, File f, OutputStream os) throws IOException {
        return addMD5(key, new FileInputStream(f), os);
    }

    /** Add the MD5 checksum for the data in the file to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param f file with the data
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, File f) throws IOException {
        return addMD5(key, f, null);
    }

    /** Add the MD5 checksum for the data in the byte array to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param b byte array with the data
    * @param os output stream to copy to (or null if not wanted)
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, byte[] b, OutputStream os) throws IOException {
        return addMD5(key, new ByteArrayInputStream(b), os);
    }

    /** Add the MD5 checksum for the data in the byte array to the
    * properties, using the specified key.
    * @param key key to store the checksum under
    * @param b byte array with the data
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(String key, byte[] b) throws IOException {
        return addMD5(key, b, null);
    }

    /** Add the MD5 checksum for the data in the input stream to the
    * properties, using the name of the file as key.
    * @param f file with the data
    * @param os output stream to copy to (or null if not wanted)
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(File f, OutputStream os) throws IOException {
        return addMD5(f.getPath().replace('\\', '/'), f, os);
    }

    /** Add the MD5 checksum for the data in the input stream to the
    * properties, using the name of the file as key.
    * @param f file with the data
    * @return false if the new MD5 checksum didn't match an existing checksum */
    public boolean addMD5(File f) throws IOException {
        return addMD5(f, null);
    }

    /** Main method. Usage:
    * no arguments --> input file list from standard in, output properties to standard out
    * <file1> --> input file list from standard in, append output properties to file1
    * <file1> <file2> --> input file list from file1, append output properties to file 2
    */
    public static void main(String[] args) throws IOException {
        InputStream is = System.in;
        OutputStream os = System.out;
        Properties prevp = new Properties();
        if (args.length == 2) {
            File outFile = new File(args[1]);
            if (outFile.exists()) {
                FileInputStream pis = new FileInputStream(outFile);
                prevp.load(pis);
                pis.close();
            }
            is = new FileInputStream(new File(args[0]));
            os = new FileOutputStream(outFile);
        } else if (args.length == 1) {
            File outFile = new File(args[0]);
            if (outFile.exists()) {
                FileInputStream pis = new FileInputStream(outFile);
                prevp.load(pis);
                pis.close();
            }
            os = new FileOutputStream(outFile);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        MD5ChecksumProperties p = new MD5ChecksumProperties();
        p.putAll(prevp);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.equals("")) break;
            p.addMD5(new File(line));
        }
        p.store(os, "MD5 Checksums");
    }
}
