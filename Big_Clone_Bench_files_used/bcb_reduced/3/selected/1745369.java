package com.genia.toolbox.basics.manager.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.genia.toolbox.basics.exception.technical.TechnicalEncodingException;
import com.genia.toolbox.basics.exception.technical.TechnicalException;
import com.genia.toolbox.basics.exception.technical.TechnicalFileNotFoundException;
import com.genia.toolbox.basics.exception.technical.TechnicalIOException;
import com.genia.toolbox.basics.manager.CryptoManager;
import com.genia.toolbox.basics.manager.StreamManager;
import com.genia.toolbox.constants.client.Charset;

/**
 * implementation of {@link com.genia.toolbox.basics.manager.CryptoManager}.
 */
public class CryptoManagerImpl implements CryptoManager {

    /**
   * algorithm SHA-1.
   */
    private static final String ALGO_SHA_1 = "SHA-1";

    /**
   * algorithm MD5.
   */
    private static final String ALGO_MD5 = "MD5";

    /**
   * the algorithm of hash using by default.
   */
    private Hash defaultHash = Hash.SHA1;

    /**
   * the {@link StreamManager} to use.
   */
    private StreamManager streamManager;

    /**
   * Enumeration.
   */
    public enum Hash {

        /**
     * algorithm SHA-1.
     */
        SHA1(ALGO_SHA_1), /**
     * algorithm MD5.
     */
        MD5(ALGO_MD5);

        /**
     * return the hash for an array of <code>byte</code> with the algorithm
     * settings by defaults.
     * 
     * @param bytes
     *          is an array of <code>byte</code>.
     * @return the hash for an array of <code>byte</code> with the algorithm
     *         settings by defaults.
     * @throws TechnicalException
     *           if the algorithm is not found.
     */
        public byte[] hash(byte[] bytes) throws TechnicalException {
            return messageDigestSum(bytes, algorithm);
        }

        /**
     * the name of the hash algorithm to use.
     */
        private final String algorithm;

        /**
     * constructor.
     * 
     * @param algorithm
     *          the name of the hash algorithm to use
     */
        private Hash(String algorithm) {
            this.algorithm = algorithm;
        }
    }

    /**
   * compute digest from an array of bytes and with the algorithm chosen.
   * 
   * @param bytes
   *          an array of bytes.
   * @param algorithm
   *          the algorithm chosen.
   * @return a string representation of the digest.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    private static byte[] messageDigestSum(byte[] bytes, String algorithm) throws TechnicalException {
        MessageDigest algoDigest;
        try {
            algoDigest = MessageDigest.getInstance(algorithm);
            algoDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(e);
        }
        return algoDigest.digest(bytes);
    }

    /**
   * return a hash representation from a String.
   * 
   * @param hash
   *          is of type <code>Hash</code>.
   * @param string
   *          the string to transform.
   * @return a hash representation from a String.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    private byte[] hash(Hash hash, String string) throws TechnicalException {
        byte hashString[];
        try {
            hashString = string.getBytes(Charset.UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new TechnicalEncodingException(e);
        }
        return hash.hash(hashString);
    }

    /**
   * return a hash representation from a file.
   * 
   * @param hash
   *          is of type <code>Hash</code>.
   * @param file
   *          the file to transform.
   * @return a hash representation from a file
   * @throws TechnicalException
   *           if the algorithm is not found.
   * @throws TechnicalIOException
   *           if the file is not found.
   */
    private byte[] hash(Hash hash, File file) throws TechnicalIOException, TechnicalException {
        try {
            return hash.hash(getStreamManager().getBytes(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            throw new TechnicalFileNotFoundException(e);
        }
    }

    /**
   * return the hash for a string with the algorithm settings by defaults.
   * 
   * @param string
   *          is of type <code>String</code>.
   * @return the hash for a string with the algorithm settings by defaults.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] hash(String string) throws TechnicalException {
        return hash(getDefaultHash(), string);
    }

    /**
   * return the hash for an array of <code>byte</code> with the algorithm
   * settings by defaults.
   * 
   * @param bytes
   *          is an array of <code>byte</code>.
   * @return the hash for an array of <code>byte</code> with the algorithm
   *         settings by defaults.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] hash(byte[] bytes) throws TechnicalException {
        return getDefaultHash().hash(bytes);
    }

    /**
   * return the hash for a file with the algorithm settings by defaults.
   * 
   * @param file
   *          is of type <code>File</code>.
   * @return the hash for a file with the algorithm settings by defaults.
   * @throws TechnicalException
   *           if the algorithm is not found.
   * @throws TechnicalIOException
   *           if the file is not found.
   */
    public byte[] hash(File file) throws TechnicalIOException, TechnicalException {
        return hash(getDefaultHash(), file);
    }

    /**
   * return the hash for a string with MD5 algorithm.
   * 
   * @param string
   *          is of type <code>String</code>.
   * @return the hash for a string with md5 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] md5(String string) throws TechnicalException {
        return hash(Hash.MD5, string);
    }

    /**
   * return the hash for an array of <code>byte</code> with MD5 algorithm.
   * 
   * @param bytes
   *          is an array of <code>byte</code>.
   * @return the hash for an array of <code>byte</code> with MD5 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] md5(byte[] bytes) throws TechnicalException {
        return Hash.MD5.hash(bytes);
    }

    /**
   * return the hash for a file with MD5 algorithm.
   * 
   * @param file
   *          is of type <code>File</code>.
   * @return the hash for a file with MD5 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   * @throws TechnicalIOException
   *           if the file is not found.
   */
    public byte[] md5(File file) throws TechnicalIOException, TechnicalException {
        return hash(Hash.MD5, file);
    }

    /**
   * return the hash for a string with SHA-1 algorithm.
   * 
   * @param string
   *          is of type <code>String</code>.
   * @return the hash for a string with SHA-1 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] sha1(String string) throws TechnicalException {
        return hash(Hash.SHA1, string);
    }

    /**
   * return the hash for an array of <code>byte</code> with SHA-1 algorithm.
   * 
   * @param bytes
   *          is an array of <code>byte</code>.
   * @return the hash for an array of <code>byte</code> with SHA-1 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   */
    public byte[] sha1(byte[] bytes) throws TechnicalException {
        return Hash.SHA1.hash(bytes);
    }

    /**
   * return the hash for a file with SHA-1 algorithm.
   * 
   * @param file
   *          is of type <code>File</code>.
   * @return the hash for a file with SHA-1 algorithm.
   * @throws TechnicalException
   *           if the algorithm is not found.
   * @throws TechnicalIOException
   *           if the file is not found.
   */
    public byte[] sha1(File file) throws TechnicalIOException, TechnicalException {
        return hash(Hash.SHA1, file);
    }

    /**
   * getter for the defaultHash property.
   * 
   * @return the defaultHash
   */
    public Hash getDefaultHash() {
        return defaultHash;
    }

    /**
   * setter for the defaultHash property.
   * 
   * @param defaultHash
   *          the defaultHash to set
   */
    public void setDefaultHash(Hash defaultHash) {
        this.defaultHash = defaultHash;
    }

    /**
   * getter for the streamManager property.
   * 
   * @return the streamManager
   */
    public StreamManager getStreamManager() {
        return streamManager;
    }

    /**
   * setter for the streamManager property.
   * 
   * @param streamManager
   *          the streamManager to set
   */
    public void setStreamManager(StreamManager streamManager) {
        this.streamManager = streamManager;
    }
}
