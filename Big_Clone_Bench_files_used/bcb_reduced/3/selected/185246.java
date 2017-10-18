package net.sf.opendf.cli.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import net.sf.opendf.util.io.BufferingInputStream;
import net.sf.opendf.util.logging.Logging;

public abstract class AbstractCachingGenericInterpreterModelClassFactory extends AbstractGenericInterpreterModelClassFactory {

    /**
	 * Read the model object from an input stream, while producing the cached data as output.
	 * 
	 * @param is The input stream containing the original model data.
	 * @param os The output stream to which the cached data is to be written.
	 * @return The object representing the model.
	 */
    protected abstract Object readModelWhileCaching(InputStream is, OutputStream os);

    /**
	 * Construct the model from the cached data.
	 * 
	 * @param is The input stream containing the cached data.
	 * @return The object representing the model.
	 */
    protected abstract Object readCachedModel(InputStream is);

    @Override
    public Class createClass(String name, ClassLoader topLevelLoader, InputStream source) throws ClassNotFoundException {
        if (!caching()) {
            return super.createClass(name, topLevelLoader, source);
        }
        try {
            Logging.dbg().fine("ACGIModelClassFactory:: Reading class " + name + "...");
            Object model = null;
            synchronized (topLevelLoader) {
                BufferingInputStream bis = new BufferingInputStream(source);
                MessageDigest md = MessageDigest.getInstance(DigestType);
                DigestInputStream dis = new DigestInputStream(bis, md);
                while (dis.read() >= 0) ;
                byte[] digest = md.digest();
                InputStream cache = getCache(name, digest);
                if (cache == null) {
                    bis.resetToStart();
                    OutputStream os = createCache(name, digest);
                    model = readModelWhileCaching(bis, os);
                    bis.close();
                    os.close();
                    installCache(name);
                    Logging.dbg().fine("ACGIModelClassFactory:: Class " + name + " cached.");
                } else {
                    bis.close();
                    model = readCachedModel(cache);
                    cache.close();
                    Logging.dbg().fine("ACGIModelClassFactory:: Reading cached version of " + name + ".");
                }
            }
            GenericInterpreterClassLoader cl = new GenericInterpreterClassLoader(topLevelLoader, name, this.getModelInterface(), null, model);
            Class c = cl.getModelInterpreterClass();
            Logging.dbg().fine("ACGIModelClassFactory:: Class " + name + " loaded.");
            return c;
        } catch (Exception exc) {
            exc.printStackTrace();
            Logging.dbg().severe("ACGIModelClassFactory:: ERROR loading class " + name + ".");
            throw new ClassNotFoundException("Could not create class '" + name + "'.", exc);
        }
    }

    protected AbstractCachingGenericInterpreterModelClassFactory(String cachePath) {
        this.cacheDir = null;
        if (cachePath != null) {
            try {
                File f = new File(cachePath);
                if (f.exists() && f.isDirectory()) {
                    this.cacheDir = f;
                } else {
                    Logging.dbg().warning("Cannot cache at specified location: " + cachePath);
                }
            } catch (Exception e) {
            }
        }
    }

    protected boolean caching() {
        return cacheDir != null;
    }

    protected InputStream getCache(String name, byte[] digest) {
        try {
            byte[] cachedDigest = readDigest(name);
            if (equalDigests(digest, cachedDigest)) {
                File f = getCacheFile(name);
                if (!f.exists()) return null; else return new FileInputStream(f);
            } else {
                return null;
            }
        } catch (Exception whatever) {
            return null;
        }
    }

    protected OutputStream createCache(String name, byte[] digest) throws IOException {
        File f = getCacheTempFile(name);
        if (f.exists()) f.delete();
        OutputStream s = new FileOutputStream(f);
        writeDigest(name, digest);
        return s;
    }

    protected void installCache(String name) {
        File fDigest = getDigestFile(name);
        File fTempDigest = getDigestTempFile(name);
        File fCache = getCacheFile(name);
        File fTempCache = getCacheTempFile(name);
        if (fDigest.exists()) {
            fDigest.delete();
        }
        if (fCache.exists()) {
            fCache.delete();
        }
        if (fTempCache.exists() && fTempCache.exists()) {
            fTempCache.renameTo(fCache);
            fTempDigest.renameTo(fDigest);
        }
    }

    protected byte[] readDigest(String name) throws IOException {
        File f = getDigestFile(name);
        if (!f.exists()) return null;
        InputStream s = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        s.read(b);
        s.close();
        return b;
    }

    protected void writeDigest(String name, byte[] digest) throws IOException {
        File f = getDigestTempFile(name);
        if (f.exists()) f.delete();
        OutputStream s = new FileOutputStream(f);
        s.write(digest);
        s.close();
    }

    protected boolean equalDigests(byte[] d1, byte[] d2) {
        if (d1.length != d2.length) {
            return false;
        }
        for (int i = 0; i < d1.length; i++) {
            if (d1[i] != d2[i]) return false;
        }
        return true;
    }

    protected File getDigestFile(String name) {
        return new File(cacheDir.getAbsolutePath() + File.separator + name + DigestSuffix);
    }

    protected File getDigestTempFile(String name) {
        return new File(cacheDir.getAbsolutePath() + File.separator + name + DigestTempSuffix);
    }

    protected File getCacheFile(String name) {
        return new File(cacheDir.getAbsolutePath() + File.separator + name);
    }

    protected File getCacheTempFile(String name) {
        return new File(cacheDir.getAbsolutePath() + File.separator + name + CacheTempSuffix);
    }

    private File cacheDir;

    private final String DigestType = "SHA-512";

    private final String DigestSuffix = "--digest";

    private final String DigestTempSuffix = "--digest-temp";

    private final String CacheTempSuffix = "--temp";
}
