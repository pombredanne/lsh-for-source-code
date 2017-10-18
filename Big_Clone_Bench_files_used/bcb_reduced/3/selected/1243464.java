package de.uni_bremen.informatik.p2p.peeranha42.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Utility for the digital Fingerprint. The digital Fingerprint is also known
 * as checksum. To verify if File has been corrupted. 
 * 
 * <p>For example before you send a file you can take a fingerprint. After 
 * send take a new Fingerprint and check with the Method isEqual.</p>
 * 
 * @inheritDoc java.security.MessageDigest

 * @author Johannes 'johu' Huber
 */
public class DigitalFingerPrint {

    /** logger for logging */
    protected static Logger log = Logger.getLogger(DigitalFingerPrint.class);

    /** constant for digest algorithm SHA1 */
    public static final String ALGO_SHA1 = "SHA1";

    /** constant for digest algorithm MD5 */
    public static final String ALGO_MD5 = "MD5";

    /** @see java.security.MessageDigest */
    private static MessageDigest md = null;

    /**
     * Genertates an digital fingerprint of an given file. At the moment you 
     * can choose between 2 digest algorithm: MD5 or SHA1. Use the existing 
     * constants: DigitalFingerPrint.ALGO_MD5 for md5 or 
     * DigitalFingerPrint.ALGO_SHA1 for SHA1. Notice if File does not exits,
     * you'll get NULL as return value.
     *
     * @param file given file for the digest
     * @param algo digest algorithm (DigitalFingerPrint.ALGO_SHA1, DigitalFingerPrint.ALGO_MD5)
     *
     * @return null or byte[] digest
     */
    public static byte[] generate(String file, String algo) {
        FileInputStream fis = null;
        byte buffer[] = new byte[8192];
        int i = 0;
        if (!algo.equals(ALGO_SHA1) && !algo.equals(ALGO_MD5)) {
            throw new IllegalArgumentException("given algo: " + algo + ", use DigitalFingerPrint.ALGO_MD5 or DigitalFingerPrint.ALGO_SHA1");
        }
        try {
            md = MessageDigest.getInstance(algo);
            fis = new FileInputStream(file);
            while ((i = fis.read(buffer)) != -1) {
                md.update(buffer, 0, i);
            }
        } catch (NoSuchAlgorithmException ex) {
            log.error("Algorithm not found", ex);
            return null;
        } catch (FileNotFoundException ex) {
            log.error("File not found", ex);
            return null;
        } catch (IOException ex) {
            log.error("reading file failed", ex);
            return null;
        }
        return md.digest();
    }

    /**
     * give you a string representation from an given fingerprint
     *
     * @param byte[] fingerprint
     *
     * @return null or string representation of given fingerprint
     */
    public static String stringRepresentation(byte[] fingerprint) {
        StringBuffer sb = new StringBuffer();
        if (fingerprint == null) {
            return null;
        }
        for (int i = 0; i < fingerprint.length; i++) {
            String s = Integer.toHexString(fingerprint[i] & 0xFF);
            sb.append((s.length() == 1) ? ("0" + s) : s);
        }
        return sb.toString();
    }

    /**
     * returns the bit length of fingerprint
     * 
     * @param byte[] fingerprint
     * @return bit count of fingerprint
     */
    public static int getFingerPrintBitLength(byte[] fingerprint) {
        if (fingerprint == null) {
            return 0;
        }
        return 8 * fingerprint.length;
    }

    /**
     * Wrapper Methode from java.security.MessageDigest
     * 
     * @param fingerprintA to compared fingerprint number 1
     * @param fingerprintB to compared fingerprint number 2
     * 
     * @return boolean value: is Fingerprint A the same how the Fingerprint B ? 
     * 
     */
    public static boolean isEqual(byte[] fingerprintA, byte[] fingerprintB) {
        return MessageDigest.isEqual(fingerprintA, fingerprintB);
    }
}
