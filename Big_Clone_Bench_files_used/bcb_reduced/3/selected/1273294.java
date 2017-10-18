package org.citycarshare.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.security.*;

/**
 * Abstracts the Encryption routines into an easily callable thing
 *
 * @author Dan Aronson, dan@citycarshare.org
 * @version $Id: Encrypt.java,v 1.1 2002/07/17 21:03:51 daronson Exp $
 */
public class Encrypt {

    private static final String ourEncryptionMethod = "MD5";

    private static Log ourLog = LogFactory.getLog(Encrypt.class.getName());

    public static String hash(String argIn) {
        StringBuffer sb = new StringBuffer();
        String s = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(ourEncryptionMethod);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeEncapsulatingException(e);
        }
        byte[] result = md.digest(argIn.getBytes());
        for (int i = 0; i < result.length; i++) {
            s = Integer.toHexString((int) result[i] & 0xff);
            if (s.length() < 2) sb.append('0');
            sb.append(s);
        }
        return (sb.toString());
    }
}
