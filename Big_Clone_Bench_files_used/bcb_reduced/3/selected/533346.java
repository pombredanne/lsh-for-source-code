package learning;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;
import au.id.jericho.lib.html.Source;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RomeLearning extends TestCase {

    public void testLearningRome() throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        String url = "http://d.hatena.ne.jp/itoyosuke/rss2";
        SyndFeed feed = input.build(new XmlReader(new URL(url)));
        System.out.println("author : " + feed.getAuthor());
        System.out.println("description : " + feed.getDescription());
        System.out.println("type : " + feed.getFeedType());
        for (Object e : feed.getEntries()) {
            SyndEntry entry = (SyndEntry) e;
            System.out.println(entry.getTitle());
            System.out.println(entry.getDescription().getType());
            String value = entry.getDescription().getValue();
            Source source = new Source(value);
            value = source.getRenderer().toString();
            System.out.println(value);
        }
    }

    public void testLearningRomeToMixi() throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        String url = "http://mixi.jp/atom/updates/r=1/member_id=1090863";
        URL url2 = new URL(url);
        URLConnection con = url2.openConnection();
        assertTrue(con instanceof HttpURLConnection);
        HttpURLConnection httpCon = (HttpURLConnection) con;
        httpCon.setRequestProperty("X-WSSE", getWsseHeaderValue("kompiro@hotmail.com", "fopcc17m"));
        SyndFeed feed = input.build(new XmlReader(httpCon));
        List<?> entries = feed.getEntries();
        assertFalse(0 == entries.size());
        for (Object entry : entries) {
            System.out.println(entry);
        }
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
