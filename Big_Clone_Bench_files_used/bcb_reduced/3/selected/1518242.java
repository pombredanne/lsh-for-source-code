package com.oauth.model;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import com.oauth.util.OauthUtil;
import android.util.Base64;
import android.util.Log;

public abstract class OAuth {

    public abstract String authorize();

    public abstract String getAccessToken(NameValuePair[] param);

    protected String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public static String getURLParam(String url, String name) {
        url = url + "&";
        name = name + "=";
        if (url.indexOf(name) == -1) {
            return null;
        }
        int start = url.indexOf(name) + name.length();
        return url.substring(start, url.indexOf("&", start));
    }

    public static String doPostEntity(String URL, List<NameValuePair> params) {
        try {
            OauthUtil util = new OauthUtil();
            URI uri = new URI(URL);
            HttpClient httpclient = util.getNewHttpClient();
            HttpPost postMethod = new HttpPost(uri);
            postMethod.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpclient.execute(postMethod);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                Log.i("DEBUG", "result: " + strResult);
                String token;
                try {
                    JSONObject obj = new JSONObject(strResult);
                    token = obj.getString("access_token");
                } catch (Exception e) {
                    token = strResult.substring(strResult.indexOf("access_token=") + 13);
                }
                return token;
            }
        } catch (Exception e) {
            Log.i("DEBUG", e.toString());
        }
        return null;
    }

    /**
	 * 提交OAuth Post请求(Http Header模式)
	 * 
	 * @param URL
	 *            地址
	 * @param params
	 *            Http头包含的参数
	 * @return
	 */
    public static String doPost(String URL, List<NameValuePair> params) {
        try {
            OauthUtil util = new OauthUtil();
            URI uri = new URI(URL);
            HttpClient httpclient = util.getNewHttpClient();
            HttpPost postMethod = new HttpPost(uri);
            StringBuffer paramString = new StringBuffer();
            paramString.append("OAuth");
            for (int i = 0; i < params.size(); i++) {
                paramString.append(" " + params.get(i).getName());
                paramString.append("=\"" + encodeUrl(params.get(i).getValue()) + "\",");
            }
            String xx = paramString.substring(0, paramString.length() - 1);
            postMethod.addHeader("Authorization", xx);
            HttpResponse httpResponse = httpclient.execute(postMethod);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                Log.i("DEBUG", "result: " + strResult);
                return strResult;
            }
        } catch (Exception e) {
            Log.i("DEBUG", e.toString());
        }
        return null;
    }

    public static final String METHOD_POST = "POST";

    /**
	 * 签名
	 * 
	 * @param method
	 *            可�?METHOD_POST
	 * @param url
	 *            地址
	 * @param params
	 *            baseString
	 * @param secret
	 *            密钥
	 * @return 签名后的字串
	 */
    public static String getOAuth_signature(String method, String url, String params, String secret) {
        String baseString = method + "&" + encodeUrl(url) + "&" + encodeUrl(params);
        return hmacsha1(baseString, secret);
    }

    /**
	 * 对字符串加密
	 * 
	 * @param data
	 *            明文
	 * @param key
	 *            密钥
	 * @return 密文
	 */
    public static String hmacsha1(String data, String key) {
        byte[] byteHMAC = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(spec);
            byteHMAC = mac.doFinal(data.getBytes());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException ignore) {
        }
        String oauth = Base64.encodeToString(byteHMAC, Base64.DEFAULT);
        return oauth;
    }

    private static final String ENCODING = "US-ASCII";

    /**
	 * 对字符串进行编码
	 * 
	 * @param s
	 * @return
	 */
    public static String encodeUrl(String s) {
        if (s == null) {
            return "";
        }
        String encoded = "";
        try {
            encoded = URLEncoder.encode(s, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '+') {
                sBuilder.append("%20");
            } else if (c == '*') {
                sBuilder.append("%2A");
            } else if ((c == '%') && ((i + 1) < encoded.length()) && ((i + 2) < encoded.length()) & (encoded.charAt(i + 1) == '7') && (encoded.charAt(i + 2) == 'E')) {
                sBuilder.append("~");
                i += 2;
            } else {
                sBuilder.append(c);
            }
        }
        return sBuilder.toString();
    }

    /**
	 * 获取时间�?
	 * 
	 * @return
	 */
    public static String generateTimeStamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    /**
	 * 产生32位随机数ֵ
	 * 
	 * @param is32
	 *            是否�?2�?
	 * @return
	 */
    public static String generateNonce(boolean is32) {
        Random random = new Random();
        String result = String.valueOf(random.nextInt(9876599) + 123400);
        if (is32) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(result.getBytes());
                byte b[] = md.digest();
                int i;
                StringBuffer buf = new StringBuffer("");
                for (int offset = 0; offset < b.length; offset++) {
                    i = b[offset];
                    if (i < 0) i += 256;
                    if (i < 16) buf.append("0");
                    buf.append(Integer.toHexString(i));
                }
                result = buf.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
