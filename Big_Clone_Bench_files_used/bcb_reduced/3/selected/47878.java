package au.gov.nla.aons.registry.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;
import au.gov.nla.aons.registry.domain.KeyFormatProperty;
import au.gov.nla.aons.registry.domain.ValueFormatProperty;

/**
 * A convenience class designed to wrap around Java's relatively raw digest
 * methods.
 * 
 * @author David
 * 
 */
public class DigestUtilImpl {

    public DigestContext createDigestContext() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("We could not find the algorithm [SHA-1] with which we intend to create the digest");
        }
        DigestContext context = new DigestContext();
        context.setDigest(digest);
        return context;
    }

    /**
     * Updates the digest with the specified valueFormatProperty
     * 
     * @param valueFormatProperty
     * @param context
     */
    public void updateWithValueFormatProperty(ValueFormatProperty valueFormatProperty, DigestContext context) {
        if (valueFormatProperty != null) {
            updateWithString(valueFormatProperty.getReadableKey(), context);
            updateWithString(valueFormatProperty.getSearchValue(), context);
        }
    }

    /**
     * Updates the digest with the specified keyFormatProperty
     * 
     * @param keyFormatProperty
     * @param context
     */
    public void updateWithKeyFormatProperty(KeyFormatProperty keyFormatProperty, DigestContext context) {
        if (keyFormatProperty != null) {
            updateWithString(keyFormatProperty.getReadableKey(), context);
        }
    }

    /**
     * Updates the digest with the specified string
     * 
     * @param value
     * @param context
     */
    public void updateWithString(String value, DigestContext context) {
        if (value != null) {
            long size = value.length();
            context.setBytesWritten(context.getBytesWritten() + size);
            MessageDigest digest = context.getDigest();
            digest.update(value.getBytes());
        }
    }

    public String createBase64Digest(DigestContext context) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(context.getDigest().digest());
    }
}
