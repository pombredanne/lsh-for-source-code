package se.inera.ifv.medcert.core.entity;

import static org.apache.commons.codec.binary.Hex.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.bouncycastle.cms.CMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.inera.ifv.medcert.core.vo.QuestionFromFkValueObject;
import se.inera.ifv.medcert.core.vo.QuestionValueObject;
import se.inera.ifv.medcert.exception.InvalidStateException;
import se.vgregion.dao.domain.patterns.entity.AbstractEntity;

/**
 * @author Pär Wenåker
 *
 */
@NamedQueries({ @NamedQuery(name = "Certificate.findAllByPatientSsn", query = "select c from Certificate c where c.patientSsn = :patientSsn and c.careUnitId = :careUnitId order by c.createdAt desc") })
@Entity
@Table(name = "CERTIFICATE")
public class Certificate extends AbstractEntity<Certificate, String> {

    private static final Logger log = LoggerFactory.getLogger(Certificate.class);

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "CARE_UNIT_ID")
    private String careUnitId;

    @Column(name = "PATIENT_NAME")
    private String patientName;

    @Column(name = "PATIENT_SSN")
    private String patientSsn;

    @Column(name = "SIGNED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date signedAt;

    @Column(name = "SENT_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "ORIGIN")
    @Enumerated(EnumType.STRING)
    private CreatorOrigin origin;

    @Column(name = "DOCUMENT")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] document;

    @Column(name = "SIGNATURE")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] signature;

    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private State state = State.CREATED;

    @OneToMany(mappedBy = "certificate", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy(value = "sentAt desc")
    private Set<Question> questions = new HashSet<Question>();

    protected Certificate() {
    }

    /**
     * Constructor. Use {@see CertificateBuilder} to create Certificates.
     * 
     * @param id
     * @param fullName
     * @param ssn
     */
    protected Certificate(String id, String careUnitId, String patientName, String patientSsn, Date signedAt, CreatorOrigin origin) {
        this.id = id;
        this.careUnitId = careUnitId;
        this.patientName = patientName;
        this.patientSsn = patientSsn;
        this.signedAt = signedAt;
        this.createdAt = new Date();
        this.origin = origin;
    }

    Certificate addQuestion(Question question) {
        questions.add(question);
        return this;
    }

    /**
     * @return the questions
     */
    public Set<Question> getQuestions() {
        return Collections.unmodifiableSet(questions);
    }

    public String getId() {
        return id;
    }

    public String getCareUnitId() {
        return careUnitId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientSsn() {
        return patientSsn;
    }

    public byte[] getDocument() {
        return document;
    }

    public void setDocument(byte[] document) {
        this.document = document;
    }

    public Date getSignedAt() {
        return signedAt;
    }

    public CreatorOrigin getOrigin() {
        return origin;
    }

    public Question createQuestionFromCare(QuestionValueObject vo) {
        return QuestionFromCare.create(this, vo);
    }

    public Question createQuestionFromFk(QuestionFromFkValueObject vo) {
        return QuestionFromFk.create(this, vo);
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setOrigin(CreatorOrigin origin) {
        this.origin = origin;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public void setSignedAt(Date signedAt) {
        this.signedAt = signedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) throws InvalidStateException {
        isValidNextState(this.state, state);
        this.state = state;
    }

    /**
     * Validates if next state it's ok. 
     * @param oldState  The old state
     * @param newState  the new state
     * @throws InvalidStateException throws invalidstate exception
     */
    public void isValidNextState(State oldState, State newState) throws InvalidStateException {
        if (oldState.equals(State.CREATED) && !newState.equals(State.EDITED)) {
            throw new InvalidStateException("", "");
        }
        if (oldState.equals(State.EDITED) && !newState.equals(State.SIGNED)) {
            throw new InvalidStateException("", "");
        }
        if (oldState.equals(State.SIGNED) && !(newState.equals(State.SENT) || newState.equals(State.PRINTED))) {
            throw new InvalidStateException("", "");
        }
    }

    /**
     * Returns true if the currentstate is ok to print in draft. Only state
     * CREATED and EDITED is valid.
     * @param currentState      current state
     * @return true if currentstate is in CREATED or EDITED
     */
    public boolean isPrintInDraftValid(State currentState) {
        if (currentState.equals(State.CREATED) && currentState.equals(State.EDITED)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Certificate [id=" + id + ", signedAt=" + signedAt + "]";
    }

    public String getMd5Hash() throws NoSuchAlgorithmException {
        byte[] certBytes = this.getDocument();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(certBytes, 0, certBytes.length);
        md5hash = md.digest();
        return encodeHexString(md5hash);
    }

    public boolean isValid() {
        if (this.state != State.SIGNED && this.state != State.SIGNED_AND_SENT) {
            return true;
        }
        try {
            String signedData = new CertificateVerifier(this.signature).getSignedDataAsString();
            return getMd5Hash().equals(signedData);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to find algorithm to calculate MD5", e);
            return false;
        } catch (CMSException e) {
            log.error("Error handling certificate", e);
            return false;
        }
    }

    public void setSigned(byte[] signature) {
        this.state = State.SIGNED;
        this.signedAt = new Date();
        this.signature = signature;
    }

    /**
     * 
     */
    public void setSavedState() {
        if (this.state != State.CREATED && this.state != State.EDITED) {
            throw new IllegalAccessError("Cannot edit this document!");
        }
        this.state = State.EDITED;
    }

    /**
     * 
     */
    public void setStatusSent(Date sentTime) {
        this.setSentAt(sentTime);
        if (this.state == State.SIGNED) {
            this.state = State.SIGNED_AND_SENT;
        } else {
            this.state = State.SENT;
        }
    }

    /**
     * @return
     */
    public boolean isEditable() {
        return this.state == State.CREATED || state == State.EDITED;
    }
}
