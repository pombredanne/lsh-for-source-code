package com.liferay.jbpm.handler;

import com.liferay.client.portal.service.http.GroupServiceSoap;
import com.liferay.client.portal.service.http.GroupServiceSoapServiceLocator;
import com.liferay.client.portal.service.http.RoleServiceSoap;
import com.liferay.client.portal.service.http.RoleServiceSoapServiceLocator;
import com.liferay.client.portal.service.http.UserServiceSoap;
import com.liferay.client.portal.service.http.UserServiceSoapServiceLocator;
import com.liferay.util.Encryptor;
import com.liferay.util.portlet.PortletProps;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <a href="DefaultHandler.java.html"><b><i>View Source</i></b></a>
 *
 * @author Charles May
 *
 */
public class DefaultHandler {

    protected URL getURL(String serviceName) throws MalformedURLException {
        return getURL(serviceName, true);
    }

    protected URL getURL(String serviceName, boolean authenticated) throws MalformedURLException {
        String url = PortletProps.get("soap.url");
        if (authenticated) {
            String userId = PortletProps.get("soap.user.id");
            String password = Encryptor.digest(PortletProps.get("soap.password"));
            int pos = url.indexOf("://");
            String protocol = url.substring(0, pos + 3);
            String host = url.substring(pos + 3, url.length());
            url = protocol + userId + ":" + password + "@" + host + "/tunnel-web/secure/axis/" + serviceName;
        } else {
            url += "/tunnel-web/axis/" + serviceName;
        }
        return new URL(url);
    }

    protected GroupServiceSoap getGroupService() throws Exception {
        GroupServiceSoapServiceLocator locator = new GroupServiceSoapServiceLocator();
        GroupServiceSoap service = locator.getPortal_GroupService(getURL("Portal_GroupService"));
        return service;
    }

    protected RoleServiceSoap getRoleService() throws Exception {
        RoleServiceSoapServiceLocator locator = new RoleServiceSoapServiceLocator();
        RoleServiceSoap service = locator.getPortal_RoleService(getURL("Portal_RoleService"));
        return service;
    }

    protected UserServiceSoap getUserService() throws Exception {
        UserServiceSoapServiceLocator locator = new UserServiceSoapServiceLocator();
        UserServiceSoap service = locator.getPortal_UserService(getURL("Portal_UserService"));
        return service;
    }

    protected String swimlane;

    protected String msg;
}
