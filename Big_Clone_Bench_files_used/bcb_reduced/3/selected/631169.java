package org.dbe.signature.xades.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public abstract class DigestTools {

    /**
	 * The digest algorithm used to calculate the digest value
	 */
    public static final String DIGEST_ALGORITHM_NAME = "SHA-1";

    /**
	 * Calculate a digest value of the specified certificate
	 * @param cert
	 * @return
	 * @throws CertDigestException
	 */
    public static byte[] getCertificateDigestValue(Certificate cert) throws CertDigestException {
        byte[] code;
        MessageDigest md;
        byte[] digestvalue;
        try {
            code = cert.getEncoded();
            md = MessageDigest.getInstance(DIGEST_ALGORITHM_NAME);
            md.update(code);
            digestvalue = md.digest(code);
        } catch (CertificateEncodingException e) {
            throw new CertDigestException("ERROR - Faild to calculate the encoded form of the certificate", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertDigestException("ERROR - The digest algorithm " + DIGEST_ALGORITHM_NAME + " is not available", e);
        }
        return digestvalue;
    }
}
