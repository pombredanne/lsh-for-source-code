package com.phototeque;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PTChecksum {

    /**
	 * Calcul le checksum SHA1 d'un fichier 
	 * @param file
	 * 		Le nom du fichier pour lequel on souhaite un checksum
	 * @return
	 * 		Une chaine de caractère contenant la représentation hexadécimale du checksum
	 */
    public static String computeSHA1(String file) {
        return computeSHA1(new File(file));
    }

    /**
	 * Calcul le checksum SHA1 d'un fichier 
	 * @param file
	 * 		Le fichier pour lequel on souhaite un checksum
	 * @return
	 * 		Une chaine de caractère contenant la représentation hexadécimale du checksum
	 */
    public static String computeSHA1(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(file);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = md.digest();
            StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
