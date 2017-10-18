package logahawk.formatters;

import java.security.*;
import java.util.*;
import net.jcip.annotations.*;
import logahawk.listeners.*;
import logahawk.util.*;

/**
 * Calculates a hash of the log message, and prepends that has to the message. This formatter uses another {@link
 * MessageFormatter} to format the actual message, and this class handles the hashing.
 *
 * The purpose of this {@link MessageFormatter}is to provide integrity for individual log messages. This cannot
 * determine whether log messages have been added, removed, or reordered. (See {@link RollingHashedMessageFormatter}
 */
@ThreadSafe
public class HashedMessageFormatter implements MessageFormatter {

    public HashedMessageFormatter(MessageFormatter messageFormatter, MessageDigest messageDigest) {
        this.messageFormatter = messageFormatter;
        this.messageDigest = messageDigest;
    }

    /**
	 * The log message is first created by the constructor provided {@link MessageFormatter}. Then that message is
	 * digested. That digest is prepended to the message, and that final message is returned.
	 */
    public String format(LogMeta meta, String message) {
        String innerMsg = this.messageFormatter.format(meta, message);
        String digest = this.createDigest(innerMsg);
        String prefix = this.getHashPrefix();
        String suffix = this.getHashSuffix();
        StringBuilder sb = new StringBuilder(prefix.length() + digest.length() + suffix.length() + innerMsg.length());
        sb.append(prefix);
        sb.append(digest);
        sb.append(suffix);
        sb.append(innerMsg);
        return sb.toString();
    }

    protected volatile String hashPrefix = "[";

    /** Returns the prefix for the Hash. */
    public String getHashPrefix() {
        return this.hashPrefix;
    }

    public void setHashPrefix(String value) {
        this.hashPrefix = value;
    }

    protected volatile String hashSuffix = "] ";

    /** Returns the suffix for the Hash. */
    public String getHashSuffix() {
        return this.hashSuffix;
    }

    public void setHashSuffix(String value) {
        this.hashSuffix = value;
    }

    /** Used to generate the hash. This will be reused for every message, and reset before each usage. */
    @GuardedBy("itself")
    protected final MessageDigest messageDigest;

    protected final MessageFormatter messageFormatter;

    /** Creates a digest from the provided message. This method may not be idempotent depending on the implementation. */
    protected String createDigest(String message) {
        byte[] messageBytes = message.getBytes();
        String digest;
        synchronized (this.messageDigest) {
            byte[] digestBytes = this.messageDigest.digest(messageBytes);
            digest = this.encode(digestBytes);
        }
        return digest;
    }

    /**
	 * Encodes the digest hash bytes into something smaller for a log message. This method does not change the {@link
	 * MessageDigest}
	 *
	 * @param digest The byte[] buffer to encode, usually the results of a {@link MessageDigest}.
	 *
	 * @return A char[] containing hexadecimal characters
	 *
	 * @since 1.4
	 */
    protected String encode(byte[] digest) {
        return Base32.encode(digest);
    }

    /**
	 * Given a list of log messages, split the hash and message and return them inside of {@link HashMessagePair}s. This
	 * method exists to simplify {@link HashedMessageFormatter#verify(List)} to make derived implementations simpler.
	 */
    protected List<HashMessagePair> splitMessage(List<String> messages) {
        List<HashMessagePair> pairs = new ArrayList<HashMessagePair>();
        String prefix = this.getHashPrefix();
        String suffix = this.getHashSuffix();
        for (String finalMessage : messages) {
            int prefixIndex = finalMessage.indexOf(prefix);
            if (prefixIndex != 0) throw new IllegalArgumentException("Cannot find hash; cannot find prefix \"" + prefix + "\"");
            int suffixIndex = finalMessage.indexOf(suffix, prefixIndex);
            if (suffixIndex < 0) throw new IllegalArgumentException("Cannot find hash; cannot find suffix \"" + suffix + "\"");
            String hash = finalMessage.substring(prefixIndex + prefix.length(), suffixIndex);
            String message = finalMessage.substring(suffixIndex + suffix.length());
            pairs.add(new HashMessagePair(hash, message));
        }
        return pairs;
    }

    /**
	 * Verifies that the hash contained within the messages is the correct hash.
	 *
	 * @return The index of the first invalid message. If the returned index equals the length of the provided {@link
	 *         List}, then all messages are valid.
	 *
	 * @throws IllegalArgumentException Thrown if the message does not contain a hash in the expected location.
	 */
    public int verify(List<String> messages) throws IllegalArgumentException {
        List<HashMessagePair> pairs = this.splitMessage(messages);
        int validIndex = 0;
        for (HashMessagePair p : pairs) {
            String calcedDigest = this.createDigest(p.getMessage());
            if (p.getHash().equals(calcedDigest)) ++validIndex; else return validIndex;
        }
        return validIndex;
    }

    /** Helper class that holds the hash and original message separately. */
    protected static class HashMessagePair {

        private final String hash;

        public String getHash() {
            return this.hash;
        }

        private final String message;

        public String getMessage() {
            return this.message;
        }

        public HashMessagePair(String hash, String message) {
            this.hash = hash;
            this.message = message;
        }
    }
}
