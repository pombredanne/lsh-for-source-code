package com.smssalama;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import com.smssalama.security.Security;
import com.smssalama.storage.contact.ContactsDisplayNameCache;
import com.smssalama.storage.framework.KeyStorageEncryptor;
import com.smssalama.storage.framework.ObjectStorageException;
import com.smssalama.storage.framework.ObjectStore;
import com.smssalama.storage.framework.Storable;
import com.smssalama.storage.framework.StorageEncryptor;

/**
 * Implementation of IUserProfile
 * 
 * @author Arnold P. Minde
 *
 */
public class UserProfile extends Storable implements IUserProfile {

    private static final String CONFIG_FILE = "SMSSalamaConfig";

    private static final String STORE_ID_MESSAGES = "_messages";

    private static final String STORE_ID_CONTACTS = "_contacts";

    static ObjectStore settingsStorage;

    final Hashtable userStores = new Hashtable();

    ContactsDisplayNameCache displayNameCache;

    private String profileName;

    private String passwordHash;

    private String password;

    private String locale;

    public UserProfile() {
    }

    private UserProfile(String profileName) {
        this.profileName = profileName;
    }

    protected void store(DataOutputStream stream) throws IOException, ObjectStorageException {
        stream.writeUTF(this.profileName != null ? this.profileName : "");
        stream.writeUTF(this.passwordHash != null ? this.passwordHash : "");
        stream.writeUTF(this.locale != null ? this.locale : "");
    }

    protected void load(DataInputStream stream) throws IOException, ObjectStorageException {
        this.profileName = stream.readUTF();
        this.passwordHash = stream.readUTF();
        this.locale = stream.readUTF();
    }

    protected synchronized ObjectStore getStore(String id) {
        if (this.password == null) {
            throw new RuntimeException("Not authenticated");
        }
        ObjectStore store = (ObjectStore) this.userStores.get(id);
        if (store == null) {
            try {
                store = new ObjectStore(id, new KeyStorageEncryptor(this.password.getBytes()));
                this.userStores.put(id, store);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
        return store;
    }

    public synchronized ObjectStore getContactsStore() {
        return getStore(UserProfile.STORE_ID_CONTACTS);
    }

    public synchronized ObjectStore getMessagesStore() {
        return getStore(UserProfile.STORE_ID_MESSAGES);
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        String hash = UserProfile.hashPassword(oldPassword);
        boolean success = hash.equals(this.passwordHash);
        if (!success) {
            throw new RuntimeException("Invalid password");
        }
        KeyStorageEncryptor newEncryptor = new KeyStorageEncryptor(newPassword.getBytes());
        ObjectStore contacts = getContactsStore();
        ObjectStore messages = getContactsStore();
        try {
            contacts.setStorageEncryptor(newEncryptor, true);
            messages.setStorageEncryptor(newEncryptor, true);
            this.passwordHash = UserProfile.hashPassword(newPassword);
            this.password = newPassword;
            UserProfile.getSettingsStore().persistStorable(this);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String hashPassword(String password) {
        byte digest[] = Security.digest(password.getBytes());
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            String c = Integer.toHexString((0xFFFFFFFF & digest[i]) & 0xFF);
            if (c.length() == 1) {
                c = '0' + c;
                hash = hash.append("0").append(c);
            } else {
                hash = hash.append(c);
            }
        }
        System.out.println("Hash:" + hash.toString());
        return hash.toString().toLowerCase();
    }

    public synchronized boolean authenticate(String password) {
        String hash = hashPassword(password);
        boolean success = hash.equals(this.passwordHash);
        if (success) {
            this.password = password;
        }
        return success;
    }

    public synchronized boolean authenticated() {
        return this.password != null;
    }

    protected static synchronized ObjectStore getSettingsStore() {
        if (UserProfile.settingsStorage == null) {
            try {
                UserProfile.settingsStorage = new ObjectStore(UserProfile.CONFIG_FILE, StorageEncryptor.NULL_ENCRYPTOR);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize SettingsStore: " + e.getMessage());
            }
        }
        return UserProfile.settingsStorage;
    }

    public static synchronized boolean initialized() {
        try {
            return UserProfile.getSettingsStore().getNumRecords() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get number of records from SettingsStore " + e.getMessage());
        }
    }

    public static synchronized UserProfile initialize(String password) throws Exception {
        if (UserProfile.initialized()) {
            throw new Exception("UserProfile have already been initialized");
        }
        UserProfile result = new UserProfile("Default");
        result.passwordHash = UserProfile.hashPassword(password);
        result.authenticate(password);
        result.setLocale(SMSSalamaSession.getSession().getLocale());
        UserProfile.getSettingsStore().persistStorable(result);
        return result;
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 * @deprecated
	 */
    public static synchronized UserProfile getSettings() throws Exception {
        if (!UserProfile.initialized()) {
            throw new Exception("UserProfile have not been initialized");
        }
        UserProfile result = (UserProfile) UserProfile.getSettingsStore().getStorable(1);
        return result;
    }

    public static synchronized UserProfile getSettings(String password) {
        if (!UserProfile.initialized()) {
            throw new RuntimeException("UserProfile have not been initialized");
        }
        UserProfile result;
        try {
            result = (UserProfile) UserProfile.getSettingsStore().getStorable(1);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to obtain first record from SettingsStore: " + e.getMessage());
        }
        if (result.authenticate(password)) {
            return result;
        }
        return null;
    }

    public synchronized ContactsDisplayNameCache getDisplayNameCache() {
        if (this.displayNameCache == null) {
            this.displayNameCache = new ContactsDisplayNameCache(this.getContactsStore());
        }
        return this.displayNameCache;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
