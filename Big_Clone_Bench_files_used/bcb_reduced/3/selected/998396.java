package ibg.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author mchaberski
 *
 */
public class HashUtility {

    public static final int DEFAULT_CHUNK_LENGTH = 1024 * 64;

    private static final int CHARS_PER_BYTE = 2;

    public static String toHexDigest(byte[] digest) {
        StringBuilder hexDigest = new StringBuilder(digest.length * CHARS_PER_BYTE);
        System.err.println("array[" + digest.length + "] " + java.util.Arrays.toString(digest));
        for (int i = 0; i < digest.length; i++) {
            String firstChar = Integer.toHexString(((digest[i] & 0xff) >> 1) & 0x0f);
            System.err.format("s[%d] = %d -> %s; ", i * CHARS_PER_BYTE, digest[i], firstChar);
            hexDigest.insert(i * CHARS_PER_BYTE, firstChar);
            String secondChar = Integer.toHexString((digest[i] & 0xff) & 0x0f);
            System.err.format("s[%d] = %d -> %s", i * CHARS_PER_BYTE + 1, digest[i], secondChar);
            hexDigest.insert(i * CHARS_PER_BYTE + 1, secondChar);
            System.err.println();
        }
        System.err.println("final hexDigest length = " + hexDigest.length());
        return hexDigest.toString();
    }

    public static byte[] computeDigest(String hashAlgorithm, File pathname, final int chunkLength) throws NoSuchAlgorithmException, IOException {
        byte[] data = new byte[chunkLength];
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        FileInputStream in = new FileInputStream(pathname);
        int chunkBytesRead;
        while ((chunkBytesRead = in.read(data, 0, chunkLength)) != -1) {
            md.update(data, 0, chunkBytesRead);
        }
        in.close();
        return md.digest();
    }

    public static byte[] computeDigest(String hashAlgorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        return md.digest(data);
    }

    /**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        File pathname = new File(args[0]);
        byte[] digest = computeDigest("MD5", pathname, DEFAULT_CHUNK_LENGTH);
        String hexDigest = toHexDigest(digest);
        System.out.println(String.format("%s %s", pathname, hexDigest));
    }
}
