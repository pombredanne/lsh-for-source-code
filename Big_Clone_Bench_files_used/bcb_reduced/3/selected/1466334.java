package net.sf.jpkgmk.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author gommma (gommma AT users.sourceforge.net)
 * @author Last changed by: $Author: gommma $
 * @version $Revision: 2 $ $Date: 2008-08-20 15:14:19 -0400 (Wed, 20 Aug 2008) $
 * @since 1.0
 */
public class ChecksumUtil {

    private static Log log = LogFactory.getLog(ChecksumUtil.class);

    private ChecksumUtil() {
    }

    /**
	 * @param data
	 * @return The MD5 checksum as hex string
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    public static String md5(File data) throws NoSuchAlgorithmException, IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(data));
        try {
            return md5(input);
        } finally {
            StreamUtil.tryCloseStream(input);
        }
    }

    /**
	 * @param data
	 * @return The MD5 checksum as hex string
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    public static String md5(InputStream data) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        int b = -1;
        while ((b = data.read()) != -1) {
            digest.update((byte) b);
        }
        byte[] hash = digest.digest();
        return StringUtil.getHexString(hash);
    }

    /**
	 * Returns the CRC32 checksum for the given file
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static Long createChecksumCrc32(File file) throws IOException {
        Checksum checksum = new CRC32();
        return createChecksum(file, checksum);
    }

    public static Long createUnixCksum(File file) throws IOException, NoSuchAlgorithmException {
        return UnixChecksum.createChecksum(file);
    }

    public static Long createUnixCksum(byte[] data) throws IOException, NoSuchAlgorithmException {
        return UnixChecksum.createChecksum(data);
    }

    /**
	 * Returns the checksum for the given file
	 * @param file
	 * @param checksum
	 * @return
	 * @throws IOException
	 */
    public static Long createChecksum(File file, Checksum checksum) throws IOException {
        long millis = System.currentTimeMillis();
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            checksum.reset();
            int b;
            while ((b = in.read()) >= 0) {
                checksum.update(b);
            }
        } finally {
            StreamUtil.tryCloseStream(in);
        }
        millis = System.currentTimeMillis() - millis;
        log.debug("Checksum computed for file '" + file + "'. Second(s): " + (millis / 1000L));
        long checksumVal = checksum.getValue();
        return Long.valueOf(checksumVal);
    }
}
