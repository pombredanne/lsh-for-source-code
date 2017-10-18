package com.ark.fix.core;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import com.ark.fix.service.Config;
import com.ark.fix.service.Encryption;
import com.ark.fix.service.ServiceException;

public class SimpleDESEncryption implements Encryption {

    static int s_enc = 0;

    static int s_dec = 0;

    static class SessionInfo {

        public byte[] DES_Key;

        public byte[] IVec;

        public byte[] ChainBinding;

        public SessionInfo() {
            DES_Key = new byte[8];
            IVec = new byte[8];
            ChainBinding = new byte[8];
        }
    }

    private static final String DES_MASTER_KEY = "encryption.des_master_key_file";

    private SecretKeyFactory m_secKeyFactory;

    private SecretKey m_masterKey, m_sessionKey;

    private IvParameterSpec m_masterIv, m_inIv, m_outIv;

    private byte[] m_sessionCBC;

    private Cipher m_decCipher, m_encCipher;

    private MessageDigest m_digest;

    private int m_credentialLen;

    private byte[] m_credential;

    private SecureRandom m_random;

    public SimpleDESEncryption() {
    }

    public void init(Config v_cfg, Object param) throws ServiceException {
        String des_key_file = v_cfg.getValue(DES_MASTER_KEY);
        if (des_key_file == null) throw new ServiceException("DES master key file must be defined.");
        byte[] key_data = new byte[8];
        byte[] iv_data = new byte[8];
        try {
            DataInputStream dins = new DataInputStream(new FileInputStream(des_key_file));
            for (int i = 0; i < 8; i++) {
                key_data[i] = dins.readByte();
            }
            for (int i = 0; i < 8; i++) {
                iv_data[i] = dins.readByte();
            }
            dins.close();
            m_secKeyFactory = SecretKeyFactory.getInstance("DES");
            m_masterKey = m_secKeyFactory.generateSecret(new DESKeySpec(key_data));
            m_masterIv = new IvParameterSpec(iv_data);
            m_decCipher = Cipher.getInstance("DES/CBC/PKCS5Padding", "SunJCE");
            m_encCipher = Cipher.getInstance("DES/CBC/PKCS5Padding", "SunJCE");
            m_digest = MessageDigest.getInstance("MD5");
            m_random = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception ie) {
            ie.printStackTrace();
            throw new ServiceException("Can not create cipher: " + ie.getMessage());
        }
    }

    public void checkCredential(int raw_data_len, byte[] raw_data) throws ServiceException {
        try {
            byte[] clear_data;
            m_decCipher.init(Cipher.DECRYPT_MODE, m_masterKey, m_masterIv);
            clear_data = m_decCipher.doFinal(raw_data);
            if (clear_data.length != 24) {
                throw new ServiceException("Invalid session information -- length " + clear_data.length);
            }
            m_sessionKey = m_secKeyFactory.generateSecret(new DESKeySpec(clear_data));
            byte[] t = new byte[8];
            System.arraycopy(clear_data, 8, t, 0, 8);
            m_inIv = new IvParameterSpec(t);
            m_outIv = new IvParameterSpec(t);
            m_sessionCBC = new byte[8];
            System.arraycopy(clear_data, 16, m_sessionCBC, 0, 8);
        } catch (Exception ee) {
            ee.printStackTrace();
            throw new ServiceException("Invalid credential: " + ee.getMessage());
        }
    }

    public void prepareCredential() throws ServiceException {
        try {
            byte[] tmp;
            if (m_sessionKey == null) {
                KeyGenerator keygen = KeyGenerator.getInstance("DES");
                m_sessionKey = keygen.generateKey();
            }
            if (m_sessionCBC == null) {
                m_sessionCBC = getRandomBytes(8);
            }
            m_encCipher.init(Cipher.ENCRYPT_MODE, m_masterKey, m_masterIv);
            byte[] t = getRandomBytes(8);
            m_outIv = new IvParameterSpec(t);
            m_inIv = new IvParameterSpec(t);
            byte[] clear_data = new byte[24];
            System.arraycopy(m_sessionKey.getEncoded(), 0, clear_data, 0, 8);
            System.arraycopy(m_outIv.getIV(), 0, clear_data, 8, 8);
            System.arraycopy(m_sessionCBC, 0, clear_data, 16, 8);
            m_credential = m_encCipher.doFinal(clear_data);
        } catch (Exception ee) {
            ee.printStackTrace();
            throw new ServiceException("Fail to create credential: " + ee.getMessage());
        }
    }

    public int getCredentialLength() throws ServiceException {
        return m_credential.length;
    }

    public byte[] getCredential() throws ServiceException {
        return m_credential;
    }

    public byte[] decrypt(byte[] message) throws ServiceException {
        try {
            m_decCipher.init(Cipher.DECRYPT_MODE, m_sessionKey, m_inIv);
            byte[] code = m_decCipher.doFinal(message);
            int tail = 0;
            while (code[code.length - 1 - tail] != 0x01) {
                tail++;
                if (tail > 7 || tail > code.length - 1) {
                    throw new ServiceException("Invalid message, without end character");
                }
            }
            if (tail > 0) {
                byte[] tmp = code;
                code = new byte[tmp.length - tail];
                System.arraycopy(tmp, 0, code, 0, code.length);
            }
            return code;
        } catch (Exception ee) {
            ee.printStackTrace();
            throw new ServiceException("Fail to decrypt: " + ee.getMessage());
        }
    }

    public byte[] encrypt(byte[] message) throws ServiceException {
        try {
            if (message.length % 8 != 0) {
                byte[] tail = getRandomBytes(8 - message.length % 8);
                byte[] tmp = new byte[8 * (message.length / 8) + 8];
                System.arraycopy(message, 0, tmp, 0, message.length);
                System.arraycopy(tail, 0, tmp, message.length, tail.length);
                message = tmp;
            }
            m_encCipher.init(Cipher.ENCRYPT_MODE, m_sessionKey, m_outIv);
            byte[] raw = m_encCipher.doFinal(message);
            return raw;
        } catch (Exception ee) {
            ee.printStackTrace();
            throw new ServiceException("Fail to encrypt: " + ee.getMessage());
        }
    }

    public byte[] sign(byte[] message) throws ServiceException {
        try {
            m_digest.update(m_sessionKey.getEncoded());
            m_digest.update(message);
            m_digest.update(m_sessionKey.getEncoded());
            byte[] t = m_digest.digest();
            return t;
        } catch (Exception ee) {
            ee.printStackTrace();
            throw new ServiceException("Cannot create signature: " + ee.getMessage());
        }
    }

    public void verify(byte[] message, byte[] signature) throws ServiceException {
        byte[] output = sign(message);
        if (output.length != signature.length) throw new ServiceException("Signature lengthes do not match.");
        for (int i = 0; i < signature.length; i++) {
            if (output[i] != signature[i]) {
                throw new ServiceException("Signatures do not match.");
            }
        }
    }

    private byte[] getRandomBytes(int count) {
        byte[] ret = new byte[count];
        boolean work = true;
        while (work) {
            m_random.nextBytes(ret);
            work = false;
            for (int i = 0; i < count; i++) {
                if (ret[i] == 0x01) {
                    work = true;
                    break;
                }
            }
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        String key_file = "h:/fix/cfg/des.key";
        KeyGenerator keygen = KeyGenerator.getInstance("DES");
        SecretKey key = keygen.generateKey();
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        KeySpec spec = keyFactory.getKeySpec(key, DESKeySpec.class);
        FileOutputStream fos = new FileOutputStream(key_file);
        fos.write(((DESKeySpec) spec).getKey());
        byte[] rand = { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88 };
        fos.write(rand);
        fos.close();
    }
}
