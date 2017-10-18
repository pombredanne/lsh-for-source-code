package edu.chop.bic.cnv.util;

import edu.chop.bic.cnv.domain.InternalCnvException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

/**
 * User: ohara
 * Date: Jun 18, 2008
 * Time: 10:57:22 AM
 */
public class Encrypter {

    /**
     * Returns the MD5 hash of the passed argument in hexadecimal representation, *with any leading zeroes stripped off*!
     * I.e. the length of the returned string is not always 32.
     * @param string String to compute MD5 on.
     * @return MD5 digest.
     * @throws InternalCnvException  when the argument is null or empty.
     */
    public static String toMd5(String string) throws InternalCnvException {
        String encrypted;
        if (string == null || string.equals("")) {
            throw new InternalCnvException("Missing non-empty string to call toMd5() on.");
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(string.getBytes());
                BigInteger number = new BigInteger(1, messageDigest);
                encrypted = number.toString(16);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return encrypted;
    }
}
