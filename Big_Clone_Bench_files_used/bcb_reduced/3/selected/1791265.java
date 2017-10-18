package org.ccnx.ccn.test.security.keys;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.util.Random;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.SystemConfiguration;
import org.ccnx.ccn.impl.CCNFlowControl;
import org.ccnx.ccn.impl.security.crypto.CCNDigestHelper;
import org.ccnx.ccn.impl.security.keys.PublicKeyCache;
import org.ccnx.ccn.impl.security.keys.KeyServer;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.ccnx.ccn.protocol.SignedInfo;
import org.ccnx.ccn.test.Flosser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Initial test of KeyManager functionality.
 *
 */
public class KeyManagerTest {

    protected static Random _rand = new Random();

    protected static final int KEY_COUNT = 5;

    protected static final int DATA_COUNT_PER_KEY = 3;

    protected static KeyPair[] pairs = new KeyPair[KEY_COUNT];

    static ContentName testprefix = ContentName.fromNative(new String[] { "test", "pubidtest" });

    static ContentName keyprefix = ContentName.fromNative(testprefix, "keys");

    static ContentName dataprefix = ContentName.fromNative(testprefix, "data");

    static PublisherPublicKeyDigest[] publishers = new PublisherPublicKeyDigest[KEY_COUNT];

    static KeyLocator[] keyLocs = new KeyLocator[KEY_COUNT];

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            for (int i = 0; i < KEY_COUNT; ++i) {
                pairs[i] = kpg.generateKeyPair();
                publishers[i] = new PublisherPublicKeyDigest(pairs[i].getPublic());
                keyLocs[i] = new KeyLocator(new ContentName(keyprefix, publishers[i].digest()));
            }
        } catch (Exception e) {
            System.out.println("Exception in test setup: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testWriteContent() throws Exception {
        Flosser flosser = new Flosser(testprefix);
        CCNHandle thandle = CCNHandle.open();
        CCNFlowControl fc = new CCNFlowControl(testprefix, thandle);
        KeyManager km = KeyManager.getDefaultKeyManager();
        CCNHandle handle = CCNHandle.open(km);
        PublicKeyCache kr = new PublicKeyCache();
        KeyServer ks = new KeyServer(handle);
        for (int i = 0; i < KEY_COUNT; ++i) {
            ks.serveKey(keyLocs[i].name().name(), pairs[i].getPublic(), km.getDefaultKeyID(), null);
        }
        Random rand = new Random();
        for (int i = 0; i < DATA_COUNT_PER_KEY; ++i) {
            byte[] buf = new byte[1024];
            rand.nextBytes(buf);
            byte[] digest = CCNDigestHelper.digest(buf);
            ContentName dataName = new ContentName(dataprefix, digest);
            for (int j = 0; j < KEY_COUNT; ++j) {
                SignedInfo si = new SignedInfo(publishers[j], keyLocs[j]);
                ContentObject co = new ContentObject(dataName, si, buf, pairs[j].getPrivate());
                System.out.println("Key " + j + ": " + publishers[j] + " signed content " + i + ": " + dataName);
                fc.put(co);
            }
        }
        for (int i = 0; i < KEY_COUNT; ++i) {
            System.out.println("Attempting to retrieive key " + i + ":");
            PublicKey pk = kr.getPublicKey(publishers[i], keyLocs[i], SystemConfiguration.getDefaultTimeout(), handle);
            if (null == pk) {
                System.out.println("..... failed.");
            } else {
                System.out.println("..... got it! Correct key? " + (pk.equals(pairs[i].getPublic())));
            }
        }
        flosser.stop();
        thandle.close();
    }
}
