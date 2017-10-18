package nuts.exts.lucene.store;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;
import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.BufferedIndexOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Constants;

public class VFSDirectory extends Directory {

    private static MessageDigest DIGESTER;

    static {
        try {
            DIGESTER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private boolean checked;

    final void createDir() throws IOException {
        if (!checked) {
            if (!directory.exists()) {
                directory.createFolder();
            }
            checked = true;
        }
    }

    /**
	 * Initializes the directory to create a new file with the given name. This
	 * method should be used in {@link #createOutput}.
	 */
    protected final void initOutput(String name) throws IOException {
        ensureOpen();
        createDir();
        FileObject file = manager.resolveFile(directory, name);
        if (file.exists() && !file.delete()) {
            throw new IOException("Cannot overwrite: " + file);
        }
        file.createFile();
    }

    protected FileSystemManager manager = null;

    protected FileObject directory = null;

    /**
	 * Create a new VFSDirectory for the named location.
	 * 
	 * @param path the path of the directory
	 * @throws IOException
	 */
    public VFSDirectory(FileSystemManager manager, FileObject path) throws IOException {
        this(manager, path, null);
    }

    /**
	 * Create a new VFSDirectory for the named location.
	 * 
	 * @param path the path of the directory
	 * @param lockFactory the lock factory to use, or null for the default (
	 *            {@link SingleInstanceLockFactory});
	 * @throws IOException
	 */
    public VFSDirectory(FileSystemManager manager, FileObject path, LockFactory lockFactory) throws IOException {
        this.manager = manager;
        if (lockFactory == null) {
            lockFactory = new SingleInstanceLockFactory();
        }
        directory = path;
        if (directory.exists() && !directory.getType().equals(FileType.FOLDER)) throw new NoSuchDirectoryException("file '" + directory + "' exists but is not a directory");
        setLockFactory(lockFactory);
    }

    /**
	 * Lists all files (not subdirectories) in the directory. This method never
	 * returns null (throws {@link IOException} instead).
	 * 
	 * @throws NoSuchDirectoryException if the directory does not exist, or does
	 *             exist but is not a directory.
	 * @throws IOException if list() returns null
	 */
    private static String[] listAll(FileObject dir) throws IOException {
        if (!dir.exists()) {
            throw new NoSuchDirectoryException("directory '" + dir + "' does not exist");
        } else if (!dir.getType().equals(FileType.FOLDER)) {
            throw new NoSuchDirectoryException("file '" + dir + "' exists but is not a directory");
        }
        FileObject[] children = dir.getChildren();
        if (children == null) {
            throw new IOException("directory '" + dir + "' exists and is a directory, but cannot be listed: list() returned null");
        }
        List<String> result = new ArrayList<String>(children.length);
        for (FileObject fo : children) {
            if (fo.getType().equals(FileType.FILE)) {
                result.add(fo.getName().getBaseName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
	 * Lists all files (not subdirectories) in the directory.
	 * 
	 * @see #listAll(FileObject)
	 */
    @Override
    public String[] listAll() throws IOException {
        ensureOpen();
        return listAll(directory);
    }

    /** Returns true iff a file with the given name exists. */
    @Override
    public boolean fileExists(String name) throws IOException {
        ensureOpen();
        FileObject file = manager.resolveFile(directory, name);
        return file.exists();
    }

    /** Returns the time the named file was last modified. */
    @Override
    public long fileModified(String name) throws IOException {
        ensureOpen();
        FileObject file = manager.resolveFile(directory, name);
        return file.getContent().getLastModifiedTime();
    }

    /** Set the modified time of an existing file to now. */
    @Override
    public void touchFile(String name) throws IOException {
        ensureOpen();
        FileObject file = manager.resolveFile(directory, name);
        file.getContent().setLastModifiedTime(System.currentTimeMillis());
    }

    /** Returns the length in bytes of a file in the directory. */
    @Override
    public long fileLength(String name) throws IOException {
        ensureOpen();
        FileObject file = manager.resolveFile(directory, name);
        return file.getContent().getSize();
    }

    /** Removes an existing file in the directory. */
    @Override
    public void deleteFile(String name) throws IOException {
        ensureOpen();
        FileObject file = manager.resolveFile(directory, name);
        file.delete();
    }

    @Override
    public void sync(String name) throws IOException {
    }

    @Override
    public IndexInput openInput(String name) throws IOException {
        ensureOpen();
        return openInput(name, BufferedIndexInput.BUFFER_SIZE);
    }

    /**
	 * So we can do some byte-to-hexchar conversion below
	 */
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @Override
    public String getLockID() {
        ensureOpen();
        String dirName = directory.getName().getURI();
        byte digest[];
        synchronized (DIGESTER) {
            digest = DIGESTER.digest(dirName.getBytes());
        }
        StringBuilder buf = new StringBuilder();
        buf.append("lucene-");
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i];
            buf.append(HEX_DIGITS[(b >> 4) & 0xf]);
            buf.append(HEX_DIGITS[b & 0xf]);
        }
        return buf.toString();
    }

    /** Closes the store to future operations. */
    @Override
    public synchronized void close() {
        isOpen = false;
    }

    public FileObject getFile() {
        ensureOpen();
        return directory;
    }

    /** For debug output. */
    @Override
    public String toString() {
        return this.getClass().getName() + "@" + directory + " lockFactory=" + getLockFactory();
    }

    /**
	 * Default read chunk size. This is a conditional default: on 32bit JVMs, it
	 * defaults to 100 MB. On 64bit JVMs, it's <code>Integer.MAX_VALUE</code>.
	 * 
	 * @see #setReadChunkSize
	 */
    public static final int DEFAULT_READ_CHUNK_SIZE = Constants.JRE_IS_64BIT ? Integer.MAX_VALUE : 100 * 1024 * 1024;

    private int chunkSize = DEFAULT_READ_CHUNK_SIZE;

    /**
	 * Sets the maximum number of bytes read at once from the underlying file
	 * during {@link IndexInput#readBytes}. The default value is
	 * {@link #DEFAULT_READ_CHUNK_SIZE};
	 * 
	 * <p>
	 * This was introduced due to <a
	 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6478546">Sun JVM
	 * Bug 6478546</a>, which throws an incorrect OutOfMemoryError when
	 * attempting to read too many bytes at once. It only happens on 32bit JVMs
	 * with a large maximum heap size.
	 * </p>
	 * 
	 * <p>
	 * Changes to this value will not impact any already-opened
	 * {@link IndexInput}s. You should call this before attempting to open an
	 * index on the directory.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE</b>: This value should be as large as possible to reduce any
	 * possible performance impact. If you still encounter an incorrect
	 * OutOfMemoryError, trying lowering the chunk size.
	 * </p>
	 */
    public final void setReadChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (!Constants.JRE_IS_64BIT) {
            this.chunkSize = chunkSize;
        }
    }

    /**
	 * The maximum number of bytes to read at once from the underlying file
	 * during {@link IndexInput#readBytes}.
	 * 
	 * @see #setReadChunkSize
	 */
    public final int getReadChunkSize() {
        return chunkSize;
    }

    /** Creates an IndexOutput for the file with the given name. */
    @Override
    public IndexOutput createOutput(String name) throws IOException {
        initOutput(name);
        return new VFSIndexOutput(manager.resolveFile(directory, name));
    }

    /** Creates an IndexInput for the file with the given name. */
    @Override
    public IndexInput openInput(String name, int bufferSize) throws IOException {
        ensureOpen();
        return new VFSIndexInput(manager.resolveFile(directory, name), bufferSize, getReadChunkSize());
    }

    protected static class VFSIndexInput extends BufferedIndexInput {

        protected static class Descriptor {

            protected volatile boolean isOpen;

            RandomAccessContent rac;

            long position;

            final long length;

            public Descriptor(FileObject file) throws IOException {
                rac = file.getContent().getRandomAccessContent(RandomAccessMode.READ);
                isOpen = true;
                length = rac.length();
            }

            public void close() throws IOException {
                if (isOpen) {
                    isOpen = false;
                    rac.close();
                }
            }

            public void seek(long position) throws IOException {
                rac.seek(position);
                this.position = position;
            }

            public void read(byte[] b, int off, int len) throws IOException {
                rac.readFully(b, off, len);
            }
        }

        protected final Descriptor file;

        boolean isClone;

        protected final int chunkSize;

        public VFSIndexInput(FileObject path, int bufferSize, int chunkSize) throws IOException {
            super(bufferSize);
            file = new Descriptor(path);
            this.chunkSize = chunkSize;
        }

        /** IndexInput methods */
        @Override
        protected void readInternal(byte[] b, int offset, int len) throws IOException {
            synchronized (file) {
                long position = getFilePointer();
                if (position != file.position) {
                    file.seek(position);
                }
                int total = 0;
                try {
                    do {
                        final int readLength;
                        if (total + chunkSize > len) {
                            readLength = len - total;
                        } else {
                            readLength = chunkSize;
                        }
                        file.read(b, offset + total, readLength);
                        file.seek(file.position + readLength);
                        total += readLength;
                    } while (total < len);
                } catch (OutOfMemoryError e) {
                    final OutOfMemoryError outOfMemoryError = new OutOfMemoryError("OutOfMemoryError likely caused by the Sun VM Bug described in " + "https://issues.apache.org/jira/browse/LUCENE-1566; try calling FSDirectory.setReadChunkSize " + "with a a value smaller than the current chunks size (" + chunkSize + ")");
                    outOfMemoryError.initCause(e);
                    throw outOfMemoryError;
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (!isClone) file.close();
        }

        @Override
        protected void seekInternal(long position) {
        }

        @Override
        public long length() {
            return file.length;
        }

        @Override
        public Object clone() {
            VFSIndexInput clone = (VFSIndexInput) super.clone();
            clone.isClone = true;
            return clone;
        }
    }

    protected static class VFSIndexOutput extends BufferedIndexOutput {

        RandomAccessContent rac = null;

        private volatile boolean isOpen;

        public VFSIndexOutput(FileObject path) throws IOException {
            rac = path.getContent().getRandomAccessContent(RandomAccessMode.READWRITE);
            isOpen = true;
        }

        /** output methods: */
        @Override
        public void flushBuffer(byte[] b, int offset, int size) throws IOException {
            rac.write(b, offset, size);
        }

        @Override
        public void close() throws IOException {
            if (isOpen) {
                boolean success = false;
                try {
                    super.close();
                    success = true;
                } finally {
                    isOpen = false;
                    if (!success) {
                        try {
                            rac.close();
                        } catch (Throwable t) {
                        }
                    } else {
                        rac.close();
                    }
                }
            }
        }

        /** Random-access methods */
        @Override
        public void seek(long pos) throws IOException {
            super.seek(pos);
            rac.seek(pos);
        }

        @Override
        public long length() throws IOException {
            return rac.length();
        }

        @Override
        public void setLength(long length) throws IOException {
        }
    }
}
