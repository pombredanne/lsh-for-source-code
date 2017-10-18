package examples.employee.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class TokenUtil {

    public static String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        try {
            return getScopeKey(session.getId(), true);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public static String getToken(HttpServletRequest request, String tokenKey) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        } else {
            synchronized (session.getId().intern()) {
                return (String) session.getAttribute(tokenKey);
            }
        }
    }

    public static boolean isTokenValid(HttpServletRequest request, String tokenKey) {
        return isTokenValid(request, tokenKey, false);
    }

    public static boolean isTokenValid(HttpServletRequest request, String tokenKey, boolean reset) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        synchronized (session.getId().intern()) {
            Object saved = session.getAttribute(tokenKey);
            if (saved == null) {
                return false;
            }
            if (reset) {
                resetToken(request, tokenKey);
            }
            String token = request.getParameter(tokenKey);
            if (token == null) {
                return false;
            }
            return saved.equals(token);
        }
    }

    public static void resetToken(HttpServletRequest request, String tokenKey) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session.getId().intern()) {
                session.removeAttribute(tokenKey);
            }
        }
    }

    public static void saveToken(HttpServletRequest request, String tokenKey) {
        saveToken(request, tokenKey, true);
    }

    public static void saveToken(HttpServletRequest request, String tokenKey, boolean force) {
        HttpSession session = request.getSession(false);
        if (!force && session != null && session.getAttribute(tokenKey) != null) {
            return;
        }
        if (session == null) {
            session = request.getSession();
        }
        synchronized (session.getId().intern()) {
            String token = generateToken(request);
            if (token != null) {
                session.setAttribute(tokenKey, token);
            }
        }
    }

    public static String getScopeKey(Object scope, boolean local) {
        if (scope == null) {
            return null;
        }
        return generateKey(String.valueOf(System.identityHashCode(scope)), local);
    }

    public static String generateLocalKey(Object scope) {
        if (scope == null) {
            return null;
        }
        return generateKey(String.valueOf(System.identityHashCode(scope)), true);
    }

    public static String generateKey(String scopeId, boolean local) {
        if (scopeId == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] id = scopeId.getBytes();
            md.update(id);
            if (local) {
                byte[] now = new Long(System.currentTimeMillis()).toString().getBytes();
                md.update(now);
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Can't happen!", ex);
        }
    }

    public static String toHex(byte[] buffer) {
        StringBuffer sb = new StringBuffer();
        String s = null;
        for (int i = 0; i < buffer.length; i++) {
            s = Integer.toHexString((int) buffer[i] & 0xff);
            if (s.length() < 2) {
                sb.append('0');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
