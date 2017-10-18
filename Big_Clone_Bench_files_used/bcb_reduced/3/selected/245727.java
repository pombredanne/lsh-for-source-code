package org.dcm4chex.archive.hsm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;
import org.dcm4che.util.MD5Utils;
import org.dcm4chex.archive.util.CacheJournal;
import org.dcm4chex.archive.util.FileSystemUtils;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 15055 $ $Date: 2011-03-09 03:35:56 -0500 (Wed, 09 Mar 2011) $
 * @since Mar 13, 2006
 */
public class TarRetrieverService extends ServiceMBeanSupport {

    private static final String NONE = "NONE";

    private static final Set<String> extracting = Collections.synchronizedSet(new HashSet<String>());

    private String dataRootDir;

    private String journalRootDir;

    private CacheJournal journal = new CacheJournal();

    private long minFreeDiskSpace;

    private long prefFreeDiskSpace;

    private ObjectName hsmModuleServicename = null;

    private int bufferSize = 8192;

    private boolean checkMD5 = true;

    public String getCacheRoot() {
        return dataRootDir;
    }

    public void setCacheRoot(String dataRootDir) {
        journal.setDataRootDir(FileUtils.resolve(new File(dataRootDir)));
        this.dataRootDir = dataRootDir;
    }

    public String getCacheJournalRootDir() {
        return journalRootDir;
    }

    public void setCacheJournalRootDir(String journalRootDir) {
        journal.setJournalRootDir(FileUtils.resolve(new File(journalRootDir)));
        this.journalRootDir = journalRootDir;
    }

    public String getCacheJournalFilePathFormat() {
        return journal.getJournalFilePathFormat();
    }

    public void setCacheJournalFilePathFormat(String journalFilePathFormat) {
        if (getState() == STARTED) {
            if (journalFilePathFormat.equals(getCacheJournalFilePathFormat())) {
                return;
            }
            if (!journal.isEmpty()) {
                throw new IllegalStateException("cache not empty!");
            }
        }
        journal.setJournalFilePathFormat(journalFilePathFormat);
    }

    public boolean isCheckMD5() {
        return checkMD5;
    }

    public void setCheckMD5(boolean checkMD5) {
        this.checkMD5 = checkMD5;
    }

    public final int getBufferSize() {
        return bufferSize;
    }

    public final void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public final String getMinFreeDiskSpace() {
        return FileUtils.formatSize(minFreeDiskSpace);
    }

    public final void setMinFreeDiskSpace(String s) {
        this.minFreeDiskSpace = FileUtils.parseSize(s, 0);
    }

    public final String getPreferredFreeDiskSpace() {
        return FileUtils.formatSize(prefFreeDiskSpace);
    }

    public final void setPreferredFreeDiskSpace(String s) {
        this.prefFreeDiskSpace = FileUtils.parseSize(s, 0);
    }

    public String getFreeDiskSpace() throws IOException {
        File dir = journal.getDataRootDir();
        return dir == null || !dir.exists() ? "N/A" : FileUtils.formatSize(FileSystemUtils.freeSpace(dir.getPath()));
    }

    public final String getHSMModulServicename() {
        return hsmModuleServicename == null ? NONE : hsmModuleServicename.toString();
    }

    public final void setHSMModulServicename(String name) throws MalformedObjectNameException {
        this.hsmModuleServicename = NONE.equals(name) ? null : ObjectName.getInstance(name);
    }

    public File retrieveFileFromTAR(String fsID, String fileID) throws IOException, VerifyTarException {
        if (!fsID.startsWith("tar:")) {
            throw new IllegalArgumentException("Not a tar file system: " + fsID);
        }
        int tarEnd = fileID.indexOf('!');
        if (tarEnd == -1) {
            throw new IllegalArgumentException("Missing ! in " + fileID);
        }
        String tarPath = fileID.substring(0, tarEnd);
        File cacheDir = new File(journal.getDataRootDir(), tarPath.replace('/', File.separatorChar));
        String fpath = fileID.substring(tarEnd + 1).replace('/', File.separatorChar);
        File f = new File(cacheDir, fpath);
        if (f.exists()) {
            journal.record(cacheDir, true);
            return f;
        } else {
            boolean extracted = false;
            if (extracting.add(tarPath)) {
                try {
                    fetchAndExtractTar(fsID, tarPath, cacheDir);
                    extracted = true;
                } finally {
                    synchronized (extracting) {
                        extracting.remove(tarPath);
                        extracting.notifyAll();
                    }
                }
            } else {
                if (log.isDebugEnabled()) log.debug("Wait for concurrent fetch and extract of tar: " + tarPath);
                synchronized (extracting) {
                    while (extracting.contains(tarPath)) try {
                        extracting.wait();
                    } catch (InterruptedException e) {
                        log.warn("Wait for concurrent fetch and extract of tar: " + tarPath + " interrupted:", e);
                    }
                }
            }
            journal.record(cacheDir, !extracted);
        }
        if (!f.exists()) {
            log.error("Tar file " + tarPath + " doesn't contain file " + fpath + "!");
            throw new FileNotFoundException(f.getPath());
        }
        return f;
    }

    private void fetchAndExtractTar(String fsID, String tarPath, File cacheDir) throws IOException, VerifyTarException {
        File tarFile = fetchTarFile(fsID, tarPath);
        try {
            extractTar(tarFile, cacheDir);
        } finally {
            fetchHSMFileFinished(fsID, tarPath, tarFile);
        }
    }

    public File fetchTarFile(String fsID, String tarPath) throws IOException {
        return hsmModuleServicename == null ? FileUtils.toFile(fsID.substring(4), tarPath) : fetchHSMFile(fsID, tarPath);
    }

    private File fetchHSMFile(String fsID, String tarPath) throws IOException {
        try {
            return (File) server.invoke(hsmModuleServicename, "fetchHSMFile", new Object[] { fsID, tarPath }, new String[] { String.class.getName(), String.class.getName() });
        } catch (Exception x) {
            log.error("Fetch of HSMFile failed! fsID:" + fsID + " tarPath:" + tarPath, x);
            IOException iox = new IOException("Fetch of HSMFile failed!");
            iox.initCause(x);
            throw iox;
        }
    }

    private void fetchHSMFileFinished(String fsID, String tarPath, File tarFile) throws IOException {
        if (hsmModuleServicename != null) {
            try {
                server.invoke(hsmModuleServicename, "fetchHSMFileFinished", new Object[] { fsID, tarPath, tarFile }, new String[] { String.class.getName(), String.class.getName(), File.class.getName() });
            } catch (Exception x) {
                log.warn("fetchHSMFileFinished! fsID:" + fsID + " tarPath:" + tarPath + " tarFile:" + tarFile, x);
            }
        }
    }

    private void extractTar(File tarFile, File cacheDir) throws IOException, VerifyTarException {
        int count = 0;
        long totalSize = 0;
        long free = FileSystemUtils.freeSpace(journal.getDataRootDir().getPath());
        long fsize = tarFile.length();
        long toDelete = fsize + minFreeDiskSpace - free;
        if (toDelete > 0) free += free(toDelete);
        byte[] buf = new byte[bufferSize];
        TarInputStream tar = new TarInputStream(new FileInputStream(tarFile));
        InputStream in = tar;
        try {
            TarEntry entry = skipDirectoryEntries(tar);
            if (entry == null) throw new IOException("No entries in " + tarFile);
            String entryName = entry.getName();
            Map<String, byte[]> md5sums = null;
            MessageDigest digest = null;
            if ("MD5SUM".equals(entryName)) {
                if (checkMD5) {
                    try {
                        digest = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    md5sums = new HashMap<String, byte[]>();
                    BufferedReader lineReader = new BufferedReader(new InputStreamReader(tar));
                    String line;
                    while ((line = lineReader.readLine()) != null) {
                        md5sums.put(line.substring(34), MD5Utils.toBytes(line.substring(0, 32)));
                    }
                }
                entry = skipDirectoryEntries(tar);
            } else if (checkMD5) {
                getLog().warn("Missing MD5SUM entry in " + tarFile);
            }
            for (; entry != null; entry = skipDirectoryEntries(tar)) {
                entryName = entry.getName();
                byte[] md5sum = null;
                if (md5sums != null && digest != null) {
                    md5sum = md5sums.remove(entryName);
                    if (md5sum == null) throw new VerifyTarException("Unexpected TAR entry: " + entryName + " in " + tarFile);
                    digest.reset();
                    in = new DigestInputStream(tar, digest);
                }
                File fOri = new File(cacheDir, entryName.replace('/', File.separatorChar));
                File f = new File(fOri.getAbsolutePath() + ".tmp");
                File dir = f.getParentFile();
                if (dir.mkdirs()) {
                    log.info("M-WRITE " + dir);
                }
                log.info("M-WRITE " + f);
                FileOutputStream out = new FileOutputStream(f);
                boolean cleanup = true;
                try {
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    cleanup = false;
                } finally {
                    try {
                        out.close();
                    } catch (Exception ignore) {
                    }
                    if (cleanup) {
                        log.info("M-DELETE " + f);
                        f.delete();
                    }
                }
                if (md5sums != null && digest != null) {
                    if (!Arrays.equals(digest.digest(), md5sum)) {
                        log.info("M-DELETE " + f);
                        f.delete();
                        throw new VerifyTarException("Failed MD5 check of TAR entry: " + entryName + " in " + tarFile);
                    } else log.info("MD5 check is successful for " + entryName + " in " + tarFile);
                }
                free -= f.length();
                count++;
                totalSize += f.length();
                if (f.exists()) f.renameTo(fOri);
            }
        } finally {
            tar.close();
        }
        toDelete = prefFreeDiskSpace - free;
        if (toDelete > 0) {
            freeNonBlocking(toDelete);
        }
    }

    private TarEntry skipDirectoryEntries(TarInputStream tar) throws IOException {
        for (TarEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
            if (!entry.isDirectory()) return entry;
        }
        return null;
    }

    private void freeNonBlocking(final long toDelete) {
        new Thread(new Runnable() {

            public void run() {
                try {
                    free(toDelete);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }).start();
    }

    public long free(long size) throws IOException {
        log.info("Start deleting LRU directories of at least " + size + " bytes from TAR cache");
        long deleted = journal.free(size);
        log.info("Finished deleting LRU directories with " + deleted + " bytes from TAR cache");
        return deleted;
    }
}
