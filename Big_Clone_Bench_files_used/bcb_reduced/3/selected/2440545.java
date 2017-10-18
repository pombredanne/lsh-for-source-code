package org.jecars.jaas;

import java.io.UnsupportedEncodingException;
import java.security.*;
import org.jecars.support.BASE64Encoder;

/**
 * CARS_PasswordService
 *
 * @version $Id: CARS_PasswordService.java,v 1.1 2007/09/26 14:14:38 weertj Exp $
 */
public final class CARS_PasswordService {

    private static CARS_PasswordService gInstance;

    private CARS_PasswordService() {
    }

    /** encrypt
   *
   * @param pPassword
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnsupportedEncodingException
   */
    public synchronized String encrypt(final String pPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(pPassword.getBytes("UTF-8"));
        final byte raw[] = md.digest();
        return BASE64Encoder.encodeBuffer(raw);
    }

    public static synchronized CARS_PasswordService getInstance() {
        if (gInstance == null) {
            return new CARS_PasswordService();
        } else {
            return gInstance;
        }
    }
}
