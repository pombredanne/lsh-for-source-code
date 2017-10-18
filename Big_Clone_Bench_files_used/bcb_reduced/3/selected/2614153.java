package ursus.server.plugins.core;

import ursus.common.*;
import ursus.server.*;
import ursus.server.persistence.*;
import ursus.server.plugins.core.persistence.*;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Anthony
 */
public class LoginListener implements Listener {

    public static final String TYPE = "login";

    public static final String ALGORITHM = "MD5";

    public static final String MAP_TYPE = "user";

    public static final String RETURN_TYPE = "text";

    public String getType() {
        return TYPE;
    }

    public void execute(Communicator com, Object o) {
        try {
            String[] args = (String[]) o;
            SQLMetaDAO dao = (SQLMetaDAO) PersistenceManager.getDAOFactory().getDAObject(SQLUsersDAO.TYPE);
            List list = (List) dao.get(SQLUserTAO.FIELD_NAME, args[0]);
            SQLUserTAO tao = (SQLUserTAO) list.get(0);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(ALGORITHM);
            byte[] check = digest.digest(args[1].getBytes());
            byte[] bytes = tao.getPassword();
            ServerHandler handler = (ServerHandler) com;
            if (check.length == bytes.length) {
                if (recursiveCheck(check, bytes, 0)) {
                    Map map = handler.getObjectMap();
                    map.put(MAP_TYPE, tao);
                    handler.write(RETURN_TYPE, "User " + tao.getName() + " logged in!");
                } else {
                    handler.write(RETURN_TYPE, "Login failed!");
                    handler.disconnect();
                }
            } else {
                com.write(RETURN_TYPE, "Login failed!");
                handler.disconnect();
            }
        } catch (DBException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    protected boolean recursiveCheck(byte[] check, byte[] bytes, int index) {
        if (check[index] == bytes[index]) if (++index < bytes.length) return recursiveCheck(check, bytes, index); else return true; else return false;
    }
}
