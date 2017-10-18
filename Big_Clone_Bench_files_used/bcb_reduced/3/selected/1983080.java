package de.searchworkorange.indexcrawler.Document.TwinFilePersistent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Checksum;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import de.searchworkorange.indexcrawler.Document.TwinFilePersistent.exception.UnableToHashException;
import de.searchworkorange.indexcrawler.Document.exception.NoInputStreamException;
import de.searchworkorange.indexcrawler.configuration.ConfigurationCollection;
import de.searchworkorange.lib.logger.LoggerCollection;

/**
 * 
 * @author Sascha Kriegesmann kriegesmann at vaxnet.de
 */
public class MD implements Checksum {

    private static final boolean CLASSDEBUG = false;

    protected static final int BUFFERSIZE = 16384;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private LoggerCollection loggerCol = null;

    private ConfigurationCollection config = null;

    private SimpleFile simpleFile = null;

    private MessageDigest md = null;

    private byte[] digest = null;

    /**
     * 
     */
    protected long value = 0;

    /**
     * 
     */
    protected long length = 0;

    private boolean virgin = true;

    private String filePath = null;

    public MD(LoggerCollection loggerCol, ConfigurationCollection config, SimpleFile simpleFile) {
        if (loggerCol == null || config == null || simpleFile == null) {
            throw new IllegalArgumentException();
        } else {
            this.loggerCol = loggerCol;
            this.config = config;
            this.simpleFile = simpleFile;
            filePath = simpleFile.getFilePath();
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ex) {
                loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
            }
        }
    }

    /**
     * 
     * @return MessageDigest
     */
    public MessageDigest getMd() {
        return md;
    }

    /**
     * 
     * @param length
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * 
     * @param md
     */
    public void setMd(MessageDigest md) {
        this.md = md;
    }

    /**
     * 
     * @param value
     */
    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public void reset() {
        md.reset();
        value = 0;
        length = 0;
        virgin = true;
    }

    /**
     * 
     * @param b
     */
    @Override
    public void update(int b) {
        update((byte) (b & 0xFF));
    }

    /**
     * 
     * @param b
     */
    public void update(byte b) {
        md.update(b);
        length++;
    }

    /**
     * 
     * @param bytes
     * @param offset
     * @param length
     */
    @Override
    public void update(byte[] bytes, int offset, int length) {
        md.update(bytes, offset, length);
        this.length += length;
    }

    /**
     * 
     * @param bytes
     */
    public void update(byte[] bytes) {
        update(bytes, 0, bytes.length);
    }

    /**
     * 
     * @return long
     */
    @Override
    public long getValue() {
        return value;
    }

    /**
     * 
     * @return long
     */
    public long getLength() {
        return length;
    }

    private byte[] getByteArray() {
        if (virgin) {
            digest = md.digest();
            virgin = false;
        }
        byte[] save = new byte[digest.length];
        System.arraycopy(digest, 0, save, 0, digest.length);
        return save;
    }

    /**
     * 
     * @return long
     * @throws UnableToHashException
     */
    public long read() throws UnableToHashException, FileNotFoundException, NoInputStreamException {
        long result = 0;
        if (config.isDoubleFileHashing()) {
            InputStream is = null;
            int retry = 0;
            long lengthBackup = 0;
            String fileName = simpleFile.getFileName();
            loggerCol.logDebug(CLASSDEBUG, this.getClass().getName(), Level.DEBUG, "Hashing: " + fileName);
            int c = 0;
            byte[] buffer = new byte[BUFFERSIZE];
            is = simpleFile.getSimpleDocument().getInputStream();
            if (is != null) {
                md.reset();
                lengthBackup = length;
                try {
                    while ((c = is.read(buffer)) > -1) {
                        md.update(buffer, 0, c);
                    }
                } catch (FileNotFoundException ex) {
                    loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                } catch (IOException ex) {
                    loggerCol.logException(CLASSDEBUG, this.getClass().getName(), Level.FATAL, ex);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            } else {
                throw new UnableToHashException();
            }
            result = (length - lengthBackup);
        }
        return result;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     *
     * @return String
     */
    @Override
    public String toString() {
        String result = null;
        byte[] bytes = getByteArray();
        if (bytes != null) {
            StringBuffer sb = new StringBuffer(bytes.length * 2);
            int b;
            for (int i = 0; i < bytes.length; i++) {
                b = bytes[i] & 0xFF;
                sb.append(HEX[b >>> 4]);
                sb.append(HEX[b & 0x0F]);
            }
            result = sb.toString();
        }
        return result;
    }
}
