package org.musicbrainz.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NotImplementedException;
import org.musicbrainz.Query;
import org.musicbrainz.model.Disc;

/**
 * Utilities for working with Audio CDs.
 * 
 * This module contains utilities for working with Audio CDs.
 * 
 * @author Chris Colvard
 */
public class DiscUtil {

    /**
	 * <p>Returns a URL for adding a disc to the MusicBrainz database.
	 * A fully initialized {@link Disc} object is needed, as
	 * returned by {@link #readDisc(String)}. A {@link Disc} object returned 
	 * by the web service doesn't provide the necessary information.</p>
	 * 
	 * <p>Note that the created URL is intended for interactive use and points
	 * to the MusicBrainz disc submission wizard by default. This method just
	 * returns a URL, no network connection is needed. The disc drive isn't used.</p>
	 * 
	 * @param disc A fully initialized {@link Disc} object
	 * @param host A string containing a host name
	 * @param port An integer containing a port number
	 * @return A string containing the submission URL
	 */
    public static String getSubmissionUrl(final Disc disc, final String host, final int port) {
        final StringBuilder submissionUrl = new StringBuilder("http://");
        submissionUrl.append(host);
        if (port != 80) {
            submissionUrl.append(host).append(":").append(port);
        }
        submissionUrl.append("/bare/cdlookup.html?id=").append(disc.getDiscId());
        submissionUrl.append("&toc=");
        submissionUrl.append(disc.getFirstTrackNum()).append("+").append(disc.getLastTrackNum()).append("+").append(disc.getSectors());
        for (Disc.Track t : disc.getTracks()) {
            submissionUrl.append("+").append(t.getOffset());
        }
        submissionUrl.append("&tracks=").append(disc.getLastTrackNum());
        return submissionUrl.toString();
    }

    /**
	 * <p>Reads an Audio CD in the disc drive.</p>
	 * <p>This reads a CD's table of contents (TOC) and calculates the MusicBrainz
	 * DiscID, which is a 28 character ASCII string. This DiscID can be used
	 * to retrieve a list of matching releases from the web service (see
	 * {@link Query}).</p>
	 * 
	 * <p>Note that an Audio CD has to be in drive for this to work. The
	 * <code>deviceName</code> argument may be used to set the device. The default
	 * depends on the operating system (on linux, it's <code>/dev/cdrom</code>.
	 * No network connection is needed for this function.</p>
	 * 
	 * <p>If the device doesn't exist or there's no valid Audio CD in the drive,
	 * a {@link DiscError} exception is raised.</p>
	 * 
	 * @param deviceName A string containing the CD drive's device name
	 * @return A {@link Disc}
	 * @throws NotImplementedException If DiscID generation isn't supported
	 * @throws DiscError If there was a problem reading the disc
	 */
    public static Disc readDisc(final String deviceName) throws NotImplementedException, DiscError {
        throw new NotImplementedException();
    }

    /**
	 * TODO document me!
	 * @param disc
	 * @return
	 */
    public String calculateID(final Disc disc) {
        String id = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            ps.printf("%02X", disc.getFirstTrackNum());
            md.update(os.toByteArray());
            os.reset();
            ps.printf("%02X", disc.getLastTrackNum());
            md.update(os.toByteArray());
            for (int i = 0; i < 100; i++) {
                os.reset();
                if (i == 0) {
                    ps.printf("%08X", disc.getSectors());
                } else if (i > disc.getLastTrackNum()) {
                    ps.printf("%08X", 0);
                } else {
                    ps.printf("%08X", disc.getTracks().get(i - 1).getOffset());
                }
                md.update(os.toByteArray());
            }
            byte[] digest = md.digest();
            String encoded = new String(Base64.encodeBase64(digest));
            encoded = encoded.replace('/', '_');
            encoded = encoded.replace('+', '.');
            encoded = encoded.replace('=', '-');
            id = encoded;
        } catch (Exception e) {
            System.err.println("Could not compute discID");
            e.printStackTrace();
        }
        return id;
    }
}
