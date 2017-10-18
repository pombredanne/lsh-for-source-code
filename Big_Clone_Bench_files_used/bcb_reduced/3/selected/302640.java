package com.producteev4j.transport;

import com.producteev4j.exceptions.ProducteevException;
import com.producteev4j.exceptions.ProducteevServiceException;
import com.producteev4j.exceptions.ProducteevSignatureException;
import com.producteev4j.model.request.ProducteevParameters;
import com.producteev4j.model.request.ProducteevRequest;
import com.producteev4j.model.response.BaseResponse;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA.
 * User: jcarrey
 * Date: 14/05/11
 * ServerTimeImpl: 17:01
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractTransport implements ProducteevTransport {

    private static final String TOKEN_PARAM = "token";

    private static final String API_KEY_PARAM = "api_key";

    private static final String ALGORITHM = "MD5";

    private static final String API_SIG = "api_sig";

    private String apiKey;

    private String apiSecret;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public Object process(ProducteevRequest request) throws ProducteevException {
        return process(request, null);
    }

    public Object process(ProducteevRequest request, String userToken) throws ProducteevException {
        if (request.getResponseClass() == null || !BaseResponse.class.isAssignableFrom(request.getResponseClass())) {
            throw new ProducteevServiceException("Response Class should extend :" + BaseResponse.class.getName());
        }
        request.getParams().putValue(API_KEY_PARAM, apiKey);
        if (userToken != null) {
            request.getParams().putValue(TOKEN_PARAM, userToken);
        }
        request.getParams().putValue(API_SIG, getSignature(request.getParams()));
        switch(request.getMethod()) {
            case GET:
                return _doGet(request);
            default:
                throw new IllegalArgumentException("Unkown method");
        }
    }

    protected abstract Object _doGet(ProducteevRequest request) throws ProducteevServiceException;

    /**
     * Gets the URL without values of params
     * somethink like :
     * <p/>
     * http://host/contextPath/service?apiKey={apiKey}&blabla
     *
     * @param request
     * @return - The full url with {paramKey} where it should be the paramValue
     */
    public String getFullUrl(ProducteevRequest request) {
        StringBuilder buffer = new StringBuilder(request.getEndpoint());
        buffer.append("?").append(request.getParams().getFullUrl());
        return buffer.toString();
    }

    public String getSignature(ProducteevParameters params) throws ProducteevSignatureException {
        StringBuilder buffer = new StringBuilder(params.getSignatureUrl());
        buffer.append(apiSecret);
        return MD5(buffer.toString());
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String MD5(String text) throws ProducteevSignatureException {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance(ALGORITHM);
            byte[] md5hash;
            md.update(text.getBytes("utf-8"), 0, text.length());
            md5hash = md.digest();
            return convertToHex(md5hash);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ProducteevSignatureException("No such algorithm : " + ALGORITHM, nsae);
        } catch (UnsupportedEncodingException e) {
            throw new ProducteevSignatureException("No such algorithm : " + ALGORITHM, e);
        }
    }
}
