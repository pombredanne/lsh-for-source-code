package org.opendedup.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Random;
import java.util.zip.Adler32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendedup.sdfs.servers.HashChunkService;

/**
 * This is a very fast, non-cryptographic hash suitable for general hash-based
 * lookup. See http://murmurhash.googlepages.com/ for more details.
 * 
 * <p>
 * The C version of MurmurHash 2.0 found at that site was ported to Java by
 * Andrzej Bialecki (ab at getopt org).
 * </p>
 */
public class HashFunctions {

    static MessageDigest algorithm;

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
            algorithm = MessageDigest.getInstance("Tiger", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int seed = 1;

    private static int m = 0x5bd1e995;

    private static int r = 24;

    public static int getMurmurHashCode(byte[] data) {
        int len = data.length;
        int h = seed ^ len;
        int i = 0;
        while (len >= 4) {
            int k = data[i + 0] & 0xFF;
            k |= (data[i + 1] & 0xFF) << 8;
            k |= (data[i + 2] & 0xFF) << 16;
            k |= (data[i + 3] & 0xFF) << 24;
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
            i += 4;
            len -= 4;
        }
        switch(len) {
            case 3:
                h ^= (data[i + 2] & 0xFF) << 16;
            case 2:
                h ^= (data[i + 1] & 0xFF) << 8;
            case 1:
                h ^= (data[i + 0] & 0xFF);
                h *= m;
        }
        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h;
    }

    public static void testFile(String file, int buffer_len) throws IOException, NoSuchAlgorithmException {
        FileInputStream from = null;
        long currMS = System.currentTimeMillis();
        System.out.println("Current time = " + ElapsedTime.getDateTime(new Date(currMS)));
        File f = new File(file);
        System.out.println("file size = " + f.length());
        try {
            from = new FileInputStream(file);
            byte[] buffer = new byte[buffer_len];
            int bytes_read;
            long current_position = 0;
            while ((bytes_read = from.read(buffer)) != -1) {
                current_position = current_position + bytes_read;
                System.out.println(getMD5Hash(buffer));
                current_position = current_position + bytes_read;
            }
            System.out.println("Elapsed time = " + (System.currentTimeMillis() - currMS) / 1000 + " to read ");
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public static void testChannelFile(String file, int buffer_len) throws IOException, NoSuchAlgorithmException {
        FileInputStream from = null;
        long currMS = System.currentTimeMillis();
        System.out.println("Current time = " + ElapsedTime.getDateTime(new Date(currMS)));
        File f = new File(file);
        System.out.println("file size = " + f.length());
        try {
            from = new FileInputStream(file);
            ReadableByteChannel channel = from.getChannel();
            byte[] buffer = new byte[buffer_len];
            ByteBuffer buf = ByteBuffer.wrap(buffer);
            int numRead = 0;
            while (numRead >= 0) {
                numRead = channel.read(buf);
                if (buf.position() == 0) break;
                String hash = StringUtils.getHexString(getMD5ByteHash(buf.array()));
                String newHash = StringUtils.getHexString(StringUtils.getHexBytes(hash));
                if (hash.equalsIgnoreCase(newHash)) System.out.println(hash); else System.out.println(hash + " " + newHash);
                buf.clear();
            }
            System.out.println("Elapsed time = " + (System.currentTimeMillis() - currMS) / 1000 + " to read ");
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    static int NUM = 100000;

    public static void main(String[] args) throws Exception {
        String rndStr = getRandomString(12);
        System.out.println(rndStr);
        String auth = getSHAHash("admin".getBytes(), "test".getBytes());
        if (auth.equals(getSHAHash("admin".getBytes(), "test".getBytes()))) System.out.println(auth); else System.out.println("failed");
    }

    public static void insertRecorts(long number) throws Exception {
        System.out.println("Inserting [" + number + "] Records....");
        long start = System.currentTimeMillis();
        Random rnd = new Random();
        for (int i = 0; i < number; i++) {
            byte[] b = new byte[64];
            rnd.nextBytes(b);
            byte[] hash = HashFunctions.getMD5ByteHash(b);
            HashChunkService.writeChunk(hash, b, 0, b.length, false);
        }
        System.out.println("Took " + (System.currentTimeMillis() - start) + " ms");
    }

    public static String getSHAHash(byte[] input) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return StringUtils.getHexString(digest.digest(input));
    }

    public static String getSHAHash(byte[] input, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update(salt);
        digest.update(input);
        return StringUtils.getHexString(digest.digest());
    }

    public static String getRandomString(int sz) {
        String str = new String("QAa0bcLdUK2eHfJgTP8XhiFj61DOklNm9nBoI5pGqYVrs3CtSuMZvwWx4yE7zR");
        StringBuffer sb = new StringBuffer();
        SecureRandom r = new SecureRandom();
        int te = 0;
        for (int i = 1; i <= sz; i++) {
            te = r.nextInt(str.length());
            sb.append(str.charAt(te));
        }
        return (sb.toString());
    }

    public static byte[] getSHAHashBytes(byte[] input) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return digest.digest(input);
    }

    public static byte[] getTigerHashBytes(byte[] input) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException {
        algorithm.reset();
        return algorithm.digest(input);
    }

    public static String getMD5Hash(byte[] input) {
        try {
            return StringUtils.getHexString(getMD5ByteHash(input));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getMD5ByteHash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(input);
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getAlder32ByteHash(byte[] input) {
        Adler32 alder = new Adler32();
        alder.update(input);
        ByteBuffer buf = ByteBuffer.wrap(new byte[8]);
        buf.putLong(alder.getValue());
        return buf.array();
    }
}
