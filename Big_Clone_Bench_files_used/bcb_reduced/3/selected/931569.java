package com.liferay.client.portal.service.http;

import com.liferay.portal.kernel.util.Digester;
import com.liferay.test.TestCase;
import com.liferay.test.TestProps;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <a href="BaseSoapTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class BaseSoapTest extends TestCase {

    protected URL getURL(String serviceName) throws MalformedURLException {
        return getURL(serviceName, true);
    }

    protected URL getURL(String serviceName, boolean authenticated) throws MalformedURLException {
        String url = TestProps.get("soap.url");
        if (authenticated) {
            String userId = TestProps.get("soap.user.id");
            String password = Digester.digest(TestProps.get("soap.password"));
            int pos = url.indexOf("://");
            String protocol = url.substring(0, pos + 3);
            String host = url.substring(pos + 3, url.length());
            url = protocol + userId + ":" + password + "@" + host + "/tunnel-web/secure/axis/" + serviceName;
        } else {
            url += "/tunnel-web/axis/" + serviceName;
        }
        return new URL(url);
    }
}
