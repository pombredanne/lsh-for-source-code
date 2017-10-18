package org.afk.ourshare.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

/**
 * This class hashs a stream and calculates chunks.
 * 
 * @author axel
 */
public class StreamHasher {

    static final int CHUNK_SIZE = 500000;

    /** contains the calculated chunks */
    Vector chunks = new Vector();

    /** contains data for calculating */
    InputStream in;

    /** Contains calculated md5-hash */
    String hash;

    /** calculates the total hash of the stream */
    MessageDigest total;

    public StreamHasher(InputStream in) throws NoSuchAlgorithmException {
        if (in == null) {
            throw new NullPointerException("No Inputstream");
        }
        total = MessageDigest.getInstance("MD5");
        this.in = in;
    }

    /** calculates the md5-hashes for each chunk and total stream */
    public void hashChunks() throws NoSuchAlgorithmException, IOException {
        int number = 0;
        long start = 0;
        byte[] buffer = new byte[CHUNK_SIZE];
        while (true) {
            int size = in.read(buffer);
            if (size == -1) break;
            String md5 = calculateMd5(buffer, size);
            Chunk chunk = new Chunk(md5, number, start, size);
            chunk.setAvailable();
            chunks.add(chunk);
            start += size;
            number++;
        }
        buffer = null;
        hash = digestToString(total.digest());
        Chunk[] chunks = getChunks();
        for (int i = 0; i < chunks.length; i++) {
            chunks[i].setFileHash(hash);
        }
    }

    /**
	 * The method that hashs the block
	 * 
	 * @param buffer
	 *           the buffer containing the bytes to be hashed
	 * @param size
	 *           the amount of valid bytes in the array
	 * @return the md5 digest as hex-string
	 * @throws NoSuchAlgorithmException
	 *            if no md5 algorythm in the jre exists
	 */
    private String calculateMd5(byte[] buffer, int size) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(buffer, 0, size);
        total.update(buffer, 0, size);
        byte[] md5 = digest.digest();
        return digestToString(md5);
    }

    /**
	 * converts a byte[] to a hexString
	 * 
	 * @param digest
	 *           the byte[] to be converted
	 * @return a string that looks like 0e34af0683...
	 */
    private String digestToString(byte[] digest) {
        StringBuffer buffer = new StringBuffer();
        int integer;
        for (int i = 0; i < digest.length; i++) {
            integer = digest[i] & 0xff;
            if (integer < 0x10) buffer.append('0');
            buffer.append(Integer.toHexString(integer));
        }
        return buffer.toString();
    }

    /**
	 * @return Returns the chunks.
	 */
    public Chunk[] getChunks() {
        if (chunks == null) return new Chunk[0];
        return (Chunk[]) chunks.toArray(new Chunk[chunks.size()]);
    }

    /**
	 * @return Returns the hash.
	 */
    public String getHash() {
        return hash;
    }
}
