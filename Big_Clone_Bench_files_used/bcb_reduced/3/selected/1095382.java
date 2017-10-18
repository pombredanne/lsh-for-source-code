package org.emergent.antbite.savant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import org.emergent.antbite.savant.log.Log;

/**
 * <p>
 * This class is the main store and retrieval unit for items
 * that are cached locally. Once an artifact is retrieved, it
 * must be copied to the local cache using this class.
 * </p>
 *
 * <p>
 * The actual location of the local cache is resolved like
 * this:
 * </p>
 *
 * <ul>
 * <li>The File passed into the constructor</li>
 * <li>If constructor parameter is null, System property
 * named <b>savant.local.repository</b></li>
 * <li>If no system property, a directory in the users home
 * directory named <b>.savant_repository</b></li>
 * </ul>
 *
 * @author  Brian Pontarelli
 */
public class LocalCacheStore {

    private File localCache;

    /**
     * Constructs a new <code>LocalCacheStore</code>.
     *
     * @param   localCache The location of the local cache
     * @throws  SavantException If the file location is not null and not a
     *          directory
     */
    public LocalCacheStore(File localCache) throws SavantException {
        if (localCache == null) {
            String localCacheDir = System.getProperty("savant.local.repository");
            if (localCacheDir == null) {
                localCacheDir = System.getProperty("user.home") + File.separator + ".savant_repository";
            }
            localCache = new File(localCacheDir);
        }
        this.localCache = localCache;
        if (!localCache.exists()) {
            localCache.mkdir();
        } else if (localCache.isFile() || !localCache.canWrite() || !localCache.canRead()) {
            throw new SavantException("Local cache directory [" + localCache.getAbsolutePath() + " is invalid");
        }
    }

    /**
     * Returns the local cache location.
     *
     * @return  The location
     */
    public File getLocation() {
        return localCache;
    }

    /**
     * Resolves the artifact dependencies from the local cache (if they exist)
     * by looking up the dependency XML file and parsing it out. If the file
     * exists and can be read, the given artifact is updated with its dependencies.
     *
     * @param   artifact The artifact to update.
     * @return  True if the artifact was updated, false otherwise.
     * @throws  SavantException If the parsing of the dependencies file failed
     *          for any reason.
     */
    public boolean resolveArtifactDependencies(Artifact artifact) throws SavantException {
        String artDepsFile = artifact.getArtifactDepsFile();
        File file = new File(localCache, artDepsFile);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        ArtifactTools.resolveArtifactDependencies(artifact, file);
        return true;
    }

    /**
     * Finds the locally cached copy of the artifact, if it exists and all the
     * dependencies that the artifact depends on.
     *
     * @param   artifact The artifact to locate
     * @return  The artifact file and its dependency files or null if it does
     *          not exist
     */
    public File find(Artifact artifact) {
        String artifactFile = artifact.getArtifactFile();
        File file = new File(localCache, artifactFile);
        if (!file.exists() || !file.isFile()) {
            file = null;
        } else {
            long curTimeMins = System.currentTimeMillis() / 60000;
            int expiry = artifact.getExpireminutes();
            if (expiry > 0) {
                long time = file.lastModified() / 60000;
                if ((time + expiry) < curTimeMins) {
                    file = null;
                }
            }
            long curTime = System.currentTimeMillis();
            Date expiryDate = artifact.getExpiretime();
            if (expiryDate != null && curTime > expiryDate.getTime()) {
                file = null;
            }
        }
        return file;
    }

    /**
     * Makes a local copy to the local cache location of the file specified. The
     * name of the file remains the same.
     *
     * @param   artifact The artifact descriptor that describes the file parameter
     *          in terms of an artifact.
     * @param   file The file to copy
     * @return  The file stored in the local cache and never null
     */
    public File store(Artifact artifact, File file) throws SavantException {
        try {
            FileInputStream stream = new FileInputStream(file);
            File cached = store(artifact, stream, null, true);
            stream.close();
            return cached;
        } catch (FileNotFoundException fnfe) {
            throw new SavantException(fnfe);
        } catch (IOException ioe) {
            throw new SavantException(ioe);
        }
    }

    /**
     * <p>
     * Makes a local copy to the local cache location of the bytes that will be
     * read from the input stream given. This local copy will have a create and
     * modified date of the current system time when it is stored.
     * </p>
     *
     * <p><b>NOTE</b> The caller must close the input stream given</p>
     *
     * @param   artifact The artifact descriptor that describes the file parameter
     *          in terms of an artifact.
     * @param   stream The InputStream to read from.
     * @param   remoteMD5 (Optional) The MD5 hash of the input file read from an
     *          external source that can be compared to the bytes read from the
     *          input file.
     * @param   failonmd5 Determines whether or not this should fail or simply
     *          warn on MD5 errors. Many MD5 hashes are bad and this can prevent
     *          failures.
     * @return  The file stored in the local cache and never null
     */
    public File store(Artifact artifact, InputStream stream, byte[] remoteMD5, boolean failonmd5) throws SavantException {
        String artifactFile = artifact.getArtifactFile();
        File file = new File(localCache, artifactFile);
        construct(file);
        try {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("MD5");
                digest.reset();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Unable to locate MD5 algorithm");
                System.exit(1);
            }
            FileOutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            BufferedInputStream bis = new BufferedInputStream(stream);
            DigestInputStream dis = new DigestInputStream(bis, digest);
            dis.on(true);
            byte[] ba = new byte[1024];
            int count;
            while ((count = dis.read(ba, 0, 1024)) != -1) {
                bos.write(ba, 0, count);
            }
            dis.close();
            bos.close();
            bis.close();
            os.close();
            byte[] localMD5 = digest.digest();
            if (remoteMD5 != null && localMD5 != null && !Arrays.equals(localMD5, remoteMD5)) {
                if (failonmd5) {
                    file.delete();
                    throw new SavantException("Downloaded artifact [" + artifact + "] corrupt according to MD5. Attempt rebuilding when remote" + " repository is fixed");
                } else {
                    Log.log("Downloaded artifact [" + artifact + "] corrupt" + " according to MD5.", Log.WARN);
                }
            }
        } catch (FileNotFoundException fnfe) {
            throw new SavantException(fnfe);
        } catch (IOException ioe) {
            throw new SavantException(ioe);
        }
        return file;
    }

    /**
     * Makes a local copy to the local cache location of the file specified. The
     * name of the file remains the same.
     *
     * @param   artifact The artifact descriptor that describes the file parameter
     *          in terms of an artifact.
     * @param   file The file to copy
     */
    public void storeDeps(Artifact artifact, File file) throws SavantException {
        try {
            File cached = new File(localCache, artifact.getArtifactDepsFile());
            FileInputStream fis = new FileInputStream(file);
            FileTools.output(fis, cached);
        } catch (IOException ioe) {
            throw new SavantException(ioe);
        }
    }

    /**
     * Deletes the artifact (if it exists) from the local cache.
     *
     * @param   artifact The artifact descriptor that describes the file parameter
     *          in terms of an artifact.
     * @return  True if the artifact was deleted, false otherwise
     */
    public boolean delete(Artifact artifact) {
        File file = new File(localCache, artifact.getArtifactFile());
        boolean deleted = false;
        if (file.exists()) {
            deleted = file.delete();
        }
        return deleted;
    }

    /**
     * Mis-leading, but this method verifies that the file is up-to-date and if
     * it isn't, it clears out the file so that it can be stored from the remote
     * source.
     */
    private void construct(File file) throws SavantException {
        if (file.exists() && file.isDirectory()) {
            throw new SavantException("ArtifactType location is a directory [" + file.getAbsolutePath() + "]");
        }
        if (file.exists() && file.isFile()) {
            file.delete();
        } else if (!file.exists()) {
            File dir = file.getParentFile();
            dir.mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException ioe) {
            throw new SavantException(ioe);
        }
    }
}
