package org.grailrtls.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;

/**
 * <p>
 * A <code>Fingerprint</code> is a set of RSSI values over a discrete time
 * interval.&nbsp; A finger print represent what a network transmitter "looks
 * like" to the localization system.&nbsp; A <code>Fingerprint</code> object
 * is immutable, and as a result it is inherently thread-safe.
 * </p>
 * 
 * @author Richard P. Martin
 * @author Robert S. Moore II
 * 
 */
public final class Fingerprint {

    /**
     * The set of RSSI values associated with each landmark for this
     * <code>Fingerprint</code>.&nbsp; The order of the values is the same as
     * the order of the {@link Landmark} objects stored in {@link #landmarks}.
     */
    private final double[] rssi_values;

    /**
     * The number of samples used to compute each RSSI value.
     */
    private final int[] count;

    /**
     * The earliest time stamp for which this finger print is valid.
     */
    final long time_start;

    /**
     * The latest time stamp for which this finger print is valid.
     */
    final long time_end;

    /**
     * A list of {@link Landmark} objects that are associated with this finger
     * print.&nbsp; The order of the landmarks is the same as the order of RSSI
     * values in {@link #rssi_values}.
     */
    final List<Landmark> landmarks;

    final long hashcode;

    /**
     * The network transmitter for which this fingerprint has been computed.
     */
    private final NetworkTransmitter transmitter;

    /**
     * The FingerprintFunction that generated this fingerprint.
     */
    public final FingerprintFunction fingerprint_function;

    /**
     * Creates a new <code>Fingerprint</code> object with the given
     * values.&nbsp; Even though finger prints are immutable, the argument
     * <code>landmarks</code> needs to be thread-safe if another
     * {@link Thread} is capable of modifying its contents.
     * 
     * @param fingerprint_function
     * 				the FingerprintFunction that generated this fingerprint.
     * @param transmitter
     *            the NetTxer that this finger print models
     * @param rssi_values
     *            the set of computed RSSI values for each {@link Landmark}
     *            object in <code>landmarks</code>, in the same order
     * @param count
     *            the number of samples used to determine each RSSI value, in
     *            the same order
     * @param time_start
     *            the time stamp of the oldest computed RSSI values in this
     *            finger print
     * @param time_end
     *            the time stamp of the newest computed RSSI values in this
     *            finger print
     * @param landmarks
     *            the set of landmarks associated with this finger print, in the
     *            same order as the RSSI values of <code>rssi_values</code>
     */
    Fingerprint(final FingerprintFunction fingerprint_function, final NetworkTransmitter transmitter, final List<Landmark> landmarks, final double[] rssi_values, final int[] count, final long time_start, final long time_end) {
        if (fingerprint_function == null) throw new NullPointerException("Cannot make a fingerprint from a null function.");
        this.fingerprint_function = fingerprint_function;
        this.transmitter = transmitter;
        this.rssi_values = rssi_values;
        this.count = count;
        this.time_start = time_start;
        this.time_end = time_end;
        this.landmarks = landmarks;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new NullPointerException(nsae.getMessage());
        }
        digest.update(this.transmitter.mac_address.toByteArray());
        digest.update(new Integer(this.transmitter.physical_layer_type).byteValue());
        digest.update(new Integer(this.fingerprint_function.id).byteValue());
        byte[] t_start = new byte[8];
        byte[] t_end = new byte[8];
        for (int i = 0; i < 7; i++) {
            t_start[i] = (byte) (this.time_start << 8 * i);
            t_end[i] = (byte) (this.time_end << 8 * i);
        }
        digest.update(t_start);
        digest.update(t_end);
        for (Landmark landmark : this.landmarks) {
            digest.update((byte) ((int) this.getRSSI(landmark)));
        }
        byte[] hash_code = digest.digest();
        long hash_long = 0l;
        for (int i = 0; i < 16; i++) {
            hash_long ^= hash_code[i] << 8 * (i % 8);
        }
        this.hashcode = hash_long;
    }

    /**
     * Gives the RSSI value of a provided {@link Landmark} for this finger
     * print.&nbsp; If the provided <code>Landmark</code> object does not
     * exist for this finger print, {@link NetworkTransmitter#RSSI_PAD_VALUE} is
     * returned.
     * 
     * @param landmark
     *            the <code>Landmark</code> object associated with the desired
     *            RSSI value
     * @return the RSSI value for the given <code>Landmark</code> object, or
     *         <code>NetworkTransmitter.RSSI_PAD_VALUE</code> if the landmark
     *         is not associated with this finger print.
     */
    public float getRSSI(final Landmark landmark) {
        final int landmark_index = this.landmarks.indexOf(landmark);
        return (float) (landmark_index == -1 ? NetworkTransmitter.RSSI_PAD_VALUE : this.rssi_values[landmark_index]);
    }

    /**
     * Generates a mapping from landmarks to the samples from that landmark that were used
     * to generate this fingerprint.
     * @return a map from a landmark to the samples from that landmark that were used to generate
     * this fingerprint.
     */
    Map<Landmark, List<Sample>> getOriginalSamples() {
        HashMap<Landmark, List<Sample>> landmarks_to_samples = new HashMap<Landmark, List<Sample>>();
        for (int landmark_iter = 0; landmark_iter < this.landmarks.size(); landmark_iter++) {
            Landmark landmark = landmarks.get(landmark_iter);
            List<Sample> samples = transmitter.samples_by_landmark.get(landmark);
            List<Sample> used_samples = new ArrayList<Sample>();
            for (int sample_iter = 0; sample_iter < count[landmark_iter]; sample_iter++) {
                used_samples.add(samples.get(sample_iter));
            }
            landmarks_to_samples.put(landmark, used_samples);
        }
        return landmarks_to_samples;
    }

    /**
     * Dumps the complete state of this fingerprint to the provided
     * <code>PrintWriter</code>.&nbsp; This is really just a debug method and
     * probably shouldn't be used for anything at the end-user level.
     * 
     * @param output
     *            what to dump to
     */
    public void dumpState(final PrintWriter output) {
        output.print(toString());
        output.flush();
    }

    /**
     * Provides a human-readable <code>String</code> representation of this
     * <code>Fingerprint</code> object.
     */
    @Override
    public String toString() {
        final StringBuffer rv = new StringBuffer("Fingerprint ");
        rv.append(this.transmitter.mac_address).append("\n\t[").append(new Date(this.time_start)).append(" - ").append(new Date(this.time_end)).append("]\n");
        for (int i = 0; i < this.rssi_values.length; i++) {
            final Landmark landmark = this.landmarks.get(i);
            rv.append('\t').append("[");
            rv.append(landmark);
            rv.append("] ").append(String.format("%.2f", new Double(this.rssi_values[i]))).append(" (").append(this.count[i]).append(")\n");
        }
        return rv.toString();
    }

    /**
     * Used by {@link commands.ShowCmd} to display information about this fingerprint to
     * the user.
     * 
     * @return a human-readable representation of the timestamps and RSSI values
     *         of this <code>Fingerprint</code>
     */
    public String show() {
        return toString();
    }

    /**
     * Gets the network transmitter associated with this fingerprint.
     * 
     * @return the {@link NetworkTransmitter} object associated with this
     *         fingerprint
     */
    public NetworkTransmitter getTransmitter() {
        return this.transmitter;
    }
}
