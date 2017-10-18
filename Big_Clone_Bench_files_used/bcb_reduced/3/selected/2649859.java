package de.iritgo.aktera.base.admin;

import java.util.*;
import javax.inject.Inject;
import lombok.Setter;
import org.apache.avalon.framework.configuration.Configuration;
import de.iritgo.aktera.address.AddressDAO;
import de.iritgo.aktera.address.entity.Address;
import de.iritgo.aktera.authentication.defaultauth.entity.*;
import de.iritgo.aktera.configuration.preferences.*;
import de.iritgo.aktera.event.EventManager;
import de.iritgo.aktera.license.LicenseTools;
import de.iritgo.aktera.model.*;
import de.iritgo.aktera.permissions.PermissionManager;
import de.iritgo.aktera.persist.*;
import de.iritgo.aktera.spring.SpringTools;
import de.iritgo.aktera.tools.ModelTools;
import de.iritgo.aktera.ui.form.*;
import de.iritgo.aktera.ui.tools.UserTools;
import de.iritgo.simplelife.math.NumberTools;
import de.iritgo.simplelife.string.StringTools;
import de.iritgo.simplelife.tools.Option;

public class UserFormularHandler extends FormularHandler {

    private static final int FREE_ADDITIONAL_USER_ACCOUNT = 1;

    @Setter
    @Inject
    private PermissionManager permissionManager;

    @Setter
    @Inject
    private PreferencesManager preferencesManager;

    @Setter
    @Inject
    private UserDAO userDAO;

    @Inject
    private AddressDAO addressDAO;

    public UserFormularHandler() {
    }

    public UserFormularHandler(FormularHandler handler) {
        super(handler);
    }

    @Override
    public void loadPersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, List<Configuration> persistentConfig, Integer id) throws ModelException, PersistenceException {
        super.loadPersistents(request, formular, persistents, persistentConfig, id);
        FormTools.createInputValuesFromPropertyTable(request, formular, persistents, "aktera.PreferencesConfig", "gui", id);
    }

    @Override
    public void adjustFormular(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents) throws ModelException, PersistenceException {
        PersistentFactory persistentManager = (PersistentFactory) request.getService(PersistentFactory.ROLE, request.getDomain());
        Persistent user = persistents.getPersistent("sysUser");
        int userId = NumberTools.toInt(user.getField("uid"), -1);
        TreeMap themes = new TreeMap();
        persistents.putAttributeValidValues("preferences.theme", themes);
        themes.put("", "$default");
        for (Iterator i = KeelPreferencesManager.themeIterator(); i.hasNext(); ) {
            KeelPreferencesManager.ThemeInfo info = (KeelPreferencesManager.ThemeInfo) i.next();
            themes.put(info.getId(), info.getName());
        }
        TreeMap roles = new TreeMap();
        persistents.putAttributeValidValues("role", roles);
        roles.put("admin", "$admin");
        roles.put("manager", "$manager");
        roles.put("user", "$user");
        if (userId == -1) {
            persistents.putAttribute("role", "user");
        } else {
            Persistent groupMember = persistentManager.create("keel.groupmembers");
            groupMember.setField("groupname", "admin");
            groupMember.setField("uid", userId);
            if (groupMember.find()) {
                persistents.putAttribute("role", "admin");
            } else {
                groupMember = persistentManager.create("keel.groupmembers");
                groupMember.setField("groupname", "manager");
                groupMember.setField("uid", userId);
                if (groupMember.find()) {
                    persistents.putAttribute("role", "manager");
                } else {
                    persistents.putAttribute("role", "user");
                }
            }
        }
        if (user.getStatus() == Persistent.NEW) {
            TreeMap groups = new TreeMap();
            persistents.putAttributeValidValues("newUsersGroup", groups);
            for (AkteraGroup group : userDAO.findAllGroups()) {
                groups.put(group.getId().toString(), group.getName());
            }
            AkteraGroup userGroup = userDAO.findGroupByName(AkteraGroup.GROUP_NAME_USER);
            persistents.putAttribute("newUsersGroup", userGroup.getId());
            formular.getField("newUsersGroup").setVisible(true);
        } else {
            formular.getField("newUsersGroup").setVisible(false);
        }
        formular.getGroup("account").getField("sysUser.name").setReadOnly(userId != -1);
        boolean readOnly = userId != -1 && persistents.getPersistent("preferences").getFieldBoolean("protect");
        if (StringTools.trim(persistents.getPersistent("preferences").getField("security")).indexOf('W') != -1) {
            readOnly = false;
        }
        formular.setReadOnly(readOnly);
        if (userId == 1) {
            formular.getGroup("settings").getField("preferences.canChangePassword").setVisible(false);
            formular.getGroup("account").getField("role").setVisible(false);
            formular.getGroup("account").getField("deletePassword").setVisible(false);
        }
    }

    @Override
    public void validatePersistents(List<Configuration> persistentConfig, ModelRequest request, ModelResponse response, FormularDescriptor formular, PersistentDescriptor persistents, boolean create, ValidationResult result) throws ModelException, PersistenceException {
        long userCount = userDAO.countNonSystemUsers();
        if (create && LicenseTools.getLicenseInfo().hasUserLimit() && (userCount == -1 || userCount >= (LicenseTools.getLicenseInfo().getUserCount() + FREE_ADDITIONAL_USER_ACCOUNT))) {
            FormTools.addError(response, result, "sysUser.name", "Aktera:licenseUserRestrictions");
        }
        String password = (String) persistents.getAttribute("passwordNew");
        if (!StringTools.isTrimEmpty(password)) {
            if (!password.equals(persistents.getAttribute("passwordNewRepeat"))) {
                FormTools.addError(response, result, "passwordNew", "passwordsDontMatch");
            }
        }
        String pin = (String) persistents.getAttribute("pinNew");
        if (!StringTools.isTrimEmpty(pin)) {
            if (!pin.equals(persistents.getAttribute("pinNewRepeat"))) {
                FormTools.addError(response, result, "pinNew", "pinsDontMatch");
            }
        }
        int size = NumberTools.toInt(persistents.getAttribute("gui.tableRowsPerPage"), 15);
        if ((size < 1) || (size > 1000)) {
            FormTools.addError(response, result, "gui.tableRowsPerPage", "illegalRowsPerPage");
        }
    }

    @Override
    public void preStorePersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, boolean modified) throws ModelException, PersistenceException {
        String password = (String) persistents.getAttribute("passwordNew");
        if (!StringTools.isTrimEmpty(password)) {
            if (password.equals(persistents.getAttribute("passwordNewRepeat"))) {
                persistents.getPersistent("sysUser").setField("password", StringTools.digest(password));
            }
        }
        if (NumberTools.toBool(persistents.getAttribute("deletePassword"), false)) {
            persistents.getPersistent("sysUser").setField("password", null);
        }
        String pin = (String) persistents.getAttribute("pinNew");
        if (!StringTools.isTrimEmpty(pin)) {
            if (pin.equals(persistents.getAttribute("pinNewRepeat"))) {
                persistents.getPersistent("preferences").setField("pin", pin);
            }
        }
        persistents.getPersistent("sysUser").setField("email", persistents.getPersistent("address").getField("email"));
        persistents.getPersistent("address").setField("internalLastname", StringTools.trim(persistents.getPersistent("address").getField("lastName")).toLowerCase());
        persistents.getPersistent("address").setField("internalCompany", StringTools.trim(persistents.getPersistent("address").getField("company")).toLowerCase());
    }

    @Override
    public void updatePersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, List<Configuration> persistentConfig, boolean modified) throws ModelException, PersistenceException {
        super.updatePersistents(request, formular, persistents, persistentConfig, modified);
        updateUserPersistents(request, formular, persistents, modified);
        FormTools.storeInputValuesToPropertyTable(request, formular, persistents, "aktera.PreferencesConfig", "gui", new Integer(persistents.getPersistent("sysUser").getFieldInt("uid")));
        EventManager em = (EventManager) SpringTools.getBean(EventManager.ID);
        Properties props = new Properties();
        props.put("id", persistents.getPersistent("sysUser").getFieldInt("uid"));
        props.put("name", persistents.getPersistent("sysUser").getFieldString("name"));
        em.fire("aktera.user.updated", request, log, props);
    }

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    public void updateUserPersistents(ModelRequest req, @SuppressWarnings("unused") FormularDescriptor formular, PersistentDescriptor persistents, @SuppressWarnings("unused") boolean modified) throws ModelException, PersistenceException {
        PersistentFactory persistentManager = (PersistentFactory) req.getService(PersistentFactory.ROLE, req.getDomain());
        updateSystemGroups(persistentManager, persistents.getPersistent("sysUser").getField("uid"), (String) persistents.getAttribute("role"));
        try {
            Properties props = new Properties();
            props.put("userId", new Integer(persistents.getPersistent("sysUser").getFieldInt("uid")));
            if (persistents.getAttribute("passwordNew") != null) {
                props.put("password", StringTools.digest((String) persistents.getAttribute("passwordNew")));
            }
            ModelTools.callModel(req, "aktera.aktario.user.modify-aktario-user", props);
        } catch (ModelException x) {
        }
        if (persistents.getPersistent("sysUser").getFieldInt("uid") == UserTools.getCurrentUserId(req)) {
            preferencesManager.clearCache(UserTools.getCurrentUserId(req));
            UserTools.setUserEnvObject(req, "sessionInfoLoaded", "N");
        }
    }

    @Override
    public int createPersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, List<Configuration> persistentConfig) throws ModelException, PersistenceException {
        int res = createUserPersistents(request, formular, persistents);
        FormTools.storeInputValuesToPropertyTable(request, formular, persistents, "aktera.PreferencesConfig", "gui", new Integer(persistents.getPersistent("sysUser").getFieldInt("uid")));
        EventManager em = (EventManager) SpringTools.getBean(EventManager.ID);
        Properties props = new Properties();
        props.put("id", persistents.getPersistent("sysUser").getFieldInt("uid"));
        props.put("name", persistents.getPersistent("sysUser").getFieldString("name"));
        em.fire("aktera.user.created", request, log, props);
        return res;
    }

    public int createUserPersistents(ModelRequest request, @SuppressWarnings("unused") FormularDescriptor formular, PersistentDescriptor persistents) throws ModelException, PersistenceException {
        PersistentFactory persistentManager = (PersistentFactory) request.getService(PersistentFactory.ROLE, request.getDomain());
        persistents.getPersistent("sysUser").add();
        Integer userId = new Integer(persistents.getPersistent("sysUser").getFieldInt("uid"));
        updateSystemGroups(persistentManager, userId, (String) persistents.getAttribute("role"));
        persistents.getPersistent("preferences").setField("userId", userId);
        persistents.getPersistent("preferences").add();
        persistents.getPersistent("party").setField("userId", userId);
        persistents.getPersistent("party").add();
        Integer partyId = new Integer(persistents.getPersistent("party").getFieldInt("partyId"));
        persistents.getPersistent("profile").setField("partyId", partyId);
        persistents.getPersistent("profile").add();
        persistents.getPersistent("address").setField("partyId", partyId);
        persistents.getPersistent("address").setField("category", "G");
        persistents.getPersistent("address").add();
        AkteraGroupEntry groupEntry = new AkteraGroupEntry();
        groupEntry.setGroupId(NumberTools.toIntInstance(persistents.getAttribute("newUsersGroup")));
        groupEntry.setUserId(userId);
        userDAO.createAkteraGroupEntry(groupEntry);
        request.setParameter("id", userId);
        KeelPreferencesManager.createDefaultValues(request, userId);
        try {
            Properties params = new Properties();
            params = new Properties();
            params.put("userId", userId);
            params.put("password", StringTools.digest((String) persistents.getAttribute("passwordNew")));
            ModelTools.callModel(request, "aktera.aktario.user.create-aktario-user", params);
        } catch (ModelException x) {
        }
        return userId.intValue();
    }

    @Override
    public void deletePersistent(ModelRequest request, ModelResponse response, Object id, Persistent persistent, boolean systemDelete) throws ModelException, PersistenceException {
        int userId = persistent.getFieldInt("uid");
        if (userId != -1 && userId != 0 && userId != 1 && userId != 2) {
            String userName = persistent.getFieldString("name");
            EventManager em = (EventManager) SpringTools.getBean(EventManager.ID);
            Properties props = new Properties();
            props.put("id", userId);
            props.put("name", userName);
            em.fire("aktera.user.delete", request, log, props);
            deleteUserPersistent(request, response, persistent, systemDelete);
            props = new Properties();
            props.put("id", userId);
            props.put("name", userName);
            em.fire("aktera.user.deleted", request, log, props);
        }
    }

    public void deleteUserPersistent(ModelRequest request, @SuppressWarnings("unused") ModelResponse response, Persistent persistent, boolean systemDelete) throws ModelException, PersistenceException {
        PersistentFactory persistentManager = (PersistentFactory) request.getService(PersistentFactory.ROLE, request.getDomain());
        Integer userId = NumberTools.toIntInstance(persistent.getField("uid"), -1);
        Persistent user = persistentManager.create("keel.user");
        user.setField("uid", userId);
        if (!user.find()) {
            return;
        }
        Persistent preferences = persistentManager.create("aktera.Preferences");
        preferences.setField("userId", userId);
        if (!preferences.find() || (preferences.getFieldBoolean("protect") && !systemDelete)) {
            return;
        }
        Persistent party = persistentManager.create("aktera.Party");
        party.setField("userId", userId);
        if (!party.find()) {
            return;
        }
        if (request.getParameter("deleteAddress") != null) {
            Option<Address> address = addressDAO.findAddressByPartyId(party.getFieldInt("partyId"));
            if (address.full()) {
                addressDAO.deleteAddress(address.get());
            }
        }
        Properties props = new Properties();
        props = new Properties();
        props.put("id", userId.toString());
        ModelTools.callModel(request, "aktera.aktario.user.delete-aktario-user", props);
        userDAO.deleteAkteraGroupEntriesByUserId(userId);
        permissionManager.deleteAllPermissionsOfPrincipal(userId, "U");
        preferences.delete();
        Persistent preferencesConfig = persistentManager.create("aktera.PreferencesConfig");
        preferencesConfig.setField("userId", userId);
        preferencesConfig.deleteAll();
        Persistent profile = persistentManager.create("aktera.Profile");
        profile.setField("partyId", party.getField("partyId"));
        if (profile.find()) {
            profile.delete();
        }
        party.delete();
        user.delete();
        Persistent keelGroups = persistentManager.create("keel.groupmembers");
        keelGroups.setField("uid", userId);
        keelGroups.deleteAll();
    }

    protected void updateSystemGroups(PersistentFactory persistentManager, Object userId, String role) throws PersistenceException {
        Persistent groupMember = persistentManager.create("keel.groupmembers");
        groupMember.setField("uid", userId);
        groupMember.deleteAll();
        if ("admin".equals(role)) {
            groupMember = persistentManager.create("keel.groupmembers");
            groupMember.setField("uid", userId);
            groupMember.setField("groupname", "root");
            groupMember.add();
            groupMember.setField("groupname", "admin");
            groupMember.add();
            groupMember.setField("groupname", "manager");
            groupMember.add();
            groupMember.setField("groupname", "user");
            groupMember.add();
        } else if ("manager".equals(role)) {
            groupMember = persistentManager.create("keel.groupmembers");
            groupMember.setField("uid", userId);
            groupMember.setField("groupname", "manager");
            groupMember.add();
            groupMember.setField("groupname", "user");
            groupMember.add();
        } else {
            groupMember = persistentManager.create("keel.groupmembers");
            groupMember.setField("uid", userId);
            groupMember.setField("groupname", "user");
            groupMember.add();
        }
    }
}
