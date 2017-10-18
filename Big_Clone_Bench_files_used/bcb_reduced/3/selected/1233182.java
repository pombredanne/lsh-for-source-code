package com.entelience.test;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.junit.*;
import static org.junit.Assert.*;
import org.apache.log4j.Logger;
import com.entelience.util.Logs;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Handler;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleChain;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.attachments.Attachments;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.handlers.SimpleSessionHandler;
import org.apache.axis.transport.http.HTTPSender;
import org.apache.axis.transport.http.HTTPTransport;
import com.entelience.objects.DropDown;
import com.entelience.objects.Id;
import com.entelience.objects.LoginResult;
import com.entelience.objects.PageCounter;
import com.entelience.objects.TwoDate;
import com.entelience.objects.directory.PeopleInfoLine;
import com.entelience.objects.geography.Continent;
import com.entelience.objects.geography.Country;
import com.entelience.objects.geography.Region;
import com.entelience.objects.geography.SubContinent;
import com.entelience.objects.geography.Timezone;
import com.entelience.objects.module.ModuleDetail;
import com.entelience.objects.module.ModuleMetric;
import com.entelience.objects.raci.RaciAuthorization;
import com.entelience.directory.PeopleFactory;
import com.entelience.soap.soapBaseHelper;
import com.entelience.soap.SoapCallHelper;
import com.entelience.directory.PeopleFactory;
import com.entelience.objects.raci.RACI;
import com.entelience.raci.DbRaci;
import com.entelience.objects.raci.RaciHistory;
import com.entelience.objects.raci.RaciInfoLine;
import com.entelience.sql.Db;
import com.entelience.sql.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.entelience.test.TargetSystemTest;

/** 
 *  Base class for soap unit testing 
 */
public class SoapTestCase {

    public static final Logger _logger = Logs.getLogger();

    /** Would be nice to add some logic here to retrieve the info from 
	the esis.properties file 
    */
    public static Service service = null;

    public static String userName;

    public static String password;

    public static String company;

    public static String soapEndPoint;

    public static Integer esisId;

    /**
	 * The constructor is called before each test.
	 */
    public SoapTestCase() {
        userName = TargetSystemTest.soapUserName;
        password = TargetSystemTest.soapPassword;
        company = TargetSystemTest.soapCompany;
    }

    public void setUserName(String _userName) {
        userName = _userName;
    }

    public void setPassword(String _password) {
        password = _password;
    }

    public void setCompnay(String _company) {
        company = _company;
    }

    public void setSoapEndPoint(String _soapEndPoint) {
        soapEndPoint = _soapEndPoint;
    }

    public String getEndPoint() {
        return soapEndPoint;
    }

    /** object comparision with some sense added */
    public void assertObjectNullEquals(Object o1, Object o2) throws Exception {
        if (o1 == null && o2 == null) return;
        if (o1 == null && o2 != null) assertTrue(false);
        if (o1 != null && o2 == null) assertTrue(false);
        assertEquals(o1, o2);
    }

    /**
     * compute the md5sum for a string
     */
    public String md5sum(String toCompute) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(toCompute.getBytes());
        java.math.BigInteger hash = new java.math.BigInteger(1, md.digest());
        return hash.toString(16);
    }

    public void init() throws Exception {
        Logs.logMethodName();
        init(userName, password, company);
        setSoapEndPoint(TargetSystemTest.soapEndPoint);
    }

    public void init(String _userName, String _password) throws Exception {
        init(_userName, _password, (String) null);
    }

    public void init(String _userName, String _password, String _company) throws Exception {
        Logs.logMethodName();
        _logger.debug("SoapTestCase : setup with user (" + _userName + ") for company (" + _company + ")");
        if (SoapTestCase.service == null) SoapTestCase.service = new Service(SoapCallHelper.createClientConfig());
        LoginResult lr = logIn(_userName, _password, _company);
        assertNotNull("Null login result", lr);
        if (lr.isLoginFail()) _logger.debug("SoapTestCase : login failed for user (" + _userName + ") for company (" + _company + ")");
    }

    /**
	 * Login using the default company
	 */
    public LoginResult logIn(String _userName, String _password) throws Exception {
        return logIn(_userName, _password, (String) null);
    }

    public LoginResult logIn(String _userName, String _password, String _company) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(TargetSystemTest.soapEndPoint + "Authentication");
        registerEnum(call);
        call.setOperationName("logIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum(_password);
        String login = _userName;
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("company", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "LoginResult");
        call.registerTypeMapping(com.entelience.objects.LoginResult.class, qn, new BeanSerializerFactory(com.entelience.objects.LoginResult.class, qn), new BeanDeserializerFactory(com.entelience.objects.LoginResult.class, qn));
        call.setReturnType(qn);
        return (LoginResult) call.invoke(new Object[] { login, passwd, company });
    }

    @After
    public void tearDown() throws Exception {
        Logs.logMethodName();
        _logger.debug("Tearing down SoapTestCase");
        if (validSession()) {
            logOut();
        }
    }

    /**
	 * Sets the module permissions. This is a replicate of test00Admin
	 */
    @BeforeClass
    public static void initRacis_mydb() throws Exception {
        Logs.logMethodName();
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            _logger.debug("Unlocking all users");
            PreparedStatement pst = db.prepareStatement("UPDATE e_people SET account_locked=false");
            db.executeUpdate(pst);
            pst = db.prepareStatement("DELETE FROM e_raci WHERE e_people_id >0 AND e_raci_obj IN (SELECT e_raci_obj FROM e_module)");
            db.executeUpdate(pst);
            pst = db.prepareStatement("DELETE FROM e_raci_history WHERE e_raci_obj IN (SELECT e_raci_obj FROM e_module)");
            db.executeUpdate(pst);
            pst = db.prepareStatement("SELECT e_raci_obj, class_name FROM e_module");
            esisId = PeopleFactory.lookupUserName(db, "esis");
            Integer guestId = PeopleFactory.lookupUserName(db, "guest");
            Integer extenId = PeopleFactory.lookupUserName(db, "exten");
            assertNotNull("esisIs is null", esisId);
            assertNotNull(guestId);
            assertNotNull(extenId);
            ResultSet rs = db.executeQuery(pst);
            RACI esis = new RACI();
            esis.setA(true);
            esis.setR(true);
            esis.setUserId(esisId.intValue());
            RACI guest = new RACI();
            guest.setC(true);
            guest.setUserId(guestId.intValue());
            RACI exten = new RACI();
            exten.setI(true);
            exten.setUserId(extenId.intValue());
            while (rs.next()) {
                esis.setRaciObjectId(rs.getInt(1));
                guest.setRaciObjectId(rs.getInt(1));
                exten.setRaciObjectId(rs.getInt(1));
                String name = rs.getString(2);
                if ("com.entelience.module.Admin".equals(name) || "com.entelience.module.Portal".equals(name)) continue;
                DbRaci.addRaci(db, esis, 0);
                DbRaci.addRaci(db, guest, 0);
                if (!"com.entelience.module.Assets".equals(name)) DbRaci.addRaci(db, exten, 0);
            }
            db.commit();
        } finally {
            db.safeClose();
        }
    }

    /**
     * end of the tests : log Out
     */
    public void logOut() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(TargetSystemTest.soapEndPoint + "Directory");
        call.setOperationName("logOut");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        Boolean b;
        try {
            b = (Boolean) call.invoke(new Object[] {});
        } catch (Exception e) {
            throw (e);
        }
        assertTrue(b.booleanValue());
    }

    /**
     * checkSession validity
     */
    private boolean validSession() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(TargetSystemTest.soapEndPoint + "Authentication");
        call.setOperationName("isSessionValid");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        Boolean b = null;
        try {
            b = (Boolean) call.invoke(new Object[] {});
        } catch (Exception e) {
            throw (e);
        }
        return b.booleanValue();
    }

    public byte[] getAttachmentContent(AttachmentPart attachment) throws Exception {
        InputStream is = null;
        ByteArrayOutputStream out = null;
        try {
            DataHandler h = attachment.getDataHandler();
            is = h.getInputStream();
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[com.entelience.util.StaticConfig.ioBufferSize];
            int length = 0;
            while ((length = is.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            return out.toByteArray();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                } finally {
                    is = null;
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                } finally {
                    out = null;
                }
            }
        }
    }

    public List<AttachmentPart> getAttachments(Call call) throws Exception {
        Attachments atts = call.getMessageContext().getCurrentMessage().getAttachmentsImpl();
        assertNotNull(atts);
        List<AttachmentPart> ret = new ArrayList<AttachmentPart>();
        for (Iterator it = atts.getAttachments().iterator(); it.hasNext(); ) {
            org.apache.axis.attachments.AttachmentPart ap = (org.apache.axis.attachments.AttachmentPart) it.next();
            ret.add(ap);
        }
        return ret;
    }

    public void addFileAsAttachment(Call call, File toAttach) throws SOAPException {
        DataHandler attachmentFile = new DataHandler(new FileDataSource(toAttach));
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        AttachmentPart attachment = soapMessage.createAttachmentPart(attachmentFile);
        call.addAttachmentPart(attachment);
    }

    /**
     * Raci WS tests.
     *  should be in test13Raci, but it's easier to call them from other tests if they are here
     */
    public QName getRaciQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "RaciInfoLine");
    }

    private QName getPageCounterQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PageCounter");
    }

    private QName getPeopleInfoLineQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PeopleInfoLine");
    }

    public void registerTypeMappingForRaciException(Call call) throws Exception {
        QName qn = getRaciExceptionQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciException.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciException.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciException.class, qn));
    }

    public QName getRaciExceptionQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PeopleInfoLine");
    }

    public void registerEnum(Call call) throws Exception {
        QName qn = new QName("urn:com.entelience.soap.soapRaci", "RaciUnit");
        call.registerTypeMapping(com.entelience.objects.raci.RaciUnit.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.entelience.objects.raci.RaciUnit.class, qn), new org.apache.axis.encoding.ser.EnumDeserializerFactory(com.entelience.objects.raci.RaciUnit.class, qn));
        qn = new QName("urn:com.entelience.soap.soapRaci", "RaciObjectType");
        call.registerTypeMapping(com.entelience.objects.raci.RaciObjectType.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.entelience.objects.raci.RaciObjectType.class, qn), new org.apache.axis.encoding.ser.EnumDeserializerFactory(com.entelience.objects.raci.RaciObjectType.class, qn));
    }

    public ModuleDetail[] listActiveModules() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Module");
        call.setOperationName("listActiveModules");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleDetail");
        call.registerTypeMapping(com.entelience.objects.module.ModuleDetail.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleDetail.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleDetail.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleDetail[]) call.invoke(new Object[] {});
    }

    public PeopleInfoLine getCurrentPeople() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Preferences");
        call.setOperationName("getCurrentPeople");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "PeopleInfoLine");
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(qn);
        PeopleInfoLine pil = null;
        pil = (PeopleInfoLine) call.invoke(new Object[] {});
        return pil;
    }

    public PageCounter countRacis(Integer userId, Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("countRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPageCounterQn();
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        return (PageCounter) call.invoke(new Object[] { userId, raciObjectId, raciToSee });
    }

    public PageCounter countModuleRacis(Integer userId, Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("countModuleRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPageCounterQn();
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        return (PageCounter) call.invoke(new Object[] { userId, raciObjectId, raciToSee });
    }

    public PageCounter countMyRacis(Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Preferences");
        call.setOperationName("countMyRacis");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "PageCounter");
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        return (PageCounter) call.invoke(new Object[] { raciObjectId, raciToSee });
    }

    public RaciInfoLine[] listRacis(Integer userId, Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { userId, raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    public RaciInfoLine[] listModuleRacis(Integer userId, Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listModuleRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { userId, raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    public RaciInfoLine[] listMyRacis(Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Preferences");
        call.setOperationName("listMyRacis");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        registerEnum(call);
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "RaciInfoLine");
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    public Boolean deleteRaci(Integer userId, Integer raciObjectId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("deleteRaci");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { userId, raciObjectId });
    }

    public Boolean updateRaci(RaciInfoLine raci) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("updateRaci");
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    public Boolean forceRaci(RaciInfoLine raci) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("forceRaci");
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    public Boolean addRaci(RaciInfoLine raci) throws Exception {
        _logger.debug("SoapTestCase - addRaci (" + raci + ")");
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("addRaci");
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    public PeopleInfoLine[] listNonUsers(Integer raciObjectId, Boolean viewDisabled) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listNonUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("viewDisabled", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId, viewDisabled });
    }

    public PeopleInfoLine[] listUsers(Integer raciObjectId, Boolean viewDisabled) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("viewDisabled", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId, viewDisabled });
    }

    public PeopleInfoLine[] listRaciableUsers(Integer raciObjectId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listRaciableUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId });
    }

    public RaciInfoLine getRaci(Integer raciObjectId, Integer userId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("getRaci");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(qn);
        return (RaciInfoLine) call.invoke(new Object[] { raciObjectId, userId });
    }

    public RaciAuthorization[] listCan(Integer userId, Integer raciObjectId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("listCan");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapRaci", "RaciAuthorization");
        call.registerTypeMapping(com.entelience.objects.raci.RaciAuthorization.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciAuthorization.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciAuthorization.class, qn));
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (RaciAuthorization[]) call.invoke(new Object[] { userId, raciObjectId });
    }

    public boolean can(Integer userId, Integer raciObjectId, String raciOperation) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("can");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciOperation", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { userId, raciObjectId, raciOperation });
    }

    public Boolean notifyMail(Integer raciObjId, Integer destUserId, String message) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("notifyMail");
        call.addParameter("raciObjId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("destUserId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("message", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raciObjId, destUserId, message });
    }

    public Boolean isIdValid(Id id) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Audit");
        call.setOperationName("isIdValid");
        QName qn = new QName("urn:com.entelience.soap.soapAudit", "Id");
        call.registerTypeMapping(com.entelience.objects.Id.class, qn, new BeanSerializerFactory(com.entelience.objects.Id.class, qn), new BeanDeserializerFactory(com.entelience.objects.Id.class, qn));
        call.addParameter("id", qn, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { id });
    }

    public Boolean isPasswordExpired() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Authentication");
        call.setOperationName("isPasswordExpired");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] {});
    }

    public String getRaciName(Integer raciObj) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("getRaciName");
        call.setOperationStyle("rpc");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_STRING);
        return (String) call.invoke(new Object[] { raciObj });
    }

    public RaciHistory[] getRaciHistory(Integer raciObj, TwoDate td, Integer page) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("getRaciHistory");
        call.setOperationStyle("rpc");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapRaci", "TwoDate");
        call.registerTypeMapping(com.entelience.objects.TwoDate.class, qn, new BeanSerializerFactory(com.entelience.objects.TwoDate.class, qn), new BeanDeserializerFactory(com.entelience.objects.TwoDate.class, qn));
        call.addParameter("dates", qn, ParameterMode.IN);
        call.addParameter("page", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qnRH = new QName("urn:com.entelience.soap.soapRaci", "RaciHistory");
        call.registerTypeMapping(com.entelience.objects.raci.RaciHistory.class, qnRH, new BeanSerializerFactory(com.entelience.objects.raci.RaciHistory.class, qnRH), new BeanDeserializerFactory(com.entelience.objects.raci.RaciHistory.class, qnRH));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (RaciHistory[]) call.invoke(new Object[] { raciObj, td, page });
    }

    public PageCounter countRaciHistory(Integer raciObj, TwoDate td) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("countRaciHistory");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapRaci", "TwoDate");
        call.registerTypeMapping(com.entelience.objects.TwoDate.class, qn, new BeanSerializerFactory(com.entelience.objects.TwoDate.class, qn), new BeanDeserializerFactory(com.entelience.objects.TwoDate.class, qn));
        call.addParameter("dates", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qnPC = getPageCounterQn();
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qnPC, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qnPC), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qnPC);
        return (PageCounter) call.invoke(new Object[] { raciObj, td });
    }

    public RaciInfoLine[] getRaciMatrixBackInTime(Integer raciObj, Date effectiveDate) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Raci");
        call.setOperationName("getRaciMatrixBackInTime");
        call.setOperationStyle("rpc");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("effectiveDate", XMLType.XSD_DATETIME, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        registerEnum(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (RaciInfoLine[]) call.invoke(new Object[] { raciObj, effectiveDate });
    }

    public Continent[] listContinents() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listContinents");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Continent");
        call.registerTypeMapping(com.entelience.objects.geography.Continent.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Continent.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Continent.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Continent[]) call.invoke(new Object[] {});
    }

    public SubContinent[] listSubContinents(Integer continentIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listSubContinents");
        call.setOperationStyle("rpc");
        call.addParameter("continentIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "SubContinent");
        call.registerTypeMapping(com.entelience.objects.geography.SubContinent.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.SubContinent.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.SubContinent.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (SubContinent[]) call.invoke(new Object[] { continentIso });
    }

    public Country[] listCountries(Integer continentIso, Integer subContinentIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listCountries");
        call.setOperationStyle("rpc");
        call.addParameter("continentIso", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("subContinentIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Country");
        call.registerTypeMapping(com.entelience.objects.geography.Country.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Country.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Country.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Country[]) call.invoke(new Object[] { continentIso, subContinentIso });
    }

    public Country[] listCountriesInBusinessZone(Integer businessZoneId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listCountriesInBusinessZone");
        call.setOperationStyle("rpc");
        call.addParameter("listCountriesInBusinessZone", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Country");
        call.registerTypeMapping(com.entelience.objects.geography.Country.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Country.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Country.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Country[]) call.invoke(new Object[] { businessZoneId });
    }

    public Region[] listRegions(Integer countryIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listRegions");
        call.setOperationStyle("rpc");
        call.addParameter("countryIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Region");
        call.registerTypeMapping(com.entelience.objects.geography.Region.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Region.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Region.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Region[]) call.invoke(new Object[] { countryIso });
    }

    public Timezone[] listTimezones() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listTimezones");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Timezone");
        call.registerTypeMapping(com.entelience.objects.geography.Timezone.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Timezone.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Timezone.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Timezone[]) call.invoke(new Object[] {});
    }

    public Timezone[] listTimezonesForCountry(int countryIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Geography");
        call.setOperationName("listTimezonesForCountry");
        call.setOperationStyle("rpc");
        call.addParameter("countryIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Timezone");
        call.registerTypeMapping(com.entelience.objects.geography.Timezone.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Timezone.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Timezone.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Timezone[]) call.invoke(new Object[] { countryIso });
    }

    public ModuleMetric[] listMetricsForModule(Integer moduleId, String shortName) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Module");
        call.setOperationName("listMetricsForModule");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("shortName", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleMetric");
        call.registerTypeMapping(com.entelience.objects.module.ModuleMetric.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleMetric.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleMetric.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleMetric[]) call.invoke(new Object[] { moduleId, shortName });
    }

    public Boolean setTarget(Integer metricId, Integer newTarget, Integer timingId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Module");
        call.setOperationName("setTarget");
        call.setOperationStyle("rpc");
        call.addParameter("metricId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("newTarget", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("timingId", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { metricId, newTarget, timingId });
    }

    public DropDown[] getListOfTimescales() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(soapEndPoint + "Module");
        call.setOperationName("getListOfTimescales");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "DropDown");
        call.registerTypeMapping(com.entelience.objects.DropDown.class, qn, new BeanSerializerFactory(com.entelience.objects.DropDown.class, qn), new BeanDeserializerFactory(com.entelience.objects.DropDown.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (DropDown[]) call.invoke(new Object[] {});
    }
}
