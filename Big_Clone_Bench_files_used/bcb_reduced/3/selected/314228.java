package com.icesoft.util;

import com.icesoft.util.encoding.Base64;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * The <code>IdGenerator</code> is responsible for generating a unique ID based
 * on a counter, the current time in milliseconds, an arbitrary string, the IP
 * address of the localhost, and a random number. </p>
 */
public class IdGenerator {

    private String seed;

    private long counter;

    private String ipAddress;

    private static MessageDigest md5;

    public IdGenerator() {
        this(String.valueOf(new Random().nextInt()));
    }

    public IdGenerator(String seed) {
        this.seed = seed.trim();
        this.counter = 0;
        try {
            md5 = MessageDigest.getInstance("MD5");
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (NoClassDefFoundError e) {
            ipAddress = "GAE";
        }
    }

    /**
     * Creates a unique ID based on the specified <code>string</code>. </p>
     *
     * @return a unique ID.
     */
    public synchronized String newIdentifier() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(++counter);
        buffer.append(System.currentTimeMillis());
        buffer.append(seed);
        buffer.append(ipAddress);
        buffer.append(Math.random());
        byte[] digest = md5.digest(buffer.toString().getBytes());
        byte[] encodedDigest = Base64.encodeForURL(digest);
        return new String(encodedDigest);
    }
}
