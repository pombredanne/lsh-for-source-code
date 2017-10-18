package org.p2pvpn.network;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.p2pvpn.tools.CryptoUtils;

/**
 * PeerID ist an unique ID for every peer.
 * @author Wolfgang Ginolas
 */
public class PeerID implements Comparable<PeerID>, Serializable {

    private static int idLen = CryptoUtils.getMessageDigest().getDigestLength();

    private byte[] id;

    /**
	 * Create a new PeerID.
	 * @param b the ID
	 * @param hash use the hash of b as ID?
	 */
    public PeerID(byte[] b, boolean hash) {
        if (hash) {
            MessageDigest md = CryptoUtils.getMessageDigest();
            id = md.digest(b);
        } else {
            id = b;
        }
    }

    /**
	 * Create a new PeerID
	 * @param addrStr the ID as Base64 String.
	 */
    public PeerID(String addrStr) {
        id = Base64.decodeBase64(addrStr.getBytes());
    }

    /**
	 * The length of PeerIDs.
	 * @return the length in bytes
	 */
    public static int getIdLen() {
        return idLen;
    }

    /**
	 * The ID as an byte array
	 * @return the bytes
	 */
    public byte[] getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PeerID other = (PeerID) obj;
        if (this.id != other.id && (this.id == null || !Arrays.equals(id, other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(id);
    }

    public int compareTo(PeerID o) {
        int d = id.length - o.id.length;
        if (d != 0) return d;
        for (int i = 0; i < id.length; i++) {
            d = id[i] - o.id[i];
            if (d != 0) return d;
        }
        return 0;
    }

    @Override
    public String toString() {
        return new String(Base64.encodeBase64(id, false));
    }
}
