package ejb;

import hibernate.User;
import hibernate.UserDAO;
import hibernate.UserRole;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 * @author bezpechnyi
 */
@Stateless
@LocalBean
public class Security {

    @EJB
    private UserDAO userManager;

    private static MessageDigest digest = null;

    private static int PASSWORD_MIN_LENGTH = 3;

    public Security() {
    }

    public User getCurrentUser() {
        return new UserSession().getCurrentUser();
    }

    /**
     * Проверяет, авторизировался ли пользователь?
     * @return true - авторизировался, false - не авторизировался
     */
    public boolean isAuthorized() {
        return new UserSession().isLogedIn();
    }

    /**
     * Пытается авторизаровать пользователя по логину и паролю.
     * @param login Логин
     * @param password Пароль
     * @return Успешность авторизации
     */
    public boolean authorize(String login, String password) {
        new UserSession().setCurrentUser(userManager.getUser(login, password));
        return isAuthorized();
    }

    /**
     * Разавторизирует пользователя.
     */
    public void logOut() {
        new UserSession().logout();
    }

    /**
     * Создает новый пустой объект User
     * @return User <b>NOT NULLABLE</b>
     */
    public User createNewUser() {
        return userManager.createEmptyUser();
    }

    /**
     * Зарегистрировать Пользователя, используя параметры
     * @param login
     * @param password
     * @param firstName
     * @param lastName
     * @param email
     * @param role
     * @return <b>true</b> - регистрация прошла успешно. <b>false</b> - произошла ошибка
     */
    public boolean registerNew(String login, String password, String firstName, String lastName, String email, UserRole role) {
        User user = userManager.createEmptyUser();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setLogin(login);
        user.setPassword(password);
        user.setRole(role);
        return registerNew(user);
    }

    /**
     * Зарегистрировать Пользователя, используя данные из user. Перед этим нужно вызвать метод CreateNewUser().
     * @param user
     * @return <b>true</b> - регистрация прошла успешно. <b>false</b> - произошла ошибка
     */
    public boolean registerNew(User user) {
        if (userManager.getUserByLogin(user.getLogin()) != null) {
            return false;
        }
        String pass = user.getPassword();
        if (pass.length() < PASSWORD_MIN_LENGTH) {
            return false;
        }
        pass = md5(pass);
        user.setPassword(pass);
        if (pass == null) {
            return false;
        }
        if (userManager.addAndSaveNewUser(user) == false) {
            return false;
        }
        new UserSession().setCurrentUser(user);
        return true;
    }

    private static String md5(String str) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        byte[] bs = digest.digest(str.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bs.length; i++) {
            sb.append(Integer.toString((bs[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Метод позволяет получить участников системы.
     * Запрашивать можно только пользователей с ролями ниже текущего. Иначе функция вернет <b>NULL</b>.
     * Если Текущий пользователь имеет права пользователя или гостя, то вернется <b>NULL</b>.
     * @param role Роль, по которой необходимо сделать выборку
     * @return Список пользователей <b>NULLABLE</b>
     */
    public List<User> getUsersByRole(UserRole role) {
        return userManager.getUsersByRole(role);
    }

    public User getUserByID(int userID) {
        UserRole currentRole = getCurrentUser().getRole();
        if (currentRole.equals(UserRole.GET_MANAGER_ROLE()) || currentRole.equals(UserRole.GET_PRESIDENT_ROLE())) {
            return userManager.getUserByID(userID);
        }
        return null;
    }

    public List<User> getAllUsers() {
        UserRole currentRole = getCurrentUser().getRole();
        if (currentRole.equals(UserRole.GET_MANAGER_ROLE()) || currentRole.equals(UserRole.GET_PRESIDENT_ROLE())) {
            return userManager.getAllUsers();
        }
        return null;
    }

    @Deprecated
    public UserRole getUserRole() {
        return userManager.getUserRoleByID(1);
    }

    @Deprecated
    public UserRole getManagerRole() {
        return userManager.getUserRoleByID(2);
    }

    @Deprecated
    public UserRole getPresidentRole() {
        return userManager.getUserRoleByID(3);
    }

    @Deprecated
    public UserRole getGuestRole() {
        return userManager.getUserRoleByID(4);
    }
}
