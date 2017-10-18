package Utilities;

import Beans.PermissionsBean;
import Beans.ServerBean;
import Managers.DatabaseManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.ws.WebServiceException;

/**
 * This class contains various static functions.
 *
 * @author Angel Sanadinov
 */
public class Functions {

    /** Regular expression for matching a valid media and machines location (file name) */
    public static final Pattern locationPattern = Pattern.compile("[^/\"\\\\|`?*<>:\n\t\f\r]+");

    /** Regular expression for retrieving "interesting" VirtualBox exception data */
    public static final Pattern virtualBoxExceptionPattern = Pattern.compile("VirtualBox\\serror:\\s(.*)\\s\\(0x(.*)\\)");

    private Functions() {
    }

    /**
     * Hashes the supplied plaintext password and returns the hash.
     *
     * @param plainTextPassword the password to be hashed
     * @param hashingAlgorithm the algorithm to be used when hashing the password
     * @param charsetName the charset to be used by the hashing function
     * @return the hashed password
     * @throws NoSuchAlgorithmException if the hashing algorithm is not implemented by any provider
     * @throws UnsupportedEncodingException if the charset that is used is not supported
     */
    public static String hashPassword(final String plainTextPassword, String hashingAlgorithm, String charsetName) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String result = null;
        MessageDigest digest = MessageDigest.getInstance(hashingAlgorithm);
        result = new String(digest.digest(plainTextPassword.getBytes(charsetName)), charsetName);
        return result;
    }

    /**
     * Checks if the supplied user id is valid.
     *
     * @param userId the user id to be checked
     * @return <code>true</code> if the id is valid or <code>false</code> otherwise
     */
    public static boolean isUserIdValid(final int userId) {
        if (userId > Constants.INVALID_USER_ID) return true; else return false;
    }

    /**
     * Checks if the supplied server id is valid.
     *
     * @param serverId the server id to be checked
     * @return <code>true</code> if the id is valid or <code>false</code> otherwise
     */
    public static boolean isServerIdValid(final int serverId) {
        if (serverId > Constants.INVALID_SERVER_ID) return true; else return false;
    }

    /**
     * Checks if the supplied permissions are valid.
     *
     * @param permissions the permissions to be checked
     * @return <code>true</code> if the permissions are valid or <code>false</code> otherwise
     */
    public static boolean arePermissionsValid(final String permissions) {
        if (permissions != null && permissions.length() == 3) {
            try {
                Integer.parseInt(permissions);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else return false;
    }

    /**
     * Checks if the supplied timestamp is valid.
     *
     * @param timestamp the timestamp to be checked
     * @return <code>true</code> if the timestamp is valid or <code>false</code> otherwise
     */
    public static boolean isTimestampValid(final Timestamp timestamp) {
        if (timestamp != null) return true; else return false;
    }

    /**
     * Checks if the supplied UUID is valid. <br><br>
     *
     * <b>Note:</b> <i>UUIDs are used for virtual resources and are created by VirtualBox.</i>
     *
     * @param uuid the ID to be checked
     * @return <code>true</code> if the UUID is valid or <code>false</code> otherwise
     */
    public static boolean isUUIDValid(final String uuid) {
        if (uuid != null) {
            try {
                UUID.fromString(uuid);
                return true;
            } catch (IllegalArgumentException e) {
            }
        } else ;
        return false;
    }

    /**
     * Checks if the specified location is valid. <br><br>
     *
     * These locations are used to define where an IMedium image or an IMachine is on the host.
     *
     * @param location the location to be checked
     * @return <code>true</code> if the location is valid or <code>false</code> otherwise
     */
    public static boolean isLocationValid(final String location) {
        return locationPattern.matcher(location).matches();
    }

    /**
     * Checks that the controller port is within valid range. <br><br>
     *
     * The unity of the sets of valid controller ports for each controller type
     * gives a range from 0 to 29 (inclusive).
     *
     * @param port the port number to be checked
     * @return <code>true</code> if the controller port number is within range
     *         or <code>false</code> otherwise
     */
    public static boolean isControllerPortValid(final int port) {
        if (port >= 0 && port <= 29) return true; else return false;
    }

    /**
     * Checks that the controller slot is valid. <br><br>
     *
     * 0 is a valid controller slot, with 1 also valid for some controller types.
     *
     * @param slot the slot number to be checked
     * @return <code>true</code> if the slot number is valid or <code>false</code> otherwise
     */
    public static boolean isControllerSlotValid(final int slot) {
        if (slot == 0 || slot == 1) return true; else return false;
    }

    /**
     * Checks if the specified user is considered to be server manager. <br><br>
     *
     * The manager status is verified by checking the permissions of the user on
     * a specific server. If the user has full access for machines, media and networks
     * that user is considered to have manager status.
     *
     * @param userId the id of the user to be checked
     * @param permissions the permissions of that user on a server
     * @return <code>true</code> if the user is a server manager or <code>false</code>
     *         otherwise
     */
    public static boolean isUserServerManager(final int userId, final PermissionsBean permissions) {
        String managerPermissions = "777";
        if (userId == permissions.getUserId() && permissions.getMachinesPermissions().equals(managerPermissions) && permissions.getMediaPermissions().equals(managerPermissions) && permissions.getNetworksPermissions().equals(managerPermissions)) return true; else return false;
    }

    /**
     * Allocates an available range of ports on the specified server. <br><br>
     * 
     * The database is modified accordingly and no further action is needed, except
     * to set the returned VRDP ports in the machine's VRDP server. <br><br>
     *
     * <b>Note:</b> <i>This function should be called on machine creation only.</i>
     *
     * @param manager the database manager to be used for the operation
     * @param serverId the ID of the server on which the machine resides
     * @return the allocated VRDP ports which can be directly set in the machine's
     *         VRDP server configuration
     * @throws IllegalArgumentException if some or all of the input is invalid
     */
    public static String allocateVRDPPorts(DatabaseManager manager, int serverId) throws IllegalArgumentException {
        if (manager != null && isServerIdValid(serverId)) {
            ServerBean serverData = manager.getServer(serverId);
            if (serverData != null && serverData.isValid()) {
                Random generator = new Random();
                int numberOfSlots = (serverData.getVrdpPortsRangeHigh() - serverData.getVrdpPortsRangeLow()) / serverData.getVrdpPortsPerMachine();
                String[] slots = serverData.getVrdpAllocatedSlots().split(";");
                if (slots.length >= numberOfSlots) return null; else ;
                boolean done = false;
                int generatedSlot = -1;
                Pattern newPattern = null;
                StringBuilder expression = new StringBuilder();
                do {
                    generatedSlot = generator.nextInt(numberOfSlots);
                    expression.delete(0, expression.length());
                    expression.append(";");
                    expression.append(generatedSlot);
                    expression.append(";|;");
                    expression.append(generatedSlot);
                    expression.append("$");
                    newPattern = Pattern.compile(expression.toString());
                    if (!newPattern.matcher(serverData.getVrdpAllocatedSlots()).find()) done = true; else ;
                } while (!done);
                if (generatedSlot >= 0) {
                    manager.updateServerVRDPAllocationSlots(serverId, serverData.getVrdpAllocatedSlots() + ";" + generatedSlot);
                    int firstPort = serverData.getVrdpPortsRangeLow() + generatedSlot * serverData.getVrdpPortsPerMachine();
                    String result = "";
                    for (int i = 0; i < serverData.getVrdpPortsPerMachine(); i++) result += (firstPort + i) + ",";
                    return result.substring(0, result.length() - 1);
                } else return null;
            } else throw new IllegalArgumentException("Invalid server ID, server does not exist.");
        } else throw new IllegalArgumentException("No database manager or invalid server ID supplied.");
    }

    /**
     * Frees the supplied VRDP ports from the specified server. <br><br>
     * The database is modified accordingly and no further action is needed. <br><br>
     *
     * <b>Note:</b> <i>This function should be called on machine removal only.</i>
     *
     * @param manager the database manager to be used for the operation
     * @param serverId the ID of the server on which the machine resides
     * @param ports the current VRDP ports of the machine
     *
     * @throws IllegalArgumentException if some or all of the input is invalid
     */
    public static void freeVRDPPorts(DatabaseManager manager, int serverId, String ports) throws IllegalArgumentException {
        if (ports == null || ports.trim().equals("")) return; else ;
        if (manager != null && isServerIdValid(serverId)) {
            ServerBean serverData = manager.getServer(serverId);
            if (serverData != null && serverData.isValid()) {
                int slot = (Integer.parseInt(ports.split(",")[0]) - serverData.getVrdpPortsRangeLow()) / serverData.getVrdpPortsPerMachine();
                StringBuilder expression = new StringBuilder();
                expression.append(";");
                expression.append(slot);
                expression.append("(;)|;");
                expression.append(slot);
                expression.append("$");
                manager.updateServerVRDPAllocationSlots(serverId, serverData.getVrdpAllocatedSlots().replaceFirst(expression.toString(), "$1"));
            } else throw new IllegalArgumentException("Invalid server ID, server does not exist.");
        } else throw new IllegalArgumentException("No database manager or invalid server ID supplied.");
    }

    /**
     * Parses the supplied VirtualBox exception. <br><br>
     *
     * The exception's code and message are retrieved, and are used to build an
     * <code>ApplicationException</code> object.
     *
     * @param exception the VirtualBox exception
     * @return the generated application exception object
     */
    public static ApplicationException parseVirtualBoxException(final WebServiceException exception) {
        ApplicationException result = null;
        Matcher exceptionMatcher = virtualBoxExceptionPattern.matcher(exception.getMessage());
        if (exceptionMatcher.find()) result = new ApplicationException(exceptionMatcher.group(2), exceptionMatcher.group(1)); else result = new ApplicationException("-1", "Failed to parse exception message: " + exception.getMessage());
        return result;
    }
}
