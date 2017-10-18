package org.ceno.communication.cli.internal;

import org.ceno.communication.cli.DigestCreationException;
import org.ceno.communication.cli.IDigesterService;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import org.eclipse.core.runtime.Status;

/**
 * Fast simple Checksum generation implementation basing on JDK Adler32
 * 
 * @author Andre Albert &lt;andre.albert82@googlemail.com&gt
 * @created 04.07.2010
 * @since 0.1.0
 */
public class JDKDigesterService implements IDigesterService {

    private final Checksum checksum;

    /**
	 * 
	 * @since 0.1.0
	 */
    public JDKDigesterService() {
        checksum = new Adler32();
    }

    /**
	 * {@inheritDoc}
	 **/
    public String digestAdler32(final InputStream data) throws DigestCreationException {
        String result = null;
        checksum.reset();
        final byte[] read = new byte[256];
        int readBytes = -1;
        try {
            while ((readBytes = data.read(read)) != -1) {
                checksum.update(read, 0, readBytes);
            }
            result = String.valueOf(checksum.getValue());
        } catch (final IOException e) {
            result = null;
            throw new DigestCreationException("Could not read in datastream to generate digest", e);
        } finally {
            try {
                data.close();
            } catch (final IOException e) {
                Activator.log(Status.WARNING, "Could not close data stream");
            }
        }
        return result;
    }

    public String digestMD5(final InputStream data) throws DigestCreationException {
        MessageDigest md;
        String result;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[128];
            int read = -1;
            while ((read = data.read(buf)) > 0) {
                md.update(buf, 0, read);
            }
            result = convertToHex(md.digest());
        } catch (Exception e) {
            result = null;
            throw new DigestCreationException("Could not read in datastream to generate digest", e);
        } finally {
            try {
                data.close();
            } catch (final IOException e) {
                Activator.log(Status.WARNING, "Could not close data stream");
            }
        }
        return result;
    }

    private String convertToHex(final byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
