package org.one.stone.soup.wiki.access.control;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.one.stone.soup.constants.TimeConstants;

public class SessionStore implements Runnable {

    private static long TIMEOUT = TimeConstants.HOUR_MILLIS;

    private static Hashtable<String, Session> sessions = null;

    private int sessionCount = 0;

    private class Session {

        private String userId;

        private long lastAccessed = System.currentTimeMillis();

        private String sessionId;

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        @SuppressWarnings("unused")
        public String getSessionId() {
            return sessionId;
        }
    }

    public SessionStore() {
        if (sessions == null) {
            sessions = new Hashtable<String, Session>();
        }
        new Thread(this, "Session Store").start();
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(TimeConstants.MINUTE_MILLIS);
                long time = System.currentTimeMillis();
                Enumeration<String> keys = sessions.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    Session session = sessions.get(key);
                    if (time - session.lastAccessed > TIMEOUT) {
                        sessions.remove(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String authenticateUser(String sessionId) {
        Session session = null;
        if (sessionId != null) {
            session = sessions.get(sessionId);
        }
        if (session == null) {
            return null;
        } else {
            session.lastAccessed = System.currentTimeMillis();
            return session.userId;
        }
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public String createSession(String userId) {
        String sessionId = getSessionId();
        Session session = new Session();
        session.userId = userId;
        session.setSessionId(sessionId);
        sessions.put(sessionId, session);
        return sessionId;
    }

    public synchronized String getSessionId() {
        sessionCount++;
        String data = sessionCount + " " + Math.random() * Integer.MAX_VALUE;
        return getMD5(data);
    }

    public String getMD5(String data) {
        try {
            MessageDigest md5Algorithm = MessageDigest.getInstance("MD5");
            md5Algorithm.update(data.getBytes(), 0, data.length());
            byte[] digest = md5Algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            String hexDigit = null;
            for (int i = 0; i < digest.length; i++) {
                hexDigit = Integer.toHexString(0xFF & digest[i]);
                if (hexDigit.length() < 2) {
                    hexDigit = "0" + hexDigit;
                }
                hexString.append(hexDigit);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ne) {
            return data;
        }
    }
}
