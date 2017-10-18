package info.metlos.jdc.fileshare;

import info.metlos.jdc.fileshare.list.FileEntry;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

/**
 * The default implementation of the {@link IHasher} interface.
 * 
 * @author metlos
 * 
 * @version $Id: DefaultHasher.java 112 2007-08-12 00:52:45Z metlos $
 */
public class DefaultHasher implements IHasher {

    private MessageDigest messageDigest;

    private List<IHasherListener> listeners;

    private final byte[] buffer = new byte[32768];

    public void deregisterHasherListener(IHasherListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void registerHasherListener(IHasherListener listener) {
        if (listeners == null) {
            listeners = new LinkedList<IHasherListener>();
        }
        listeners.add(listener);
    }

    public MessageDigest getMessageDigest() {
        return messageDigest;
    }

    public void setMessageDigest(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
    }

    /**
	 * This will inform all listeners that a file has been hashed.
	 * 
	 * @param fe
	 * @param absolutePath
	 *            the path of the file
	 */
    protected void dispatchHashedEvent(FileEntry fe) {
        if (listeners != null) {
            for (IHasherListener l : listeners) {
                l.handleFileHashed(this, fe);
            }
        }
    }

    public void createHashFor(FileEntry fe, InputStream fileData) {
        if (messageDigest == null) {
            throw new IllegalStateException("No message digest to use.");
        }
        fe.setContentDigest(createHash(fileData));
        dispatchHashedEvent(fe);
    }

    /**
	 * Computes the hash of the data from the input stream.
	 * 
	 * @param fis
	 *            the input stream to create the hash of
	 * @return the hash of the input data
	 */
    protected byte[] createHash(InputStream fis) {
        try {
            messageDigest.reset();
            int cnt = -1;
            while ((cnt = fis.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, cnt);
            }
            return messageDigest.digest();
        } catch (IOException e) {
            return null;
        }
    }
}
