package jp.gara;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class WsseHttpClient extends HttpClient {

    private static final String CHARSET_UTF8 = "UTF-8";

    public WsseHttpClient() {
        super();
    }

    private HttpMethodBase executeMethod(HttpMethodBase method, String username, String password) throws HttpException, IOException {
        method.addRequestHeader("X-WSSE", this.getWsseHeaderValue(username, password));
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setProxy("cachetky2", 8080);
        super.executeMethod(hostConfiguration, method);
        System.out.println("\t" + super.getClass().getName() + "#executeMethod: " + method.getStatusLine().toString());
        System.out.println(method.getResponseBodyAsString());
        return method;
    }

    public HttpMethodBase post(String url, NameValuePair[] namVals, String username, String password) throws HttpException, IOException {
        PostMethod postMethod = new PostMethod(url);
        postMethod.addParameters(namVals);
        postMethod.getParams().setContentCharset(CHARSET_UTF8);
        this.executeMethod(postMethod, username, password);
        return postMethod;
    }

    public HttpMethodBase get(String url, String username, String password) throws HttpException, IOException {
        GetMethod getMethod = new GetMethod(url);
        this.executeMethod(getMethod, username, password);
        return getMethod;
    }

    protected final String getWsseHeaderValue(String username, String password) {
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
