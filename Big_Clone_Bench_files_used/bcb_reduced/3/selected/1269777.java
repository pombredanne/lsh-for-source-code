package it.unibo.deis.uniboEnv_p2p.application.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

public class QosClass {

    protected static Logger logger = Logger.getLogger(QosClass.class);

    public static boolean compare(XMLServiceMessagge xmlmess, File recivedFile) {
        String computeDigest = checksum(recivedFile);
        logger.debug("Hash dal XMLSERVMESS > ?" + xmlmess.getPayload());
        logger.debug("Hash dal FILE        > ?" + computeDigest);
        if (computeDigest.equals(xmlmess.getPayload())) {
            return true;
        }
        logger.debug("Hash del file non corrispondente");
        return false;
    }

    public static String checksum(File file) {
        try {
            InputStream fin = new FileInputStream(file);
            java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0) md5er.update(buffer, 0, read);
            } while (read != -1);
            fin.close();
            byte[] digest = md5er.digest();
            if (digest == null) return null;
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        XMLServiceMessagge xmlmess = new XMLServiceMessagge("uno", "due", "tre", "quattro");
        File inputFile = new File("data/bd.mp3");
        String ck = QosClass.checksum(inputFile);
        System.out.println(ck);
    }
}
