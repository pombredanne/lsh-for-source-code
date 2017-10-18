package org.opencms.db.jpa;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.db.CmsDbContext;
import org.opencms.db.CmsDbEntryAlreadyExistsException;
import org.opencms.db.CmsDbEntryNotFoundException;
import org.opencms.db.CmsDbIoException;
import org.opencms.db.CmsDriverManager;
import org.opencms.db.CmsUserSettings;
import org.opencms.db.I_CmsDriver;
import org.opencms.db.I_CmsProjectDriver;
import org.opencms.db.I_CmsUserDriver;
import org.opencms.db.jpa.CmsSqlManager;
import org.opencms.db.jpa.persistence.CmsDAOGroups;
import org.opencms.db.jpa.persistence.CmsDAOGroupUsers;
import org.opencms.db.jpa.persistence.CmsDAOOfflineAccessControl;
import org.opencms.db.jpa.persistence.CmsDAOOnlineAccessControl;
import org.opencms.db.jpa.persistence.I_CmsDAOAccessControl;
import org.opencms.db.jpa.persistence.CmsDAOUserData;
import org.opencms.db.jpa.persistence.CmsDAOUsers;
import org.opencms.db.jpa.persistence.CmsDAOGroupUsers.CmsDAOGroupUsersPK;
import org.opencms.file.CmsDataAccessException;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsInitException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.relations.CmsRelationType;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsOrganizationalUnit;
import org.opencms.security.CmsPasswordEncryptionException;
import org.opencms.security.CmsRole;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsDataTypeUtil;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

/**
 * @author gosho
 *
 */
public class CmsUserDriver implements I_CmsDriver, I_CmsUserDriver {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(org.opencms.db.jpa.CmsUserDriver.class);

    /** The name of the offline project. */
    private static final String OFFLINE_PROJECT_NAME = "Offline";

    /** The root path for organizational units. */
    public static final String ORGUNIT_BASE_FOLDER = "/system/orgunits/";

    /** Property for the organizational unit description. */
    private static final String ORGUNIT_PROPERTY_DESCRIPTION = CmsPropertyDefinition.PROPERTY_DESCRIPTION;

    /** Property for the organizational unit default project id. */
    private static final String ORGUNIT_PROPERTY_PROJECTID = CmsPropertyDefinition.PROPERTY_KEYWORDS;

    /** A digest to encrypt the passwords. */
    protected MessageDigest m_digest;

    /** The algorithm used to encode passwords. */
    protected String m_digestAlgorithm;

    /** The file.encoding to code passwords after encryption with digest. */
    protected String m_digestFileEncoding;

    /** The driver manager. */
    protected CmsDriverManager m_driverManager;

    /** The SQL manager. */
    protected CmsSqlManager m_sqlManager;

    private static final String C_GROUPS_DELETE_GROUP_2 = "C_GROUPS_DELETE_GROUP_2";

    private static final String C_USERS_DELETE_2 = "C_USERS_DELETE_2";

    private static final String C_GROUPS_CHECK_BY_NAME_2 = "C_GROUPS_CHECK_BY_NAME_2";

    private static final String C_USERS_CHECK_BY_NAME_2 = "C_USERS_CHECK_BY_NAME_2";

    private static final String C_GROUPS_GET_GROUPS_0 = "C_GROUPS_GET_GROUPS_0";

    private static final String C_USERS_GET_WEBUSERS_FOR_ORGUNIT_1 = "C_USERS_GET_WEBUSERS_FOR_ORGUNIT_1";

    private static final String C_USERS_GET_USERS_FOR_ORGUNIT_1 = "C_USERS_GET_USERS_FOR_ORGUNIT_1";

    private static final String C_ACCESS_READ_ENTRIES_1 = "C_ACCESS_READ_ENTRIES_1";

    private static final String C_ACCESS_READ_ENTRY_2 = "C_ACCESS_READ_ENTRY_2";

    private static final String C_GROUPS_GET_CHILD_1 = "C_GROUPS_GET_CHILD_1";

    private static final String C_GROUPS_READ_BY_NAME_2 = "C_GROUPS_READ_BY_NAME_2";

    private static final String C_GROUPS_GET_GROUPS_OF_USER_1 = "C_GROUPS_GET_GROUPS_OF_USER_1";

    private static final String C_USERS_READ_BY_NAME_2 = "C_USERS_READ_BY_NAME_2";

    private static final String C_USERS_READ_WITH_PWD_3 = "C_USERS_READ_WITH_PWD_3";

    private static final String C_USERDATA_READ_1 = "C_USERDATA_READ_1";

    private static final String C_GROUPS_GET_USERS_OF_GROUP_2 = "C_GROUPS_GET_USERS_OF_GROUP_2";

    private static final String C_GROUPS_GET_ALL_USERS_OF_GROUP_2 = "C_GROUPS_GET_ALL_USERS_OF_GROUP_2";

    private static final String C_ACCESS_REMOVE_ALL_1 = "C_ACCESS_REMOVE_ALL_1";

    private static final String C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_1 = "C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_1";

    private static final String C_ACCESS_REMOVE_2 = "C_ACCESS_REMOVE_2";

    private static final String C_USERS_SET_ORGUNIT_2 = "C_USERS_SET_ORGUNIT_2";

    private static final String C_ACCESS_UPDATE_5 = "C_ACCESS_UPDATE_5";

    private static final String C_GROUPS_WRITE_GROUP_4 = "C_GROUPS_WRITE_GROUP_4";

    private static final String C_USERS_SET_PWD_3 = "C_USERS_SET_PWD_3";

    private static final String C_USERS_WRITE_6 = "C_USERS_WRITE_6";

    private static final String C_GROUPS_GROUP_OU_EQUALS_1 = "C_GROUPS_GROUP_OU_EQUALS_1";

    private static final String C_GROUPS_GROUP_OU_LIKE_1 = "C_GROUPS_GROUP_OU_LIKE_1";

    private static final String C_GROUPS_SELECT_GROUPS_1 = "C_GROUPS_SELECT_GROUPS_1";

    private static final String C_GROUPS_SELECT_ROLES_1 = "C_GROUPS_SELECT_ROLES_1";

    private static final String C_GROUPS_ORDER_0 = "C_GROUPS_ORDER_0";

    private static final String C_GROUPS_USER_IN_GROUP_2 = "C_GROUPS_USER_IN_GROUP_2";

    /**
     * @see org.opencms.db.I_CmsUserDriver#addResourceToOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, org.opencms.file.CmsResource)
     */
    public void addResourceToOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, CmsResource resource) throws CmsDataAccessException {
        try {
            if (resource.isFile()) {
                throw new CmsDataAccessException(Messages.get().container(Messages.ERR_ORGUNIT_RESOURCE_IS_NOT_FOLDER_2, orgUnit.getName(), dbc.removeSiteRoot(resource.getRootPath())));
            }
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
            List vfsPaths = new ArrayList(internalResourcesForOrgUnit(dbc, ouResource));
            if (orgUnit.getParentFqn() != null) {
                CmsOrganizationalUnit parentOu = m_driverManager.readOrganizationalUnit(dbc, orgUnit.getParentFqn());
                internalValidateResourceForOrgUnit(dbc, parentOu, resource.getRootPath());
            } else {
                if (!vfsPaths.isEmpty()) {
                    throw new CmsDataAccessException(org.opencms.security.Messages.get().container(org.opencms.security.Messages.ERR_ORGUNIT_ROOT_EDITION_0));
                }
            }
            CmsRelation relation = new CmsRelation(ouResource, resource, CmsRelationType.OU_RESOURCE);
            m_driverManager.getVfsDriver().createRelation(dbc, dbc.currentProject().getUuid(), relation);
            m_driverManager.getVfsDriver().createRelation(dbc, CmsProject.ONLINE_PROJECT_ID, relation);
            try {
                CmsProject project = m_driverManager.readProject(dbc, orgUnit.getProjectId());
                m_driverManager.getProjectDriver().createProjectResource(dbc, orgUnit.getProjectId(), resource.getRootPath());
                OpenCms.fireCmsEvent(I_CmsEventListener.EVENT_PROJECT_MODIFIED, Collections.singletonMap("project", project));
            } catch (CmsDbEntryNotFoundException e) {
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID, int, int, int)
     */
    public void createAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsUUID resource, CmsUUID principal, int allowed, int denied, int flags) throws CmsDataAccessException {
        try {
            I_CmsDAOAccessControl pac = CmsProject.isOnlineProject(project.getUuid()) ? new CmsDAOOnlineAccessControl() : new CmsDAOOfflineAccessControl();
            pac.setResourceId(resource.toString());
            pac.setPrincipalId(principal.toString());
            pac.setAccessAllowed(allowed);
            pac.setAccessDenied(denied);
            pac.setAccessFlags(flags);
            dbc.getEntityManager().persist(pac);
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, java.lang.String, java.lang.String, int, java.lang.String)
     */
    public CmsGroup createGroup(CmsDbContext dbc, CmsUUID groupId, String groupFqn, String description, int flags, String parentGroupFqn) throws CmsDataAccessException {
        CmsUUID parentId = CmsUUID.getNullUUID();
        CmsGroup group = null;
        if (existsGroup(dbc, groupFqn)) {
            CmsMessageContainer message = Messages.get().container(Messages.ERR_GROUP_WITH_NAME_ALREADY_EXISTS_1, groupFqn);
            if (LOG.isErrorEnabled()) {
                LOG.error(message.key());
            }
            throw new CmsDbEntryAlreadyExistsException(message);
        }
        try {
            if (CmsStringUtil.isNotEmpty(parentGroupFqn)) {
                parentId = readGroup(dbc, parentGroupFqn).getId();
            }
            CmsDAOGroups g = new CmsDAOGroups();
            g.setGroupId(groupId.toString());
            g.setParentGroupId(parentId.toString());
            g.setGroupName(CmsOrganizationalUnit.getSimpleName(groupFqn));
            g.setGroupDescription(m_sqlManager.validateEmpty(description));
            g.setGroupFlags(flags);
            g.setGroupOu(CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(groupFqn));
            dbc.getEntityManager().persist(g);
            group = new CmsGroup(groupId, parentId, groupFqn, description, flags);
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createOrganizationalUnit(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, int, org.opencms.security.CmsOrganizationalUnit, String)
     */
    public CmsOrganizationalUnit createOrganizationalUnit(CmsDbContext dbc, String name, String description, int flags, CmsOrganizationalUnit parent, String associatedResource) throws CmsDataAccessException {
        if ((parent == null) && !name.equals("")) {
            throw new CmsDataAccessException(org.opencms.db.Messages.get().container(org.opencms.db.Messages.ERR_PARENT_ORGUNIT_NULL_0));
        }
        try {
            CmsResource parentFolder = internalOrgUnitFolder(dbc, parent);
            CmsResource resource = null;
            if (((flags & CmsOrganizationalUnit.FLAG_WEBUSERS) == 0) || (associatedResource != null)) {
                resource = m_driverManager.readFolder(dbc, associatedResource, CmsResourceFilter.ALL);
            }
            String ouPath = ORGUNIT_BASE_FOLDER;
            if ((parentFolder != null) && (resource != null)) {
                internalValidateResourceForOrgUnit(dbc, internalCreateOrgUnitFromResource(dbc, parentFolder), resource.getRootPath());
            }
            if (parentFolder != null) {
                ouPath = parentFolder.getRootPath();
                if (!ouPath.endsWith("/")) {
                    ouPath += "/";
                }
            }
            CmsResource ouFolder = internalCreateResourceForOrgUnit(dbc, ouPath + name, flags);
            internalWriteOrgUnitProperty(dbc, ouFolder, new CmsProperty(ORGUNIT_PROPERTY_DESCRIPTION, description, null));
            CmsOrganizationalUnit ou = internalCreateOrgUnitFromResource(dbc, ouFolder);
            if ((ou.getParentFqn() != null)) {
                Locale locale = dbc.getRequestContext().getLocale();
                if (locale == null) {
                    locale = CmsLocaleManager.getDefaultLocale();
                }
                internalCreateDefaultGroups(dbc, ou.getName(), ou.getDisplayName(locale), ou.hasFlagWebuser());
                if (!ou.hasFlagWebuser()) {
                    CmsProject project = m_driverManager.createProject(dbc, ou.getName() + OFFLINE_PROJECT_NAME, "", ou.getName() + OpenCms.getDefaultUsers().getGroupUsers(), ou.getName() + OpenCms.getDefaultUsers().getGroupUsers(), CmsProject.PROJECT_TYPE_NORMAL);
                    internalWriteOrgUnitProperty(dbc, ouFolder, new CmsProperty(ORGUNIT_PROPERTY_PROJECTID, project.getUuid().toString(), null));
                } else {
                    internalWriteOrgUnitProperty(dbc, ouFolder, new CmsProperty(ORGUNIT_PROPERTY_PROJECTID, CmsUUID.getNullUUID().toString(), null));
                }
            } else {
                internalWriteOrgUnitProperty(dbc, ouFolder, new CmsProperty(ORGUNIT_PROPERTY_PROJECTID, CmsUUID.getNullUUID().toString(), null));
            }
            ou = internalCreateOrgUnitFromResource(dbc, ouFolder);
            if (resource != null) {
                m_driverManager.addResourceToOrgUnit(dbc, ou, resource);
            }
            OpenCms.fireCmsEvent(I_CmsEventListener.EVENT_CLEAR_ONLINE_CACHES, null);
            return ou;
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createRootOrganizationalUnit(org.opencms.db.CmsDbContext)
     */
    public void createRootOrganizationalUnit(CmsDbContext dbc) {
        try {
            readOrganizationalUnit(dbc, "");
        } catch (CmsException e) {
            try {
                CmsProject onlineProject = dbc.currentProject();
                CmsProject setupProject = onlineProject;
                try {
                    setupProject = m_driverManager.readProject(dbc, I_CmsProjectDriver.SETUP_PROJECT_NAME);
                } catch (CmsException exc) {
                    try {
                        setupProject = m_driverManager.readProject(dbc, "Offline");
                    } catch (CmsException exc2) {
                    }
                }
                dbc.getRequestContext().setCurrentProject(setupProject);
                try {
                    createOrganizationalUnit(dbc, "", CmsMacroResolver.localizedKeyMacro(Messages.GUI_ORGUNIT_ROOT_DESCRIPTION_0, null), 0, null, "/");
                } finally {
                    dbc.getRequestContext().setCurrentProject(onlineProject);
                }
                if (CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ROOT_ORGUNIT_DEFAULTS_INITIALIZED_0));
                }
            } catch (CmsException exc) {
                if (CmsLog.INIT.isErrorEnabled()) {
                    CmsLog.INIT.error(Messages.get().getBundle().key(Messages.INIT_ROOT_ORGUNIT_INITIALIZATION_FAILED_0), exc);
                }
                throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), exc);
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createUser(CmsDbContext, CmsUUID, String, String, String, String, String, long, int, long, Map)
     */
    public CmsUser createUser(CmsDbContext dbc, CmsUUID id, String userFqn, String password, String firstname, String lastname, String email, long lastlogin, int flags, long dateCreated, Map additionalInfos) throws CmsDataAccessException {
        if (existsUser(dbc, userFqn)) {
            CmsMessageContainer message = Messages.get().container(Messages.ERR_USER_WITH_NAME_ALREADY_EXISTS_1, userFqn);
            if (LOG.isErrorEnabled()) {
                LOG.error(message.key());
            }
            throw new CmsDbEntryAlreadyExistsException(message);
        }
        try {
            CmsDAOUsers u = new CmsDAOUsers();
            u.setUserId(id.toString());
            u.setUserName(CmsOrganizationalUnit.getSimpleName(userFqn));
            u.setUserPassword(password);
            u.setUserFirstName(m_sqlManager.validateEmpty(firstname));
            u.setUserLastName(m_sqlManager.validateEmpty(lastname));
            u.setUserEmail(m_sqlManager.validateEmpty(email));
            u.setUserLastLogin(lastlogin);
            u.setUserFlags(flags);
            u.setUserOu(CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            u.setUserDateCreated((dateCreated == 0 ? System.currentTimeMillis() : dateCreated));
            dbc.getEntityManager().persist(u);
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        internalWriteUserInfos(dbc, id, additionalInfos);
        return readUser(dbc, id);
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createUserInGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void createUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId) throws CmsDataAccessException {
        if (!internalValidateUserInGroup(dbc, userId, groupId)) {
            try {
                CmsDAOGroupUsers gu = new CmsDAOGroupUsers();
                gu.setGroupId(groupId.toString());
                gu.setUserId(userId.toString());
                gu.setGroupUserFlags(0);
                dbc.getEntityManager().persist(gu);
            } catch (PersistenceException e) {
                throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     * 
     * @deprecated use {@link #removeAccessControlEntries(CmsDbContext, CmsProject, CmsUUID)} instead
     */
    public void deleteAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource) throws CmsDataAccessException {
        removeAccessControlEntries(dbc, project, resource);
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public void deleteGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, C_GROUPS_DELETE_GROUP_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(groupFqn));
            List<CmsDAOGroups> res = q.getResultList();
            for (CmsDAOGroups g : res) {
                dbc.getEntityManager().remove(g);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public void deleteOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit organizationalUnit) throws CmsDataAccessException {
        try {
            CmsResource resource = m_driverManager.readResource(dbc, organizationalUnit.getId(), CmsResourceFilter.DEFAULT);
            internalDeleteOrgUnitResource(dbc, resource);
            if (organizationalUnit.getProjectId() != null) {
                try {
                    m_driverManager.deleteProject(dbc, m_driverManager.readProject(dbc, organizationalUnit.getProjectId()));
                } catch (CmsDbEntryNotFoundException e) {
                }
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public void deleteUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {
        CmsUser user = readUser(dbc, userFqn);
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_DELETE_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                dbc.getEntityManager().remove(u);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        deleteUserInfos(dbc, user.getId());
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteUserInfos(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public void deleteUserInfos(CmsDbContext dbc, CmsUUID userId) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERDATA_READ_1);
            q.setParameter(1, userId.toString());
            List<CmsDAOUserData> res = q.getResultList();
            EntityManager em = dbc.getEntityManager();
            for (CmsDAOUserData ud : res) {
                em.remove(ud);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteUserInGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void deleteUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId) throws CmsDataAccessException {
        try {
            EntityManager em = dbc.getEntityManager();
            CmsDAOGroupUsersPK pk = new CmsDAOGroupUsersPK();
            pk.m_groupId = groupId.toString();
            pk.m_userId = userId.toString();
            CmsDAOGroupUsers gu = em.find(CmsDAOGroupUsers.class, pk);
            if (gu != null) {
                em.remove(gu);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#destroy()
     */
    public void destroy() throws Throwable {
        m_sqlManager = null;
        m_driverManager = null;
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SHUTDOWN_DRIVER_1, getClass().getName()));
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#existsGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public boolean existsGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {
        boolean result = false;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_GROUPS_CHECK_BY_NAME_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(groupFqn));
            int count = CmsDataTypeUtil.numberToInt((Number) q.getSingleResult());
            if (count > 0) {
                result = true;
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#existsUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public boolean existsUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {
        boolean result = false;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_CHECK_BY_NAME_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            int count = CmsDataTypeUtil.numberToInt((Number) q.getSingleResult());
            if (count > 0) {
                result = true;
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#fillDefaults(org.opencms.db.CmsDbContext)
     */
    public void fillDefaults(CmsDbContext dbc) throws CmsInitException {
        try {
            internalCreateDefaultGroups(dbc, "", "", false);
        } catch (CmsException e) {
            if (CmsLog.INIT.isErrorEnabled()) {
                CmsLog.INIT.error(Messages.get().getBundle().key(Messages.INIT_DEFAULT_USERS_CREATION_FAILED_0), e);
            }
            throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getGroups(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean, boolean)
     */
    public List getGroups(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, boolean includeSubOus, boolean readRoles) throws CmsDataAccessException {
        String sqlQuery = createRoleQuery(C_GROUPS_GET_GROUPS_0, includeSubOus, readRoles);
        String ouFqn = CmsOrganizationalUnit.SEPARATOR + orgUnit.getName();
        if (includeSubOus) {
            ouFqn += "%";
        }
        List groups = new ArrayList();
        try {
            Query q = m_sqlManager.createQueryFromJPQL(dbc, sqlQuery);
            q.setParameter(1, ouFqn);
            q.setParameter(2, I_CmsPrincipal.FLAG_GROUP_ROLE);
            List<CmsDAOGroups> res = q.getResultList();
            for (CmsDAOGroups g : res) {
                groups.add(internalCreateGroup(g));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return groups;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getOrganizationalUnits(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean)
     */
    public List getOrganizationalUnits(CmsDbContext dbc, CmsOrganizationalUnit parent, boolean includeChildren) throws CmsDataAccessException {
        List orgUnits = new ArrayList();
        try {
            CmsResource parentFolder = internalOrgUnitFolder(dbc, parent);
            Iterator itResources = m_driverManager.readResources(dbc, parentFolder, CmsResourceFilter.DEFAULT, includeChildren).iterator();
            while (itResources.hasNext()) {
                CmsResource resource = (CmsResource) itResources.next();
                orgUnits.add(internalCreateOrgUnitFromResource(dbc, resource));
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
        return orgUnits;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getResourcesForOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public List getResourcesForOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit) throws CmsDataAccessException {
        List result = new ArrayList();
        try {
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
            Iterator itPaths = internalResourcesForOrgUnit(dbc, ouResource).iterator();
            while (itPaths.hasNext()) {
                String path = (String) itPaths.next();
                try {
                    result.add(m_driverManager.readResource(dbc, path, CmsResourceFilter.ALL));
                } catch (CmsVfsResourceNotFoundException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getSqlManager()
     */
    public CmsSqlManager getSqlManager() {
        return m_sqlManager;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getUsers(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean)
     */
    public List getUsers(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, boolean recursive) throws CmsDataAccessException {
        List users = new ArrayList();
        try {
            Query q;
            if (orgUnit.hasFlagWebuser()) {
                q = m_sqlManager.createQuery(dbc, C_USERS_GET_WEBUSERS_FOR_ORGUNIT_1);
            } else {
                q = m_sqlManager.createQuery(dbc, C_USERS_GET_USERS_FOR_ORGUNIT_1);
            }
            String param = CmsOrganizationalUnit.SEPARATOR + orgUnit.getName();
            if (recursive) {
                param += "%";
            }
            q.setParameter(1, param);
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                users.add(internalCreateUser(dbc, u));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return users;
    }

    /**
     * @see org.opencms.db.I_CmsDriver#init(org.opencms.db.CmsDbContext, org.opencms.configuration.CmsConfigurationManager, java.util.List, org.opencms.db.CmsDriverManager)
     */
    public void init(CmsDbContext dbc, CmsConfigurationManager configurationManager, List successiveDrivers, CmsDriverManager driverManager) {
        Map configuration = configurationManager.getConfiguration();
        ExtendedProperties config;
        if (configuration instanceof ExtendedProperties) {
            config = (ExtendedProperties) configuration;
        } else {
            config = new ExtendedProperties();
            config.putAll(configuration);
        }
        String poolUrl = config.get("db.user.pool").toString();
        String classname = config.get("db.user.sqlmanager").toString();
        m_sqlManager = (CmsSqlManager) this.initSqlManager(classname);
        m_driverManager = driverManager;
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ASSIGNED_POOL_1, poolUrl));
        }
        m_digestAlgorithm = config.getString(CmsDriverManager.CONFIGURATION_DB + ".user.digest.type", "MD5");
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DIGEST_ALGORITHM_1, m_digestAlgorithm));
        }
        m_digestFileEncoding = config.getString(CmsDriverManager.CONFIGURATION_DB + ".user.digest.encoding", CmsEncoder.ENCODING_UTF_8);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DIGEST_ENCODING_1, m_digestFileEncoding));
        }
        try {
            m_digest = MessageDigest.getInstance(m_digestAlgorithm);
            if (CmsLog.INIT.isInfoEnabled()) {
                CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DIGEST_ENC_3, m_digest.getAlgorithm(), m_digest.getProvider().getName(), String.valueOf(m_digest.getProvider().getVersion())));
            }
        } catch (NoSuchAlgorithmException e) {
            if (CmsLog.INIT.isInfoEnabled()) {
                CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SET_DIGEST_ERROR_0), e);
            }
        }
        if ((successiveDrivers != null) && !successiveDrivers.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().getBundle().key(Messages.LOG_SUCCESSIVE_DRIVERS_UNSUPPORTED_1, getClass().getName()));
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#initSqlManager(String)
     */
    public org.opencms.db.CmsSqlManager initSqlManager(String classname) {
        return CmsSqlManager.getInstance(classname);
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#publishAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void publishAccessControlEntries(CmsDbContext dbc, CmsProject offlineProject, CmsProject onlineProject, CmsUUID offlineId, CmsUUID onlineId) throws CmsDataAccessException {
        m_driverManager.getUserDriver().removeAccessControlEntries(dbc, onlineProject, onlineId);
        try {
            CmsUUID dbcProjectId = dbc.getProjectId();
            dbc.setProjectId(CmsUUID.getNullUUID());
            Query q = m_sqlManager.createQuery(dbc, offlineProject, C_ACCESS_READ_ENTRIES_1);
            q.setParameter(1, offlineId.toString());
            dbc.setProjectId(dbcProjectId);
            List<I_CmsDAOAccessControl> res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                CmsAccessControlEntry ace = internalCreateAce(ac, onlineId);
                m_driverManager.getUserDriver().writeAccessControlEntry(dbc, onlineProject, ace);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, boolean)
     */
    public List readAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource, boolean inheritedOnly) throws CmsDataAccessException {
        List aceList = new ArrayList();
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_READ_ENTRIES_1);
            String resId = resource.toString();
            q.setParameter(1, resId);
            List<I_CmsDAOAccessControl> res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                CmsAccessControlEntry ace = internalCreateAce(ac);
                if (inheritedOnly && !ace.isInheriting()) {
                    continue;
                }
                if (inheritedOnly && ace.isInheriting()) {
                    ace.setFlags(CmsAccessControlEntry.ACCESS_FLAGS_INHERITED);
                }
                aceList.add(ace);
            }
            return aceList;
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public CmsAccessControlEntry readAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsDataAccessException {
        CmsAccessControlEntry ace = null;
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_READ_ENTRY_2);
            q.setParameter(1, resource.toString());
            q.setParameter(2, principal.toString());
            try {
                I_CmsDAOAccessControl ac = (I_CmsDAOAccessControl) q.getSingleResult();
                ace = internalCreateAce(ac);
            } catch (NoResultException e) {
                throw new CmsDbEntryNotFoundException(Messages.get().container(Messages.ERR_NO_ACE_FOUND_2, resource, principal));
            }
            return ace;
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readChildGroups(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public List readChildGroups(CmsDbContext dbc, String parentGroupFqn) throws CmsDataAccessException {
        List children = new ArrayList();
        try {
            CmsGroup parent = m_driverManager.getUserDriver().readGroup(dbc, parentGroupFqn);
            if (parent != null) {
                Query q = m_sqlManager.createQuery(dbc, C_GROUPS_GET_CHILD_1);
                q.setParameter(1, parent.getId().toString());
                List<CmsDAOGroups> res = q.getResultList();
                for (CmsDAOGroups g : res) {
                    children.add(internalCreateGroup(g));
                }
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return children;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public CmsGroup readGroup(CmsDbContext dbc, CmsUUID groupId) throws CmsDataAccessException {
        CmsGroup group = null;
        try {
            CmsDAOGroups g = dbc.getEntityManager().find(CmsDAOGroups.class, groupId.toString());
            if (g != null) {
                group = internalCreateGroup(g);
            } else {
                CmsMessageContainer message = Messages.get().container(Messages.ERR_NO_GROUP_WITH_ID_1, groupId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public CmsGroup readGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {
        CmsGroup group = null;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_GROUPS_READ_BY_NAME_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(groupFqn));
            try {
                CmsDAOGroups g = (CmsDAOGroups) q.getSingleResult();
                group = internalCreateGroup(g);
            } catch (NoResultException e) {
                CmsMessageContainer message = org.opencms.db.Messages.get().container(org.opencms.db.Messages.ERR_UNKNOWN_GROUP_1, groupFqn);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key(), new Exception());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroupsOfUser(CmsDbContext, CmsUUID, String, boolean, String, boolean)
     */
    public List readGroupsOfUser(CmsDbContext dbc, CmsUUID userId, String ouFqn, boolean includeChildOus, String remoteAddress, boolean readRoles) throws CmsDataAccessException {
        String sqlQuery = createRoleQuery(C_GROUPS_GET_GROUPS_OF_USER_1, includeChildOus, readRoles);
        String ouFqnParam = CmsOrganizationalUnit.SEPARATOR + ouFqn;
        if (includeChildOus) {
            ouFqnParam += "%";
        }
        List groups = new ArrayList();
        try {
            Query q = m_sqlManager.createQueryFromJPQL(dbc, sqlQuery);
            q.setParameter(1, userId.toString());
            q.setParameter(2, ouFqnParam);
            q.setParameter(3, I_CmsPrincipal.FLAG_GROUP_ROLE);
            List<Object[]> res = q.getResultList();
            for (Object[] o : res) {
                groups.add(internalCreateGroup(((CmsDAOGroups) o[0])));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return groups;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readOrganizationalUnit(org.opencms.db.CmsDbContext, String)
     */
    public CmsOrganizationalUnit readOrganizationalUnit(CmsDbContext dbc, String ouFqn) throws CmsDataAccessException {
        try {
            CmsResource resource = m_driverManager.readResource(dbc, ORGUNIT_BASE_FOLDER + ouFqn, CmsResourceFilter.DEFAULT);
            return internalCreateOrgUnitFromResource(dbc, resource);
        } catch (CmsVfsResourceNotFoundException e) {
            throw new CmsDbEntryNotFoundException(Messages.get().container(Messages.ERR_READ_ORGUNIT_1, ouFqn), e);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public CmsUser readUser(CmsDbContext dbc, CmsUUID id) throws CmsDataAccessException {
        CmsUser user = null;
        try {
            CmsDAOUsers u = dbc.getEntityManager().find(CmsDAOUsers.class, id.toString());
            if (u != null) {
                user = internalCreateUser(dbc, u);
            } else {
                CmsMessageContainer message = Messages.get().container(Messages.ERR_NO_USER_WITH_ID_1, id);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
            return user;
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public CmsUser readUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {
        CmsUser user = null;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_READ_BY_NAME_2);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            try {
                CmsDAOUsers u = (CmsDAOUsers) q.getSingleResult();
                user = internalCreateUser(dbc, u);
            } catch (NoResultException e) {
                CmsMessageContainer message = org.opencms.db.Messages.get().container(org.opencms.db.Messages.ERR_UNKNOWN_USER_1, userFqn);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return user;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, String)
     */
    public CmsUser readUser(CmsDbContext dbc, String userFqn, String password, String remoteAddress) throws CmsDataAccessException, CmsPasswordEncryptionException {
        CmsUser user = null;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_READ_WITH_PWD_3);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            q.setParameter(3, OpenCms.getPasswordHandler().digest(password));
            try {
                CmsDAOUsers u = (CmsDAOUsers) q.getSingleResult();
                user = internalCreateUser(dbc, u);
            } catch (NoResultException e) {
                CmsMessageContainer message = org.opencms.db.Messages.get().container(org.opencms.db.Messages.ERR_UNKNOWN_USER_1, userFqn);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
            return user;
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUserInfos(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public Map readUserInfos(CmsDbContext dbc, CmsUUID userId) throws CmsDataAccessException {
        Map infos = new HashMap();
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERDATA_READ_1);
            q.setParameter(1, userId.toString());
            List<CmsDAOUserData> res = q.getResultList();
            for (CmsDAOUserData ud : res) {
                String key = ud.getDataKey();
                String type = ud.getDataType();
                byte[] value = ud.getDataValue();
                Object data = null;
                try {
                    data = CmsDataTypeUtil.dataDeserialize(value, type);
                } catch (IOException e) {
                    LOG.error(Messages.get().container(Messages.ERR_READING_ADDITIONAL_INFO_1, userId.toString()).key(), e);
                } catch (ClassNotFoundException e) {
                    LOG.error(Messages.get().container(Messages.ERR_READING_ADDITIONAL_INFO_1, userId.toString()).key(), e);
                } catch (NullPointerException e) {
                }
                if ((key != null) && (data != null)) {
                    infos.put(key, data);
                }
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return infos;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUsersOfGroup(CmsDbContext, String, boolean)
     */
    public List readUsersOfGroup(CmsDbContext dbc, String groupFqn, boolean includeOtherOuUsers) throws CmsDataAccessException {
        String sqlQuery = C_GROUPS_GET_USERS_OF_GROUP_2;
        if (includeOtherOuUsers) {
            sqlQuery = C_GROUPS_GET_ALL_USERS_OF_GROUP_2;
        }
        List users = new ArrayList();
        try {
            Query q = m_sqlManager.createQuery(dbc, sqlQuery);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(groupFqn));
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                users.add(internalCreateUser(dbc, u));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return users;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_REMOVE_ALL_1);
            q.setParameter(1, resource.toString());
            List<I_CmsDAOAccessControl> res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                dbc.getEntityManager().remove(ac);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntriesForPrincipal(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntriesForPrincipal(CmsDbContext dbc, CmsProject project, CmsProject onlineProject, CmsUUID principal) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_1);
            q.setParameter(1, principal.toString());
            List<I_CmsDAOAccessControl> res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                dbc.getEntityManager().remove(ac);
            }
            q = m_sqlManager.createQuery(dbc, onlineProject, C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_1);
            q.setParameter(1, principal.toString());
            res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                dbc.getEntityManager().remove(ac);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsUUID resource, CmsUUID principal) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_REMOVE_2);
            q.setParameter(1, resource.toString());
            q.setParameter(2, principal.toString());
            List<I_CmsDAOAccessControl> res = q.getResultList();
            for (I_CmsDAOAccessControl ac : res) {
                dbc.getEntityManager().remove(ac);
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeResourceFromOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, CmsResource)
     */
    public void removeResourceFromOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, CmsResource resource) throws CmsDataAccessException {
        if (orgUnit.getParentFqn() == null) {
            throw new CmsDataAccessException(org.opencms.security.Messages.get().container(org.opencms.security.Messages.ERR_ORGUNIT_ROOT_EDITION_0));
        }
        try {
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
            List vfsPaths = new ArrayList(internalResourcesForOrgUnit(dbc, ouResource));
            if (!vfsPaths.contains(resource.getRootPath())) {
                throw new CmsDataAccessException(Messages.get().container(Messages.ERR_ORGUNIT_DOESNOT_CONTAINS_RESOURCE_2, orgUnit.getName(), dbc.removeSiteRoot(resource.getRootPath())));
            }
            if ((vfsPaths.size() == 1) && !orgUnit.hasFlagWebuser()) {
                throw new CmsDataAccessException(Messages.get().container(Messages.ERR_ORGUNIT_REMOVE_LAST_RESOURCE_2, orgUnit.getName(), dbc.removeSiteRoot(resource.getRootPath())));
            }
            CmsRelationFilter filter = CmsRelationFilter.TARGETS.filterPath(resource.getRootPath());
            m_driverManager.getVfsDriver().deleteRelations(dbc, dbc.currentProject().getUuid(), ouResource, filter);
            m_driverManager.getVfsDriver().deleteRelations(dbc, CmsProject.ONLINE_PROJECT_ID, ouResource, filter);
            try {
                CmsProject project = m_driverManager.readProject(dbc, orgUnit.getProjectId());
                m_driverManager.getProjectDriver().deleteProjectResource(dbc, orgUnit.getProjectId(), resource.getRootPath());
                OpenCms.fireCmsEvent(I_CmsEventListener.EVENT_PROJECT_MODIFIED, Collections.singletonMap("project", project));
            } catch (CmsDbEntryNotFoundException e) {
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#setUsersOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, org.opencms.file.CmsUser)
     */
    public void setUsersOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, CmsUser user) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_SET_ORGUNIT_2);
            q.setParameter(1, user.getId().toString());
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                u.setUserOu((orgUnit == null ? null : CmsOrganizationalUnit.SEPARATOR + orgUnit.getName()));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.security.CmsAccessControlEntry)
     */
    public void writeAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsAccessControlEntry acEntry) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, project, C_ACCESS_READ_ENTRY_2);
            q.setParameter(1, acEntry.getResource().toString());
            q.setParameter(2, acEntry.getPrincipal().toString());
            try {
                I_CmsDAOAccessControl ac = (I_CmsDAOAccessControl) q.getSingleResult();
                q = m_sqlManager.createQuery(dbc, project, C_ACCESS_UPDATE_5);
                q.setParameter(1, acEntry.getResource().toString());
                q.setParameter(2, acEntry.getPrincipal().toString());
                List<I_CmsDAOAccessControl> res = q.getResultList();
                for (I_CmsDAOAccessControl a : res) {
                    a.setAccessAllowed(acEntry.getAllowedPermissions());
                    a.setAccessDenied(acEntry.getDeniedPermissions());
                    a.setAccessFlags(acEntry.getFlags());
                }
            } catch (NoResultException e) {
                m_driverManager.getUserDriver().createAccessControlEntry(dbc, project, acEntry.getResource(), acEntry.getPrincipal(), acEntry.getAllowedPermissions(), acEntry.getDeniedPermissions(), acEntry.getFlags());
                return;
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeGroup(org.opencms.db.CmsDbContext, org.opencms.file.CmsGroup)
     */
    public void writeGroup(CmsDbContext dbc, CmsGroup group) throws CmsDataAccessException {
        if (group != null) {
            try {
                Query q = m_sqlManager.createQuery(dbc, C_GROUPS_WRITE_GROUP_4);
                q.setParameter(1, group.getId().toString());
                List<CmsDAOGroups> res = q.getResultList();
                for (CmsDAOGroups g : res) {
                    g.setGroupDescription(m_sqlManager.validateEmpty(group.getDescription()));
                    g.setGroupFlags(group.getFlags());
                    g.setParentGroupId(group.getParentId().toString());
                }
            } catch (PersistenceException e) {
                throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
            }
        } else {
            throw new CmsDbEntryNotFoundException(org.opencms.db.Messages.get().container(org.opencms.db.Messages.ERR_UNKNOWN_GROUP_1, "null"));
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public void writeOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit organizationalUnit) throws CmsDataAccessException {
        try {
            CmsResource resource = m_driverManager.readResource(dbc, organizationalUnit.getId(), CmsResourceFilter.DEFAULT);
            internalWriteOrgUnitProperty(dbc, resource, new CmsProperty(ORGUNIT_PROPERTY_DESCRIPTION, organizationalUnit.getDescription(), null));
            resource.setFlags(organizationalUnit.getFlags() | CmsResource.FLAG_INTERNAL);
            m_driverManager.writeResource(dbc, resource);
            resource.setState(CmsResource.STATE_UNCHANGED);
            m_driverManager.getVfsDriver().writeResource(dbc, dbc.currentProject().getUuid(), resource, CmsDriverManager.NOTHING_CHANGED);
            if (!dbc.currentProject().isOnlineProject()) {
                CmsProject onlineProject = m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID);
                resource.setState(CmsResource.STATE_UNCHANGED);
                m_driverManager.getVfsDriver().writeResource(dbc, onlineProject.getUuid(), resource, CmsDriverManager.NOTHING_CHANGED);
            }
            OpenCms.fireCmsEvent(I_CmsEventListener.EVENT_CLEAR_ONLINE_CACHES, null);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writePassword(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, java.lang.String)
     */
    public void writePassword(CmsDbContext dbc, String userFqn, String oldPassword, String newPassword) throws CmsDataAccessException, CmsPasswordEncryptionException {
        if (oldPassword != null) {
            readUser(dbc, userFqn, oldPassword, "");
        }
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_SET_PWD_3);
            q.setParameter(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            q.setParameter(2, CmsOrganizationalUnit.SEPARATOR + CmsOrganizationalUnit.getParentFqn(userFqn));
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                u.setUserPassword(OpenCms.getPasswordHandler().digest(newPassword));
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeUser(org.opencms.db.CmsDbContext, org.opencms.file.CmsUser)
     */
    public void writeUser(CmsDbContext dbc, CmsUser user) throws CmsDataAccessException {
        try {
            Query q = m_sqlManager.createQuery(dbc, C_USERS_WRITE_6);
            q.setParameter(1, user.getId().toString());
            List<CmsDAOUsers> res = q.getResultList();
            for (CmsDAOUsers u : res) {
                u.setUserFirstName(m_sqlManager.validateEmpty(user.getFirstname()));
                u.setUserLastName(m_sqlManager.validateEmpty(user.getLastname()));
                u.setUserEmail(m_sqlManager.validateEmpty(user.getEmail()));
                u.setUserLastLogin(user.getLastlogin());
                u.setUserFlags(user.getFlags());
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        internalWriteUserInfos(dbc, user.getId(), user.getAdditionalInfo());
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeUserInfo(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, java.lang.String, java.lang.Object)
     */
    public void writeUserInfo(CmsDbContext dbc, CmsUUID userId, String key, Object value) throws CmsDataAccessException {
        try {
            CmsDAOUserData ud = new CmsDAOUserData();
            ud.setUserId(userId.toString());
            ud.setDataKey(key);
            ud.setDataValue(CmsDataTypeUtil.dataSerialize(value));
            ud.setDataType(value.getClass().getName());
            dbc.getEntityManager().persist(ud);
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_SERIALIZING_USER_DATA_1, userId), e);
        }
    }

    /**
     * Returns a sql query to select groups.<p>
     * 
     * @param mainQuery the main select sql query
     * @param includeSubOus if groups in sub-ous should be included in the selection
     * @param readRoles if groups or roles whould be selected
     * 
     * @return a sql query to select groups
     */
    protected String createRoleQuery(String mainQuery, boolean includeSubOus, boolean readRoles) {
        String sqlQuery = m_sqlManager.readQuery(mainQuery);
        sqlQuery += " ";
        if (includeSubOus) {
            sqlQuery += m_sqlManager.readQuery(C_GROUPS_GROUP_OU_LIKE_1);
        } else {
            sqlQuery += m_sqlManager.readQuery(C_GROUPS_GROUP_OU_EQUALS_1);
        }
        sqlQuery += AND_CONDITION;
        if (readRoles) {
            sqlQuery += m_sqlManager.readQuery(C_GROUPS_SELECT_ROLES_1);
        } else {
            sqlQuery += m_sqlManager.readQuery(C_GROUPS_SELECT_GROUPS_1);
        }
        sqlQuery += " ";
        sqlQuery += m_sqlManager.readQuery(C_GROUPS_ORDER_0);
        return sqlQuery;
    }

    /**
     * Internal helper method to create an access control entry from a database record.<p>
     * 
     * @param res resultset of the current query
     * 
     * @return a new {@link CmsAccessControlEntry} initialized with the values from the current database record 
     */
    protected CmsAccessControlEntry internalCreateAce(I_CmsDAOAccessControl ac) {
        return internalCreateAce(ac, new CmsUUID(ac.getResourceId()));
    }

    /**
     * Internal helper method to create an access control entry from a database record.<p>
     * 
     * @param res resultset of the current query
     * @param newId the id of the new access control entry
     * 
     * @return a new {@link CmsAccessControlEntry} initialized with the values from the current database record
     */
    protected CmsAccessControlEntry internalCreateAce(I_CmsDAOAccessControl ac, CmsUUID newId) {
        return new CmsAccessControlEntry(newId, new CmsUUID(ac.getPrincipalId()), ac.getAccessAllowed(), ac.getAccessDenied(), ac.getAccessFlags());
    }

    /**
     * Creates the default groups and user for the given organizational unit.<p>
     * 
     * @param dbc the database context
     * @param ouFqn the fully qualified name of the organizational unit to create the principals for
     * @param ouDescription the description of the given organizational unit
     * @param webuser the webuser ou flag
     * 
     * @throws CmsException if something goes wrong 
     */
    protected void internalCreateDefaultGroups(CmsDbContext dbc, String ouFqn, String ouDescription, boolean webuser) throws CmsException {
        String rootAdminRole = CmsRole.ROOT_ADMIN.getGroupName();
        try {
            if ((CmsOrganizationalUnit.getParentFqn(ouFqn) != null) || ((CmsOrganizationalUnit.getParentFqn(ouFqn) == null) && !existsGroup(dbc, rootAdminRole))) {
                Iterator itRoles = CmsRole.getSystemRoles().iterator();
                while (itRoles.hasNext()) {
                    CmsRole role = (CmsRole) itRoles.next();
                    if (webuser && (role != CmsRole.ACCOUNT_MANAGER)) {
                        continue;
                    }
                    if (role.isOrganizationalUnitIndependent() && (CmsOrganizationalUnit.getParentFqn(ouFqn) != null)) {
                        continue;
                    }
                    String groupName = ouFqn + role.getGroupName();
                    int flags = I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_ROLE;
                    if ((role == CmsRole.WORKPLACE_USER) || (role == CmsRole.PROJECT_MANAGER)) {
                        flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_USER;
                    }
                    if (role == CmsRole.PROJECT_MANAGER) {
                        flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER;
                    }
                    createGroup(dbc, CmsUUID.getConstantUUID(groupName), groupName, "A system role group", flags, null);
                }
                if ((CmsOrganizationalUnit.getParentFqn(ouFqn) == null) && CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SYSTEM_ROLES_CREATED_0));
                }
            }
        } catch (CmsException e) {
            if ((CmsOrganizationalUnit.getParentFqn(ouFqn) == null) && CmsLog.INIT.isErrorEnabled()) {
                CmsLog.INIT.error(Messages.get().getBundle().key(Messages.INIT_SYSTEM_ROLES_CREATION_FAILED_0), e);
            }
            throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), e);
        }
        if (webuser) {
            return;
        }
        String administratorsGroup = ouFqn + OpenCms.getDefaultUsers().getGroupAdministrators();
        String guestGroup = ouFqn + OpenCms.getDefaultUsers().getGroupGuests();
        String usersGroup = ouFqn + OpenCms.getDefaultUsers().getGroupUsers();
        String projectmanagersGroup = ouFqn + OpenCms.getDefaultUsers().getGroupProjectmanagers();
        String guestUser = ouFqn + OpenCms.getDefaultUsers().getUserGuest();
        String adminUser = ouFqn + OpenCms.getDefaultUsers().getUserAdmin();
        String exportUser = ouFqn + OpenCms.getDefaultUsers().getUserExport();
        String deleteUser = ouFqn + OpenCms.getDefaultUsers().getUserDeletedResource();
        if (existsGroup(dbc, administratorsGroup)) {
            if (CmsOrganizationalUnit.getParentFqn(ouFqn) == null) {
                internalUpdateRoleGroup(dbc, administratorsGroup, CmsRole.ROOT_ADMIN);
                internalUpdateRoleGroup(dbc, usersGroup, CmsRole.WORKPLACE_USER.forOrgUnit(ouFqn));
                internalUpdateRoleGroup(dbc, projectmanagersGroup, CmsRole.PROJECT_MANAGER.forOrgUnit(ouFqn));
            }
            return;
        }
        String parentOu = CmsOrganizationalUnit.getParentFqn(ouFqn);
        String parentGroup = null;
        if (parentOu != null) {
            parentGroup = parentOu + OpenCms.getDefaultUsers().getGroupUsers();
        }
        String groupDescription = (CmsStringUtil.isNotEmptyOrWhitespaceOnly(ouDescription) ? CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTGROUP_OU_USERS_DESCRIPTION_1, new String[] { ouDescription }) : CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTGROUP_ROOT_USERS_DESCRIPTION_0, null));
        createGroup(dbc, CmsUUID.getConstantUUID(usersGroup), usersGroup, groupDescription, I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_USER | CmsRole.WORKPLACE_USER.getVirtualGroupFlags(), parentGroup);
        if (parentOu != null) {
            return;
        }
        CmsGroup guests = createGroup(dbc, CmsUUID.getConstantUUID(guestGroup), guestGroup, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTGROUP_ROOT_GUESTS_DESCRIPTION_0, null), I_CmsPrincipal.FLAG_ENABLED, null);
        int flags = CmsRole.ROOT_ADMIN.getVirtualGroupFlags();
        createGroup(dbc, CmsUUID.getConstantUUID(administratorsGroup), administratorsGroup, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTGROUP_ROOT_ADMINS_DESCRIPTION_0, null), I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER | flags, null);
        parentGroup = ouFqn + OpenCms.getDefaultUsers().getGroupUsers();
        createGroup(dbc, CmsUUID.getConstantUUID(projectmanagersGroup), projectmanagersGroup, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTGROUP_ROOT_PROJMANS_DESCRIPTION_0, null), I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER | I_CmsPrincipal.FLAG_GROUP_PROJECT_USER | CmsRole.PROJECT_MANAGER.getVirtualGroupFlags(), parentGroup);
        CmsUser guest = createUser(dbc, CmsUUID.getConstantUUID(guestUser), guestUser, OpenCms.getPasswordHandler().digest(""), " ", " ", " ", 0, I_CmsPrincipal.FLAG_ENABLED, 0, Collections.singletonMap(CmsUserSettings.ADDITIONAL_INFO_DESCRIPTION, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTUSER_ROOT_GUEST_DESCRIPTION_0, null)));
        createUserInGroup(dbc, guest.getId(), guests.getId());
        CmsUser admin = createUser(dbc, CmsUUID.getConstantUUID(adminUser), adminUser, OpenCms.getPasswordHandler().digest("admin"), " ", " ", " ", 0, I_CmsPrincipal.FLAG_ENABLED, 0, Collections.singletonMap(CmsUserSettings.ADDITIONAL_INFO_DESCRIPTION, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTUSER_ROOT_ADMIN_DESCRIPTION_0, null)));
        createUserInGroup(dbc, admin.getId(), CmsUUID.getConstantUUID(CmsRole.ROOT_ADMIN.getGroupName()));
        createUserInGroup(dbc, admin.getId(), CmsUUID.getConstantUUID(administratorsGroup));
        if (!OpenCms.getDefaultUsers().getUserExport().equals(OpenCms.getDefaultUsers().getUserAdmin()) && !OpenCms.getDefaultUsers().getUserExport().equals(OpenCms.getDefaultUsers().getUserGuest())) {
            CmsUser export = createUser(dbc, CmsUUID.getConstantUUID(exportUser), exportUser, OpenCms.getPasswordHandler().digest((new CmsUUID()).toString()), " ", " ", " ", 0, I_CmsPrincipal.FLAG_ENABLED, 0, Collections.singletonMap(CmsUserSettings.ADDITIONAL_INFO_DESCRIPTION, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTUSER_ROOT_EXPORT_DESCRIPTION_0, null)));
            createUserInGroup(dbc, export.getId(), guests.getId());
        }
        if (!OpenCms.getDefaultUsers().getUserDeletedResource().equals(OpenCms.getDefaultUsers().getUserAdmin()) && !OpenCms.getDefaultUsers().getUserDeletedResource().equals(OpenCms.getDefaultUsers().getUserGuest()) && !OpenCms.getDefaultUsers().getUserDeletedResource().equals(OpenCms.getDefaultUsers().getUserExport())) {
            createUser(dbc, CmsUUID.getConstantUUID(deleteUser), deleteUser, OpenCms.getPasswordHandler().digest((new CmsUUID()).toString()), " ", " ", " ", 0, I_CmsPrincipal.FLAG_ENABLED, 0, Collections.singletonMap(CmsUserSettings.ADDITIONAL_INFO_DESCRIPTION, CmsMacroResolver.localizedKeyMacro(Messages.GUI_DEFAULTUSER_ROOT_DELETED_DESCRIPTION_0, null)));
        }
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DEFAULT_USERS_CREATED_0));
        }
    }

    /**
     * Semi-constructor to create a {@link CmsGroup} instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * @return CmsGroup the new CmsGroup object
     */
    protected CmsGroup internalCreateGroup(CmsDAOGroups g) {
        String ou = CmsOrganizationalUnit.removeLeadingSeparator(g.getGroupOu());
        String description = g.getGroupDescription();
        return new CmsGroup(new CmsUUID(g.getGroupId()), new CmsUUID(g.getParentGroupId()), ou + g.getGroupName(), description, g.getGroupFlags());
    }

    /**
     * Returns the organizational unit represented by the given resource.<p>
     * 
     * @param dbc the current db context
     * @param resource the resource that represents an organizational unit
     * 
     * @return the organizational unit represented by the given resource
     * 
     * @throws CmsException if something goes wrong
     */
    protected CmsOrganizationalUnit internalCreateOrgUnitFromResource(CmsDbContext dbc, CmsResource resource) throws CmsException {
        if (!resource.getRootPath().startsWith(ORGUNIT_BASE_FOLDER)) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_READ_ORGUNIT_1, resource.getRootPath()));
        }
        String name = resource.getRootPath().substring(ORGUNIT_BASE_FOLDER.length());
        if ((name.length() > 0) && !name.endsWith(CmsOrganizationalUnit.SEPARATOR)) {
            name += CmsOrganizationalUnit.SEPARATOR;
        }
        String description = m_driverManager.readPropertyObject(dbc, resource, ORGUNIT_PROPERTY_DESCRIPTION, false).getStructureValue();
        int flags = (resource.getFlags() & ~CmsResource.FLAG_INTERNAL);
        String projectId = m_driverManager.readPropertyObject(dbc, resource, ORGUNIT_PROPERTY_PROJECTID, false).getStructureValue();
        return new CmsOrganizationalUnit(resource.getStructureId(), name, description, flags, (projectId == null ? null : new CmsUUID(projectId)));
    }

    /**
     * Creates a folder with the given path an properties, offline AND online.<p>
     * 
     * @param dbc the current database context
     * @param path the path to create the folder
     * @param flags the resource flags
     * 
     * @return the new created offline folder
     * 
     * @throws CmsException if something goes wrong
     */
    protected CmsResource internalCreateResourceForOrgUnit(CmsDbContext dbc, String path, int flags) throws CmsException {
        CmsResource resource = new CmsFolder(new CmsUUID(), new CmsUUID(), path, CmsResourceTypeFolder.RESOURCE_TYPE_ID, (CmsResource.FLAG_INTERNAL | flags), dbc.currentProject().getUuid(), CmsResource.STATE_NEW, 0, dbc.currentUser().getId(), 0, dbc.currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 0);
        CmsUUID projectId = dbc.getProjectId().isNullUUID() ? dbc.currentProject().getUuid() : dbc.getProjectId();
        m_driverManager.getVfsDriver().createResource(dbc, projectId, resource, null);
        resource.setState(CmsResource.STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeResource(dbc, projectId, resource, CmsDriverManager.NOTHING_CHANGED);
        if (!dbc.currentProject().isOnlineProject() && dbc.getProjectId().isNullUUID()) {
            CmsProject onlineProject = m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID);
            m_driverManager.getVfsDriver().createResource(dbc, onlineProject.getUuid(), resource, null);
        }
        OpenCms.getMemoryMonitor().clearAccessControlListCache();
        OpenCms.getMemoryMonitor().flushProperties();
        OpenCms.getMemoryMonitor().flushPropertyLists();
        OpenCms.fireCmsEvent(new CmsEvent(I_CmsEventListener.EVENT_RESOURCE_CREATED, Collections.singletonMap("resource", resource)));
        return resource;
    }

    /**
     * Semi-constructor to create a {@link CmsUser} instance from a JDBC result set.<p>
     * 
     * @param dbc the current database context
     * @param res the JDBC ResultSet
     * 
     * @return the new CmsUser object
     * 
     * @throws CmsDataAccessException if there is an error in deserializing the user info
     */
    protected CmsUser internalCreateUser(CmsDbContext dbc, CmsDAOUsers u) throws CmsDataAccessException {
        String userName = u.getUserName();
        String ou = CmsOrganizationalUnit.removeLeadingSeparator(u.getUserOu());
        CmsUUID userId = new CmsUUID(u.getUserId());
        Map info = readUserInfos(dbc, userId);
        return new CmsUser(userId, ou + userName, u.getUserPassword(), u.getUserFirstName(), u.getUserLastName(), u.getUserEmail(), u.getUserLastLogin(), u.getUserFlags(), u.getUserDateCreated(), info);
    }

    /**
     * Deletes a resource representing a organizational unit, offline AND online.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource to delete
     * 
     * @throws CmsException if something goes wrong
     */
    protected void internalDeleteOrgUnitResource(CmsDbContext dbc, CmsResource resource) throws CmsException {
        CmsRelationFilter filter = CmsRelationFilter.TARGETS;
        if (!dbc.currentProject().isOnlineProject()) {
            CmsProject project = dbc.currentProject();
            dbc.getRequestContext().setCurrentProject(m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID));
            try {
                m_driverManager.getVfsDriver().deletePropertyObjects(dbc, CmsProject.ONLINE_PROJECT_ID, resource, CmsProperty.DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES);
                m_driverManager.getVfsDriver().removeFolder(dbc, dbc.currentProject(), resource);
                m_driverManager.getUserDriver().removeAccessControlEntries(dbc, dbc.currentProject(), resource.getResourceId());
                m_driverManager.getVfsDriver().deleteRelations(dbc, dbc.getRequestContext().currentProject().getUuid(), resource, filter);
            } finally {
                dbc.getRequestContext().setCurrentProject(project);
            }
        }
        m_driverManager.getVfsDriver().deletePropertyObjects(dbc, CmsProject.ONLINE_PROJECT_ID, resource, CmsProperty.DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES);
        m_driverManager.getVfsDriver().removeFolder(dbc, dbc.currentProject(), resource);
        m_driverManager.getUserDriver().removeAccessControlEntries(dbc, dbc.currentProject(), resource.getResourceId());
        m_driverManager.getVfsDriver().deleteRelations(dbc, dbc.getRequestContext().currentProject().getUuid(), resource, filter);
        OpenCms.getMemoryMonitor().clearAccessControlListCache();
        OpenCms.getMemoryMonitor().flushProperties();
        OpenCms.getMemoryMonitor().flushPropertyLists();
        OpenCms.getMemoryMonitor().flushProjectResources();
        OpenCms.fireCmsEvent(new CmsEvent(I_CmsEventListener.EVENT_RESOURCE_DELETED, Collections.singletonMap("resources", Collections.singletonList(resource))));
        OpenCms.fireCmsEvent(new CmsEvent(I_CmsEventListener.EVENT_RESOURCE_AND_PROPERTIES_MODIFIED, Collections.singletonMap("resource", resource)));
    }

    /**
     * Returns the folder for the given organizational units, or the base folder if <code>null</code>.<p>
     * 
     * The base folder will be created if it does not exist.<p> 
     * 
     * @param dbc the current db context
     * @param orgUnit the organizational unit to get the folder for
     * 
     * @return the base folder for organizational units
     *  
     * @throws CmsException if something goes wrong
     */
    protected CmsResource internalOrgUnitFolder(CmsDbContext dbc, CmsOrganizationalUnit orgUnit) throws CmsException {
        if (orgUnit != null) {
            return m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.DEFAULT);
        } else {
            return null;
        }
    }

    /**
     * Returns the list of root paths associated to the organizational unit represented by the given resource.<p>
     * 
     * @param dbc the current db context
     * @param ouResource the resource that represents the organizational unit to get the resources for
     * 
     * @return the list of associated resource names
     * 
     * @throws CmsException if something goes wrong
     */
    protected List internalResourcesForOrgUnit(CmsDbContext dbc, CmsResource ouResource) throws CmsException {
        List relations = m_driverManager.getRelationsForResource(dbc, ouResource, CmsRelationFilter.TARGETS);
        List paths = new ArrayList();
        Iterator it = relations.iterator();
        while (it.hasNext()) {
            CmsRelation relation = (CmsRelation) it.next();
            paths.add(relation.getTargetPath());
        }
        return paths;
    }

    /**
     * Updates a group to a virtual group.<p>
     * 
     * @param dbc the database context
     * @param groupName the name of the group to update
     * @param role the role for this group
     * 
     * @throws CmsDataAccessException if something goes wrong 
     */
    protected void internalUpdateRoleGroup(CmsDbContext dbc, String groupName, CmsRole role) throws CmsDataAccessException {
        CmsGroup group = readGroup(dbc, groupName);
        if ((CmsRole.valueOf(group) == null) || !CmsRole.valueOf(group).equals(role)) {
            CmsGroup roleGroup = readGroup(dbc, role.getGroupName());
            Iterator it = readUsersOfGroup(dbc, groupName, false).iterator();
            while (it.hasNext()) {
                CmsUser user = (CmsUser) it.next();
                createUserInGroup(dbc, user.getId(), roleGroup.getId());
            }
            group.setFlags(role.getVirtualGroupFlags());
        }
    }

    /**
     * Validates the given root path to be in the scope of the resources of the given organizational unit.<p>
     * 
     * @param dbc the current db context
     * @param orgUnit the organizational unit
     * @param rootPath the root path to check
     * 
     * @throws CmsException if something goes wrong
     */
    protected void internalValidateResourceForOrgUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, String rootPath) throws CmsException {
        CmsResource parentResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
        boolean found = false;
        Iterator itParentPaths = internalResourcesForOrgUnit(dbc, parentResource).iterator();
        while (!found && itParentPaths.hasNext()) {
            String parentPath = (String) itParentPaths.next();
            if (rootPath.startsWith(parentPath)) {
                found = true;
            }
        }
        if (!found) {
            throw new CmsException(Messages.get().container(Messages.ERR_PATH_NOT_IN_PARENT_ORGUNIT_SCOPE_2, orgUnit.getName(), dbc.removeSiteRoot(rootPath)));
        }
    }

    /**
     * Checks if a user is member of a group.<p>
     * 
     * @param dbc the database context
     * @param userId the id of the user to check
     * @param groupId the id of the group to check
     *
     * @return true if user is member of group
     * 
     * @throws CmsDataAccessException if operation was not succesful
     */
    protected boolean internalValidateUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId) throws CmsDataAccessException {
        boolean userInGroup = false;
        try {
            Query q = m_sqlManager.createQuery(dbc, C_GROUPS_USER_IN_GROUP_2);
            q.setParameter(1, groupId.toString());
            q.setParameter(2, userId.toString());
            try {
                CmsDAOGroupUsers gu = (CmsDAOGroupUsers) q.getSingleResult();
                userInGroup = true;
            } catch (NoResultException e) {
            }
        } catch (PersistenceException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_JPA_PERSITENCE, e), e);
        }
        return userInGroup;
    }

    /**
     * Writes a property for an organizational unit resource, online AND offline.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource representing the organizational unit
     * @param property the property to write
     * 
     * @throws CmsException if something goes wrong
     */
    protected void internalWriteOrgUnitProperty(CmsDbContext dbc, CmsResource resource, CmsProperty property) throws CmsException {
        m_driverManager.writePropertyObject(dbc, resource, property);
        resource.setState(CmsResource.STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeResource(dbc, dbc.currentProject().getUuid(), resource, CmsDriverManager.NOTHING_CHANGED);
        CmsProject project = dbc.currentProject();
        dbc.getRequestContext().setCurrentProject(m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID));
        try {
            m_driverManager.writePropertyObject(dbc, resource, property);
            resource.setState(CmsResource.STATE_UNCHANGED);
            m_driverManager.getVfsDriver().writeResource(dbc, dbc.currentProject().getUuid(), resource, CmsDriverManager.NOTHING_CHANGED);
        } finally {
            dbc.getRequestContext().setCurrentProject(project);
        }
    }

    /**
     * Updates the user additional information map.<p>
     * 
     * @param dbc the current database context
     * @param userId the id of the user to update
     * @param additionalInfo the info to write
     * 
     * @throws CmsDataAccessException if something goes wrong
     */
    protected void internalWriteUserInfos(CmsDbContext dbc, CmsUUID userId, Map additionalInfo) throws CmsDataAccessException {
        deleteUserInfos(dbc, userId);
        if (additionalInfo == null) {
            return;
        }
        Iterator itEntries = additionalInfo.entrySet().iterator();
        while (itEntries.hasNext()) {
            Map.Entry entry = (Map.Entry) itEntries.next();
            if ((entry.getKey() != null) && (entry.getValue() != null)) {
                writeUserInfo(dbc, userId, (String) entry.getKey(), entry.getValue());
            }
        }
    }
}
