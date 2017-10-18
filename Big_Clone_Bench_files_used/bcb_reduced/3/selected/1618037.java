package net.sourceforge.liftoff.installer.items;

import java.io.*;
import net.sourceforge.liftoff.installer.*;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;

/**
 * base class for installable that are installed somewhere
 * in the filesystem.<p>
 *
 * FSInstallables have
 * <ul>
 *   <li>a location where they want to be copied to.
 *   <li>a source name
 *   <li>a target name
 *   <li>a size.
 *   <li>a message digest for the source.
 *   <li>a message digest for the installed file.
 *   <li>after installation a name on the file system.
 * </ul>
 * 
 * Some methods (uninstall,checkModified) use the fields <code>size</code>, 
 * <code>installedName</code> and <code>instdigest</code> to be valid. This fields
 * are set by the <code>copy</code> method, if you have installed an FSInstallable,
 * with your own method, make shure these fields are set correctly.
 * <p>
 *
 */
public abstract class FSInstallable extends Installable {

    protected String source;

    protected String target;

    protected String location;

    protected long size;

    protected long lastModified = 0;

    /** MessageDigest of the source file */
    protected byte[] digest = null;

    /** MessageDigest of the copied File. */
    protected byte[] instdigest = null;

    protected String installedName = null;

    protected String backupName = null;

    /**
     * create a new FSInstallable.
     *
     * @param ident an identifier for this object.
     * @param source the name of the source.
     * @param location the location where this file should be copied to.
     * @param target the name of the target.
     */
    public FSInstallable(String ident, String source, String location, String target, long size, byte[] digest) {
        super(ident);
        this.location = location;
        this.source = source;
        this.target = target;
        this.size = size;
        this.digest = digest;
    }

    /**
     * create a new FSInstallable.
     *
     * @param ident an identifier for this object.
     * @param source the name of the source.
     * @param location the location where this file should be copied to.
     * @param target the name of the target.
     */
    public FSInstallable(String ident, String source, String location, String target, long size) {
        this(ident, source, location, target, size, null);
    }

    /**
     * return the size of this installable.<p>
     */
    public long getSize() {
        return size;
    }

    public void addToDirTree(DirTree tree) {
        String fullName = Info.getSystemActions().getTargetName(location, target);
        tree.addNodeForFile(fullName);
    }

    /**
     * return the location for this installable.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Save the given file.
     *
     * If the given file does not exist, no copy will be done.
     * This method also sets the field backupName in this object.
     *
     * @param moni an install monitor.
     * @param fileToBackup Name of the file to backup.
     */
    protected boolean backupFile(InstallMonitor moni, String fileToBackup) throws AbortInstallException {
        File tf = new File(fileToBackup);
        if (tf.exists()) {
            boolean result = moni.canOverwrite(fileToBackup);
            if (!result) {
                File bak = new File(fileToBackup + ".bak");
                tf.renameTo(bak);
                if (!bak.exists()) {
                    System.err.println("can not rename ");
                    return false;
                }
                backupName = fileToBackup + ".bak";
            }
        }
        return true;
    }

    /**
     * call this method from the install method to do a 
     * copy operation for this installable.
     *
     * @param moni an Install Monitor that monitors the copy
     *             operation.
     */
    protected boolean copy(InstallMonitor moni) throws AbortInstallException {
        boolean retryCopy = true;
        String fullName = Info.getSystemActions().getTargetName(location, target);
        moni.showCopyOp(source, fullName);
        if (!backupFile(moni, fullName)) return false;
        while (retryCopy) {
            MessageDigest sha = null;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                System.err.println("can get instance of an SHA-1 Digest Object");
                sha = null;
            }
            InputStream is = null;
            boolean retry = true;
            while (retry) {
                try {
                    is = Info.getInstallationSource().getFile(source, location);
                    retry = false;
                } catch (IOException e) {
                    retry = moni.showIOException(e);
                    if (retry == false) {
                        throw new AbortInstallException();
                    }
                }
            }
            if (is == null) return false;
            OutputStream os = Info.getSystemActions().openOutputFile(location, target);
            if (os == null) return false;
            byte[] buffer = new byte[4096];
            try {
                int bytes = 0;
                try {
                    while ((bytes = is.read(buffer)) >= 0) {
                        if (sha != null) sha.update(buffer, 0, bytes);
                        os.write(buffer, 0, bytes);
                        moni.addInstalledBytes(bytes);
                    }
                } catch (EOFException e) {
                }
                os.flush();
                os.close();
                is.close();
                if (sha != null) {
                    if ((digest == null) || (digest.length == 0)) {
                        digest = sha.digest();
                        retryCopy = false;
                    } else {
                        byte[] newDigest = sha.digest();
                        if (!sha.isEqual(digest, newDigest)) {
                            System.err.println("Checksum error for file " + fullName);
                            retryCopy = moni.showChecksumError(fullName);
                        } else {
                            retryCopy = false;
                        }
                        instdigest = newDigest;
                    }
                }
            } catch (IOException e) {
                System.err.println("error while copying " + source + " to " + fullName + ": " + e);
                retryCopy = moni.showIOException(e);
            }
        }
        installedName = fullName;
        wasInstalled = true;
        File instf = new File(fullName);
        lastModified = instf.lastModified();
        size = instf.length();
        return true;
    }

    /**
     * Copy a text file.<p>
     *
     * This method copies the file line-by-line and does the needed
     * cr/lf conversions.
     */
    protected boolean copyText(InstallMonitor moni) throws AbortInstallException {
        String fullName = Info.getSystemActions().getTargetName(location, target);
        boolean retryCopy = true;
        if (!backupFile(moni, fullName)) return false;
        moni.showCopyOp(source, fullName);
        while (retryCopy) {
            MessageDigest insha = null;
            MessageDigest sha = null;
            try {
                sha = MessageDigest.getInstance("SHA-1");
                insha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                System.err.println("can get instance of an SHA-1 Digest Object");
                sha = null;
                insha = null;
            }
            InputStream is = null;
            boolean retry = true;
            while (retry) {
                try {
                    is = Info.getInstallationSource().getFile(source, location);
                    retry = false;
                } catch (IOException e) {
                    retry = moni.showIOException(e);
                    if (retry == false) {
                        throw new AbortInstallException();
                    }
                }
            }
            if (is == null) return false;
            OutputStream os = Info.getSystemActions().openOutputFile(location, target);
            if (os == null) return false;
            OutputStream ods = null;
            if (sha != null) {
                ods = new DigestOutputStream(os, sha);
                ((DigestOutputStream) ods).on(true);
            } else {
                ods = os;
            }
            InputStream ids = null;
            if (insha != null) {
                ids = new DigestInputStream(is, insha);
                ((DigestInputStream) ids).on(true);
            } else {
                ids = is;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(ids), 4096);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ods));
            try {
                String data = null;
                while ((data = br.readLine()) != null) {
                    bw.write(data);
                    bw.newLine();
                    moni.addInstalledBytes(data.length() + 1);
                }
                bw.flush();
                bw.close();
                br.close();
                ids.close();
                retryCopy = false;
            } catch (IOException e) {
                System.err.println("error while copying " + source + " to " + fullName + ": " + e);
                retryCopy = moni.showIOException(e);
            }
            if (insha != null) {
                byte[] calcDigest = insha.digest();
                if (!insha.isEqual(digest, calcDigest)) {
                    System.err.println("Checksum error for file " + fullName);
                    retryCopy = moni.showChecksumError(fullName);
                }
            }
            if (sha != null) {
                instdigest = sha.digest();
            }
        }
        installedName = fullName;
        wasInstalled = true;
        File instf = new File(fullName);
        lastModified = instf.lastModified();
        size = instf.length();
        return true;
    }

    /**
     * Check for file modifications.
     *
     * This method first checks the size of the file. If the size
     * is the same, it will calculate the SHA-1 checksum of the file.
     * <p>
     * It is very unlikely that we accept an modified file as not
     * modified.
     *
     * @return true if the file was modified.
     */
    public boolean wasModified() {
        File instf = new File(installedName);
        if (!instf.exists()) {
            System.err.println("File " + installedName + " does not exist !");
            return true;
        }
        if ((size != 0) && (instf.length() != size)) {
            System.err.println("stored size = " + size + " actual size " + instf.length());
            return true;
        }
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("can not get instance of an SHA-1 Digest Object");
            sha = null;
        }
        if ((sha == null) || (instdigest == null) || (instdigest.length == 0)) {
            if (instf.lastModified() != lastModified) {
                System.err.println("modification time differs");
                return true;
            }
            return false;
        }
        try {
            int bytes = 0;
            byte[] buffer = new byte[4096];
            FileInputStream is = new FileInputStream(instf);
            while ((bytes = is.read(buffer)) >= 0) {
                sha.update(buffer, 0, bytes);
            }
            is.close();
        } catch (IOException e) {
            System.err.println("error while reading file " + installedName);
            return true;
        }
        byte[] newDigest = sha.digest();
        if (!sha.isEqual(newDigest, instdigest)) {
            System.err.println("digest mismatch");
            return true;
        }
        return false;
    }

    /**
     * Uninstall this installable. <p>
     * This default implementation removes the file if it was
     * not modified.
     */
    public boolean uninstall() {
        if (!wasInstalled) {
            return true;
        }
        if (installedName == null) {
            System.err.println("No name for " + this);
            return false;
        }
        if (wasModified()) {
            System.err.println("won't uninstall " + installedName + " : file was modified");
            return false;
        }
        System.err.println("uninstall " + installedName);
        File instf = new File(installedName);
        if (!instf.exists()) {
            System.err.println("File " + installedName + " does not exist !");
            return true;
        }
        instf.delete();
        if (instf.exists()) {
            System.err.println("can not remove " + installedName);
            return false;
        }
        if (backupName != null) {
            File backup = new File(backupName);
            if (backup.exists()) {
                System.err.println("restore file " + backupName);
                backup.renameTo(instf);
            }
        }
        return true;
    }
}
