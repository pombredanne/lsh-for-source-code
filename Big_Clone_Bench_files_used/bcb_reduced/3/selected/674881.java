package com.panopset;

import static com.panopset.Util.log;
import static com.panopset.Util.pad;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Convenient access to a Java implemenation of the Linux md5sum command.
 * @author Karl Dinwiddie
 *
 */
public class UtilMD5 implements Commons {

    public static final String MD2 = "MD2";

    public static final String MD5 = "MD5";

    public static final String SHA1 = "SHA - 1";

    public static final String SHA256 = "SHA - 256";

    public static final String SHA384 = "SHA - 384";

    public static final String SHA512 = "SHA - 512";

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static final String[] ALGORITHMS = { MD2, MD5, SHA1, SHA256, SHA384, SHA512 };

    public static String md5sum(InputStream is, int bufferSize, String alg, DspmsgHandler msgHandler) {
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            MessageDigest md = MessageDigest.getInstance(alg);
            byte[] inb = new byte[bufferSize];
            int br = bis.read(inb);
            while (br > -1) {
                md.update(inb, 0, br);
                if (msgHandler != null) {
                    msgHandler.dspmsg(EMPTY_STRING + br);
                }
                br = bis.read(inb);
            }
            StringBuffer sb = new StringBuffer();
            synchronized (sb) {
                for (byte b : md.digest()) sb.append(pad(Integer.toHexString(0xFF & b), ZERO.charAt(0), 2, true));
            }
            bis.close();
            return sb.toString();
        } catch (Exception ex) {
            log(ex);
        }
        return null;
    }

    public static String md5sum(File f, int bufferSize, String alg, DspmsgHandler msgHandler) {
        try {
            FileInputStream fis = new FileInputStream(f);
            String rtn = md5sum(fis, bufferSize, alg, msgHandler);
            fis.close();
            return rtn;
        } catch (Exception ex) {
            log(ex);
        }
        return null;
    }

    public static String md5sum(String s, String alg) {
        try {
            MessageDigest md = MessageDigest.getInstance(alg);
            md.update(s.getBytes(), 0, s.length());
            StringBuffer sb = new StringBuffer();
            synchronized (sb) {
                for (byte b : md.digest()) sb.append(pad(Integer.toHexString(0xFF & b), ZERO.charAt(0), 2, true));
            }
            return sb.toString();
        } catch (Exception ex) {
            log(ex);
        }
        return null;
    }

    /**
     * Defaults to MD5 when called from a static context.
s     *
     * @param s String to run md5sum on.
     * @return String md5sum result.
     */
    public static String md5sum(String s) {
        return md5sum(s, MD5);
    }

    /**
     * Defaults to MD5 when called from a static context.
     *
     * @param is
     *            InputStream to run md5sum on.
     * @return String md5sum result.
     * @throws Exception
     */
    public static String md5sum(InputStream is) throws Exception {
        return md5sum(is, DEFAULT_BUFFER_SIZE, MD5, null);
    }

    /**
     * Defaults to MD5 when called from a static context.
     *
     * @param f
     *            File to run md5sum on.
     * @return String md5sum result.
     * @throws Exception
     */
    public static String md5sum(File f) throws Exception {
        return md5sum(f, DEFAULT_BUFFER_SIZE, MD5, null);
    }

    private UtilMD5() {
    }
}
