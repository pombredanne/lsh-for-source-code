package cl.vhf.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Marcelo
 */
public class Util {

    public static String devuelveHash(String txtOrigen) {
        String devuelve = "";
        txtOrigen = txtOrigen.trim();
        txtOrigen = txtOrigen.toUpperCase();
        try {
            BASE64Encoder encode = new BASE64Encoder();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] input = txtOrigen.getBytes("UTF8");
            sha.update(input);
            byte[] myhash = sha.digest();
            devuelve = encode.encode(myhash);
        } catch (Exception e) {
            System.out.println("error : " + e);
        }
        return devuelve;
    }

    public static final float redondeaDecimal(float number, int decimals) {
        BigDecimal bd = new BigDecimal(number);
        bd = bd.setScale(decimals, RoundingMode.HALF_EVEN);
        return bd.floatValue();
    }
}
