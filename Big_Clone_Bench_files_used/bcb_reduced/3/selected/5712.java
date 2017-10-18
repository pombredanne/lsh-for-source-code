package cryptix.sasl.sm2;

import cryptix.sasl.ClientFactory;
import cryptix.sasl.ClientMechanism;
import cryptix.sasl.IllegalMechanismStateException;
import cryptix.sasl.InputBuffer;
import cryptix.sasl.NoSuchMechanismException;
import cryptix.sasl.OutputBuffer;
import cryptix.sasl.SaslParams;
import cryptix.sasl.SaslUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientExt;
import javax.security.sasl.SaslException;
import org.apache.log4j.Category;

/**
 * The client-side implementation of the SASL SM2 mechanism.
 *
 * @version $Revision: 1.7 $
 * @since draft-naffah-cat-sasl-sm2-00
 */
public class SM2Client extends ClientMechanism implements SaslClient, SM2Params, SaslParams {

    private static Category cat = Category.getInstance(SM2Client.class);

    /** The reference to the Client Factory. */
    private static ClientFactory factory = new ClientFactory();

    /** A hash algorithm to help compute instance UIDs. */
    private static MessageDigest sha;

    /** The name of the underlying mechanism for this object. */
    private String umn;

    /** The unique identifier for this instance. use to locate the sid. */
    private String uid;

    /**
    * The client's evidence in case of a session re-use; cached to test with
    * server's evidence to detect concurrent modification(s).
    */
    private byte[] Ec;

    /** The underlying mechanism peer. */
    private SaslClientExt spi;

    public SM2Client(String umn, String authorizationID, String protocol, String serverName, Map props, CallbackHandler handler) {
        super(SM2_MECHANISM + "-" + umn, authorizationID, protocol, serverName, props, handler);
        this.umn = umn;
        complete = false;
        state = 0;
        if (sha == null) try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException x) {
            cat.error("SM2Client()", x);
            throw new RuntimeException(String.valueOf(x));
        }
        sha.update(String.valueOf(umn).getBytes());
        sha.update(String.valueOf(authorizationID).getBytes());
        sha.update(String.valueOf(protocol).getBytes());
        sha.update(String.valueOf(serverName).getBytes());
        sha.update(String.valueOf(properties).getBytes());
        sha.update(String.valueOf(Thread.currentThread().getName()).getBytes());
        uid = new BigInteger(1, sha.digest()).toString(26);
        Ec = null;
    }

    public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
        cat.debug("==> evaluateChallenge()");
        cat.debug("challenge: " + SaslUtil.dumpString(challenge));
        cat.debug("state: " + String.valueOf(state));
        if (complete) {
            cat.error("SPI Authentication phase completed");
            throw new IllegalMechanismStateException("evaluateChallenge()");
        }
        String sid = null;
        byte[] result = null;
        InputBuffer frameIn;
        switch(state) {
            case 0:
                spi = newSpiInstance();
                sid = SM2ClientStore.instance().getSessionID(uid);
                if (sid == null) {
                    result = newSession();
                    state = 1;
                } else {
                    result = reuseSession(sid);
                    state = 10;
                }
                cat.debug("<== evaluateChallenge()");
                return result;
            case 1:
                if (!spi.isComplete()) {
                    result = spi.evaluateChallenge(challenge);
                    if (result != null) return result; else if (!spi.isComplete()) throw new SaslException("Challenge is null yet underlying client" + " is incomplete");
                }
            case 2:
                complete = true;
                int ttl;
                frameIn = new InputBuffer(challenge);
                try {
                    sid = frameIn.getText();
                    cat.debug("sid: " + String.valueOf(sid));
                    ttl = frameIn.getScalar(4);
                    cat.debug("ttl: " + String.valueOf(ttl & 0xFFFFFFFFL));
                } catch (IOException x) {
                    cat.error("evaluateChallenge()", x);
                    if (x instanceof SaslException) throw (SaslException) x;
                    throw new SaslException("While at end of establishing new session", x);
                }
                SM2ClientStore.instance().cacheSession(uid, sid, ttl, spi);
                state = Integer.MAX_VALUE;
                cat.debug("<== evaluateChallenge()");
                return null;
            case 10:
                int ack;
                sid = SM2ClientStore.instance().getSessionID(uid);
                frameIn = new InputBuffer(challenge);
                try {
                    ack = frameIn.getScalar(1);
                    cat.debug("Server's command: " + String.valueOf(ack));
                } catch (IOException x) {
                    if (x instanceof SaslException) throw (SaslException) x;
                    throw new SaslException("While at end of re-using session " + String.valueOf(sid), x);
                }
                if (ack == USE_SESSION_ACK) {
                    byte[] Es = null;
                    try {
                        Es = frameIn.getOS();
                        cat.debug("Es: " + SaslUtil.dumpString(Es));
                    } catch (IOException x) {
                        cat.error(x);
                        throw new SaslException("While processing a server <ACK>: " + String.valueOf(x));
                    }
                    cat.debug("Ec: " + SaslUtil.dumpString(Ec));
                    if (!SaslUtil.areEqual(Ec, Es)) {
                        SM2ClientStore.instance().invalidateSession(uid, sid);
                        throw new SM2SessionModifiedException(String.valueOf(sid));
                    }
                    complete = true;
                    state = Integer.MAX_VALUE;
                } else if (ack == USE_SESSION_NAK) {
                    SM2ClientStore.instance().invalidateSession(uid, sid);
                    sid = null;
                    spi = newSpiInstance();
                    result = newSession();
                    state = 1;
                } else {
                    String msg = "Illegal SM2 command from server";
                    cat.error(msg);
                    throw new SaslException(msg);
                }
                cat.debug("<== evaluateChallenge()");
                return result;
            default:
                throw new IllegalMechanismStateException(String.valueOf(state));
        }
    }

    public boolean hasInitialResponse() {
        return true;
    }

    protected byte[] engineUnwrap(byte[] incoming, int offset, int len) throws SaslException {
        return spi.unwrap(incoming, offset, len);
    }

    protected byte[] engineWrap(byte[] outgoing, int offset, int len) throws SaslException {
        return spi.wrap(outgoing, offset, len);
    }

    public Object getNegotiatedProperty(String propName) throws SaslException {
        if (!isComplete()) throw new IllegalMechanismStateException("getNegotiatedProperty()");
        return spi.getNegotiatedProperty(propName);
    }

    public void dispose() throws SaslException {
        spi.dispose();
    }

    private byte[] newSession() throws SaslException {
        cat.debug("==> newSession()");
        OutputBuffer frameOut = new OutputBuffer();
        byte[] umir;
        if (spi.hasInitialResponse()) umir = spi.evaluateChallenge(null); else umir = new byte[0];
        try {
            frameOut.setScalar(1, NEW_SESSION_COMMAND);
            frameOut.setEOS(umir);
        } catch (IOException x) {
            cat.error("newSession()", x);
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("newSession()", x);
        }
        Ec = null;
        byte[] result = frameOut.encode();
        cat.debug("<== newSession()");
        return result;
    }

    private byte[] reuseSession(String sid) throws SaslException {
        cat.debug("==> reuseSession(" + String.valueOf(sid) + ")");
        Ec = SM2ClientStore.instance().computeEvidence(sid, spi);
        cat.debug("Ec = " + SaslUtil.dumpString(Ec));
        OutputBuffer frameOut = new OutputBuffer();
        try {
            frameOut.setScalar(1, USE_SESSION_COMMAND);
            frameOut.setText(sid);
            frameOut.setOS(Ec);
        } catch (IOException x) {
            cat.error("reuseSession()", x);
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("reuseSession(" + String.valueOf(sid) + ")", x);
        }
        byte[] result = frameOut.encode();
        cat.debug("<== reuseSession()");
        return result;
    }

    private SaslClientExt newSpiInstance() throws SaslException {
        cat.debug("==> newSpiInstance()");
        SaslClientExt result = (SaslClientExt) factory.createSaslClient(new String[] { umn }, authorizationID, protocol, serverName, properties, handler);
        if (result == null) {
            NoSuchMechanismException x = new NoSuchMechanismException(umn);
            cat.error("newSpiInstance()", x);
            throw x;
        }
        Ec = null;
        cat.debug("<== newSpiInstance()");
        return result;
    }
}
