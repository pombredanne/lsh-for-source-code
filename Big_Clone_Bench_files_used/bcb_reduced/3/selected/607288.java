package ch.unibe.a3ubAdmin.persistence.serializedtables;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This singleton calculates a special one-way-hash. It is intended to generate
 * the password-hashes for the local accounts in a3ub
 * 
 * @author daniel marthaler
 * @version 1.0 / last change: 4.10.2006
 * @since JDK 1.5.0
 */
public class HashUtil {

    private static HashUtil instance = null;

    private MessageDigest digester1 = null;

    private MessageDigest digester2 = null;

    /**
	 * Private constructor from the singleton
	 * 
	 * @throws Exception
	 */
    private HashUtil() {
        try {
            digester1 = MessageDigest.getInstance("MD5");
            digester2 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
	 * returns the singele instance
	 * 
	 * @return PersistenceManager
	 * @throws Exception
	 */
    public static synchronized HashUtil getInstance() throws Exception {
        if (instance == null) {
            instance = new HashUtil();
        }
        return instance;
    }

    public String digest(String string) throws Exception {
        byte digest[] = messageDigest(string + "8*erut724oeuc:", digester1);
        String s = "";
        for (int i = 0; i < digest.length; i++) {
            s = s + Integer.toHexString(digest[i] & 0xFF);
        }
        byte[] digest2 = messageDigest(s, digester2);
        s = "";
        for (int i = 0; i < digest.length; i++) {
            s = s + Integer.toHexString(digest2[i] & 0xFF);
        }
        return s;
    }

    private byte[] messageDigest(String string, MessageDigest digester) throws Exception {
        byte md[] = new byte[8192];
        StringBuffer StringBuffer1 = new StringBuffer(string);
        ByteArrayInputStream bis1 = new ByteArrayInputStream(StringBuffer1.toString().getBytes("UTF-8"));
        InputStream in = bis1;
        for (int n = 0; (n = in.read(md)) > -1; ) digester.update(md, 0, n);
        return digester.digest();
    }
}
