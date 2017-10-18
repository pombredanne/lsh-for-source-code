package org.oclc.da.ndiipp.fileinfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Checksum;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * @author Matt Cordial
 */
public class Checksummer implements Checksum {

    private MessageDigest md5;

    private MessageDigest sha1;

    /**
	 */
    public Checksummer() {
        reset();
    }

    /**
	 * @return  Message Digest
	 */
    public MessageDigest getMd5Digest() {
        return md5;
    }

    /**
	 * @return  Message Digest
	 */
    public MessageDigest getSha1Digest() {
        return sha1;
    }

    /**
	 * @return value
	 */
    public long getValue() {
        return 0;
    }

    /**
	 * 
	 */
    public void reset() {
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    /**
     *  Updates the checksum with the argument.
     *  Called when a signed byte is available.
	 * @param b 
     */
    public void update(byte b) {
        if ((md5 != null) && (sha1 != null)) {
            md5.update(b);
            sha1.update(b);
        }
    }

    /**
     *  Updates the checksum with the argument.
     *  Called when an unsigned byte is available.
     * @param b 
     */
    public void update(int b) {
        byte sb;
        if (b > 127) {
            sb = (byte) (b - 256);
        } else {
            sb = (byte) b;
        }
        update(sb);
    }

    /**
     *  Updates the checksum with the argument.
     *  Called when a byte array is available.
     * @param b 
     */
    public void update(byte[] b) {
        if ((md5 != null) && (sha1 != null)) {
            md5.update(b);
            sha1.update(b);
        }
    }

    /**
     *  Updates the checksum with the argument.
     *  Called when a byte array is available.
     * @param b 
     * @param off 
     * @param len 
     */
    public void update(byte[] b, int off, int len) {
        if ((md5 != null) && (sha1 != null)) {
            md5.update(b, off, len);
            sha1.update(b, off, len);
        }
    }

    /**
     *  Returns the value of the MD5 digest as a Base64 encoded string.
     *  Returns null if the digest is not available.
     * @return Base 64 Encoding
     */
    public String getBase64EncodedMD5() {
        String value = null;
        if (md5 != null) {
            value = new String(Base64.encodeBase64(md5.digest()));
        }
        return value;
    }

    /**
     *  Returns the value of the SHA1 digest as a Base64 encoded string.
     *  Returns null if the digest is not available.
     * @return Base 64 Encoding
     */
    public String getBase64EncodedSHA1() {
        String value = null;
        if (sha1 != null) {
            value = new String(Base64.encodeBase64(sha1.digest()));
        }
        return value;
    }

    /**
     *  Returns the value of the MD5 digest as a Hex encoded string.
     *  Returns null if the digest is not available.
     * @return Hex Encoding
     */
    public String getHexEncodedMD5() {
        String value = null;
        if (md5 != null) {
            value = new String(Hex.encodeHex(md5.digest()));
        }
        return value;
    }

    /**
     *  Returns the value of the SHA1 digest as a Hex encoded string.
     *  Returns null if the digest is not available.
     * @return Hex Encoding
     */
    public String getHexEncodedSHA1() {
        String value = null;
        if (sha1 != null) {
            value = new String(Hex.encodeHex(sha1.digest()));
        }
        return value;
    }
}
