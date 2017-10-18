package WaWiSys.Data;

import java.security.MessageDigest;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ObjectUser extends DataObjectBase {

    private String login;

    private String pwHash;

    private boolean isAdmin;

    private long defaultLanguage;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        if (!login.equals(this.login)) {
            this.login = login;
            this.objectChanged = true;
        }
    }

    public String getPwHash() {
        return pwHash;
    }

    public void setPwHash(String pwHash) {
        if (!pwHash.equals(this.pwHash)) {
            this.pwHash = pwHash;
            this.objectChanged = true;
        }
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        if (this.isAdmin != isAdmin) {
            this.isAdmin = isAdmin;
            this.objectChanged = true;
        }
    }

    public long getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(long defaultLanguage) {
        if (defaultLanguage != this.defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
            objectChanged = true;
        }
    }

    public boolean save() {
        boolean result;
        Dictionary<String, Object> values = new Hashtable<String, Object>();
        values.put("Login", getLogin());
        values.put("PWMD5", getPwHash());
        values.put("IsAdmin", isAdmin());
        values.put("DefaultLanguage", getDefaultLanguage());
        long id = dataAccess.save("user", getId(), values);
        setId(id);
        result = id > 0;
        objectChanged = !result;
        return result;
    }

    public static ObjectUser getById(Long id) {
        ObjectUser user = new ObjectUser();
        Dictionary<String, Object> values = dataAccess.getData("user", id);
        user.setId((Long) values.get("ID"));
        user.setLogin((String) values.get("Login"));
        user.setPwHash((String) values.get("PWMD5"));
        user.setAdmin((Boolean) values.get("IsAdmin"));
        user.setDefaultLanguage((Long) values.get("DefaultLanguage"));
        user.objectChanged = false;
        return user;
    }

    public static List<Long> getAllIds() {
        return dataAccess.getAllIds("user");
    }

    public static String getMD5Hash(String input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(input.getBytes());
            byte[] result = md5.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                String byteStr = Integer.toHexString(result[i]);
                String swap = null;
                switch(byteStr.length()) {
                    case 1:
                        swap = "0" + Integer.toHexString(result[i]);
                        break;
                    case 2:
                        swap = Integer.toHexString(result[i]);
                        break;
                    case 8:
                        swap = (Integer.toHexString(result[i])).substring(6, 8);
                        break;
                }
                hexString.append(swap);
            }
            return hexString.toString();
        } catch (Exception ex) {
            System.out.println("Fehler beim Ermitteln eines Hashs (" + ex.getMessage() + ")");
        }
        return null;
    }

    public static ObjectUser getUser(String login, String pwHash) {
        ObjectUser result = null;
        List<Long> ids = getAllIds();
        for (Long id : ids) {
            ObjectUser user = getById(id);
            if (user.getLogin().equals(login) && user.getPwHash().equals(pwHash)) {
                result = user;
                break;
            }
        }
        return result;
    }

    public String toString() {
        return super.toString() + " (Benutzer, Login: " + login + ", PWMD5: " + pwHash + ", Admin: " + isAdmin + ", defaultLanguage: " + defaultLanguage + ")";
    }
}
