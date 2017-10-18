package jrgp.util;

public class SUIDStream {

    private java.io.DataOutputStream stream;

    private java.security.MessageDigest md;

    public SUIDStream() throws java.security.NoSuchAlgorithmException {
        md = java.security.MessageDigest.getInstance("SHA");
        stream = new java.io.DataOutputStream(new java.security.DigestOutputStream(new java.io.ByteArrayOutputStream(512), md));
    }

    public void appendName(String name) throws java.io.IOException {
        stream.writeUTF(name);
    }

    public void appendInt(int v) throws java.io.IOException {
        stream.writeInt(v);
    }

    public long endReturnSUID() throws java.io.IOException {
        long h = 0;
        stream.flush();
        byte[] hash = md.digest();
        for (int i = 0; i < Math.min(hash.length, 8); i++) {
            int sh = i * 8;
            h += (long) (hash[i] & 255) << sh;
        }
        return h;
    }
}
