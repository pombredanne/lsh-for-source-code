package ch.unibe.a3ubAdmin.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import ch.unibe.a3ubAdmin.control.ApplRightManager;
import ch.unibe.a3ubAdmin.control.DatabaseManager;
import ch.unibe.a3ubAdmin.exception.ValidationException;
import ch.unibe.a3ubAdmin.model.AppRightClass;
import ch.unibe.a3ubAdmin.model.AppRightValue;
import ch.unibe.a3ubAdmin.model.Application;
import ch.unibe.a3ubAdmin.model.Group;
import ch.unibe.a3ubAdmin.model.ViewUser;
import ch.unibe.a3ubAdmin.persistence.LocalPersistenceManager;
import ch.unibe.a3ubAdmin.persistence.NonHibernateSQLManager;
import ch.unibe.a3ubAdmin.persistence.PersistenceManager;
import ch.unibe.a3ubAdmin.persistence.serializedtables.QuickPersistenceManagerProperties_v02;
import ch.unibe.a3ubAdmin.util.RightStringParser;
import ch.unibe.id.se.beans.InternalAuthorizationBean_v03;
import ch.unibe.id.se.beans.LocalAuthorizationBean_v03;

/**
 * This manager provides acces to the new business-layer built in september 2006
 * for the new version 2 of a3ubadmin2.
 * 
 * @author daniel marthaler
 * @version 1.0 / last change: 14.09.2006
 * @since JDK 1.5.0
 */
public class BusinessManager {

    Log log = LogFactory.getLog(getClass());

    /**
	 * Singleton instance
	 */
    private static BusinessManager instance = null;

    /**
	 * Private constructor from the singleton
	 */
    private BusinessManager() {
    }

    /**
	 * returns the singele instance
	 * 
	 * @return PersistenceManager
	 */
    public static synchronized BusinessManager getInstance() {
        if (instance == null) {
            instance = new BusinessManager();
        }
        return instance;
    }

    /**
	 * Gets a LocalAuthorizationBean_v03 from the persistence by appid and
	 * username
	 * 
	 * @param appid
	 * @param username
	 * @return LocalAuthorizationBean_v03
	 */
    public LocalAuthorizationBean_v03 getLocalAccount(String appid, String username) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            ViewUser user = man.loadViewUser(username);
            if (user == null) {
                return null;
            }
            ViewUser bean = man.loadViewUser(username);
            if (bean == null || bean.getCustommap() == null) {
                return null;
            }
            return (LocalAuthorizationBean_v03) bean.getCustommap().get("authbean");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error loading local user: " + username, e);
            }
        }
        return null;
    }

    public InternalAuthorizationBean_v03 getInternalAccount(String username) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            ViewUser user = man.loadViewUser(username);
            if (user == null) {
                return null;
            }
            ViewUser bean = man.loadViewUser(username);
            if (bean == null || bean.getCustommap() == null) {
                return null;
            }
            return (InternalAuthorizationBean_v03) bean.getCustommap().get("internalauthbean");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error loading internal user: " + username, e);
            }
        }
        return null;
    }

    public LocalAuthorizationBean_v03 getLocalAccountWithPassword(String appid, String username) {
        LocalAuthorizationBean_v03 tempL = new LocalAuthorizationBean_v03();
        DatabaseManager db = new DatabaseManager();
        ViewUser user = null;
        try {
            user = db.loadViewUser(username);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e, e);
            }
        }
        tempL = (LocalAuthorizationBean_v03) user.getCustommap().get("authbean");
        return tempL;
    }

    public InternalAuthorizationBean_v03 getLocalAccountWithPassword(String username) {
        InternalAuthorizationBean_v03 tempL = new InternalAuthorizationBean_v03();
        DatabaseManager db = new DatabaseManager();
        ViewUser user = null;
        try {
            user = db.loadViewUser(username);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e, e);
            }
        }
        return (InternalAuthorizationBean_v03) user.getCustommap().get("internalauthbean");
    }

    /**
	 * Persists a LocalAuthorizationBean_v03
	 * 
	 * @param bean -
	 *            to save
	 */
    public void saveLocalAccount(LocalAuthorizationBean_v03 bean) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
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
            String uidnS = bean.getUserIdentifierNumber();
            int uidn = -1;
            if (uidnS == null || uidnS.equals("")) {
                uidn = PersistenceManager.getInstance().getNextLocalUidNumber();
            } else {
                uidn = Integer.parseInt(uidnS);
            }
            ViewUser user = null;
            try {
                user = man.loadViewUser(uidn);
            } catch (ObjectNotFoundException e) {
                user = new ViewUser();
                user.setUidnumber(uidn);
            }
            bean.setUserIdentifierNumber("" + user.getUidnumber());
            user.setCn(bean.getCommonName());
            Group group = man.loadGroup(-1);
            if (log.isDebugEnabled()) {
                log.debug("group: " + group);
            }
            user.setGroup(group);
            user.setUid(bean.getUserIdentifier());
            user.setUidnumber(Integer.parseInt(bean.getUserIdentifierNumber()));
            if (user.getCustommap() == null) {
                user.setCustommap(new HashMap<String, Object>());
            }
            user.getCustommap().put("authbean", bean);
            man.saveUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            if (log.isErrorEnabled()) {
                log.error("error saving local user", e);
            }
        }
    }

    /**
     * Creates a new ViewUser in the db. A already existing one (i.e. one with the same uid) is entirely
     * removed before.
     * The corresponding group (given by the groupid) is retrieved from the db and set to the viewUser.
     * @param viewUser the ViewUser to create and insert into the db
     * @param groupId the GroupId of the ViewUser
     */
    public void createUser(ViewUser viewUser, int groupId) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            try {
                ViewUser oldUser = null;
                oldUser = man.loadViewUser(viewUser.getUidnumber());
                if (oldUser != null) {
                    this.deleteUser(oldUser.getUid());
                }
            } catch (ObjectNotFoundException e) {
                log.debug("No existing user in the db with the id '" + viewUser.getUidnumber() + "' found.");
            }
            Group group = man.loadGroup(groupId);
            viewUser.setGroup(group);
            man.saveUser(viewUser);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("error creating new user", e);
            }
        }
    }

    /**
	 * Persists a LocalAuthorizationBean_v03
	 * 
	 * @param bean -
	 *            to save
	 */
    public void saveInternalAccount(InternalAuthorizationBean_v03 bean) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
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
                InternalAuthorizationBean_v03 old = (InternalAuthorizationBean_v03) this.getInternalAccount(bean.getUserIdentifier());
                if (old != null && old.getPassword() != null && !old.getPassword().equals("")) {
                    newPassword = old.getPassword();
                }
            }
            bean.setPassword(newPassword);
            String uidnS = bean.getUserIdentifierNumber();
            int uidn = 0;
            if (uidnS == null || uidnS.equals("")) {
                uidn = PersistenceManager.getInstance().getNextInternalUidNumber();
            } else {
                uidn = Integer.parseInt(uidnS);
            }
            ViewUser user = null;
            try {
                user = man.loadViewUser(uidn);
            } catch (ObjectNotFoundException e) {
                user = new ViewUser();
                user.setUidnumber(uidn);
            }
            bean.setUserIdentifierNumber("" + user.getUidnumber());
            user.setCn(bean.getCommonName());
            Group group = man.loadGroup(-2);
            if (log.isDebugEnabled()) {
                log.debug("group: " + group);
            }
            user.setGroup(group);
            user.setUid(bean.getUserIdentifier());
            user.setUidnumber(Integer.parseInt(bean.getUserIdentifierNumber()));
            if (user.getCustommap() == null) {
                user.setCustommap(new HashMap<String, Object>());
            }
            user.getCustommap().put("internalauthbean", bean);
            man.saveUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            if (log.isErrorEnabled()) {
                log.error("error saving internal user", e);
            }
        }
    }

    /**
	 * Returns a List with all the LocalAuthorizationBean_v03 to a specific
	 * application
	 * 
	 * @param appId
	 * @return LocalAuthorizationBean_v03
	 */
    public List<LocalAuthorizationBean_v03> getLocalAccounts(String appId) {
        List<LocalAuthorizationBean_v03> result = new ArrayList<LocalAuthorizationBean_v03>();
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            List<ViewUser> users = man.loadLocalViewUserWithSet(appId);
            for (ViewUser v : users) {
                result.add((LocalAuthorizationBean_v03) v.getCustommap().get("authbean"));
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting local accounts", e);
            }
        }
        return result;
    }

    public List<InternalAuthorizationBean_v03> getInternalAccounts() {
        List<InternalAuthorizationBean_v03> result = new ArrayList<InternalAuthorizationBean_v03>();
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            List<ViewUser> users = man.loadInternalViewUserWithSet();
            for (ViewUser v : users) {
                result.add((InternalAuthorizationBean_v03) v.getCustommap().get("internalauthbean"));
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting internal accounts user", e);
            }
        }
        return result;
    }

    /**
	 * Returns a List with all the LocalAuthorizationBean_v03 in Hibernate
	 * Objects to a specific application
	 * 
	 * @param appId
	 * @return LocalAuthorizationBean_v03
	 */
    public List<ViewUser> getLocalAccountsAsViewUserList(String appId) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        List<ViewUser> users = null;
        try {
            users = man.loadLocalViewUserWithSet(appId);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting locall accounts", e);
            }
        }
        return users;
    }

    public List<ViewUser> getInternalAccountsAsViewUserList() {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        List<ViewUser> users = null;
        try {
            users = man.loadInternalViewUserWithSet();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting internal accounts", e);
            }
        }
        return users;
    }

    public void saveRightsOnLocalAccount(Map<Integer, Application> map, String appid) {
        List<LocalAuthorizationBean_v03> locallist = this.getLocalAccounts(appid);
        Map<String, List<String>> newMap = new TreeMap<String, List<String>>();
        for (Integer id : map.keySet()) {
            Application app = map.get(id);
            List<AppRightClass> clList = app.getRightClasses();
            for (AppRightClass r : clList) {
                List<String> nValList = new ArrayList<String>();
                List<AppRightValue> rvl = r.getValues();
                for (AppRightValue rv : rvl) {
                    if (rv.isSelected()) {
                        nValList.add(rv.getValue());
                    }
                }
                if (nValList.size() > 0) {
                    newMap.put(r.getName(), nValList);
                }
            }
            for (LocalAuthorizationBean_v03 loc : locallist) {
                if (loc.getUserIdentifierNumber().equals("" + id)) {
                    LocalAuthorizationBean_v03 temp = this.getLocalAccount(appid, loc.getUserIdentifier());
                    TreeMap<String, Map<String, List<String>>> temM = new TreeMap<String, Map<String, List<String>>>();
                    temM.put("default", newMap);
                    temp.setUnibeApplAuthorizationMap(temM);
                    this.saveLocalAccount(temp);
                }
            }
        }
    }

    /**
	 * Checks if there are some local accounts on a specific application
	 * 
	 * @param appId
	 * @return boolean
	 */
    public boolean hasLocalAccounts(String appId) {
        List l = this.getLocalAccountsAsViewUserList(appId);
        if (l.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasInternalAccounts(String appId) {
        List l = this.getInternalAccountsAsViewUserList();
        if (l.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public void deleteUser(String uid) {
        ch.unibe.a3ubAdmin.control.DatabaseManager man = new ch.unibe.a3ubAdmin.control.DatabaseManager();
        try {
            man.deleteUser(man.loadViewUser(uid), this.getAllApplicationNames());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error deleting user: ", e);
            }
        }
    }

    /**
	 * Loads the rights from a user into the application
	 * @deprecated
	 * @param uid
	 *            The userid identifying the user
	 * @param appl
	 *            The application in which to load the rights
	 * @return The application with the rigths loaded
	 */
    public Application loadLocalUserAppRights(String uid, Application appl) throws ValidationException {
        ApplRightManager.sortApplicationRights(appl);
        LocalAuthorizationBean_v03 bean = null;
        try {
            bean = LocalPersistenceManager.getInstance().getLocalAccount(appl.getAppId(), uid);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error with local-persistencemanager: ", e);
            }
        }
        Map<String, List> map = bean.getUnibeApplAuthorizationMap();
        String right = "";
        Set<String> keySet = map.keySet();
        List<String> valueList;
        for (String rightStr : keySet) {
            right += RightStringParser.RIGHT_CLASS_SEPARATOR + rightStr + RightStringParser.RIGHT_CLASS_ASSIGNMENT_OPERATOR;
            valueList = map.get(rightStr);
            for (String valueStr : valueList) {
                right += valueStr + RightStringParser.RIGHT_VALUE_SEPARATOR;
            }
            right = right.substring(0, right.length() - 1);
        }
        if (!right.equals("")) {
            right = right.substring(1);
        }
        if (map == null) {
            appl.resetSelection();
        } else {
            RightStringParser.fromRightStringToApplication(right, appl);
        }
        return appl;
    }

    /**
	 * Returns a List with all the registred Applications
	 * 
	 * @return List<String>
	 */
    public List<String> getAllApplicationNames() {
        List<String> temp = new ArrayList<String>();
        Map<String, Properties> m = null;
        try {
            m = ch.unibe.a3ubAdmin.persistence.PersistenceManager.getInstance().readPropertieMap();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
        Set<String> iter = m.keySet();
        for (String temps : iter) {
            if (!temps.equals(".DS_Store")) {
                temp.add(temps);
            }
        }
        return temp;
    }

    public Map<String, String> getRootMap() {
        Map<String, String> list = new HashMap<String, String>();
        try {
            list = PersistenceManager.getInstance().readA3ubadminMaps("rootMap");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting root map: ", e);
            }
        }
        return list;
    }

    public String getRootListString() {
        String result = "";
        try {
            Map<String, String> map = PersistenceManager.getInstance().readA3ubadminMaps("rootMap");
            if (map != null) {
                for (String s : map.keySet()) {
                    result = result + s + ", ";
                }
            } else {
                return "-> Leere Liste";
            }
            result = result.substring(0, result.length() - 2);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error getting root map: ", e);
            }
        }
        return result;
    }

    public void addRootToList(String id, String name) {
        try {
            Map<String, String> map = PersistenceManager.getInstance().readA3ubadminMaps("rootMap");
            if (map == null) {
                map = new HashMap<String, String>();
            }
            map.put(id, name);
            PersistenceManager.getInstance().saveA3ubadminMaps("rootMap", map);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error dealing wiht the root map on the persistence: ", e);
            }
        }
    }

    public void removeRootFromList(String id) {
        try {
            Map<String, String> map = PersistenceManager.getInstance().readA3ubadminMaps("rootMap");
            map.remove(id);
            PersistenceManager.getInstance().saveA3ubadminMaps("rootMap", map);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error dealing wiht the root map on the persistence: ", e);
            }
        }
    }

    public String getRights(String uidText) {
        return NonHibernateSQLManager.getInstance().getRights(uidText);
    }

    public void addApplication(Properties app, String appName) throws Exception {
        Properties p = PersistenceManager.getInstance().readProperties(appName);
        if (p != null) {
            throw new Exception("can not add " + appName + " because there is one registred with that name");
        }
        Properties po = PersistenceManager.getInstance().readOldProperties(appName);
        if (po != null) {
            throw new Exception("can not add " + appName + " because there is an old one registred with that name ");
        }
        PersistenceManager.getInstance().writeProperties(appName, app);
    }

    public Properties getApplication(String oldAppName) {
        QuickPersistenceManagerProperties_v02 p = null;
        try {
            p = new QuickPersistenceManagerProperties_v02(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Properties app = p.readProperties(oldAppName);
        return app;
    }

    public void deleteApplication(String appName) throws Exception {
        if (appName != null && appName.equals("a3ubadmin2")) {
            throw new Exception("can not delete " + appName);
        }
        Properties p = PersistenceManager.getInstance().readProperties(appName);
        if (p == null) {
            throw new Exception("can not delete " + appName + " because it does not exist");
        }
        PersistenceManager.getInstance().writeOldProperties(appName, p);
        PersistenceManager.getInstance().deleteProperties(appName);
    }

    public void restoreApplication(String appName) throws Exception {
        Properties p = PersistenceManager.getInstance().readProperties(appName);
        if (p != null) {
            throw new Exception("can not restore " + appName + " because there is one registred with that name");
        }
        Properties po = PersistenceManager.getInstance().readOldProperties(appName);
        if (po == null) {
            throw new Exception("can not restore app because " + appName + " is not an old app");
        }
        PersistenceManager.getInstance().writeProperties(appName, po);
        PersistenceManager.getInstance().deleteOldProperties(appName);
    }

    /**
	 * Returns a List with all the registred Applications
	 * 
	 * @return List<String>
	 */
    public List<String> getAllOldApplicationNames() {
        List<String> temp = new ArrayList<String>();
        Map<String, Properties> m = null;
        try {
            m = ch.unibe.a3ubAdmin.persistence.PersistenceManager.getInstance().readOldPropertieMap();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
        Set<String> iter = m.keySet();
        for (String temps : iter) {
            if (!temps.equals(".DS_Store")) {
                temp.add(temps);
            }
        }
        return temp;
    }

    public static void main(String[] args) {
        BusinessManager man = BusinessManager.getInstance();
        Application app = new Application();
        app.setAppId("studiss");
        System.out.println(man.getLocalAccount("sms4ub", "sms4ub.daniel"));
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
