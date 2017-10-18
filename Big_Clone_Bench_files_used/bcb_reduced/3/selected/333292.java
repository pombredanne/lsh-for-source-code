package org.dcm4chee.xds.docstore.spi.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.management.ObjectName;
import org.dcm4chee.xds.common.XDSConstants;
import org.dcm4chee.xds.common.exception.XDSException;
import org.dcm4chee.xds.common.store.BasicXDSDocument;
import org.dcm4chee.xds.common.store.StoredDocument;
import org.dcm4chee.xds.common.store.XDSDocument;
import org.dcm4chee.xds.common.store.XDSDocumentIdentifier;
import org.dcm4chee.xds.common.store.XDSDocumentWriter;
import org.dcm4chee.xds.common.store.XDSDocumentWriterFactory;
import org.dcm4chee.xds.docstore.spi.Availability;
import org.dcm4chee.xds.docstore.spi.XDSDocumentStorage;
import org.jboss.system.server.ServerConfigLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XDSDocumentFileStorage implements XDSDocumentStorage {

    private static final String MIMEFILE_EXTENSION = ".mime";

    public static final String STORAGE_NAME = "XDSFileStorage";

    private static final String DEFAULT_BASE_DIR = "xds/repo/docs";

    private File baseDir;

    private Logger log = LoggerFactory.getLogger(XDSDocumentFileStorage.class);

    public XDSDocumentFileStorage() {
    }

    public void init(String initString) {
        setBaseDir(initString == null ? DEFAULT_BASE_DIR : initString);
        log.info("XDSDocumentFileStorage initialized! baseDir:" + baseDir);
    }

    public void setBaseDir(String dirName) {
        File dir = new File(dirName);
        if (dir.isAbsolute()) {
            baseDir = dir;
        } else {
            File serverHomeDir = ServerConfigLocator.locate().getServerHomeDir();
            baseDir = new File(serverHomeDir, dir.getPath());
        }
    }

    public boolean deleteDocument(XDSDocumentIdentifier xdsDoc) {
        File f = getDocumentFile(xdsDoc.getDocumentUID());
        if (f.exists()) {
            File mimeFile = new File(f.getAbsolutePath() + MIMEFILE_EXTENSION);
            mimeFile.delete();
            return f.delete();
        }
        return false;
    }

    public Availability getAvailabilty(String docUid) {
        File f = getDocumentFile(docUid);
        return f.exists() ? Availability.ONLINE : Availability.UNAVAILABLE;
    }

    public String getName() {
        return STORAGE_NAME;
    }

    public String getRetrieveURL(String docUid) {
        return null;
    }

    public ObjectName getMBeanServiceName() {
        return null;
    }

    public String getStorageType() {
        return "FILE";
    }

    public BasicXDSDocument retrieveDocument(String docUid) throws IOException {
        File f = getDocumentFile(docUid);
        String mime = readMime(f);
        if (f.exists()) {
            return new BasicXDSDocument(docUid, mime, XDSDocumentWriterFactory.getInstance().getDocumentWriter(f));
        } else {
            return null;
        }
    }

    public StoredDocument storeDocument(XDSDocument xdsDoc) throws XDSException {
        log.debug("#### Store Document  UID:" + xdsDoc.getDocumentUID());
        log.debug("#### Store Document mime:" + xdsDoc.getMimeType());
        log.debug("#### Store Document size:" + xdsDoc.getXdsDocWriter().size());
        File f = getDocumentFile(xdsDoc.getDocumentUID());
        log.debug("#### Document File:" + f);
        log.debug("#### Document File exist?:" + f.exists());
        try {
            byte[] digest = writeFile(f, xdsDoc.getXdsDocWriter());
            writeMime(f, xdsDoc.getMimeType());
            return digest == null ? null : new StoredDocument(xdsDoc.getDocumentUID(), xdsDoc.getXdsDocWriter().size(), digest, f.getAbsolutePath());
        } catch (Throwable x) {
            log.error("Storage of document failed:" + xdsDoc.getDocumentUID(), x);
            throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Storage of document failed:" + xdsDoc.getDocumentUID(), x);
        } finally {
            try {
                xdsDoc.getXdsDocWriter().close();
            } catch (IOException ignore) {
                log.warn("Error closing XDS Document Writer! Ignored", ignore);
            }
        }
    }

    private byte[] writeFile(File f, XDSDocumentWriter writer) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = null;
        DigestOutputStream dos = null;
        if (!f.exists()) {
            log.info("#### Write File:" + f);
            try {
                f.getParentFile().mkdirs();
                md = MessageDigest.getInstance("SHA1");
                FileOutputStream fos = new FileOutputStream(f);
                dos = new DigestOutputStream(fos, md);
                writer.writeTo(dos);
                log.info("#### File written:" + f + " exists:" + f.exists());
            } finally {
                if (dos != null) try {
                    dos.close();
                } catch (IOException ignore) {
                    log.error("Ignored error during close!", ignore);
                }
            }
        }
        return md == null ? null : md.digest();
    }

    private void writeMime(File f, String mimeType) throws IOException {
        FileOutputStream fos = null;
        try {
            File mimeFile = new File(f.getAbsolutePath() + MIMEFILE_EXTENSION);
            fos = new FileOutputStream(mimeFile);
            fos.write(mimeType.getBytes());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                    log.warn("Cant close FileOutputStream! ignored! reason:" + ignore);
                }
            }
        }
    }

    private String readMime(File f) throws IOException {
        FileInputStream fis = null;
        try {
            File mimeFile = new File(f.getAbsolutePath() + MIMEFILE_EXTENSION);
            fis = new FileInputStream(mimeFile);
            byte[] ba = new byte[fis.available()];
            fis.read(ba);
            return new String(ba);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignore) {
                    log.warn("Cant close FileInputStream! ignored! reason:" + ignore);
                }
            }
        }
    }

    private File getDocumentFile(String docUid) {
        String fn = docUid.replace('.', '_');
        return new File(baseDir, fn);
    }
}
