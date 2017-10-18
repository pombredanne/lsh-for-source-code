package de.sreindl.amavisadmin.bo;

import de.sreindl.amavisadmin.ApplicationBean1;
import de.sreindl.amavisadmin.SessionBean1;
import de.sreindl.amavisadmin.db.MailQueueEntry;
import de.sreindl.amavisadmin.db.MailQueueEntryDAO;
import de.sreindl.amavisadmin.db.MailQueueReceipient;
import de.sreindl.amavisadmin.db.MailQueueReceipientDAO;
import de.sreindl.amavisadmin.db.Request;
import de.sreindl.amavisadmin.db.RequestDAO;
import de.sreindl.amavisadmin.db.User;
import de.sreindl.amavisadmin.db.UserDAO;
import de.sreindl.amavisadmin.db.util.HibernateSessionFactory;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.mail.internet.InternetAddress;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jstk.JSTKUtil;

/**
 * This class encasulates user business handling.
 *
 * @author sreindl
 */
public class UserBO {

    private static Log log = LogFactory.getLog(UserBO.class);

    /** Creates a new instance of UserBO */
    public UserBO() {
    }

    /**
     * Lookup a user from the database.
     *
     * @param userName
     * @throws ArrayIndexOutOfBoundsException
     * @return the user from the storage. In case the user cannot be found,
     * null will be returned.
     */
    public static User lookupUser(String userName) throws ArrayIndexOutOfBoundsException, BOException {
        User user = null;
        try {
            UserDAO userDAO = new UserDAO();
            List userList = null;
            userList = userDAO.findByUsername(userName);
            if (userList.size() > 1) {
                throw new ArrayIndexOutOfBoundsException("More than one user returned for user " + userName);
            }
            if (userList != null) {
                user = (User) userList.get(0);
            }
        } catch (HibernateException he) {
            log.error(he.getMessage(), he);
            throw new BOException(he);
        }
        return user;
    }

    /**
     * Encode password by default algorithm
     *
     * @param password The clear text password to encode
     *
     * @return the encoded password with the algorithm in front of the
     *         password
     */
    public static String encodePassword(String password) {
        String algorithm = ConfigurationBO.getConfValue(CONF_PASSWORD_ALGORITHM, DEFAULT_PASSWORD_ALGORITHM);
        return encodePassword(password, algorithm);
    }

    /**
     * Encode password by given algorithm
     *
     * @param password The password to encode
     *
     * @param algorithm The algorithm to use
     *
     * @return The encoded password with the algorithm in front of the passowrd
     *         separated by a '/'.
     */
    public static String encodePassword(String password, String algorithm) {
        if (password == null) return null;
        MessageDigest passwdSignature = null;
        try {
            passwdSignature = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsae) {
            log.fatal("encode password exception", nsae);
            return null;
        }
        byte[] buffer = passwdSignature.digest(password.getBytes());
        String encPwd = algorithm + "/" + JSTKUtil.hexStringFromBytes(buffer);
        if (log.isDebugEnabled()) log.debug("Encoded password is " + encPwd);
        return encPwd;
    }

    /**
     * Check password for correctness
     */
    public static int checkPassword(User user, String password) {
        assert (user != null);
        if (user.getLocked() != null && user.getLocked().booleanValue()) {
            return ACCOUNT_LOCKED;
        }
        String algorithm = ConfigurationBO.getConfValue(CONF_PASSWORD_ALGORITHM, DEFAULT_PASSWORD_ALGORITHM);
        String userPwd = user.getPassword();
        if (userPwd == null) {
            return ACCOUNT_WRONG_PWD;
        }
        if (userPwd.indexOf('/') > -1) {
            String newAlgorithm = userPwd.substring(0, userPwd.indexOf('/'));
            if (!newAlgorithm.equals(algorithm)) {
                log.info("Password algorithm for user " + user.getUsername() + " (" + newAlgorithm + ") is different to default algorithm " + algorithm);
                algorithm = newAlgorithm;
            }
        } else {
            userPwd = algorithm + '/' + userPwd;
        }
        return userPwd.equals(encodePassword(password, algorithm)) ? ACCOUNT_OK : ACCOUNT_WRONG_PWD;
    }

    /**
     * Logout current user from session
     * @param session
     */
    public static void logoutUser(SessionBean1 session) {
        User user = session.getCurrentUser();
        if (user == null) {
            log.warn("Try to log out from session which doesn't contain an active user");
            return;
        }
        session.setLoggedOn(false);
        log.info("User " + user.getUsername() + " logged off");
        ApplicationBean1.deregisterUser(user);
        ;
        session.setCurrentUser(null);
    }

    /**
     * Generate default password.
     *
     * <p>Generates a default password for the user and returns the password
     * as clear text. In addition the password is stored encrypted in the
     * user field. </p>
     * <p>The default password is the user's primary email address</p>
     */
    public static String generateDefaultPassword(User user) {
        if (user.getEmail() == null) {
            return "";
        }
        return encodePassword(user.getEmail());
    }

    /**
     * Reset user password
     *
     * @param contextPath the context path for the links
     * @param user the user where the account needs to be reset.
     * @throws Exception In case of mail, template or database errors
     */
    public static void resetPassword(String contextPath, User user) throws Exception, BORequestDoesAlreadyExistException {
        Session session = null;
        Transaction trx = null;
        Request req = null;
        VelocityContext context = new VelocityContext();
        log.info("Request password request for user " + user.getUsername() + "/" + user.getFullname());
        try {
            session = HibernateSessionFactory.getSession();
            trx = session.beginTransaction();
            Integer validUntil = ConfigurationBO.getConfValueNumber(CONF_LOST_PASSWD_MAIL_VALIDITY, CONF_DEFAULT_LOST_PASSWD_MAIL_VALIDIY);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR_OF_DAY, validUntil);
            req = getNewRequest(user, cal);
            if (req.getParam2() == null) {
                context.put("reset", Boolean.FALSE);
            } else {
                context.put("reset", Boolean.TRUE);
            }
            context.put("user", user);
            context.put("link", contextPath + "/faces/HandleRequest.jsp?token=" + req.getKey());
            context.put("signature", ConfigurationBO.getConfValue(ConfigurationBO.APP_TITLE));
            context.put("baselink", contextPath + "/");
            context.put("validhours", validUntil);
            Calendar expDate = Calendar.getInstance();
            expDate.add(Calendar.HOUR_OF_DAY, validUntil.intValue());
            context.put("validto", expDate.getTime().toString());
            Template lostPwd = Velocity.getTemplate("/de/sreindl/amavisadmin/templates/LostPassword.vm");
            StringWriter w = new StringWriter();
            lostPwd.merge(context, w);
            MailQueueEntry mqe = new MailQueueEntry();
            mqe.setEncoding("text/plain");
            mqe.setStatus(MailQueueEntryDAO.STATUS_IMMEDIATE);
            mqe.setFrom(ConfigurationBO.getConfValue(ConfigurationBO.MAIL_SENDER));
            mqe.setMailText(w.toString());
            mqe.setSubject((String) context.get("subject"));
            MailQueueReceipient mqr = new MailQueueReceipient();
            mqr.setReceipient(UserBO.formatMailAddress(user));
            mqr.setType(MailQueueReceipientDAO.TYPE_TO);
            mqe.setReceipients(new HashSet());
            mqe.getReceipients().add(mqr);
            session.save(mqe);
            mqr.setMailQueueEntry(mqe);
            session.save(mqr);
            MailHandler.sendIntermediateMail(mqe);
            session.update(mqe);
            trx.commit();
            HibernateSessionFactory.closeSession();
        } catch (HibernateException he) {
            log.error("Database error during password reset: " + he.getMessage(), he);
            try {
                if (trx != null && trx.isActive()) {
                    trx.rollback();
                }
                HibernateSessionFactory.closeSession();
            } catch (HibernateException he1) {
                log.error("Error during rollback", he1);
            }
            throw he;
        } catch (Exception e) {
            log.error("Exception during password reset " + e.getMessage(), e);
            throw e;
        }
    }

    public static String formatMailAddress(User user) {
        try {
            InternetAddress ia = new InternetAddress(user.getEmail(), user.getFullname());
            return ia.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return user.getEmail();
        }
    }

    /**
     * look for existing requests
     */
    private static Request getNewRequest(User user, Calendar cal) {
        RequestDAO rDAO = new RequestDAO();
        Date now = new Date();
        Session session = HibernateSessionFactory.getSession();
        Request req = new Request();
        req.setHandlerType(HANDLER_TYPE_LOST_PASSWORD);
        req.setStatus(RequestDAO.STATUS_ACTIVE);
        req.setParam1(user.getUsername());
        List list = rDAO.findByExample(req);
        req = null;
        if (list != null) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                Request r = (Request) i.next();
                if (r.getExpirationDate().before(now)) {
                    r.setStatus(RequestDAO.STATUS_EXPIRED);
                    session.update(r);
                } else {
                    if (req != null) {
                        log.fatal("PANIC! Incosistend database records for lost password!");
                        log.fatal("Setting illegal record " + r.getKey() + " to error!");
                        r.setStatus(RequestDAO.STATUS_ERROR);
                        session.update(r);
                    } else {
                        req = r;
                    }
                }
            }
        }
        if (req == null) {
            req = new Request();
            req.setKey(UUID.randomUUID().toString());
            req.setStatus(RequestDAO.STATUS_ACTIVE);
            req.setExpirationDate(cal.getTime());
            req.setHandlerType(HANDLER_TYPE_LOST_PASSWORD);
            req.setParam1(user.getUsername());
            session.save(req);
            return req;
        } else {
            req.setParam2("Updated where old expiration date was " + req.getExpirationDate().toString());
            req.setExpirationDate(cal.getTime());
            session.update(req);
            return req;
        }
    }

    /** login was ok  */
    public static final int ACCOUNT_OK = 0;

    /** account is locked */
    public static final int ACCOUNT_LOCKED = 1;

    /** password didn't matched */
    public static final int ACCOUNT_WRONG_PWD = 2;

    /** Default retention in days for a new subscriber */
    public static final String CONF_DEFAULT_RETENTION = "amavisadmin.user.default_retention";

    /** Algorithm used to encode password */
    public static final String CONF_PASSWORD_ALGORITHM = "amavisadmin.user.password_algorithm";

    /** How long a lost password mail remains valid */
    public static final String CONF_LOST_PASSWD_MAIL_VALIDITY = "amavisadmin.user.lost_password_valid";

    /** How long a lost password mail remains valid */
    public static final Integer CONF_DEFAULT_LOST_PASSWD_MAIL_VALIDIY = new Integer(48);

    private static final String DEFAULT_PASSWORD_ALGORITHM = "SHA-256";

    /**
     * Handler type for request table in case of lost password.
     *
     * In this case field param1 has to be filled with the user name of the user where the password
     * has to be reset.
     */
    public static final String HANDLER_TYPE_LOST_PASSWORD = "lost password";
}
