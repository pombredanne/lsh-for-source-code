package vlan.webgame.manage.services;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.jmantis.core.utils.UUID;
import org.jmantis.web.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vlan.webgame.manage.LoginInfo;
import vlan.webgame.manage.LoginManager;
import vlan.webgame.manage.dao.AdminDao;
import vlan.webgame.manage.dao.AdminLoginDao;
import vlan.webgame.manage.entity.Admin;
import vlan.webgame.manage.entity.AdminLogin;

public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private static final int MAX = 50;

    private static final String salt = "ZKNugmkm";

    private AdminDao adminDao;

    private AdminLoginDao adminLoginDao;

    /**
	 * 缓冲
	 */
    private Map<String, LoginInfo> loginCache = new LinkedHashMap<String, LoginInfo>() {

        /**
		 * 
		 */
        private static final long serialVersionUID = 8757075088528107811L;

        @Override
        protected boolean removeEldestEntry(Entry<String, LoginInfo> eldest) {
            return size() > MAX;
        }
    };

    public void cleanCache(AdminLogin login) {
        loginCache.remove(login.getLoginId());
    }

    public LoginInfo getAdmin(String uuid) {
        LoginInfo info = loginCache.get(uuid);
        if (info == null) {
            AdminLogin login = adminLoginDao.get(uuid);
            if (uuid != null) {
                Admin a = adminDao.get(login.getName());
                info = new LoginInfo();
                info.setLogin(login);
                info.setAdmin(a);
                loginCache.put(uuid, info);
                log.debug("重新加载管理员登陆信息成功!LoginInfo:{}", login);
            }
        }
        return info;
    }

    public Admin login(String user, String password, String remoteInfo, HttpResponse resp) {
        if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
            Admin a = adminDao.get(user);
            password = hash(password);
            if (a != null && password.equals(a.getPassword())) {
                String uuid = UUID.generateNoSep();
                LoginInfo info = new LoginInfo();
                AdminLogin login = new AdminLogin();
                login.setLoginId(uuid);
                login.setLoginTime(new Timestamp(System.currentTimeMillis()));
                login.setName(user);
                login.setRemoteInfo(remoteInfo);
                info.setLogin(login);
                info.setAdmin(a);
                loginCache.put(uuid, info);
                adminLoginDao.insert(login);
                resp.addCookie(LoginManager.ADMIN_COOKIS_NAME, uuid);
                log.debug("管理员成功!LoginInfo:{}", info);
                return a;
            }
        }
        return null;
    }

    public static final String hash(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
            password = password + salt;
            md.update(password.getBytes("utf8"));
            byte[] b = md.digest();
            StringBuilder output = new StringBuilder(32);
            for (int i = 0; i < b.length; i++) {
                String temp = Integer.toHexString(b[i] & 0xff);
                if (temp.length() < 2) {
                    output.append("0");
                }
                output.append(temp);
            }
            return output.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setAdminDao(AdminDao adminDao) {
        this.adminDao = adminDao;
    }

    public void setAdminLoginDao(AdminLoginDao adminLoginDao) {
        this.adminLoginDao = adminLoginDao;
    }
}
