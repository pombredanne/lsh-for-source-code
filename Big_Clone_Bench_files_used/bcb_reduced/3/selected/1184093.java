package org.dbe.kb.xdb;

import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.dbxml.XmlManager;
import com.sleepycat.dbxml.XmlManagerConfig;
import com.sleepycat.dbxml.XmlContainer;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlInputStream;
import com.sleepycat.dbxml.XmlUpdateContext;
import com.sleepycat.dbxml.XmlQueryContext;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlValue;
import java.io.File;
import java.io.FileNotFoundException;
import com.sleepycat.dbxml.XmlContainerConfig;
import com.sleepycat.dbxml.XmlDocument;
import java.io.DataInputStream;
import com.sleepycat.dbxml.XmlIndexSpecification;
import com.sleepycat.dbxml.XmlDocumentConfig;
import com.sleepycat.dbxml.XmlQueryExpression;
import java.util.Vector;
import com.sleepycat.dbxml.XmlIndexDeclaration;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import com.sleepycat.dbxml.XmlMetaDataIterator;
import com.sleepycat.dbxml.XmlMetaData;
import com.sleepycat.dbxml.XmlTransaction;
import java.security.MessageDigest;
import org.w3c.dom.Document;
import javax.xml.parsers.*;

/**
 *
 * <p>XMLDB Manager</p>
 *
 * <p>Implements the main functionality provided by the XMLDB</p>
 *
 * <p>TUC/MUSIC 2005</p>
 *
 */
public class XMLDBmanager {

    static final String _VERSION_IDENTIFIER = "VERSION_VAR";

    static final int _XDB_CACHE_SIZE = 1048576;

    int _versionCount = 0;

    XmlDocument _versionDoc = null;

    Environment _xmldbEnv = null;

    XmlManager _xmlManager = null;

    Hashtable _containers = null;

    String _envPath = null;

    public XMLDBmanager(String envPath) throws XmlException {
        System.out.println("XDB:Starting XDB Server ... ");
        initializeEnvironment(envPath);
        initializeManager();
        _containers = new Hashtable();
    }

    public void initializeVersioning() {
        try {
            DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XmlContainer vcont = getContainer(XDB._VAR_CONTAINER);
            try {
                _versionDoc = vcont.getDocument(_VERSION_IDENTIFIER);
            } catch (Exception ex) {
            }
            if (_versionDoc == null) {
                _versionCount = 0;
                _versionDoc = _xmlManager.createDocument();
                XmlDocumentConfig xdconf = new XmlDocumentConfig();
                XmlUpdateContext theContext = _xmlManager.createUpdateContext();
                _versionDoc.setName(_VERSION_IDENTIFIER);
                _versionDoc.setContent("<VERSION-MAX>" + _versionCount + "</VERSION-MAX>");
                vcont.putDocument(_versionDoc, theContext, xdconf);
                System.out.println("XDB:set version init");
            } else {
                Document doc = dbuilder.parse(new java.io.ByteArrayInputStream(_versionDoc.getContent()));
                String content = doc.getFirstChild().getFirstChild().getNodeValue();
                _versionCount = Integer.parseInt(content);
            }
            System.out.println("XDB:Versioning Initialized");
        } catch (Exception ex) {
            System.out.println("XDB:Error in Version Init:" + ex.getMessage());
        }
    }

    public int createNewVersion() {
        try {
            XmlUpdateContext uc = _xmlManager.createUpdateContext();
            _versionDoc.setContent("<VERSION-MAX>" + ++_versionCount + "</VERSION-MAX>");
            XmlContainer vcont = getContainer(XDB._VAR_CONTAINER);
            vcont.updateDocument(_versionDoc, uc);
            System.out.println("XDB:new version " + _versionCount);
        } catch (Exception ex) {
            System.out.println("XDB:new version error:" + ex.getMessage());
        }
        return _versionCount;
    }

    public void initializeContainer(String cname) {
        XmlContainer c = null;
        if (_containers.containsKey(cname)) return;
        try {
            XmlContainerConfig containerConf = new XmlContainerConfig();
            containerConf.setTransactional(true);
            containerConf.setAllowCreate(true);
            c = _xmlManager.openContainer(cname, containerConf);
            _containers.put(cname, c);
            System.out.println("XDB:Container " + cname + " ready. Enable Transactions");
            if (cname.equals(XDB._VAR_CONTAINER)) initializeVersioning();
        } catch (XmlException e) {
            System.out.println("XDB:Container " + cname + " could not be opened or created");
            e.printStackTrace(new java.io.PrintStream(System.out));
        }
    }

    public void initializeEnvironment(String envPath) {
        File envHome = new File(envPath);
        _envPath = envPath;
        try {
            EnvironmentConfig envConf = new EnvironmentConfig();
            envConf.setAllowCreate(true);
            envConf.setInitializeCache(true);
            System.out.println("XDB:Initialize System Cache");
            envConf.setCacheSize(_XDB_CACHE_SIZE);
            System.out.println("XDB:Enable Locking Subsystem");
            envConf.setInitializeLocking(true);
            System.out.println("XDB:Enable Logging Subsystem");
            envConf.setInitializeLogging(true);
            System.out.println("XDB:Start Transaction Manager");
            envConf.setTransactional(true);
            System.out.println("XDB:Check database environment..");
            System.out.println("XDB:Recovery mode = " + System.getProperty("org.dbe.kb.xmldb.recovery"));
            if (System.getProperty("org.dbe.kb.xmldb.recovery").equals("normal")) {
                System.out.println("XDB:Running Normal Recovery. Please wait ...");
                envConf.setRunRecovery(true);
            } else if (System.getProperty("org.dbe.kb.xmldb.recovery").equals("fatal")) {
                System.out.println("XDB:Running Fatal Recovery. Please wait ...");
                envConf.setRunFatalRecovery(true);
            }
            _xmldbEnv = new Environment(envHome, envConf);
            System.out.println("XDB:Database Environment Created");
        } catch (DatabaseException de) {
            de.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
    }

    public void initializeManager() throws XmlException {
        XmlManagerConfig managerConfig = new XmlManagerConfig();
        managerConfig.setAdoptEnvironment(true);
        _xmlManager = new XmlManager(_xmldbEnv, managerConfig);
    }

    public XmlContainer getContainer(String cname) {
        return (XmlContainer) _containers.get(cname);
    }

    public void deleteContainer() {
    }

    public XmlResults executeQueryOnValue(XmlValue value, String expr) throws com.sleepycat.dbxml.XmlException {
        XmlQueryContext documentContext = _xmlManager.createQueryContext();
        documentContext.setReturnType(XmlQueryContext.DeadValues);
        XmlQueryExpression xexpr = _xmlManager.prepare(expr, documentContext);
        XmlResults results = xexpr.execute(value, documentContext);
        return results;
    }

    public java.util.Collection listDocuments(String container) {
        XmlContainer cont = null;
        java.util.ArrayList list = new java.util.ArrayList();
        try {
            cont = getContainer(container);
            XmlDocumentConfig dc = new XmlDocumentConfig();
            dc.setLazyDocs(true);
            XmlResults results = cont.getAllDocuments(dc);
            XmlValue value = results.next();
            for (int i = 0; value != null; i++) {
                String[] ress = new String[2];
                XmlDocument theDoc = value.asDocument();
                ress[0] = theDoc.getName();
                XmlValue xv = new XmlValue();
                theDoc.getMetaData(XDB._DBE_NAMESPACE, XDB._SYNOPSIS_MDATA, xv);
                try {
                    ress[1] = xv.asString();
                } catch (Exception ex) {
                    System.out.println("XDB:Mdata[synopsis] for " + theDoc.getName() + " does not exist");
                    ress[1] = XDB._DESC_EMPTY;
                }
                if (ress[0].indexOf("-MOF") == -1 && ress[0].indexOf("-version") == -1 && ress[0].indexOf("-COUNTER") == -1) {
                    list.add(ress);
                }
                value = results.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    public void deleteDocument(String name, String container) {
        XmlContainer cont = null;
        try {
            cont = getContainer(container);
            XmlUpdateContext theContext = _xmlManager.createUpdateContext();
            cont.deleteDocument(name, theContext);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("XDB:Document not deleted: " + ex.getMessage());
        } finally {
            try {
                if (cont != null) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getDocument(String name, String container) {
        XmlContainer cont = null;
        String ret = null;
        try {
            cont = getContainer(container);
            XmlDocument doc = cont.getDocument(name);
            if (doc != null) {
                ret = doc.getContentAsString();
            }
        } catch (Exception ex) {
            System.out.println("XDB:Doc[" + name + "] in container " + container + " does not exist");
        } finally {
            try {
                if (cont != null) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ret;
    }

    public XmlDocument getXmlDocument(String name, XmlContainer container) {
        XmlDocument doc = null;
        try {
            doc = container.getDocument(name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return doc;
    }

    public void replaceXmlDocument(XmlDocument xdoc, String container) {
        XmlContainer myContainer = null;
        try {
            myContainer = getContainer(container);
            XmlUpdateContext uc = _xmlManager.createUpdateContext();
            myContainer.updateDocument(xdoc, uc);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (myContainer != null) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Collection getDocumentMetadata(String name, String container) {
        XmlDocument doc = null;
        XmlContainer cont = null;
        try {
            cont = getContainer(container);
            doc = cont.getDocument(name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ArrayList res = new ArrayList();
        try {
            XmlMetaDataIterator it = doc.getMetaDataIterator();
            XmlMetaData mdata = null;
            while ((mdata = it.next()) != null) {
                String[] md = new String[3];
                md[0] = mdata.get_uri();
                md[1] = mdata.get_name();
                md[2] = mdata.get_value().asString();
                System.out.println("XDB:Mdata[" + md[0] + "][" + md[1] + "=" + md[2] + "]");
                res.add(md);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public boolean documentExists(String name, String container) {
        XmlContainer cont = null;
        boolean ret = false;
        try {
            cont = getContainer(container);
            XmlDocument doc = cont.getDocument(name);
            ret = true;
        } catch (Exception ex) {
            ret = false;
        } finally {
            try {
                if (cont != null) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ret;
    }

    public void loadFile(String docName, String filename, String description, String container) {
        File f = new File(filename);
        byte[] dataByte = null;
        try {
            DataInputStream in = new DataInputStream(new java.io.FileInputStream(f));
            dataByte = new byte[(int) f.length()];
            in.read(dataByte);
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        loadDocument(docName, false, new String(dataByte), description, container);
    }

    public String updateDocument(String docName, boolean concatUniquePostfix, String doc, String desc, String container) {
        Vector v = new Vector();
        String[] mdata = new String[2];
        mdata[0] = "synopsis";
        mdata[1] = desc;
        v.add(mdata);
        return updateDocument(docName, concatUniquePostfix, doc, v, container);
    }

    public String updateDocument(String docName, boolean concatUniquePostfix, String doc, Vector metadata, String container) {
        XmlContainer myContainer = null;
        XmlDocument theDoc = null;
        try {
            myContainer = getContainer(container);
            theDoc = myContainer.getDocument(docName);
        } catch (XmlException e) {
        }
        if (theDoc == null) {
            loadDocument(docName, false, doc, metadata, container);
            return null;
        }
        if (concatUniquePostfix) {
            String peerID = "";
            if (docName.indexOf(":") != -1) {
                peerID = docName.substring(0, docName.indexOf(":"));
            }
            System.out.println("XDB:PEER=" + peerID);
            for (int i = 0; i < metadata.size(); i++) {
                String[] mdata = (String[]) metadata.elementAt(i);
                if (mdata[0].equals(XDB._PEER_ID)) {
                    if (!peerID.equals(mdata[1])) docName = mdata[1] + docName.substring(docName.indexOf(":"));
                }
            }
            return loadDocument(docName, true, doc, metadata, container);
        }
        try {
            theDoc.setContent(doc);
            String shid = SHKgenerate(doc);
            theDoc.setMetaData(XDB._DBE_NAMESPACE, XDB._SHID_MDATA, new XmlValue(shid));
            XmlUpdateContext uc = _xmlManager.createUpdateContext();
            myContainer.updateDocument(theDoc, uc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return docName;
    }

    public String loadDocument(String docName, boolean concatUniquePostfix, String doc, String description, String container) {
        Vector v = new Vector();
        String[] mdata = new String[2];
        mdata[0] = "synopsis";
        mdata[1] = description;
        v.add(mdata);
        return loadDocument(docName, concatUniquePostfix, doc, v, container);
    }

    public String loadDocument(String docName, boolean concatUniquePostfix, String doc, Vector metadata, String container) {
        XmlContainer myContainer = null;
        try {
            myContainer = getContainer(container);
            XmlUpdateContext theContext = _xmlManager.createUpdateContext();
            XmlDocument myDoc = _xmlManager.createDocument();
            XmlDocumentConfig xdconf = new XmlDocumentConfig();
            if (concatUniquePostfix) {
                myDoc.setMetaData(XDB._DBE_NAMESPACE, XDB._VERSION_PARENT, new XmlValue(docName));
                System.out.println("XDB:Add mdata[" + XDB._VERSION_PARENT + "]=" + docName);
                if (docName.indexOf("_v.") != -1) docName = docName.substring(0, docName.lastIndexOf("_v."));
                docName += "_v." + createNewVersion();
            }
            myDoc.setName(docName);
            myDoc.setMetaData(XDB._DBE_NAMESPACE, XDB._DOCUMENT_ID_MDATA, new XmlValue(docName));
            String shid = SHKgenerate(doc);
            myDoc.setMetaData(XDB._DBE_NAMESPACE, XDB._SHID_MDATA, new XmlValue(shid));
            System.out.println("XDB:Add mdata[shid]=" + shid);
            for (int i = 0; i < metadata.size(); i++) {
                String[] mdata = (String[]) metadata.elementAt(i);
                System.out.println("XDB:Add mdata[" + mdata[0] + "]=" + mdata[1]);
                myDoc.setMetaData(XDB._DBE_NAMESPACE, mdata[0], new XmlValue(mdata[1]));
            }
            myDoc.setContent(doc);
            myContainer.putDocument(myDoc, theContext, xdconf);
            System.out.println("XDB:Doc[" + myDoc.getName() + "] stored in " + container);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (myContainer != null) {
                }
            } catch (Exception ce) {
                ce.printStackTrace();
            }
        }
        return docName;
    }

    public void createIndex(String container, String indexString) {
        XmlContainer myContainer = null;
        try {
            myContainer = getContainer(container);
            XmlIndexSpecification is = myContainer.getIndexSpecification();
            int count = 0;
            if (is.getDefaultIndex().indexOf(indexString) != -1) ; else {
                is.addDefaultIndex(indexString);
                XmlUpdateContext uc = _xmlManager.createUpdateContext();
                myContainer.setIndexSpecification(is, uc);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (myContainer != null) {
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public XmlResults executeQuery(String query) throws XmlException {
        XmlQueryContext context = _xmlManager.createQueryContext();
        context.setNamespace("dbe", XDB._DBE_NAMESPACE);
        context.setNamespace("sdl", "org.omg.xmi.namespace.sdl");
        long time = System.currentTimeMillis();
        System.out.println("XDB:Processing Query");
        XmlResults results = _xmlManager.query(query, context);
        time = System.currentTimeMillis() - time;
        System.out.println("XDB:Pure Query Processing Time = " + time + "msecs");
        String message = "XDB:Query Results =  ";
        message += results.size() + " Documents";
        System.out.println(message);
        return results;
    }

    public Collection processQuery(String query) {
        ArrayList rlist = new ArrayList();
        XmlResults res = null;
        try {
            res = executeQuery(query);
            while (res.hasNext()) {
                String rstr = res.next().asString();
                rlist.add(rstr);
            }
        } catch (Exception ex) {
            System.out.println("XDB:Error in Query Processing: " + ex.getMessage());
        }
        return rlist;
    }

    public void close() {
        System.out.println("XDB:Start Normal shutdown ..");
        System.out.println("XDB:Closing all containers");
        try {
            for (Iterator it = _containers.values().iterator(); it.hasNext(); ) {
                XmlContainer cont = (XmlContainer) it.next();
                System.out.println("XDB:Close " + cont.getName());
                cont.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("XDB:Removing old log files");
        try {
            _xmldbEnv.removeOldLogFiles();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (_xmlManager != null) {
                _xmlManager.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("XDB:Server stopped.");
    }

    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private static String SHKgenerate(String source) {
        byte[] bytes = null;
        source = source.replaceAll("[\\s]+", " ");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            bytes = md.digest(source.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < bytes.length; ++idx) {
            byte b = bytes[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        try {
            XMLDBmanager m = new XMLDBmanager(args[0]);
            m.initializeContainer("test.db");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
