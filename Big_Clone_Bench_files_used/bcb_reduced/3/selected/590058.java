package net.m2technologies.open_arm.utilities.guid;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class RandomGUID {

    private String valueBeforeMD5 = "";

    private String valueAfterMD5;

    private static Random myRand;

    private static SecureRandom mySecureRand;

    private static String id;

    private static final int SHIFT_SPACE = 0xFF;

    private static final int ZERO_TEST = 0x10;

    private static final char CHAR_ZERO = '0';

    private static final char SEMI_COLON = ':';

    private static final char DASH = '-';

    private static final int GUID_SUBSTRING_ELEMENT_1 = 12;

    private static final int GUID_SUBSTRING_ELEMENT_2 = 16;

    private static final int GUID_SUBSTRING_ELEMENT_3 = 20;

    private static final int TEST_LIMIT = 100;

    static {
        mySecureRand = new SecureRandom();
        final long secureInitializer = mySecureRand.nextLong();
        myRand = new Random(secureInitializer);
        try {
            id = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            id = "42";
        }
    }

    public RandomGUID() {
        this.valueAfterMD5 = getRandomGUID(false);
    }

    public RandomGUID(final boolean secure) {
        this.valueAfterMD5 = getRandomGUID(secure);
    }

    /**
     * Extra constructor added for OpenArm -- here, we presume that you know what you're doing.  The value passed as a
     * byte array will simply be stored here, and represented as a String.
     *
     * @param bytes
     */
    public RandomGUID(final byte[] bytes) {
        this.valueAfterMD5 = new String(bytes);
    }

    public void reset() {
        this.valueAfterMD5 = getRandomGUID(false);
    }

    public void reset(final boolean secure) {
        this.valueAfterMD5 = getRandomGUID(secure);
    }

    private String getRandomGUID(final boolean secure) {
        MessageDigest md5 = null;
        final StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            final long time = System.currentTimeMillis();
            final long rand;
            if (secure) {
                rand = mySecureRand.nextLong();
            } else {
                rand = myRand.nextLong();
            }
            sbValueBeforeMD5.append(id);
            sbValueBeforeMD5.append(SEMI_COLON);
            sbValueBeforeMD5.append(Long.toString(time));
            sbValueBeforeMD5.append(SEMI_COLON);
            sbValueBeforeMD5.append(Long.toString(rand));
            valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());
            final byte[] array = md5.digest();
            final StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                final int bufferIndex = array[j] & SHIFT_SPACE;
                if (ZERO_TEST > bufferIndex) sb.append(CHAR_ZERO);
                sb.append(Integer.toHexString(bufferIndex));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getValue() {
        return valueAfterMD5;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RandomGUID)) return false;
        final RandomGUID randomGUID = (RandomGUID) o;
        return !(null != valueAfterMD5 ? !valueAfterMD5.equals(randomGUID.valueAfterMD5) : null != randomGUID.valueAfterMD5);
    }

    public int hashCode() {
        return null != valueAfterMD5 ? valueAfterMD5.hashCode() : 0;
    }

    public String toString() {
        final String raw = valueAfterMD5.toUpperCase();
        final StringBuffer sb = new StringBuffer();
        sb.append(raw.substring(0, 8));
        sb.append(DASH);
        sb.append(raw.substring(8, GUID_SUBSTRING_ELEMENT_1));
        sb.append(DASH);
        sb.append(raw.substring(GUID_SUBSTRING_ELEMENT_1, GUID_SUBSTRING_ELEMENT_2));
        sb.append(DASH);
        sb.append(raw.substring(GUID_SUBSTRING_ELEMENT_2, GUID_SUBSTRING_ELEMENT_3));
        sb.append(DASH);
        sb.append(raw.substring(GUID_SUBSTRING_ELEMENT_3));
        return sb.toString();
    }

    public static void main(final String[] args) {
        for (int i = 0; TEST_LIMIT > i; i++) {
            final RandomGUID myGUID = new RandomGUID();
            System.out.println(new StringBuffer().append("[main] Seeding String=").append(myGUID.valueBeforeMD5).toString());
            System.out.println(new StringBuffer().append("[main] rawGUID=").append(myGUID.valueAfterMD5).toString());
            System.out.println(new StringBuffer().append("[main] RandomGUID=").append(myGUID.toString()).toString());
        }
    }
}
