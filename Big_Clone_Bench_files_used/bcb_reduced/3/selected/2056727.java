package net.sourceforge.buildprocess.autodeploy.model;

import java.io.Serializable;
import java.security.MessageDigest;
import net.sourceforge.buildprocess.autodeploy.AutoDeployException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.w3c.dom.Element;

/**
 * Represents the <code>user</code> tag in the AutoDeploy XML configuration
 * file
 * 
 * @author <a href="mailto:jb@nanthrax.net">Jean-Baptiste Onofr�</a>
 */
public class User implements Serializable, Cloneable {

    /**
    * Generated Serial Version UID
    */
    private static final long serialVersionUID = -1628759131745053332L;

    private static final Log log = LogFactory.getLog(User.class);

    private String id;

    private String name;

    private String email;

    private String password;

    /**
    * Default constructor to create a new <code>User</code>
    */
    public User() {
        log.debug("Create a User object");
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
    * Warning : this method returns the encrypted password
    */
    public String getPassword() {
        return this.password;
    }

    /**
    * Warning : this method is expecting for an encrypted password
    */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
    * MD5 crypt of a given password
    * 
    * @param password
    *           the password to crypt
    * @return the MD5 crypted password
    */
    public static String md5PasswordCrypt(String password) throws AutoDeployException {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(password.getBytes());
            StringBuffer hashString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(hash[i]);
                if (hex.length() == 1) {
                    hashString.append('0');
                    hashString.append(hex.charAt(hex.length() - 1));
                } else {
                    hashString.append(hex.substring(hex.length() - 2));
                }
            }
            return hashString.toString();
        } catch (Exception e) {
            log.error("Can't crypt the password due to an unexpected error : " + e.getMessage());
            throw new AutoDeployException("Cant' crypt the password due to an unexpected error : " + e.getMessage());
        }
    }

    /**
    * Check if a given password match the <code>User</code> password
    * 
    * @param password
    *           the given password
    * @return true of the password match the <code>User</code> password, false
    *         else
    */
    public boolean checkPassword(String password) throws AutoDeployException {
        String crypt = User.md5PasswordCrypt(password);
        if (this.getPassword().equals(crypt)) {
            return true;
        } else {
            return false;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        User clone = new User();
        clone.setId(this.getId());
        clone.setName(this.getName());
        clone.setEmail(this.getEmail());
        clone.setPassword(this.getPassword());
        return clone;
    }

    /**
    * Transform the <code>User</code> POJO to a DOM element
    * 
    * @param document
    *           the XML core document
    * @return the DOM element
    */
    protected Element toDOMElement(CoreDocumentImpl document) {
        ElementImpl element = new ElementImpl(document, "user");
        element.setAttribute("id", this.getId());
        element.setAttribute("name", this.getName());
        element.setAttribute("email", this.getEmail());
        element.setAttribute("password", this.getPassword());
        return element;
    }
}
