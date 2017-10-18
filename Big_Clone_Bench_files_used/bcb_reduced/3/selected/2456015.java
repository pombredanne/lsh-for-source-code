package net.sf.jukebox.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple utility class to produce a message digest of a message.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class MessageDigestFactory {

    /**
     * Produce an MD5 message digest.
     * 
     * @param message Message to get MD5 digest for.
     * @return A string representation of the message digest.
     * @throws NoSuchAlgorithmException if by some miracle SHA algorithm is not
     * available.
     */
    public String getMD5(String message) {
        try {
            return getDigest(MessageDigest.getInstance("MD5"), message);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Can't create a MD5 message disgest", ex);
        }
    }

    /**
     * Produce an SHA message digest.
     * 
     * @param message Message to get SHA digest for.
     * @return A string representation of the message digest.
     * @throws NoSuchAlgorithmException if by some miracle SHA algorithm is not
     * available.
     */
    public String getSHA(String message) {
        try {
            return getDigest(MessageDigest.getInstance("SHA"), message);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Can't create a SHA message disgest", ex);
        }
    }

    /**
     * Produce a message digest with a given algorithm instance.
     * 
     * @param md MessageDigest instance to use.
     * @param message Message to get message digest for.
     * @return A string representation of the message digest.
     * @throws NoSuchAlgorithmException if by some miracle the algorithm is not available.
     */
    public String getDigest(MessageDigest md, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DigestOutputStream dos = new DigestOutputStream(baos, md);
        PrintWriter pw = new PrintWriter(dos);
        pw.print(message);
        pw.flush();
        byte digest[] = dos.getMessageDigest().digest();
        StringBuffer sb = new StringBuffer();
        for (int offset = 0; offset < digest.length; offset++) {
            byte b = digest[offset];
            if ((b & 0xF0) == 0) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(b & 0xFF));
        }
        return sb.toString();
    }
}
