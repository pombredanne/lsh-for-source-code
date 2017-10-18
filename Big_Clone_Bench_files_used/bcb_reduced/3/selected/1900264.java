package containers;

import java.security.MessageDigest;
import java.util.Date;
import java.util.Random;
import sun.misc.BASE64Encoder;

/**
 * Ta klasa reprezentuje token niezbędny do zmiany hasła przez użytkownika, w
 * przypadku gdy zostanie zapomniane. Token jest ważny tylko przez pewien okres,
 * po czym jest usuwany z bazy (lub w przypadku wykorzystania).
 * 
 * @author zulu
 * 
 */
public class Token {

    /**
   * Każdy token może być użyty tylko i wyłącznie przez jednego użytkownika
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
   * Wartość tokena jest generowana losowo, mozna np zrobic md5(losowa
   * wartosc+data+cos tam) wtedy otrzymamy losowy ciag 32 znakowy
   * 
   * @uml.property name="tokenValue"
   */
    private String tokenValue = "";

    /**
   * Getter of the property <tt>tokenValue</tt>
   * 
   * @return Returns the tokenValue.
   * @uml.property name="tokenValue"
   */
    public String getTokenValue() {
        return tokenValue;
    }

    /**
   * Setter of the property <tt>tokenValue</tt>
   * 
   * @param tokenValue
   *          The tokenValue to set.
   * @uml.property name="tokenValue"
   */
    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    /**
   * Jeśli token nie zostanie wykorzystany do tego czasu to traci ważność i
   * zostaje usunięty z bazy danych
   * 
   * @uml.property name="expires"
   */
    private Date expires;

    /**
   * Getter of the property <tt>exires</tt>
   * 
   * @return Returns the expires.
   * @uml.property name="expires"
   */
    public Date getExpires() {
        return expires;
    }

    /**
   * Setter of the property <tt>exires</tt>
   * 
   * @param exires
   *          The exires to set.
   * @uml.property name="expires"
   */
    public void setExpires(Date expires) {
        this.expires = expires;
    }

    /**
   * Id field for Hibernate to use it as primary key
   */
    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return (this.id);
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
   * Ustawia date kiedy zdarzenie powinno być obsłużone na data aktualna +
   * seconds sekund
   * 
   * @param seconds
   *          na tyle sekund do przodu ustawia date zdarzenia
   * @author zulu
   */
    public void setInterval(long seconds) {
        Date expires = new Date();
        expires.setTime(expires.getTime() + seconds * 1000);
        this.setExpires(expires);
    }

    /**
   * Generuje losowy token ważny przez najbliższe 24 godziny
   * 
   * @param user
   * @return
   */
    public static Token generateToken(User user) {
        Token token = new Token();
        token.setInterval(3600 * 24);
        token.setUser(user);
        Random randomGenerator = new Random();
        token.setTokenValue(hash(new Date() + Double.toString(randomGenerator.nextDouble())));
        return token;
    }
}
