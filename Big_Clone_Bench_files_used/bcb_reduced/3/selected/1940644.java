package ru.goldenforests.forum.engine;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.JDBCException;
import net.sf.hibernate.Session;
import org.apache.log4j.Logger;
import ru.goldenforests.forum.AccessException;
import ru.goldenforests.forum.ForumUser;
import ru.goldenforests.forum.beans.ConstForumUser;

public class AuthenticationEngine {

    private static final Logger logger = Logger.getLogger(AuthenticationEngine.class.getName());

    private static final MessageDigest md5Digester;

    static {
        try {
            md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            logger.fatal(ex.toString());
            throw new RuntimeException(ex);
        }
    }

    private BruteForceProtectionSettings settings = new BruteForceProtectionSettings();

    public static final String makePasswordHash(String password) {
        return new BigInteger(1, md5Digester.digest(password.getBytes())).toString(16);
    }

    private final void doAntiBruteForceDelay(SortedMap loginFailures) throws InterruptedException {
        if (loginFailures.isEmpty()) {
            logger.debug("no failures yet - no delay");
            return;
        }
        long lastAttempt = ((Date) loginFailures.lastKey()).getTime();
        long now = new Date().getTime();
        long delayQuantifier = (lastAttempt - now > this.settings.getCeasePeriod()) ? 1 : (1 << loginFailures.size());
        long delay = Math.min(this.settings.getBaseDelay() * delayQuantifier, this.settings.getMaxDelay());
        logger.debug("...anti brute force delay quantifier: " + delayQuantifier + " -> delay=" + delay);
        Thread.sleep(delay);
    }

    public void setBruteForceProtectionSettings(BruteForceProtectionSettings settings) {
        this.settings = settings;
    }

    public final AuthenticationInfo authenticate(Session hs, String remoteIP, String login, String password) throws HibernateException, JDBCException, AccessException {
        if (login.equals("")) return new AuthenticationInfo(UsersManager.getAnonymous(hs));
        String loginFailedPrefix = "Login failed: '" + login + "'/" + remoteIP + " - ";
        String suppliedPasswordHash = makePasswordHash(password);
        try {
            ForumUser requestedUser = UsersManager.getUserByLogin(hs, login);
            SortedMap failures = requestedUser.getLoginFailures();
            logger.debug("Authenticating '" + login + "' (failures so far: " + failures.size() + ")");
            doAntiBruteForceDelay(failures);
            if (!requestedUser.isLocked() && requestedUser.getPasswordHash().equals(suppliedPasswordHash)) {
                logger.info("... successfully authenticated '" + login + "' / " + remoteIP);
                AuthenticationInfo info = new AuthenticationInfo(requestedUser);
                failures.clear();
                return info;
            }
            Timestamp now = new Timestamp(new Date().getTime());
            failures.put(now, remoteIP);
            int totalFailures = failures.size();
            String error;
            if (requestedUser.isLocked()) {
                error = "account is already locked";
            } else if (totalFailures >= this.settings.getSuccessiveFailuresToLockout()) {
                requestedUser.setLocked(true);
                error = "locking account!";
            } else {
                error = "increasing BFP delay";
            }
            logger.warn(loginFailedPrefix + error + " (failures: " + totalFailures + ")");
        } catch (Throwable ex) {
            logger.debug(ex);
            logger.warn(loginFailedPrefix + "\"" + ex.getMessage() + "\"");
        }
        throw new AccessException("��� ������������ ��� ������ ������� �������.");
    }
}
