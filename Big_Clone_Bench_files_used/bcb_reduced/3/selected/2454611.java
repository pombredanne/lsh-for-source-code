package com.xucia.jsponic.security;

import static com.xucia.jsponic.data.GlobalData.ACCESS_FIELD;
import static com.xucia.jsponic.data.GlobalData.BASIS_FIELD;
import static com.xucia.jsponic.data.GlobalData.IN_GROUPS_FIELD;
import static com.xucia.jsponic.data.GlobalData.PASSWORD_FIELD;
import static com.xucia.jsponic.data.GlobalData.PERMISSION_LEVEL_FIELD;
import static com.xucia.jsponic.data.GlobalData.ROLE_FIELD;
import static com.xucia.jsponic.data.GlobalData.USER_DATA_FIELD;
import static com.xucia.jsponic.data.GlobalData.USER_Id_FIELD;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import org.apache.commons.codec.binary.Base64;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import com.xucia.jsponic.JsponicRuntime;
import com.xucia.jsponic.data.DataSourceManager;
import com.xucia.jsponic.data.GlobalData;
import com.xucia.jsponic.data.Persistable;
import com.xucia.jsponic.data.PersistableImpl;
import com.xucia.jsponic.data.PersistentList;
import com.xucia.jsponic.data.JsonPath;
import com.xucia.jsponic.data.SimpleStipulation;
import com.xucia.jsponic.data.Stipulation;
import com.xucia.jsponic.data.Templates;

/**
 *
 * There are three states of a user:
 * 1. anonymous private - user is maintained across sessions through cookies and can be converted to a real user by establishing a username and password.  This is the default state.
 * 2. shared public - multiple users are using this computer so a user is not maintained across sessions
 * 3. authenticated - either one or two can convert to this, but if a user already exists, it must do a merge of data
 */
public class User extends NativeObject implements Principal {

    public static final String PUBLIC_USER_NAME = "_public_";

    public Map<Persistable, PermissionLevel> computedPermissions = new WeakHashMap();

    boolean isSharedPublic = false;

    public static Map<Thread, User> threadUserMap = new HashMap();

    private static User publicUserObject;

    static Persistable securitySettings;

    static java.security.acl.Group supervisorGroup;

    public static void resetSecurity() {
        doPriviledgedAction(new PrivilegedAction() {

            public Object run() {
                Object securitySettingsObject = DataSourceManager.getRootObject().get("securitySettings");
                Object supervisorGroupObject = ScriptableObject.NOT_FOUND;
                if (securitySettingsObject instanceof Persistable) {
                    securitySettings = (Persistable) securitySettingsObject;
                    supervisorGroupObject = getSecuritySettings().get("supervisor");
                }
                if (supervisorGroupObject == ScriptableObject.NOT_FOUND) supervisorGroup = new Anyone(); else supervisorGroup = (java.security.acl.Group) supervisorGroupObject;
                return null;
            }
        });
    }

    static {
        resetSecurity();
    }

    long id;

    public User(final String username, final String password) {
        doPriviledgedAction(new PrivilegedAction() {

            public Object run() {
                validateUsername(username);
                setupNewUser(true);
                setUsername(username);
                setPassword(password);
                return null;
            }
        });
    }

    public User() {
    }

    private User(boolean isShared) {
        this.isSharedPublic = isShared;
    }

    public static User PUBLIC_USER = new User(true);

    private static Persistable getSecuritySettings() {
        return securitySettings;
    }

    public static java.security.acl.Group getSupervisorGroup() {
        return (java.security.acl.Group) supervisorGroup;
    }

    private User findUser(String id) {
        List correctUsers = (List) JsonPath.query("#[?(@.userid=#)]", usersTable(), id);
        if (correctUsers.isEmpty()) return publicUserObject; else return (User) correctUsers.get(0);
    }

    private void setupNewUser(final boolean realUser) {
        usersTable().add(User.this);
        boolean registerAsNewUser = true;
        if (realUser) {
            if (Scriptable.NOT_FOUND == getSecuritySettings().get("supervisor")) {
                supervisorGroup = new Group();
                ((Persistable) supervisorGroup).set(GlobalData.BASIS_FIELD, Templates.getBasisForClass(Group.class));
                getSecuritySettings().set("supervisor", supervisorGroup);
                getSupervisorGroup().addMember(User.this);
                registerAsNewUser = true;
            }
        }
        set(BASIS_FIELD, Templates.getBasisForClass(User.class));
        Persistable userData = (Persistable) set(USER_DATA_FIELD, JsponicRuntime.newArray());
        List security = (List) userData.set(ACCESS_FIELD, JsponicRuntime.newArray());
        ((Persistable) security).set(GlobalData.NAME_FIELD, getName() + "'s");
        Persistable rolePermit = JsponicRuntime.newObject();
        Object writeLevel = ((List) JsonPath.query("select * from ? where name='write'", (List) securitySettings.get("permissionLevelTypes"))).get(0);
        rolePermit.set(PERMISSION_LEVEL_FIELD, writeLevel);
        rolePermit.set(ROLE_FIELD, User.this);
        security.add(rolePermit);
    }

    static long getNewId() {
        return (new Random()).nextLong();
    }

    @Deprecated
    public static User publicUser() {
        return PUBLIC_USER;
    }

    String currentTicket;

    public String getCurrentTicket() {
        return currentTicket;
    }

    public static User getUserByTicket(String id, String ipAddress) throws LoginException {
        if (PUBLIC_USER_NAME.equals(id)) return publicUser();
        final String ticket = id + ipAddress;
        Object userObject = doPriviledgedAction(new PrivilegedAction() {

            public Object run() {
                Object userTickets = DataSourceManager.getRootObject().get("userTickets");
                if (!(userTickets instanceof Persistable)) {
                    userTickets = JsponicRuntime.newObject();
                    DataSourceManager.getRootObject().set("userTickets", userTickets);
                }
                return ((Persistable) userTickets).get(ticket);
            }
        });
        if (userObject instanceof User) {
            return (User) userObject;
        } else throw new LoginException("The user ticket is no longer valid");
    }

    @Deprecated
    public String getNewTicket(String ipAddress) {
        String ticket = ipAddress + new Date().getTime() + "" + new Random().nextInt();
        ((Persistable) DataSourceManager.getRootObject().get("userTickets")).set(ticket, this);
        return ticket;
    }

    public static User getUserByUsername(String username) {
        if (username.equals(currentUser().getName())) return currentUser();
        if (getSupervisorGroup().isMember(currentUser())) throw new RuntimeException("You do have access to this user.  To access other users, you must be a system supervisor");
        List correctUsers = (List) JsonPath.query("select * from ? where name=?", usersTable(), username);
        if (correctUsers.size() == 0) {
            throw new RuntimeException("user " + username + " not found");
        }
        return (User) ((Persistable) correctUsers.get(0));
    }

    void mergeUserData(User user) {
    }

    public static User authenticate(final String username, final String password) throws LoginException {
        Object result = doPriviledgedAction(new PrivilegedAction() {

            public Object run() {
                List correctUsers = (List) JsonPath.query("select * from ? where name=?", usersTable(), username);
                if (correctUsers.size() == 0) {
                    return new LoginException("user " + username + " not found");
                }
                Persistable userObject = (Persistable) correctUsers.get(0);
                boolean alreadyHashed = false;
                boolean passwordMatch = password.equals(userObject.get(PASSWORD_FIELD));
                if (!passwordMatch) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA");
                        md.update(((String) userObject.get(PASSWORD_FIELD)).getBytes());
                        passwordMatch = password.equals(new String(new Base64().encode(md.digest())));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    alreadyHashed = true;
                }
                if (passwordMatch) {
                    Logger.getLogger(User.class.toString()).info("User " + username + " has been authenticated");
                    User user = (User) userObject;
                    try {
                        if (alreadyHashed) user.currentTicket = password; else {
                            MessageDigest md = MessageDigest.getInstance("SHA");
                            md.update(password.getBytes());
                            user.currentTicket = new String(new Base64().encode(md.digest()));
                        }
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    return user;
                } else {
                    Logger.getLogger(User.class.toString()).info("The password was incorrect for " + username);
                    return new LoginException("The password was incorrect for user " + username + ". ");
                }
            }
        });
        if (result instanceof LoginException) throw (LoginException) result;
        return (User) result;
    }

    static boolean hostSpecificSecurity = false;

    protected static List usersTable() {
        if (hostSpecificSecurity) return (List) ((Persistable) getWebsiteSourceObject().get("settings")).get("allUsersGroup"); else {
            return (List) doPriviledgedAction(new PrivilegedAction() {

                public Object run() {
                    Object usersObject = securitySettings.get("users");
                    if (usersObject == ScriptableObject.NOT_FOUND) return securitySettings.set("users", JsponicRuntime.newArray()); else return (List) usersObject;
                }
            });
        }
    }

    public void logout() {
    }

    public String getName() {
        Object nameObject = get(GlobalData.NAME_FIELD);
        if (nameObject instanceof String) return (String) nameObject;
        return null;
    }

    public String getPassword() {
        return (String) get(GlobalData.PASSWORD_FIELD);
    }

    public static final Persistable PRIVILEDGED_USER_OBJECT = JsponicRuntime.newObject();

    public static final String EVERYONE = "everyone";

    public static final Object SUPER_ROLE = new Object();

    void validateUsername(String username) {
        List correctUsers = (List) JsonPath.query("select * from ? where name=?", usersTable(), username);
        if (correctUsers.size() != 0) throw new RuntimeException("The username " + username + " is already taken");
    }

    public void setUsername(String username) {
        List correctUsers = (List) JsonPath.query("select * from ? where name=?", usersTable(), username);
        if (correctUsers.size() == 0) {
            set(GlobalData.NAME_FIELD, username);
        } else throw new RuntimeException("The username " + username + " is already taken");
    }

    public User su(String username) {
        if (!getSupervisorGroup().isMember(this)) throw new RuntimeException("You are not a part of the su capable group, so you can not su");
        List correctUsers = (List) JsonPath.query("select * from ? where name=?", usersTable(), username);
        if (correctUsers.size() == 0) {
            throw new RuntimeException("user " + username + " not found");
        }
        Persistable userObject = (Persistable) correctUsers.get(0);
        return (User) userObject;
    }

    public void setPassword(String password) {
        Logger.getLogger(User.class.toString()).info("now changing the password for " + getName());
        set(PASSWORD_FIELD, password);
    }

    static String encrypt(String plaintext) {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("SHA-1");
            d.update(plaintext.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(Base64.encodeBase64(d.digest()));
    }

    private static boolean oneWayCompare(Collection c1, Collection c2) {
        for (Object obj : c1) if (!c2.contains(obj)) return false;
        return true;
    }

    private static boolean compare(Collection c1, Collection c2) {
        return oneWayCompare(c1, c2) && oneWayCompare(c2, c1);
    }

    public static Set<Persistable> calculateMembership(Persistable member, List groupToConsider) {
        Logger.getLogger(User.class.toString()).info("calculating membership");
        Set newInGroups = new HashSet();
        Set<List> groupsToConsider = new HashSet();
        if (groupToConsider != null) groupsToConsider.add(groupToConsider);
        try {
            groupsToConsider.addAll((List) member.get(IN_GROUPS_FIELD));
        } catch (ClassCastException e) {
        }
        try {
            for (List group : groupsToConsider) {
                if (group.contains(member)) {
                    newInGroups.addAll(calculateMembership((Persistable) group, null));
                }
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        Object oldInGroups = member.get(IN_GROUPS_FIELD);
        if (!(oldInGroups instanceof List) || !compare((List) oldInGroups, newInGroups)) {
            NativeArray newInGroupsObject = JsponicRuntime.newArray();
            for (Object obj : newInGroups) newInGroupsObject.add(obj);
            member.set(IN_GROUPS_FIELD, newInGroupsObject);
            for (Persistable subMember : (List<Persistable>) member) calculateMembership(subMember, (List) member);
        }
        newInGroups.add(member);
        resetComputedPermissions();
        return newInGroups;
    }

    public void registerThisThread() {
        threadUserMap.put(Thread.currentThread(), this);
    }

    public static User currentUser() {
        return threadUserMap.get(Thread.currentThread());
    }

    public int compareTo(Object o) {
        return new Integer(hashCode()).compareTo(new Integer(o.hashCode()));
    }

    public static final boolean USERS_HAVE_SEPARATE_SPACE_WITHIN_WEBSITES = false;

    public PersistableImpl getUserData() {
        if (this == publicUserObject) {
            setupNewUser(true);
        }
        PrivilegedAction<PersistableImpl> action = new PrivilegedAction<PersistableImpl>() {

            public PersistableImpl run() {
                Object userDataObject = get(USER_DATA_FIELD);
                if (userDataObject == Scriptable.NOT_FOUND) return null;
                PersistableImpl userData = (PersistableImpl) get(USER_DATA_FIELD);
                if (userData.get("inbox") == Scriptable.NOT_FOUND) {
                    ((Persistable) userData.set("inbox", JsponicRuntime.newObject())).set(GlobalData.CONTAINS_REPEATING_IDENTIFIER, true);
                }
                if (USERS_HAVE_SEPARATE_SPACE_WITHIN_WEBSITES) {
                    PersistableImpl websiteUserData = getOrCreatePersistentObject(userData, getWebsite().getId().toString());
                    if (websiteUserData.get(GlobalData.PARENT_FIELD) != DataSourceManager.getRootObject()) ((PersistableImpl) websiteUserData).set(GlobalData.PARENT_FIELD, DataSourceManager.getRootObject());
                    return (PersistableImpl) websiteUserData;
                }
                return (PersistableImpl) userData;
            }
        };
        if (currentUser() == this) return (PersistableImpl) doPriviledgedAction(action);
        return action.run();
    }

    public static Map<Thread, String> threadWebsiteMap = new HashMap();

    /** This registers which website we are using for this particular user, which affects the users data and possibly the user table that is used */
    public static void registerThisWebsite(String webappContextName) {
        threadWebsiteMap.put(Thread.currentThread(), webappContextName);
        if (!GlobalData.DEFAULT_WEBSITE.equals(webappContextName)) getWebsiteSourceObject();
    }

    public static String getCurrentWebsiteName() {
        return threadWebsiteMap.get(Thread.currentThread());
    }

    static Stipulation websiteStipulation() {
        return new SimpleStipulation("webapp name", getCurrentWebsiteName());
    }

    public static Persistable getWebsite() {
        return getWebsiteSourceObject();
    }

    public static Persistable getWebsiteSourceObject() {
        Object website = doPriviledgedAction(new PrivilegedAction() {

            public Object run() {
                Object websitesObject = DataSourceManager.getRootObject().get("websites");
                Persistable websites = null;
                try {
                    websites = (Persistable) websitesObject;
                } catch (ClassCastException e) {
                    throw new RuntimeException(e);
                }
                return websites.get(getCurrentWebsiteName());
            }
        });
        if (website instanceof Persistable) return (Persistable) website;
        registerThisWebsite(GlobalData.DEFAULT_WEBSITE);
        return null;
    }

    public static Persistable getWebsites() {
        return (Persistable) DataSourceManager.getRootObject().get("websites");
    }

    private static PersistableImpl getOrCreatePersistentObject(Persistable object, String name) {
        Object value = object.get(name);
        if (!(value instanceof PersistableImpl)) {
            value = JsponicRuntime.newObject();
            object.put(name, object, value);
        }
        return (PersistableImpl) value;
    }

    private static PersistentList getOrCreatePersistentList(Persistable object, String name) {
        Object value = object.get(name);
        if (!(value instanceof PersistentList)) {
            value = JsponicRuntime.newArray();
            object.put(name, object, value);
        }
        return (PersistentList) value;
    }

    public PersistableImpl getCurrentEditingVersion() {
        PersistentList editingVersions = getOrCreatePersistentList(getUserData(), "editingVersions");
        PersistableImpl currentVersion = getOrCreatePersistentObject(editingVersions, "currentVersion");
        if (currentVersion == getWebsite()) return currentVersion;
        if (currentVersion.get(GlobalData.BASIS_FIELD) == Scriptable.NOT_FOUND) {
            currentVersion.set(GlobalData.BASIS_FIELD, Templates.findTemplate("versionBasis"));
        }
        if (currentVersion.get(GlobalData.VERSION_OF_FIELD) == Scriptable.NOT_FOUND) {
            currentVersion.set(GlobalData.VERSION_OF_FIELD, getWebsite());
            currentVersion.set(GlobalData.NAME_FIELD, getName() + "'s version of " + getWebsite().get(GlobalData.NAME_FIELD));
        }
        if (!Boolean.TRUE.equals(editingVersions.get(GlobalData.CONTAINS_REPEATING_IDENTIFIER))) {
            editingVersions.set(GlobalData.BASIS_FIELD, Templates.findTemplate("versionsBasis"));
            editingVersions.add(currentVersion);
            editingVersions.add(currentVersion = (PersistableImpl) getWebsite());
            editingVersions.set("currentVersion", currentVersion);
        }
        return currentVersion;
    }

    public static class PriviledgedUser extends User {

        User realCurrentUser;

        PriviledgedUser(User currentUser) {
            super(false);
            this.realCurrentUser = currentUser;
        }

        public User getRealCurrentUser() {
            return realCurrentUser;
        }

        public PersistableImpl getUserData() {
            return realCurrentUser.getUserData();
        }
    }

    public abstract static class PrivilegedActionUserAware implements PrivilegedAction {

        User currentUser;

        public User getCurrentUser() {
            return currentUser;
        }
    }

    public static Object doPriviledgedAction(PrivilegedAction action) {
        PriviledgedUser priviledgedUser = new PriviledgedUser(currentUser());
        if (action instanceof PrivilegedActionUserAware) ((PrivilegedActionUserAware) action).currentUser = currentUser();
        try {
            priviledgedUser.registerThisThread();
            return action.run();
        } finally {
            if (priviledgedUser.realCurrentUser != null) priviledgedUser.realCurrentUser.registerThisThread();
        }
    }

    public static boolean isHostSpecificSecurity() {
        return hostSpecificSecurity;
    }

    public static void setHostSpecificSecurity(boolean websiteSpecificSecurity) {
        User.hostSpecificSecurity = websiteSpecificSecurity;
    }
}
