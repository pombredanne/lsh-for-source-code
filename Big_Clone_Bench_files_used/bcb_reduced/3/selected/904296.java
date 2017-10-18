package org.turnlink.sclm.dao.hibernate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.turnlink.sclm.dao.AccountDao;
import org.turnlink.sclm.model.Account;

@Repository("accountDao")
@Transactional
public class AccountDaoHibernate implements AccountDao {

    private final Log log = LogFactory.getLog(getClass());

    private HibernateTemplate hibernateTemplate;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    public Account findByAccountId(Integer accountId) {
        return hibernateTemplate.get(Account.class, accountId);
    }

    private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b : bytes) {
            String hexString = Integer.toHexString(0x00FF & b);
            string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
    }

    @SuppressWarnings("unchecked")
    public Account findByUsername(String userName) {
        List<Account> res = hibernateTemplate.find("from Account where username = ? ", userName);
        if (res != null && res.size() > 1) {
            log.error("More than one account user with same username & password! (userName = " + userName + ")");
            for (Account account : res) log.error("\t accountId " + account.getAccountId());
            return null;
        }
        return res == null || res.isEmpty() ? null : res.get(0);
    }

    @Transactional(readOnly = false)
    public void removeByAccountId(Integer accountId) {
        hibernateTemplate.delete(accountId);
    }

    @Transactional(readOnly = false)
    public void saveOrUpdate(Account account) {
        hibernateTemplate.saveOrUpdate(account);
    }

    public static void main(String[] args) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
        }
        String hash = bytes2String(sha1.digest(("password-salt").getBytes()));
        System.out.println(hash);
    }
}
