package org.dcm4chex.archive.dcm.storescp;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.management.ObjectName;
import javax.security.auth.Subject;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParseException;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationListener;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.PDU;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.ParticipantObject;
import org.dcm4che2.audit.message.PatientRecordMessage;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.codec.CompressCmd;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.config.CompressionRules;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.jdbc.QueryFilesCmd;
import org.dcm4chex.archive.exceptions.NonUniquePatientIDException;
import org.dcm4chex.archive.mbean.HttpUserInfo;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfMonDelegate;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 15749 $
 * @since 03.08.2003
 */
public class StoreScp extends DcmServiceBase implements AssociationListener {

    private static final String ALL = "ALL";

    private static final int MISSING_USER_ID_ERR_STATUS = 0xCE10;

    private static final int NO_APPEND_PERMISSION_ERR_STATUS = 0xCE24;

    private static final String MISSING_USER_ID_ERR_MSG = "Missing user identification for appending existing Study";

    private static final String NO_APPEND_PERMISSION_ERR_MSG = "No permission to append existing Study";

    private static final String STORE_XSL = "cstorerq.xsl";

    private static final String STORE_XML = "-cstorerq.xml";

    private static final String MWL2STORE_XSL = "mwl-cfindrsp2cstorerq.xsl";

    private static final String STORE2MWL_XSL = "cstorerq2mwl-cfindrq.xsl";

    private static final String RECEIVE_BUFFER = "RECEIVE_BUFFER";

    private static final String SERIES_STORED = "SERIES_STORED";

    protected final StoreScpService service;

    private final Logger log;

    private boolean studyDateInFilePath = false;

    private boolean sourceAETInFilePath = false;

    private boolean yearInFilePath = true;

    private boolean monthInFilePath = true;

    private boolean dayInFilePath = true;

    private boolean hourInFilePath = false;

    private boolean acceptMissingPatientID = true;

    private boolean acceptMissingPatientName = true;

    private boolean serializeDBUpdate = false;

    private int updateDatabaseMaxRetries = 2;

    private int maxCountUpdateDatabaseRetries = 0;

    private boolean storeDuplicateIfDiffMD5 = true;

    private boolean storeDuplicateIfDiffHost = true;

    private long updateDatabaseRetryInterval = 0L;

    private CompressionRules compressionRules = new CompressionRules("");

    private String[] coerceWarnCallingAETs = {};

    private String[] acceptMismatchIUIDCallingAETs = {};

    private String[] onlyWellKnownInstancesCallingAETs = {};

    private boolean checkIncorrectWorklistEntry = true;

    private String[] referencedDirectoryPath;

    private String[] referencedDirectoryURI;

    private String refFileSystemGroupID;

    private boolean readReferencedFile = true;

    private boolean md5sumReferencedFile = true;

    private boolean coerceBeforeWrite = false;

    protected PerfMonDelegate perfMon;

    private volatile Executor syncFileExecutor;

    public StoreScp(StoreScpService service) {
        this.service = service;
        this.log = service.getLog();
        perfMon = new PerfMonDelegate(this.service);
    }

    public final ObjectName getPerfMonServiceName() {
        return perfMon.getPerfMonServiceName();
    }

    public final void setPerfMonServiceName(ObjectName perfMonServiceName) {
        perfMon.setPerfMonServiceName(perfMonServiceName);
    }

    public final boolean isAcceptMissingPatientID() {
        return acceptMissingPatientID;
    }

    public final void setAcceptMissingPatientID(boolean accept) {
        this.acceptMissingPatientID = accept;
    }

    public final boolean isAcceptMissingPatientName() {
        return acceptMissingPatientName;
    }

    public final void setAcceptMissingPatientName(boolean accept) {
        this.acceptMissingPatientName = accept;
    }

    public final boolean isSerializeDBUpdate() {
        return serializeDBUpdate;
    }

    public final void setSerializeDBUpdate(boolean serialize) {
        this.serializeDBUpdate = serialize;
    }

    public final String getCoerceWarnCallingAETs() {
        return StringUtils.toString(coerceWarnCallingAETs, '\\');
    }

    public final void setCoerceWarnCallingAETs(String aets) {
        coerceWarnCallingAETs = StringUtils.split(aets, '\\');
    }

    public final String getAcceptMismatchIUIDCallingAETs() {
        return StringUtils.toString(acceptMismatchIUIDCallingAETs, '\\');
    }

    public final void setAcceptMismatchIUIDCallingAETs(String aets) {
        acceptMismatchIUIDCallingAETs = StringUtils.split(aets, '\\');
    }

    public final String getOnlyWellKnownInstancesCallingAETs() {
        return StringUtils.toString(onlyWellKnownInstancesCallingAETs, '\\');
    }

    public final void setOnlyWellKnownInstancesCallingAETs(String aets) {
        onlyWellKnownInstancesCallingAETs = StringUtils.split(aets, '\\');
    }

    public final boolean isStudyDateInFilePath() {
        return studyDateInFilePath;
    }

    public final void setStudyDateInFilePath(boolean studyDateInFilePath) {
        this.studyDateInFilePath = studyDateInFilePath;
    }

    public final boolean isSourceAETInFilePath() {
        return sourceAETInFilePath;
    }

    public final void setSourceAETInFilePath(boolean sourceAETInFilePath) {
        this.sourceAETInFilePath = sourceAETInFilePath;
    }

    public final boolean isYearInFilePath() {
        return yearInFilePath;
    }

    public final void setYearInFilePath(boolean yearInFilePath) {
        this.yearInFilePath = yearInFilePath;
    }

    public final boolean isMonthInFilePath() {
        return monthInFilePath;
    }

    public final void setMonthInFilePath(boolean monthInFilePath) {
        this.monthInFilePath = monthInFilePath;
    }

    public final boolean isDayInFilePath() {
        return dayInFilePath;
    }

    public final void setDayInFilePath(boolean dayInFilePath) {
        this.dayInFilePath = dayInFilePath;
    }

    public final boolean isHourInFilePath() {
        return hourInFilePath;
    }

    public final void setHourInFilePath(boolean hourInFilePath) {
        this.hourInFilePath = hourInFilePath;
    }

    public final String getReferencedDirectoryPath() {
        if (referencedDirectoryPath == null) return ALL;
        StringBuffer sb = new StringBuffer();
        String nl = System.getProperty("line.separator", "\n");
        for (String s : referencedDirectoryPath) {
            sb.append(s).append(nl);
        }
        return sb.toString();
    }

    public final void setReferencedDirectoryPath(String pathOrURI) {
        if (ALL.equals(pathOrURI.trim())) {
            referencedDirectoryURI = null;
            referencedDirectoryPath = null;
        } else {
            StringTokenizer st = new StringTokenizer(pathOrURI, " \t\r\n;");
            int len = st.countTokens();
            referencedDirectoryURI = new String[len];
            referencedDirectoryPath = new String[len];
            String trimmed;
            for (int i = 0; st.hasMoreElements(); i++) {
                trimmed = st.nextToken().trim();
                referencedDirectoryURI[i] = isURI(trimmed) ? (trimmed + '/') : FileUtils.toFile(trimmed).toURI().toString();
                referencedDirectoryPath[i] = trimmed;
            }
        }
    }

    public void setReferencedFileSystemGroupID(String groupID) {
        this.refFileSystemGroupID = groupID;
    }

    public String getReferencedFileSystemGroupID() {
        return refFileSystemGroupID;
    }

    private static boolean isURI(String pathOrURI) {
        return pathOrURI.indexOf(':') > 1;
    }

    public final boolean isMd5sumReferencedFile() {
        return md5sumReferencedFile;
    }

    public final void setMd5sumReferencedFile(boolean md5ReferencedFile) {
        this.md5sumReferencedFile = md5ReferencedFile;
    }

    public final boolean isCoerceBeforeWrite() {
        return this.coerceBeforeWrite;
    }

    public final void setCoerceBeforeWrite(boolean coerceBeforeWrite) {
        this.coerceBeforeWrite = coerceBeforeWrite;
    }

    public final boolean isReadReferencedFile() {
        return readReferencedFile;
    }

    public final void setReadReferencedFile(boolean readReferencedFile) {
        this.readReferencedFile = readReferencedFile;
    }

    public final boolean isStoreDuplicateIfDiffHost() {
        return storeDuplicateIfDiffHost;
    }

    public final void setStoreDuplicateIfDiffHost(boolean storeDuplicate) {
        this.storeDuplicateIfDiffHost = storeDuplicate;
    }

    public final boolean isStoreDuplicateIfDiffMD5() {
        return storeDuplicateIfDiffMD5;
    }

    public final void setStoreDuplicateIfDiffMD5(boolean storeDuplicate) {
        this.storeDuplicateIfDiffMD5 = storeDuplicate;
    }

    public final CompressionRules getCompressionRules() {
        return compressionRules;
    }

    public final void setCompressionRules(CompressionRules compressionRules) {
        this.compressionRules = compressionRules;
    }

    public final int getUpdateDatabaseMaxRetries() {
        return updateDatabaseMaxRetries;
    }

    public final void setUpdateDatabaseMaxRetries(int updateDatabaseMaxRetries) {
        this.updateDatabaseMaxRetries = updateDatabaseMaxRetries;
    }

    public final int getMaxCountUpdateDatabaseRetries() {
        return maxCountUpdateDatabaseRetries;
    }

    public final void setMaxCountUpdateDatabaseRetries(int count) {
        this.maxCountUpdateDatabaseRetries = count;
    }

    public final long getUpdateDatabaseRetryInterval() {
        return updateDatabaseRetryInterval;
    }

    public final void setUpdateDatabaseRetryInterval(long interval) {
        this.updateDatabaseRetryInterval = interval;
    }

    /**
     * @return Returns the checkIncorrectWorklistEntry.
     */
    public boolean isCheckIncorrectWorklistEntry() {
        return checkIncorrectWorklistEntry;
    }

    /**
     * @param checkIncorrectWorklistEntry
     *                The checkIncorrectWorklistEntry to set.
     */
    public void setCheckIncorrectWorklistEntry(boolean checkIncorrectWorklistEntry) {
        this.checkIncorrectWorklistEntry = checkIncorrectWorklistEntry;
    }

    protected void doCStore(ActiveAssociation activeAssoc, Dimse rq, Command rspCmd) throws IOException, DcmServiceException {
        InputStream in = rq.getDataAsStream();
        perfMon.start(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_IN);
        perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DIMSE, rq);
        DcmDecodeParam decParam = DcmDecodeParam.valueOf(rq.getTransferSyntaxUID());
        Dataset ds = objFact.newDataset();
        DcmParser parser = DcmParserFactory.getInstance().newDcmParser(in);
        try {
            parser.setMaxValueLength(service.getMaxValueLength());
            parser.setDcmHandler(ds.getDcmHandler());
            parser.parseDataset(decParam, Tags.PixelData);
            if (!parser.hasSeenEOF() && parser.getReadTag() != Tags.PixelData) {
                parser.unreadHeader();
                parser.parseDataset(decParam, -1);
            }
            doActualCStore(activeAssoc, rq, rspCmd, ds, parser);
        } catch (DcmParseException x) {
            throw new DcmServiceException(Status.ProcessingFailure, x.getMessage(), x);
        }
        perfMon.stop(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_IN);
    }

    /**
     * Actual CStore request handling. Allows for subclasses to do some
     * preliminary work with the rq Dataset before reading and handling the
     * pixel data.
     * 
     * This method expects that the Dataset has already been parsed from the
     * Dimse InputStream, and the DcmParser is initialized already with the
     * Dataset.
     * 
     * @param activeAssoc
     *                The ActiveAssociation
     * @param rq
     *                The Dimse request
     * @param rspCmd
     *                The response Command
     * @param ds
     *                The parsed Dataset from the Dimse rq
     * @param parser
     *                The DcmParser initialized with the InputStream from the
     */
    protected void doActualCStore(ActiveAssociation activeAssoc, Dimse rq, Command rspCmd, Dataset ds, DcmParser parser) throws IOException, DcmServiceException {
        File file = null;
        boolean dcm4cheeURIReferenced = rq.getTransferSyntaxUID().equals(UIDs.Dcm4cheURIReferenced);
        try {
            Command rqCmd = rq.getCommand();
            Association assoc = activeAssoc.getAssociation();
            String callingAET = assoc.getCallingAET();
            String calledAET = assoc.getCalledAET();
            String iuid = checkSOPInstanceUID(rqCmd, ds, callingAET);
            checkAppendPermission(assoc, ds);
            if (!checkOnlyWellKnownInstances(assoc, iuid, callingAET)) {
                log.info("StoreSCP only accepts well known instances from AE " + callingAET + " ! Ignored Instance:" + iuid);
                return;
            }
            List duplicates = new QueryFilesCmd(iuid).getFileDTOs();
            if (!(duplicates.isEmpty() || storeDuplicateIfDiffMD5 || storeDuplicateIfDiffHost && !containsLocal(duplicates))) {
                log.info("Received Instance[uid=" + iuid + "] already exists - ignored");
                return;
            }
            service.preProcess(ds);
            if (log.isDebugEnabled()) {
                log.debug("Dataset:\n");
                log.debug(ds);
            }
            perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET, ds);
            service.logDIMSE(assoc, STORE_XML, ds);
            if (isCheckIncorrectWorklistEntry() && checkIncorrectWorklistEntry(ds)) {
                log.info("Received Instance[uid=" + iuid + "] ignored! Reason: Incorrect Worklist entry selected!");
                return;
            }
            String retrieveAET;
            String availability;
            FileSystemDTO fsDTO = null;
            String filePath = null;
            byte[] md5sum = null;
            Dataset coerced = service.getCoercionAttributesFor(callingAET, STORE_XSL, ds, assoc);
            if (coerceBeforeWrite) {
                ds.setPrivateCreatorID(PrivateTags.CreatorID);
                ds.putAE(PrivateTags.CallingAET, callingAET);
                ds.putAE(PrivateTags.CalledAET, calledAET);
                ds.setPrivateCreatorID(null);
                if (coerced != null) {
                    service.coerceAttributes(ds, coerced);
                }
                service.postCoercionProcessing(ds);
            }
            if (dcm4cheeURIReferenced) {
                String uri = ds.getString(Tags.RetrieveURI);
                if (uri == null) {
                    retrieveAET = ds.getString(Tags.RetrieveAET);
                    availability = ds.getString(Tags.InstanceAvailability);
                    if (retrieveAET == null || availability == null) {
                        throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Missing (0040,E010) Retrieve URI - required for Dcm4che Retrieve URI Transfer Syntax");
                    }
                } else {
                    String[] selected = selectReferencedDirectoryURI(uri);
                    if (selected == null) {
                        throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "(0040,E010) Retrieve URI: " + uri + " does not match with configured Referenced Directory Path: " + getReferencedDirectoryPath());
                    }
                    filePath = uri.substring(selected[1].length());
                    if (uri.startsWith("file:/")) {
                        file = new File(new URI(uri));
                        if (!file.isFile()) {
                            throw new DcmServiceException(Status.ProcessingFailure, "File referenced by (0040,E010) Retrieve URI: " + uri + " not found!");
                        }
                    }
                    fsDTO = getFileSystemMgt().getFileSystemOfGroup(refFileSystemGroupID, selected[0].startsWith("file:") ? new URI(selected[0]).getPath() : selected[0]);
                    retrieveAET = fsDTO.getRetrieveAET();
                    availability = Availability.toString(fsDTO.getAvailability());
                    if (file != null && readReferencedFile) {
                        log.info("M-READ " + file);
                        Dataset fileDS = objFact.newDataset();
                        FileInputStream fis = new FileInputStream(file);
                        try {
                            if (md5sumReferencedFile) {
                                MessageDigest digest = MessageDigest.getInstance("MD5");
                                DigestInputStream dis = new DigestInputStream(fis, digest);
                                BufferedInputStream bis = new BufferedInputStream(dis);
                                fileDS.readFile(bis, null, Tags.PixelData);
                                byte[] buf = getByteBuffer(assoc);
                                while (bis.read(buf) != -1) ;
                                md5sum = digest.digest();
                            } else {
                                BufferedInputStream bis = new BufferedInputStream(fis);
                                fileDS.readFile(bis, null, Tags.PixelData);
                            }
                        } finally {
                            fis.close();
                        }
                        fileDS.putAll(ds, Dataset.REPLACE_ITEMS);
                        ds = fileDS;
                    }
                }
                if (ds.getFileMetaInfo() == null) {
                    ds.setPrivateCreatorID(PrivateTags.CreatorID);
                    String tsuid = ds.getString(PrivateTags.Dcm4cheURIReferencedTransferSyntaxUID, UIDs.ImplicitVRLittleEndian);
                    ds.setPrivateCreatorID(null);
                    ds.setFileMetaInfo(objFact.newFileMetaInfo(rqCmd.getAffectedSOPClassUID(), rqCmd.getAffectedSOPInstanceUID(), tsuid));
                }
            } else {
                String fsgrpid = service.selectFileSystemGroup(callingAET, calledAET, ds);
                fsDTO = service.selectStorageFileSystem(fsgrpid);
                retrieveAET = fsDTO.getRetrieveAET();
                availability = Availability.toString(fsDTO.getAvailability());
                File baseDir = FileUtils.toFile(fsDTO.getDirectoryPath());
                file = makeFile(baseDir, ds, callingAET);
                filePath = file.getPath().substring(baseDir.getPath().length() + 1).replace(File.separatorChar, '/');
                CompressCmd compressCmd = null;
                if (parser.getReadTag() == Tags.PixelData && parser.getReadLength() != -1) {
                    compressCmd = compressionRules.getCompressFor(assoc, ds);
                    if (compressCmd != null) compressCmd.coerceDataset(ds);
                }
                ds.setFileMetaInfo(objFact.newFileMetaInfo(ds, compressCmd != null ? compressCmd.getTransferSyntaxUID() : rq.getTransferSyntaxUID()));
                perfMon.start(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_STORE);
                perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.DICOM_FILE, file);
                md5sum = storeToFile(parser, ds, file, compressCmd, getByteBuffer(assoc));
                perfMon.stop(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_STORE);
            }
            if (md5sum != null && ignoreDuplicate(duplicates, md5sum)) {
                log.info("Received Instance[uid=" + iuid + "] already exists - ignored");
                if (!dcm4cheeURIReferenced) {
                    deleteFailedStorage(file);
                }
                return;
            }
            ds.putAE(Tags.RetrieveAET, retrieveAET);
            if (!coerceBeforeWrite) {
                ds.setPrivateCreatorID(PrivateTags.CreatorID);
                ds.putAE(PrivateTags.CallingAET, callingAET);
                ds.putAE(PrivateTags.CalledAET, calledAET);
                ds.setPrivateCreatorID(null);
                if (coerced != null) {
                    service.coerceAttributes(ds, coerced);
                }
                service.postCoercionProcessing(ds);
            }
            checkPatientIdAndName(ds, callingAET);
            Storage store = getStorage(assoc);
            SeriesStored seriesStored = handleSeriesStored(assoc, store, ds);
            boolean newSeries = seriesStored == null;
            boolean newStudy = false;
            String seriuid = ds.getString(Tags.SeriesInstanceUID);
            if (newSeries) {
                Dataset mwlFilter = service.getCoercionAttributesFor(callingAET, STORE2MWL_XSL, ds, assoc);
                if (mwlFilter != null) {
                    coerced = merge(coerced, mergeMatchingMWLItem(assoc, ds, seriuid, mwlFilter));
                }
                if (!callingAET.equals(calledAET)) {
                    service.ignorePatientIDForUnscheduled(ds, Tags.RequestAttributesSeq, callingAET);
                    service.supplementIssuerOfPatientID(ds, assoc, callingAET, false);
                    service.supplementIssuerOfAccessionNumber(ds, assoc, callingAET, false);
                    service.supplementInstitutionalData(ds, assoc, callingAET);
                    service.generatePatientID(ds, ds, calledAET);
                }
                newStudy = !store.studyExists(ds.getString(Tags.StudyInstanceUID));
            }
            perfMon.start(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);
            long fileLength = file != null ? file.length() : 0L;
            long fspk = fsDTO != null ? fsDTO.getPk() : -1L;
            Dataset coercedElements;
            try {
                coercedElements = updateDB(store, ds, fspk, filePath, fileLength, md5sum, newSeries);
            } catch (NonUniquePatientIDException e) {
                service.coercePatientID(ds);
                coerced.putLO(Tags.PatientID, ds.getString(Tags.PatientID));
                coerced.putLO(Tags.IssuerOfPatientID, ds.getString(Tags.IssuerOfPatientID));
                coercedElements = updateDB(store, ds, fspk, filePath, fileLength, md5sum, newSeries);
            }
            if (newSeries) {
                seriesStored = initSeriesStored(ds, callingAET, retrieveAET);
                assoc.putProperty(SERIES_STORED, seriesStored);
                if (newStudy) {
                    service.sendNewStudyNotification(ds);
                }
            }
            appendInstanceToSeriesStored(seriesStored, ds, retrieveAET, availability);
            coerced = merge(coerced, coercedElements);
            try {
                logCoercion(ds, coerced);
            } catch (Exception e) {
                log.warn("Failed to generate audit log for attribute coercion:", e);
            }
            ds.putAll(coercedElements, Dataset.MERGE_ITEMS);
            perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET, ds);
            perfMon.stop(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);
            if (coerced.isEmpty() || !contains(coerceWarnCallingAETs, callingAET)) {
                rspCmd.putUS(Tags.Status, Status.Success);
            } else {
                int[] coercedTags = new int[coerced.size()];
                Iterator it = coerced.iterator();
                for (int i = 0; i < coercedTags.length; i++) {
                    coercedTags[i] = ((DcmElement) it.next()).tag();
                }
                rspCmd.putAT(Tags.OffendingElement, coercedTags);
                rspCmd.putUS(Tags.Status, Status.CoercionOfDataElements);
            }
            service.postProcess(ds);
        } catch (DcmServiceException e) {
            log.warn(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw e;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    protected SeriesStored handleSeriesStored(Association assoc, Storage store, Dataset ds) throws FinderException, RemoteException, Exception {
        String seriuid = ds.getString(Tags.SeriesInstanceUID);
        SeriesStored seriesStored = (SeriesStored) assoc.getProperty(SERIES_STORED);
        if (seriesStored != null && !seriuid.equals(seriesStored.getSeriesInstanceUID())) {
            service.logInstancesStoredAndUpdateDerivedFields(store, assoc.getSocket(), seriesStored);
            doAfterSeriesIsStored(store, assoc, seriesStored);
            seriesStored = null;
        }
        return seriesStored;
    }

    private void logCoercion(Dataset ds, Dataset coerced) {
        if (coerced.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (Iterator<DcmElement> i = coerced.iterator(); i.hasNext(); ) {
            DcmElement coercedElement = i.next();
            DcmElement originalElement = ds.get(coercedElement.tag());
            if (originalElement != null) {
                String originalValue = originalElement.toString();
                String coercedValue = coercedElement.toString();
                if (!originalValue.equals(coercedValue)) {
                    if (sb.length() > 0) sb.append(";");
                    sb.append(" [");
                    sb.append(originalValue);
                    sb.append("->");
                    sb.append(coercedValue);
                    sb.append("]");
                }
            }
        }
        if (sb.length() > 0) {
            sb.insert(0, "The following elements were coerced during storage: ");
            HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
            PatientRecordMessage msg = new PatientRecordMessage(PatientRecordMessage.UPDATE);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo.getHostName(), true);
            PersonName pn = ds.getPersonName(Tags.PatientName);
            String pname = pn != null ? pn.format() : null;
            ParticipantObject patient = msg.addPatient(ds.getString(Tags.PatientID, "NO_PATIENT_ID"), pname);
            patient.addParticipantObjectDetail("Description", sb.toString());
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        }
    }

    private String[] selectReferencedDirectoryURI(String uri) {
        if (referencedDirectoryPath == null) {
            log.debug("ReferencedDirectoryPath is set to ALL! uri:" + uri);
            try {
                FileSystemDTO[] fsDTOs = getFileSystemMgt().getFileSystemsOfGroup(this.refFileSystemGroupID);
                String dir, fsUri;
                for (FileSystemDTO dto : fsDTOs) {
                    dir = dto.getDirectoryPath();
                    fsUri = isURI(dir) ? dir + "/" : FileUtils.toFile(dir).toURI().toString();
                    log.debug("Filesystem URI:" + fsUri);
                    if (uri.startsWith(fsUri)) {
                        return new String[] { dir, fsUri };
                    }
                }
            } catch (Exception x) {
                log.error("Can't get FilesystemMgt Bean!", x);
            }
        } else {
            for (int i = 0; i < referencedDirectoryURI.length; i++) {
                if (uri.startsWith(referencedDirectoryURI[i])) {
                    return new String[] { referencedDirectoryPath[i], referencedDirectoryURI[i] };
                }
            }
        }
        return null;
    }

    private SeriesStored initSeriesStored(Dataset ds, String callingAET, String retrieveAET) {
        Dataset patAttrs = AttributeFilter.getPatientAttributeFilter().filter(ds);
        Dataset studyAttrs = AttributeFilter.getStudyAttributeFilter().filter(ds);
        Dataset seriesAttrs = AttributeFilter.getSeriesAttributeFilter().filter(ds);
        Dataset ian = DcmObjectFactory.getInstance().newDataset();
        ian.putUI(Tags.StudyInstanceUID, ds.getString(Tags.StudyInstanceUID));
        Dataset refSeries = ian.putSQ(Tags.RefSeriesSeq).addNewItem();
        refSeries.putUI(Tags.SeriesInstanceUID, ds.getString(Tags.SeriesInstanceUID));
        refSeries.putSQ(Tags.RefSOPSeq);
        Dataset pps = seriesAttrs.getItem(Tags.RefPPSSeq);
        DcmElement refPPSSeq = ian.putSQ(Tags.RefPPSSeq);
        if (pps != null) {
            if (!pps.contains(Tags.PerformedWorkitemCodeSeq)) {
                pps.putSQ(Tags.PerformedWorkitemCodeSeq);
            }
            refPPSSeq.addItem(pps);
        }
        return new SeriesStored(callingAET, retrieveAET, patAttrs, studyAttrs, seriesAttrs, ian);
    }

    private void appendInstanceToSeriesStored(SeriesStored seriesStored, Dataset ds, String retrieveAET, String availability) {
        Dataset refSOP = seriesStored.getIAN().get(Tags.RefSeriesSeq).getItem().get(Tags.RefSOPSeq).addNewItem();
        refSOP.putUI(Tags.RefSOPClassUID, ds.getString(Tags.SOPClassUID));
        refSOP.putUI(Tags.RefSOPInstanceUID, ds.getString(Tags.SOPInstanceUID));
        refSOP.putAE(Tags.RetrieveAET, retrieveAET);
        refSOP.putCS(Tags.InstanceAvailability, availability);
    }

    private void checkAppendPermission(Association a, Dataset ds) throws Exception {
        if (service.hasUnrestrictedAppendPermissions(a.getCallingAET())) {
            return;
        }
        String seriuid = ds.getString(Tags.SeriesInstanceUID);
        SeriesStored seriesStored = (SeriesStored) a.getProperty(SERIES_STORED);
        if (seriesStored != null && seriuid.equals(seriesStored.getSeriesInstanceUID())) {
            return;
        }
        String suid = ds.getString(Tags.StudyInstanceUID);
        if (!getStorage(a).studyExists(suid)) {
            return;
        }
        Subject subject = (Subject) a.getProperty("user");
        if (subject == null) {
            throw new DcmServiceException(MISSING_USER_ID_ERR_STATUS, MISSING_USER_ID_ERR_MSG);
        }
        if (!service.getStudyPermissionManager(a).hasPermission(suid, StudyPermissionDTO.APPEND_ACTION, subject)) {
            throw new DcmServiceException(NO_APPEND_PERMISSION_ERR_STATUS, NO_APPEND_PERMISSION_ERR_MSG);
        }
    }

    private boolean checkOnlyWellKnownInstances(Association assoc, String iuid, String callingAET) throws Exception {
        if (contains(onlyWellKnownInstancesCallingAETs, callingAET)) {
            return getStorage(assoc).instanceExists(iuid);
        }
        return true;
    }

    private Dataset merge(Dataset ds, Dataset merge) {
        if (ds == null) {
            return merge;
        }
        if (merge == null) {
            return ds;
        }
        ds.putAll(merge, Dataset.MERGE_ITEMS);
        return ds;
    }

    private Dataset mergeMatchingMWLItem(Association assoc, Dataset ds, String seriuid, Dataset mwlFilter) {
        List mwlItems;
        log.info("Query for matching worklist entries for received Series[" + seriuid + "]");
        try {
            mwlItems = service.findMWLEntries(mwlFilter);
        } catch (Exception e) {
            log.error("Query for matching worklist entries for received Series[" + seriuid + "] failed:", e);
            return null;
        }
        int size = mwlItems.size();
        log.info("" + size + " matching worklist entries found for received Series[ " + seriuid + "]");
        if (size == 0) {
            return null;
        }
        String callingAET = assoc.getCallingAET();
        Dataset coerce = service.getCoercionAttributesFor(callingAET, MWL2STORE_XSL, (Dataset) mwlItems.get(0), assoc);
        if (coerce == null) {
            log.error("Failed to find or load stylesheet " + MWL2STORE_XSL + " for " + callingAET + ". Cannot coerce object attributes with request information.");
            return null;
        }
        if (size > 1) {
            DcmElement rqAttrsSq = coerce.get(Tags.RequestAttributesSeq);
            Dataset coerce0 = coerce.exclude(new int[] { Tags.RequestAttributesSeq });
            for (int i = 1; i < size; i++) {
                Dataset coerce1 = service.getCoercionAttributesFor(callingAET, MWL2STORE_XSL, (Dataset) mwlItems.get(i), assoc);
                if (!coerce1.match(coerce0, true, true)) {
                    log.warn("Several (" + size + ") matching worklist entries " + "found for received Series[ " + seriuid + "], which differs also in attributes NOT mapped to the Request Attribute Sequence item " + "- Do not coerce object attributes with request information.");
                    return null;
                }
                if (rqAttrsSq != null) {
                    Dataset item = coerce1.getItem(Tags.RequestAttributesSeq);
                    if (item != null) {
                        rqAttrsSq.addItem(item);
                    }
                }
            }
        }
        service.coerceAttributes(ds, coerce);
        return coerce;
    }

    private boolean checkIncorrectWorklistEntry(Dataset ds) throws Exception {
        Dataset refPPS = ds.getItem(Tags.RefPPSSeq);
        if (refPPS == null) {
            return false;
        }
        String ppsUID = refPPS.getString(Tags.RefSOPInstanceUID);
        if (ppsUID == null) {
            return false;
        }
        Dataset mpps;
        try {
            mpps = getMPPSManager().getMPPS(ppsUID);
        } catch (ObjectNotFoundException e) {
            return false;
        }
        Dataset item = mpps.getItem(Tags.PPSDiscontinuationReasonCodeSeq);
        return item != null && "110514".equals(item.getString(Tags.CodeValue)) && "DCM".equals(item.getString(Tags.CodingSchemeDesignator));
    }

    private MPPSManager getMPPSManager() throws CreateException, RemoteException, HomeFactoryException {
        return ((MPPSManagerHome) EJBHomeFactory.getFactory().lookup(MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME)).create();
    }

    private FileSystemMgt2 getFileSystemMgt() throws RemoteException, CreateException, HomeFactoryException {
        return ((FileSystemMgt2Home) EJBHomeFactory.getFactory().lookup(FileSystemMgt2Home.class, FileSystemMgt2Home.JNDI_NAME)).create();
    }

    private byte[] getByteBuffer(Association assoc) {
        byte[] buf = (byte[]) assoc.getProperty(RECEIVE_BUFFER);
        if (buf == null) {
            buf = new byte[service.getBufferSize()];
            assoc.putProperty(RECEIVE_BUFFER, buf);
        }
        return buf;
    }

    private boolean containsLocal(List duplicates) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (service.isFileSystemGroupLocalAccessable(dto.getFileSystemGroupID())) return true;
        }
        return false;
    }

    private boolean ignoreDuplicate(List duplicates, byte[] md5sum) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (storeDuplicateIfDiffMD5 && !Arrays.equals(md5sum, dto.getFileMd5())) continue;
            if (storeDuplicateIfDiffHost && !service.isFileSystemGroupLocalAccessable(dto.getFileSystemGroupID())) continue;
            return true;
        }
        return false;
    }

    private void deleteFailedStorage(File file) {
        if (file == null) {
            return;
        }
        log.info("M-DELETE file:" + file);
        file.delete();
        File seriesDir = file.getParentFile();
        if (seriesDir.delete()) {
            seriesDir.getParentFile().delete();
        }
    }

    protected Dataset updateDB(Storage storage, Dataset ds, long fspk, String filePath, long fileLength, byte[] md5, boolean updateStudyAccessTime) throws DcmServiceException, NonUniquePatientIDException {
        int retry = 0;
        for (; ; ) {
            try {
                if (serializeDBUpdate) {
                    synchronized (storage) {
                        return storage.store(ds, fspk, filePath, fileLength, md5, updateStudyAccessTime, service.patientMatching());
                    }
                } else {
                    return storage.store(ds, fspk, filePath, fileLength, md5, updateStudyAccessTime, service.patientMatching());
                }
            } catch (NonUniquePatientIDException e) {
                throw e;
            } catch (Exception e) {
                ++retry;
                if (retry > updateDatabaseMaxRetries) {
                    service.getLog().error("failed to update DB with entries for received " + filePath, e);
                    throw new DcmServiceException(Status.ProcessingFailure, e);
                }
                maxCountUpdateDatabaseRetries = Math.max(retry, maxCountUpdateDatabaseRetries);
                service.getLog().warn("failed to update DB with entries for received " + filePath + " - retry", e);
                try {
                    Thread.sleep(updateDatabaseRetryInterval);
                } catch (InterruptedException e1) {
                    log.warn("update Database Retry Interval interrupted:", e1);
                }
            }
        }
    }

    Storage getStorage(Association assoc) throws RemoteException, CreateException, HomeFactoryException {
        Storage store = (Storage) assoc.getProperty(StorageHome.JNDI_NAME);
        if (store == null) {
            store = service.getStorage();
            assoc.putProperty(StorageHome.JNDI_NAME, store);
        }
        return store;
    }

    File makeFile(File basedir, Dataset ds, String callingAET) throws Exception {
        Calendar date = Calendar.getInstance();
        StringBuffer filePath = new StringBuffer();
        if (sourceAETInFilePath && callingAET != null) {
            filePath.append(callingAET);
            filePath.append(File.separatorChar);
        }
        if (studyDateInFilePath) {
            Date studyDate = ds.getDateTime(Tags.StudyDate, Tags.StudyTime);
            if (studyDate != null) date.setTime(studyDate);
        }
        if (yearInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.YEAR)));
            filePath.append(File.separatorChar);
        }
        if (monthInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.MONTH) + 1));
            filePath.append(File.separatorChar);
        }
        if (dayInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
            filePath.append(File.separatorChar);
        }
        if (hourInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.HOUR_OF_DAY)));
            filePath.append(File.separatorChar);
        }
        filePath.append(FileUtils.toHex(ds.getString(Tags.StudyInstanceUID).hashCode()));
        filePath.append(File.separatorChar);
        filePath.append(FileUtils.toHex(ds.getString(Tags.SeriesInstanceUID).hashCode()));
        File dir = new File(basedir, filePath.toString());
        return FileUtils.createNewFile(dir, ds.getString(Tags.SOPInstanceUID).hashCode());
    }

    private byte[] storeToFile(DcmParser parser, Dataset ds, final File file, CompressCmd compressCmd, byte[] buffer) throws Exception {
        log.info("M-WRITE file:" + file);
        MessageDigest md = null;
        BufferedOutputStream bos = null;
        FileOutputStream fos = new FileOutputStream(file);
        if (service.isMd5sum()) {
            md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(fos, md);
            bos = new BufferedOutputStream(dos, buffer);
        } else {
            bos = new BufferedOutputStream(fos, buffer);
        }
        try {
            DcmDecodeParam decParam = parser.getDcmDecodeParam();
            String tsuid = ds.getFileMetaInfo().getTransferSyntaxUID();
            DcmEncodeParam encParam = DcmEncodeParam.valueOf(tsuid);
            ds.writeFile(bos, encParam);
            if (parser.getReadTag() == Tags.PixelData) {
                int len = parser.getReadLength();
                InputStream in = parser.getInputStream();
                if (encParam.encapsulated) {
                    ds.writeHeader(bos, encParam, Tags.PixelData, VRs.OB, -1);
                    if (decParam.encapsulated) {
                        parser.parseHeader();
                        while (parser.getReadTag() == Tags.Item) {
                            len = parser.getReadLength();
                            ds.writeHeader(bos, encParam, Tags.Item, VRs.NONE, len);
                            bos.copyFrom(in, len);
                            parser.parseHeader();
                        }
                    } else {
                        int read = compressCmd.compress(decParam.byteOrder, parser.getInputStream(), bos, null);
                        skipFully(in, parser.getReadLength() - read);
                    }
                    ds.writeHeader(bos, encParam, Tags.SeqDelimitationItem, VRs.NONE, 0);
                } else {
                    ds.writeHeader(bos, encParam, Tags.PixelData, parser.getReadVR(), len);
                    bos.copyFrom(in, len);
                }
                parser.parseDataset(decParam, -1);
                ds.subSet(Tags.PixelData, -1).writeDataset(bos, encParam);
            }
            bos.flush();
            if (service.isSyncFileBeforeCStoreRSP()) {
                fos.getFD().sync();
            } else if (service.isSyncFileAfterCStoreRSP()) {
                final FileOutputStream fos2 = fos;
                syncFileExecutor().execute(new Runnable() {

                    public void run() {
                        try {
                            fos2.getFD().sync();
                        } catch (Exception e) {
                            log.error("sync of " + file + " failed:", e);
                        } finally {
                            try {
                                fos2.close();
                            } catch (Exception ignore) {
                            }
                        }
                    }
                });
                fos = null;
            }
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (Exception ignore) {
            }
        }
        return md != null ? md.digest() : null;
    }

    private Executor syncFileExecutor() {
        Executor result = syncFileExecutor;
        if (result == null) {
            synchronized (this) {
                result = syncFileExecutor;
                if (result == null) {
                    syncFileExecutor = result = Executors.newSingleThreadExecutor();
                }
            }
        }
        return result;
    }

    private static void skipFully(InputStream in, int n) throws IOException {
        int remaining = n;
        int skipped = 0;
        while (remaining > 0) {
            if ((skipped = (int) in.skip(remaining)) == 0) {
                throw new EOFException();
            }
            remaining -= skipped;
        }
    }

    private String checkSOPInstanceUID(Command rqCmd, Dataset ds, String aet) throws DcmServiceException {
        String cuid = checkNotNull(ds.getString(Tags.SOPClassUID), "Missing SOP Class UID (0008,0016)");
        String iuid = checkNotNull(ds.getString(Tags.SOPInstanceUID), "Missing SOP Instance UID (0008,0018)");
        checkNotNull(ds.getString(Tags.StudyInstanceUID), "Missing Study Instance UID (0020,000D)");
        checkNotNull(ds.getString(Tags.SeriesInstanceUID), "Missing Series Instance UID (0020,000E)");
        if (!rqCmd.getAffectedSOPInstanceUID().equals(iuid)) {
            String prompt = "SOP Instance UID in Dataset [" + iuid + "] differs from Affected SOP Instance UID[" + rqCmd.getAffectedSOPInstanceUID() + "]";
            log.warn(prompt);
            if (!contains(acceptMismatchIUIDCallingAETs, aet)) {
                throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, prompt);
            }
        }
        if (!rqCmd.getAffectedSOPClassUID().equals(cuid)) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "SOP Class UID in Dataset differs from Affected SOP Class UID");
        }
        return iuid;
    }

    private static String checkNotNull(String val, String msg) throws DcmServiceException {
        if (val == null) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, msg);
        }
        return val;
    }

    private void checkPatientIdAndName(Dataset ds, String aet) throws DcmServiceException, HomeFactoryException, RemoteException, CreateException, FinderException {
        String pid = ds.getString(Tags.PatientID);
        String pname = ds.getString(Tags.PatientName);
        if (pid == null && !acceptMissingPatientID) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Acceptance of objects without Patient ID is disabled");
        }
        if (pname == null && !acceptMissingPatientName) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Acceptance of objects without Patient Name is disabled");
        }
    }

    private boolean contains(Object[] a, Object e) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(e)) {
                return true;
            }
        }
        return false;
    }

    public void write(Association src, PDU pdu) {
        if (pdu instanceof AAssociateAC) perfMon.assocEstEnd(src, Command.C_STORE_RQ);
    }

    public void received(Association src, PDU pdu) {
        if (pdu instanceof AAssociateRQ) perfMon.assocEstStart(src, Command.C_STORE_RQ);
    }

    public void write(Association src, Dimse dimse) {
    }

    public void received(Association src, Dimse dimse) {
    }

    public void error(Association src, IOException ioe) {
    }

    public void closing(Association assoc) {
        if (assoc.getAAssociateAC() != null) perfMon.assocRelStart(assoc, Command.C_STORE_RQ);
        SeriesStored seriesStored = (SeriesStored) assoc.getProperty(SERIES_STORED);
        if (seriesStored != null) {
            try {
                Storage store = getStorage(assoc);
                service.logInstancesStoredAndUpdateDerivedFields(store, assoc.getSocket(), seriesStored);
                doAfterSeriesIsStored(store, assoc, seriesStored);
            } catch (Exception e) {
                handleClosingFailed(e);
            }
        }
    }

    protected void handleClosingFailed(Exception e) {
        log.error("Clean up on Association close failed:", e);
    }

    public void closed(Association assoc) {
        if (assoc.getAAssociateAC() != null) perfMon.assocRelEnd(assoc, Command.C_STORE_RQ);
    }

    protected void doAfterSeriesIsStored(Storage store, Association assoc, SeriesStored seriesStored) throws Exception {
        return;
    }
}
