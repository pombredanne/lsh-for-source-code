package com.sun.j2me.crypto;

/**
 * This MessageDigest class provides applications the functionality of a
 * message digest algorithm, such as MD5 or SHA.
 * Message digests are secure one-way hash functions that take arbitrary-sized
 * data and output a fixed-length hash value.
 *
 * <p>A <code>MessageDigest</code> object starts out initialized. The data is 
 * processed through it using the <code>update</code>
 * method. At any point {@link #reset() reset} can be called
 * to reset the digest. Once all the data to be updated has been
 * updated, the <code>digest</code> method should 
 * be called to complete the hash computation.
 *
 * <p>The <code>digest</code> method can be called once for a given number 
 * of updates. After <code>digest</code> has been called, 
 * the <code>MessageDigest</code>
 * object is reset to its initialized state.
 */
public class MessageDigest {

    /**
     * Message digest implementation.
     */
    com.sun.midp.crypto.MessageDigest messageDigest;

    public MessageDigest(String algorithm) throws NoSuchAlgorithmException {
        try {
            messageDigest = com.sun.midp.crypto.MessageDigest.getInstance(algorithm);
        } catch (com.sun.midp.crypto.NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    public void reset() {
        messageDigest.reset();
    }

    public void update(byte[] input, int offset, int len) {
        messageDigest.update(input, offset, len);
    }

    public void digest(byte[] buf, int offset, int len) throws DigestException {
        try {
            messageDigest.digest(buf, offset, len);
        } catch (com.sun.midp.crypto.DigestException e) {
            throw new DigestException(e.getMessage());
        }
    }

    public int getDigestLength() {
        return messageDigest.getDigestLength();
    }
}
