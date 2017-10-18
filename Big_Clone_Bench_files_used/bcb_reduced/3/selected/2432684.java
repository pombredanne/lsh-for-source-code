package net.sourceforge.stripes.authentication;

import org.apache.commons.codec.binary.Hex;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.util.Random;

/**
 * @author Stefan Arentz
 */
public class DigestAuthenticationResolution extends AbstractAuthenticationResolution {

    /**
     *
     */
    public DigestAuthenticationResolution(String realm) {
        super(realm);
    }

    /**
     * @see net.sourceforge.stripes.action.Resolution#execute(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("WWW-Authenticate", String.format("Digest realm=\"%s\", nonce=\"%s\", algorithm=\"MD5\", qop=\"auth\"", getRealm(), generateRandomNonce()));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     *
     */
    private String generateRandomNonce() throws Exception {
        Random random = new Random();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = new byte[4];
        for (int i = 0; i < 8; i++) {
            random.nextBytes(bytes);
            md.digest(bytes);
        }
        return new String(Hex.encodeHex(md.digest()));
    }
}
