package com.jflickrorganizr.flickr.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.jflickrorganizr.flickr.model.APIMethod;
import com.jflickrorganizr.flickr.model.SortedParameters;
import com.jflickrorganizr.flickr.service.connector.ConnectorService;

/**
 * The base class of all Flickr service classes that holds basic functionality needed by most services.
 * 
 * @author David Lim
 */
public abstract class FlickrService {

    protected static final String PARAM_API_KEY = "api_key";

    protected static final String PARAM_METHOD = "method";

    protected static final String PARAM_API_SIG = "api_sig";

    private static final String MD5_ALGORITHM = "MD5";

    private ConnectorService connectorService;

    private String apiKey;

    private String sharedSecret;

    /**
     * Creates a <code>FlickrService</code> with the given API key and shared secret given by Flickr.
     * 
     * @param connectorService
     *            the connector service to communicate with Flickr
     * @param apiKey
     *            the API key for the application
     * @param sharedSecret
     *            the Flickr-produced shared secret string
     */
    public FlickrService(final ConnectorService connectorService, final String apiKey, final String sharedSecret) {
        this.connectorService = connectorService;
        this.apiKey = apiKey;
        this.sharedSecret = sharedSecret;
    }

    /**
     * Gets the connector service to Flickr.
     * 
     * @return the connector service instance
     */
    protected ConnectorService getConnectorService() {
        return connectorService;
    }

    /**
     * Gets the application's API key.
     * 
     * @return API key
     */
    protected String getApiKey() {
        return apiKey;
    }

    /**
     * Gets the application's shared secret.
     * 
     * @return the shared secret string
     */
    protected String getSharedSecret() {
        return sharedSecret;
    }

    /**
     * Convenience method to add a parameter for a connector service call.
     * 
     * @param name
     *            the name of the parameter to add
     * @param value
     *            the value of the parameter to add; the <code>toString()</code> method will be called so this must not
     *            be <code>null</code>
     * @param params
     *            the parameters to add to
     */
    protected void addParam(final String name, final Object value, final SortedParameters params) {
        params.add(name, value.toString());
    }

    /**
     * Convenience method to add the application's API key as a parameter for a connector service call.
     * 
     * @param params
     *            the parameters to add to; must not be <code>null</code>
     */
    protected void addApiKeyParam(final SortedParameters params) {
        params.add(PARAM_API_KEY, apiKey);
    }

    /**
     * Convenience method to add a Flickr API's method name as a parameter for a connector service call.
     * 
     * @param method
     *            the API method whose name it is to add
     * @param params
     *            the parameters to add to; must not be <code>null</code>
     */
    protected void addMethodNameParam(final APIMethod method, final SortedParameters params) {
        params.add(PARAM_METHOD, method.getMethodName());
    }

    /**
     * Convenience method to add an API authentication signature as a parameter for a connector service call. Note that
     * the signature value is generated from the given list of existing <code>params</code>; any other required
     * parameters must first be added prior to calling this method.
     * 
     * @param params
     *            the parameters to add to and to generate from; must not be <code>null</code>
     */
    protected void signAndClose(final SortedParameters params) {
        params.add(PARAM_API_SIG, generateSignature(params));
        params.close();
    }

    /**
     * Generates an API signature for Flickr based on the given parameters.
     * 
     * @param params
     *            the parameters to generate a signature from
     * @return the API signature
     */
    protected String generateSignature(final SortedParameters params) {
        StringBuilder sig = new StringBuilder(sharedSecret);
        for (String paramName : params.keySet()) {
            sig.append(paramName).append(params.get(paramName));
        }
        MessageDigest md5 = getMD5Digest();
        byte[] md5digest = md5.digest(sig.toString().getBytes());
        return convertToHexString(md5digest);
    }

    private MessageDigest getMD5Digest() {
        try {
            return MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String convertToHexString(final byte[] data) {
        StringBuilder partialHex = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int count = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    partialHex.append((char) ('0' + halfbyte));
                } else {
                    partialHex.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (count++ < 1);
        }
        return partialHex.toString();
    }
}
