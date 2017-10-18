package es.alvsanand.webpage.web.beans.session;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.apache.commons.codec.binary.Base64;
import es.alvsanand.webpage.common.Logger;
import es.alvsanand.webpage.security.exception.AuthenticationException;
import es.alvsanand.webpage.services.security.CryptographyService;
import es.alvsanand.webpage.services.security.CryptographyServiceImpl;
import es.alvsanand.webpage.services.security.LoginService;
import es.alvsanand.webpage.services.security.LoginServiceImpl;

/**
 * 
 * 
 * @author alvaro.santos
 * @date 30/11/2009
 * 
 */
@RequestScoped
@ManagedBean(name = "loginBean")
public class LoginBean implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 8778520431388212339L;

    private static final transient Logger logger = new Logger(LoginBean.class);

    private static final transient String LOGIN_VIEW_ID = "pretty:home";

    private static final transient String LOGOUT_VIEW_ID = "pretty:home";

    public static final transient String GOOGLE_SSO_LOGIN_URL = "pretty:loginGoogleSSO";

    public static final transient String GOOGLE_SSO_LOGOUT_URL = "pretty:logoutGoogleSSO";

    private transient CryptographyService cryptographyService;

    private transient LoginService loginService;

    private String password;

    private String username;

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

    public CryptographyService getCryptographyService() {
        if (cryptographyService == null) {
            cryptographyService = new CryptographyServiceImpl();
        }
        return cryptographyService;
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginServiceImpl();
        }
        return loginService;
    }

    public String getloginGoogleSSOURL() throws AuthenticationException {
        logger.info("Launched LoginBean.getloginGoogleSSOURL");
        return getLoginService().getloginGoogleSSOURL();
    }

    public String getLogoutGoogleSSOURL() throws AuthenticationException {
        logger.info("Launched LoginBean.getLogoutGoogleSSOURL");
        return getLoginService().getLogoutGoogleSSOURL();
    }

    public String login() throws AuthenticationException {
        logger.info("Launched LoginBean.login[" + username + "]");
        String passwordDigested = null;
        if (getPassword() != null) {
            try {
                passwordDigested = new String(Base64.encodeBase64(getCryptographyService().digest(getPassword().getBytes())));
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            }
        }
        getLoginService().loginByUsername(username, passwordDigested);
        return LOGIN_VIEW_ID;
    }

    public String logout() throws AuthenticationException {
        logger.info("Launched LoginBean.logout");
        getLoginService().logout();
        return LOGOUT_VIEW_ID;
    }

    public String loginGoogleSSO() throws AuthenticationException {
        logger.info("Launched LoginBean.loginGoogleSSO");
        getLoginService().loginGoogleSSO();
        return LOGIN_VIEW_ID;
    }

    public String logoutGoogleSSO() throws AuthenticationException {
        logger.info("Launched LoginBean.logoutGoogleSSO");
        getLoginService().logoutGoogleSSO();
        return LOGOUT_VIEW_ID;
    }
}
