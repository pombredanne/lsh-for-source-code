package ch.heuscher.simple.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import ch.heuscher.simple.IntegrityMarker;
import ch.heuscher.simple.SimpleConstants;

public class HashingUtil {

    private HashingUtil() {
        super();
    }

    public static String[] calculateHashes(InputStream inputStream, String[] hashMethods) throws NoSuchAlgorithmException, IOException {
        MessageDigest[] messageDigests = new MessageDigest[hashMethods.length];
        for (int i = 0; i < hashMethods.length; i++) {
            messageDigests[i] = MessageDigest.getInstance(hashMethods[i]);
        }
        long lFileOctetLength = -1;
        lFileOctetLength = HashingUtil.calculateHashes(inputStream, messageDigests);
        inputStream.close();
        String[] hashValues = new String[hashMethods.length];
        for (int i = 0; i < messageDigests.length; i++) {
            hashValues[i] = HashingUtil.convertHashToString(messageDigests[i]);
        }
        return hashValues;
    }

    public static String convertHashToString(MessageDigest messageDigest) {
        try {
            byte[] bHash = cloneMessageDigest(messageDigest).digest();
            StringBuffer stringBuffer = new StringBuffer();
            for (int j = 0; j < bHash.length; j++) {
                stringBuffer.append(Integer.toHexString((bHash[j] >> 4) & 0x0f));
                stringBuffer.append(Integer.toHexString(bHash[j] & 0x0f));
            }
            return stringBuffer.toString();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static long calculateHashes(InputStream inputStream, MessageDigest[] messageDigests) throws NoSuchAlgorithmException, IOException {
        long lTotalStreamLength = 0;
        int nBytesRead = 0;
        byte[] bBuffer = new byte[4000];
        while ((nBytesRead = inputStream.read(bBuffer)) > 0) {
            lTotalStreamLength += nBytesRead;
            for (int j = 0; j < messageDigests.length; j++) {
                messageDigests[j].update(bBuffer, 0, nBytesRead);
            }
        }
        return lTotalStreamLength;
    }

    public static String getDummyString(String sDigestName) throws NoSuchAlgorithmException {
        int nDigestLength = 2 * MessageDigest.getInstance(sDigestName).getDigestLength();
        StringBuffer dummyHashStringBuffer = new StringBuffer();
        for (int i = 0; i < nDigestLength; i++) {
            dummyHashStringBuffer.append(SimpleConstants.DUMMYHASH_CHAR);
        }
        return dummyHashStringBuffer.toString();
    }

    public static MessageDigest[] createMessageDigests(IntegrityMarker[] integrityMarkers) throws NoSuchAlgorithmException {
        if (integrityMarkers == null) {
            return null;
        }
        ArrayList messageDigests = new ArrayList();
        for (int i = 0; i < integrityMarkers.length; i++) {
            String hashFunction = integrityMarkers[i].getHashFunction();
            messageDigests.add(MessageDigest.getInstance(hashFunction));
        }
        return (MessageDigest[]) messageDigests.toArray(new MessageDigest[0]);
    }

    public static MessageDigest[] cloneMessageDigests(MessageDigest[] digestsToBeCloned) throws CloneNotSupportedException {
        MessageDigest[] clonedDigests = new MessageDigest[digestsToBeCloned.length];
        for (int i = 0; i < digestsToBeCloned.length; i++) {
            clonedDigests[i] = cloneMessageDigest(digestsToBeCloned[i]);
        }
        return clonedDigests;
    }

    public static MessageDigest cloneMessageDigest(MessageDigest digestToBeCloned) throws CloneNotSupportedException {
        return (MessageDigest) digestToBeCloned.clone();
    }
}
