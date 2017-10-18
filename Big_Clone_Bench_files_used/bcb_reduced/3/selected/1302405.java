package org.apache.geronimo.crypto;

import org.apache.geronimo.crypto.encoders.HexEncoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utility functions for dealing with X.509 certificates
 *
 * @version $Rev: 617737 $ $Date: 2008-02-02 02:39:58 +0100 (Sat, 02 Feb 2008) $
 */
public class CertificateUtil {

    public static String generateFingerprint(Certificate cert, String digestAlgorithm) throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
        byte[] digest = md.digest(cert.getEncoded());
        ByteArrayOutputStream out = new ByteArrayOutputStream(digest.length * 2);
        new HexEncoder().encode(digest, 0, digest.length, out);
        String all = new String(out.toByteArray(), "US-ASCII").toUpperCase();
        Matcher matcher = Pattern.compile("..").matcher(all);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            if (buf.length() > 0) {
                buf.append(":");
            }
            buf.append(matcher.group());
        }
        return buf.toString();
    }
}
