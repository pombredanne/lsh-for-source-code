package ajaxnet4j;

import java.security.*;

final class MD5Helper {

    static String GetHash(String data) {
        byte[] bytes = data.getBytes();
        return GetHash(bytes);
    }

    private static final char[] hexes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String GetHash(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception ex) {
        }
        byte[] result = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length; i++) {
            int n = (int) result[i];
            if (n < 0) {
                n = n + 256;
            }
            int a = n / 16;
            int b = n % 16;
            sb.append(hexes[a]);
            sb.append(hexes[b]);
        }
        return sb.toString();
    }
}
