package org.grailrtls.server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;
import java.security.*;

/**
 * Represents a link-layer frame and associated information including time of
 * arrival at the server, received signal strength indicator (RSSI), noise
 * level, angle information (if available), layer 2 and layer 3 (if applicable)
 * addresses.&nbsp; Samples are created by the {@link Landmark} class while
 * accepting sample data from a landmark.
 * 
 * @author Richard P. Martin
 * @author Robert S. Moore II
 */
public final class Sample {

    /**
	 * The date format for printing dates.
	 */
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SSS");

    /**
	 * The layer-2 address associated with this sample.
	 */
    public final MACAddress mac_address;

    /**
	 * The layer-3 address associated with this sample.&nbsp; For devices that
	 * have no layer-2 address, this value will be <code>null</code>.
	 */
    public final InetAddress net_address;

    /**
	 * The noise level of this sample, in the units of whatever reader is being
	 * used.
	 */
    public final double noise;

    /**
	 * The first <i>X</i> bytes of the frame header when it was captured by a
	 * landmark.&nbsp; Typically, this is only the first 256 bytes, but can
	 * really be any size.&nbsp; Can be used for extracting additional
	 * information from a sample.
	 */
    public final byte header[];

    /**
	 * The time stamps of the oldest and newest frames captured in a histogram
	 * sample, measured at the landmark.&nbsp; This array has a size of 2, and
	 * contains the oldest time stamp and the newest time stamp,
	 * respectively.&nbsp; These values are not associated with
	 * {@link #time_stamp}, which is the time that the sample arrived on the
	 * server.
	 */
    public final long[] time_interval;

    /**
	 * The received signal strength indicator (RSSI) value of this sample.&nbsp;
	 * If a landmark is operating in {@link Landmark#MODE_HISTOGRAM} mode, this
	 * value will represent the median RSSI for the transmitted histogram.
	 */
    public final double rssi;

    /**
	 * The Sequence Number associated with the packet. If the landmark is
	 * operating in the Histogram mode, this sequence number will be set to 0.
	 */
    public final int sequence_number;

    /**
	 * Frame Control bits from MAC header. Frame Control Value Can be parsed to
	 * detect the type of packet - Data, QoS, Management, Control, Retry, etc.
	 */
    public final int frame_ctrl;

    /**
	 * QoS Priority is the Priority with which QoS Data packets are sent. For
	 * non-QoS packets, the default value is 0
	 */
    public final int QoS_Priority;

    /**
	 * Stores the histogram of RSSI values received from a landmark.&nbsp; If a
	 * landmark is operating in {@link Landmark#MODE_PACKET} mode, this value
	 * will be <code>null</code>.
	 */
    public final double[] rssi_histogram_values;

    /**
	 * Stores the number of times each RSSI value was received by a
	 * landmark.&nbsp; The index of the count is the same as the index of the
	 * RSSI value in {@link #rssi_histogram_values}.&nbsp; If a landmark is
	 * operating in (@link Landmark#MODE_PACKET} mode, this value will be
	 * <code>null</code>.
	 */
    public final int[] rssi_histogram_count;

    /**
	 * The {@link Landmark} object from which this <code>Sample</code> was
	 * received.
	 */
    public final Landmark landmark;

    /**
	 * The time (in milliseconds since the epoch) that this sample was received
     * by a hub.
	 */
    public final long time_stamp;

    /**
     * The time (in milliseconds since the epoch) that this sample was created
     * within the server. &nbsp; This is the value used in computing whether a sample
     * should be "expired" or not.
     */
    public final long time_created;

    /**
     * The number of bytes used by the server-hub protocol to send this sample.
     */
    public final int protocol_byte_length;

    /**
	 * The XOR of the {@code time_stamp} and {@code mac_address} fields.
	 */
    public final long hash_code;

    public final byte[] header_hash;

    /**
	 * The physical layer type of the network transmitter that generated this
	 * sample.
	 */
    public final int phy;

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            digest = null;
        }
    }

    /**
	 * Creates a new <code>Sample</code> object with the given parameters.
	 * 
	 * @param time_interval
	 *            contains the time stamps of the oldest and newest packets
	 *            (respectively) in a histogram
	 * @param mac_address
	 *            the layer-2 address of the network transmitter associated with
	 *            this sample
	 * @param phy
	 *            the physical layer identifier for the device that generated
	 *            this sample.
	 * @param net_address
	 *            the layer-3 address (if available) of the network transmitter
	 *            associated with this sample
	 * @param rssi
	 *            the received signal strength indicated associated with this
	 *            sample, as calculated by the landmark
	 * @param sequence_number
	 *            MAC sequence Number associated with the packet
	 * @param frame_ctrl
	 *            The MAC header's frame Control bytes (2 bytes) associated with
	 *            the packet.
	 * @param QoS_priority
	 *            The QoS Priority number for QoS enabled Packets. This value
	 *            ranges between 0 to 7. For Non, QoS packets, this value by
	 *            default is set to 0.
	 * @param landmark
	 *            the landmark that received this sample
	 * @param rssi_histogram_values
	 *            the RSSI readings of this network transmitter when the
	 *            associated landmark is operating in
	 *            {@link Landmark#MODE_HISTOGRAM} mode
	 * @param rssi_histogram_count
	 *            the count for each observed RSSI reading of this network
	 *            transmitter when the associated landmark is operating in
	 *            {@link Landmark#MODE_HISTOGRAM} mode
	 * @param header
	 *            the first X bytes of the received frame
	 */
    Sample(final long[] time_interval, final MACAddress mac_address, final int phy, final InetAddress net_address, final double rssi, final int sequence_number, final int frame_ctrl, final int QoS_priority, Landmark landmark, final double[] rssi_histogram_values, final int[] rssi_histogram_count, final byte[] header, final int protocol_byte_length) {
        this.time_created = System.currentTimeMillis();
        this.protocol_byte_length = protocol_byte_length;
        this.time_interval = time_interval;
        this.net_address = net_address;
        this.mac_address = mac_address;
        this.rssi = rssi;
        this.sequence_number = sequence_number;
        this.frame_ctrl = frame_ctrl;
        this.QoS_Priority = QoS_priority;
        this.landmark = landmark;
        this.noise = 0;
        this.header = header;
        this.rssi_histogram_count = rssi_histogram_count;
        this.rssi_histogram_values = rssi_histogram_values;
        this.time_stamp = time_interval[0] + ((time_interval[1] - time_interval[0]) / 2);
        if (Sample.digest != null) {
            byte[] mac_bytes = this.mac_address.toByteArray();
            byte[] hashcode_bytes;
            synchronized (Sample.digest) {
                digest.update(mac_bytes);
                for (int i = 0; i < 8; i++) {
                    digest.update((byte) (this.time_created >> 8 * i));
                    digest.update((byte) (this.time_stamp >> 8 * i));
                }
                digest.update((byte) phy);
                digest.update((byte) landmark.antenna);
                digest.update((byte) landmark.mode);
                digest.update((byte) this.sequence_number);
                digest.update((byte) this.QoS_Priority);
                digest.update((byte) Double.doubleToLongBits(this.rssi));
                if (this.header != null && this.header.length > 0) digest.update(this.header);
                hashcode_bytes = digest.digest();
            }
            long temp_hash = 0l;
            for (int i = 0; i < 8; i++) {
                temp_hash ^= ((hashcode_bytes[2 * i] ^ hashcode_bytes[2 * i + 1]) << 8 * i);
            }
            this.hash_code = temp_hash;
        } else {
            int hi = (int) (this.time_stamp >> 32) ^ (int) (this.mac_address.toLong() >> 32);
            int lo = (int) this.time_stamp ^ (int) this.mac_address.toLong();
            this.hash_code = (long) hi << 32 + lo;
        }
        this.phy = phy;
        if (this.header != null && this.header.length > 145) {
            byte[] temp_bytes = new byte[this.header.length - 145];
            System.arraycopy(this.header, 145, temp_bytes, 0, temp_bytes.length);
            synchronized (Sample.digest) {
                this.header_hash = digest.digest(temp_bytes);
            }
        } else this.header_hash = null;
    }

    /**
	 * Writes this <code>Sample</code> to <code>output</code> for debugging
	 * purposes.
	 * 
	 * @param output
	 *            where to write this <code>Sample</code>
	 */
    public void dumpState(final PrintWriter output) {
        output.print("Sample from ");
        output.print(this.mac_address);
        output.print('/');
        output.println(this.net_address);
        output.println(this.landmark);
        output.print("Timestamp: ");
        output.println(new Date(this.time_stamp));
        output.print("RSSI: ");
        output.println(new Double(this.rssi));
        output.print("Noise: ");
        output.println(new Double(this.noise));
        output.print("Time interval: ");
        output.print(new Date(this.time_interval[0]));
        output.print(" - ");
        output.println(new Date(this.time_interval[1]));
        if (this.rssi_histogram_values != null && this.rssi_histogram_values.length > 0) {
            output.println("Histogram (VALUE | COUNT):");
            for (int i = 0; i < this.rssi_histogram_values.length; i++) {
                output.print(this.rssi_histogram_values[i]);
                output.print(" | ");
                output.println(this.rssi_histogram_count[i]);
            }
        }
        if (this.header != null) {
            int b;
            output.println("Packet Header:");
            for (int i = 1; i <= this.header.length; i++) {
                b = ((this.header[i - 1]) & 0xFF);
                if (b <= 15) {
                    output.print("0");
                }
                output.print(Integer.toHexString(b));
                if ((i % 4) == 0) {
                    output.print(":");
                }
                if ((i % 32) == 0) {
                    output.print("\n");
                }
            }
        }
        output.println();
        output.flush();
    }

    /**
	 * Generates a human-readable representation of this sample.
	 */
    @Override
    public String toString() {
        StringBuffer rv = new StringBuffer();
        String dateString = "?";
        try {
            dateString = dateFormatter.format(new Date(this.time_stamp));
        } catch (ArrayIndexOutOfBoundsException aioob) {
        }
        rv.append(this.rssi).append('\t').append(" [").append(dateString).append(']');
        return rv.toString();
    }

    public String fullString() {
        return null;
    }

    /**
	 * Used by {@link commands.ShowCmd} to display information about this sample
	 * to the user.
	 * 
	 * @return a human-readable representation of the RSSI value of this sample
	 */
    public String show() {
        return String.valueOf(this.rssi);
    }
}
