package etc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class FrankLogin {

    private HttpClient client;

    private String password = "fillin";

    private String login = "fillin";

    private String host;

    private String u;

    private String challenge;

    public FrankLogin() {
        client = new HttpClient();
    }

    public void parseResponse(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        Pattern uP = Pattern.compile(".*name=\"\\.u\" value=\"([^\"]+)\".*");
        Pattern challengeP = Pattern.compile(".*name=\"\\.challenge\" value=\"([^\"]+)\".*");
        Pattern hostP = Pattern.compile(".*method=\"post\" action=\"([^\"]+)\".*");
        while ((line = reader.readLine()) != null) {
            Matcher uM = uP.matcher(line);
            Matcher challengeM = challengeP.matcher(line);
            Matcher hostM = hostP.matcher(line);
            if (uM.matches()) {
                u = uM.group(1);
            }
            if (challengeM.matches()) {
                challenge = challengeM.group(1);
            }
            if (hostM.matches()) {
                host = hostM.group(1);
            }
        }
        System.out.println("u=" + u);
        System.out.println("host=" + host);
        System.out.println("challenge=" + challenge);
    }

    public void loginNormal() throws Exception {
        GetMethod get = new GetMethod("http://login.yahoo.com/config/login?.src=www&.done=http://www.yahoo.com");
        get.setRequestHeader("user-agent", "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-us) AppleWebKit/124 (KHTML, like Gecko) Safari/125.1");
        client.executeMethod(get);
        parseResponse(get.getResponseBodyAsStream());
        NameValuePair[] pairs = { new NameValuePair("login", login), new NameValuePair("password", password), new NameValuePair(".tries", "1"), new NameValuePair(".src", "www"), new NameValuePair(".md5", ""), new NameValuePair(".hash", ""), new NameValuePair(".js", ""), new NameValuePair(".last", ""), new NameValuePair(".promo", ""), new NameValuePair(".intl", "us"), new NameValuePair(".bypass", ""), new NameValuePair(".partner", ""), new NameValuePair(".u", u), new NameValuePair(".v", "0"), new NameValuePair(".challenge", challenge), new NameValuePair(".yplus", ""), new NameValuePair(".emailCode", ""), new NameValuePair("pkg", ""), new NameValuePair("stepid", ""), new NameValuePair(".ev", ""), new NameValuePair("hasMsgr", "0"), new NameValuePair(".chkP", "Y"), new NameValuePair(".done", "http://www.yahoo.com"), new NameValuePair(".persistent", "y") };
        PostMethod post = new PostMethod(host);
        post.setRequestHeader("user-agent", "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-us) AppleWebKit/124 (KHTML, like Gecko) Safari/125.1");
        post.addRequestHeader("Accept", "*/*");
        post.addRequestHeader("Accept-Language", "en-us, ja;q=0.21, de-de;q=0.86, de;q=0.79, fr-fr;q=0.71, fr;q=0.64, nl-nl;q=0.57, nl;q=0.50, it-it;q=0.43, it;q=0.36, ja-jp;q=0.29, en;q=0.93, es-es;q=0.14, es;q=0.07");
        post.setRequestBody(pairs);
        client.executeMethod(post);
        post.getResponseBodyAsString();
    }

    public void loginMD5() throws Exception {
        GetMethod get = new GetMethod("http://login.yahoo.com/config/login?.src=www&.done=http://www.yahoo.com");
        get.setRequestHeader("user-agent", "Mozilla/5.0 (Macintosh; U; PPC MacOS X; en-us) AppleWebKit/124 (KHTML, like Gecko) Safari/125.1");
        client.executeMethod(get);
        parseResponse(get.getResponseBodyAsStream());
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(password.getBytes("US-ASCII"));
        String hash1 = new String(digest.digest(), "US-ASCII");
        String hash2 = hash1 + challenge;
        digest.update(hash2.getBytes("US-ASCII"));
        String hash = new String(digest.digest(), "US-ASCII");
        NameValuePair[] pairs = { new NameValuePair("login", login), new NameValuePair("password", hash), new NameValuePair(".save", "1"), new NameValuePair(".tries", "1"), new NameValuePair(".src", "www"), new NameValuePair(".md5", "1"), new NameValuePair(".hash", "1"), new NameValuePair(".js", "1"), new NameValuePair(".last", ""), new NameValuePair(".promo", ""), new NameValuePair(".intl", "us"), new NameValuePair(".bypass", ""), new NameValuePair(".u", u), new NameValuePair(".v", "0"), new NameValuePair(".challenge", challenge), new NameValuePair(".yplus", ""), new NameValuePair(".emailCode", ""), new NameValuePair("pkg", ""), new NameValuePair("stepid", ""), new NameValuePair(".ev", ""), new NameValuePair("hasMsgr", "0"), new NameValuePair(".chkP", "Y"), new NameValuePair(".done", "http://www.yahoo.com"), new NameValuePair(".persistent", "y") };
        get = new GetMethod("http://login.yahoo.com/config/login");
        get.setRequestHeader("user-agent", "Mozilla/5.0 (Macintosh; U; PPC MacOS X; en-us) AppleWebKit/124 (KHTML, like Gecko) Safari/125.1");
        get.addRequestHeader("Accept", "*/*");
        get.addRequestHeader("Accept-Language", "en-us, ja;q=0.21, de-de;q=0.86, de;q=0.79, fr-fr;q=0.71, fr;q=0.64, nl-nl;q=0.57, nl;q=0.50, it-it;q=0.43, it;q=0.36, ja-jp;q=0.29, en;q=0.93, es-es;q=0.14, es;q=0.07");
        get.setQueryString(pairs);
        client.executeMethod(get);
        get.getResponseBodyAsString();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "trace");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        new FrankLogin().loginMD5();
    }
}
