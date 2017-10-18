package com.entelience.tomcat;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jcifs.Config;
import jcifs.smb.NtlmPasswordAuthentication;

/**
 * This is an J2EE filter to authenticate user with NTLM and grant access based on groups memberships. It extends the Samba JCIFS 
 * <a href="http://jcifs.samba.org/src/docs/ntlmhttpauth.html">NtlmHttpFilter</a>.
 *
 * The following parameters can be defined within the <code>web.xml</code>:
 * <ul><li>com.entelience.tomcat.debug : to activate debug logs</li>
 * <li>com.entelience.tomcat.denyPage : to define the deny login page</li>
 * <li>com.entelience.tomcat.paranoid : to activate the NTLM/AD check for every request</li>
 * <li>com.entelience.tomcat.autoRedirect : to activate the redirection to the first allowed url</li>
 * <li>url: to define the url to be protected, the AD group is the value</li>
 * <li>com.entelience.tomcat.ad_base : AD base CN</li>
 * </ul>
 *
 * It uses cookies to ensure the persistent of the session. The HttpSession seems to break things with Axis.
 */
public class ExtNtlmHttpFilter extends jcifs.http.NtlmHttpFilter {

    private FilterConfig filterConfig = null;

    private Map<String, String> urlToProtect = null;

    private String denyPage = "deny.html";

    private boolean DEBUG = false;

    private boolean PARANOID = false;

    private boolean REDIRECT = false;

    private static final int max_queries = 1000;

    private int queries = 0;

    private static final String COOKIE_NAME = "ntlm_signon";

    private Double salt = null;

    /** 
     * Filter public constructor. Fetches the config parameter from the web.xml file 
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        urlToProtect = new HashMap<String, String>();
        Enumeration e = filterConfig.getInitParameterNames();
        String name = "";
        String value = null;
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            value = filterConfig.getInitParameter(name);
            if (name.startsWith("com.entelience.tomcat")) {
                Config.setProperty(name, value);
                filterConfig.getServletContext().log(this.getClass().getName() + " setting " + name + " to " + value);
            } else if (name.startsWith("url:") && value != null) {
                urlToProtect.put(value, name.substring(4));
                filterConfig.getServletContext().log(this.getClass().getName() + " limiting access to " + name + " to users from group " + value);
            }
        }
        if (Config.getProperty("com.entelience.tomcat.denyPage") == null) {
            filterConfig.getServletContext().log(this.getClass().getName() + " no deny page configured");
        } else {
            denyPage = Config.getProperty("com.entelience.tomcat.denyPage");
        }
        if (Config.getProperty("com.entelience.tomcat.debug") != null) {
            DEBUG = true;
        }
        if (Config.getProperty("com.entelience.tomcat.paranoid") != null) {
            PARANOID = true;
        }
        if (Config.getProperty("com.entelience.tomcat.autoRedirect") != null) {
            REDIRECT = true;
        }
        salt = new Double(Math.random() * 1000000);
        super.init(filterConfig);
        filterConfig.getServletContext().log(this.getClass().getName() + " is configured");
    }

    /**
     * This is called by Tomcat to purge the filter, for instance
     * on restart or shutdown
     */
    public void destroy() {
        this.filterConfig = null;
        super.destroy();
    }

    /** 
     * This is the only method that we need to overwrite 
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (filterConfig == null) {
            return;
        }
        if (++queries > max_queries) {
            queries = 0;
            salt = new Double(Math.random() * 1000000);
            if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " resetting salt");
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String url = req.getRequestURI();
        if (!isUrlProtected(url)) {
            if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " unprotected url " + url);
            chain.doFilter(request, response);
        }
        if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " filtering " + url);
        if (checkCookie(request) && !PARANOID) {
            if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " pass-through");
            chain.doFilter(request, response);
            return;
        }
        NtlmPasswordAuthentication ntlm;
        if ((ntlm = negotiate(req, resp, false)) == null) {
            filterConfig.getServletContext().log(this.getClass().getName() + " NTLM negotiate failure");
            filterConfig.getServletContext().getRequestDispatcher("/" + denyPage).forward(request, response);
            return;
        }
        String userName = ntlm.getName();
        filterConfig.getServletContext().log(this.getClass().getName() + " NTLM authenticated user is " + userName);
        List<String> e = adLookup(userName);
        Iterator<String> ie = e.iterator();
        boolean authorized = false;
        String group = "";
        String alternateUrl = "";
        while (ie.hasNext() && !authorized) {
            group = ie.next();
            if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " user " + userName + " is part of group " + group);
            if (urlToProtect.containsKey(group)) {
                alternateUrl = (String) urlToProtect.get(group);
                if ((url.startsWith(alternateUrl))) {
                    validateSignOn(request, response, alternateUrl);
                    authorized = true;
                }
            }
        }
        if (!authorized) {
            if (!"".equals(alternateUrl) && REDIRECT) {
                filterConfig.getServletContext().log(this.getClass().getName() + " redirecting user " + userName + " to a url he is allowed to " + alternateUrl);
                filterConfig.getServletContext().getRequestDispatcher(alternateUrl).forward(request, response);
            } else {
                filterConfig.getServletContext().log(this.getClass().getName() + " denying url " + url + " for user " + userName);
                filterConfig.getServletContext().getRequestDispatcher("/" + denyPage).forward(request, response);
            }
            return;
        }
        if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " url " + url + " is ok for user " + userName);
        chain.doFilter(request, response);
    }

    /**
      * Returns true if the url is one that is protected by
      * this filter.
      */
    private boolean isUrlProtected(String url) {
        Iterator<String> ie = urlToProtect.values().iterator();
        while (ie.hasNext()) {
            if (url.startsWith(ie.next())) return true;
        }
        return false;
    }

    /**
      * Returns null or the url to protect 
      */
    private String getUrlProtected(String url) {
        Iterator<String> ie = urlToProtect.values().iterator();
        String urlTP = null;
        while (ie.hasNext()) {
            urlTP = ie.next();
            if (url.startsWith(urlTP)) return urlTP;
        }
        return null;
    }

    /**
     * Sessions are stored within a cookie. I tried using the HttpSession but somehow it is screwing up what
     * Axis is doing...need to revisit this later on.
     */
    private void validateSignOn(ServletRequest request, ServletResponse response, String url) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        Cookie cookie = new Cookie(COOKIE_NAME, cookieString(url, req.getRemoteAddr()));
        cookie.setMaxAge(-1);
        cookie.setPath(url);
        res.addCookie(cookie);
        if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " cookie ");
    }

    /**
     * Returns the cookie string which contains a random part
	 * that is generated each time the filter is (re)started.
	 *
	 * Cookies can be stolen/sniffed... so turning on SSL would be a 
	 * great idea. This version lacks something unique to the user that
	 * can be retrieved without relaunching the NTLM. Any ideas ?
     */
    private String cookieString(String url, String ip) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.update((url + "&&" + ip + "&&" + salt.toString()).getBytes());
            java.math.BigInteger hash = new java.math.BigInteger(1, md.digest());
            return hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            filterConfig.getServletContext().log(this.getClass().getName() + " error " + e);
            return null;
        }
    }

    /**
     * Checks it a cookie exists for this request
     */
    private boolean checkCookie(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int loop = 0; loop < cookies.length; loop++) {
                if (DEBUG) filterConfig.getServletContext().log(this.getClass().getName() + " cookie = " + cookies[loop].getName());
                if (cookies[loop].getName().equals(COOKIE_NAME)) {
                    String cookie = cookieString(getUrlProtected(req.getRequestURI()), req.getRemoteAddr());
                    if (cookie.equals(cookies[loop].getValue())) return true;
                }
            }
        }
        return false;
    }

    /**
     * Lookup the groups of user in a domain
     */
    private List<String> adLookup(String userName) {
        List<String> groups = new ArrayList<String>();
        try {
            String DOMAIN_CONTROLER = Config.getProperty("jcifs.http.domainController");
            String DOMAIN_USER = Config.getProperty("jcifs.smb.client.username");
            String DOMAIN_PWD = Config.getProperty("jcifs.smb.client.password");
            String DOMAIN = Config.getProperty("jcifs.smb.client.domain");
            String userLogon = userName.substring(DOMAIN.length() + 1);
            String AD_BASE = Config.getProperty("com.entelience.tomcat.ad_base");
            String AD_SEARCH = Config.getProperty("com.entelience.tomcat.ad_search", "(&(objectClass=user)(CN=" + userLogon + "))");
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, DOMAIN + "\\" + DOMAIN_USER);
            env.put(Context.SECURITY_CREDENTIALS, DOMAIN_PWD);
            String url = "ldap://" + DOMAIN_CONTROLER + ":389";
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, url);
            if (DEBUG) filterConfig.getServletContext().log("Connecting to " + url);
            DirContext ctx;
            ctx = new InitialDirContext(env);
            SearchControls ctls = new SearchControls();
            String returnedAtts[] = { "memberOf" };
            ctls.setReturningObjFlag(false);
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ctls.setReturningAttributes(returnedAtts);
            NamingEnumeration answer = ctx.search(AD_BASE, AD_SEARCH, ctls);
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                Attributes attrs = sr.getAttributes();
                if (attrs != null) {
                    try {
                        for (NamingEnumeration ae = attrs.getAll(); ae.hasMore(); ) {
                            Attribute attr = (Attribute) ae.next();
                            if (DEBUG) filterConfig.getServletContext().log("Attribute: " + attr.getID());
                            for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                                String group[] = (e.next().toString()).split(",");
                                if (group[0].startsWith("CN=")) {
                                    groups.add(group[0].substring(3));
                                } else {
                                    filterConfig.getServletContext().log("Expecting a CN in " + group);
                                }
                            }
                        }
                    } catch (NamingException e) {
                        filterConfig.getServletContext().log(this.getClass().getName() + " Problem listing membership: " + e);
                    }
                }
            }
            ctx.close();
        } catch (Exception e) {
            filterConfig.getServletContext().log(this.getClass().getName() + " error " + e);
        }
        return groups;
    }
}
