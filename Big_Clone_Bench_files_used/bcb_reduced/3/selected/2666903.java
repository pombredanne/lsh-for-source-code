package com.sun.iis.tools.cmd;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * UUIDGenerator is the class that contains factory methods for
 * generating UUIDs using one of the three specified 'standard'
 * UUID generation methods:
 * <ul>
 * <li>Time-based generation generates UUID using spatial and
 *     temporal uniqueness. Spatial uniqueness is derived from
 *     ethernet address (MAC, 802.1); temporal from system clock.
 * <li>Name-based method uses MD5 hash (or, optionally any user-specified
 *     digest method) of the string formed from
 *     a name space and name.
 * <li>Random method uses Java2 API's SecureRandom to produce
 *     cryptographically secure UUIDs.
 * </ul>
 * @author Derek Frankforth
 */
public final class UUIDGenerator {

    /**
     * Singleton
     */
    private static final UUIDGenerator SINGLETON = new UUIDGenerator();

    /**
     * A RandomNumber generator.
     */
    private SecureRandom mRnd = null;

    /**
     * A UUIDTimer objects for Timestamp generation
     */
    private UUIDTimer mTimer;

    /**
     * Ethernet MAC Address.
     */
    private byte[] mRandomEthernetAddress;

    /**
     * Constructor is private to enforce singleton access.
     */
    private UUIDGenerator() {
        mRnd = Random.getInstance();
    }

    /**
     * Method used for accessing the singleton generator instance.
     * @return UUIDGenerator instance
     */
    public static UUIDGenerator getInstance() {
        return SINGLETON;
    }

    /**
     * Method that returns a randomly generated dummy ethernet address.
     * To prevent collision with real addresses, the returned address has
     * the broadcast bit set.
     *
     * @return Randomly generated dummy ethernet broadcast address.
     */
    private byte[] getRandomEthernetAddress() {
        synchronized (this) {
            if (mRandomEthernetAddress == null) {
                byte[] dummy = new byte[6];
                mRnd.nextBytes(dummy);
                dummy[0] |= (byte) 0x80;
                mRandomEthernetAddress = dummy;
            }
        }
        return mRandomEthernetAddress;
    }

    /**
     * Method for generating (pseudo-)random based UUIDs, using the
     * default (shared) SecureRandom object.
     * 
     * @return UUID generated using (pseudo-)random based method
     */
    public UUID generateRandomBasedUUID() {
        byte[] rnd = new byte[UUID.UUID_SIZE_IN_BYTES];
        mRnd.nextBytes(rnd);
        return new UUID(UUID.TYPE_RANDOM_BASED, rnd);
    }

    /**
     * Method for generating time based UUIDs. Note that this version
     * doesn't use any existing Hardware address for two reasons: None 
     * is available from Java, and it's considered a security flaw if a
     * MAC address is revealed outside a firewall.
     *
     * @return UUID generated using time based method
     */
    public UUID generateTimeBasedUUID() {
        return generateTimeBasedUUID(getRandomEthernetAddress());
    }

    /**
     * Method for generating time based UUIDs.
     * 
     * @param anEthernetMacAddress byte[] for initialization.
     *
     * @return UUID generated using time based method
     */
    private UUID generateTimeBasedUUID(byte[] anEthernetMacAddress) {
        byte[] contents = new byte[UUID.UUID_SIZE_IN_BYTES];
        System.arraycopy(anEthernetMacAddress, 0, contents, 10, 6);
        synchronized (this) {
            if (mTimer == null) {
                mTimer = new UUIDTimer(mRnd);
            }
            mTimer.getTimestamp(contents);
        }
        return new UUID(UUID.TYPE_TIME_BASED, contents);
    }

    /**
     * Method for generating name-based UUIDs.
     * 
     * @param aNameSpaceUUID of the namespace.
     * @param aName String to base the UUID on.
     * @param aHash Instance of MessageDigest to use for hashing.
     *
     * @return UUID generated using name-based method.
     */
    public UUID generateNameBasedUUID(UUID aNameSpaceUUID, String aName, MessageDigest aHash) {
        aHash.reset();
        if (aNameSpaceUUID != null) {
            aHash.update(aNameSpaceUUID.asByteArray());
        }
        aHash.update(aName.getBytes());
        return new UUID(UUID.TYPE_NAME_BASED, aHash.digest());
    }
}
