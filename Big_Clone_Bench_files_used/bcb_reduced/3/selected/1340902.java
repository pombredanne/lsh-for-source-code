package up5.mi.visio.proxy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import org.apache.log4j.Logger;
import up5.mi.visio.sipdb.DBFactory;
import up5.mi.visio.sipdb.UnknowDBException;
import up5.mi.visio.sipdb.accounting.UserCredential;
import up5.mi.visio.sipdb.sip.ChallengeBean;
import up5.mi.visio.sipdb.sip.ChallengeNotFoundException;

/**
 *  This class creates and manages challenge.
 *  It also provides for authentication checks features.
 */
public class SecurityManager {

    private final Logger logger = Logger.getLogger(SecurityManager.class.getName());

    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * check if authentication is required for this request.
	 * 
	 * @param request the request to check.
	 * @param hosts which are trusted.
	 * @return true if authentication is required.
	 */
    public boolean isAuthRequired(SipServletRequest request, List<String> hosts) {
        for (String host : hosts) {
            if (request.getRemoteAddr().equals(host)) {
                return false;
            }
        }
        if (request.getMethod().equals("REGISTER") || request.getMethod().equals("INVITE")) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * return the stored challenge bean of a dialog.
	 * 
	 * @param CallId of the dialog.
	 * @return the stored challenge bean.
	 */
    public ChallengeBean getChallenge(String CallId) throws ChallengeNotFoundException, UnknowDBException {
        return DBFactory.getAuthModule().getChallenge(CallId);
    }

    /**
	 * Create a challenge for a dialog and store it.
	 * 
	 * @param callId the call id of the dialog.
	 * @return the generated challenge bean.
	 */
    public ChallengeBean createChallenge(String callId, int expires) throws UnknowDBException {
        ChallengeBean challenge = new ChallengeBean();
        challenge.setNonce(this.generateNonce());
        challenge.setOpaque(this.generateOpaque());
        challenge.setScheme("Digest");
        challenge.setAlgorythm("MD5");
        challenge.setQop("auth");
        challenge.setNc("00000001");
        challenge.setStale("FALSE");
        DBFactory.getAuthModule().registerChallenge(callId, challenge, expires);
        return challenge;
    }

    /**
	 * Create the response header and populate it with the challenge and user parameter
	 * 
	 * @param rep : the response to send to the client user agent.
	 */
    public void addChallengeToResponse(SipServletResponse rep, ChallengeBean challenge, UserCredential credential) {
        String authHeader = "WWW-Authenticate : ";
        String authValue = (challenge.getScheme() + " " + "realm=\"" + credential.getRealm() + "\"," + "qop=" + "\"" + challenge.getQop() + "\"," + "nonce=" + "\"" + challenge.getNonce() + "\"," + "opaque=" + challenge.getOpaque() + "," + "stale=" + challenge.getStale() + "," + "algorithm=" + challenge.getAlgorythm() + "," + "nc=" + challenge.getNc());
        logger.debug("generated challenge :" + authValue);
        rep.addHeader(authHeader, authValue);
    }

    /**
	 * Retrieve response parameters from a header and convert it to a bean. 
	 * 
	 * @param authHeader the header to parse.
	 * @return the response bean.
	 */
    public ChallengeResponseBean getChallengeResponseBean(String authHeader) {
        ChallengeResponseBean response = new ChallengeResponseBean();
        String params[] = authHeader.split(" ");
        params = params[1].split(",");
        for (int i = 0; i < params.length; i++) {
            if (params[i].contains("uri=")) {
                response.setUri(params[i].substring(4).replace("\"", ""));
            } else {
                String param[] = params[i].split("=");
                if (param[0].equals("username")) {
                    response.setPrivateUser(param[1].replace("\"", ""));
                } else if (param[0].equals("cnonce")) {
                    response.setCnonce(param[1].replace("\"", ""));
                } else if (param[0].equals("response")) {
                    response.setResponse(param[1].replace("\"", ""));
                } else if (param[0].equals("nc")) {
                    response.setNc(param[1].replace("\"", ""));
                }
            }
        }
        return response;
    }

    /**
	 * generate a random nonce in order to be use in a challenge.
	 * Use the time of the day as basis.
	 * 
	 * @return the generated nonce String.
	 */
    public String generateNonce() {
        Date date = new Date();
        long time = date.getTime();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = (new Long(time)).toString() + (new Long(pad)).toString();
        return this.crypt(nonceString);
    }

    /**
	 * generate opaque parameter in order to be used in a challenge.
	 * Use the time of the day as basis.
	 * 
	 * @return the generated opaque String.
	 */
    public String generateOpaque() {
        Date date = new Date();
        long time = date.getTime();
        Random rand = new Random();
        long pad = rand.nextLong();
        String opaqueString = (new Long(time)).toString() + (new Long(pad)).toString();
        return this.crypt(opaqueString);
    }

    /**
	 * Check if the response is correct.
	 * Validate the presence of needed parameters.
	 * Calculate the waiting response then check if the response match it.
	 * 
	 * @param authHeader the response header content.
	 * @param request the of the challenge.
	 * @param credential the user credential information.
	 * @param challenge the challenge bean used for this dialog.
	 * 
	 * @return true if the response is correct.
	 */
    public boolean checkResponse(String authHeader, SipServletRequest request, UserCredential credential, ChallengeBean challenge) {
        ChallengeResponseBean responseBean = this.getChallengeResponseBean(authHeader);
        if (responseBean.getPrivateUser() == null) {
            logger.debug("No private user in authorization header");
            return false;
        }
        if (responseBean.getCnonce() == null) {
            logger.debug("No cnonce in authorization header");
            return false;
        }
        if (responseBean.getUri() == null) {
            logger.debug("No uri in authorization header");
            return false;
        }
        if (responseBean.getResponse() == null) {
            logger.debug("No response in authorization header");
            return false;
        }
        String A1 = responseBean.getPrivateUser() + ":" + credential.getRealm() + ":" + credential.getPasswd();
        logger.debug("A1: " + A1);
        String A2 = request.getMethod().toUpperCase() + ":" + responseBean.getUri();
        logger.debug("A2: " + A2);
        String HA1 = this.crypt(A1);
        logger.debug("HA1: " + HA1);
        String HA2 = this.crypt(A2);
        logger.debug("HA2: " + HA2);
        String KD = HA1 + ":" + challenge.getNonce() + ":" + responseBean.getNc();
        KD += ":" + responseBean.getCnonce();
        KD += ":" + challenge.getQop() + ":" + HA2;
        logger.debug("KD: " + KD);
        String mdString = this.crypt(KD);
        logger.debug("waiting for: " + mdString);
        return (mdString.equals(responseBean.getResponse()));
    }

    /**
	 * hash the key with MD5.
	 * 
	 * @param key the String to Hash.
	 * @return the hashed string.
	 */
    public String crypt(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(key.getBytes());
            return toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Erreur");
        }
    }

    /**
	 * Convert a String to an hex only char.
	 * 
	 * @param b the String to modify.
	 * @return the hex String.
	 */
    private static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }
}
