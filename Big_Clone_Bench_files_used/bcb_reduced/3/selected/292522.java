package org.jairou.util;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.Random;
import org.jairou.core.Objects;
import org.jairou.core.XML;
import sun.misc.BASE64Encoder;

/**
 * Common utility providing core Security support
 * and uniform access to Security variables
 * <br/>
 * Security properties can be configured in jairou.xml
 * using the following xml configuration: 
 *  <pre>{@code
 * 	<security algorithm="SHA-1" user-key="session.user" permissions-key="permissions"/>
 *  }</pre>
 * Leverages user session based security.  The primary functions are:
 * <pre>
 * 	Security.authenticated() - checks the given context for the xml configured user-key property
 *  Security.authorized() - checks the given context to see if the specified permission is contained 
 *  						within the the permissions object.  
 *  Security.encrypt() - encrypts a plain text string using the xml defined algorithm
 *  Security.randomPassword() - generates a random combination of letters and numbers
 *  						    in the specified length
 * </pre>
 * @author Roger Ramia
 */
public class Security {

    private static final String letters = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ";

    /**
	 * given a plaintext String
	 * encrypts using the algorithm 
	 * defined in security xml
	 * if no algorithm is defined uses
	 * SHA-1 hash
	 * 
	 * returns the encrypted hash as a String
	 * 
	 * @param plaintext
	 * @return encrypted string
	 * @throws Exception
	 */
    public static String encrypt(String plaintext) throws Exception {
        String algorithm = XML.get("security.algorithm");
        if (algorithm == null) algorithm = "SHA-1";
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(plaintext.getBytes("UTF-8"));
        return new BASE64Encoder().encode(md.digest());
    }

    /**
	 * checks to see of the xml specified
	 * user-key exists in the current context.
	 * If the user-key property is found and not null, 
	 * the assumption is that the user has an active
	 * authenticated session.
	 * 
	 * @return - boolean indicating if the user is authenticated
	 * @throws Exception
	 */
    public static boolean authenticated(Object ctx) {
        if (ctx != null) return (Objects.find(XML.get("security.user-key"), ctx) != null); else return false;
    }

    /**
	 * checks if the given current user object
	 * contains the specified permission within it's permissions list
	 * <br/>
	 * Permissions are assumed to be a collection of Strings accessible via
	 * a public getter (or key) defined on the user object (or map)
	 * <br/>
	 * The user object is obtained from the provided context using the 
	 * xml configured user-key property.  Then the permission collection
	 * is obtained using the permission-key property of the user object
	 *
	 * 
	 * @param permission - string representing the permission required
	 * @return - true if the specified permission exists within 
	 * 			 the context user object
	 * @throws Exception
	 */
    public static boolean authorized(String permission, Object ctx) throws SecurityException {
        try {
            if (ctx != null) {
                Object user = Objects.find(XML.get("security.user-key"), ctx);
                if (user != null) {
                    Object permissions = Objects.get(XML.get("security.permissions-key"), user);
                    if (permissions != null) {
                        if (permissions instanceof Collection) return ((Collection) permissions).contains(permission); else return Objects.containsKey(permission, permissions);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    /**
	 * returns a random alpha numeric password
	 * of a specified size
	 * @param int - length of the string to be generated
	 * @return - random alpha numeric string
	 */
    public static String randomPassword(int length) {
        Random random = new Random(System.currentTimeMillis());
        StringBuffer pswd = new StringBuffer();
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) pswd.append(letters.charAt(random.nextInt(letters.length()))); else pswd.append(random.nextInt(10));
        }
        return pswd.toString();
    }
}
