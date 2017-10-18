package com.progiweb.fbconnect.session;

import com.progiweb.fbconnect.FacebookConfig;
import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.userprofiling.Profile;
import atg.userprofiling.ProfileRequest;
import atg.userprofiling.ProfileRequestServlet;
import javax.servlet.http.Cookie;
import java.util.TreeSet;
import java.util.Iterator;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class FacebookConnector extends GenericService {

    private FacebookConfig mFbConfig;

    private String mConnectContextPath;

    private String mProfilePath;

    private String mLogoutURL;

    private String mSiteHttpServerName;

    private int mSiteHttpServerPort;

    private String mApiKey, mCookiePrefix, mCookieUser, mCookieLogout;

    /**
   * Initializes the service and sets the properties
   * @throws ServiceException if errors in the service initialization
   */
    public void doStartService() throws ServiceException {
        super.doStartService();
        mApiKey = mFbConfig.getApiKey();
        mCookiePrefix = mApiKey + '_';
        mCookieUser = mCookiePrefix + FacebookConnectContext.USER_ID;
        mCookieLogout = mCookiePrefix + "logout";
    }

    /**
   * Returns true if the current request comes from a browser logged into Facebook
   * @param pRequest dynamo request
   * @param pResponse dynamo response
   * @return true if the browser is logged into Facebook, false otherwise
   */
    public boolean isLoggedInFacebook(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) {
        String ctx = "isLoggedInFacebook - ";
        Cookie[] cookies = pRequest.getCookies();
        if (cookies == null || cookies.length == 0) {
            return false;
        }
        FacebookConnectContext connectContext = (FacebookConnectContext) pRequest.resolveName(getConnectContextPath());
        String signature = "";
        Cookie logoutCookie = null;
        TreeSet sortedFields = new TreeSet();
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            String cookieName = cookie.getName();
            if (mApiKey.equals(cookieName)) {
                signature = cookie.getValue();
            } else if (cookieName.startsWith(mApiKey)) {
                String field = cookieName.substring(mCookiePrefix.length());
                if (!field.equals(mCookieLogout)) {
                    sortedFields.add(field);
                    connectContext.setPropertyValue(field, cookie.getValue());
                } else {
                    logoutCookie = cookie;
                }
            }
        }
        if (signature.length() == 0 || sortedFields.size() == 0 || connectContext.getPropertyValue(FacebookConnectContext.USER_ID) == null) {
            return false;
        }
        StringBuffer base = new StringBuffer();
        for (Iterator it = sortedFields.iterator(); it.hasNext(); ) {
            String sField = (String) it.next();
            base.append(sField).append('=').append(connectContext.getPropertyValue(sField));
        }
        base.append(mFbConfig.getSecretKey());
        try {
            String hash = md5(base.toString());
            if (hash.equals(signature)) {
                if (logoutCookie != null) {
                    logoutCookie.setMaxAge(0);
                    logoutCookie.setPath("/");
                    pResponse.addCookie(logoutCookie);
                }
                Profile profile = (Profile) pRequest.resolveName(mProfilePath);
                profile.setPropertyValue("facebookUserId", connectContext.getPropertyValue(FacebookConnectContext.USER_ID));
                return true;
            }
        } catch (NoSuchAlgorithmException nsae) {
            if (isLoggingError()) {
                logError(ctx + "could not make a MD5 digest", nsae);
            }
        }
        return false;
    }

    /**
   * Returns a Facebook logout URL for the application
   * @param pRequest dynamo request
   * @param pLogoutSuccessURL original logout success url as configured in the JSP page
   * @return url to call to log the user out of Facebook, or the original URL if the user is not logged into Facebook
   */
    public String getLogoutFacebookURL(DynamoHttpServletRequest pRequest, String pLogoutSuccessURL) {
        String ctx = "getLogoutFacebookURL - ";
        FacebookConnectContext connectContext = (FacebookConnectContext) pRequest.resolveName(getConnectContextPath());
        String sessionKey = (String) connectContext.getPropertyValue(FacebookConnectContext.SESSION_KEY);
        if (sessionKey != null && !"".equals(sessionKey)) {
            String absoluteSuccessURL = getAbsoluteURL(pRequest, pLogoutSuccessURL);
            if (absoluteSuccessURL.indexOf(ProfileRequestServlet.LOGOUT_PARAM) == -1) {
                absoluteSuccessURL += "?" + ProfileRequestServlet.LOGOUT_PARAM + "=true";
            }
            String encodedLogoutSuccessURL = null;
            String encodedSessionKey = null;
            try {
                encodedLogoutSuccessURL = URLEncoder.encode(absoluteSuccessURL, "UTF-8");
                encodedSessionKey = URLEncoder.encode(sessionKey, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                if (isLoggingError()) {
                    logError(ctx + "could not encode URL", uee);
                }
            }
            StringBuffer logoutFacebookURL = new StringBuffer(mLogoutURL);
            logoutFacebookURL.append("?app_key=").append(mFbConfig.getApiKey());
            logoutFacebookURL.append("&session_key=").append(encodedSessionKey);
            logoutFacebookURL.append("&next=").append(encodedLogoutSuccessURL);
            return logoutFacebookURL.toString();
        }
        return pLogoutSuccessURL;
    }

    /**
   * Returns an absolute URL corresponding to the original URL
   * @param pRequest dynamo request
   * @param pOriginalURL original logout success URL
   * @return absolute logout success URL
   * Make sure to configure the /atg/dynamo/Configuration.siteHttpServerName property to have the correct URL returned
   */
    private String getAbsoluteURL(DynamoHttpServletRequest pRequest, String pOriginalURL) {
        String ctx = "getAbsoluteURL - ";
        if (!pOriginalURL.startsWith("http://") && !pOriginalURL.startsWith("https://")) {
            String reqURL = pRequest.getRequestURL().toString();
            String serverName = pRequest.getServerName();
            int serverNamePos = reqURL.indexOf(serverName);
            String serverPort = String.valueOf(pRequest.getServerPort());
            int serverPortPos = reqURL.indexOf(serverPort);
            String absoluteURL = reqURL.substring(0, serverNamePos) + getSiteHttpServerName();
            if (serverPortPos != -1) {
                absoluteURL += ":" + getSiteHttpServerPort() + reqURL.substring(serverPortPos + serverPort.length());
            } else {
                absoluteURL += reqURL.substring(serverPortPos);
            }
            if (isLoggingDebug()) {
                logDebug(ctx + "original URL = " + pOriginalURL + "  - absolute URL = " + absoluteURL);
            }
            return absoluteURL;
        }
        return pOriginalURL;
    }

    /**
   * Returns the id of the user making the request
   * @param pRequest dynamo request
   * @return user id
   */
    public String getUserId(DynamoHttpServletRequest pRequest) {
        return pRequest.getCookieParameter(mCookieUser);
    }

    public String getConnectContextPath() {
        return mConnectContextPath;
    }

    public void setConnectContextPath(String pConnectContextPath) {
        mConnectContextPath = pConnectContextPath;
    }

    public String getProfilePath() {
        return mProfilePath;
    }

    public void setProfilePath(String pProfilePath) {
        mProfilePath = pProfilePath;
    }

    public String getLogoutURL() {
        return mLogoutURL;
    }

    public void setLogoutURL(String pLogoutURL) {
        mLogoutURL = pLogoutURL;
    }

    public FacebookConfig getFbConfig() {
        return mFbConfig;
    }

    public void setFbConfig(FacebookConfig pFbConfig) {
        mFbConfig = pFbConfig;
    }

    public String getSiteHttpServerName() {
        return mSiteHttpServerName;
    }

    public void setSiteHttpServerName(String pSiteHttpServerName) {
        mSiteHttpServerName = pSiteHttpServerName;
    }

    public int getSiteHttpServerPort() {
        return mSiteHttpServerPort;
    }

    public void setSiteHttpServerPort(int pSiteHttpServerPort) {
        mSiteHttpServerPort = pSiteHttpServerPort;
    }

    /**
   * Calculates and returns the MD5 Hash from a string.
   * @param base the string to get the MD5 hash from
   * @return the MD5 hash
   * @throws NoSuchAlgorithmException if the MD5 algorithm is not available
   */
    private static String md5(String base) throws NoSuchAlgorithmException {
        MessageDigest hash;
        hash = MessageDigest.getInstance("MD5");
        byte[] data = hash.digest(base.getBytes());
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
