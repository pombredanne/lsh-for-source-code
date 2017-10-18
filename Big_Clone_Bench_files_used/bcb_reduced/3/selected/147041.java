package com.g2inc.scap.library.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtil {

    /**
	 * Return the sha1 of a file
	 * 
	 * @param f
	 * @return
	 */
    public static String sha1File(File f) {
        String ret = null;
        if (f == null || !f.isFile()) {
            throw new IllegalArgumentException("File was null or is not a file");
        }
        byte[] buff = new byte[16384];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            FileInputStream fin = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fin);
            DigestInputStream dis = new DigestInputStream(bis, md);
            while (dis.read() != -1) ;
            byte[] sha1Hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < sha1Hash.length; x++) {
                byte b = sha1Hash[x];
                sb.append(String.format("%02x", new Byte(b)));
            }
            dis.close();
            bis.close();
            fin.close();
            ret = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
