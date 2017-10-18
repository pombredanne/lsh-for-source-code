package edu.vt.middleware.crypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import edu.vt.middleware.crypt.digest.MD5;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 * Helps with naming and creating file-based streams.
 *
 * @author  Middleware Services
 * @version  $Revision: 12 $
 */
public class FileHelper {

    /** Protected constructor of utility class. */
    protected FileHelper() {
    }

    /**
   * Gets a file name that includes all the given parameters.
   *
   * @param  basename  Base name of file.
   * @param  algorithm  Cryptographic algorithm.
   * @param  type  Type of data in file. May be null to indicate raw data.
   *
   * @return  File name.
   */
    public static String getFileName(final String basename, final Algorithm algorithm, final String type) {
        final StringBuffer sb = new StringBuffer();
        sb.append("target/test-output/");
        sb.append(basename);
        sb.append('-');
        sb.append(algorithm.getAlgorithm());
        if (algorithm instanceof EncryptionAlgorithm) {
            final EncryptionAlgorithm encAlg = (EncryptionAlgorithm) algorithm;
            sb.append('-');
            sb.append(encAlg.getMode());
            sb.append('-');
            sb.append(encAlg.getPadding());
        }
        if (type != null) {
            sb.append("-");
            sb.append(type);
        }
        sb.append(".out");
        return sb.toString();
    }

    /**
   * Gets an input stream for the given parameters.
   *
   * @param  basename  Base name of file.
   * @param  algorithm  Cryptographic algorithm.
   * @param  type  Type of data in file. May be null to indicate raw data.
   *
   * @return  File input stream.
   */
    public static InputStream getIn(final String basename, final Algorithm algorithm, final String type) {
        final String name = getFileName(basename, algorithm, type);
        try {
            return new BufferedInputStream(new FileInputStream(new File(name)));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File not found: " + name);
        }
    }

    /**
   * Gets an output stream for the given parameters.
   *
   * @param  basename  Base name of file.
   * @param  algorithm  Cryptographic algorithm.
   * @param  type  Type of data in file. May be null to indicate raw data.
   *
   * @return  File output stream.
   */
    public static OutputStream getOut(final String basename, final Algorithm algorithm, final String type) {
        final String name = getFileName(basename, algorithm, type);
        try {
            return new BufferedOutputStream(new FileOutputStream(new File(name)));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File not found: " + name);
        }
    }

    /**
   * Determines whether the contents of two files are equal by hashing their
   * contents and comparing the resulting hash.
   *
   * @param  a  File.
   * @param  b  File.
   *
   * @return  True if file contents are equal, false otherwise.
   */
    public static boolean equal(final File a, final File b) {
        InputStream inA = null;
        InputStream inB = null;
        try {
            inA = new BufferedInputStream(new FileInputStream(a));
            inB = new BufferedInputStream(new FileInputStream(b));
            return equal(inA, inB);
        } catch (Exception ex) {
            return false;
        } finally {
            if (inA != null) {
                try {
                    inA.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inB != null) {
                try {
                    inB.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
   * Determines whether the contents of two streams are equal by hashing their
   * contents and comparing the resulting hash.
   *
   * @param  a  Input stream.
   * @param  b  Input stream.
   *
   * @return  True if stream contents are equal, false otherwise.
   */
    public static boolean equal(final InputStream a, final InputStream b) {
        final MD5 md5 = new MD5();
        try {
            final String hashA = md5.digest(a, new HexConverter());
            final String hashB = md5.digest(b, new HexConverter());
            return hashA.equals(hashB);
        } catch (Exception ex) {
            return false;
        }
    }
}
