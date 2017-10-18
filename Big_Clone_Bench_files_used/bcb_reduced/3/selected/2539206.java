package com.once.jdbc;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import com.once.WebAPI;

abstract class OnceConnector {

    private String m_url;

    protected OnceConnector(String url) {
        m_url = url;
    }

    @SuppressWarnings("unchecked")
    protected String doPost(Map data) {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(m_url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        for (Iterator i = data.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            String value = (String) data.get(name);
            method.addParameter(name, value);
        }
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Request failed: " + method.getStatusLine());
            }
            byte[] responseBody = method.getResponseBody();
            return new String(responseBody);
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            method.releaseConnection();
        }
    }

    protected boolean isResponseValid(String response) {
        Pattern ptn = Pattern.compile("^(\\-?\\d+)\\r\\n(.+?)\\r\\n(.*)", Pattern.MULTILINE + Pattern.DOTALL);
        Matcher match = ptn.matcher(response);
        return match.find();
    }

    protected boolean isResponseSucess(String response) {
        return WebAPI.RESPONSE_STATUS_CODE_OK == getResponseCode(response) && WebAPI.RESPONSE_OK.equalsIgnoreCase(getResponseStatus(response));
    }

    protected int getResponseCode(String response) {
        String code = getResponsePart(response, 1);
        try {
            return Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return WebAPI.RESPONSE_STATUS_CODE_GENERIC_ERROR;
        }
    }

    protected String getResponseStatus(String response) {
        return getResponsePart(response, 2);
    }

    protected String getResponseData(String response) {
        return getResponsePart(response, 3);
    }

    protected String[] getResponseDataSplitted(String response) {
        String data = getResponsePart(response, 3);
        return data == null ? new String[0] : data.split("\\r\\n");
    }

    private String getResponsePart(String response, int part) {
        Pattern ptn = Pattern.compile("^(\\-?\\d+)\\r\\n(.+?)\\r\\n(.*)", Pattern.MULTILINE + Pattern.DOTALL);
        Matcher match = ptn.matcher(response);
        while (match.find()) {
            return match.group(part);
        }
        return null;
    }

    public String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(source.getBytes());
            return getHexString(bytes);
        } catch (Throwable t) {
            return null;
        }
    }

    private String getHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] < 0 ? bytes[i] + 256 : bytes[i];
            if (b < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
}
