package com.threerings.jpkg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import com.threerings.jpkg.ar.ArchiveEntry;

/**
 * A wrapper around TarOutputStream to handle adding files from a destroot into a tar file.
 * Every regular file will have its md5 checksum recorded and the total amount of file data in
 * kilobytes will be stored.
 */
public class PackageTarFile implements ArchiveEntry {

    /**
     * Convenience constructor to create {@link PackageTarFile} with an empty {@link PermissionsMap}.
     * @see PackageTarFile#PackageTarFile(File, PermissionsMap)
     */
    public PackageTarFile(File safeTemp) throws IOException {
        this(safeTemp, new PermissionsMap());
    }

    /**
     * Initialize a PackageTar file.
     * @param safeTemp A location with enough free space to hold the tar data.
     * @param permissions A {@link PermissionsMap} will be used to manipulate entries before being
     * added to the tar file.
     * @throws IOException If the tar file cannot be initialized due to i/o errors.
     */
    public PackageTarFile(File safeTemp, PermissionsMap permissions) throws IOException {
        _permissions = permissions;
        _tar = File.createTempFile("jpkgtmp", ".tar.gz", safeTemp);
        _tarOut = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(_tar)));
        _tarOut.setLongFileMode(TarOutputStream.LONGFILE_GNU);
    }

    /**
     * Add the contents of the supplied directory to the tar file. The root of the directory path
     * will be stripped from all entries being added to the tar file.
     * e.g. If /root is supplied: /root/directory/file.txt -> directory/file.txt
     */
    public void addDirectory(File directory) throws IOException {
        final DestrootWalker walker = new DestrootWalker(directory, this);
        walker.walk();
    }

    /**
     * Add directories and files to the tar archive without stripping a leading path.
     * @see PackageTarFile#addFile(File, String)
     */
    public void addFile(File file) throws DuplicatePermissionsException, IOException {
        addFile(file, NO_STRIP_PATH);
    }

    /**
     * Add directories and files to the tar archive. All file paths are treated as absolute paths.
     * @param file The {@link File} to add to the tar archive.
     * @param stripPath The path to stripped from the start of any entry path before adding it to
     * the tar file.
     * @throws InvalidPathException If the supplied stripPath path cannot be normalized.
     * @throws DuplicatePermissionsException If more than one permission in the defined
     * {@link PermissionsMap} maps to the file being added.
     * @throws IOException If any i/o exceptions occur when appending the file data.
     */
    public void addFile(File file, String stripPath) throws DuplicatePermissionsException, IOException {
        final String normalizedStripPath = PathUtils.stripLeadingSeparators(PathUtils.normalize(stripPath));
        final TarEntry entry = new TarEntry(file.getAbsoluteFile());
        entry.setName(PathUtils.normalize(entry.getName()));
        final String entryPath = entry.getName();
        if (entryPath.startsWith(normalizedStripPath)) {
            final String stripped = entryPath.substring(normalizedStripPath.length());
            entry.setName(PathUtils.stripLeadingSeparators(stripped));
        }
        if (file.isDirectory()) {
            entry.setSize(0);
            entry.setMode(UnixStandardPermissions.STANDARD_DIR_MODE);
        } else if (file.isFile()) {
            entry.setMode(UnixStandardPermissions.STANDARD_FILE_MODE);
        }
        setEntryPermissions(entry, file);
        if (file.isDirectory()) {
            final StringBuffer currentPath = new StringBuffer(entry.getName());
            if (currentPath.charAt(currentPath.length() - 1) != File.separatorChar) {
                currentPath.append(File.separatorChar);
            }
            entry.setName(currentPath.toString());
        }
        _tarOut.putNextEntry(entry);
        if (file.isFile()) {
            handleRegularFile(file, entry);
        }
        _tarOut.closeEntry();
    }

    /**
     * Closes the tar file. This must be called to create a valid tar file.
     */
    public void close() throws IOException {
        _tarOut.close();
    }

    /**
     * Deletes the tar file. Returns true if the file was deleted, false otherwise.
     */
    public boolean delete() {
        return _tar.delete();
    }

    /**
     * Return the map of tar entry paths to md5 checksums for regular files added to this tar file.
     */
    public Map<String, String> getMd5s() {
        return _md5s;
    }

    /**
     * Return the total amount of file data added to this tar file, in kilobytes.
     * If the supplied data is less than a kilobyte, 1 is returned.
     */
    public long getTotalDataSize() {
        return _totalSize;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(_tar);
    }

    public long getSize() {
        return _tar.length();
    }

    public String getPath() {
        return DEB_AR_DATA_FILE;
    }

    public int getUserId() {
        return UnixStandardPermissions.ROOT_USER.getId();
    }

    public int getGroupId() {
        return UnixStandardPermissions.ROOT_GROUP.getId();
    }

    public int getMode() {
        return UnixStandardPermissions.STANDARD_FILE_MODE;
    }

    /**
     * Set the permissions in the TarEntry, applying any matches from the PermissionsMap.
     * @throws DuplicatePermissionsException
     */
    private void setEntryPermissions(TarEntry entry, File file) throws DuplicatePermissionsException {
        entry.setNames(UnixStandardPermissions.ROOT_USER.getName(), UnixStandardPermissions.ROOT_GROUP.getName());
        entry.setIds(UnixStandardPermissions.ROOT_USER.getId(), UnixStandardPermissions.ROOT_GROUP.getId());
        boolean permissionFound = false;
        for (final Entry<String, PathPermissions> permEntry : _permissions.getPermissions()) {
            final String permPath = PathUtils.stripLeadingSeparators(permEntry.getKey());
            final String entryPath = entry.getName();
            final PathPermissions permissions = permEntry.getValue();
            if (!permissionMatches(permPath, entryPath, permissions.isRecursive())) {
                continue;
            }
            if (permissionFound) {
                throw new DuplicatePermissionsException("A permission already mapped to this file. Refusing to apply another. path=[" + file.getAbsolutePath() + "].");
            }
            entry.setNames(permissions.getUser(), permissions.getGroup());
            entry.setIds(permissions.getUid(), permissions.getGid());
            entry.setMode(permissions.getMode());
            permissionFound = true;
        }
    }

    /**
     * Handles adding a regular file {@link File} object to the tar file. This includes
     * calculating and recording the md5 checksum of the file data.
     */
    private void handleRegularFile(File file, TarEntry entry) throws FileNotFoundException, IOException {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                final byte[] buf = new byte[1024];
                int len;
                while ((len = input.read(buf)) > 0) {
                    _tarOut.write(buf, 0, len);
                    md.update(buf, 0, len);
                }
            } finally {
                IOUtils.closeQuietly(input);
            }
            _md5s.put(entry.getName(), new String(Hex.encodeHex(md.digest())));
        } catch (final NoSuchAlgorithmException nsa) {
            throw new RuntimeException("md5 algorthm not found.", nsa);
        }
        _totalSize += bytesToKilobytes(file.length());
    }

    /**
     * Convert bytes into kilobytes. If the supplied bytes are less than a kilobyte, 1 is returned.
     */
    private long bytesToKilobytes(long bytes) {
        if (bytes <= FileUtils.ONE_KB) {
            return 1;
        }
        return bytes / FileUtils.ONE_KB;
    }

    /**
     * Determine if the supplied permission path matches the supplied entry path.
     */
    private boolean permissionMatches(String permPath, String entryPath, boolean recursive) {
        if (!entryPath.startsWith(permPath)) {
            return false;
        }
        if ((permPath.equals(entryPath))) {
            return true;
        }
        if (recursive && entryPath.charAt(permPath.length()) == File.separatorChar) {
            return true;
        }
        return false;
    }

    /** The name of the data file in the Debian package. */
    private static final String DEB_AR_DATA_FILE = "data.tar.gz";

    /** Used to indicate that the file being added should have nothing stripped from its path. */
    private static final String NO_STRIP_PATH = "";

    /** The PermissionsMap to be applied to this tar file. */
    private final PermissionsMap _permissions;

    /** An md5 map for every regular file entry in the tar file. */
    private final Map<String, String> _md5s = new HashMap<String, String>();

    /** The amount of file data added to the tar file, stored in kilobytes. */
    private long _totalSize;

    /** The file location of the tar file. */
    private final File _tar;

    /** Used to write the tar file to the file system. */
    private final TarOutputStream _tarOut;
}
