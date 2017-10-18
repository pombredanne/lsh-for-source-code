package com.liferay.util.spring.remoting;

import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.util.GetterUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.apache.commons.codec.binary.Base64;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;

/**
 * <a href="AuthenticatingHttpInvokerRequestExecutor.java.html"><b><i>
 * View Source</i></b></a>
 *
 * <p>
 * An HttpInvoker request executor for Spring Remoting that provides HTTP BASIC
 * authentication information for service invocations.
 * </p>
 *
 * @author Joel Kozikowski
 *
 */
public class AuthenticatingHttpInvokerRequestExecutor extends SimpleHttpInvokerRequestExecutor {

    public AuthenticatingHttpInvokerRequestExecutor() {
        super();
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = Digester.digest(password);
    }

    public String getUserId() {
        return _userId;
    }

    public void setUserId(String userId) {
        _userId = userId;
    }

    /**
	 * Called every time a HTTP invocation is made. This implementation allows
	 * the parent to setup the connection, and then adds an
	 * <code>Authorization</code> HTTP header property for BASIC authentication.
	 */
    protected void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
        prepareConnection(con, contentLength);
        if (getUserId() != null) {
            String password = GetterUtil.getString(getPassword());
            String base64 = getUserId() + StringPool.COLON + password;
            con.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(base64.getBytes())));
        }
    }

    private String _userId;

    private String _password;
}
