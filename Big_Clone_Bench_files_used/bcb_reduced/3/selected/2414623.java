package de.g18.gruppe3.ldapinterface.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import de.g18.gruppe3.common.exception.NotPersistentLDAPModelException;
import de.g18.gruppe3.common.exception.ServiceException;
import de.g18.gruppe3.common.exception.UserAlreadyExistsException;
import de.g18.gruppe3.common.exception.UserNotFoundException;
import de.g18.gruppe3.common.ldap.OrganizationalUnitService;
import de.g18.gruppe3.common.ldap.UserService;
import de.g18.gruppe3.common.model.OrganizationalUnit;
import de.g18.gruppe3.common.model.User;
import de.g18.gruppe3.ldapinterface.service.provider.LDAPServiceProvider;

/**
 * Implementierung des {@link UserService}
 *
 * @author <a href="mailto:kevinhuber.kh@gmail.com">Kevin Huber</a>
 */
public class UserServiceImpl implements UserService {

    private static final LDAPContext LDAP = LDAPContext.getInstance();

    private static final OrganizationalUnitService ouService = LDAPServiceProvider.getInstance().getOrganizationalUnitService();

    @Override
    public User createUser(User aUser) throws ServiceException, UserAlreadyExistsException {
        if (aUser.getLogin().isEmpty() || aUser.getNachname().isEmpty() || aUser.getVorname().isEmpty() || aUser.getOU() == null || aUser.getOU().getName().isEmpty() || aUser.getPasswort().isEmpty()) {
            throw new IllegalArgumentException("User muss komplett gefüllt sein!");
        }
        if (!aUser.getOU().isPersisted()) {
            throw new NotPersistentLDAPModelException(OrganizationalUnit.class);
        }
        Attributes container = new BasicAttributes();
        container.put(new UserAttributeClass());
        container.put(new BasicAttribute("sAMAccountName", aUser.getLogin()));
        container.put(new BasicAttribute("userPrincipalName", aUser.getLogin() + "@" + LDAPContext.DOMAIN_NAME));
        container.put(new BasicAttribute("uid", aUser.getLogin()));
        String md5PasswordHash = "";
        try {
            md5PasswordHash = new String(MessageDigest.getInstance("MD5").digest(aUser.getPasswort().getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e1) {
            throw new IllegalStateException(e1);
        } catch (UnsupportedEncodingException e1) {
            throw new IllegalStateException(e1);
        }
        container.put(new BasicAttribute("userPassword", "{MD5}" + md5PasswordHash));
        container.put(new BasicAttribute("givenName", aUser.getVorname()));
        container.put(new BasicAttribute("sn", aUser.getNachname()));
        container.put(new BasicAttribute("cn", aUser.getVorname() + " " + aUser.getNachname()));
        container.put(new BasicAttribute("userAccountControl", "512"));
        container.put(new BasicAttribute("homeDrive", "H:"));
        container.put(new BasicAttribute("homeDirectory", "\\\\S215-03\\SchülerHomeDir$\\" + aUser.getLogin()));
        aUser.setDistinguishedName(generateUserDN(aUser, aUser.getOU()));
        try {
            LDAP.createSubcontext(aUser.getDistinguishedName(), container);
        } catch (AuthenticationException e) {
            throw new UserAlreadyExistsException();
        } catch (NamingException e) {
            throw new IllegalStateException("Fehler! Benutzer konnte nicht angelegt werden!", e);
        }
        aUser.setPersisted(true);
        return aUser;
    }

    private String generateUserDN(User aUser, OrganizationalUnit aOU) throws NotPersistentLDAPModelException {
        if (!aUser.getOU().isPersisted()) {
            throw new NotPersistentLDAPModelException(OrganizationalUnit.class);
        }
        return "cn=" + aUser.getName() + "," + aOU.getDistinguishedName();
    }

    @Override
    public User getUserByLogin(String aLogin) throws UserNotFoundException {
        SearchControls controlls = new SearchControls();
        String[] resultAttributes = { "sn", "givenName", "dn" };
        controlls.setReturningAttributes(resultAttributes);
        controlls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = "(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + aLogin + "))";
        try {
            NamingEnumeration<SearchResult> resultList = LDAP.search(LDAPContext.DOMAIN_ROOT, filter, controlls);
            if (!resultList.hasMore()) {
                throw new UserNotFoundException(aLogin);
            }
            SearchResult result = resultList.next();
            Attributes attributes = result.getAttributes();
            User user = new User();
            user.setLogin(aLogin);
            user.setVorname((String) attributes.get("givenName").get());
            user.setNachname((String) attributes.get("sn").get());
            user.setDistinguishedName((String) attributes.get("dn").get());
            String ouName = getOUNameFromDN(user.getDistinguishedName());
            try {
                OrganizationalUnit ou = ouService.getOrganizationalUnitByName(ouName);
                user.setOU(ou);
            } catch (ServiceException e) {
                throw new IllegalStateException(e);
            }
            user.setPersisted(true);
            return user;
        } catch (NamingException e) {
            throw new IllegalStateException("Benutzer konnte nicht geladen werden!", e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<User>();
        SearchControls controlls = new SearchControls();
        String[] resultAttributes = { "sn", "givenName", "sAMAccountName", "distinguishedName" };
        controlls.setReturningAttributes(resultAttributes);
        controlls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try {
            NamingEnumeration<SearchResult> resultList = LDAP.search(LDAPContext.DOMAIN_ROOT, "(&(objectCategory=person)(objectClass=user))", controlls);
            while (resultList.hasMore()) {
                SearchResult result = resultList.next();
                Attributes attributes = result.getAttributes();
                String nachname = attributes.get("sn") == null ? "" : (String) attributes.get("sn").get();
                User user = new User();
                user.setLogin((String) attributes.get("sAMAccountName").get());
                user.setVorname((String) attributes.get("givenName").get());
                user.setNachname(nachname);
                user.setDistinguishedName((String) attributes.get("distinguishedName").get());
                String ouName = getOUNameFromDN(user.getDistinguishedName());
                try {
                    OrganizationalUnit ou = ouService.getOrganizationalUnitByName(ouName);
                    user.setOU(ou);
                } catch (ServiceException e) {
                    throw new IllegalStateException(e);
                }
                user.setPersisted(true);
                users.add(user);
            }
        } catch (NamingException e) {
            throw new IllegalStateException("Benutzer konnten nicht geladen werden!", e);
        }
        return users;
    }

    private String getOUNameFromDN(String aDN) {
        int ouIndex = aDN.toUpperCase().indexOf("OU=");
        if (ouIndex < 0) {
            return "";
        }
        String ou = aDN.substring(ouIndex + 3);
        int commaIndex = ou.indexOf(",");
        return commaIndex < 0 ? ou : ou.substring(0, commaIndex);
    }

    @Override
    public void deleteUser(User aUser) throws ServiceException {
        if (!aUser.isPersisted()) {
            throw new NotPersistentLDAPModelException(User.class);
        }
        try {
            LDAP.destroySubcontext(aUser.getDistinguishedName());
        } catch (NamingException e) {
            throw new IllegalStateException("Benutzer konnten nicht gelöscht werden!", e);
        }
    }

    /**
	 * LDAP-Benutzer Klasse
	 */
    private class UserAttributeClass extends BasicAttribute {

        private static final long serialVersionUID = 100L;

        public UserAttributeClass() {
            super("objectClass");
            add("top");
            add("person");
            add("organizationalPerson");
            add("user");
        }
    }
}
