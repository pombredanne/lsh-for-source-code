package net.sylvek.facebook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;

/**
 * MyKeys is an abstract class containing
 * 
 * static final String SECRET = "secret";
 * 
 * static final String API_KEY = "api_key";
 * 
 * @author Sylvain Maucourt (smaucourt-at-gmail.com)
 * 
 */
public class FacebookClient extends MyKeys {

    static final String PREF = "net.sylvek.where";

    static final String PREF_SESSION = "net.sylvek.where.facebook.session";

    static final String PREF_SECRET = "net.sylvek.where.facebook.secret";

    static final String PREF_UID = "net.sylvek.where.facebook.uid";

    private static final String v = "1.0";

    private static final String format = "JSON";

    private static final String server = "http://api.facebook.com/restserver.php";

    String secretGenerated;

    String uid;

    String session;

    String name;

    String status;

    private SharedPreferences pref;

    private SharedPreferences.Editor editor;

    public FacebookClient(Context context) {
        pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        editor = pref.edit();
        initialize();
    }

    public void initialize() {
        session = pref.getString(PREF_SESSION, null);
        uid = pref.getString(PREF_UID, null);
        secretGenerated = pref.getString(PREF_SECRET, null);
    }

    public boolean initialized() {
        return secretGenerated != null && uid != null && session != null;
    }

    private static final String getSig(StringBuilder params, String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return SimpleMD5.MD5(params.toString().replaceAll("&", "") + s);
    }

    public static final String createToken() throws ClientProtocolException, IOException, NoSuchAlgorithmException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&format=" + format);
        params.append("&method=Auth.createToken");
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, SECRET));
        return postRequest(params.toString());
    }

    public void getSession(String token) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&auth_token=" + token);
        params.append("&format=" + format);
        params.append("&generate_session_secret=true");
        params.append("&method=Auth.getSession");
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, SECRET));
        String result = postRequest(params.toString());
        JSONObject json = new JSONObject(result);
        String error = json.optString("error_code", null);
        if (error != null) {
            throw new IOException(json.getString("error_msg"));
        }
        secretGenerated = json.getString("secret");
        uid = json.getString("uid");
        session = json.getString("session_key");
        commit();
    }

    public void commit() {
        editor.putString(PREF_SESSION, session);
        editor.putString(PREF_SECRET, secretGenerated);
        editor.putString(PREF_UID, uid);
        editor.commit();
    }

    public void reset() {
        session = null;
        secretGenerated = null;
        uid = null;
        editor.remove(PREF_SESSION);
        editor.remove(PREF_SECRET);
        editor.remove(PREF_UID);
        editor.commit();
    }

    public synchronized void initCurrentUser() throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        List<User> user = getUserInfo(uid);
        name = Html.fromHtml(user.get(0).name).toString();
        status = Html.fromHtml(user.get(0).status).toString();
    }

    public boolean isAppUser() throws ClientProtocolException, IOException, NoSuchAlgorithmException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&format=" + format);
        params.append("&method=Users.isAppUser");
        params.append("&session_key=" + session);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        return Boolean.parseBoolean(result);
    }

    public boolean setStatus(String status) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&format=" + format);
        params.append("&method=Status.set");
        params.append("&session_key=" + session);
        params.append("&status=" + status);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        boolean b = Boolean.parseBoolean(result);
        if (!b) {
            JSONObject o = new JSONObject(result);
            throw new IOException(o.getString("error_msg"));
        }
        return b;
    }

    public void notify(String notification) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        notify(notification, "");
    }

    public void notify(String notification, long[] uids) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder strUids = new StringBuilder("[");
        for (int i = 0; i < uids.length; i++) {
            strUids.append(uids[i]);
            if (i + 1 < uids.length) {
                strUids.append(",");
            }
        }
        strUids.append("]");
        notify(notification, strUids.toString());
    }

    public void notify(String notification, String uids) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&format=" + format);
        params.append("&method=Notifications.send");
        params.append("&notification=" + notification);
        params.append("&session_key=" + session);
        params.append("&to_ids=" + uids);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        if (result.contains("error_msg")) {
            JSONObject o = new JSONObject(result);
            throw new IOException(o.getString("error_msg"));
        }
    }

    public String[] getStatus(String uid, int limit) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&format=" + format);
        params.append("&limit=" + limit);
        params.append("&method=Status.get");
        params.append("&session_key=" + session);
        params.append("&uid=" + uid);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        JSONArray array = new JSONArray(result);
        String[] status = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            status[i] = array.getJSONObject(i).getString("message");
        }
        return status;
    }

    public long[] getFriends() throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&format=" + format);
        params.append("&method=Friends.get");
        params.append("&session_key=" + session);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        JSONArray json = new JSONArray(result);
        long[] friends = new long[json.length()];
        for (int i = 0; i < json.length(); i++) {
            friends[i] = json.getInt(i);
        }
        return friends;
    }

    private static String postRequest(String params) throws ClientProtocolException, IOException {
        return postRequest(params, "application/x-www-form-urlencoded");
    }

    private static String postRequest(String params, String contentType) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(server);
        post.setHeader(HTTP.CONTENT_TYPE, contentType);
        post.setEntity(new StringEntity(params));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        }
        throw new IOException(response.getStatusLine().getReasonPhrase());
    }

    public List<User> getCurrentUserInfo() throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        return getUserInfo(uid);
    }

    public List<User> getUserInfo(String uids) throws ClientProtocolException, IOException, NoSuchAlgorithmException, JSONException {
        String fields = "about_me," + "activities," + "proxied_email," + "interests," + "meeting_for," + "meeting_sex," + "name," + "pic," + "profile_url," + "relationship_status," + "sex," + "status";
        StringBuilder params = new StringBuilder();
        params.append("api_key=" + API_KEY);
        params.append("&call_id=" + System.currentTimeMillis());
        params.append("&fields=" + fields);
        params.append("&format=" + format);
        params.append("&method=Users.getInfo");
        params.append("&session_key=" + session);
        params.append("&uids=" + uids);
        params.append("&v=" + v);
        params.append("&sig=" + getSig(params, secretGenerated));
        String result = postRequest(params.toString());
        JSONArray users = new JSONArray(result);
        List<User> r = new ArrayList<User>();
        for (int i = 0; i < users.length(); i++) {
            JSONObject u = users.getJSONObject(i);
            User user = new User();
            user.aboutMe = u.optString("about_me", "");
            user.activities = u.optString("activities", "");
            user.interests = u.optString("interests", "");
            user.name = u.optString("name", "");
            user.picUrl = u.optString("pic", "");
            user.profileUrl = u.optString("profile_url", "");
            user.relationshipStatus = u.optString("relationship_status", "");
            user.sex = u.optString("sex", "");
            JSONObject status = u.optJSONObject("status");
            if (status != null) {
                user.status = status.optString("message", "");
            } else {
                user.status = "";
            }
            r.add(user);
        }
        return r;
    }

    public void upload(byte[] picture, String caption) throws NoSuchAlgorithmException, ClientProtocolException, IOException, JSONException {
        StringBuilder toSig = new StringBuilder();
        toSig.append("api_key=" + API_KEY);
        long call_id = System.currentTimeMillis();
        toSig.append("&call_id=" + call_id);
        toSig.append("&format=" + format);
        toSig.append("&method=Photos.upload");
        toSig.append("&session_key=" + session);
        toSig.append("&v=" + v);
        String sig = getSig(toSig, secretGenerated);
        MultipartEntity multi = new MultipartEntity(HttpMultipartMode.STRICT);
        InputStreamBody photo = new InputStreamBody(new ByteArrayInputStream(picture), "ou.jpg");
        multi.addPart("api_key", new StringBody(API_KEY));
        multi.addPart("call_id", new StringBody(Long.toString(call_id)));
        multi.addPart("format", new StringBody(format));
        multi.addPart("method", new StringBody("Photos.upload"));
        multi.addPart("session_key", new StringBody(session));
        multi.addPart("sig", new StringBody(sig));
        multi.addPart("", photo);
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(server);
        post.setEntity(multi);
        String result = EntityUtils.toString(client.execute(post).getEntity());
        JSONObject json = new JSONObject(result);
        String error = json.optString("error_code", null);
        if (error != null) {
            throw new IOException(json.getString("error_msg"));
        }
    }

    static class User {

        String aboutMe;

        String activities;

        String name;

        String sex;

        String status;

        String interests;

        String relationshipStatus;

        String picUrl;

        String profileUrl;

        int lattitude;

        int longitude;

        boolean online;
    }

    static class SimpleMD5 {

        static String convertToHex(byte[] data) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
            return buf.toString();
        }

        static String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            md5hash = md.digest();
            return convertToHex(md5hash);
        }
    }
}
