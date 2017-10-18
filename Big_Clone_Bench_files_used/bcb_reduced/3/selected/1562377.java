package com.sun.satsa.acl;

import com.sun.cdc.io.j2me.apdu.APDUManager;
import java.security.MessageDigest;

/**
 * This class represents access control information manager.
 */
public class AccessControlManager {

    /** Access control information for card slots. */
    private static ACSlot[] ACLInfo;

    /**
     * Initialise ACL information.
     */
    private static synchronized void init() {
        if (ACLInfo != null) {
            return;
        }
        int maxSlot = APDUManager.getSlotCount();
        ACLInfo = new ACSlot[maxSlot];
    }

    /**
     * Initialize ACL information.
     * @param slot int the slot number.
     */
    public static synchronized void init(int slot) {
        if (ACLInfo == null) {
            init();
        }
        if (ACLInfo != null) {
            ACLInfo[slot] = ACSlot.load(slot);
        }
    }

    /**
     * SHA-1 message digest object.
     */
    private static MessageDigest sha;

    /**
     * Synchronization object for message digest calculation.
     */
    private static Object shaSync = new Object();

    /**
     * Calculates hash value.
     * @param inBuf data buffer.
     * @param inOff offset of data in the buffer.
     * @param inLen length of data.
     * @return array containing SHA-1 hash.
     */
    public static byte[] getHash(byte[] inBuf, int inOff, int inLen) {
        synchronized (shaSync) {
            try {
                if (sha == null) {
                    sha = MessageDigest.getInstance("SHA-1");
                }
                sha.reset();
                byte[] hash = new byte[20];
                sha.update(inBuf, inOff, inLen);
                sha.digest(hash, 0, hash.length);
                return hash;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Returns object that should be used for access control verification.
     * @param slot slot number.
     * @param selectAPDU SELECT APDU command data.
     * @param root name of CA that authorized the suite.
     * @return object that can be used to check permissions.
     */
    public static APDUPermissions getAPDUPermissions(int slot, byte[] selectAPDU, String root) {
        if (ACLInfo == null || ACLInfo[slot] == null) {
            APDUPermissions perm;
            perm = new APDUPermissions(null);
            perm.setType(ACLPermissions.ALLOW);
            return perm;
        }
        return (APDUPermissions) ACLInfo[slot].getACLPermissions(true, selectAPDU, root);
    }

    /**
     * Returns object that should be used for access control verification.
     * @param slot slot number.
     * @param selectAPDU SELECT APDU command data.
     * @param root name of CA that authorized the suite.
     * @return object that can be used to check permissions.
     */
    public static JCRMIPermissions getJCRMIPermissions(int slot, byte[] selectAPDU, String root) {
        if (ACLInfo == null || ACLInfo[slot] == null) {
            JCRMIPermissions perm = new JCRMIPermissions(null);
            perm.setType(ACLPermissions.ALLOW);
            return perm;
        }
        return (JCRMIPermissions) ACLInfo[slot].getACLPermissions(false, selectAPDU, root);
    }
}
