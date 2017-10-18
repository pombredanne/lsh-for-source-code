package com.ever365.oauth.baidu;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.gqu.utils.FileCopyUtils;
import net.gqu.utils.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import com.ever365.security.SetUserFilter;

/**
 * Servlet implementation class BaiduCallBackServlet
 */
public class BaiduCallBackServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * @see HttpServlet#HttpServlet()
     */
    public BaiduCallBackServlet() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet("https://openapi.baidu.com/oauth/2.0/token?grant_type=authorization_code&code=" + request.getParameter("code") + "&client_id=ot00cKk2xAAWsxHyCwmGC9Af&client_secret=xCY3cPnOreRq7eXwixFqOAjSlMIWdomk&redirect_uri=http%3A%2F%2Fwww.ever365.com%2Foauth%2Fbaidu");
            HttpResponse hr = httpclient.execute(httpget);
            String rawString = FileCopyUtils.copyToString(new InputStreamReader(hr.getEntity().getContent()));
            JSONObject jso = new JSONObject(rawString);
            if (!jso.isNull("access_token")) {
                String at = jso.getString("access_token");
                request.getSession().setAttribute("_baidu_access_token", jso);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("session_key", jso.getString("session_key"));
                params.put("timestamp", URLEncoder.encode(dateformat.format(new Date())));
                String sign = getSignature(params, jso.getString("session_secret"));
                HttpGet userInfoGet = new HttpGet("https://openapi.baidu.com/rest/2.0/passport/users/getLoggedInUser?" + "access_token=" + jso.getString("access_token") + "&format=json");
                HttpResponse userinfohr = httpclient.execute(userInfoGet);
                JSONObject userinfo = new JSONObject(FileCopyUtils.copyToString(new InputStreamReader(userinfohr.getEntity().getContent())));
                if (!userinfo.isNull("uname")) {
                    request.getSession().setAttribute(SetUserFilter.AUTHENTICATION_USER, userinfo.getString("uname") + "@baidu");
                    if (request.getSession().getAttribute("rediretTo") != null) {
                        response.sendRedirect((String) request.getSession().getAttribute("rediretTo"));
                        return;
                    } else {
                        response.sendRedirect("/");
                    }
                } else {
                    response.sendRedirect("/");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
	 * 签名生成算法
	 * @param HashMap<String,String> params 请求参数集，所有参数必须已转换为字符串类型
	 * @param String secret 签名密钥
	 * @return 签名
	 * @throws IOException
	 */
    public static String getSignature(HashMap<String, String> params, String secret) throws IOException {
        Map<String, String> sortedParams = new TreeMap<String, String>(params);
        Set<Entry<String, String>> entrys = sortedParams.entrySet();
        StringBuilder basestring = new StringBuilder();
        for (Entry<String, String> param : entrys) {
            basestring.append(param.getKey()).append("=").append(param.getValue());
        }
        basestring.append(secret);
        byte[] bytes = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            bytes = md5.digest(basestring.toString().getBytes("UTF-8"));
        } catch (GeneralSecurityException ex) {
            throw new IOException(ex);
        }
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex);
        }
        return sign.toString();
    }
}
