package net.sf.nic.java;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Wraps a single command and its parameters.
 *
 * @author Juergen_Kellerer, 2010-01-27
 * @version 1.0
 */
public class Command {

    private static Charset utf8 = Charset.forName("UTF-8");

    private static MessageDigest getDigest(CommandType type, char... secretKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toBytes(secretKey));
            md.update(new byte[] { type.getId() });
            return md;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] toBytes(char[] secretKey) {
        if (secretKey == null || secretKey.length == 0) return new byte[0];
        return utf8.encode(CharBuffer.wrap(secretKey)).array();
    }

    private CommandType type;

    private String[] parameters;

    Command(CommandType type, String[] parameters) {
        if (type == null) throw new NullPointerException("Type may not be null");
        this.type = type;
        this.parameters = parameters;
    }

    public CommandType getType() {
        return type;
    }

    public String[] getParameters() {
        return parameters;
    }

    public ByteBuffer writeTo(ByteBuffer buffer, char[] secretKey) {
        MessageDigest md = getDigest(type, secretKey);
        buffer.put(type.getId());
        buffer.putInt(parameters == null ? -1 : parameters.length);
        if (parameters != null) {
            for (String p : parameters) {
                byte[] param = p.getBytes(utf8);
                buffer.putInt(param.length);
                buffer.put(param);
                md.update(param);
            }
        }
        buffer.put(md.digest());
        return buffer;
    }

    public static Command readFrom(ByteBuffer buffer, char[] secretKey) {
        CommandType type = CommandType.valueOf(buffer.get());
        Command c = new Command(type, null);
        MessageDigest md = getDigest(type, secretKey);
        int paramCount = buffer.getInt();
        if (paramCount != -1) {
            int p = 0;
            c.parameters = new String[paramCount];
            while (paramCount-- > 0) {
                byte[] buf = new byte[buffer.getInt()];
                buffer.get(buf);
                md.update(buf);
                c.parameters[p++] = new String(buf, utf8);
            }
        }
        byte[] digest = md.digest(), referenceDigest = new byte[digest.length];
        buffer.get(referenceDigest);
        if (!Arrays.equals(digest, referenceDigest)) throw new IllegalArgumentException("The secret that was used for signing, differs.");
        return c;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;
        Command command = (Command) o;
        if (!Arrays.equals(parameters, command.parameters)) return false;
        if (type != command.type) return false;
        return true;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (parameters != null ? Arrays.hashCode(parameters) : 0);
        return result;
    }
}
