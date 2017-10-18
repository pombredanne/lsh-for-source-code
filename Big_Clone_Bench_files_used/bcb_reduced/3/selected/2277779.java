package knet.net.chord;

import knet.net.*;
import knet.crypt.*;
import java.net.*;
import java.security.*;

public class ChordAddress implements KAddress, Comparable, Cloneable {

    ChordInterface iface;

    InetAddress iaddress;

    int port;

    int[] address;

    static final int HIGHBIT = 0x80000000;

    public boolean equals(Object otherObject) {
        try {
            ChordAddress other = (ChordAddress) otherObject;
            if (address.length != other.address.length) return false;
            for (int ind = 0; ind < address.length; ind++) {
                if (address[ind] != other.address[ind]) return false;
            }
            return true;
        } catch (ClassCastException cce) {
            return false;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public ChordAddress copy() {
        return new ChordAddress(iface, (int[]) address.clone());
    }

    public boolean equals(byte[] buffer, int off, int len) {
        for (int ind = 0; ind < address.length; ind++) {
            int value = address[ind];
            if (buffer[off++] != (byte) (value >>> 24)) return false;
            if (buffer[off++] != (byte) (value >>> 16)) return false;
            if (buffer[off++] != (byte) (value >>> 8)) return false;
            if (buffer[off++] != (byte) value) return false;
        }
        return true;
    }

    public void addPowerOf2(int bit) {
        int ind = address.length - 1 - (bit >> 5);
        if (ind < 0) return;
        int oldValue = address[ind];
        address[ind] += (1 << (bit & 0x1f));
        while ((address[ind] + HIGHBIT) < (oldValue + HIGHBIT) && ind > 0) {
            ind--;
            oldValue = address[ind];
            address[ind]++;
        }
    }

    public void subPowerOf2(int bit) {
        int ind = address.length - 1 - (bit >> 5);
        if (ind < 0) return;
        int oldValue = address[ind];
        address[ind] -= (1 << (bit & 0x1f));
        while ((address[ind] + HIGHBIT) > (oldValue + HIGHBIT) && ind > 0) {
            ind--;
            oldValue = address[ind];
            address[ind]--;
        }
    }

    public int compareTo(Object otherObject) {
        ChordAddress other = (ChordAddress) otherObject;
        if (address.length < other.address.length) return -1;
        if (address.length > other.address.length) return 1;
        for (int ind = 0; ind < address.length; ind++) {
            int addr = address[ind] + HIGHBIT;
            int oaddr = other.address[ind] + HIGHBIT;
            if (addr < oaddr) return -1;
            if (addr > oaddr) return 1;
        }
        return 0;
    }

    ChordAddress() {
    }

    ChordAddress(ChordInterface anIface, int[] anAddress) {
        iface = anIface;
        address = anAddress;
    }

    ChordAddress(int[] anAddress, int off, int len) {
        address = new int[len];
        System.arraycopy(anAddress, off, address, 0, len);
    }

    ChordAddress(byte[] buffer, int off) {
        address = new int[Constants.MAX_BITS / 32];
        for (int ind = 0; ind < Constants.MAX_BITS / 32; ind++) {
            int value = buffer[off++] & 0xFF;
            value = (value << 8) | (buffer[off++] & 0xFF);
            value = (value << 8) | (buffer[off++] & 0xFF);
            value = (value << 8) | (buffer[off++] & 0xFF);
            address[ind] = value;
        }
    }

    void setInterface(ChordInterface iface) {
        this.iface = iface;
    }

    void setAddress(int[] address) {
        this.address = address;
    }

    void hashAddress(MessageDigest digest) {
        address = new int[(digest.getDigestLength() + 3) / 4];
        digest.update(iaddress.getAddress(), 0, 4);
        digest.update((byte) (port >>> 8));
        digest.update((byte) port);
        byte[] buffer = digest.digest();
        int off = 0;
        for (int ind = 0; ind < Constants.MAX_BITS / 32; ind++) {
            int value = buffer[off++] & 0xFF;
            value = (value << 8) | (buffer[off++] & 0xFF);
            value = (value << 8) | (buffer[off++] & 0xFF);
            value = (value << 8) | (buffer[off++] & 0xFF);
            address[ind] = value;
        }
    }

    public String str() {
        StringBuffer buf = new StringBuffer();
        char[] prepend = { '0', '0', '0', '0', '0', '0', '0', '0' };
        for (int ind = 0; ind < address.length; ind++) {
            String str = Integer.toHexString(address[ind]);
            buf.append(prepend, str.length(), 8 - str.length());
            buf.append(str);
        }
        return buf.toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        char[] prepend = { '0', '0', '0', '0', '0', '0', '0', '0' };
        String str = Integer.toHexString(address[0] >>> 16);
        buf.append(prepend, str.length(), 4 - str.length());
        buf.append(str);
        return buf.toString();
    }

    void setIAddress(InetAddress iaddress, int port) {
        this.iaddress = iaddress;
        this.port = port;
    }

    int[] getAddress() {
        return address;
    }

    InetAddress getIAddress() {
        return iaddress;
    }

    int getPort() {
        return port;
    }

    public KInterface getInterface() {
        return iface;
    }

    public boolean isAllZeros() {
        for (int ind = 0; ind < address.length; ind++) {
            if (address[ind] != 0) return false;
        }
        return true;
    }

    public boolean isBetween(ChordAddress min, ChordAddress max) {
        int comp1 = min.compareTo(this);
        int comp2 = this.compareTo(max);
        int comp3 = min.compareTo(max);
        if (comp1 <= 0 && comp2 < 0 && comp3 < 0) return true;
        if (comp1 <= 0 && comp3 > 0) return true;
        if (comp2 < 0 && comp3 > 0) return true;
        return false;
    }
}
