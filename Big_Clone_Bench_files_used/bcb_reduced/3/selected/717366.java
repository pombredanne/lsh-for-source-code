package ch.olsen.servicecontainer.internalservice.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ch.olsen.servicecontainer.commongwt.client.RegistrationExceptionException;
import ch.olsen.servicecontainer.commongwt.client.SessionException;
import ch.olsen.servicecontainer.commongwt.client.UserRoleAndService;
import ch.olsen.servicecontainer.domain.SCDomain;
import ch.olsen.servicecontainer.naming.OsnURI;
import ch.olsen.servicecontainer.service.ServiceInterface;

@ServiceInterface(name = "auth")
public interface AuthInterface {

    public static final String SUPERADMIN = "SC_SuperAdmin";

    public static final String OWNER = "SC_Owner";

    public static final String ADMIN = "SC_Admin";

    /**
	 * this Role is not actually implemented, but checking against this
	 * role calls the service isAnonymousAccessEnabled()
	 */
    public static final String GUEST = "SC_Guest";

    /**
	 * authenticate a user
	 * @param user 
	 * @param password encrtypted using the below Encrypt.encrypt() method
	 * @return the session token. this can be passed around to claim legitimate rights in 
	 * case other services require a login. Other service will pass this to the Auth Service
	 * again to establish the identity of the user
	 */
    String login(String user, String password);

    /**
	 * returns the user associated with the session parameter 
	 */
    User getUser(String session) throws SessionException;

    /**
	 * closes current user session making any further connection attempt invalid
	 * with the same session string
	 */
    void closeSession(String session);

    /**
	 * check inclusion of the logged user to the specified role. 
	 * @param session
	 * @param roleName forbidden value is "SC_SuperAdmin" 
	 * @param uri
	 * @param objId
	 * @return null if not access is not granted
	 */
    UserRoleAndService checkAccess(String session, String roleName, OsnURI uri, String objId);

    UserRoleAndService checkAccess(User user, String roleName, OsnURI uri, String objId);

    /**
	 * create a AccessToken using provided data
	 * @param user
	 * @param owner2
	 * @param domain
	 * @param string
	 * @throws SessionException 
	 */
    void grantAccess(String session, String role, SCDomain domain, String objId) throws SessionException;

    /**
	 * generate a new password for the user and send it by email
	 * @param userName
	 */
    String forgotPassword(String userName);

    String changePassword(String session, String newpwd) throws SessionException;

    void register(String userName, String password, String fullName, String email) throws RegistrationExceptionException;

    String completeRegistration(String activateId) throws RegistrationExceptionException;

    public static final class Encrtypt {

        public static final String enctrypt(String password) {
            MessageDigest md = null;
            byte[] byteHash = null;
            StringBuffer resultString = new StringBuffer();
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("NoSuchAlgorithmException caught!");
                throw new RuntimeException("NoSuchAlgorithmException SHA1");
            }
            md.reset();
            md.update(password.getBytes());
            byteHash = md.digest();
            for (int i = 0; i < byteHash.length; i++) {
                resultString.append(Integer.toHexString(0xFF & byteHash[i]));
            }
            return (resultString.toString());
        }
    }

    /**
	 * Gets all the services where the logged in user has a role defined
	 * @param session
	 * @return
	 * @throws SessionException 
	 */
    UserRoleAndService[] getRolesAndServices(String session) throws SessionException;

    /**
	 * gets all users that have a permission on the selected service
	 * @param session
	 * @param uri
	 * @param objId
	 * @return
	 * @throws SessionException
	 */
    UserRoleAndService[] getRolesForService(String session, String uri, String objId) throws SessionException;

    /**
	 * gets all the roles an user can invite others for
	 * @param session
	 * @param service
	 * @param objId
	 * @param inherit include sub roles in the results
	 * @return
	 * @throws SessionException 
	 */
    String[] getAccessibleRoles(String session, String service, String objId, boolean inherit) throws SessionException;

    UserRoleAndService[] getAccessibleRoles(User user, String service, String objId, boolean inherit) throws SessionException;

    /**
	 * sends an invitation to a user to share a resource
	 * @param session
	 * @param name
	 * @param email
	 * @param role
	 * @param service
	 * @param objId
	 * @throws SessionException 
	 */
    void inviteUser(String session, String name, String email, String role, String service, String objId) throws SessionException;

    /**
	 * activate share request after the user has received the email
	 * @param session
	 * @param id
	 * @throws SessionException 
	 */
    void activateAccess(String session, String id) throws SessionException;

    /**
	 * when a domain is disposed, delete all access control related stuff
	 * @param osnUri
	 */
    void unregisterService(OsnURI osnUri);

    /**
	 * delete the associated access token
	 * @param session
	 * @param rs
	 * @throws SessionException 
	 */
    void deleteToken(String session, UserRoleAndService rs) throws SessionException;

    /**
	 * gets a session associated with the owner of the service
	 * @param osnUri
	 * @return
	 */
    String getOwnerSession(OsnURI osnUri);
}
