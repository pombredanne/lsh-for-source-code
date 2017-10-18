package org.nodevision.portal.search.axis;

import java.rmi.Remote;
import java.security.MessageDigest;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Stub;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.token.UsernameToken;

public class NVSearchSkeletonServiceLocator extends org.apache.axis.client.Service implements org.nodevision.portal.search.axis.NVSearchSkeletonService {

    String username = "";

    String password = "";

    public NVSearchSkeletonServiceLocator() {
    }

    public NVSearchSkeletonServiceLocator(String NVSearchService_address) {
        this.NVSearchService_address = NVSearchService_address;
    }

    public NVSearchSkeletonServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public NVSearchSkeletonServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    private java.lang.String NVSearchService_address;

    public java.lang.String getNVSearchServiceAddress() {
        return NVSearchService_address;
    }

    private java.lang.String NVSearchServiceWSDDServiceName = "NVSearchService";

    public java.lang.String getNVSearchServiceWSDDServiceName() {
        return NVSearchServiceWSDDServiceName;
    }

    public void setNVSearchServiceWSDDServiceName(java.lang.String name) {
        NVSearchServiceWSDDServiceName = name;
    }

    public org.nodevision.portal.search.axis.NVSearchSkeleton getNVSearchService() throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NVSearchService_address);
        } catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNVSearchService(endpoint);
    }

    public org.nodevision.portal.search.axis.NVSearchSkeleton getNVSearchService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.nodevision.portal.search.axis.NVSearchServiceSoapBindingStub _stub = new org.nodevision.portal.search.axis.NVSearchServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getNVSearchServiceWSDDServiceName());
            return (NVSearchSkeleton) addAuthentication((Stub) _stub);
        } catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNVSearchServiceEndpointAddress(java.lang.String address) {
        NVSearchService_address = address;
    }

    /**
	 * For the given interface, get the stub implementation. If this service has
	 * no port for the given interface, then ServiceException is thrown.
	 */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.nodevision.portal.search.axis.NVSearchSkeleton.class.isAssignableFrom(serviceEndpointInterface)) {
                org.nodevision.portal.search.axis.NVSearchServiceSoapBindingStub _stub = new org.nodevision.portal.search.axis.NVSearchServiceSoapBindingStub(new java.net.URL(NVSearchService_address), this);
                _stub.setPortName(getNVSearchServiceWSDDServiceName());
                return (Remote) addAuthentication((Stub) _stub);
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
        if ("NVSearchService".equals(inputPortName)) {
            return getNVSearchService();
        } else {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return (Remote) addAuthentication((Stub) _stub);
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://search.portal.nodevision.org", "NVSearchSkeletonService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://search.portal.nodevision.org", "NVSearchService"));
        }
        return ports.iterator();
    }

    /**
	 * Set the endpoint address for the specified port name.
	 */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        if ("NVSearchService".equals(portName)) {
            setNVSearchServiceEndpointAddress(address);
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
