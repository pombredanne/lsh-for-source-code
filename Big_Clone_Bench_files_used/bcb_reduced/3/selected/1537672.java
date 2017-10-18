package org.snmp4j.security;

import java.security.MessageDigest;
import org.snmp4j.log.*;
import org.snmp4j.smi.OctetString;

/**
 * The abstract class AuthGeneric implements common operations for
 * SNMP authentication protocols, such as MD5 and SHA.
 *
 * @author Jochen Katz & Frank Fock
 * @version 1.0
 */
public abstract class AuthGeneric implements AuthenticationProtocol {

    private static final LogAdapter logger = LogFactory.getLogger(AuthGeneric.class);

    private int digestLength;

    private String protoName;

    public AuthGeneric(String protoName, int digestLength) {
        this.protoName = protoName;
        this.digestLength = digestLength;
    }

    public int getDigestLength() {
        return digestLength;
    }

    /**
   * Get a fresh MessageDigest object of the Algorithm specified in the
   * constructor.
   *
   * @return a new, fresh Message Digest object.
   */
    protected MessageDigest getDigestObject() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(protoName);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new InternalError(protoName + " not supported in this VM.");
        }
        return md;
    }

    public boolean authenticate(byte[] authenticationKey, byte[] message, int messageOffset, int messageLength, ByteArrayWindow digest) {
        MessageDigest md = getDigestObject();
        byte[] newDigest;
        byte[] k_ipad = new byte[64];
        byte[] k_opad = new byte[64];
        for (int i = 0; i < MESSAGE_AUTHENTICATION_CODE_LENGTH; ++i) {
            digest.set(i, (byte) 0);
        }
        for (int i = 0; i < authenticationKey.length; ++i) {
            k_ipad[i] = (byte) (authenticationKey[i] ^ 0x36);
            k_opad[i] = (byte) (authenticationKey[i] ^ 0x5c);
        }
        for (int i = authenticationKey.length; i < 64; ++i) {
            k_ipad[i] = 0x36;
            k_opad[i] = 0x5c;
        }
        md.update(k_ipad);
        md.update(message, messageOffset, messageLength);
        newDigest = md.digest();
        md.reset();
        md.update(k_opad);
        md.update(newDigest);
        newDigest = md.digest();
        for (int i = 0; i < 12; ++i) {
            digest.set(i, newDigest[i]);
        }
        return true;
    }

    public boolean isAuthentic(byte[] authenticationKey, byte[] message, int messageOffset, int messageLength, ByteArrayWindow digest) {
        ByteArrayWindow origDigest = new ByteArrayWindow(new byte[MESSAGE_AUTHENTICATION_CODE_LENGTH], 0, MESSAGE_AUTHENTICATION_CODE_LENGTH);
        System.arraycopy(digest.getValue(), digest.getOffset(), origDigest.getValue(), 0, MESSAGE_AUTHENTICATION_CODE_LENGTH);
        if (!authenticate(authenticationKey, message, messageOffset, messageLength, digest)) {
            return false;
        }
        return digest.equals(origDigest, 12);
    }

    public byte[] changeDelta(byte[] oldKey, byte[] newKey, byte[] random) {
        MessageDigest hash = getDigestObject();
        int digestLength = hash.getDigestLength();
        if (logger.isDebugEnabled()) {
            logger.debug(protoName + "oldKey: " + new OctetString(oldKey).toHexString());
            logger.debug(protoName + "newKey: " + new OctetString(newKey).toHexString());
            logger.debug(protoName + "random: " + new OctetString(random).toHexString());
        }
        int iterations = (oldKey.length - 1) / hash.getDigestLength();
        OctetString tmp = new OctetString(oldKey);
        OctetString delta = new OctetString();
        for (int k = 0; k < iterations; k++) {
            tmp.append(random);
            hash.update(tmp.getValue());
            tmp.setValue(hash.digest());
            delta.append(new byte[digestLength]);
            for (int kk = 0; kk < digestLength; kk++) {
                delta.set(k * digestLength + kk, (byte) (tmp.get(kk) ^ newKey[k * digestLength + kk]));
            }
        }
        tmp.append(random);
        hash.update(tmp.getValue());
        tmp = new OctetString(hash.digest(), 0, oldKey.length - delta.length());
        for (int j = 0; j < tmp.length(); j++) {
            tmp.set(j, (byte) (tmp.get(j) ^ newKey[iterations * digestLength + j]));
        }
        byte[] keyChange = new byte[random.length + delta.length() + tmp.length()];
        System.arraycopy(random, 0, keyChange, 0, random.length);
        System.arraycopy(delta.getValue(), 0, keyChange, random.length, delta.length());
        System.arraycopy(tmp.getValue(), 0, keyChange, random.length + delta.length(), tmp.length());
        if (logger.isDebugEnabled()) {
            logger.debug(protoName + "keyChange:" + new OctetString(keyChange).toHexString());
        }
        return keyChange;
    }

    public byte[] passwordToKey(OctetString passwordString, byte[] engineID) {
        MessageDigest md = getDigestObject();
        byte[] digest;
        byte[] buf = new byte[64];
        int password_index = 0;
        int count = 0;
        byte[] password = passwordString.getValue();
        while (count < 1048576) {
            for (int i = 0; i < 64; ++i) {
                buf[i] = password[password_index++ % password.length];
            }
            md.update(buf);
            count += 64;
        }
        digest = md.digest();
        if (logger.isDebugEnabled()) {
            logger.debug(protoName + "First digest: " + new OctetString(digest).toHexString());
        }
        md.reset();
        md.update(digest);
        md.update(engineID);
        md.update(digest);
        digest = md.digest();
        if (logger.isDebugEnabled()) {
            logger.debug(protoName + "localized key: " + new OctetString(digest).toHexString());
        }
        return digest;
    }

    public byte[] hash(byte[] data) {
        MessageDigest md = getDigestObject();
        md.update(data);
        return md.digest();
    }

    public byte[] hash(byte[] data, int offset, int length) {
        MessageDigest md = getDigestObject();
        md.update(data, offset, length);
        return md.digest();
    }
}
