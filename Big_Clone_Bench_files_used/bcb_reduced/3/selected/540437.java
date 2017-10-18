package tony;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import javax.ws.rs.core.UriBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;

public class TinyFBClient {

    String apiKey;

    String secretKey;

    String version = "1.0";

    String format = "JSON";

    String session;

    TreeMap<String, String> standardParms = new TreeMap<String, String>();

    ClientResponse restResponse;

    Client restClient;

    String facebookRestServer = "http://api.facebook.com/restserver.php";

    String callId;

    public TinyFBClient() {
        standardParms.put("v", version);
        standardParms.put("format", format);
        restClient = Client.create();
    }

    public TinyFBClient(String appIdParm, String appSecretParm) {
        this();
        apiKey = appIdParm;
        secretKey = appSecretParm;
        standardParms.put("secret_key", secretKey);
        standardParms.put("api_key", apiKey);
    }

    public TinyFBClient(String appIdParm, String appSecretParm, String sessionParm) {
        this(appIdParm, appSecretParm);
        session = sessionParm;
        standardParms.put("session_key", sessionParm);
    }

    public TinyFBClient(String appIdParm, String appSecretParm, String sessionParm, String versionParm, String formatParm) {
        this(appIdParm, appSecretParm, sessionParm);
        version = versionParm;
        format = formatParm;
        standardParms.put("v", version);
        standardParms.put("format", format);
    }

    public ClientResponse getResponse(TreeMap<String, String> parms) {
        String currentKey;
        String currentValue;
        String sigParms = "";
        String encodedParm;
        UriBuilder ub = UriBuilder.fromPath(facebookRestServer);
        TreeMap<String, String> restParms = new TreeMap<String, String>();
        restParms.putAll(standardParms);
        restParms.putAll(parms);
        restParms.put("call_id", String.valueOf(System.currentTimeMillis()));
        Collection<String> c = restParms.keySet();
        Iterator<String> itr = c.iterator();
        while (itr.hasNext()) {
            currentKey = (String) itr.next();
            currentValue = restParms.get(currentKey);
            sigParms = sigParms + currentKey + "=" + currentValue;
            if ((currentValue.indexOf("{") >= 0) || (currentValue.indexOf("}") >= 0)) {
                encodedParm = UriComponent.contextualEncode(restParms.get(currentKey), UriComponent.Type.QUERY_PARAM, false);
                ub.queryParam(currentKey, encodedParm);
            } else {
                ub.queryParam(currentKey, currentValue);
            }
        }
        String signature = generateSignature(sigParms, secretKey);
        ub.queryParam("sig", signature);
        WebResource resource;
        URI uri;
        uri = ub.build();
        resource = restClient.resource(uri);
        restResponse = resource.get(ClientResponse.class);
        return (restResponse);
    }

    public ClientResponse getResponse(String method, TreeMap<String, String> parms) {
        parms.put("method", method);
        return (this.getResponse(parms));
    }

    public String call(TreeMap<String, String> parms) {
        ClientResponse thisResponse;
        thisResponse = this.getResponse(parms);
        return (thisResponse.getEntity(String.class));
    }

    public String call(String method, TreeMap<String, String> parms) {
        parms.put("method", method);
        return (this.call(parms));
    }

    public String generateSignature(String requestString, String secretKey) {
        requestString = requestString + secretKey;
        StringBuilder result = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (byte b : md.digest(requestString.toString().getBytes())) {
                result.append(Integer.toHexString((b & 0xf0) >>> 4));
                result.append(Integer.toHexString(b & 0x0f));
            }
            return (result.toString());
        } catch (NoSuchAlgorithmException e) {
            return ("Error: no MD5 ");
        }
    }

    public void setRequestParms(TreeMap<String, String> parms) {
        TreeMap<String, String> requestParms = new TreeMap<String, String>();
        requestParms.putAll(standardParms);
        requestParms.putAll(parms);
    }

    public void setSession(String sessionParm) {
        this.session = sessionParm;
        standardParms.put("session_key", session);
    }

    public void setFormat(String formatParm) {
        this.format = formatParm;
        standardParms.put("format", format);
    }

    public void setVersion(String versionParm) {
        this.version = versionParm;
        standardParms.put("v", version);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        standardParms.put("api_key", apiKey);
    }

    public String getApiKey() {
        return (this.apiKey);
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        standardParms.put("secret_key", secretKey);
    }

    public String getSecretKey() {
        return (this.secretKey);
    }
}
