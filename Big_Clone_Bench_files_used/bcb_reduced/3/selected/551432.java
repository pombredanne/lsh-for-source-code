package com.quikj.server.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * 
 * @author amit
 */
public class AceLicenseManager {

    private String errorMessage = "";

    private static AceLicenseManager instance = null;

    private HashMap featureList = new HashMap();

    private String encryptionSecret = null;

    private String absPath = null;

    public static final String ROOT_NODE_NAME = "license";

    public static final String EL_CODE = "code";

    public static final String ATT_CODE = "value";

    public static final String EL_FEATURE = "feature";

    public static final String ATT_NAME = "name";

    public static final String ATT_EXPIRES = "expires";

    public static final String ATT_UNITS = "units";

    public static final String ATT_CHECKSUM = "checksum";

    public static final String ATT_INDEX = "index";

    public AceLicenseManager(String path) throws FileNotFoundException, IOException, SAXException, AceException, ParserConfigurationException {
        absPath = path;
        loadLicenseFile();
        instance = this;
    }

    /** Creates a new instance of AceLicenseManager */
    public AceLicenseManager(String dir, String file) throws FileNotFoundException, IOException, SAXException, AceException, ParserConfigurationException {
        this(AceConfigTableFileParser.getAcePath(AceConfigTableFileParser.LOCAL_DATA, dir, file));
    }

    public static String computeChecksum(String name, int units, Date date, int index, String secret) throws AceException {
        String date_s = null;
        if (date != null) {
            date_s = date.toString();
        } else {
            date_s = "null";
        }
        String str = secret + name + index + units + date_s + secret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] output = md.digest(str.getBytes());
            return Encryption.byteToString(output);
        } catch (NoSuchAlgorithmException ex) {
            throw new AceException("Checksum compute failed : " + ex.getMessage());
        }
    }

    public static AceLicenseManager getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        try {
            AceLicenseManager a = new AceLicenseManager(args[0]);
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + " : " + ex.getMessage());
        }
        System.exit(0);
    }

    public static Date processDate(String date) {
        StringTokenizer tokens = new StringTokenizer(date, "-");
        try {
            int num = tokens.countTokens();
            if (num != 3) {
                return null;
            }
            int month = Calendar.JANUARY + (Integer.parseInt(tokens.nextToken()) - 1);
            int day = Integer.parseInt(tokens.nextToken());
            int year = Integer.parseInt(tokens.nextToken());
            Calendar cal = Calendar.getInstance();
            cal.setLenient(false);
            try {
                cal.set(year, month, day, 0, 0, 0);
            } catch (Exception ex) {
                return null;
            }
            return cal.getTime();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String computeChecksum(String name, int units, Date date, int index) throws AceException {
        return computeChecksum(name, units, date, index, encryptionSecret);
    }

    public synchronized boolean consumeUnits(String feature, int units) {
        AceFeatureLicenseElement element = findFeature(feature);
        if (element == null) {
            errorMessage = "The feature \"" + feature + "\" was not found";
            return false;
        }
        ArrayList list = element.getElementList();
        int size = list.size();
        int count = 0;
        Date cur_time = new Date();
        for (int i = 0; i < size; i++) {
            AceLicenseElement fe = (AceLicenseElement) list.get(i);
            Date expire_date = fe.getFeatureExpires();
            if (expire_date != null) {
                if (cur_time.getTime() >= expire_date.getTime()) {
                    continue;
                }
            }
            int units_assigned = fe.getUnitsAssigned();
            if (units_assigned > 0) {
                count += units_assigned;
            } else {
                count = -1;
                break;
            }
        }
        if (count == 0) {
            errorMessage = "The feature \"" + feature + "\" has expired";
            return false;
        } else if (count == -1) {
            return true;
        } else {
            int units_consumed = element.getUnitsConsumed();
            if (units_consumed + units > count) {
                errorMessage = "The feature \"" + feature + "\" has exceeded the licensed quota";
                return false;
            }
            element.incrementUnitsConsumed(units);
            return true;
        }
    }

    public AceFeatureLicenseElement findFeature(String feature) {
        return (AceFeatureLicenseElement) featureList.get(feature);
    }

    /**
	 * Getter for property errorMessage.
	 * 
	 * @return Value of property errorMessage.
	 */
    public java.lang.String getErrorMessage() {
        return errorMessage;
    }

    public HashMap getFeatureList() {
        return featureList;
    }

    public synchronized boolean licenseFeature(String feature) {
        return consumeUnits(feature, 0);
    }

    public void loadLicenseFile() throws FileNotFoundException, IOException, SAXException, AceException, ParserConfigurationException {
        featureList.clear();
        File file = new File(absPath);
        FileInputStream fis = new FileInputStream(file);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        DocumentBuilder doc_builder = dbf.newDocumentBuilder();
        Document doc = doc_builder.parse(fis);
        processDoc(doc);
        fis.close();
    }

    private void processCode(Node node) throws AceException {
        String code_s = AceXMLHelper.getXMLAttribute(node, ATT_CODE);
        if (code_s == null) {
            throw new AceException("The syntax for " + EL_CODE + " is incorrect");
        }
        try {
            Encryption decryptor = new Encryption();
            encryptionSecret = decryptor.decrypt(code_s, SysParam.getAuth1(getClass()));
            if (encryptionSecret == null) {
                throw new AceException("Invalid code specified");
            }
        } catch (Exception ex) {
            throw new AceException(ex.getClass().getName() + " occured : " + ex.getMessage());
        }
    }

    private void processDoc(Node doc) throws AceException {
        if (doc.getNodeType() != Node.DOCUMENT_NODE) {
            throw new AceException("Document does not begin with an XML node");
        }
        Node root_element;
        boolean root_found = false;
        for (root_element = doc.getFirstChild(); root_element != null; root_element = root_element.getNextSibling()) {
            if (root_element.getNodeType() == Node.ELEMENT_NODE) {
                if (root_element.getNodeName().equals(ROOT_NODE_NAME) == false) {
                    throw new AceException("Root node name " + root_element.getNodeName() + " must be " + ROOT_NODE_NAME);
                }
                root_found = true;
                break;
            }
        }
        if (root_found == false) {
            throw new AceException("Root node " + ROOT_NODE_NAME + " not found");
        }
        HashMap index_map = new HashMap();
        Node ele_node;
        for (ele_node = root_element.getFirstChild(); ele_node != null; ele_node = ele_node.getNextSibling()) {
            if (ele_node.getNodeType() == Node.ELEMENT_NODE) {
                String name = ele_node.getNodeName();
                if (name.equals(EL_CODE) == true) {
                    processCode(ele_node);
                } else if (name.equals(EL_FEATURE) == true) {
                    processFeature(ele_node, index_map);
                }
            }
        }
        String error = validateParams();
        if (error != null) {
            throw new AceException(error);
        }
    }

    private void processFeature(Node node, HashMap map) throws AceException {
        if (encryptionSecret == null) {
            throw new AceException("The code element must be specified first");
        }
        String[] attributes = { ATT_NAME, ATT_INDEX, ATT_UNITS, ATT_EXPIRES, ATT_CHECKSUM };
        String[] attrib_values = AceXMLHelper.getXMLAttributes(node, attributes);
        if ((attrib_values[0] == null) || (attrib_values[1] == null) || (attrib_values[4] == null)) {
            throw new AceException("Either " + ATT_NAME + "," + ATT_INDEX + " and/or " + ATT_CHECKSUM + " not specified");
        }
        String name = attrib_values[0];
        int index = -1;
        try {
            index = Integer.parseInt(attrib_values[1]);
        } catch (NumberFormatException ex) {
            throw new AceException("Feature " + name + " has invalid value for " + ATT_INDEX);
        }
        if (map.get(new Integer(index)) != null) {
            throw new AceException("Feature " + name + " has a duplicate index");
        }
        int units = -1;
        if (attrib_values[2] != null) {
            try {
                units = Integer.parseInt(attrib_values[2]);
            } catch (NumberFormatException ex) {
                throw new AceException("Feature " + name + " has invalid value for " + ATT_UNITS);
            }
        }
        Date date = null;
        if (attrib_values[3] != null) {
            date = processDate(attrib_values[3]);
            if (date == null) {
                throw new AceException("Feature " + name + " has invalid value for " + ATT_EXPIRES);
            }
        }
        if (computeChecksum(name, units, date, index).equals(attrib_values[4]) == false) {
            throw new AceException("Feature " + name + " has invalid checksum");
        }
        AceFeatureLicenseElement fle = (AceFeatureLicenseElement) featureList.get(name);
        if (fle != null) {
            AceLicenseElement ale = new AceLicenseElement(name, units, date);
            fle.addElement(ale);
        } else {
            fle = new AceFeatureLicenseElement();
            AceLicenseElement ale = new AceLicenseElement(name, units, date);
            fle.addElement(ale);
            featureList.put(name, fle);
        }
        map.put(new Integer(index), new Integer(index));
    }

    public synchronized void returnUnits(String feature, int units) {
        AceFeatureLicenseElement element = findFeature(feature);
        if (element != null) {
            element.decrementUnitsConsumed(units);
        }
    }

    private String validateParams() {
        if (encryptionSecret == null) {
            return "Code element not specified";
        }
        return null;
    }
}
