package org.xmldap.infocard.roaming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import net.sourceforge.lightcrypto.SafeObject;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.crypto.EncryptedStoreKeys;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.util.Base64;
import org.xmldap.util.XmlFileUtil;
import org.xmldap.ws.WSConstants;
import org.xmldap.xml.Canonicalizable;
import org.xmldap.xml.XmlUtils;

public class EncryptedStore {

    private static byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    String roamingStoreString = null;

    String encryptedStoreString = null;

    byte[] salt = null;

    public String getRoamingStoreString() {
        return roamingStoreString;
    }

    public String getEncryptedStoreString() {
        return encryptedStoreString;
    }

    public String getParsedRoamingStore() {
        Builder parser = new Builder();
        Document roamingStore = null;
        try {
            roamingStore = parser.build(roamingStoreString, "");
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roamingStore.toXML();
    }

    public EncryptedStore(byte[] bytes, String password) throws CryptoException, ParsingException {
        String encoding = XmlFileUtil.getEncoding(bytes);
        int offset = XmlFileUtil.getBomLength(bytes);
        roamingStoreString = decryptStore(encoding, bytes, offset, password);
    }

    public EncryptedStore(InputStream encryptedStoreStream, String password) throws CryptoException, ParsingException {
        String encoding = null;
        try {
            encoding = XmlFileUtil.getEncoding(encryptedStoreStream);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
        roamingStoreString = decryptStore(encoding, encryptedStoreStream, password);
    }

    public EncryptedStore(String encoding, InputStream encryptedStoreStream, String password) throws CryptoException, ParsingException {
        roamingStoreString = decryptStore(encoding, encryptedStoreStream, password);
    }

    private String decryptStore(String encoding, byte[] bytes, int offset, String password) throws CryptoException, ParsingException {
        Document encryptedStore = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, bytes.length);
            encryptedStore = XmlFileUtil.readXml(encoding, bais);
        } catch (IOException e) {
            throw new ParsingException("Error parsing EncryptedStore", e);
        }
        return decryptStore(encryptedStore, password);
    }

    private String decryptStore(String encoding, InputStream encryptedStoreStream, String password) throws CryptoException, ParsingException {
        Document encryptedStore = null;
        try {
            encryptedStore = XmlFileUtil.readXml(encoding, encryptedStoreStream);
        } catch (IOException e) {
            throw new ParsingException("Error parsing EncryptedStore", e);
        }
        return decryptStore(encryptedStore, password);
    }

    private static String decryptStore(Document encryptedStore, String password) throws CryptoException, ParsingException {
        XPathContext context = new XPathContext();
        context.addNamespace("id", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        context.addNamespace("enc", "http://www.w3.org/2001/04/xmlenc#");
        Nodes saltNodes = encryptedStore.query("//id:StoreSalt", context);
        Element saltElm = (Element) saltNodes.get(0);
        Nodes cipherValueNodes = encryptedStore.query("//enc:CipherValue", context);
        Element cipherValueElm = (Element) cipherValueNodes.get(0);
        return decrypt(cipherValueElm.getValue(), password, saltElm.getValue());
    }

    private static String decrypt(String cipherText, String password, String salt) throws CryptoException {
        EncryptedStoreKeys keys = new EncryptedStoreKeys(password, Base64.decode(salt));
        byte[] cipherBytes = Base64.decode(cipherText);
        byte[] iv = new byte[16];
        byte[] integrityCode = new byte[32];
        byte[] data = new byte[cipherBytes.length - iv.length - integrityCode.length];
        byte[] ivPlusData = new byte[cipherBytes.length - integrityCode.length];
        System.arraycopy(cipherBytes, 0, iv, 0, 16);
        System.arraycopy(cipherBytes, 16, integrityCode, 0, 32);
        System.arraycopy(cipherBytes, 48, data, 0, data.length);
        System.arraycopy(iv, 0, ivPlusData, 0, 16);
        System.arraycopy(data, 0, ivPlusData, 16, data.length);
        SafeObject keyBytes = new SafeObject();
        try {
            keyBytes.setText(keys.getEncryptionKey());
        } catch (Exception e) {
            throw new CryptoException("Error Parsing Roaming Store", e);
        }
        StringBuffer clearText = null;
        clearText = CryptoUtils.decryptAESCBC(new StringBuffer(Base64.encodeBytesNoBreaks(ivPlusData)), keyBytes);
        byte[] hashedIntegrityCode;
        try {
            hashedIntegrityCode = getHashedIntegrityCode(iv, keys.getIntegrityKey(), clearText.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("NoSuchAlgorithmException while Parsing Roaming Store", e);
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UnsupportedEncodingException while Error Parsing Roaming Store", e);
        }
        boolean valid = Arrays.equals(integrityCode, hashedIntegrityCode);
        if (!valid) {
            throw new CryptoException("The cardstore did not pass the integrity check - it may have been tampered with");
        }
        int start = clearText.indexOf("<RoamingStore");
        return clearText.substring(start);
    }

    public void toStream(OutputStream output) throws IOException {
        Element encryptedStore = new Element("EncryptedStore", WSConstants.INFOCARD_NAMESPACE);
        Element storeSalt = new Element("StoreSalt", WSConstants.INFOCARD_NAMESPACE);
        storeSalt.appendChild(Base64.encodeBytesNoBreaks(salt));
        encryptedStore.appendChild(storeSalt);
        Element encryptedData = new Element("EncryptedData", WSConstants.ENC_NAMESPACE);
        Element cipherData = new Element("CipherData", WSConstants.ENC_NAMESPACE);
        Element cipherValue = new Element("CipherValue", WSConstants.ENC_NAMESPACE);
        encryptedStore.appendChild(encryptedData);
        encryptedData.appendChild(cipherData);
        cipherData.appendChild(cipherValue);
        cipherValue.appendChild(getEncryptedStoreString());
        try {
            output.write(bom);
            output.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>".getBytes("UTF8"));
            byte[] bytes = XmlUtils.canonicalize(encryptedStore, Canonicalizable.EXCLUSIVE_CANONICAL_XML);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public EncryptedStore(RoamingStore roamingStore, String password) throws CryptoException {
        Random rand = new Random();
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        byte[] iv = new byte[16];
        rand.nextBytes(iv);
        init(roamingStore, password, salt, iv);
    }

    public EncryptedStore(RoamingStore roamingStore, String password, byte[] salt, byte[] iv) throws CryptoException {
        init(roamingStore, password, salt, iv);
    }

    void init(RoamingStore roamingStore, String password, byte[] salt, byte[] iv) throws CryptoException {
        this.salt = salt;
        EncryptedStoreKeys keys = new EncryptedStoreKeys(password, salt);
        try {
            roamingStoreString = roamingStore.toXML();
        } catch (SerializationException e) {
            throw new CryptoException("Error getting RoamingStore XML", e);
        }
        byte[] integrityCode;
        try {
            integrityCode = getHashedIntegrityCode(iv, keys.getIntegrityKey(), roamingStoreString);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("NoSuchAlgorithmException while getting getHashedIntegrityCode", e);
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("UnsupportedEncodingException while getting getHashedIntegrityCode", e);
        }
        ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
        try {
            dataBytes.write(bom);
            dataBytes.write(roamingStoreString.getBytes("UTF8"));
        } catch (IOException e) {
            throw new CryptoException("IOException while writing to ByteArrayOutputStream", e);
        }
        AESLightEngine aes = new AESLightEngine();
        CBCBlockCipher cbc = new CBCBlockCipher(aes);
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbc);
        KeyParameter key = new KeyParameter(keys.getEncryptionKey());
        ParametersWithIV paramWithIV = new ParametersWithIV(key, iv);
        byte inputBuffer[] = new byte[16];
        byte outputBuffer[] = new byte[16];
        int bytesProcessed = 0;
        cipher.init(true, paramWithIV);
        int bytesRead = 0;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes.toByteArray());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(inputBuffer)) > 0) {
                int outsize = cipher.getUpdateOutputSize(bytesRead);
                if (outputBuffer.length < outsize) {
                    outputBuffer = new byte[outputBuffer.length * 2];
                }
                bytesProcessed = cipher.processBytes(inputBuffer, 0, bytesRead, outputBuffer, 0);
                if (bytesProcessed > 0) outputStream.write(outputBuffer, 0, bytesProcessed);
            }
        } catch (DataLengthException e) {
            throw new CryptoException(e);
        } catch (IllegalStateException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
        try {
            int outsize = cipher.getOutputSize(0);
            if (outputBuffer.length < outsize) {
                outputBuffer = new byte[outsize];
            }
            bytesProcessed = cipher.doFinal(outputBuffer, 0);
        } catch (DataLengthException e) {
            throw new CryptoException(e);
        } catch (IllegalStateException e) {
            throw new CryptoException(e);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException(e);
        }
        if (bytesProcessed > 0) outputStream.write(outputBuffer, 0, bytesProcessed);
        byte[] cipherText = outputStream.toByteArray();
        byte[] blob = new byte[48 + cipherText.length];
        System.arraycopy(iv, 0, blob, 0, 16);
        System.arraycopy(integrityCode, 0, blob, 16, 32);
        System.arraycopy(cipherText, 0, blob, 48, cipherText.length);
        encryptedStoreString = Base64.encodeBytesNoBreaks(blob);
    }

    private static byte[] getHashedIntegrityCode(byte[] iv, byte[] integrityKey, String clearText) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] clearBytes = new byte[0];
        clearBytes = clearText.getBytes("UTF8");
        byte[] lastBlock = new byte[16];
        System.arraycopy(clearBytes, clearBytes.length - 16, lastBlock, 0, 16);
        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA-256");
        byte[] integrityCheck = new byte[64];
        System.arraycopy(iv, 0, integrityCheck, 0, 16);
        System.arraycopy(integrityKey, 0, integrityCheck, 16, 32);
        System.arraycopy(lastBlock, 0, integrityCheck, 48, 16);
        digest.update(integrityCheck);
        return digest.digest();
    }
}
