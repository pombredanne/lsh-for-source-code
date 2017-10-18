package cn.vlabs.dlog.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

public class EventMD5Hash {

    private static MessageDigest md;

    private EventMD5Hash() {
    }

    /**
	 * ����md5
	 * 
	 * @param eventstr
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
    public static String eventHash(String eventstr) {
        try {
            if (md == null) {
                md = MessageDigest.getInstance("MD5");
            }
            md.update(eventstr.getBytes("utf-8"));
            byte[] theDigest = md.digest();
            return new BASE64Encoder().encode(theDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
