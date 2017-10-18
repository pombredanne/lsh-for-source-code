package org.inasnet.util;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

/**
 * Utility class with different needed methods.
 */
public class Util {

    /**
	 * Load file and return String of all file.
	 */
    public static String loadFile(String filename) {
        try {
            FileInputStream stream = new FileInputStream(filename);
            int bytes = stream.available();
            byte[] bytesArray = new byte[bytes];
            stream.read(bytesArray);
            return new String(bytesArray, 0, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String hashMD5(String s) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] theTextToDigestAsBytes = s.getBytes("8859_1");
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(theTextToDigestAsBytes);
        byte[] digest = messageDigest.digest();
        String code = new String();
        for (int i = 0; i < digest.length; i++) {
            code += "" + Integer.toHexString(digest[i] & 0xff);
        }
        return code;
    }

    /**
	 * Load image from special root/images direcory.
	 */
    public static BufferedImage loadImage(String imagefilename) {
        return mUtilExt.loadImage(imagefilename);
    }

    public static String bytesToString(byte[] message, int limit) {
        if (message == null) return "(null)"; else {
            int lim = message.length;
            if (limit < lim && limit != -1) lim = limit;
            String result = "(";
            for (int i = 0; i < lim; i++) {
                result += message[i];
                if (i + 1 < lim) {
                    result += ",";
                }
            }
            if (lim != message.length) result += "...";
            result += ")";
            return result;
        }
    }

    static UtilExt mUtilExt = new UtilExt();
}

class UtilExt {

    public BufferedImage loadImage(String imagefilename) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResource("\\..\\..\\..\\..\\images\\" + imagefilename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
