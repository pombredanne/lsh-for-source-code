package ch.ethz.dcg.spamato.peerato.common.msg.data;

import java.io.*;
import java.security.*;
import java.util.ArrayList;
import ch.ethz.dcg.spamato.peerato.common.util.XmlObjectStream;

/**
 * Container for the hash of a file and hashes for the chunks of the file.
 * The HashFile can be written to or read from a disk.
 * It can also be generated from a file, that is used to publish new file.
 * 
 * @author Michelle Ackermann
 */
public class HashFile implements Serializable {

    public static final String HASH_FILE_ENDING = ".hashfile.xml";

    public static final String DEFAULT_HASH_ALGORITHM = "SHA";

    public static final int DEFAULT_CHUNK_SIZE = 32;

    private transient File file;

    private Hash fileId;

    private long fileSize;

    private ArrayList<Hash> chunkHashes;

    private int chunkSize;

    private String hashAlgorithm;

    public HashFile() {
        chunkHashes = new ArrayList<Hash>();
    }

    public File getFile() {
        return file;
    }

    public ArrayList<Hash> getChunkHashes() {
        return chunkHashes;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public Hash getFileId() {
        return fileId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
	 * Writes the hash file to the xml file that starts with the path of the shared file and ends with
	 * HashFile.HASH_FILE_ENDING.
	 * If the file already exists, it is overwritten.
	 */
    private void writeToFile() throws IOException {
        File hashFile = new File(file.getAbsolutePath() + HASH_FILE_ENDING);
        writeToFile(hashFile);
    }

    public void writeToFile(File dest) throws IOException {
        dest.getParentFile().mkdirs();
        dest.createNewFile();
        XmlObjectStream objectStream = new XmlObjectStream();
        ObjectOutputStream out = objectStream.createObjectOutputStream(dest);
        out.writeObject(this);
        out.close();
    }

    /**
	 * Create a HashFile from the specified shared file, which can be used to publish the shared file.
	 * The path of the shared file will be relative to the shared folder.
	 * @param sharedFile the shared file to create the HashFile from
	 * @param algorithm the hash algorithm to be used
	 * @param chunkSize the size of the chunks of the shared file
	 * @return a HashFile that represents the id and the hashes of the chunks of the file
	 * @throws IOException if I/O errors occur 
	 * @throws NoSuchAlgorithmException if the hash algorithm taken from PeerSettings isn't a supported hash algorithm
	 */
    public static HashFile createHashFile(File sharedFile, String algorithm, int chunkSize) throws IOException, NoSuchAlgorithmException {
        HashFile hashFile = new HashFile();
        InputStream in = new BufferedInputStream(new FileInputStream(sharedFile));
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] chunk = new byte[chunkSize];
        long fileSize = 0;
        int numBytesRead = in.read(chunk);
        while (numBytesRead != -1) {
            fileSize += numBytesRead;
            Hash hash = Hash.createHash(chunk, numBytesRead, algorithm);
            hashFile.chunkHashes.add(hash);
            md.update(chunk);
            numBytesRead = in.read(chunk);
        }
        hashFile.file = sharedFile;
        hashFile.fileId = new Hash(md.digest());
        hashFile.fileSize = fileSize;
        hashFile.chunkSize = chunkSize;
        hashFile.hashAlgorithm = algorithm;
        hashFile.writeToFile();
        return hashFile;
    }

    /**
	 * Reads the HashFile from an xml file.
	 * @param hashFile file that represents the HashFile
	 * @return the HashFile the given file represents
	 * @throws IOException if the hash file can't be read
	 * @throws MalformedHashFileException if the given file doesn't represent a HashFile,
	 * or the number of chunks doesn't match the number of chunk hashs,
	 * or the chunk size is zero, or chunk hash Array contains other objects than HashFiles
	 */
    public static HashFile readFromFile(File hashFile) throws IOException, MalformedHashFileException {
        XmlObjectStream objectStream = new XmlObjectStream();
        ObjectInputStream in = objectStream.createObjectInputStream(hashFile);
        Object obj = null;
        try {
            obj = in.readObject();
        } catch (ClassNotFoundException e) {
            throw new MalformedHashFileException("The given file doesn't represent a HashFile.");
        }
        in.close();
        if (!(obj instanceof HashFile)) {
            throw new MalformedHashFileException("The given file doesn't represent a HashFile.");
        }
        HashFile hashFileObject = (HashFile) obj;
        if (hashFileObject.getChunkSize() == 0) {
            throw new MalformedHashFileException("Chunksize cannot be zero.");
        }
        int expectedNumberOfChunks = (int) ((hashFileObject.getFileSize() - 1) / hashFileObject.getChunkSize() + 1);
        if (expectedNumberOfChunks != hashFileObject.getChunkHashes().size()) {
            throw new MalformedHashFileException("Expected number of chunks doesn't match number of chunk hash.");
        }
        String hashFilePath = hashFile.getAbsolutePath();
        String filePath = hashFilePath.substring(0, hashFilePath.length() - HASH_FILE_ENDING.length());
        hashFileObject.file = new File(filePath);
        return hashFileObject;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, MalformedHashFileException {
        File file = new File(args[0]);
        HashFile hashFile = HashFile.createHashFile(file, DEFAULT_HASH_ALGORITHM, DEFAULT_CHUNK_SIZE);
        File pluginHashFile = new File(file.getAbsolutePath() + HashFile.HASH_FILE_ENDING);
        HashFile fromFile = HashFile.readFromFile(pluginHashFile);
    }
}
