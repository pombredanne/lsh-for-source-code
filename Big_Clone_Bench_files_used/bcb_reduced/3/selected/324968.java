package managers;

import containers.Island;
import containers.Technology;
import containers.Message;
import containers.User;
import containers.Token;
import containers.UserTechnology;
import java.util.Collection;
import java.util.ArrayList;
import containers.World;
import javax.mail.internet.InternetAddress;
import java.security.MessageDigest;
import sun.misc.BASE64Encoder;
import misc.HibernateUtil;
import managers.IslandManager;

/**
 * Zarządza użytkownikiem, obsługuje rejestracje użytkownika, logowanie,
 * przeglądanie wysp, technologii oraz wiadomości.
 * 
 * @author zulu
 * 
 */
public class UserManager {

    /**
   * Użytkownik którym zarządza dany egzemplarz UserManagera
   * 
   * @uml.property name="user"
   */
    private User user;

    /**
   * Getter of the property <tt>user</tt>
   * 
   * @return Returns the user.
   * @uml.property name="user"
   */
    public User getUser() {
        return user;
    }

    /**
   * Setter of the property <tt>user</tt>
   * 
   * @param user
   *          The user to set.
   * @uml.property name="user"
   */
    public void setUser(User user) {
        this.user = user;
    }

    /**
   * Zwraca kolekcję wysp danego użytkownika
   * 
   * @author Tomek
   */
    public Collection<Island> viewIslands() {
        return this.user.getIslands();
    }

    /**
   * Sprawdza czy mail jest poprawny, jeśli tak to zmienia maila danego
   * użytkownika oraz zwraca true, w przeciwnym razie nic nie robi i zwraca
   * false
   * 
   * @param newEmail
   * @return
   */
    public boolean changeEmail(String newEmail) {
        try {
            InternetAddress address = new InternetAddress(newEmail, true);
        } catch (Exception e) {
            return false;
        }
        this.user.setEmail(newEmail);
        return true;
    }

    /**
   * Sprawdza czy hasło ma minimalną długość 6 znaków, jeśli tak to zapisuje
   * zhaszowane hasło i zwraca true, w przeciwnym razie nic nie robi i zwraca
   * false
   * 
   * @param newPassword
   */
    public boolean changePassword(String newPassword) {
        if (newPassword != null && newPassword.length() > 5) {
            this.user.setPassword(UserManager.hash(newPassword));
            return true;
        }
        return false;
    }

    /**
   * Zmienia hasło użytkownika o podanym loginie tylko jeśli przesłano
   * odpowiedni token - token jest generowany i zapisywany do bazy przez metodę
   * remindPassword. Zwraca prawdę gdy hasło zostało zmienione, fałsz w
   * przeciwnym razy (gdy token się nie zgadza). Jeśli hasło zostanie zmienione
   * token jest usuwany z bazy
   * 
   * @param login
   * @param token
   * @param newPassword
   * @return
   */
    public static boolean changePassword(String login, String tokenValue, String newPassword) {
        Token token = World.getInstance().validateToken(login, tokenValue);
        if (token != null) {
            new UserManager(token.getUser()).changePassword(newPassword);
            World.getInstance().getTokens().remove(token);
            return true;
        }
        return false;
    }

    /**
   * Sprawdza czy nazwa jest niepusta, jeśli tak to zmienia nazwę użytkownika i
   * zwraca true, w przeciwnym razie zwraca false
   * 
   * @param newName
   * @return
   */
    public boolean changeName(String newName) {
        if (newName != null && newName.length() >= 3) {
            this.user.setName(newName);
            return true;
        }
        return false;
    }

    /**
   * Do konstruktora przekazywany jest użytkownik, którym ma zarządzać dany
   * egzemplarz UserManagera
   * 
   * @param user
   * @author Tomek
   */
    public UserManager(User user) {
        this.user = user;
    }

    /**
   * Zwraca listę wiadomości danego użytkownika
   * 
   * @return java.util.Collection<containers.Message> Lista wiadomości
   * @author Tomek
   */
    public Collection<Message> viewMessages() {
        return this.user.getMessages();
    }

    /**
   * Usuwa wiadomość Message z listy wiadomości danego użytkownika.
   * 
   * @param containers.Message
   *          Wiadomość do usunięcia
   * @author Tomek
   */
    public void deleteMessage(Message message) {
        this.user.deleteMessage(message);
    }

    /**
   * Statyczna (aby otrzymać obiekt konkretnego użytkownika przed stworzeniem
   * UserManagera) metoda obsługująca logowanie do systemu.
   * 
   * @param login
   *          Nazwa użytkownika (login)
   * @param password
   *          Hasło użytkownika
   * @return Zwraca obiekt reprezentujący użytkownika w przypadku pomyślnego
   *         logowania lub null w przeciwnym razie
   */
    public static User login(String login, String password) {
        for (User user : World.getInstance().getUsers()) {
            if (user.getLogin().equals(login) && user.getPassword().equals(UserManager.hash(password))) return user;
        }
        return null;
    }

    /**
   * Wysyła maila z tokenem do użytkownika (moze byc w postaci linku i/lub
   * tekstu), który będzie potrzebny do zmiany zapomnianego hasła
   * 
   * @param login
   */
    public static void remindPassword(String login) {
        for (User user : World.getInstance().getUsers()) if (user.getLogin().equals(login)) {
            Token token = Token.generateToken(user);
            World.getInstance().getTokens().add(token);
            String activateLink = "http://adresserwa/index.jsp?module=changePasswd&token=" + token.getTokenValue() + "&login=" + user.getLogin();
            MessageManager.sendMail("Zmiana hasła", "Witaj " + user.getName() + "!\n\nAby zmienić hasło do twojego konta (" + user.getLogin() + ") kliknij poniższy link:\n" + activateLink, user.getEmail());
            return;
        }
    }

    /**
   * Funkcja haszująca łańcuch znaków - używa fkcji SHA1
   * 
   * @param string
   * @return
   */
    private static String hash(String string) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            return null;
        }
        try {
            md.update(string.getBytes("UTF-8"));
        } catch (Exception e) {
            return null;
        }
        byte raw[] = md.digest();
        return (new BASE64Encoder()).encode(raw);
    }

    /**
   * Waliduje najpierw wprowadzone dane, jeśli wszystko jest ok, dodaje
   * użytkownika do bazy oraz wysyła mailem tokena oraz zwraca null, jeśli
   * jakieś dane są niepoprawne to nic nie robi i zwraca tablicę String[]
   * komunikatów opisujących błędy
   * 
   * @param login
   * @param name
   * @param email
   * @param password
   * @return
   */
    public static String[] register(String login, String name, String email, String password) {
        ArrayList<String> errorMsg = new ArrayList<String>();
        if (login == null || login.length() < 3) errorMsg.add("Login musi składać się przynajmniej z 3 znaków"); else for (User user : World.getInstance().getUsers()) if (login.equals(user.getLogin())) {
            errorMsg.add("Ten login jest już zajęty");
            break;
        }
        if (password == null || password.length() < 5) errorMsg.add("Hasło powinno składać się przynajmniej z 5 znaków");
        if (name == null || name.length() < 3) errorMsg.add("Nazwa powinna składać się przynajmniej z 3 znaków");
        try {
            InternetAddress address = new InternetAddress(email, true);
        } catch (Exception e) {
            errorMsg.add("Błędny adres email");
        }
        if (errorMsg.size() > 0) {
            String[] result = new String[errorMsg.size()];
            int i = 0;
            for (String s : errorMsg) {
                result[i++] = s;
            }
            return result;
        }
        UserManager.addUser(login, name, email, password);
        return null;
    }

    /**
   * Dodaje nowego użytkownika do systemu - inicjalizuje parametry początkowe
   * (wyspę, technologie itp)
   * 
   * @param login
   * @param name
   * @param email
   * @param password
   */
    private static void addUser(String login, String name, String email, String password) {
        User user = new User();
        user.setId(HibernateUtil.getDefaultIdFor(user));
        UserManager manager = new UserManager(user);
        manager.changeEmail(email);
        manager.changePassword(password);
        manager.changeName(name);
        user.setLogin(login);
        user.setBanned(true);
        user.setTechnologies(new ArrayList<UserTechnology>());
        for (Technology technology : World.getInstance().getTechnologyTemplates()) {
            UserTechnology userTech = new UserTechnology();
            userTech.setTechnology(technology);
            userTech.setLevel(0);
            if (technology.getRequired() == null || technology.getRequired().size() == 0) userTech.setAvailable(true); else userTech.setAvailable(false);
            userTech.setId(HibernateUtil.getDefaultIdFor(userTech));
            user.getTechnologies().add(userTech);
        }
        Island motherIsland = World.getInstance().getRandomUninhabitatedIsland();
        motherIsland.setIsMotherIsland(true);
        IslandManager.inhabitIsland(motherIsland, user);
        Token token = Token.generateToken(user);
        World.getInstance().getTokens().add(token);
        String activateLink = "http://adresserwa/index.jsp?token=" + token.getTokenValue() + "&login=" + user.getLogin();
        MessageManager.sendMail("Aktywacja konta", "Witaj " + user.getName() + "!\n\nAby aktywować konto w grze Wasserwelt kliknij poniższy link:\n" + activateLink, user.getEmail());
    }

    /**
   * Aktywuje konto nowo zarejestrowanego użytkownika - sprawdza czy login i
   * token się zgadza, jeśli tak zwraca true, jeśli nie zwraca false i nie
   * aktywkuje konta
   * 
   * @param login
   * @param tokenValue
   * @return
   */
    public boolean activateUser(String login, String tokenValue) {
        Token token = World.getInstance().validateToken(login, tokenValue);
        if (token != null) {
            token.getUser().setBanned(true);
            return true;
        }
        return true;
    }

    /**
   * Wyszukuiwanie wyspy. null jesli nie znalazlo
   */
    public Island findIsland(int islandID) {
        for (Island island : user.getIslands()) {
            if (island.getId() == islandID) return (island);
        }
        return (null);
    }
}
