package com.tripadvisor.friendster;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FriendsterUtil {

    private static final Logger log = Logger.getLogger(FriendsterClient.class);

    static InputStream logInputStream(InputStream is, Logger log, String message) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        String s = buffer.toString();
        log.debug(message + ": " + s);
        return new ByteArrayInputStream(s.getBytes("UTF-8"));
    }

    static String convertToUrlString(Map<String, String> params) throws UnsupportedEncodingException {
        if (params == null || params.isEmpty()) {
            return null;
        }
        List<String> lKeys = new ArrayList<String>(params.keySet());
        Collections.sort(lKeys);
        StringBuilder sb = new StringBuilder();
        boolean nf = false;
        for (String sKey : lKeys) {
            if (nf) {
                sb.append("&");
            } else {
                nf = true;
            }
            sb.append(sKey);
            sb.append("=");
            sb.append(URLEncoder.encode(params.get(sKey), "UTF8"));
        }
        return sb.toString();
    }

    static Document getDocument(InputStream is) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(is);
        doc.normalizeDocument();
        return doc;
    }

    static String generateSig(String url, String params, String secretKey) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        url = url.replaceAll(FriendsterClient.API_SERVER, "");
        params = URLDecoder.decode(params.replaceAll("&", ""), "UTF-8");
        byte[] buffer = md.digest((url + params + secretKey).getBytes());
        return toHexString(buffer);
    }

    private static final String HEX_DIGITS = "0123456789abcdef";

    static String toHexString(byte[] v) {
        StringBuffer sb = new StringBuffer(v.length * 2);
        for (int i = 0; i < v.length; i++) {
            int b = v[i] & 0xFF;
            sb.append(HEX_DIGITS.charAt(b >>> 4)).append(HEX_DIGITS.charAt(b & 0xF));
        }
        return sb.toString();
    }

    static String collectionToCSV(Collection list) {
        boolean addSep = false;
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (Object uid : list) {
                if (addSep) {
                    sb.append(",");
                } else {
                    addSep = true;
                }
                sb.append(uid);
            }
        }
        return sb.toString();
    }

    static String getNodeText(String tagName, Document doc) {
        NodeList nl = doc.getElementsByTagName(tagName);
        String nodeText = null;
        if (nl != null && nl.getLength() > 0) {
            nodeText = nl.item(0).getTextContent();
        }
        return nodeText;
    }
}
