package fi.hiit.cutehip.utils;

import java.util.Arrays;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import fi.hiit.framework.crypto.SHA1Digest;
import fi.hiit.framework.utils.Helpers;

public class HostIdentityTag {

    public static final byte[] NULL_HIT = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    public static final String TAG = "F0EFF02FBFF43D0FE7930C3C6E6174EA";

    public static final String PREFIX = "20010010";

    public static final byte[] PREFIX_BYTES = Helpers.hexStringToByteArray(PREFIX);

    public static final String HASH = "SHA1";

    public static final int TAGLENGTH = 16;

    public static final int PREFIXLENGTH = 4;

    public HostIdentityTag(HostIdentity hi) {
        __hit = HostIdentityTag.generateHIT(hi.getBytes());
    }

    public HostIdentityTag(InetAddress hit) {
        __hit = hit.getAddress();
    }

    public HostIdentityTag(byte[] hit) {
        __hit = hit;
    }

    public HostIdentityTag(String hit) {
        try {
            __hit = InetAddress.getByName(hit).getAddress();
        } catch (UnknownHostException e) {
        }
    }

    public boolean equals(byte[] hit) {
        return Arrays.equals(hit, __hit);
    }

    public int compare(byte[] hit) {
        return new BigInteger(__hit).compareTo(new BigInteger(hit));
    }

    public byte[] getAsBytes() {
        return __hit;
    }

    public Inet6Address getAsAddress() throws java.net.UnknownHostException {
        return (Inet6Address) Inet6Address.getByAddress(__hit);
    }

    public static boolean isHit(byte[] hit) {
        if (hit == null) return false;
        if (hit.length != HostIdentityTag.TAGLENGTH) return false;
        return ((hit[0] == PREFIX_BYTES[0]) && (hit[1] == PREFIX_BYTES[1]) && (hit[2] == PREFIX_BYTES[2]) && ((hit[3] & 0xF0) == (PREFIX_BYTES[3] & 0xF0)));
    }

    public static boolean isNullHit(byte[] hit) {
        return Arrays.equals(hit, NULL_HIT);
    }

    public static boolean isHit(InetAddress addr) {
        return isHit(addr.getAddress());
    }

    public static byte[] generateHIT(byte[] hi) {
        SHA1Digest md = new SHA1Digest();
        byte[] tag = Helpers.hexStringToByteArray(TAG);
        byte[] input = new byte[tag.length + hi.length - HostIdentity.HI_PREAMBLE_LENGTH];
        System.arraycopy(tag, 0, input, 0, tag.length);
        System.arraycopy(hi, HostIdentity.HI_PREAMBLE_LENGTH, input, tag.length, hi.length - HostIdentity.HI_PREAMBLE_LENGTH);
        byte[] hashBytes = md.digest(input);
        byte[] hit = new byte[TAGLENGTH];
        System.arraycopy(PREFIX_BYTES, 0, hit, 0, PREFIXLENGTH - 1);
        hit[PREFIXLENGTH - 1] = (byte) (PREFIX_BYTES[PREFIXLENGTH - 1] & 0xF0);
        hit[PREFIXLENGTH - 1] |= (byte) ((hashBytes[PREFIXLENGTH - 1] & 0x03) << 2);
        hit[PREFIXLENGTH - 1] |= (byte) ((hashBytes[PREFIXLENGTH] & 0xC0) >> 6);
        for (int i = PREFIXLENGTH; i < PREFIXLENGTH + 12; i++) {
            hit[i] |= (byte) ((hashBytes[i] & 0x3F) << 2);
            hit[i] |= (byte) ((hashBytes[i + 1] & 0xC0) >> 6);
        }
        return hit;
    }

    private byte[] __hit;
}
