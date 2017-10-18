package com.bonkey.filesystem.backup;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import nu.xom.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import com.bonkey.config.BonkeyConstants;
import com.bonkey.config.ConfigManager;
import com.bonkey.filesystem.browsable.BrowsableFile;
import com.bonkey.filesystem.browsable.BrowsableFileSystem;
import com.bonkey.filesystem.browsable.BrowsableItem;
import com.bonkey.filesystem.local.LocalFileSystem;
import com.bonkey.filesystem.writable.WritableFileSystem;
import com.bonkey.report.BackupFileReport;
import com.bonkey.report.BackupReport;
import com.bonkey.restore.RestoreManager;
import com.bonkey.schedule.BackupManager;

/**
 * A backup file which is separated into chunks. Each chunk is hashed,
 * and hashes are compared with existing hashes (stored locally) to
 * see if the file has been updated
 * 
 * @author marcel
 */
public class LargeBackupFile extends BackupFile {

    private static final long serialVersionUID = -5979104306441065594L;

    /**
	 * Size of divisions in bytes - currently 1024 * 1024
	 */
    private static final int SECTION_SIZE = 1048576;

    private static final String HASH_FILE_EXTENSION = ".hashes";

    private transient int numberOfSectionsCurrently;

    public LargeBackupFile(Element e) {
        super(e);
    }

    public LargeBackupFile(BrowsableItem item, String fileSystem, String uri) {
        super(item, fileSystem, uri);
    }

    public BackupReport doBackup(BackupManager monitor) {
        BackupReport report = new BackupFileReport(getName(), getItem().getURI());
        BackupGroup group = (BackupGroup) getFileSystem();
        WritableFileSystem target = monitor.getCurrentTarget();
        monitor.subTask(Messages.getString("LargeBackupFile.Copying") + getName() + Messages.getString("LargeBackupFile.To") + target.getName());
        String relativePath = group.getFolderName() + '/' + getRelativeURI();
        relativePath += '/';
        try {
            for (int i = 0; i < getNumberOfSections(); i++) {
                monitor.reportFile(relativePath + getName() + '.' + i);
            }
            if (!isEnabled()) {
                report.reportNotRun(Messages.getString("LargeBackupFile.Disabled"));
                return report;
            }
            String hashFilePath = relativePath + getName() + HASH_FILE_EXTENSION;
            BrowsableItem backupTo = target.getFile(relativePath);
            BrowsableItem fileSource = getItem();
            if (backupTo != null && (backupTo instanceof BrowsableFile)) {
                target.deleteFile(backupTo);
                backupTo = null;
            }
            HashSet hashSet = null;
            if (backupTo == null) {
                target.createFolder(relativePath);
                hashSet = createEmptyHashSet();
            } else {
                BrowsableItem hashFile = backupTo.getFileSystem().getFile(hashFilePath);
                if (hashFile == null) {
                    hashSet = createEmptyHashSet();
                } else {
                    if (isModified(hashFile, fileSource)) {
                        hashSet = getHashes((BrowsableFile) hashFile);
                    }
                }
            }
            if (hashSet != null) {
                long size = uploadIfHashesDontMatch(monitor, fileSource, hashSet, target, relativePath);
                if (!monitor.isCanceled()) {
                    hashSet.writeHashes(target, hashFilePath);
                    report.reportSuccess(target.getName(), size);
                } else {
                    report.reportCancelled();
                }
            } else {
                monitor.worked(work());
                report.reportNotRun();
            }
        } catch (NoSuchAlgorithmException e) {
            ConfigManager.getConfigManager().logError(Messages.getString("LargeBackupFile.ErrorMD5") + e.getMessage());
            report.reportNonCriticalFailure(e.getMessage());
            monitor.worked(work());
        } catch (FileNotFoundException e) {
            ConfigManager.getConfigManager().logError(Messages.getString("LargeBackupFile.File") + getName() + Messages.getString("LargeBackupFile.NotFound") + e.getMessage());
            report.reportNonCriticalFailure(e.getMessage());
            monitor.worked(work());
        } catch (IOException e) {
            ConfigManager.getConfigManager().logError(Messages.getString("LargeBackupFile.ErrorBackingUp") + getName() + ": " + e.getMessage());
            report.reportCriticalFailure(e);
        }
        return report;
    }

    private HashSet createEmptyHashSet() throws IOException {
        HashSet hashSet = new HashSet(getNumberOfSections());
        hashSet.createEmptyHashSet();
        return hashSet;
    }

    private int getNumberOfSections() throws IOException {
        if (numberOfSectionsCurrently == 0) {
            numberOfSectionsCurrently = (int) Math.ceil(((double) getItem().getSize()) / SECTION_SIZE);
        }
        return numberOfSectionsCurrently;
    }

    /**
	 * Compare hashes and upload if different; update hash array with new hash; return true if an upload is made
	 * @param monitor progress monitor to report on
	 * @param fileSource
	 * @param hashes
	 * @param target
	 * @param relativePath
	 * @return the total bytes transferred
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
    private long uploadIfHashesDontMatch(IProgressMonitor monitor, BrowsableItem fileSource, HashSet hashSet, WritableFileSystem target, String relativePath) throws IOException, NoSuchAlgorithmException {
        DigestInputStream sourceInput = new DigestInputStream(((BrowsableFile) fileSource).getInputStream(), MessageDigest.getInstance("MD5"));
        byte[] currentHash = null;
        byte[] currentData = null;
        long totalTransferred = 0;
        int read = 0;
        int readCurrent = 0;
        int currentSection = 0;
        while (readCurrent != -1 && !monitor.isCanceled()) {
            currentData = new byte[SECTION_SIZE];
            read = 0;
            readCurrent = 0;
            while (read != SECTION_SIZE && readCurrent != -1) {
                readCurrent = sourceInput.read(currentData, read, SECTION_SIZE - read);
                if (readCurrent != -1) {
                    read += readCurrent;
                }
            }
            currentHash = sourceInput.getMessageDigest().digest();
            if (!hashSet.sectionEquals(currentSection, currentHash)) {
                target.putFileContents(new ByteArrayInputStream(currentData, 0, read), read, relativePath + getName() + '.' + currentSection, null, 0);
                totalTransferred += read;
                hashSet.setHash(currentSection, currentHash);
            }
            if (!monitor.isCanceled() && currentSection < getNumberOfSections()) {
                monitor.worked(1);
            }
            currentSection++;
        }
        if (!monitor.isCanceled()) {
            BrowsableItem file = target.getFile(relativePath + getName() + '.' + currentSection);
            while (file != null) {
                target.deleteFile(file);
                currentSection++;
                file = target.getFile(relativePath + getName() + '.' + currentSection);
            }
        }
        sourceInput.close();
        return totalTransferred;
    }

    /**
	 * Get a byte array with all the 16 byte hashes sequentially, after two initial bytes indicating number of hashes
	 * @param hashFile the file with the stored hashes
	 * @return the set of hashes loaded from the file
	 * @throws IOException if an IO operation fails
	 */
    private HashSet getHashes(BrowsableFile hashFile) throws IOException {
        InputStream input = hashFile.getInputStream();
        HashSet hashSet = new HashSet(getNumberOfSections());
        hashSet.loadFromFile(input);
        input.close();
        return hashSet;
    }

    public String getImageName() {
        if (isEnabled()) {
            return BonkeyConstants.ICON_FILE_LARGE;
        }
        return BonkeyConstants.ICON_FILE_EXCL;
    }

    public int work() {
        if (isEnabled()) {
            try {
                return getNumberOfSections();
            } catch (IOException e) {
            }
        }
        return 0;
    }

    class HashSet {

        private byte[] hashes;

        private int totalHashes;

        private int numberOfSections;

        public HashSet(int numberOfSections) {
            this.numberOfSections = numberOfSections;
        }

        public void writeHashes(WritableFileSystem target, String hashFilePath) throws IOException {
            hashes[0] = (byte) ((int) numberOfSections / 255);
            hashes[1] = (byte) (numberOfSections % 255);
            target.putFileContents(new ByteArrayInputStream(hashes, 0, numberOfSections * 16 + 2), numberOfSections * 16 + 2, hashFilePath, null, 0);
        }

        public boolean sectionEquals(int currentSection, byte[] currentHash) {
            int start = currentSection * 16 + 2;
            if (start < hashes.length) {
                for (int i = 0; i < 16; i++) {
                    if (hashes[start + i] != currentHash[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public void createEmptyHashSet() {
            hashes = new byte[numberOfSections * 16 + 2];
            for (int i = 0; i < hashes.length; i++) {
                hashes[i] = 0;
            }
        }

        public void loadFromFile(InputStream input) throws IOException {
            int numberOfHashesFirst = input.read();
            int numberOfHashesSecond = input.read();
            totalHashes = numberOfHashesFirst * 255 + numberOfHashesSecond;
            hashes = new byte[Math.max(totalHashes, numberOfSections) * 16 + 2];
            for (int i = 0; i < hashes.length; i++) {
                hashes[i] = 0;
            }
            byte[] buffer = null;
            int read = 0;
            int sectionNumber = 0;
            while (sectionNumber < totalHashes) {
                buffer = new byte[16];
                read = 0;
                while (read != 16) {
                    read += input.read(buffer, read, 16 - read);
                }
                setHash(sectionNumber, buffer);
                sectionNumber++;
            }
        }

        private void setHash(int sectionNumber, byte[] buffer) {
            int start = sectionNumber * 16 + 2;
            for (int i = 0; i < 16; i++) {
                hashes[start + i] = buffer[i];
            }
        }
    }

    public void doRestoreToFolder(RestoreManager manager, String compress, BrowsableFileSystem restoreFromFS, BrowsableItem restoreRoot, WritableFileSystem restoreToFS, String restoreToPath) throws IOException {
        if (!isEnabled()) return;
        if (!(restoreToFS instanceof LocalFileSystem)) {
            throw new IOException(Messages.getString("LargeBackupFile.ErrorOnlyLocal1") + getName() + Messages.getString("LargeBackupFile.ErrorOnlyLocal2") + getName());
        }
        manager.subTask(getName());
        restoreToPath += getName();
        if (manager.checkOverwrite(restoreToFS, restoreToPath)) {
            String resultingURI = getRelativeRestoreURI(manager, restoreFromFS, restoreRoot, BackupGroup.COMPRESS_NONE);
            resultingURI += '/' + getName() + '.';
            int currentSection = 0;
            BrowsableFile file = (BrowsableFile) restoreFromFS.getFile(resultingURI + currentSection);
            while (file != null) {
                if (currentSection == 0) {
                    ((LocalFileSystem) restoreToFS).putFileContents(file.getInputStream(), SECTION_SIZE, restoreToPath, null, 0);
                } else {
                    ((LocalFileSystem) restoreToFS).appendFileContents(file.getInputStream(), restoreToPath, null, 0);
                }
                currentSection++;
                file = (BrowsableFile) restoreFromFS.getFile(resultingURI + currentSection);
            }
        }
        manager.worked(1);
    }
}
