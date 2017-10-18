package freenet.crypt;

import java.security.*;

class JavaSHA1 implements Digest {

    MessageDigest digest;

    public Object clone() throws CloneNotSupportedException {
        return new JavaSHA1((MessageDigest) (digest.clone()));
    }

    protected JavaSHA1(MessageDigest d) {
        digest = d;
    }

    public JavaSHA1() throws Exception {
        digest = MessageDigest.getInstance("SHA1");
    }

    public void extract(int[] digest, int offset) {
        throw new UnsupportedOperationException();
    }

    public void update(byte b) {
        digest.update(b);
    }

    public void update(byte[] data, int offset, int length) {
        digest.update(data, offset, length);
    }

    public void update(byte[] data) {
        digest.update(data);
    }

    public byte[] digest() {
        return digest.digest();
    }

    public void digest(boolean reset, byte[] buffer, int offset) {
        if (reset != true) throw new UnsupportedOperationException();
        try {
            digest.digest(buffer, offset, digest.getDigestLength());
        } catch (DigestException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    public int digestSize() {
        return 160;
    }
}
