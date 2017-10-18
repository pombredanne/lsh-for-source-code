package com.entelience.provider.audit;

import org.apache.log4j.Logger;
import com.entelience.sql.Db;
import com.entelience.sql.IdHelper;
import com.entelience.sql.DbHelper;
import com.entelience.util.Logs;
import com.entelience.util.DateHelper;
import com.entelience.mail.MailHelper;
import com.entelience.objects.audit.Document;
import com.entelience.objects.audit.DocumentMeta;
import com.entelience.objects.audit.DocumentHistory;
import com.entelience.objects.audit.AuditDocumentId;
import com.entelience.objects.audit.AuditId;
import com.entelience.objects.audit.AuditRecId;
import com.entelience.objects.audit.Recommendation;
import com.entelience.raci.audit.RaciDocument;
import com.entelience.raci.audit.RaciAudit;
import com.entelience.raci.audit.RaciRec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

/**
 * create, update, list, delete documents for audit module
 *
 */
public class DbDocument {

    private DbDocument() {
    }

    protected static final Logger _logger = Logs.getLogger();

    /**
     * check that the rec exists, is not closed, and is linked to the audit
     *
     *
     */
    protected static void checkRecValidForAudit(Db db, AuditRecId recId, AuditId audId) throws Exception {
        try {
            db.enter();
            Recommendation rec = DbRecommendation.getRecommendation(db, recId);
            if (rec == null) throw new IllegalArgumentException("Could not find the recommendation " + recId);
            if (rec.getRec_status_id() > 1) throw new IllegalArgumentException("The recommendation " + recId + " is closed");
            AuditId aId = DbRecommendation.getAuditIdForRecId(db, recId);
            if (aId == null) throw new IllegalArgumentException("Could not find the audit for recommendation " + recId);
            if (aId.getId() != audId.getId()) throw new IllegalArgumentException("The recommendation audit (" + aId + ") does not match the provided audit (" + audId + ")");
        } finally {
            db.exit();
        }
    }

    /**
     * create a document for an audit, and return its unique id.
     * @param db     a Db object that encapsulate a database connection.
     * @param doc the document to create
     * @return an AuditDocumentId object representing the newly created document
     * @throws IllegalArgumentException if the owner is not assignable, if the audit does not exist
     * @see com.entelience.provider.audit.DbAudit#checkCorrectAudit
     * @see com.entelience.provider.audit.DbAudit#checkCorrectOwner
     */
    public static AuditDocumentId addDocument(Db db, Document doc, DocumentMeta meta, byte[] content, int currentUser) throws Exception {
        try {
            db.enter();
            DbAudit.checkCorrectAudit(db, doc.getAuditId());
            DbAudit.checkCorrectConfidentialityId(db, doc.getConfidentialityId());
            DbAudit.checkNonClosedAudit(db, doc.getAuditId());
            if (doc.getAuditRecId() != null) {
                checkRecValidForAudit(db, doc.getAuditRecId(), doc.getAuditId());
            }
            checkCorrectDocumentTypeId(db, doc.getTypeId());
            PreparedStatement pst = db.prepareStatement("INSERT INTO audit.e_audit_document (e_audit_id, creation_date, title, description, reference, e_audit_rec_id, e_confidentiality_id, verified, e_audit_document_type_id) VALUES (?, current_timestamp, ?, ?, ?, ?, ?, ?, ?)");
            pst.setInt(1, doc.getAuditId().getId());
            pst.setString(2, DbHelper.nullify(doc.getTitle()));
            pst.setString(3, DbHelper.nullify(doc.getDescription()));
            pst.setString(4, DbHelper.nullify(doc.getReference()));
            if (doc.getAuditRecId() == null) pst.setObject(5, null); else pst.setInt(5, doc.getAuditRecId().getId());
            pst.setInt(6, doc.getConfidentialityId());
            pst.setBoolean(7, doc.isVerified());
            pst.setInt(8, doc.getTypeId());
            if (db.executeUpdate(pst) != 1) {
                throw new Exception("Error during creation of an interview");
            }
            PreparedStatement pstId = db.prepareStatement("SELECT e_audit_document_id, obj_ser FROM audit.e_audit_document WHERE e_audit_document_id = currval('audit.e_audit_document_serial')");
            AuditDocumentId id = IdHelper.getAuditDocumentId(pstId);
            RaciDocument rd;
            if (doc.getAuditRecId() == null) rd = new RaciDocument(new RaciAudit(db, doc.getAuditId())); else rd = new RaciDocument(new RaciRec(db, doc.getAuditRecId()));
            rd.documentCreated(db, id.getId(), currentUser);
            MailHelper.automaticMessageOnObjectCreation(db, currentUser, rd.getRaciObjectId());
            Integer contentId = null;
            if (content != null && content.length > 0 && meta != null) contentId = Integer.valueOf(addDocumentContent(db, content, meta, id));
            addDocumentHistory(db, id, contentId, currentUser);
            return id;
        } finally {
            db.exit();
        }
    }

    public static List<DocumentHistory> getDocumentHistory(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT h.change_date, ph.user_name, h.responsible, pr.user_name, h.title, h.e_audit_document_content_id, h.revision_number FROM audit.e_audit_document_history h INNER JOIN e_people ph ON ph.e_people_id = h.modifier INNER JOIN e_people pr ON pr.e_people_id = h.responsible WHERE h.e_audit_document_id = ? ORDER BY 1 DESC");
            pst.setInt(1, docId.getId());
            ResultSet rs = db.executeQuery(pst);
            List<DocumentHistory> l = new ArrayList<DocumentHistory>();
            DocumentHistory dh;
            if (rs.next()) {
                do {
                    dh = new DocumentHistory();
                    dh.setModificationDate(DateHelper.toDate(rs.getTimestamp(1)));
                    dh.setModifierName(rs.getString(2));
                    dh.setResponsible(rs.getInt(3));
                    dh.setResponsibleName(rs.getString(4));
                    dh.setTitle(rs.getString(5));
                    dh.setContentId((Integer) rs.getObject(6));
                    dh.setRevisionNumber((Integer) rs.getObject(7));
                    l.add(dh);
                } while (rs.next());
            }
            return l;
        } finally {
            db.exit();
        }
    }

    /**
     * add an history line if the document description or the content changed
     * 
     *
     */
    public static boolean addDocumentHistory(Db db, AuditDocumentId docId, Integer newContentId, int modifier) throws Exception {
        try {
            db.enter();
            List<DocumentHistory> h = getDocumentHistory(db, docId);
            DocumentHistory lastHistory = null;
            if (h != null && h.size() > 0) lastHistory = h.get(0);
            Document doc = getDocumentDescription(db, docId);
            if (lastHistory != null && DbAudit.compareObjects(doc.getTitle(), lastHistory.getTitle()) && DbAudit.compareObjects(doc.getOwnerId(), lastHistory.getResponsible()) && (newContentId == null || DbAudit.compareObjects(newContentId, lastHistory.getContentId()))) {
                _logger.debug("duplicate history line not inserted");
                return false;
            }
            int revision = 0;
            if (lastHistory != null && lastHistory.getRevisionNumber() != null) revision = lastHistory.getRevisionNumber().intValue();
            if (newContentId != null) revision++;
            Integer lastContentId = (lastHistory == null) ? null : lastHistory.getContentId();
            PreparedStatement pst = db.prepareStatement("INSERT INTO audit.e_audit_document_history (modifier, e_audit_document_id, responsible, title, e_audit_document_content_id, revision_number) SELECT ?, e_audit_document_id, owner, title, ?, ? FROM v_raci_audit_document WHERE e_audit_document_id = ?");
            pst.setInt(1, modifier);
            pst.setObject(2, (newContentId == null) ? lastContentId : newContentId);
            pst.setObject(3, (revision == 0) ? null : Integer.valueOf(revision));
            pst.setInt(4, docId.getId());
            int res = db.executeUpdate(pst);
            if (res != 1) throw new Exception("Error when adding a document history");
            return true;
        } finally {
            db.exit();
        }
    }

    private static int addDocumentContent(Db db, byte[] content, DocumentMeta meta, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            String computedChecksum = computeChecksum(content);
            if (meta.getChecksum() == null) {
                _logger.warn("No checksum provided by the frontend for an audit document.");
            } else {
                if (meta.getChecksum().equals(computedChecksum)) {
                    _logger.debug("The checksums provided by the frontend and computed match");
                } else {
                    throw new IllegalArgumentException("The checksums computed and provided do not match. ");
                }
            }
            PreparedStatement pstIns = db.prepareStatement("INSERT INTO audit.e_audit_document_content (e_audit_document_id, checksum, content, file_name, mime_type) VALUES (?, ?, ?, ?, ?)");
            pstIns.setInt(1, docId.getId());
            pstIns.setString(2, computedChecksum);
            ByteArrayInputStream is = new ByteArrayInputStream(content);
            pstIns.setBinaryStream(3, is, content.length);
            pstIns.setString(4, meta.getFileName());
            pstIns.setString(5, meta.getMimeType());
            int res = db.executeUpdate(pstIns);
            if (res != 1) throw new Exception("Error during creation of a document content");
            PreparedStatement pst = db.prepareStatement("SELECT currval('audit.e_audit_document_content_serial')");
            return DbHelper.getIntKey(pst);
        } finally {
            db.exit();
        }
    }

    /**
     * checksum is SHA
     *
     */
    private static String computeChecksum(byte[] toCompute) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(toCompute);
        java.math.BigInteger hash = new java.math.BigInteger(1, md.digest());
        return hash.toString(16);
    }

    /**
     * export the last version of a document content in the provided outputstream
     *
     */
    public static void exportDocumentContent(Db db, AuditDocumentId docId, OutputStream os) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT e_audit_document_content_id FROM audit.e_audit_document_content WHERE e_audit_document_id = ? AND change_date = (SELECT MAX (change_date) FROM audit.e_audit_document_content WHERE e_audit_document_id= ?)");
            pst.setInt(1, docId.getId());
            pst.setInt(2, docId.getId());
            exportDocumentContent(db, docId, DbHelper.getIntKey(pst), os);
        } finally {
            db.exit();
        }
    }

    /**
     * export the content of a document specifying its version in the provided outputstream
     *
     */
    public static void exportDocumentContent(Db db, AuditDocumentId docId, int auditDocumentContentId, OutputStream out) throws Exception {
        InputStream is = null;
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT content FROM audit.e_audit_document_content WHERE e_audit_document_content_id = ? AND e_audit_document_id = ?");
            pst.setInt(1, auditDocumentContentId);
            pst.setInt(2, docId.getId());
            ResultSet rs = db.executeQuery(pst);
            if (rs.next()) {
                is = rs.getBinaryStream(1);
                byte[] buffer = new byte[com.entelience.util.StaticConfig.ioBufferSize];
                int length = 0;
                while ((length = is.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            } else {
                _logger.debug("Document content id not found");
            }
        } finally {
            db.exit();
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    _logger.warn("Caught exception for is.close()", e);
                } finally {
                    is = null;
                }
            }
        }
    }

    /**
     * get the last version of a document
     **/
    public static byte[] getDocumentContent(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT e_audit_document_content_id FROM audit.e_audit_document_content WHERE e_audit_document_id = ? AND change_date = (SELECT MAX (change_date) FROM audit.e_audit_document_content WHERE e_audit_document_id= ?)");
            pst.setInt(1, docId.getId());
            pst.setInt(2, docId.getId());
            return getDocumentContent(db, docId, DbHelper.getIntKey(pst));
        } finally {
            db.exit();
        }
    }

    /**
     * get the content of a document specifying its version
     *
     */
    public static byte[] getDocumentContent(Db db, AuditDocumentId docId, int auditDocumentContentId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            exportDocumentContent(db, docId, auditDocumentContentId, out);
            return out.toByteArray();
        } finally {
            db.exit();
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    _logger.warn("Caught exception for out.close()", e);
                } finally {
                    out = null;
                }
            }
        }
    }

    protected static List<Document> getDocumentListFromRs(ResultSet rs) throws Exception {
        List<Document> ret = new ArrayList<Document>();
        if (rs.next()) {
            do {
                Document d = new Document();
                d.setDocumentId(IdHelper.getAuditDocumentId(rs, 1, 2));
                d.setAuditId(IdHelper.getAuditId(rs, 3, 4));
                d.setE_raci_obj(Integer.valueOf(rs.getInt(5)));
                d.setCreationDate(DateHelper.toDateOrNull(rs.getTimestamp(6)));
                d.setTitle(rs.getString(7));
                d.setDescription(rs.getString(8));
                d.setOwner(rs.getString(9));
                d.setReference(rs.getString(10));
                d.setAuditRecId(IdHelper.getAuditRecId(rs, 11, 12));
                d.setConfidentialityId(rs.getInt(13));
                d.setConfidentiality(rs.getString(14));
                d.setLastModified(DateHelper.toDateOrNull(rs.getTimestamp(15)));
                d.setVerified(rs.getBoolean(16));
                d.setTypeId(rs.getInt(17));
                d.setType(rs.getString(18));
                d.setOwnerId(Integer.valueOf(rs.getInt(19)));
                d.setFileName(rs.getString(20));
                ret.add(d);
            } while (rs.next());
        }
        return ret;
    }

    /** 
     * get the meta infos of a document (file name, file type, ...)
     *
     */
    public static DocumentMeta getDocumentMeta(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT e_audit_document_content_id FROM audit.e_audit_document_content WHERE e_audit_document_id = ? AND change_date = (SELECT MAX (change_date) FROM audit.e_audit_document_content WHERE e_audit_document_id= ?)");
            pst.setInt(1, docId.getId());
            pst.setInt(2, docId.getId());
            return getDocumentMeta(db, docId, DbHelper.getIntKey(pst));
        } finally {
            db.exit();
        }
    }

    /** 
     * get the meta infos of a document specifying its version (content id)
     *
     */
    public static DocumentMeta getDocumentMeta(Db db, AuditDocumentId docId, int contentId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT c.e_audit_document_content_id, d.e_audit_document_id, d.obj_ser, c.checksum, c.file_name, c.mime_type, c.change_date FROM audit.e_audit_document_content c INNER JOIN audit.e_audit_document d ON d.e_audit_document_id = c.e_audit_document_id WHERE c.e_audit_document_id = ? AND c.e_audit_document_content_id = ?");
            pst.setInt(1, docId.getId());
            pst.setInt(2, contentId);
            ResultSet rs = db.executeQuery(pst);
            if (rs.next()) {
                DocumentMeta m = new DocumentMeta();
                m.setContentId(rs.getInt(1));
                m.setDocumentId(IdHelper.getAuditDocumentId(rs, 2, 3));
                m.setChecksum(rs.getString(4));
                m.setFileName(rs.getString(5));
                m.setMimeType(rs.getString(6));
                m.setUploadDate(DateHelper.toDate(rs.getTimestamp(7)));
                return m;
            } else {
                _logger.debug("No document meta found for document id " + docId);
                return null;
            }
        } finally {
            db.exit();
        }
    }

    /**
     * get a document's details
     * @param db     a Db object that encapsulate a database connection.
     * @param docId the document unique id
     * @param currentUser the current user
     * @return a Document object
     */
    public static Document getDocumentDescription(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT d.e_audit_document_id, d.obj_ser, a.e_audit_id, a.obj_ser, d.e_raci_obj, d.creation_date, d.title, d.description, p.display_name, d.reference, r.e_audit_rec_id, r.obj_ser, c.e_audit_confidentiality_id, c.confidentiality, d.obj_lm, d.verified, t.e_audit_document_type_id, t.document_type, d.owner, con.file_name FROM v_raci_audit_document d INNER JOIN audit.e_audit a ON a.e_audit_id = d.e_audit_id LEFT JOIN audit.e_audit_rec r ON r.e_audit_rec_id = d.e_audit_rec_id INNER JOIN audit.e_audit_confidentiality c ON c.e_audit_confidentiality_id = d.e_confidentiality_id INNER JOIN audit.e_audit_document_type t ON t.e_audit_document_type_id = d.e_audit_document_type_id INNER JOIN e_people p ON p.e_people_id = d.owner LEFT JOIN audit.e_audit_document_content con ON ( con.e_audit_document_id = d.e_audit_document_id AND con.change_date = (SELECT MAX(change_date) FROM audit.e_audit_document_content WHERE e_audit_document_id = con.e_audit_document_id) ) WHERE d.e_audit_document_id = ?");
            pst.setInt(1, docId.getId());
            List<Document> l = getDocumentListFromRs(db.executeQuery(pst));
            if (l != null && l.size() == 1) return l.get(0); else return null;
        } finally {
            db.exit();
        }
    }

    /**
     * getAuditDocuments return a List of Document object.
     * If the <b>my</b> mode is activated, it returns only documents whose owner is the user, or whose audit owner is the user.
     * @param db     a Db object that encapsulate a database connection.
     * @param auditId a AuditId to select documents only linked to a specific audit
     * @param auditRecId a recId to select documents only linked to a specific recommendation
     * @param myId a possibly null user id (from e_people table) to select the My mode for a user
     * @return a non null List object containing only Document objects
     */
    public static List<Document> getDocumentsDescriptions(Db db, AuditId auditId, AuditRecId auditRecId, Boolean myMode, int currentUser) throws Exception {
        try {
            db.enter();
            if (auditId == null && auditRecId == null) throw new IllegalArgumentException("You must provide a auditId or auditRecId to get documents");
            StringBuffer sql = new StringBuffer("SELECT d.e_audit_document_id, d.obj_ser, a.e_audit_id, a.obj_ser, d.e_raci_obj, d.creation_date, d.title, d.description, p.display_name, d.reference, r.e_audit_rec_id, r.obj_ser, c.e_audit_confidentiality_id, c.confidentiality, d.obj_lm, d.verified, t.e_audit_document_type_id, t.document_type, d.owner, con.file_name FROM v_raci_audit_document d INNER JOIN audit.e_audit a ON a.e_audit_id = d.e_audit_id LEFT JOIN audit.e_audit_rec r ON r.e_audit_rec_id = d.e_audit_rec_id INNER JOIN audit.e_audit_confidentiality c ON c.e_audit_confidentiality_id = d.e_confidentiality_id INNER JOIN audit.e_audit_document_type t ON t.e_audit_document_type_id = d.e_audit_document_type_id INNER JOIN e_people p ON p.e_people_id = d.owner LEFT JOIN audit.e_audit_document_content con ON ( con.e_audit_document_id = d.e_audit_document_id AND con.change_date = (SELECT MAX(change_date) FROM audit.e_audit_document_content WHERE e_audit_document_id = con.e_audit_document_id) ) WHERE NOT d.deleted ");
            if (auditId != null) sql.append(" AND a.e_audit_id = ? ");
            if (auditRecId != null) sql.append(" AND r.e_audit_rec_id = ? ");
            if (myMode != null) {
                sql.append(" AND d.e_raci_obj IN ( SELECT e_raci_obj FROM e_raci WHERE e_people_id = ?");
                if (myMode.booleanValue()) {
                    sql.append(" AND (r OR a)");
                } else {
                    sql.append(" AND (r OR a OR c OR i)");
                }
                sql.append(" ) ");
            }
            PreparedStatement pst = db.prepareStatement(sql.toString());
            int n = 1;
            if (auditId != null) pst.setInt(n++, auditId.getId());
            if (auditRecId != null) pst.setInt(n++, auditRecId.getId());
            if (myMode != null) {
                pst.setInt(n++, currentUser);
            }
            return getDocumentListFromRs(db.executeQuery(pst));
        } finally {
            db.exit();
        }
    }

    /**
     * update the different fields of a document.
     * @param db     a Db object that encapsulate a database connection.
     * @param doc the document to update
     * @return the updated AuditDocumentId object
     * @throws java.lang.IllegalArgumentException if the document does not exist, if the audit does not exist, if the owner is not assignable
     * @see com.entelience.provider.audit.DbAudit#checkCorrectAudit
     * @see com.entelience.provider.audit.DbAudit#checkCorrectOwner
     */
    public static AuditDocumentId updateDocument(Db db, Document doc, DocumentMeta meta, byte[] content, int currentUser) throws Exception {
        try {
            db.enter();
            DbAudit.checkCorrectAudit(db, doc.getAuditId());
            DbAudit.checkNonClosedAudit(db, doc.getAuditId());
            if (doc.getDocumentId() == null) {
                throw new IllegalArgumentException("Bad Document ID");
            }
            Document oldDoc = getDocumentDescription(db, doc.getDocumentId());
            if (oldDoc == null) {
                throw new IllegalArgumentException("Document non existing");
            }
            if (doc.getAuditRecId() != null) {
                checkRecValidForAudit(db, doc.getAuditRecId(), doc.getAuditId());
            }
            DbAudit.checkCorrectConfidentialityId(db, doc.getConfidentialityId());
            checkCorrectDocumentTypeId(db, doc.getTypeId());
            PreparedStatement pst = db.prepareStatement("UPDATE audit.e_audit_document SET title = ?, description = ?, reference = ?, e_confidentiality_id = ?, verified = ?, e_audit_document_type_id = ?, e_audit_rec_id = ? WHERE e_audit_document_id = ?");
            pst.setString(1, DbHelper.nullify(doc.getTitle()));
            pst.setString(2, DbHelper.nullify(doc.getDescription()));
            pst.setString(3, DbHelper.nullify(doc.getReference()));
            pst.setInt(4, doc.getConfidentialityId());
            pst.setBoolean(5, doc.isVerified());
            pst.setInt(6, doc.getTypeId());
            if (doc.getAuditRecId() == null) pst.setObject(7, null); else pst.setInt(7, doc.getAuditRecId().getId());
            pst.setInt(8, doc.getDocumentId().getId());
            if (db.executeUpdate(pst) != 1) {
                throw new Exception("Error during modification of a document");
            }
            Integer contentId = null;
            if (content != null && content.length > 0 && meta != null) {
                _logger.info("An file has been uploaded, new content is beeing added to the document " + doc.getDocumentId());
                contentId = Integer.valueOf(addDocumentContent(db, content, meta, doc.getDocumentId()));
            }
            if (addDocumentHistory(db, doc.getDocumentId(), contentId, currentUser)) {
                RaciDocument rd = new RaciDocument(db, doc.getDocumentId());
                MailHelper.automaticMessageOnObjectModification(db, currentUser, rd.getRaciObjectId());
            }
            PreparedStatement pst_id = db.prepareStatement("SELECT e_audit_document_id, obj_ser FROM audit.e_audit_document WHERE e_audit_document_id = ?");
            pst_id.setInt(1, doc.getDocumentId().getId());
            return IdHelper.getAuditDocumentId(pst_id);
        } finally {
            db.exit();
        }
    }

    /**
     * mark a document as deleted.
     * @param db     a Db object that encapsulate a database connection.
     * @param docId the document unique id
     * @return the updated AuditDocumentId object
     */
    public static AuditDocumentId deleteDocument(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            AuditId aId = getAuditForDoc(db, docId);
            DbAudit.checkNonClosedAudit(db, aId);
            PreparedStatement pst = db.prepareStatement("UPDATE audit.e_audit_document SET deleted = true WHERE e_audit_document_id = ?");
            pst.setInt(1, docId.getId());
            if (db.executeUpdate(pst) != 1) {
                throw new Exception("Error during deletion of document");
            }
            PreparedStatement pst_id = db.prepareStatement("SELECT e_audit_document_id, obj_ser FROM audit.e_audit_document WHERE e_audit_document_id = ?");
            pst_id.setInt(1, docId.getId());
            return IdHelper.getAuditDocumentId(pst_id);
        } finally {
            db.exit();
        }
    }

    /**
     * return the audit unique id containing this document.
     * @param db     a Db object that encapsulate a database connection.
     * @param docId the document unique id
     * @return a AuditId object
     * @see com.entelience.objects.audit.AuditActionId
     * @see com.entelience.objects.audit.AuditId
     */
    public static AuditId getAuditForDoc(Db db, AuditDocumentId docId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT e_audit_id, obj_ser FROM audit.e_audit_document WHERE e_audit_document_id = ?");
            pst.setInt(1, docId.getId());
            AuditId res = IdHelper.getAuditId(pst);
            if (res == null) {
                throw new Exception("Unknown Document id");
            }
            return res;
        } finally {
            db.exit();
        }
    }

    /**
     * check if a document type id is valid .
     * @param db     a Db object that encapsulate a database connection.
     * @param statusId the status id
     * @throws java.lang.IllegalArgumentException if the status does not exist
     */
    private static void checkCorrectDocumentTypeId(Db db, int typeId) throws Exception {
        try {
            db.enter();
            PreparedStatement pst = db.prepareStatement("SELECT e_audit_document_type_id FROM audit.e_audit_document_type WHERE e_audit_document_type_id = ?");
            pst.setInt(1, typeId);
            if (DbHelper.noRows(pst)) {
                _logger.error("illegal document type id :" + typeId);
                throw new IllegalArgumentException("Invalid document type");
            }
        } finally {
            db.exit();
        }
    }
}
