package com.columboid.protocol.syncml.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import com.columboid.protocol.syncml.CredentialType;

public class EncryptionHelper {

    public static String GetHashString(String userName, String hashedPassword, String nonceToken, CredentialType credentialType) throws NoSuchAlgorithmException {
        return GetHashString(userName + hashedPassword + nonceToken, credentialType);
    }

    private static String GetHashString(String data, CredentialType credentialType) throws NoSuchAlgorithmException {
        byte[] defaultBytes = data.getBytes();
        MessageDigest algorithm = MessageDigest.getInstance(credentialType.toString());
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        data = GetStringFromArray(messageDigest);
        return data;
    }

    private static String GetStringFromArray(byte[] array) {
        String data = "";
        data = Base64.encodeBase64String(array).replaceAll("\r\n", "");
        return data;
    }
}
