package org.tripcom.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.tripcom.security.exceptions.InternalFailureException;

/**
 * 
 * 
 * @author Francesco Corcoglioniti &lt;francesco.corcoglioniti@cefriel.it&gt;
 */
public final class Certificates {

    /** The certificate factory for X509v3 certificates. */
    private static CertificateFactory certificateFactory;

    static {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException ex) {
            throw new InternalFailureException("X509v3 certificates unsupported", ex);
        }
    }

    /**
     * Private constructor, avoid class instantiation.
     */
    private Certificates() {
    }

    /**
     * Load a X509 certificate from the classpath resource or the file
     * specified.The method throws a {@link InternalFailureException} in case of
     * error.
     * 
     * @param location the location of the resource available on the classpath
     *            (not null).
     * @return the certificate read from the resource.
     */
    public static X509Certificate load(String location) {
        InputStream stream = Resources.load(location);
        try {
            X509Certificate result = (X509Certificate) certificateFactory.generateCertificate(stream);
            stream.close();
            return result;
        } catch (Exception ex) {
            throw new InternalFailureException("Unable to load certificate", ex);
        }
    }

    /**
     * Return a reference to a static certificate factory for X509 certificates.
     * 
     * @return a reference to a static X509 certificate factory.
     */
    public static CertificateFactory getFactory() {
        return certificateFactory;
    }

    /**
     * Encode the specified certificate as a PEM string. This method accepts an
     * X509Certificate object and produces its PEM representation. PEM is a
     * textual format consisting of a DER binary certificate encoded through
     * Base64 and represented in an ASCII armored format, using the
     * <tt>-----BEGIN
     * CERTIFICATE-----</tt> and
     * <tt>-----END CERTIFICATE-----</tt> delimiters.
     * 
     * @param certificate the certificate to be encoded as a PEM string (not
     *            null).
     * @return the PEM textual representation of the certificate.
     */
    public static String encode(X509Certificate certificate) {
        if (certificate == null) {
            throw new NullPointerException();
        }
        try {
            byte[] der = certificate.getEncoded();
            StringBuilder result = new StringBuilder();
            result.append("-----BEGIN CERTIFICATE-----\n");
            result.append(Base64.encodeBytes(der));
            result.append("\n-----END CERTIFICATE-----");
            return result.toString();
        } catch (CertificateEncodingException ex) {
            throw new InternalFailureException("Unexpected error", ex);
        }
    }

    /**
     * Parse the certificate provided as a PEM string. The method accepts a
     * string representing an X509v3 certificate encoded using the textual PEM
     * format, that is, a DER binary certificate encoded through Base64 and
     * represented in an ASCII armored format, using the <tt>-----BEGIN
     * CERTIFICATE-----</tt>
     * and <tt>-----END CERTIFICATE-----</tt> delimiters. The method returns a
     * null certificate in case of parsing exception.
     * 
     * @param text the PEM encoded certificate, included the delimiters.
     * @return the decoded X509v3 certificate.
     */
    public static X509Certificate decode(String text) {
        if (text == null) {
            throw new NullPointerException("text cannot be null.");
        }
        try {
            int start = text.indexOf("-----BEGIN CERTIFICATE-----") + 27;
            int end = text.indexOf("-----END CERTIFICATE-----");
            text = text.substring(start, end);
            StringBuilder builder = new StringBuilder();
            builder.append("-----BEGIN CERTIFICATE-----\n");
            for (int i = 0; i < text.length(); ++i) {
                char ch = text.charAt(i);
                if ((ch != ' ') && (ch != '\t') && (ch != '\n') && (ch != '\t')) {
                    builder.append(ch);
                }
            }
            builder.append("\n-----END CERTIFICATE-----\n");
            InputStream stream = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
            Certificate result = certificateFactory.generateCertificate(stream);
            stream.close();
            return (result instanceof X509Certificate) ? (X509Certificate) result : null;
        } catch (CertificateException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IOException ex) {
            throw new InternalFailureException("Unexpected error", ex);
        }
    }

    /**
     * Return the SHA1 digest of the specified X509v3 certificate. This method
     * retrieve the DER standard representation of the certificate and then
     * produce a digest from it.
     * 
     * @param certificate the certificate whose SHA1 digest should be returned
     *            (not null).
     * @return the digest for the specified certificate.
     */
    public static byte[] hash(X509Certificate certificate) {
        if (certificate == null) {
            throw new NullPointerException("certificate cannot be null");
        }
        try {
            byte[] encodedCertificate = certificate.getEncoded();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(encodedCertificate);
            return md.digest();
        } catch (CertificateEncodingException ex) {
            throw new InternalFailureException("Unable to encode a X509v3 certificate (!)", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalFailureException("No SHA1 algorithm (!)", ex);
        }
    }

    /**
     * Return an HEX string encoding the SHA1 digest of the specified X509v3
     * certificate. This method retrieve the DER standard representation of the
     * certificate and then produce a digest from it.
     * 
     * @param certificate the certificate whose SHA1 digest should be returned
     *            (not null).
     * @return the HEX string with the digest for the specified certificate.
     */
    public static String stringHash(X509Certificate certificate) {
        return Util.encodeHexString(hash(certificate));
    }

    /**
     * Return a textual representation of the X500 name specified. The returned
     * representation is a lowercase string containing only the attributes: CN,
     * L, ST, O, OU, C, STREET, DC, UID (in this order and without spaces
     * between them); OID-typed attributes are removed.
     * 
     * @param principal the principal whose name should be returned as a
     *            canonical string (not null).
     * @return the textual representation for the X500 name specified.
     */
    public static String encodeDN(X500Principal principal) {
        if (principal == null) {
            throw new NullPointerException();
        }
        String result = principal.getName(X500Principal.RFC2253);
        int start = result.indexOf("CN=");
        if (start > 0) {
            result = result.substring(start);
        }
        return result;
    }
}
