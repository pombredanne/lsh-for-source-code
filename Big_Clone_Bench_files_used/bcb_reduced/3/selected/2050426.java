package org.apache.zookeeper.server.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.ServerCnxn;

public class DigestAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DigestAuthenticationProvider.class);

    /** specify a command line property with key of 
     * "zookeeper.DigestAuthenticationProvider.superDigest"
     * and value of "super:<base64encoded(SHA1(password))>" to enable
     * super user access (i.e. acls disabled)
     */
    private static final String superDigest = System.getProperty("zookeeper.DigestAuthenticationProvider.superDigest");

    public String getScheme() {
        return "digest";
    }

    private static final String base64Encode(byte b[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; ) {
            int pad = 0;
            int v = (b[i++] & 0xff) << 16;
            if (i < b.length) {
                v |= (b[i++] & 0xff) << 8;
            } else {
                pad++;
            }
            if (i < b.length) {
                v |= (b[i++] & 0xff);
            } else {
                pad++;
            }
            sb.append(encode(v >> 18));
            sb.append(encode(v >> 12));
            if (pad < 2) {
                sb.append(encode(v >> 6));
            } else {
                sb.append('=');
            }
            if (pad < 1) {
                sb.append(encode(v));
            } else {
                sb.append('=');
            }
        }
        return sb.toString();
    }

    private static final char encode(int i) {
        i &= 0x3f;
        if (i < 26) {
            return (char) ('A' + i);
        }
        if (i < 52) {
            return (char) ('a' + i - 26);
        }
        if (i < 62) {
            return (char) ('0' + i - 52);
        }
        return i == 62 ? '+' : '/';
    }

    public static String generateDigest(String idPassword) throws NoSuchAlgorithmException {
        String parts[] = idPassword.split(":", 2);
        byte digest[] = MessageDigest.getInstance("SHA1").digest(idPassword.getBytes());
        return parts[0] + ":" + base64Encode(digest);
    }

    public KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
        String id = new String(authData);
        try {
            String digest = generateDigest(id);
            if (digest.equals(superDigest)) {
                cnxn.addAuthInfo(new Id("super", ""));
            }
            cnxn.addAuthInfo(new Id(getScheme(), digest));
            return KeeperException.Code.OK;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Missing algorithm", e);
        }
        return KeeperException.Code.AUTHFAILED;
    }

    public boolean isAuthenticated() {
        return true;
    }

    public boolean isValid(String id) {
        String parts[] = id.split(":");
        return parts.length == 2;
    }

    public boolean matches(String id, String aclExpr) {
        return id.equals(aclExpr);
    }

    /** Call with a single argument of user:pass to generate authdata.
     * Authdata output can be used when setting superDigest for example. 
     * @param args single argument of user:pass
     * @throws NoSuchAlgorithmException
     */
    public static void main(String args[]) throws NoSuchAlgorithmException {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i] + "->" + generateDigest(args[i]));
        }
    }
}
