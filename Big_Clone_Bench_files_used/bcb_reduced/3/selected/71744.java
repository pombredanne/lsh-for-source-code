package unibg.overencrypt.core;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.GregorianCalendar;

/**
 * Class that provides static utility methods 
 * @author Flavio
 *
 */
public class Utility {

    /**
    * Copy a byte's array ( java 1.5 )
    * @param original - original array
    * @param length - length of the original array
    * @return byte[] - copy of the array
    */
    public static byte[] arrayCopy(byte[] original, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(original, 0, copy, 0, length);
        return copy;
    }

    /**
	 * Get the user key
	 * @return user key - key of the user
	 * @throws Exception in case of failed retrieve
	 */
    public static final byte[] getKey(String username) throws Exception {
        FileInputStream fin;
        DataInputStream dis;
        fin = new FileInputStream("Key" + File.separatorChar + username);
        dis = new DataInputStream(fin);
        byte[] userKey = new byte[16];
        dis.read(userKey);
        fin.close();
        return userKey;
    }

    /**
	 * Convert a string with date with yyyy/MM/dd format into a Date object
	 *  @param date - string to convert
	 *  @return Date - converted Date object
	 */
    public static Date stringToDate(String date) {
        String[] temp = null;
        temp = date.split("/");
        try {
            if (temp.length == 3) {
                int year = Integer.parseInt(0 + temp[0]);
                int month = Integer.parseInt(0 + temp[1]);
                int day = Integer.parseInt(0 + temp[2]);
                GregorianCalendar cal = new GregorianCalendar(year, month - 1, day);
                cal.setLenient(false);
                return new java.sql.Date(cal.getTimeInMillis());
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
	 * Get the byte's array containing the result of hashing with SHA-1 Algorithm of the passed text
	 * @param text - text to hashing
	 * @return byte[] - containing the result of hash
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    public static byte[] SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return sha1hash;
    }

    /**
	 * Convert a byte in hex values and write the into a StringBuffer
	 * @param b - Byte to convert
	 * @param buf- StringBuffer in which add the converted hex values
	 */
    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    public static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
	 * Convert a byte array into a hex string
	 * @param	byte[]	block	-	byte array to convert
	 * @return	String			-	converted hex string
	 */
    public static String toStr(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
        }
        return buf.toString();
    }

    /**
	 * Convert a hex string into a byte array
	 * 
	 * @param	String	strToConvert	-	hex string
	 * @return	byte[]					-	converted byte array
	 */
    public static byte[] toBytes(String strToConvert) {
        byte[] byteSeq = new byte[strToConvert.length() / 2];
        for (int i = 0; i < strToConvert.length() / 2; i++) {
            byteSeq[i] = (byte) 0xff;
            switch(strToConvert.charAt(i * 2)) {
                case '0':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x0f);
                    break;
                case '1':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x1f);
                    break;
                case '2':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x2f);
                    break;
                case '3':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x3f);
                    break;
                case '4':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x4f);
                    break;
                case '5':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x5f);
                    break;
                case '6':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x6f);
                    break;
                case '7':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x7f);
                    break;
                case '8':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x8f);
                    break;
                case '9':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0x9f);
                    break;
                case 'A':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xaf);
                    break;
                case 'B':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xbf);
                    break;
                case 'C':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xcf);
                    break;
                case 'D':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xdf);
                    break;
                case 'E':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xef);
                    break;
                case 'F':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xff);
                    break;
            }
            switch(strToConvert.charAt((i * 2) + 1)) {
                case '0':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf0);
                    break;
                case '1':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf1);
                    break;
                case '2':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf2);
                    break;
                case '3':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf3);
                    break;
                case '4':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf4);
                    break;
                case '5':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf5);
                    break;
                case '6':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf6);
                    break;
                case '7':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf7);
                    break;
                case '8':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf8);
                    break;
                case '9':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xf9);
                    break;
                case 'A':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xfa);
                    break;
                case 'B':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xfb);
                    break;
                case 'C':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xfc);
                    break;
                case 'D':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xfd);
                    break;
                case 'E':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xfe);
                    break;
                case 'F':
                    byteSeq[i] = (byte) (byteSeq[i] & (byte) 0xff);
                    break;
            }
        }
        return byteSeq;
    }
}
