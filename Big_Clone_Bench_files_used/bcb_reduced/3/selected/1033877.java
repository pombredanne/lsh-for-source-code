package tristero.util;

import java.io.*;
import java.util.*;

public class StringUtils {

    public static void main(String[] args) throws Exception {
        printBinary("test", (byte) (-72));
        String s = new String(hash("hi!".getBytes()));
        System.out.println(s);
        s = byteToHex(s.getBytes());
        System.out.println(s);
        s = hexToByte(s);
        System.out.println(s);
    }

    public static byte[] hash(byte[] key) throws Exception {
        SHA1 sha = new SHA1();
        sha.init();
        sha.update(key, 0, key.length);
        sha.finish();
        return sha.digest();
    }

    public static void printBinary(String prefix, byte b) {
        System.out.print(prefix + " ");
        if (b < 0) {
            System.out.print("1");
            b = (byte) (b * -1);
        } else {
            System.out.print("0");
        }
        if ((b - 64) >= 0) {
            System.out.print("1");
            b = (byte) (b - 64);
        } else System.out.print("0");
        if ((b - 32) >= 0) {
            System.out.print("1");
            b = (byte) (b - 32);
        } else System.out.print("0");
        if ((b - 16) >= 0) {
            System.out.print("1");
            b = (byte) (b - 16);
        } else System.out.print("0");
        if ((b - 8) >= 0) {
            System.out.print("1");
            b = (byte) (b - 8);
        } else System.out.print("0");
        if ((b - 4) >= 0) {
            System.out.print("1");
            b = (byte) (b - 4);
        } else System.out.print("0");
        if ((b - 2) >= 0) {
            System.out.print("1");
            b = (byte) (b - 2);
        } else System.out.print("0");
        if ((b - 1) >= 0) {
            System.out.print("1");
            b = (byte) (b - 1);
        } else System.out.print("0");
        System.out.println();
    }

    public static String byteToHex(byte[] b) throws Exception {
        StringBuffer buff = new StringBuffer();
        for (int x = 0; x < b.length; x++) {
            int i = b[x];
            if (i < 0) i = i + 256;
            if (i < 16) buff.append("0");
            buff.append(Integer.toString(i, 16));
        }
        return buff.toString();
    }

    public static String hexToByte(String in) {
        int len = in.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Even length string expected.");
        }
        byte[] out = new byte[len / 2];
        try {
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) (Integer.parseInt(in.substring(i * 2, i * 2 + 2), 16));
            }
        } catch (NumberFormatException doh) {
            doh.printStackTrace();
            throw new IllegalArgumentException("ParseError");
        }
        return new String(out);
    }

    public static Vector split(String path, String delim) {
        int i;
        Vector parts = new Vector();
        if (path == null) {
            return parts;
        }
        while (path.length() > 0) {
            i = path.indexOf(delim);
            if (i == -1) {
                parts.addElement(path);
                break;
            } else {
                parts.addElement(path.substring(0, i));
                path = path.substring(i + 1);
            }
        }
        return parts;
    }

    public static String replace(String s, String olds, String news) {
        StringBuffer rb = new StringBuffer("");
        int i, j;
        i = 0;
        while ((j = s.indexOf(olds, i)) != -1) {
            rb.append(s.substring(i, j));
            rb.append(news);
            i = j + olds.length();
        }
        rb.append(s.substring(i));
        return rb.toString();
    }

    public static String join(String a, String b, String c, String d) {
        Vector v = new Vector();
        v.addElement(a);
        v.addElement(b);
        v.addElement(c);
        return join(v, d);
    }

    public static String join(String a, String b, String c) {
        Vector v = new Vector();
        v.addElement(a);
        v.addElement(b);
        return join(v, c);
    }

    public static String join(String[] sarr, String delim) {
        Vector v = new Vector();
        for (int x = 0; x < sarr.length; x++) v.add(sarr[x]);
        return join(v, delim);
    }

    public static String join(List v, String delim) {
        StringBuffer buff = new StringBuffer();
        Iterator iterator = v.iterator();
        while (iterator.hasNext()) {
            String s = (String) iterator.next();
            buff.append(s);
            if (iterator.hasNext()) buff.append(delim);
        }
        return buff.toString();
    }

    public static boolean detectReply(String subject) {
        subject = subject.toLowerCase();
        if (subject.startsWith("re:")) return true;
        if (subject.startsWith("re[")) return true;
        return false;
    }

    /** A method that strips off Re: and such from a message.
   *  This really should go in MailUtils, but there currently is no
   *  such thing.
   */
    public static String stripSubject(String subject) {
        if (subject == null) {
            return null;
        }
        String temp = subject.toLowerCase();
        boolean done = false;
        while (!done) {
            done = true;
            temp = subject.toLowerCase();
            if (temp.startsWith("re:")) {
                done = false;
                subject = subject.substring(3).trim();
                continue;
            }
            if (temp.startsWith("re[")) {
                done = false;
                subject = subject.substring(3);
                int i = subject.indexOf(']');
                if (i == -1) {
                    return subject.trim();
                }
                subject = subject.substring(i + 2).trim();
            }
        }
        return subject;
    }
}
