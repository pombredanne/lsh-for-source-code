package vavi.net.im.protocol.ymsg.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;
import vavi.util.StringUtil;

/**
 * ChallengeResponseWeb.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040221 nsano initial version <br>
 */
public class ChallengeResponseWeb implements ChallengeResponse {

    /** for thread control */
    static final String EVENT_NAME = "YMSG_WEB_AUTH_";

    /** receiving timeout */
    static final int SOCK_RECEIVE_TIMEOUT = 10 * 1000;

    /** */
    static final int RECEIVE_TIMEOUT = 20 * 1000;

    /** */
    private String proxyHost;

    /** */
    private int proxyPort;

    /** �ݒ�t�@�C�� */
    private static Preferences userPrefs = Preferences.userNodeForPackage(ChallengeResponseWeb.class);

    /** �ݒ�t�@�C�� */
    private static Preferences systemPrefs = Preferences.systemNodeForPackage(ChallengeResponseWeb.class);

    /** */
    {
        System.setProperty("http.proxyHost", userPrefs.get("proxyhost", systemPrefs.get("proxyhost", "")));
        System.setProperty("http.proxyPort", userPrefs.get("proxyport", systemPrefs.get("proxyport", "80")));
    }

    /**
     * @return 0: key for 6
     */
    public String[] getResponses(String account, String password, String seed) {
        try {
            URL url = new URL("http", "login.yahoo.com", 80, getAuthUrlString(account, password));
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("Host", "login.yahoo.com");
            BufferedReader is = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String data = is.readLine();
            String ticket = parseAuthData(data);
            is.close();
            return new String[] { ticket };
        } catch (NoSuchAlgorithmException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        } catch (IOException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /**
     * @throws NoSuchAlgorithmException
     */
    private String getAuthUrlString(String account, String password) throws IOException, NoSuchAlgorithmException {
        Map<String, String> dict = retrieveLoginPage();
        if (dict == null) {
            return null;
        }
        StringBuilder url = new StringBuilder("/config/login?login=");
        url.append(account);
        url.append("&passwd=");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes(), 0, password.length());
        byte[] result = md5.digest();
        for (int i = 0; i < 16; i++) {
            url.append(StringUtil.toHex2(result[i]));
        }
        String md5chal = dict.get(".challenge");
        md5 = MessageDigest.getInstance("MD5");
        md5.update(md5chal.getBytes(), 0, md5chal.length());
        result = md5.digest();
        for (int i = 0; i < 16; i++) {
            url.append(StringUtil.toHex2(result[i]));
        }
        Iterator<String> j = dict.keySet().iterator();
        while (j.hasNext()) {
            String key = j.next();
            String value = dict.get(key);
            if (!key.equals("passwd")) {
                if (key.equals(".save") || key.equals(".js")) {
                    url.append("&" + key + "=1");
                } else if (key.equals(".challenge")) {
                    url.append("&" + key + "=" + value);
                } else {
                    String u = URLEncoder.encode(value, "UTF-8");
                    url.append("&" + key + "=" + u);
                }
            }
        }
        url.append("&");
        url.append(".hash=1");
        url.append("&");
        url.append(".md5=1");
        return url.toString();
    }

    /** */
    private Map<String, String> retrieveLoginPage() throws IOException {
        if (proxyHost.length() > 0) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", String.valueOf(proxyPort));
        }
        URL url = new URL("http://login.yahoo.com/config/login?.src=pg");
        URLConnection uc = url.openConnection();
        Object content = uc.getContent();
        Map<String, String> dict = makeLoginPageDict((String) content);
        return dict;
    }

    /** */
    private Map<String, String> makeLoginPageDict(String content) {
        Map<String, String> dict = new HashMap<String, String>();
        int c = 0;
        int d;
        byte[] name = new byte[64];
        byte[] value = new byte[64];
        while (c < content.length() && (c = content.indexOf("<input ")) != -1) {
            c = content.indexOf("name=\"") + "name=\"".length();
            for (d = 0; content.charAt(c) != '"'; c++, d++) {
                name[d] = (byte) content.charAt(c);
            }
            d = content.indexOf("value=\"") + "value=\"".length();
            if (content.indexOf('>') < d) {
                break;
            }
            for (c = d, d = 0; content.charAt(c) != '"'; c++, d++) {
                value[d] = (byte) content.charAt(c);
            }
            dict.put(new String(name), new String(value));
        }
        return dict;
    }

    /** */
    private String parseAuthData(String data) {
        if ((data.length() == 0) || data.startsWith("HTTP/1.0 302")) {
            return null;
        }
        int i = 0;
        byte[] buf = new byte[data.length()];
        int r = 0;
        while ((i = data.indexOf("Set-Cookie: ", i)) > 0) {
            i += "Set-Cookie: ".length();
            while (data.charAt(i) != ';') {
                buf[r++] = (byte) data.indexOf(i++);
            }
            buf[r++] = ';';
            buf[r++] = ' ';
        }
        return new String(buf, 0, buf.length - 2);
    }
}
