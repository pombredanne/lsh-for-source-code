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

/**
 * Tests saving a SecureKeyCache to disk
 */
public class MergeSecureKeyCacheTestRepo {

    static KeyPair pair2 = null;

    static KeyPair myPair2 = null;

    static Key key2 = null;

    static KeyPair pair1 = null;

    static KeyPair myPair1 = null;

    static Key key1 = null;

    static SecureKeyCache cache1 = null;

    static SecureKeyCache cache2 = null;

    static byte[] pubIdentifier2 = null;

    static byte[] myPubIdentifier2 = null;

    static byte[] keyIdentifier2 = null;

    static ContentName keyName2 = null;

    static ContentName privateKeyName2 = null;

    static byte[] pubIdentifier1 = null;

    static byte[] myPubIdentifier1 = null;

    static byte[] keyIdentifier1 = null;

    static ContentName keyName1 = null;

    static ContentName privateKeyName1 = null;

    static KeyPairGenerator kpg = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
    }

    @Test
    public void testDisjointMerge() throws Exception {
        populateCache1();
        populateCache2();
        byte[] origKey1 = pair1.getPrivate().getEncoded();
        byte[] origMyKey1 = myPair1.getPrivate().getEncoded();
        byte[] origSymKey1 = key1.getEncoded();
        byte[] origKey2 = pair2.getPrivate().getEncoded();
        byte[] origMyKey2 = myPair2.getPrivate().getEncoded();
        byte[] origSymKey2 = key2.getEncoded();
        cache1.merge(cache2);
        Assert.assertTrue(cache1.size() == 6);
        Assert.assertTrue(cache1.getPrivateKeys().length == 4);
        Assert.assertTrue(cache1.containsKey(keyName1));
        Assert.assertTrue(cache1.containsKey(privateKeyName1));
        Assert.assertTrue(cache1.containsKey(keyName2));
        Assert.assertTrue(cache1.containsKey(privateKeyName2));
        Assert.assertFalse(cache1.containsKey(ContentName.fromNative("/nothere")));
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(myPubIdentifier1).getEncoded(), origMyKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(pubIdentifier1).getEncoded(), origKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKey(keyIdentifier1).getEncoded(), origSymKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(privateKeyName1), pubIdentifier1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(keyName1), keyIdentifier1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(myPubIdentifier2).getEncoded(), origMyKey2) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(pubIdentifier2).getEncoded(), origKey2) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKey(keyIdentifier2).getEncoded(), origSymKey2) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(privateKeyName2), pubIdentifier2) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(keyName2), keyIdentifier2) == 0);
    }

    @Test
    public void testSameCache() throws Exception {
        populateCache1();
        cache2 = cache1;
        cache1.merge(cache2);
        byte[] origKey1 = pair1.getPrivate().getEncoded();
        byte[] origMyKey1 = myPair1.getPrivate().getEncoded();
        byte[] origSymKey1 = key1.getEncoded();
        Assert.assertTrue(cache1.size() == 3);
        Assert.assertTrue(cache1.getPrivateKeys().length == 2);
        Assert.assertTrue(cache1.containsKey(keyName1));
        Assert.assertTrue(cache1.containsKey(privateKeyName1));
        Assert.assertFalse(cache1.containsKey(keyName2));
        Assert.assertFalse(cache1.containsKey(privateKeyName2));
        Assert.assertFalse(cache1.containsKey(ContentName.fromNative("/nothere")));
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(myPubIdentifier1).getEncoded(), origMyKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(pubIdentifier1).getEncoded(), origKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKey(keyIdentifier1).getEncoded(), origSymKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(privateKeyName1), pubIdentifier1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(keyName1), keyIdentifier1) == 0);
    }

    @Test
    public void testDomination() throws Exception {
        populateCache1();
        cache2 = new SecureKeyCache();
        ContentName newPrivName = ContentName.fromNative("/test/newpriv");
        cache2.addPrivateKey(newPrivName, pubIdentifier1, pair1.getPrivate());
        cache2.addMyPrivateKey(myPubIdentifier1, myPair1.getPrivate());
        ContentName newKeyName = ContentName.fromNative("/test/newkey");
        cache2.addKey(newKeyName, key1);
        cache1.merge(cache2);
        byte[] origKey1 = pair1.getPrivate().getEncoded();
        byte[] origMyKey1 = myPair1.getPrivate().getEncoded();
        byte[] origSymKey1 = key1.getEncoded();
        Assert.assertTrue(cache1.size() == 3);
        Assert.assertTrue(cache1.getPrivateKeys().length == 2);
        Assert.assertTrue(cache1.containsKey(keyName1));
        Assert.assertTrue(cache1.containsKey(privateKeyName1));
        Assert.assertFalse(cache1.containsKey(newKeyName));
        Assert.assertFalse(cache1.containsKey(newPrivName));
        Assert.assertFalse(cache1.containsKey(ContentName.fromNative("/nothere")));
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(myPubIdentifier1).getEncoded(), origMyKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getPrivateKey(pubIdentifier1).getEncoded(), origKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKey(keyIdentifier1).getEncoded(), origSymKey1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(privateKeyName1), pubIdentifier1) == 0);
        Assert.assertTrue(DataUtils.compare(cache1.getKeyID(keyName1), keyIdentifier1) == 0);
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
    }

    private void populateCache1() throws Exception {
        pair1 = kpg.generateKeyPair();
        privateKeyName1 = ContentName.fromNative("/test/priv1");
        cache1 = new SecureKeyCache();
        pubIdentifier1 = new PublisherPublicKeyDigest(pair1.getPublic()).digest();
        cache1.addPrivateKey(privateKeyName1, pubIdentifier1, pair1.getPrivate());
        myPair1 = kpg.generateKeyPair();
        myPubIdentifier1 = new PublisherPublicKeyDigest(myPair1.getPublic()).digest();
        cache1.addMyPrivateKey(myPubIdentifier1, myPair1.getPrivate());
        key1 = WrappedKey.generateNonceKey();
        keyName1 = ContentName.fromNative("/test/key1");
        keyIdentifier1 = SecureKeyCache.getKeyIdentifier(key1);
        cache1.addKey(keyName1, key1);
    }

    private void populateCache2() throws Exception {
        pair2 = kpg.generateKeyPair();
        privateKeyName2 = ContentName.fromNative("/test/priv2");
        cache2 = new SecureKeyCache();
        pubIdentifier2 = new PublisherPublicKeyDigest(pair2.getPublic()).digest();
        cache2.addPrivateKey(privateKeyName2, pubIdentifier2, pair2.getPrivate());
        myPair2 = kpg.generateKeyPair();
        myPubIdentifier2 = new PublisherPublicKeyDigest(myPair2.getPublic()).digest();
        cache2.addMyPrivateKey(myPubIdentifier2, myPair2.getPrivate());
        key2 = WrappedKey.generateNonceKey();
        keyName2 = ContentName.fromNative("/test/key2");
        keyIdentifier2 = SecureKeyCache.getKeyIdentifier(key2);
        cache2.addKey(keyName2, key2);
    }
}
