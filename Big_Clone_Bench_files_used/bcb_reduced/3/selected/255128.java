package com.jtstand;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.xml.bind.annotation.*;
import javax.persistence.*;

/**
 *
 * @author albert_kurucz
 */
@Entity
@XmlType
@XmlAccessorType(value = XmlAccessType.PROPERTY)
public class Authentication implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private FileRevision creator;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private DomainUsers domainUsers;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private LocalUsers localUsers;

    private transient String operator;

    @OneToOne(mappedBy = "authentication")
    private TestProject testProject;

    @XmlTransient
    public TestProject getTestProject() {
        return testProject;
    }

    public void setTestProject(TestProject testProject) {
        this.testProject = testProject;
        if (testProject != null) {
            setCreator(testProject.getCreator());
        }
    }

    public enum AuthenticationMode {

        PASSWORD, NO_PASSWORD
    }

    ;

    private AuthenticationMode authenticatonMode = null;

    @XmlAttribute
    public AuthenticationMode getAuthenticatonMode() {
        return authenticatonMode;
    }

    public void setAuthenticatonMode(AuthenticationMode authenticatonMode) {
        this.authenticatonMode = authenticatonMode;
    }

    @XmlTransient
    public boolean isPassword() {
        return authenticatonMode == null || authenticatonMode.equals(AuthenticationMode.PASSWORD);
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (creator != null ? creator.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Authentication)) {
            return false;
        }
        Authentication other = (Authentication) object;
        if ((this.creator == null && other.creator != null) || (this.creator != null && !this.creator.equals(other.creator))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Authentication.class.getCanonicalName() + "[id=" + id + "]";
    }

    @XmlElement(name = "localUsers")
    public LocalUsers getLocalUsers() {
        return localUsers;
    }

    public void setLocalUsers(LocalUsers localUsers) {
        this.localUsers = localUsers;
        if (localUsers != null) {
            localUsers.setCreator(creator);
        }
    }

    @XmlElement(name = "domainUsers")
    public DomainUsers getDomainUsers() {
        return domainUsers;
    }

    public void setDomainUsers(DomainUsers domainUsers) {
        this.domainUsers = domainUsers;
        if (domainUsers != null) {
            domainUsers.setCreator(creator);
        }
    }

    @XmlTransient
    public FileRevision getCreator() {
        return creator;
    }

    public void setCreator(FileRevision creator) {
        this.creator = creator;
        setLocalUsers(getLocalUsers());
        setDomainUsers(getDomainUsers());
    }

    public static String encryptString(String x) throws NoSuchAlgorithmException {
        return byteArrayToHexString(encrypt(x));
    }

    public static byte[] encrypt(String x) throws NoSuchAlgorithmException {
        MessageDigest d = null;
        d = MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(x.getBytes());
        return d.digest();
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    @XmlTransient
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
        if (operator != null) {
            System.out.println("Operator '" + operator + "' is logged in.");
        } else {
            System.out.println("Operator is logged out.");
        }
    }

    public void login(String username, String password) throws Exception {
        String op = getEmployeeNumber(username, password);
        setOperator(op);
        if (op == null) {
            throw new Exception("Could not login");
        }
    }

    public void logout() {
        setOperator(null);
    }

    public String getEmployeeNumber(String username, String password) {
        if ((localUsers == null || localUsers.getLocalUsers().isEmpty()) && (domainUsers == null || domainUsers.getDomainUsers().isEmpty())) {
            return username;
        }
        if (domainUsers != null) {
            for (DomainUser domuser : domainUsers.getDomainUsers()) {
                if (domuser.getLoginName().equalsIgnoreCase(username) || domuser.getEmployeeNumber().equals(username)) {
                    try {
                        domuser.login(password);
                        return domuser.getEmployeeNumber();
                    } catch (NamingException ex) {
                        Logger.getLogger(Authentication.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (localUsers != null) {
            try {
                String encpw = encryptString(password);
                for (LocalUser locuser : localUsers.getLocalUsers()) {
                    if (locuser.getLoginName().equalsIgnoreCase(username) || locuser.getEmployeeNumber().equals(username)) {
                        if (locuser.getPassword().equals(encpw)) {
                            return locuser.getEmployeeNumber();
                        }
                    }
                }
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Authentication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
