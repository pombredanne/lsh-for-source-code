package com.centraview.license;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Properties;
import org.apache.axis.encoding.Base64;
import org.apache.log4j.Logger;
import com.centraview.settings.Settings;

/**
 * This singleton object will hold the license file information
 * in memory. It will be fed information from the getlicensefile
 * service. It will be accessable from the login module.
 *
 * @author Ryan Grier <ryan@centraview.com>
 */
public class LicenseInstanceVO implements Serializable {

    /** The license file Status value Key. */
    private static final String LICENSE_FILE_STATUS_KEY = "Status";

    /** The license file Number of Users value Key. */
    private static final String LICENSE_FILE_USERS_KEY = "Users";

    /** The license file MAC Address value Key. */
    private static final String LICENSE_FILE_MAC_KEY = "MAC";

    /** The license file Host Name value Key. */
    private static final String LICENSE_FILE_HOST_NAME_KEY = "Host_Name";

    /** The license file Expiration Date value Key. */
    private static final String LICENSE_FILE_EXP_DATE_KEY = "Exp_Date";

    /** The license file SHA1 Validation value Key. */
    private static final String LICENSE_FILE_SHA_KEY = "Validate";

    /** The license file license key value Key. */
    private static final String LICENSE_KEY_KEY = "Key";

    /** The license file expires value Key. */
    private static final String LICENSE_EXPIRES_KEY = "Ex";

    /** The license file Offset value Key. */
    private static final String LICENSE_FILE_OFFSET_KEY = "Offset";

    /** The License File. */
    private Properties licenseFile = null;

    /** The License's current status. */
    private String status = null;

    /** Whether or not this copy of CentraView expires. */
    private boolean expires = false;

    /** The License's expiration date. */
    private Date expirationDate = null;

    /** The License's current Mac Address. */
    private String macAddress = null;

    /** The License's current Host Name. */
    private String hostName = null;

    /** The License's validation string. */
    private String validationKey = null;

    /** The server's offset from the CentraView License Server. */
    private long serverOffset = 0;

    /** The number of concurrent users allowed to login to CentraView. */
    private int numberOfUsers = 0;

    /** The license key from the license file. */
    private String licenseKey = null;

    /** Determines whether this instance has been setup. */
    private boolean isSetup = true;

    private static Logger logger = Logger.getLogger(LicenseInstanceVO.class);

    private static final long MILLIS_IN_A_DAY = 86400000;

    public LicenseInstanceVO() {
    }

    /**
   * Constructs a LicenseInstanceVO object with the license properties.
   * 
   * @param licenseFile
   */
    public LicenseInstanceVO(Properties licenseFile) {
        this.updateLicenseInformation(licenseFile);
    }

    /**
   * Updates the existing LicenseInstanceVO object with 
   * information from the license file.
   *
   * @param licenseFile the decrypted license file.
   */
    public final synchronized void updateLicenseInformation(Properties licenseFile) {
        this.licenseFile = licenseFile;
        this.setMACAddress(this.licenseFile.getProperty(LICENSE_FILE_MAC_KEY));
        this.setHostName(this.licenseFile.getProperty(LICENSE_FILE_HOST_NAME_KEY));
        this.setStatus(this.licenseFile.getProperty(LICENSE_FILE_STATUS_KEY));
        this.setValidationKey(this.licenseFile.getProperty(LICENSE_FILE_SHA_KEY));
        this.setLicenseKey(this.licenseFile.getProperty(LICENSE_KEY_KEY));
        String thisNumberOfUsersString = this.licenseFile.getProperty(LICENSE_FILE_USERS_KEY);
        String thisExpirationDateString = this.licenseFile.getProperty(LICENSE_FILE_EXP_DATE_KEY);
        String thisExpiresString = this.licenseFile.getProperty(LICENSE_EXPIRES_KEY);
        String thisOffsetString = this.licenseFile.getProperty(LICENSE_FILE_OFFSET_KEY);
        Date thisExpirationDate = (thisExpirationDateString == null) ? new Date() : new Date(Long.parseLong(thisExpirationDateString));
        long thisOffset = (thisOffsetString == null) ? 0 : Long.parseLong(thisOffsetString);
        int thisNumberOfUsers = (thisNumberOfUsersString == null) ? 0 : Integer.parseInt(thisNumberOfUsersString);
        boolean thisExpires = false;
        if (thisExpiresString != null && thisExpiresString.equalsIgnoreCase("true")) {
            thisExpires = true;
        }
        this.setOffset(thisOffset);
        this.setNumberOfUsers(thisNumberOfUsers);
        this.setExpires(thisExpires);
        this.setExpirationDate(thisExpirationDate);
        setIsSetup(true);
    }

    /**
   * Updates the existing LicenseInstanceVO object 
   * and sets it to 'INVALID'.
   */
    public final synchronized void setLicenseToInvalid() {
        this.setStatus("INVALID");
        setIsSetup(true);
    }

    /**
   * Sets the license file.
   *
   * @param licenseFile The license file.
   */
    private final void setLicenseFile(Properties licenseFile) {
        this.licenseFile = licenseFile;
    }

    /**
   * Returns the license file.
   *
   * @return The license file.
   */
    private final Properties getLicenseFile() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.licenseFile;
    }

    /**
   * Sets the MAC Address from the license file.
   *
   * @param macAddress The MAC Address from the license file.
   */
    private final void setMACAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    /**
   * Returns the MAC Address from the license file.
   *
   * @return The MAC Address from the license file.
   */
    public final String getMACAddress() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.macAddress;
    }

    /**
   * Sets the Host Name from the license file.
   *
   * @param hostName The Host Name from the license file.
   */
    private final void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
   * Returns the Host Name from the license file.
   *
   * @return The Host Name from the license file.
   */
    public final String getHostName() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.hostName;
    }

    /**
   * Sets the Validation Key from the license file.
   *
   * @param hostName The Validation Key from the license file.
   */
    private final void setValidationKey(String validationKey) {
        this.validationKey = validationKey;
    }

    /**
   * Returns the Validation Key from the license file.
   *
   * @return The Validation Key from the license file.
   */
    private final String getValidationKey() {
        return this.validationKey;
    }

    /**
   * Sets the Status from the license file.
   *
   * @param status The Status from the license file.
   */
    private final void setStatus(String status) {
        this.status = status;
    }

    /**
   * Returns the Status from the license file.
   *
   * @return The Status from the license file.
   */
    public final String getStatus() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.status;
    }

    /**
   * Sets the License Key from the license file.
   *
   * @param licenseKey The License Key from the license file.
   */
    private final void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    /**
   * Returns the License Key from the license file.
   *
   * @return The License Key from the license file.
   */
    public final String getLicenseKey() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.licenseKey;
    }

    /**
   * Sets the Expiration Date from the license file.
   *
   * @param expirationDate The Expiration Date from the license file.
   */
    private final void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
   * Returns the Expiration Date from the license file.
   *
   * @return The Expiration Date from the license file.
   */
    public final Date getExpirationDate() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.expirationDate;
    }

    /**
   * Sets the Offset from the license file.
   *
   * @param offset The Offset from the license file.
   */
    private final void setOffset(long offset) {
        this.serverOffset = offset;
    }

    /**
   * Returns the Offset from the license file.
   *
   * @return The Offset from the license file.
   */
    private final long getOffset() {
        return this.serverOffset;
    }

    /**
   * Sets the Expires flag from the license file.
   *
   * @param expires The Expires flag from the license file.
   */
    private final void setExpires(boolean expires) {
        this.expires = expires;
    }

    /**
   * Returns the Expires flag from the license file.
   *
   * @return The Expires flag from the license file.
   */
    public final boolean expires() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.expires;
    }

    /**
   * Returns whether the current License is expired.
   * 
   * @return True if the license is expired, false otherwise.
   */
    public final boolean isExpired() throws LicenseNotSetupException {
        return this.getExpirationDate().before(new Date());
    }

    /**
   * Returns the number of days remaining with this license.
   * <p>
   * The following are possible results from this
   * method.
   * <ul>
   * <li>Any negative number means the license has expired.
   * <li>0 Means the license expires today.
   * <li>Any positive number means that many days remain
   * until the license expires.
   * </ul>
   *
   * @return The number of days remaining on this license.
   */
    public final int getNumberOfDaysRemaining() throws LicenseNotSetupException {
        long nowTimestamp = new Date().getTime();
        long expirationDateTimestamp = this.getExpirationDate().getTime();
        long difference = expirationDateTimestamp - nowTimestamp;
        double daysLeft = (difference / MILLIS_IN_A_DAY);
        return new Double(daysLeft).intValue();
    }

    /**
   * Sets the Number of Users from the license file.
   *
   * @param numberOfUsers The Number of Users from the license file.
   */
    private final void setNumberOfUsers(int numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    /**
   * Returns the Number of Users from the license file.
   *
   * @return The Number of Users from the license file.
   */
    public final int getNumberOfUsers() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        return this.numberOfUsers;
    }

    /**
   * Sets whether this instance has been setup.
   *
   * @param isSetup Whether this instance has been setup.
   */
    private final void setIsSetup(boolean isSetup) {
        this.isSetup = isSetup;
    }

    /**
   * Returns whether this instance has been setup.
   *
   * @return Whether this instance has been setup.
   */
    private final boolean isSetup() {
        return this.isSetup;
    }

    /**
   * Gets the LicnseFile (in memory) and validates the file.
   * 
   * @return true if the license file is valid. false if the license file is not
   *         valid.
   */
    public final synchronized boolean isValidLicenseFile() throws LicenseNotSetupException {
        if (!isSetup()) {
            throw new LicenseNotSetupException();
        }
        boolean returnValue = false;
        Properties properties = getLicenseFile();
        logger.debug("isValidLicenseFile: License to validate:");
        logger.debug(properties);
        StringBuffer validationStringBuffer = new StringBuffer();
        validationStringBuffer.append(LICENSE_KEY_KEY + ":" + properties.getProperty(LICENSE_KEY_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_STATUS_KEY + ":" + properties.getProperty(LICENSE_FILE_STATUS_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_USERS_KEY + ":" + properties.getProperty(LICENSE_FILE_USERS_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_MAC_KEY + ":" + properties.getProperty(LICENSE_FILE_MAC_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_HOST_NAME_KEY + ":" + properties.getProperty(LICENSE_FILE_HOST_NAME_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_OFFSET_KEY + ":" + properties.getProperty(LICENSE_FILE_OFFSET_KEY) + ",");
        validationStringBuffer.append(LICENSE_FILE_EXP_DATE_KEY + ":" + properties.getProperty(LICENSE_FILE_EXP_DATE_KEY) + ",");
        validationStringBuffer.append(LICENSE_EXPIRES_KEY + ":" + properties.getProperty(LICENSE_EXPIRES_KEY));
        logger.debug("isValidLicenseFile: Validation String Buffer: " + validationStringBuffer.toString());
        String validationKey = (String) properties.getProperty(LICENSE_FILE_SHA_KEY);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(validationStringBuffer.toString().getBytes());
            String newValidation = Base64.encode(messageDigest.digest());
            if (newValidation.equals(validationKey)) {
                if (getMACAddress().equals(Settings.getInstance().getMACAddress())) {
                    returnValue = true;
                }
            }
        } catch (Exception exception) {
            System.out.println("Exception in LicenseInstanceVO.isValidLicenseFile");
        }
        return returnValue;
    }

    /**
   * Returns a String representation of the current LicenseInstanceVO
   * object. Each value is on a new line. This method overrides the
   * standard Object.toString method.
   *
   * @return A string representation of this object.
   */
    public final String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("{");
            sb.append("MAC Address: " + this.getMACAddress() + ",");
            sb.append("Host Name: " + this.getHostName() + ",");
            sb.append("Status: " + this.getStatus() + ",");
            sb.append("License Key: " + this.getLicenseKey() + ",");
            sb.append("Number Of Users: " + this.getNumberOfUsers() + ",");
            sb.append("Expires: " + this.expires() + ",");
            sb.append("Expiration Date: " + this.getExpirationDate() + ",");
            sb.append("Expired: " + this.isExpired() + ",");
            sb.append("Days Remaining: " + this.getNumberOfDaysRemaining());
            sb.append("}");
        } catch (LicenseNotSetupException licenseNotSetupException) {
            sb = new StringBuffer(licenseNotSetupException.getMessage());
        }
        return sb.toString();
    }
}
