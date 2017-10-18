package com.lightminds.map.tileserver.admin;

import com.lightminds.map.tileserver.admin.xml.CustomerElement;
import java.util.logging.Level;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import java.util.Random;
import java.util.Date;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;

/**
 * Use to check access control with customers configuration
 * From 2009-07-31 uses database instead of xml
 * @author Jon Åkerström
 * @author Peter Johan Salomonsen
 */
public class Security {

    private static final Logger logger = Logger.getLogger(Security.class);

    EntityManager em;

    {
        try {
            em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/em");
        } catch (NamingException ex) {
            java.util.logging.Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Creates a new instance of Security */
    public Security() {
    }

    public String login(HttpSession callingSession, String username, String password) {
        String token = null;
        String customer = null;
        int timeoutInSeconds = 0;
        HashSet<Integer> tileProviderIds = new HashSet<Integer>();
        boolean bLoginOk = false;
        String dbPassword = (String) em.createNamedQuery("getCustomerPasswordByUsername").setParameter("username", username).getSingleResult();
        if (dbPassword.equals(password)) {
            CustomerElement ce = (CustomerElement) em.createNamedQuery("getCustomerByUsername").setParameter("username", username).getSingleResult();
            customer = ce.getName();
            timeoutInSeconds = ce.getTimeout();
            String[] tileProviderIdsArray = ce.getTileProvideridsArray();
            for (String tileProviderId : tileProviderIdsArray) tileProviderIds.add(Integer.parseInt(tileProviderId));
            bLoginOk = true;
        }
        if (bLoginOk) {
            token = SessionHandler.getInstance().alreadyGotValidSession(customer);
            if (token == null) {
                Random random = new Random();
                token = callingSession.getId() + new Date().getTime() + random.nextLong();
                MessageDigest md5 = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Unable to digest the token.", e);
                }
                md5.update(token.getBytes());
                byte[] array = md5.digest();
                StringBuffer sb = new StringBuffer();
                for (int j = 0; j < array.length; ++j) {
                    int b = array[j] & 0xFF;
                    if (b < 0x10) sb.append('0');
                    sb.append(Integer.toHexString(b));
                }
                token = sb.toString();
                SessionHandler.getInstance().registerValidSession(token, customer, timeoutInSeconds, tileProviderIds);
            }
        }
        return token;
    }
}
