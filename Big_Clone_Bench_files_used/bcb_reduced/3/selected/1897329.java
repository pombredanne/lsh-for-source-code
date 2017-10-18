package edu.uiuc.ncsa.security.storage;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Initializable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.XMLSerializable;
import edu.uiuc.ncsa.security.core.cache.SimpleEntryImpl;
import edu.uiuc.ncsa.security.core.exceptions.GeneralException;
import edu.uiuc.ncsa.security.core.exceptions.UninitializedException;
import edu.uiuc.ncsa.security.core.util.AbstractEnvironment;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A store backed by the file system. This works with all implementations since it just serializes whatever
 * item (which must be of type {@link edu.uiuc.ncsa.security.core.Identifiable}) it has.
 * <h3>How's it work?</h3>
 * There are two directories, one, the storage directory, contains the serialized items themselves. The file names are
 * hashes of the unique identifiers. The other directory, the index directory, contains files whose names are hashes of
 * the other information (e.g. temporary credentials, client identifier, verifier, access token,...).  You will have
 * to override to get the right index entries. (See below)
 * Each of these index files contains a single line which is
 * the file name in the storage directory. So a request to get by the temporary credential will hash the credential,
 * grab the index file, read the name of the actual transaction file and load that.
 * <p>This does no caching of any sort. If you want caching (strongly suggested) create a transaction cache and
 * set its backing cache to be an instance of this.
 * <h3>Usage</h3>
 * To use this, you need to override a couple of methods:<br><br>
 * <ul>
 * <li>{@link #create} to return whatever V really is.</li>
 * <li>{@link #realSave(boolean, edu.uiuc.ncsa.security.core.Identifiable)} Optional if you need some other processing before or after the save.
 * In the case, e.g., of transactions, you want to save them by access token and verifier too, e.g. by invoking
 * {@link #createIndexEntry(String, String)} of the real identifier and the other one. Retrieval of these is
 * done with the method {@link #getIndexEntry(String)}.</li>
 * <li>{@link #realRemove(edu.uiuc.ncsa.security.core.Identifiable)} After calling super on the object, remove all index entries with
 * {@link #removeIndexEntry(String)}.</li>
 * </ul>
 * A store that uses a file system.
 * <p>Created by Jeff Gaynor<br>
 * on 11/3/11 at  1:54 PM
 */
public abstract class FileStore<K, V extends Identifiable> implements Initializable, Store<K, V> {

    protected FileStore(File storeDirectory, File indexDirectory) {
        checkStorage(indexDirectory);
        this.indexDirectory = indexDirectory;
        checkStorage(storeDirectory);
        this.storageDirectory = storeDirectory;
    }

    public File getIndexDirectory() {
        return indexDirectory;
    }

    boolean indexChecked = false;

    File indexDirectory;

    public File getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Make sure everything exists like it's supposed to
     *
     * @param directory
     */
    protected void checkStorage(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        } else {
            if (!directory.isDirectory()) {
                throw new GeneralException("Error: The given directory \"" + directory.getAbsolutePath() + "\" is not a directory");
            }
        }
    }

    File storageDirectory = null;

    /**
     * A hash map of items created by this store. You <i>should</i> keep track of every item created
     * and if an item already exists return that.
     *
     * @return
     */
    public HashMap<K, V> getCreatedItems() {
        if (createdItems == null) {
            createdItems = new HashMap<K, V>();
        }
        return createdItems;
    }

    public void setCreatedItems(HashMap<K, V> createdItems) {
        this.createdItems = createdItems;
    }

    HashMap<K, V> createdItems;

    protected void put(V t) {
        if (t.getIdentifier() == null) {
            throw new UninitializedException("Error: There is no identifier for this store entry");
        }
        update(t);
    }

    public V put(K key, V value) {
        V oldValue = get(value.getIdentifier());
        if (oldValue == null) {
            save(value);
        } else {
            update(value);
        }
        return oldValue;
    }

    protected String hashString(String identifier) {
        return getDigest(identifier);
    }

    String getDigest(String identifier) {
        byte[] digest = getDigestBytes(identifier);
        if (digest == null) {
            return null;
        } else {
            return hexEncode(digest);
        }
    }

    public static String hexEncode(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte aByte : bytes) {
            sb.append(HEX_CHAR[(aByte & 0xf0) >>> 4]);
            sb.append(HEX_CHAR[aByte & 0x0f]);
        }
        return sb.toString();
    }

    byte[] getDigestBytes(String input) {
        try {
            MessageDigest md = getMessageDigest();
            if (md == null) {
                return null;
            }
            byte[] inputBytes = input.getBytes(CHARSET);
            return md.digest(inputBytes);
        } catch (UnsupportedEncodingException x) {
            System.err.println("Unsupported encoding: " + CHARSET);
            return null;
        }
    }

    public static String DIGEST_ALGORITHM = "SHA-1";

    public static String CHARSET = "US-ASCII";

    static char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    MessageDigest messageDigest;

    MessageDigest getMessageDigest() {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            } catch (NoSuchAlgorithmException x) {
                System.err.println("No such algorithm: " + DIGEST_ALGORITHM);
            }
        }
        return messageDigest;
    }

    protected File getItemFile(V t) {
        return getItemFile(t.getIdentifier());
    }

    protected File getItemFile(String identifier) {
        if (identifier == null || identifier.length() == 0) {
            throw new IllegalArgumentException("Error: no identifier found. Cannot retrieve any store entry");
        }
        return new File(getStorageDirectory(), hashString(identifier));
    }

    /**
     * Does the actual work of writing everything to the data directory. Override this as needed and
     * invoke {@link #createIndexEntry(String, String)} to put and entry for the item into the index.
     * When overriding, call this via super first or the item itself will not be saved.
     *
     * @param checkExists
     * @param t
     */
    public void realSave(boolean checkExists, V t) {
        FileOutputStream fos = null;
        File f = getItemFile(t);
        if (checkExists && !f.exists()) {
            throw new GeneralException("Error: Cannot update a non-existent store entry. Save it first.");
        }
        getCreatedItems().remove(t.getIdentifier());
        try {
            fos = new FileOutputStream(f);
            if (t instanceof XMLSerializable) {
                ((XMLSerializable) t).serialize(fos);
                fos.flush();
                fos.close();
            } else {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(t);
                oos.flush();
                oos.close();
            }
        } catch (FileNotFoundException e) {
            try {
                throw new GeneralException("Error loading file \"" + f.getCanonicalPath() + "\" for store entry " + t, e);
            } catch (IOException e1) {
                throw new GeneralException("Error loading file \"" + f + "\" for store entry " + t, e1);
            }
        } catch (IOException e) {
            throw new GeneralException("Error serializing store entry " + t + "to file \"" + f, e);
        }
    }

    /**
     * Add an index entry for an item that is not the unique identifier.
     *
     * @param otherKey   the other key to index
     * @param identifier the unique identifier for this item
     * @throws IOException
     */
    protected void createIndexEntry(String otherKey, String identifier) throws IOException {
        String h = hashString(otherKey);
        File f = new File(getIndexDirectory(), h);
        FileWriter fw = new FileWriter(f);
        fw.write(hashString(identifier));
        fw.flush();
        fw.close();
    }

    /**
     * Finds a file with the given index value. This will look in the index directory for the file with
     * the same name as the lookup, then read the contents of the lookup which is a hashed uri
     *
     * @param hashedName
     * @return
     * @throws IOException
     */
    protected V loadFromIndex(String hashedName) {
        File f = new File(getIndexDirectory(), hashedName);
        if (!f.exists() || !f.isFile()) {
            return null;
        }
        try {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String realFilename = br.readLine();
            return loadFile(new File(getStorageDirectory(), realFilename));
        } catch (IOException e) {
            throw new GeneralException("Error: could not load file from index dir with hashed name \"" + hashedName + "\"", e);
        }
    }

    protected V loadByIdentifier(String identifier) {
        try {
            return loadFile(getItemFile(identifier));
        } catch (GeneralException e) {
            return null;
        }
    }

    V objectDeserialize(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        V t = (V) ois.readObject();
        ois.close();
        return t;
    }

    protected V loadFile(File f) {
        try {
            V t = null;
            FileInputStream fis = new FileInputStream(f);
            if (t instanceof XMLSerializable) {
                try {
                    t = (V) t.getClass().newInstance();
                    ((XMLSerializable) t).deserialize(fis);
                    fis.close();
                } catch (Throwable tt) {
                    t = objectDeserialize(fis);
                }
            } else {
                t = objectDeserialize(fis);
            }
            return t;
        } catch (StreamCorruptedException q) {
            throw new GeneralException("Error: Could not load file \"" + f + "\". This exception usually means either that " + "you have an out of date library for transactions or that the operating system could not find " + "the file. Is your file store configured correctly?", q);
        } catch (IOException x) {
            throw new GeneralException("Error: Could not load file \"" + f + "\". Is your file store configured correctly?", x);
        } catch (ClassNotFoundException e) {
            throw new GeneralException("Error: Cannot find the item's class");
        }
    }

    public FileStore(File file) {
        File storeDir = new File(file, "store");
        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }
        File index = new File(file, "index");
        if (!index.exists()) {
            index.mkdirs();
        }
        storageDirectory = storeDir;
        indexDirectory = index;
    }

    AbstractEnvironment environment;

    @Override
    public AbstractEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public void setEnvironment(AbstractEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void update(V t) {
        realSave(true, t);
    }

    @Override
    public void register(V t) {
        realSave(false, t);
    }

    public void clear() {
        if (getIndexDirectory() != null) {
            for (File f : getIndexDirectory().listFiles()) {
                f.delete();
            }
        }
        if (getStorageDirectory() != null) {
            for (File f : getStorageDirectory().listFiles()) {
                f.delete();
            }
        }
    }

    @Override
    public void save(V t) {
        realSave(false, t);
    }

    public boolean createNew() {
        if (doesStorageExist()) {
            return false;
        }
        getStorageDirectory().mkdirs();
        getIndexDirectory().mkdirs();
        setDestroyed(false);
        return true;
    }

    public boolean isCreated() {
        return doesStorageExist();
    }

    protected boolean doesStorageExist() {
        if (getStorageDirectory() == null || getIndexDirectory() == null) {
            return false;
        }
        return getStorageDirectory().exists() && getIndexDirectory().exists();
    }

    public boolean isInitialized() {
        if (!doesStorageExist()) {
            return false;
        }
        return getStorageDirectory().list().length == 0 && getIndexDirectory().list().length == 0;
    }

    boolean destroyed = false;

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean destroy() {
        if (doesStorageExist()) {
            clearEntries();
        }
        getStorageDirectory().delete();
        getIndexDirectory().delete();
        setDestroyed(true);
        return true;
    }

    /**
     * Clears out any and all entries in the storage/index directories.
     */
    protected void clearEntries() {
        for (File f : getStorageDirectory().listFiles()) {
            f.delete();
        }
        for (File f : getIndexDirectory().listFiles()) {
            f.delete();
        }
    }

    public boolean init() {
        if (!doesStorageExist()) {
            return false;
        }
        clearEntries();
        return true;
    }

    public Set<K> keySet() {
        HashSet<K> ids = new HashSet<K>();
        String[] filenames = getStorageDirectory().list();
        for (String filename : filenames) {
            V t = null;
            t = loadFile(new File(getStorageDirectory(), filename));
            ids.add((K) t.getIdentifier());
        }
        return ids;
    }

    /**
     * Not an efficient way to get the values, but this will get them all.
     *
     * @return
     */
    public Collection<V> values() {
        Collection<V> allOfThem = new LinkedList<V>();
        for (File f : getStorageDirectory().listFiles()) {
            allOfThem.add(loadFile(f));
        }
        return allOfThem;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new HashSet<Entry<K, V>>();
        for (File f : getStorageDirectory().listFiles()) {
            V t = loadFile(f);
            entries.add(new SimpleEntryImpl<K, V>((K) t.getIdentifier(), (V) t));
        }
        return entries;
    }

    public int size() {
        return getStorageDirectory().list().length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    class IdentifierFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.equals(id);
        }

        String id;

        IdentifierFileFilter(String identifier) {
            id = hashString(identifier);
        }
    }

    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        return getStorageDirectory().list(new IdentifierFileFilter(key.toString())).length == 1;
    }

    public boolean containsValue(Object value) {
        V t = (V) value;
        return containsKey(t.getIdentifier());
    }

    public V get(Object key) {
        return (V) loadByIdentifier(key.toString());
    }

    public boolean delete(String identifier) {
        V t = loadByIdentifier(identifier);
        try {
            realRemove(t);
        } catch (Throwable throwable) {
            return false;
        }
        return true;
    }

    /**
     * Does the actual removal of the item from the store. Be sure to override this to remove any index
     * entries if you need to.
     *
     * @param oldItem The item (which is Identifiable) to be removed.
     * @return
     */
    protected V realRemove(V oldItem) {
        File f = getItemFile(oldItem.getIdentifier());
        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }
        return oldItem;
    }

    /**
     * This is required by the map interface. The argument is really the identifier. This returns the transaction
     * if there was one already associated with this identifier
     *
     * @param key
     * @return
     */
    public V remove(Object key) {
        if (!containsKey(key)) {
            return null;
        }
        V t = (V) loadByIdentifier(key.toString());
        return realRemove(t);
    }

    public void putAll(Map m) {
        for (Object e : m.entrySet()) {
            put((V) ((Map.Entry) e).getValue());
        }
    }

    /**
     * Remove an index entry (not the actual item!). To remove the item, use {@link #remove(Object)}.
     *
     * @param token
     */
    protected boolean removeIndexEntry(String token) {
        File f = new File(getIndexDirectory(), hashString(token));
        return f.delete();
    }

    /**
     * Get a stored item using a key other than the identifier.
     *
     * @param token
     * @return
     */
    protected V getIndexEntry(String token) {
        return (V) loadFromIndex(hashString(token));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[dataPath=" + getStorageDirectory().getAbsolutePath() + ", indexPath=" + getIndexDirectory().getAbsolutePath() + "]";
    }
}
