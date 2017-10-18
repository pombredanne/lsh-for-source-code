package util.webSearch;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import util.io.URLInput;

/**
 * 
 * @author sergio
 * 
 */
public class DeliciousQuery {

    /**
	 * Parse Delicious ULR-info post obtained in JSON format from
	 * 
	 * http://feeds.delicious.com/v2/json/urlinfo/
	 * 
	 * 
	 */
    private static final String delicious_api_url = "http://feeds.delicious.com/v2/json/urlinfo/";

    private static DeliciousURLInfo parseDeliciousInfoPost(String post) {
        Object obj = JSONValue.parse(post);
        JSONArray array = (JSONArray) obj;
        DeliciousURLInfo del_post = new DeliciousURLInfo();
        if (array != null && array.size() > 0) {
            JSONObject obj2 = (JSONObject) array.get(0);
            if (obj2.containsKey("title")) {
                del_post.setTitle(obj2.get("title").toString().trim());
            }
            if (obj2.containsKey("url")) {
                del_post.setUrl(obj2.get("url").toString().trim());
            }
            if (obj2.containsKey("total_posts")) {
                String tmp = obj2.get("total_posts").toString().trim();
                del_post.setTotal_posts(Long.valueOf(tmp));
            }
            if (obj2.containsKey("hash")) {
                del_post.setHash(obj2.get("hash").toString().trim());
            }
            if (obj2.containsKey("top_tags")) {
                if (obj2.get("top_tags").getClass().getCanonicalName().equals("org.json.simple.JSONObject")) {
                    JSONObject obj3 = (JSONObject) obj2.get("top_tags");
                    Iterator<String> keys = obj3.keySet().iterator();
                    Hashtable<String, Long> hash = del_post.getFrequent_tags();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        hash.put(key, Long.valueOf(obj3.get(key).toString().trim()));
                    }
                }
            }
        }
        if (del_post.getUrl() == null) return null;
        return del_post;
    }

    /**
	 * 
	 * Query 'url' in the Delicious tag system (public tags) Return a
	 * DeliciousURLInfoPost object with the result
	 * 
	 * 
	 * @param url
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
    public static DeliciousURLInfo getDeliciousURLInfo(String url) {
        if (!url.startsWith("http")) {
            url = "http:// " + url;
        }
        String md5 = null;
        try {
            md5 = hashStringMD5(url);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String del_url = delicious_api_url + md5;
        String post = URLInput.readFromURL(del_url);
        DeliciousURLInfo del = parseDeliciousInfoPost(post);
        return del;
    }

    /**
	 * Get the JSON entry returned by the delicious API given an URL
	 * 
	 * @param url
	 * @return
	 */
    public static String getDeliciousJSONEntry(String url) {
        if (!url.startsWith("http")) {
            url = "http:// " + url;
        }
        String md5 = null;
        try {
            md5 = hashStringMD5(url);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String del_url = delicious_api_url + md5;
        String post = URLInput.readFromURL(del_url);
        return post;
    }

    public static String hashStringMD5(String string) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(string.getBytes());
        byte byteData[] = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
