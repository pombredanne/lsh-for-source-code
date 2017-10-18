package com.googlecode.webduff.authentication.provider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import com.googlecode.webduff.util.MD5Encoder;

public class DigestCredential implements Credential {

    private String userName;

    private String realm;

    private String clientDigest;

    private String precomputedDigest;

    protected static final MD5Encoder md5Encoder = new MD5Encoder();

    private MessageDigest md5Helper;

    public DigestCredential(HttpServletRequest request, String userName, String realm, String clientDigest, String nOnce, String nC, String cnOnce, String qop, String uri) {
        this.userName = userName;
        this.realm = realm;
        this.clientDigest = clientDigest;
        try {
            if (md5Helper == null) {
                md5Helper = MessageDigest.getInstance("MD5");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
        byte[] buffer = null;
        synchronized (md5Helper) {
            buffer = md5Helper.digest((request.getMethod() + ":" + uri).getBytes());
        }
        precomputedDigest = ":" + nOnce + ":" + nC + ":" + cnOnce + ":" + qop + ":" + md5Encoder.encode(buffer);
    }

    public String getUsername() {
        return userName;
    }

    public boolean checkPassword(String password) {
        String A1 = getA1Digest(password);
        String digestValue = A1 + precomputedDigest;
        byte[] valueBytes = digestValue.getBytes();
        String serverDigest = null;
        synchronized (md5Helper) {
            serverDigest = md5Encoder.encode(md5Helper.digest(valueBytes));
        }
        return serverDigest.equals(clientDigest);
    }

    private String getA1Digest(String password) {
        String digestValue = userName + ":" + realm + ":" + password;
        byte[] valueBytes = digestValue.getBytes();
        byte[] digest = null;
        synchronized (md5Helper) {
            digest = md5Helper.digest(valueBytes);
        }
        return md5Encoder.encode(digest);
    }
}
