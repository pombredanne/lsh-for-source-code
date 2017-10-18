package org.openthinclient.common.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.castor.util.Base64Encoder;

/**
 * @author levigo
 */
public class User extends DirectoryObject implements AssociatedObjectsProvider {

    private static final long serialVersionUID = 1L;

    private Set<UserGroup> userGroups;

    private Set<ApplicationGroup> applicationGroups;

    private Set<Application> applications;

    private Set<Printer> printers;

    private Location location;

    private String sn;

    private String givenName;

    private byte[] userPassword;

    private String newPassword = "";

    private String verifyPassword = "";

    public Set<ApplicationGroup> getApplicationGroups() {
        return applicationGroups;
    }

    public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
        this.applicationGroups = applicationGroups;
        firePropertyChange("applicationGroups", null, applicationGroups);
    }

    public Set<Application> getApplications() {
        return applications;
    }

    public void setApplications(Set<Application> applications) {
        this.applications = applications;
        firePropertyChange("applications", null, applications);
    }

    public void setLocation(Location location) {
        this.location = location;
        firePropertyChange("location", null, location);
    }

    public Set<Printer> getPrinters() {
        return printers;
    }

    public void setPrinters(Set<Printer> printers) {
        this.printers = printers;
        firePropertyChange("printers", null, printers);
    }

    public Set<UserGroup> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups;
        firePropertyChange("userGroups", null, userGroups);
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
        firePropertyChange("givenName", null, givenName);
    }

    public String getSn() {
        if (null == sn) this.sn = getName();
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
        firePropertyChange("sn", null, sn);
    }

    public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
        final Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
        assocObjects.put(Application.class, applications);
        assocObjects.put(ApplicationGroup.class, applicationGroups);
        assocObjects.put(Printer.class, printers);
        assocObjects.put(UserGroup.class, userGroups);
        return assocObjects;
    }

    public void setAssociatedObjects(Class subgroupClass, Set<? extends DirectoryObject> subgroups) {
        if (subgroupClass.equals(Application.class)) setApplications((Set<Application>) subgroups);
        if (subgroupClass.equals(ApplicationGroup.class)) setApplicationGroups((Set<ApplicationGroup>) subgroups);
        if (subgroupClass.equals(Printer.class)) setPrinters((Set<Printer>) subgroups);
        if (subgroupClass.equals(UserGroup.class)) setUserGroups((Set<UserGroup>) subgroups);
    }

    /**
	 * @return
	 * @deprecared Used for LDAP-Mapping only
	 */
    public byte[] getUserPassword() {
        return userPassword;
    }

    /**
	 * @param userPassword
	 * @deprecared Used for LDAP-Mapping only
	 */
    public void setUserPassword(byte[] userPassword) {
        this.userPassword = userPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String password) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            final String encrypted = "{MD5}" + new String(Base64Encoder.encode(digest.digest()));
            setUserPassword(encrypted.getBytes());
            this.newPassword = password;
            firePropertyChange("newPassword", "", password);
            firePropertyChange("password", new byte[0], getUserPassword());
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Can't encrypt user's password", e);
        }
    }

    public String getVerifyPassword() {
        return verifyPassword;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
        firePropertyChange("verifyPassword", "", verifyPassword);
    }

    public Location getLocation() {
        return location;
    }
}
