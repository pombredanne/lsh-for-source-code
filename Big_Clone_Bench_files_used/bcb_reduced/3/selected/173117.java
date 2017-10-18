package org.dcm4chex.archive.dcm.storescp;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.FutureRSP;
import org.dcm4che.net.PDU;
import org.dcm4che.net.PresContext;
import org.dcm4che.net.RoleSelection;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;
import org.dcm4chex.archive.ejb.jdbc.AECmd;
import org.dcm4chex.archive.ejb.jdbc.AEData;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 1119 $ $Date: 2004-05-21 05:50:27 -0400 (Fri, 21 May 2004) $
 * @since 01.03.2004
 */
class StgCmtCmd {

    private static final String[] NATIVE_LE_TS = { UIDs.ExplicitVRLittleEndian, UIDs.ImplicitVRLittleEndian };

    private static final DcmObjectFactory objFact = DcmObjectFactory.getInstance();

    private static final DcmParserFactory pFact = DcmParserFactory.getInstance();

    private static final AssociationFactory aFact = AssociationFactory.getInstance();

    private final StoreScpService service;

    private final String callingAET;

    private final AEData aeData;

    private final FileInfo[][] fileInfos;

    private final DcmElement refSOPSeq;

    private final Dataset eventInfo = objFact.newDataset();

    public StgCmtCmd(StoreScpService service, Association a, Dataset actionInfo) throws DcmServiceException {
        this.service = service;
        this.callingAET = a.getCalledAET();
        String uid = actionInfo.getString(Tags.TransactionUID);
        if (uid == null) {
            throw new DcmServiceException(Status.MissingAttributeValue, "Missing Transaction UID (0008,1195) in Action Information");
        }
        this.eventInfo.putUI(Tags.TransactionUID, uid);
        this.refSOPSeq = actionInfo.get(Tags.RefSOPSeq);
        if (refSOPSeq == null) {
            throw new DcmServiceException(Status.MissingAttributeValue, "Missing Referenced SOP Sequence (0008,1199) in Action Information");
        }
        try {
            this.aeData = new AECmd(service.getDataSource(), a.getCallingAET()).execute();
            if (aeData == null) {
                throw new DcmServiceException(Status.ProcessingFailure, "Failed to resolve AET:" + a.getCallingAET());
            }
            this.fileInfos = RetrieveCmd.create(service.getDataSource(), refSOPSeq).execute();
        } catch (SQLException e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    public void execute() {
        int failureReason = Status.ProcessingFailure;
        Storage storage = null;
        try {
            StorageHome home = (StorageHome) EJBHomeFactory.getFactory().lookup(StorageHome.class, StorageHome.JNDI_NAME);
            storage = home.create();
        } catch (Exception e) {
            service.getLog().error("Failed to access Storage EJB", e);
        }
        DcmElement successSOPSeq = eventInfo.putSQ(Tags.RefSOPSeq);
        DcmElement failedSOPSeq = eventInfo.putSQ(Tags.FailedSOPSeq);
        for (int i = 0, n = refSOPSeq.vm(); i < n; ++i) {
            Dataset refSOP = refSOPSeq.getItem(i);
            if (storage != null && (failureReason = commit(storage, refSOP)) == Status.Success) {
                successSOPSeq.addItem(refSOP);
            } else {
                refSOP.putUS(Tags.FailureReason, failureReason);
                failedSOPSeq.addItem(refSOP);
            }
        }
        if (failedSOPSeq.isEmpty()) {
            eventInfo.remove(Tags.FailedSOPSeq);
        }
        sendResult();
    }

    private int commit(Storage storage, Dataset refSOP) {
        final String iuid = refSOP.getString(Tags.RefSOPInstanceUID);
        final String cuid = refSOP.getString(Tags.RefSOPClassUID);
        FileInfo[] fileInfo = findByIUID(iuid);
        if (fileInfo == null) {
            service.getLog().warn("Failed Storage Commitment of Instance[uid=" + iuid + "]: no such object");
            return Status.NoSuchObjectInstance;
        }
        if (!fileInfo[0].sopCUID.equals(cuid)) {
            service.getLog().warn("Failed Storage Commitment of Instance[uid=" + iuid + "]: SOP Class in request[" + cuid + "] does not match SOP Class in stored object[" + fileInfo[0].sopCUID + "]");
            return Status.ClassInstanceConflict;
        }
        try {
            checkFile(fileInfo);
            storage.commit(fileInfo[0].sopIUID);
            return Status.Success;
        } catch (Exception e) {
            service.getLog().error("Failed Storage Commitment of Instance[uid=" + fileInfo[0].sopIUID + "]:", e);
            return Status.ProcessingFailure;
        }
    }

    private FileInfo[] findByIUID(String iuid) {
        for (int i = 0; i < fileInfos.length; i++) {
            if (fileInfos[i][0].sopIUID.equals(iuid)) {
                return fileInfos[i];
            }
        }
        return null;
    }

    private void checkFile(FileInfo[] fileInfo) throws IOException {
        Arrays.sort(fileInfo, FileInfo.DESC_ORDER);
        for (int i = 0; i < fileInfo.length; i++) {
            if (isLocal(fileInfo[i])) {
                checkFile(fileInfo[i]);
                return;
            }
        }
        throw new IOException("Instance not accessable from " + callingAET);
    }

    private boolean isLocal(FileInfo info) {
        if (info.fileRetrieveAETs == null) return false;
        Set aets = toHashSet(info.fileRetrieveAETs);
        aets.retainAll(service.getRetrieveAETSet());
        return !aets.isEmpty();
    }

    private HashSet toHashSet(String aets) {
        return new HashSet(Arrays.asList(StringUtils.split(aets, '\\')));
    }

    private void checkFile(FileInfo info) throws IOException {
        File file = info.toFile();
        service.getLog().info("M-READ file:" + file);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        DigestInputStream dis = new DigestInputStream(in, md);
        try {
            DcmParser parser = pFact.newDcmParser(dis);
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            if (parser.getReadTag() == Tags.PixelData) {
                if (parser.getReadLength() == -1) {
                    while (parser.parseHeader() == Tags.Item) {
                        readOut(parser.getInputStream(), parser.getReadLength());
                    }
                }
                readOut(parser.getInputStream(), parser.getReadLength());
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
            }
        } finally {
            try {
                dis.close();
            } catch (IOException ignore) {
            }
        }
        byte[] md5 = md.digest();
        if (!Arrays.equals(md5, info.getFileMd5())) {
            throw new IOException("MD5 mismatch");
        }
    }

    private void readOut(InputStream in, int len) throws IOException {
        int toRead = len;
        while (toRead-- > 0) {
            if (in.read() < 0) {
                throw new EOFException();
            }
        }
    }

    private Dimse makeNEventReportRQ() {
        Command cmd = objFact.newCommand();
        cmd.initNEventReportRQ(1, UIDs.StorageCommitmentPushModel, UIDs.StorageCommitmentPushModelSOPInstance, eventInfo.contains(Tags.FailedSOPSeq) ? 2 : 1);
        return aFact.newDimse(1, cmd, eventInfo);
    }

    private void sendResult() {
        try {
            Association assoc = aFact.newRequestor(createSocket());
            assoc.setAcTimeout(service.getAcTimeout());
            PDU pdu = assoc.connect(makeAAssociateRQ());
            if (!(pdu instanceof AAssociateAC)) {
                service.getLog().error("connection to " + aeData + " failed: " + pdu);
                return;
            }
            ActiveAssociation activeAssoc = aFact.newActiveAssociation(assoc, null);
            activeAssoc.start();
            if (checkAAssociateAC((AAssociateAC) pdu)) {
                service.logDataset("Storage Commitment Result:\n", eventInfo);
                FutureRSP rsp = activeAssoc.invoke(makeNEventReportRQ());
                Command rspCmd = rsp.get().getCommand();
                if (rspCmd.getStatus() != Status.Success) {
                    service.getLog().warn("" + aeData + " returns N-EVENT-REPORT with error status: " + rspCmd);
                }
            } else {
                service.getLog().error("storage commitment (requestor=SCP) rejected by " + aeData);
            }
            try {
                activeAssoc.release(false);
            } catch (Exception e) {
                service.getLog().warn("release association to " + aeData + " failed:", e);
            }
        } catch (Exception e) {
            service.getLog().error("sending storage commitment result to " + aeData + " failed:", e);
        }
    }

    private AAssociateRQ makeAAssociateRQ() {
        AAssociateRQ rq = aFact.newAAssociateRQ();
        rq.setCallingAET(callingAET);
        rq.setCalledAET(aeData.getTitle());
        rq.addPresContext(aFact.newPresContext(1, UIDs.StorageCommitmentPushModel, NATIVE_LE_TS));
        rq.addRoleSelection(aFact.newRoleSelection(UIDs.StorageCommitmentPushModel, false, true));
        return rq;
    }

    private boolean checkAAssociateAC(AAssociateAC ac) {
        RoleSelection rs = ac.getRoleSelection(UIDs.StorageCommitmentPushModel);
        return ac.getPresContext(1).result() == PresContext.ACCEPTANCE && rs != null && rs.scp();
    }

    private Socket createSocket() throws IOException {
        String[] cipherSuites = aeData.getCipherSuites();
        if (cipherSuites == null || cipherSuites.length == 0) {
            return new Socket(aeData.getHostName(), aeData.getPort());
        } else {
            return service.getSocketFactory(cipherSuites).createSocket(aeData.getHostName(), aeData.getPort());
        }
    }
}
