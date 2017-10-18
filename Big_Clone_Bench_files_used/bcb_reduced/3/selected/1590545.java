package gnu.javax.net.ssl.provider;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.sig.ISignature;
import gnu.java.security.sig.rsa.RSA;

/**
 * The RSA signature algorithm as used in the SSL protocol. Note that this
 * is different from the RSA signature used to verify certificates.
 *
 * <p>This signature scheme works as follows:</p>
 *
 * <blockquote><p><pre>digitally-signed struct {
 *     opaque md5_hash[16];
 *     opaque sha_hash[20];
 * }</pre></p></blockquote>
 *
 * <p>Where a <code>digitally-signed struct</code> is RSA-encrypted with
 * block type 0 or 1 according to PKCS #1, version 1.5.</p>
 */
final class SSLRSASignature implements ISignature {

    private RSAPublicKey pubkey;

    private RSAPrivateKey privkey;

    private final IMessageDigest md5, sha;

    private boolean initVerify = false, initSign = false;

    SSLRSASignature() {
        this(HashFactory.getInstance("MD5"), HashFactory.getInstance("SHA-1"));
    }

    SSLRSASignature(IMessageDigest md5, IMessageDigest sha) {
        this.md5 = md5;
        this.sha = sha;
    }

    public String name() {
        return "RSA/SSL";
    }

    public void setupVerify(Map attrib) {
        PublicKey key = (PublicKey) attrib.get(VERIFIER_KEY);
        if (key == null) {
            if (initSign) {
                return;
            }
            throw new IllegalArgumentException("no key supplied");
        }
        if (!(key instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("not an RSA key");
        }
        pubkey = (RSAPublicKey) key;
        privkey = null;
        initSign = false;
        initVerify = true;
    }

    public void setupSign(Map attrib) {
        PrivateKey key = (PrivateKey) attrib.get(SIGNER_KEY);
        if (key == null) {
            if (initVerify) {
                return;
            }
            throw new IllegalArgumentException("no key supplied");
        }
        if (!(key instanceof RSAPrivateKey)) {
            throw new IllegalArgumentException("not an RSA key");
        }
        privkey = (RSAPrivateKey) key;
        pubkey = null;
        initVerify = false;
        initSign = true;
    }

    public void update(byte b) {
        if (!initVerify && !initSign) {
            throw new IllegalStateException();
        }
        md5.update(b);
        sha.update(b);
    }

    public void update(byte[] buf, int off, int len) {
        if (!initVerify && !initSign) {
            throw new IllegalStateException();
        }
        md5.update(buf, off, len);
        sha.update(buf, off, len);
    }

    public Object sign() {
        if (!initSign) {
            throw new IllegalStateException();
        }
        final int k = (privkey.getModulus().bitLength() + 7) >>> 3;
        final byte[] d = Util.concat(md5.digest(), sha.digest());
        if (k - 11 < d.length) {
            throw new IllegalArgumentException("message too long");
        }
        final byte[] eb = new byte[k];
        eb[0] = 0x00;
        eb[1] = 0x01;
        for (int i = 2; i < k - d.length - 1; i++) {
            eb[i] = (byte) 0xFF;
        }
        System.arraycopy(d, 0, eb, k - d.length, d.length);
        BigInteger EB = new BigInteger(eb);
        BigInteger EM = RSA.sign(privkey, EB);
        return Util.trim(EM);
    }

    public boolean verify(Object signature) {
        if (!initVerify) {
            throw new IllegalStateException();
        }
        BigInteger EM = new BigInteger(1, (byte[]) signature);
        BigInteger EB = RSA.verify(pubkey, EM);
        int i = 0;
        final byte[] eb = EB.toByteArray();
        if (eb[0] == 0x00) {
            for (i = 0; i < eb.length && eb[i] == 0x00; i++) ;
        } else if (eb[0] == 0x01) {
            for (i = 1; i < eb.length && eb[i] != 0x00; i++) {
                if (eb[i] != (byte) 0xFF) {
                    throw new IllegalArgumentException("bad padding");
                }
            }
            i++;
        } else {
            throw new IllegalArgumentException("decryption failed");
        }
        byte[] d1 = Util.trim(eb, i, eb.length - i);
        byte[] d2 = Util.concat(md5.digest(), sha.digest());
        return Arrays.equals(d1, d2);
    }

    public Object clone() {
        throw new UnsupportedOperationException();
    }
}
