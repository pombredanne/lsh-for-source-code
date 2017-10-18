package org.tn5250j.tools;

import java.io.Serializable;
import java.security.MessageDigest;

public class DESSHA1 implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Message digest object. */
    private final MessageDigest md;

    /** quick array to convert byte values to hex codes. */
    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * Creates a new <code>MessageDigest</code> instance.
	 * @throws InstantiationException
	 */
    public DESSHA1() throws InstantiationException {
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new InstantiationException("No such algorithm: SHA");
        }
    }

    /**
	 * used to hash passwords and other key data to fireSend over the wire,
	 * to prevent plaintext transmit.
	 *
	 * @param identifier prefix, such as the SessionID
	 * @param key passkey, such as password or handshake
	 * @return <code>String</code> with the hash in hex string form
	 */
    public String digest(String identifier, String key) {
        if (identifier != null) md.update(identifier.getBytes());
        if (key != null) md.update(key.getBytes());
        return DESSHA1.bytesToHex(md.digest());
    }

    /**
	 * This utility method is passed an array of bytes. It returns
	 * this array as a String in hexadecimal format. This is used
	 * internally by <code>digest()</code>. Data is returned in
	 * the format specified by the Jabber protocol.
	 *
	 * @param data
	 * @return String of our hex bytes
	 */
    public static String bytesToHex(byte[] data) {
        StringBuffer retval = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            retval.append(HEX[(data[i] >> 4) & 0x0F]);
            retval.append(HEX[data[i] & 0x0F]);
        }
        return retval.toString();
    }
}
