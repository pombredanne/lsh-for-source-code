package frost.util;

import java.security.*;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * This class is used to generate random GUIDs of the type
 * F45C47D0-FF4E-11D8-9669-0800200C9A66 (38 characters long, 
 * including dashes).
 * 
 * @author $Author: bback $
 * @version $Revision: 2564 $
 */
public class RandomGuid {

    private static Random random = new Random();

    private String guid;

    /**
	 * This creates a new instance of RandomGuid. 
	 * @throws NoSuchAlgorithmException if the instance could not be created because
	 * 					the "MD5" algorithm is not available
	 */
    public RandomGuid() throws NoSuchAlgorithmException {
        generateGuid();
    }

    /**
	 * This method generates the random guid
	 */
    private void generateGuid() throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        StringBuilder stringToDigest = new StringBuilder();
        long time = System.currentTimeMillis();
        long rand = random.nextLong();
        stringToDigest.append(time);
        stringToDigest.append("-");
        stringToDigest.append(rand);
        md5.update(stringToDigest.toString().getBytes());
        byte[] digestBytes = md5.digest();
        StringBuilder digest = new StringBuilder();
        for (int i = 0; i < digestBytes.length; ++i) {
            int b = digestBytes[i] & 0xFF;
            if (b < 0x10) {
                digest.append('0');
            }
            digest.append(Integer.toHexString(b));
        }
        guid = digest.toString();
    }

    /** 
	 * This method returns a String representation of the guid in
	 * the standard format for GUIDs, like F45C47D0-FF4E-11D8-9669-0800200C9A66
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        String guidUpperCase = guid.toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append(guidUpperCase.substring(0, 8));
        sb.append("-");
        sb.append(guidUpperCase.substring(8, 12));
        sb.append("-");
        sb.append(guidUpperCase.substring(12, 16));
        sb.append("-");
        sb.append(guidUpperCase.substring(16, 20));
        sb.append("-");
        sb.append(guidUpperCase.substring(20));
        return sb.toString();
    }
}
