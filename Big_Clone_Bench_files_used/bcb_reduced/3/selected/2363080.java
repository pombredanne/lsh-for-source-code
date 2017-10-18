package com.liferay.portal.auth;

import java.io.Serializable;
import com.liferay.util.Encryptor;

/**
 * <a href="HttpPrincipal.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class HttpPrincipal implements Serializable {

    public HttpPrincipal(String url) {
        _url = url;
    }

    public HttpPrincipal(String url, String userId, String password) {
        this(url, userId, password, false);
    }

    public HttpPrincipal(String url, String userId, String password, boolean digested) {
        _url = url;
        _userId = userId;
        if (digested) {
            _password = password;
        } else {
            _password = Encryptor.digest(password);
        }
    }

    public String getUrl() {
        return _url;
    }

    public String getCompanyId() {
        return _companyId;
    }

    public void setCompanyId(String companyId) {
        _companyId = companyId;
    }

    public String getUserId() {
        return _userId;
    }

    public String getPassword() {
        return _password;
    }

    private String _url;

    private String _companyId;

    private String _userId;

    private String _password;
}
