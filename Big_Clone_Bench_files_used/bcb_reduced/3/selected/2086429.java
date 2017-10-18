package jaxlib.net.ssh;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jaxlib.logging.Log;
import jaxlib.net.ssh.event.SshMessageEvent;
import jaxlib.net.ssh.event.SshMessageListener;
import jaxlib.net.ssh.message.SshDebug;
import jaxlib.net.ssh.message.SshDhGroupExchangeGroup;
import jaxlib.net.ssh.message.SshDhGroupExchangeInit;
import jaxlib.net.ssh.message.SshDhGroupExchangeReply;
import jaxlib.net.ssh.message.SshDhGroupExchangeRequest;
import jaxlib.net.ssh.message.SshDhGroupExchangeRequestOld;
import jaxlib.net.ssh.message.SshDhInit;
import jaxlib.net.ssh.message.SshDhReply;
import jaxlib.net.ssh.message.SshKeyExchangeInit;
import jaxlib.net.ssh.message.SshKeyExchangeNewKeys;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: SshKeyExchange.java 2739 2009-07-29 16:36:39Z joerg_wassmer $
 */
final class SshKeyExchange extends Object implements SshMessageListener {

    private static final BigInteger G = BigInteger.valueOf(2);

    private static final BigInteger P1 = new BigInteger("17976931348623159077083915679378745319786029604875" + "60117064444236841971802161585193689478337958649255415021805654859805036464" + "40548199239100050792877003355816639229553136239076508735759914822574862575" + "00742530207744771258955095793777842444242661733472762929938766870920560605" + "0270810842907692932019128194467627007");

    private static final BigInteger P14 = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129" + "024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0" + "A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB" + "6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A" + "163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208" + "552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36C" + "E3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF69558171" + "83995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);

    private static final Map<String, String> sshToJceCipherName = new LinkedHashMap<String, String>();

    static {
        sshToJceCipherName.put("aes128-cbc", "AES/CBC/NoPadding");
        sshToJceCipherName.put("3des-cbc", "DESede/CBC/NoPadding");
        sshToJceCipherName.put("blowfish-cbc", "Blowfish/CBC/NoPadding");
        sshToJceCipherName.put("aes192-cbc", "AES/CBC/NoPadding");
        sshToJceCipherName.put("aes256-cbc", "AES/CBC/NoPadding");
        sshToJceCipherName.put("aes128-ctr", "AES/CTR/NoPadding");
        sshToJceCipherName.put("aes192-ctr", "AES/CTR/NoPadding");
        sshToJceCipherName.put("aes256-ctr", "AES/CTR/NoPadding");
    }

    private static final Map<String, String> sshKeyExchangeToJceDigestName = new LinkedHashMap<String, String>();

    static {
        sshKeyExchangeToJceDigestName.put("diffie-hellman-group-exchange-sha256", "SHA-256");
        sshKeyExchangeToJceDigestName.put("diffie-hellman-group-exchange-sha1", "SHA1");
        sshKeyExchangeToJceDigestName.put("diffie-hellman-group14-sha1", "SHA1");
        sshKeyExchangeToJceDigestName.put("diffie-hellman-group1-sha1", "SHA1");
    }

    private static final Map<String, String> sshToJceMacName = new LinkedHashMap<String, String>();

    static {
        sshToJceMacName.put("hmac-md5", "HMAC/MD5");
        sshToJceMacName.put("hmac-sha1", "HMAC/SHA1");
    }

    static final Map<String, Provider> cipherProvidersBySshName;

    static final Map<String, Provider> digestProvidersBySshKeyExchangeName;

    static final Map<String, Provider> macProvidersBySshName;

    static {
        final List<Provider> providers = new ArrayList<Provider>();
        final Map<String, Provider> ciphers = new LinkedHashMap<String, Provider>();
        final Map<String, Provider> digests = new LinkedHashMap<String, Provider>();
        final Map<String, Provider> macs = new LinkedHashMap<String, Provider>();
        providers.addAll(Arrays.asList(Security.getProviders()));
        try {
            Provider p = (Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
            for (final Provider e : providers) {
                if (e.getClass() == p.getClass()) {
                    p = null;
                    break;
                }
            }
            if (p != null) providers.add(p);
        } catch (final Exception ex) {
            SshOptions.defaultLog.config("unable to instantiate BouncyCastleProvider", ex);
        }
        for (final Map.Entry<String, String> e : sshToJceCipherName.entrySet()) {
            for (int i = providers.size(); --i >= 0; ) {
                final Provider p = providers.get(i);
                try {
                    Cipher.getInstance(e.getValue(), p);
                    ciphers.put(e.getKey(), p);
                    break;
                } catch (final NoSuchAlgorithmException ex) {
                } catch (final NoSuchPaddingException ex) {
                }
            }
        }
        for (final Map.Entry<String, String> e : sshKeyExchangeToJceDigestName.entrySet()) {
            for (final Provider p : providers) {
                try {
                    MessageDigest.getInstance(e.getValue(), p);
                    digests.put(e.getKey(), p);
                    break;
                } catch (final NoSuchAlgorithmException ex) {
                }
            }
        }
        for (final Map.Entry<String, String> e : sshToJceMacName.entrySet()) {
            for (final Provider p : providers) {
                try {
                    Mac.getInstance(e.getValue(), p);
                    macs.put(e.getKey(), p);
                    break;
                } catch (final NoSuchAlgorithmException ex) {
                }
            }
        }
        cipherProvidersBySshName = Collections.unmodifiableMap(ciphers);
        digestProvidersBySshKeyExchangeName = Collections.unmodifiableMap(digests);
        macProvidersBySshName = Collections.unmodifiableMap(macs);
    }

    private static Cipher createCipher(final String sshName) throws NoSuchAlgorithmException, NoSuchPaddingException {
        String jceName = sshToJceCipherName.get(sshName);
        if (jceName == null) jceName = sshName;
        final Provider provider = cipherProvidersBySshName.get(sshName);
        return (provider == null) ? Cipher.getInstance(jceName) : Cipher.getInstance(jceName, provider);
    }

    private static Mac createMac(final String sshName) throws NoSuchAlgorithmException {
        String jceName = sshToJceMacName.get(sshName);
        if (jceName == null) jceName = sshName;
        final Provider provider = macProvidersBySshName.get(sshName);
        return (provider == null) ? Mac.getInstance(jceName) : Mac.getInstance(jceName, provider);
    }

    private static void updateBigInt(final MessageDigest md, final BigInteger v) {
        updateByteArray(md, v.toByteArray());
    }

    private static void updateByteArray(final MessageDigest md, final byte[] v) {
        updateInt(md, v.length);
        md.update(v);
    }

    private static void updateInt(final MessageDigest md, final int v) {
        md.update((byte) ((v >>> 24) & 0xff));
        md.update((byte) ((v >>> 16) & 0xff));
        md.update((byte) ((v >>> 8) & 0xff));
        md.update((byte) (v & 0xff));
    }

    private static void updateString(final MessageDigest md, final String v) {
        final int len = v.length();
        updateInt(md, len);
        for (int i = 0; i < len; i++) md.update((byte) v.charAt(i));
    }

    private final Log log;

    private final SshConnection connection;

    private boolean ignoreNextMessage;

    private SshDhGroupExchangeRequest dhGroupExchangeParameters;

    private SshKeyExchangeInit keyExchangeInitLocal;

    private SshKeyExchangeInit keyExchangeInitRemote;

    private byte[] sessionId;

    private byte[] h;

    private BigInteger k;

    private DHParameterSpec paramSpec;

    private KeyPair keyPair;

    private volatile boolean done;

    SshKeyExchange(final SshConnection connection) throws IOException {
        super();
        this.log = connection.log;
        this.connection = connection;
        this.dhGroupExchangeParameters = new SshDhGroupExchangeRequest(1024, 1024, 4096);
        connection.getImpl().messageListeners.add(this);
    }

    private MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        final String sshName = this.connection.info.getAlgorithms().keyExchange;
        String jceName = sshKeyExchangeToJceDigestName.get(sshName);
        if (jceName == null) jceName = sshName;
        final Provider provider = digestProvidersBySshKeyExchangeName.get(sshName);
        return (provider == null) ? MessageDigest.getInstance(jceName) : MessageDigest.getInstance(jceName, provider);
    }

    private byte[] deriveKey(final MessageDigest md, final char id, final int len, final BigInteger k, final byte[] h) throws GeneralSecurityException {
        md.reset();
        updateBigInt(md, k);
        md.update(h);
        md.update((byte) id);
        md.update(this.sessionId);
        byte[] a = md.digest();
        final byte[] key = new byte[len];
        System.arraycopy(a, 0, key, 0, Math.min(len, a.length));
        for (int pos = a.length; pos < len; ) {
            a = null;
            updateBigInt(md, k);
            md.update(h);
            md.update(key, 0, pos);
            a = md.digest();
            System.arraycopy(a, 0, key, pos, Math.min(len - pos, a.length));
            pos += a.length;
        }
        return key;
    }

    private void initDhGroup1SHA1() throws IOException {
        try {
            this.paramSpec = new DHParameterSpec(P1, G);
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(this.paramSpec, this.connection.getImpl().random);
            this.keyPair = keyGen.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new SshException(ex);
        }
        this.connection.send(new SshDhInit(((DHPublicKey) this.keyPair.getPublic()).getY()));
    }

    private void initDhGroup14SHA1() throws IOException {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            this.paramSpec = new DHParameterSpec(P14, G);
            keyGen.initialize(this.paramSpec, this.connection.getImpl().random);
            this.keyPair = keyGen.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new SshException(ex);
        }
        this.connection.send(new SshDhInit(((DHPublicKey) this.keyPair.getPublic()).getY()));
    }

    private void initDhGroupExchange() throws IOException {
        if (this.dhGroupExchangeParameters.minLength == 0) this.connection.send(new SshDhGroupExchangeRequestOld(this.dhGroupExchangeParameters.preferredLength)); else this.connection.send(this.dhGroupExchangeParameters);
    }

    private void onDhGroupExchangeGroup(final SshDhGroupExchangeGroup msg) throws GeneralSecurityException, IOException {
        if ((this.keyPair != null) || this.connection.isServer()) throw new SshException("%s: unexpected %s", this.connection.uri, msg.getType());
        this.paramSpec = new DHParameterSpec(msg.p, msg.g, this.dhGroupExchangeParameters.preferredLength);
        {
            final BigInteger mx = this.paramSpec.getP().subtract(BigInteger.ONE).shiftRight(1);
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(this.paramSpec, this.connection.getImpl().random);
            for (int i = 64, k = 16; --i >= 0; ) {
                final KeyPair keyPair = keyGen.generateKeyPair();
                final BigInteger x = ((DHPrivateKey) keyPair.getPrivate()).getX();
                final BigInteger y = ((DHPublicKey) keyPair.getPublic()).getY();
                if ((x.compareTo(BigInteger.ONE) > 0) && (y.compareTo(BigInteger.ONE) >= 1) && (x.compareTo(mx) < 0) && (y.compareTo(this.paramSpec.getP()) < 0)) {
                    this.keyPair = keyPair;
                    break;
                }
                if (--k <= 0) {
                    k = 8;
                    Thread.yield();
                }
            }
            if (this.keyPair == null) throw new SshException("%s: key generation failed:\np = %s\ng = %s", this.connection.uri, msg.p, msg.g);
        }
        this.connection.send(new SshDhGroupExchangeInit(((DHPublicKey) this.keyPair.getPublic()).getY()));
    }

    private void onDhGroupExchangeReply(final SshDhGroupExchangeReply msg) throws GeneralSecurityException, IOException {
        if ((this.keyPair == null) || this.connection.isServer()) throw new SshException("%s: unexpected %s", this.connection.uri, msg.getType());
        if ((msg.f.signum() <= 0) || (msg.f.compareTo(this.paramSpec.getP()) >= 0)) throw new SshException("%s: illegal f:\nf = %s\np = %s", this.connection.uri, msg.f, this.paramSpec.getP());
        final BigInteger k;
        {
            k = msg.f.modPow(((DHPrivateKey) this.keyPair.getPrivate()).getX(), this.paramSpec.getP());
            if ((k.compareTo(BigInteger.ONE) < 0) || (k.compareTo(this.paramSpec.getP()) >= 0)) {
                throw new SshException("%s: illegal key:\nk = %s\np = %s\ng = %s\nx = %s\ny = %s", this.connection.uri, k, this.paramSpec.getP(), this.paramSpec.getG(), ((DHPrivateKey) this.keyPair.getPrivate()).getX(), ((DHPublicKey) this.keyPair.getPublic()).getY());
            }
        }
        final byte[] h;
        {
            final MessageDigest md = createMessageDigest();
            updateString(md, SshVersion.LOCAL.toString());
            updateString(md, this.connection.getRemoteSshVersion().toString());
            updateByteArray(md, this.keyExchangeInitLocal.getPayload());
            updateByteArray(md, this.keyExchangeInitRemote.getPayload());
            updateByteArray(md, msg.hostKey);
            updateInt(md, this.dhGroupExchangeParameters.minLength);
            updateInt(md, this.dhGroupExchangeParameters.preferredLength);
            updateInt(md, this.dhGroupExchangeParameters.maxLength);
            updateBigInt(md, this.paramSpec.getP());
            updateBigInt(md, this.paramSpec.getG());
            updateBigInt(md, ((DHPublicKey) this.keyPair.getPublic()).getY());
            updateBigInt(md, msg.f);
            updateBigInt(md, k);
            h = md.digest();
        }
        if (this.sessionId == null) this.sessionId = h;
        this.h = h;
        this.k = k;
        this.connection.send(new SshKeyExchangeNewKeys());
    }

    private void onDhReply(final SshDhReply msg) throws GeneralSecurityException, IOException {
        if ((this.keyPair == null) || this.connection.isServer()) throw new SshException("%s: unexpected %s", this.connection.uri, msg.getType());
        final BigInteger k;
        {
            final DHPublicKeySpec remoteKeySpec = new DHPublicKeySpec(new BigInteger(msg.f), P1, G);
            final KeyFactory dhKeyFact = KeyFactory.getInstance("DH");
            final DHPublicKey remotePubKey = (DHPublicKey) dhKeyFact.generatePublic(remoteKeySpec);
            final KeyAgreement dhKex = KeyAgreement.getInstance("DH");
            dhKex.init(this.keyPair.getPrivate());
            dhKex.doPhase(remotePubKey, true);
            k = new BigInteger(dhKex.generateSecret());
        }
        final MessageDigest md = createMessageDigest();
        final byte[] h;
        {
            updateByteArray(md, SshVersion.LOCAL.toString().getBytes());
            updateByteArray(md, this.connection.getRemoteSshVersion().toString().getBytes());
            updateByteArray(md, this.keyExchangeInitLocal.getPayload());
            updateByteArray(md, this.keyExchangeInitRemote.getPayload());
            updateByteArray(md, msg.hostKey);
            updateByteArray(md, ((DHPublicKey) this.keyPair.getPublic()).getY().toByteArray());
            updateByteArray(md, msg.f);
            updateBigInt(md, k);
            h = md.digest();
        }
        if (this.sessionId == null) this.sessionId = h;
        this.keyExchangeInitLocal = null;
        this.keyExchangeInitRemote = null;
        this.h = h;
        this.k = k;
        this.connection.send(new SshKeyExchangeNewKeys());
    }

    private void onInit(final SshKeyExchangeInit msg) throws IOException {
        if (this.connection.info.getAlgorithms() != null) throw new SshException("%s: unexpected %s", this.connection.uri, msg.getType());
        this.keyExchangeInitRemote = msg;
        final SshConnectionInfo.Algorithms algorithms = new SshConnectionInfo.Algorithms(this.connection.getImpl(), msg);
        this.connection.info.setAlgorithms(algorithms);
        log.info("%s:\n%s", this.connection.uri, algorithms);
        if (algorithms.keyExchange.equals("diffie-hellman-group-exchange-sha1") || algorithms.keyExchange.equals("diffie-hellman-group-exchange-sha256")) initDhGroupExchange(); else if (algorithms.keyExchange.equals("diffie-hellman-group1-sha1")) initDhGroup1SHA1(); else if (algorithms.keyExchange.equals("diffie-hellman-group14-sha1")) initDhGroup14SHA1(); else throw new SshException("%s: unexpected key exchange method %s", this.connection.uri, algorithms.keyExchange);
    }

    private void onNewKeys(final SshKeyExchangeNewKeys msg) throws IOException, GeneralSecurityException {
        final byte[] h = this.h;
        if (h == null) throw new SshException("%s: unexpected %s", this.connection.uri, msg.getType());
        final BigInteger k = this.k;
        final MessageDigest md = createMessageDigest();
        final SshConnectionInfo.Algorithms algos = this.connection.info.getAlgorithms();
        final Cipher cipherIn;
        final Cipher cipherOut;
        if (algos.encryptionClientToServer.equals("none")) {
            cipherIn = null;
            cipherOut = null;
        } else {
            cipherIn = createCipher(algos.encryptionServerToClient);
            cipherOut = createCipher(algos.encryptionClientToServer);
            cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(deriveKey(md, 'D', algos.getCipherKeySize(), k, h), cipherIn.getAlgorithm()), new IvParameterSpec(deriveKey(md, 'B', cipherIn.getBlockSize(), k, h)));
            cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(deriveKey(md, 'C', algos.getCipherKeySize(), k, h), cipherOut.getAlgorithm()), new IvParameterSpec(deriveKey(md, 'A', cipherOut.getBlockSize(), k, h)));
        }
        final Mac macIn;
        final Mac macOut;
        if (algos.macClientToServer.equals("none")) {
            macIn = null;
            macOut = null;
        } else {
            macIn = createMac(algos.macServerToClient);
            macOut = createMac(algos.macClientToServer);
            macIn.init(new SecretKeySpec(deriveKey(md, 'F', algos.getMacKeySize(), k, h), macIn.getAlgorithm()));
            macOut.init(new SecretKeySpec(deriveKey(md, 'E', algos.getMacKeySize(), k, h), macOut.getAlgorithm()));
        }
        this.h = null;
        this.k = null;
        this.done = true;
        final SshConnectionImpl connectionImpl = this.connection.getImpl();
        synchronized (connectionImpl.inLock) {
            connectionImpl.in.setCipher(cipherIn);
            connectionImpl.in.setMac(macIn);
        }
        synchronized (connectionImpl.outLock) {
            connectionImpl.out.setCipher(cipherOut);
            connectionImpl.out.setMac(macOut);
        }
        connectionImpl.send(new SshDebug(false, "KEX done", ""));
    }

    final void init() throws IOException {
        this.h = null;
        this.k = null;
        this.paramSpec = null;
        this.keyPair = null;
        this.keyExchangeInitRemote = null;
        this.keyExchangeInitLocal = new SshKeyExchangeInit(this.connection.options, this.connection.getImpl().random);
        this.connection.send(this.keyExchangeInitLocal);
    }

    final boolean isDone() {
        return this.done;
    }

    @Override
    public final void onShhMessage(final SshMessageEvent e) throws IOException, GeneralSecurityException {
        if (!e.received || !e.message.getType().keyExchange || !e.consume()) return;
        if (e.connection != this.connection) throw new SecurityException("wrong connection");
        synchronized (this) {
            if (this.ignoreNextMessage) this.ignoreNextMessage = false; else {
                switch(e.message.getType()) {
                    case KEYEXCHANGE_INIT:
                        onInit((SshKeyExchangeInit) e.message);
                        return;
                    case KEYEXCHANGE_NEW_KEYS:
                        onNewKeys((SshKeyExchangeNewKeys) e.message);
                        return;
                    case KEYEXCHANGE_DH_REPLY:
                        onDhReply((SshDhReply) e.message);
                        return;
                    case KEYEXCHANGE_DH_GEX_GROUP:
                        onDhGroupExchangeGroup((SshDhGroupExchangeGroup) e.message);
                        return;
                    case KEYEXCHANGE_DH_GEX_REPLY:
                        onDhGroupExchangeReply((SshDhGroupExchangeReply) e.message);
                        return;
                    default:
                        log.warning("%s: unexpected %s", this.connection.uri, e.message.getType());
                        break;
                }
            }
        }
    }
}
