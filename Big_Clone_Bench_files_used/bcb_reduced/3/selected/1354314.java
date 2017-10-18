package org.dcm4chex.archive.util;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.interfaces.MD5;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 15104 $ $Date: 2011-03-14 11:18:35 -0400 (Mon, 14 Mar 2011) $
 * @since 19.09.2004
 * 
 */
public class FileUtils {

    protected static final Logger log = Logger.getLogger(FileUtils.class);

    private static final int BUFFER_SIZE = 512;

    public static final long MEGA = 1000000L;

    public static final long GIGA = 1000000000L;

    public static final long MAX_TIMES_CREATE_FILE = 10;

    public static final long INTERVAL_CREATE_FILE = 250;

    private static char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String toHex(int val) {
        char[] ch8 = new char[8];
        for (int i = 8; --i >= 0; val >>= 4) {
            ch8[i] = HEX_DIGIT[val & 0xf];
        }
        return String.valueOf(ch8);
    }

    /**
     * Create a new file based on hash code, including the directory structure
     * if necessary. In case of collision, we will reset hashcode and try again.
     * 
     * @param dir
     *            the directory where the new file will be created
     * @param hash
     *            the initial hash value
     * @return the file created successfully
     * @throws Exception
     */
    public static File createNewFile(File dir, int hash) throws Exception {
        File f = null;
        boolean success = false;
        Exception lastException = null;
        for (int i = 0; i < MAX_TIMES_CREATE_FILE && !success; i++) {
            try {
                if (!dir.exists()) {
                    success = dir.mkdirs();
                    if (!success) throw new IOException("Directory creation failed: " + dir.getCanonicalPath());
                }
                f = new File(dir, toHex(hash++));
                success = f.createNewFile();
                if (i > 0) log.info("file: " + dir.getCanonicalPath() + " created successfully after retries: " + i);
                if (success) return f; else i--;
            } catch (Exception e) {
                if (lastException == null) log.warn("failed to create file: " + dir.getCanonicalPath() + " - retry: " + (i + 1) + " of " + MAX_TIMES_CREATE_FILE + ". Will retry again.", e); else log.warn("failed to create file: " + dir.getCanonicalPath() + " - got the same exception as above - retry: " + (i + 1) + " of " + MAX_TIMES_CREATE_FILE + ". Will retry again");
                lastException = e;
                success = false;
                try {
                    Thread.sleep(INTERVAL_CREATE_FILE);
                } catch (InterruptedException e1) {
                }
            }
        }
        throw lastException;
    }

    public static String slashify(File f) {
        return f.getPath().replace(File.separatorChar, '/');
    }

    public static File resolve(File f) {
        if (f.isAbsolute()) return f;
        File serverHomeDir = new File(System.getProperty("jboss.server.home.dir"));
        return new File(serverHomeDir, f.getPath());
    }

    public static File toFile(String unixPath) {
        return resolve(new File(unixPath.replace('/', File.separatorChar)));
    }

    public static File toExistingFile(String unixPath) throws FileNotFoundException {
        File f = toFile(unixPath);
        if (!f.isFile()) {
            throw new FileNotFoundException(f.toString());
        }
        return f;
    }

    public static File toFile(String unixDirPath, String unixFilePath) {
        return resolve(new File(unixDirPath.replace('/', File.separatorChar), unixFilePath.replace('/', File.separatorChar)));
    }

    public static String formatSize(long size) {
        return (size == -1) ? "UNKNOWN" : (size < GIGA) ? ((float) size / MEGA) + "MB" : ((float) size / GIGA) + "GB";
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

    public static boolean equalsPixelData(File f1, File f2) throws IOException {
        InputStream in1 = new BufferedInputStream(new FileInputStream(f1));
        try {
            InputStream in2 = new BufferedInputStream(new FileInputStream(f2));
            try {
                Dataset attrs = DcmObjectFactory.getInstance().newDataset();
                DcmParserFactory pf = DcmParserFactory.getInstance();
                DcmParser p1 = pf.newDcmParser(in1);
                DcmParser p2 = pf.newDcmParser(in2);
                p1.setDcmHandler(attrs.getDcmHandler());
                p1.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
                p2.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
                int samples = attrs.getInt(Tags.SamplesPerPixel, 1);
                int frames = attrs.getInt(Tags.NumberOfFrames, 1);
                int rows = attrs.getInt(Tags.Rows, 1);
                int columns = attrs.getInt(Tags.Columns, 1);
                int bitsAlloc = attrs.getInt(Tags.BitsAllocated, 8);
                int bitsStored = attrs.getInt(Tags.BitsStored, bitsAlloc);
                int frameLength = rows * columns * samples * bitsAlloc / 8;
                int pixelDataLength = frameLength * frames;
                if (pixelDataLength > p1.getReadLength() || pixelDataLength > p2.getReadLength()) {
                    return false;
                }
                byte[] b1 = new byte[BUFFER_SIZE];
                byte[] b2 = new byte[BUFFER_SIZE];
                int[] mask = { 0xff, 0xff };
                int len, len2;
                if (bitsAlloc == 16 && bitsStored < 16) {
                    mask[p1.getDcmDecodeParam().byteOrder == ByteOrder.LITTLE_ENDIAN ? 1 : 0] = 0xff >>> (16 - bitsStored);
                }
                int pos = 0;
                while (pos < pixelDataLength) {
                    len = in1.read(b1, 0, Math.min(pixelDataLength - pos, BUFFER_SIZE));
                    if (len < 0) return false;
                    int off = 0;
                    while (off < len) {
                        off += len2 = in2.read(b2, off, len - off);
                        if (len2 < 0) return false;
                    }
                    for (int i = 0; i < len; i++, pos++) if (((b1[i] - b2[i]) & mask[pos & 1]) != 0) return false;
                }
                return true;
            } finally {
                in2.close();
            }
        } finally {
            in1.close();
        }
    }

    public static int maxDiffPixelData(File f1, File f2) throws IOException {
        int maxDiff = 0;
        InputStream in1 = new BufferedInputStream(new FileInputStream(f1));
        try {
            InputStream in2 = new BufferedInputStream(new FileInputStream(f2));
            try {
                DcmObjectFactory df = DcmObjectFactory.getInstance();
                DcmParserFactory pf = DcmParserFactory.getInstance();
                Dataset attrs1 = df.newDataset();
                Dataset attrs2 = df.newDataset();
                DcmParser p1 = pf.newDcmParser(in1);
                DcmParser p2 = pf.newDcmParser(in2);
                p1.setDcmHandler(attrs1.getDcmHandler());
                p2.setDcmHandler(attrs2.getDcmHandler());
                p1.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
                p2.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
                int samples = attrs1.getInt(Tags.SamplesPerPixel, 1);
                int frames = attrs1.getInt(Tags.NumberOfFrames, 1);
                int rows = attrs1.getInt(Tags.Rows, 1);
                int columns = attrs1.getInt(Tags.Columns, 1);
                int bitsAlloc = attrs1.getInt(Tags.BitsAllocated, 8);
                int bitsStored = attrs1.getInt(Tags.BitsStored, bitsAlloc);
                int bitsStored2 = attrs2.getInt(Tags.BitsStored, bitsAlloc);
                int pixelRepresentation1 = attrs1.getInt(Tags.PixelRepresentation, 0);
                int pixelRepresentation2 = attrs2.getInt(Tags.PixelRepresentation, 0);
                int frameLength = rows * columns * samples * bitsAlloc / 8;
                int pixelDataLength = frameLength * frames;
                if (pixelDataLength > p1.getReadLength() || pixelDataLength > p2.getReadLength()) {
                    return Integer.MAX_VALUE;
                }
                byte[] b1 = new byte[BUFFER_SIZE];
                byte[] b2 = new byte[BUFFER_SIZE];
                byte lsb1 = 0, lsb2 = 0;
                int w1, w2, len, len2;
                int bitmask1 = 0xffff >>> (bitsAlloc - bitsStored);
                int bitmask2 = 0xffff >>> (bitsAlloc - bitsStored2);
                int signed1 = pixelRepresentation1 != 0 ? (-1 & ~bitmask1) >> 1 : 0;
                int signed2 = pixelRepresentation2 != 0 ? (-1 & ~bitmask2) >> 1 : 0;
                int pos = 0;
                while (pos < pixelDataLength) {
                    len = in1.read(b1, 0, Math.min(pixelDataLength - pos, BUFFER_SIZE));
                    if (len < 0) return Integer.MAX_VALUE;
                    int off = 0;
                    while (off < len) {
                        off += len2 = in2.read(b2, off, len - off);
                        if (len2 < 0) return Integer.MAX_VALUE;
                    }
                    if (bitsAlloc == 8) for (int i = 0; i < len; i++, pos++) maxDiff = Math.max(maxDiff, Math.abs((b1[i] & 0xff) - (b2[i] & 0xff))); else {
                        for (int i = 0; i < len; i++, pos++) if ((pos & 1) == 0) {
                            lsb1 = b1[i];
                            lsb2 = b2[i];
                        } else {
                            if (((w1 = ((b1[i] << 8) | (lsb1 & 0xff)) & bitmask1) & signed1) != 0) w1 |= signed1;
                            if (((w2 = ((b2[i] << 8) | (lsb2 & 0xff)) & bitmask2) & signed2) != 0) w2 |= signed2;
                            maxDiff = Math.max(maxDiff, Math.abs(w1 - w2));
                        }
                    }
                }
                return maxDiff;
            } finally {
                in2.close();
            }
        } finally {
            in1.close();
        }
    }

    public static boolean delete(File file, boolean deleteEmptyParents) {
        return delete(file, deleteEmptyParents, null);
    }

    /**
     * 
     * @param file - the file to delete
     * @param deleteEmptyParents - removing empty parent folders flag
     * @param parentFolderToKeep - if deleteEmptyParents is true, this parameters shows the parent folder 
     * where removing parents must stop
     * @return True, if the file has been successfully removed, otherwise returns false
     */
    public static boolean delete(File file, boolean deleteEmptyParents, String parentFolderToKeep) {
        log.info("M-DELETE file: " + file);
        if (!file.exists()) {
            log.warn("File: " + file + " was already deleted");
            return true;
        }
        if (!file.delete()) {
            log.warn("Failed to delete file: " + file);
            return false;
        }
        if (deleteEmptyParents) {
            File parentToKeep = null;
            if (parentFolderToKeep != null) parentToKeep = toFile(parentFolderToKeep);
            File parent = file.getParentFile();
            while (!parent.equals(parentToKeep) && parent.delete()) {
                log.info("M-DELETE directory: " + parent);
                parent = parent.getParentFile();
            }
        }
        return true;
    }

    public static void verifyMD5(FileInfo info) throws Exception {
        if (info.md5 == null || info.basedir == null || info.availability != Availability.ONLINE || info.basedir.startsWith("ftp://")) return;
        File file = FileUtils.toFile(info.basedir, info.fileID);
        verifyMD5(file, info.md5);
    }

    public static void verifyMD5(File file, String originalMd5) throws Exception {
        log.info("M-READ file:" + file);
        MessageDigest md = MessageDigest.getInstance("MD5");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        DigestInputStream dis = new DigestInputStream(in, md);
        try {
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            if ((parser.getReadTag() & 0xFFFFFFFFL) >= Tags.PixelData) {
                if (parser.getReadLength() == -1) {
                    while (parser.parseHeader() == Tags.Item) {
                        readOut(parser.getInputStream(), parser.getReadLength());
                    }
                }
                readOut(parser.getInputStream(), parser.getReadLength());
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
            }
        } finally {
            try {
                dis.close();
            } catch (IOException ignore) {
            }
        }
        byte[] md5 = md.digest();
        if (!Arrays.equals(md5, MD5.toBytes(originalMd5))) {
            log.error("MD5 for " + file.getAbsolutePath() + " is different that expected.  Has the file been changed or corrupted?");
            throw new IllegalStateException("MD5 mismatch");
        }
    }

    protected static void readOut(InputStream in, int len) throws IOException {
        int toRead = len;
        while (toRead-- > 0) {
            if (in.read() < 0) {
                throw new EOFException();
            }
        }
    }
}
