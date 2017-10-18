package com.triplea.rolap.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Vector;

/**
 * @author kononyhin
 *
 */
public class ROLAPClientAux {

    private static Properties properties = null;

    public static Properties loadProperties() throws IOException {
        if (null == properties) {
            properties = new Properties();
            properties.load(new FileInputStream("config/AcceptanceTests.properties"));
        }
        return properties;
    }

    public static Vector<String> sendRequest(String request) throws IOException {
        Vector<String> response = null;
        URL url = new URL(loadProperties().getProperty("url") + request);
        HttpURLConnection connection = null;
        int code = 0;
        int repeat = 10;
        for (; repeat > 0; repeat--) {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "text/xml");
            try {
                code = connection.getResponseCode();
                repeat = 0;
            } catch (java.net.ConnectException e) {
                if (repeat == 1) throw e;
            }
        }
        if (code == 400) {
            response = readResponse(connection.getErrorStream());
        } else {
            response = readResponse(connection.getInputStream());
        }
        connection.disconnect();
        return response;
    }

    private static String sid = null;

    private static Vector<String> readResponse(InputStream input) throws IOException {
        Vector<String> responce = new Vector<String>();
        BufferedReader rd = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = rd.readLine()) != null) {
            responce.add(line);
        }
        rd.close();
        return responce;
    }

    private static final String asHexString(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            result.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1));
        }
        return result.toString();
    }

    public static String login() throws Exception {
        if (sid == null) {
            String login = ROLAPClientAux.loadProperties().getProperty("user");
            String password = ROLAPClientAux.loadProperties().getProperty("password");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            String password2 = asHexString(md.digest());
            String query = "/server/login?user=" + login + "&extern_password=" + password + "&password=" + password2;
            Vector<String> res = ROLAPClientAux.sendRequest(query);
            String vals[] = res.get(0).split(";");
            sid = vals[0];
        }
        return sid;
    }

    public static void logout() throws Exception {
        if (sid != null) {
            String query = "/server/logout?sid=" + sid;
            ROLAPClientAux.sendRequest(query);
            sid = null;
        }
    }
}
