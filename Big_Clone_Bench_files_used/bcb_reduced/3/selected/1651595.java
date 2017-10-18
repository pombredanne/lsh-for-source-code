package com.loribel.commons.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tools for MD5.
 *
 * See Test : GB_MD5ToolsTest
 * 
 * @author Gregory Borelli
 */
public final class GB_MD5Tools {

    static class MD5 {

        private MessageDigest md = null;

        /**
         * Constructor is private so you must use the getInstance method
         */
        private MD5() throws NoSuchAlgorithmException {
            md = MessageDigest.getInstance("MD5");
        }

        private byte[] calculateHash(byte[] dataToHash) {
            md.update(dataToHash, 0, dataToHash.length);
            return (md.digest());
        }

        public String hashData(byte[] dataToHash) {
            return hexStringFromBytes((calculateHash(dataToHash)));
        }

        public String hexStringFromBytes(byte[] b) {
            String hex = "";
            int msb;
            int lsb = 0;
            int i;
            for (i = 0; i < b.length; i++) {
                msb = (b[i] & 0x000000FF) / 16;
                lsb = (b[i] & 0x000000FF) % 16;
                hex = hex + hexChars[msb] + hexChars[lsb];
            }
            return (hex);
        }
    }

    private static MD5 md5 = null;

    private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
        * This returns the singleton instance
        */
    static MD5 getInstance() throws NoSuchAlgorithmException {
        if (md5 == null) {
            md5 = new MD5();
        }
        return (md5);
    }

    public static void main(String[] args) {
        try {
            MD5 md = getInstance();
            System.out.println(md.hashData("http://www.sfpq.qc.ca/Le_Syndicat/Qui_sommes-nous/Se_syndiquer_avec_le_SFPQ/index.php".getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.out);
        }
    }

    public static String toMd5(String a_str) throws NoSuchAlgorithmException {
        byte[] l_data = a_str.getBytes();
        String retour = getInstance().hashData(l_data);
        return retour;
    }

    private GB_MD5Tools() {
    }
}
