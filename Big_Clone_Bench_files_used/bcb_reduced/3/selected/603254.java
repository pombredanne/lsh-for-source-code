package com.landak.ipod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Util {

    /** Converts a java date as long to a mac date as long
	 *
	 * <PRE>
	 * mac date is seconds since 01/01/1904
	 * java date is milliseconds since 01/01/1970
	 * </PRE>
	 *
	 * @return date in mac format
	 * @param javaDate date in java format
	 */
    public static int dateToMacDate(int javaDate) {
        int macDate = 0;
        macDate = (javaDate / 1000) + 2082844800;
        return macDate;
    }

    /** Converts a date as long from iPod to a java date as long
	 *
	 * <PRE>
	 * mac date is seconds since 01/01/1904
	 * java date is milliseconds since 01/01/1970
	 * </PRE>
	 *
	 * @return date in java format
	 * @param macDate date on iPod
	 */
    public static int macDateToDate(int macDate) {
        int javaDate = 0;
        javaDate = (macDate - 2082844800) * 1000;
        return javaDate;
    }

    public static String md5sum(String fn) throws IOException {
        RandomAccessFile f = new RandomAccessFile(fn, "r");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] buf = new byte[2048];
        while (f.read(buf) != -1) {
            md.update(buf);
        }
        return Util.byteArrayToHexString(md.digest());
    }

    static String byteArrayToHexString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            out.append(pseudo[(int) ch]);
            ch = (byte) (in[i] & 0x0F);
            out.append(pseudo[(int) ch]);
            i++;
        }
        String rslt = new String(out);
        return rslt;
    }

    public static void copyfile(String src, String dst) throws IOException {
        dst = new File(dst).getAbsolutePath();
        new File(new File(dst).getParent()).mkdirs();
        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel dstChannel = new FileOutputStream(dst).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    public static int findId(ArrayList db, int id) {
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            if (fm.id == id) return i;
        }
        return -1;
    }

    public static int findPath(ArrayList db, String s) {
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            if (fm.path.compareTo(s) == 0) return i;
        }
        return -1;
    }

    public static int comparethe(String a, String b) {
        try {
            a = a.replaceAll("[0-9\\.\\-\\(\\)]", "").trim();
            b = b.replaceAll("[0-9\\.\\-\\(\\)]", "").trim();
            if (a.substring(0, 4).compareToIgnoreCase("the ") == 0) a = a.substring(4);
            if (b.substring(0, 4).compareToIgnoreCase("the ") == 0) b = b.substring(4);
        } catch (StringIndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
            return 0;
        }
        return a.compareToIgnoreCase(b);
    }

    public static String padding(String s, int len) {
        if (s == null) s = "";
        if (s.length() > len) return s.substring(0, len);
        for (int i = 0, n = len - s.length(); i < n; i++) {
            s = s + " ";
        }
        return s;
    }

    public static String padding(int in, int len) {
        String s = String.valueOf(in);
        if (s.length() > len) return s.substring(0, len);
        for (int i = 0, n = len - s.length(); i < n; i++) {
            s = " " + s;
        }
        return s;
    }
}
