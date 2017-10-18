package org.signserver.server.entities;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 * Counter in database for number of signings made with a particular key.
 * 
 * @author Markus Kilås
 * @version $Id: KeyUsageCounter.java 2267 2012-03-23 08:20:31Z netmackan $
 */
@Entity
@Table(name = "KeyUsageCounter")
public class KeyUsageCounter implements Serializable {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(KeyUsageCounter.class);

    @Id
    private String keyHash;

    private long counter;

    public KeyUsageCounter() {
        counter = 0;
    }

    public KeyUsageCounter(String keyHash) {
        this();
        this.keyHash = keyHash;
    }

    public long getCounter() {
        return counter;
    }

    public String getKeyHash() {
        return keyHash;
    }

    @Override
    public String toString() {
        return "Counter(" + keyHash + ", " + counter + ")";
    }

    public static String createKeyHash(PublicKey key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256", "BC");
            final String res = new String(Hex.encode(md.digest(key.getEncoded())));
            md.reset();
            return res;
        } catch (NoSuchProviderException ex) {
            final String message = "Nu such provider trying to hash public key";
            LOG.error(message, ex);
            throw new RuntimeException(message, ex);
        } catch (NoSuchAlgorithmException ex) {
            final String message = "Nu such algorithm trying to hash public key";
            LOG.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
}
