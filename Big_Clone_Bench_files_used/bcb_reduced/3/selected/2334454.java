package joggle.data;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author  $Author: mosterme@gmail.com $
 * @version $Revision: 22 $
 */
public class Serializer {

    private static final Gson json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private static final Gson string = new GsonBuilder().serializeNulls().create();

    private static final String hexs = "0123456789ABCDEF";

    public static String decode(String url) {
        if (url == null) return null;
        byte[] bytes = url.getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b == '+') bos.write(' '); else if (b == '%') {
                int u = Character.digit((char) bytes[++i], 16);
                int l = Character.digit((char) bytes[++i], 16);
                bos.write((char) ((u << 4) + l));
            } else bos.write(b);
        }
        return bos.toString();
    }

    public static final String hash(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(s.getBytes());
        return hex(hash);
    }

    public static final String hex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder s = new StringBuilder(2 * bytes.length);
        for (final byte b : bytes) s.append(hexs.charAt((b & 0xF0) >> 4)).append(hexs.charAt((b & 0x0F)));
        return s.toString();
    }

    public static String normalize(String s) {
        String u = s.toUpperCase();
        return Normalizer.normalize(u, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    public static String toJson(Object o) {
        return json.toJson(o);
    }

    public static String toString(Object o) {
        return o.getClass().getSimpleName() + string.toJson(o);
    }
}
