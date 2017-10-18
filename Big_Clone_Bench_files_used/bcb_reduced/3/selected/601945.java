package org.kompiro.readviewer.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.commons.codec.binary.Base64;

public class AuthenticationService {

    private URL url;

    private String password;

    private String username;

    private boolean wsseMode;

    public AuthenticationService(URL url, String username, String password, boolean wsseMode) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.wsseMode = wsseMode;
    }

    public URLConnection getConnection() throws IOException {
        URLConnection con = url.openConnection();
        con.setConnectTimeout(30 * 1000);
        if (username == null || "".equals(username) || password == null || "".equals(password)) return con;
        if (wsseMode) {
            con.setRequestProperty("X-WSSE", getWsseHeaderValue());
            return con;
        }
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication auth = null;
                if (username != null && password != null) {
                    auth = new PasswordAuthentication(username, password.toCharArray());
                }
                return auth;
            }
        });
        return con;
    }

    private String getWsseHeaderValue() {
        try {
            byte[] nonceB = new byte[8];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(nonceB);
            SimpleDateFormat zulu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            zulu.setTimeZone(TimeZone.getTimeZone("GMT"));
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(System.currentTimeMillis());
            String created = zulu.format(now.getTime());
            byte[] createdB = created.getBytes("utf-8");
            byte[] passwordB = password.getBytes("utf-8");
            byte[] v = new byte[nonceB.length + createdB.length + passwordB.length];
            System.arraycopy(nonceB, 0, v, 0, nonceB.length);
            System.arraycopy(createdB, 0, v, nonceB.length, createdB.length);
            System.arraycopy(passwordB, 0, v, nonceB.length + createdB.length, passwordB.length);
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(v);
            byte[] digest = md.digest();
            StringBuffer buf = new StringBuffer();
            buf.append("UsernameToken Username=\"");
            buf.append(username);
            buf.append("\", PasswordDigest=\"");
            buf.append(new String(Base64.encodeBase64(digest)));
            buf.append("\", Nonce=\"");
            buf.append(new String(Base64.encodeBase64(nonceB)));
            buf.append("\", Created=\"");
            buf.append(created);
            buf.append('"');
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
