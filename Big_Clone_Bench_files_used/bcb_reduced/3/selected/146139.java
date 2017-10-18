package com.bluesky.jwf.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;

public class ResourceUtil {

    /**
	 * compress support
	 * 
	 * @param obj
	 * @return
	 */
    public static byte[] writeObjectToByteArray(Object obj) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GZIPOutputStream zos = new GZIPOutputStream(os);
            writeObjectToStream(obj, zos);
            zos.flush();
            zos.close();
            byte[] bytes = os.toByteArray();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64Encode(byte[] buf) {
        Base64 base64 = new Base64();
        String s = base64.encodeBase64String(buf);
        return s;
    }

    public static Object readObjectFromByteArray(byte[] buf) {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(buf);
            GZIPInputStream zis = new GZIPInputStream(is);
            return readObjectFromStream(zis);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] base64Decode(String s) {
        byte[] buf = new Base64().decodeBase64(s);
        return buf;
    }

    private static void writeObjectToStream(Object obj, OutputStream stream) {
        try {
            ObjectOutputStream ooStream;
            ooStream = new ObjectOutputStream(stream);
            ooStream.writeObject(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeObjectToFile(Object obj, String fileName) {
        try {
            FileOutputStream os = new FileOutputStream(fileName);
            writeObjectToStream(obj, os);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object readObjectFromStream(InputStream stream) {
        try {
            ObjectInputStream objectStream = new ObjectInputStream(stream);
            return objectStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Object readObjectFromFile(String fileName) {
        try {
            FileInputStream stream = new FileInputStream(fileName);
            return readObjectFromStream(stream);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static byte[] md5(byte[] buf) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(buf);
            byte[] hash = digest.digest();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        byte[] buf = new byte[2];
        buf[0] = 1;
        buf[0] = 2;
        System.out.println(md5(buf));
    }
}
