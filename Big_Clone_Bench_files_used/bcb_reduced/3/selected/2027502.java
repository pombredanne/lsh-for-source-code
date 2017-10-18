package opennlp.tools.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import opennlp.model.Event;
import opennlp.model.EventStream;

public class HashSumEventStream implements EventStream {

    private final EventStream eventStream;

    private MessageDigest digest;

    public HashSumEventStream(EventStream eventStream) {
        this.eventStream = eventStream;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean hasNext() throws IOException {
        return eventStream.hasNext();
    }

    public Event next() throws IOException {
        Event event = eventStream.next();
        try {
            digest.update(event.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not available!");
        }
        return event;
    }

    /**
   * Calculates the hash sum of the stream. The method must be
   * called after the stream is completely consumed.
   * 
   * @return the hash sum
   * @throws IllegalStateException if the stream is not consumed completely,
   * completely means that hasNext() returns false
   */
    public BigInteger calculateHashSum() {
        return new BigInteger(1, digest.digest());
    }

    public void remove() {
    }
}
