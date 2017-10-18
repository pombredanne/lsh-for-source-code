package fr.x9c.cadmium.primitives.stdlib;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * Implements all primitives from 'md5.c'.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Md5 {

    /** Identifier for digest algorithm. */
    private static final String ALGO = "MD5";

    /** Internal buffer size. */
    private static final int BUFFER_SIZE = 4096;

    /**
     * No instance of this class.
     */
    private Md5() {
    }

    /**
     * Computes the digest of a substring.
     * @param ctxt context
     * @param str string
     * @param ofs starting offset of substring
     * @param len lenght of substring
     * @return digest, as a 16-character string
     * @throws Fail.Exception if MD5 algorithm is unavailable
     */
    @Primitive
    public static Value caml_md5_string(final CodeRunner ctxt, final Value str, final Value ofs, final Value len) throws Fail.Exception {
        try {
            final MessageDigest md5 = MessageDigest.getInstance(Md5.ALGO);
            md5.update(str.asBlock().getBytes(), ofs.asLong(), len.asLong());
            return Value.createFromBlock(Block.createString(md5.digest()));
        } catch (final NoSuchAlgorithmException nsae) {
            Fail.invalidArgument("Digest.substring");
            return Value.UNIT;
        }
    }

    /**
     * Computes the digest of a channel.
     * @param ctxt context
     * @param channel input channel
     * @param len number of bytes to read from the channel to compute digest
     *        <br/>
     *        if negative then bytes are read until end of file
     * @return digest, as a 16-character string
     * @throws Fail.Exception if MD5 algorithm is unavailable
     */
    @Primitive
    public static Value caml_md5_chan(final CodeRunner ctxt, final Value channel, final Value len) throws Fail.Exception, FalseExit {
        try {
            final byte[] buffer = new byte[Md5.BUFFER_SIZE];
            final Channel ch = (Channel) channel.asBlock().asCustom();
            final MessageDigest md5 = MessageDigest.getInstance(Md5.ALGO);
            final int l = len.asLong();
            if (l < 0) {
                int nb = ch.read(buffer, 0, Md5.BUFFER_SIZE);
                while (nb != -1) {
                    md5.update(buffer, 0, nb);
                    nb = ch.read(buffer, 0, Md5.BUFFER_SIZE);
                }
            } else {
                int rem = l;
                while (rem > 0) {
                    final int nb = ch.read(buffer, 0, Math.min(buffer.length, rem));
                    if (nb == -1) {
                        Fail.raiseEndOfFile();
                    }
                    md5.update(buffer, 0, nb);
                    rem -= nb;
                }
            }
            return Value.createFromBlock(Block.createString(md5.digest()));
        } catch (final NoSuchAlgorithmException nsae) {
            Fail.invalidArgument("Digest.substring");
            return Value.UNIT;
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Sys.sysError(null, ioe.toString());
            return Value.UNIT;
        }
    }
}
