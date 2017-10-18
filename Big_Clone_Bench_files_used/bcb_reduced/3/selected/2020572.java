package com.chungco.rest.evdb.service;

import java.io.UnsupportedEncodingException;
import com.chungco.rest.evdb.EvdbUtils;
import com.chungco.rest.exception.RestCommandException;

public class DigestAuthenticationService extends BasicAuthenticationService {

    protected static final String KEY_NONCE = "nonce";

    protected static final String KEY_RESPONSE = "response";

    protected Boolean pDigestURL;

    @Override
    public String getEndpointURL() {
        if (pDigestURL) {
            return getEvdbConfig().getHostName() + "/rest/users/login?" + makeGET(KEY_APP_KEY, KEY_USERNAME, KEY_NONCE, KEY_RESPONSE);
        } else {
            return getEvdbConfig().getHostName() + "/rest/users/login?" + makeGET(KEY_APP_KEY);
        }
    }

    @Override
    protected AuthenticationResult doExecute() throws InterruptedException, RestCommandException {
        pDigestURL = false;
        final AuthenticationResult first = super.doExecute();
        String respStr;
        try {
            final String password = getParam(KEY_PASSWORD);
            final String nonce = first.getNonce();
            respStr = EvdbUtils.digest(nonce, password);
        } catch (final UnsupportedEncodingException e) {
            respStr = "";
        }
        setParam(KEY_NONCE, first.getNonce());
        setParam(KEY_RESPONSE, respStr);
        pDigestURL = true;
        final AuthenticationResult result = super.doExecute();
        result.setNonce(first.getNonce());
        setParam(KEY_NONCE, null);
        setParam(KEY_RESPONSE, null);
        return result;
    }
}
