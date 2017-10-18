package com.googlecode.webduff;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.http.HttpServletRequest;
import com.googlecode.webduff.authentication.provider.Credential;
import com.googlecode.webduff.store.WebdavStoreFactory;

public class WebDuffSessionManager {

    @SuppressWarnings("unused")
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WebDuffSessionManager.class);

    private Map<String, WebDuffSession> theSessions;

    private long intervalForZuzplaPulvInMilliseconds;

    private WebDuffSessionZuzplaPulv theZuzlaPulv;

    private static MessageDigest md5Digester;

    static {
        try {
            md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private class WebDuffSessionZuzplaPulv extends TimerTask {

        private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WebDuffSessionZuzplaPulv.class);

        private WebDuffSessionManager theManager;

        public WebDuffSessionZuzplaPulv(WebDuffSessionManager manager) {
            theManager = manager;
        }

        public void run() {
            theManager.cleanUp();
            log.debug("ZuzlaPulv is cleaning up.");
        }
    }

    public WebDuffSessionManager() {
        this(300000);
    }

    public void finalize() {
        log.debug("Stopping ZuzplaPulv.");
        theZuzlaPulv.cancel();
    }

    public WebDuffSessionManager(long intervalCleanUp) {
        theSessions = new Hashtable<String, WebDuffSession>();
        theZuzlaPulv = new WebDuffSessionZuzplaPulv(this);
        log.debug("Starting ZuzlaPulv.");
        Timer aTimer = new Timer();
        aTimer.scheduleAtFixedRate(theZuzlaPulv, intervalCleanUp, intervalCleanUp);
    }

    public WebDuffSession getSession(WebdavStoreFactory theStoreFactory, Credential aCredential, HttpServletRequest aRequest) {
        synchronized (this) {
            String sessionId = getSessionId(aCredential, aRequest);
            if (!theSessions.containsKey(sessionId)) {
                theSessions.put(sessionId, new WebDuffSession(aCredential, theStoreFactory.create(aCredential)));
            }
            return theSessions.get(sessionId).touch();
        }
    }

    private String getSessionId(Credential credential, HttpServletRequest request) {
        md5Digester.reset();
        String aString = credential.getUsername();
        md5Digester.update(aString.getBytes(), 0, aString.length());
        return new BigInteger(1, md5Digester.digest()).toString(16);
    }

    public long getIntevalForZuzplaPulv() {
        return intervalForZuzplaPulvInMilliseconds;
    }

    private void cleanUp() {
        synchronized (this) {
            for (String aSessionId : theSessions.keySet()) {
                WebDuffSession aSession = theSessions.get(aSessionId);
                if ((aSession.getTimestamp() + 3600) < System.currentTimeMillis()) {
                    theSessions.remove(aSessionId);
                }
            }
        }
    }
}
