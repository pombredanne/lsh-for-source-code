package org.bitdrive.jlan.impl;

import org.bitdrive.network.P2PServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalFileList {

    private static LocalFileList ourInstance = new LocalFileList();

    private Settings settings;

    private FileListNode filelist;

    private FileListNode activeHashFile;

    private FileListNode activeUpdateFile;

    private Hashtable<Integer, FileListNode> files;

    private Hashtable<Integer, SynchronizedRandomAccessFile> openFiles;

    private Logger logger = Main.getLogger(LogTypes.LOG_FILESYS);

    private FileListNode onDiscFileInfo;

    private Timer speedTimer;

    private long totaltSize = 0;

    private long activeHashSize = 0;

    private boolean activeHash = false;

    private long hashingSpeed = 0;

    private long hashingSpeedValue = 0;

    private long activeUpdateMax = 0;

    private long activeUpdateCount = 0;

    private boolean activeUpdate = false;

    public static LocalFileList getInstance() {
        return ourInstance;
    }

    public byte[] getOnDiscHash() {
        return onDiscFileInfo.getHash();
    }

    public long getOnDiscSize() {
        return onDiscFileInfo.getSize();
    }

    public FileListNode getFilelist() {
        return this.filelist;
    }

    public long getFileSize() {
        return totaltSize;
    }

    public LocalFileList() {
        settings = Settings.getInstance();
        filelist = new FileListNode("", "Local.Files", 0, true);
        files = new Hashtable<Integer, FileListNode>();
        openFiles = new Hashtable<Integer, SynchronizedRandomAccessFile>();
        speedTimer = new Timer();
    }

    public FileListNode updateFile() {
        return activeUpdateFile;
    }

    public double updateStatus() {
        return activeUpdateCount / (double) activeUpdateMax;
    }

    public boolean updateDone() {
        return !activeUpdate;
    }

    public FileListNode hashingFile() {
        if (activeHashFile != null) {
            return activeHashFile;
        }
        return null;
    }

    public double hashingStatus() {
        return activeHashSize / (double) totaltSize;
    }

    public boolean hashingDone() {
        return !activeHash;
    }

    public long hashingSpeed() {
        return this.hashingSpeed;
    }

    public Collection<SynchronizedRandomAccessFile> getOpenFiles() {
        return this.openFiles.values();
    }

    private void updateListFile() {
        try {
            for (FileListNode node : filelist.toList()) {
                if (node.isFolder()) continue;
                if (node.getHash() != null) files.put(getHashKey(node.getHash()), node);
            }
            this.toFile(settings.getFileListPath());
            onDiscFileInfo = new FileListNode(new File(settings.getFileListPath()));
            onDiscFileInfo.hash();
            if (onDiscFileInfo.getHash() == null) {
                logger.log(Level.INFO, "LocalFileList.updateListFile: Failed to hash filelist on disc. File is " + onDiscFileInfo.getLocalPath() + " is folder? " + onDiscFileInfo.isFolder());
            } else {
                files.put(getHashKey(onDiscFileInfo.getHash()), onDiscFileInfo);
            }
            settings.updated(SettingsKeyNames.OBJ_SHARED_FOLDERS.toString());
        } catch (Exception e) {
            logger.log(Level.WARNING, "LocalFileList.updateListFile: Failed to write filelist to disc. (message: " + e.getMessage() + ")");
        }
    }

    public boolean addFolder(String folder) {
        FileListNode node;
        ArrayList<String> sharedFolders;
        node = FileListNode.fromFolderTree(folder);
        sharedFolders = (ArrayList<String>) settings.getObject(SettingsKeyNames.OBJ_SHARED_FOLDERS);
        if (node != null) {
            filelist.addChild(node);
            totaltSize = filelist.sum();
            if (!sharedFolders.contains(folder)) sharedFolders.add(folder);
            return true;
        }
        return false;
    }

    public boolean addFile(String filePath) {
        File file;
        FileListNode node;
        ArrayList<String> sharedFolders;
        file = new File(filePath);
        sharedFolders = (ArrayList<String>) settings.getObject(SettingsKeyNames.OBJ_SHARED_FOLDERS);
        if (file.exists()) {
            node = new FileListNode(file);
            filelist.addChild(node);
            totaltSize = filelist.sum();
            if (!sharedFolders.contains(filePath)) sharedFolders.add(filePath);
            return true;
        }
        return false;
    }

    public void loadFromDisc() throws Exception {
        try {
            filelist = FileListNode.fromXmlFile(settings.getFileListPath());
            updateTree();
            updateListFile();
        } catch (Exception e) {
            logger.log(Level.INFO, "LocalFileList.loadFromDisc: Failed to load filelist from disc.");
        }
        updateListFile();
    }

    public void toFile(String file) throws Exception {
        filelist.toXmlFile(file);
    }

    public void updateFiles() {
        new Thread(new Runnable() {

            public void run() {
                activeUpdate = true;
                activeUpdateCount = 0;
                activeUpdateMax = Long.MAX_VALUE;
                updateTree();
                updateListFile();
                hashFiles();
                activeUpdate = false;
            }
        }).start();
    }

    private void updateTree() {
        ArrayList<String> sharedFolders;
        ArrayList<FileListNode> oldFiles;
        ArrayList<FileListNode> newFiles;
        sharedFolders = (ArrayList<String>) settings.getObject(SettingsKeyNames.OBJ_SHARED_FOLDERS);
        oldFiles = filelist.toList();
        filelist.getChildren().clear();
        for (String path : sharedFolders) {
            File f = new File(path);
            activeUpdateFile = new FileListNode(f);
            if (!f.exists()) continue;
            if (f.isDirectory()) {
                addFolder(path);
            } else {
                addFile(path);
            }
        }
        newFiles = filelist.toList();
        activeUpdateCount = 0;
        activeUpdateMax = newFiles.size() * oldFiles.size();
        for (FileListNode node : oldFiles) {
            activeUpdateFile = node;
            for (FileListNode newNode : newFiles) {
                if ((node.getFileName().equals(newNode.getFileName())) && (node.getSize() == newNode.getSize())) {
                    newNode.setHash(node.getHash());
                    break;
                }
            }
            activeUpdateCount += newFiles.size();
        }
    }

    public void hashFiles() {
        new Thread(new Runnable() {

            public void run() {
                activeHash = true;
                activeHashSize = 0;
                hashTree(filelist);
                updateListFile();
                activeHash = false;
                P2PServer.getInstance().sendFileListUpdate();
            }
        }).start();
        speedTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                if (hashingDone()) {
                    speedTimer.purge();
                    hashingSpeedValue = 0;
                    return;
                }
                hashingSpeed = (long) (hashingSpeedValue / (double) 3);
                hashingSpeedValue = 0;
            }
        }, 0, 3000);
    }

    private void hashFile(FileListNode node) {
        int read;
        byte[] data;
        MessageDigest digest;
        FileInputStream stream;
        logger.log(Level.INFO, "LocalFilelist.hashTree hashing file: " + node.getFileName());
        try {
            data = new byte[512 * 1024];
            digest = MessageDigest.getInstance("SHA-256");
            stream = new FileInputStream(node.getLocalPath());
            while ((read = stream.read(data)) != -1) {
                digest.update(data, 0, read);
                activeHashSize += read;
                hashingSpeedValue += read;
            }
            node.setHash(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "LocalFilelist.hashTree: Failed to find hash instance");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "LocalFilelist.hashTree: File not found (" + node.getLocalPath() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "LocalFilelist.hashTree: Failed to hash file: " + node.getLocalPath());
        }
    }

    private void hashTree(FileListNode node) {
        activeHashFile = node;
        if (!node.isFolder()) {
            if (node.getHash() == null) {
                hashFile(node);
            } else if (node.getHash().length == 0) {
                hashFile(node);
            }
        } else {
            for (FileListNode n : node.getChildren()) {
                if (n == null) continue;
                hashTree(n);
            }
        }
    }

    public SynchronizedRandomAccessFile getFile(byte[] hash) {
        int key;
        FileListNode node;
        SynchronizedRandomAccessFile file;
        key = getHashKey(hash);
        if (openFiles.containsKey(key)) {
            file = openFiles.get(key);
        } else {
            if (key == getHashKey(onDiscFileInfo.getHash())) {
                logger.log(Level.FINE, "LocalFileList.getFile: Filelist requested (hash: " + Misc.byteArrayToHexString(hash) + ")");
                node = onDiscFileInfo;
            } else {
                node = files.get(getHashKey(hash));
            }
            if (node == null) {
                logger.log(Level.SEVERE, "LocalFileList.getFile: failed to open file, node not found! (hash: " + Misc.byteArrayToHexString(hash) + ")");
                return null;
            }
            if (node.getLocalPath() == null) {
                logger.log(Level.SEVERE, "LocalFileList.getFile: failed to open file, filename is NULL!");
                return null;
            }
            try {
                file = new SynchronizedRandomAccessFile(node.getLocalPath());
                openFiles.put(key, file);
            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, "LocalFileList.getFile: failed to open file " + node.getFileName());
                return null;
            }
        }
        closeFiles(file);
        return file;
    }

    private int getHashKey(byte[] hash) {
        int hashcode = 0;
        if (hash == null) {
            logger.log(Level.WARNING, "LocalFileList.getHashKey Hash is NULL!");
            return 0;
        }
        for (byte aData : hash) {
            hashcode <<= 1;
            hashcode ^= aData;
        }
        return hashcode;
    }

    private synchronized void closeFiles(SynchronizedRandomAccessFile file) {
        long lastUsed;
        SynchronizedRandomAccessFile f;
        Iterator<SynchronizedRandomAccessFile> iterator = openFiles.values().iterator();
        while (iterator.hasNext()) {
            f = iterator.next();
            if (file.equals(f)) continue;
            lastUsed = System.currentTimeMillis() - f.getLastUse();
            if (lastUsed > 30000) {
                try {
                    f.close();
                    logger.log(Level.FINEST, "LocalFileList.closeFiles: Closing file");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "LocalFileList.closeFiles: Failed to close file");
                }
                iterator.remove();
            }
        }
    }
}
