package com.restfb;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.unizone.client.SessionManager;
import com.google.unizone.server.DB_Logic;
import com.google.unizone.server.Student;
import com.restfb.types.OAuth;
import com.restfb.types.User;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * 
 * An extendet RestFB facebook client.
 * 
 * Includes functions for retrieving object's access token.
 *  
 * 
 * @author Nitzan Bar
 *
 */
public class ExtendedFaceBookClient extends DefaultFacebookClient {

    protected static final String EXCHANGE_SESSIONS_OBJECT = "oauth/exchange_sessions";

    protected static final String ACCESS_TOKEN_OBJECT = "oauth/access_token";

    protected static final String AUTHORIZE_OBJECT = "oauth/authorize";

    protected static final String SCOPE_PARAM_NAME = "scope";

    protected String APP_ID;

    protected String APP_SECRET;

    private static final String FB_CANVAS_PARAM_PREFIX = "fb_sig";

    protected HashMap<String, String> fb_params;

    /**
	 * Exchange facebook session_key with access_token 
	 * @param session_key
	 * @return An OAuth access token
	 * @throws FacebookException
	 */
    public OAuth exchangeSession(String session) throws FacebookException {
        verifyParameterPresence("session", session);
        String response = makeRequest(EXCHANGE_SESSIONS_OBJECT, false, true, false, null, Parameter.with("client_id", this.APP_ID), Parameter.with("client_secret", this.APP_SECRET), Parameter.with("sessions", session));
        List<String> list = jsonMapper.toJavaList(response, String.class);
        if (list != null) {
            return jsonMapper.toJavaObject(list.get(0), OAuth.class);
        } else return null;
    }

    /**
	 * Exchange facebook code with access_token 
	 * @param code
	 * @param redirect_uri - The uri used to obtain the code. MUST match the previous request!
	 * @return An OAuth access token
	 * @throws FacebookException
	 */
    public void readAccessToken(String code, String redirect_uri) {
        Pattern p = Pattern.compile("(.*)&.*");
        Matcher matcher;
        try {
            String fb_accessTokenSeq = "access_token=";
            String response = makeRequest(ACCESS_TOKEN_OBJECT, false, true, false, null, Parameter.with("client_id", this.APP_ID), Parameter.with("client_secret", this.APP_SECRET), Parameter.with("redirect_uri", redirect_uri), Parameter.with("code", code));
            if (response != null & response.startsWith(fb_accessTokenSeq)) {
                response = response.substring(fb_accessTokenSeq.length());
                matcher = p.matcher(response);
                matcher.find();
                response = matcher.group(1);
                this.accessToken = response;
            }
        } catch (FacebookException e) {
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public List<Student> parseFriends() {
        Pattern friendUID = Pattern.compile(":\"(.*)\"}");
        Matcher matcher;
        FacebookClient fbclient = new DefaultFacebookClient(this.getAccessToken());
        String temp = null;
        List<String> query = new ArrayList<String>();
        List<String> myFriends = new ArrayList<String>();
        List<Student> appFriends = new ArrayList<Student>();
        List<Student> allStudents;
        try {
            String fqlFriends = "SELECT uid2 FROM friend WHERE uid1 = me()";
            query = fbclient.executeQuery(fqlFriends, String.class);
        } catch (Exception e) {
            return null;
        }
        String uid;
        for (String string : query) {
            matcher = friendUID.matcher(string);
            matcher.find();
            string = matcher.group(1);
            myFriends.add(string);
            temp += string + "\n";
        }
        ;
        temp += "\n";
        allStudents = DB_Logic.getAllStudents();
        for (Student iStudent : allStudents) {
            temp += iStudent.getFacebookID() + "\n";
            uid = iStudent.getFacebookID();
            if (myFriends.contains(uid)) {
                appFriends.add(iStudent);
            }
        }
        return appFriends;
    }

    public String appFriendsToString() {
        List<Student> appFriends = parseFriends();
        String output = "";
        for (Student iStudent : appFriends) {
            output += "<div id=\"line\">" + iStudent.toString(false) + "</div>";
        }
        return output;
    }

    public List<String> getMutualFriends(Student student) {
        FacebookClient fbclient = new DefaultFacebookClient(this.accessToken);
        List<String> mutualFriends = new ArrayList<String>();
        User u;
        String fqlMututal = "SELECT uid, name, pic_square FROM user where uid IN (SELECT uid1 FROM friend WHERE uid2=" + student.getFacebookID() + " AND uid1 IN (SELECT uid2 FROM friend WHERE uid1=me()))";
        try {
            u = fbclient.fetchObject("me", User.class);
            if (u == null) return null;
            mutualFriends = fbclient.executeQuery(fqlMututal, String.class);
        } catch (FacebookException e) {
            return null;
        }
        return mutualFriends;
    }

    public String getMyPic() {
        Pattern pa = Pattern.compile("profile.*jpg");
        Matcher match;
        FacebookClient fbclient = new DefaultFacebookClient(this.getAccessToken());
        List<String> myPic = new ArrayList<String>();
        User u;
        String output = "";
        try {
            u = fbclient.fetchObject("me", User.class);
            String fqlPic = "SELECT pic_square From user WHERE uid = me()";
            myPic = fbclient.executeQuery(fqlPic, String.class);
        } catch (FacebookException e) {
            return null;
        }
        output = myPic.get(0);
        match = pa.matcher(output);
        match.find();
        return "http://" + match.group();
    }

    public List<String> getMyData() {
        Pattern name = Pattern.compile("name\":\"(.*)\",");
        Pattern id = Pattern.compile("uid\":(\\d*)");
        Pattern pa = Pattern.compile("profile.*jpg");
        Matcher matcher, matcher2, matcher3;
        String temp;
        FacebookClient fbclient = new DefaultFacebookClient(this.getAccessToken());
        List<String> myData, query = new ArrayList<String>();
        User u;
        String output = "";
        try {
            u = fbclient.fetchObject("me", User.class);
            String fql = "SELECT name, pic_square, uid FROM user WHERE uid = me()";
            myData = fbclient.executeQuery(fql, String.class);
        } catch (FacebookException e) {
            query.add("glaaa");
            return query;
        }
        output = myData.get(0);
        matcher = pa.matcher(output);
        matcher.find();
        temp = "http:////" + matcher.group();
        query.add(0, temp);
        matcher2 = name.matcher(output);
        matcher2.find();
        temp = matcher2.group(1);
        query.add(1, temp);
        matcher3 = id.matcher(output);
        matcher3.find();
        temp = matcher3.group(1);
        query.add(2, temp);
        return query;
    }

    public String exchangeCode(String code, String redirect_uri) throws FacebookException {
        String fb_accessTokenSeq = "access_token=";
        verifyParameterPresence("code", code);
        verifyParameterPresence("redirect_uri", redirect_uri);
        String response = makeRequest(ACCESS_TOKEN_OBJECT, false, true, false, null, Parameter.with("client_id", this.APP_ID), Parameter.with("client_secret", this.APP_SECRET), Parameter.with("redirect_uri", redirect_uri), Parameter.with("code", code));
        if (response != null & response.startsWith(fb_accessTokenSeq)) {
            return response.substring(fb_accessTokenSeq.length());
        } else return null;
    }

    public ExtendedFaceBookClient(String access_token) {
        super(access_token);
    }

    public ExtendedFaceBookClient(String APP_ID, String APP_SECRET) {
        super();
        this.APP_ID = APP_ID;
        this.APP_SECRET = APP_SECRET;
        this.fb_params = new HashMap<String, String>();
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    /** 
	 * This function handles authentication procedure for canvas & non-canvas apps in facebook
	 * @param session
	 * @param isCanvas
	 * @param scope
	 * @return true if authentication procedure sucedeed false other wise. Note: Some procedures make involve sending a redirect  returning false.
	 */
    public boolean checkUser(SessionManager session, boolean isCanvas, Parameter scope) {
        verifyParameterPresence("scope", scope);
        OAuth oauth;
        if (isCanvas) {
            if (!getAndValidateCanvasFBParams(session.getRequest())) {
                return false;
            }
            if (!fb_params.containsKey("added") || !fb_params.get("added").equals("1")) {
                return false;
            }
            if (!fb_params.containsKey("user")) {
                return false;
            }
            try {
                oauth = exchangeSession(fb_params.get("session_key"));
            } catch (FacebookException e) {
                return false;
            }
            this.accessToken = StringUtils.trimToNull(oauth.getAccessToken());
            return true;
        } else {
            String code = session.getRequest().getParameter("code");
            if (code == null) {
                String redirect_uri = session.getRequest().getRequestURL() + "?" + session.getRequest().getQueryString();
                String url = getCanvasAuthorizeURL(redirect_uri);
                try {
                    session.getResponse().sendRedirect(url);
                } catch (IOException e) {
                    return false;
                }
            } else {
                String redirect_uri = session.getRequest().getRequestURL() + "?";
                String subString = "&code=";
                int x = session.getRequest().getQueryString().lastIndexOf(subString);
                redirect_uri += session.getRequest().getQueryString().substring(0, x);
                String access_token = null;
                try {
                    access_token = exchangeCode(code, redirect_uri);
                } catch (FacebookException e) {
                    return false;
                }
                if (access_token != null) {
                    this.accessToken = StringUtils.trimToNull(access_token);
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * Get Http request params and parse into HasMap<String,String>
	 * @param The HttpServletRequest 
	 * @return A HashMap containin all the request's parameters
	 */
    private HashMap<String, String> getRequestParams(HttpServletRequest request) {
        HashMap<String, String> params = new HashMap<String, String>();
        Map<String, String[]> map = request.getParameterMap();
        for (Entry<String, String[]> entry : map.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }
        return params;
    }

    /**
	 * Get Facebook parameters from request and validate signature 
	 * @param request
	 * @return True iff parameters match signature. this.fb_params will hold all of the parameters
	 */
    private boolean getAndValidateCanvasFBParams(HttpServletRequest request) {
        HashMap<String, String> params = getRequestParams(request);
        String prefix = FB_CANVAS_PARAM_PREFIX + "_";
        int prefix_length = prefix.length();
        for (Entry<String, String> param : params.entrySet()) {
            if (param.getKey().indexOf(prefix) == 0) {
                String key = param.getKey().substring(prefix_length);
                this.fb_params.put(key, param.getValue());
            }
        }
        String str = generateCanvasSignature(this.fb_params);
        String expectedSig = params.get(FB_CANVAS_PARAM_PREFIX);
        return verifyCanvasSignature(str, expectedSig);
    }

    /**
	 * @return
	 */
    private String generateCanvasSignature(HashMap<String, String> map) {
        SortedSet<String> keys = new TreeSet<String>(map.keySet());
        String str = new String();
        for (String key : keys) {
            str += key + "=" + fb_params.get(key);
        }
        str += this.APP_SECRET;
        return str;
    }

    private boolean verifyCanvasSignature(String str, String expectedSig) {
        byte[] hash;
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(StringUtils.toBytes(str));
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The platform does nto support MD5", e);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(Integer.toHexString((b & 0xf0) >>> 4));
            result.append(Integer.toHexString(b & 0x0f));
        }
        return result.toString().equals(expectedSig);
    }

    /**
	 * 
	 * Generate Facebook App Authorize URL based on parameters (For Canvas App)
	 * 
	 * @param redirect_url The URL to redirect after login
	 * @param scope The permissions to request from User
	 * 
	 */
    public String getCanvasAuthorizeURL(String redirect_url) {
        StringBuilder url = new StringBuilder();
        url.append("https://www.facebook.com/dialog/oauth?");
        url.append("client_id=" + this.APP_ID);
        url.append("&redirect_uri=" + StringUtils.urlEncode(redirect_url));
        url.append("&scope=" + "email");
        return url.toString();
    }

    public String getAccessTokenURL(String redirect_url, String code) {
        StringBuilder url = new StringBuilder();
        url.append("https://www.facebook.com/oauth/access_token?");
        url.append("client_id=" + this.APP_ID);
        url.append("&redirect_uri=" + StringUtils.urlEncode(redirect_url));
        url.append("&client_secret=" + this.APP_SECRET);
        url.append("&code=" + code);
        return url.toString();
    }

    /**
	 * Return the HTTP request URL (URL Encoded)
	 * @return
	 */
    private String getCurrentURL(HttpServletRequest request) {
        return StringUtils.urlEncode(request.getRequestURL().toString());
    }
}
