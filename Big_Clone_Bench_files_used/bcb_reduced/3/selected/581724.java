package com.freeture.frmwk.cryptographie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.freeture.frmwk.utils.Converter;

public class HashCode {

    public static final String SHA1 = "SHA-1";

    public static final String MD5 = "MD5";

    public static final String SHA256 = "SHA-256";

    public static final String SHA384 = "SHA-384";

    public static final String SHA512 = "SHA-512";

    private static final Log logger = LogFactory.getLog(HashCode.class);

    /**
     * MD2: The MD2 message digest algorithm as defined in RFC 1319. MD5: The
     * MD5 message digest algorithm as defined in RFC 1321. SHA-1: The Secure
     * Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1.
     * SHA-256, SHA-384, and SHA-512: New hash algorithms for which the draft
     * Federal Information Processing Standard 180-2, Secure Hash Standard (SHS)
     * is now available. SHA-256 is a 256-bit hash function intended to provide
     * 128 bits of security against collision attacks, while SHA-512 is a
     * 512-bit hash function intended to provide 256 bits of security. A 384-bit
     * hash may be obtained by truncating the SHA-512 output.
     * 
     * @param str
     * @return String
     */
    public static String toMD5(String str) {
        return Converter.toString(toEncode(MD5, str));
    }

    public static String toSHA1(String str) {
        return Converter.toString(toEncode(SHA1, str));
    }

    public static String toSHA256(String str) {
        return Converter.toString(toEncode(SHA256, str));
    }

    public static String toSHA384(String str) {
        return Converter.toString(toEncode(SHA384, str));
    }

    public static String toSHA512(String str) {
        return Converter.toString(toEncode(SHA512, str));
    }

    public static byte[] toEncode(String algo, String chaine) {
        try {
            byte[] hash = MessageDigest.getInstance(algo).digest(chaine.getBytes());
            return hash;
        } catch (NoSuchAlgorithmException exception) {
            logger.error("Erreur : l'algo " + algo + " du MessageDigest est indisponible");
        }
        return null;
    }
}
