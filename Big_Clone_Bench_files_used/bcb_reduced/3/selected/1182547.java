package org.microsoft.security.ntlm.impl;

import org.microsoft.security.ntlm.NtlmAuthenticator;
import javax.crypto.Mac;
import java.security.MessageDigest;
import static org.microsoft.security.ntlm.NtlmAuthenticator.*;
import static org.microsoft.security.ntlm.impl.Algorithms.ByteArray;
import static org.microsoft.security.ntlm.impl.Algorithms.UNICODE_ENCODING;
import static org.microsoft.security.ntlm.impl.Algorithms.calculateHmacMD5;
import static org.microsoft.security.ntlm.impl.Algorithms.concat;
import static org.microsoft.security.ntlm.impl.Algorithms.createHmacMD5;
import static org.microsoft.security.ntlm.impl.Algorithms.createMD4;
import static org.microsoft.security.ntlm.impl.NtlmRoutines.Z;

/**
 * @author <a href="http://profiles.google.com/109977706462274286343">Veritatem Quaeres</a>
 * @version $Revision: $
 */
public class NtlmV2Session extends NtlmSessionBase {

    private byte[] ntowfv2;

    public NtlmV2Session(ConnectionType connectionType, byte[] ntowfv2, WindowsVersion windowsVersion, String hostname, String domain, String username) {
        super(connectionType, windowsVersion, hostname, domain, username);
        this.ntowfv2 = ntowfv2;
    }

    private static final ByteArray ALL_RESPONSER_VERSION = new ByteArray(new byte[] { 1, 1 });

    @Override
    protected void calculateNTLMResponse(ByteArray time, byte[] clientChallengeArray, ByteArray targetInfo) {
        byte[] responseKeyNT = ntowfv2;
        byte[] responseKeyLM = ntowfv2;
        ByteArray clientChallenge = new ByteArray(clientChallengeArray);
        byte[] temp2 = concat(serverChallenge, ALL_RESPONSER_VERSION, Z(6), time, clientChallenge, Z(4), targetInfo, Z(4));
        ByteArray temp = new ByteArray(temp2, 8, temp2.length - serverChallenge.getLength());
        byte[] ntProofStr = calculateHmacMD5(responseKeyNT, temp2);
        ntChallengeResponse = concat(new ByteArray(ntProofStr), temp);
        lmChallengeResponse = concat(calculateHmacMD5(responseKeyLM, concat(serverChallenge, clientChallenge)), clientChallenge);
        sessionBaseKey = calculateHmacMD5(responseKeyNT, ntProofStr);
    }

    @Override
    public byte[] kxkey() {
        return sessionBaseKey;
    }

    public static byte[] calculateNTOWFv2(String domain, String username, String password) {
        try {
            MessageDigest md4 = createMD4();
            md4.update(password.getBytes(UNICODE_ENCODING));
            Mac hmacMD5 = createHmacMD5(md4.digest());
            hmacMD5.update(username.toUpperCase().getBytes(UNICODE_ENCODING));
            hmacMD5.update(domain.getBytes(UNICODE_ENCODING));
            return hmacMD5.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
