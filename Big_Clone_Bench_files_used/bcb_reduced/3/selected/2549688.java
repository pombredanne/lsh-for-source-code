package de.ibis.permoto.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.xml.sax.SAXException;
import de.ibis.permoto.model.basic.applicationmodel.ApplicationModel;
import de.ibis.permoto.model.basic.predictionbusinesscase.PredictionBusinessCase;
import de.ibis.permoto.model.basic.scenario.ArrivalRate;
import de.ibis.permoto.model.basic.scenario.BusinessCase;
import de.ibis.permoto.model.basic.scenario.Cell;
import de.ibis.permoto.model.basic.scenario.Cells;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.model.basic.scenario.ClassPart;
import de.ibis.permoto.model.basic.scenario.ClassSection;
import de.ibis.permoto.model.basic.scenario.Connection;
import de.ibis.permoto.model.basic.scenario.Connections;
import de.ibis.permoto.model.basic.scenario.DistributionParameter;
import de.ibis.permoto.model.basic.scenario.ModellingGuiSection;
import de.ibis.permoto.model.basic.scenario.PerMoToObjectFactory;
import de.ibis.permoto.model.basic.scenario.ServiceTime;
import de.ibis.permoto.model.basic.scenario.ServiceTimes;
import de.ibis.permoto.model.basic.scenario.Station;
import de.ibis.permoto.model.basic.scenario.StationSection;
import de.ibis.permoto.model.basic.types.DistributionNameType;
import de.ibis.permoto.model.basic.types.DropRuleType;
import de.ibis.permoto.model.basic.types.QueueingDisciplineType;
import de.ibis.permoto.model.basic.types.RoutingAlgorithmType;
import de.ibis.permoto.model.basic.types.StationTypeType;
import de.ibis.permoto.model.definitions.IBusinessCase;
import de.ibis.permoto.model.definitions.IPredictionBusinessCase;
import de.ibis.permoto.model.definitions.impl.PerMoToBusinessCase;
import de.ibis.permoto.model.definitions.impl.PerMoToPredictionBusinessCase;

/**
 * Provides methods to load and save {@link BusinessCase} objects. Note: The
 * load and save functionalities are based on JAXB
 * @author Andreas Schamberger
 * @author Oliver H�hn
 * @author Slavko Segota
 */
public class ModelPersistenceManager {

    /** xml schema file for {@link ApplicationModel} */
    public static URL APPLICATION_MODEL_SCHEMA = ModelPersistenceManager.class.getResource("/de/ibis/permoto/model/schema/PerMoToApplicationModel.xsd");

    /** xml schema file for {@link BusinessCase} */
    public static URL BUSINESS_CASE_SCHEMA = ModelPersistenceManager.class.getResource("/de/ibis/permoto/model/schema/PerMoToBusinessCase.xsd");

    /** xml schema file for {@link PredictionBusinessCase} */
    public static URL PREDICTION_BUSINESS_CASE_SCHEMA = ModelPersistenceManager.class.getResource("/de/ibis/permoto/model/schema/PerMoToPredictionBusinessCase.xsd");

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(ModelPersistenceManager.class);

    /** pmt file filter */
    public static PerMoToFileFilter PERMOTO_FILE_FILTER = new PerMoToFileFilter(".pmt", "PerMoTo business case");

    /** ppt file filter */
    public static PerMoToFileFilter PERMOTO_PREDICTION_FILE_FILTER = new PerMoToFileFilter(".ppm", "PerMoTo precition business case");

    /**
	 * Returns the MD5 checksum for the specified file.
	 * @param filename the file to calculate the checksum for
	 * @return String the calculated MD5 checksum, empty String if Exception
	 *         thrown
	 */
    public static String getMD5Checksum(String filename) {
        try {
            InputStream fis = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            byte[] b = complete.digest();
            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        } catch (FileNotFoundException fnf) {
            logger.warn("FileNotFoundException thrown: " + fnf.getLocalizedMessage());
            return "";
        } catch (NoSuchAlgorithmException nsa) {
            logger.warn("NoSuchAlgorithmException thrown: " + nsa.getLocalizedMessage());
            return "";
        } catch (IOException io) {
            logger.warn("IOException thrown: " + io.getLocalizedMessage());
            return "";
        }
    }

    /**
	 * Returns a sample {@link BusinessCase}.
	 * @return BusinessCase the sample business case
	 * @deprecated marked by Olli - this sample BC is not maintained any longer
	 */
    public static BusinessCase getSampleBusinessCase() {
        PerMoToObjectFactory pof = new PerMoToObjectFactory();
        BusinessCase businessCase = pof.createBusinessCase();
        StationSection sd = pof.createStationSection();
        businessCase.setStationSection(sd);
        List<Station> stationList = sd.getStation();
        Station station;
        station = pof.createStation("Source1", StationTypeType.SOURCE, 1, false, QueueingDisciplineType.FCFS);
        stationList.add(station);
        station = pof.createStation("WebServer", StationTypeType.SERVER, 2, true, QueueingDisciplineType.FCFS, 100, 1.0f);
        stationList.add(station);
        station = pof.createStation("ApplicationServer", StationTypeType.SERVER, 2, true, QueueingDisciplineType.FCFS, 100, 1.0f);
        stationList.add(station);
        station = pof.createStation("Database", StationTypeType.SERVER, 2, true, QueueingDisciplineType.FCFS, 100, 1.0f);
        stationList.add(station);
        station = pof.createStation("Sink1", StationTypeType.SINK, 1, false, QueueingDisciplineType.FCFS);
        stationList.add(station);
        ClassSection cd = new ClassSection();
        cd.setAreClassesCoupled(false);
        businessCase.setClassSection(cd);
        List<Class> classList = cd.getClazz();
        Class c = pof.createClass("cart.do", "Source1", true);
        classList.add(c);
        List<ClassPart> classPartList = c.getClassParts().getClassPart();
        ClassPart cp;
        ServiceTimes std;
        List<ServiceTime> stList;
        ServiceTime st;
        cp = pof.createClassPart("Source1", RoutingAlgorithmType.RANDOM_ROUTING);
        classPartList.add(cp);
        cp.setOutgoingConnections(pof.createOutgoingConnections("WebServer"));
        cp = pof.createClassPart("WebServer", DropRuleType.DROP, RoutingAlgorithmType.RANDOM_ROUTING);
        classPartList.add(cp);
        std = pof.createServiceTimes(0.0, 100.0);
        cp.setServiceTimes(std);
        stList = std.getServiceTime();
        st = pof.createServiceTime(DistributionNameType.EXPONENTIAL, pof.createDistributionParameter("lambda", "0.0010"));
        stList.add(st);
        cp.setOutgoingConnections(pof.createOutgoingConnections("ApplicationServer"));
        cp = pof.createClassPart("ApplicationServer", DropRuleType.DROP, RoutingAlgorithmType.RANDOM_ROUTING);
        classPartList.add(cp);
        std = pof.createServiceTimes(0.0, 100.0);
        cp.setServiceTimes(std);
        stList = std.getServiceTime();
        st = pof.createServiceTime(DistributionNameType.EXPONENTIAL, pof.createDistributionParameter("lambda", "0.0017"));
        stList.add(st);
        cp.setOutgoingConnections(pof.createOutgoingConnections("Database"));
        cp = pof.createClassPart("Database", DropRuleType.DROP, RoutingAlgorithmType.RANDOM_ROUTING);
        classPartList.add(cp);
        std = pof.createServiceTimes();
        cp.setServiceTimes(std);
        stList = std.getServiceTime();
        st = pof.createServiceTime(DistributionNameType.EXPONENTIAL, pof.createDistributionParameter("lambda", "0.0020"));
        stList.add(st);
        cp.setOutgoingConnections(pof.createOutgoingConnections("Sink1"));
        cp = pof.createClassPart("Sink1");
        classPartList.add(cp);
        ArrivalRate ar = pof.createArrivalRate(DistributionNameType.EXPONENTIAL, pof.createDistributionParameter("lambda", "10"));
        c.setArrivalRate(ar);
        ModellingGuiSection mgd = pof.createModellingGuiSection();
        businessCase.setModellingGuiSection(mgd);
        Cells cells = pof.createCells();
        mgd.setCells(cells);
        List<Cell> cellList = cells.getCell();
        cellList.add(pof.createCell("Source1", "Source", -1, 22, 64));
        cellList.add(pof.createCell("Sink1", "Sink", -1, 579, 64));
        cellList.add(pof.createCell("WebServer", "Station", 0, 120, 61));
        cellList.add(pof.createCell("ApplicationServer", "Station", 1, 280, 61));
        cellList.add(pof.createCell("Database", "Station", 2, 441, 61));
        Connections connections = pof.createConnections();
        mgd.setConnections(connections);
        List<Connection> conList = connections.getConnection();
        conList.add(pof.createConnection("Source1", "WebServer"));
        conList.add(pof.createConnection("WebServer", "ApplicationServer"));
        conList.add(pof.createConnection("ApplicationServer", "Database"));
        conList.add(pof.createConnection("Database", "Sink1"));
        return businessCase;
    }

    /**
	 * Returns a textual representation of the given {@link BusinessCase}.
	 * @param businessCase the BusinessCase to generate the representation for.
	 * @return String the textual representation
	 */
    public static String getTextRepresentationOfBusinessCase(BusinessCase businessCase) {
        String output = "";
        StationSection sd = businessCase.getStationSection();
        if (null != sd) {
            output += getTextRepresentationOfStationSection(sd);
        }
        ClassSection cd = businessCase.getClassSection();
        if (null != cd) {
            output += getTextRepresentationOfClassSection(cd);
        }
        ModellingGuiSection mgd = businessCase.getModellingGuiSection();
        if (null != mgd) {
            output += getTextRepresentationOfModellingGuiSection(mgd);
        }
        return output;
    }

    /**
	 * Returns a textual representation of the given {@link ClassSection}.
	 * @param cd the ClassSection to generate the representation for.
	 * @return String the textual representation
	 */
    private static String getTextRepresentationOfClassSection(ClassSection cd) {
        String output = "";
        output += "\n\n";
        output += "ClassSection: (areClassesCoupled: " + cd.isAreClassesCoupled() + ")\n";
        output += "======================================================\n";
        List<Class> classList = cd.getClazz();
        for (Class clazz : classList) {
            output += "classID:\t\t" + clazz.getClassID() + "\n";
            output += "referenceSource:\t" + clazz.getReferenceSource() + "\n";
            output += "openClass:\t\t" + clazz.isOpenClass() + "\n";
            output += "population:\t\t" + clazz.getPopulation() + "\n";
            output += "thinktime:\t\t" + clazz.getThinktime() + "\n";
            output += "priority:\t\t" + clazz.getPriority() + "\n";
            output += "classPart:\n\n";
            List<ClassPart> classPartList = clazz.getClassParts().getClassPart();
            for (ClassPart classPart : classPartList) {
                output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
                output += "\tstationID:\t\t" + classPart.getStationID() + "\n";
                output += "\tdropRule:\t\t" + classPart.getDropRule() + "\n";
                output += "\troutingAlgorithm:\t" + classPart.getRoutingAlgorithm() + "\n";
                ServiceTimes std = classPart.getServiceTimes();
                if (null != std) {
                    output += "\tServiceTimes: (loadRangeLowerLimit: " + std.getServiceTime().get(0).getLoadRangeLowerLimit() + "; loadRangeUpperLimit: " + std.getServiceTime().get(0).getLoadRangeUpperLimit() + ")\n\n";
                    List<ServiceTime> stList = std.getServiceTime();
                    int i = 0;
                    for (ServiceTime st : stList) {
                        output += "\t\tServiceTime #" + i + ": (distributionName: " + st.getDistributionName() + ")\n";
                        output += "\t\t----------------------------------------------\n";
                        List<DistributionParameter> dpList = st.getDistributionParameter();
                        for (DistributionParameter dp : dpList) {
                            output += "\t\tname:\t\t" + dp.getName() + "\n";
                            output += "\t\tvalue:\t\t" + dp.getValue() + "\n";
                            output += "\t\t----------------------------------------------\n";
                        }
                        i++;
                    }
                }
                output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n";
            }
            ArrivalRate arrivalRate = clazz.getArrivalRate();
            if (null != arrivalRate) {
                output += "arrivalRate: (distributionName: " + arrivalRate.getDistributionName() + ")\n";
                output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
                List<DistributionParameter> dpList = arrivalRate.getDistributionParameter();
                for (DistributionParameter dp : dpList) {
                    output += "\tname:\t\t" + dp.getName() + "\n";
                    output += "\tvalue:\t\t" + dp.getValue() + "\n";
                    output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
                }
            }
            output += "------------------------------------------------------\n";
        }
        output += "======================================================\n";
        return output;
    }

    /**
	 * Returns a textual representation of the given {@link ModellingGuiSection}
	 * .
	 * @param mgs the ModellingGuiSection to generate the representation for.
	 * @return String the textual representation
	 */
    private static String getTextRepresentationOfModellingGuiSection(ModellingGuiSection mgs) {
        String output = "";
        output += "\n\n";
        output += "ModellingGuiSection:\n";
        output += "======================================================\n";
        Cells cells = mgs.getCells();
        if (null != cells) {
            output += "Cells:\n";
            output += "\ttype:\tcount:\tposX:\tposY:\tname:\n";
            output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
            List<Cell> cellList = cells.getCell();
            for (Cell cell : cellList) {
                output += "\t" + cell.getType() + "\t" + cell.getCount() + "\t" + cell.getPosX() + "\t" + cell.getPosY() + "\t" + cell.getName() + "\n";
            }
        }
        Connections cons = mgs.getConnections();
        if (null != cons) {
            output += "Connections:\n";
            output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
            List<Connection> conList = cons.getConnection();
            for (Connection con : conList) {
                output += "\tsource:\t\t" + con.getSource() + "\n";
                output += "\ttarget:\t\t" + con.getTarget() + "\n";
                output += "\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n";
            }
        }
        output += "======================================================\n";
        return output;
    }

    /**
	 * Returns a textual representation of the given {@link StationSection}.
	 * @param ss the StationSection to generate the representation for.
	 * @return String the textual representation
	 */
    private static String getTextRepresentationOfStationSection(StationSection ss) {
        String output = "";
        output += "\n\n";
        output += "StationSection:\n";
        output += "======================================================\n";
        List<Station> stationList = ss.getStation();
        for (Station station : stationList) {
            output += "stationID:\t\t" + station.getStationID() + "\n";
            output += "stationType:\t\t" + station.getStationType() + "\n";
            output += "nrParallelWorkers:\t" + station.getNrParallelWorkers() + "\n";
            output += "loaddependent:\t\t" + station.isLoaddependent() + "\n";
            output += "queueLength:\t\t" + station.getQueueLength() + "\n";
            output += "\tQueueingDiscipline:\t" + station.getQueueingDiscipline() + "\n";
            output += "serviceMultiplier:\t" + station.getServiceMultiplier() + "\n";
            output += "------------------------------------------------------\n";
        }
        output += "======================================================\n";
        return output;
    }

    /**
	 * Loads an {@link ApplicationModel} from a file.
	 * @param is the input stream to the file
	 * @return ApplicationModel the loaded ApplicationModel
	 */
    public static ApplicationModel loadApplicationModel(InputStream is) {
        org.w3c.dom.Document pam = null;
        try {
            Document doc = new SAXBuilder().build(is);
            JDOMSource xmlFile = new JDOMSource(doc);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Source xsltFileSource = transformerFactory.getAssociatedStylesheet(xmlFile, null, null, null);
            DOMOutputter domOutputter = new DOMOutputter();
            if (xsltFileSource != null) {
                String xsltFileName = xsltFileSource.getSystemId().substring(xsltFileSource.getSystemId().lastIndexOf("/") + 1);
                InputStream xsltIS = new ModelPersistenceManager().getClass().getResourceAsStream("/de/ibis/permoto/loganalyzer/pam/" + xsltFileName);
                Document xsltDoc = new SAXBuilder().build(xsltIS);
                JDOMSource xsltFile = new JDOMSource(xsltDoc);
                JDOMResult xmlResult = new JDOMResult();
                Transformer transformer = transformerFactory.newTransformer(xsltFile);
                transformer.transform(xmlFile, xmlResult);
                pam = domOutputter.output(xmlResult.getDocument());
            } else {
                pam = domOutputter.output(doc);
            }
        } catch (JDOMException e) {
            logger.error("error while loading/transforming PAM file", e);
        } catch (IOException e) {
            logger.error("error while loading/transforming PAM file", e);
        } catch (TransformerConfigurationException e) {
            logger.error("error while loading/transforming PAM file", e);
        } catch (TransformerFactoryConfigurationError e) {
            logger.error("error while loading/transforming PAM file", e);
        } catch (TransformerException e) {
            logger.error("error while loading/transforming PAM file", e);
        }
        try {
            JAXBContext jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.applicationmodel");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(APPLICATION_MODEL_SCHEMA);
            unmarshaller.setSchema(schema);
            ApplicationModel applicationModel = (ApplicationModel) unmarshaller.unmarshal(pam);
            return applicationModel;
        } catch (JAXBException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (IllegalArgumentException npe) {
            logger.error("No filename specified.", npe);
        }
        return null;
    }

    /**
	 * Loads an {@link IBusinessCase} from a file.
	 * @param file the file to load from
	 * @return IBusinessCase the loaded IBusinessCase
	 */
    public static IBusinessCase loadIBusinessCaseFromFile(File file) {
        try {
            JAXBContext jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.scenario");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(BUSINESS_CASE_SCHEMA);
            unmarshaller.setSchema(schema);
            BusinessCase businessCase = (BusinessCase) unmarshaller.unmarshal(file);
            String checkSum = getMD5Checksum(file.getPath());
            logger.info("Calculated MD5 for " + file.getPath() + ": " + checkSum);
            IBusinessCase ibc = (IBusinessCase) new PerMoToBusinessCase(businessCase);
            ibc.setPmtFileCRC(checkSum);
            return ibc;
        } catch (JAXBException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (IllegalArgumentException npe) {
            logger.error("No filename specified.", npe);
        } catch (Exception e) {
            logger.error("error while trying to calculate checksum", e);
        }
        return null;
    }

    /**
	 * Loads a {@link IPredictionBusinessCase} from a file.
	 * @param file the file to load from
	 * @return IPredictionBusinessCase the loaded IPredictionBusinessCase
	 */
    public static IPredictionBusinessCase loadPredictionBusinessCaseFromFile(File file) {
        try {
            JAXBContext jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.predictionbusinesscase");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(PREDICTION_BUSINESS_CASE_SCHEMA);
            unmarshaller.setSchema(schema);
            PredictionBusinessCase predictionbusinessCase = (PredictionBusinessCase) unmarshaller.unmarshal(file);
            String checkSum = getMD5Checksum(file.getPath());
            logger.info("Calculated MD5 for " + file.getPath() + ": " + checkSum);
            IPredictionBusinessCase ipbc = new PerMoToPredictionBusinessCase(predictionbusinessCase);
            ipbc.setPpmFileCRC(checkSum);
            return (IPredictionBusinessCase) ipbc;
        } catch (JAXBException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while unmarshalling with JAXB", e);
        } catch (IllegalArgumentException npe) {
            logger.error("No filename specified.", npe);
        } catch (Exception e) {
            logger.error("error while trying to calculate checksum", e);
        }
        return null;
    }

    /**
	 * Saves a {@link BusinessCase} to the given file.
	 * @param businessCase the BusinessCase to save
	 * @param file the file to save to
	 */
    public static void saveBusinessCase(BusinessCase businessCase, File file) {
        try {
            JAXBContext jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.scenario");
            Marshaller marshaller = jc.createMarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(BUSINESS_CASE_SCHEMA);
            marshaller.setSchema(schema);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            marshaller.marshal(businessCase, new FileOutputStream(file));
        } catch (JAXBException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (FileNotFoundException e) {
            logger.error("error while marshalling with JAXB", e);
        }
    }

    /**
	 * Saves a {@link PredictionBusinessCase} to the given file.
	 * @param pbc the PredictionBusinessCase to save
	 * @param file the file to save to
	 */
    public static void savePredictionBusinessCase(PredictionBusinessCase pbc, File file) {
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.predictionbusinesscase");
            Marshaller marshaller = jc.createMarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(PREDICTION_BUSINESS_CASE_SCHEMA);
            marshaller.setSchema(schema);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            marshaller.marshal(pbc, new FileOutputStream(file));
        } catch (JAXBException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (FileNotFoundException e) {
            logger.error("error while marshalling with JAXB", e);
        }
    }

    /**
	 * Saves a {@link BusinessCase} to the given file with a schema location
	 * that matches the tomcat webapps folder for the Texo Web Service.
	 * @param businessCase the BusinessCase to save
	 * @param file the file to save to
	 */
    public static void saveTexoBusinessCase(final BusinessCase businessCase, final File file) {
        try {
            JAXBContext jc = JAXBContext.newInstance("de.ibis.permoto.model.basic.scenario");
            Marshaller marshaller = jc.createMarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File("webapps/axis/WEB-INF/classes/de/ibis/permoto/model/schema/PerMoToBusinessCase.xsd"));
            marshaller.setSchema(schema);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            marshaller.marshal(businessCase, new FileOutputStream(file));
        } catch (JAXBException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (SAXException e) {
            logger.error("error while marshalling with JAXB", e);
        } catch (FileNotFoundException e) {
            logger.error("error while marshalling with JAXB", e);
        }
    }
}
