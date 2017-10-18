package com.flickj;

import com.flickj.data.Photo;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.security.*;
import java.math.*;

/**
 * Class to handle the RAW REST requests to Flickr.  Flickrs should use the
 * FlickrConnection class for actual interaction.
 *
 * @author bradw
 */
public class FlickjRestConnection {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final String apiKey;

    public FlickjRestConnection(String apiKey) {
        this.apiKey = apiKey;
    }

    public String echo(String value) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("name", value);
        params.put("api_key", apiKey);
        URL url = FlickjRestConnection.getServicesURL("flickr.test.echo", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String photosetsGetList(String userID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("user_id", userID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photosets.getList", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String photosGetInfo(String photoID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("photo_id", photoID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photos.getInfo", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String photosGetSize(String photoID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("photo_id", photoID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photos.getSizes", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String photosetsGetInfo(String photosetID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("photoset_id", photosetID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photosets.getInfo", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String photosetsGetPhotos(String photosetID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("photoset_id", photosetID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photosets.getPhotos", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String getInterestingList(Date onDay, int maxPhotos) throws Exception {
        Hashtable<String, String> params = new Hashtable<String, String>();
        String result;
        params.put("api_key", apiKey);
        params.put("per_page", "" + maxPhotos);
        if (onDay != null) {
            params.put("date", dateFormat.format(onDay));
        }
        URL url = FlickjRestConnection.getServicesURL("flickr.interestingness.getList", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    public String getGPSLocationForPhoto(String photoID) throws Exception {
        String result;
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("api_key", apiKey);
        params.put("photo_id", photoID);
        URL url = FlickjRestConnection.getServicesURL("flickr.photos.geo.getLocation", params);
        HttpURLConnection httpConn = getDefaultConnection(url);
        httpConn.connect();
        result = readInputStream(httpConn.getInputStream());
        httpConn.disconnect();
        return result;
    }

    private static HttpURLConnection getDefaultConnection(URL url) throws Exception {
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);
        httpConn.setUseCaches(false);
        httpConn.setDefaultUseCaches(false);
        httpConn.setAllowUserInteraction(true);
        httpConn.setRequestMethod("POST");
        return httpConn;
    }

    private static String readInputStream(InputStream in) throws IOException {
        StringBuffer buff = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String data;
        while ((data = reader.readLine()) != null) {
            buff.append(data).append("\n");
        }
        reader.close();
        return buff.toString();
    }

    public static URL getServicesURL(String method, Map<String, String> parameters) throws Exception {
        StringBuffer buffer = new StringBuffer();
        buffer.append("http://api.flickr.com/services/rest/?");
        buffer.append("method=" + method);
        Set<String> keys = parameters.keySet();
        for (String key : keys) {
            buffer.append("&").append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(parameters.get(key), "UTF-8"));
        }
        return new URL(buffer.toString());
    }

    public static URL getAuthenticationURL(String apiKey, String permission, String sharedSecret) throws Exception {
        String apiSig = sharedSecret + "api_key" + apiKey + "perms" + permission;
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(apiSig.getBytes(), 0, apiSig.length());
        apiSig = new BigInteger(1, m.digest()).toString(16);
        StringBuffer buffer = new StringBuffer();
        buffer.append("http://flickr.com/services/auth/?");
        buffer.append("api_key=" + apiKey);
        buffer.append("&").append("perms=").append(permission);
        buffer.append("&").append("api_sig=").append(apiSig);
        return new URL(buffer.toString());
    }
}
