package com.kni.etl;

import java.io.File;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.KETLCluster;
import com.kni.etl.ketl.exceptions.KETLException;
import com.kni.etl.util.XMLHelper;

/**
 * The Class XMLMetadataBridge.
 * 
 * @author nwakefield To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class XMLMetadataBridge implements XMLMetadataCalls {

    /**
	 * The Class MetadataConnectionException.
	 */
    class MetadataConnectionException extends Exception {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 8981416986333722268L;

        /**
		 * Instantiates a new metadata connection exception.
		 * 
		 * @param string
		 *            the string
		 */
        public MetadataConnectionException(String string) {
            super(string);
        }

        /**
		 * Instantiates a new metadata connection exception.
		 * 
		 * @param cause
		 *            the cause
		 */
        public MetadataConnectionException(Throwable cause) {
            super(cause);
        }
    }

    /**
	 * Get job errors if any as XML from the metadata <ETL><JOB ID="?" PROJECT="?" STATUS="> <ERRORS> <ERROR
	 * TIMESTAMP="" CODE="">Message</ERROR> </ERRORS> </JOB> </ETL>.
	 */
    private static int maxRows = 500;

    /** The md cache. */
    private static HashMap mdCache = new HashMap();

    /** The doc builder. */
    private static DocumentBuilder mDocBuilder;

    /** The lock. */
    private static Object mLock = new Object();

    /** The Constant REQUEST_CANCEL. */
    private static final String REQUEST_CANCEL = "Cancel";

    /** The Constant REQUEST_EXECUTE. */
    private static final String REQUEST_EXECUTE = "Execute";

    /** The Constant REQUEST_FAIL. */
    private static final String REQUEST_FAIL = "Fail";

    /** The Constant REQUEST_PAUSE. */
    private static final String REQUEST_PAUSE = "Pause";

    /** The Constant REQUEST_SKIP. */
    private static final String REQUEST_SKIP = "Skip";

    /** The Constant REQUEST_SUCCESS. */
    private static final String REQUEST_SUCCESS = "Success";

    /** The Constant ROOTNODE_TAG. */
    private static final String ROOTNODE_TAG = "ETL";

    /** The xml config. */
    private static Document xmlConfig = null;

    /**
	 * Adds the child element.
	 * 
	 * @param target
	 *            the target
	 * @param newTag
	 *            the new tag
	 * 
	 * @return the element
	 */
    private static Element addChildElement(Node target, String newTag) {
        Document doc = target.getOwnerDocument();
        Element e = doc.createElement(newTag);
        target.appendChild(e);
        return e;
    }

    /**
	 * Configure.
	 * 
	 * @param pKETLPath
	 *            the KETL path
	 * @param pKETLConfigFile
	 *            the KETL config file
	 * 
	 * @throws Exception
	 *             the exception
	 */
    public static void configure(String pKETLPath, String pKETLConfigFile) throws Exception {
        if (pKETLConfigFile == null) throw new Exception("Connection to KETL metadata not possible, as KETL path is null");
        if (pKETLConfigFile == null) throw new Exception("Connection to KETL metadata not possible, as configuration file is null");
        System.out.println("Using the following KETL config file: " + pKETLPath + File.separator + pKETLConfigFile);
        XMLMetadataBridge.xmlConfig = Metadata.LoadConfigFile(pKETLPath, pKETLPath + File.separator + pKETLConfigFile);
        XMLMetadataBridge.mDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    /**
	 * Gets the new document.
	 * 
	 * @return the new document
	 */
    private static synchronized Element getNewDocument() {
        Document doc = XMLMetadataBridge.mDocBuilder.newDocument();
        Element e = doc.createElement(XMLMetadataBridge.ROOTNODE_TAG);
        doc.appendChild(e);
        doc.setXmlStandalone(true);
        return e;
    }

    /** The default date format. */
    private SimpleDateFormat mDefaultDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");

    /**
	 * Instantiates a new XML metadata bridge.
	 */
    public XMLMetadataBridge() {
        super();
    }

    /**
	 * Updates the KETLServers.xml file on the application server to include another server entry the XML in the file
	 * looks like <SERVERS DEFAULTSERVER="test"> <SERVER NAME="test"> <USERNAME>ketlmd</USERNAME> <PASSWORD>ketlmd</PASSWORD>
	 * <NETWORKNAME>localhost</NETWORKNAME> <DRIVER>org.postgresql.Driver</DRIVER> <PRESQL>set search_path = 'ketlmd'</PRESQL>
	 * <URL>jdbc:postgresql://localhost/postgres?prepareThreshold=1</URL> <MDPREFIX></MDPREFIX>
	 * <PASSPHRASE>ZATXO+7vBD7k9uicS/JOlBtsuscFIA8bpWBHZcHYNrc=</PASSPHRASE> </SERVER> </SERVERS> Return true for
	 * success else false
	 * 
	 * @param pUsername
	 *            the username
	 * @param pPassword
	 *            the password
	 * @param pJDBCDriver
	 *            the JDBC driver
	 * @param pURL
	 *            the URL
	 * @param pMDPrefix
	 *            the MD prefix
	 * @param pPassphrase
	 *            the passphrase
	 * 
	 * @return true, if add server
	 */
    public boolean addServer(String pUsername, String pPassword, String pJDBCDriver, String pURL, String pMDPrefix, String pPassphrase) {
        return false;
    }

    /**
	 * Adds the XML error node.
	 * 
	 * @param parent
	 *            the parent
	 * @param err
	 *            the err
	 * 
	 * @return the element
	 */
    private Element addXMLErrorNode(Element parent, ETLJobError err) {
        Element e = XMLMetadataBridge.addChildElement(parent, "ERROR");
        e.setAttribute("DATETIME", err.getDate() == null ? "" : this.mDefaultDateFormat.format(err.getDate()));
        e.setAttribute("CODE", err.getCode());
        e.setAttribute("MESSAGE", err.getMessage());
        e.setAttribute("DETAILS", err.getDetails());
        e.setAttribute("STEP_NAME", err.getStepName());
        return e;
    }

    /**
	 * Adds the XML job execution node.
	 * 
	 * @param parent
	 *            the parent
	 * @param j
	 *            the j
	 * 
	 * @return the element
	 */
    private Element addXMLJobExecutionNode(Element parent, ETLJob j) {
        Element e = XMLMetadataBridge.addChildElement(parent, "JOB");
        e.setAttribute("ID", j.getJobID());
        e.setAttribute("NAME", j.getName());
        e.setAttribute("TYPE", j.getJobTypeName());
        e.setAttribute("EXECUTION_ID", String.valueOf(j.getJobExecutionID()));
        e.setAttribute("EXECUTION_DATE", j.getStatus().getExecutionDate() == null ? "" : this.mDefaultDateFormat.format(j.getStatus().getExecutionDate()));
        e.setAttribute("END_DATE", j.getStatus().getEndDate() == null ? "" : this.mDefaultDateFormat.format(j.getStatus().getEndDate()));
        int statusCode = j.getStatus().getStatusCode();
        e.setAttribute("STATUS_ID", Integer.toString(statusCode));
        e.setAttribute("STATUS_TEXT", j.getStatus().getStatusMessageForCode(statusCode));
        e.setAttribute("SERVER_ID", String.valueOf(j.jsStatus.getServerID()));
        return e;
    }

    /**
	 * Adds the XML load node.
	 * 
	 * @param parent
	 *            the parent
	 * @param load
	 *            the load
	 * 
	 * @return the element
	 */
    private Element addXMLLoadNode(Element parent, ETLLoad load) {
        Element ld = XMLMetadataBridge.addChildElement(parent, "LOAD");
        ld.setAttribute("START_JOB_ID", load.start_job_id);
        ld.setAttribute("LOAD_ID", Integer.toString(load.LoadID));
        ld.setAttribute("FAILED", load.failed ? "TRUE" : "FALSE");
        ld.setAttribute("IGNORED_PARENTS", load.ignored_parents ? "TRUE" : "FALSE");
        ld.setAttribute("PROJECT_ID", Integer.toString(load.project_id));
        ld.setAttribute("RUNNING", load.running ? "TRUE" : "FALSE");
        ld.setAttribute("START_DATE", load.start_date == null ? "" : this.mDefaultDateFormat.format(load.start_date));
        ld.setAttribute("END_DATE", load.end_date == null ? "" : this.mDefaultDateFormat.format(load.end_date));
        return ld;
    }

    /**
	 * Creates the XML message.
	 * 
	 * @param msg
	 *            the msg
	 * 
	 * @return the element
	 */
    Element createXMLMessage(String msg) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element e = XMLMetadataBridge.addChildElement(root, "MESSAGE");
        e.setTextContent(msg);
        return root;
    }

    /**
	 * Delete the specified load from the current job_error_hist, job_log_hist and load table return true for success
	 * else false.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pLoadID
	 *            the load ID
	 * 
	 * @return true, if delete load
	 */
    public boolean deleteLoad(String pServerID, String pLoadID) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                boolean isSuccessful = md.deleteLoad(pLoadID);
                return isSuccessful;
            }
        } catch (Exception e1) {
            this.handleError(e1);
            return false;
        }
    }

    /**
	 * Execute a job immediately Returns "success" for "failure".
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pProjectID
	 *            the project ID
	 * @param pJobID
	 *            the job ID
	 * @param pIgnoreDependencies
	 *            the ignore dependencies
	 * @param pAllowMultiple
	 *            the allow multiple
	 * 
	 * @return the string
	 */
    public String executeJob(String pServerID, int pProjectID, String pJobID, boolean pIgnoreDependencies, boolean pAllowMultiple) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                boolean isSuccessful = md.executeJob(pProjectID, pJobID, pIgnoreDependencies, pAllowMultiple) != -1;
                if (isSuccessful) return "success"; else return "failure";
            }
        } catch (Exception e1) {
            this.handleError(e1);
            return "failure";
        }
    }

    /**
	 * Gets the current DB time stamp.
	 * 
	 * @param pServerID
	 *            the server ID
	 * 
	 * @return the current DB time stamp
	 */
    public String getCurrentDBTimeStamp(String pServerID) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                Date currDate = md.getCurrentDBTimeStamp();
                return currDate == null ? "" : this.mDefaultDateFormat.format(currDate);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getExecutionErrors(String pServerID, int pLoadID, int pExecID, Date pLastModified) {
        Element root = XMLMetadataBridge.getNewDocument();
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLLoad[] loads = md.getLoads(pLastModified, pLoadID);
                for (ETLLoad load : loads) {
                    Element e0 = this.addXMLLoadNode(root, load);
                    ETLJob[] jobs = md.getExecutionDetails(pLastModified, pLoadID, pExecID);
                    for (ETLJob job : jobs) {
                        Element e1 = this.addXMLJobExecutionNode(e0, job);
                        int execID = job.getJobExecutionID();
                        ETLJobError[] errors = md.getExecutionErrors(pLastModified, execID, XMLMetadataBridge.maxRows);
                        for (ETLJobError err : errors) {
                            this.addXMLErrorNode(e1, err);
                        }
                    }
                }
                return XMLHelper.outputXML(root, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getJob(String pServerID, int pProjectID, String pJobID) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJob j = md.getJob(pJobID);
                if (j == null) return null;
                Element e = XMLMetadataBridge.getNewDocument();
                j.getXMLJobDefinition(e);
                return XMLHelper.outputXML(e, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getJobErrors(String pServerID, String pJobName, Date pLastModified) {
        Element root = XMLMetadataBridge.getNewDocument();
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLLoad[] loads = md.getJobLoads(pLastModified, pJobName);
                for (ETLLoad load : loads) {
                    Element e0 = this.addXMLLoadNode(root, load);
                    ETLJob[] jobs = md.getExecutionDetails(pLastModified, load.LoadID, load.jobExecutionID);
                    for (ETLJob job : jobs) {
                        Element e1 = this.addXMLJobExecutionNode(e0, job);
                        int execID = job.getJobExecutionID();
                        ETLJobError[] errors = md.getExecutionErrors(pLastModified, execID, XMLMetadataBridge.maxRows);
                        for (ETLJobError err : errors) {
                            this.addXMLErrorNode(e1, err);
                        }
                    }
                }
                return XMLHelper.outputXML(root, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    /**
	 * Gets the jobs as XML.
	 * 
	 * @param root
	 *            the root
	 * @param jobs
	 *            the jobs
	 * @param pGetStatus
	 *            the get status
	 */
    private void getJobsAsXML(Element root, ETLJob[] jobs, boolean pGetStatus) {
        for (ETLJob j : jobs) {
            Element e = XMLMetadataBridge.addChildElement(root, "JOB");
            e.setAttribute("ID", j.getJobID());
            e.setAttribute("NAME", j.getName());
            e.setAttribute("PROJECT", j.getProjectName());
            e.setAttribute("TYPE", j.getJobTypeName());
            if (pGetStatus) {
                Element status = XMLMetadataBridge.addChildElement(e, "STATUS");
                status.setAttribute("START_DATE", this.mDefaultDateFormat.format(j.getStatus().getStartDate()));
                status.setAttribute("END_DATE", j.getStatus().getEndDate() == null ? "" : this.mDefaultDateFormat.format(j.getStatus().getEndDate()));
                status.setAttribute("EXECUTION_DATE", j.getStatus().getExecutionDate() == null ? "" : this.mDefaultDateFormat.format(j.getStatus().getExecutionDate()));
                status.setAttribute("EXECUTION_ID", String.valueOf(j.getJobExecutionID()));
                int statusCode = j.getStatus().getStatusCode();
                status.setAttribute("STATUS_ID", Integer.toString(statusCode));
                status.setAttribute("STATUS_TEXT", j.getStatus().getStatusMessageForCode(statusCode));
                status.setTextContent(j.getStatus().getExtendedMessage());
            }
            e.setAttribute("SECONDS_BEFORE_RETRY", Integer.toString(j.getSecondsBeforeRetry()));
            e.setAttribute("RETRY_ATTEMPTS", Integer.toString(j.getMaxRetries()));
            e.setAttribute("DESCRIPTION", j.getDescription());
            if (j.isAlertingDisabled()) {
                e.setAttribute("DISABLE_ALERTING", "Y");
            } else {
                e.setAttribute("DISABLE_ALERTING", "N");
            }
            for (String[] element0 : j.dependencies) {
                Element deps;
                if (element0[1] == Metadata.DEPENDS_ON) {
                    deps = XMLMetadataBridge.addChildElement(e, "DEPENDS_ON");
                } else {
                    deps = XMLMetadataBridge.addChildElement(e, "WAITS_ON");
                }
                deps.setTextContent(element0[0]);
            }
        }
    }

    public String getJobStatus(String pServerID, int pJobExecutionID) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                return "";
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getLoadErrors(String pServerID, int pLoadID, Date pLastModified) {
        Element root = XMLMetadataBridge.getNewDocument();
        String MSG_TOO_LARGE = "FILE TOO LARGE";
        int MAX_ROWS = 1000;
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                int rowCnt = 0;
                ETLLoad[] loads = md.getLoads(pLastModified, pLoadID);
                rowCnt += loads.length;
                if (rowCnt > MAX_ROWS) {
                    Element msgXML = this.createXMLMessage(MSG_TOO_LARGE);
                    return XMLHelper.outputXML(msgXML, true);
                }
                for (ETLLoad load : loads) {
                    Element e0 = this.addXMLLoadNode(root, load);
                    ETLJob[] jobs = md.getLoadJobs(pLastModified, pLoadID);
                    rowCnt += jobs.length;
                    if (rowCnt > MAX_ROWS) {
                        Element msgXML = this.createXMLMessage(MSG_TOO_LARGE);
                        return XMLHelper.outputXML(msgXML, true);
                    }
                    for (ETLJob job : jobs) {
                        Element e1 = this.addXMLJobExecutionNode(e0, job);
                        int execID = job.getJobExecutionID();
                        ETLJobError[] errors = md.getExecutionErrors(pLastModified, execID, XMLMetadataBridge.maxRows);
                        rowCnt += errors.length;
                        if (rowCnt > MAX_ROWS) {
                            Element msgXML = this.createXMLMessage(MSG_TOO_LARGE);
                            return XMLHelper.outputXML(msgXML, true);
                        }
                        for (ETLJobError err : errors) {
                            this.addXMLErrorNode(e1, err);
                        }
                    }
                }
                return XMLHelper.outputXML(root, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getLoadJobs(String pServerID, int pLoadID, Date pLastModified) {
        Element root = XMLMetadataBridge.getNewDocument();
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJob[] jobs = md.getLoadJobs(pLastModified, pLoadID);
                this.getJobsAsXML(root, jobs, true);
            }
            return XMLHelper.outputXML(root, true);
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getLoads(String pServerID, Date pLastModified) {
        Element e = XMLMetadataBridge.getNewDocument();
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLLoad[] loads = md.getLoads(pLastModified, -1);
                Element loadsRoot = XMLMetadataBridge.addChildElement(e, "LOADS");
                for (ETLLoad element : loads) {
                    Element ld = XMLMetadataBridge.addChildElement(loadsRoot, "LOAD");
                    ld.setAttribute("START_JOB_ID", element.start_job_id);
                    ld.setAttribute("LOAD_ID", Integer.toString(element.LoadID));
                    ld.setAttribute("FAILED", element.failed ? "TRUE" : "FALSE");
                    ld.setAttribute("IGNORED_PARENTS", element.ignored_parents ? "TRUE" : "FALSE");
                    ld.setAttribute("PROJECT_ID", Integer.toString(element.project_id));
                    ld.setAttribute("RUNNING", element.running ? "TRUE" : "FALSE");
                    ld.setAttribute("START_DATE", this.mDefaultDateFormat.format(element.start_date));
                    ld.setAttribute("END_DATE", element.end_date == null ? "" : this.mDefaultDateFormat.format(element.end_date));
                }
            }
            return XMLHelper.outputXML(e, true);
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    /**
	 * Create a project lock in the metadata with a timeout according to the value in the system.xml for the object
	 * specified return an alphanumeric string as the lockid or -1 if lock not available
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pProjectID
	 *            the project ID
	 * @param pForceOverwrite
	 *            the force overwrite
	 * 
	 * @return the lock
	 */
    public int getLock(String pServerID, String pProjectID, boolean pForceOverwrite) {
        return -1;
    }

    /**
	 * Gets the metadata by server.
	 * 
	 * @param pServerID
	 *            the server ID
	 * 
	 * @return the metadata by server
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private Metadata getMetadataByServer(String pServerID) throws Exception {
        if (pServerID == null) {
            throw new KETLException("Server ID cannot be null");
        }
        if (XMLMetadataBridge.mdCache.containsKey(pServerID)) {
            ResourcePool.setMetadata((Metadata) XMLMetadataBridge.mdCache.get(pServerID));
            return ResourcePool.getMetadata();
        }
        if (XMLMetadataBridge.xmlConfig == null) {
            throw new KETLException("KETL path or KETL servers file not found");
        }
        int pos1 = pServerID.indexOf("@");
        int pos2 = pServerID.indexOf("@", pos1 + 1);
        String serverName = pServerID.substring(0, pos1);
        String clientHashedUser = pServerID.substring(pos1 + 1, pos2);
        String clientHashedPwsd = pServerID.substring(pos2 + 1);
        Node serverNode = XMLHelper.findElementByName(XMLMetadataBridge.xmlConfig, "SERVER", "NAME", serverName);
        if (serverNode == null) {
            throw new KETLException("ERROR: Problems getting server name, check config file");
        }
        String[] mdUser = new String[5];
        mdUser[0] = XMLHelper.getChildNodeValueAsString(serverNode, "USERNAME", null, null, null);
        mdUser[1] = XMLHelper.getChildNodeValueAsString(serverNode, "PASSWORD", null, null, null);
        mdUser[2] = XMLHelper.getChildNodeValueAsString(serverNode, "URL", null, null, null);
        mdUser[3] = XMLHelper.getChildNodeValueAsString(serverNode, "DRIVER", null, null, null);
        mdUser[4] = XMLHelper.getChildNodeValueAsString(serverNode, "MDPREFIX", null, null, "");
        String passphrase = XMLHelper.getChildNodeValueAsString(serverNode, "PASSPHRASE", null, null, null);
        String serverHashedUser = this.hashMD5(mdUser[0]);
        String serverHashedPswd = this.hashMD5(mdUser[1]);
        if (!clientHashedUser.equals(serverHashedUser) || !clientHashedPwsd.equals(serverHashedPswd)) {
            throw new KETLException("Invalid UserID and/or Password for " + serverName);
        }
        Metadata md = null;
        String mdPrefix = null;
        if ((mdUser != null) && (mdUser.length == 5)) {
            mdPrefix = mdUser[4];
        }
        md = new Metadata(true, passphrase);
        try {
            md.setRepository(mdUser[0], mdUser[1], mdUser[2], mdUser[3], mdPrefix);
            XMLMetadataBridge.mdCache.put(pServerID, md);
            ResourcePool.setMetadata(md);
            return ResourcePool.getMetadata();
        } catch (Exception e) {
            throw new MetadataConnectionException("Server connect failed: " + e.getMessage());
        }
    }

    /**
	 * Hash M d5.
	 * 
	 * @param strToHash
	 *            the str to hash
	 * 
	 * @return the string
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private String hashMD5(String strToHash) throws Exception {
        try {
            byte[] bHash = new byte[strToHash.length() * 2];
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(strToHash.getBytes("UTF-16LE"));
            bHash = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (byte element : bHash) {
                String strTemp = Integer.toHexString(element);
                hexString.append(strTemp.replaceAll("f", ""));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException duu) {
            throw new Exception("NoSuchAlgorithmException: " + duu.getMessage());
        }
    }

    public String getProjectJobs(String pServerID, int pProjectID, Date pLastModified) {
        Element root = XMLMetadataBridge.getNewDocument();
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJob[] jobs = md.getProjectJobs(pLastModified, pProjectID);
                this.getJobsAsXML(root, jobs, false);
            }
            return XMLHelper.outputXML(root, true);
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getProjects(String pServerID, Date pLastModified) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Element root = XMLMetadataBridge.getNewDocument();
                Metadata md = this.getMetadataByServer(pServerID);
                Element e = XMLMetadataBridge.addChildElement(root, "PROJECTS");
                Object[] result = md.getProjects();
                for (Object element : result) {
                    Element p = XMLMetadataBridge.addChildElement(e, "PROJECT");
                    Object[] tmp = (Object[]) element;
                    p.setAttribute("ID", ((Integer) tmp[0]).toString());
                    p.setAttribute("NAME", (String) tmp[1]);
                }
                return XMLHelper.outputXML(root, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getServerClusterDetails(String pRootServerID) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pRootServerID);
                KETLCluster kc = md.getClusterDetails();
                return kc.getAsXML();
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String getConnected(String pServerID) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element servers = XMLMetadataBridge.addChildElement(root, "SERVERS");
        try {
            this.getMetadataByServer(pServerID);
            Element server = XMLMetadataBridge.addChildElement(servers, "SERVER");
            int pos1 = pServerID.indexOf("@");
            String serverName = pServerID.substring(0, pos1);
            server.setTextContent(serverName);
            return XMLHelper.outputXML(root, true);
        } catch (Exception e) {
            return this.handleError(e);
        }
    }

    public String getServerList(String clientHashedUser, String clientHashedPwsd) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element servers = XMLMetadataBridge.addChildElement(root, "SERVERS");
        Iterator it = XMLMetadataBridge.mdCache.keySet().iterator();
        while (it.hasNext()) {
            String pServerID = (String) it.next();
            int pos1 = pServerID.indexOf("@");
            int pos2 = pServerID.indexOf("@", pos1 + 1);
            String serverName = pServerID.substring(0, pos1);
            String serverHashedUser = pServerID.substring(pos1 + 1, pos2);
            String serverHashedPswd = pServerID.substring(pos2 + 1);
            if (clientHashedUser.equals(serverHashedUser) && clientHashedPwsd.equals(serverHashedPswd)) {
                Element server = XMLMetadataBridge.addChildElement(servers, "SERVER");
                server.setTextContent(serverName);
            }
        }
        return XMLHelper.outputXML(root, true);
    }

    /**
	 * Handle error.
	 * 
	 * @param e
	 *            the e
	 * 
	 * @return the string
	 */
    public String handleError(Exception e) {
        Element e1 = XMLMetadataBridge.getNewDocument();
        XMLMetadataBridge.addChildElement(e1, "ERROR").setTextContent(e.getMessage());
        if (e instanceof PassphraseException) {
            System.out.println(e.getMessage());
            System.out.println("Passphrase location: " + ((PassphraseException) e).getPassphraseFilePath());
        } else if (e instanceof MetadataConnectionException) {
            System.out.println(e.getMessage());
        } else e.printStackTrace();
        return XMLHelper.outputXML(e1, true);
    }

    /**
	 * Get a summary of load status changes for jobs after the last refresh date <ETL><JOB ID="?" STATUS=""> <JOB
	 * ID="?" STATUS=""> </ETL>.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pLoadID
	 *            the load ID
	 * @param pLastRefreshDate
	 *            the last refresh date
	 * 
	 * @return the string
	 */
    public String refreshLoadStatus(String pServerID, int pLoadID, Date pLastRefreshDate) {
        return null;
    }

    /**
	 * Refresh a project lock to extend the lock timeout.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pLockID
	 *            the lock ID
	 * 
	 * @return true, if refresh lock
	 */
    public boolean refreshLock(String pServerID, int pLockID) {
        return false;
    }

    /**
	 * Get a summary of new or modified jobs after the last refresh date <ETL><JOB ID="?" STATUS=""> <JOB ID="?"
	 * STATUS=""> </ETL>.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pProjectID
	 *            the project ID
	 * @param pLastRefreshDate
	 *            the last refresh date
	 * 
	 * @return the string
	 */
    public String refreshProjectStatus(String pServerID, String pProjectID, Date pLastRefreshDate) {
        return null;
    }

    /**
	 * Release a project lock in the metadata.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pLockID
	 *            the lock ID
	 */
    public void releaseLock(String pServerID, int pLockID) {
        return;
    }

    /**
	 * Removes the specified server id from the KETLServers.xml file on the applications server
	 * 
	 * @param pServerID
	 *            the server ID
	 * 
	 * @return true, if remove server
	 */
    public boolean removeServer(String pServerID) {
        return false;
    }

    /**
	 * Call the metadata library to add an entry to the schedule table return the new schedule id.
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pProjectID
	 *            the project ID
	 * @param pJobID
	 *            the job ID
	 * @param pMonth
	 *            the month
	 * @param pMonthOfYear
	 *            the month of year
	 * @param pDay
	 *            the day
	 * @param pDayOfWeek
	 *            the day of week
	 * @param pDayOfMonth
	 *            the day of month
	 * @param pHour
	 *            the hour
	 * @param pHourOfDay
	 *            the hour of day
	 * @param pMinute
	 *            the minute
	 * @param pMinuteOfHour
	 *            the minute of hour
	 * @param pDescription
	 *            the description
	 * @param pOnceOnlyDate
	 *            the once only date
	 * @param pEnableDate
	 *            the enable date
	 * @param pDisableDate
	 *            the disable date
	 * 
	 * @return the string
	 */
    public String scheduleJob(String pServerID, int pProjectID, String pJobID, int pMonth, int pMonthOfYear, int pDay, int pDayOfWeek, int pDayOfMonth, int pHour, int pHourOfDay, int pMinute, int pMinuteOfHour, String pDescription, Date pOnceOnlyDate, Date pEnableDate, Date pDisableDate) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element eSchedID = XMLMetadataBridge.addChildElement(root, "SCHEDULE_ID");
        Element eError = XMLMetadataBridge.addChildElement(root, "ERROR");
        eSchedID.setTextContent("-1");
        Calendar cal = java.util.Calendar.getInstance();
        cal.set(1900, 01, 01);
        if (pOnceOnlyDate == null || pOnceOnlyDate.before(cal.getTime())) pOnceOnlyDate = null;
        if (pEnableDate == null || pEnableDate.before(cal.getTime())) pEnableDate = null;
        if (pDisableDate == null || pDisableDate.before(cal.getTime())) pDisableDate = null;
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJobSchedule sched = new ETLJobSchedule(pMonth, pMonthOfYear, pDay, pDayOfWeek, pDayOfMonth, pHour, pHourOfDay, pMinute, pMinuteOfHour, pDescription, pOnceOnlyDate, pEnableDate, pDisableDate);
                String err = sched.validateSchedule();
                if (err.length() == 0) {
                    int sched_id = md.scheduleJob(pJobID, sched);
                    eSchedID.setTextContent(Integer.toString(sched_id));
                } else eError.setTextContent(err);
            }
            return XMLHelper.outputXML(root, true);
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String setExecutionStatus(String pServerID, int pLoadID, int pExecID, String pStatus) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element eStatus = XMLMetadataBridge.addChildElement(root, "STATUS");
        eStatus.setTextContent("Failed");
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJob[] jobs = md.getExecutionDetails(null, pLoadID, pExecID);
                if (jobs.length == 1) {
                    ETLJob job = jobs[0];
                    int currJobStatus = job.getStatus().getStatusCode();
                    switch(currJobStatus) {
                        case ETLJobStatus.WAITING_FOR_CHILDREN:
                        case ETLJobStatus.WAITING_TO_BE_RETRIED:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_EXECUTE)) job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_PAUSE)) job.getStatus().setStatusCode(ETLJobStatus.WAITING_TO_PAUSE);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_SKIP)) job.getStatus().setStatusCode(ETLJobStatus.WAITING_TO_SKIP);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_CANCEL)) {
                                job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_CANCELLED);
                            }
                            break;
                        case ETLJobStatus.WAITING_TO_PAUSE:
                        case ETLJobStatus.WAITING_TO_SKIP:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_EXECUTE)) job.getStatus().setStatusCode(ETLJobStatus.WAITING_FOR_CHILDREN);
                            break;
                        case ETLJobStatus.EXECUTING:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_CANCEL)) job.getStatus().setStatusCode(ETLJobStatus.ATTEMPT_CANCEL);
                            break;
                        case ETLJobStatus.PAUSED:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_EXECUTE)) job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_SKIP)) job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_SKIP);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_SUCCESS)) job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_FAIL)) job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_FAILED);
                            break;
                        case ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL:
                        case ETLJobStatus.SUCCESSFUL:
                        case ETLJobStatus.PENDING_CLOSURE_SKIP:
                        case ETLJobStatus.SKIPPED:
                        case ETLJobStatus.PENDING_CLOSURE_CANCELLED:
                        case ETLJobStatus.CANCELLED:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_EXECUTE)) job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                            break;
                        case ETLJobStatus.FAILED:
                        case ETLJobStatus.PENDING_CLOSURE_FAILED:
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_SUCCESS)) job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL);
                            if (pStatus.equals(XMLMetadataBridge.REQUEST_EXECUTE)) job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                            break;
                        default:
                            break;
                    }
                    if (job.getStatus().getStatusCode() != currJobStatus) {
                        md.setJobStatus(job);
                        eStatus.setTextContent("Success");
                    } else eStatus.setTextContent("ERROR: This action is not allowed. The status of this job may have already changed.");
                } else eStatus.setTextContent("ERROR: Could not find execution details.");
            }
            return XMLHelper.outputXML(root, true);
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    public String setJobStatus(String pServerID, String pProjectID, String pJobID, int pLoadID, int pJobExecutionID, String pState) {
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                ETLJob j = md.getJob(pJobID, pLoadID, pJobExecutionID);
                if (j == null) return "Job could not be found";
                if (j.getStatus() == null) return "Job is not current in the job queue";
                int status_id = -1;
                if (pState.equalsIgnoreCase("PAUSE")) status_id = ETLJobStatus.PAUSED; else if (pState.equalsIgnoreCase("RESUME")) status_id = ETLJobStatus.QUEUED_FOR_EXECUTION; else if (pState.equalsIgnoreCase("SKIP")) status_id = ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL; else if (pState.equalsIgnoreCase("CANCEL")) status_id = ETLJobStatus.PENDING_CLOSURE_CANCELLED; else if (pState.equalsIgnoreCase("RESTART")) status_id = ETLJobStatus.READY_TO_RUN;
                if (status_id == -1) return "Invalid status";
                if (j.getStatus() == null) return "Job is not current in the job queue";
                j.getStatus().setStatusCode(status_id);
                md.setJobStatus(j);
                return "Status changed";
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }

    /**
	 * Updates the metdata associated with pServerID, pProjectID with the new pJobXML. The existing pJobXML is compared
	 * against the supplied if the modification date of the destination is greater than the one supplied then an
	 * exception is raised Returns an XML document as string confirming change. <ETL><JOB ID="?" PROJECT="?"
	 * SUCCESS="TRUE|FALSE"/></ETL>
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param pProjectID
	 *            the project ID
	 * @param pJobXML
	 *            the job XML
	 * @param pForceOverwrite
	 *            the force overwrite
	 * 
	 * @return the string
	 */
    public String updateJob(String pServerID, String pProjectID, String pJobXML, boolean pForceOverwrite) {
        return null;
    }

    /**
	 * Dao Nguyen 2007-01-24 This function mimics the Console's importjobs() and importParameters() functions. TODO:
	 * These should really go into a class that can be called upon by both the Console and the GUI. But for now, we
	 * don't want to touch the Console. NOTE: XML reader usually read top-down. If the parameter-list nodes comes after
	 * the job nodes, this code could have problem...
	 * 
	 * @param pServerID
	 *            the server ID
	 * @param xmlFile
	 *            the xml file
	 * 
	 * @return the string
	 */
    public String addJobsAndParams(String pServerID, String xmlFile) {
        Element root = XMLMetadataBridge.getNewDocument();
        Element eStatus = XMLMetadataBridge.addChildElement(root, "STATUS");
        eStatus.setTextContent("Failed");
        try {
            synchronized (XMLMetadataBridge.mLock) {
                Metadata md = this.getMetadataByServer(pServerID);
                if (xmlFile == null) eStatus.setTextContent("ERROR: No filename given.");
                DocumentBuilder builder = null;
                Document jobNodes = null;
                try {
                    DocumentBuilderFactory dmf = DocumentBuilderFactory.newInstance();
                    builder = dmf.newDocumentBuilder();
                    jobNodes = builder.parse(new InputSource(new StringReader(xmlFile)));
                    ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "WARNING: Any duplicate parameters will be given the last value found");
                    NodeList nlp = jobNodes.getElementsByTagName("PARAMETER_LIST");
                    for (int i = 0; i < nlp.getLength(); i++) {
                        Node parameterList = nlp.item(i);
                        md.importParameterList(parameterList);
                    }
                    NodeList nlj = jobNodes.getElementsByTagName("JOB");
                    for (int i = 0; i < nlj.getLength(); i++) {
                        Node job = nlj.item(i);
                        md.importJob(job);
                    }
                    eStatus.setTextContent("Success");
                } catch (org.xml.sax.SAXException e) {
                    ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Parsing XML document, " + e.toString());
                    eStatus.setTextContent("ERROR parsing XML document:  " + e.toString());
                }
                return XMLHelper.outputXML(root, true);
            }
        } catch (Exception e1) {
            return this.handleError(e1);
        }
    }
}
