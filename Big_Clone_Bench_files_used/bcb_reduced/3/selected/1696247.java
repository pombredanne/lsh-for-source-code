package org.tcpfile.fileio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tcpfile.main.Misc;

/**
 * Hashes Objects with given algorithm.
 * Default: SHA1
 * @author stivo
 *
 */
public class Hasher {

    private static Logger log = LoggerFactory.getLogger(Hasher.class);

    private String algorithm = "";

    MessageDigest md;

    private int sleeptime = 0;

    private static final long bufferlength = 524288;

    public static int hashLength = 20;

    /**
	 * Initialize a Hasher that uses SHA1 Algorithm (Default)
	 *
	 */
    public Hasher() {
        this("SHA1");
    }

    /**
	 * Initialize a Hasher with given algorithm.
	 * @param algorithm
	 */
    public Hasher(String algorithm) {
        this.algorithm = algorithm;
        try {
            this.md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            log.warn("", e);
        }
    }

    /**
	 * Hashes Files.
	 * @param filename The name of the File, relative or absolut.
	 * @return
	 */
    public byte[] hash(String filename) {
        FileInputStream bl;
        try {
            bl = new FileInputStream(filename);
        } catch (IOException e) {
            throw new NullPointerException("File was not found " + filename);
        }
        return hash(bl);
    }

    /**
	 * Hashes the given byte[].
	 * @param b 
	 * @return
	 */
    public byte[] hash(byte[] b) {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        return hash(bais);
    }

    public byte[] hashString(String b) {
        return hash(b.getBytes());
    }

    /**
	 * Hashes something given from an inputstream
	 * @param in
	 * @return
	 */
    public byte[] hash(InputStream in) {
        return hash(in, 0);
    }

    public byte[] hash(InputStream in, float speed) {
        return hash(in, speed, 0, 0);
    }

    /**
	 * Hashes somewhat slower than the given speed.
	 * Found on: <br>
	 * http://kickjava.com/1284.htm
	 * @param in InputStream
	 * @param speed in Megabytes/second
	 * @param blocksize if not 0, the method returns a number of hashes
	 * @param inputlength if blocksize is not 0, the length of the input has to be told in advance
	 * @return The hashes for each block
	 */
    public byte[] hash(InputStream in, float speed, int blocksize, long inputlength) {
        if (speed != 0) setSpeed(speed);
        int localsleeptime = sleeptime;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (in instanceof ByteArrayInputStream) localsleeptime = 0;
        DigestInputStream dis = new DigestInputStream(in, md);
        byte[] buffer = new byte[(int) bufferlength];
        try {
            if (blocksize == 0) while ((dis.read(buffer) != -1)) Misc.sleeps(localsleeptime); else {
                int bytes = 0;
                int adding = 0;
                while ((adding = dis.read(buffer, 0, (int) blocksize)) != -1) {
                    if ((bytes += adding) == blocksize) {
                        baos.write(md.digest());
                        md.reset();
                        bytes = 0;
                    }
                    Misc.sleeps(localsleeptime);
                }
            }
        } catch (IOException e) {
            log.warn("", e);
        }
        try {
            baos.write(md.digest());
        } catch (IOException e) {
            log.warn("", e);
        }
        try {
            dis.close();
        } catch (IOException e) {
            log.debug("", e);
        }
        if (blocksize != 0) {
            int length = (int) (inputlength / blocksize) + 1;
            if (baos.size() != length * md.getDigestLength()) throw new RuntimeException("Programming error, postcondition failed");
        }
        return baos.toByteArray();
    }

    /**
	 * Sets the speed (mbytes/second).
	 */
    public void setSpeed(float speed) {
        sleeptime = 0;
        if (speed > 0) sleeptime = (int) (1000 * bufferlength / ((long) speed * 1024L * 1024L));
    }

    public byte[] createSubHashes(String filename, int blocksize) {
        File file = new File(filename);
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (IOException e) {
            throw new NullPointerException("File was not found");
        }
        return hash(fis, 0, blocksize, file.length());
    }

    /**
	 * Static function to hash something.
	 * If you hash several times, dont use this, intialize a hasher.
	 * @param filename The filename
	 * @return SHA-256 Hash
	 */
    public static byte[] hashonce(String filename) {
        Hasher b = new Hasher();
        return b.hash(filename);
    }

    /**
	 * Static function to hash a byte array
	 * If you hash several times, dont use this, intialize a hasher.
	 * @param filename The filename
	 * @return SHA1 Hash
	 */
    public static byte[] hashonce(byte[] ba) {
        Hasher b = new Hasher();
        return b.hash(ba);
    }

    public static byte[] hashOneString(String ba) {
        Hasher b = new Hasher();
        return b.hashString(ba);
    }

    /**
	 * To prevent a compiler warning saying that the field algorithm is never used.
	 * @return The algorithm of this hasher
	 */
    public String getAlgorithm() {
        return algorithm;
    }
}
