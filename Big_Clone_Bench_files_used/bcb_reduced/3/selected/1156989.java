package com.openbravo.possync;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.pos.util.Base64Encoder;
import com.openbravo.ws.customers.Customer;
import com.openbravo.ws.customers.WebServiceImpl;
import com.openbravo.ws.customers.WebServiceImplServiceLocator;
import com.openbravo.ws.externalsales.ExternalSalesImpl;
import com.openbravo.ws.externalsales.ExternalSalesImplServiceLocator;
import com.openbravo.ws.externalsales.Order;
import com.openbravo.ws.externalsales.Product;
import com.openbravo.ws.externalsales.ProductPlus;

public class ExternalSalesHelper {

    private ExternalSalesImpl externalSales;

    private WebServiceImpl externalCustomers;

    private String m_sERPUser;

    private String m_sERPPassword;

    private int m_iERPId;

    private int m_iERPOrg;

    private int m_iERPPos;

    /** Creates a new instance of WebServiceHelper */
    public ExternalSalesHelper(DataLogicSystem dlsystem) throws BasicException, ServiceException, MalformedURLException {
        Properties prop = dlsystem.getResourceAsProperties("openbravo.properties");
        if (prop == null) {
            throw new BasicException(AppLocal.getIntString("message.propsnotdefined"));
        } else {
            String url = prop.getProperty("url");
            if (url == null || url.equals("")) {
                throw new BasicException(AppLocal.getIntString("message.urlnotdefined"));
            } else {
                url = url.trim();
                if (url.endsWith("/ExternalSales")) {
                    url = url.substring(0, url.length() - 14);
                }
                externalSales = new ExternalSalesImplServiceLocator().getExternalSales(new URL(url + "/ExternalSales"));
                externalCustomers = new WebServiceImplServiceLocator().getWebService(new URL(url + "/WebService"));
                m_sERPUser = prop.getProperty("user");
                m_sERPPassword = prop.getProperty("password");
                if (m_sERPUser != null && m_sERPPassword != null && m_sERPPassword.startsWith("crypt:")) {
                    AltEncrypter cypher = new AltEncrypter("key" + m_sERPUser);
                    m_sERPPassword = cypher.decrypt(m_sERPPassword.substring(6));
                }
                m_sERPPassword = getPasswordHash(m_sERPPassword);
                m_iERPId = Integer.parseInt(prop.getProperty("id"));
                m_iERPOrg = Integer.parseInt(prop.getProperty("org"));
                m_iERPPos = Integer.parseInt(prop.getProperty("pos"));
            }
        }
    }

    public Customer[] getCustomers() throws RemoteException {
        return externalCustomers.getCustomers(m_iERPId, m_sERPUser, m_sERPPassword);
    }

    public Product[] getProductsCatalog() throws RemoteException {
        return externalSales.getProductsCatalog(m_iERPId, m_iERPOrg, m_iERPPos, m_sERPUser, m_sERPPassword);
    }

    public ProductPlus[] getProductsPlusCatalog() throws RemoteException {
        return externalSales.getProductsPlusCatalog(m_iERPId, m_iERPOrg, m_iERPPos, m_sERPUser, m_sERPPassword);
    }

    public void uploadOrders(Order[] orderstoupload) throws RemoteException {
        externalSales.uploadOrders(m_iERPId, m_iERPOrg, m_iERPPos, orderstoupload, m_sERPUser, m_sERPPassword);
    }

    private static String getPasswordHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte raw[] = md.digest(password.getBytes("UTF-8"));
            return Base64Encoder.encode(raw);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
