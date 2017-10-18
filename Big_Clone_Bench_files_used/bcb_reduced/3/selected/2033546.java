package com.belmont.backup;

import java.io.*;
import java.security.MessageDigest;
import java.security.DigestException;
import java.util.Vector;
import java.util.zip.ZipEntry;

public class Node extends ZipEntry implements IBackupConstants {

    String nodeName;

    boolean marked;

    boolean isDir;

    String digest;

    long fsize;

    Vector<Node> children;

    public Node(String path) {
        super(path);
    }

    public Node(ZipEntry z, long fsize, String digest) {
        super(z);
        this.isDir = z.getName().endsWith("/");
        this.fsize = fsize;
        this.digest = digest;
    }

    public String getNodeName() {
        if (nodeName == null) {
            String n = getName();
            int l = n.length();
            if (n == null || l == 0) {
                nodeName = "";
            } else {
                int ind = n.lastIndexOf('/', (isDir) ? (l - 2) : l);
                if (ind == -1 || ind == 0) {
                    nodeName = n;
                } else {
                    nodeName = n.substring(ind + 1, (isDir) ? (l - 1) : l);
                }
            }
        }
        return nodeName;
    }

    public Node get(String name) {
        if (children == null) {
            return null;
        }
        int s = children.size();
        for (int i = 0; i < s; i++) {
            Node c = children.elementAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public Node(String path, boolean isDir, long size, long lastModified, String digest) {
        super(path);
        this.isDir = isDir;
        setSize(fsize = size);
        setTime(lastModified);
        this.digest = digest;
    }

    public void scan(Node old, File dir, MessageDigest md, FileStorage storage) throws IOException {
        File file = new File(dir, getName());
        this.fsize = file.length();
        setTime(file.lastModified());
        this.isDir = file.isDirectory();
        if (!isDir) {
            if (old == null || old.getFileSize() != fsize || old.getTime() != getTime() || old.isDirectory()) {
                checksum(file, md);
            } else {
                this.digest = old.getDigest();
            }
            if (storage != null) {
                SFile sf = storage.getFileCreate(digest);
                if (sf != null) {
                    sf.addExternalFile(file.getAbsolutePath());
                }
            }
        }
    }

    public void mark() {
        this.marked = true;
    }

    public boolean isMarked() {
        return marked;
    }

    public boolean isDirectory() {
        return isDir;
    }

    public long getFileSize() {
        return fsize;
    }

    public String getDigest() {
        return digest;
    }

    public void add(Node child) {
        if (children == null) {
            children = new Vector<Node>();
        }
        children.addElement(child);
    }

    void checksum(File file, MessageDigest md) {
        try {
            FileInputStream in = new FileInputStream(file);
            byte buf[] = BufferPool.getInstance().get(1024);
            int len;
            try {
                while ((len = in.read(buf)) != -1) {
                    md.update(buf, 0, len);
                }
                byte digestBytes[] = md.digest();
                digest = Utils.formatDigest(digestBytes);
            } finally {
                BufferPool.getInstance().put(buf);
                in.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Will be used by JTree's that use this node to display the node
     * lable so toString only returns a suitable name for that purpose
     */
    public String toString() {
        return getNodeName();
    }
}
