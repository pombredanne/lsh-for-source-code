package org.nfcsigning.algorithm;

import java.security.DigestException;
import java.security.MessageDigest;
import org.bouncycastle.crypto.Digest;

/**
 *
 * @author Markus Kilås
 */
public class SatsaSha1Digest implements Digest {

    private MessageDigest md;

    public SatsaSha1Digest(MessageDigest messageDigest) {
        this.md = messageDigest;
    }

    public String getAlgorithmName() {
        return "SHA-1";
    }

    public int getDigestSize() {
        return 20;
    }

    public void update(byte in) {
        md.update(new byte[] { in }, 0, 1);
    }

    public void update(byte[] in, int inOff, int len) {
        md.update(in, inOff, len);
    }

    public int doFinal(byte[] out, int outOff) {
        try {
            return md.digest(out, outOff, 20);
        } catch (DigestException ex) {
            return 0;
        }
    }

    public void reset() {
        md.reset();
    }
}
