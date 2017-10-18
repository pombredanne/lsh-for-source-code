package org.dcm4chex.cdw.mbean;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmService;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.util.MD5Utils;
import org.dcm4chex.cdw.common.ConfigurationException;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 9839 $ $Date: 2007-06-28 11:35:54 +0200 (Thu, 28 Jun
 *          2007) $
 * @since 22.06.2004
 * 
 */
public class StoreScpService extends AbstractScpService {

    private static final String LOOKUP_MEDIA_WRITER_NAME = "lookupMediaWriterName";

    private static final String NONE = "NONE";

    private static final int[] TYPE1_ATTR = { Tags.StudyInstanceUID, Tags.SeriesInstanceUID, Tags.SOPInstanceUID, Tags.SOPClassUID };

    private Map imageCUIDS = new LinkedHashMap();

    private Map imageTSUIDS = new LinkedHashMap();

    private Map waveformCUIDS = new LinkedHashMap();

    private Map waveformTSUIDS = new LinkedHashMap();

    private Map videoCUIDS = new LinkedHashMap();

    private Map videoTSUIDS = new LinkedHashMap();

    private Map srCUIDS = new LinkedHashMap();

    private Map srTSUIDS = new LinkedHashMap();

    private Map otherCUIDS = new LinkedHashMap();

    private String generatePatientID = "DCMCDW-##########";

    private String issuerOfPatientID = "DCMCDW";

    private int bufferSize = 512;

    private ObjectName mediaCreationRequestEmulatorServiceName;

    private final DcmService service = new DcmServiceBase() {

        protected void doCStore(ActiveAssociation assoc, Dimse rq, Command rspCmd) throws IOException, DcmServiceException {
            StoreScpService.this.doCStore(assoc, rq, rspCmd);
        }
    };

    public final String getGeneratePatientID() {
        return generatePatientID != null ? generatePatientID : NONE;
    }

    public final void setGeneratePatientID(String pattern) {
        this.generatePatientID = pattern.equalsIgnoreCase(NONE) ? null : pattern;
    }

    public final String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public final void setIssuerOfPatientID(String issuerOfPatientID) {
        this.issuerOfPatientID = issuerOfPatientID;
    }

    public String getAcceptedImageSOPClasses() {
        return toString(imageCUIDS);
    }

    public void setAcceptedImageSOPClasses(String s) {
        updateAcceptedSOPClass(imageCUIDS, s, service);
    }

    public String getAcceptedTransferSyntaxForImageSOPClasses() {
        return toString(imageTSUIDS);
    }

    public void setAcceptedTransferSyntaxForImageSOPClasses(String s) {
        updateAcceptedTransferSyntax(imageTSUIDS, s);
    }

    public String getAcceptedVideoSOPClasses() {
        return toString(videoCUIDS);
    }

    public void setAcceptedVideoSOPClasses(String s) {
        updateAcceptedSOPClass(videoCUIDS, s, service);
    }

    public String getAcceptedTransferSyntaxForVideoSOPClasses() {
        return toString(videoTSUIDS);
    }

    public void setAcceptedTransferSyntaxForVideoSOPClasses(String s) {
        updateAcceptedTransferSyntax(videoTSUIDS, s);
    }

    public String getAcceptedSRSOPClasses() {
        return toString(srCUIDS);
    }

    public void setAcceptedSRSOPClasses(String s) {
        updateAcceptedSOPClass(srCUIDS, s, service);
    }

    public String getAcceptedTransferSyntaxForSRSOPClasses() {
        return toString(srTSUIDS);
    }

    public void setAcceptedTransferSyntaxForSRSOPClasses(String s) {
        updateAcceptedTransferSyntax(srTSUIDS, s);
    }

    public String getAcceptedWaveformSOPClasses() {
        return toString(waveformCUIDS);
    }

    public void setAcceptedWaveformSOPClasses(String s) {
        updateAcceptedSOPClass(waveformCUIDS, s, service);
    }

    public String getAcceptedTransferSyntaxForWaveformSOPClasses() {
        return toString(waveformTSUIDS);
    }

    public void setAcceptedTransferSyntaxForWaveformSOPClasses(String s) {
        updateAcceptedTransferSyntax(waveformTSUIDS, s);
    }

    public String getAcceptedOtherSOPClasses() {
        return toString(otherCUIDS);
    }

    public void setAcceptedOtherSOPClasses(String s) {
        updateAcceptedSOPClass(otherCUIDS, s, service);
    }

    public ObjectName getMediaCreationRequestEmulatorServiceName() {
        return mediaCreationRequestEmulatorServiceName;
    }

    public void setMediaCreationRequestEmulatorServiceName(ObjectName name) {
        this.mediaCreationRequestEmulatorServiceName = name;
    }

    private boolean emulateMediaCreationRequestForAET(String aet) {
        try {
            return ((Boolean) server.invoke(mediaCreationRequestEmulatorServiceName, "containsSourceAETitle", new Object[] { aet }, new String[] { String.class.getName() })).booleanValue();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public final int getBufferSize() {
        return bufferSize;
    }

    public final void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected void bindDcmServices() {
        bindAll(valuesToStringArray(imageCUIDS), service);
        bindAll(valuesToStringArray(videoCUIDS), service);
        bindAll(valuesToStringArray(srCUIDS), service);
        bindAll(valuesToStringArray(waveformCUIDS), service);
        bindAll(valuesToStringArray(otherCUIDS), service);
    }

    protected void unbindDcmServices() {
        unbindAll(valuesToStringArray(imageCUIDS));
        unbindAll(valuesToStringArray(videoCUIDS));
        unbindAll(valuesToStringArray(srCUIDS));
        unbindAll(valuesToStringArray(waveformCUIDS));
        unbindAll(valuesToStringArray(otherCUIDS));
    }

    protected void updatePresContexts() {
        putPresContexts(valuesToStringArray(imageCUIDS), valuesToStringArray(imageTSUIDS));
        putPresContexts(valuesToStringArray(videoCUIDS), valuesToStringArray(videoTSUIDS));
        putPresContexts(valuesToStringArray(srCUIDS), valuesToStringArray(srTSUIDS));
        putPresContexts(valuesToStringArray(waveformCUIDS), valuesToStringArray(waveformTSUIDS));
        putPresContexts(valuesToStringArray(otherCUIDS), valuesToStringArray(tsuidMap));
    }

    protected void removePresContexts() {
        putPresContexts(valuesToStringArray(imageCUIDS), null);
        putPresContexts(valuesToStringArray(videoCUIDS), null);
        putPresContexts(valuesToStringArray(srCUIDS), null);
        putPresContexts(valuesToStringArray(waveformCUIDS), null);
        putPresContexts(valuesToStringArray(otherCUIDS), null);
    }

    private String lookupMediaWriterName(String aet) throws DcmServiceException {
        try {
            return (String) server.invoke(dcmServerName, LOOKUP_MEDIA_WRITER_NAME, new Object[] { aet }, new String[] { String.class.getName() });
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    private void doCStore(ActiveAssociation assoc, Dimse rq, Command rspCmd) throws DcmServiceException, IOException {
        Command rqCmd = rq.getCommand();
        InputStream in = rq.getDataAsStream();
        if (spoolDir.isArchiveHighWater()) {
            in.close();
            throw new DcmServiceException(Status.OutOfResources);
        }
        String cuid = rqCmd.getAffectedSOPClassUID();
        String iuid = rqCmd.getAffectedSOPInstanceUID();
        String tsuid = rq.getTransferSyntaxUID();
        DcmDecodeParam decParam = DcmDecodeParam.valueOf(tsuid);
        String fileTS = decParam.encapsulated ? tsuid : UIDs.ExplicitVRLittleEndian;
        DcmEncodeParam encParam = DcmEncodeParam.valueOf(fileTS);
        DcmObjectFactory dof = DcmObjectFactory.getInstance();
        Dataset ds = dof.newDataset();
        DcmParserFactory pf = DcmParserFactory.getInstance();
        DcmParser parser = pf.newDcmParser(in);
        parser.setDcmHandler(ds.getDcmHandler());
        parser.parseDataset(decParam, Tags.PixelData);
        checkDataset(rqCmd, ds, parser);
        ds.setFileMetaInfo(dof.newFileMetaInfo(cuid, iuid, fileTS));
        File file = spoolDir.getInstanceFile(iuid);
        File md5file = MD5Utils.makeMD5File(file);
        try {
            spoolDir.delete(file);
            spoolDir.delete(md5file);
            log.info("M-WRITE " + file);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            OutputStream out = new BufferedOutputStream(new DigestOutputStream(new FileOutputStream(file), digest));
            try {
                ds.writeFile(out, encParam);
                if (parser.getReadTag() == Tags.PixelData) {
                    writePixelData(in, encParam, ds, parser, out);
                    parser.parseDataset(decParam, -1);
                    ds.subSet(Tags.PixelData, -1).writeDataset(out, encParam);
                }
            } finally {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
                spoolDir.register(file);
            }
            log.info("M-WRITE " + md5file);
            Writer md5out = new FileWriter(md5file);
            try {
                md5out.write(MD5Utils.toHexChars(digest.digest()));
            } finally {
                try {
                    md5out.close();
                } catch (IOException ignore) {
                }
                spoolDir.register(md5file);
            }
            Association a = assoc.getAssociation();
            String sourceAET = a.getCallingAET();
            if (emulateMediaCreationRequestForAET(sourceAET)) {
                File f = spoolDir.getEmulateRequestFile(sourceAET, ds.getString(Tags.PatientID), ds.getString(Tags.IssuerOfPatientID));
                boolean firstObject;
                if (firstObject = !f.exists()) {
                    File dir = f.getParentFile();
                    if (dir.mkdirs()) log.info("M-WRITE " + dir);
                }
                FileWriter fw = new FileWriter(f, true);
                try {
                    if (firstObject) {
                        fw.write(lookupMediaWriterName(a.getCalledAET()) + '\n');
                    }
                    fw.write(cuid + '\t' + iuid + '\n');
                } finally {
                    fw.close();
                    log.info((firstObject ? "M-WRITE " : "M-UPDATE ") + f);
                }
            }
        } catch (Throwable t) {
            log.error("Processing Failure during receive of instance[uid=" + iuid + "]:", t);
            spoolDir.delete(md5file);
            spoolDir.delete(file);
            throw new DcmServiceException(Status.ProcessingFailure, t);
        } finally {
            in.close();
        }
    }

    private void writePixelData(InputStream in, DcmEncodeParam encParam, Dataset ds, DcmParser parser, OutputStream out) throws IOException {
        int len = parser.getReadLength();
        byte[] buffer = new byte[bufferSize];
        if (encParam.encapsulated) {
            ds.writeHeader(out, encParam, Tags.PixelData, VRs.OB, -1);
            parser.parseHeader();
            while (parser.getReadTag() == Tags.Item) {
                len = parser.getReadLength();
                ds.writeHeader(out, encParam, Tags.Item, VRs.NONE, len);
                copy(in, out, len, buffer);
                parser.parseHeader();
            }
            ds.writeHeader(out, encParam, Tags.SeqDelimitationItem, VRs.NONE, 0);
        } else {
            ds.writeHeader(out, encParam, Tags.PixelData, parser.getReadVR(), len);
            copy(in, out, len, buffer);
        }
    }

    private void checkDataset(Command rqCmd, Dataset ds, DcmParser parser) throws DcmServiceException {
        for (int i = 0; i < TYPE1_ATTR.length; ++i) {
            if (!ds.containsValue(TYPE1_ATTR[i])) {
                throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Missing Type 1 Attribute " + Tags.toString(TYPE1_ATTR[i]));
            }
        }
        if (!rqCmd.getAffectedSOPInstanceUID().equals(ds.getString(Tags.SOPInstanceUID))) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "SOP Instance UID in Dataset differs from Affected SOP Instance UID");
        }
        if (!rqCmd.getAffectedSOPClassUID().equals(ds.getString(Tags.SOPClassUID))) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "SOP Class UID in Dataset differs from Affected SOP Class UID");
        }
        if (parser.getReadTag() != Tags.PixelData) return;
        final int alloc = ds.getInt(Tags.BitsAllocated, 8);
        if (alloc != 8 && alloc != 16) throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Illegal Value of Bits Allocated: " + alloc);
        final int pixelDataLength = parser.getReadLength();
        if (pixelDataLength == -1) return;
        final int rows = ds.getInt(Tags.Rows, 0);
        final int columns = ds.getInt(Tags.Columns, 0);
        final int frames = ds.getInt(Tags.NumberOfFrames, 1);
        final int samples = ds.getInt(Tags.SamplesPerPixel, 1);
        if (rows * columns * frames * samples * alloc / 8 > pixelDataLength) {
            throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError, "Pixel Data Length[" + pixelDataLength + "] < Rows[" + rows + "]xColumns[" + columns + "]xFrames[" + frames + "]xSamples[" + samples + "]xBytes[" + (alloc / 8) + "]");
        }
        if (!ds.containsValue(Tags.PatientID)) {
            final String pname = ds.getString(Tags.PatientName);
            if (generatePatientID == null) {
                log.warn("Receive object without Patient ID with Patient Name - " + pname + " -> Creation of Media containing this object will fail!");
            } else {
                String pid = generatePatientID(ds);
                ds.putLO(Tags.PatientID, pid);
                ds.putLO(Tags.IssuerOfPatientID, issuerOfPatientID);
                log.info("Receive object without Patient ID with Patient Name - " + pname + " -> add generated Patient ID - " + pid);
            }
        }
    }

    private String generatePatientID(Dataset ds) {
        int left = generatePatientID.indexOf('#');
        if (left == -1) {
            return generatePatientID;
        }
        StringBuffer sb = new StringBuffer(generatePatientID.substring(0, left));
        String num = String.valueOf(0xffffffffL & (37 * ds.getString(Tags.PatientName, ds.getString(Tags.StudyInstanceUID)).hashCode() + ds.getString(Tags.PatientBirthDate, "").hashCode()));
        left += num.length();
        final int right = generatePatientID.lastIndexOf('#') + 1;
        while (left++ < right) {
            sb.append('0');
        }
        sb.append(num);
        sb.append(generatePatientID.substring(right));
        return sb.toString();
    }

    private void copy(InputStream in, OutputStream out, int totLen, byte[] buffer) throws IOException {
        for (int len, toRead = totLen; toRead > 0; toRead -= len) {
            len = in.read(buffer, 0, Math.min(toRead, buffer.length));
            if (len == -1) {
                throw new EOFException();
            }
            out.write(buffer, 0, len);
        }
    }
}
