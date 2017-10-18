package com.fusteeno.gnutella.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Class for generating 128 bit UUIDs.
 * Specification: http://www.ics.uci.edu/pub/ietf/webdav/uuid-guid/draft-leach-uuids-guids-01.txt
 *	
 * @version 0.1 03/30/2001
 * @author Frederik Zimmer 
 */
public class UUID {

    protected static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    protected byte[] node;

    protected long time;

    protected static Object lock = new Object();

    protected static Random random = new Random();

    protected static byte[] ip;

    protected static final byte version = 1;

    protected static long lastTime = System.currentTimeMillis();

    protected static short clockSequence = (short) random.nextInt();

    static {
        try {
            ip = InetAddress.getLocalHost().getAddress();
        } catch (Exception e) {
        }
    }

    public UUID() {
        synchronized (lock) {
            time = getCurrentTime();
            if (lastTime > time) clockSequence++;
            node = getNodeAdress();
        }
    }

    /**
     * Parses the String argument as a UUID.
     *
     * @return a bytearray that contains the 128 bits of the UUID
     */
    public static byte[] parseUUID(String s) {
        if (s.length() != 36) throw new NumberFormatException();
        StringBuffer sb = new StringBuffer(s);
        sb.deleteCharAt(8);
        sb.deleteCharAt(12);
        sb.deleteCharAt(16);
        sb.deleteCharAt(20);
        char[] uuidchars = sb.toString().toCharArray();
        byte[] uuid = new byte[16];
        for (int i = 0, j = 0; i < 16; i++, j++) {
            uuid[i] = (byte) (((Character.digit(uuidchars[j++], 16) << 4) & 0xF0) | (Character.digit(uuidchars[j], 16) & 0x0F));
        }
        return uuid;
    }

    /**
     * Get the next UUID.
     *
     * @return a UUID;
     */
    public static byte[] nextUUID() {
        UUID uuid = new UUID();
        return uuid.getUUID();
    }

    /**
     * Generate the 128 bit UUID.
     *
     * @return the UUID
     */
    public byte[] getUUID() {
        byte[] uuid = new byte[16];
        for (int i = 0; i < 8; i++) {
            uuid[i] = (byte) ((time >> 8 * i) & 0xFF);
        }
        uuid[7] |= (byte) (version << 12);
        uuid[8] = (byte) (clockSequence & 0xFF);
        uuid[9] = (byte) ((clockSequence & 0x3F00) >> 8);
        uuid[8] |= 0x80;
        System.arraycopy(node, 0, uuid, 10, 6);
        return uuid;
    }

    /**
     * Get the current time.
     *
     * @return the current time in milliseconds.
     */
    private long getCurrentTime() {
        long time = System.currentTimeMillis();
        while (time == lastTime) {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException e) {
            }
            time = System.currentTimeMillis();
        }
        lastTime = time;
        return time;
    }

    /**
     * Generate the node field. We can't obtain the IEEE 802 address,
     * therefore we put random bytes in the field. The multicast bit is set,
     * in order that the address will never conflict with addresses obtained
     * from network cards.
     *
     * @return 48 bit cryptographic quality random number
     */
    protected byte[] getNodeAdress() {
        byte[] address = new byte[6];
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeLong(random.nextLong());
            out.writeInt(hashCode());
            if (ip != null) out.write(ip);
            out.close();
        } catch (IOException e) {
        }
        byte[] randomBytes = byteOut.toByteArray();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(randomBytes);
            byte[] hash = md5.digest();
            System.arraycopy(hash, 0, address, 0, 6);
        } catch (NoSuchAlgorithmException e) {
        }
        address[0] = (byte) (address[0] | (byte) 0x80);
        return address;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        byte[] uuid = getUUID();
        for (int i = 0; i < 16; i++) {
            sb.append(hexDigits[(uuid[i] & 0xF0) >> 4]);
            sb.append(hexDigits[uuid[i] & 0x0F]);
            if (i == 3 || i == 5 || i == 7 || i == 9) sb.append('-');
        }
        return sb.toString();
    }
}
