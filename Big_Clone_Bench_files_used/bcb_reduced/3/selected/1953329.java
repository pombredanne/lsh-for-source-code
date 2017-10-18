package com.belmont.backup;

import java.io.*;
import java.util.*;
import java.security.*;

/**
 * An SFile object represents a logical file in a FileStorage
 * container. The file is composed of a directory which encloses the
 * following files:
 * - properties.txt -- List of attributes about this file:
 *    - refCount -- number of manifests that point to this file. When
 *      this count reaches zero it is ok to delete this SFile.
 *    - externalFiles -- A comma-separated list of files in the real
 *      filesystem that represent this file. The digest of those
 *      external files cannot be relied on. This is used so that a
 *      restore operation on a client doesn't require a duplicate copy
 *      in the FileStorage container. 
 *    - length -- the length of this file if it is complete.
 *    - digest -- the SHA-1 digest of this file.
 * - data -- The content of this file. The digest and length are
 *   guaranteed to match. This file may be missing if this file was
 *   partially downloaded or if it is on a client system and is
 *   represented by external files.
 * - partialData -- Partial content for this file. The length of this
 *   file can be used as the offset at which downloading of this file
 *   should continue. When a file is first created it is always
 *   created under this name and then an atomic rename operation is
 *   done only when all the content is complete and the digest has
 *   been verified. 
 */
public class SFile implements IBackupConstants {

    static final boolean DEBUG = false;

    static final String SFILE_PROPFILE = "properties.txt";

    static final String SFILE_DATA = "data";

    static final String SFILE_PARTIAL_DATA = "partialData";

    static final String SFILE_PROP_LENGTH = "length";

    static final String SFILE_PROP_DIGEST = "digest";

    static final String SFILE_PROP_REFCOUNT = "refCount";

    static final String SFILE_PROP_EXTFILES = "externalFiles";

    MessageDigest md;

    File dir;

    String digest;

    int refCount;

    Vector<String> externalFiles;

    Properties properties;

    Vector<File> openTmpFiles;

    Vector<File> closedTmpFiles;

    public SFile(File dir, String digest) {
        this.dir = dir;
        this.digest = digest;
        this.properties = new Properties();
    }

    public synchronized void init() throws IOException {
        try {
            md = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(DIGEST_ALGORITHM + " not found");
        }
        String list[] = dir.list();
        boolean committed = new File(dir, SFILE_DATA).exists();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].startsWith("pdata")) {
                    File f = new File(dir, list[i]);
                    if (committed) {
                        f.delete();
                    } else {
                        addClosedTmpFile(f);
                    }
                }
            }
        }
        File p = new File(dir, SFILE_PROPFILE);
        if (p.exists()) {
            try {
                FileInputStream in = new FileInputStream(p);
                try {
                    properties.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            String r = properties.getProperty(SFILE_PROP_REFCOUNT);
            if (r != null) {
                try {
                    refCount = Integer.parseInt(r);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            r = properties.getProperty(SFILE_PROP_EXTFILES);
            if (r != null) {
                externalFiles = new Vector<String>();
                int ind;
                int start = 0;
                while ((ind = r.indexOf(',', start)) != -1) {
                    externalFiles.addElement(r.substring(start, ind).trim());
                    start = ind + 1;
                }
                ind = r.length();
                if (start < ind) {
                    externalFiles.addElement(r.substring(start, ind));
                }
                if (externalFiles.size() == 0) {
                    externalFiles = null;
                }
            }
        }
    }

    public synchronized int getRefCount() {
        return this.refCount;
    }

    public synchronized int incrementRefCount(int amount) {
        this.refCount += amount;
        saveProperties();
        return refCount;
    }

    public synchronized Vector<String> getExternalFiles() {
        return externalFiles;
    }

    void saveProperties() {
        File f = new File(dir, SFILE_PROPFILE);
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                properties.store(out, "SFile: " + digest);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public InputStream getInputStream() throws IOException {
        return getInputStream(0);
    }

    public synchronized InputStream getInputStream(long offset) throws IOException {
        File data = new File(dir, SFILE_DATA);
        if (data.exists()) {
            if (offset == 0) {
                return new FileInputStream(data);
            } else if (offset <= data.length()) {
                InputStream in = new FileInputStream(data);
                in.skip(offset);
                return in;
            } else {
                throw new IOException("Bad offset: " + offset + " " + data.length());
            }
        } else if (externalFiles != null && externalFiles.size() > 0) {
            Enumeration<String> efiles = externalFiles.elements();
            while (efiles.hasMoreElements()) {
                String p = efiles.nextElement();
                try {
                    InputStream in = new FileInputStream(new File(p));
                    if (offset > 0) {
                        in.skip(offset);
                    }
                    return in;
                } catch (IOException ex) {
                    Utils.log(LOG_ERROR, "Failed to open external file: " + p, ex);
                }
            }
            throw new IOException("No external file found: " + digest);
        }
        throw new IOException("Failed to get input stream for " + digest);
    }

    public void addExternalFile(String path) {
        if (externalFiles == null) {
            externalFiles = new Vector<String>();
        }
        if (!externalFiles.contains(path)) {
            externalFiles.addElement(path);
        }
        String s = properties.getProperty(SFILE_PROP_EXTFILES);
        if (s == null) {
            properties.setProperty(SFILE_PROP_EXTFILES, path);
        } else {
            properties.setProperty(SFILE_PROP_EXTFILES, s + "," + path);
        }
        saveProperties();
    }

    public synchronized boolean checkExternalFiles(String path) {
        path = path.replace('/', File.separatorChar);
        if (externalFiles == null) {
            return false;
        }
        Enumeration<String> fe = externalFiles.elements();
        while (fe.hasMoreElements()) {
            String f = fe.nextElement();
            if (f.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

    public synchronized long getAvailableData() {
        File data = new File(dir, SFILE_DATA);
        if (data.exists()) {
            return data.length();
        } else {
            data = new File(dir, SFILE_PARTIAL_DATA);
            if (data.exists()) {
                return data.length();
            } else {
                long avail = 0;
                Enumeration<String> efiles = externalFiles.elements();
                while (efiles.hasMoreElements()) {
                    String p = efiles.nextElement();
                    long v = new File(p).length();
                    if (v > avail) {
                        avail = v;
                    }
                }
                return avail;
            }
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return getOutputStream(0);
    }

    static class SFileOutputStream extends FileOutputStream {

        SFile sf;

        File f;

        SFileOutputStream(SFile sf, File f, boolean append) throws FileNotFoundException {
            super(f, append);
            this.sf = sf;
            this.f = f;
        }

        public void close() throws IOException {
            super.close();
            synchronized (sf) {
                sf.openTmpFiles.removeElement(f);
                if (new File(sf.dir, SFILE_DATA).exists()) {
                    f.delete();
                } else {
                    sf.addClosedTmpFile(f);
                }
            }
        }
    }

    synchronized void addOpenTmpFile(File f) {
        if (openTmpFiles == null) {
            openTmpFiles = new Vector<File>();
        }
        openTmpFiles.addElement(f);
    }

    synchronized void addClosedTmpFile(File f) {
        if (closedTmpFiles == null) {
            closedTmpFiles = new Vector<File>();
        }
        closedTmpFiles.addElement(f);
    }

    /**
     * Returns an output stream for this SFile. If multiple clients
     * are sending the same file at roughly the same time then we have
     * to create a temporary file for each client. We record the name
     * of the temp file and the stream that was returned. The stream
     * overrides the close method so that we know when the temp file
     * is done. 
     */
    public synchronized OutputStream getOutputStream(long offset) throws IOException {
        File data = new File(dir, SFILE_DATA);
        if (data.exists()) {
            Utils.log(LOG_WARNING, "FILE COMMITTED getOutputStream returning null " + data.getAbsolutePath());
            return null;
        }
        File pdata = null;
        if (offset > 0) {
            if (closedTmpFiles == null) {
                throw new IOException("Bad offset: " + offset + " no matching closed tmp files.");
            }
            int l = closedTmpFiles.size();
            for (int i = 0; i < l; i++) {
                File p = closedTmpFiles.elementAt(i);
                if (p.length() == offset) {
                    pdata = p;
                    break;
                }
            }
            if (pdata == null) {
                throw new IOException("Bad offset: " + offset + " no matching closed tmp files.");
            }
        } else {
            pdata = File.createTempFile("pdata", null, dir);
        }
        addOpenTmpFile(pdata);
        return new SFileOutputStream(this, pdata, offset > 0);
    }

    /**
     * Commits a file which entails checking to see that there is at
     * least one temporary file that matches the digest this SFile
     * object is supposed to have.
     */
    public synchronized void commit() throws IOException {
        File data = new File(dir, SFILE_DATA);
        if (data.exists()) {
            return;
        }
        Enumeration<File> closedFiles = closedTmpFiles.elements();
        while (closedFiles.hasMoreElements()) {
            File pdata = closedFiles.nextElement();
            FileInputStream in = new FileInputStream(pdata);
            byte buf[] = BufferPool.getInstance().get(1024);
            int len;
            md.reset();
            try {
                while ((len = in.read(buf)) != -1) {
                    md.update(buf, 0, len);
                }
            } finally {
                BufferPool.getInstance().put(buf);
                in.close();
            }
            String dg = Utils.formatDigest(md.digest());
            if (!dg.equals(digest)) {
                Utils.log(LOG_WARNING, pdata.getAbsolutePath() + " checksum mismatch: " + dg + " should be " + digest);
                continue;
            } else if (pdata.renameTo(data)) {
                break;
            } else {
                throw new IOException("Rename operation failed for " + data.getAbsolutePath());
            }
        }
        if (!data.exists()) {
            throw new IOException("Commit operation failed for " + data.getAbsolutePath());
        }
        closedFiles = closedTmpFiles.elements();
        while (closedFiles.hasMoreElements()) {
            File f = closedFiles.nextElement();
            f.delete();
        }
    }

    boolean copyExternalFile(File efile) {
        File pdata = new File(dir, SFILE_PARTIAL_DATA);
        try {
            FileInputStream in = new FileInputStream(efile);
            FileOutputStream out = new FileOutputStream(pdata);
            byte buf[] = BufferPool.getInstance().get(1024);
            int len;
            md.reset();
            try {
                try {
                    while ((len = in.read(buf)) != -1) {
                        md.update(buf, 0, len);
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                BufferPool.getInstance().put(buf);
                in.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        String pdigest = Utils.formatDigest(md.digest());
        if (pdigest.equals(digest)) {
            return pdata.renameTo(new File(dir, SFILE_DATA));
        } else {
            Utils.log(LOG_ERROR, "Checksum mismatch in copyExternalFile: " + digest);
            pdata.delete();
        }
        return false;
    }
}
