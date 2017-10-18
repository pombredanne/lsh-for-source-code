package eu.europa.tmsearch.services.resources.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.ws.rs.core.EntityTag;
import org.apache.log4j.Logger;

public class MD5ETagGenerator implements ETagGenerator {

    private static Logger log = Logger.getLogger(MD5ETagGenerator.class);

    /**
     * An Entity Tag for the set resource
     */
    private EntityTag entityTag;

    /**
     * Creates an EntityTag for the set object.
     * 
     * @param source
     *            Object to create the EntityTag from
     * @return A EntityTag
     */
    public EntityTag setSourceObject(Object source) {
        try {
            byte[] resultsToByte = objToByteArray(source);
            this.entityTag = new EntityTag(getMd5Digest(resultsToByte));
            if (log.isDebugEnabled()) log.debug("Hash created: [" + this.entityTag + "]");
        } catch (IOException ioe) {
            if (log.isDebugEnabled()) log.debug("Exception while converting source object to byte array" + ioe);
        }
        return this.entityTag;
    }

    public EntityTag getETag() {
        return this.entityTag;
    }

    /**
     * Creates an MD5 hash out of a byte array.
     * 
     * @param bytes
     *            <code>byte[]</code> the byte array
     * @return the MD5 hash in 16 byte <code>String</code> format
     */
    private String getMd5Digest(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 cryptographic algorithm is not available.", e);
        }
        byte[] messageDigest = md.digest(bytes);
        BigInteger number = new BigInteger(1, messageDigest);
        StringBuffer sb = new StringBuffer('0');
        sb.append(number.toString(16));
        return sb.toString();
    }

    /**
     * Converts an object to a byte array.
     * 
     * @param obj
     *            the <code>Object</code> to convert
     * @return <code>byte[]</code>
     */
    private byte[] objToByteArray(Object obj) throws IOException {
        byte[] byteArray = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream out = null;
        try {
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            byteArray = baos.toByteArray();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return byteArray;
    }
}
