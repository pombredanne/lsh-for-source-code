package de.lamasep.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Utility Klasse für Hashfunktionen.
 */
public final class Hash {

    private static final Logger log = Logger.getLogger(Hash.class);

    /**
     * Privater Konstruktur für Utility Klasse.
     */
    private Hash() {
    }

    /**
     * Erzeugt für den übergebenen String einen MD5-Hash und gibt diesen als
     * String aus Hexadezimalzeichen zurück.
     * @param source String für den ein Hash berechnet werden soll.
     * @return Hashwert als String in Hexadezimal-Darstellung oder
     *          <code>null</code> wenn der MD5-Algorithmus nicht
     *          zur Verfügung steht.
     */
    public static String md5String(final String source) {
        byte[] sourceBytes = source.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(sourceBytes);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            log.warn(e);
            return null;
        }
        return hexString.toString();
    }
}
