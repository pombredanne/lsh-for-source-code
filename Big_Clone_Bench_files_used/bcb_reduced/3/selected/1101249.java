package net.sourceforge.epoint.pgp;

import net.sourceforge.epoint.io.*;
import java.security.Key;
import java.security.MessageDigest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.IOException;

/**
 * OpenPGP Symmetric Key Encrypted and MDC protected data packet
 * 
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 * @see MDCPacket
 */
public final class PROCRYPTEDPacket extends CRYPTEDPacket {

    /**
     * Load the packet from an <code>InputStream</code>
     * @param in source
     */
    public PROCRYPTEDPacket(InputStream in) throws SyntaxException, IOException {
        super(in);
        if (getTag() != PROCRYPTED) throw new SyntaxException(TAG_ERROR + " " + Integer.toString(getTag()));
    }

    public PROCRYPTEDPacket(Packet p) throws SyntaxException, IOException {
        super(p);
        if (getTag() != PROCRYPTED) throw new SyntaxException(TAG_ERROR + " " + Integer.toString(getTag()));
    }

    /**
     * Decrypted input with a MDC at the end
     *
     * <p>It is a very ugly implementation, but it is necessary because some
     * versions of <a href="http://www.gnupg.org">GPG</a> use old-style
     * undefined length compressed packets before the MDC, which is a very
     * nasty thing to do, in my opinion, and goes somewhat against the
     * recommendations of RFC2440, but interoperability is king.</p>
     */
    private class MDCInputStream extends InputStream {

        private MessageDigest digest;

        private PushbackInputStream input;

        private byte[] mdc = new byte[22];

        public MDCInputStream(InputStream in, MessageDigest md) throws IOException {
            input = new PushbackInputStream(in, 23);
            int l = input.read(mdc);
            if (l < 22) throw new IOException("No MDC.");
            input.unread(mdc);
            digest = md;
        }

        public int read() throws IOException {
            int c = input.read();
            int l = input.read(mdc);
            if (l < 22) {
                input.unread(mdc, 0, l);
                input.unread(c);
                return -1;
            }
            input.unread(mdc);
            digest.update((byte) c);
            return c;
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            int c = input.read(buf, off, len);
            if (c < len) {
                input.unread(buf, off + c - 22, 22);
                digest.update(buf, off, c - 22);
                return c - 22;
            }
            int l = input.read(mdc);
            if (l < 22) {
                input.unread(mdc, 0, l);
                input.unread(buf, off + c + l - 22, 22 - l);
                digest.update(buf, off, c + l - 22);
                return c + l - 22;
            }
            input.unread(mdc);
            digest.update(buf, off, c);
            return c;
        }

        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        public void close() throws IOException {
            int tag = input.read();
            if (tag != 0xD3) throw new SyntaxException(Packet.TAG_ERROR + " 0x" + Integer.toHexString(tag & 0xFF));
            digest.update((byte) tag);
            digest.update((byte) input.read());
            byte[] calc = digest.digest();
            int i = calc.length;
            for (i = 0; i < calc.length; i++) if (calc[i] != (byte) input.read()) throw new MDCException("Bad MDC");
            input.close();
            super.close();
        }
    }

    /**
     * Decrypted payload
     *
     * <p><strong>WARNING:</strong>
     * The quick check for symmetric key integrity constitutes a serious
     * vulnerability under certain circumstances, when using ePointPGP
     * in server applications.</p>
     * <p>In short, by repeated attempts at decrypting a number of
     * carefully chosen fake ciphertext messages purporting to have been
     * encrypted with the same symmetric key as the attacked message, the
     * attacker can gain enough information from the fact whether or not
     * it has passed the quick check to decrypt the first two bytes in
     * the next block (and then those in all subsequent blocks).</p>
     * <p>For a detailed analysis and explaination, please read
     * <a href="http://eprint.iacr.org/2005/033.pdf">An Attack on CFB Mode
     * Encryption As Used By OpenPGP</a> by <em>Serge Mister</em> and
     * <em>Robert Zuccherato</em>.</p>
     * <p>Please note that ePointPGP is intended primarily for client-side
     * applications (applets) and we do not recommend it for any server
     * application. On the client side, the quick check is still useful,
     * and cannot be in practice exploited in the above described way.</p>
     * 
     * @param k decryption key
     */
    public InputStream getDecryptStream(Key k) throws Exception {
        MessageDigest md;
        int version = integer(1);
        switch(version) {
            case 1:
                md = MessageDigest.getInstance("SHA1");
                break;
            default:
                throw new NoSuchAlgorithmException(ERROR_VERSION + version);
        }
        Cipher c = Cipher.getInstance(k.getAlgorithm() + "/CFB/NoPadding");
        int b = c.getBlockSize(), i;
        byte[] head;
        c.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(new byte[8]));
        InputStream is = new MDCInputStream(new CipherInputStream(getStream(), c), md);
        head = new byte[b + 2];
        i = is.read(head);
        if (i < b + 2) throw new IOException("Premature EoS");
        if ((head[b - 2] != head[b]) || (head[b - 1] != head[b + 1])) throw new InvalidKeyException("Wrong symmetric key");
        return is;
    }
}
