package com.zanfar.milkman;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import android.util.Log;

public class RTM {

    private static final String URL_PREFIX = "http://api.rememberthemilk.com/services/rest?";

    private static final String RES_FORMAT = "json";

    private static final String API_KEY = "717e0c5e40ba49fd134141643c88c81e";

    private static final String API_SECRET = "7c19a44ba2382791";

    private static final String AUTH_URL = "http://www.rememberthemilk.com/services/auth/";

    private String auth_token;

    private String auth_frob;

    private String user_id;

    private String user_name;

    private String user_fullname;

    public RTM(String token) {
        auth_token = token;
        auth_frob = "";
        user_id = "";
        user_name = "";
        user_fullname = "";
    }

    public boolean setToken(String token) {
        auth_token = token;
        boolean ret = authorized();
        if (!ret) auth_token = "";
        return ret;
    }

    public String getToken() {
        return auth_token;
    }

    public String getUserId() {
        return user_id;
    }

    public String getUserName() {
        return user_name;
    }

    public String getFullName() {
        return user_fullname;
    }

    public boolean authorized() {
        Log.d("RTM", "Checking authorization status");
        if (auth_token.length() == 0) {
            Log.e("RTM", "No token set, failed");
            return false;
        }
        TreeMap<String, String> data = new TreeMap<String, String>();
        data.put("auth_token", auth_token);
        JSONObject response = runMethod("rtm.auth.checkToken", data, true);
        if (!response.optString("stat", "fail").equals("fail")) {
            Log.d("RTM", "Token passed checkToken() test");
            JSONObject user = response.optJSONObject("auth").optJSONObject("user");
            user_id = user.optString("id");
            user_name = user.optString("username");
            user_fullname = user.optString("fullname");
            return true;
        } else {
            Log.e("RTM", "Token failed checkToken() test");
            return false;
        }
    }

    public String authorizeURL() {
        Log.d("RTM", "Generating Authorization URL");
        JSONObject response = runMethod("rtm.auth.getFrob", true);
        if (response.optString("stat", "fail").equals("fail")) {
            Log.e("RTM", "RTM authorization failed");
            return "";
        }
        auth_frob = response.optString("frob");
        Log.d("RTM", "RTM Frob: '" + auth_frob + "'");
        SortedMap<String, String> data = new TreeMap<String, String>();
        data.put("api_key", API_KEY);
        data.put("perms", "delete");
        data.put("frob", auth_frob);
        String URL = buildURL(AUTH_URL + "?", data, true);
        Log.d("RTM", "URL: '" + URL + "'");
        return URL;
    }

    public boolean completeAuth() {
        SortedMap<String, String> data = new TreeMap<String, String>();
        data.put("frob", auth_frob);
        JSONObject response = runMethod("rtm.auth.getToken", data, true);
        if (response.optString("stat", "fail").equals("fail")) {
            Log.e("RTM", "getToken failed with given frob");
            return false;
        } else {
            auth_token = response.optJSONObject("auth").optString("token", "");
            return authorized();
        }
    }

    private JSONObject runMethod(String method, boolean sign) {
        SortedMap<String, String> data = new TreeMap<String, String>();
        return runMethod(method, data, sign);
    }

    private JSONObject runMethod(String method, SortedMap<String, String> data, boolean sign) {
        JSONObject json = new JSONObject();
        Log.d("RTM", "Running Method '" + method + "'");
        data.put("method", method);
        data.put("format", RES_FORMAT);
        data.put("api_key", API_KEY);
        String URL = buildURL(URL_PREFIX, data, sign);
        HttpClient client = new DefaultHttpClient();
        HttpGet request;
        HttpResponse response;
        String raw;
        Log.d("RTM", "Request URL '" + URL + "'");
        request = new HttpGet(URL);
        try {
            Log.d("HTTPRequest", "URL: " + URL);
            response = client.execute(request);
            raw = GetText(response.getEntity().getContent());
            Log.d("HTTPRequest", "Raw Data: " + raw);
            json = new JSONObject(raw);
        } catch (Exception e) {
            Log.e("HTTPRequest", e.getMessage());
        } finally {
            request.abort();
        }
        String resp_code = json.optJSONObject("rsp").optString("stat").trim().toLowerCase();
        if (resp_code.equalsIgnoreCase("fail")) {
            JSONObject error = json.optJSONObject("rsp").optJSONObject("err");
            Log.e("RTM", "Error " + error.optString("code") + ": " + error.optString("msg"));
        }
        return json.optJSONObject("rsp");
    }

    private static String GetText(InputStream in) {
        String text = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            text = sb.toString();
        } catch (Exception ex) {
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
        }
        return text;
    }

    private String MD5(String s) {
        Log.d("MD5", "Hashing '" + s + "'");
        String hash = "";
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(), 0, s.length());
            hash = new BigInteger(1, m.digest()).toString(16);
            Log.d("MD5", "Hash: " + hash);
        } catch (Exception e) {
            Log.e("MD5", e.getMessage());
        }
        return hash;
    }

    private String buildURL(String URL, SortedMap<String, String> data, boolean sign) {
        Set<String> coll = data.keySet();
        for (Iterator<String> i = coll.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            URL += key + "=" + data.get(key) + "&";
        }
        if (sign) {
            String keys = "";
            coll = data.keySet();
            for (Iterator<String> i = coll.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                keys += key;
                keys += data.get(key);
            }
            keys = API_SECRET + keys;
            String hash = MD5(keys);
            URL += "api_sig=" + hash;
        } else {
            URL = URL.substring(0, URL.length() - 1);
        }
        return URL;
    }
}
