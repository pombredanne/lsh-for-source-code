package com.sun.security.sasl;

import javax.security.sasl.SaslException;
import javax.security.sasl.Sasl;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
  * Base class for implementing CRAM-MD5 client and server mechanisms.
  *
  * @author Vincent Ryan
  * @author Rosanna Lee
  */
abstract class CramMD5Base {

    protected boolean completed = false;

    protected boolean aborted = false;

    protected byte[] pw;

    protected CramMD5Base() {
        initLogger();
    }

    /**
     * Retrieves this mechanism's name.
     *
     * @return  The string "CRAM-MD5".
     */
    public String getMechanismName() {
        return "CRAM-MD5";
    }

    /**
     * Determines whether this mechanism has completed.
     * CRAM-MD5 completes after processing one challenge from the server.
     *
     * @return true if has completed; false otherwise;
     */
    public boolean isComplete() {
        return completed;
    }

    /**
      * Unwraps the incoming buffer. CRAM-MD5 supports no security layer.
      *
      * @throws SaslException If attempt to use this method.
      */
    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
        if (completed) {
            throw new IllegalStateException("CRAM-MD5 supports neither integrity nor privacy");
        } else {
            throw new IllegalStateException("CRAM-MD5 authentication not completed");
        }
    }

    /**
      * Wraps the outgoing buffer. CRAM-MD5 supports no security layer.
      *
      * @throws SaslException If attempt to use this method.
      */
    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        if (completed) {
            throw new IllegalStateException("CRAM-MD5 supports neither integrity nor privacy");
        } else {
            throw new IllegalStateException("CRAM-MD5 authentication not completed");
        }
    }

    /**
     * Retrieves the negotiated property.
     * This method can be called only after the authentication exchange has
     * completed (i.e., when <tt>isComplete()</tt> returns true); otherwise, a
     * <tt>SaslException</tt> is thrown.
     *
     * @return value of property; only QOP is applicable to CRAM-MD5.
     * @exception IllegalStateException if this authentication exchange has not completed
     */
    public Object getNegotiatedProperty(String propName) {
        if (completed) {
            if (propName.equals(Sasl.QOP)) {
                return "auth";
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("CRAM-MD5 authentication not completed");
        }
    }

    public void dispose() throws SaslException {
        clearPassword();
    }

    protected void clearPassword() {
        if (pw != null) {
            for (int i = 0; i < pw.length; i++) {
                pw[i] = (byte) 0;
            }
            pw = null;
        }
    }

    protected void finalize() {
        clearPassword();
    }

    private static final int MD5_BLOCKSIZE = 64;

    /**
     * Hashes its input arguments according to HMAC-MD5 (RFC 2104)
     * and returns the resulting digest in its ASCII representation.
     *
     * HMAC-MD5 function is described as follows:
     *
     *       MD5(key XOR opad, MD5(key XOR ipad, text))
     *
     * where key  is an n byte key
     *       ipad is the byte 0x36 repeated 64 times
     *       opad is the byte 0x5c repeated 64 times
     *       text is the data to be protected
     */
    static final String HMAC_MD5(byte[] key, byte[] text) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        if (key.length > 64) {
            key = md5.digest(key);
        }
        byte[] ipad = new byte[MD5_BLOCKSIZE];
        byte[] opad = new byte[MD5_BLOCKSIZE];
        byte[] digest;
        int i;
        for (i = 0; i < MD5_BLOCKSIZE; i++) {
            for (; i < key.length; i++) {
                ipad[i] = key[i];
                opad[i] = key[i];
            }
            ipad[i] = 0x00;
            opad[i] = 0x00;
        }
        for (i = 0; i < MD5_BLOCKSIZE; i++) {
            ipad[i] ^= 0x36;
            opad[i] ^= 0x5c;
        }
        md5.update(ipad);
        md5.update(text);
        digest = md5.digest();
        md5.update(opad);
        md5.update(digest);
        digest = md5.digest();
        StringBuffer digestString = new StringBuffer();
        for (i = 0; i < digest.length; i++) {
            if ((digest[i] & 0x000000ff) < 0x10) {
                digestString.append("0" + Integer.toHexString(digest[i] & 0x000000ff));
            } else {
                digestString.append(Integer.toHexString(digest[i] & 0x000000ff));
            }
        }
        return (digestString.toString());
    }

    /**
     * Sets logger field.
     */
    private static synchronized void initLogger() {
        if (logger == null) {
            logger = Logger.getLogger(SASL_LOGGER_NAME);
        }
    }

    /**
     * Logger for debug messages
     */
    private static final String SASL_LOGGER_NAME = "javax.security.sasl";

    protected static Logger logger;
}
