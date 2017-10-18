package model;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;

/**
 * @author Michal Kolodziejski
 *
 */
public class FileHandler {

    private final dataTypes.File fileInfo;

    /**
	 * Constructor
	 * @param f file details
	 */
    public FileHandler(dataTypes.File f) {
        fileInfo = f;
    }

    /**
	 * Saves a part of the file.
	 * @param data data to be saved
	 * @param partNumber a number of part of the file
	 * @return <b>true</b> if saved successfully, <b>false</b> otherwise
	 */
    public boolean savePartOfFile(byte[] data, int partNumber) {
        if (!(fileInfo instanceof model.ModelDownloadFile)) return false;
        ModelDownloadFile dFile = (ModelDownloadFile) fileInfo;
        java.io.File file = new java.io.File(dFile.getPath());
        if (!file.exists() || !file.canWrite()) {
            System.err.println("Error in FileHandler.saveData(): file does not exist or you don't have rights to write!");
            return false;
        }
        long offset = partNumber * Model.partSize;
        int partLength = Model.partSize;
        if (partNumber == (int) (fileInfo.getSize() / Model.partSize)) {
            partLength = (int) (fileInfo.getSize() - partNumber * Model.partSize);
        }
        ByteBuffer dataBuffer = ByteBuffer.allocate(partLength);
        dataBuffer.put(data, 0, partLength);
        dataBuffer.position(0);
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fc = raf.getChannel();
            fc.lock();
            fc.write(dataBuffer, offset);
            fc.close();
            raf.close();
        } catch (IOException e) {
            System.err.println("Error in FileHandler.saveData(): IOException occured!");
            return false;
        }
        return true;
    }

    /**
	 * Reads and returns a specified part of the file
	 * @param partNumber a number of part of the file 
	 * @return read data, or <b>null</b> if error occured
	 */
    public byte[] getPartOfFile(int partNumber) {
        byte[] data = new byte[Model.partSize];
        java.io.File file = new java.io.File(fileInfo.getPath());
        if (!file.exists() || !file.canRead() || (fileInfo.getSize() < partNumber * Model.partSize)) return null;
        long offset = partNumber * Model.partSize;
        int partLength = Model.partSize;
        if (partNumber == (int) (fileInfo.getSize() / Model.partSize)) {
            partLength = (int) (fileInfo.getSize() - partNumber * Model.partSize);
        }
        byte[] d = new byte[partLength];
        MappedByteBuffer dataBuffer = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            if (!fc.isOpen()) System.err.println("Error in FileHandler.getPartOfFile(): channel not opened");
            dataBuffer = fc.map(MapMode.READ_ONLY, offset, partLength);
            fc.close();
            fis.close();
        } catch (IOException e) {
            System.err.println("Error in FileHandler.getPartOfFile(): IOException occured!");
        }
        if (dataBuffer != null) {
            dataBuffer.get(d);
            for (int i = 0; i < partLength; ++i) data[i] = d[i];
        }
        return data;
    }

    /**
	 * Deletes a file.
	 * @return <b>true</b> if file has been deleted successfully, <b>false</b> otherwise
	 */
    public boolean deleteFile() {
        java.io.File file = new java.io.File(fileInfo.getPath());
        if (!file.exists() || !file.canWrite()) {
            return false;
        }
        return file.delete();
    }

    /**
	 * Counts MD5 sum of the file.
	 * @return MD5 sum
	 */
    public String getMD5sum() {
        String result = "";
        try {
            InputStream is = new FileInputStream(fileInfo.getPath());
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[1024];
            int count;
            count = is.read(buf);
            while (count != -1) {
                md.update(buf, 0, count);
                count = is.read(buf);
            }
            is.close();
            byte[] b = md.digest();
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return result;
    }

    /**
	 * Returns a size of the file.
	 * @return size of the file
	 */
    public long getSize() {
        long size;
        File file = new File(fileInfo.getPath());
        size = file.length();
        return size;
    }

    public boolean fileExists() {
        File file = new File(fileInfo.getPath());
        return file.exists();
    }

    public boolean canRead() {
        File file = new File(fileInfo.getPath());
        return file.canRead();
    }

    public boolean create() {
        boolean success = false;
        File file = new File(fileInfo.getPath());
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (success) {
            long fileSize = fileInfo.getSize();
            int partSize = 2048;
            int steps = (int) ((fileSize + partSize - 1) / partSize);
            byte[] data = new byte[partSize];
            for (int i = 0; i < partSize; ++i) data[i] = '0';
            try {
                FileOutputStream fos = new FileOutputStream(file);
                for (int i = 0; i < steps; ++i) {
                    fos.write(data, 0, ((i != steps - 1) ? partSize : (int) fileSize));
                    fileSize -= partSize;
                }
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return success;
    }
}
