package org.skins.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.skins.io.HexReader;

public class Util {

    public static String calcolaMD5(byte[] stream) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] b = messageDigest.digest(stream);
            String md5 = HexReader.bytesToHex(b);
            return md5;
        } catch (NoSuchAlgorithmException e) {
            return "errore md5 " + e.getMessage();
        }
    }

    public static Class[] getTypes(Object[] obj) {
        Class[] c = new Class[obj.length];
        for (int i = 0; i < obj.length; i++) {
            Object object = obj[i];
            if (object == null) {
                throw new RuntimeException("Oggetto nullo impossibile determinarne il tipo!");
            }
            c[i] = object.getClass();
        }
        return c;
    }
}
