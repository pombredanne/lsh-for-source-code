package org.neodatis.odb.tool;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.OdbConfiguration;
import org.neodatis.odb.core.NeoDatisError;

/**
 * @sharpen.ignore
 * A simple cypher. Used to cypher password of the NeoDatis ODB Database.
 * @author osmadja
 *
 */
public class Cryptographer {

    private static MessageDigest md = null;

    public Cryptographer() {
        super();
    }

    private static synchronized void checkInit() {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new ODBRuntimeException(NeoDatisError.CRYPTO_ALGORITH_NOT_FOUND);
            }
        }
    }

    public static String encrypt(String string) {
        if (string == null) {
            return null;
        }
        String encoding = OdbConfiguration.getDatabaseCharacterEncoding();
        checkInit();
        try {
            md.update(string.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            return new String(md.digest(), encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new String(md.digest());
    }
}
