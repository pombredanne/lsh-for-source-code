package org.dcm4chee.xds.docstore.mbean;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.dcm4chee.xds.common.XDSConstants;
import org.dcm4chee.xds.common.exception.XDSException;
import org.dcm4chee.xds.common.store.BasicXDSDocument;
import org.dcm4chee.xds.common.store.StoredDocument;
import org.dcm4chee.xds.common.store.XDSDocument;
import org.dcm4chee.xds.common.store.XDSDocumentIdentifier;
import org.dcm4chee.xds.common.store.XDSDocumentWriter;
import org.dcm4chee.xds.docstore.spi.XDSDocumentStorage;
import org.dcm4chee.xds.docstore.spi.XDSDocumentStorageRegistry;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision: 5476 $ $Date: 2007-11-21 09:45:36 +0100 (Mi, 21 Nov 2007) $
 * @since Mar 11, 2008
 */
public class DocumentStoreService extends ServiceMBeanSupport {

    private static final char[] HEX_STRINGS = "0123456789abcdef".toCharArray();

    private XDSDocumentStorageRegistry storageRegistry;

    private XDSDocumentStorage documentStorageBeforeRegister;

    private Logger log = LoggerFactory.getLogger(DocumentStoreService.class);

    private byte[] buf = new byte[65535];

    public DocumentStoreService() {
        storageRegistry = new XDSDocumentStorageRegistry();
    }

    public String getDocumentStorageBeforeRegister() {
        if (documentStorageBeforeRegister == null) {
            setDocumentStorageBeforeRegister("XDSFileStorage");
        }
        return documentStorageBeforeRegister.getName();
    }

    public void setDocumentStorageBeforeRegister(String name) {
        XDSDocumentStorage docStore = storageRegistry.getXDSDocumentStorage(name);
        if (docStore == null) throw new IllegalArgumentException("Unknown XDSDocumentStorage! name:" + name);
        documentStorageBeforeRegister = docStore;
    }

    public StoredDocument storeDocument(XDSDocument xdsDoc) throws XDSException {
        log.info("#### Store Document:" + xdsDoc.getDocumentUID());
        if (documentStorageBeforeRegister == null) {
            throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Configuration error! No DocumentStorage set (DocumentStorageBeforeRegister)", null);
        }
        return documentStorageBeforeRegister.storeDocument(xdsDoc);
    }

    public BasicXDSDocument retrieveDocument(String docUid) throws IOException {
        log.info("#### Retrieve Document:" + docUid);
        return documentStorageBeforeRegister.retrieveDocument(docUid);
    }

    public boolean documentExists(String docUid) {
        log.info("#### Document Exists?:" + docUid);
        return false;
    }

    public boolean commitDocuments(Collection storedDocuments) {
        log.info("#### Commit Documents:" + storedDocuments);
        return true;
    }

    public boolean rollbackDocuments(Collection storedDocuments) {
        log.info("#### Rollback Documents:" + storedDocuments);
        if (storedDocuments == null || storedDocuments.size() < 1) return true;
        XDSDocumentIdentifier doc;
        boolean success = true;
        for (Iterator iter = storedDocuments.iterator(); iter.hasNext(); ) {
            doc = (StoredDocument) iter.next();
            log.debug("Delete XDSDocument:" + doc);
            success = success & documentStorageBeforeRegister.deleteDocument(doc);
        }
        return success;
    }

    public Set listDocumentStorageProvider() {
        return storageRegistry.getAllXDSDocumentStorageProviderNames();
    }

    public String computeHash(String filename) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = new FileInputStream(new File(filename));
        MessageDigest md = MessageDigest.getInstance("SHA1");
        DigestInputStream dis = new DigestInputStream(fis, md);
        while (dis.read(buf) != -1) ;
        String hash = toHexString(md.digest());
        log.info("SHA1 read digest:" + hash);
        return hash;
    }

    public String computeHash(String filename, String alg) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = new FileInputStream(new File(filename));
        if (alg == null || alg.trim().length() < 1) alg = "SHA1";
        MessageDigest md = MessageDigest.getInstance(alg);
        DigestOutputStream dos = new DigestOutputStream(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
            }
        }, md);
        int len;
        int size = 0;
        while ((len = fis.read(buf)) > 0) {
            dos.write(buf, 0, len);
            size += len;
        }
        String hash = toHexString(md.digest());
        dos.close();
        log.info("SHA1 write digest (alg:" + alg + "):" + hash);
        return hash;
    }

    private String toHexString(byte[] hash) {
        StringBuffer sb = new StringBuffer();
        int h;
        for (int i = 0; i < hash.length; i++) {
            h = hash[i] & 0xff;
            sb.append(HEX_STRINGS[h >>> 4]);
            sb.append(HEX_STRINGS[h & 0x0f]);
        }
        return sb.toString();
    }
}
