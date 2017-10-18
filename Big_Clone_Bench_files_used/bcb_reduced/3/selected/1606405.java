package com.induslogic.uddi.server.util;

import java.net.InetAddress;
import java.util.Date;
import java.util.Random;
import java.io.*;
import java.security.MessageDigest;

/**
	As the profile primary key we use a UUID.

	A Universally Unique Identifier (UUID) is a 128 bit number generated
	according to an algorithm that is garanteed to be unique in time and
	space from all other UUIDs. It consists of an IEEE 802 Internet Address
	and various time stamps to ensure uniqueness. For a complete
 specification,
	see ftp://ietf.org/internet-drafts/draft-leach-uuids-guids-01.txt
 [leach].
	@author Jim Amsden &lt;jamsden@us.ibm.com&gt;

	Modifications by Torgeir Veimo &lt;torgeir.veimo@ecomda.de&gt; for
 Ecomda GmbH.
	Modifications by Roman Levenshteyn &lt;Roman.Levenshteyn@eed.ericsson.se&gt; for Ericsson Eurolab Deutschland GmbH.
*/
public class JavaUUIDGenerator implements Serializable {

    private static byte[] internetAddress = null;

    private static String uuidFile = null;

    private long time;

    private short clockSequence;

    private byte version = 1;

    private byte[] node = new byte[6];

    private static final int UUIDsPerTick = 128;

    private static long lastTime = new Date().getTime();

    private static int uuidsThisTick = UUIDsPerTick;

    private static JavaUUIDGenerator previousUUID = null;

    private static long nextSave = new Date().getTime();

    private static Random randomGenerator = new Random(new Date().getTime());

    private static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static {
        try {
            internetAddress = InetAddress.getLocalHost().getAddress();
        } catch (Exception exc) {
            System.out.println("Can't get host address: " + exc);
            exc.printStackTrace();
        }
        try {
            uuidFile = System.getProperty("UUID_HOME");
            if (uuidFile == null) {
                uuidFile = System.getProperty("user.home");
            }
            if (uuidFile == null) {
                uuidFile = System.getProperty("java.home");
            }
            if (!uuidFile.endsWith(File.separator)) {
                uuidFile = uuidFile + File.separator;
            }
            uuidFile = uuidFile + "UUID";
            previousUUID = getUUIDState();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /** Generate a UUID for this host using version 1 of [leach].
         */
    public JavaUUIDGenerator() {
        synchronized (this) {
            time = getCurrentTime();
            node = previousUUID.getNode();
            if (previousUUID == null || nodeChanged(previousUUID)) {
                clockSequence = (short) random();
            } else if (time < previousUUID.getTime()) {
                clockSequence++;
            }
            previousUUID = this;
            setUUIDState(this);
        }
    }

    /** Generate a UUID for this host using version 1 of [leach].
         * @param node the node to use in the UUID
         */
    public JavaUUIDGenerator(byte[] node) {
        synchronized (this) {
            time = getCurrentTime();
            this.node = node;
            setUUIDState(this);
        }
    }

    /** Generate a UUID from a name (NOT IMPLEMENTED)
         */
    public JavaUUIDGenerator(JavaUUIDGenerator context, String name) {
    }

    /** Lexically compare this UUID with withUUID. Note: lexical ordering
         * is not temporal ordering.
         *
         * @param withUUID the UUID to compare with
         * @return
         * <ul>
         *    <li>-1 if this UUID is less than withUUID
         *    <li>0 if this UUID is equal to withUUID
         *    <li>1 if this UUID is greater than withUUID
         * </ul>
         */
    public int compare(JavaUUIDGenerator withUUID) {
        if (time < withUUID.getTime()) {
            return -1;
        } else if (time > withUUID.getTime()) {
            return 1;
        }
        if (version < withUUID.getVersion()) {
            return -1;
        } else if (version > withUUID.getVersion()) {
            return 1;
        }
        if (clockSequence < withUUID.getClockSequence()) {
            return -1;
        } else if (clockSequence > withUUID.getClockSequence()) {
            return 1;
        }
        byte[] withNode = withUUID.getNode();
        for (int i = 0; i < 6; i++) {
            if (node[i] < withNode[i]) {
                return -1;
            } else if (node[i] > withNode[i]) {
                return 1;
            }
        }
        return 0;
    }

    /** Get a 48 bit cryptographic quality random number to use as the node
         field
         * of a UUID as specified in section 6.4.1 of version 10 of the WebDAV
         spec.
         * This is an alternative to the IEEE 802 host address which is not
         available
         * from Java. The number will not conflict with any IEEE 802 host address
         because
         * the most significant bit of the first octet is set to 1.
         * @return a 48 bit number specifying an id for this node
         */
    private static byte[] computeNodeAddress() {
        byte[] address = new byte[6];
        int thread = Thread.currentThread().hashCode();
        long time = System.currentTimeMillis();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            if (internetAddress != null) {
                out.write(internetAddress);
            }
            out.write(thread);
            out.writeLong(time);
            out.close();
        } catch (IOException exc) {
        }
        byte[] rand = byteOut.toByteArray();
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception exc) {
        }
        md5.update(rand);
        byte[] temp = md5.digest();
        for (int i = 0; i < 6; i++) {
            address[i] = temp[i + 5];
        }
        address[0] = (byte) (address[0] | (byte) 0x80);
        return address;
    }

    /** Get the clock sequence number.
         * @return the clock sequence number
         */
    public int getClockSequence() {
        return clockSequence;
    }

    /** Get the current time compensating for the fact that the real
         * clock resolution may be less than 100ns.
         *
         * @return the current date and time
         */
    private static long getCurrentTime() {
        long now = 0;
        boolean waitForTick = true;
        while (waitForTick) {
            now = new Date().getTime();
            if (lastTime < now) {
                uuidsThisTick = 0;
                waitForTick = false;
            } else if (uuidsThisTick < UUIDsPerTick) {
                uuidsThisTick++;
                waitForTick = false;
            }
        }
        now += uuidsThisTick;
        lastTime = now;
        return now;
    }

    /** Get the 48 bit IEEE 802 host address. NOT IMPLEMENTED
         * @return a 48 bit number specifying a unique location
         */
    private static byte[] getIEEEAddress() {
        byte[] address = new byte[6];
        return address;
    }

    /** Get the spatially unique portion of the UUID. This is either
         * the 48 bit IEEE 802 host address, or if on is not available, a random
         * number that will not conflict with any IEEE 802 host address.
         * @return node portion of the UUID
         */
    public byte[] getNode() {
        return node;
    }

    /** Get the temporal unique portion of the UUID.
         * @return the time portion of the UUID
         */
    public long getTime() {
        return time;
    }

    /** Get the 128 bit UUID.
         */
    public byte[] getUUID() {
        byte[] uuid = new byte[16];
        long t = time;
        for (int i = 0; i < 8; i++) {
            uuid[i] = (byte) ((t >> 8 * i) & 0xFF);
        }
        uuid[7] |= (byte) (version << 12);
        uuid[8] = (byte) (clockSequence & 0xFF);
        uuid[9] = (byte) ((clockSequence & 0x3F00) >> 8);
        uuid[9] |= 0x80;
        for (int i = 0; i < 6; i++) {
            uuid[10 + i] = node[i];
        }
        return uuid;
    }

    /** Get the UUID generator state. This consists of the last (or
         * nearly last) UUID generated. This state is used in the construction
         * of the next UUID. May return null if the UUID state is not
         * available.
         * @return the last UUID generator state
         */
    private static JavaUUIDGenerator getUUIDState() {
        JavaUUIDGenerator uuid = null;
        try {
            FileInputStream in = new FileInputStream(uuidFile);
            ObjectInputStream s = new ObjectInputStream(in);
            uuid = (JavaUUIDGenerator) s.readObject();
        } catch (Exception exc) {
            uuid = new JavaUUIDGenerator(computeNodeAddress());
            System.err.println("Can't get saved UUID state: " + exc);
        }
        return uuid;
    }

    /** Get the UUID version number.
         */
    public int getVersion() {
        return version;
    }

    /** Compare two UUIDs
         * @return true if the UUIDs are equal
         */
    public boolean isEqual(JavaUUIDGenerator toUUID) {
        return compare(toUUID) == 0;
    }

    /** Determine if the node changed with respect to previousUUID.
         * @param previousUUID the UUID to compare with
         * @return true if the the previousUUID has a different node than this
         UUID
         */
    private boolean nodeChanged(JavaUUIDGenerator previousUUID) {
        byte[] previousNode = previousUUID.getNode();
        boolean nodeChanged = false;
        int i = 0;
        while (!nodeChanged && i < 6) {
            nodeChanged = node[i] != previousNode[i];
            i++;
        }
        return nodeChanged;
    }

    /** Generate a crypto-quality random number. This implementation
         * doesn't do that.
         * @return a random number
         */
    private static int random() {
        return randomGenerator.nextInt();
    }

    /** Set the persistent UUID state.
         * @param aUUID the UUID to save
         */
    private static void setUUIDState(JavaUUIDGenerator aUUID) {
        if (aUUID.getTime() > nextSave) {
            try {
                FileOutputStream f = new FileOutputStream(uuidFile);
                ObjectOutputStream s = new ObjectOutputStream(f);
                s.writeObject(aUUID);
                s.close();
                nextSave = aUUID.getTime() + 10 * 10 * 1000 * 1000;
            } catch (Exception exc) {
                System.err.println("Can't save UUID state: " + exc);
            }
        }
    }

    /** Provide a String representation of a UUID as specified in section
         * 3.5 of [leach].
         */
    public String toString() {
        byte[] uuid = getUUID();
        StringWriter s = new StringWriter();
        for (int i = 0; i < 16; i++) {
            s.write(hexDigits[(uuid[i] & 0xF0) >> 4]);
            s.write(hexDigits[uuid[i] & 0x0F]);
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                s.write('-');
            }
        }
        return s.toString();
    }

    public static String uuidgen() {
        String result = new JavaUUIDGenerator().toString();
        return result;
    }
}
