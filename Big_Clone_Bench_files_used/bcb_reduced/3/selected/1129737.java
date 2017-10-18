package eu.planets_project.tb.impl.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.tb.api.services.TestbedServiceTemplate;
import eu.planets_project.tb.api.services.tags.ServiceTag;
import eu.planets_project.tb.impl.persistency.ExperimentPersistencyImpl;
import eu.planets_project.tb.impl.services.tags.ServiceTagImpl;

/**
 * @author alindley
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@XmlAccessorType(XmlAccessType.FIELD)
public class TestbedServiceTemplateImpl implements TestbedServiceTemplate, java.io.Serializable, Cloneable {

    private String sServiceDescription, sServiceEndpoint, sServiceName;

    @XmlTransient
    private String sWSDLContent;

    private boolean bURIisWSICompliant;

    @XmlTransient
    @Lob
    @Column(columnDefinition = ExperimentPersistencyImpl.BLOB_TYPE)
    private Vector<ServiceOperationImpl> lAllRegisteredServiceOperations;

    @SuppressWarnings("unused")
    @Lob
    @Column(columnDefinition = ExperimentPersistencyImpl.BLOB_TYPE)
    private Vector<String> lAllOperationNamesFromWSDL;

    @Lob
    @Column(columnDefinition = ExperimentPersistencyImpl.BLOB_TYPE)
    private Vector<ServiceTagImpl> lTags;

    private Calendar deploymentDate = new GregorianCalendar();

    @Transient
    @XmlTransient
    public String DISCR_TEMPLATE = "template";

    @Transient
    @XmlTransient
    public String DISCR_EXPERIMENT = "experiment";

    @Column(name = "discr")
    private String sdiscr = DISCR_TEMPLATE;

    @Transient
    @XmlTransient
    private static Log log;

    @Column(name = "hashUUID")
    private String sServiceID;

    @Id
    @GeneratedValue
    @XmlTransient
    private long lEntityID;

    public TestbedServiceTemplateImpl() {
        log = LogFactory.getLog(this.getClass());
        sServiceDescription = "";
        sServiceEndpoint = "";
        sServiceName = "";
        sServiceID = "";
        sWSDLContent = "";
        bURIisWSICompliant = false;
        lAllOperationNamesFromWSDL = new Vector<String>();
        lAllRegisteredServiceOperations = new Vector<ServiceOperationImpl>();
        lTags = new Vector<ServiceTagImpl>();
    }

    public long getEntityID() {
        return this.lEntityID;
    }

    public void setEntityID(long entityID) {
        this.lEntityID = entityID;
    }

    public String getDescription() {
        return this.sServiceDescription;
    }

    public void setDescription(String sDescription) {
        this.sServiceDescription = sDescription;
    }

    public String getEndpoint() {
        return this.sServiceEndpoint;
    }

    public void setEndpoint(String sURL) {
        this.sServiceEndpoint = sURL;
    }

    public String getName() {
        return this.sServiceName;
    }

    public void setName(String sName) {
        this.sServiceName = sName;
    }

    /**
	 * Used in the context of: If you're missing a registered operation, use this method
	 * to see which operations are offered by the service to then possibly ask ask the TB
	 * admin to register this operation for you
	 */
    public List<String> getAllWSDLOperationNames() {
        return new Vector<String>();
    }

    /**
	 * Used in the context of: If you're missing a registered operation, use this method
	 * to see which operations are offered by the service to then possibly ask ask the TB
	 * admin to register this operation for you
	 */
    public void setAllWSDLOperationNames(List<String> operationNames) {
    }

    public void addServiceOperation(String sOperationName, String xmlRequestTemplate, String xpathtoOutput) {
        if ((sOperationName != null) && (xmlRequestTemplate != null) && (xpathtoOutput != null)) {
            ServiceOperation op = new ServiceOperationImpl(xmlRequestTemplate, xpathtoOutput);
            op.setName(sOperationName);
            addServiceOperation(op);
        }
    }

    public void addServiceOperation(ServiceOperation operation) {
        if (operation != null) {
            this.lAllRegisteredServiceOperations.add((ServiceOperationImpl) operation);
        }
    }

    public void removeServiceOperation(String sOperationName) {
        if (sOperationName != null) {
            if (this.getAllServiceOperationNames().contains(sOperationName)) {
                Iterator<ServiceOperation> it = this.getAllServiceOperations().iterator();
                boolean bFound = false;
                ServiceOperation opRet = null;
                while (it.hasNext()) {
                    ServiceOperation op = it.next();
                    if (op.getName().equals(sOperationName)) {
                        opRet = op;
                        bFound = true;
                    }
                }
                if (bFound) {
                    this.lAllRegisteredServiceOperations.remove(opRet);
                }
            }
        }
    }

    public void setServiceOperations(List<ServiceOperation> operations) {
        if (operations != null) {
            this.lAllRegisteredServiceOperations = new Vector<ServiceOperationImpl>();
            Iterator<ServiceOperation> it = operations.iterator();
            while (it.hasNext()) {
                addServiceOperation(it.next());
            }
        }
    }

    public ServiceOperation getServiceOperation(String sName) {
        if (sName != null) {
            if (this.getAllServiceOperationNames().contains(sName)) {
                Iterator<ServiceOperation> it = this.getAllServiceOperations().iterator();
                while (it.hasNext()) {
                    ServiceOperation op = it.next();
                    if (op.getName().equals(sName)) {
                        return op;
                    }
                }
            }
        }
        return null;
    }

    public boolean isEndpointWSICompliant() {
        return this.bURIisWSICompliant;
    }

    public void setEndpointWSICompliant(boolean compliant) {
        this.bURIisWSICompliant = compliant;
    }

    public String getUUID() {
        if ((this.sServiceID == null) || (this.sServiceID.length() == 0)) {
            try {
                this.sServiceID = this.generateUUID();
            } catch (Exception e) {
            }
        }
        return this.sServiceID;
    }

    public String getWSDLContent() {
        return this.sWSDLContent;
    }

    public void setWSDLContent(String content) {
        if (content != null) {
            this.sWSDLContent = content;
        }
    }

    public List<ServiceOperation> getAllServiceOperations() {
        Vector<ServiceOperation> sos = new Vector<ServiceOperation>();
        for (ServiceOperationImpl soi : this.lAllRegisteredServiceOperations) sos.add(soi);
        return sos;
    }

    public List<ServiceOperation> getAllServiceOperationsByType(String serviceOperationType) {
        List<ServiceOperation> ret = new Vector<ServiceOperation>();
        Iterator<ServiceOperation> operations = getAllServiceOperations().iterator();
        while (operations.hasNext()) {
            ServiceOperation operation = operations.next();
            if (operation.getServiceOperationType().equals(serviceOperationType)) ret.add(operation);
        }
        return ret;
    }

    public List<String> getAllServiceOperationNames() {
        List<String> lRet = new Vector<String>();
        Iterator<ServiceOperation> it = this.getAllServiceOperations().iterator();
        while (it.hasNext()) {
            lRet.add(it.next().getName());
        }
        return lRet;
    }

    public boolean isOperationRegistered(String opName) {
        if (opName != null) {
            return this.getAllServiceOperationNames().contains(opName);
        }
        return false;
    }

    public void setEndpoint(String sURL, boolean extract) {
        if (sURL != null) {
            this.setEndpoint(sURL);
            if (extract) {
                try {
                    this.extractWSDLContent(sURL);
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
            }
        }
    }

    public void addTag(ServiceTag tag) {
        if (tag != null) {
            this.removeTag(tag.getName());
            this.lTags.add((ServiceTagImpl) tag);
        }
    }

    public List<ServiceTag> getAllTags() {
        List<ServiceTag> sts = new Vector<ServiceTag>();
        for (ServiceTagImpl tag : this.lTags) sts.add(tag);
        return sts;
    }

    public ServiceTag getTag(String sTagName) {
        if (sTagName != null) {
            Iterator<ServiceTagImpl> tags = this.lTags.iterator();
            while (tags.hasNext()) {
                ServiceTag tagit = tags.next();
                if (tagit.getName().equals(sTagName)) {
                    return tagit;
                }
            }
        }
        return null;
    }

    public void removeTag(String sTagName) {
        if (sTagName != null) {
            Iterator<ServiceTagImpl> tags = this.lTags.iterator();
            boolean bFound = false;
            ServiceTag bFoundTag = null;
            while (tags.hasNext()) {
                ServiceTag tagit = tags.next();
                if (tagit.getName().equals(sTagName)) {
                    bFound = true;
                    bFoundTag = tagit;
                }
            }
            if (bFound) {
                this.lTags.remove(bFoundTag);
            }
        }
    }

    public void removeTags() {
        this.lTags = new Vector<ServiceTagImpl>();
    }

    public void extractWSDLContent(String sURL) throws FileNotFoundException, IOException, NullPointerException {
        InputStream in = null;
        try {
            in = new URL(sURL).openStream();
            boolean eof = false;
            String content = "";
            StringBuffer sb = new StringBuffer();
            while (!eof) {
                int byteValue = in.read();
                if (byteValue != -1) {
                    char b = (char) byteValue;
                    sb.append(b);
                } else {
                    eof = true;
                }
            }
            content = sb.toString();
            if (content != null) {
                this.setWSDLContent(content);
            }
        } finally {
            in.close();
        }
    }

    public void setDeploymentDate(long timeInMillis) {
        this.deploymentDate.setTimeInMillis(timeInMillis);
    }

    public Calendar getDeploymentDate() {
        return this.deploymentDate;
    }

    /**
	 * UniqueIDs are created by using an MD5 hashing on the service's WSDL content. This allows to distinguish
	 * different services with the same name and on the other hand to classify a service just on behalf of its 
	 * WSDL contract fingerprint.
	 * @throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException 
	 */
    private String generateUUID() throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
        if ((this.getWSDLContent() == null) || (this.getWSDLContent().length() == 0)) {
            if ((this.getEndpoint() != null) && (this.getEndpoint().length() != 0)) {
                this.extractWSDLContent(this.getEndpoint());
            } else {
                throw new IOException("WSDL Content could not be loaded");
            }
        }
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] result = md5.digest(this.getWSDLContent().getBytes("UTF-8"));
        return hexEncode(result);
    }

    /**
	* The byte[] returned by MessageDigest does not have a nice
	* textual representation, so some form of encoding is usually performed.
	*
	* This implementation follows the example of David Flanagan's book
	* "Java In A Nutshell", and converts a byte array into a String
	* of hex characters.
	*
	* Another popular alternative is to use a "Base64" encoding.
	*/
    private String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    public TestbedServiceTemplateImpl clone() {
        TestbedServiceTemplateImpl template = new TestbedServiceTemplateImpl();
        try {
            template = (TestbedServiceTemplateImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Error cloning TestbedServiceTemplateImpl Object" + e.toString());
        }
        return template;
    }

    /**
	 * Sets a discriminator to distinguish between
	 * a) templates that are used within an experiment and therefore cannot be deleted anymore
	 * b) templates that are displayed in the list of available TBServiceTemplates - these can also be deleted
	 * @param discr
	 */
    public void setDiscriminator(String discr) {
        if ((discr != null) && (discr.equals(this.DISCR_TEMPLATE))) {
            this.sdiscr = discr;
        }
        if ((discr != null) && (discr.equals(this.DISCR_EXPERIMENT))) {
            this.sdiscr = discr;
        }
    }

    public String getDiscriminator() {
        return this.sdiscr;
    }

    /**
	 * @author Andrew Lindley, ARC
	 *
	 */
    @Embeddable
    public class ServiceOperationImpl implements TestbedServiceTemplate.ServiceOperation, java.io.Serializable {

        private String sName = "";

        private String sDescription = "";

        private String sXMLRequestTemplate = "";

        private String sXPathToOutput = "";

        private int iMaxSupportedInputFiles = 100;

        private int iMinRequiredInputFiles = 1;

        private String sServiceType = "";

        private String sOutputOjbectType = "";

        private boolean bInputTypeIsCallByValue = true;

        private String sOutputFileType = "";

        public ServiceOperationImpl(String sXMLRequestTemplate, String sXPathToOutput) {
            if ((sXMLRequestTemplate != null) && (sXPathToOutput != null)) {
                this.setXMLRequestTemplate(sXMLRequestTemplate);
                this.setXPathToOutput(sXPathToOutput);
            }
        }

        public ServiceOperationImpl() {
        }

        public String getName() {
            return this.sName;
        }

        public void setName(String name) {
            if (name != null) this.sName = name;
        }

        public String getXMLRequestTemplate() {
            return this.sXMLRequestTemplate;
        }

        public void setXMLRequestTemplate(String template) {
            if (template != null) this.sXMLRequestTemplate = template;
        }

        public String getXPathToOutput() {
            return this.sXPathToOutput;
        }

        public void setXPathToOutput(String xpath) {
            if (xpath != null) this.sXPathToOutput = xpath;
        }

        public boolean isExecutionInformationComplete() {
            if ((sName.length() > 0) && (sXMLRequestTemplate.length() > 0) && (sXPathToOutput.length() > 0)) return true;
            return false;
        }

        public void setMaxSupportedInputFiles(int i) {
            if (i >= 1) this.iMaxSupportedInputFiles = i;
        }

        public int getMaxSupportedInputFiles() {
            return this.iMaxSupportedInputFiles;
        }

        public void setMinRequiredInputFiles(int i) {
            if (i >= 1) this.iMinRequiredInputFiles = i;
        }

        public int getMinRequiredInputFiles() {
            return this.iMinRequiredInputFiles;
        }

        public String getServiceOperationType() {
            return this.sServiceType;
        }

        public void setServiceOperationType(String sType) {
            if (sType != null) {
                this.sServiceType = sType;
            }
        }

        public String getOutputObjectType() {
            return this.sOutputOjbectType;
        }

        public void setOutputObjectType(String type) {
            if (type != null) {
                this.sOutputOjbectType = type;
            }
        }

        public String getDescription() {
            return this.sDescription;
        }

        public void setDescription(String sDescr) {
            if (sDescr != null) {
                this.sDescription = sDescr;
            }
        }

        public boolean isInputTypeCallByValue() {
            return this.bInputTypeIsCallByValue;
        }

        public void setInputTypeIsCallByReference(boolean b) {
            this.bInputTypeIsCallByValue = !b;
        }

        public void setInputTypeIsCallByValue(boolean b) {
            this.bInputTypeIsCallByValue = b;
        }

        public String getOutputFileType() {
            return this.sOutputFileType;
        }

        public void setOutputFileType(String s) {
            this.sOutputFileType = s;
        }
    }
}
