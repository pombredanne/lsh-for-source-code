package de.dirkdittmar.flickr.group.comment.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.apache.log4j.Logger;

abstract class AbstractService {

    private static final Logger log = Logger.getLogger(AbstractService.class);

    protected String createSignature(String secret, String methodName, Map<String, String> params) {
        Map<String, String> extParams = new HashMap<String, String>(params);
        extParams.put("method", methodName);
        return createSignature(secret, extParams);
    }

    protected String createSignature(String secret, Map<String, String> params) {
        TreeSet<String> sortedSet = new TreeSet<String>(params.keySet());
        StringBuilder sig = new StringBuilder();
        sig.append(secret);
        for (String paramName : sortedSet) {
            sig.append(paramName);
            sig.append(params.get(paramName));
        }
        log.debug("building MD5 for: " + sig.toString());
        try {
            MessageDigest msgDigest = MessageDigest.getInstance("MD5");
            byte[] bytes = sig.toString().getBytes("UTF-8");
            msgDigest.update(bytes, 0, bytes.length);
            return getHexString(msgDigest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
