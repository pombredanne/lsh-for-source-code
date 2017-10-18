package org.dcm4chee.xds.store.mbean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.dcm4chee.docstore.Availability;
import org.dcm4chee.docstore.BaseDocument;
import org.dcm4chee.docstore.DataHandlerVO;
import org.dcm4chee.docstore.DocumentStore;
import org.dcm4chee.docstore.Feature;
import org.dcm4chee.docstore.spi.DocumentStorage;
import org.dcm4chee.xds.common.XDSConstants;
import org.dcm4chee.xds.common.exception.XDSException;
import org.dcm4chee.xds.common.store.CountingOutputStream;
import org.dcm4chee.xds.common.store.InputStreamDataSource;
import org.dcm4chee.xds.common.store.XDSDocument;
import org.dcm4chee.xds.common.store.XDSDocumentWriter;
import org.dcm4chee.xds.common.store.XDSDocumentWriterFactory;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.util.stream.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision: 5476 $ $Date: 2007-11-21 09:45:36 +0100 (Mi, 21 Nov 2007) $
 * @since Mar 11, 2008
 */
public class XDSStoreService extends ServiceMBeanSupport {

    private static final String MIME_METADATA = "application/metadata+xml";

    private static final int BUFFER_SIZE = 65535;

    private static final String NONE = "NONE";

    private DocumentStore docStore;

    private String storeBeforeRegisterPool;

    private String storeAfterRegisterPool;

    private Logger log = LoggerFactory.getLogger(XDSStoreService.class);

    private byte[] buf = new byte[65535];

    private boolean storeMetadata;

    private boolean ignoreMetadataPersistenceErrors;

    private boolean forceMetadataPersistence;

    private String metadataStoragePool;

    XDSDocumentWriterFactory dwFac = XDSDocumentWriterFactory.getInstance();

    public XDSStoreService() {
    }

    public String getStoreBeforeRegisterPool() {
        return storeBeforeRegisterPool;
    }

    public void setStoreBeforeRegisterPool(String poolName) {
        storeBeforeRegisterPool = poolName.trim();
    }

    public String getStoreAfterRegisterPool() {
        return storeAfterRegisterPool;
    }

    public void setStoreAfterRegisterPool(String storeAfterRegisterPool) {
        this.storeAfterRegisterPool = storeAfterRegisterPool.trim();
    }

    public boolean isStoreMetadata() {
        return storeMetadata;
    }

    public void setStoreMetadata(boolean storeMetadata) {
        this.storeMetadata = storeMetadata;
    }

    public String getMetadataStoragePool() {
        return metadataStoragePool == null ? NONE : metadataStoragePool;
    }

    public void setMetadataStoragePool(String pool) {
        this.metadataStoragePool = NONE.equals(pool) ? null : pool.trim();
    }

    public boolean isIgnoreMetadataPersistenceErrors() {
        return ignoreMetadataPersistenceErrors;
    }

    public void setIgnoreMetadataPersistenceErrors(boolean ignoreMetadataPersistenceErrors) {
        this.ignoreMetadataPersistenceErrors = ignoreMetadataPersistenceErrors;
    }

    public boolean isForceMetadataPersistence() {
        return forceMetadataPersistence;
    }

    public void setForceMetadataPersistence(boolean forceMetadataPersistence) {
        this.forceMetadataPersistence = forceMetadataPersistence;
    }

    public XDSDocument storeDocument(XDSDocument xdsDoc) throws XDSException {
        log.info("#### Store Document:" + xdsDoc.getDocumentUID() + " without metadata");
        return storeDocument(xdsDoc, null);
    }

    /**
     * Store given document with optional metadata.
     * <p>
     * <dl><dt>Return XDSDocument with status:</dt>
     * <dd> CREATED: If document is stored. </dd>
     * <dd> STORED: If document already exists (doc with same UID and identical hash exists)</dd>
     * </dl>
     * When Document with same UID already exists but hash doesn't match an Exception
     * will be thrown for 'XDSNonIdenticalHash'!
     * 
     * @param xdsDoc
     * @param metadata
     * @return
     * @throws XDSException
     */
    public XDSDocument storeDocument(XDSDocument xdsDoc, Source metadata) throws XDSException {
        String documentUID = xdsDoc.getDocumentUID();
        Availability avail = docStore.getAvailability(documentUID);
        if (avail.compareTo(Availability.NONEEXISTENT) < 0) {
            if (log.isDebugEnabled()) log.info("Document " + documentUID + " already exists with availability " + avail);
            return checkIdenticalHash(xdsDoc, documentUID);
        }
        log.info("#### Store Document:" + documentUID + " to pool " + storeBeforeRegisterPool + "\nmetadata:" + metadata);
        boolean error = false;
        boolean docAdded = false;
        try {
            XDSDocument storedDoc = null;
            BaseDocument doc = docStore.createDocument(storeBeforeRegisterPool, xdsDoc.getDocumentUID(), xdsDoc.getMimeType());
            if (doc != null) {
                docAdded = true;
                storedDoc = writeDocument(doc, xdsDoc.getXdsDocWriter());
                if (storeMetadata && metadata != null) {
                    createMetadataDoc(metadata, doc);
                }
            } else {
                log.debug("DocumentStorage does not support createDocument! Use storeDocument with DataHandler and trust to get SHA1 hash!");
                storedDoc = storeDoc(documentUID, xdsDoc, metadata);
                if (storedDoc != null) {
                    docAdded = true;
                }
            }
            return storedDoc.setStatus(XDSDocument.CREATED);
        } catch (Throwable x) {
            log.error("Storage of document failed:" + documentUID, x);
            error = true;
            throw (x instanceof XDSException) ? (XDSException) x : new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Storage of document failed:" + documentUID, x);
        } finally {
            try {
                xdsDoc.getXdsDocWriter().close();
            } catch (IOException ignore) {
                log.warn("Error closing XDS Document Writer! Ignored", ignore);
            }
            if (error && docAdded) {
                docStore.deleteDocument(documentUID);
            }
        }
    }

    private XDSDocument checkIdenticalHash(XDSDocument xdsDoc, String documentUID) throws XDSException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            DigestOutputStream dos = new DigestOutputStream(NullOutputStream.STREAM, md);
            xdsDoc.getXdsDocWriter().writeTo(dos);
            String newHash = DocumentStore.toHexString(md.digest());
            BaseDocument doc = docStore.getDocument(documentUID, xdsDoc.getMimeType());
            if (log.isDebugEnabled()) {
                log.debug("checkIdenticalHash verification:\nstored  :" + doc.getHash() + "\nreceived:" + newHash);
            }
            if (!newHash.equals(doc.getHash())) {
                throw new XDSException(XDSConstants.XDS_ERR_NON_IDENTICAL_HASH, "Document " + documentUID + " already exists with non identical hash value!", null);
            }
            return new XDSDocument(doc.getDocumentUID(), doc.getMimeType(), getXdsDocWriter(doc), doc.getHash(), null).setStatus(XDSDocument.STORED);
        } catch (XDSException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cant check hash!", e);
            throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Document " + documentUID + " already exists! Failed to check HASH value!", e);
        }
    }

    private XDSDocument storeDoc(String documentUID, XDSDocument xdsDoc, Source metadata) throws IOException, XDSException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Set<DataHandlerVO> dhVOs = new HashSet<DataHandlerVO>(2);
        DataHandler dh = xdsDoc.getXdsDocWriter().getDataHandler();
        if (!xdsDoc.getMimeType().equals(dh.getContentType())) {
            dh = this.correctContentType(dh, xdsDoc.getMimeType());
        }
        dhVOs.add(new DataHandlerVO(documentUID, dh));
        if (metadata != null && this.storeMetadata && metadataStoragePool != null) {
            DataSource ds = toDataSource(metadata);
            dhVOs.add(new DataHandlerVO(documentUID, new DataHandler(ds)));
        }
        BaseDocument[] docs = docStore.storeDocuments(storeBeforeRegisterPool, dhVOs);
        BaseDocument doc = null;
        if (docs.length == 1) {
            doc = docs[0];
            if (metadata != null && this.storeMetadata) {
                DataSource ds = toDataSource(metadata);
                dhVOs.clear();
                dhVOs.add(new DataHandlerVO(documentUID, new DataHandler(ds)));
                docs = docStore.storeDocuments(metadataStoragePool, dhVOs);
            }
        } else {
            String mime = xdsDoc.getMimeType();
            for (BaseDocument doc1 : docs) {
                if (mime.equals(doc1.getMimeType())) {
                    doc = doc1;
                    break;
                }
            }
        }
        XDSDocument storedDoc = null;
        if (doc != null) {
            if (doc.getHash() == null) {
                throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "SHA1 hash value missing! Storage does not support SHA1 hash! docUID:" + documentUID, null);
            }
            if (doc.getDataHandler() != null) {
                storedDoc = new XDSDocument(doc.getDocumentUID(), doc.getMimeType(), getXdsDocWriter(doc), doc.getHash(), null);
            } else {
                storedDoc = new XDSDocument(doc.getDocumentUID(), doc.getMimeType(), doc.getSize(), doc.getHash(), "StoredDocument(no content provider)");
            }
        }
        return storedDoc;
    }

    private DataHandler correctContentType(DataHandler dh, String mime) throws IOException {
        DataSource ds = dh.getDataSource();
        if (ds != null) {
            return new DataHandler(new InputStreamDataSource(ds.getInputStream(), mime));
        }
        throw new IllegalArgumentException("Can't correct ContentType! DataHandler has no DataSource!");
    }

    private XDSDocument writeDocument(BaseDocument doc, XDSDocumentWriter xdsDocWriter) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = null;
        DigestOutputStream dos = null;
        OutputStream out = doc.getOutputStream();
        CountingOutputStream counting = new CountingOutputStream(out);
        if (out != null) {
            log.info("#### Write Document:" + doc.getDocumentUID());
            try {
                md = MessageDigest.getInstance("SHA1");
                dos = new DigestOutputStream(counting, md);
                xdsDocWriter.writeTo(dos);
                log.info("#### File written:" + doc.getDocumentUID());
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException ignore) {
                        log.error("Ignored error during close!", ignore);
                    }
                }
            }
        }
        if (md != null) {
            String hash = DocumentStore.toHexString(md.digest());
            doc.getStorage().setHash(doc, hash);
            return new XDSDocument(doc.getDocumentUID(), doc.getMimeType(), dwFac.getDocumentWriter(doc.getDataHandler(), counting.getCount()), hash, null);
        } else {
            return null;
        }
    }

    private XDSDocumentWriter getXdsDocWriter(BaseDocument doc) throws IOException {
        DataHandler dh = doc.getDataHandler();
        if (dh == null) {
            return dwFac.getDocumentWriter(doc.getSize(), doc.getMimeType());
        } else {
            return dwFac.getDocumentWriter(dh, doc.getSize());
        }
    }

    public XDSDocument retrieveDocument(String docUid, String mime) throws IOException {
        log.info("#### Retrieve Document from storage:" + docUid);
        BaseDocument doc = docStore.getDocument(docUid, mime);
        return doc != null && doc.getAvailability().compareTo(Availability.UNAVAILABLE) < 0 ? new XDSDocument(docUid, doc.getMimeType(), getXdsDocWriter(doc)).setStatus(XDSDocument.STORED) : null;
    }

    /**
     * Return true if availability of document in docstore is better than UNAVAILABLE
     * (i.e. not UNAVAILABLE or NONEEXISTENT
     * 
     * @param docUid
     * @param mime
     * @return
     */
    public boolean documentExists(String docUid, String mime) {
        log.info("#### Document Exists?:" + docUid);
        return docStore.getAvailability(docUid).compareTo(Availability.UNAVAILABLE) < 0;
    }

    public boolean commitDocuments(Collection<XDSDocument> documents) {
        log.info("#### Commit Documents:" + documents);
        if (documents == null || documents.size() < 1) return true;
        boolean success = true;
        for (XDSDocument doc : documents) {
            log.debug("commit XDSDocument:" + doc);
            if (doc.getStatus() == XDSDocument.CREATED) {
                success = success & docStore.commitDocument(storeBeforeRegisterPool, doc.getDocumentUID());
                doc.setStatus(XDSDocument.STORED);
            }
        }
        return success;
    }

    public boolean rollbackDocuments(Collection<XDSDocument> documents) {
        log.info("#### Rollback Documents:" + documents);
        if (documents == null || documents.size() < 1) return true;
        boolean success = true;
        for (XDSDocument doc : documents) {
            if (doc.getStatus() != XDSDocument.STORED) {
                log.info("Delete XDSDocument:" + doc);
                success = success & docStore.deleteDocument(storeBeforeRegisterPool, doc.getDocumentUID());
            } else {
                log.info("Ignore Deletion of already existing XDSDocument:" + doc);
            }
        }
        return success;
    }

    public String computeHash(String filename) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = new FileInputStream(new File(filename));
        MessageDigest md = MessageDigest.getInstance("SHA1");
        DigestInputStream dis = new DigestInputStream(fis, md);
        while (dis.read(buf) != -1) ;
        String hash = DocumentStore.toHexString(md.digest());
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
        long size = 0;
        while ((len = fis.read(buf)) > 0) {
            dos.write(buf, 0, len);
            size += len;
        }
        String hash = DocumentStore.toHexString(md.digest());
        dos.close();
        if (log.isDebugEnabled()) log.debug("SHA1 write digest (alg:" + alg + "):" + hash);
        return hash;
    }

    /**
     * Store Metadata of an document (XDS metadata) as an extra document with mime type 'application/metadata+xml'
     * @param metadata
     * @param baseDocumetStorage 
     * @throws XDSException 
     */
    private void createMetadataDoc(Source metadata, BaseDocument doc) throws XDSException {
        DocumentStorage storage = getMetadataStorage(doc);
        if (storage != null) {
            BaseDocument metadataDoc = null;
            OutputStream out = null;
            try {
                metadataDoc = storage.createDocument(doc.getDocumentUID(), MIME_METADATA);
                if (metadataDoc != null) {
                    out = metadataDoc.getOutputStream();
                    if (out != null) {
                        writeXML(metadata, out);
                    }
                } else {
                    DataSource ds = toDataSource(metadata);
                    metadataDoc = storage.storeDocument(doc.getDocumentUID(), new DataHandler(ds));
                }
            } catch (Throwable x) {
                if (ignoreMetadataPersistenceErrors) {
                    log.debug("Storage of XDS Metadata failed!", x);
                    return;
                } else {
                    log.error("Storage of XDS Metadata failed!", x);
                    throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Storage of XDS Metadata failed!", x);
                }
            } finally {
                if (out != null) try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        } else if (forceMetadataPersistence) {
            log.error("Storage of XDS Metadata is not supported!");
            throw new XDSException(XDSConstants.XDS_ERR_REPOSITORY_ERROR, "Storage of XDS Metadata is not supported!", null);
        } else {
            log.debug("Storage of XDS Metadata is not supported!");
            return;
        }
    }

    private DataSource toDataSource(Source metadata) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
        writeXML(metadata, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        DataSource ds = new ByteArrayDataSource(bos.toByteArray(), MIME_METADATA);
        return ds;
    }

    private void writeXML(Source metadata, OutputStream out) throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        StreamResult result = new StreamResult(out);
        TransformerHandler th = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();
        th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        th.getTransformer().transform(metadata, result);
    }

    private DocumentStorage getMetadataStorage(BaseDocument doc) {
        if (log.isDebugEnabled()) log.debug("metadataStoragePool:" + metadataStoragePool);
        if (metadataStoragePool != null) {
            return docStore.getDocStorageFromPool(metadataStoragePool);
        } else {
            DocumentStorage storage = doc.getStorage();
            if (log.isDebugEnabled()) log.debug("storage.hasFeature(Feature.MULTI_MIME):" + storage.hasFeature(Feature.MULTI_MIME));
            return storage.hasFeature(Feature.MULTI_MIME) ? storage : null;
        }
    }

    protected void startService() throws Exception {
        docStore = DocumentStore.getInstance("XDSStoreService", "XDS");
    }
}
