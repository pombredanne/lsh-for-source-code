package org.openjf.usergroup;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.openjf.Board;
import org.openjf.container.ComponentLifecycle;
import org.openjf.exception.BoardException;
import org.openjf.exception.BoardObjectNotFoundException;
import org.openjf.exception.BoardRuntimeException;
import org.openjf.exception.BoardValidationException;
import org.openjf.settings.SettingsControl;
import org.openjf.util.StringUtil;
import org.openjf.util.ValidateUtil;

public class UserControl implements ComponentLifecycle {

    private UserDao dao;

    private SettingsControl settingsControl;

    private GroupControl groupControl;

    private UserInGroupControl userInGroupControl;

    public UserControl(UserDao dao, SettingsControl settingsControl, GroupControl groupControl) {
        this.dao = dao;
        this.settingsControl = settingsControl;
        this.groupControl = groupControl;
    }

    public void start() {
        userInGroupControl = (UserInGroupControl) Board.getComponent(UserInGroupControl.class);
        if (!Board.isInstalled()) {
            try {
                install();
            } catch (BoardException e) {
                throw new BoardRuntimeException("Unable to install initial users", e);
            }
        }
    }

    public void stop() {
    }

    public static final String PROP_SEC_ALLOW_ANONYMOUS = "openjf.security.allowAnonymous";

    public static final String PROP_SEC_CONTROL_PASSWORDS = "openjf.security.controlPasswords";

    public static final String PROP_SEC_CONTROL_LOGIN = "openjf.security.controlLogin";

    public static final String PROP_SEC_LOGIN_PATH = "openjf.security.loginPath";

    public static final String PROP_SEC_LOGOUT_PATH = "openjf.security.logoutPath";

    public static final String PROP_USER_ANONYMOUS_ID = "openjf.user.anonymous.id";

    public static final String PROP_AUTO_NICK = "openjf.user.auto_nick";

    public static final String PROP_CONTROL_USER_REGISTRATION = "openjf.user.allowRegister";

    public static final String PROP_NEW_SHOW_TERMS = "openjf.user.showTerms";

    public static final String PROP_NEW_EMAIL_CONFIRM = "openjf.user.mailConfirm";

    public int getAnonymousId() {
        Integer id = settingsControl.getIntegerSetting(PROP_USER_ANONYMOUS_ID);
        if (id == null) {
            throw new BoardRuntimeException("There is no ID for the anonymous user");
        } else {
            return id.intValue();
        }
    }

    private void setAnonymousId(int id) throws BoardException {
        settingsControl.setIntSetting(PROP_USER_ANONYMOUS_ID, id);
    }

    public boolean getAutoNick() {
        return settingsControl.getBooleanSetting(PROP_AUTO_NICK, true);
    }

    public void setAutoNick(boolean autoNick) throws BoardException {
        settingsControl.setBooleanSetting(PROP_AUTO_NICK, autoNick);
    }

    public boolean getSecurityControlPasswords() {
        return settingsControl.getBooleanSetting(PROP_SEC_CONTROL_PASSWORDS, true);
    }

    public void setSecurityControlPasswords(boolean controlPasswords) throws BoardException {
        settingsControl.setBooleanSetting(PROP_SEC_CONTROL_PASSWORDS, controlPasswords);
    }

    public boolean getSecurityAllowAnonymous() {
        return settingsControl.getBooleanSetting(PROP_SEC_ALLOW_ANONYMOUS, true);
    }

    public void setSecurityAllowAnonymous(boolean value) throws BoardException {
        settingsControl.setBooleanSetting(PROP_SEC_ALLOW_ANONYMOUS, value);
    }

    public boolean getSecurityControlLogin() {
        return settingsControl.getBooleanSetting(PROP_SEC_CONTROL_LOGIN, true);
    }

    public void setSecurityControlLogin(boolean value) throws BoardException {
        settingsControl.setBooleanSetting(PROP_SEC_CONTROL_LOGIN, value);
    }

    public String getSecurityLoginPath() {
        return settingsControl.getSetting(PROP_SEC_LOGIN_PATH, "/logon/login.do");
    }

    public void setSecurityLoginPath(String value) throws BoardException {
        settingsControl.setSetting(PROP_SEC_LOGIN_PATH, value);
    }

    public String getSecurityLogoutPath() {
        return settingsControl.getSetting(PROP_SEC_LOGOUT_PATH, "/logon/logout.do");
    }

    public void setSecurityLogoutPath(String value) throws BoardException {
        settingsControl.setSetting(PROP_SEC_LOGOUT_PATH, value);
    }

    public boolean getShowTerms() {
        return settingsControl.getBooleanSetting(PROP_NEW_SHOW_TERMS, true);
    }

    public void setShowTerms(boolean showTerms) throws BoardException {
        settingsControl.setBooleanSetting(PROP_NEW_SHOW_TERMS, showTerms);
    }

    public boolean getEmailConfirm() {
        return settingsControl.getBooleanSetting(PROP_NEW_EMAIL_CONFIRM, true);
    }

    public void setEmailConfirm(boolean emailConfirm) throws BoardException {
        settingsControl.setBooleanSetting(PROP_NEW_EMAIL_CONFIRM, emailConfirm);
    }

    public boolean getControlUserRegistration() {
        return settingsControl.getBooleanSetting(PROP_CONTROL_USER_REGISTRATION, true);
    }

    public void setControlUserRegistration(boolean controlUserRegistration) throws BoardException {
        settingsControl.setBooleanSetting(PROP_CONTROL_USER_REGISTRATION, controlUserRegistration);
    }

    private static final String USER_ADMIN_NICK = "admin";

    private static final String USER_ADMIN_LOGIN = "admin";

    private static final String USER_ADMIN_PASSWORD = "openjfadmin";

    private static final String USER_ANONYMOUS_NICK = "anonymous";

    private static final String USER_ANONYMOUS_LOGIN = "anonymous";

    public void install() throws BoardException {
        User userAdmin = insertUser(USER_ADMIN_LOGIN, USER_ADMIN_NICK, "");
        updateUserPassword(userAdmin.getId(), USER_ADMIN_PASSWORD);
        updateUserAdminStatus(userAdmin.getId(), true);
        updateUserActiveStatus(userAdmin.getId(), true);
        User userAnonymous = insertUser(USER_ANONYMOUS_LOGIN, USER_ANONYMOUS_NICK, "");
        setAnonymousId(userAnonymous.getId());
        updateUserActiveStatus(userAnonymous.getId(), true);
        userInGroupControl.insertUserInGroup(userAdmin.getId(), groupControl.getAdminId());
        userInGroupControl.insertUserInGroup(userAdmin.getId(), groupControl.getRegisteredId());
        userInGroupControl.insertUserInGroup(userAnonymous.getId(), groupControl.getAnonymousId());
    }

    public final int USER_NICK_MIN_LENGTH = 3;

    public final int USER_NICK_MAX_LENGTH = 200;

    public final int USER_PASS_MIN_LENGTH = 6;

    public final int USER_PASS_MAX_LENGTH = 20;

    public final int USER_LOGIN_MIN_LENGTH = 3;

    public final int USER_LOGIN_MAX_LENGTH = 60;

    public User findUserNE(int id) {
        return dao.findNE(id);
    }

    public User findUser(int id) throws BoardObjectNotFoundException {
        User user = findUserNE(id);
        if (user == null) {
            throw new BoardObjectNotFoundException("O usuário de id \"" + id + "\" não existe");
        }
        return user;
    }

    public User findUserByLoginNE(String login) {
        if (login == null) {
            throw new IllegalArgumentException("O login do usuário não deve ser null");
        }
        return dao.findByLoginNE(login);
    }

    public User findUserByLogin(String login) throws BoardObjectNotFoundException {
        User user = findUserByLoginNE(login);
        if (user == null) {
            throw new BoardObjectNotFoundException("O usuário de login \"" + login + "\" não existe");
        }
        return user;
    }

    public User findUserByNickNE(String nick) {
        nick = StringUtil.trimIn(nick);
        if (nick == null) {
            throw new IllegalArgumentException("O nick do usuário não deve ser null");
        }
        return dao.findByNickNE(nick);
    }

    public User findUserByNick(String nick) throws BoardObjectNotFoundException {
        User user = findUserByNickNE(nick);
        if (user == null) {
            throw new BoardObjectNotFoundException("O usuário de nick \"" + nick + "\" não existe");
        }
        return user;
    }

    public User findUserByEmailNE(String email) {
        email = StringUtil.trimIn(email);
        if (email == null) {
            throw new IllegalArgumentException("O e-mail do usuário não deve ser null");
        }
        return dao.findByEmailNE(email);
    }

    public User findUserByEmail(String email) throws BoardObjectNotFoundException {
        User user = findUserByEmailNE(email);
        if (user == null) {
            throw new BoardObjectNotFoundException("O usuário de e-mail \"" + email + "\" não existe");
        }
        return user;
    }

    /**
     * Busca usuários ordenados por nick 
     */
    public List findUsersByNick(String nick, int firstResult, int maxResults) throws BoardObjectNotFoundException {
        return dao.findUsersByNick(nick, firstResult, maxResults);
    }

    public int getUserCountByNick(String nick) throws BoardObjectNotFoundException {
        return dao.getCountByNick(nick);
    }

    public List findAllUsersSortedById() {
        return dao.findAllSortedById();
    }

    public List findAllUsersSortedByNick() {
        return findAllUsersSortedByNick(-1, -1);
    }

    public int getUserCount() {
        return dao.getCount();
    }

    public List findAllUsersSortedByNick(int first, int maxResults) {
        return dao.findAllSortedByNick(first, maxResults);
    }

    public User findUserAnonymous() throws BoardObjectNotFoundException {
        return findUser(getAnonymousId());
    }

    private void prepareUserToInsertUpdate(User user, String login, String nick, String email, boolean isInsert) throws BoardValidationException {
        login = StringUtil.trimIn(login);
        ValidateUtil.notEmpty(login, "login");
        ValidateUtil.checkLength(login, USER_LOGIN_MIN_LENGTH, USER_LOGIN_MAX_LENGTH, "login");
        User userByLogin = findUserByLoginNE(login);
        if ((userByLogin != null) && (isInsert || (userByLogin.getId() != user.getId()))) {
            throw new BoardValidationException("O usuário com login \"" + login + "\" já existe");
        }
        user.setLogin(login);
        if (!getAutoNick()) {
            ValidateUtil.notEmpty(login, "apelido");
            nick = StringUtil.trimIn(nick);
            ValidateUtil.checkLength(nick, USER_NICK_MIN_LENGTH, USER_NICK_MAX_LENGTH, "apelido");
        }
        if (getAutoNick()) {
            nick = login;
        }
        User userByNick = findUserByNickNE(nick);
        if ((userByNick != null) && (isInsert || (userByNick.getId() != user.getId()))) {
            throw new BoardValidationException("O usuário de apelido \"" + nick + "\" já existe");
        }
        user.setNick(nick);
        email = StringUtil.trimIn(email);
        if (email == null) {
            throw new BoardValidationException("O e-mail do usuário não pode ser nulo");
        }
        if (!Board.isInstalling()) {
            if (email.length() < 3) {
                throw new BoardValidationException("O e-mail do usuário deve conter pelo menos 3 caracteres");
            }
        }
        user.setEmail(email);
        if (isInsert) {
            user.setViewAllPostsTime(new Timestamp(0));
        }
    }

    public User insertUser(String login, String nick, String email) throws BoardException {
        User user = new User();
        user.setEncryptedPassword("");
        user.setActive(true);
        user.setViewAllPostsTime(new Timestamp(0));
        prepareUserToInsertUpdate(user, login, nick, email, true);
        dao.save(user);
        if (!Board.isInstalling()) {
            userInGroupControl.insertUserInGroup(user.getId(), groupControl.getRegisteredId());
        }
        return user;
    }

    public void updateUser(int userId, String login, String nick, String email) throws BoardException {
        User user = findUser(userId);
        prepareUserToInsertUpdate(user, login, nick, email, false);
    }

    public void deleteUser(int userId) throws BoardException {
        User user = findUser(userId);
        if (!Board.isDeletingAll()) {
            if (user.isAdmin()) {
                throw new BoardValidationException("Não posso excluir nenhum administrador");
            } else if (userId == getAnonymousId()) {
                throw new BoardValidationException("Não posso excluir o usuários especial anônimo");
            }
        }
        userInGroupControl.setIsDeletingUser(true);
        try {
            dao.delete(user);
        } finally {
            userInGroupControl.setIsDeletingUser(false);
        }
    }

    public void clearUserLoginInfo(int userId) throws BoardException {
        User user = findUser(userId);
        user.setLoginId(null);
        user.setLoginExpiration(null);
    }

    public String setNewLoginId(int userId) throws BoardException {
        User user = findUser(userId);
        String loginId = RandomStringUtils.randomAlphanumeric(30);
        user.setLoginId(loginId);
        return loginId;
    }

    public void setUserLoginExpiration(int userId, Timestamp expiration) throws BoardException {
        User user = findUser(userId);
        user.setLoginExpiration(expiration);
    }

    public boolean isLoginExpired(User user, Date now) {
        String loginId = user.getLoginId();
        Timestamp expiration = user.getLoginExpiration();
        return (StringUtils.isEmpty(loginId) || expiration == null || expiration.before(now));
    }

    public String setNewEmailToken(int userId) throws BoardException {
        User user = findUser(userId);
        String token = RandomStringUtils.randomAlphanumeric(30);
        user.setEmailToken(token);
        return token;
    }

    public void clearEmailToken(int userId) throws BoardException {
        User user = findUser(userId);
        user.setEmailToken(null);
    }

    public void updateUserActiveStatus(int userId, boolean active) {
        User user = findUser(userId);
        user.setActive(active);
    }

    public void updateUserAdminStatus(int userId, boolean admin) {
        User user = findUser(userId);
        user.setAdmin(admin);
    }

    public void updateUserViewAllPostsTime(int userId, Timestamp viewAllPostsTime) throws BoardException {
        if (viewAllPostsTime == null) {
            throw new BoardValidationException("O dado \"viewAllPostsTime\" não deve ser nulo");
        }
        User user = findUser(userId);
        user.setViewAllPostsTime((Timestamp) viewAllPostsTime.clone());
    }

    private String getHexadecimalValue1(int i) {
        return "" + Character.forDigit(i, 16);
    }

    private String getHexadecimalValue2(int i) {
        return getHexadecimalValue1(i / 16) + getHexadecimalValue1(i % 16);
    }

    private String encryptUserPassword(int userId, String password) {
        password = password.trim();
        if (password.length() == 0) {
            return "";
        } else {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException ex) {
                throw new BoardRuntimeException(ex);
            }
            md.update(String.valueOf(userId).getBytes());
            md.update(password.getBytes());
            byte b[] = md.digest();
            StringBuffer sb = new StringBuffer(1 + b.length * 2);
            for (int i = 0; i < b.length; i++) {
                int ii = b[i];
                if (ii < 0) {
                    ii = 256 + ii;
                }
                sb.append(getHexadecimalValue2(ii));
            }
            return sb.toString();
        }
    }

    public String getUserEncryptedPassword(int userId) throws BoardException {
        User user = findUser(userId);
        return user.getEncryptedPassword();
    }

    public void updateUserPassword(int userId, String password) throws BoardException {
        if (password == null) {
            throw new BoardValidationException("A senha não pode ser nula.");
        }
        ValidateUtil.notEmpty(password, "senha");
        ValidateUtil.minLength(password, 6, "senha");
        password = password.trim();
        User user = findUser(userId);
        String encryptedPassword = encryptUserPassword(userId, password);
        user.setEncryptedPassword(encryptedPassword);
    }

    public boolean verifyUserPassword(int userId, String password) throws BoardException {
        if (password == null) {
            throw new BoardValidationException("A senha não pode ser nula.");
        }
        String encryptedPasswordOfUser = getUserEncryptedPassword(userId);
        String encryptedPassword = encryptUserPassword(userId, password);
        return encryptedPasswordOfUser.equals(encryptedPassword);
    }
}
