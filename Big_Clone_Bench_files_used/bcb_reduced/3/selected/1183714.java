package gnu.javax.crypto.key.srp6;

import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.util.Util;
import gnu.javax.crypto.key.KeyAgreementException;
import gnu.javax.crypto.key.IncomingMessage;
import gnu.javax.crypto.key.OutgoingMessage;
import gnu.javax.crypto.sasl.srp.SRP;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the User in the SRP-6 protocol.
 * <p>
 * Reference:
 * <ol>
 * <li><a href="http://srp.stanford.edu/design.html">SRP Protocol Design</a><br>
 * Thomas J. Wu.</li>
 * </ol>
 */
public class SRP6User extends SRP6KeyAgreement {

    /** The user's identity. */
    private String I;

    /** The user's cleartext password. */
    private byte[] p;

    /** The user's ephemeral key pair. */
    private KeyPair userKeyPair;

    protected void engineInit(final Map attributes) throws KeyAgreementException {
        rnd = (SecureRandom) attributes.get(SOURCE_OF_RANDOMNESS);
        N = (BigInteger) attributes.get(SHARED_MODULUS);
        if (N == null) throw new KeyAgreementException("missing shared modulus");
        g = (BigInteger) attributes.get(GENERATOR);
        if (g == null) throw new KeyAgreementException("missing generator");
        final String md = (String) attributes.get(HASH_FUNCTION);
        if (md == null || md.trim().length() == 0) throw new KeyAgreementException("missing hash function");
        srp = SRP.instance(md);
        I = (String) attributes.get(USER_IDENTITY);
        if (I == null) throw new KeyAgreementException("missing user identity");
        p = (byte[]) attributes.get(USER_PASSWORD);
        if (p == null) throw new KeyAgreementException("missing user password");
    }

    protected OutgoingMessage engineProcessMessage(final IncomingMessage in) throws KeyAgreementException {
        switch(step) {
            case 0:
                return sendIdentity(in);
            case 1:
                return computeSharedSecret(in);
            default:
                throw new IllegalStateException("unexpected state");
        }
    }

    protected void engineReset() {
        I = null;
        p = null;
        userKeyPair = null;
        super.engineReset();
    }

    private OutgoingMessage sendIdentity(final IncomingMessage in) throws KeyAgreementException {
        final SRPKeyPairGenerator kpg = new SRPKeyPairGenerator();
        final Map attributes = new HashMap();
        if (rnd != null) attributes.put(SRPKeyPairGenerator.SOURCE_OF_RANDOMNESS, rnd);
        attributes.put(SRPKeyPairGenerator.SHARED_MODULUS, N);
        attributes.put(SRPKeyPairGenerator.GENERATOR, g);
        kpg.setup(attributes);
        userKeyPair = kpg.generate();
        final OutgoingMessage result = new OutgoingMessage();
        result.writeString(I);
        result.writeMPI(((SRPPublicKey) userKeyPair.getPublic()).getY());
        return result;
    }

    private OutgoingMessage computeSharedSecret(final IncomingMessage in) throws KeyAgreementException {
        final BigInteger s = in.readMPI();
        final BigInteger B = in.readMPI();
        final BigInteger A = ((SRPPublicKey) userKeyPair.getPublic()).getY();
        final BigInteger u = uValue(A, B);
        final BigInteger x;
        try {
            x = new BigInteger(1, srp.computeX(Util.trim(s), I, p));
        } catch (Exception e) {
            throw new KeyAgreementException("computeSharedSecret()", e);
        }
        final BigInteger a = ((SRPPrivateKey) userKeyPair.getPrivate()).getX();
        final BigInteger S = B.subtract(THREE.multiply(g.modPow(x, N))).modPow(a.add(u.multiply(x)), N);
        final byte[] sBytes = Util.trim(S);
        final IMessageDigest hash = srp.newDigest();
        hash.update(sBytes, 0, sBytes.length);
        K = new BigInteger(1, hash.digest());
        complete = true;
        return null;
    }
}
