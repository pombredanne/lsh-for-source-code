package es.alvsanand.webpage.services.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptographyServiceImpl implements CryptographyService {

    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-512";

    private String digestAlgorithm;

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public byte[] digest(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance((digestAlgorithm != null) ? digestAlgorithm : DEFAULT_DIGEST_ALGORITHM);
        md.update(data);
        return md.digest();
    }
}
