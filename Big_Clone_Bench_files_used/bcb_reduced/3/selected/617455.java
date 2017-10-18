package org.one.stone.soup.wiki.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.one.stone.soup.file.FileHelper;
import sun.misc.BASE64Encoder;
import com.sun.java.browser.net.ProxyInfo;
import com.sun.java.browser.net.ProxyService;

public class WikiURLConnection {

    public static final String WWW_AUTHENTICATION = "WWW_AUTHENTICATION";

    public static final String SESSION_COOKIE_AUTHENTICATION = "SESSION_COOKIE_AUTHENTICATION";

    private URL targetUrl;

    private String proxyHost = null;

    private int proxyPort = -1;

    private Socket connection;

    private String authenticationType = SESSION_COOKIE_AUTHENTICATION;

    private String cookie = null;

    private String authentication;

    private String userId;

    private String password;

    public WikiURLConnection(URL url) {
        this(url, null);
    }

    public WikiURLConnection(URL url, String authentication) {
        this.authentication = authentication;
        this.targetUrl = url;
        proxyHost = null;
        proxyPort = -1;
        try {
            ProxyInfo info[] = ProxyService.getProxyInfo(url);
            if (info != null && info.length > 0 && info[0] != null) {
                proxyPort = info[0].getPort();
                if (proxyPort != -1) {
                    proxyHost = info[0].getHost();
                    System.out.println("Proxy set as " + proxyHost + ":" + proxyPort);
                }
            }
        } catch (Exception ex) {
        }
    }

    private Socket getSocketForUrl() throws IOException {
        String host = targetUrl.getHost();
        int port = targetUrl.getPort();
        if (port == -1) {
            port = 80;
        }
        return new Socket(host, port);
    }

    public Socket openPutConnection(String pageName, String fileName, long length) throws IOException {
        connection = getSocketForUrl();
        OutputStream oStream = connection.getOutputStream();
        if (proxyHost != null) {
            proxyConnect(oStream);
        }
        oStream.write(new String("PUT " + pageName + "/" + fileName + " HTTP/1.0\r\n").getBytes());
        oStream.write(new String("Content-Length: " + length + "\r\n").getBytes());
        addStandardHeader(oStream);
        oStream.write(new String("\r\n").getBytes());
        oStream.flush();
        return connection;
    }

    public Socket openGetConnection(String pageName) throws IOException {
        return openGetConnection(pageName, null);
    }

    public Socket openGetConnection(String pageName, String fileName) throws IOException {
        connection = getSocketForUrl();
        OutputStream oStream = connection.getOutputStream();
        if (proxyHost != null) {
            proxyConnect(oStream);
        }
        if (fileName == null) {
            oStream.write(new String("GET " + pageName + " HTTP/1.0\r\n").getBytes());
        } else {
            oStream.write(new String("GET " + pageName + "/" + fileName + " HTTP/1.0\r\n").getBytes());
        }
        addStandardHeader(oStream);
        oStream.write(new String("\r\n").getBytes());
        oStream.flush();
        return connection;
    }

    public Socket openPostConnection(String pageName, String fileName, long dataLength) throws IOException {
        connection = getSocketForUrl();
        OutputStream oStream = connection.getOutputStream();
        if (proxyHost != null) {
            proxyConnect(oStream);
        }
        if (fileName == null) {
            oStream.write(new String("POST " + pageName + " HTTP/1.0\r\n").getBytes());
        } else {
            oStream.write(new String("POST " + pageName + "/" + fileName + " HTTP/1.0\r\n").getBytes());
        }
        addStandardHeader(oStream);
        oStream.write(new String("Content-Length: " + dataLength + "\r\n").getBytes());
        oStream.write(new String("\r\n").getBytes());
        oStream.flush();
        return connection;
    }

    public Socket openDeleteConnection(String pageName, String fileName) throws IOException {
        connection = getSocketForUrl();
        OutputStream oStream = connection.getOutputStream();
        if (proxyHost != null) {
            proxyConnect(oStream);
        }
        oStream.write(new String("DELETE " + pageName + "/" + fileName + " HTTP/1.0\r\n").getBytes());
        addStandardHeader(oStream);
        oStream.write(new String("\r\n").getBytes());
        oStream.flush();
        return connection;
    }

    private void proxyConnect(OutputStream oStream) throws IOException {
        oStream.write(new String("CONNECT " + proxyHost + ":" + proxyPort + "\r\n").getBytes());
        oStream.write(new String("HOST " + proxyHost + ":" + proxyPort + "\r\n").getBytes());
    }

    private void addStandardHeader(OutputStream oStream) throws IOException {
        if (authentication != null && authentication.length() > 0) {
            oStream.write(new String("Authorization: " + authentication + "\r\n").getBytes());
        }
        if (cookie != null && cookie.length() > 0) {
            oStream.write(new String("Cookie: " + cookie + "\r\n").getBytes());
        }
        oStream.write(new String("Host: " + targetUrl.getHost() + "\r\n").getBytes());
        oStream.write(new String("User-Agent: OpenForum Wiki Applet\r\n").getBytes());
    }

    public void setCookieAuthentication(String userId, String password) throws Exception {
        authenticationType = SESSION_COOKIE_AUTHENTICATION;
        setAuthentication(userId, password);
        URLConnection connection = new URL("http://services.rensmart.com/SignIn?action=getChallenge").openConnection();
        String challenge = new String(FileHelper.readFile(connection.getInputStream()));
        String challengeResponse = getMD5(password + challenge);
        String data = "isBrowser=false&memberName=" + userId + "&challenge=" + challenge + "&challengeResponse=" + challengeResponse;
        connection = new URL("http://services.rensmart.com/SignIn").openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.getOutputStream().write(data.getBytes());
        connection.getOutputStream().flush();
        String result = new String(FileHelper.readFile(connection.getInputStream()));
        if (result.startsWith("wikiSession=")) {
            cookie = result;
            System.out.println("Authenticated");
        } else {
            System.out.println("Authentication Failed:" + result);
        }
    }

    public void setAuthentication(String userId, String password) {
        this.userId = userId;
        this.password = password;
        if (userId != null) {
            String auth = new BASE64Encoder().encode((userId + ":" + password).getBytes());
            authentication = "Basic=" + auth;
        }
    }

    public void disconnect() throws IOException {
        connection.close();
    }

    private String getMD5(String data) {
        try {
            MessageDigest md5Algorithm = MessageDigest.getInstance("MD5");
            md5Algorithm.update(data.getBytes(), 0, data.length());
            byte[] digest = md5Algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            String hexDigit = null;
            for (int i = 0; i < digest.length; i++) {
                hexDigit = Integer.toHexString(0xFF & digest[i]);
                if (hexDigit.length() < 2) {
                    hexDigit = "0" + hexDigit;
                }
                hexString.append(hexDigit);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ne) {
            return data;
        }
    }
}
