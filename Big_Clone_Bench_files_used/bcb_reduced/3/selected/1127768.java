package cryptix.sasl.otp;

import cryptix.sasl.IllegalMechanismStateException;
import cryptix.sasl.InputBuffer;
import cryptix.sasl.NoSuchUserException;
import cryptix.sasl.OutputBuffer;
import cryptix.sasl.SaslParams;
import cryptix.sasl.SaslUtil;
import cryptix.sasl.ServerMechanism;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.apache.log4j.Category;

/**
 * Implementation of the server-side of the SASL OTP protocol.
 *
 * @version $Revision: 1.7 $
 */
public class OTPServer extends ServerMechanism implements SaslServer, OTPParams, SaslParams {

    private static Category cat = Category.getInstance(OTPServer.class);

    /** The client's username. */
    private String username = null;

    private Map credentials;

    private byte[] lastHash;

    /** The OTP algorithm instance used by this server. */
    private OTP otp;

    public OTPServer(String protocol, String serverName, Map props, CallbackHandler cbh) {
        super(OTP_MECHANISM, protocol, serverName, props, cbh);
    }

    public byte[] evaluateResponse(byte[] response) throws SaslException {
        switch(state) {
            case 0:
                state++;
                return sendChallenge(response);
            case 1:
                state++;
                return sendFinalResponse(response);
            default:
                throw new IllegalMechanismStateException("evaluateResponse()");
        }
    }

    public void dispose() throws SaslException {
        credentials = null;
        lastHash = null;
        authenticator.passivate();
    }

    private byte[] sendChallenge(byte[] input) throws SaslException {
        cat.debug("==> sendChallenge()");
        cat.debug("response: " + SaslUtil.dumpString(input));
        InputBuffer frameIn = new InputBuffer(input);
        String identity;
        try {
            identity = frameIn.getText();
            cat.debug("Got identity: " + identity);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendChallenge()", x);
        }
        int index = identity.indexOf(0);
        this.username = identity.substring(0, index);
        authorizationID = identity.substring(index + 1);
        authenticator.activate(properties);
        try {
            Map userID = new HashMap();
            userID.put(USERNAME, username);
            credentials = authenticator.lookup(userID);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendChallenge()", x);
        }
        String algorithm = (String) credentials.get(ALGORITHM_FIELD);
        String seqnum = (String) credentials.get(SEGNUM_FIELD);
        String seed = (String) credentials.get(SEED_FIELD);
        String lastHashStr = (String) credentials.get(LAST_HASH_FIELD);
        otp = OTP.instance(algorithm);
        this.lastHash = SaslUtil.fromb64(lastHashStr);
        StringBuffer challenge = new StringBuffer(algorithm.length() + 1 + seqnum.length() + 1 + seed.length() + 1 + EXT.length() + 1 + WORD.length() + 1 + HEX.length() + 1 + INIT_WORD.length() + 1 + INIT_HEX.length());
        challenge.append(algorithm);
        challenge.append(SPACE);
        challenge.append(seqnum);
        challenge.append(SPACE);
        challenge.append(seed.toLowerCase());
        challenge.append(SPACE);
        challenge.append(EXT);
        challenge.append(COMMA);
        challenge.append(WORD);
        challenge.append(COMMA);
        challenge.append(HEX);
        challenge.append(COMMA);
        challenge.append(INIT_WORD);
        challenge.append(COMMA);
        challenge.append(INIT_HEX);
        OTPOutputBuffer frameOut = new OTPOutputBuffer();
        try {
            frameOut.setText(challenge.toString());
            cat.debug("Encoding challenge: " + challenge.toString());
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendChallenge()", x);
        }
        byte[] output = frameOut.encode();
        cat.debug("<== sendChallenge() --> " + SaslUtil.dumpString(output));
        return output;
    }

    private byte[] sendFinalResponse(byte[] input) throws SaslException {
        cat.debug("==> sendFinalResponse()");
        InputBuffer frameIn = new InputBuffer(input);
        String response;
        try {
            response = frameIn.getText();
            cat.debug("Response: " + response);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendFinalResponse()", x);
        }
        if (response.startsWith(INIT_HEX + COLON) || response.startsWith(INIT_WORD + COLON)) validateInitResponse(response); else if (response.startsWith(HEX + COLON) || response.startsWith(WORD + COLON)) validateResponse(response); else throw new SaslException("sendFinalResponse: Unknown format");
        OutputBuffer frameOut = new OutputBuffer();
        byte[] result = frameOut.encode();
        cat.debug("<== sendFinalResponse() --> " + SaslUtil.dumpString(result));
        this.complete = true;
        return result;
    }

    private void validateResponse(String response) throws SaslException {
        byte[] responseHash;
        if (response.startsWith(WORD + COLON)) {
            response = response.substring(WORD.length());
            long l = OTPDictionary.convertWordsToHash(response);
            responseHash = OTPUtil.convertHexToBytes(response);
            cat.debug("responseHash: " + responseHash);
        } else if (response.startsWith(HEX + COLON)) {
            response = response.substring(HEX.length());
            responseHash = OTPUtil.convertHexToBytes(response);
            cat.debug("responseHash: " + responseHash);
        } else throw new SaslException("Unknown format");
        byte[] hashResponseHash = otp.digest(responseHash);
        cat.debug("hash(responseHash): " + SaslUtil.dumpString(hashResponseHash));
        cat.debug("lastHash          : " + SaslUtil.dumpString(lastHash));
        if (!SaslUtil.areEqual(this.lastHash, hashResponseHash)) throw new SaslException("validateResponse: " + ERR_AUTH_FAILURE);
        try {
            String sequence = (String) credentials.get(SEGNUM_FIELD);
            int seqnum = Integer.parseInt(sequence);
            seqnum--;
            credentials.put(SEGNUM_FIELD, new Integer(seqnum).toString());
            credentials.put(LAST_HASH_FIELD, SaslUtil.tob64(responseHash));
            authenticator.update(credentials);
        } catch (Exception x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("validateResponse()", x);
        }
    }

    private void validateInitResponse(String response) throws SaslException {
        StringTokenizer st = new StringTokenizer(response, COLON);
        if (st.countTokens() != 4) throw new SaslException("validateInitResponse: " + ERR_INVALID_RESPONSE);
        String format = st.nextToken();
        String curOTP = st.nextToken();
        String newParams = st.nextToken();
        String newOTP = st.nextToken();
        st = new StringTokenizer(newParams);
        if (st.countTokens() != 3) throw new SaslException("validateInitResponse: " + ERR_INVALID_RESPONSE);
        String newAlgorithm = st.nextToken();
        String newSequence = st.nextToken();
        String newSeed = st.nextToken();
        OTP.validateAlgorithm(newAlgorithm);
        OTP.validateSeed(newSeed);
        OTP.validateSequenceNumber(newSequence);
        if (format.equalsIgnoreCase(INIT_WORD)) validateResponse(WORD + COLON + curOTP); else if (format.equalsIgnoreCase(INIT_HEX)) validateResponse(HEX + COLON + curOTP); else throw new SaslException("validateInitResponse: " + ERR_UNKNOWN_COMMAND);
        try {
            credentials.put(ALGORITHM_FIELD, newAlgorithm);
            credentials.put(SEGNUM_FIELD, newSequence);
            credentials.put(SEED_FIELD, newSeed);
            credentials.put(LAST_HASH_FIELD, newOTP);
            authenticator.update(credentials);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("validateInitResponse()", x);
        }
    }
}
