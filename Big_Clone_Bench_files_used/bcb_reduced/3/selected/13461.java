package sipinspector;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.Random;

/**
 *
 * @author Zarko Coklin
 */
public class MD5Digest {

    public MD5Digest(String authHdr, String method, String username, String password, String uri) {
        int start;
        int end;
        this.username = username;
        this.password = password;
        this.method = method;
        this.uri = uri;
        Random rand = new Random();
        this.cnonce = Long.toHexString(rand.nextLong());
        start = authHdr.indexOf("realm=\"");
        if (start == -1) {
            realm = "";
        } else {
            start += 7;
            end = authHdr.indexOf('"', start);
            realm = authHdr.substring(start, end);
        }
        start = authHdr.indexOf("nonce=\"");
        if (start == -1) {
            nonce = "";
        } else {
            start += 7;
            end = authHdr.indexOf('"', start);
            nonce = authHdr.substring(start, end);
        }
        start = authHdr.indexOf("opaque=\"");
        if (start == -1) {
            opaque = "";
        } else {
            start += 8;
            end = authHdr.indexOf('"', start);
            opaque = authHdr.substring(start, end);
        }
        start = authHdr.indexOf("qop=");
        if (start == -1) {
            qop = "";
            return;
        } else {
            end = authHdr.indexOf('"', start + 5);
            if (end == -1) {
                end = authHdr.indexOf(' ', start + 5);
                if (end == -1) {
                    end = authHdr.indexOf(',', start + 5);
                    if (end == -1) {
                        qop = authHdr.substring(start + 4);
                        qop = qop.replace("\"", "");
                        return;
                    }
                }
            }
            qop = authHdr.substring(start, end);
            qop = qop.replace("\"", "");
        }
    }

    public String calculateMD5DigestResponse() {
        String firstStr = new String(username + ":" + realm + ":" + password);
        String secondStr = new String(method + ":" + uri);
        String firstStrHashed = calculateMD5(firstStr);
        String secondStrHashed = calculateMD5(secondStr);
        if (qop.equals("auth") == true) {
            return calculateMD5(firstStrHashed + ":" + nonce + ":00000001:" + getCnonce() + ":auth:" + secondStrHashed);
        } else {
            return calculateMD5(firstStrHashed + ":" + nonce + ":" + secondStrHashed);
        }
    }

    public String getRealm() {
        return realm;
    }

    public String getNonce() {
        return nonce;
    }

    public String getOpaque() {
        return opaque;
    }

    public String getQop() {
        return qop;
    }

    public String getCnonce() {
        return cnonce;
    }

    private String calculateMD5(String value) {
        String finalString = new String("");
        try {
            MessageDigest md5Alg = MessageDigest.getInstance("MD5");
            md5Alg.reset();
            md5Alg.update(value.getBytes());
            byte messageDigest[] = md5Alg.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            finalString = hexString.toString();
        } catch (NoSuchAlgorithmException exc) {
            throw new RuntimeException("Hashing error happened:", exc);
        }
        return finalString;
    }

    private String username;

    private String password;

    private String realm;

    private String method;

    private String uri;

    private String nonce;

    private String cnonce;

    private String opaque;

    private String qop;
}
