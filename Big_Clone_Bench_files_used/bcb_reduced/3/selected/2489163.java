package org.dcm4chex.cdw.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4198 $ $Date: 2004-10-11 19:17:34 -0400 (Mon, 11 Oct 2004) $
 * @since 28.06.2004
 */
public class MD5Utils {

    public static final long MEGA = 1000000L;

    public static final long GIGA = 1000000000L;

    private static final int BUF_SIZE = 512;

    private MD5Utils() {
    }

    public static File makeMD5File(File f) {
        return new File(f.getParent(), f.getName() + ".MD5");
    }

    public static char[] toHexChars(byte[] bs) {
        char[] cbuf = new char[bs.length * 2];
        toHexChars(bs, cbuf);
        return cbuf;
    }

    private static final char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static void toHexChars(byte[] bs, char[] cbuf) {
        for (int i = 0, j = 0; i < bs.length; i++, j++, j++) {
            cbuf[j] = HEX_DIGIT[(bs[i] >>> 4) & 0xf];
            cbuf[j + 1] = HEX_DIGIT[bs[i] & 0xf];
        }
    }

    public static void md5sum(File fileOrDir, char[] cbuf, MessageDigest digest, byte[] bbuf) throws IOException {
        digest.reset();
        InputStream in = new DigestInputStream(new FileInputStream(fileOrDir), digest);
        try {
            while (in.read(bbuf) != -1) ;
        } finally {
            in.close();
        }
        toHexChars(digest.digest(), cbuf);
    }

    public static boolean verify(File driveDir, File fsDir, Logger log) throws IOException {
        File md5sums = new File(driveDir, "MD5_SUMS");
        return md5sums.exists() ? verifyMd5Sums(md5sums, log, new byte[BUF_SIZE]) : equals(driveDir, fsDir, log, new byte[BUF_SIZE], new byte[BUF_SIZE]);
    }

    private static boolean equals(File dst, File src, Logger log, byte[] srcBuf, byte[] dstBuf) throws IOException {
        if (src.isDirectory()) {
            String[] ss = src.list();
            for (int i = 0; i < ss.length; i++) {
                String s = ss[i];
                if (!(equals(new File(dst, s), new File(src, s), log, srcBuf, dstBuf))) return false;
            }
        } else {
            if (!dst.isFile()) {
                log.warn("File " + dst + " missing");
                return false;
            }
            log.debug("check " + dst + " = " + src);
            final long srcLen = src.length();
            final long dstLen = dst.length();
            if (dstLen != srcLen) {
                log.warn("File " + dst + " has wrong length");
                return false;
            }
            DataInputStream dstIn = new DataInputStream(new FileInputStream(dst));
            try {
                InputStream srcIn = new FileInputStream(src);
                try {
                    int len;
                    while ((len = srcIn.read(srcBuf)) != -1) {
                        dstIn.readFully(dstBuf, 0, len);
                        if (!equals(dstBuf, srcBuf, len)) {
                            log.warn("File " + dst + " corrupted");
                            return false;
                        }
                    }
                } finally {
                    srcIn.close();
                }
            } finally {
                dstIn.close();
            }
        }
        return true;
    }

    private static boolean equals(byte[] dstBuf, byte[] srcBuf, int len) {
        for (int i = 0; i < len; i++) if (dstBuf[i] != srcBuf[i]) return false;
        return true;
    }

    private static boolean verifyMd5Sums(File md5sums, Logger log, byte[] bbuf) throws IOException {
        String base = md5sums.getParentFile().toURI().toString();
        BufferedReader md5sumsIn = new BufferedReader(new FileReader(md5sums));
        try {
            final char[] cbuf = new char[32];
            String line;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            while ((line = md5sumsIn.readLine()) != null) {
                if (line.length() < 33) continue;
                File f = new File(new URI(base + line.substring(32).trim()));
                log.debug("md5sum " + f);
                md5sum(f, cbuf, digest, bbuf);
                if (!Arrays.equals(cbuf, line.substring(0, 32).toCharArray())) {
                    log.warn("File " + f + " corrupted");
                    return false;
                }
            }
        } catch (URISyntaxException e) {
            log.warn("File " + md5sums + " corrupted");
            return false;
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException(e);
        } finally {
            md5sumsIn.close();
        }
        return true;
    }

    public static String formatSize(long size) {
        if (size < GIGA) return ((float) size / MEGA) + "MB"; else return ((float) size / GIGA) + "GB";
    }

    public static long parseSize(String s, long minSize) {
        long u;
        if (s.endsWith("GB")) u = GIGA; else if (s.endsWith("MB")) u = MEGA; else throw new IllegalArgumentException(s);
        try {
            long size = (long) (Float.parseFloat(s.substring(0, s.length() - 2)) * u);
            if (size >= minSize) return size;
        } catch (IllegalArgumentException e) {
        }
        throw new IllegalArgumentException(s);
    }
}
