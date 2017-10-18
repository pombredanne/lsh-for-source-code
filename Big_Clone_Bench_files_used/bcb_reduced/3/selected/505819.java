package org.dcm4chex.archive.hsm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 15642 $ $Date: 2011-07-01 02:46:14 -0400 (Fri, 01 Jul 2011) $
 * @since Mar 1, 2006
 */
public class VerifyTar {

    private static Logger log = LoggerFactory.getLogger(VerifyTar.class);

    private static final int BUF_SIZE = 8192;

    private static final String USAGE = "Usage: java -jar verifytar.jar [-p<num>] <file or directory path>[..]\n\n" + " -p<num>  Strip the smallest prefix containing <num> leading slashes from each\n" + "          file name prompted to stdout.";

    public static Map<String, byte[]> verify(File file, byte[] buf) throws IOException, VerifyTarException {
        FileInputStream in = new FileInputStream(file);
        try {
            return verify(in, file.toString(), buf);
        } finally {
            in.close();
        }
    }

    public static Map<String, byte[]> verify(InputStream in, String tarname, byte[] buf) throws IOException, VerifyTarException {
        return verify(in, tarname, buf, null);
    }

    public static Map<String, byte[]> verify(InputStream in, String tarname, byte[] buf, ArrayList<String> objectNames) throws IOException, VerifyTarException {
        TarInputStream tar = new TarInputStream(in);
        try {
            log.debug("Verify tar file: {}", tarname);
            TarEntry entry = tar.getNextEntry();
            if (entry == null) throw new VerifyTarException("No entries in " + tarname);
            String entryName = entry.getName();
            if (!"MD5SUM".equals(entryName)) throw new VerifyTarException("Missing MD5SUM entry in " + tarname);
            BufferedReader dis = new BufferedReader(new InputStreamReader(tar));
            HashMap<String, byte[]> md5sums = new HashMap<String, byte[]>();
            String line;
            while ((line = dis.readLine()) != null) {
                char[] c = line.toCharArray();
                byte[] md5sum = new byte[16];
                for (int i = 0, j = 0; i < md5sum.length; i++, j++, j++) {
                    md5sum[i] = (byte) ((fromHexDigit(c[j]) << 4) | fromHexDigit(c[j + 1]));
                }
                md5sums.put(line.substring(34), md5sum);
            }
            Map<String, byte[]> entries = new HashMap<String, byte[]>(md5sums.size());
            entries.putAll(md5sums);
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            while ((entry = tar.getNextEntry()) != null) {
                entryName = entry.getName();
                log.debug("START: Check MD5 of entry: {}", entryName);
                if (objectNames != null && !objectNames.remove(entryName)) throw new VerifyTarException("TAR " + tarname + " contains entry: " + entryName + " not in file list");
                byte[] md5sum = (byte[]) md5sums.remove(entryName);
                if (md5sum == null) throw new VerifyTarException("Unexpected TAR entry: " + entryName + " in " + tarname);
                digest.reset();
                in = new DigestInputStream(tar, digest);
                while (in.read(buf) > 0) ;
                if (!Arrays.equals(digest.digest(), md5sum)) {
                    throw new VerifyTarException("Failed MD5 check of TAR entry: " + entryName + " in " + tarname);
                }
                log.debug("DONE: Check MD5 of entry: {}", entryName);
            }
            if (!md5sums.isEmpty()) throw new VerifyTarException("Missing TAR entries: " + md5sums.keySet() + " in " + tarname);
            if (objectNames != null && !objectNames.isEmpty()) throw new VerifyTarException("Missing TAR entries from object list: " + objectNames.toString() + " in " + tarname);
            return entries;
        } finally {
            tar.close();
        }
    }

    public static int fromHexDigit(char c) {
        return c - ((c <= '9') ? '0' : (((c <= 'F') ? 'A' : 'a') - 10));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(-1);
        }
        int off = 0;
        int strip = 0;
        if (args[0].startsWith("-p")) {
            try {
                strip = Integer.parseInt(args[0].substring(2));
                off = 1;
            } catch (NumberFormatException e) {
                System.out.println(USAGE);
                System.exit(-1);
            }
        }
        int errors = 0;
        byte[] buf = new byte[BUF_SIZE];
        for (int i = off; i < args.length; i++) {
            try {
                errors += VerifyTar.verify(new File(args[i]), strip, buf);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.exit(-2);
            }
        }
        System.exit(errors);
    }

    private static int verify(File file, int strip, byte[] buf) throws FileNotFoundException {
        int errors = 0;
        if (file.isDirectory()) {
            String[] ss = file.list();
            for (int i = 0; i < ss.length; i++) {
                errors += verify(new File(file, ss[i]), strip, buf);
            }
        } else {
            String tarname = file.getPath();
            try {
                int pos = 0;
                while (strip-- > 0) {
                    pos = tarname.indexOf(File.separatorChar, pos);
                    if (pos == -1) break;
                    pos++;
                }
                if (pos != -1) {
                    log.info(tarname.substring(pos) + ' ');
                }
                verify(file, buf);
                log.info("ok");
            } catch (Exception e) {
                errors = 1;
                log.error("Failed to create substring", e.getMessage());
            }
        }
        return errors;
    }
}
