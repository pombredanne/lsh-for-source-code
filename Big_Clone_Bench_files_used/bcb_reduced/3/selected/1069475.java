package mail.node;

import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author Arne Müller
 *
 */
public class ID {

    /**
	 * Length of the ID in bytes. <br>
	 * A ID-size of 256 bits allows 2^256 different ID's, 
	 * assumed there are 2^64 users (which will never happen)
	 * the probability for each two users to have the same key is 2^-256
	 * there are about (2^64)^2 = 2^128 pairs of users 
	 * -> the probability that any two users have the same key is less than 2^-256 * 2^128 = 2^-128
	 * thus the probability is very small and we can assume that everybody got a different ID
	 */
    public static final int ID_LENGTH = 32;

    private final byte[] id;

    /**
	 * creates the ID out of the given keydata of a public key
	 * the algorithm is not very complex, so if parts of the key are known,
	 * the data of this ID can easily be used to recreate the key
	 * @param keydata
	 */
    public ID(byte[] keydata) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.reset();
        md.update(keydata);
        id = md.digest();
    }

    public ID(String s) {
        id = fromHex(s);
    }

    /**
	 * calculates the Hamming-distance between two ID's.
	 * The Hamming-distance is defined as the number of bits 
	 * that need to be flipped to get the other key
	 * @param other ID to calculate the distance to
	 * @return the Hamming-distance
	 */
    public final int distance(ID other) {
        int dist = 0;
        for (int i = 0; i < ID_LENGTH; i++) {
            for (int j = 0; j < 8; j++) {
                if ((id[i] & (1 << j)) != (other.id[i] & (1 << j))) {
                    dist++;
                }
            }
        }
        return (dist);
    }

    private static char toHexChar(int b) {
        if (b < 10) return ((char) ('0' + b)); else return ((char) ('A' + b - 10));
    }

    private static int fromHexChar(char c) {
        if (c >= 'a') return ((c - 'a' + 10)); else if (c >= 'A') return ((c - 'A' + 10)); else return ((c - '0'));
    }

    private static byte[] fromHex(String s) {
        byte[] out = new byte[ID_LENGTH];
        for (int i = 0; 2 * i < s.length() && i < ID_LENGTH; i++) {
            out[i] = (byte) (fromHexChar(s.charAt(2 * i)) | (fromHexChar(s.charAt(2 * i + 1)) << 4));
        }
        return (out);
    }

    public String send() {
        String out = "";
        for (int i = 0; i < ID_LENGTH; i++) {
            out = out + toHexChar(id[i] & 0x0f) + toHexChar((id[i] & 0xf0) >>> 4);
        }
        return (out);
    }

    public String toString() {
        String out = "";
        for (int i = 0; i < 4; i++) {
            out = out + toHexChar(id[i] & 0x0f) + toHexChar((id[i] & 0xf0) >>> 4);
        }
        return (out);
    }

    public final int hashCode() {
        int out = 0;
        for (int i = 0; i < ID_LENGTH; i++) {
            out = out ^ ((toHexChar(id[i] & 0x0f) + toHexChar((id[i] & 0xf0) >>> 4)) >>> ((i % 4) * 8));
        }
        return (out);
    }

    public boolean equals(Object o) {
        if (o instanceof ID) {
            return (Arrays.equals(((ID) o).id, id));
        } else return (false);
    }

    public String getStoreName() {
        return (send());
    }
}
