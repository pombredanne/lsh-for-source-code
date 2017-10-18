package org.bitdrive.jlan.impl;

import net.decasdev.dokan.ByHandleFileInformation;
import net.decasdev.dokan.Win32FindData;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileType;
import org.bitdrive.Main;
import org.bitdrive.Misc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileListNode extends FileInfo {

    protected static long defaultCreateTime = System.currentTimeMillis() / 1000;

    private byte[] hash = null;

    private String[] tags = new String[0];

    private String localPath;

    private ArrayList<FileListNode> children = new ArrayList<FileListNode>();

    private static Logger logger = Main.getLogger(LogTypes.LOG_FILESYS);

    private static final String separator = "\\";

    private void init(String fpath, String fname, long fsize, boolean folder) {
        this.setPath(fpath);
        this.setFileName(fname);
        this.setSize(fsize);
        this.setFolder(folder);
        if (fname != null) {
            if (fname.startsWith(".")) this.addFileAttribute(FileAttribute.Hidden);
        }
        this.addFileAttribute(FileAttribute.ReadOnly);
        this.setModifyDateTime(defaultCreateTime);
        this.setAccessDateTime(defaultCreateTime);
        this.setCreationDateTime(defaultCreateTime);
    }

    public FileListNode(File file) {
        String path;
        int namePos;
        namePos = file.getAbsolutePath().lastIndexOf(file.getName());
        path = file.getAbsolutePath().substring(0, namePos);
        init(path, file.getName(), file.length(), file.isDirectory());
        this.setLocalPath(path + File.separator + file.getName());
    }

    public FileListNode(String fpath, String fname, long fsize, boolean isFolder) {
        this.init(fpath, fname, fsize, isFolder);
    }

    public FileListNode() {
        init(null, null, 0, false);
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public ArrayList<FileListNode> getChildren() {
        return children;
    }

    public void addChild(FileListNode node) {
        this.children.add(node);
    }

    public void setChildren(ArrayList<FileListNode> children) {
        this.children = children;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isFolder() {
        return (this.getFileAttributes() & FileAttribute.Directory) != 0;
    }

    public void setFolder(boolean isFolder) {
        if (isFolder) {
            this.addFileAttribute(FileAttribute.Directory);
            this.setFileType(FileType.Directory);
        } else {
            this.addFileAttribute(FileAttribute.Normal);
            this.setFileType(FileType.RegularFile);
        }
    }

    public void addFileAttribute(int attr) {
        this.setFileAttributes(this.getFileAttributes() + attr);
    }

    private String[] splitPath(String path) {
        String[] parts;
        parts = path.split("\\" + separator);
        if (parts.length == 0) return new String[] { separator };
        parts[0] = separator;
        return parts;
    }

    private FileListNode search(String[] paths, int index) {
        if (index >= paths.length) return null;
        if (paths[index].equals(this.getFileName())) {
            index++;
            if (paths.length <= (index)) return this;
            for (FileListNode n : this.getChildren()) {
                FileListNode childNode;
                childNode = n.search(paths, index);
                if (childNode != null) return childNode;
            }
        }
        return null;
    }

    public FileListNode search(String searchPath) {
        return this.search(splitPath(searchPath), 0);
    }

    public void hash() {
        int read;
        byte[] data;
        MessageDigest digest;
        FileInputStream stream;
        if (this.isFolder()) return;
        try {
            data = new byte[1024 * 1024];
            digest = MessageDigest.getInstance("SHA-256");
            stream = new FileInputStream(this.getLocalPath());
            while ((read = stream.read(data)) != -1) {
                digest.update(data, 0, read);
            }
            this.setHash(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "FileListNode.HashNode: Failed to find hash instance");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "FileListNode.HashNode: File not found (" + this.getLocalPath() + ")");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "FileListNode.HashNode: Failed to hash file");
        }
    }

    /**
     * Calculates the sum of all the sumfiles (if folder)
     *
     * @return The size of the folder.
     */
    public long sum() {
        long sum = 0;
        if (this.isFolder()) {
            for (FileListNode node : this.getChildren()) {
                if (node == null) continue;
                sum += node.sum();
            }
        } else {
            sum += this.getSize();
        }
        return sum;
    }

    public ArrayList<FileListNode> toList() {
        ArrayList<FileListNode> list;
        list = new ArrayList<FileListNode>();
        list.add(this);
        toList(list);
        return list;
    }

    private void toList(ArrayList<FileListNode> list) {
        for (FileListNode node : this.getChildren()) {
            if (node == null) continue;
            list.add(node);
            node.toList(list);
        }
    }

    private Element toXml(Document document) {
        Element em;
        StringBuilder tags;
        tags = new StringBuilder();
        if (this.isFolder()) {
            em = document.createElement("folder");
            for (FileListNode n : this.getChildren()) {
                if (n == null) continue;
                em.appendChild(n.toXml(document));
            }
        } else {
            em = document.createElement("file");
            em.setAttribute("size", Long.toString(this.getSize()));
            em.setAttribute("hash", Misc.byteArrayToHexString(this.getHash()));
        }
        for (String s : this.getTags()) tags.append(s).append(";");
        em.setAttribute("name", this.getFileName());
        em.setAttribute("tags", tags.toString());
        return em;
    }

    public String toXmlString() throws Exception {
        StreamResult result;
        ByteArrayOutputStream stream;
        stream = new ByteArrayOutputStream();
        result = new StreamResult(stream);
        this.toXmlStream(result);
        return stream.toString();
    }

    public void toXmlFile(String filename) throws Exception {
        StreamResult result;
        FileOutputStream stream;
        stream = new FileOutputStream(filename);
        result = new StreamResult(stream);
        this.toXmlStream(result);
        stream.close();
    }

    private void toXmlStream(StreamResult result) throws Exception {
        DOMSource source;
        Document document;
        DocumentBuilderFactory documentBuilderFactory;
        DocumentBuilder documentBuilder;
        Transformer transformer;
        TransformerFactory transformerFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();
            document.appendChild(this.toXml(document));
            source = new DOMSource(document);
            transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "FileListNode.FileListToXmlFile: Failed to write XML, " + e.getMessage());
            throw new Exception("Failed to write XML, " + e.getMessage());
        } catch (TransformerException e) {
            logger.log(Level.SEVERE, "FileListNode.FileListToXmlFile: Failed to write XML, " + e.getMessage());
            throw new Exception("Failed to write XML, " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FileListNode.FileListToXmlFile: Failed to write XML, " + e.getMessage());
            throw new Exception("Failed to write XML, " + e.getMessage());
        }
    }

    private int getDokanFileAttr() {
        int fileAttribute = 0;
        fileAttribute |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NORMAL;
        fileAttribute |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_READONLY;
        if (this.getFileName().startsWith(".")) {
            fileAttribute |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_HIDDEN;
        }
        if (this.isFolder()) {
            fileAttribute |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_DIRECTORY;
        }
        return fileAttribute;
    }

    public ByHandleFileInformation toDokanFileInformation(int volumeSerialNumber) {
        return new ByHandleFileInformation(this.getDokanFileAttr(), this.getCreationDateTime(), this.getAccessDateTime(), this.getModifyDateTime(), volumeSerialNumber, this.getSize(), 1, 0);
    }

    public Win32FindData toDokanFindData() {
        return new Win32FindData(this.getDokanFileAttr(), this.getCreationDateTime(), this.getAccessDateTime(), this.getModifyDateTime(), this.getSize(), 0, 0, this.getFileName(), this.getFileName());
    }

    private static FileListNode fromXml(Element xmlNode) {
        NodeList nodes;
        FileListNode node;
        node = new FileListNode();
        if (xmlNode.getNodeName().equals("folder")) {
            node.setFolder(true);
            nodes = xmlNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    node.addChild(fromXml((Element) nodes.item(i)));
                }
            }
        } else {
            node.setFolder(false);
            node.setSize(Long.parseLong(xmlNode.getAttribute("size")));
            node.setHash(Misc.hexStringToByteArray(xmlNode.getAttribute("hash")));
        }
        node.setFileName(xmlNode.getAttribute("name"));
        node.setTags(xmlNode.getAttribute("tags").split(";"));
        return node;
    }

    public static FileListNode fromXmlString(String xmlStr) throws Exception {
        ByteArrayInputStream stream;
        stream = new ByteArrayInputStream(xmlStr.getBytes());
        return fromXml(stream);
    }

    public static FileListNode fromXmlFile(String filename) throws Exception {
        return fromXml(new FileInputStream(filename));
    }

    private static FileListNode fromXml(InputStream stream) throws Exception {
        Document document;
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(stream);
            document.getDocumentElement().normalize();
            if (document.getFirstChild().getNodeType() == Node.ELEMENT_NODE) {
                return fromXml((Element) document.getFirstChild());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FileListNode.fromXml: Failed to read XML, " + e.getMessage());
            throw new Exception("Failed to read XML, " + e.getMessage());
        }
        logger.log(Level.SEVERE, "FileListNode.fromXml: Failed to read XML, NodeType != ELEMENT_NODE");
        return null;
    }

    public static FileListNode fromFolderTree(String folderPath) {
        File file = new File(folderPath);
        if (!file.exists()) return null;
        FileListNode node;
        node = new FileListNode(file);
        node.setFileName(file.getName());
        node.setPath("");
        for (File f : file.listFiles()) {
            FileListNode child;
            child = fromFolderTree(f, "");
            if (child != null) node.addChild(child);
        }
        return node;
    }

    private static FileListNode fromFolderTree(File file, String path) {
        FileListNode node;
        if (!file.exists()) return null;
        if (file.isHidden()) return null;
        if (file.getName().startsWith(".")) return null;
        node = new FileListNode(file);
        node.setPath(path + separator + file.getName());
        if (!file.isDirectory()) return node;
        for (File f : file.listFiles()) {
            node.addChild(fromFolderTree(f, node.getPath()));
        }
        return node;
    }
}
