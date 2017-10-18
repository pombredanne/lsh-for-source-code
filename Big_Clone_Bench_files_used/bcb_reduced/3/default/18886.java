import java.io.*;
import java.security.*;

public class sha1 {

    private static final int BUFSIZE = 16 * 1024;

    public static void main(String[] args) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            FileInputStream fis = new FileInputStream(args[0]);
            byte[] data = new byte[BUFSIZE];
            do {
                int numread = fis.read(data);
                if (numread == -1) {
                    break;
                } else {
                    sha.update(data, 0, numread);
                }
            } while (true);
            fis.close();
            byte[] hash = sha.digest();
            System.out.println(stringForm(hash));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String stringForm(byte[] h) {
        String s = "";
        for (int i = 0; i < h.length; i++) {
            String prefix;
            int ub = h[i] & 0xff;
            if (ub <= 0xf) {
                prefix = "0";
            } else {
                prefix = "";
            }
            s += prefix + Integer.toHexString(ub);
        }
        return s;
    }
}
