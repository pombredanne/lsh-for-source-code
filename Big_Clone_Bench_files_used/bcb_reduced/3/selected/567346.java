package ua.org.nuos.sdms.middle.util.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA.
 * User: dio
 * Date: 06.11.11
 * Time: 14:00
 * To change this template use File | Settings | File Templates.
 */
public class MD5 implements Hash {

    @Override
    public String getHash(String text) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(text.getBytes());
        BigInteger hash = new BigInteger(1, md5.digest());
        return hash.toString(16);
    }
}
