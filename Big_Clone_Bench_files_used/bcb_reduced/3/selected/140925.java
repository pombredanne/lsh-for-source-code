package org.jes.common.plugins;

import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.File;
import java.util.*;
import org.jes.core.plugins.MailboxManager;
import org.jes.core.plugins.Mailbox;
import org.jes.core.plugins.PluginFactory;
import org.jes.core.plugins.ConfigurationManager;
import org.jes.core.info.Address;
import org.jes.common.plugins.FileMailbox;
import org.apache.log4j.Logger;

/**
 * Provides an implementation of the MailboxManager interface that uses the
 * local file system to store messages.
 *
 * @author Eric Daugherty
 */
public class FileMailboxManager implements MailboxManager {

    public static final String CONFIGURATION_PREFIX = "filemailbox.";

    public static final String MAILBOX_PATH_PARAMETER = CONFIGURATION_PREFIX + "root.dir";

    /**
     * The location of all the mailbox files.
     */
    private File rootDirectory;

    private Map mailboxLocks;

    private ConfigurationManager configurationManager;

    private static Logger log = Logger.getLogger(FileMailboxManager.class);

    /**
     * Initializes the using the file path from the ConfigurationManager.
     */
    public FileMailboxManager() {
        this(null);
    }

    /**
     * Initializes the required plugins and verifies the storage path.
     *
     * @param rootDirectory the directory that the mailboxes are stored in.
     */
    public FileMailboxManager(File rootDirectory) {
        PluginFactory pluginFactory = PluginFactory.getFactory();
        configurationManager = pluginFactory.getConfigurationManager();
        mailboxLocks = Collections.synchronizedMap(new HashMap());
        if (rootDirectory == null) {
            String mailboxPath = configurationManager.getProperty(MAILBOX_PATH_PARAMETER);
            if (mailboxPath == null) {
                log.error("Mailbox Path ( " + MAILBOX_PATH_PARAMETER + " ) not specified.  Unable to initialize FileMaiboxValidator Plugin.");
                throw new RuntimeException("Mailbox Path ( " + MAILBOX_PATH_PARAMETER + " ) not specified.  Unable to initialize FileMaiboxValidator Plugin.");
            }
            rootDirectory = new File(mailboxPath);
        }
        this.rootDirectory = rootDirectory;
        if (!rootDirectory.exists()) {
            if (log.isDebugEnabled()) log.debug("Mailbox Root Directory does not exist ( " + rootDirectory.getAbsolutePath() + " ).  Creating...");
            if (!rootDirectory.mkdirs()) {
                log.error("Unable to create Maibox Root Directory ( " + rootDirectory.getAbsolutePath() + " ). Unable to initialize FileMaiboxValidator Plugin.");
                throw new RuntimeException("Unable to create Maibox Root Directory ( " + rootDirectory.getAbsolutePath() + " ). Unable to initialize FileMaiboxValidator Plugin.");
            }
        }
        if (!rootDirectory.isDirectory()) {
            log.error("Mailbox Root Directory ( " + rootDirectory.getAbsolutePath() + " ) is not a directory!.  Unable to initialize FileMaiboxValidator Plugin.");
            throw new RuntimeException("Mailbox Root Directory ( " + rootDirectory.getAbsolutePath() + " ) is not a directory!.  Unable to initialize FileMaiboxValidator Plugin.");
        }
    }

    /**
     * Verifies whether connections should be accepted from the
     * specified client address.
     *
     * @param address client's Internet address
     * @return true if it is acceptable.
     */
    public boolean acceptClient(InetAddress address) {
        return true;
    }

    /**
     * Verifies whether the specified user is a local user.
     * <p>
     * This class is called when the processor accepts a username
     * from the client.  Implementations may choose to always
     * return true and delay the validation until after the
     * password has been specified.  Performing this validation
     * will provide more information to the user, but may cause
     * security concerns (username harvesting).
     *
     * @param userName the username specified by the client.
     * @return true if the username is valid, false otherwise.
     */
    public boolean validateUser(String userName) {
        return true;
    }

    /**
     * Verfies that the specified login criteria match an existing
     * mailbox.  If they match, the mailbox id is returned.  This
     * id can be used by the openMailbox method to access the mailbox.
     * If they do not match, -1 is returned.
     *
     * @param userName the username specified by the client.
     * @param password the password specified by the client.
     * @return the mailbox's id, if matched, or null the mailbox does not exist.
     */
    public String valiateMailbox(String userName, String password) {
        String mailboxId = null;
        userName = userName.toLowerCase();
        String realPassword = configurationManager.getProperty(CONFIGURATION_PREFIX + "user." + userName + ".password");
        if (password != null) {
            password = encryptPassword(password);
        }
        if (password == null || realPassword == null || !password.equals(realPassword)) {
            log.info("Authentication failed for user: " + userName);
        } else {
            mailboxId = configurationManager.getProperty(CONFIGURATION_PREFIX + "user." + userName + ".mailbox");
            if (mailboxId == null || mailboxId.length() == 0) {
                mailboxId = null;
                log.error("User " + userName + " has a valid password, but is not assigned a mailbox.");
            } else {
                if (log.isInfoEnabled()) log.info("User: " + userName + " successfully authenticated.");
            }
        }
        return mailboxId;
    }

    /**
     * Determines which mailbox mail addressed to the the specified address
     * should be delivered.  If the address is invalid, null is returned.
     *
     * @param address email address of mailbox to deliver to.
     * @return the mailboxId the mail should be delivered to, or null if the address is invalid.
     */
    public String validateAddress(Address address) {
        String userName = address.getUsername().toLowerCase();
        String realPassword = configurationManager.getProperty(CONFIGURATION_PREFIX + "user." + userName + ".password");
        if (realPassword != null) {
            return userName;
        }
        return null;
    }

    /**
     * Opens a mailbox for exclusive access.  The mailboxId should be
     * retrieved using the validateMailbox method.
     *
     * @param mailboxId the unique id for the requested mailbox.
     * @return a Mailbox opened for exclusive access.
     */
    public synchronized Mailbox lockMailbox(String mailboxId) {
        File mailboxDirectory = getMailboxDirectory(mailboxId);
        if (mailboxDirectory == null) {
            return null;
        }
        if (mailboxLocks.containsKey(mailboxId)) {
            Long lockTime = (Long) mailboxLocks.get(mailboxId);
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTimeInMillis(lockTime.longValue());
            GregorianCalendar unlockTime = new GregorianCalendar();
            unlockTime.setTimeInMillis(System.currentTimeMillis());
            unlockTime.roll(Calendar.MINUTE, -5);
            if (lastModified.before(unlockTime)) {
                if (log.isInfoEnabled()) log.info("Lock timed out for mailbox: " + mailboxId);
                mailboxLocks.remove(mailboxId);
            }
        }
        if (!mailboxLocks.containsKey(mailboxId)) {
            mailboxLocks.put(mailboxId, new Long(System.currentTimeMillis()));
            if (log.isInfoEnabled()) log.info("Mailbox: " + mailboxId + " successfully locked.");
            if (log.isDebugEnabled()) log.debug("Mailbox: " + mailboxId + " using directory: " + mailboxDirectory.getAbsolutePath());
            return new FileMailbox(this, mailboxId, mailboxDirectory);
        } else {
            if (log.isInfoEnabled()) log.info("Unable to aquire lock for mailbox: " + mailboxId + " because it is already locked");
            return null;
        }
    }

    /**
     * Unlocks a mailbox.
     *
     * @param mailboxId the unique ID of the mailbox.
     */
    synchronized void unlockMailbox(String mailboxId) {
        mailboxLocks.remove(mailboxId);
        if (log.isInfoEnabled()) log.info("Mailbox: " + mailboxId + " successfully unlocked.");
    }

    /**
     * Creates a one-way has of the specified password.  This allows passwords to be
     * safely stored without exposing the original plain text password.
     *
     * @param password the password to encrypt.
     * @return the encrypted password, or null if encryption failed.
     */
    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes());
            byte[] hash = md.digest();
            int hashLength = hash.length;
            StringBuffer hashStringBuf = new StringBuffer();
            String byteString;
            int byteLength;
            for (int index = 0; index < hashLength; index++) {
                byteString = String.valueOf(hash[index] + 128);
                byteLength = byteString.length();
                switch(byteLength) {
                    case 1:
                        byteString = "00" + byteString;
                        break;
                    case 2:
                        byteString = "0" + byteString;
                        break;
                }
                hashStringBuf.append(byteString);
            }
            return hashStringBuf.toString();
        } catch (NoSuchAlgorithmException nsae) {
            log.error("Error getting password hash: " + nsae.getMessage());
            return null;
        }
    }

    /**
     * Returns a file that references a specific mailbox's directory.
     *
     * @param mailboxId the unique id of the mailbox.
     * @return the mailbox directory File, or null if it could not be found or created.
     */
    private File getMailboxDirectory(String mailboxId) {
        File mailboxDirectory = new File(rootDirectory, mailboxId);
        if (!mailboxDirectory.exists()) {
            if (!mailboxDirectory.mkdirs()) {
                log.error("Unable to create mailbox: " + mailboxId + " directory: " + mailboxDirectory.getAbsolutePath());
                mailboxDirectory = null;
            }
        }
        return mailboxDirectory;
    }
}
