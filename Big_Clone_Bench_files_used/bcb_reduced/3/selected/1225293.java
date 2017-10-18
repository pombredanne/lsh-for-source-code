package jtmsmon;

import java.security.MessageDigest;
import java.util.Random;

/**
 *
 * @author th
 */
public class CreateRandomPassword {

    /** Field description */
    private static MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
        }
    }

    /**
   * Method description
   *
   *
   * @param message
   *
   * @return
   */
    public static String encode(String message) {
        String hashString = null;
        messageDigest.reset();
        messageDigest.update(message.getBytes());
        byte hashCode[] = messageDigest.digest();
        hashString = "";
        for (int i = 0; i < hashCode.length; i++) {
            int x = hashCode[i] & 0xff;
            if (x < 16) {
                hashString += '0';
            }
            hashString += Integer.toString(x, 16);
        }
        return hashString;
    }

    /**
   * Method description
   *
   *
   * @param numberOfChars
   *
   * @return
   */
    public static String getRandomPassword(int numberOfChars) {
        final char[] pwChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
        Random random = new Random(System.currentTimeMillis());
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < numberOfChars; i++) {
            buffer.append(pwChars[random.nextInt(pwChars.length)]);
        }
        return buffer.toString();
    }

    /**
   * @param args the command line arguments
   */
    public static void main(String[] args) {
        String password = getRandomPassword(8);
        System.out.println("\n (PLAIN)" + password + " (MD5)" + encode(password) + "\n");
    }
}
