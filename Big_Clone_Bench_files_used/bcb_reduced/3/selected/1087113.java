package org.springforge.ldap.user;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SchemaViolationException;
import org.apache.commons.codec.binary.Base64;

public class User {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public String firstName;

    public String lastName;

    public String password;

    public String email;

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public Attributes getAttributes() throws SchemaViolationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        BasicAttributes outAttrs = new BasicAttributes(true);
        BasicAttribute oc = new BasicAttribute("objectclass", "inetOrgPerson");
        oc.add("organizationalPerson");
        oc.add("person");
        outAttrs.put(oc);
        if (lastName != null && firstName != null) {
            outAttrs.put("sn", lastName);
            outAttrs.put("givenName", firstName);
            outAttrs.put("cn", firstName + " " + lastName);
        } else {
            throw new SchemaViolationException("user must have surname");
        }
        if (password != null) {
            MessageDigest sha = MessageDigest.getInstance("md5");
            sha.reset();
            sha.update(password.getBytes("utf-8"));
            byte[] digest = sha.digest();
            String hash = Base64.encodeBase64String(digest);
            outAttrs.put("userPassword", "{MD5}" + hash);
        }
        if (email != null) {
            outAttrs.put("mail", email);
        }
        return (Attributes) outAttrs;
    }
}
