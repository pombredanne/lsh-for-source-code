package org.bresearch.websec.utils.botlist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * This is class is used by botverse.
 * 
 * Usage: BotListUniqueId.getUniqueId())
 * @author Berlin Brown
 *
 */
public class BotlistUniqueId {

    private String toHexString(byte[] bytes) {
        char[] ret = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int c = (int) bytes[i];
            if (c < 0) {
                c += 0x100;
            }
            ret[j++] = Character.forDigit(c / 0x10, 0x10);
            ret[j++] = Character.forDigit(c % 0x10, 0x10);
        }
        return new String(ret);
    }

    public String getUniqueId() {
        String digest = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String timeVal = "" + (System.currentTimeMillis() + 1);
            String localHost = "";
            ;
            try {
                localHost = InetAddress.getLocalHost().toString();
            } catch (UnknownHostException e) {
            }
            String randVal = "" + new Random().nextInt();
            String val = timeVal + localHost + randVal;
            md.reset();
            md.update(val.getBytes());
            digest = toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
        }
        return digest;
    }
}
