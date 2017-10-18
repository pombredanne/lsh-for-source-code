package sdloader.javaee;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.servlet.http.HttpSession;
import sdloader.util.Base64;
import sdloader.util.MessageDigestUtil;

/**
 * セッション管理クラス
 * 
 * @author c9katayama
 * @author shot
 */
public abstract class SessionManager {

    private static long sessionIdSeed = System.currentTimeMillis() + (int) new SecureRandom().nextInt() * 1000;

    private static final MessageDigest digest = MessageDigestUtil.createMessageDigest();

    public abstract HttpSession getSession(String sessionId, boolean createNew, InternalWebApplication webApplication);

    protected synchronized String createNewSessionId() {
        String sessionId = null;
        long sesIdSeed = ++sessionIdSeed;
        byte[] digestId = digest.digest(Long.toString(sesIdSeed).getBytes());
        sessionId = new String(Base64.encodeBase64(digestId));
        return sessionId;
    }

    public abstract void close();
}
