package net.sourceforge.epoint.pgp;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import net.sourceforge.epoint.io.PacketOutputStream;
import net.sourceforge.epoint.util.Dumper;

/**
 * OpenPGP encryption engine
 * 
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class EncryptStream extends FilterOutputStream {

    private MessageDigest md;

    private PacketOutputStream pos, sos;

    private Signer sig;

    /**
     * used for signatures inside the encrypted stream
     * @see ONEPASSPacket
     * @see SIGNATUREPacket
     */
    public interface Signer {

        /**
         * Write preamble before <code>LITERALPacket</code>.
         *
         * typically one or more <code>ONEPASSPacket</code>s
         */
        public void writePreamble(OutputStream os) throws Exception;

        public void update(int b) throws IOException;

        public void update(byte[] b, int off, int len) throws IOException;

        /**
         * Writet signatures after <code>LITERALPacket</code>.
         */
        public void writeSignatures(OutputStream os) throws IOException;
    }

    public static class SignerStream extends OutputStream {

        private Signer sig;

        private OutputStream out;

        public void write(int b) throws IOException {
            sig.update(b);
            out.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            sig.update(b, off, len);
            out.write(b, off, len);
        }

        public void close() throws IOException {
            out.close();
        }

        public void flush() throws IOException {
            out.flush();
        }

        public SignerStream(Signer signer, OutputStream o) {
            sig = signer;
            out = o;
        }
    }

    /**
     * Open OpenPGP encryption stream with default settings.
     * In this implementation, it means the following:
     * <ul>
     * <li><code>ZLIB</code> (a.k.a. deflate) compression</li>
     * <li><code>DESede</code> (a.k.a. 3DES, triple-DES) encryption</li>
     * </ul>
     * @param key symmetric encryption key
     * @param fname internal filename (e.g. <code>_CONSOLE</code> for FYEO)
     * @param output destination
     * @param rnd random source
     *
     * @see PassEncryptStream
     */
    public EncryptStream(Key key, String fname, OutputStream output, SecureRandom rnd) throws Exception {
        this(key, fname, output, rnd, 'b');
    }

    /**
     * Open OpenPGP encryption stream with default settings.
     * In this implementation, it means the following:
     * <ul>
     * <li><code>ZLIB</code> (a.k.a. deflate) compression</li>
     * <li><code>DESede</code> (a.k.a. 3DES, triple-DES) encryption</li>
     * </ul>
     * @param key symmetric encryption key
     * @param fname internal filename (e.g. <code>_CONSOLE</code> for FYEO)
     * @param output destination
     * @param rnd random source
     * @param mode <code>'t'</code> for text, <code>'b'</code> for binary
     *
     * @see PassEncryptStream
     */
    public EncryptStream(Key key, String fname, OutputStream output, SecureRandom rnd, char mode) throws Exception {
        this(key, fname, output, rnd, mode, null);
    }

    /**
     * Open OpenPGP encryption stream with default settings.
     * In this implementation, it means the following:
     * <ul>
     * <li><code>ZLIB</code> (a.k.a. deflate) compression</li>
     * <li><code>DESede</code> (a.k.a. 3DES, triple-DES) encryption</li>
     * </ul>
     * @param key symmetric encryption key
     * @param fname internal filename (e.g. <code>_CONSOLE</code> for FYEO)
     * @param output destination
     * @param rnd random source
     * @param mode <code>'t'</code> for text, <code>'b'</code> for binary
     * @param signer
     *
     * @see PassEncryptStream
     */
    public EncryptStream(Key key, String fname, OutputStream output, SecureRandom rnd, char mode, Signer signer) throws Exception {
        super(output);
        byte[] salt = new byte[8];
        byte[] prefix = new byte[10];
        Cipher c = Cipher.getInstance("DESede/CFB/NoPadding");
        long time = new Date().getTime() / 1000;
        sig = signer;
        out.write(0xc0 | Packet.PROCRYPTED);
        out = new PacketOutputStream(out, 13);
        out.write(1);
        rnd.nextBytes(salt);
        System.arraycopy(salt, 0, prefix, 0, 8);
        System.arraycopy(salt, 6, prefix, 8, 2);
        md = MessageDigest.getInstance("SHA1");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[8]));
        out = new CipherOutputStream(out, c);
        out = new DigestOutputStream(out, md);
        out.write(prefix);
        out.write(0xc0 | Packet.COMPRESSED);
        out = pos = new PacketOutputStream(out, 13);
        out.write(2);
        out = new DeflaterOutputStream(out);
        if (sig != null) sig.writePreamble(out);
        out.write(0xc0 | Packet.LITERAL);
        out = sos = new PacketOutputStream(out, 13);
        out.write(mode);
        if (fname.length() > 255) fname = fname.substring(0, 255);
        out.write(fname.length());
        out.write(fname.getBytes());
        out.write(new Long((time >> 24) & 0xff).byteValue());
        out.write(new Long((time >> 16) & 0xff).byteValue());
        out.write(new Long((time >> 8) & 0xff).byteValue());
        out.write(new Long(time & 0xff).byteValue());
        if (sig != null) out = new SignerStream(sig, out);
    }

    public void close() throws IOException {
        OutputStream m = sos.getOutputStream(), o;
        sos.last = false;
        super.close();
        if (sig != null) sig.writeSignatures(m);
        o = pos.getOutputStream();
        pos.last = false;
        m.close();
        o.write(0xD3);
        o.write(md.getDigestLength());
        o.write(md.digest());
        o.close();
        o = null;
        m = null;
        out = null;
        pos = null;
        sos = null;
        md = null;
    }
}
