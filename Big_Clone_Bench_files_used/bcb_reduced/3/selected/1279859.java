package com.servengine.user;

import com.servengine.portal.Portal;
import com.servengine.portal.PortalManagerBean;
import com.servengine.util.FileContent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.ejb.Stateless;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import sun.misc.BASE64Encoder;

/**
 * UserManager session EJB encloses all business logic related tu user management.
 * @see UserManagerLocal
 */
@Stateless(name = "UserManager")
public class UserManagerBean implements UserManagerLocal, UserManagerLocalJAAS {

    protected static String guestUserid = "guest";

    protected static String USER_REQUIRES_SIGNUP_CONFIRMATION = "user.requiressignupconfirmation";

    protected static String FACEBOOK_USERID_PROPERTY_NAME = "facebookUserID";

    protected static String FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME = "friendshipRequestUserids";

    protected static String SIGNUP_CODE_PROPERTY_NAME = "SignupCode";

    protected static String SIGNUP_CODE_REQUIRED = "SignupCodeRequired";

    @PersistenceContext(unitName = "mainPersistenceUnit")
    private EntityManager entityManager;

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(UserManagerBean.class.getName());

    private String[] globalAdminUserids = { "admin" };

    public UserSession getUserSession(String portalid, String userid, String password) throws javax.security.auth.login.FailedLoginException {
        if (userid == null) throw new javax.security.auth.login.FailedLoginException("error.emptyUserid");
        User user = getUser(portalid, userid);
        if (user == null) throw new javax.security.auth.login.FailedLoginException("error.invalidUseridOrPassword");
        if (!user.checkEncodedPassword(password)) throw new javax.security.auth.login.FailedLoginException("error.invalidUseridOrPassword");
        if (user.getJaasUserName() == null || !user.getJaasUserName().equals(userid + "@" + portalid)) user.setJaasUserName(userid + "@" + portalid);
        UserSession usersession = new UserSession(user);
        user.setLastLogin(usersession.getStart());
        entityManager.merge(user);
        entityManager.persist(usersession);
        loadLazyProperties(user);
        loadPortalGuestUser(user.getPortal());
        return usersession;
    }

    public User checkUserPassword(String portalid, String userid, String password) throws javax.security.auth.login.FailedLoginException {
        if (userid == null) throw new javax.security.auth.login.FailedLoginException("error.emptyUserid");
        User user = getUser(portalid, userid);
        if (user == null) throw new javax.security.auth.login.FailedLoginException("error.invalidUseridOrPassword");
        if (!user.checkEncodedPassword(password)) throw new javax.security.auth.login.FailedLoginException("error.invalidUseridOrPassword");
        return user;
    }

    /** Convenience method for authentication in Custom Appserver Auth Module (Glassfish) */
    public String[] getUserRoleNames(String portalid, String userid, String password) throws javax.security.auth.login.FailedLoginException {
        return checkUserPassword(portalid, userid, password).getRoleNames();
    }

    /** Convenience method for authentication in Custom Appserver Auth Module (Glassfish) */
    public Enumeration<String> getUserRoleNames(String portalid, String userid) {
        Vector<String> roleNames = new Vector<String>();
        for (String name : getUser(portalid, userid).getRoleNames()) roleNames.add(name);
        return roleNames.elements();
    }

    /**
	 * Devuelve un usuario para utilizarlo en una nueva sesión, partiendo de una antigua (para identificar el usuario). No solo devuelve el usuario sino que crea una entrada en UserSession y la asocia a ese usuario. Este método se usa en el proceso de autologin.
	 */
    public User getUserBySession(Integer userSessionId) {
        UserSession oldusersession = entityManager.find(UserSession.class, userSessionId);
        return getUser(oldusersession.getPortalid(), oldusersession.getUserid());
    }

    public UserSession getNewUserSession(String portalid, String oldUserSessionIdData) {
        Integer oldUserSessionId = new Integer(oldUserSessionIdData.split(",")[0]);
        UserSession oldusersession = entityManager.find(UserSession.class, oldUserSessionId);
        if (!portalid.equals(oldusersession.getPortalid())) throw new IllegalArgumentException();
        try {
            oldusersession.checkEncodedId(oldUserSessionIdData.split(",")[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
        User user = getUser(oldusersession.getPortalid(), oldusersession.getUserid());
        if (isAdmin(user)) log.warn("Admin user " + user.getUserid() + "@" + portalid + " loggedin via auto login");
        UserSession usersession = new UserSession(user);
        entityManager.persist(usersession);
        user.setLastLogin(usersession.getStart());
        loadLazyProperties(user);
        loadPortalGuestUser(user.getPortal());
        return usersession;
    }

    public static void loadLazyProperties(User user) {
        user.getPortal();
        user.getRoles().size();
        user.getExtraUserdata().size();
        if (user.getFavourites() != null) user.getFavourites().size();
        if (user.getFans() != null) user.getFans().size();
        if (user.getFriends() != null) user.getFriends().size();
        PortalManagerBean.loadCommonLazyRelations(user.getPortal());
    }

    /**
	 * Returns first admin's email (the first user with admin priviledges found in the query).
	 */
    public String[] getAdminEmails(Portal portal) {
        ArrayList<String> emails = new ArrayList<String>();
        for (User user : getAdminUsers(portal)) if (user.getEmail() != null) emails.add(user.getEmail());
        if (emails.size() > 0) return emails.toArray(new String[emails.size()]);
        log.warn("No admins in portal " + portal.getId());
        return new String[0];
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsers() {
        return entityManager.createNamedQuery("User.findAll").getResultList();
    }

    /**
	 * Returns all users in the portal as a Collection of UserSBean objects. If there are no users (¿?) returns an empty ArrayList. never returns null.
	 */
    @SuppressWarnings("unchecked")
    public List<User> getUsers(String portalid) {
        Query query = entityManager.createNamedQuery("User.findByPortalid");
        query.setParameter("portalid", portalid);
        return query.getResultList();
    }

    public Role getAdminRole(Portal portal) {
        Query query = entityManager.createNamedQuery("Role.findByName");
        query.setParameter("portalid", portal.getId());
        query.setParameter("name", portal.getAdminRoleName());
        Role role = (Role) query.getSingleResult();
        loadRoleLazyData(role);
        return role;
    }

    public Collection<User> getAdminUsers(Portal portal) {
        ArrayList<User> users = new ArrayList<User>();
        Role adminRole = getAdminRole(portal);
        if (adminRole == null) log.warn("Portal " + portal.getId() + " has no Admin role!!!"); else for (User user : getUsersByRoleId(portal, adminRole.getId())) users.add(user);
        return users;
    }

    public void persist(User user) {
        if (user.getId() == null) {
            if (exists(user.getPortal().getId(), user.getUserid())) throw new IllegalArgumentException("userExists");
            entityManager.persist(user);
            if (user.getExtraUserdata() != null) for (ExtraUserdata datum : user.getExtraUserdata()) entityManager.persist(datum);
        } else {
            Collection<ExtraUserdata> expurganda = new ArrayList<ExtraUserdata>(entityManager.find(User.class, user.getId()).getExtraUserdata());
            for (ExtraUserdata datum : expurganda) entityManager.remove(datum);
            entityManager.flush();
            for (ExtraUserdata datum : user.getExtraUserdata()) if (datum.getId() == null) entityManager.persist(datum); else entityManager.merge(datum);
            user.setExtraUserdata(null);
            user.setJaasUserName(user.getUserid() + "@" + user.getPortalid());
            entityManager.merge(user);
        }
        updateJAASGroupEntries(user);
    }

    @Deprecated
    public void persist(User user, Map<Integer, String> extraUserdataMap) {
        if (user.getId() == null) {
            if (exists(user.getPortal().getId(), user.getUserid())) throw new EntityExistsException();
            entityManager.persist(user);
        } else {
            user.setJaasUserName(user.getUserid() + "@" + user.getPortalid());
            entityManager.merge(user);
        }
        if (extraUserdataMap != null) {
            Set<ExtraUserdata> expurganda = new HashSet<ExtraUserdata>();
            expurganda.addAll(entityManager.find(User.class, user.getId()).getExtraUserdata());
            for (ExtraUserdata datum : expurganda) entityManager.remove(datum);
            entityManager.flush();
            HashSet<ExtraUserdata> newExtraUserdata = new HashSet<ExtraUserdata>();
            for (ExtraUserdataField field : entityManager.find(Portal.class, user.getPortal().getId()).getExtraUserdataFields()) if (extraUserdataMap.keySet().contains(field.getId())) {
                ExtraUserdata datum = new ExtraUserdata(field, user, extraUserdataMap.get(field.getId()));
                entityManager.persist(datum);
                newExtraUserdata.add(datum);
            }
            user.setExtraUserdata(newExtraUserdata);
        }
        updateJAASGroupEntries(user);
    }

    public void setUserRoles(String portalid, Integer userId, Integer[] roleIds) {
        User user = getUser(portalid, userId);
        Set<Role> roles = new HashSet<Role>();
        if (roleIds != null) for (Integer roleId : roleIds) if (roleId != null) {
            Role role = user.getPortal().getRole(roleId);
            roles.add(role);
        }
        user.setRoles(roles);
        user.setJaasUserName(user.getUserid() + "@" + user.getPortalid());
        entityManager.merge(user);
        updateJAASGroupEntries(user);
    }

    protected void updateJAASGroupEntries(User dumbUser) {
        User user = entityManager.find(User.class, dumbUser.getId());
        if (user.getJAASGroupEntries() == null) user.setJAASGroupEntries(new HashSet<JAASGroupEntry>());
        for (Role role : user.getRoles()) {
            JAASGroupEntry jaasGroupEntry = new JAASGroupEntry(user, role);
            if (!user.getJAASGroupEntries().contains(jaasGroupEntry)) {
                entityManager.persist(jaasGroupEntry);
                user.getJAASGroupEntries().add(jaasGroupEntry);
            }
        }
        Set<JAASGroupEntry> expurganda = new HashSet<JAASGroupEntry>();
        for (JAASGroupEntry jaasGroupEntry : user.getJAASGroupEntries()) if (!user.getRoles().contains(jaasGroupEntry.getRole())) expurganda.add(jaasGroupEntry);
        for (JAASGroupEntry jaasGroupEntry : expurganda) {
            entityManager.remove(jaasGroupEntry);
            user.getJAASGroupEntries().remove(jaasGroupEntry);
        }
    }

    public void userUnbound(Integer userSessionId) {
        entityManager.find(UserSession.class, userSessionId).setEnd(new java.util.Date());
    }

    public boolean isAdmin(User user) {
        return user.getRoles().contains(getAdminRole(user.getPortal()));
    }

    @SuppressWarnings("unchecked")
    public Set<Role> getNewUserRoles(String portalid) {
        Query query = entityManager.createNamedQuery("Role.findNewUserRoles");
        query.setParameter("portalid", portalid);
        return new HashSet<Role>(query.getResultList());
    }

    public boolean exists(String portalid, String userid) {
        Query query = entityManager.createNamedQuery("User.findByUserid");
        query.setParameter("portalid", portalid);
        query.setParameter("userid", userid.toLowerCase());
        try {
            query.getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    public void changeUserPassword(String portalid, String userid, String password) {
        Query query = entityManager.createNamedQuery("User.findByUserid");
        query.setParameter("portalid", portalid);
        query.setParameter("userid", userid.toLowerCase());
        User user = ((User) query.getSingleResult());
        user.encodePassword(password);
        entityManager.persist(user);
    }

    public void changePassword(User user, String password) {
        user.encodePassword(password);
        entityManager.merge(user);
    }

    /**
	 * Este es un "metodo de conveniencia" para cuando no se tiene el usuario instanciado por que es una bean de fuera de ServEngineEJB. El metodo devuelve el usuario instanciado por si la Bean lo necesita (generalmente si)
	 */
    public boolean checkaccess(String portalid, String userid, String classname) {
        Query query = entityManager.createNamedQuery("User.findByUserid");
        query.setParameter("portalid", portalid);
        query.setParameter("userid", userid.toLowerCase());
        User user = (User) query.getSingleResult();
        if (user == null) return false;
        return checkAccess(user, classname);
    }

    /**
	 * Checks if userid has at least one of the roles required by jndiname component.
	 */
    @SuppressWarnings("unchecked")
    public boolean checkAccess(User user, String classname) {
        List<Role> componentroles;
        Query query = entityManager.createNamedQuery("Role.findByClassName");
        query.setParameter("portalid", user.getPortal().getId());
        query.setParameter("classname", classname);
        componentroles = query.getResultList();
        if (componentroles.size() == 0) {
            if (classname.indexOf("Admin") == -1) return true;
            log.warn(classname + " has no roles set in " + user.getPortal().getId());
            if (classname.indexOf("GlobalAdminManagerClient") > -1) return true;
            log.warn(classname + " has no roles set. I can't admit it as a guest client");
            return false;
        }
        Collection<Role> userroles = user.getRoles();
        Iterator<Role> i = componentroles.iterator();
        while (i.hasNext()) if (userroles.contains(i.next())) {
            log.info("Access to " + classname + " for " + user.getUserid() + "@" + user.getPortal().getId() + " granted");
            return true;
        }
        log.info("Access to " + classname + " for " + user.getUserid() + "@" + user.getPortal().getId() + " not granted");
        return false;
    }

    public void removeUser(User user) {
        if (user.getUserid().equals(UserManagerBean.guestUserid)) throw new IllegalArgumentException();
        user.setFans(new HashSet<User>());
        entityManager.merge(user);
        entityManager.flush();
        entityManager.remove(user);
    }

    public void removeUser(String portalid, String userid) {
        removeUser(getUser(portalid, userid));
    }

    public void createAdminUsers(Portal portal) {
        for (String userid : globalAdminUserids) {
            Role adminRole = getAdminRole(portal);
            if (!exists(portal.getId(), userid)) createAdminUser(portal, userid, "", null, null, null, null); else {
                User user = getUser(portal.getId(), userid);
                if (!user.getRoles().contains(adminRole)) {
                    user.getRoles().add(adminRole);
                    persist(user);
                }
            }
        }
    }

    public void createAdminUser(Portal portal, String adminuserid, String adminpassword, String adminfirstname, String adminlastname, String adminlastname2, String adminemail) {
        if (exists(portal.getId(), adminuserid)) throw new IllegalArgumentException();
        User admin = new User(portal, adminuserid);
        if (adminpassword == null) adminpassword = "";
        admin.encodePassword(adminpassword);
        admin.setFirstname(adminfirstname);
        admin.setLastname(adminlastname);
        admin.setLastname2(adminlastname2);
        admin.setEmail(adminemail);
        entityManager.persist(admin);
        entityManager.flush();
        Integer roleIds[] = new Integer[2];
        roleIds[0] = getRole(portal, "admin", true).getId();
        roleIds[1] = getRole(portal, "user", true).getId();
        setUserRoles(admin.getPortalid(), admin.getId(), roleIds);
        log.info("Admin user created: " + admin.getUserid() + "@" + admin.getPortal().getId());
        return;
    }

    public User getUser(String portalid, String userid) {
        Query query = entityManager.createNamedQuery("User.findByUserid");
        query.setParameter("portalid", portalid);
        query.setParameter("userid", userid.toLowerCase());
        User user = (User) query.getSingleResult();
        loadLazyProperties(user);
        return user;
    }

    public User getUser(String portalid, Integer userId) {
        User user = entityManager.find(User.class, userId);
        if (!user.getPortal().getId().equals(portalid)) throw new IllegalArgumentException("Uffffffff... does that user really belong to your portal?");
        loadLazyProperties(user);
        return user;
    }

    public long getUserCount() {
        return (Long) entityManager.createNamedQuery("User.getUserCount").getSingleResult();
    }

    public long getUserCount(String portalid) {
        return (Long) entityManager.createNamedQuery("User.getUserCountByPortalid").setParameter("portalid", portalid).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsersByEmail(String email) {
        Query query = entityManager.createNamedQuery("User.findByEmail");
        query.setParameter("email", email);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<UserSession> getLastUserSessions() {
        return entityManager.createNamedQuery("UserSession.findAll").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<UserSession> getLastUserSessions(String portalid) {
        Query query = entityManager.createNamedQuery("UserSession.findByPortalid");
        query.setParameter("portalid", portalid);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<UserSession> getLastUserSessions(String portalid, String userid) {
        Query query = entityManager.createNamedQuery("UserSession.findByUserid");
        query.setParameter("portalid", portalid);
        query.setParameter("userid", userid.toLowerCase());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<UserSession> getLastUserSessionsBySearchString(String portalid, String searchstring) {
        Query query = entityManager.createNamedQuery("UserSession.findBySearchString");
        query.setParameter("portalid", portalid);
        query.setParameter("searchstring", "%" + searchstring.toLowerCase() + "%");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<User> getLastUsers(String portalid) {
        Query query = entityManager.createNamedQuery("User.findByPortalidOrderByCreatedDesc");
        query.setParameter("portalid", portalid);
        return query.getResultList();
    }

    public void persist(Role role) {
        if (role.getId() == null) {
            if (entityManager.createNamedQuery("Role.findByName").setParameter("portalid", role.getPortal().getId()).setParameter("name", role.getName()).getResultList().size() > 0) throw new IllegalArgumentException();
            entityManager.persist(role);
            entityManager.find(Portal.class, role.getPortal().getId()).getRoles().add(role);
        } else entityManager.merge(role);
        entityManager.flush();
    }

    @Deprecated
    public void removeRole(String portalid, Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        if (!role.getPortal().getId().equals(portalid)) throw new IllegalArgumentException("My very very good friend. That role is not yours...");
        remove(role);
    }

    public void remove(Role role) {
        if (role.getName().equals("admin") || role.getName().equals("user")) throw new IllegalArgumentException("You can't remove role " + role.getName());
        entityManager.remove(entityManager.find(Role.class, role.getId()));
    }

    @SuppressWarnings("unchecked")
    public List<User> getNewUsers(String portalid, Date from) {
        Query query = entityManager.createNamedQuery("User.findNew");
        query.setParameter("portalid", portalid);
        query.setParameter("from", from);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<User> findUsers(String queryString) {
        return entityManager.createNativeQuery(queryString, User.class).getResultList();
    }

    public void signupConfirmation(User user, String signature) throws SecurityException {
        if (!signature.equals(getSignupSignature(user.getPortalid(), user.getUserid()))) throw new SecurityException();
        user.setRoles(getNewUserRoles(user.getPortal().getId()));
        entityManager.merge(user);
        updateJAASGroupEntries(user);
    }

    private String getSignupSignature(String portalid, String userid) {
        String[] values = { portalid, userid };
        return getMessageDigest(values);
    }

    @SuppressWarnings("unchecked")
    public Collection<User> getUsersByRoleIds(Portal portal, Integer[] roleIds) {
        if (roleIds == null || roleIds.length == 0) return entityManager.createNamedQuery("User.findByPortalAndNoRole").setParameter("portal", portal).getResultList();
        Set<User> users = new HashSet<User>();
        for (Integer roleId : roleIds) {
            Role role = entityManager.find(Role.class, roleId);
            if (!role.getPortal().equals(portal)) throw new IllegalArgumentException();
            users.addAll(getUsersByRoleId(portal, role.getId()));
        }
        return users;
    }

    @SuppressWarnings("unchecked")
    public List<User> searchUsers(String portalid, Map<String, Object> searchParameters) {
        Portal portal = entityManager.find(Portal.class, portalid);
        Set<String> parameterNames = searchParameters.keySet();
        Collection<Collection<User>> foundCollections = new ArrayList<Collection<User>>();
        StringBuffer jbossQL = new StringBuffer();
        jbossQL.append("select u from User u");
        Map<String, String> extraUserdataSearchFields = new HashMap<String, String>();
        for (ExtraUserdataField field : portal.getExtraUserdataFields()) if (parameterNames.contains(field.getName()) && ((String[]) searchParameters.get(field.getName()))[0].length() > 0) extraUserdataSearchFields.put(field.getName(), ((String[]) searchParameters.get(field.getName()))[0]);
        if (!extraUserdataSearchFields.isEmpty() || (parameterNames.contains("searchString") && !portal.getExtraUserdataFields().isEmpty())) jbossQL.append(" left join u.extraUserdata e");
        if (portal != null) jbossQL.append(" where u.portal = :portal");
        StringBuffer where = new StringBuffer();
        Calendar createdFrom = GregorianCalendar.getInstance();
        Calendar createdTo = GregorianCalendar.getInstance();
        Calendar lastLoginFrom = GregorianCalendar.getInstance();
        Calendar lastLoginTo = GregorianCalendar.getInstance();
        if (parameterNames.contains("searchString")) {
            String searchString = ((String[]) searchParameters.get("searchString"))[0].toLowerCase();
            if (searchString.length() > 0) {
                where.append(" and (lcase(u.userid) like '%" + searchString + "%' or lcase(u.email) like '%" + searchString + "%' or lcase(u.firstname) like '%" + searchString + "%' or lcase(u.lastname) like '%" + searchString + "%' or lcase(u.lastname2) like '%" + searchString + "%'");
                if (!extraUserdataSearchFields.isEmpty()) where.append("or lcase(e.value) like '%" + searchString + "%'");
                where.append(")");
            }
        }
        if (!extraUserdataSearchFields.isEmpty()) for (String fieldName : extraUserdataSearchFields.keySet()) where.append(" and (lcase(e.value) like '%" + extraUserdataSearchFields.get(fieldName) + "%' and e.field.name = '" + fieldName + "')");
        if (parameterNames.contains("created")) {
            where.append(" and (u.created >= :createdFrom and u.created <= :createdTo)");
            createdFrom.setTime((Date) searchParameters.get("created"));
            createdTo.setTime(createdFrom.getTime());
            createdTo.add(Calendar.DATE, 1);
        }
        if (parameterNames.contains("lastLogin")) {
            where.append(" and (u.lastLogin >= :lastLoginFrom and u.lastLogin <= :lastLoginTo)");
            lastLoginFrom.setTime((Date) searchParameters.get("lastLogin"));
            lastLoginTo.setTime(lastLoginFrom.getTime());
            lastLoginTo.add(Calendar.DATE, 1);
        }
        for (Map.Entry<String, Object> entry : searchParameters.entrySet()) {
            String fieldname = entry.getKey();
            if (fieldname.equals("userid") || fieldname.equals("email") || fieldname.equals("firstname") || fieldname.equals("lastname")) if (entry.getValue() != null && ((String[]) entry.getValue())[0].length() > 0) where.append(" and lcase(u." + fieldname + ") like '%" + ((String[]) entry.getValue())[0].toLowerCase() + "%'");
        }
        jbossQL.append(where.toString());
        log.info(jbossQL);
        Query query = entityManager.createQuery(jbossQL.toString());
        query.setParameter("portal", portal);
        if (parameterNames.contains("created")) {
            query.setParameter("createdFrom", createdFrom.getTime());
            query.setParameter("createdTo", createdTo.getTime());
        }
        if (parameterNames.contains("lastLogin")) {
            query.setParameter("lastLoginFrom", lastLoginFrom.getTime());
            query.setParameter("lastLoginTo", lastLoginTo.getTime());
        }
        foundCollections.add(query.getResultList());
        if (parameterNames.contains("roleId")) {
            Collection<Integer> roleIds = new ArrayList<Integer>();
            Object roleIdParameter = searchParameters.get("roleId");
            if (roleIdParameter instanceof Integer) roleIds.add((Integer) roleIdParameter); else if (roleIdParameter instanceof Collection) for (Integer roleId : (Collection<Integer>) roleIdParameter) {
                if (roleId != null) roleIds.add(roleId);
            } else if (roleIdParameter instanceof Integer[]) for (Integer roleId : (Integer[]) roleIdParameter) {
                if (roleId != null) roleIds.add(roleId);
            } else log.warn("Wrong user search parameter roleId of type " + roleIdParameter.getClass().getName());
            for (Integer roleId : roleIds) {
                Role role = entityManager.find(Role.class, roleId);
                if (!role.getPortal().equals(portal)) throw new IllegalArgumentException();
                foundCollections.add(role.getUsers());
            }
        }
        List<User> collection = new ArrayList<User>();
        boolean first = true;
        for (Collection<User> subcollection : foundCollections) if (first) {
            collection.addAll(subcollection);
            first = false;
        } else collection.retainAll(subcollection);
        return collection;
    }

    public String getUsersAsCSV(String portalid) {
        StringWriter usersAsCSV = new StringWriter();
        BufferedWriter writer = new BufferedWriter(usersAsCSV);
        TreeSet<ExtraUserdataField> fields = new TreeSet<ExtraUserdataField>();
        Portal portal = entityManager.find(Portal.class, portalid);
        fields.addAll(portal.getExtraUserdataFields());
        try {
            for (User user : portal.getUsers()) {
                writer.write(user.getUserid() + "," + user.getFirstname() + "," + user.getLastname() + "," + user.getLastname2() + "," + user.getEmail());
                Map<String, String> extrauserdata = new HashMap<String, String>();
                for (ExtraUserdata datum : user.getExtraUserdata()) extrauserdata.put(datum.getField().getName(), datum.getValue());
                for (ExtraUserdataField fieldSBean : fields) writer.write("," + (extrauserdata.containsKey(fieldSBean.getName()) ? extrauserdata.get(fieldSBean.getName()) : ""));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return usersAsCSV.toString();
    }

    @Deprecated
    public static String getDigitalSignature(String[] inputs, String content) {
        String[] newInputs = new String[inputs.length + 1];
        for (int i = 0; i < inputs.length; i++) newInputs[i] = inputs[i];
        newInputs[inputs.length] = content;
        return getMessageDigest(newInputs);
    }

    public static String getMessageDigest(String input) {
        if (input == null) {
            log.warn("Returning SHA-1 null value for null input");
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(input.getBytes("UTF-8"));
            byte[] bytes = md.digest();
            return new BASE64Encoder().encode(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static String getMessageDigest(String[] inputs) {
        if (inputs.length == 0) return null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            for (String input : inputs) sha.update(input.getBytes());
            byte[] hash = sha.digest();
            String CPass = "";
            int h = 0;
            String s = "";
            for (int i = 0; i < 20; i++) {
                h = hash[i];
                if (h < 0) h += 256;
                s = Integer.toHexString(h);
                if (s.length() < 2) CPass = CPass.concat("0");
                CPass = CPass.concat(s);
            }
            CPass = CPass.toUpperCase();
            return CPass;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Deprecated
    public Role getRole(String portalid, Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        if (!role.getPortal().getId().equals(portalid)) throw new IllegalArgumentException("My very very good friend. That role is not yours...");
        loadRoleLazyData(role);
        return role;
    }

    @Deprecated
    public Role getRole(String portalid, String name) {
        Query query = entityManager.createNamedQuery("Role.findByName");
        query.setParameter("portalid", portalid);
        query.setParameter("name", name);
        Role role = (Role) query.getSingleResult();
        if (role != null) loadRoleLazyData(role);
        return role;
    }

    public Role getRole(Portal portal, String name, boolean create) {
        try {
            Query query = entityManager.createNamedQuery("Role.findByName");
            query.setParameter("portalid", portal.getId());
            query.setParameter("name", name);
            Role role = (Role) query.getSingleResult();
            loadRoleLazyData(role);
            return role;
        } catch (NoResultException e) {
            if (!create) throw e;
            Role role = new Role(portal, name);
            entityManager.persist(role);
            return role;
        }
    }

    private void loadRoleLazyData(Role role) {
        List<String> services = new ArrayList<String>();
        List<String> adminServices = new ArrayList<String>();
        role.setServices(services.toArray(new String[services.size()]));
        role.setAdminServices(adminServices.toArray(new String[services.size()]));
    }

    public void removeUserPicture(User user) {
        user.setPicture(null);
        entityManager.merge(user);
    }

    public User addFavourite(User user, String userid) {
        user.getFavourites().add(getUser(user.getPortalid(), userid));
        entityManager.merge(user);
        User favourite = getUser(user.getPortalid(), userid);
        favourite.getFans().add(user);
        return favourite;
    }

    public User removeFavourite(User user, String userid) {
        user.getFavourites().remove(getUser(user.getPortalid(), userid));
        entityManager.merge(user);
        User favourite = getUser(user.getPortalid(), userid);
        favourite.getFans().remove(user);
        return favourite;
    }

    public User acceptFriendship(User user, String userid) {
        UserProperty property = null;
        for (UserProperty whichOne : user.getProperties()) if (whichOne.getName().equals(FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME)) property = whichOne;
        String newValue = "";
        for (String pendingRequestUserid : property.getValue().split(",")) if (!pendingRequestUserid.equals(userid)) newValue += ((newValue.length() > 0 ? "," : "") + pendingRequestUserid);
        if (newValue.length() > 0) {
            property.setValue(newValue);
            entityManager.merge(property);
        } else {
            user.getProperties().remove(property);
            entityManager.remove(entityManager.find(UserProperty.class, property.getId()));
        }
        user.getFriends().add(getUser(user.getPortalid(), userid));
        entityManager.merge(user);
        User newFriend = getUser(user.getPortalid(), userid);
        newFriend.getFriends().add(user);
        return newFriend;
    }

    public User destroyFriendship(User user, String userid) {
        user.getFriends().remove(getUser(user.getPortalid(), userid));
        entityManager.merge(user);
        User oldFriend = getUser(user.getPortalid(), userid);
        oldFriend.getFriends().remove(user);
        return oldFriend;
    }

    public void checkPortal(Portal portal) {
        getRole(portal, portal.getAdminRoleName(), true);
        getRole(portal, "user", true);
        if (getRoleUserCount(getRole(portal, portal.getAdminRoleName(), false)) == 0) {
            createAdminUsers(portal);
            log.warn("Portal " + portal.getId() + " had no admin user. Created.");
        }
        if (!exists(portal.getId(), UserManagerBean.guestUserid)) {
            User guestUser = new User(portal, UserManagerBean.guestUserid);
            entityManager.persist(guestUser);
            log.warn("Portal " + portal.getId() + " had no guest user. Created.");
            portal.getUsers().add(guestUser);
        }
        for (User user : getUsers(portal.getId())) if (user.getJaasUserName() == null) persist(user);
    }

    public void persist(FileContent picture) {
        if (picture.getId() == null) entityManager.persist(picture); else entityManager.merge(picture);
    }

    public List<User> getUsersByRoleId(Portal portal, Integer id) {
        Role role = entityManager.find(Role.class, id);
        if (!role.getPortal().equals(portal)) throw new IllegalArgumentException();
        return new ArrayList<User>(role.getUsers());
    }

    public long getRoleUserCount(Role role) {
        return (Long) (entityManager.createNamedQuery("Role.getUserCount").setParameter("id", role.getId()).getSingleResult());
    }

    public long getRoleActionCount(Role role) {
        return (Long) (entityManager.createNamedQuery("Role.getActionCount").setParameter("id", role.getId()).getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsersByRoles(Collection<Role> roles) {
        Set<User> users = new HashSet<User>();
        for (Role role : roles) users.addAll(entityManager.createNamedQuery("User.findByRole").setParameter("role", role).getResultList());
        return new ArrayList<User>(users);
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsersByRole(Role role) {
        return entityManager.createNamedQuery("User.findByRole").setParameter("role", role).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsers(Portal portal, int firstResult, int maxResults) {
        return entityManager.createNamedQuery("User.findByPortal").setParameter("portal", portal).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    public UserSession getFacebookUserSession(String portalid, String facebookUserID) {
        @SuppressWarnings("unchecked") List<User> resultList = entityManager.createNamedQuery("User.findByPortalidAndFacebookUserID").setParameter("portalid", portalid).setParameter("facebookUserID", facebookUserID).getResultList();
        if (resultList.size() == 1) {
            User user = resultList.get(0);
            UserSession usersession = new UserSession(user);
            user.setLastLogin(usersession.getStart());
            entityManager.merge(user);
            entityManager.persist(usersession);
            loadLazyProperties(user);
            loadPortalGuestUser(user.getPortal());
            log.info("Facebook Connect login for Facebook user " + facebookUserID + " as " + user.getUserid() + "@" + user.getPortal().getId());
            return usersession;
        }
        if (resultList.size() > 1) {
            String userids = "";
            for (User user : resultList) userids += user.getUserid();
            log.warn("Too many users for facebookUserID " + facebookUserID + ": " + userids);
        } else log.info("No user for facebookUserID " + facebookUserID);
        UserSession userSession = new UserSession(getUser(portalid, UserManagerBean.guestUserid));
        UserProperty userProperty = new UserProperty(userSession.getUser(), FACEBOOK_USERID_PROPERTY_NAME, facebookUserID);
        userSession.getUser().getProperties().add(userProperty);
        loadLazyProperties(userSession.getUser());
        loadPortalGuestUser(userSession.getUser().getPortal());
        return userSession;
    }

    protected void loadPortalGuestUser(Portal portal) {
        try {
            portal.setGuestUser(getUser(portal.getId(), UserManagerBean.guestUserid));
        } catch (NoResultException e) {
            if (log.isDebugEnabled()) log.debug(e.getMessage(), e); else log.warn(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void removeFacebookUsers(String facebookUserID) {
        for (User user : (List<User>) entityManager.createNamedQuery("User.findByFacebookUserID").setParameter("facebookUserID", facebookUserID).getResultList()) {
            UserProperty expurganda = user.removeProperty(FACEBOOK_USERID_PROPERTY_NAME);
            entityManager.remove(expurganda);
            entityManager.merge(user);
            log.info("Facebook user " + facebookUserID + " removed for user " + user.getUserid() + "@" + user.getPortal().getId());
        }
    }

    public void persist(UserProperty userProperty) {
        if (userProperty.getId() == null) entityManager.persist(userProperty); else entityManager.merge(userProperty);
    }

    public User getUserWithFacebookUserID(String portalid, String facebookUserID) {
        @SuppressWarnings("unchecked") List<User> resultList = entityManager.createNamedQuery("User.findByPortalidAndFacebookUserID").setParameter("portalid", portalid).setParameter("facebookUserID", facebookUserID).getResultList();
        if (resultList.size() == 1) {
            User user = resultList.get(0);
            return user;
        }
        if (resultList.size() > 1) log.warn(resultList.size() + " users with facebookUserID=" + facebookUserID + " in portal " + portalid);
        return null;
    }

    public void removeUserProperty(User user, String propertyName) {
        @SuppressWarnings("unchecked") List<UserProperty> result = entityManager.createNamedQuery("UserProperty.findByuserIdAndName").setParameter("userId", user.getId()).setParameter("name", propertyName).getResultList();
        if (result.size() == 1) {
            UserProperty property = result.get(0);
            UserProperty expurganda = null;
            for (UserProperty aux : user.getProperties()) if (property.getId().equals(aux.getId())) {
                expurganda = aux;
                break;
            }
            if (expurganda == null) log.warn("Could not remove property object from user"); else user.getProperties().remove(expurganda);
            entityManager.remove(property);
        } else if (result.size() > 1) log.error("Multiple properties named " + propertyName + " for user " + user.getUserid() + "@" + user.getPortalid()); else log.warn("User " + user.getUserid() + "@" + user.getPortalid() + " has no property named " + propertyName);
    }

    @Deprecated
    public Map<String, String> getExtraUserdataMap(User user) {
        return entityManager.find(User.class, user.getId()).getExtraUserdataMap();
    }

    public User requestFriendship(User user, String userid) {
        UserProperty property = null;
        try {
            property = (UserProperty) entityManager.createNamedQuery("UserProperty.findByPortalUseridAndName").setParameter("portal", user.getPortal()).setParameter("userid", userid).setParameter("name", FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME).getSingleResult();
            for (String requestUserid : property.getValue().split(",")) if (requestUserid.equals(user.getUserid())) throw new IllegalArgumentException("friendshipAlreadyRequested");
            property.setValue(property.getValue() + "," + user.getUserid());
            entityManager.merge(property);
        } catch (NoResultException e) {
            property = new UserProperty(getUser(user.getPortalid(), userid), FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME, user.getUserid());
            entityManager.persist(property);
            property.getUser().getProperties().add(property);
        }
        return property.getUser();
    }

    public Collection<User> getFriendshipRequestUsers(User user) {
        Collection<User> users = new ArrayList<User>();
        if (user.getPropertyValue(FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME) != null) for (String userid : user.getPropertyValue(FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME).split(",")) try {
            users.add(getUser(user.getPortalid(), userid));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return users;
    }

    public User discardFriendship(User user, String userid) {
        UserProperty property = null;
        for (UserProperty whichOne : user.getProperties()) if (whichOne.getName().equals(FRIENDSHIP_REQUEST_USERIDS_PROPERTYNAME)) property = whichOne;
        String newValue = "";
        for (String pendingRequestUserid : property.getValue().split(",")) if (!pendingRequestUserid.equals(userid)) newValue += ((newValue.length() > 0 ? "," : "") + pendingRequestUserid);
        if (newValue.length() > 0) {
            property.setValue(newValue);
            entityManager.merge(property);
        } else {
            user.getProperties().remove(property);
            entityManager.remove(property);
        }
        return getUser(user.getPortalid(), userid);
    }

    public User getUserByEmail(String portalid, String email) {
        Query query = entityManager.createNamedQuery("User.findByportalidAndEmail");
        query.setParameter("portalid", portalid);
        query.setParameter("email", email);
        User user = (User) query.getSingleResult();
        loadLazyProperties(user);
        return user;
    }

    public boolean validateSignupCode(String code) {
        String prefix = code.substring(0, code.length() / 2);
        return code.substring(code.length() / 2).equals(getMessageDigest(prefix).substring(0, code.length() / 2));
    }

    @SuppressWarnings("unchecked")
    public Set<User> getUsersWithProperty(String propertyName, String propertyValue) {
        Set<User> users = new HashSet<User>();
        Query query = entityManager.createNamedQuery("UserProperty.findByNameAndValue");
        query.setParameter("name", propertyName);
        query.setParameter("value", propertyValue);
        for (UserProperty property : (List<UserProperty>) query.getResultList()) users.add(property.getUser());
        return users;
    }

    public boolean isSignupCodeUsed(String code) {
        return getUsersWithProperty(SIGNUP_CODE_PROPERTY_NAME, code).size() != 0;
    }

    public String getSignupCode(String prefix) {
        return getMessageDigest(prefix).substring(0, prefix.length());
    }

    public List<String> getSignupCodes(int generateSignupCodesFrom, int generateSignupCodesTo) {
        DecimalFormat decimalFormat = new DecimalFormat("0");
        decimalFormat.setMinimumIntegerDigits(("" + generateSignupCodesTo).length());
        decimalFormat.setMaximumFractionDigits(0);
        List<String> codes = new ArrayList<String>();
        for (int i = generateSignupCodesFrom; i < generateSignupCodesTo; i++) codes.add(decimalFormat.format(i) + getSignupCode(decimalFormat.format(i)));
        return codes;
    }

    /**
	 * This method is needed because roles is lazy
	 * @param user
	 * @return
	 */
    public Set<Role> getRolesByUser(User user) {
        return new HashSet<Role>(entityManager.find(User.class, user.getId()).getRoles());
    }

    public User createGuestUser(Portal portal) {
        User user = new User(portal, guestUserid);
        entityManager.persist(user);
        return user;
    }

    public User getGuestUser(Portal portal) {
        User guestUser = (User) entityManager.createNamedQuery("User.findByUserid").setParameter("portalid", portal.getId()).setParameter("userid", guestUserid).getSingleResult();
        loadLazyProperties(guestUser);
        return guestUser;
    }

    @Override
    public List<String> getExtraUserdataValueList(User user, List<ExtraUserdataField> fields) {
        List<String> values = new ArrayList<String>();
        Map<String, String> valueMap = user.getExtraUserdataMap();
        for (ExtraUserdataField field : fields) if (valueMap.containsKey(field.getName())) values.add(valueMap.get(field.getName())); else values.add(null);
        return values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> findUseridsStartingWith(Portal portal, String userid) {
        return entityManager.createNamedQuery("User.findUseridsStartingWith").setParameter("portal", portal).setParameter("useridLike", userid + "%").getResultList();
    }
}
