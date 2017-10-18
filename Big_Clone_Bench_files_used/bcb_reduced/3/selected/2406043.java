package ants.p2p.utils.indexer;

import java.security.*;
import java.io.*;
import ants.p2p.utils.encoding.*;
import ants.p2p.filesharing.*;
import ants.p2p.*;
import java.beans.*;
import java.util.*;

public class DigestManager {

    Object[] digests;

    private static final int chunkSize = WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource;

    public static final String hashName = "MD5";

    File f;

    static ArrayList propertyChangeListeners = new ArrayList();

    public DigestManager() {
        for (int x = 0; x < propertyChangeListeners.size(); x++) {
            this.propertyChangeSupport.addPropertyChangeListener((PropertyChangeListener) propertyChangeListeners.get(x));
        }
    }

    static void addPropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.add(pcl);
    }

    static void removePropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeListeners.remove(pcl);
    }

    public PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private Object[] computeDigests(File f) throws Exception {
        this.f = f;
        FileInputStream fis = new FileInputStream(f);
        byte[] block;
        long index = 0;
        this.propertyChangeSupport.firePropertyChange("fileIndexingInProgress", "[ANts2k] " + f.getName(), new Integer(0));
        this.digests = new Object[(int) Math.ceil(f.length() / ((WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource) * 1.0))];
        int counter = 0;
        while (fis.available() > 0) {
            MessageDigest md = MessageDigest.getInstance(hashName);
            if (fis.available() >= WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource) block = new byte[(int) (WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource)]; else block = new byte[fis.available()];
            index += fis.read(block);
            md.update(block);
            this.digests[counter++] = md.digest();
            int progress = (int) Math.floor(((index * 1.0) / f.length()) * 100);
            this.propertyChangeSupport.firePropertyChange("fileIndexingInProgress", "[ANts2k] " + f.getName(), new Integer(progress));
            Thread.currentThread().sleep(100);
        }
        return this.digests;
    }

    public Object[] getDigests(File f) throws Exception {
        if (digests != null && this.f.equals(f)) return digests; else {
            this.computeDigests(f);
            for (int x = 0; x < propertyChangeListeners.size(); x++) {
                this.propertyChangeSupport.removePropertyChangeListener((PropertyChangeListener) propertyChangeListeners.get(x));
            }
            return digests;
        }
    }

    public String getDigest(File f) throws Exception {
        this.f = f;
        MessageDigest md = MessageDigest.getInstance(hashName);
        FileInputStream fis = new FileInputStream(f);
        byte[] block;
        while (fis.available() > 0) {
            if (fis.available() >= WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource) block = new byte[(int) (WarriorAnt.blockSizeInDownload * MultipleSourcesDownloadManager.blocksPerSource)]; else block = new byte[fis.available()];
            fis.read(block);
            md.update(block);
            Thread.currentThread().sleep(100);
        }
        return Base16.toHexString(md.digest());
    }

    public String getDigest(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance(hashName);
        md.update(s.getBytes());
        byte[] dig = md.digest();
        return Base16.toHexString(dig);
    }

    public String getDigest(Object[] hashes) throws Exception {
        MessageDigest md = MessageDigest.getInstance(hashName);
        for (int x = 0; x < hashes.length; x++) {
            md.update((byte[]) hashes[x]);
        }
        byte[] dig = md.digest();
        return Base16.toHexString(dig);
    }
}
