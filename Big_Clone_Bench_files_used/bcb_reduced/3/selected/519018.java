package org.metastatic.rsync;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GeneratorStream {

    /**
    * The configuration.
    */
    protected final Configuration config;

    /**
    * The list of {@link GeneratorListeners}.
    */
    protected final List listeners;

    /**
    * The intermediate byte buffer.
    */
    protected final byte[] buffer;

    /**
    * The current index in {@link #buffer}.
    */
    protected int ndx;

    /**
    * The number of bytes summed thusfar.
    */
    protected long count;

    public GeneratorStream(Configuration config) {
        this.config = config;
        this.listeners = new LinkedList();
        buffer = new byte[config.blockLength];
        reset();
    }

    /**
    * Add a {@link GeneratorListener} to the list of listeners.
    *
    * @param listener The listener to add.
    */
    public void addListener(GeneratorListener listener) {
        if (listener == null) throw new IllegalArgumentException();
        listeners.add(listener);
    }

    /**
    * Remove a {@link GeneratorListener} from the list of listeners.
    *
    * @param listener The listener to add.
    * @return True if a listener was really removed (i.e. that the
    *         listener was in the list to begin with).
    */
    public boolean removeListener(GeneratorListener listener) {
        return listeners.remove(listener);
    }

    /**
    * Reset this generator, to be used for another data set.
    */
    public void reset() {
        ndx = 0;
        count = 0L;
    }

    /**
    * Update this generator with a single byte.
    *
    * @param b The next byte
    */
    public void update(byte b) throws ListenerException {
        ListenerException exception = null, current = null;
        buffer[ndx++] = b;
        if (ndx == buffer.length) {
            ChecksumPair p = generateSum(buffer, 0, buffer.length);
            for (Iterator it = listeners.listIterator(); it.hasNext(); ) {
                try {
                    ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
                } catch (ListenerException le) {
                    if (exception != null) {
                        current.setNext(le);
                        current = le;
                    } else {
                        exception = le;
                        current = le;
                    }
                }
            }
            if (exception != null) throw exception;
            ndx = 0;
        }
    }

    /**
    * Update this generator with a portion of a byte array.
    *
    * @param buf The next bytes.
    * @param off The offset to begin at.
    * @param len The number of bytes to update.
    */
    public void update(byte[] buf, int off, int len) throws ListenerException {
        ListenerException exception = null, current = null;
        int i = off;
        do {
            int l = Math.min(len - (i - off), buffer.length - ndx);
            System.arraycopy(buf, i, buffer, ndx, l);
            i += l;
            ndx += l;
            if (ndx == buffer.length) {
                ChecksumPair p = generateSum(buffer, 0, buffer.length);
                for (Iterator it = listeners.listIterator(); it.hasNext(); ) {
                    try {
                        ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
                    } catch (ListenerException le) {
                        if (exception != null) {
                            current.setNext(le);
                            current = le;
                        } else {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null) throw exception;
                ndx = 0;
            }
        } while (i < off + len);
    }

    /**
    * Update this generator with a byte array.
    *
    * @param buf The next bytes.
    */
    public void update(byte[] buf) throws ListenerException {
        update(buf, 0, buf.length);
    }

    /**
    * Finish generating checksums, flushing any buffered data and
    * resetting this instance.
    */
    public void doFinal() throws ListenerException {
        ListenerException exception = null, current = null;
        if (ndx > 0) {
            ChecksumPair p = generateSum(buffer, 0, ndx);
            for (Iterator it = listeners.listIterator(); it.hasNext(); ) {
                try {
                    ((GeneratorListener) it.next()).update(new GeneratorEvent(p));
                } catch (ListenerException le) {
                    if (exception != null) {
                        current.setNext(le);
                        current = le;
                    } else {
                        exception = le;
                        current = le;
                    }
                }
            }
            if (exception != null) throw exception;
        }
        reset();
    }

    /**
    * Generate a sum pair for a portion of a byte array.
    * 
    * @param buf The byte array to checksum.
    * @param off Where in <code>buf</code> to start.
    * @param len How many bytes to checksum.
    * @return A {@link ChecksumPair} for this byte array.
    */
    protected ChecksumPair generateSum(byte[] buf, int off, int len) {
        ChecksumPair p = new ChecksumPair();
        config.weakSum.check(buf, off, len);
        config.strongSum.update(buf, off, len);
        if (config.checksumSeed != null) {
            config.strongSum.update(config.checksumSeed, 0, config.checksumSeed.length);
        }
        p.weak = config.weakSum.getValue();
        p.strong = new byte[config.strongSumLength];
        System.arraycopy(config.strongSum.digest(), 0, p.strong, 0, config.strongSumLength);
        p.offset = count;
        p.length = len;
        count += len;
        return p;
    }
}
