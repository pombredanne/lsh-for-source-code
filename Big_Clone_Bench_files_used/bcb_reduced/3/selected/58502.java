package net.sbbi.upnp.jmx;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sbbi.upnp.services.ServiceStateVariable;
import net.sbbi.upnp.services.ServiceStateVariableTypes;

/**
 * Class to expose an JMX Mbean as an UPNP device service, this class shouldn't be used
 * directly
 * @author <a href="mailto:superbonbon@sbbi.net">SuperBonBon</a>
 * @version 1.0
 */
public class UPNPMBeanService {

    private static final Log log = LogFactory.getLog(UPNPMBeanService.class);

    private String serviceType;

    private String serviceId;

    private String serviceUUID;

    private String deviceUUID;

    private String deviceSCDP;

    private Map operationsStateVariables;

    private MBeanServer targetServer;

    private MBeanInfo mbeanInfo;

    private ObjectName mbeanName;

    private Class targetBeanClass;

    protected UPNPMBeanService(String deviceUUID, String vendorDomain, String serviceId, String serviceType, int serviceVersion, MBeanInfo mbeanInfo, ObjectName mbeanName, MBeanServer targetServer) throws IOException {
        this.deviceUUID = deviceUUID;
        if (serviceId == null) {
            serviceId = generateServiceId(mbeanName);
        }
        this.serviceUUID = deviceUUID + "/" + serviceId;
        this.serviceType = "urn:" + vendorDomain + ":service:" + serviceType + ":" + serviceVersion;
        this.serviceId = "urn:" + vendorDomain + ":serviceId:" + serviceId;
        deviceSCDP = getDeviceSSDP(mbeanInfo);
        try {
            targetBeanClass = Class.forName(mbeanInfo.getClassName());
        } catch (ClassNotFoundException ex) {
            IOException ex2 = new IOException("Unable to find target MBean class " + mbeanInfo.getClassName());
            ex2.initCause(ex);
            throw ex2;
        }
        this.mbeanName = mbeanName;
        this.mbeanInfo = mbeanInfo;
        this.targetServer = targetServer;
    }

    protected String getServiceUUID() {
        return serviceUUID;
    }

    protected String getDeviceUUID() {
        return deviceUUID;
    }

    private String generateServiceId(ObjectName mbeanName) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(mbeanName.toString().getBytes());
            StringBuffer hexString = new StringBuffer();
            byte[] digest = md5.digest();
            for (int i = 0; i < digest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
            }
            return hexString.toString().toUpperCase();
        } catch (Exception ex) {
            RuntimeException runTimeEx = new RuntimeException("Unexpected error during MD5 hash creation, check your JRE");
            runTimeEx.initCause(ex);
            throw runTimeEx;
        }
    }

    protected String getServiceInfo() {
        StringBuffer rtrVal = new StringBuffer();
        rtrVal.append("<service>\r\n");
        rtrVal.append("<serviceType>").append(serviceType).append("</serviceType>\r\n");
        rtrVal.append("<serviceId>").append(serviceId).append("</serviceId>\r\n");
        rtrVal.append("<controlURL>").append("/").append(serviceUUID).append("/control").append("</controlURL>\r\n");
        rtrVal.append("<eventSubURL>").append("/").append(serviceUUID).append("/events").append("</eventSubURL>\r\n");
        rtrVal.append("<SCPDURL>").append("/").append(serviceUUID).append("/scpd.xml").append("</SCPDURL>\r\n");
        rtrVal.append("</service>\r\n");
        return rtrVal.toString();
    }

    protected Map getOperationsStateVariables() {
        return operationsStateVariables;
    }

    protected String getDeviceSCDP() {
        return deviceSCDP;
    }

    protected String getServiceType() {
        return serviceType;
    }

    protected Class getTargetBeanClass() {
        return targetBeanClass;
    }

    protected ObjectName getObjectName() {
        return mbeanName;
    }

    protected Object getAttribute(String attributeName) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
        return targetServer.getAttribute(mbeanName, attributeName);
    }

    protected Object invoke(String actionName, Object[] params, String[] signature) throws ReflectionException, InstanceNotFoundException, MBeanException {
        return targetServer.invoke(mbeanName, actionName, params, signature);
    }

    private String getDeviceSSDP(MBeanInfo info) throws IllegalArgumentException {
        MBeanOperationInfo[] ops = info.getOperations();
        MBeanAttributeInfo[] atts = info.getAttributes();
        if ((ops == null || ops.length == 0) && (atts == null || atts.length == 0)) {
            throw new IllegalArgumentException("MBean has no operation and no attribute and cannot be exposed as an UPNP device, provide at least one attribute");
        }
        Set deployedActionNames = new HashSet();
        operationsStateVariables = new HashMap();
        StringBuffer rtrVal = new StringBuffer();
        rtrVal.append("<?xml version=\"1.0\" ?>\r\n");
        rtrVal.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\r\n");
        rtrVal.append("<specVersion><major>1</major><minor>0</minor></specVersion>\r\n");
        if (ops != null && ops.length > 0) {
            rtrVal.append("<actionList>\r\n");
            for (int i = 0; i < ops.length; i++) {
                MBeanOperationInfo op = ops[i];
                StringBuffer action = new StringBuffer();
                if (deployedActionNames.contains(op.getName())) {
                    log.debug("The " + op.getName() + " is allready deplyoed and cannot be reused, skipping operation deployment");
                    continue;
                }
                action.append("<action>\r\n");
                action.append("<name>");
                action.append(op.getName());
                action.append("</name>\r\n");
                action.append("<argumentList>\r\n");
                action.append("<argument>\r\n");
                action.append("<name>");
                String outVarName = op.getName() + "_out";
                String actionOutDataType = ServiceStateVariable.getUPNPDataTypeMapping(op.getReturnType());
                if (actionOutDataType == null) actionOutDataType = ServiceStateVariableTypes.STRING;
                action.append(outVarName);
                action.append("</name>\r\n");
                action.append("<direction>out</direction>\r\n");
                action.append("<relatedStateVariable>");
                action.append(outVarName);
                action.append("</relatedStateVariable>\r\n");
                action.append("</argument>\r\n");
                boolean nonPrimitiveInputType = false;
                boolean duplicatedInputVarname = false;
                Map operationsInputStateVariables = new HashMap();
                if (op.getSignature() != null) {
                    for (int z = 0; z < op.getSignature().length; z++) {
                        MBeanParameterInfo param = op.getSignature()[z];
                        String actionInDataType = ServiceStateVariable.getUPNPDataTypeMapping(param.getType());
                        if (actionInDataType == null) {
                            nonPrimitiveInputType = true;
                            log.debug("The " + param.getType() + " type is not an UPNP compatible data type, use only primitives");
                            break;
                        }
                        String inVarName = param.getName();
                        String existing = (String) operationsStateVariables.get(inVarName);
                        if (existing != null && !existing.equals(actionInDataType)) {
                            String msg = "The operation " + op.getName() + " " + inVarName + " parameter already exists for another method with another data type (" + existing + ") either match the data type or change the parameter name" + " in you MBeanParameterInfo object for this operation";
                            duplicatedInputVarname = true;
                            log.debug(msg);
                            break;
                        }
                        action.append("<argument>\r\n");
                        action.append("<name>");
                        operationsInputStateVariables.put(inVarName, actionInDataType);
                        action.append(inVarName);
                        action.append("</name>\r\n");
                        action.append("<direction>in</direction>\r\n");
                        action.append("<relatedStateVariable>");
                        action.append(inVarName);
                        action.append("</relatedStateVariable>\r\n");
                        action.append("</argument>\r\n");
                    }
                }
                action.append("</argumentList>\r\n");
                action.append("</action>\r\n");
                if (!nonPrimitiveInputType && !duplicatedInputVarname) {
                    operationsStateVariables.putAll(operationsInputStateVariables);
                    operationsStateVariables.put(outVarName, actionOutDataType);
                    rtrVal.append(action.toString());
                    deployedActionNames.add(op.getName());
                }
            }
            rtrVal.append("</actionList>\r\n");
        } else {
            rtrVal.append("<actionList/>\r\n");
        }
        rtrVal.append("<serviceStateTable>\r\n");
        for (Iterator i = operationsStateVariables.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            String type = (String) operationsStateVariables.get(name);
            rtrVal.append("<stateVariable sendEvents=\"no\">\r\n");
            rtrVal.append("<name>");
            rtrVal.append(name);
            rtrVal.append("</name>\r\n");
            rtrVal.append("<dataType>");
            rtrVal.append(type);
            rtrVal.append("</dataType>\r\n");
            rtrVal.append("</stateVariable>\r\n");
        }
        if (atts != null && atts.length > 0) {
            for (int i = 0; i < atts.length; i++) {
                MBeanAttributeInfo att = atts[i];
                if (att.isReadable()) {
                    rtrVal.append("<stateVariable sendEvents=\"no\">\r\n");
                    rtrVal.append("<name>");
                    rtrVal.append(att.getName());
                    rtrVal.append("</name>\r\n");
                    rtrVal.append("<dataType>");
                    String stateVarType = ServiceStateVariable.getUPNPDataTypeMapping(att.getType());
                    if (stateVarType == null) stateVarType = ServiceStateVariableTypes.STRING;
                    rtrVal.append(stateVarType);
                    rtrVal.append("</dataType>\r\n");
                    rtrVal.append("</stateVariable>\r\n");
                }
            }
        }
        rtrVal.append("</serviceStateTable>\r\n");
        rtrVal.append("</scpd>");
        return rtrVal.toString();
    }

    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }
}
