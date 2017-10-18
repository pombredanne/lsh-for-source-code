package de.plugmail.defaults;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.log4j.Logger;
import de.plugmail.data.Criteria;
import de.plugmail.data.Folder;
import de.plugmail.data.Message;
import de.plugmail.defaults.gui.MailHelper;
import de.plugmail.plugins.AccountPlugin;
import de.plugmail.plugins.MessageStorePlugin;

public class DefaultMessageStore extends MessageStorePlugin {

    private String defaultStorageDir;

    private Logger log;

    private static MessageDigest md5 = null;

    public DefaultMessageStore() {
        log = Logger.getLogger(this.getClass());
        defaultStorageDir = System.getProperty("user.home") + File.separator + "PlugMail" + File.separator + "messages";
        File check = new File(defaultStorageDir);
        boolean created;
        if (!check.exists()) {
            log.debug("storagefolder " + defaultStorageDir + " does not exist!");
            check = new File(System.getProperty("user.home") + File.separator + "PlugMail");
            if (!check.exists()) {
                created = check.mkdir();
                log.debug("Could we create " + check + "? " + created);
            }
            check = new File(System.getProperty("user.home") + File.separator + "PlugMail" + File.separator + "messages");
            if (!check.exists()) {
                created = check.mkdir();
                log.debug("Could we create " + check + "? " + created);
            }
        } else {
            log.debug("found storagefolder " + defaultStorageDir);
        }
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                log.error("Could not get MessageDigest-instace for md5!", e);
            }
        }
        log.info("defaultStorageDir: " + defaultStorageDir);
    }

    public void activate() {
        log.debug("activating messageStore...");
        checkAccountDirs();
        log.debug("messageStore activated!");
    }

    public void deactivate() {
    }

    private void checkAccountDirs() {
        AccountPlugin account;
        String accountName;
        File accountStorageDir;
        log.debug("checking " + accounts.size() + " account dirs...");
        Iterator keys = accounts.keySet().iterator();
        while (keys.hasNext()) {
            account = (AccountPlugin) accounts.get(keys.next());
            accountName = account.getName();
            log.debug("accountName is " + accountName);
            File accountInfoFile = new File(getAccountStorageDir(accountName).getPath() + File.separator + "info");
            Properties props;
            try {
                if (!accountInfoFile.exists()) {
                    boolean fileCreated = accountInfoFile.createNewFile();
                    if (!fileCreated) {
                        log.error("Could not create info file (" + accountInfoFile + ")!");
                        return;
                    }
                }
                props = new Properties();
                props.load(new FileInputStream(accountInfoFile));
            } catch (Exception e) {
                log.error("Could not load properties!", e);
                return;
            }
            accountStorageDir = new File(getAccountStorageDir(accountName).getPath() + File.separator + "inbox");
            log.debug("accountStorageDir is " + accountStorageDir);
            if (!accountStorageDir.exists()) {
                log.debug("creating accountStorageDir " + accountStorageDir);
                boolean created = accountStorageDir.mkdir();
                if (!created) {
                    log.error("Could not created accountStorageDir " + accountStorageDir);
                } else {
                    log.debug("created accountStorageDir " + accountStorageDir);
                }
            } else {
                if (!accountStorageDir.isDirectory()) {
                    log.error(accountStorageDir + " is not a directory!");
                    throw new RuntimeException(accountStorageDir + " is not a directory!");
                } else {
                    log.debug("accountStorageDir " + accountStorageDir + " exists!");
                }
            }
            String[] fileNames = accountStorageDir.list();
            Folder inbox = account.getDefaultInbox();
            Vector<Message> messages = new Vector<Message>();
            Message message;
            File messageFile;
            log.debug("getting MailHelper instance...");
            MailHelper mimeMessage = new MailHelper();
            for (int j = 0; j < fileNames.length; j++) {
                try {
                    message = new Message();
                    message.setMessageId(accountName + "\t" + fileNames[j]);
                    String info = props.getProperty(fileNames[j]);
                    if (info != null) {
                        log.debug("info is: " + info);
                        String[] infos = info.split("\t");
                        String subject = infos[0];
                        log.debug("subject is: " + subject);
                        if (infos.length == 1) {
                            message.setState(Message.UNREAD);
                        } else {
                            String state = infos[1];
                            log.debug("state is: " + state);
                            message.setState(Integer.parseInt(state));
                        }
                        message.setSubject(subject);
                    } else {
                        messageFile = new File(accountStorageDir.getPath() + File.separator + fileNames[j]);
                        ByteArrayOutputStream rawData = new ByteArrayOutputStream();
                        BufferedInputStream messageStream = new BufferedInputStream(new FileInputStream(messageFile));
                        byte[] buffer = new byte[1024];
                        int avail;
                        log.debug("getting message " + fileNames[j]);
                        while ((avail = messageStream.read(buffer)) > 0) {
                            rawData.write(buffer, 0, avail);
                        }
                        log.debug("creating ByteArrayInputStream...");
                        ByteArrayInputStream in = new ByteArrayInputStream(rawData.toByteArray());
                        log.debug("parsing message...");
                        mimeMessage.parseMessage(in);
                        message.setSubject(mimeMessage.getSubject());
                        message.setState(Message.UNREAD);
                        String subject = mimeMessage.getSubject().replaceAll("\t", "");
                        if (subject == null) subject = "";
                        props.setProperty(fileNames[j], subject + "\t" + Message.UNREAD);
                    }
                    messages.add(message);
                } catch (Exception e) {
                    log.error("Could not read message (" + fileNames[j] + ")!", e);
                }
            }
            try {
                props.store(new FileOutputStream(accountInfoFile), "lala");
            } catch (Exception e) {
                log.error("Could not store properties!", e);
            }
            inbox.setMessages(messages);
        }
    }

    private File getAccountStorageDir(String accountName) {
        return new File(defaultStorageDir + File.separator + accountName);
    }

    public boolean storeMessage(String accountName, Message message) {
        try {
            log.debug("loading account info...");
            File accountInfoFile = new File(getAccountStorageDir(accountName).getPath() + File.separator + "info");
            Properties props = new Properties();
            props.load(new FileInputStream(accountInfoFile));
            String messageName = getMD5Hash(message.getMessageId().toString());
            log.debug("saving mail (" + messageName + ")...");
            File accountStorageDir = getAccountStorageDir(accountName);
            File messageStorageFile = new File(accountStorageDir.getPath() + File.separator + "inbox" + File.separator + messageName);
            FileOutputStream save = new FileOutputStream(messageStorageFile);
            save.write(message.getRawData());
            save.close();
            log.debug("mail saved!");
            message.setMessageId(accountName + "\t" + messageName);
            ((AccountPlugin) accounts.get(accountName)).getDefaultInbox().addMessage(message);
            log.debug("props are: " + props.getProperty(messageName));
            log.debug("saving props... state is " + message.getState());
            props.setProperty(messageName, message.getSubject() + "\t" + message.getState());
            props.store(new FileOutputStream(accountInfoFile), "lala");
            log.debug("props saved!");
        } catch (Exception e) {
            log.error("Could not save message with id " + message.getMessageId(), e);
            return false;
        }
        return true;
    }

    public boolean updateMessage(String accountName, Message message) {
        try {
            log.debug("loading account info...");
            File accountInfoFile = new File(getAccountStorageDir(accountName).getPath() + File.separator + "info");
            Properties props = new Properties();
            props.load(new FileInputStream(accountInfoFile));
            String messageName = message.getMessageId().toString().split("\t")[1];
            log.debug("saving mail (" + messageName + ")...");
            File accountStorageDir = getAccountStorageDir(accountName);
            File messageStorageFile = new File(accountStorageDir.getPath() + File.separator + "inbox" + File.separator + messageName);
            if (!messageStorageFile.exists()) {
                log.error("Tried to update a message that is not stored!");
                return false;
            }
            FileOutputStream save = new FileOutputStream(messageStorageFile);
            save.write(message.getRawData());
            save.close();
            log.debug("mail saved!");
            message.setMessageId(accountName + "\t" + messageName);
            ((AccountPlugin) accounts.get(accountName)).getDefaultInbox().addMessage(message);
            log.debug("props are: " + props.getProperty(messageName));
            log.debug("saving props... state is " + message.getState());
            props.setProperty(messageName, message.getSubject() + "\t" + message.getState());
            props.store(new FileOutputStream(accountInfoFile), "lala");
            log.debug("props saved!");
        } catch (Exception e) {
            log.error("Could not save message with id " + message.getMessageId(), e);
            return false;
        }
        return true;
    }

    private String getMD5Hash(String name) {
        byte[] md5bytes = md5.digest(name.getBytes());
        String md5String = new String();
        int upper;
        int lower;
        for (int i = 0; i < md5bytes.length; i++) {
            upper = (md5bytes[i] & (15 << 4)) >> 4;
            lower = md5bytes[i] & 15;
            md5String += Integer.toHexString(upper);
            md5String += Integer.toHexString(lower);
        }
        return md5String;
    }

    public Message[] findMessages(Criteria[] criteria) {
        log.error("Method deleteMessage(MessageId messageId) is not yet implemented!");
        throw new java.lang.UnsupportedOperationException("Method findMessage() not yet implemented.");
    }

    public boolean deleteMessage(String messageId) {
        log.error("Method deleteMessage(MessageId messageId) is not yet implemented!");
        return false;
    }

    public Message getMessage(String messageId) {
        Message message = null;
        log.debug("messageId is: " + messageId);
        String accountName = messageId.toString().split("\t")[0];
        String messageName = messageId.toString().split("\t")[1];
        try {
            File accountStorageDir = this.getAccountStorageDir(((AccountPlugin) accounts.get(accountName)).getName());
            File possibleMessage = new File(accountStorageDir.getPath() + File.separator + "inbox" + File.separator + messageName);
            log.debug("possible message is " + possibleMessage);
            if (possibleMessage.exists()) {
                message = new Message();
                ByteArrayOutputStream rawData = new ByteArrayOutputStream();
                BufferedInputStream messageStream = new BufferedInputStream(new FileInputStream(possibleMessage));
                byte[] buffer = new byte[1024];
                int avail;
                log.debug("getting message " + messageId);
                while ((avail = messageStream.read(buffer)) > 0) {
                    rawData.write(buffer, 0, avail);
                }
                MailHelper mimeMessage = new MailHelper();
                ByteArrayInputStream in = new ByteArrayInputStream(rawData.toByteArray());
                mimeMessage.parseMessage(in);
                message.setRawData(rawData.toByteArray());
                message.setSubject(mimeMessage.getSubject());
                message.setMessageId(messageId);
                if (mimeMessage.getContent() instanceof javax.mail.internet.MimeMultipart) {
                    ((javax.mail.internet.MimeMultipart) mimeMessage.getContent()).writeTo(rawData);
                    message.setContent(new String(rawData.toByteArray()));
                } else {
                    message.setContent((String) mimeMessage.getContent());
                }
                log.debug(mimeMessage.getContent().getClass());
            }
        } catch (Exception e) {
            log.error("Could not read message with id " + messageId, e);
        }
        return message;
    }
}
