package de.creditster.client.model.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import de.creditster.client.model.IfBudget;
import de.creditster.client.model.IfUser;
import de.creditster.webservice.ServicesStub.User;

/**
 * Diese Klasse ist die Standart-Implementierung des {@link IfUser Benutzer-Interfaces} und fungiert
 * als Wrapper.
 * 
 * @author Alexander Esslinger
 * @author Hannes Rempel
 */
public class UserImpl implements IfUser {

    /**
	 * Der Benutzer, der gewrappt wird.
	 */
    private User stubUser;

    public UserImpl() {
        stubUser = new User();
    }

    public UserImpl(User user) {
        if (user == null) {
            stubUser = new User();
        } else {
            stubUser = user;
        }
    }

    @Override
    public String getBirthday() {
        return stubUser.getBirthday();
    }

    @Override
    public IfBudget getBudget() {
        return new BudgetImpl(stubUser.getBudget());
    }

    @Override
    public String getCity() {
        return stubUser.getCity();
    }

    @Override
    public String getEmail() {
        return stubUser.getEmail();
    }

    @Override
    public String getFirstName() {
        return stubUser.getFirstName();
    }

    @Override
    public String getHouseNumber() {
        return stubUser.getHouseNumber();
    }

    @Override
    public String getLastName() {
        return stubUser.getLastName();
    }

    @Override
    public String getLogin() {
        return stubUser.getLogin();
    }

    @Override
    public String getPhone() {
        return stubUser.getPhone();
    }

    @Override
    public String getStreetName() {
        return stubUser.getStreetName();
    }

    /**
	 * @return der gewrappte Benutzer
	 */
    protected User getUser() {
        return stubUser;
    }

    @Override
    public String getZipCode() {
        return stubUser.getZipCode();
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
        return stubUser.getEmployee();
    }

    @Override
    public UserImpl setBirthday(String birthday) {
        stubUser.setBirthday(birthday);
        return this;
    }

    @Override
    public UserImpl setCity(String city) {
        stubUser.setCity(city);
        return this;
    }

    @Override
    public UserImpl setEmail(String email) {
        stubUser.setEmail(email);
        return this;
    }

    @Override
    public IfUser setEmployee(boolean employee) {
        stubUser.setEmployee(employee);
        return this;
    }

    @Override
    public UserImpl setFirstName(final String firstName) {
        stubUser.setFirstName(firstName);
        return this;
    }

    @Override
    public IfUser setHouseNumber(String houseNumber) {
        stubUser.setHouseNumber(houseNumber);
        return this;
    }

    @Override
    public UserImpl setLastName(final String lastName) {
        stubUser.setLastName(lastName);
        return this;
    }

    @Override
    public UserImpl setLogin(String login) {
        stubUser.setLogin(login);
        return this;
    }

    @Override
    public UserImpl setPassword(String password) {
        if (password != null && password.trim().length() > 2) {
            stubUser.setPassword(hashPassword(password));
        }
        return this;
    }

    @Override
    public UserImpl setPhone(String phone) {
        stubUser.setPhone(phone);
        return this;
    }

    @Override
    public IfUser setStreetName(String streetName) {
        stubUser.setStreetName(streetName);
        return this;
    }

    @Override
    public IfUser setZipCode(String zipCode) {
        stubUser.setZipCode(zipCode);
        return this;
    }

    @Override
    public String toString() {
        return getFirstName() + " " + getLastName();
    }
}
