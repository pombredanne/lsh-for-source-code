package espider.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Random;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;

/**
 * @author christophe
 *
 */
public class Utils {

    private static MessageDigest messageDigest = null;

    private static final NumberFormat numberFormat = NumberFormat.getInstance();

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Le syst�me ne supporte pas le MD5... et il le faut");
            System.exit(1);
        }
    }

    /**
	 * Repris sur Koders.com
	 * Je sais pas trop ce que ca fait, j'ai pas non plus cherch� � comprendr.. :)
	 * 
	 * 
	 * @param str
	 * @return
	 */
    public static String computeMD5(String str) {
        return computeMD5(str.getBytes());
    }

    /**
	 * 
	 * @param bytes
	 * @return
	 */
    public static String computeMD5(byte[] bytes) {
        byte[] md5Bytes = messageDigest.digest(bytes);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = md5Bytes[i] & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    /**
	 * Convert an int to a byte array
	 * 
	 * @param value
	 * @return
	 */
    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    /**
     * Convert the byte array to an int.
     *
     * @param b The byte array
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     * 
     * @return
     */
    public static boolean isSWTBrowserEnabled() {
        Browser browser = null;
        boolean isSWTBrowserEnabled = false;
        try {
            browser = new Browser(Display.getCurrent().getShells()[0], SWT.NONE);
            isSWTBrowserEnabled = true;
            browser.dispose();
        } catch (SWTError e) {
        }
        return isSWTBrowserEnabled;
    }

    /**
     * 
     * @return
     */
    public static String getMyIP() {
        String ip = "";
        try {
            InputStream in = new URL("http://www.whatismyip.org/").openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            ip = br.readLine().trim();
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException uhe) {
                ip = "";
            }
        }
        return ip;
    }

    /**
     * 
     * @param number
     * @return
     */
    public static String formatNumer(long number) {
        return numberFormat.format(number);
    }

    /**
     * 
     * @param number
     * @return
     */
    public static String formatNumer(double number) {
        return numberFormat.format(number);
    }

    public static String generateID() {
        long time = System.currentTimeMillis();
        Random r = new Random(time);
        return Utils.computeMD5(String.valueOf(time).concat(String.valueOf(r.nextLong())));
    }

    public static String addSlashes(String str) {
        if (str == null) return "";
        StringBuffer s = new StringBuffer((String) str);
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\'') s.insert(i++, '\\');
        return s.toString();
    }
}
