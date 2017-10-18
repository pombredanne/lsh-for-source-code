package org.kirhgoff.vkontakte.methods;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;

public class VkontakteRequest {

    private String version;

    private long timestamp;

    private long random;

    private final String vkontakteApiURL;

    private String apiID;

    private String method;

    private Object secret;

    private boolean testMode = false;

    public VkontakteRequest(String vkontakteAPI) {
        this.vkontakteApiURL = vkontakteAPI;
    }

    public VkontakteRequest() {
        vkontakteApiURL = "http://api.vkontakte.ru/api.php";
        timestamp = System.currentTimeMillis();
        version = "2.0";
        random = new Random().nextInt();
    }

    public void setApiID(String apiID) {
        this.apiID = apiID;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setRandom(long random) {
        this.random = random;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getURL() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(vkontakteApiURL).append("?");
        sb.append("api_id=").append(apiID).append("&");
        sb.append("v=").append(version).append("&");
        sb.append("method=").append(method).append("&");
        sb.append("timestamp=").append(timestamp).append("&");
        sb.append("random=").append(random).append("&");
        if (testMode) sb.append("test_mode=").append(1).append("&");
        sb.append("sig=").append(calculateSignature());
        return sb.toString();
    }

    private String calculateSignature() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(buildMD5String());
        byte[] digestBytes = digest.digest();
        BigInteger bigInteger = new BigInteger(1, digestBytes);
        return bigInteger.toString(16);
    }

    private byte[] buildMD5String() {
        StringBuilder sb = new StringBuilder();
        sb.append("api_id=").append(apiID);
        sb.append("method=").append(method);
        sb.append("random=").append(random);
        if (testMode) sb.append("test_mode=").append(1);
        sb.append("timestamp=").append(timestamp);
        sb.append("v=").append(version);
        sb.append(secret);
        return sb.toString().getBytes();
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }
}
