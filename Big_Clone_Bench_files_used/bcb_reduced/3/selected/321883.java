package com.atolsystems.atolutilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

/**
 * Class to store data into multiple files. When reading back, the class is able to figure out
 * what is the order of the files and which files contains the same data stream.
 *
 * File format:
 * header, data chunk
 *
 * Header format:
 * encoding (1 byte): either "B" or "T". If "T", remaining part of the header is decoded using base64
 * fileHash (32 bytes): SHA-256 hash of the original file
 * fileSize (4 bytes)
 * dataOffset (4 bytes): the offset of the data chunk in the original file
 * dataSize (4 bytes): data chunk size
 * dataHash (32 bytes): SHA-256 hash of the data in this file
 * 
 * multi bytes integers are written high byte first. Hashes are written low byte first.
 *
 * @author seb
 */
public class MultiPartStore {

    static final byte BASE64_ENCODING = 'T';

    static final byte BINARY_ENCODING = 'B';

    static final int FILEHASH_OFFSET = 1;

    static final int FILEHASH_SIZE = 32;

    static final int FILESIZE_OFFSET = FILEHASH_OFFSET + FILEHASH_SIZE;

    static final int FILESIZE_SIZE = 4;

    static final int DATAOFFSET_OFFSET = FILESIZE_OFFSET + FILESIZE_SIZE;

    static final int DATAOFFSET_SIZE = 4;

    static final int DATASIZE_OFFSET = DATAOFFSET_OFFSET + DATAOFFSET_SIZE;

    static final int DATASIZE_SIZE = 4;

    static final int DATAHASH_OFFSET = DATASIZE_OFFSET + DATASIZE_SIZE;

    static final int DATAHASH_SIZE = 32;

    static final int HEADER_SIZE = DATAHASH_OFFSET + DATAHASH_SIZE;

    static final int HEADER_ENCODED_DATA_CNT = (HEADER_SIZE - 1);

    static final int HEADER64_B64PAD_CNT = (3 - (HEADER_ENCODED_DATA_CNT % 3)) % 3;

    static final int HEADER64_SIZE = 1 + ((HEADER_ENCODED_DATA_CNT + HEADER64_B64PAD_CNT) / 3) * 4;

    static int getHeaderSize(boolean useBase64) {
        return useBase64 ? HEADER64_SIZE : HEADER_SIZE;
    }

    static int getHeaderDataCnt() {
        return HEADER_ENCODED_DATA_CNT;
    }

    static int getHeaderPadCnt(boolean useBase64) {
        return useBase64 ? HEADER64_B64PAD_CNT : 0;
    }

    static int getHeaderDataAndPadCnt(boolean useBase64) {
        return getHeaderDataCnt() + getHeaderPadCnt(useBase64);
    }

    public static class Header implements Comparable {

        final boolean base64;

        final byte[] fileHash;

        final int fileSize;

        final int dataOffset;

        final int dataSize;

        final byte[] dataHash;

        final File file;

        public Header(boolean base64, byte[] fileHash, int fileSize, int dataOffset, int dataSize, byte[] dataHash, File file) {
            this.base64 = base64;
            this.fileHash = fileHash;
            this.fileSize = fileSize;
            this.dataOffset = dataOffset;
            this.dataSize = dataSize;
            this.dataHash = dataHash;
            this.file = file;
        }

        public boolean isBase64() {
            return base64;
        }

        public byte[] getDataHash() {
            return dataHash;
        }

        public int getDataOffset() {
            return dataOffset;
        }

        public int getDataSize() {
            return dataSize;
        }

        public File getFile() {
            return file;
        }

        public byte[] getFileHash() {
            return fileHash;
        }

        public int getFileSize() {
            return fileSize;
        }

        public int compareTo(Object o) {
            Header other = (Header) o;
            if (!Arrays.equals(fileHash, other.fileHash)) {
                String comparison = "\n" + AStringUtilities.bytesToHex(fileHash) + "\n" + AStringUtilities.bytesToHex(other.fileHash);
                throw new RuntimeException("Illegal comparision: the headers are not related to the same file (fileHash mismatch):" + comparison);
            }
            if (dataOffset != other.dataOffset) return dataOffset - other.dataOffset;
            if (this.equals(o)) return 0;
            throw new RuntimeException("Internal state corrpution: two headers are not equal but have same fileHash and same dataOffset");
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Header other = (Header) obj;
            if (this.base64 != other.base64) {
                return false;
            }
            if (!Arrays.equals(this.fileHash, other.fileHash)) {
                return false;
            }
            if (this.fileSize != other.fileSize) {
                return false;
            }
            if (this.dataOffset != other.dataOffset) {
                return false;
            }
            if (this.dataSize != other.dataSize) {
                return false;
            }
            if (!Arrays.equals(this.dataHash, other.dataHash)) {
                return false;
            }
            if (this.file != other.file && (this.file == null || !this.file.equals(other.file))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + (this.base64 ? 1 : 0);
            hash = 41 * hash + Arrays.hashCode(this.fileHash);
            hash = 41 * hash + this.fileSize;
            hash = 41 * hash + this.dataOffset;
            hash = 41 * hash + this.dataSize;
            hash = 41 * hash + Arrays.hashCode(this.dataHash);
            hash = 41 * hash + (this.file != null ? this.file.hashCode() : 0);
            return hash;
        }

        public long getHeaderSize() {
            return MultiPartStore.getHeaderSize(this.base64);
        }
    }

    /**
     * Split the content of the input file into several files
     * @param baseTarget set the base name for output files
     * @param input the file to store in multiple files
     * @param chunkSize the maximum size of each output file
     * @param base64 if true, the file headers are encoded in base64 (does not apply to data from input stream)
     * @return the number of output files (which is the value of the <code>lastChunkIndex</code> field which is in each output file)
     */
    public static int write(File baseTarget, File input, int chunkSize, boolean useBase64) throws FileNotFoundException, IOException {
        return write(baseTarget, new FileInputStream(input), input.length(), chunkSize, useBase64);
    }

    static final class OutputFile {

        final File file;

        RandomAccessFile raf;

        RandomAccessFileOutputStream rafOutputStream;

        OutputStream outputStream;

        final long headerPos;

        final long dataPos;

        final boolean base64;

        long dataCnt = 0;

        public OutputFile(File file, boolean useBase64) throws FileNotFoundException, IOException {
            this.file = file;
            this.base64 = useBase64;
            raf = new RandomAccessFile(file, "rws");
            raf.setLength(0);
            rafOutputStream = new RandomAccessFileOutputStream(raf);
            if (useBase64) {
                raf.write(BASE64_ENCODING);
                outputStream = new Base64OutputStream(rafOutputStream, true, 0, null);
            } else {
                raf.write(BINARY_ENCODING);
                outputStream = rafOutputStream;
            }
            outputStream.flush();
            headerPos = getFilePointer();
            writeHeaderPlaceHolder();
            dataPos = getFilePointer();
        }

        private void writeHeaderPlaceHolder() throws IOException {
            byte[] header = new byte[MultiPartStore.getHeaderDataAndPadCnt(base64)];
            outputStream.write(header);
            outputStream.flush();
        }

        public void writeHeaderAndClose(byte[] header) throws IOException {
            if (base64) {
            }
            if (header.length != MultiPartStore.getHeaderDataAndPadCnt(base64)) throw new RuntimeException();
            seek(headerPos);
            outputStream.write(header);
            close();
        }

        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
            dataCnt += len;
        }

        public void flush() throws IOException {
            outputStream.flush();
        }

        public void close() throws IOException {
            outputStream.close();
        }

        private void seek(long pos) throws IOException {
            outputStream.flush();
            raf.seek(pos);
        }

        private long getFilePointer() throws IOException {
            outputStream.flush();
            return raf.getFilePointer();
        }
    }

    /**
     * Split the content of the input stream into several files
     * @param baseTarget set the base name for output files
     * @param input the input stream to store
     * @param inputLength the size of the data to store, in bytes
     * @param chunkSize the maximum size of each output file
     * @param base64 if true, the file headers are encoded in base64 (does not apply to data from input stream)
     * @return the number of output files
     */
    public static int write(File baseTarget, InputStream input, long inputLength, int chunkSize, boolean useBase64) throws IOException {
        final int headerSize = getHeaderSize(useBase64);
        final int dataChunkSize;
        if (useBase64) {
            final int maxDataChunkSize = chunkSize - headerSize;
            final int rejected = maxDataChunkSize % 3;
            dataChunkSize = maxDataChunkSize - rejected;
        } else dataChunkSize = chunkSize - headerSize;
        if (dataChunkSize <= 0) throw new IllegalArgumentException("chunkSize is smaller or equal than the header size.");
        final int lastChunkIndex = (inputLength % dataChunkSize == 0) ? (int) (inputLength / dataChunkSize) - 1 : (int) (inputLength / dataChunkSize);
        if (lastChunkIndex >= 65536) throw new IllegalArgumentException("chunkSize is so small that more than 65536 files would be generated. This is not supported.");
        MessageDigest inputDigest;
        try {
            inputDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        OutputFile[] outputFiles = new OutputFile[lastChunkIndex + 1];
        MessageDigest[] chunkDigests = new MessageDigest[lastChunkIndex + 1];
        int[] offsets = new int[lastChunkIndex + 1];
        byte[] dataBytes = new byte[4096];
        int cumulatedSize = 0;
        for (int chunkIndex = 0; chunkIndex <= lastChunkIndex; chunkIndex++) {
            File outputFile = new File(baseTarget.getCanonicalPath() + chunkIndex);
            outputFiles[chunkIndex] = new OutputFile(outputFile, useBase64);
            try {
                chunkDigests[chunkIndex] = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            offsets[chunkIndex] = cumulatedSize;
            final int dataSize = (int) ((chunkIndex == lastChunkIndex) ? inputLength - cumulatedSize : dataChunkSize);
            cumulatedSize += dataSize;
            int remaining = dataSize;
            while (remaining != 0) {
                int toRead = Math.min(remaining, dataBytes.length);
                int nRead = input.read(dataBytes, 0, toRead);
                if (-1 == nRead) {
                    throw new EOFException("End of stream reached but at least " + remaining + " additional bytes were expected.");
                }
                outputFiles[chunkIndex].write(dataBytes, 0, nRead);
                chunkDigests[chunkIndex].update(dataBytes, 0, nRead);
                inputDigest.update(dataBytes, 0, nRead);
                remaining -= nRead;
            }
            if (useBase64 && (chunkIndex == lastChunkIndex)) {
                final int padCnt = (3 - (dataSize % 3)) % 3;
                if (padCnt > 0) {
                    byte[] padding = new byte[padCnt];
                    outputFiles[chunkIndex].write(padding, 0, padCnt);
                    chunkDigests[chunkIndex].update(padding, 0, padCnt);
                    inputDigest.update(padding, 0, padCnt);
                }
            }
        }
        byte[] fileHash = inputDigest.digest();
        if (FILEHASH_SIZE != fileHash.length) throw new RuntimeException("32!=fileHash.length. fileHash.length=" + fileHash.length);
        if (inputLength != cumulatedSize) throw new RuntimeException("inputLength!=cumulatedSize: inputLength=" + inputLength + ", cumulatedSize=" + cumulatedSize);
        cumulatedSize = 0;
        for (int chunkIndex = 0; chunkIndex <= lastChunkIndex; chunkIndex++) {
            int thisChunkDataSize = (int) ((chunkIndex != lastChunkIndex) ? dataChunkSize : inputLength - cumulatedSize);
            cumulatedSize += thisChunkDataSize;
            byte[] header = new byte[MultiPartStore.getHeaderDataAndPadCnt(useBase64)];
            System.arraycopy(fileHash, 0, header, 0, fileHash.length);
            AArrayUtilities.int2Bytes((int) inputLength, header, FILESIZE_OFFSET - 1);
            AArrayUtilities.int2Bytes(offsets[chunkIndex], header, DATAOFFSET_OFFSET - 1);
            AArrayUtilities.int2Bytes(thisChunkDataSize, header, DATASIZE_OFFSET - 1);
            byte[] dataHash = chunkDigests[chunkIndex].digest();
            System.arraycopy(dataHash, 0, header, DATAHASH_OFFSET - 1, DATAHASH_SIZE);
            outputFiles[chunkIndex].writeHeaderAndClose(header);
        }
        return lastChunkIndex + 1;
    }

    static class ReconstructedFile {

        SortedSet<Header> srcHeaders;

        File file;

        ReconstructedFile(Header firstHeader) {
            srcHeaders = new TreeSet<Header>();
            srcHeaders.add(firstHeader);
        }

        void add(Header header) {
            srcHeaders.add(header);
        }
    }

    public static Set<File> read(File inputFolder, File outputFolder, String postFix) throws FileNotFoundException, IOException {
        return read(inputFolder.listFiles(), outputFolder, postFix);
    }

    public static Set<File> read(File[] inputFiles, File outputFolder, String postFix) throws FileNotFoundException, IOException {
        Set<Header> headers = new HashSet<Header>();
        for (File f : inputFiles) {
            Header header = getHeader(f);
            if (null != header) {
                headers.add(header);
            }
        }
        HashMap<Integer, ReconstructedFile> map = new HashMap<Integer, ReconstructedFile>();
        for (Header header : headers) {
            byte[] hash = header.getFileHash();
            ReconstructedFile file = map.get(Arrays.hashCode(hash));
            if (null != file) {
                file.add(header);
            } else {
                file = new ReconstructedFile(header);
                map.put(Arrays.hashCode(hash), file);
            }
        }
        for (ReconstructedFile file : map.values()) {
            file.file = reconstructFile(file.srcHeaders, outputFolder, postFix);
        }
        Set<File> out = new HashSet<File>();
        for (ReconstructedFile file : map.values()) out.add(file.file);
        return out;
    }

    static File reconstructFile(SortedSet<Header> headers, File outputFolder, String postFix) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        String fileName = headers.first().getFile().getName() + postFix;
        File out = new File(outputFolder, fileName);
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        try {
            raf.setLength(headers.first().getFileSize());
            for (Header header : headers) {
                File in = header.getFile();
                fis = new FileInputStream(in);
                final int dataBytesBufferSize = 4096;
                byte[] dataBytes = new byte[dataBytesBufferSize];
                try {
                    InputStream inputStream;
                    {
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        inputStream = header.isBase64() ? new Base64InputStream(bis) : bis;
                    }
                    try {
                        long nSkip;
                        long toSkip = header.getHeaderSize();
                        while ((nSkip = fis.skip(toSkip)) != toSkip) {
                            if (-1 == nSkip) throw new RuntimeException("Unexcepted end of file in chunk file " + in.getCanonicalPath());
                            toSkip -= nSkip;
                        }
                        raf.seek(header.dataOffset);
                        int nRead;
                        int remaining = header.getDataSize();
                        while ((remaining > 0) && (nRead = inputStream.read(dataBytes)) != -1) {
                            if (remaining < nRead) nRead = remaining;
                            remaining -= nRead;
                            raf.write(dataBytes, 0, nRead);
                        }
                    } finally {
                        inputStream.close();
                    }
                } finally {
                    fis.close();
                }
            }
        } finally {
            raf.close();
        }
        return out;
    }

    public static int getHeaderSize(byte firstByte) {
        if (firstByte == BASE64_ENCODING) {
            return HEADER64_SIZE;
        } else if (firstByte == BINARY_ENCODING) {
            return HEADER_SIZE;
        } else {
            throw new RuntimeException(AStringUtilities.byteToHex(firstByte) + " is not a valid first byte");
        }
    }

    public static int getChunkSize(byte headerBytes[]) {
        InputStream in = new ByteArrayInputStream(headerBytes);
        Header header = null;
        try {
            header = getHeader(in, null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return header.getDataSize();
    }

    /**
     * Read a file and if the file is a valid chunk file, return the header
     * @param f File to read
     * @return the header of f or null if f is a not chunk file
     */
    public static Header getHeader(File f) throws FileNotFoundException, IOException {
        if (f.isDirectory()) return null;
        InputStream in = new FileInputStream(f);
        return getHeader(in, f);
    }

    /**
     * Read a file and if the file is a valid chunk file, return the header
     * @param in InputStream to read
     * @return the header of f or null if f is a not chunk file
     */
    private static Header getHeader(InputStream inputStream, File f) throws IOException {
        InputStream in64 = null;
        try {
            int firstByte = inputStream.read();
            boolean base64;
            if (firstByte == BASE64_ENCODING) {
                in64 = new Base64InputStream(inputStream);
                inputStream = in64;
                base64 = true;
            } else if (firstByte == BINARY_ENCODING) {
                base64 = false;
            } else {
                return null;
            }
            byte[] fileHash = new byte[FILEHASH_SIZE];
            for (int i = 0; i < FILEHASH_SIZE; i++) fileHash[i] = (byte) inputStream.read();
            int fileSize = 0;
            for (int i = 0; i < FILESIZE_SIZE; i++) fileSize = (fileSize << 8) + (0xFF & inputStream.read());
            int dataOffset = 0;
            for (int i = 0; i < DATAOFFSET_SIZE; i++) dataOffset = (dataOffset << 8) + (0xFF & inputStream.read());
            int dataChunkSize = 0;
            for (int i = 0; i < DATASIZE_SIZE; i++) dataChunkSize = (dataChunkSize << 8) + (0xFF & inputStream.read());
            byte[] dataHash = new byte[DATAHASH_SIZE];
            for (int i = 0; i < DATAHASH_SIZE; i++) dataHash[i] = (byte) inputStream.read();
            if (base64) {
                for (int i = 0; i < HEADER64_B64PAD_CNT; i++) inputStream.read();
            }
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            int nRead;
            int remaining = dataChunkSize;
            final int dataBytesBufferSize = 4096;
            byte[] dataBytes = new byte[dataBytesBufferSize];
            while ((remaining > 0) && (nRead = inputStream.read(dataBytes)) != -1) {
                if (remaining < nRead) {
                    nRead = remaining;
                }
                md.update(dataBytes, 0, nRead);
                remaining -= nRead;
            }
            byte[] actualDataHash = md.digest();
            if (!Arrays.equals(dataHash, actualDataHash)) return null;
            Header header = new Header(base64, fileHash, fileSize, dataOffset, dataChunkSize, dataHash, f);
            return header;
        } finally {
            if (null != in64) in64.close();
            inputStream.close();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File inputDir = new File("./test input");
        File outputDir = new File("./test output");
        AFileUtilities.removeAll(outputDir);
        File input = new File(inputDir, "nimpsmartcardsrc.hex");
        File output = new File(outputDir, "card1.bin");
        aCat.main(new String[] { "dst:" + output.getCanonicalPath(), "inHex", "add:" + input.getCanonicalPath() });
        MultiPartStore.read(outputDir, outputDir, "_reconstructed");
    }
}
