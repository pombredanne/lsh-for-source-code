package ch.unibe.a3ubAdmin.persistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ch.unibe.a3ubAdmin.persistence.localaccounts.QuickPersistenceManager_v02;
import ch.unibe.id.se.beans.LocalAuthorizationBean_v03;

/**
 * This manager provides acces to the new Persistence-Layer built septembre 2006
 * for the new version 2 of a3ublogin this Manager implements the different
 * interfaces from the
 * 
 * @author daniel marthaler
 * @version 1.0 / last change: 05.08.2006
 * @since JDK 1.5.0
 */
public class LocalPersistenceManager {

    private Log log = LogFactory.getLog(getClass());

    private QuickPersistenceManager_v02 locMan = null;

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

    public LocalAuthorizationBean_v03 getLocalAccount(String appid, String username) {
        LocalAuthorizationBean_v03 tempL = new LocalAuthorizationBean_v03();
        tempL = (LocalAuthorizationBean_v03) locMan.loadObject(appid + "-" + username, tempL);
        if (tempL != null) {
            tempL.setPassword("");
        }
        return tempL;
    }

    private LocalAuthorizationBean_v03 getLocalAccountWithPassword(String appid, String username) {
        LocalAuthorizationBean_v03 tempL = new LocalAuthorizationBean_v03();
        tempL = (LocalAuthorizationBean_v03) locMan.loadObject(appid + "-" + username, tempL);
        return tempL;
    }

    public void saveLocalAccount(LocalAuthorizationBean_v03 bean) {
        String appid = bean.getApplicationIdentifier();
        String uid = bean.getUserIdentifier();
        String newPassword = "";
        if (!"".equals(bean.getPassword())) {
            try {
                newPassword = ch.unibe.a3ubAdmin.persistence.localaccounts.HashUtil.getInstance().digest(bean.getPassword());
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("error calculating hash: ", e);
                }
            }
        } else {
            LocalAuthorizationBean_v03 old = this.getLocalAccountWithPassword(bean.getApplicationIdentifier(), bean.getUserIdentifier());
            if (old != null && old.getPassword() != null && !old.getPassword().equals("")) {
                newPassword = old.getPassword();
            }
        }
        bean.setPassword(newPassword);
        if (bean.getUserIdentifierNumber() == null || bean.getUserIdentifierNumber().equals("")) {
            bean.setUserIdentifierNumber("" + this.getNextUid());
        }
        locMan.saveObject(appid + "-" + uid, bean);
        List lte = (List) locMan.loadObject("applist." + appid, new ArrayList());
        if (lte == null) {
            lte = new ArrayList();
        }
        if (!lte.contains(appid + "-" + uid)) {
            lte.add(appid + "-" + uid);
            locMan.saveObject("applist." + bean.getApplicationIdentifier(), lte);
        }
    }

    public void deleteLocalAccount(LocalAuthorizationBean_v03 bean) {
        String appid = bean.getApplicationIdentifier();
        String uid = bean.getUserIdentifier();
        locMan.saveObject(appid + "-" + uid, bean);
        List lte = (List) locMan.loadObject("applist." + bean.getApplicationIdentifier(), new ArrayList());
        if (lte == null) {
            lte = new ArrayList();
        }
        lte.remove(appid + "-" + uid);
        if (lte.size() == 0) {
            locMan.deleteObject("applist." + bean.getApplicationIdentifier(), lte);
        } else {
            locMan.saveObject("applist." + bean.getApplicationIdentifier(), lte);
        }
        locMan.deleteObject(appid + "-" + uid, bean);
    }

    /**
	 * Loads all Beans to a specific Application
	 * 
	 * @param appid -
	 *            String
	 * @return List of LocalAuthorizationBean_v03
	 */
    public List<LocalAuthorizationBean_v03> getLocalAccountsForApplicationName(String appid) {
        List<LocalAuthorizationBean_v03> tempL = new ArrayList<LocalAuthorizationBean_v03>();
        List lte = (List) locMan.loadObject("applist." + appid, new ArrayList());
        if (lte == null) {
            return new ArrayList<LocalAuthorizationBean_v03>();
        }
        Iterator<String> iter = lte.iterator();
        while (iter.hasNext()) {
            LocalAuthorizationBean_v03 tempB = new LocalAuthorizationBean_v03();
            tempB = (LocalAuthorizationBean_v03) locMan.loadObject(iter.next(), tempB);
            tempB.setUserGroupIdentifierNumber("-1");
            tempL.add(tempB);
        }
        return tempL;
    }

    /**
	 * Returns the next (-) uidnumber
	 * 
	 * @param appid -
	 *            String
	 * @return List of LocalAuthorizationBean_v03
	 */
    protected int getNextUid() {
        Integer store = (Integer) locMan.loadObject("uidstore", new Integer(0));
        if (store == null || store.intValue() < 1000000) {
            store = new Integer(1000000);
        }
        store = new Integer(store.intValue() + 1);
        locMan.saveObject("uidstore", store);
        return store.intValue();
    }
}
