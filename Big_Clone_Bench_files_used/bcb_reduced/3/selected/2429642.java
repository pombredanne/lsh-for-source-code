package wtanaka.praya.gale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Hashtable;
import wtanaka.debug.Debug;

/**
 * Cache for public and private keys.
 *
 * The cache consists of three parts:
 * <UL>
 * <LI>A memory component.  This part of the cache may have a maximum
 * size.
 * <LI>An ordered list of on-disk caches to choose from.  These disk
 * caches take the form of directories on the disk available to the
 * Java VM.
 * </UL>
 *
 * The cache supports two operations:
 * <UL>
 * <LI>Add a key to be stored.
 * <LI>Retrieve a previously stored key.
 * </UL>
 *
 * If you would like to explicitly move keys from disk to memory, you
 * will need to retrieve the key and then re-store it.  This may have
 * the side-effect of re-storing the key on the disk in a directory
 * earlier in the list than the directory the key was fetched from.
 *
 * <p>
 * Return to <A href="http://sourceforge.net/projects/praya/">
 * <IMG src="http://sourceforge.net/sflogo.php?group_id=2302&type=1"
 *   alt="Sourceforge" width="88" height="31" border="0"></A>
 * or the <a href="http://praya.sourceforge.net/">Praya Homepage</a>
 *
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2003/12/17 01:25:17 $
 **/
public class KeyCache {

    private static KeyCache s_singleton;

    /**
    * Helper method which ensures that the singleton instance is
    * instantiated.
    **/
    private static synchronized void initSingleton() {
        if (s_singleton == null) {
            s_singleton = new KeyCache();
        }
    }

    /**
    * Returns the singleton instance, instantiating it if it doesn't
    * already exist.
    * @return the singleton instance, instantiating it if it doesn't
    * already exist.
    **/
    public static KeyCache getInstance() {
        if (s_singleton == null) {
            initSingleton();
        }
        return s_singleton;
    }

    /**
    * Map of Location (id) into GalePublicKey
    **/
    private Hashtable m_public = new Hashtable();

    /**
    * Map of Location (id) into GalePrivateKey
    **/
    private Hashtable m_private = new Hashtable();

    /**
    * location of GALE_SYS_DIR
    **/
    private File m_sysDir = new File("/usr/etc/gale");

    /**
    * location of ~/.gale/
    **/
    private File m_confDir = new File(System.getProperty("user.home") + File.separatorChar + ".gale");

    /**
    * default domain
    **/
    private String m_defaultDomain = "bug.in.praya";

    /**
    * Constructor
    **/
    public KeyCache() {
    }

    private static final String hexEncode(byte[] bytes) {
        StringBuffer toReturn = new StringBuffer();
        String[] strings = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        for (int i = 0; i < bytes.length; ++i) {
            toReturn.append(strings[bytes[i] & 0xF]);
            toReturn.append(strings[(bytes[i] >>> 4) & 0xF]);
        }
        return toReturn.toString();
    }

    /**
    * Tries to generate a unique filename.
    **/
    private String generateFilename() {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try {
                digest.update(InetAddress.getLocalHost().toString().getBytes());
            } catch (UnknownHostException e) {
            }
            digest.update(String.valueOf(System.currentTimeMillis()).getBytes());
            digest.update(String.valueOf(Runtime.getRuntime().freeMemory()).getBytes());
            byte[] foo = new byte[128];
            new SecureRandom().nextBytes(foo);
            digest.update(foo);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            Debug.assrt(false);
        }
        return hexEncode(hash);
    }

    /**
    * Adds a key to the cache.  This will store the key in memory, and
    * then to the first disk cache that: exists AND does not already
    * contain the key.  If the key is found in a disk cache while
    * trying to figure out where to store it, this method will not
    * write the key to disk.
    **/
    public synchronized void addKey(GalePublicKey key) {
        Location location = key.getLocation();
        System.err.println("adding key for " + location);
        m_public.put(location, key);
        boolean finished = false;
        File[] writeFiles = getWriteOrderForPublicKey(key.getLocation(), m_sysDir, m_confDir);
        for (int i = 0; i < writeFiles.length && !finished; ++i) {
            try {
                if (writeFiles[i].exists()) {
                    finished = true;
                } else {
                    Debug.println("gale.keycache", "Writing key to " + new File(writeFiles[i].getParent(), generateFilename()));
                    File tmpFile = new File(writeFiles[i].getParent(), generateFilename());
                    FileOutputStream fout = new FileOutputStream(tmpFile);
                    GaleOutputStream gout = new GaleOutputStream(fout);
                    try {
                        key.write(gout);
                        gout.flush();
                        fout.close();
                        tmpFile.renameTo(writeFiles[i]);
                        finished = true;
                    } catch (NotEnoughInfoException e) {
                        fout.close();
                        tmpFile.delete();
                    }
                }
            } catch (IOException e) {
            }
        }
        if (!finished) {
            Debug.println("gale.keycache", "Could not store key");
        }
        this.notifyAll();
    }

    /**
    * Adds a key to the cache.  This will store the key in memory, and
    * then to the first disk cache that: exists AND does not already
    * contain the key.  If the key is found in a disk cache while
    * trying to figure out where to store it, this method will not
    * write the key to disk.
    * @bug does not write to the disk.
    **/
    public synchronized void addKey(GalePrivateKey key) {
        Location location = new Location(key.getID(), m_defaultDomain);
        m_private.put(location, key);
        File[] writeFiles = getWriteOrderForPrivateKey(new Location(key.getID(), m_defaultDomain), m_sysDir, m_confDir);
        boolean finished = false;
        for (int i = 0; i < writeFiles.length && !finished; ++i) {
            try {
                if (writeFiles[i].exists()) {
                    finished = true;
                } else {
                    Debug.println("gale.keycache", "Writing key to " + new File(writeFiles[i].getParent(), generateFilename()));
                    File tmpFile = new File(writeFiles[i].getParent(), generateFilename());
                    FileOutputStream fout = new FileOutputStream(tmpFile);
                    GaleOutputStream gout = new GaleOutputStream(fout);
                    try {
                        key.write(gout);
                        gout.flush();
                        fout.close();
                        tmpFile.renameTo(writeFiles[i]);
                        finished = true;
                    } catch (NotEnoughInfoException e) {
                        fout.close();
                        tmpFile.delete();
                    }
                }
            } catch (IOException e) {
            }
        }
        if (!finished) {
            Debug.println("gale.keycache", "Could not store key");
        }
        this.notifyAll();
    }

    /**
    * Removes a key from the memory cache.  Does not affect the disk
    * cache.
    **/
    public synchronized GalePublicKey removePublicFromMemory(Location id) {
        return (GalePublicKey) m_public.remove(id);
    }

    /**
    * Removes a key from the memory cache.  Does not affect the disk
    * cache.
    **/
    public synchronized GalePrivateKey removePrivateFromMemory(Location id) {
        return (GalePrivateKey) m_private.remove(id);
    }

    /**
    * Retrieve a key.
    * @deprecated use getPublic(Location) instead.
    **/
    public synchronized GalePublicKey getKey(String id) {
        return getPublic(new Location(id, m_defaultDomain));
    }

    /**
    * Retrieve a public key.  This will attempt to find the key in
    * memory.  If the key is not in memory, it will attempt to find it
    * in each of its disk caches, in order.
    **/
    public synchronized GalePublicKey getPublic(Location id) {
        GalePublicKey toReturn = (GalePublicKey) m_public.get(id);
        Debug.println("gale.keycache", "Searching for public key " + id);
        File[] search = getSearchOrderForPublicKey(id, m_sysDir, m_confDir);
        for (int i = 0; i < search.length && toReturn == null; ++i) {
            try {
                Debug.println("gale.keycache", "  Searching in " + search[i]);
                toReturn = new GalePublicKey(search[i]);
                Debug.println("gale.keycache", "  Found!");
            } catch (IOException e) {
            }
        }
        return toReturn;
    }

    /**
    * Retrieve a private key.  This will attempt to find the key in
    * memory.  If the key is not in memory, it will attempt to find it
    * in each of its disk caches, in order.
    * @param id the location for which this method should retrieve a
    * private key.
    * @return the private key corresponding to that location, or null,
    * if it could not be found.
    **/
    public synchronized GalePrivateKey getPrivate(Location id) {
        GalePrivateKey toReturn = (GalePrivateKey) m_private.get(id);
        Debug.println("gale.keycache", "Searching for private key " + id);
        File[] search = getSearchOrderForPrivateKey(id, m_sysDir, m_confDir);
        for (int i = 0; i < search.length && toReturn == null; ++i) {
            try {
                Debug.println("gale.keycache", "  Searching in " + search[i]);
                toReturn = new GalePrivateKey(search[i]);
                Debug.println("gale.keycache", "  Found!");
            } catch (IOException e) {
            }
        }
        return toReturn;
    }

    /**
    * sets the default domain for locations
    **/
    public synchronized void setDefaultDomain(String defaultDomain) {
        m_defaultDomain = defaultDomain;
    }

    /**
    * Sets the GALE_SYS_DIR directory
    **/
    public synchronized void setSysDir(File sysDir) {
        m_sysDir = sysDir;
    }

    /**
    * Sets the ~/.gale/ directory
    **/
    public synchronized void setConfDir(File confDir) {
        m_confDir = confDir;
    }

    /**
    * @deprecated use waitForPublic (long, Location) instead.
    **/
    public synchronized void waitForPublic(long timeout, String id) {
        waitForPublic(timeout, new Location(id, m_defaultDomain));
    }

    /**
    * Waits for a single public key.
    **/
    public synchronized void waitForPublic(long timeout, Location id) {
        waitForFirstPublic(timeout, new Location[] { id });
    }

    /**
    * Waits for the first of the given keys to come back for a maximum
    * of timeout milliseconds.
    **/
    public synchronized void waitForFirstPublic(long timeout, Location[] id) {
        long targetTime = System.currentTimeMillis() + timeout;
        long timeLeft = targetTime - System.currentTimeMillis();
        boolean hasAtLeastOne = false;
        for (int i = 0; i < id.length; ++i) {
            if (getPublic(id[i]) != null) hasAtLeastOne = true;
        }
        while (!hasAtLeastOne && timeLeft > 0) {
            try {
                this.wait(timeLeft);
            } catch (InterruptedException e) {
            }
            timeLeft = targetTime - System.currentTimeMillis();
            hasAtLeastOne = false;
            for (int i = 0; i < id.length; ++i) {
                if (getPublic(id[i]) != null) hasAtLeastOne = true;
            }
        }
    }

    /**
    * Generates the list of files to search for a given public key.
    * For a key named top.sub.bar@dom, this returns the following
    * list:
    * <ul>
    * <li>~/auth/trusted/top.sub.bar@dom.gpub
    * <li>~/auth/local/top.sub.bar@dom.gpub
    * <li>~/auth/cache/top.sub.bar@dom.gpub
    * <li>GALE_SYS_DIR/auth/trusted/top.sub.bar@dom.gpub
    * <li>GALE_SYS_DIR/auth/local/top.sub.bar@dom.gpub
    * <li>GALE_SYS_DIR/auth/cache/top.sub.bar@dom.gpub
    * <li>~/auth/trusted/bar.sub.top@dom
    * <li>~/auth/local/bar.sub.top@dom
    * <li>~/auth/cache/bar.sub.top@dom
    * <li>GALE_SYS_DIR/auth/trusted/bar.sub.top@dom
    * <li>GALE_SYS_DIR/auth/local/bar.sub.top@dom
    * <li>GALE_SYS_DIR/auth/cache/bar.sub.top@dom
    * </ul>
    **/
    public static File[] getSearchOrderForPublicKey(Location key, File sysDir, File confDir) {
        File[] toReturn = new File[12];
        final char l = File.separatorChar;
        final String authTrusted = "auth" + l + "trusted" + l;
        final String authLocal = "auth" + l + "local" + l;
        final String authCache = "auth" + l + "cache" + l;
        final String nwoKeyName = key.getFullString() + ".gpub";
        final String owoKeyName = key.getReversedString();
        toReturn[0] = new File(confDir, authTrusted + nwoKeyName);
        toReturn[1] = new File(confDir, authLocal + nwoKeyName);
        toReturn[2] = new File(confDir, authCache + nwoKeyName);
        toReturn[3] = new File(sysDir, authTrusted + nwoKeyName);
        toReturn[4] = new File(sysDir, authLocal + nwoKeyName);
        toReturn[5] = new File(sysDir, authCache + nwoKeyName);
        toReturn[6] = new File(confDir, authTrusted + owoKeyName);
        toReturn[7] = new File(confDir, authLocal + owoKeyName);
        toReturn[8] = new File(confDir, authCache + owoKeyName);
        toReturn[9] = new File(sysDir, authTrusted + owoKeyName);
        toReturn[10] = new File(sysDir, authLocal + owoKeyName);
        toReturn[11] = new File(sysDir, authCache + owoKeyName);
        return toReturn;
    }

    /**
    * Generates the list of files to search for a given public key.
    * For a public key named top.sub.bar@dom, returns the following
    * list:
    * <ul>
    * <li>~/auth/private/top.sub.bar@dom.gpri
    * <li>GALE_SYS_DIR/auth/private/top.sub.bar@dom.gpri
    * <li>~/auth/private/bar.sub.top@dom
    * <li>GALE_SYS_DIR/auth/private/bar.sub.top@dom
    * </ul>
    **/
    public static File[] getSearchOrderForPrivateKey(Location key, File sysDir, File confDir) {
        File[] toReturn = new File[4];
        final char l = File.separatorChar;
        final String authPrivate = "auth" + l + "private" + l;
        final String nwoKeyName = key.getFullString() + ".gpri";
        final String owoKeyName = key.getReversedString();
        toReturn[0] = new File(confDir, authPrivate + nwoKeyName);
        toReturn[1] = new File(sysDir, authPrivate + nwoKeyName);
        toReturn[2] = new File(confDir, authPrivate + owoKeyName);
        toReturn[3] = new File(sysDir, authPrivate + owoKeyName);
        return toReturn;
    }

    /**
    * Generates the list of files to attempt to write a public key to.
    * Returns the following list:
    * <ul>
    * <li>GALE_SYS_DIR/auth/cache/top.sub@dom.gpub
    * <li>~/auth/cache/top.sub@dom.gpub
    * </ul>
    **/
    public static File[] getWriteOrderForPublicKey(Location key, File sysDir, File confDir) {
        File[] toReturn = new File[2];
        final char l = File.separatorChar;
        final String authCache = "auth" + l + "cache" + l;
        final String nwoKeyName = key.getFullString() + ".gpub";
        toReturn[0] = new File(sysDir, authCache + nwoKeyName);
        toReturn[1] = new File(confDir, authCache + nwoKeyName);
        return toReturn;
    }

    /**
    * Generates the list of files to attempt to write a private key to.
    * Returns the following list:
    * <ul>
    * <li>~/auth/private/top.sub@dom.gpri
    * </ul>
    **/
    public static File[] getWriteOrderForPrivateKey(Location key, File sysDir, File confDir) {
        File[] toReturn = new File[1];
        final char l = File.separatorChar;
        final String authPrivate = "auth" + l + "private" + l;
        final String nwoKeyName = key.getFullString() + ".gpri";
        toReturn[0] = new File(confDir, authPrivate + nwoKeyName);
        return toReturn;
    }
}
