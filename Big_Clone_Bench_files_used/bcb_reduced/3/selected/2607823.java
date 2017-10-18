package ori.provider.impl;

import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import ori.provider.IPasswordEncoder;

/**
 * IPasswordEncoder implementation that encode the password with MessageDigest.
 * 
 * @author Attila Korompai
 * @see java.security.MessageDigest
 * 
 */
public class MessageDigestEncoder implements IPasswordEncoder {

    /**
     * Default encoding.
     */
    public static final String DEFAULT_ENCODING = "MD5";

    /**
     * Needed to transform string in hexa.
     */
    private static final int HEXA = 16;

    /**
     * Lower byte mask.
     */
    private static final int LOWER_BYTE = 0xff;

    /**
     * Logger.
     */
    private static Logger log = LogManager.getLogManager().getLogger(MessageDigestEncoder.class.getName());

    /**
     * 
     */
    private MessageDigest md;

    /**
     * Uses default encoding (MD5).
     *
     */
    public MessageDigestEncoder() {
        setEncoding(DEFAULT_ENCODING);
    }

    /**
     * Encode the password, then transform it to hexa.
     * 
     * @param password Passord to encode
     * @return encode password
     * @see ori.IPasswordEncoder#encode(java.lang.String)
     */
    public final String encode(final String password) {
        if (password == null) {
            throw new IllegalArgumentException();
        }
        byte[] encrypted = md5(password);
        return md5ToHexa(encrypted);
    }

    /**
     * {@inheritDoc}
     */
    public void setConfiguration(String configuration) {
        setEncoding(configuration);
    }

    /**
     * Initializes underlying MessageDigest.
     * @param encoding Encoding
     */
    public void setEncoding(String encoding) {
        if (encoding == null) {
            throw new IllegalArgumentException("Parameter encoding cannot be null!");
        }
        try {
            md = MessageDigest.getInstance(encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs MD5 encoding.
     * 
     * @param clear string to encode
     * @return encoded string
     */
    private byte[] md5(final String clear) {
        if (clear == null) {
            throw new IllegalArgumentException();
        }
        try {
            byte[] data = md.digest(clear.getBytes());
            return data;
        } catch (Exception e) {
            log.log(Level.SEVERE, "md5 failed!", e);
        }
        return null;
    }

    /**
     * Makes md5 more readable (Hexa).
     * 
     * @param md5Bytes bytes of the encoded string
     * @return readable hexa
     */
    private String md5ToHexa(final byte[] md5Bytes) {
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & LOWER_BYTE;
            if (val < HEXA) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
