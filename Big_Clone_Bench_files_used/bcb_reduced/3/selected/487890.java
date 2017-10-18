package zhyi.zse.hash;

import java.security.MessageDigest;
import java.util.Objects;

/**
 * Adapts {@link MessageDigest} to {@link Hash}.
 * @author Zhao Yi
 */
public class MessageDigestHash implements Hash {

    private MessageDigest md;

    public MessageDigestHash(MessageDigest md) {
        this.md = Objects.requireNonNull(md);
    }

    @Override
    public void update(byte[] data, int offset, int length) {
        md.update(data, offset, length);
    }

    @Override
    public void reset() {
        md.reset();
    }

    @Override
    public String complete() {
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
