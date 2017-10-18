package com.icteam.fiji.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.icteam.fiji.configuration.SystemConfigurationProperties;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author t.bezzi
 */
public class HashUtils {

    private static final Log logger = LogFactory.getLog(HashUtils.class);

    private static String hashAlgorithm;

    private static String DEFAULT_ALGORITHM = "MD5";

    private static String HASH_ALGORITHM_PROPERTY = "fiji.passwd.hash.algorithm";

    private static MessageDigest md;

    static {
        SystemConfigurationProperties config = new SystemConfigurationProperties("fiji.properties");
        if (config != null) {
            hashAlgorithm = config.getProperty(HASH_ALGORITHM_PROPERTY);
            logger.debug("fiji.passwd.hash.algorithm in fiji.properties = " + hashAlgorithm);
        }
        if (StringUtils.isBlank(hashAlgorithm)) hashAlgorithm = DEFAULT_ALGORITHM;
        logger.debug("fiji.passwd.hash.algorithm = " + hashAlgorithm);
    }

    /**
     * 
     * @param in Message to hash
     * @return Hashed message with the algorithm specified by the fiji property fiji.passwd.hash.algorithm (MD5 if not specified)
     */
    public static String hash(String in) {
        return hash(in, hashAlgorithm);
    }

    /**
     * 
     * @param in String to hash
     * @param algorithm hash algorithm 
     * 		  (see Message Digest Algorithms http://java.sun.com/j2se/1.4.2/docs/guide/security/CryptoSpec.html#AppA)
     * 		  if param is null the default MD5 is used
     * @return hashed string
     */
    public static String hash(String in, String algorithm) {
        if (StringUtils.isBlank(algorithm)) algorithm = DEFAULT_ALGORITHM;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("No such algorithm exception", nsae);
        }
        md.reset();
        md.update(in.getBytes());
        String out = null;
        try {
            out = Base64Encoder.encode(md.digest());
        } catch (IOException e) {
            logger.error("Error converting to Base64 ", e);
        }
        if (out.endsWith("\n")) out = out.substring(0, out.length() - 1);
        return out;
    }
}
