package org.msurtani.ssh4j;

import java.math.BigInteger;
import org.msurtani.ssh4j.util.SshConstants;
import org.apache.log4j.Logger;
import java.util.Random;
import java.io.IOException;
import org.msurtani.ssh4j.crypto.SHA1;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.KeyAgreement;
import java.security.Key;

/**
 *
 * @author  manik
 */
public class DiffieHellmanGroup1Sha1 implements KeyExchanger {

    public static final BigInteger PRIME_P = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16);

    public static final BigInteger GENERATOR_G = BigInteger.ONE.add(BigInteger.ONE);

    public static final BigInteger GROUP_ORDER_Q = (PRIME_P.subtract(BigInteger.ONE)).divide(new BigInteger("2"));

    public static Logger logger = Logger.getLogger("DiffieHellmanGroup1Sha1");

    private SshTransport transport;

    public byte[] initIVCToS = new byte[16];

    public byte[] initIVSToC = new byte[16];

    public byte[] encCToS = new byte[16];

    public byte[] encSToC = new byte[16];

    public byte[] integCToS = new byte[16];

    public byte[] integSToC = new byte[16];

    private SHA1 hasher = new SHA1();

    private byte[] s, H, I_S, I_C;

    private BigInteger e, f;

    private SecretKey sharedSecretK;

    private PublicKey serverHostKey = null, serverPublicKey = null;

    private KeyPair clientKeyPair;

    private KeyAgreement clientKeyAgreement;

    /** Creates a new instance of DiffieHellmanGroup1Sha1 */
    public DiffieHellmanGroup1Sha1(SshTransport transport, byte[] I_C, byte[] I_S) {
        this.I_C = I_C;
        this.I_S = I_S;
        this.transport = transport;
    }

    public void exchangeKeys() throws IOException {
        logger.info("Starting Diffie-Hellman key exchange");
        try {
            DHParameterSpec dhGroup1Spec = new DHParameterSpec(PRIME_P, GENERATOR_G);
            logger.debug("DHParameterSpec = " + dhGroup1Spec);
            KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("DH");
            logger.debug("KPGen = " + keypairGen);
            keypairGen.initialize(dhGroup1Spec);
            clientKeyPair = keypairGen.generateKeyPair();
            byte[] privKey = clientKeyPair.getPrivate().getEncoded();
            BigInteger privKeyBI = new BigInteger(privKey);
            e = GENERATOR_G.modPow(privKeyBI, PRIME_P);
            byte[] clientPublicKey = SshMisc.createMpInt(e.toByteArray());
            byte[] payload = new byte[clientPublicKey.length + 1];
            payload[0] = SshConstants.SSH_MSG_KEXDH_INIT;
            for (int i = 0; i < clientPublicKey.length; i++) {
                payload[i + 1] = clientPublicKey[i];
            }
            transport.sendPacket(payload);
            SshPacket packet = transport.readPacket();
            parseKexDHReplyData(packet.getPayload());
            logger.debug("About to calc shared secret");
            sharedSecretK = computeSharedSecretK();
            logger.debug("Computed shared secret K");
            H = computeH();
            logger.debug("Generated H.");
            boolean isValid = verifyH();
            if (isValid) {
                logger.debug("H is valid ... ");
            } else {
                logger.debug("H seems to be invalid - proceeding anyway!!");
            }
            initIVCToS = generateKey(65);
            initIVSToC = generateKey(66);
            encCToS = generateKey(67);
            encSToC = generateKey(68);
            integSToC = generateKey(69);
            integSToC = generateKey(70);
            byte[] newKeys = new byte[1];
            newKeys[0] = SshConstants.SSH_MSG_NEWKEYS;
            transport.sendPacket(newKeys);
        } catch (InvalidAlgorithmParameterException pe) {
            logger.fatal("Kex failed - ", pe);
        } catch (NoSuchAlgorithmException ne) {
            logger.fatal("Kex failed - ", ne);
        }
    }

    private byte[] generateKey(int knownValue) {
        byte[] K = SshMisc.createMpInt(sharedSecretK.getEncoded());
        byte[] dataToHash = new byte[K.length + (2 * H.length) + 1];
        int offset = 0;
        for (int i = 0; i < K.length; i++) {
            dataToHash[offset++] = K[i];
        }
        for (int i = 0; i < H.length; i++) {
            dataToHash[offset++] = H[i];
        }
        dataToHash[offset++] = (byte) knownValue;
        for (int i = 0; i < H.length; i++) {
            dataToHash[offset++] = H[i];
        }
        hasher.update(dataToHash, 0, dataToHash.length);
        byte[] key = hasher.digest();
        byte[] keyToReturn = new byte[16];
        for (int i = 0; i < 16; i++) {
            keyToReturn[i] = key[i];
        }
        return keyToReturn;
    }

    private boolean verifyH() {
        boolean stat = false;
        String signatureType = SshMisc.getString(0, s);
        logger.debug("signatureType is " + signatureType);
        if (!signatureType.equalsIgnoreCase("ssh-rsa")) {
            logger.error("Unsupported key type " + signatureType + "!!");
            return false;
        }
        byte[] signature = SshMisc.getStringAsBytes(4 + signatureType.length(), s);
        try {
            java.security.Signature sig = java.security.Signature.getInstance("SHA1withRSA");
            sig.initVerify(serverHostKey);
            sig.update(H);
            stat = sig.verify(signature);
        } catch (Exception e) {
            logger.error("", e);
        }
        return stat;
    }

    private PublicKey getServerHostKey(byte[] data) {
        String keyType = SshMisc.getString(0, data);
        logger.debug("Key type: " + keyType);
        if (!keyType.equalsIgnoreCase("ssh-rsa")) {
            logger.error("Unsupported key type " + keyType + "!!");
            return null;
        }
        int offset = 4 + keyType.length();
        BigInteger sshRsaE = new BigInteger(SshMisc.getMpInt(offset, data));
        offset += 4;
        offset += sshRsaE.toByteArray().length;
        BigInteger sshRsaN = new BigInteger(SshMisc.getMpInt(offset, data));
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(sshRsaN, sshRsaE);
            KeyFactory keyfactory = KeyFactory.getInstance("RSA");
            PublicKey serverPubKey = keyfactory.generatePublic(keySpec);
            logger.info("retrieved server host key " + serverPubKey);
            return serverPubKey;
        } catch (Exception e) {
            logger.error("Problems creating server host key", e);
        }
        return null;
    }

    private byte[] computeH() {
        String V_C = transport.clientId;
        String V_S = transport.serverId;
        if (V_C.endsWith("\n") || V_C.endsWith("\r")) {
            V_C = V_C.substring(0, V_C.length() - 1);
        }
        if (V_C.endsWith("\n") || V_C.endsWith("\r")) {
            V_C = V_C.substring(0, V_C.length() - 1);
        }
        if (V_S.endsWith("\n") || V_S.endsWith("\r")) {
            V_S = V_S.substring(0, V_S.length() - 1);
        }
        if (V_S.endsWith("\n") || V_S.endsWith("\r")) {
            V_S = V_S.substring(0, V_S.length() - 1);
        }
        byte[] V_C_s = SshMisc.createString(V_C);
        byte[] V_S_s = SshMisc.createString(V_S);
        byte[] I_C_s = SshMisc.createString(I_C);
        byte[] I_S_s = SshMisc.createString(I_S);
        byte[] K_S_s = SshMisc.createMpInt(serverHostKey.getEncoded());
        byte[] e_mpint = SshMisc.createMpInt(e);
        byte[] f_mpint = SshMisc.createMpInt(f);
        byte[] K_mpint = SshMisc.createMpInt(sharedSecretK.getEncoded());
        byte[] stuffToHash = new byte[V_C_s.length + V_S_s.length + I_C_s.length + I_S_s.length + K_S_s.length + e_mpint.length + f_mpint.length + K_mpint.length];
        int offset = 0;
        for (int i = 0; i < V_C_s.length; i++) stuffToHash[offset++] = V_C_s[i];
        for (int i = 0; i < V_S_s.length; i++) stuffToHash[offset++] = V_S_s[i];
        for (int i = 0; i < I_C_s.length; i++) stuffToHash[offset++] = I_C_s[i];
        for (int i = 0; i < I_S_s.length; i++) stuffToHash[offset++] = I_S_s[i];
        for (int i = 0; i < K_S_s.length; i++) stuffToHash[offset++] = K_S_s[i];
        for (int i = 0; i < e_mpint.length; i++) stuffToHash[offset++] = e_mpint[i];
        for (int i = 0; i < f_mpint.length; i++) stuffToHash[offset++] = f_mpint[i];
        for (int i = 0; i < K_mpint.length; i++) stuffToHash[offset++] = K_mpint[i];
        hasher.update(stuffToHash, 0, stuffToHash.length);
        return hasher.digest();
    }

    private SecretKey computeSharedSecretK() {
        BigInteger x = new BigInteger(clientKeyPair.getPrivate().getEncoded());
        BigInteger ssK = f.modPow(x, PRIME_P);
        return new SecretKeySpec(ssK.toByteArray(), "DES");
    }

    private void parseKexDHReplyData(byte[] data) {
        if (data[0] != SshConstants.SSH_MSG_KEXDH_REPLY) {
            logger.fatal("Invalid server response!  Dying ... ");
            return;
        }
        int offset = 1;
        byte[] K_S = SshMisc.getStringAsBytes(offset, data);
        offset += 4;
        offset += K_S.length;
        serverHostKey = getServerHostKey(K_S);
        byte[] fb = SshMisc.getMpInt(offset, data);
        f = new BigInteger(fb);
        offset += 4;
        offset += fb.length;
        s = SshMisc.getStringAsBytes(offset, data);
    }

    public byte[] getSessionId() {
        return H;
    }

    public KeyPair getClientKeyPair() {
        return clientKeyPair;
    }

    public PublicKey getHostPublicKey() {
        return serverHostKey;
    }

    public Key getSharedSecret() {
        return sharedSecretK;
    }

    public SecretKey getEncCtoS() {
        return new SecretKeySpec(encCToS, "DES");
    }
}
