package org.grailrtls.server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Represents a landmark to the {@link LocalizationServer}.&nbsp; Responsible
 * for generating a stream of {@link Sample} objects and placing them in the
 * appropriate {@link Region}.
 * 
 * @author Richard P. Martin
 * @author Robert S. Moore II
 * 
 */
public final class Landmark implements Comparable<Landmark> {

    /**
	 * Unrecognized transmit format.
	 */
    public static final int MODE_UNDEFINED = -1;

    /**
	 * Transmit raw packet headers.
	 */
    public static final int MODE_PACKET = 0;

    /**
	 * Transmit a histogram of RSSI values.
	 */
    public static final int MODE_HISTOGRAM = 1;

    /**
	 * Transmit the arithmetic mean and standard deviation of the RSSI values.
	 */
    public static final int MODE_MEAN = 2;

    /**
	 * Transmit the arithmetic mean, standard deviation, mode, and median of the
	 * RSSI values.
	 */
    public static final int MODE_STAT = 3;

    /**
	 * Identifier for unimplemented physical layers.
	 */
    public static final int PHY_UNDEFINED = 3;

    /**
	 * Identifier for 802.11 (Wi-Fi) physical layer.
	 */
    public static final int PHY_802_11 = 0;

    /**
	 * Identifier for PIPSQUEAK RFID physical layer.
	 */
    public static final int PHY_ROLLCALL = 1;

    /**
	 * Identifier for Bluetooth physical layer.
	 */
    public static final int PHY_802_15_4 = 2;

    private final int hashCode;

    /**
	 * Human-readable strings to represent each physical layer. Used by the
	 * {@code toString} and {@code dumpState} methods.
	 */
    public static final String[] PHY_STRINGS = { "802.11", "RollCall", "802.15.4", "Undefined" };

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Can't load SHA-1 hashing algorithm.  Exiting.");
            System.exit(1);
        }
    }

    /**
	 * The regions in which this landmark exists.&nbsp; Every time a sample
	 * arrives, it has to be inserted into each {@link Region} in which the
	 * landmark exists.
	 */
    final List<Region> regions = Collections.synchronizedList(new ArrayList<Region>());

    /**
	 * How the landmark should send samples.
	 */
    public final int mode;

    /**
	 * The physical layer the landmark uses.
	 */
    public final int phy;

    /**
	 * The antenna number of this landmark on the hub.
	 */
    public final int antenna;

    /**
	 * The hub that this landmark is a part of.
	 */
    public final Hub hub;

    /**
	 * The name of this landmark.&nbsp; Used only for management, not
	 * localization.
	 */
    public final String name;

    /**
	 * Creates a new landmark with the specified parameters.
	 *
	 * @param name
	 * 		the name of this region.
	 * @param region
	 *            an initial {@link Region} in which this landmark exists.
	 * @param hub
	 *            the hub that this landmark is a part of.
	 * @param mode
	 *            the sample-sending mode that this landmark uses.
	 * @param phy
	 *            the physical layer of this landmark.
	 * @param antenna
	 *            the antenna discriminator of this landmark within the hub.
	 * @throws IllegalArgumentException
	 *             if <code>hub</code> is <code>null</code>.
	 */
    public Landmark(final String name, final Region region, final Hub hub, int mode, int phy, int antenna) throws IllegalArgumentException {
        if (hub == null) throw new IllegalArgumentException("Cannot create a landmark with a null hub.");
        this.hub = hub;
        this.mode = mode;
        this.phy = phy;
        this.antenna = antenna;
        if (region != null) this.regions.add(region);
        this.name = name;
        byte[] sha1Bytes = null;
        synchronized (Landmark.digest) {
            Landmark.digest.reset();
            Landmark.digest.update(this.hub.getID().toByteArray());
            Landmark.digest.update((byte) this.phy);
            Landmark.digest.update((byte) this.mode);
            Landmark.digest.update((byte) this.antenna);
            sha1Bytes = Landmark.digest.digest();
        }
        if (sha1Bytes == null) {
            System.err.println("Can't compute hash code: " + this.hub.getID() + "/" + this.hub.getName() + "/" + this.phy + "/" + this.antenna);
            System.exit(1);
        }
        int hashCode = 0;
        for (int i = 0; i < sha1Bytes.length; i++) {
            hashCode ^= (int) sha1Bytes[i] << ((i % 4) * 8);
        }
        this.hashCode = hashCode;
    }

    /**
	 * Adds a {@link Region} to this landmark's list of regions.
	 * 
	 * @param region
	 *            the <code>Region</code> to add
	 */
    public void addRegion(final Region region) {
        this.regions.add(region);
    }

    /**
	 * Returns a collection containing all of the regions that this landmark
	 * exists in.
	 * 
	 * @return
	 */
    public Collection<Region> getRegions() {
        final ArrayList<Region> regions = new ArrayList<Region>();
        regions.addAll(this.regions);
        return regions;
    }

    /**
	 * Removes this landmark from a region.
	 * 
	 * @param region
	 */
    public void removeRegion(final Region region) {
        this.regions.remove(region);
    }

    /**
	 * Generates a hash code for this landmark.&nbsp; Used for putting landmarks
	 * into hashing data structures.
	 */
    @Override
    public int hashCode() {
        return this.hashCode;
    }

    /**
	 * Compares this landmark to another object for equality.
	 */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Landmark) {
            Landmark landmark = (Landmark) o;
            return landmark.hub.getID().equals(this.hub.getID()) && landmark.phy == this.phy && landmark.antenna == this.antenna;
        }
        return o.equals(this);
    }

    /**
	 * Dumps the state of this object to a <code>PrintWriter</code>.&nbsp;
	 * Intended for debugging purposes.
	 * 
	 * @param output
	 *            where to write the output
	 */
    public void dumpState(PrintWriter output) {
        output.println(this.toString());
        output.flush();
    }

    /**
	 * Generates a human-readable <code>String</code> representation of this
	 * landmark object.
	 */
    @Override
    public String toString() {
        final StringBuffer rv = new StringBuffer();
        rv.append("Landmark (");
        rv.append(this.hub.getName() == null ? this.hub.getID() : this.hub.getName());
        rv.append('|');
        rv.append(Landmark.PHY_STRINGS[this.phy]);
        rv.append(':');
        rv.append(this.antenna).append(')');
        return rv.toString();
    }

    /**
	 * Used by {@link commands.ShowCmd} to display information about this
	 * landmark to the user.
	 * 
	 * @return a human-readable representation of this landmark, specifically
	 *         its hub, physical layer, and antenna.
	 */
    public String show() {
        return toString();
    }

    /**
	 * Compares a <code>Landmark</code> to another Landmark.&nbsp; This method
	 * is essentially pointless, as an applying an ordering to Landmark objects
	 * is meaningless.
	 * 
	 * @param l
	 *            the landmark to compare this landmark to.
	 * @return <0 if this landmark is "less than" {@code l}, >0 if this
	 *         landmark is "greater than" {@code l}, else 0.
	 */
    public int compareTo(Landmark l) {
        int rv = this.hub.getID().compareTo(l.hub.getID());
        if (rv != 0) return rv;
        rv = this.phy - l.phy;
        if (rv != 0) return rv;
        return this.antenna - l.antenna;
    }

    /**
	 * Returns an integer representing the physical layer type. If
	 * {@code phy_string} is not defined in {@link PHY_STRINGS},
	 * {@link #PHY_UNDEFINED} is returned.
	 * 
	 * @param phy_string
	 *            a string representing the physical layer type as a
	 *            human-readable string.
	 * @return an integer corresponding to the physical layer type, or
	 *         {@code PHY_UNDEFINED} if the physical layer type is not
	 *         recognized.
	 */
    public static int getPhyID(String phy_string) {
        if (phy_string.equalsIgnoreCase(PHY_STRINGS[PHY_802_11])) return PHY_802_11; else if (phy_string.equalsIgnoreCase(PHY_STRINGS[PHY_802_15_4])) return PHY_802_15_4; else if (phy_string.equalsIgnoreCase(PHY_STRINGS[PHY_ROLLCALL])) return PHY_ROLLCALL; else return PHY_UNDEFINED;
    }
}
