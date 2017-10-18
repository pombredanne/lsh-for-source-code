package org.springforge.ldap.service.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.commons.codec.binary.Base64;
import org.springforge.ldap.user.User;
import org.springforge.ldap.service.LdapService;
import org.springforge.ldap.service.impl.connexion.jndiManagerConnection;
import javax.naming.directory.Attributes;

public class LdapServiceImpl implements LdapService {

    private static String dn = "ou=users,dc=forge,dc=org";

    public void create(String email, String pwd, String firstname, String lastname) throws NamingException {
        jndiManagerConnection ldapConnection = new jndiManagerConnection();
        User user = new User(firstname, lastname, email, pwd);
        String ldapDNCreate = "mail=" + user.email + "," + dn;
        try {
            ldapConnection.getLDAPDirContext().createSubcontext(ldapDNCreate, user.getAttributes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ldapConnection.disConnectLDAPConnection(ldapConnection.getLDAPDirContext());
    }

    public void delete(String email) throws NamingException {
        jndiManagerConnection ldapConnection = new jndiManagerConnection();
        DirContext ctx = ldapConnection.getLDAPDirContext();
        ctx.unbind("mail=" + email + "," + dn);
        ctx.close();
    }

    public User getUser(String email) throws NamingException {
        jndiManagerConnection ldapConnection = new jndiManagerConnection();
        String ldapDNToLookFor = "mail=" + email + "," + dn;
        DirContext ctx = ldapConnection.getLDAPDirContext();
        return (User) ctx.lookup(ldapDNToLookFor);
    }

    public String getPassword(String email) throws NamingException {
        return get(email, "userPassword");
    }

    @Override
    public String getFirstName(String email) throws NamingException {
        return get(email, "givenName");
    }

    @Override
    public String getLastName(String email) throws NamingException {
        return get(email, "sn");
    }

    public String getDestList() throws NamingException {
        String DestList = "";
        jndiManagerConnection ldapConnection = new jndiManagerConnection();
        DirContext ctx = ldapConnection.getLDAPDirContext();
        SearchControls portee = new SearchControls();
        portee.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> resultat = ctx.search(dn, "(mail=*)", portee);
        while (resultat != null && resultat.hasMoreElements()) {
            SearchResult sr = (SearchResult) resultat.next();
            Attributes attrs = sr.getAttributes();
            DestList = DestList + attrs.get("mail").get() + ",";
        }
        DestList = DestList.substring(0, DestList.length() - 1);
        return DestList;
    }

    private String get(String needle, String key) throws NamingException {
        jndiManagerConnection ldapConnection = new jndiManagerConnection();
        String ldapDNToLookFor = "mail=" + needle + "," + dn;
        DirContext ctx = ldapConnection.getLDAPDirContext();
        String response = null;
        if (key.equals("userPassword")) {
            response = new String((byte[]) ctx.getAttributes(ldapDNToLookFor).get(key).get());
        } else {
            response = ctx.getAttributes(ldapDNToLookFor).get(key).get().toString();
        }
        return response;
    }

    public boolean authenticate(String username, String password) {
        try {
            jndiManagerConnection connection = new jndiManagerConnection();
            connection.getAuthLDAPDirContext(username, password);
        } catch (NamingException e) {
            return false;
        }
        return true;
    }

    @Override
    public void update(String mail, String email, String pwd, String firstname, String lastname) throws NamingException, NoSuchAlgorithmException, UnsupportedEncodingException {
        jndiManagerConnection connection = new jndiManagerConnection();
        Attributes attrs = new BasicAttributes();
        attrs.put("sn", lastname);
        attrs.put("givenName", firstname);
        attrs.put("cn", firstname + " " + lastname);
        if (!pwd.isEmpty()) {
            MessageDigest sha = MessageDigest.getInstance("md5");
            sha.reset();
            sha.update(pwd.getBytes("utf-8"));
            byte[] digest = sha.digest();
            String hash = Base64.encodeBase64String(digest);
            attrs.put("userPassword", "{MD5}" + hash);
        }
        DirContext ctx = connection.getLDAPDirContext();
        ctx.modifyAttributes("mail=" + mail + "," + dn, DirContext.REPLACE_ATTRIBUTE, attrs);
        if (!mail.equals(email)) {
            String newName = "mail=" + email + "," + dn;
            String oldName = "mail=" + mail + "," + dn;
            ctx.rename(oldName, newName);
        }
    }
}
