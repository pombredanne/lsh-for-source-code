package net.sf.emusiccenter.audioscrobbler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.digester.Digester;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.SAXException;

/**
 * @author Andreas Beckers
 * 
 */
public class Radio {

    private static final String HOST = "ws.audioscrobbler.com";

    private static final int PORT = 80;

    private static final String VERSION = "1.5.2.38918";

    private HttpClient _client;

    private String _session;

    private boolean _canceled = false;

    /**
	 * 
	 */
    public Radio() {
        _client = new HttpClient();
        _client.getParams().setBooleanParameter("http.protocol.reject-relative-redirect", false);
    }

    public boolean login(String user, String password) throws HttpException, IOException, NoSuchAlgorithmException {
        String uri1 = "http://" + HOST + ":" + PORT + "/radio/handshake.php" + "?version=" + VERSION + "&platform=" + "&username=" + user + "&passwordmd5=" + getMD5(password) + "&debug=0" + "&partner=";
        HttpMethod m1 = new GetMethod(uri1);
        int r = _client.executeMethod(m1);
        if (r != HttpStatus.SC_OK) {
            m1.releaseConnection();
            return false;
        }
        Properties props = new Properties();
        props.load(m1.getResponseBodyAsStream());
        m1.releaseConnection();
        _session = props.getProperty("session");
        return true;
    }

    public List<Track> getTracks(String artist) throws IOException, SAXException {
        String uri2 = "http://" + HOST + ":" + PORT + "/radio/" + "adjust.php" + "?session=" + _session + "&url=lastfm://artist/" + URLEncoder.encode(artist, "UTF-8") + "/similarartists&debug=0";
        HttpMethod m2 = new GetMethod(uri2);
        int r = _client.executeMethod(m2);
        if (r != HttpStatus.SC_OK) {
            m2.releaseConnection();
            return null;
        }
        m2.releaseConnection();
        String uri3 = "http://" + HOST + ":" + PORT + "/radio/xspf.php?sk=" + _session + "&discovery=" + 0 + "&desktop=" + VERSION;
        HttpMethod m3 = new GetMethod(uri3);
        r = _client.executeMethod(m3);
        if (r != HttpStatus.SC_OK) {
            m3.releaseConnection();
            return null;
        }
        Digester d = new Digester();
        ClassLoader cl = d.getClassLoader();
        d.setClassLoader(this.getClass().getClassLoader());
        List<Track> l = new ArrayList<Track>();
        d.push(l);
        d.addObjectCreate("playlist/trackList/track", Track.class);
        d.addCallMethod("playlist/trackList/track/location", "setLocation", 0);
        d.addCallMethod("playlist/trackList/track/title", "setTitle", 0);
        d.addCallMethod("playlist/trackList/track/album", "setAlbum", 0);
        d.addCallMethod("playlist/trackList/track/creator", "setArtist", 0);
        d.addCallMethod("playlist/trackList/track/duration", "setDurationString", 0);
        d.addCallMethod("playlist/trackList/track/image", "setImage", 0);
        d.addSetNext("playlist/trackList/track", "add");
        d.parse(m3.getResponseBodyAsStream());
        m3.releaseConnection();
        d.setClassLoader(cl);
        return l;
    }

    public static String getMD5(String s) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(s.getBytes());
        byte[] result = md5.digest();
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < result.length; i++) {
            hexString.append(String.format("%02x", 0xFF & result[i]));
        }
        return hexString.toString();
    }

    public void fetch(String song, File f, DownloadListener listener) throws HttpException, IOException {
        _canceled = false;
        HttpMethod m = new GetMethod(song);
        m.getParams().setSoTimeout(60000);
        m.setFollowRedirects(true);
        int r = _client.executeMethod(m);
        int length = Integer.parseInt(m.getResponseHeader("Content-Length").getValue());
        if (r != HttpStatus.SC_OK) {
            m.releaseConnection();
            return;
        }
        if (listener != null) listener.started();
        FileOutputStream o = new FileOutputStream(f);
        int total = 0;
        try {
            InputStream in = m.getResponseBodyAsStream();
            byte[] b = new byte[1024];
            while (total < length) {
                int x = in.read(b);
                if (x == -1 || _canceled) break;
                total += x;
                o.write(b, 0, x);
                if (listener != null) listener.progress(total * 100 / length);
            }
        } finally {
            o.close();
            if (listener != null) if (_canceled) listener.canceled(); else listener.ended(f);
        }
        m.releaseConnection();
    }

    /**
	 * 
	 */
    public void cancel() {
        _canceled = true;
    }
}
