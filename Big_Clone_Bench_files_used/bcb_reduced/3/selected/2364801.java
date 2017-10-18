package net.sf.dz.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple utility class to produce an SHA message digest of a message.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim
 * Tkachenko</a>
 * @version $Id: MessageDigestFactory.java,v 1.4 2007-03-01 07:08:10 vtt Exp $
 */
public class MessageDigestFactory {

    /**
     * Produce an SHA message digest.
     * 
     * @param message Message to get SHA digest for.
     * @return A string representation of the message digest.
     * @throws NoSuchAlgorithmException if by some miracle SHA algorithm is not
     * available.
     */
    public String getSHA(String message) throws NoSuchAlgorithmException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageDigest sha = MessageDigest.getInstance("SHA");
        DigestOutputStream dos = new DigestOutputStream(baos, sha);
        PrintWriter pw = new PrintWriter(dos);
        pw.print(message);
        pw.flush();
        byte md[] = dos.getMessageDigest().digest();
        StringBuffer sb = new StringBuffer();
        for (int offset = 0; offset < md.length; offset++) {
            byte b = md[offset];
            if ((b & 0xF0) == 0) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(b & 0xFF));
        }
        return sb.toString();
    }
}
