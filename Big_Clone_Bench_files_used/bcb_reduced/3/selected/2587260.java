package org.xmldap.xmldsig;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import nu.xom.Attribute;
import nu.xom.Element;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.ws.WSConstants;

/**
 * Created by IntelliJ IDEA.
 * User: cmort
 * Date: Mar 26, 2006
 * Time: 3:19:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SymmetricKeyInfo implements KeyInfo {

    private byte[] secretKey;

    private PublicKey publicKey;

    public SymmetricKeyInfo(PublicKey publicKey, byte[] secretKey) {
        this.secretKey = secretKey;
        this.publicKey = publicKey;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public Element getEmphemeralSymmetricKeyInfo() throws SerializationException {
        Element keyInfo = new Element(WSConstants.DSIG_PREFIX + ":KeyInfo", WSConstants.DSIG_NAMESPACE);
        Element encryptedKey = new Element(WSConstants.ENC_PREFIX + ":EncryptedKey", WSConstants.ENC_NAMESPACE);
        Element encryptionMethod = new Element(WSConstants.ENC_PREFIX + ":EncryptionMethod", WSConstants.ENC_NAMESPACE);
        Attribute encMethAlg = new Attribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
        encryptionMethod.addAttribute(encMethAlg);
        encryptedKey.appendChild(encryptionMethod);
        Element subKeyInfo = new Element(WSConstants.DSIG_PREFIX + ":KeyInfo", WSConstants.DSIG_NAMESPACE);
        Element x509Data = new Element(WSConstants.DSIG_PREFIX + ":X509Data", WSConstants.DSIG_NAMESPACE);
        Element keyIdentifier = new Element(WSConstants.WSSE_PREFIX + ":KeyIdentifier", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Attribute valueType = new Attribute("ValueType", "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1");
        Attribute encodingType = new Attribute("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
        keyIdentifier.addAttribute(valueType);
        keyIdentifier.addAttribute(encodingType);
        String fingerPrint = "";
        try {
            fingerPrint = CryptoUtils.digest(publicKey.getEncoded(), "SHA");
        } catch (org.xmldap.exceptions.CryptoException e) {
            e.printStackTrace();
        }
        keyIdentifier.appendChild(fingerPrint);
        x509Data.appendChild(keyIdentifier);
        subKeyInfo.appendChild(x509Data);
        encryptedKey.appendChild(subKeyInfo);
        Element cipherData = new Element(WSConstants.ENC_PREFIX + ":CipherData", WSConstants.ENC_NAMESPACE);
        Element cipherValue = new Element(WSConstants.ENC_PREFIX + ":CipherValue", WSConstants.ENC_NAMESPACE);
        try {
            String cipherText = CryptoUtils.rsaoaepEncrypt(secretKey, (RSAPublicKey) publicKey);
            cipherValue.appendChild(cipherText);
        } catch (org.xmldap.exceptions.CryptoException e) {
            e.printStackTrace();
        }
        cipherData.appendChild(cipherValue);
        encryptedKey.appendChild(cipherData);
        keyInfo.appendChild(encryptedKey);
        return keyInfo;
    }

    public String toXML() throws SerializationException {
        Element keyInfo = serialize();
        return keyInfo.toXML();
    }

    public Element serialize() throws SerializationException {
        return getEmphemeralSymmetricKeyInfo();
    }
}
