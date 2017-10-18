package it.cefriel.glue2.util;

import it.cefriel.glue2.exceptions.GlueException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoaderUtil {

    public static String retrieve_content(URL address) throws GlueException {
        String content = "";
        String str = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(address.openStream()));
            while ((str = in.readLine()) != null) {
                str += "\n";
                content += str;
            }
            in.close();
            return content;
        } catch (IOException e) {
            throw new GlueException("Error parsing wsml document, failed to retrieve content");
        }
    }

    public static BufferedReader retrieve_content_reader(URL address) throws GlueException {
        try {
            return new BufferedReader(new InputStreamReader(address.openStream()));
        } catch (IOException e) {
            throw new GlueException("Error parsing wsml document, failed to retrieve content", e);
        }
    }

    public static String get_hash(String content) {
        String hash = null;
        String md5val = "";
        MessageDigest algorithm = null;
        StringBuffer hexString = null;
        byte messageDigest[] = null;
        byte[] content_bytes = content.getBytes();
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            hash = "";
        }
        if (algorithm != null) {
            hexString = new StringBuffer();
            algorithm.reset();
            algorithm.update(content_bytes);
            messageDigest = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            hash = hexString.toString();
        }
        return hash;
    }
}
