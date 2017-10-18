package org.dcm4chex.xds.mbean.store;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;
import org.dcm4chex.xds.XDSDocumentMetadata;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 5474 $ $Date: 2007-11-21 09:41:28 +0100 (Mi, 21 Nov 2007) $
 * @since Mar 08, 2006
 */
public class StorageImpl implements Storage {

    private static final String DEFAULT_ROOT = "xds_repository";

    private static final int[] DEFAULT_DIRECTORY_TREE = new int[] { 307, 317, 331 };

    private static final int BUFFER_SIZE = 65535;

    private File rootDir;

    private int[] directoryTree;

    private static Logger log = Logger.getLogger(StorageImpl.class.getName());

    private static StorageImpl singleton;

    private StorageImpl() {
    }

    public static StorageImpl getInstance() {
        if (singleton == null) singleton = new StorageImpl();
        return singleton;
    }

    /**
	 * @return Returns the baseDir.
	 */
    public File getRootDir() {
        if (rootDir == null) {
            File serverHomeDir = ServerConfigLocator.locate().getServerHomeDir();
            rootDir = new File(serverHomeDir, DEFAULT_ROOT);
        }
        return rootDir;
    }

    /**
	 * @param baseDir The baseDir to set.
	 */
    public void setRootDir(File newRoot) {
        this.rootDir = newRoot;
    }

    /**
	 * @return Returns the directoryTree.
	 */
    public int[] getDirectoryTree() {
        if (directoryTree == null) {
            directoryTree = DEFAULT_DIRECTORY_TREE;
        }
        return directoryTree;
    }

    /**
	 * @param directoryTree The directoryTree to set.
	 */
    public void setDirectoryTree(int[] directoryTree) {
        this.directoryTree = directoryTree;
    }

    public StoredDocument store(String uid, AttachmentPart part, XDSDocumentMetadata metadata) throws IOException {
        File docFile = getDocFile(uid);
        if (docFile == null) return null;
        byte[] hash = null;
        try {
            hash = saveAttachment(part, docFile);
        } catch (Throwable t) {
            throw (IOException) new IOException("Store document (uid:" + uid + ") failed! Reason:" + t.getMessage()).initCause(t);
        }
        return new StoredDocumentAsFile(docFile, hash);
    }

    public File get(String uid) {
        return null;
    }

    private File getDocFile(String uid) throws IOException {
        File file = getRootDir();
        if (directoryTree != null) {
            file = new File(file, getSubDirName(uid));
        }
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("Cant create Directory:" + file + "(uid:" + uid + ")!");
                throw new IOException("Cant create directory " + file + " (uid:" + uid + ")!");
            }
        }
        return new File(file, uid);
    }

    private String getSubDirName(String uid) {
        if (getDirectoryTree() == null) return uid;
        int hash = uid.hashCode();
        StringBuffer sb = new StringBuffer();
        int modulo;
        for (int i = 0; i < directoryTree.length; i++) {
            if (directoryTree[i] == 0) {
                sb.append(Integer.toHexString(hash)).append(File.separatorChar);
            } else {
                modulo = hash % directoryTree[i];
                if (modulo < 0) {
                    modulo *= -1;
                }
                sb.append(modulo).append(File.separatorChar);
            }
        }
        return sb.toString();
    }

    private byte[] saveAttachment(AttachmentPart part, File docFile) throws SOAPException, IOException, MessagingException, NoSuchAlgorithmException {
        log.info("Save Attachment " + part.getContentId() + " (size=" + part.getSize() + ") to file " + docFile);
        Object content = part.getContent();
        BufferedOutputStream bos = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
            DigestOutputStream dos = new DigestOutputStream(new FileOutputStream(docFile), md);
            bos = new BufferedOutputStream(dos);
            if (content instanceof String) {
                dos.write(content.toString().getBytes());
            } else {
                if (content instanceof StreamSource) {
                    content = ((StreamSource) content).getInputStream();
                }
                if (content instanceof InputStream) {
                    InputStream is = (InputStream) content;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    for (int len; (len = is.read(buffer)) > 0; ) {
                        dos.write(buffer, 0, len);
                    }
                } else if (content instanceof MimeMultipart) {
                    MimeMultipart mmp = (MimeMultipart) content;
                    mmp.writeTo(dos);
                } else {
                    throw new IllegalArgumentException("Unknown content:" + content.getClass().getName() + " contentType:" + part.getContentType());
                }
            }
        } finally {
            if (bos != null) bos.close();
        }
        return md == null ? null : md.digest();
    }
}
