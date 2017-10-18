package org.ccnx.ccn.test.profiles.security.access.group;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;
import javax.crypto.KeyGenerator;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.impl.security.crypto.CCNDigestHelper;
import org.ccnx.ccn.impl.support.ByteArrayCompare;
import org.ccnx.ccn.io.content.Link;
import org.ccnx.ccn.io.content.WrappedKey;
import org.ccnx.ccn.io.content.WrappedKey.WrappedKeyObject;
import org.ccnx.ccn.profiles.VersioningProfile;
import org.ccnx.ccn.profiles.security.access.group.Group;
import org.ccnx.ccn.profiles.security.access.group.GroupAccessControlManager;
import org.ccnx.ccn.profiles.security.access.group.GroupAccessControlProfile;
import org.ccnx.ccn.profiles.security.access.group.PrincipalKeyDirectory;
import org.ccnx.ccn.protocol.ContentName;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class KeyDirectoryTestRepo {

    static Random rand = new Random();

    static final String directoryBase = "/test";

    static final String keyDirectoryBase = "/test/KeyDirectoryTestRepo-";

    static ContentName keyDirectoryName;

    static ContentName userStore;

    static String principalName = "pgolle-";

    static ContentName publicKeyName;

    static ContentName versionedDirectoryName;

    static PrivateKey wrappedPrivateKey;

    static Key AESSecretKey;

    static KeyPair wrappingKeyPair;

    static byte[] wrappingPKID;

    static GroupAccessControlManager acm;

    class TestPrincipalKeyDirectory extends PrincipalKeyDirectory {

        public TestPrincipalKeyDirectory() throws IOException {
            super(acm, versionedDirectoryName, handle);
        }

        public void testGetWrappedKeyForPrincipal() throws Exception {
            WrappedKeyObject wko = kd.getWrappedKeyForPrincipal(principalName);
            Assert.assertNotNull(wko);
            WrappedKey wk = wko.wrappedKey();
            Key unwrappedSecretKey = wk.unwrapKey(wrappingKeyPair.getPrivate());
            Assert.assertEquals(AESSecretKey, unwrappedSecretKey);
        }
    }

    static TestPrincipalKeyDirectory kd;

    static int testCount = 0;

    static CCNHandle handle;

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        kd.stopEnumerating();
        KeyManager.closeDefaultKeyManager();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        keyDirectoryName = ContentName.fromNative(keyDirectoryBase + Integer.toString(rand.nextInt(10000)));
        principalName = principalName + Integer.toString(rand.nextInt(10000));
        handle = CCNHandle.getHandle();
        ContentName cnDirectoryBase = ContentName.fromNative(directoryBase);
        ContentName groupStore = GroupAccessControlProfile.groupNamespaceName(cnDirectoryBase);
        userStore = ContentName.fromNative(cnDirectoryBase, "Users");
        acm = new GroupAccessControlManager(cnDirectoryBase, groupStore, userStore);
        versionedDirectoryName = VersioningProfile.addVersion(keyDirectoryName);
    }

    /**
	 * Ensures that the tests run in the correct order.
	 * @throws Exception
	 */
    @Test
    public void testInOrder() throws Exception {
        testKeyDirectoryCreation();
        testAddPrivateKey();
        testGetUnwrappedKeyGroupMember();
        testAddWrappedKey();
        addWrappingKeyToACM();
        testGetWrappedKeyForKeyID();
        kd.testGetWrappedKeyForPrincipal();
        testGetUnwrappedKey();
        testGetPrivateKey();
        testGetUnwrappedKeySuperseded();
        testAddPreviousKeyBlock();
    }

    public void testKeyDirectoryCreation() throws Exception {
        kd = new TestPrincipalKeyDirectory();
        Assert.assertNotNull(kd);
    }

    public void testAddPrivateKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        wrappingKeyPair = kpg.generateKeyPair();
        wrappedPrivateKey = wrappingKeyPair.getPrivate();
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        AESSecretKey = kg.generateKey();
        kd.addPrivateKeyBlock(wrappedPrivateKey, AESSecretKey);
        kd.waitForChildren();
        Assert.assertTrue(kd.hasPrivateKeyBlock());
    }

    public void testGetUnwrappedKeyGroupMember() throws Exception {
        ContentName myIdentity = ContentName.fromNative(userStore, "pgolle");
        acm.publishMyIdentity(myIdentity, null);
        String randomGroupName = "testGroup" + rand.nextInt(10000);
        ArrayList<Link> newMembers = new ArrayList<Link>();
        newMembers.add(new Link(myIdentity));
        Group myGroup = acm.groupManager().createGroup(randomGroupName, newMembers, 0);
        Assert.assertTrue(acm.groupManager().haveKnownGroupMemberships());
        Thread.sleep(5000);
        PrincipalKeyDirectory pkd = myGroup.privateKeyDirectory(acm);
        pkd.waitForChildren();
        Assert.assertTrue(pkd.hasPrivateKeyBlock());
        ContentName versionDirectoryName2 = VersioningProfile.addVersion(ContentName.fromNative(keyDirectoryBase + Integer.toString(rand.nextInt(10000))));
        PrincipalKeyDirectory kd2 = new PrincipalKeyDirectory(acm, versionDirectoryName2, handle);
        PublicKey groupPublicKey = myGroup.publicKey();
        ContentName groupPublicKeyName = myGroup.publicKeyName();
        kd2.addWrappedKeyBlock(AESSecretKey, groupPublicKeyName, groupPublicKey);
        byte[] expectedKeyID = CCNDigestHelper.digest(AESSecretKey.getEncoded());
        kd2.waitForChildren();
        Thread.sleep(10000);
        Key unwrappedSecretKey = kd2.getUnwrappedKey(expectedKeyID);
        Assert.assertEquals(AESSecretKey, unwrappedSecretKey);
        kd2.stopEnumerating();
    }

    public void testAddWrappedKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        wrappingKeyPair = kpg.generateKeyPair();
        PublicKey publicKey = wrappingKeyPair.getPublic();
        wrappingPKID = CCNDigestHelper.digest(publicKey.getEncoded());
        publicKeyName = ContentName.fromNative(userStore, principalName);
        ContentName versionPublicKeyName = VersioningProfile.addVersion(publicKeyName);
        kd.addWrappedKeyBlock(AESSecretKey, versionPublicKeyName, publicKey);
    }

    public void addWrappingKeyToACM() throws Exception {
        PrivateKey privKey = wrappingKeyPair.getPrivate();
        byte[] publicKeyIdentifier = CCNDigestHelper.digest(wrappingKeyPair.getPublic().getEncoded());
        handle.keyManager().getSecureKeyCache().addMyPrivateKey(publicKeyIdentifier, privKey);
    }

    public void testGetWrappedKeyForKeyID() throws Exception {
        CCNHandle handle = CCNHandle.open();
        PrincipalKeyDirectory uvkd = new PrincipalKeyDirectory(acm, keyDirectoryName, handle);
        while (!uvkd.hasChildren() || uvkd.getCopyOfWrappingKeyIDs().size() == 0) {
            uvkd.waitForNewChildren();
        }
        TreeSet<byte[]> wkid = uvkd.getCopyOfWrappingKeyIDs();
        Assert.assertEquals(1, wkid.size());
        Comparator<byte[]> byteArrayComparator = new ByteArrayCompare();
        Assert.assertEquals(0, byteArrayComparator.compare(wkid.first(), wrappingPKID));
        ContentName wkName = uvkd.getWrappedKeyNameForKeyID(wrappingPKID);
        Assert.assertNotNull(wkName);
        WrappedKeyObject wko = uvkd.getWrappedKeyForKeyID(wrappingPKID);
        WrappedKey wk = wko.wrappedKey();
        Key unwrappedSecretKey = wk.unwrapKey(wrappingKeyPair.getPrivate());
        Assert.assertEquals(AESSecretKey, unwrappedSecretKey);
        uvkd.stopEnumerating();
    }

    public void testGetUnwrappedKey() throws Exception {
        byte[] expectedKeyID = CCNDigestHelper.digest(AESSecretKey.getEncoded());
        Key unwrappedSecretKey = kd.getUnwrappedKey(expectedKeyID);
        Assert.assertEquals(AESSecretKey, unwrappedSecretKey);
    }

    public void testGetPrivateKey() throws Exception {
        Assert.assertTrue(kd.hasPrivateKeyBlock());
        PrivateKey privKey = kd.getPrivateKey();
        Assert.assertEquals(wrappedPrivateKey, privKey);
    }

    public void testGetUnwrappedKeySuperseded() throws Exception {
        ContentName supersededKeyDirectoryName = ContentName.fromNative(keyDirectoryBase + rand.nextInt(10000) + "/superseded");
        ContentName versionSupersededKeyDirectoryName = VersioningProfile.addVersion(supersededKeyDirectoryName);
        CCNHandle handle = CCNHandle.open();
        PrincipalKeyDirectory skd = new PrincipalKeyDirectory(acm, versionSupersededKeyDirectoryName, handle);
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key supersededAESSecretKey = kg.generateKey();
        byte[] expectedKeyID = CCNDigestHelper.digest(supersededAESSecretKey.getEncoded());
        ContentName supersedingKeyName = keyDirectoryName;
        skd.addSupersededByBlock(supersededAESSecretKey, supersedingKeyName, null, AESSecretKey);
        while (!skd.hasChildren() || !skd.hasSupersededBlock()) skd.waitForNewChildren();
        Assert.assertTrue(skd.hasSupersededBlock());
        Assert.assertNotNull(skd.getSupersededBlockName());
        Key unwrappedSecretKey = skd.getUnwrappedKey(expectedKeyID);
        Assert.assertEquals(supersededAESSecretKey, unwrappedSecretKey);
        skd.stopEnumerating();
    }

    public void testAddPreviousKeyBlock() throws Exception {
        Assert.assertTrue(!kd.hasPreviousKeyBlock());
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key newAESSecretKey = kg.generateKey();
        ContentName supersedingKeyName = ContentName.fromNative(keyDirectoryBase + "previous");
        kd.addPreviousKeyBlock(AESSecretKey, supersedingKeyName, newAESSecretKey);
        kd.waitForNewChildren();
        Assert.assertTrue(kd.hasPreviousKeyBlock());
    }
}
