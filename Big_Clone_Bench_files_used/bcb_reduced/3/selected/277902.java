package com.simplenix.nicasio.admin.eb;

import com.simplenix.nicasio.hmaint.HMaintHide;
import com.simplenix.nicasio.hmaint.annotations.DescriptionField;
import com.simplenix.nicasio.hmaint.annotations.Digester;
import com.simplenix.nicasio.hmaint.annotations.HMaint;
import com.simplenix.nicasio.hmaint.annotations.Hide;
import com.simplenix.nicasio.hmaint.annotations.Searchable;
import com.simplenix.nicasio.hmaint.annotations.Secret;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author fronald
 */
@Entity
@Table(name = "NIC_USERS")
@HMaint(order = "usuId,name,login,email")
public class User implements Serializable {

    private long usuId;

    private String name;

    private String login;

    private String password;

    private String email;

    private List<UsuGroup> usuGroups;

    /**
	 * @return the usuId
	 */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "USU_ID")
    @Hide(where = HMaintHide.HIDE_ON_LIST_AND_ADD_AND_UPDATE)
    public long getUsuId() {
        return usuId;
    }

    /**
	 * @param usuId the usuId to set
	 */
    public void setUsuId(long usuId) {
        this.usuId = usuId;
    }

    /**
	 * @return the name
	 */
    @Column(name = "NAME")
    @DescriptionField
    @Searchable
    public String getName() {
        return name;
    }

    /**
	 * @param name the name to set
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * @return the login
	 */
    @Column(name = "LOGIN", length = 80, unique = true)
    @Searchable
    public String getLogin() {
        return login;
    }

    /**
	 * @param login the login to set
	 */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
	 * @return the password
	 */
    @Column(name = "PASSWORD")
    @Secret
    @Hide(where = HMaintHide.HIDE_ON_LIST)
    public String getPassword() {
        return password;
    }

    /**
	 * @param password the password to set
	 */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
	 * @return the email
	 */
    @Column(name = "EMAIL", unique = true)
    @Searchable
    public String getEmail() {
        return email;
    }

    /**
	 * @param email the email to set
	 */
    public void setEmail(String email) {
        this.email = email;
    }

    @Digester(forField = "password")
    public static String encriptPassword(String passwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(passwd.getBytes());
            BigInteger hash = new BigInteger(1, md.digest());
            return hash.toString(16);
        } catch (Exception e) {
            return null;
        }
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "USU_ID")
    public List<UsuGroup> getUsuGroups() {
        return usuGroups;
    }

    public void setUsuGroups(List<UsuGroup> usuGroups) {
        this.usuGroups = usuGroups;
    }
}
