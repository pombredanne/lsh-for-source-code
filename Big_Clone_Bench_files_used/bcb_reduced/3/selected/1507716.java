package com.entelience.test.test06soap;

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
import junit.framework.TestCase;
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
import com.entelience.objects.raci.RaciHistory;
import com.entelience.objects.raci.RaciInfoLine;
import com.entelience.soap.soapBaseHelper;

/** 
 *  Base class for soap unit testing 
 */
public class SoapTestCase extends TestCase {

    /** Would be nice to add some logic here to retrieve the info from 
	the esis.properties file 
    */
    protected static Service service = null;

    protected static String userName = "esis";

    protected static String password = "esis";

    /** object comparision with some sense added */
    protected void assertObjectNullEquals(Object o1, Object o2) throws Exception {
        if (o1 == null && o2 == null) return;
        if (o1 == null && o2 != null) assertTrue(false);
        if (o1 != null && o2 == null) assertTrue(false);
        assertEquals(o1, o2);
    }

    protected void assertTrue(Boolean b) throws Exception {
        assertNotNull(b);
        assertTrue(b.booleanValue());
    }

    protected void assertFalse(Boolean b) throws Exception {
        assertNotNull(b);
        assertFalse(b.booleanValue());
    }

    public String getEndPoint() throws Exception {
        return soapBaseHelper.getEndPoint();
    }

    /**
     * compute the md5sum for a string
     */
    protected String md5sum(String toCompute) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(toCompute.getBytes());
        java.math.BigInteger hash = new java.math.BigInteger(1, md.digest());
        return hash.toString(16);
    }

    public EngineConfiguration createClientConfig() {
        SimpleProvider clientConfig = new SimpleProvider();
        Handler sessionHandler = (Handler) new SimpleSessionHandler();
        SimpleChain reqHandler = new SimpleChain();
        SimpleChain respHandler = new SimpleChain();
        reqHandler.addHandler(sessionHandler);
        respHandler.addHandler(sessionHandler);
        Handler pivot = (Handler) new HTTPSender();
        Handler transport = new SimpleTargetedChain(reqHandler, pivot, respHandler);
        clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME, transport);
        return clientConfig;
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (SoapTestCase.service == null) {
            SoapTestCase.service = new Service(createClientConfig());
        }
        LoginResult lr = logIn();
        assertNotNull(lr);
        assertFalse(lr.isLoginFail());
        assertFalse(lr.isAccountLocked());
        assertFalse(lr.isAccountNewlyLocked());
        assertTrue(lr.isSuccess());
    }

    protected LoginResult logIn() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("logIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum(password);
        String login = userName;
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "LoginResult");
        call.registerTypeMapping(com.entelience.objects.LoginResult.class, qn, new BeanSerializerFactory(com.entelience.objects.LoginResult.class, qn), new BeanDeserializerFactory(com.entelience.objects.LoginResult.class, qn));
        call.setReturnType(qn);
        return (LoginResult) call.invoke(new Object[] { login, passwd });
    }

    protected void tearDown() throws Exception {
        if (validSession()) {
            logOut();
        }
        super.tearDown();
    }

    /**
     * end of the tests : log Out
     */
    protected void logOut() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Directory");
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
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
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

    protected byte[] getAttachmentContent(AttachmentPart attachment) throws Exception {
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

    protected List<AttachmentPart> getAttachments(Call call) throws Exception {
        Attachments atts = call.getMessageContext().getCurrentMessage().getAttachmentsImpl();
        assertNotNull(atts);
        List<AttachmentPart> ret = new ArrayList<AttachmentPart>();
        for (Iterator it = atts.getAttachments().iterator(); it.hasNext(); ) {
            org.apache.axis.attachments.AttachmentPart ap = (org.apache.axis.attachments.AttachmentPart) it.next();
            ret.add(ap);
        }
        return ret;
    }

    protected void addFileAsAttachment(Call call, File toAttach) throws SOAPException {
        DataHandler attachmentFile = new DataHandler(new FileDataSource(toAttach));
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        AttachmentPart attachment = soapMessage.createAttachmentPart(attachmentFile);
        call.addAttachmentPart(attachment);
    }

    /**
     * Raci WS tests.
     *  should be in test13Raci, but it's easier to call them from other tests if they are here
     *
     */
    protected QName getRaciQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "RaciInfoLine");
    }

    private QName getPageCounterQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PageCounter");
    }

    private QName getPeopleInfoLineQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PeopleInfoLine");
    }

    protected void registerTypeMappingForRaciException(Call call) throws Exception {
        QName qn = getRaciExceptionQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciException.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciException.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciException.class, qn));
    }

    protected QName getRaciExceptionQn() {
        return new QName("urn:com.entelience.soap.soapRaci", "PeopleInfoLine");
    }

    protected ModuleDetail[] listActiveModules() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listActiveModules");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleDetail");
        call.registerTypeMapping(com.entelience.objects.module.ModuleDetail.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleDetail.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleDetail.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleDetail[]) call.invoke(new Object[] {});
    }

    protected PeopleInfoLine getCurrentPeople() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Preferences");
        call.setOperationName("getCurrentPeople");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "PeopleInfoLine");
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(qn);
        PeopleInfoLine pil = null;
        pil = (PeopleInfoLine) call.invoke(new Object[] {});
        return pil;
    }

    protected PageCounter countRacis(Integer userId, Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
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

    protected PageCounter countModuleRacis(Integer userId, Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
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

    protected PageCounter countMyRacis(Integer raciObjectId, List<String> raciToSee) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Preferences");
        call.setOperationName("countMyRacis");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "PageCounter");
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        return (PageCounter) call.invoke(new Object[] { raciObjectId, raciToSee });
    }

    protected RaciInfoLine[] listRacis(Integer userId, Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("listRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { userId, raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    protected RaciInfoLine[] listModuleRacis(Integer userId, Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("listModuleRacis");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { userId, raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    protected RaciInfoLine[] listMyRacis(Integer raciObjectId, List<String> raciToSee, Integer pageNumber, String order, String way) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Preferences");
        call.setOperationName("listMyRacis");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciToSee", XMLType.SOAP_ARRAY, ParameterMode.IN);
        call.addParameter("pageNumber", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = new QName("urn:com.entelience.soap.soapPreferences", "RaciInfoLine");
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        RaciInfoLine[] raci = null;
        raci = (RaciInfoLine[]) call.invoke(new Object[] { raciObjectId, raciToSee, pageNumber, order, way });
        return raci;
    }

    protected Boolean deleteRaci(Integer userId, Integer raciObjectId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("deleteRaci");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { userId, raciObjectId });
    }

    protected Boolean updateRaci(RaciInfoLine raci) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("updateRaci");
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    protected Boolean forceRaci(RaciInfoLine raci) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("forceRaci");
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    protected Boolean addRaci(RaciInfoLine raci) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("addRaci");
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.addParameter("raci", qn, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raci });
    }

    protected PeopleInfoLine[] listNonUsers(Integer raciObjectId, Boolean viewDisabled) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("listNonUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("viewDisabled", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId, viewDisabled });
    }

    protected PeopleInfoLine[] listUsers(Integer raciObjectId, Boolean viewDisabled) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("listUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("viewDisabled", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId, viewDisabled });
    }

    protected PeopleInfoLine[] listRaciableUsers(Integer raciObjectId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("listRaciableUsers");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getPeopleInfoLineQn();
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (PeopleInfoLine[]) call.invoke(new Object[] { raciObjectId });
    }

    protected RaciInfoLine getRaci(Integer raciObjectId, Integer userId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("getRaci");
        call.addParameter("raciObjectId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(qn);
        return (RaciInfoLine) call.invoke(new Object[] { raciObjectId, userId });
    }

    protected Boolean notifyMail(Integer raciObjId, Integer destUserId, String message) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("notifyMail");
        call.addParameter("raciObjId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("destUserId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("message", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { raciObjId, destUserId, message });
    }

    protected Boolean isIdValid(Id id) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Audit");
        call.setOperationName("isIdValid");
        QName qn = new QName("urn:com.entelience.soap.soapAudit", "Id");
        call.registerTypeMapping(com.entelience.objects.Id.class, qn, new BeanSerializerFactory(com.entelience.objects.Id.class, qn), new BeanDeserializerFactory(com.entelience.objects.Id.class, qn));
        call.addParameter("id", qn, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { id });
    }

    protected Boolean isPasswordExpired() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("isPasswordExpired");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] {});
    }

    protected String getRaciName(Integer raciObj) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("getRaciName");
        call.setOperationStyle("rpc");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_STRING);
        return (String) call.invoke(new Object[] { raciObj });
    }

    protected RaciHistory[] getRaciHistory(Integer raciObj, TwoDate td, Integer page) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
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

    protected PageCounter countRaciHistory(Integer raciObj, TwoDate td) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
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

    protected RaciInfoLine[] getRaciMatrixBackInTime(Integer raciObj, Date effectiveDate) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Raci");
        call.setOperationName("getRaciMatrixBackInTime");
        call.setOperationStyle("rpc");
        call.addParameter("raciObj", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("effectiveDate", XMLType.XSD_DATETIME, ParameterMode.IN);
        registerTypeMappingForRaciException(call);
        QName qn = getRaciQn();
        call.registerTypeMapping(com.entelience.objects.raci.RaciInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.raci.RaciInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (RaciInfoLine[]) call.invoke(new Object[] { raciObj, effectiveDate });
    }

    protected Continent[] listContinents() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
        call.setOperationName("listContinents");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Continent");
        call.registerTypeMapping(com.entelience.objects.geography.Continent.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Continent.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Continent.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Continent[]) call.invoke(new Object[] {});
    }

    protected SubContinent[] listSubContinents(Integer continentIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
        call.setOperationName("listSubContinents");
        call.setOperationStyle("rpc");
        call.addParameter("continentIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "SubContinent");
        call.registerTypeMapping(com.entelience.objects.geography.SubContinent.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.SubContinent.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.SubContinent.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (SubContinent[]) call.invoke(new Object[] { continentIso });
    }

    protected Country[] listCountries(Integer continentIso, Integer subContinentIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
        call.setOperationName("listCountries");
        call.setOperationStyle("rpc");
        call.addParameter("continentIso", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("subContinentIso", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Country");
        call.registerTypeMapping(com.entelience.objects.geography.Country.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Country.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Country.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Country[]) call.invoke(new Object[] { continentIso, subContinentIso });
    }

    protected Country[] listCountriesInBusinessZone(Integer businessZoneId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
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
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
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
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
        call.setOperationName("listTimezones");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapGeography", "Timezone");
        call.registerTypeMapping(com.entelience.objects.geography.Timezone.class, qn, new BeanSerializerFactory(com.entelience.objects.geography.Timezone.class, qn), new BeanDeserializerFactory(com.entelience.objects.geography.Timezone.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (Timezone[]) call.invoke(new Object[] {});
    }

    public Timezone[] listTimezonesForCountry(int countryIso) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Geography");
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
        call.setTargetEndpointAddress(getEndPoint() + "Module");
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
        call.setTargetEndpointAddress(getEndPoint() + "Module");
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
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getListOfTimescales");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "DropDown");
        call.registerTypeMapping(com.entelience.objects.DropDown.class, qn, new BeanSerializerFactory(com.entelience.objects.DropDown.class, qn), new BeanDeserializerFactory(com.entelience.objects.DropDown.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (DropDown[]) call.invoke(new Object[] {});
    }
}
