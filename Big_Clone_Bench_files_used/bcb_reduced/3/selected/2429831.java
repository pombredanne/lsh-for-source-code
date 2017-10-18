package ecks;

import ecks.Threads.EmailThread;
import ecks.services.SrvAuth;
import sun.misc.BASE64Encoder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class util {

    public static List<Thread> threads;

    /**
     * Method getVersion returns the version of ecks
     *
     * @return the version (type String)
     */
    public static String getVersion() {
        return "0.7B";
    }

    public static Thread startThread(Thread whattostart) {
        if (threads == null) threads = new ArrayList<Thread>();
        threads.add(whattostart);
        Logging.verbose("THREADING", "New thread created!");
        return whattostart;
    }

    public static List<Thread> getThreads() {
        return threads;
    }

    public static synchronized String pad(String s, int n) {
        return paddingString(s, n, ' ', false);
    }

    /**
     * Method getTS returns the current unix time
     *
     * @return the TS (type String)
     */
    public static String getTS() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    public static synchronized String paddingString(String s, int n, char c, boolean paddingLeft) {
        StringBuffer str = new StringBuffer(s);
        int strLength = str.length();
        if (n > 0 && n > strLength) {
            for (int i = 0; i <= n; i++) {
                if (paddingLeft) {
                    if (i < n - strLength) str.insert(0, c);
                } else {
                    if (i > strLength) str.append(c);
                }
            }
        }
        return str.toString();
    }

    public static long ip2long(InetAddress ip) {
        long l = 0;
        byte[] addr = ip.getAddress();
        if (addr.length == 4) {
            for (int i = 0; i < 4; ++i) l += (((long) addr[i] & 0xFF) << 8 * (3 - i));
        } else {
            return 0;
        }
        return l;
    }

    public static boolean checkemail(String input) {
        return true;
    }

    public static boolean sanitize(String input) {
        return !input.contains("\"") && !input.contains("\'") && input.matches("(\\w+)*");
    }

    public static void SendRegMail(String to, String code) {
        startThread(new Thread(new EmailThread(to, code))).start();
    }

    public static String readFileAsString(String filePath) throws java.io.IOException {
        StringBuffer sb = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] chars = new char[1024];
        int numRead = 0;
        while (numRead > -1) {
            numRead = reader.read(chars);
            sb.append(String.valueOf(chars));
        }
        reader.close();
        return sb.toString();
    }

    public static String makeCookie() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String pass = "";
        for (int x = 0; x < 10; x++) {
            int i = (int) Math.floor(Math.random() * 62);
            pass += chars.charAt(i);
        }
        return pass;
    }

    public static String encodeUTF(String what) {
        String result = null;
        try {
            result = URLEncoder.encode(what, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    public static boolean checkaccess(String user, int level) {
        return ((SrvAuth) Configuration.getSvc().get(Configuration.authservice)).checkAccess(user.toLowerCase()).ordinal() >= level;
    }

    public static String decodeUTF(String what) {
        String result = null;
        try {
            result = URLDecoder.decode(what, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    public static synchronized String hash(String plaintext) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        byte raw[] = md.digest();
        return (new BASE64Encoder()).encode(raw);
    }

    static class CustomException extends Exception {

        CustomException(String message) {
            super(message);
        }
    }

    public static String long2string(Long i) {
        double in = i.doubleValue();
        double SIP1 = in / Math.pow(256, 3);
        double SIP2 = ((in % Math.pow(256, 3)) / Math.pow(256, 2));
        double SIP3 = (((in % Math.pow(256, 3)) % Math.pow(256, 2)) / Math.pow(256, 1));
        double SIP4 = ((((in % Math.pow(256, 3)) % Math.pow(256, 2)) % Math.pow(256, 1)) / Math.pow(256, 0));
        return (int) SIP1 + "." + (int) SIP2 + "." + (int) SIP3 + "." + (int) SIP4;
    }

    public static long ip2long(String s) {
        return (convert1(s));
    }

    public static long convert1(String s) {
        long IP = 0x00;
        char[] data = s.toCharArray();
        for (int i = 0; i < data.length; i++) {
            char c = data[i];
            int b = 0x00;
            while (c != '.') {
                b = b * 10 + c - '0';
                if (++i >= data.length) break;
                c = data[i];
            }
            IP = (IP << 8) + b;
        }
        return IP;
    }
}
