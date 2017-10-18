package ch.unibe.a3ubAdmin.persistence.serializedtables;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ch.unibe.id.se.beans.AuthorizationBean_v03;
import ch.unibe.id.se.beans.LocalAuthorizationBean_v03;

/**
 * This Manager provides acces to the new Persistence-Layer built septembre 2006
 * for the new version 2 of a3ublogin this Manager implements the different
 * interfaces from the
 * 
 * @author daniel marthaler
 * @version 1.0 / last change: 05.08.2006
 * @since JDK 1.5.0
 */
public class LocalPersistenceManager implements ILocalAccountStorage {

    private IPersistenceManager locMan = null;

    private Log log = null;

    /**
	 * Singleton instance
	 */
    private static LocalPersistenceManager instance = null;

    /**
	 * Private constructor from the singleton
	 * 
	 * @throws Exception
	 */
    public LocalPersistenceManager() throws Exception {
        locMan = new QuickPersistenceManager_v02(false);
        this.log = LogFactory.getLog(getClass());
        if (log.isInfoEnabled()) {
            log.info("Single instance of LocalPersistenceManager created");
        }
    }

    /**
	 * returns the singele instance
	 * 
	 * @return PersistenceManager
	 * @throws Exception
	 */
    public static synchronized LocalPersistenceManager getInstance() throws Exception {
        if (instance == null) {
            instance = new LocalPersistenceManager();
        }
        return instance;
    }

    public AuthorizationBean_v03 doLocalLogin(LocalAuthorizationBean_v03 tempL, String password) {
        if (log.isDebugEnabled()) {
            log.debug("New Login: appid; " + tempL.getApplicationIdentifier() + " username; " + tempL.getUserIdentifier());
        }
        if (log.isDebugEnabled()) {
            log.debug("Found user: " + tempL);
        }
        String hashedPassword = "";
        try {
            hashedPassword = HashUtil.getInstance().digest(password);
        } catch (Exception e) {
            if (log.isFatalEnabled()) {
                log.fatal("Error hashing password: ", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Password user: " + tempL.getPassword() + " given: " + hashedPassword);
        }
        String logincount = "";
        if (tempL.getCustomAttributesMap() != null) {
            List<String> strList = tempL.getCustomAttributesMap().get("localaccount.logincount");
            if (strList != null && strList.size() > 0) {
                logincount = strList.get(0);
            }
        }
        int logincountI = 0;
        if (logincount != null && !logincount.equals("")) {
            logincountI = Integer.parseInt(logincount);
            logincountI = logincountI + 1;
        } else {
            logincountI = 1;
        }
        List lastlogin = new ArrayList<String>();
        lastlogin.add("" + System.currentTimeMillis());
        List countlist = new ArrayList<String>();
        countlist.add("" + logincountI);
        tempL.getCustomAttributesMap().put("localaccount.lastlogin", lastlogin);
        tempL.getCustomAttributesMap().put("localaccount.logincount", countlist);
        locMan.saveObject(tempL.getApplicationIdentifier() + "-" + tempL.getUserIdentifier(), tempL);
        if (log.isDebugEnabled()) {
            log.debug("User to give back: " + tempL.getPassword() + " -> " + tempL.getPassword().equals(hashedPassword));
        }
        if (tempL != null && tempL.getPassword() != null && tempL.getPassword().equals(hashedPassword)) {
            tempL.setPassword("");
            return tempL;
        }
        return null;
    }

    public LocalAuthorizationBean_v03 getLocalUser(String appid, String username) {
        LocalAuthorizationBean_v03 tempL = new LocalAuthorizationBean_v03();
        tempL = (LocalAuthorizationBean_v03) locMan.loadObject(appid + "-" + username, tempL);
        return tempL;
    }

    public static void main(String[] args) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
