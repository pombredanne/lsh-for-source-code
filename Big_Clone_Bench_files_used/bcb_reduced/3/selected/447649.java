package org.zurell.java.SortImages;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author  m0554
 */
public class MD5Calculator extends java.lang.Thread {

    Set myKeys;

    HashMap myPile;

    Logger log;

    /** Creates a new instance of MD5Calculator */
    public MD5Calculator(HashMap pile, Logger l) {
        this.log = l;
        this.myPile = pile;
        this.myKeys = pile.keySet();
        log.debug("[MD5Calculator] Pile-Size:" + pile.size());
    }

    public void run() {
        log.debug("[MD5Calculator] Starting md5sum");
        Iterator iter = myKeys.iterator();
        while (iter.hasNext()) {
            String myFilename = (String) iter.next();
            synchronized (myPile) {
                ImagePile tmpPile = (ImagePile) myPile.get(myFilename);
                tmpPile.setMD5SUM(calculate(myFilename));
                myPile.put(myFilename, tmpPile);
                myPile.notify();
            }
        }
    }

    String calculate(String Filename) {
        String MD5SUM = new String();
        ;
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            FileInputStream in = new FileInputStream(Filename);
            int len;
            byte[] data = new byte[1024];
            while ((len = in.read(data)) > 0) {
                md.update(data, 0, len);
            }
            in.close();
            byte[] result = md.digest();
            for (int i = 0; i < result.length; ++i) {
                MD5SUM = MD5SUM + toHexString(result[i]);
            }
        } catch (Exception e) {
            log.error("[MD5Calculator] MD5-Fehler: " + e.toString());
        }
        return MD5SUM;
    }

    public static String toHexString(byte b) {
        int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        String ret = (value < 16 ? "0" : "");
        ret += Integer.toHexString(value).toUpperCase();
        return ret;
    }
}
