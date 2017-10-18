package com.gmvc.server.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Kullanici sifre dogrulugunu kontrol etmek icin bu sinif kullanilir. 
 * Benzer bir yapi client tarafinda da javascript kodlari ile yazildi
 * 
 * @author mdpinar
 * 
 */
public final class Hash {

    private static char hexChars[];

    private static MessageDigest digest;

    private static final Logger log = Logger.getLogger(Hash.class);

    /**
	 * Ilk erisimde yapilan atamalar
	 * 
	 */
    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        hexChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    }

    /**
	 * Ornegi alinamaz
	 * 
	 */
    private Hash() {
        ;
    }

    /**
	 * Verilen duz string veriyi md5 dizesine cevirir, sifre denetiminde kullaniliyor
	 *  
	 * @param data duz string data
	 * @return md5 dizesine cevrilmis data
	 * 
	 */
    public static String md5(String data) {
        String result = "";
        if (data == null || data.isEmpty()) return null;
        int lsb = 0;
        digest.update(data.getBytes());
        byte b[] = digest.digest();
        for (int i = 0; i < b.length; i++) {
            int msb = (b[i] & 0xff) / 16;
            lsb = (b[i] & 0xff) % 16;
            result = (new StringBuilder()).append(result).append(hexChars[msb]).append(hexChars[lsb]).toString();
        }
        return result.toString();
    }
}
