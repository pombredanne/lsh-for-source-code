package de.iritgo.aktera.base.my;

import de.iritgo.aktera.configuration.preferences.KeelPreferencesManager;
import de.iritgo.aktera.configuration.preferences.PreferencesManager;
import de.iritgo.aktera.event.EventManager;
import de.iritgo.aktera.model.ModelException;
import de.iritgo.aktera.model.ModelRequest;
import de.iritgo.aktera.model.ModelResponse;
import de.iritgo.aktera.permissions.PermissionException;
import de.iritgo.aktera.persist.PersistenceException;
import de.iritgo.aktera.persist.Persistent;
import de.iritgo.aktera.spring.SpringTools;
import de.iritgo.aktera.tools.ModelTools;
import de.iritgo.aktera.ui.form.FormTools;
import de.iritgo.aktera.ui.form.FormularDescriptor;
import de.iritgo.aktera.ui.form.FormularHandler;
import de.iritgo.aktera.ui.form.PersistentDescriptor;
import de.iritgo.aktera.ui.form.ValidationResult;
import de.iritgo.aktera.ui.tools.UserTools;
import de.iritgo.simplelife.math.NumberTools;
import de.iritgo.simplelife.string.StringTools;
import org.apache.avalon.framework.configuration.Configuration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

/**
 *
 */
public class SettingsFormularHandler extends FormularHandler {

    private PreferencesManager preferencesManager;

    public void setPreferencesManager(PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    @Override
    public Object getPersistentId(ModelRequest request, String formName, String keyName) {
        try {
            return getActualUserId(request);
        } catch (PermissionException x) {
        }
        return UserTools.getCurrentUserId(request);
    }

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    @Override
    public void loadPersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, List<Configuration> persistentConfig, Integer id) throws ModelException, PersistenceException {
        super.loadPersistents(request, formular, persistents, persistentConfig, id);
        FormTools.createInputValuesFromPropertyTable(request, formular, persistents, "aktera.PreferencesConfig");
        if (id.equals(UserTools.getCurrentUserId(request))) {
            formular.setTitle("editSettings");
        } else {
            formular.setTitle("editSettingsFor|" + getActualUserName(request));
        }
    }

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    @Override
    public void adjustFormular(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents) throws ModelException, PersistenceException {
        Persistent user = persistents.getPersistent("sysUser");
        TreeMap themes = new TreeMap();
        persistents.putAttributeValidValues("preferences.theme", themes);
        themes.put("", "$default");
        for (Iterator i = KeelPreferencesManager.themeIterator(); i.hasNext(); ) {
            KeelPreferencesManager.ThemeInfo info = (KeelPreferencesManager.ThemeInfo) i.next();
            themes.put(info.getId(), info.getName());
        }
        boolean readOnly = user.getStatus() == Persistent.CURRENT && persistents.getPersistent("preferences").getFieldBoolean("protect");
        if (StringTools.trim(persistents.getPersistent("preferences").getField("security")).indexOf('W') != -1) {
            readOnly = false;
        }
        formular.setReadOnly(readOnly);
        if (user.getStatus() == Persistent.CURRENT && !persistents.getPersistent("preferences").getFieldBoolean("canChangePassword")) {
            formular.getGroup("account").getField("passwordNew").setVisible(false);
            formular.getGroup("account").getField("passwordNewRepeat").setVisible(false);
            formular.getGroup("account").getField("pinNew").setVisible(false);
            formular.getGroup("account").getField("pinNewRepeat").setVisible(false);
        }
    }

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    @Override
    public void validatePersistents(List<Configuration> persistentConfig, ModelRequest request, ModelResponse response, FormularDescriptor formular, PersistentDescriptor persistents, boolean create, ValidationResult result) throws ModelException, PersistenceException {
        String password = (String) persistents.getAttribute("passwordNew");
        if (!StringTools.isTrimEmpty(password)) {
            if (!persistents.getPersistent("preferences").getFieldBoolean("canChangePassword")) {
                FormTools.addError(response, result, "passwordNew", "userNotAllowedToChangePasswort");
            } else {
                if (!password.equals(persistents.getAttribute("passwordNewRepeat"))) {
                    FormTools.addError(response, result, "passwordNew", "passwordsDontMatch");
                }
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

    /**
	 * @see de.iritgo.aktera.ui.form.FormularHandler
	 */
    @Override
    public void updatePersistents(ModelRequest request, FormularDescriptor formular, PersistentDescriptor persistents, List<Configuration> persistentConfig, boolean modified) throws ModelException, PersistenceException {
        String password = (String) persistents.getAttribute("passwordNew");
        if (!StringTools.isTrimEmpty(password) && persistents.getPersistent("preferences").getFieldBoolean("canChangePassword")) {
            persistents.getPersistent("sysUser").setField("password", StringTools.digest((String) persistents.getAttribute("passwordNew")));
        }
        String pin = (String) persistents.getAttribute("pinNew");
        if (!StringTools.isTrimEmpty(pin)) {
            if (pin.equals(persistents.getAttribute("pinNewRepeat"))) {
                persistents.getPersistent("preferences").setField("pin", pin);
            }
        }
        persistents.getPersistent("sysUser").setField("email", persistents.getPersistent("address").getField("email"));
        super.updatePersistents(request, formular, persistents, persistentConfig, modified);
        try {
            Properties props = new Properties();
            props.put("userId", persistents.getPersistent("sysUser").getFieldInt("uid"));
            if (!StringTools.isTrimEmpty(password)) {
                props.put("password", StringTools.digest((String) persistents.getAttribute("passwordNew")));
            }
            ModelTools.callModel(request, "aktera.aktario.user.modify-aktario-user", props);
        } catch (ModelException x) {
        }
        FormTools.storeInputValuesToPropertyTable(request, formular, persistents, "aktera.PreferencesConfig");
        int userId = UserTools.getCurrentUserId(request).intValue();
        if (userId == persistents.getPersistent("sysUser").getFieldInt("uid")) {
            preferencesManager.clearCache(UserTools.getCurrentUserId(request));
            UserTools.setUserEnvObject(request, "sessionInfoLoaded", "N");
        }
        EventManager em = (EventManager) (EventManager) SpringTools.getBean(EventManager.ID);
        Properties props = new Properties();
        props.put("id", persistents.getPersistent("sysUser").getFieldInt("uid"));
        props.put("name", persistents.getPersistent("sysUser").getFieldString("name"));
        em.fire("aktera.user.updated", request, log, props);
    }
}
