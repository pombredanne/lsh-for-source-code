package ru.amse.jsynchro.kernel;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class DigestAlgorithm {

    /**
     * MessageDigest algorithm to be used.
     */
    private String algorithm = "MD5";

    /**
     * MessageDigest Algorithm provider
     */
    private String provider = null;

    /**
     * Message Digest instance
     */
    private MessageDigest messageDigest = null;

    /**
     * Size of the read buffer to use.
     */
    private int readBufferSize = 8 * 1024;

    /**
     * Specifies the algorithm to be used to compute the checksum.
     * Defaults to "MD5". Other popular algorithms like "SHA" may be used as well.
     * @param algorithm the digest algorithm to use
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Sets the MessageDigest algorithm provider to be used
     * to calculate the checksum.
     * @param provider provider to use
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /** Initialize the security message digest. */
    public void initMessageDigest() {
        if (messageDigest != null) {
            return;
        }
        if ((provider != null) && !"".equals(provider) && !"null".equals(provider)) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm, provider);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new AssertionError(noalgo);
            } catch (NoSuchProviderException noprovider) {
                throw new AssertionError(noprovider);
            }
        } else {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new AssertionError(noalgo);
            }
        }
    }

    /**
     * This algorithm doesn't need any configuration.
     * Therefore it's always valid.
     * @return <i>true</i> if all is ok, otherwise <i>false</i>.
     */
    public boolean isValid() {
        return true;
    }

    public String getValue(File file) {
        initMessageDigest();
        String checksum = null;
        try {
            if (!file.canRead()) {
                return null;
            }
            FileInputStream fis = null;
            byte[] buf = new byte[readBufferSize];
            try {
                messageDigest.reset();
                fis = new FileInputStream(file);
                DigestInputStream dis = new DigestInputStream(fis, messageDigest);
                while (dis.read(buf, 0, readBufferSize) != -1) {
                }
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest();
                StringBuffer checksumSb = new StringBuffer();
                for (int i = 0; i < fileDigest.length; i++) {
                    String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);
                    if (hexStr.length() < 2) {
                        checksumSb.append("0");
                    }
                    checksumSb.append(hexStr);
                }
                checksum = checksumSb.toString();
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return checksum;
    }

    /**
     * Override Object.toString().
     * @return some information about this algorithm.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<DigestAlgorithm:");
        buf.append("algorithm=").append(algorithm);
        buf.append(";provider=").append(provider);
        buf.append(">");
        return buf.toString();
    }
}
