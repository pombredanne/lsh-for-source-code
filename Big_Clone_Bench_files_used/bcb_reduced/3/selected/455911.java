package core.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * @author Glauber Magalh�es Pires
 *
 */
public final class CryptoUtil {

    /**
     *  This method is used to reduce the size of the crypto key by using CRC32 of the password.
     *  This is to avoid the users to need to install "Unlimited Strength Java(TM) Cryptography
     *  Extension Policy Files". As CRC32 is a many-to-one function it reduce the security of
     *  the encryptation.<br/>
     *
     *  So If you really need security simple don`t use this class and INSTALL the "Unlimited
     *  Strength Java(TM) Cryptography Extension Policy Files" in every computer that will use it
     *  to avoid key size problems.
    */
    public static final String CRC32(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes());
        BigInteger newValue = new BigInteger(crc32.getValue() + "");
        return newValue.toString(36);
    }

    public static final String MD5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(value.getBytes());
            BigInteger hash = new BigInteger(1, md.digest());
            String newValue = hash.toString(16);
            return newValue;
        } catch (NoSuchAlgorithmException ns) {
            ns.printStackTrace();
            return null;
        }
    }
}
