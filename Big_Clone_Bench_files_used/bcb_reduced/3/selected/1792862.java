package com.bardsoftware.foronuvolo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import com.bardsoftware.foronuvolo.data.ForumUser;
import com.google.appengine.api.memcache.InvalidValueException;

public class UserService {

    private static Cache ourCache;

    static {
        try {
            ourCache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
        } catch (CacheException e) {
            e.printStackTrace();
            ourCache = null;
        }
    }

    public static ForumUser getUser(HttpServletRequest req) {
        ForumUser user;
        Object sessionUserID = req.getSession().getAttribute("user_id");
        if (sessionUserID == null) {
            user = fetchUserFromCookie(req);
        } else {
            user = fetchUserByID(sessionUserID.toString());
        }
        if (user == null) {
            user = ForumUser.ANONYMOUS;
        } else {
            req.setAttribute("user_id", user.getID());
        }
        return user;
    }

    public static void getUsers(Map<String, ForumUser> user_id2object) {
        List<String> notCached = new ArrayList<String>();
        for (String id : user_id2object.keySet()) {
            ForumUser cachedUser = fetchFromCache(id);
            if (cachedUser == null) {
                notCached.add(id);
            } else {
                user_id2object.put(id, cachedUser);
            }
        }
        if (!notCached.isEmpty()) {
            Collection<ForumUser> forumUsers = ForumUser.find(notCached);
            for (ForumUser user : forumUsers) {
                user_id2object.put(user.getID(), user);
                cache(user);
            }
        }
    }

    private static void cache(ForumUser user) {
        if (ourCache == null || user == null) {
            return;
        }
        ourCache.put(user.getID(), user);
    }

    private static ForumUser fetchFromCache(String id) {
        if (ourCache == null) {
            return null;
        }
        try {
            return (ForumUser) ourCache.get(id);
        } catch (InvalidValueException e) {
            return null;
        }
    }

    public static ForumUser fetchUserByID(String id) {
        ForumUser user = fetchFromCache(id);
        if (user == null) {
            user = ForumUser.find(id);
            cache(user);
        }
        return user;
    }

    private static ForumUser fetchUserFromCookie(HttpServletRequest req) {
        System.out.println(req.toString());
        ForumUser user = fetchUserFromCookieOfVKOpenAPI(req);
        if (user != null) {
            return user;
        }
        return fetchUserFromCookieOfGoogleFriends(req);
    }

    private static ForumUser getTestForumUser() {
        String id = "1234567890";
        ForumUser result = ForumUser.find(id);
        String userName = "Test User";
        if (result == null) {
            result = new ForumUser(id, userName);
            cache(result);
            result.save();
        } else if (!userName.equals(result.getDisplayName())) {
            result.setDisplayName(userName);
        }
        return result;
    }

    private static ForumUser fetchUserFromCookieOfVKOpenAPI(HttpServletRequest req) {
        String cookieName = "vk_app_" + ForoNuvoloConstants.VK_OPEN_API_APP_ID;
        Cookie authCookie = getCookieByName(cookieName, req);
        if (authCookie == null) {
            return null;
        }
        final List<String> validKeys = new ArrayList<String>();
        validKeys.addAll(Arrays.asList(new String[] { "expire", "mid", "secret", "sid", "sig" }));
        final String cookieStringValue = authCookie.getValue();
        final Map<String, String> paramName2Value = getCookieAsMap(cookieStringValue);
        if (!paramName2Value.keySet().containsAll(validKeys)) {
            System.out.println("WARNING. Cookie from VK incorrect. Cookie: " + cookieStringValue);
            return null;
        }
        if (!datasConformWithSign(validKeys, paramName2Value)) {
            System.out.println("WARNING. Cookie from VK incorrect. Cookie: " + cookieStringValue);
            return null;
        }
        String id = paramName2Value.get("mid");
        String userName = "vkUserName";
        ForumUser result = ForumUser.find(id);
        if (result == null) {
            result = new ForumUser(id, userName);
            cache(result);
            result.save();
        } else if (!userName.equals(result.getDisplayName())) {
            result.setDisplayName(userName);
        }
        return result;
    }

    private static String getUserName(String idForCheck) {
        final Map<String, String> paramName2Value = new HashMap<String, String>();
        paramName2Value.put("api_id", ForoNuvoloConstants.VK_OPEN_API_APP_ID);
        paramName2Value.put("method", "getUserInfo");
        paramName2Value.put("v", "3.0");
        paramName2Value.put("format", "json");
        final List<String> validKeys = new ArrayList<String>();
        validKeys.addAll(paramName2Value.keySet());
        paramName2Value.put("sig", getMD5Sig(validKeys, paramName2Value));
        String restString = "http://api.vkontakte.ru/api.php?";
        for (Iterator iterator = paramName2Value.keySet().iterator(); iterator.hasNext(); ) {
            String paramName = (String) iterator.next();
            restString += paramName + "=" + paramName2Value.get(paramName);
            if (iterator.hasNext()) {
                restString += "&";
            }
        }
        try {
            URL restUrl = new URL(restString);
            URLConnection restCon = restUrl.openConnection();
            BufferedReader restRead = new BufferedReader(new InputStreamReader(restCon.getInputStream()));
            StringBuilder textResponse = new StringBuilder();
            for (String line = restRead.readLine(); line != null; line = restRead.readLine()) {
                textResponse.append(line);
            }
            JSONObject json = new JSONObject(textResponse.toString());
            JSONObject response = json.getJSONObject("response");
            String user_id = response.getString("user_id");
            assert user_id.equals(idForCheck);
            return response.getString("user_name");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, String> getCookieAsMap(final String cookieStringValue) {
        final Map<String, String> paramName2Value = new HashMap<String, String>();
        for (StringTokenizer tokenizer = new StringTokenizer(cookieStringValue, "=&"); tokenizer.hasMoreTokens(); ) {
            String paramName = tokenizer.nextToken();
            String paramValue;
            if (tokenizer.hasMoreElements()) {
                paramValue = tokenizer.nextToken();
            } else {
                System.out.println("WARNING. Cookie from VK incorrect. Cookie: " + cookieStringValue);
                return null;
            }
            paramName2Value.put(paramName, paramValue);
        }
        return paramName2Value;
    }

    private static Cookie getCookieByName(String cookieName, HttpServletRequest req) {
        if (req.getCookies() == null) {
            return null;
        }
        Cookie authCookie = null;
        for (Cookie cookie : req.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                authCookie = cookie;
                break;
            }
        }
        return authCookie;
    }

    private static boolean datasConformWithSign(List<String> validKeys, Map<String, String> paramName2Value) {
        long timeInSec = System.currentTimeMillis() / 1000;
        String md5Sig = getMD5Sig(validKeys, paramName2Value);
        if (paramName2Value.get("sig").equals(md5Sig) && new Long(paramName2Value.get("expire")) > timeInSec) {
            return true;
        }
        System.out.println("expire = " + paramName2Value.get("expire") + " time = " + timeInSec + " (expire - time) = " + (new Long(paramName2Value.get("expire")) - timeInSec));
        System.out.println("MD5 trueSig  = " + md5Sig);
        System.out.println("sigFromGookie= " + paramName2Value.get("sig"));
        return false;
    }

    private static String getMD5Sig(List<String> validKeys, Map<String, String> paramName2Value) {
        Collections.sort(validKeys);
        String sig = "";
        for (String paramValue : validKeys) {
            if (!paramValue.equals("sig")) {
                sig += paramValue + "=" + paramName2Value.get(paramValue);
            }
        }
        sig += ForoNuvoloConstants.VK_OPEN_API_APP_SHARED_SECRET;
        return hashToMD5(sig);
    }

    private static String hashToMD5(String sig) {
        try {
            MessageDigest lDigest = MessageDigest.getInstance("MD5");
            lDigest.update(sig.getBytes());
            BigInteger lHashInt = new BigInteger(1, lDigest.digest());
            return String.format("%1$032X", lHashInt).toLowerCase();
        } catch (NoSuchAlgorithmException lException) {
            throw new RuntimeException(lException);
        }
    }

    private static ForumUser fetchUserFromCookieOfGoogleFriends(HttpServletRequest req) {
        String cookieName = "fcauth" + ForoNuvoloConstants.FRIEND_CONNECT_SITE_ID;
        Cookie authCookie = getCookieByName(cookieName, req);
        if (authCookie == null) {
            return null;
        }
        String cookieStringValue = authCookie.getValue();
        String restString = "http://www.google.com/friendconnect/api/people/@me/@self?fcauth=" + cookieStringValue;
        try {
            URL restUrl = new URL(restString);
            URLConnection restCon = restUrl.openConnection();
            BufferedReader restRead = new BufferedReader(new InputStreamReader(restCon.getInputStream(), "UTF-8"));
            StringBuilder textResponse = new StringBuilder();
            for (String line = restRead.readLine(); line != null; line = restRead.readLine()) {
                textResponse.append(line);
            }
            JSONObject json = new JSONObject(textResponse.toString());
            JSONObject entry = json.getJSONObject("entry");
            String id = entry.getString("id");
            ForumUser result = ForumUser.find(id);
            String userName = entry.getString("displayName");
            if (result == null) {
                result = new ForumUser(id, userName);
                cache(result);
                result.save();
            } else if (!userName.equals(result.getDisplayName())) {
                result.setDisplayName(userName);
                cache(result);
                result.save();
            }
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
