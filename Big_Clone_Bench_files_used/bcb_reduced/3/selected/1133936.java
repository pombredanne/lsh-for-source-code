package de.bwb.ekp.commons;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * This class represents an unique identificator (id) for objects. They differ
 * in an important aspect from ordinary object ids referring to memory
 * addresses. Identificators are unique over time and space. Therefore they are
 * suitable for persistence and object migration. Identificators are created by
 * the IDFactory. The algorithm used is based in java.rmi.dgc.VMID This class is
 * declared a final since the handling of identificators is a essential task of
 * the system. Changes to that handling of always very dangerous.
 * 
 */
public final class WamIdUtil {

    private static String adressPrefix = byteToString();

    private static Random random = new Random();

    private WamIdUtil() {
    }

    public static synchronized String createId() {
        final StringBuffer buf = new StringBuffer(adressPrefix);
        buf.append((int) System.currentTimeMillis());
        buf.append(":");
        buf.append((long) random.nextInt() << 16);
        return buf.toString();
    }

    private static String byteToString() {
        final byte[] adresse = computeAddressHash();
        String result = "";
        for (int i = 0; i < adresse.length; ++i) {
            final int x = adresse[i] & 0xFF;
            result += (x < 0x10 ? "0" : "") + Integer.toString(x, 16);
        }
        result += ':';
        return result;
    }

    /**
   * Aus VMID
   */
    private static byte[] computeAddressHash() {
        try {
            final byte[] addr = InetAddress.getLocalHost().getAddress();
            byte[] addrHash;
            final int addrHashLength = 8;
            final MessageDigest md = MessageDigest.getInstance("SHA");
            final ByteArrayOutputStream sink = new ByteArrayOutputStream(64);
            final DataOutputStream out = new DataOutputStream(new DigestOutputStream(sink, md));
            out.write(addr, 0, addr.length);
            out.flush();
            final byte digest[] = md.digest();
            final int hashlength = Math.min(addrHashLength, digest.length);
            addrHash = new byte[hashlength];
            System.arraycopy(digest, 0, addrHash, 0, hashlength);
            return addrHash;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
