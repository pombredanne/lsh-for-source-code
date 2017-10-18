package comm;

import gnu.cajo.utils.extra.TransparentItemProxy;
import interfaces.IClientToMain;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import logic.Configuration;
import common.ConnectionInfo;
import common.UserInfo;

public class MainServerComm {

    public static IClientToMain mainServer = null;

    public static void init(String mainServerIP, Integer mainServerPort) throws Exception {
        mainServer = (IClientToMain) TransparentItemProxy.getItem("//" + mainServerIP + ":" + mainServerPort.toString() + "/MainServer", new Class[] { IClientToMain.class });
    }

    public static String hashPass(String password) {
        byte[] digestedPass = null;
        try {
            byte[] bytesOfMessage;
            bytesOfMessage = password.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            digestedPass = md.digest(bytesOfMessage);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return String.format("%x", new BigInteger(digestedPass));
    }

    public static ConnectionInfo login(String username, String password) throws Exception {
        try {
            return mainServer.logIn(username, hashPass(password));
        } catch (Exception e) {
            MainServerComm.init(Configuration.get("mainServerIP"), Integer.parseInt(Configuration.get("mainServerPort")));
            return mainServer.logIn(username, hashPass(password));
        }
    }

    public static ConnectionInfo addUser(UserInfo user) throws Exception {
        user.setUserPassword(hashPass(user.getUserPassword()));
        try {
            return mainServer.registerUser(user);
        } catch (Exception e) {
            MainServerComm.init(Configuration.get("mainServerIP"), Integer.parseInt(Configuration.get("mainServerPort")));
            return mainServer.registerUser(user);
        }
    }
}
