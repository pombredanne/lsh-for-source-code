package org.turnlink.sclm.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.turnlink.sclm.dao.AccountDao;
import org.turnlink.sclm.dao.AccountSessionDao;
import org.turnlink.sclm.model.Account;
import org.turnlink.sclm.model.AccountSession;
import org.turnlink.sclm.service.SessionManager;

@Service
public class SessionManagerImpl implements SessionManager {

    private final Log log = LogFactory.getLog(getClass());

    private MessageDigest sha1;

    private AccountSessionDao accountSessionDao;

    private AccountDao accountDao;

    @Autowired
    public SessionManagerImpl(AccountSessionDao accountSessionDao, AccountDao accountDao) {
        this.accountSessionDao = accountSessionDao;
        this.accountDao = accountDao;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to create sha1 MessageDigest, missing appropriate Algorithm");
        }
    }

    public AccountSession createAccountSession(String userName, String password) {
        Account account = accountDao.findByUsername(userName);
        if (account == null) {
            log.info("Username does not exist: " + userName);
            return null;
        }
        String hash = bytes2String(sha1.digest((password + "-" + account.getPasswordsalt()).getBytes()));
        if (!account.getPasswordsha1().equals(hash)) {
            log.info("Invalid credentials supplied for : " + userName);
            return null;
        }
        String token = generateToken();
        AccountSession session = new AccountSession();
        session.setTokenCreation(new Timestamp(System.currentTimeMillis()));
        session.setToken(token);
        accountSessionDao.saveOrUpdate(account.getAccountId(), session);
        return session;
    }

    public AccountSession getActiveSession(String token) {
        return accountSessionDao.findByToken(token);
    }

    private String generateToken() {
        return bytes2String(sha1.digest(UUID.randomUUID().toString().getBytes()));
    }

    private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b : bytes) {
            String hexString = Integer.toHexString(0x00FF & b);
            string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
    }

    public void removeOldSessions() {
        accountSessionDao.removeOldSessions();
    }
}
