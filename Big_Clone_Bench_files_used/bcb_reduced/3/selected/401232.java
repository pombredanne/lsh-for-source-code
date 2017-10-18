package com.cashier3.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import com.cashier3.client.PrivateCashier;
import com.cashier3.client.services.LoginService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class LoginServiceImpl extends RemoteServiceServlet implements LoginService {

    private PersistenceManager pm = PMF.get().getPersistenceManager();

    private static final Logger log = Logger.getLogger(PrivateCashier.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    public void login(String mail, String password) throws IllegalArgumentException, UnsupportedEncodingException, NoSuchAlgorithmException {
        DBUser user;
        List<DBUser> results;
        try {
            Query query = pm.newQuery(DBUser.class, "email == '" + mail.trim() + "'");
            results = (List<DBUser>) query.execute();
        } catch (Exception e) {
            log.warning(e.getMessage());
            log.warning("Failed to retrieve user accounts");
            throw new IllegalArgumentException("Failed to retrieve user accounts");
        }
        if (results.size() == 1) {
            user = results.get(0);
            log.info("Found user's account. Checking password match");
            byte[] bytesOfPassword = password.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] theDigest = md.digest(bytesOfPassword);
            if (Arrays.equals(theDigest, user.getPassword())) {
                log.info("Password matching ok. Logged in.");
            } else {
                log.info("Wrong password");
                throw new IllegalArgumentException("Wrong password");
            }
        } else {
            log.info("Failed to find user's account");
            throw new IllegalArgumentException("Failed to find user's account");
        }
    }
}
