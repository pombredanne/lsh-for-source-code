package ymsg.network;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ChallengeResponseUtility {

    private static final String Y64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789._";

    protected static MessageDigest md5Obj;

    static {
        try {
            md5Obj = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    static String yahoo64(byte[] buffer) {
        int limit = buffer.length - (buffer.length % 3);
        int pos = 0;
        String out = "";
        int[] buff = new int[buffer.length];
        for (int i = 0; i < buffer.length; i++) buff[i] = (int) buffer[i] & 0xff;
        for (int i = 0; i < limit; i += 3) {
            out = out + Y64.charAt(buff[i] >> 2);
            out = out + Y64.charAt(((buff[i] << 4) & 0x30) | (buff[i + 1] >> 4));
            out = out + Y64.charAt(((buff[i + 1] << 2) & 0x3c) | (buff[i + 2] >> 6));
            out = out + Y64.charAt(buff[i + 2] & 0x3f);
        }
        int i = limit;
        switch(buff.length - i) {
            case 1:
                out = out + Y64.charAt(buff[i] >> 2);
                out = out + Y64.charAt(((buff[i] << 4) & 0x30));
                out = out + "--";
                break;
            case 2:
                out = out + Y64.charAt(buff[i] >> 2);
                out = out + Y64.charAt(((buff[i] << 4) & 0x30) | (buff[i + 1] >> 4));
                out = out + Y64.charAt(((buff[i + 1] << 2) & 0x3c));
                out = out + "-";
                break;
        }
        return out;
    }

    static byte[] md5(String s) throws NoSuchAlgorithmException {
        return md5(s.getBytes());
    }

    static byte[] md5(byte[] buff) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(buff);
    }

    static byte[] md5Singleton(byte[] buff) throws NoSuchAlgorithmException {
        md5Obj.reset();
        return md5Obj.digest(buff);
    }

    static byte[] md5Crypt(String k, String s) {
        return UnixMD5Crypt.crypt(k, s).getBytes();
    }
}
