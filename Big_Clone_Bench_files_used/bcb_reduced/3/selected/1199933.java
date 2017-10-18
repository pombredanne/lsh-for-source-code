package jvmshare.server.core.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;
import jvmshare.server.core.delegate.ClientInfo;

public class ServerUtil {

    public static String generateToken(ClientInfo clientInfo) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            Random rand = new Random();
            String random = clientInfo.getIpAddress() + ":" + clientInfo.getPort() + ":" + rand.nextInt();
            md5.update(random.getBytes());
            String token = toHexString(md5.digest((new Date()).toString().getBytes()));
            clientInfo.setToken(token);
            return token;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
}
