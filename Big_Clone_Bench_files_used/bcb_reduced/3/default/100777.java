import java.sql.*;
import java.util.*;
import javax.swing.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DBInterface {

    private static final String DB_URL = "jdbc:mysql://127.0.0.1/cdstore";

    private static final String DB_USER = "cdstoreGuest";

    private static final String DB_PASS = "cdstoreGuestpass";

    private Connection connection;

    private boolean isGuest;

    private Hashtable<Integer, String> errMsgs = new Hashtable<Integer, String>();

    private Hashtable<Integer, DeviceType> deviceTypes = null;

    public DBInterface() throws Exception {
        Class.forName("org.gjt.mm.mysql.Driver").newInstance();
        if (makeGuestConnection() == false) {
            throw new Exception("Could not connect to database");
        }
    }

    private boolean isPrivilegedUser() {
        if (isGuest) {
            return false;
        }
        return true;
    }

    private boolean makePrivilegedConnection() {
        if (isPrivilegedUser()) {
            return true;
        }
        while (true) {
            LoginManager lM = new LoginManager();
            Vector<String> v = lM.getCredentials();
            if (v != null) {
                try {
                    Connection c = DriverManager.getConnection(DB_URL, "cdstore_" + v.get(0), v.get(1));
                    connection = c;
                    CDStore.loginManager = lM;
                    isGuest = false;
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (JOptionPane.showConfirmDialog(null, "Invalid password. Would you like to retry?", "Retry?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }
        }
    }

    private boolean makeGuestConnection() {
        isGuest = true;
        CDStore.loginManager = null;
        try {
            connection = null;
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String makePassword(String password) {
        try {
            String encryptedPassword = "*";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(md.digest(password.getBytes()));
            for (int i = 0; i < digest.length; i++) {
                String s = Integer.toHexString(digest[i] & 0xFF).toUpperCase();
                if (s.length() == 1) {
                    s = "0" + s;
                }
                encryptedPassword = encryptedPassword + s;
            }
            return encryptedPassword;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No algorithm. FIXME");
        }
        return null;
    }

    public int userRegister(String newUsername, String newPassword, String newRealName) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        if ((newUsername == null) || (newPassword == null) || (newRealName == null)) {
            System.out.println("Null field provided");
            return -1;
        }
        newUsername = newUsername.trim();
        newPassword = newPassword.trim();
        newRealName = newRealName.trim();
        if ((newUsername.length() == 0) || (newPassword.length() == 0) || (newRealName.length() == 0)) {
            System.out.println("Zero length field provided");
            return -1;
        }
        int retval = -1;
        try {
            String sql = "CALL ui_userRegister(" + "\"" + newUsername + "\", " + "\"" + makePassword(newPassword) + "\", " + "\"" + newRealName + "\");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int userDeregister(String usernameToDelete) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        if (usernameToDelete == null) {
            return -1;
        }
        usernameToDelete = usernameToDelete.trim();
        if (usernameToDelete.length() == 0) {
            return -1;
        }
        int retval = -1;
        try {
            String sql = "CALL ui_userDeregister(" + "\"" + usernameToDelete + "\");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            if (CDStore.loginManager != null) {
                Vector<String> creds = CDStore.loginManager.getCredentials();
                if (usernameToDelete.compareToIgnoreCase(creds.get(0)) == 0) {
                    makeGuestConnection();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int userModify(String usernameToModify, String newPassword, String newRealName) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        if (usernameToModify == null) {
            return -1;
        }
        usernameToModify = usernameToModify.trim();
        if (usernameToModify.length() == 0) {
            return -1;
        }
        if (newPassword != null) {
            newPassword = newPassword.trim();
            if (newPassword.length() == 0) {
                newPassword = null;
            }
        }
        if (newRealName != null) {
            newRealName = newRealName.trim();
            if (newRealName.length() == 0) {
                newRealName = null;
            }
        }
        if ((newPassword == null) && (newRealName == null)) {
            return -1;
        }
        int retval = -1;
        try {
            String sql = "CALL ui_userModify(" + "\"" + usernameToModify + "\", " + (newPassword == null ? "NULL, " : "\"" + makePassword(newPassword) + "\", ") + (newRealName == null ? "NULL);" : "\"" + newRealName + "\");");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public Vector<User> userSearch(String partialUsername, String partialRealName) {
        if (partialUsername != null) {
            partialUsername = partialUsername.trim();
            if (partialUsername.length() == 0) {
                partialUsername = null;
            }
        }
        if ((partialRealName != null) && (partialRealName.length() == 0)) {
            partialRealName = partialRealName.trim();
            if (partialRealName.length() == 0) {
                partialRealName = null;
            }
        }
        Vector<User> users = new Vector<User>();
        try {
            String sql = "CALL ui_userSearch(" + (partialUsername == null ? "NULL, " : "\"" + partialUsername + "\", ") + (partialRealName == null ? "NULL);" : "\"" + partialRealName + "\");");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                User u = new User();
                u.userid = rs.getInt("userid");
                u.username = rs.getString("username");
                u.realName = rs.getString("realname");
                users.add(u);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return users;
    }

    public int deviceDeregister(int deviceID) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_deviceDeregister(" + deviceID + ");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int deviceModify(int deviceID, String deviceName) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        if (deviceName == null) {
            return -1;
        }
        deviceName = deviceName.trim();
        if (deviceName.length() == 0) {
            return -1;
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_deviceModify(" + deviceID + ", " + "\"" + deviceName + "\");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public Vector<Device> deviceSearch(String partialDeviceName, Integer deviceID, Integer deviceType) {
        if ((partialDeviceName != null) && (partialDeviceName.length() == 0)) {
            partialDeviceName = partialDeviceName.trim();
            if (partialDeviceName.length() == 0) {
                partialDeviceName = null;
            }
        }
        Vector<Device> devices = new Vector<Device>();
        try {
            String sql = "CALL ui_deviceSearch(" + (partialDeviceName == null ? "NULL, " : "\"" + partialDeviceName + "\", ") + (deviceID == null ? "NULL, " : "\"" + deviceID + "\", ") + (deviceType == null ? "NULL);" : deviceType + ");");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Device d = new Device();
                d.deviceID = rs.getInt("deviceid");
                d.deviceType = rs.getInt("typeid");
                d.serial = rs.getInt("serial");
                d.lastSeen = rs.getTimestamp("lastseen");
                d.deviceName = rs.getString("name");
                devices.add(d);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return devices;
    }

    public Hashtable<Integer, DeviceType> deviceGetTypes() {
        if (deviceTypes != null) {
            return deviceTypes;
        }
        Hashtable<Integer, DeviceType> types = new Hashtable<Integer, DeviceType>();
        try {
            String sql = "CALL ui_deviceGetTypes();";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                DeviceType d = new DeviceType();
                d.typeID = rs.getInt("typeid");
                d.vendor = rs.getString("vendor");
                d.product = rs.getString("product");
                d.maxSlots = rs.getInt("maxslot");
                types.put(d.typeID, d);
            }
            rs.close();
            stmt.close();
            deviceTypes = types;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return deviceTypes;
    }

    public int diskRegister() {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_diskRegister();";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int diskDeregister(int diskID) {
        int retval = -1;
        try {
            String sql = "SELECT ui_diskDeregister(" + diskID + ");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int diskModify(int diskID, Boolean diskEjected, String diskAssignedTo, Integer diskContainerDeviceID, Integer diskContainerSlot) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        if (diskAssignedTo != null) {
            diskAssignedTo = diskAssignedTo.trim();
            if (diskAssignedTo.length() == 0) {
                diskAssignedTo = null;
            }
        }
        if ((diskEjected == null) && (diskAssignedTo == null) && (diskContainerDeviceID == null) && (diskContainerSlot == null)) {
            return -1;
        }
        if (diskEjected != null) {
            if (diskEjected) {
                if ((diskContainerDeviceID != null) || (diskContainerSlot != null)) {
                    return -1;
                }
            } else {
                if (diskAssignedTo != null) {
                    return -1;
                }
            }
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_diskModify(" + diskID + ", " + (diskEjected == null ? "NULL, " : diskEjected + ", ") + (diskAssignedTo == null ? "NULL, " : "\"" + diskAssignedTo + "\", ") + (diskContainerDeviceID == null ? "NULL, " : diskContainerDeviceID + ", ") + (diskContainerSlot == null ? "NULL);" : diskContainerSlot + ");");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public Vector<Disk> diskSearch(Integer diskID, Boolean diskEjected, String diskAssignedTo, Integer diskContainerDeviceID) {
        if (diskAssignedTo != null) {
            diskAssignedTo = diskAssignedTo.trim();
            if (diskAssignedTo.length() == 0) {
                diskAssignedTo = null;
            }
        }
        Vector<Disk> disks = new Vector<Disk>();
        try {
            String sql = "CALL ui_diskSearch(" + (diskID == null ? "NULL, " : diskID) + (diskEjected == null ? "NULL, " : diskEjected + ", ") + (diskAssignedTo == null ? "NULL, " : "\"" + diskAssignedTo + "\", ") + (diskContainerDeviceID == null ? "NULL);" : diskContainerDeviceID + ");");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Disk d = new Disk();
                d.diskID = rs.getInt("diskid");
                d.deviceid = rs.getInt("deviceid");
                if (rs.wasNull()) d.deviceid = null;
                d.slot = rs.getInt("slot");
                if (rs.wasNull()) d.slot = null;
                d.eventtime = rs.getTimestamp("eventtime");
                if (rs.wasNull()) d.eventtime = null;
                d.userid = rs.getInt("userid");
                if (rs.wasNull()) d.userid = null;
                disks.add(d);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return disks;
    }

    public Vector<Association> diskGetUnassociated() {
        Vector<Association> unassociated = new Vector<Association>();
        try {
            String sql = "CALL ui_diskGetUnassociated();";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Association a = new Association();
                a.deviceID = rs.getInt("deviceid");
                if (rs.wasNull()) a.deviceID = null;
                a.deviceName = rs.getString("devicename");
                if (rs.wasNull()) a.deviceName = null;
                a.slot = rs.getInt("slot");
                if (rs.wasNull()) a.slot = null;
                a.diskID = rs.getInt("diskid");
                if (rs.wasNull()) a.diskID = null;
                a.eventtime = rs.getTimestamp("eventtime");
                if (rs.wasNull()) a.eventtime = null;
                a.username = rs.getString("username");
                if (rs.wasNull()) a.username = null;
                unassociated.add(a);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return unassociated;
    }

    public int diskAssociate(int diskID, int deviceID, int slot) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_diskAssociate(" + diskID + ", " + deviceID + ", " + slot + ");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int diskEject(int diskID) {
        if (makePrivilegedConnection() == false) {
            System.out.println("No privileged connection made.");
            return -1;
        }
        int retval = -1;
        try {
            String sql = "SELECT ui_diskEject(" + diskID + ");";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                retval = rs.getInt(1);
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return retval;
    }

    public int getVersion() {
        int version = 0;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT general_getVersion();");
            if (rs.next()) {
                int v = rs.getInt(1);
                if (v > 0) {
                    version = v;
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return version;
    }

    public String getErrorDescription(int errval) {
        String errMsg = "Unspecified error message (" + errval + ").";
        if (errMsgs.containsKey(errval)) {
            return errMsgs.get(errval);
        }
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT general_getErrorDescription(" + errval + ");");
            if (rs.next()) {
                String msg = rs.getString(1);
                if (msg != null) {
                    errMsg = msg;
                }
            }
            rs.close();
            stmt.close();
            errMsgs.put(errval, errMsg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return errMsg;
    }

    public void displayErrorMessage(int errval) {
        String errMsg = getErrorDescription(errval);
        if (errMsg != null) {
            JOptionPane.showMessageDialog(null, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
