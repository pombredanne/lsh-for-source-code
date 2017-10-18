package org.ccnx.ccn.test.impl.security.keys;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import junit.framework.Assert;
import org.ccnx.ccn.impl.security.keys.SecureKeyCache;
import org.ccnx.ccn.impl.support.DataUtils;
import org.ccnx.ccn.io.content.WrappedKey;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Tests saving a SecureKeyCache to disk
 */
public class SaveSecureKeyCacheTestRepo {

    static KeyPair pair = null;

    static KeyPair myPair = null;

    static Key key = null;

    static SecureKeyCache cache = null;

    static byte[] pubIdentifier = null;

    static byte[] myPubIdentifier = null;

    static byte[] keyIdentifier = null;

    static String file = "try";

    static ContentName keyName = null;

    static ContentName privateKeyName = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        pair = kpg.generateKeyPair();
        privateKeyName = ContentName.fromNative("/test/priv");
        cache = new SecureKeyCache();
        pubIdentifier = new PublisherPublicKeyDigest(pair.getPublic()).digest();
        cache.addPrivateKey(privateKeyName, pubIdentifier, pair.getPrivate());
        myPair = kpg.generateKeyPair();
        myPubIdentifier = new PublisherPublicKeyDigest(myPair.getPublic()).digest();
        cache.addMyPrivateKey(myPubIdentifier, myPair.getPrivate());
        key = WrappedKey.generateNonceKey();
        keyName = ContentName.fromNative("/test/key");
        keyIdentifier = SecureKeyCache.getKeyIdentifier(key);
        cache.addKey(keyName, key);
        File f = new File(file);
        FileOutputStream fos = new FileOutputStream(f);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(cache);
        out.close();
    }

    @Test
    public void testReadSecureKeyCache() throws Exception {
        byte[] origKey = pair.getPrivate().getEncoded();
        byte[] origMyKey = myPair.getPrivate().getEncoded();
        byte[] origSymKey = key.getEncoded();
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        SecureKeyCache newCache = (SecureKeyCache) ois.readObject();
        Assert.assertTrue(newCache.getPrivateKeys().length == 2);
        Assert.assertTrue(newCache.containsKey(keyName));
        Assert.assertTrue(newCache.containsKey(privateKeyName));
        Assert.assertFalse(newCache.containsKey(ContentName.fromNative("/nothere")));
        Assert.assertTrue(DataUtils.compare(newCache.getPrivateKey(myPubIdentifier).getEncoded(), origMyKey) == 0);
        Assert.assertTrue(DataUtils.compare(newCache.getPrivateKey(pubIdentifier).getEncoded(), origKey) == 0);
        Assert.assertTrue(DataUtils.compare(newCache.getKey(keyIdentifier).getEncoded(), origSymKey) == 0);
        Assert.assertTrue(DataUtils.compare(newCache.getKeyID(privateKeyName), pubIdentifier) == 0);
        Assert.assertTrue(DataUtils.compare(newCache.getKeyID(keyName), keyIdentifier) == 0);
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        File f = new File(file);
        f.delete();
    }
}
