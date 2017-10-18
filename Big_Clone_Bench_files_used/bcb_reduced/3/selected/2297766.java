package com.appspot.battlerafts.utils;

import com.google.appengine.api.urlfetch.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Static class to send messages to Pusher's REST API.
 * 
 * Please set pusherApplicationId, pusherApplicationKey, pusherApplicationSecret accordingly
 * before sending any request. 
 * 
 * @author Stephan Scheuermann
 * Copyright 2010. Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 */
public class Pusher {

    private static final Logger log = Logger.getLogger(Pusher.class.getName());

    /**
	 *  Pusher Host name
	 */
    private static final String pusherHost = "api.pusherapp.com";

    /**
	 * Pusher Application Identifier
	 */
    private static final String pusherApplicationId = "17701";

    /**
	 * Pusher Application Key
	 */
    public static final String pusherApplicationKey = "8d367b23c1b1356383d5";

    /**
	 * Pusher Secret
	 */
    private static final String pusherApplicationSecret = "0a0a02f56e74c3ea322e";

    /**
	 * Converts a byte array to a string representation
	 * @param data
	 * @return
	 */
    private static String byteArrayToString(byte[] data) {
        BigInteger bigInteger = new BigInteger(1, data);
        String hash = bigInteger.toString(16);
        while (hash.length() < 32) {
            hash = "0" + hash;
        }
        return hash;
    }

    /**
	 * Returns a md5 representation of the given string
	 * @param data
	 * @return
	 */
    private static String md5Representation(String data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(data.getBytes("US-ASCII"));
            return byteArrayToString(digest);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("No MD5 algorithm");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8");
        }
    }

    /**
	 * Returns a HMAC/SHA256 representation of the given string
	 * @param data
	 * @return
	 */
    public static String hmacsha256Representation(String data) {
        try {
            final SecretKeySpec signingKey = new SecretKeySpec(pusherApplicationSecret.getBytes(), "HmacSHA256");
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] digest = mac.doFinal(data.getBytes("UTF-8"));
            digest = mac.doFinal(data.getBytes());
            BigInteger bigInteger = new BigInteger(1, digest);
            return String.format("%0" + (digest.length << 1) + "x", bigInteger);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("No HMac SHA256 algorithm");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key exception while converting to HMac SHA256");
        }
    }

    /**
     * Build query string that will be appended to the URI and HMAC/SHA256 encoded
     * @param eventName
     * @param jsonData
     * @return
     */
    private static String buildQuery(String eventName, String jsonData, String socketID) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("auth_key=");
        buffer.append(pusherApplicationKey);
        buffer.append("&auth_timestamp=");
        buffer.append(System.currentTimeMillis() / 1000);
        buffer.append("&auth_version=1.0");
        buffer.append("&body_md5=");
        buffer.append(md5Representation(jsonData));
        buffer.append("&name=");
        buffer.append(eventName);
        if (!socketID.isEmpty()) {
            buffer.append("&socket_id=");
            buffer.append(socketID);
        }
        return buffer.toString();
    }

    /**
     * Build path of the URI that is also required for Authentication
     * @return
     */
    private static String buildURIPath(String channelName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("/apps/");
        buffer.append(pusherApplicationId);
        buffer.append("/channels/");
        buffer.append(channelName);
        buffer.append("/events");
        return buffer.toString();
    }

    /**
     * Build authentication signature to assure that our event is recognized by Pusher
     * @param uriPath
     * @param query
     * @return
     */
    private static String buildAuthenticationSignature(String uriPath, String query) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("POST\n");
        buffer.append(uriPath);
        buffer.append("\n");
        buffer.append(query);
        String h = buffer.toString();
        return hmacsha256Representation(h);
    }

    /**
     * Build URI where request is send to
     * @param uriPath
     * @param query
     * @param signature
     * @return
     */
    private static URL buildURI(String uriPath, String query, String signature) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("http://");
        buffer.append(pusherHost);
        buffer.append(uriPath);
        buffer.append("?");
        buffer.append(query);
        buffer.append("&auth_signature=");
        buffer.append(signature);
        try {
            return new URL(buffer.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URI");
        }
    }

    /**
     * Delivers a message to the Pusher API without providing a socket_id
     * @param channel
     * @param event
     * @param jsonData
     * @return
     */
    public static HTTPResponse triggerPush(String channel, String event, String jsonData) {
        return triggerPush(channel, event, jsonData, "");
    }

    /**
     * Delivers a message to the Pusher API
     * @param channel
     * @param event
     * @param jsonData
     * @param socketId
     * @return
     */
    public static HTTPResponse triggerPush(String channel, String event, String jsonData, String socketId) {
        String uriPath = buildURIPath(channel);
        String query = buildQuery(event, jsonData, socketId);
        String signature = buildAuthenticationSignature(uriPath, query);
        URL url = buildURI(uriPath, query, signature);
        URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
        HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
        request.addHeader(new HTTPHeader("Content-Type", "application/json"));
        request.setPayload(jsonData.getBytes());
        try {
            return urlFetchService.fetch(request);
        } catch (IOException e) {
            log.warning("Pusher request could not be send to the following URI " + url.toString());
            return null;
        }
    }
}
