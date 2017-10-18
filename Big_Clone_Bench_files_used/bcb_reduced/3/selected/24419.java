package eu.mpower.framework.security.encryption;

import eu.mpower.framework.security.encryption.soap.Algorithm;
import eu.mpower.framework.security.encryption.soap.ErrorCodes;
import eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashRequest;
import eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashResponse;
import eu.mpower.framework.security.encryption.soap.IEncryption;
import eu.mpower.framework.security.encryption.soap.IntegrityRequestSymmetricKey;
import eu.mpower.framework.security.encryption.soap.Status;
import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateFactory;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.jws.WebService;
import javax.xml.ws.WebServiceRef;
import eu.mpower.framework.security.authorization.soap.AuthorizationWServiceService;
import eu.mpower.framework.security.pki.soap.PKI;

/**
 *
 * decryptSymmetricKey is the operation used to decrypt data that is encrypted by means of symmetric key cryptography. The input to the operation is defined through the DecryptSymmetricKey type. The operation returns cleartext data in form of a byte array (byte[]). 
 * decryptPKI is used for decryption of data that is encrypted with public key cryptography. Data is encrypted with an actor’s public key, and its private key is used for decryption. DecryptRequestPKI defines the operation’s input. The operation has return type byte[], which returns cleartext data.
 * encryptSymmetricKey is used to encrypt data by use of symmetric key algorithms. EncryptSymmetricKey defines the operation’s input, and encrypted data is returned via the return type byte[].
 * encryptPKI encrypts data by use of public key cryptography. The recipient’s public key is used for encryption of data. Needed input for the operation is defined through EncryptRequestPKI. The output of the operation is a byte[] containing encrypted data.
 * generateCryptographicHash is used to generate the hash value that is needed to verify the integrity of its corresponding data. The operation should take advantage of a symmetric cryptography hash function. The input to the operation is defined via IntegrityRequestSymmetricKey. The cryptographic hash value is returned as byte[]. 
 * generateSignedHash is the operation to use when integrity protection by means of public key cryptography is necessary. A hash value is generated by some hash function that is not dependent on any keys as input. For a recipient to be able to trust the integrity of the received data and to verify the actual sender, the hash value is signed by the senders private key. The operation’s input is defined via IntegrityRequestPKI, and the signed hash value is returned as byte[]. 
 * verifyIntegrityPKI is the operation used to verify the integrity of received data that is protected through public key cryptography. A hash value of the received cleartext data is computed. This is compared to the received hash value, and the signature is validated. VerifyIntegrityRequestPKI defines the input to the operation. The operation returns a boolean value to express the validation result. 
 * verifyIntegritySymmetricKey is used to verify the integrity of data that is protected through symmetric key cryptography. A cryptographic hash value is computed from the received cleartext data. This value is compared to the received hash value. The operation’s input is defined via VerifyIntegritySymmetricKey. The result of the operation is returned via a boolean value.
 * @author jesus.gomez
 */
@WebService(serviceName = "Encryption", portName = "iEncryption", endpointInterface = "eu.mpower.framework.security.encryption.soap.IEncryption", targetNamespace = "http://soap.encryption.security.framework.mpower.eu", wsdlLocation = "WEB-INF/wsdl/Encryption/Encryption.wsdl")
public class Encryption implements IEncryption {

    @WebServiceRef(wsdlLocation = "WEB-INF/wsdl/client/PKI/PKI.wsdl")
    private PKI service;

    @WebServiceRef(wsdlLocation = "WEB-INF/wsdl/client/AuthorizationWServiceService/AuthorizationWServiceService.wsdl")
    private AuthorizationWServiceService service2;

    private static String digits = "0123456789abcdef";

    public eu.mpower.framework.security.encryption.soap.DecryptSymmetricKeyResponse decryptSymmetricKey(eu.mpower.framework.security.encryption.soap.DecryptSymmetricKeyRequest decryptSymmetricKeyRequest) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        eu.mpower.framework.security.encryption.soap.DecryptSymmetricKeyResponse response = new eu.mpower.framework.security.encryption.soap.DecryptSymmetricKeyResponse();
        Status status = new Status();
        try {
            eu.mpower.framework.security.authorization.soap.PortTypeAuthorizationWService port = service2.getAuthorizationWServicePort();
            java.lang.String serviceID = "Encryption";
            java.lang.String methodName = "decryptSymmetricKey";
            eu.mpower.framework.security.types.soap.OperationStatus result = port.isAuthorized(decryptSymmetricKeyRequest.getSecurityToken(), serviceID, methodName);
            if (!result.isBoolValue()) {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_ERROR_AUTHORIZATION_REQUIRED.value());
                response.setStatus(status);
                return response;
            }
            String algorithm = decryptSymmetricKeyRequest.getDecryptSymmetricKey().getAlgorithm().value();
            byte[] data = decryptSymmetricKeyRequest.getDecryptSymmetricKey().getEncryptedData();
            byte[] cipherText = new byte[data.length];
            byte[] keyBytes = null;
            byte[] sessionId = decryptSymmetricKeyRequest.getSecurityToken().getSessionID();
            if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.DES.value())) {
                keyBytes = new byte[8];
                if (sessionId.length > 8) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 8); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.AES.value())) {
                keyBytes = new byte[16];
                if (sessionId.length > 16) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 16); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.DE_SEDE.value())) {
                keyBytes = new byte[16];
                if (sessionId.length > 16) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 16); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_ERROR.name());
                status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_ERROR.ordinal());
                status.setTimestamp(java.lang.System.currentTimeMillis());
                response.setStatus(status);
                return response;
            }
            SecretKeySpec key = new SecretKeySpec(keyBytes, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm, "BC");
            cipher.init(Cipher.DECRYPT_MODE, key);
            cipherText = cipher.doFinal(data);
            response.setData(cipherText);
            status.setResult(1);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (IllegalBlockSizeException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (BadPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (InvalidKeyException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchAlgorithmException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchProviderException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.DECRYPT_SYMMETRIC_KEY_OK.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        }
        response.setStatus(status);
        return response;
    }

    public eu.mpower.framework.security.encryption.soap.DecryptPKIResponse decryptPKI(eu.mpower.framework.security.encryption.soap.DecryptPKIRequest decryptPKIRequest) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public eu.mpower.framework.security.encryption.soap.EncryptSymmetricKeyResponse encryptSymmetricKey(eu.mpower.framework.security.encryption.soap.EncryptSymmetricKeyRequest encryptSymmetricKeyRequest) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        eu.mpower.framework.security.encryption.soap.EncryptSymmetricKeyResponse response = new eu.mpower.framework.security.encryption.soap.EncryptSymmetricKeyResponse();
        Status status = new Status();
        try {
            eu.mpower.framework.security.authorization.soap.PortTypeAuthorizationWService port = service2.getAuthorizationWServicePort();
            java.lang.String serviceID = "Encryption";
            java.lang.String methodName = "encryptSymmetricKey";
            eu.mpower.framework.security.types.soap.OperationStatus result = port.isAuthorized(encryptSymmetricKeyRequest.getSecurityToken(), serviceID, methodName);
            if (!result.isBoolValue()) {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_ERROR_AUTHORIZATION_REQUIRED.value());
                response.setStatus(status);
                return response;
            }
            String algorithm = encryptSymmetricKeyRequest.getEncryptSymmetricKey().getAlgorithm().value();
            byte[] data = encryptSymmetricKeyRequest.getEncryptSymmetricKey().getData();
            byte[] cipherText = new byte[data.length];
            byte[] keyBytes = null;
            byte[] sessionId = encryptSymmetricKeyRequest.getSecurityToken().getSessionID();
            if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.DES.value())) {
                keyBytes = new byte[8];
                if (sessionId.length > 8) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 8); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.AES.value())) {
                keyBytes = new byte[16];
                if (sessionId.length > 16) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 16); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else if (algorithm.equals(eu.mpower.framework.security.encryption.soap.Algorithm.DE_SEDE.value())) {
                keyBytes = new byte[16];
                if (sessionId.length > 16) {
                    for (int i = 0; (i < keyBytes.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                } else {
                    for (int i = 0; (i < sessionId.length); i++) {
                        keyBytes[i] = sessionId[i];
                    }
                    for (int i = sessionId.length; (i < 16); i++) {
                        keyBytes[i] = 0;
                    }
                }
            } else {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_SYMMETRIC_KEY_ERROR.name());
                status.setMessageId(ErrorCodes.ENCRYPT_SYMMETRIC_KEY_ERROR.ordinal());
                status.setTimestamp(java.lang.System.currentTimeMillis());
                response.setStatus(status);
                return response;
            }
            SecretKeySpec key = new SecretKeySpec(keyBytes, algorithm);
            Cipher cipher = Cipher.getInstance(algorithm, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(data);
            response.setData(cipherText);
            status.setResult(1);
            status.setErrorCause(ErrorCodes.ENCRYPT_SYMMETRIC_KEY_OK.name());
            status.setMessageId(ErrorCodes.ENCRYPT_SYMMETRIC_KEY_OK.ordinal());
        } catch (IllegalBlockSizeException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.ILLEGAL_BLOCK_SIZE.name());
            status.setMessageId(ErrorCodes.ILLEGAL_BLOCK_SIZE.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (BadPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.BAD_PADDING.name());
            status.setMessageId(ErrorCodes.BAD_PADDING.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (InvalidKeyException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.INVALID_KEY.name());
            status.setMessageId(ErrorCodes.INVALID_KEY.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchAlgorithmException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.NO_SUCH_ALGORITHM.name());
            status.setMessageId(ErrorCodes.NO_SUCH_ALGORITHM.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchProviderException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.NO_SUCH_PROVIDER.name());
            status.setMessageId(ErrorCodes.NO_SUCH_PROVIDER.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        } catch (NoSuchPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.NO_SUCH_PADDING.name());
            status.setMessageId(ErrorCodes.NO_SUCH_PADDING.ordinal());
            status.setTimestamp(java.lang.System.currentTimeMillis());
        }
        status.setTimestamp(java.lang.System.currentTimeMillis());
        response.setStatus(status);
        return response;
    }

    public eu.mpower.framework.security.encryption.soap.EncryptPKIResponse encryptPKI(eu.mpower.framework.security.encryption.soap.EncryptPKIRequest encryptPKIRequest) {
        eu.mpower.framework.security.encryption.soap.EncryptPKIResponse response = new eu.mpower.framework.security.encryption.soap.EncryptPKIResponse();
        Status status = new Status();
        try {
            eu.mpower.framework.security.authorization.soap.PortTypeAuthorizationWService port2 = service2.getAuthorizationWServicePort();
            java.lang.String serviceID = "Encryption";
            java.lang.String methodName = "encryptPKI";
            eu.mpower.framework.security.types.soap.OperationStatus result2 = port2.isAuthorized(encryptPKIRequest.getSecurityToken(), serviceID, methodName);
            if (!result2.isBoolValue()) {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_ERROR_AUTHORIZATION_REQUIRED.value());
                response.setStatus(status);
                return response;
            }
            String serialNumber = encryptPKIRequest.getEncryptPKIRequest().getSerialNumber();
            eu.mpower.framework.security.pki.soap.IPKI port = service.getIPKI();
            eu.mpower.framework.security.pki.soap.GetCertificateRequest getCertificateRequest = new eu.mpower.framework.security.pki.soap.GetCertificateRequest();
            getCertificateRequest.setSerialNumber(serialNumber);
            getCertificateRequest.setSecurityToken(encryptPKIRequest.getSecurityToken());
            eu.mpower.framework.security.pki.soap.GetCertificateResponse result = port.getCertificate(getCertificateRequest);
            if (result != null && result.getCertificate() != null && result.getStatus().getResult() == 1) {
                java.security.cert.Certificate cert = null;
                CertificateFactory certf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bais = new ByteArrayInputStream(result.getCertificate());
                cert = certf.generateCertificate(bais);
                Cipher cipher = Cipher.getInstance(Algorithm.RSA.value());
                cipher.init(Cipher.ENCRYPT_MODE, cert.getPublicKey());
                byte[] cipherText = cipher.doFinal(encryptPKIRequest.getEncryptPKIRequest().getData());
                response.setData(cipherText);
                status.setResult(1);
                status.setErrorCause(ErrorCodes.ENCRYPT_PKI_OK.name());
                status.setMessageId(ErrorCodes.ENCRYPT_PKI_OK.ordinal());
            } else {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_PKI_ERROR.name());
                status.setMessageId(ErrorCodes.ENCRYPT_PKI_ERROR.ordinal());
            }
        } catch (IllegalBlockSizeException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.ILLEGAL_BLOCK_SIZE.name());
            status.setMessageId(ErrorCodes.ILLEGAL_BLOCK_SIZE.ordinal());
        } catch (BadPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.BAD_PADDING.name());
            status.setMessageId(ErrorCodes.BAD_PADDING.ordinal());
        } catch (InvalidKeyException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.INVALID_KEY.name());
            status.setMessageId(ErrorCodes.INVALID_KEY.ordinal());
        } catch (NoSuchAlgorithmException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.NO_SUCH_ALGORITHM.name());
            status.setMessageId(ErrorCodes.NO_SUCH_ALGORITHM.ordinal());
        } catch (NoSuchPaddingException ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.NO_SUCH_PADDING.name());
            status.setMessageId(ErrorCodes.NO_SUCH_PADDING.ordinal());
        } catch (Exception ex) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.ENCRYPT_PKI_ERROR.name());
            status.setMessageId(ErrorCodes.ENCRYPT_PKI_ERROR.ordinal());
        }
        status.setTimestamp(java.lang.System.currentTimeMillis());
        response.setStatus(status);
        return response;
    }

    public eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashResponse generateCryptographicHash(eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashRequest generateCryptographicHashRequest) {
        eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashResponse response = new eu.mpower.framework.security.encryption.soap.GenerateCryptographicHashResponse();
        Status status = new Status();
        try {
            eu.mpower.framework.security.authorization.soap.PortTypeAuthorizationWService port2 = service2.getAuthorizationWServicePort();
            java.lang.String serviceID = "Encryption";
            java.lang.String methodName = "generateCryptographicHash";
            eu.mpower.framework.security.types.soap.OperationStatus result2 = port2.isAuthorized(generateCryptographicHashRequest.getSecurityToken(), serviceID, methodName);
            if (!result2.isBoolValue()) {
                status.setResult(0);
                status.setErrorCause(ErrorCodes.ENCRYPT_ERROR_AUTHORIZATION_REQUIRED.value());
                response.setStatus(status);
                return response;
            }
            eu.mpower.framework.security.encryption.soap.MessageDigest algorithm = generateCryptographicHashRequest.getGenerateCryptographicHash().getAlgorithm();
            java.security.MessageDigest messageDigest = java.security.MessageDigest.getInstance(algorithm.value());
            messageDigest.update(generateCryptographicHashRequest.getGenerateCryptographicHash().getData());
            response.setData(messageDigest.digest());
            status.setResult(1);
            status.setMessageId(ErrorCodes.GENERATE_SIGNED_HASH_OK.ordinal());
            status.setErrorCause(ErrorCodes.GENERATE_SIGNED_HASH_OK.value());
        } catch (NoSuchAlgorithmException ex) {
            status.setResult(0);
            status.setMessageId(ErrorCodes.GENERATE_SIGNED_HASH_ERROR.ordinal());
            status.setErrorCause(ErrorCodes.GENERATE_SIGNED_HASH_ERROR.value());
        }
        status.setTimestamp(System.currentTimeMillis());
        response.setStatus(status);
        return response;
    }

    public eu.mpower.framework.security.encryption.soap.GenerateSignedHashResponse generateSignedHash(eu.mpower.framework.security.encryption.soap.GenerateSignedHashRequest generateSignedHashRequest) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public eu.mpower.framework.security.encryption.soap.VerifyIntegrityPKIResponse verifyIntegrityPKI(eu.mpower.framework.security.encryption.soap.VerifyIntegrityPKIRequest verifyIntegrityPKIRequest) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public eu.mpower.framework.security.encryption.soap.VerifyIntegritySymmetricKeyResponse verifyIntegritySymmetricKey(eu.mpower.framework.security.encryption.soap.VerifyIntegritySymmetricKeyRequest verifyIntegritySymmetricKeyRequest) {
        eu.mpower.framework.security.encryption.soap.VerifyIntegritySymmetricKeyResponse response = new eu.mpower.framework.security.encryption.soap.VerifyIntegritySymmetricKeyResponse();
        Status status = new Status();
        eu.mpower.framework.security.authorization.soap.PortTypeAuthorizationWService port2 = service2.getAuthorizationWServicePort();
        java.lang.String serviceID = "Encryption";
        java.lang.String methodName = "verifyIntegritySymmetricKey";
        eu.mpower.framework.security.types.soap.OperationStatus result2 = port2.isAuthorized(verifyIntegritySymmetricKeyRequest.getSecurityToken(), serviceID, methodName);
        if (!result2.isBoolValue()) {
            status.setResult(0);
            status.setErrorCause(ErrorCodes.ENCRYPT_ERROR_AUTHORIZATION_REQUIRED.value());
            response.setStatus(status);
            return response;
        }
        GenerateCryptographicHashRequest req = new GenerateCryptographicHashRequest();
        IntegrityRequestSymmetricKey integrity = new IntegrityRequestSymmetricKey();
        integrity.setAlgorithm(verifyIntegritySymmetricKeyRequest.getVerifyIntegritySymmetricKey().getAlgorithm());
        integrity.setData(verifyIntegritySymmetricKeyRequest.getVerifyIntegritySymmetricKey().getData());
        req.setGenerateCryptographicHash(integrity);
        req.setSecurityToken(verifyIntegritySymmetricKeyRequest.getSecurityToken());
        GenerateCryptographicHashResponse resp = generateCryptographicHash(req);
        if (resp.getStatus().getResult() == 1) {
            if (resp.getData() == verifyIntegritySymmetricKeyRequest.getVerifyIntegritySymmetricKey().getHashData()) {
                response.setIsVerified(true);
            } else {
                response.setIsVerified(false);
            }
            status.setResult(1);
            status.setMessageId(ErrorCodes.GENERATE_SIGNED_HASH_OK.ordinal());
            status.setErrorCause(ErrorCodes.GENERATE_SIGNED_HASH_OK.value());
        } else {
            status = resp.getStatus();
        }
        status.setTimestamp(System.currentTimeMillis());
        response.setStatus(status);
        return response;
    }
}