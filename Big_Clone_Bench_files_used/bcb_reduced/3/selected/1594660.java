package org.dcm4chex.archive.hsm.spi;

import org.jboss.system.ServiceMBeanSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.compress.tar.TarInputStream;
import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarOutputStream;
import org.dcm4che.util.MD5Utils;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.hsm.spi.utils.HsmUtils;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import java.io.*;
import java.text.MessageFormat;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * <code>TarService</code> is an MBean implementation of {@link org.dcm4chex.archive.hsm.spi.TarArchiver}.
 * <br>
 * On top of the contract of the <code>TarArchiver</code>, this implementation provides mechanism to check MD5 sums
 * of the packed and unpacked files.
 *
 * @see #setCheckMd5(boolean) 
 * @author Fuad Ibrahimov
 * @since Feb 19, 2007
 */
public class TarService extends ServiceMBeanSupport implements TarArchiver {

    private static final Log logger = LogFactory.getLog(TarService.class);

    private static final String WRONG_BUFFER_SIZE = "Wrong buffer size: {0}";

    static final String MD5_SUM = "MD5SUM";

    public static final String UNEXPECTED_TAR_ENTRY = "Unexpected tar entry [{0}]. Digest check failed.";

    private static final String PACKING_FILES_INTO_A_TAR_FILE = "Packing files into a tar file";

    private static final String TAR_SUFFIX = ".tar";

    private static final int SIZE_OF_ADDITIONAL_CHARS = 6;

    private static final String NOTHING_TO_PACK_INTO_TAR = "Nothing to pack into tar, passed files were: [{0}]";

    private static final String COULD_NOT_DELETE_TAR_FILE = "Could not delete tar file [{0}]";

    private static final String WARNING_TAR_FILE_EXISTS = "Tar file exists, will delete it [{0}]";

    private static final String US_ASCII = "US-ASCII";

    private static final String COULD_NOT_CLOSE_FILE_INPUT_STREAM = "Could not close file input stream [{0}]";

    private static final String COULD_NOT_CLOSE_TAR_OUTPUT_STREAM = "Could not close TAR output stream [{0}]";

    static final String FAILED_DIGEST_CHECK = "Failed {0} check of tar entry [{1}], expected [{2}], was [{3}]";

    private boolean checkMd5 = true;

    private int bufferSize = 8192;

    /**
     * Unpacks a TAR file specified by <code>tarFilePath</code> into <code>destinationDir</code>.
     * <code>tarFilePath</code> is expected to be a full file path in an OS dependent format.
     * <p>
     * <b>Note:</b> Unpacked files will replace files with same name.
     * @see #unpack(java.io.File, String)
     * @param tarFilePath OS dependent full file path of an archive to unpack
     * @param destinationDir destination directory to unpack files to
     * @throws Exception in case of errors
     */
    public void unpack(String tarFilePath, String destinationDir) throws Exception {
        unpack(new File(tarFilePath), destinationDir);
    }

    /**
     * Unpacks <code>tarFile</code> into <code>destinationDir</code>.
     * <p>
     * <b>Note:</b> Unpacked files will replace files with same name.
     * @see #unpack(String, String)
     * @param tarFile TAR archive to unpack
     * @param destinationDir destination directory to unpack files to
     * @throws Exception in case of errors
     */
    public void unpack(File tarFile, String destinationDir) throws Exception {
        new TarExtractor(tarFile, destinationDir, bufferSize, checkMd5).extractEntries();
    }

    /**
     * Packs given files into a TAR archive and returns the TAR archive file. Uses <code>baseDir</code> to create
     * the TAR file in. The name of the created TAR file depends on DCM4CHEE's file path naming convention and will
     * be as the filepath of the first file in the list with the last "/" (slash) replaced with a "-" (dash) and with an
     * added ".tar" extension. TAR entries will have the same relative path as the files had on the original file
     * system, e.g. if a file had a relative path to it's parent file system as <code>A/B/C/D</code>, the same relative
     * path will be used for the corresponding TAR entry in the archive.
     * <br>
     * The first entry in the created TAR archive will be an entry containing MD5 sums of all files in this archive.
     * <p>
     * <b>Note:</b> If there already is a file with the same name as the created TAR file, it will be replaced.
     * 
     * @param baseDir a directory to create the TAR file in
     * @param files list of files to be packed into an archive
     * @return created TAR archive file
     * @throws Exception in case of errors
     */
    public File pack(String baseDir, List<FileInfo> files) throws Exception {
        if (files != null && !files.isEmpty()) {
            File tar = newTarFile(baseDir, files.get(0).fileID);
            if (logger.isDebugEnabled()) {
                logger.debug(PACKING_FILES_INTO_A_TAR_FILE);
            }
            deleteFileIfExists(tar);
            writeTarEntries(tar, files);
            return tar;
        }
        throw new IllegalArgumentException(MessageFormat.format(NOTHING_TO_PACK_INTO_TAR, files));
    }

    private void deleteFileIfExists(File tar) throws IOException {
        if (tar.exists()) {
            if (logger.isWarnEnabled()) {
                logger.warn(MessageFormat.format(WARNING_TAR_FILE_EXISTS, tar.getCanonicalPath()));
            }
            if (!tar.delete()) throw new IOException(MessageFormat.format(COULD_NOT_DELETE_TAR_FILE, tar));
        }
    }

    private void writeTarEntries(File tar, List<FileInfo> files) throws IOException {
        TarOutputStream tarOutputStream = new TarOutputStream(new FileOutputStream(tar));
        try {
            Map<String, String> fileMd5Sums = new HashMap<String, String>(files.size());
            for (FileInfo file : files) fileMd5Sums.put(file.fileID, file.md5);
            writeMD5SUM(tarOutputStream, fileMd5Sums);
            for (FileInfo file : files) {
                writeEntry(tarOutputStream, file.fileID, FileUtils.toFile(file.basedir, file.fileID));
            }
        } finally {
            try {
                tarOutputStream.close();
            } catch (IOException ignored) {
                logger.warn(MessageFormat.format(COULD_NOT_CLOSE_TAR_OUTPUT_STREAM, tar), ignored);
            }
        }
    }

    private void writeEntry(TarOutputStream tarOutputStream, String fileID, File file) throws IOException {
        TarEntry entry = new TarEntry(slashify(fileID));
        entry.setSize(file.length());
        tarOutputStream.putNextEntry(entry);
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buf = newBuffer(bufferSize);
            int read;
            while ((read = fis.read(buf)) > 0) {
                tarOutputStream.write(buf, 0, read);
            }
        } finally {
            try {
                fis.close();
            } catch (IOException ignored) {
                logger.warn(MessageFormat.format(COULD_NOT_CLOSE_FILE_INPUT_STREAM, file), ignored);
            }
        }
        tarOutputStream.closeEntry();
    }

    private byte[] newBuffer(int tarBufferSize) {
        if (tarBufferSize <= 0) throw new IllegalArgumentException("Tar buffer size must be a positive integer.");
        return new byte[tarBufferSize];
    }

    private void writeMD5SUM(TarOutputStream tar, Map<String, String> fileMd5Sums) throws IOException {
        byte[] md5Entry = buildMd5Entry(fileMd5Sums);
        final TarEntry tarEntry = new TarEntry(MD5_SUM);
        tarEntry.setSize(md5Entry.length);
        tar.putNextEntry(tarEntry);
        tar.write(md5Entry);
        tar.closeEntry();
    }

    private byte[] buildMd5Entry(Map<String, String> fileMd5Sums) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Map.Entry<String, String> entry : fileMd5Sums.entrySet()) {
            byte[] md5sum = new byte[entry.getValue().length()];
            MD5Utils.toHexChars(MD5Utils.toBytes(entry.getValue()), md5sum, 0);
            baos.write(md5sum);
            baos.write(' ');
            baos.write(' ');
            baos.write(slashify(entry.getKey()).getBytes(US_ASCII));
            baos.write('\n');
        }
        return baos.toByteArray();
    }

    private String slashify(String path) {
        return path.replaceAll(File.separator, "/");
    }

    private File newTarFile(String baseDir, String fileID) {
        File file = FileUtils.toFile(buildUnixPath(baseDir, fileID));
        file.getParentFile().mkdirs();
        return file;
    }

    public String buildUnixPath(String baseDir, String firstFilePath) {
        StringBuffer tf = new StringBuffer(totalSizeOfFilePath(baseDir, firstFilePath));
        tf.append(baseDir.endsWith("/") ? baseDir : baseDir + "/");
        tf.append(firstFilePath);
        int ind = tf.lastIndexOf("/");
        if (ind > -1) tf.setCharAt(ind, '-');
        tf.append(TAR_SUFFIX);
        return tf.toString();
    }

    private int totalSizeOfFilePath(String baseDir, String tarFile) {
        return baseDir.length() + tarFile.length() + SIZE_OF_ADDITIONAL_CHARS;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets a buffer size used during pack and unpack operations.
     * @param bufferSize the bufferSize to use
     */
    public void setBufferSize(int bufferSize) {
        if (bufferSize <= 0) throw new IllegalArgumentException(MessageFormat.format(WRONG_BUFFER_SIZE, bufferSize));
        this.bufferSize = bufferSize;
    }

    public boolean isCheckMd5() {
        return checkMd5;
    }

    /**
     * Sets if an MD5 digest check is needed on a TAR file unpack. If it is set to <code>true</code>, then the first
     * entry in the archive is expected to be named as "MD5SUM" and containing MD5 sums of all files in the archive.
     * If TAR files don't contain MD5 sums entries, then set this parameter to <code>false</code>. Othervise all
     * unpack operations will fail.
     * @param checkMd5 a flag indicating if an MD5 digest check is needed on a TAR file unpack
     */
    public void setCheckMd5(boolean checkMd5) {
        this.checkMd5 = checkMd5;
    }

    private static class TarExtractor {

        private TarInputStream tarInputStream;

        private File destDir;

        private boolean checkMd5 = true;

        private List<File> extractedEntries = new ArrayList<File>();

        private Map<String, byte[]> md5Sums = null;

        private int bufferSize;

        private static final String MISSING_MD_SUMS_ENTRY = "Missing MD5SUMS entry in [{0}]";

        private static final int NAME_BEGIN_INDEX = 34;

        private static final int MD5_END_INDEX = 32;

        private static final String M_WRITE = "M-WRITE: [{0}]";

        private static final String M_DELETE = "M-DELETE: [{0}]";

        private static final String CLEANING_UP = "Cleaning up all extracted files of last tar file due to exception.";

        private static final String COULDNT_DELETE_FILE = "Could not delete file [{0}]";

        private static final String FILE_EXISTS_WILL_OVERWRITE_IT = "File exists: [{0}], will overwrite it.";

        private static final String COULDNT_DELETE_PARENT_FOLDERS = "Could not delete parent folders of [{0}].";

        private static final String TAR_UNPACKING = "TAR - Unpacking [{0}]";

        TarExtractor(File tar, String destDir, int bufferSize, boolean checkMd5) throws Exception {
            if (logger.isInfoEnabled()) {
                logger.info(MessageFormat.format(TAR_UNPACKING, tar));
            }
            this.tarInputStream = new TarInputStream(new FileInputStream(tar));
            this.destDir = FileUtils.toFile(destDir);
            this.bufferSize = bufferSize;
            this.checkMd5 = checkMd5;
            if (this.checkMd5) {
                TarEntry nextEntry = this.tarInputStream.getNextEntry();
                if (!MD5_SUM.equals(nextEntry.getName())) {
                    throw new IOException(MessageFormat.format(MISSING_MD_SUMS_ENTRY, tar));
                }
                this.md5Sums = new HashMap<String, byte[]>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.tarInputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    this.md5Sums.put(line.substring(NAME_BEGIN_INDEX), MD5Utils.toBytes(line.substring(0, MD5_END_INDEX)));
                }
            }
        }

        void extractEntries() throws Exception {
            try {
                doExtractEntries();
            } finally {
                try {
                    if (this.tarInputStream != null) this.tarInputStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        @SuppressWarnings({ "EmptyCatchBlock", "ThrowFromFinallyBlock" })
        private void doExtractEntries() throws Exception {
            TarEntry entry;
            while ((entry = this.tarInputStream.getNextEntry()) != null) {
                File file = new File(this.destDir, nativeName(entry.getName()));
                if (file.exists()) {
                    logWarn(file, FILE_EXISTS_WILL_OVERWRITE_IT);
                    if (!file.delete()) {
                        logWarn(file, COULDNT_DELETE_FILE);
                    }
                }
                File parent = file.getParentFile();
                if (parent.mkdirs()) {
                    logInfo(parent, M_WRITE);
                }
                extractedEntries.add(file);
                OutputStream fos = null;
                try {
                    fos = this.checkMd5 ? newDigestVerifyingOS(file, entry.getName()) : new FileOutputStream(file);
                    byte[] buf = new byte[this.bufferSize];
                    int len;
                    while ((len = this.tarInputStream.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                } catch (Exception e) {
                    cleanup();
                    throw e;
                } finally {
                    try {
                        if (fos != null) fos.close();
                    } catch (FailedDigestCheckException e) {
                        cleanup();
                        throw e;
                    } catch (Exception ignore) {
                    }
                }
                logInfo(file, M_WRITE);
            }
        }

        private void logInfo(File file, String msg) {
            if (logger.isInfoEnabled()) {
                logger.info(MessageFormat.format(msg, file));
            }
        }

        private void cleanup() {
            logger.warn(CLEANING_UP);
            for (File file : extractedEntries) {
                if (file.delete()) {
                    logInfo(file, M_DELETE);
                } else {
                    logWarn(file, COULDNT_DELETE_FILE);
                }
                try {
                    HsmUtils.deleteParentsTill(file, destDir.getCanonicalPath());
                } catch (IOException e) {
                    logger.warn(MessageFormat.format(COULDNT_DELETE_PARENT_FOLDERS, file), e);
                }
            }
        }

        private void logWarn(File file, String msg) {
            if (logger.isWarnEnabled()) {
                logger.warn(MessageFormat.format(msg, file));
            }
        }

        private DigestVerifyingOutputStream newDigestVerifyingOS(File file, String entryName) throws Exception {
            byte[] expectedDigest = this.md5Sums.remove(entryName);
            if (expectedDigest == null) {
                cleanup();
                throw new FailedDigestCheckException(MessageFormat.format(UNEXPECTED_TAR_ENTRY, entryName));
            }
            return new DigestVerifyingOutputStream(new FileOutputStream(file), expectedDigest, entryName);
        }

        private String nativeName(String name) {
            return name.replace('/', File.separatorChar);
        }
    }

    private static class DigestVerifyingOutputStream extends DigestOutputStream {

        private static final String MD5 = "MD5";

        private final byte[] expectedDigest;

        private final String entryName;

        public DigestVerifyingOutputStream(OutputStream stream, byte[] expectedDigest, String entryName) throws Exception {
            super(stream, MessageDigest.getInstance(MD5));
            this.expectedDigest = expectedDigest;
            this.entryName = entryName;
        }

        public void close() throws IOException {
            super.close();
            byte[] actualDigest = getMessageDigest().digest();
            if (!MessageDigest.isEqual(expectedDigest, actualDigest)) {
                throw new FailedDigestCheckException(MessageFormat.format(FAILED_DIGEST_CHECK, getMessageDigest().getAlgorithm(), entryName, toHexStr(expectedDigest), toHexStr(actualDigest)));
            }
        }

        private String toHexStr(byte[] digest) {
            return new String(MD5Utils.toHexChars(digest));
        }
    }
}
