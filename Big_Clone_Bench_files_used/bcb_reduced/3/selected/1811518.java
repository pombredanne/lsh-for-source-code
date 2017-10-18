package org.neodatis.odb.core.layers.layer4.crypto;

import java.io.IOException;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.wrappers.OdbThread;
import org.neodatis.tool.wrappers.io.OdbFileIO;

/**@sharpen.ignore
 *  A simple cypher based on AES/MD5. Code from Grant Slender
 * @author osmadja
 *
 */
public class AesMd5Cypher {

    private OdbFileIO fileIO;

    private String fileName;

    private static int CRYPTO_BLOCKSIZE = 16;

    private static int BLOCKSIZE = 2048;

    long currentPosition = 0;

    Cipher decipher;

    Cipher encipher;

    public AesMd5Cypher() {
    }

    public void init(String fileName, boolean canWrite, String password) throws Exception {
        fileIO = new OdbFileIO(fileName, canWrite, password);
        this.fileName = fileName;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] key = md5.digest(password.getBytes());
        SecretKeySpec skey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(md5.digest(key));
        encipher = Cipher.getInstance("AES/CTR/NoPadding");
        encipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);
        decipher = Cipher.getInstance("AES/CTR/NoPadding");
        decipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
    }

    public void close() throws IOException {
        fileIO.close();
        fileIO = null;
    }

    public int read() throws IOException {
        byte[] bytes = new byte[1];
        long l = read(bytes, 0, 1);
        return bytes[0];
    }

    /**
	 * TODO check offste, it is not being used
	 */
    public long read(byte[] bytes, int offfset, int size) throws IOException {
        int totalread = 0;
        long pos = currentPosition;
        int balength = bytes.length;
        long pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
        byte[] blockbit = new byte[BLOCKSIZE];
        try {
            fileIO.seek(pos1);
            totalread += fileIO.read(blockbit, 0, BLOCKSIZE);
            blockbit = decipher.doFinal(blockbit);
            int len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(blockbit, (int) (pos - pos1), bytes, 0, len1);
            pos += len1;
            balength -= len1;
            int blocks = balength / BLOCKSIZE;
            for (int i = 0; i < blocks; i++) {
                pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
                fileIO.seek(pos1);
                totalread += fileIO.read(blockbit, 0, BLOCKSIZE);
                blockbit = decipher.doFinal(blockbit);
                len1 = BLOCKSIZE - (int) (pos - pos1);
                if (balength < len1) {
                    len1 = balength;
                }
                System.arraycopy(blockbit, 0, bytes, bytes.length - balength, len1);
                pos += len1;
                balength -= len1;
            }
            if (balength > 0) {
                blockbit = new byte[BLOCKSIZE];
                pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
                fileIO.seek(pos1);
                totalread += fileIO.read(blockbit, 0, BLOCKSIZE);
                blockbit = decipher.doFinal(blockbit);
                len1 = BLOCKSIZE - (int) (pos - pos1);
                if (balength < len1) {
                    len1 = balength;
                }
                System.arraycopy(blockbit, 0, bytes, bytes.length - balength, len1);
                pos += len1;
                balength -= len1;
            }
            currentPosition += Math.min(totalread, bytes.length);
            return Math.min(totalread, bytes.length);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public void seek(long position) throws IOException {
        try {
            if (position < 0) {
                throw new ODBRuntimeException(NeoDatisError.NEGATIVE_POSITION.addParameter(position));
            }
            currentPosition = position;
            fileIO.seek(currentPosition);
        } catch (IOException e) {
            throw new ODBRuntimeException(NeoDatisError.GO_TO_POSITION.addParameter(position).addParameter(fileIO.length()), e);
        }
    }

    public void write(byte b) throws IOException {
        try {
            byte[] bytes = { b };
            write(bytes, 0, 1);
        } catch (IOException e) {
            DLogger.error(e.getMessage() + " - " + OdbThread.getCurrentThreadName());
            throw e;
        }
    }

    /**
	 * TODO check offset, it is not being used
	 */
    public void write(byte[] bytes, int offset, int size) throws IOException {
        try {
            long pos = currentPosition;
            int balength = bytes.length;
            long pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
            byte[] blockbit = new byte[BLOCKSIZE];
            fileIO.seek(pos1);
            if (fileIO.read(blockbit, 0, BLOCKSIZE) > 0) {
                blockbit = decipher.doFinal(blockbit);
            }
            fileIO.seek(pos1);
            int len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(bytes, 0, blockbit, (int) (pos - pos1), len1);
            blockbit = encipher.doFinal(blockbit);
            fileIO.write(blockbit, 0, BLOCKSIZE);
            pos += len1;
            balength -= len1;
            int blocks = balength / BLOCKSIZE;
            for (int i = 0; i < blocks; i++) {
                pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
                fileIO.seek(pos1);
                if (fileIO.read(blockbit, 0, BLOCKSIZE) > 0) {
                    blockbit = decipher.doFinal(blockbit);
                }
                fileIO.seek(pos1);
                len1 = BLOCKSIZE - (int) (pos - pos1);
                if (balength < len1) {
                    len1 = balength;
                }
                System.arraycopy(bytes, bytes.length - balength, blockbit, 0, len1);
                blockbit = encipher.doFinal(blockbit);
                fileIO.write(blockbit, 0, BLOCKSIZE);
                pos += len1;
                balength -= len1;
            }
            if (balength > 0) {
                blockbit = new byte[BLOCKSIZE];
                pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
                fileIO.seek(pos1);
                if (fileIO.read(blockbit, 0, BLOCKSIZE) > 0) {
                    blockbit = decipher.doFinal(blockbit);
                }
                fileIO.seek(pos1);
                len1 = BLOCKSIZE - (int) (pos - pos1);
                if (balength < len1) {
                    len1 = balength;
                }
                System.arraycopy(bytes, bytes.length - balength, blockbit, 0, len1);
                blockbit = encipher.doFinal(blockbit);
                fileIO.write(blockbit, 0, BLOCKSIZE);
                pos += len1;
                balength -= len1;
            }
            currentPosition += bytes.length;
        } catch (IOException e) {
            DLogger.error(e.getMessage() + " - " + OdbThread.getCurrentThreadName());
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public long length() throws IOException {
        return fileIO.length();
    }

    public boolean lockFile() throws IOException {
        return fileIO.lockFile();
    }

    public boolean isLocked() throws IOException {
        return fileIO.isLocked();
    }

    public boolean unlockFile() throws IOException {
        return fileIO.unlockFile();
    }
}
