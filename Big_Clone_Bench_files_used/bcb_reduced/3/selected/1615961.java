package com.ampedlabs.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ampedlabs.FileServlet;
import com.ampedlabs.util.*;

public class UserManager {

    protected String appPath = "";

    protected ServletContext context = null;

    protected HttpServletRequest request = null;

    protected String reportRunDirectory = "";

    protected String hostname = "nohost";

    protected String ipaddress = "noip";

    protected String userName = "guest";

    protected String password = encrypt("nopass");

    protected int history = 0;

    protected int maxHistory = 10;

    protected Properties reportProperties = null;

    protected Properties userProperties = null;

    protected boolean ipSet = false;

    private boolean authenticated = false;

    protected String Icode = "";

    private Log log = LogFactory.getLog(this.getClass().getName());

    public UserManager() {
        super();
        init();
    }

    public UserManager(ServletContext inContext, String inPath) {
        super();
        context = inContext;
        appPath = inPath;
        init();
    }

    protected void init() {
        String appPathInternal = "";
    }

    public String getIcode() {
        if (null == Icode || Icode.equals("")) {
            Icode = getNewIcode();
        }
        return Icode;
    }

    public String getNewIcode() {
        Icode = encrypt(getIdentity());
        writeUserData(Icode);
        return Icode;
    }

    public void readUserProperties() {
    }

    public void writeUserProperties() {
    }

    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    public ServletContext getContext() {
        return context;
    }

    public void setContext(ServletContext context) {
        this.context = context;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = Strings.nvl(ipaddress, "0.0.0.0");
        ipSet = true;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = encrypt(Strings.nvl(password, "nopass"));
    }

    public String getIdentity() {
        return Strings.nvl(userName, "guest") + "." + Strings.nvl(password);
    }

    public String getIdentity(String Icode) {
        getUserProperties(Icode);
        return Strings.nvl(userName, "guest") + "." + Strings.nvl(password);
    }

    public String getUserName() {
        return Strings.nvl(userName, "guest");
    }

    public String getUserPassword() {
        return Strings.nvl(password, encrypt("nopass"));
    }

    public void setUserName(String userName) {
        this.userName = Strings.nvl(userName, "guest");
    }

    public int getNextHistory() {
        if (history > maxHistory) {
            history = 0;
        }
        history++;
        return history;
    }

    public void setHistory(int history) {
        this.history = history;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public Properties getReportProperties() {
        return reportProperties;
    }

    public void setReportProperties(Properties reportProperties) {
        this.reportProperties = reportProperties;
        setMaxHistory(Integer.valueOf(reportProperties.getProperty("user.report.history", "10")).intValue());
        setReportRunDirectory(reportProperties.getProperty("user.report.data"));
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        if (request != null) {
            this.request = request;
            if (!ipSet) {
                if (null != request) {
                    String remoteIp = request.getParameter("REMOTE_ADDR");
                    if (null != remoteIp && remoteIp != "") {
                        setIpaddress(remoteIp);
                    }
                    String remoteHost = Strings.nvl(request.getRemoteHost(), "noip");
                    if (!remoteHost.equals(remoteIp)) {
                        setHostname(remoteHost);
                    }
                }
            }
            String inputIcode = request.getParameter("Icode");
            if ((inputIcode != null && !inputIcode.equals(""))) {
                setUserData(inputIcode);
            } else {
                inputIcode = request.getParameter("inputIcode");
                if ((inputIcode != null && !inputIcode.equals(""))) {
                    setUserData(inputIcode);
                } else {
                    if (Icode.equals("")) {
                        getUserProperties("default");
                    }
                }
            }
        }
    }

    public void setUserData(String inputIcode) {
        if ((inputIcode != null && !inputIcode.equals("")) && (null == Icode || Icode.equals("")) || (!inputIcode.equals(Icode))) {
            Icode = inputIcode;
            getUserProperties(Icode);
            String uName = Strings.nvl(request.getParameter("UMUserName"));
            String setName = Strings.nvl(userName);
            if (uName.equals("guest")) {
                return;
            }
            if ((!uName.equals("") && setName.equals("")) || (!uName.equals(setName))) {
                setUserName(uName);
                getNewIcode();
            }
            String uPass = Strings.nvl(request.getParameter("UMUserPassword"));
            String setPass = Strings.nvl(password);
            if ((!uPass.equals("") && setPass.equals("")) || (!uPass.equals(setPass))) {
                if (authenticated) {
                    setPassword(uPass);
                    getNewIcode();
                }
            }
        }
    }

    public int getHistory() {
        return history;
    }

    public String getReportRunDirectory() {
        return reportRunDirectory;
    }

    public void setReportRunDirectory(String reportRunDirectory) {
        this.reportRunDirectory = reportRunDirectory;
    }

    public String getReportName(int history) {
        String reportName = Strings.nvl(getReportRunDirectory() + "/" + getIcode() + ".rpt.", "/user/report/data/guest.0.0.0.0.rpt.");
        reportName = reportName + Strings.nvl(String.valueOf(history));
        return Strings.nvl(reportName);
    }

    public static String encrypt(String s) {
        try {
            byte[] message = null;
            MessageDigest dig = MessageDigest.getInstance("MD5");
            String encoding = "CP1252";
            try {
                message = s.getBytes(encoding);
                encoding = String.valueOf(System.getProperty("file.encoding"));
            } catch (UnsupportedEncodingException e) {
                message = s.getBytes();
            }
            dig.update(message);
            String encrYptEd = hex(dig.digest());
            return encrYptEd;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Properties getUserProperties() {
        return userProperties;
    }

    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toUpperCase().substring(1, 3));
        }
        return sb.toString();
    }

    public String md5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    public String readUserDataFile(String Icode) {
        Properties rProp = getReportProperties();
        String staticParmDir = Strings.nvl(String.valueOf(rProp.get("user.prop.data")));
        String fileName = appPath + staticParmDir + "/" + Icode + ".properties";
        String userData = "";
        try {
            FileServlet fr = new FileServlet();
            userData = fr.readDataFromDisk(fileName);
        } catch (Exception e) {
            userData = e.getMessage();
        }
        return userData;
    }

    public String writeUserData(String inputIcode) {
        String retIcode = inputIcode;
        StringBuffer udata = new StringBuffer("# User properties");
        if (null == inputIcode || inputIcode.length() < 10) {
            retIcode = getNewIcode();
        }
        if (!retIcode.equals(Icode)) {
            retIcode = getNewIcode();
            Icode = retIcode;
        }
        udata.append("\nuserName=" + userName);
        udata.append("\npassword=" + password);
        udata.append("\nipaddress=" + ipaddress);
        udata.append("\nhostname=" + hostname);
        writeUserDataFile(inputIcode, udata);
        return retIcode;
    }

    public String writeUserDataFile(String Icode, StringBuffer udata) {
        Properties rProp = getReportProperties();
        String staticParmDir = Strings.nvl(String.valueOf(rProp.get("user.prop.data")));
        String fileName = appPath + staticParmDir + "/" + Icode + ".properties";
        String message = "file written.";
        try {
            FileServlet fr = new FileServlet();
            fr.writeDataToDisk(udata.toString(), fileName);
        } catch (Exception e) {
            message = "Error writing report data to disk: " + e.getMessage();
            log.error(message);
        }
        return message;
    }

    public boolean authenticateUser(String inIcode, String inName, String inPass) {
        if (!authenticated) {
            String inputIcode = Strings.nvl(inIcode);
            getUserProperties(inputIcode);
            String uName = Strings.nvl(inName);
            String uPass = Strings.nvl(inPass);
            if (!uName.equals("") && !uPass.equals("")) {
                if (uName.equals(userName) && encrypt(uPass).equals(password)) {
                    authenticated = true;
                    return authenticated;
                }
            }
            authenticated = false;
            return authenticated;
        } else {
            return authenticated;
        }
    }

    public boolean authenticateUser(HttpServletRequest request) {
        if (!authenticated) {
            String inputIcode = Strings.nvl(request.getParameter("Icode"));
            getUserProperties(inputIcode);
            String uName = Strings.nvl(request.getParameter("UMUserName"));
            String uPass = Strings.nvl(request.getParameter("UMPassword"));
            if (!uName.equals("") && !uPass.equals("")) {
                if (uName.equals(userName) && encrypt(uPass).equals(password)) {
                    authenticated = true;
                    return authenticated;
                }
            }
            authenticated = false;
            return authenticated;
        } else {
            return authenticated;
        }
    }

    public Properties getUserProperties(String inputIcode) {
        Properties rProp = getReportProperties();
        String staticParmDir = Strings.nvl(String.valueOf(rProp.get("user.prop.data")));
        String fileName = appPath + staticParmDir + "/" + inputIcode + ".properties";
        Properties uProp = new Properties();
        try {
            File propertyFile = new File(fileName);
            FileInputStream fin = new FileInputStream(propertyFile);
            uProp.load(fin);
        } catch (IOException e) {
            authenticated = true;
        }
        if (null != uProp) {
            userName = uProp.getProperty("userName", "guest");
            password = uProp.getProperty("password", "nopass");
        }
        userProperties = uProp;
        return uProp;
    }

    public boolean auth(String input, String storedCrypt) {
        if (encrypt(input).equals(storedCrypt)) {
            return true;
        } else {
            return false;
        }
    }
}
