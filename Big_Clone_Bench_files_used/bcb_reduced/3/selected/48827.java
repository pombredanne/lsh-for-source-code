package com.makeabyte.jhosting.server.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.makeabyte.jhosting.server.Configuration;

public class Crypto {

    private static Log log = LogFactory.getLog(Crypto.class);

    private static String encryptActiveDirectory(String data) throws IOException {
        String userPassword = "\"" + data + "\"";
        try {
            byte[] newPassword = userPassword.getBytes("UTF-16LE");
            return newPassword.toString();
        } catch (Exception ex) {
            String error = "Failed to encode UTF-16LE password";
            log.error(error, ex);
            throw new IOException(ex.getMessage());
        }
    }

    /**
	    * Encrypts the specified data per jhosting.properties configuration
	    * 
	    * @param data The data to encrypt
	    * @return The encrypted data
	    * @throws IOException
	    * @throws NoSuchAlgorithmException
	    */
    public static String encrypt(String data) throws IOException, NoSuchAlgorithmException {
        Properties props = Configuration.getInstance().getProperties();
        String algorithm = props.getProperty("com.makeabyte.jhosting.server.persistence.security.algorithm");
        String encryptedData = data;
        boolean encrypt = Boolean.parseBoolean(props.getProperty("com.makeabyte.jhosting.server.persistence.security.encrypt"));
        if (encrypt) {
            if (algorithm.equalsIgnoreCase("UTF-16LE")) return encryptActiveDirectory(data);
            MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
            md.reset();
            md.update(data.getBytes());
            encryptedData = md.digest().toString();
        }
        return encryptedData;
    }

    /**
	    * Returns a MessageDigest object suitable for passing to base64Encode.
	    * To get the digest from the return value simply invoke returnValue.digest()
	    *  
	    * @param algorithm
	    * @param data
	    * @return
	    * @throws IOException
	    * @throws NoSuchAlgorithmException 
	    */
    public MessageDigest encrypt(String algorithm, String data) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
        md.reset();
        md.update(data.getBytes());
        return md;
    }

    /**
	    * Returns a digest string using the specified algorithm and data
	    * 
	    * @param algorithm The algorithm to use (MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512)
	    * @param data The data to encrypt
	    * @return The data digest
	    * @throws IOException
	    * @throws NoSuchAlgorithmException
	    */
    public String getDigest(String algorithm, String data) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
        md.reset();
        md.update(data.getBytes());
        return md.digest().toString();
    }

    /**
	    * Base64 encodes the specified MessageDigest data
	    * 
	    * @param data MessageDigest object containing the data to base64 encode
	    * @return The base64 encoded value
	    */
    public String base64Encode(MessageDigest data) {
        return org.jboss.seam.util.Base64.encodeBytes(data.digest());
    }
}
