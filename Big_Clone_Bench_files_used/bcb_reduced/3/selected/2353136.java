package seventhsense.data.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * File reference for media files.
 * On deserialization, the file must be sent to the FileReferenceManager
 * 
 * @author Parallan
 *
 */
public class FileReference implements Serializable {

    /**
	 * Default serial version
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * Logger
	 */
    private static final Logger LOGGER = Logger.getLogger(FileReference.class.getName());

    /**
	 * Path to the file
	 */
    private final String _path;

    /**
	 * File hash value
	 */
    private byte[] _hash;

    /**
	 * Create a new file reference
	 * 
	 * @param path path to the file
	 */
    public FileReference(final String path) {
        _path = path;
    }

    /**
	 * Calculates the hash for the file and saves it
	 * 
	 * @throws IOException
	 */
    public void generateHash() throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(_path);
        final FileChannel fileChannel = fileInputStream.getChannel();
        final ByteBuffer fileData = ByteBuffer.allocate((int) fileChannel.size());
        if (fileChannel.read(fileData) != fileChannel.size()) {
            throw new IOException("can't read entire file");
        }
        fileInputStream.close();
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            _hash = md.digest(fileData.array());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
	 * Getter for file hash, if one was calculated (else null)
	 * 
	 * @return hash generated hash or null
	 */
    public byte[] getHash() {
        return _hash;
    }

    /**
	 * Gets the path
	 * 
	 * @return path path
	 */
    public String getPath() {
        return _path;
    }

    @Override
    public String toString() {
        return _path;
    }
}
