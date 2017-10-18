package com.ericdaugherty.mail.server.info;

import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.auth.SCRAMServerMode;
import com.ericdaugherty.mail.server.crypto.PBKDF2;
import com.ericdaugherty.mail.server.crypto.digest.JESMessageDigest;
import com.ericdaugherty.mail.server.crypto.mac.HMACParameterSpec;
import com.ericdaugherty.mail.server.crypto.mac.JESMac;
import com.ericdaugherty.mail.server.persistence.POP3MessagePersistenceFactory;
import com.ericdaugherty.mail.server.persistence.POP3MessagePersistenceProccessor;
import com.ericdaugherty.mail.server.utils.ByteUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractUser implements User {

    /** Logger */
    protected static final Log log = LogFactory.getLog(AbstractUser.class);

    protected String username;

    private String domain;

    protected String password;

    private EmailAddress[] forwardAddresses;

    private Message[] messages = null;

    private final POP3MessagePersistenceProccessor pOP3MessagePersistenceProccessor;

    private byte[] storedKey;

    private byte[] serverKey;

    private byte[] serverSignature;

    public AbstractUser(EmailAddress address) {
        username = address.getUsername();
        domain = address.getDomain();
        if (!(address.isNULL() || address.isMailerDaemon())) {
            pOP3MessagePersistenceProccessor = POP3MessagePersistenceFactory.getInstance().getPOP3PersistenceProccessor();
        } else {
            pOP3MessagePersistenceProccessor = POP3MessagePersistenceFactory.getInstance().getNullPeristenceProccessor();
        }
        pOP3MessagePersistenceProccessor.setUser(this);
    }

    public boolean isPasswordValid(String plainTextPassword, String authenticationMechanism, Object authenticationData) {
        if (authenticationMechanism.startsWith("CRAM")) {
            try {
                JESMac jesMac = JESMac.getInstance("Hmac" + authenticationMechanism.substring(5));
                byte[] challengeBytes = (byte[]) authenticationData;
                byte[] key = password.startsWith("{ENC}") ? password.substring(5).getBytes("UTF-8") : password.getBytes("UTF-8");
                jesMac.init(new SecretKeySpec(key, "Hmac" + authenticationMechanism.substring(5)), new HMACParameterSpec());
                jesMac.update(challengeBytes, 0, challengeBytes.length);
                return Arrays.equals(jesMac.doFinal(), ByteUtils.toByteArray(plainTextPassword.toCharArray()));
            } catch (Exception ex) {
                log.error(ex);
                return false;
            }
        } else if (authenticationMechanism.startsWith("SCRAM")) {
            try {
                JESMac jesMac = JESMac.getInstance("Hmac" + authenticationMechanism.substring(6));
                SCRAMServerMode.AuthenticationData authenticationDataSCRAM = (SCRAMServerMode.AuthenticationData) authenticationData;
                MessageDigest md = JESMessageDigest.getInstance(authenticationMechanism.substring(6));
                byte[] clientKey;
                if (storedKey == null) {
                    String password = this.password.startsWith("{ENC}") ? this.password.substring(5) : this.password;
                    byte[] key = password.getBytes("UTF-8");
                    jesMac.init(new SecretKeySpec(key, "Hmac" + authenticationMechanism.substring(6)), new HMACParameterSpec());
                    byte[] saltedPassword = new byte[jesMac.getMacLength()];
                    PBKDF2 pbkdf2 = new PBKDF2();
                    pbkdf2.getDerivedKey(jesMac, authenticationDataSCRAM.getSalt(), authenticationDataSCRAM.getIteration(), saltedPassword);
                    jesMac.init(new SecretKeySpec(saltedPassword, "Hmac" + authenticationMechanism.substring(6)), new HMACParameterSpec());
                    clientKey = "Client Key".getBytes();
                    jesMac.update(clientKey, 0, clientKey.length);
                    clientKey = jesMac.doFinal();
                    storedKey = md.digest(clientKey);
                    jesMac.init(new SecretKeySpec(saltedPassword, "Hmac" + authenticationMechanism.substring(6)), new HMACParameterSpec());
                    serverKey = "Server Key".getBytes();
                    jesMac.update(serverKey, 0, serverKey.length);
                    serverKey = jesMac.doFinal();
                }
                jesMac.init(new SecretKeySpec(storedKey, "Hmac" + authenticationMechanism.substring(6)), new HMACParameterSpec());
                byte[] authMessageBytes = authenticationDataSCRAM.getAuthMessage().getBytes("UTF-8");
                jesMac.update(authMessageBytes, 0, authMessageBytes.length);
                clientKey = jesMac.doFinal();
                byte[] clientProof = plainTextPassword.getBytes("UTF-8");
                if (clientKey.length != clientProof.length) {
                    return false;
                }
                for (int i = 0; i < clientKey.length; i++) {
                    clientKey[i] ^= clientProof[i];
                }
                jesMac.init(new SecretKeySpec(serverKey, "Hmac" + authenticationMechanism.substring(6)), new HMACParameterSpec());
                jesMac.update(authMessageBytes, 0, authMessageBytes.length);
                serverSignature = jesMac.doFinal();
                return Arrays.equals(md.digest(clientKey), storedKey);
            } catch (Exception ex) {
                log.error(ex);
                return false;
            }
        } else {
            return false;
        }
    }

    public byte[] getServerSignature() {
        return serverSignature;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getFullUsername() {
        return getFullUsername(username, domain);
    }

    public EmailAddress getEmailAddress() {
        return EmailAddress.getEmailAddress(username, domain);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public EmailAddress[] getForwardAddresses() {
        return forwardAddresses;
    }

    public void setForwardAddresses(EmailAddress[] forwardAddresses) {
        this.forwardAddresses = forwardAddresses;
    }

    /**
    * Returns an array of Strings that represent email addresses to deliver
    * email to this user.  If the forwardAddresses is not null or empty,
    * this will return the forwardAddresses array.  Otherwise, this will return
    * the user's email address.
    *
    * @return array of strings that represent email addresses.
    */
    public EmailAddress[] getDeliveryAddresses() {
        if (forwardAddresses != null && forwardAddresses.length > 0) {
            return forwardAddresses;
        } else {
            return new EmailAddress[] { EmailAddress.getEmailAddress(getUsername(), getDomain()) };
        }
    }

    /**
    * Returns an array of Message objects that represents all messages
    * stored for this user.
    */
    public Message[] getMessages() {
        if (messages == null) {
            String[] fileNames = pOP3MessagePersistenceProccessor.populatePOP3MessageList();
            int numMessage = fileNames.length;
            messages = new Message[numMessage];
            Message currentMessage;
            for (int index = 0; index < numMessage; index++) {
                currentMessage = new Message();
                currentMessage.setMessageLocation(fileNames[index]);
                messages[index] = currentMessage;
            }
        }
        return messages;
    }

    /**
    * Returns an array of Message objects that represents all messaged
    * stored for this user not marked for deletion.
    */
    public Message[] getNonDeletedMessages() {
        Message[] allMessages = getMessages();
        int allCount = allMessages.length;
        int nonDeletedCount = 0;
        for (int i = 0; i < allCount; i++) {
            if (!allMessages[i].isDeleted()) {
                nonDeletedCount++;
            }
        }
        Message[] nonDeletedMessages = new Message[nonDeletedCount];
        nonDeletedCount = 0;
        for (int i = 0; i < allCount; i++) {
            if (!allMessages[i].isDeleted()) {
                nonDeletedMessages[nonDeletedCount++] = allMessages[i];
            }
        }
        return nonDeletedMessages;
    }

    /**
    * Gets the specified message.  Message numbers are 1 based.  
    * This method counts on the calling method to verify that the
    * messageNumber actually exists.
    */
    public Message getMessage(int messageNumber) {
        return getMessages()[messageNumber - 1];
    }

    /**
    * Gets the total number of messages currently stored for this user.
    */
    public long getNumberOfMessage() {
        return getMessages().length;
    }

    /**
    * Gets the total number of non deleted messages currently stored for this user.
    */
    public long getNumberOfNonDeletedMessages() {
        return getNonDeletedMessages().length;
    }

    /**
    * Gets the total size of the non deleted messages currently stored for this user.
    */
    public long getSizeOfAllNonDeletedMessages() {
        Message[] message = getNonDeletedMessages();
        long totalSize = 0;
        for (int index = 0; index < message.length; index++) {
            totalSize += message[index].getMessageSize(this);
        }
        return totalSize;
    }

    /**
    * This method removes any cached message information this user may have stored
    */
    public void reset() {
        messages = null;
    }

    public POP3MessagePersistenceProccessor getPOP3MessagePersistenceProccessor() {
        return pOP3MessagePersistenceProccessor;
    }

    /**
    * Converts a username and domain to the combined string representation of an e-mail address.
    */
    private String getFullUsername(String username, String domain) {
        return username + '@' + domain;
    }
}
