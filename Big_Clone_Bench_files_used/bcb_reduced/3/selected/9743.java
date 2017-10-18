package tw.bennu.feeler.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tw.bennu.feeler.db.service.DbService;
import tw.bennu.feeler.db.service.IFeelerUser;
import tw.bennu.feeler.log.service.LogService;
import tw.bennu.feeler.user.service.UserService;

public class DbUserServiceImpl implements UserService {

    private LogService logger = null;

    private DbService dbServ = null;

    private List<UUID> localUUIDList = new ArrayList<UUID>();

    @Override
    public UUID getEntityUUID(String username) {
        IFeelerUser user = this.getDbServ().queryFeelerUser(username);
        if (user == null) {
            return null;
        } else {
            return user.getUuid();
        }
    }

    @Override
    public boolean register(String username, String password) {
        this.getLogger().info(DbUserServiceImpl.class, ">>>rigister " + username + "<<<");
        try {
            if (this.getDbServ().queryFeelerUser(username) != null) {
                return false;
            }
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            String passwordMd5 = new String(md5.digest());
            this.getDbServ().addFeelerUser(username, passwordMd5);
            return this.identification(username, password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean identification(String username, String password) {
        this.getLogger().info(DbUserServiceImpl.class, ">>>identification " + username + "<<<");
        try {
            IFeelerUser user = this.getDbServ().queryFeelerUser(username);
            if (user == null) {
                return false;
            }
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            if (user.getPassword().equals(new String(md5.digest()))) {
                if (!this.localUUIDList.contains(user.getUuid())) {
                    this.localUUIDList.add(user.getUuid());
                }
                return true;
            } else {
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<UUID> getLocalUUIDList() {
        return this.localUUIDList;
    }

    public DbService getDbServ() {
        return dbServ;
    }

    public void setDbServ(DbService dbServ) {
        this.dbServ = dbServ;
    }

    public LogService getLogger() {
        return logger;
    }

    public void setLogger(LogService logger) {
        this.logger = logger;
    }
}
