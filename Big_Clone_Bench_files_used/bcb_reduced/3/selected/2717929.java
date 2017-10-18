package es.cim.mediators.validarDocumentoFirma;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLStreamReader;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.jaxen.JaxenException;

/**
 *<pre>
 * <validarDocumentoFirmaCIM>
 *      <source xpath="expression"/>      
 * </validarDocumentoFirmaCIM>
 * </pre>
 */
public class ValidarDocumentoFirmaMediatorCIM extends AbstractMediator implements ManagedLifecycle {

    private ConfigurationContext cfgCtx = null;

    private String request = null;

    private SynapseXPath requestXPath = null;

    public static final String POLICY_XML = "./resources/policy/policyCIM.xml";

    private static final Log traceLog = LogFactory.getLog("TRACE_LOGGER");

    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);
        boolean ok = false;
        try {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug(" *** ValidarDocumentoFirmaMediatorCIM *** (mediate). Start : ValidarDocumentoFirmaMediatorCIM mediator **SCOOPY 300**");
                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace(" *** ValidarDocumentoFirmaMediatorCIM *** (mediate). Message : " + synCtx.getEnvelope());
                }
            }
            OMElement omRequest = getRequestPayload(synCtx);
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceTrace(" *** ValidarDocumentoFirmaMediatorCIM *** (mediate). Request message payload : " + omRequest);
            }
            ok = verificarFirma(omRequest, synCtx);
            if (!ok) {
                handleException("El documento no concuerda con los datos firmados", synCtx);
            }
        } catch (Exception e) {
            handleException("Error validando documento firmado", synCtx);
        }
        synLog.traceOrDebug(" *** ValidarDocumentoFirmaMediatorCIM *** (mediate). End : ValidarDocumentoFirmaMediatorCIM mediator");
        return true;
    }

    public void init(SynapseEnvironment synEnv) {
        traceLog.trace("*** ValidarDocumentoFirmaMediatorCIM mediator INITIALIZED ***");
    }

    /**
	 * Establece opciones de cliente
	 * @throws AxisFault 
	 */
    private Options getClientOptions(String serviceURL, String action, String user, String password, MessageContext synCtx) throws Exception {
        SynapseLog synLog = getLog(synCtx);
        Options options = new Options();
        options.setTo(new EndpointReference(serviceURL));
        if (action != null) {
            options.setAction(action);
            synLog.traceOrDebug(" *** FirmaMediatorCIM *** (mediate).  action==" + action);
        } else {
            if (synCtx.isSOAP11()) {
                options.setProperty(Constants.Configuration.DISABLE_SOAP_ACTION, true);
            } else {
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                axis2MessageCtx.getTransportOut().addParameter(new Parameter(HTTPConstants.OMIT_SOAP_12_ACTION, true));
            }
        }
        options.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
        if (user != null) {
            StAXOMBuilder builder = new StAXOMBuilder(POLICY_XML);
            options.setProperty(RampartMessageData.KEY_RAMPART_POLICY, PolicyEngine.getPolicy(builder.getDocumentElement()));
            options.setUserName(user);
            options.setPassword(password);
        }
        return options;
    }

    public void destroy() {
        traceLog.trace("*** ValidarDocumentoFirmaMediatorCIM mediator DESTROYED ***");
    }

    /**
     * Obtiene payload
     */
    private OMElement getRequestPayload(MessageContext synCtx) throws AxisFault {
        SynapseLog synLog = getLog(synCtx);
        synLog.traceOrDebug(" %%% ESTOY DENTRO DE getRequestPayload.");
        try {
            Object o = requestXPath.evaluate(MessageHelper.cloneMessageContext(synCtx));
            if (o instanceof OMElement) {
                return (OMElement) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                return (OMElement) ((List) o).get(0);
            } else {
                handleException("The evaluation of the XPath expression : " + requestXPath.toString() + " did not result in an OMElement", synCtx);
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath expression : " + requestXPath.toString(), e, synCtx);
        }
        return null;
    }

    /**
     * 
     * @param omRequest
     * @param synCtx
     * @see Devuelve el proceso correcto si el formato de la firma es CAdES y se corresponde con los datos firmados o simplemente si el formato de la firma
     * es XAdES.
     * @return
     */
    public boolean verificarFirma(OMElement omRequest, MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);
        boolean verificacionOK = false;
        OMElement node = null, response = null;
        String nodeName, nodeValue;
        String datos = "", firma = "", formatoFirma = "";
        String TAG_DATOS = "datos", TAG_FIRMA = "firma", TAG_FORMATO_FIRMA = "formatoFirma";
        String FORMATO_FIRMA_XADES_BES = "XADES-BES", FORMATO_FIRMA_XADES_T = "XADES-T", FORMATO_FIRMA_CADES_BES = "CADES-BES", FORMATO_FIRMA_CADES_T = "CADES-T";
        for (Iterator it = omRequest.getChildElements(); it.hasNext(); ) {
            synLog.traceOrDebug(" *** SI hay Childs");
            Object o = it.next();
            if (o instanceof OMElement) {
                node = (OMElement) o;
                nodeName = node.getLocalName();
                nodeValue = node.getText();
                if (nodeName.equals(TAG_DATOS)) {
                    datos = nodeValue;
                } else if (nodeName.equals(TAG_FIRMA)) {
                    firma = nodeValue;
                } else if (nodeName.equals(TAG_FORMATO_FIRMA)) {
                    formatoFirma = nodeValue;
                }
            }
        }
        HashMap<String, String> res = new HashMap<String, String>();
        res.put(TAG_DATOS, datos);
        res.put(TAG_FIRMA, firma);
        res.put(TAG_FORMATO_FIRMA, formatoFirma);
        synLog.traceOrDebug(" *** Datos: " + datos);
        synLog.traceOrDebug(" *** Firma: " + firma);
        synLog.traceOrDebug(" *** FormatoFirma: " + formatoFirma);
        byte[] firmaBytes = org.apache.commons.codec.binary.Base64.decodeBase64(firma.getBytes());
        byte[] documentoBytes = org.apache.commons.codec.binary.Base64.decodeBase64(datos.getBytes());
        byte[] datosEnFirma = null;
        try {
            if (formatoFirma.equals(FORMATO_FIRMA_CADES_BES) || formatoFirma.equals(FORMATO_FIRMA_CADES_T)) {
                synLog.traceOrDebug(" *** Es formato de firma CADES");
                synLog.traceOrDebug(" *** Es formato de firma CADES 1. FirmaBytes: " + firmaBytes.toString());
                synLog.traceOrDebug(" *** Es formato de firma CADES 2. documentoBytes: " + documentoBytes.toString());
                ByteArrayInputStream bis = new ByteArrayInputStream(firmaBytes);
                synLog.traceOrDebug(" *** Es formato de firma CADES 3 ");
                ASN1InputStream lObjDerOut = new ASN1InputStream(bis);
                DERObject lObjDER = null;
                synLog.traceOrDebug(" *** Es formato de firma CADES 4.lObjDerOut: " + lObjDerOut);
                try {
                    lObjDER = lObjDerOut.readObject();
                    synLog.traceOrDebug(" *** Es formato de firma CADES 5. lObjDER: " + lObjDER);
                } catch (IOException e) {
                    synLog.traceOrDebug("No encuentra formato de la firma");
                    handleException("No encuentra formato de la firma", synCtx);
                }
                synLog.traceOrDebug(" *** Es formato de firma CADES 6 ");
                ContentInfo lObjPKCS7 = ContentInfo.getInstance(lObjDER);
                synLog.traceOrDebug(" *** Es formato de firma CADES 7 ");
                SignedData lObjSignedData = SignedData.getInstance(lObjPKCS7.getContent());
                synLog.traceOrDebug(" *** Es formato de firma CADES 8 ");
                ContentInfo lObjContent = lObjSignedData.getContentInfo();
                synLog.traceOrDebug(" *** Es formato de firma CADES 9 ");
                datosEnFirma = ((ASN1OctetString) lObjContent.getContent()).getOctets();
                synLog.traceOrDebug(" *** Es formato de firma CADES 10 ");
                synLog.traceOrDebug("Son iguales? " + Arrays.equals(documentoBytes, datosEnFirma));
                if (Arrays.equals(documentoBytes, datosEnFirma)) {
                    verificacionOK = true;
                }
            } else if (formatoFirma.equals(FORMATO_FIRMA_XADES_BES) || formatoFirma.equals(FORMATO_FIRMA_XADES_T)) {
                synLog.traceOrDebug(" *** Es formato de firma XADES 1");
                InputStream in = new ByteArrayInputStream(firmaBytes);
                synLog.traceOrDebug(" *** Es formato de firma XADES 2");
                XMLStreamReader reader = StAXUtils.createXMLStreamReader(in);
                synLog.traceOrDebug(" *** Es formato de firma XADES 3");
                StAXOMBuilder builder = new StAXOMBuilder(reader);
                synLog.traceOrDebug(" *** Es formato de firma XADES 4");
                OMElement documentElement = builder.getDocumentElement();
                synLog.traceOrDebug(" *** Es formato de firma XADES 5");
                AXIOMXPath xpath = null;
                OMElement nodo = null;
                xpath = new AXIOMXPath("/AFIRMA/CONTENT");
                synLog.traceOrDebug(" *** Es formato de firma XADES 6");
                nodo = (OMElement) xpath.selectSingleNode(documentElement);
                synLog.traceOrDebug(" *** Es formato de firma XADES 7");
                String hashB64Xades = nodo.getText();
                synLog.traceOrDebug(" *** HASH B64 EN XADES: " + hashB64Xades);
                MessageDigest dig = MessageDigest.getInstance("SHA1");
                byte[] hash = dig.digest(documentoBytes);
                String hashB64Doc = new String(org.apache.commons.codec.binary.Base64.encodeBase64(hash));
                synLog.traceOrDebug(" *** HASH B64 DOC:      " + hashB64Doc);
                synLog.traceOrDebug(" *** IGUALES?: " + hashB64Doc.equals(hashB64Xades));
                if (hashB64Doc.equals(hashB64Xades)) {
                    verificacionOK = true;
                }
            } else {
                handleException("Formato de firma irreconocible ('" + formatoFirma + "')", synCtx);
            }
        } catch (Exception ex) {
            synLog.traceOrDebug(" %%% ValidarDocumentoFirmaMediatorCIM *** . Excepcion en verificarFirma: " + stackTraceToString(ex));
        }
        return verificacionOK;
    }

    public void setRequestXPath(SynapseXPath requestXPath) throws JaxenException {
        this.requestXPath = requestXPath;
    }

    public SynapseXPath getRequestXPath() {
        return requestXPath;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    private String stackTraceToString(Throwable e) {
        if (e == null) {
            return "";
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            PrintWriter writer = new PrintWriter(bytes, true);
            e.printStackTrace(writer);
        } catch (Exception ex) {
        }
        return bytes.toString();
    }

    private static int copy(InputStream input, OutputStream output) throws IOException {
        byte buffer[] = new byte[4096];
        int count = 0;
        for (int n = 0; -1 != (n = input.read(buffer)); ) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
