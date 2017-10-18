package gnu.javax.crypto.key.srp6;

import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.util.Util;
import gnu.javax.crypto.key.KeyAgreementException;
import gnu.javax.crypto.key.IncomingMessage;
import gnu.javax.crypto.key.OutgoingMessage;
import java.math.BigInteger;

/**
 * A variation of the SRP-6 protocol as used in the SASL-SRP mechanism, for the
 * Host (server side).
 * <p>
 * In this alternative, the exchange goes as follows:
 * 
 * <pre>
 *     C -&gt; S:  I                      (identifies self)
 *     S -&gt; C:  N, g, s, B = 3v + g&circ;b  (sends salt, b = random number)
 *     C -&gt; S:  A = g&circ;a                (a = random number)
 * </pre>
 * 
 * <p>
 * All elements are computed the same way as in the standard version.
 * <p>
 * Reference:
 * <ol>
 * <li><a
 * href="http://www.ietf.org/internet-drafts/draft-burdis-cat-srp-sasl-09.txt">
 * Secure Remote Password Authentication Mechanism</a><br>
 * K. Burdis, R. Naffah.</li>
 * <li><a href="http://srp.stanford.edu/design.html">SRP Protocol Design</a><br>
 * Thomas J. Wu.</li>
 * </ol>
 */
public class SRP6SaslServer extends SRP6TLSServer {

    protected OutgoingMessage computeSharedSecret(final IncomingMessage in) throws KeyAgreementException {
        super.computeSharedSecret(in);
        final byte[] sBytes = Util.trim(K);
        final IMessageDigest hash = srp.newDigest();
        hash.update(sBytes, 0, sBytes.length);
        K = new BigInteger(1, hash.digest());
        return null;
    }
}
