package chord;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

public class ChordIdentifier {

    static final String MD_ALGORITHM = "SHA-1";

    static final int KEYSIZE = 20;

    static final BigInteger WRAP_AROUND_NUM = BigInteger.valueOf(2).pow(KEYSIZE * 8);

    protected byte[] data;

    BigInteger bigNumId;

    public ChordIdentifier(byte[] raw, int offset) {
        if ((raw.length - offset) < KEYSIZE) {
            ChordLogger.getInstance().error("Wrong size for ChordIdentifier: " + (raw.length - offset) + " versus " + KEYSIZE);
            return;
        }
        data = new byte[KEYSIZE];
        System.arraycopy(data, 0, raw, offset, KEYSIZE);
        bigNumId = new BigInteger(1, data);
    }

    public ChordIdentifier(byte[] info) {
        try {
            MessageDigest md = MessageDigest.getInstance(MD_ALGORITHM);
            data = md.digest(info);
        } catch (NoSuchAlgorithmException e) {
            ChordLogger.getInstance().error("Message Digest Algorithm unrecognized: " + e.getMessage());
        }
        bigNumId = new BigInteger(1, data);
    }

    public ChordIdentifier(BigInteger bi) {
        bigNumId = bi;
    }

    public boolean leq(ChordIdentifier other) {
        return (bigNumId.compareTo(other.bigNumId) <= 0);
    }

    public boolean between(ChordIdentifier lower, ChordIdentifier higher) {
        if (lower.bigNumId.compareTo(higher.bigNumId) < 0) {
            return ((bigNumId.compareTo(lower.bigNumId) > 0) && (bigNumId.compareTo(higher.bigNumId) < 0));
        } else {
            return ((bigNumId.compareTo(lower.bigNumId) > 0) || (bigNumId.compareTo(higher.bigNumId) < 0));
        }
    }

    public boolean betweenInc(ChordIdentifier lower, ChordIdentifier higher) {
        if (lower.bigNumId.compareTo(higher.bigNumId) <= 0) {
            return ((bigNumId.compareTo(lower.bigNumId) >= 0) && (bigNumId.compareTo(higher.bigNumId) <= 0));
        } else {
            return ((bigNumId.compareTo(lower.bigNumId) >= 0) || (bigNumId.compareTo(higher.bigNumId) <= 0));
        }
    }

    public boolean betweenIncRight(ChordIdentifier lower, ChordIdentifier higher) {
        if (lower.bigNumId.compareTo(higher.bigNumId) < 0) {
            return ((bigNumId.compareTo(lower.bigNumId) > 0) && (bigNumId.compareTo(higher.bigNumId) <= 0));
        } else {
            return ((bigNumId.compareTo(lower.bigNumId) > 0) || (bigNumId.compareTo(higher.bigNumId) <= 0));
        }
    }

    public boolean equals(ChordIdentifier other) {
        return (bigNumId.compareTo(other.bigNumId) == 0);
    }

    public byte[] getBytes() {
        return data;
    }

    public BigInteger getBigNumId() {
        return bigNumId;
    }

    public int bitLength() {
        return bigNumId.bitLength();
    }

    public String toString() {
        return new String(data);
    }
}
