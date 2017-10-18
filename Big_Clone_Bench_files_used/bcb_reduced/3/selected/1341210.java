package de.creditster.webservice.model;

import static javax.persistence.TemporalType.DATE;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import de.creditster.webservice.interfaces.IfUser;

/**
 * Diese Entität ist eine persistente Klasse für die Benutzer.
 */
@Entity
public class User implements Serializable, IfUser {

    private static final long serialVersionUID = 1L;

    @Temporal(DATE)
    private Date birthday;

    @Embedded
    private Budget budget = new Budget();

    private String city;

    private String email;

    private boolean employee;

    private String firstName;

    private String houseNumber;

    private String lastName;

    @Id
    private String login;

    private String password;

    private String phone;

    @Embedded
    private Plan plan;

    private String streetName;

    private String zipCode;

    public User() {
    }

    @Override
    public String getBirthday() {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(birthday);
    }

    @Override
    public Budget getBudget() {
        return budget;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getHouseNumber() {
        return houseNumber;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

    @Override
    public String getStreetName() {
        return streetName;
    }

    @Override
    public String getZipCode() {
        return zipCode;
    }

    /**
	 * Diese Methode erzeugt einen MD5-Hash.
	 * 
	 * @param password
	 *            Passwort
	 * @return MD5-Hash des Passworts
	 */
    private String hashPassword(String password) {
        if (password != null && password.trim().length() > 0) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(password.trim().getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                return hash.toString(16);
            } catch (NoSuchAlgorithmException nsae) {
            }
        }
        return null;
    }

    @Override
    public boolean isEmployee() {
        return employee;
    }

    @Override
    public boolean isPassword(String passwd) {
        if (password != null) {
            return password.equals(hashPassword(passwd));
        }
        return false;
    }

    @Override
    public void setBirthday(String birthday) {
        if (birthday != null && birthday.trim().length() > 0) {
            final List<Locale> locales = new ArrayList<Locale>();
            locales.add(Locale.GERMAN);
            locales.add(Locale.ENGLISH);
            for (final Locale locale : locales) {
                for (int i = 3; i >= 0; i--) {
                    try {
                        final DateFormat df = DateFormat.getDateInstance(i, locale);
                        this.birthday = df.parse(birthday);
                    } catch (final ParseException e) {
                    }
                }
            }
        }
    }

    @Override
    public void setBudget(Budget budget) {
        this.budget = budget;
    }

    @Override
    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setEmployee(boolean employee) {
        this.employee = employee;
    }

    @Override
    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    @Override
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    @Override
    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    @Override
    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    @Override
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
