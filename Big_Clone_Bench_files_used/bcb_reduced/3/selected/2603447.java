package model.beans;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.List;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import model.connections.database.Users;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

public class LoginCheck {

    private String username;

    private String password;

    private Users user;

    public boolean isLoginValid() {
        return user != null;
    }

    public int getUserID() {
        if (user != null) {
            return DataObjectUtils.intPKForObject(user);
        }
        return 0;
    }

    public Users getUser() {
        if (user == null) {
        }
        return user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSHA1Hex(String original) {
        MessageDigest digest;
        String result = "";
        try {
            digest = MessageDigest.getInstance("sha1");
            digest.reset();
            digest.update(original.getBytes());
            byte[] dig = digest.digest();
            String hexStr = "";
            for (int i = 0; i < dig.length; i++) {
                hexStr += Integer.toString((dig[i] & 0xff) + 0x100, 16).substring(1);
            }
            result = hexStr;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Check if the entered values match a user in the database
	 * @return
	 */
    public String validateLogin() {
        DataContext context = DataContext.createDataContext();
        if (username != "" && password != "") {
            if (doLdapLogin(username, password)) {
                Expression exp = ExpressionFactory.matchExp(Users.LOGIN_PROPERTY, username);
                SelectQuery query = new SelectQuery(Users.class, exp);
                List<Users> users = context.performQuery(query);
                if (users.size() == 1) {
                    user = users.get(0);
                    return "login-success";
                } else {
                    username = "";
                    password = "";
                }
            }
        }
        return "error";
    }

    private boolean doLdapLogin(String username, String password) {
        Hashtable<String, String> authEnv = new Hashtable<String, String>(11);
        String base = "ou=People, o=tsisolutions";
        String dn = "uid=" + username + "," + base;
        String ldapURL = "ldap://fds.int.traserv.com:389";
        authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        authEnv.put(Context.PROVIDER_URL, ldapURL);
        authEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        authEnv.put(Context.SECURITY_PRINCIPAL, dn);
        authEnv.put(Context.SECURITY_CREDENTIALS, password);
        try {
            DirContext authContext = new InitialDirContext(authEnv);
            authContext.close();
            System.out.println("Authentication Success!");
        } catch (AuthenticationException authEx) {
            System.out.println("Authentication failed!");
        } catch (NamingException namEx) {
            System.out.println("Something went wrong!");
            namEx.printStackTrace();
        }
        return true;
    }

    public void logoutSession() {
        username = "";
        password = "";
        user = null;
    }
}
