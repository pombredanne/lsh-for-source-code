package logahawk.formatters;

import java.security.*;
import java.util.*;
import net.jcip.annotations.*;

/**
 * Hashes log messages using a rolling hash -- that is the hash of the previous message is part of the hash for the
 * following hash. This means the hash can be used to detect when a log messages have been added, removed, or
 * reordered.
 */
@ThreadSafe
public class RollingHashedMessageFormatter extends HashedMessageFormatter {

    /**
	 * @throws CloneNotSupportedException The {@link MessageDigest} must be cloneable for {@link
	 * RollingHashedMessageFormatter#verify(List)} to work correctly. The constructor tests cloning by calling {@link
	 * MessageDigest#clone()} and letting the exception go uncaught.
	 */
    public RollingHashedMessageFormatter(MessageFormatter messageFormatter, MessageDigest messageDigest) throws CloneNotSupportedException {
        super(messageFormatter, messageDigest);
        this.messageDigest.clone();
    }

    @Override
    protected String createDigest(String message) {
        byte[] messageBytes = message.getBytes();
        String digest;
        synchronized (this.messageDigest) {
            if (this.previousDigest != null) this.messageDigest.update(this.previousDigest.getBytes());
            byte[] digestBytes = this.messageDigest.digest(messageBytes);
            digest = this.encode(digestBytes);
            this.previousDigest = digest;
        }
        return digest;
    }

    @Override
    public int verify(List<String> messages) throws IllegalArgumentException {
        RollingHashedMessageFormatter formatter;
        synchronized (this.messageDigest) {
            try {
                MessageDigest digest = (MessageDigest) this.messageDigest.clone();
                digest.reset();
                formatter = new RollingHashedMessageFormatter(this.messageFormatter, digest);
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        }
        List<HashMessagePair> pairs = this.splitMessage(messages);
        int validIndex = 0;
        for (HashMessagePair p : pairs) {
            String calcedDigest = formatter.createDigest(p.getMessage());
            if (p.getHash().equals(calcedDigest)) ++validIndex; else return validIndex;
        }
        return validIndex;
    }

    @GuardedBy("messageDigest")
    protected String previousDigest;
}
