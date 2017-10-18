package com.liferay.portal.ejb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import javax.mail.internet.InternetAddress;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.mail.ejb.MailManagerUtil;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.DuplicateUserIdException;
import com.liferay.portal.NoSuchGroupException;
import com.liferay.portal.NoSuchRoleException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.RequiredUserException;
import com.liferay.portal.ReservedUserEmailAddressException;
import com.liferay.portal.ReservedUserIdException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserIdException;
import com.liferay.portal.UserIdValidator;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.UserSmsException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layer;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.Skin;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.admin.ejb.AdminConfigManagerUtil;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.CollectionFactory;
import com.liferay.util.Encryptor;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Time;
import com.liferay.util.Validator;
import com.liferay.util.mail.MailMessage;

/**
 * <a href="UserLocalManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class UserLocalManagerImpl implements UserLocalManager {

    public boolean addGroup(String userId, String groupId) throws PortalException, SystemException {
        return UserUtil.addGroup(userId, groupId);
    }

    public boolean addGroup(String userId, Group group) throws PortalException, SystemException {
        return UserUtil.addGroup(userId, group);
    }

    public boolean addRole(String userId, String roleId) throws PortalException, SystemException {
        return UserUtil.addRole(userId, roleId);
    }

    public boolean addRole(String userId, Role role) throws PortalException, SystemException {
        return UserUtil.addRole(userId, role);
    }

    public User addUser(String companyId, boolean autoUserId, String userId, boolean autoPassword, String password1, String password2, boolean passwordReset, String firstName, String middleName, String lastName, String nickName, boolean male, Date birthday, String emailAddress, Locale locale) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        emailAddress = emailAddress.trim().toLowerCase();
        boolean alwaysAutoUserId = GetterUtil.getBoolean(PropsUtil.get(PropsUtil.USERS_ID_ALWAYS_AUTOGENERATE));
        if (alwaysAutoUserId) {
            autoUserId = true;
        }
        validate(companyId, autoUserId, userId, autoPassword, password1, password2, firstName, lastName, emailAddress);
        Company company = CompanyUtil.findByPrimaryKey(companyId);
        if (autoUserId) {
            userId = companyId + "." + Long.toString(CounterManagerUtil.increment(User.class.getName() + "." + companyId));
        }
        User user = UserUtil.create(userId);
        if (autoPassword) {
            password1 = PwdToolkitUtil.generate();
        }
        int passwordsLifespan = GetterUtil.getInteger(PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));
        Date expirationDate = null;
        if (passwordsLifespan > 0) {
            expirationDate = new Date(System.currentTimeMillis() + Time.DAY * passwordsLifespan);
        }
        user.setCompanyId(companyId);
        user.setCreateDate(new Date());
        user.setPassword(Encryptor.digest(password1));
        user.setPasswordEncrypted(true);
        user.setPasswordExpirationDate(expirationDate);
        user.setPasswordReset(passwordReset);
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setNickName(nickName);
        user.setMale(male);
        user.setBirthday(birthday);
        user.setEmailAddress(emailAddress);
        if (user.hasCompanyMx()) {
            MailManagerUtil.addUser(userId, password1, firstName, middleName, lastName, emailAddress);
        }
        User defaultUser = getDefaultUser(companyId);
        String greeting = null;
        try {
            greeting = LanguageUtil.get(companyId, locale, "welcome") + ", " + user.getFullName() + "!";
        } catch (LanguageException le) {
            greeting = "Welcome, " + user.getFullName() + "!";
        }
        user.setLanguageId(locale.toString());
        user.setTimeZoneId(defaultUser.getTimeZoneId());
        user.setSkinId(defaultUser.getSkinId());
        user.setDottedSkins(defaultUser.isDottedSkins());
        user.setRoundedSkins(defaultUser.isRoundedSkins());
        user.setGreeting(greeting);
        user.setResolution(defaultUser.getResolution());
        user.setRefreshRate(defaultUser.getRefreshRate());
        user.setLayoutIds("");
        user.setActive(true);
        UserUtil.update(user);
        UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(companyId);
        List groups = new ArrayList();
        String groupNames[] = userConfig.getGroupNames();
        for (int i = 0; groupNames != null && i < groupNames.length; i++) {
            try {
                groups.add(GroupUtil.findByC_N(companyId, groupNames[i]));
            } catch (NoSuchGroupException nsge) {
            }
        }
        UserUtil.setGroups(userId, groups);
        List roles = new ArrayList();
        String roleNames[] = userConfig.getRoleNames();
        for (int i = 0; roleNames != null && i < roleNames.length; i++) {
            try {
                Role role = RoleLocalManagerUtil.getRoleByName(companyId, roleNames[i]);
                roles.add(role);
            } catch (NoSuchRoleException nsre) {
            }
        }
        UserUtil.setRoles(userId, roles);
        EmailConfig registrationEmail = userConfig.getRegistrationEmail();
        if (registrationEmail != null && registrationEmail.isSend()) {
            String adminName = company.getAdminName();
            String subject = registrationEmail.getSubject();
            subject = StringUtil.replace(subject, new String[] { "[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]", "[$COMPANY_MX$]", "[$COMPANY_NAME$]", "[$PORTAL_URL$]", "[$USER_EMAIL_ADDRESS$]", "[$USER_ID$]", "[$USER_NAME$]", "[$USER_PASSWORD$]" }, new String[] { company.getEmailAddress(), adminName, company.getMx(), company.getName(), company.getPortalURL(), user.getEmailAddress(), user.getUserId(), user.getFullName(), password1 });
            String body = registrationEmail.getBody();
            body = StringUtil.replace(body, new String[] { "[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]", "[$COMPANY_MX$]", "[$COMPANY_NAME$]", "[$PORTAL_URL$]", "[$USER_EMAIL_ADDRESS$]", "[$USER_ID$]", "[$USER_NAME$]", "[$USER_PASSWORD$]" }, new String[] { company.getEmailAddress(), adminName, company.getMx(), company.getName(), company.getPortalURL(), user.getEmailAddress(), user.getUserId(), user.getFullName(), password1 });
            try {
                InternetAddress from = new InternetAddress(company.getEmailAddress(), adminName);
                InternetAddress[] to = new InternetAddress[] { new InternetAddress(user.getEmailAddress(), user.getFullName()) };
                InternetAddress[] cc = null;
                InternetAddress[] bcc = new InternetAddress[] { new InternetAddress(company.getEmailAddress(), adminName) };
                MailManagerUtil.sendEmail(new MailMessage(from, to, cc, bcc, subject, body));
            } catch (IOException ioe) {
                throw new SystemException(ioe);
            }
        }
        return user;
    }

    public boolean deleteGroup(String userId, String groupId) throws PortalException, SystemException {
        return UserUtil.removeGroup(userId, groupId);
    }

    public boolean deleteGroup(String userId, Group group) throws PortalException, SystemException {
        return UserUtil.removeGroup(userId, group);
    }

    public boolean deleteRole(String userId, String roleId) throws PortalException, SystemException {
        return UserUtil.removeRole(userId, roleId);
    }

    public boolean deleteRole(String userId, Role role) throws PortalException, SystemException {
        return UserUtil.removeRole(userId, role);
    }

    public void deleteUser(String userId) throws PortalException, SystemException {
        if (!GetterUtil.getBoolean(PropsUtil.get(PropsUtil.USERS_DELETE))) {
            throw new RequiredUserException();
        }
        User user = UserUtil.findByPrimaryKey(userId);
        ImageLocalUtil.remove(userId);
        SkinLocalManagerUtil.deleteSkin(userId);
        PortletPreferencesLocalManagerUtil.deleteAllByUser(userId);
        LayoutLocalManagerUtil.deleteAll(userId);
        PasswordTrackerLocalManagerUtil.deleteAll(userId);
        AddressLocalManagerUtil.deleteAll(user.getCompanyId(), User.class.getName(), userId);
        MailManagerUtil.deleteUser(userId);
        UserUtil.remove(userId);
    }

    public List findByC_SMS(String companyId) throws SystemException {
        return UserFinder.findByC_SMS(companyId);
    }

    public User getDefaultUser(String companyId) throws PortalException, SystemException {
        return UserUtil.findByPrimaryKey(User.getDefaultUserId(companyId));
    }

    public List getGroups(String userId) throws PortalException, SystemException {
        return UserUtil.getGroups(userId);
    }

    public List getGroups(String userId, int begin, int end) throws PortalException, SystemException {
        return UserUtil.getGroups(userId, begin, end);
    }

    public int getGroupsSize(String userId) throws SystemException {
        return UserUtil.getGroupsSize(userId);
    }

    public List getRoles(String userId) throws PortalException, SystemException {
        return UserUtil.getRoles(userId);
    }

    public List getRoles(String userId, int begin, int end) throws PortalException, SystemException {
        return UserUtil.getRoles(userId, begin, end);
    }

    public int getRolesSize(String userId) throws SystemException {
        return UserUtil.getRolesSize(userId);
    }

    public User getUserByEmailAddress(String companyId, String emailAddress) throws PortalException, SystemException {
        emailAddress = emailAddress.trim().toLowerCase();
        return UserUtil.findByC_EA(companyId, emailAddress);
    }

    public User getUserById(String userId) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        return UserUtil.findByPrimaryKey(userId);
    }

    public User getUserById(String companyId, String userId) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        return UserUtil.findByC_U(companyId, userId);
    }

    public boolean hasGroupById(String userId, String groupId) throws PortalException, SystemException {
        return UserUtil.containsGroup(userId, groupId);
    }

    public boolean hasGroupByName(String companyId, String userId, String name) throws PortalException, SystemException {
        Group group = GroupLocalManagerUtil.getGroupByName(companyId, name);
        return UserUtil.containsGroup(userId, group.getGroupId());
    }

    public boolean hasRedoUpdateSkin(String userId) {
        Stack redoStack = SkinLocalUtil.getRedoStack(userId);
        return !redoStack.empty();
    }

    public boolean hasUndoUpdateSkin(String userId) {
        Stack undoStack = SkinLocalUtil.getUndoStack(userId);
        return !undoStack.empty();
    }

    public User redoUpdateSkin(String userId) throws PortalException, SystemException {
        User user = UserUtil.findByPrimaryKey(userId);
        Stack redoStack = SkinLocalUtil.getRedoStack(userId);
        Stack undoStack = SkinLocalUtil.getUndoStack(userId);
        Skin skin = user.getSkin();
        undoStack.push(skin);
        skin = (Skin) redoStack.pop();
        return _updateSkin(user, skin);
    }

    public void setGroups(String userId, String[] groupIds) throws PortalException, SystemException {
        UserUtil.setGroups(userId, groupIds);
    }

    public void setGroups(String userId, List groups) throws PortalException, SystemException {
        UserUtil.setGroups(userId, groups);
    }

    public void setLayouts(String userId, String[] layoutIds) throws PortalException, SystemException {
        if (layoutIds == null) {
            return;
        }
        User user = UserUtil.findByPrimaryKey(userId);
        Set layoutIdsSet = new LinkedHashSet();
        for (int i = 0; i < layoutIds.length; i++) {
            layoutIdsSet.add(layoutIds[i]);
        }
        Set newLayoutIdsSet = CollectionFactory.getHashSet();
        Iterator itr = LayoutUtil.findByUserId(userId).iterator();
        while (itr.hasNext()) {
            Layout layout = (Layout) itr.next();
            if (!layoutIdsSet.contains(layout.getLayoutId())) {
                LayoutLocalManagerUtil.deleteLayout(layout.getPrimaryKey());
            } else {
                newLayoutIdsSet.add(layout.getLayoutId());
            }
        }
        StringBuffer layoutIdsSB = new StringBuffer();
        itr = layoutIdsSet.iterator();
        while (itr.hasNext()) {
            String layoutId = (String) itr.next();
            if (newLayoutIdsSet.contains(layoutId)) {
                layoutIdsSB.append(layoutId);
                layoutIdsSB.append(StringPool.COMMA);
            }
        }
        user.setLayoutIds(layoutIdsSB.toString());
        UserUtil.update(user);
    }

    public void setRoles(String userId, String[] roleIds) throws PortalException, SystemException {
        UserUtil.setRoles(userId, roleIds);
    }

    public void setRoles(String userId, List roles) throws PortalException, SystemException {
        UserUtil.setRoles(userId, roles);
    }

    public User undoUpdateSkin(String userId) throws PortalException, SystemException {
        User user = UserUtil.findByPrimaryKey(userId);
        Stack redoStack = SkinLocalUtil.getRedoStack(userId);
        Stack undoStack = SkinLocalUtil.getUndoStack(userId);
        Skin skin = user.getSkin();
        redoStack.push(skin);
        skin = (Skin) undoStack.pop();
        return _updateSkin(user, skin);
    }

    public User updateActive(String userId, boolean active) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        User user = UserUtil.findByPrimaryKey(userId);
        user.setActive(active);
        UserUtil.update(user);
        return user;
    }

    public User updateSkin(String userId, String skinId) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        User user = UserUtil.findByPrimaryKey(userId);
        Stack undoStack = SkinLocalUtil.getUndoStack(userId);
        undoStack.push(user.getSkin());
        user.setSkinId(skinId);
        UserUtil.update(user);
        return user;
    }

    public User updateUser(String userId, String password1, String password2, boolean passwordReset) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        validate(userId, password1, password2);
        User user = UserUtil.findByPrimaryKey(userId);
        String oldEncPwd = user.getPassword();
        if (!user.isPasswordEncrypted()) {
            oldEncPwd = Encryptor.digest(user.getPassword());
        }
        String newEncPwd = Encryptor.digest(password1);
        int passwordsLifespan = GetterUtil.getInteger(PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));
        Date expirationDate = null;
        if (passwordsLifespan > 0) {
            expirationDate = new Date(System.currentTimeMillis() + Time.DAY * passwordsLifespan);
        }
        if (user.hasCompanyMx()) {
            MailManagerUtil.updatePassword(userId, password1);
        }
        user.setPassword(newEncPwd);
        user.setPasswordEncrypted(true);
        user.setPasswordExpirationDate(expirationDate);
        user.setPasswordReset(passwordReset);
        UserUtil.update(user);
        PasswordTrackerLocalManagerUtil.trackPassword(userId, oldEncPwd);
        return user;
    }

    public User updateUser(String userId, String password, String firstName, String middleName, String lastName, String nickName, boolean male, Date birthday, String emailAddress, String smsId, String aimId, String icqId, String msnId, String ymId, String favoriteActivity, String favoriteBibleVerse, String favoriteFood, String favoriteMovie, String favoriteMusic, String languageId, String timeZoneId, String skinId, boolean dottedSkins, boolean roundedSkins, String greeting, String resolution, String refreshRate, String comments) throws PortalException, SystemException {
        userId = userId.trim().toLowerCase();
        emailAddress = emailAddress.trim().toLowerCase();
        validate(userId, firstName, lastName, emailAddress, smsId);
        User user = UserUtil.findByPrimaryKey(userId);
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setNickName(nickName);
        user.setMale(male);
        user.setBirthday(birthday);
        if (!emailAddress.equals(user.getEmailAddress())) {
            if (!user.hasCompanyMx() && user.hasCompanyMx(emailAddress)) {
                MailManagerUtil.addUser(userId, password, firstName, middleName, lastName, emailAddress);
            } else if (user.hasCompanyMx() && user.hasCompanyMx(emailAddress)) {
                MailManagerUtil.updateEmailAddress(userId, emailAddress);
            } else if (user.hasCompanyMx() && !user.hasCompanyMx(emailAddress)) {
                MailManagerUtil.deleteEmailAddress(userId);
            }
            user.setEmailAddress(emailAddress);
        }
        user.setSmsId(smsId);
        user.setAimId(aimId);
        user.setIcqId(icqId);
        user.setMsnId(msnId);
        user.setYmId(ymId);
        user.setFavoriteActivity(favoriteActivity);
        user.setFavoriteBibleVerse(favoriteBibleVerse);
        user.setFavoriteFood(favoriteFood);
        user.setFavoriteMovie(favoriteMovie);
        user.setFavoriteMusic(favoriteMusic);
        user.setLanguageId(languageId);
        user.setTimeZoneId(timeZoneId);
        user.setSkinId(skinId);
        user.setDottedSkins(dottedSkins);
        user.setRoundedSkins(roundedSkins);
        user.setGreeting(greeting);
        user.setResolution(resolution);
        user.setRefreshRate(refreshRate);
        user.setComments(comments);
        UserUtil.update(user);
        return user;
    }

    public void validate(String companyId, boolean autoUserId, String userId, boolean autoPassword, String password1, String password2, String firstName, String lastName, String emailAddress) throws PortalException, SystemException {
        if (Validator.isNull(firstName)) {
            throw new UserFirstNameException();
        } else if (Validator.isNull(lastName)) {
            throw new UserLastNameException();
        }
        if (!autoUserId) {
            if (Validator.isNull(userId)) {
                throw new UserIdException();
            }
            UserIdValidator userIdValidator = (UserIdValidator) InstancePool.get(PropsUtil.get(PropsUtil.USERS_ID_VALIDATOR));
            if (!userIdValidator.validate(userId, companyId)) {
                throw new UserIdException();
            }
            String[] anonymousNames = PrincipalSessionBean.ANONYMOUS_NAMES;
            for (int i = 0; i < anonymousNames.length; i++) {
                if (userId.equalsIgnoreCase(anonymousNames[i])) {
                    throw new UserIdException();
                }
            }
            String[] companyIds = PortalInstances.getCompanyIds();
            for (int i = 0; i < companyIds.length; i++) {
                if (userId.indexOf(companyIds[i]) != -1) {
                    throw new UserIdException();
                }
            }
            try {
                User user = UserUtil.findByPrimaryKey(userId);
                if (user != null) {
                    throw new DuplicateUserIdException();
                }
            } catch (NoSuchUserException nsue) {
            }
            UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(companyId);
            if (userConfig.hasReservedUserId(userId)) {
                throw new ReservedUserIdException();
            }
        }
        if (!Validator.isEmailAddress(emailAddress)) {
            throw new UserEmailAddressException();
        } else {
            try {
                User user = UserUtil.findByC_EA(companyId, emailAddress);
                if (user != null) {
                    throw new DuplicateUserEmailAddressException();
                }
            } catch (NoSuchUserException nsue) {
            }
            UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(companyId);
            if (userConfig.hasReservedUserEmailAddress(emailAddress)) {
                throw new ReservedUserEmailAddressException();
            }
        }
        if (!autoPassword) {
            if (!password1.equals(password2)) {
                throw new UserPasswordException(UserPasswordException.PASSWORDS_DO_NOT_MATCH);
            } else if (!PwdToolkitUtil.validate(password1) || !PwdToolkitUtil.validate(password2)) {
                throw new UserPasswordException(UserPasswordException.PASSWORD_INVALID);
            }
        }
    }

    public void validate(String userId, String password1, String password2) throws PortalException, SystemException {
        if (!password1.equals(password2)) {
            throw new UserPasswordException(UserPasswordException.PASSWORDS_DO_NOT_MATCH);
        } else if (!PwdToolkitUtil.validate(password1) || !PwdToolkitUtil.validate(password2)) {
            throw new UserPasswordException(UserPasswordException.PASSWORD_INVALID);
        } else if (!PasswordTrackerLocalManagerUtil.isValidPassword(userId, password1)) {
            throw new UserPasswordException(UserPasswordException.PASSWORD_ALREADY_USED);
        }
    }

    public void validate(String userId, String firstName, String lastName, String emailAddress, String smsId) throws PortalException, SystemException {
        if (Validator.isNull(firstName)) {
            throw new UserFirstNameException();
        } else if (Validator.isNull(lastName)) {
            throw new UserLastNameException();
        }
        User user = UserUtil.findByPrimaryKey(userId);
        if (!Validator.isEmailAddress(emailAddress)) {
            throw new UserEmailAddressException();
        } else {
            try {
                if (!user.getEmailAddress().equals(emailAddress)) {
                    if (UserUtil.findByC_EA(user.getCompanyId(), emailAddress) != null) {
                        throw new DuplicateUserEmailAddressException();
                    }
                }
            } catch (NoSuchUserException nsue) {
            }
            UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(user.getCompanyId());
            if (userConfig.hasReservedUserEmailAddress(emailAddress)) {
                throw new ReservedUserEmailAddressException();
            }
        }
        if (Validator.isNotNull(smsId) && !Validator.isEmailAddress(smsId)) {
            throw new UserSmsException();
        }
    }

    private User _updateSkin(User user, Skin skin) throws PortalException, SystemException {
        if (user.getUserId().equals(skin.getSkinId())) {
            Layer alphaLayer = skin.getAlpha();
            Layer betaLayer = skin.getBeta();
            Layer gammaLayer = skin.getGamma();
            Layer bgLayer = skin.getBg();
            SkinLocalManagerUtil.updateSkin(skin.getSkinId(), skin.getName(), alphaLayer.getBackground(), alphaLayer.getForeground(), betaLayer.getBackground(), betaLayer.getForeground(), gammaLayer.getBackground(), gammaLayer.getForeground(), bgLayer.getBackground(), bgLayer.getForeground(), alphaLayer.getHref(), alphaLayer.getNegAlert(), alphaLayer.getPosAlert());
        }
        user.setSkinId(skin.getSkinId());
        UserUtil.update(user);
        return user;
    }
}
