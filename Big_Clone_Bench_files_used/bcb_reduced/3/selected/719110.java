package org.nodevision.portal.bp.axis;

import java.rmi.Remote;
import java.security.MessageDigest;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Stub;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.token.UsernameToken;

public class NVBPSkeletonServiceLocator extends org.apache.axis.client.Service implements org.nodevision.portal.bp.axis.NVBPSkeletonService {

    private String username = "";

    private String password = "";

    public NVBPSkeletonServiceLocator() {
    }

    public NVBPSkeletonServiceLocator(String NVBPService_address) {
        this.NVBPService_address = NVBPService_address;
    }

    public NVBPSkeletonServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public NVBPSkeletonServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    private java.lang.String NVBPService_address;

    public java.lang.String getNVBPServiceAddress() {
        return NVBPService_address;
    }

    private java.lang.String NVBPServiceWSDDServiceName = "NVBPService";

    public java.lang.String getNVBPServiceWSDDServiceName() {
        return NVBPServiceWSDDServiceName;
    }

    public void setNVBPServiceWSDDServiceName(java.lang.String name) {
        NVBPServiceWSDDServiceName = name;
    }

    public org.nodevision.portal.bp.axis.NVBPSkeleton getNVBPService() throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NVBPService_address);
        } catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNVBPService(endpoint);
    }

    public org.nodevision.portal.bp.axis.NVBPSkeleton getNVBPService(String username, String password) throws javax.xml.rpc.ServiceException {
        this.username = username;
        this.password = password;
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NVBPService_address);
        } catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNVBPService(endpoint);
    }

    public org.nodevision.portal.bp.axis.NVBPSkeleton getNVBPService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.nodevision.portal.bp.axis.NVBPServiceSoapBindingStub _stub = new org.nodevision.portal.bp.axis.NVBPServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getNVBPServiceWSDDServiceName());
            return (NVBPSkeleton) addAuthentication((Stub) _stub);
        } catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNVBPServiceEndpointAddress(java.lang.String address) {
        NVBPService_address = address;
    }

    /**
	 * For the given interface, get the stub implementation. If this service has
	 * no port for the given interface, then ServiceException is thrown.
	 */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.nodevision.portal.bp.axis.NVBPSkeleton.class.isAssignableFrom(serviceEndpointInterface)) {
                org.nodevision.portal.bp.axis.NVBPServiceSoapBindingStub _stub = new org.nodevision.portal.bp.axis.NVBPServiceSoapBindingStub(new java.net.URL(NVBPService_address), this);
                _stub.setPortName(getNVBPServiceWSDDServiceName());
                return (Remote) addAuthentication(_stub);
            }
        } catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
	 * For the given interface, get the stub implementation. If this service has
	 * no port for the given interface, then ServiceException is thrown.
	 */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("NVBPService".equals(inputPortName)) {
            return getNVBPService();
        } else {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return (Remote) addAuthentication((Stub) _stub);
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://bp.portal.nodevision.org", "NVBPSkeletonService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://bp.portal.nodevision.org", "NVBPService"));
        }
        return ports.iterator();
    }

    /**
	 * Set the endpoint address for the specified port name.
	 */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        if ("NVBPService".equals(portName)) {
            setNVBPServiceEndpointAddress(address);
        } else {
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
	 * Set the endpoint address for the specified port name.
	 */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

    private Stub addAuthentication(Stub _stub) throws ServiceException {
        try {
            _stub._setProperty(UsernameToken.PASSWORD_TYPE, WSConstants.PASSWORD_DIGEST);
            _stub._setProperty(WSHandlerConstants.USER, username);
            _stub.setPassword(createMD5(password));
            return _stub;
        } catch (Exception e) {
            throw new ServiceException(e.toString());
        }
    }

    private final String createMD5(String pwd) throws Exception {
        MessageDigest md = (MessageDigest) MessageDigest.getInstance("MD5").clone();
        md.update(pwd.getBytes("UTF-8"));
        byte[] pd = md.digest();
        StringBuffer app = new StringBuffer();
        for (int i = 0; i < pd.length; i++) {
            String s2 = Integer.toHexString(pd[i] & 0xFF);
            app.append((s2.length() == 1) ? "0" + s2 : s2);
        }
        return app.toString();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
