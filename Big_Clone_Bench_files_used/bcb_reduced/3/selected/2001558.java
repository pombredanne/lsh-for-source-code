package net.spatula.tally_ho.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import net.spatula.tally_ho.model.Account;
import net.spatula.tally_ho.service.beans.UserBean;
import net.spatula.tally_ho.utils.FreeBSDCrypt;
import net.spatula.tally_ho.utils.TextUtils;

/**
 * @author spatula
 * 
 */
public class UserService {

    private static UserService instance = null;

    private UserService() {
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void createUser(EntityManager em, UserBean user) {
        Account account = new Account();
        Query q = em.createQuery("select x from Account x where x.username = 'System'");
        Account creator = (Account) q.getSingleResult();
        account.setCreateDate(new Timestamp(System.currentTimeMillis()));
        account.setChangeSummary("Initial account creation");
        account.setChanger(creator);
        account.setUsername(user.getUsername());
        account.setCryptPassword(FreeBSDCrypt.crypt(user.getPassword(), TextUtils.gibberish(8, 2)));
        account.setEmail(user.getEmail());
        account.setPasswordAnswer(user.getPasswordAnswer());
        account.setPasswordQuestion(user.getPasswordQuestion());
        account.setUid(user.getUid());
        account.setVerified(user.isVerified());
        account.setPremiumUntil(new Date(user.getPremiumUntil().getTime()));
        em.persist(account);
    }

    public void destroyUser(EntityManager em, UserBean user) {
        Account system = getAccountByUsername(em, "System");
        Account destroyAccount = getAccountByUsername(em, user.getUsername());
        destroyAccount.setChanger(system);
        destroyAccount.setChangeSummary("System delete.");
        em.remove(destroyAccount);
    }

    public UserBean authenticateUser(EntityManager em, String username, String password) {
        Account account = getAccountByUsername(em, username);
        if (account == null) {
            return null;
        }
        if (!account.isVerified()) {
            return null;
        }
        if (!account.getCryptPassword().equals(FreeBSDCrypt.crypt(password, account.getCryptPassword()))) {
            return null;
        }
        UserBean bean = getBeanFromAccount(account, UserBean.Authenticator.SERVICE);
        return bean;
    }

    UserBean getBeanFromAccount(Account account, UserBean.Authenticator authenticator) {
        UserBean bean = new UserBean();
        bean.setEmail(account.getEmail());
        bean.setPasswordQuestion(account.getPasswordQuestion());
        bean.setPasswordAnswer(account.getPasswordAnswer());
        bean.setPremiumUntil(account.getPremiumUntil());
        bean.setUid(account.getUid());
        bean.setUsername(account.getUsername());
        bean.setVerified(account.isVerified());
        bean.setCredential(calculateCredential(account));
        bean.setAuthenticator(authenticator);
        return bean;
    }

    Account getAccount(EntityManager em, UserBean user) {
        return getAccountByUsername(em, user.getUsername());
    }

    Account getAccountByUsername(EntityManager em, String username) {
        Query q = em.createQuery("select x from Account x where x.username = ?1");
        q.setParameter(1, username);
        Account account = (Account) q.getSingleResult();
        return account;
    }

    public boolean verifyCredential(EntityManager em, UserBean user) {
        Account account = getAccountByUsername(em, user.getUsername());
        if (account == null) {
            return false;
        }
        String accountCredential = calculateCredential(account);
        return (accountCredential.equals(user.getCredential()));
    }

    private String calculateCredential(Account account) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        try {
            md5.update(account.getUsername().getBytes("UTF-8"));
            md5.update(account.getCryptPassword().getBytes("UTF-8"));
            md5.update(String.valueOf(account.getObjectId()).getBytes("UTF-8"));
            md5.update(account.getUid().getBytes("UTF-8"));
            byte[] digest = md5.digest();
            return TextUtils.calculateMD5(digest);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public UserBean getUserById(EntityManager em, int i) {
        Query q = em.createQuery("select x from Account x where x.objectId = ?1");
        q.setParameter(1, i);
        Account account;
        try {
            account = (Account) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return getBeanFromAccount(account, UserBean.Authenticator.NONE);
    }
}
