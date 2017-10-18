package net.sf.aoscat.database.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;
import net.sf.aoscat.blackboard.IBlackboard;
import net.sf.aoscat.core.EDataKind;
import net.sf.aoscat.core.datastructure.CUnit;
import net.sf.aoscat.database.file.configuration.CFileBlackboardConfiguration;
import net.sf.aoscat.exceptions.CErrorException;
import net.sf.aoscat.i18n.EErrorMessages;
import net.sf.aoscat.utils.CFileUtils;
import net.sf.aoscat.utils.CStreamUtils;

/**
 * @author nightling
 *
 * The directory structure for the file plugin will be as follows:
 * <root as configured in the config class/file>/<project>/<unitType>/<unitname>/<plugin>
 * respectively <project>/meta/<plugin>
 */
@SuppressWarnings("restriction")
public class CFileBlackboard implements IBlackboard {

    private static final transient Logger logger = Logger.getLogger(CFileBlackboard.class);

    private final CFileUtils fileUtils = new CFileUtils();

    private final CFileBlackboardConfiguration config;

    private final String root;

    private final XMLOutputFactory xof = XMLOutputFactory.newInstance();

    private final BASE64Encoder encoder = new BASE64Encoder();

    /** This string is used to separate the 'file' attribute from the name of the unit. */
    public static final String FILESEPARATOR = "\t";

    /**
	 * The digest algorithm to use for directory naming / identifying a specific unit.
	 */
    public static final String DIGEST = "SHA-1";

    /** The file name that will contain the meta information on the unit*/
    public static final String INFOFILENAME = "unit.info";

    private static final CharSequence SEPARATOR = "__SEP__";

    CFileBlackboard(CFileBlackboardConfiguration config) {
        this.config = config;
        root = this.config.getOption(CFileBlackboardConfiguration.EOptions.DATAROOT) + File.separator;
    }

    @Override
    public void exportInformationFor(String aProject, String aPath) throws CErrorException {
        fileUtils.copyStreamToFile(getAllInformationForAsStream(aProject), new File(aPath));
    }

    @Override
    public void finalize(String aProject) {
    }

    @Override
    public String getAllInformationFor(String aProject) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public InputStream getAllInformationForAsStream(final String aProject) throws CErrorException {
        final PipedOutputStream vDrain = new PipedOutputStream();
        PipedInputStream vRet = null;
        try {
            vRet = new PipedInputStream(vDrain);
        } catch (IOException e) {
            throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
        }
        Thread vPlumber = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    writeDataForProjectToStream(aProject, vDrain);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        vPlumber.setName("XML Stream Adapter Thread");
        vPlumber.start();
        return vRet;
    }

    protected void writeDataForProjectToStream(String aProject, PipedOutputStream aDrain) throws XMLStreamException, CErrorException {
        XMLStreamWriter vWrite;
        try {
            vWrite = xof.createXMLStreamWriter(new OutputStreamWriter(aDrain, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
        }
        logger.info("Writing XML to Stream");
        vWrite.writeStartDocument("UTF-8", "1.0");
        vWrite.writeStartElement("project");
        vWrite.writeAttribute("name", aProject);
        writeMetaDataToStream(aProject, aDrain, vWrite);
        File vUnitRoot = new File(root + aProject + File.separator + "unit");
        logger.info("Writing unit data.. this will take a while");
        for (File vType : vUnitRoot.listFiles()) {
            for (File vUnit : vType.listFiles()) {
                vWrite.writeStartElement("unit");
                vWrite.writeAttribute("kind", vType.getName());
                String vMeta;
                try {
                    vMeta = fileUtils.readStringFromFile(vUnit.getAbsolutePath() + File.separator + INFOFILENAME);
                    String[] values = vMeta.split(FILESEPARATOR);
                    if (!values[2].equals(vType.getName())) {
                        throw new CErrorException(EErrorMessages.IMPLEMENTATIONERROR);
                    }
                    vWrite.writeAttribute("name", values[1]);
                    vWrite.writeAttribute("file", values[3]);
                    for (File vPlugin : vUnit.listFiles()) {
                        if (vPlugin.getName().equals(INFOFILENAME)) {
                            continue;
                        }
                        vWrite.writeStartElement(vPlugin.getName());
                        vWrite.writeCharacters("");
                        vWrite.flush();
                        FileInputStream fis = new FileInputStream(vPlugin);
                        final ReadableByteChannel vInputChannel = Channels.newChannel(fis);
                        final WritableByteChannel vOutputChannel = Channels.newChannel(aDrain);
                        CStreamUtils.fastChannelCopy(vInputChannel, vOutputChannel);
                        fis.close();
                        aDrain.flush();
                        vWrite.writeEndElement();
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
                } catch (IOException e) {
                    throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
                }
                vWrite.writeEndElement();
            }
        }
        vWrite.writeEndElement();
        vWrite.writeEndDocument();
        vWrite.flush();
        try {
            aDrain.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMetaDataToStream(String aProject, PipedOutputStream aDrain, XMLStreamWriter aWriter) throws XMLStreamException {
        aWriter.writeStartElement("meta");
        String vDirName = root + "meta";
        File vDir = new File(vDirName);
        for (File f : vDir.listFiles()) {
            String vPluginName = f.getName();
            logger.info("Writing Metadata for " + vPluginName);
            aWriter.writeStartElement(vPluginName);
            aWriter.writeCharacters("");
            aWriter.flush();
            aWriter.writeEndElement();
        }
        aWriter.writeEndElement();
    }

    @Override
    public CUnit getUnit(String aProject, String aName, EDataKind aKind, String aFile) throws CErrorException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void setInformationFor(String aProject, String aPluginName, String aName, EDataKind aKind, String aFile, String aXMLData) throws CErrorException {
        if (aName.contains(FILESEPARATOR)) {
            throw new CErrorException(EErrorMessages.COULDNOTIMPORTDATA, aProject, aPluginName, aName, aKind.value(), aFile);
        }
        String vDirName = getUnitDir(aProject, aKind, aName, aFile);
        fileUtils.createDirectoryIfNotExistent(vDirName);
        String vFileName = vDirName + File.separator + aPluginName;
        String vInfoFile = vDirName + File.separator + INFOFILENAME;
        try {
            fileUtils.saveStringToFile(aXMLData, vFileName);
            fileUtils.saveStringToFile(createInfoString(aProject, aName, aKind, aFile), vInfoFile);
        } catch (IOException e) {
            throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
        }
    }

    private String createInfoString(String aProject, String aName, EDataKind aKind, String aFile) {
        return aProject + FILESEPARATOR + aName + FILESEPARATOR + aKind.value() + FILESEPARATOR + aFile;
    }

    private String getUnitDir(String aProject, EDataKind aKind, String aName, String aFile) {
        String vStatic = root + aProject + File.separator + "unit" + File.separator + aKind.value() + File.separator;
        String vDynamic = aName + aFile;
        if (aFile != null && aFile.length() != 0) {
            vDynamic += "/" + aFile;
        }
        MessageDigest m;
        try {
            m = MessageDigest.getInstance(DIGEST);
            vDynamic = encoder.encode(m.digest(vDynamic.getBytes())).replace(File.separator, SEPARATOR);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        return vStatic + vDynamic;
    }

    @Override
    public void setMetaInformationFor(String aProjectName, String aPluginName, String aXMLData) throws CErrorException {
        String vDirName = root + "meta";
        fileUtils.createDirectoryIfNotExistent(vDirName);
        String vFileName = vDirName + File.separator + aPluginName;
        try {
            fileUtils.saveStringToFile(aXMLData, vFileName);
        } catch (IOException e) {
            throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, e);
        }
    }

    @Override
    public void startTransaction(String aProject, String aPluginName) {
        String vProjectPath = root + aProject;
        try {
            fileUtils.createDirectoryIfNotExistent(vProjectPath);
        } catch (CErrorException e) {
            e.printStackTrace();
        }
        logger.info("Clearing data from previous runs of " + aPluginName);
        try {
            this.walkDirectory(new File(vProjectPath), aPluginName);
        } catch (CErrorException e) {
            e.printStackTrace();
        }
    }

    private void walkDirectory(File aDir, String aPluginName) throws CErrorException {
        if (aDir == null || !aDir.exists() || !aDir.isDirectory()) {
            throw new CErrorException(EErrorMessages.UNEXPECTEDERROR, "Implementation error! " + aDir + " invalid");
        }
        for (File vChild : aDir.listFiles()) {
            if (vChild.isFile() && vChild.getName().equals(aPluginName)) {
                if (!vChild.delete()) {
                    throw new CErrorException(EErrorMessages.COULDNOTDELETE, vChild.getAbsolutePath());
                }
            } else if (vChild.isDirectory()) {
                walkDirectory(vChild, aPluginName);
            }
        }
    }
}
