package signitserver.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Sachin Sudheendra
 */
public class MessageDigestCalculator {

    @SuppressWarnings("empty-statement")
    public static byte[] computeDigest(String input) throws NoSuchAlgorithmException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
        byte[] buffer = new byte[4242];
        while (digestInputStream.read(buffer) != -1) ;
        return messageDigest.digest();
    }
}
