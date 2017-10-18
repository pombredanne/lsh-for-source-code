package org.proteomecommons.MSExpedite.SignalProcessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 *
 * @author takis
 */
public class SignatureFactory {

    public static String getMd5Hash(File file) throws Exception {
        return getSignature("MD5", new FileInputStream(file));
    }

    public static String getSha1Hash(File file) throws Exception {
        return getSignature("SHA-1", new FileInputStream(file));
    }

    public static String getSha512Hash(File file) throws Exception {
        return getSignature("SHA-512", new FileInputStream(file));
    }

    public static String getSignature(String alg, InputStream is) {
        String sig = "";
        try {
            MessageDigest digest = MessageDigest.getInstance(alg);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(1, hash);
            sig = bigInt.toString(16);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return sig;
    }

    public static int getLength(InputStream is) {
        int length = 0;
        try {
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                length += read;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return length;
    }
}
