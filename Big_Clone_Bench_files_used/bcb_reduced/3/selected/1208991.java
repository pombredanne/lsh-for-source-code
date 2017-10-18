package net.sourceforge.stripes.authentication;

import org.apache.commons.codec.binary.Hex;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class DigestCredentials {

    String method = null;

    String username = null;

    String realm = null;

    String nonce = null;

    String nc = null;

    String cnonce = null;

    String qop = null;

    String uri = null;

    String response = null;

    DigestCredentials(String method) {
        this.method = method;
    }

    public boolean check(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(username.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(realm.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(password.getBytes("ISO-8859-1"));
        byte[] ha1 = md.digest();
        String hexHa1 = new String(Hex.encodeHex(ha1));
        md.reset();
        md.update(method.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(uri.getBytes("ISO-8859-1"));
        byte[] ha2 = md.digest();
        String hexHa2 = new String(Hex.encodeHex(ha2));
        md.reset();
        md.update(hexHa1.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(nonce.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(nc.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(cnonce.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(qop.getBytes("ISO-8859-1"));
        md.update((byte) ':');
        md.update(hexHa2.getBytes("ISO-8859-1"));
        byte[] digest = md.digest();
        String hexDigest = new String(Hex.encodeHex(digest));
        return (hexDigest.equalsIgnoreCase(response));
    }
}
