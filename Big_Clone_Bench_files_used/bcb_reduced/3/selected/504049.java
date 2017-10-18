package com.ericdaugherty.mail.server.auth;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.codec.binary.Base64;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager.DomainWithPassword;
import com.ericdaugherty.mail.server.errors.*;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.services.general.DeliveryService;
import com.ericdaugherty.mail.server.utils.ByteUtils;

/**
 * Verify client authentication using SASL DIGEST-MD5. Possibly protect
 * the data stream using integrity/privacy wrapping.
 *
 * @author Andreas Kyrmegalos
 */
public class DigestMd5ServerMode implements AuthServerMode, SaslServer {

    /** Logger Category for this class. */
    private static Log log = LogFactory.getLog(DigestMd5ServerMode.class);

    /** The ConfigurationManager */
    private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

    /** The IP address of the client */
    private String clientIp;

    private Map<String, List<Integer>> nonces = new HashMap();

    private Locale locale = Locale.ENGLISH;

    private FinalizeAuthentication finalizeAuthentication;

    private User user;

    private boolean completed, integrity, privacy, domainNeeded, userMBLocked;

    private String sessionCipher;

    private String encoding = "ISO-8859-1";

    private byte[] MD5DigestSessionKey;

    private Wrapper wrapper;

    private boolean firstEvaluation = true;

    public DigestMd5ServerMode(boolean isSMTPSession) {
        if (isSMTPSession) {
            finalizeAuthentication = new FinalizeAuthenticationSMTP();
        } else {
            finalizeAuthentication = new FinalizeAuthenticationPOP3();
        }
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    private String responseTokenProcessor(List<byte[]> splittedBytes, Map<String, byte[]> directives, String key, int position, int tokenCountMinus1) throws SaslException, UnsupportedEncodingException {
        byte[] temp = splittedBytes.get(position);
        byte[] value;
        int valueStart, valueFinish;
        if (directives.containsKey(key)) {
            throw new SaslException("Digest-Response can not contain multiple identical keys");
        }
        int lastCommaPos = byteArrayLastIndexOf(temp, 0x2c);
        if (lastCommaPos == -1) {
            if (position != tokenCountMinus1) {
                throw new SaslException("Error encountered while parsing Digest-Response content");
            }
            lastCommaPos = temp.length;
        }
        if (lastCommaPos < byteArrayLastIndexOf(temp, 0x22) && position != tokenCountMinus1) {
            throw new SaslException("Inappropriate Digest-Response format");
        }
        valueStart = firstNonLWS(temp);
        valueFinish = lastNonLWS(temp, lastCommaPos - 1);
        if (isImproperlyQuoted(temp, valueStart, valueFinish)) {
            throw new SaslException("Inappropriate Digest-Response format");
        }
        if (temp[valueStart] == 0x22) {
            valueStart++;
            valueFinish--;
        }
        value = new byte[valueFinish - valueStart + 1];
        System.arraycopy(temp, valueStart, value, 0, valueFinish - valueStart + 1);
        directives.put(key, value);
        return position == tokenCountMinus1 ? "" : new String(temp, lastCommaPos + 1, temp.length - lastCommaPos - 1, "US-ASCII").toLowerCase(locale).trim();
    }

    private boolean isImproperlyQuoted(byte[] target, int first, int last) {
        if ((target[first] != 0x22 && target[last] == 0x22) || (target[first] == 0x22 && target[last] != 0x22)) return true;
        return false;
    }

    private boolean isLWS(int target) {
        if (target == 0x0d || target == 0x0a || target == 0x20 || target == 0x09) return true;
        return false;
    }

    private int firstNonLWS(byte[] array) {
        int length = array.length;
        for (int i = 0; i < length; i++) {
            if (!isLWS(array[i])) return i;
        }
        return 0;
    }

    private int lastNonLWS(byte[] array, int startAt) {
        for (int i = startAt; i >= 0; i--) {
            if (!isLWS(array[i])) return i;
        }
        return startAt;
    }

    private int byteArrayLastIndexOf(byte[] array, int target) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] == target) return i;
        }
        return -1;
    }

    private List<byte[]> splitByteArray(byte[] array, byte splitter) {
        List<byte[]> splitted = new ArrayList();
        byte[] current;
        int starting = 0, length = array.length;
        boolean comma = false, first = true;
        for (int i = starting; i < length; i++) {
            if (i == length - 1) {
                current = new byte[i - starting + 1];
                System.arraycopy(array, starting, current, 0, i - starting + 1);
                splitted.add(current);
            } else if (array[i] == splitter) {
                if (!comma && !first) continue;
                current = new byte[i - starting];
                System.arraycopy(array, starting, current, 0, i - starting);
                splitted.add(current);
                i++;
                starting = i;
                comma = first = false;
            }
            if (array[i] == ',') comma = true;
        }
        return splitted;
    }

    private void decodeMixed(Map rawDirectives) throws UnsupportedEncodingException {
        List<String> keys = new ArrayList(rawDirectives.keySet());
        Iterator<String> iter = keys.iterator();
        String key, stringValue;
        byte[] value;
        while (iter.hasNext()) {
            key = iter.next();
            value = (byte[]) rawDirectives.remove(key);
            if (key.equals("realm") || key.equals("username") || key.equals("authzid")) {
                stringValue = new String(value, "UTF-8");
            } else {
                stringValue = new String(value, "ISO-8859-1");
            }
            rawDirectives.put(key, stringValue);
        }
    }

    private void decodeAllAs8859(Map rawDirectives) throws UnsupportedEncodingException {
        List<String> keys = new ArrayList(rawDirectives.keySet());
        Iterator<String> iter = keys.iterator();
        String key, stringValue;
        byte[] value;
        while (iter.hasNext()) {
            key = iter.next();
            value = (byte[]) rawDirectives.remove(key);
            if (key.equals("authzid")) {
                stringValue = new String(value, "UTF-8");
            } else {
                stringValue = new String(value, "ISO-8859-1");
            }
            rawDirectives.put(key, stringValue);
        }
    }

    public byte[] evaluateResponse(byte[] responseBytes) throws SaslException {
        if (firstEvaluation) {
            firstEvaluation = false;
            StringBuilder challenge = new StringBuilder(100);
            Iterator iter = configurationManager.getRealms().values().iterator();
            Realm aRealm;
            while (iter.hasNext()) {
                aRealm = (Realm) iter.next();
                if (aRealm.getFullRealmName().equals("null")) continue;
                challenge.append("realm=\"" + aRealm.getFullRealmName() + "\"");
                challenge.append(",");
            }
            String nonceUUID = UUID.randomUUID().toString();
            String nonce = null;
            try {
                nonce = new String(Base64.encodeBase64(MD5Digest(String.valueOf(System.nanoTime() + ":" + nonceUUID))), "US-ASCII");
            } catch (UnsupportedEncodingException uee) {
                throw new SaslException(uee.getMessage(), uee);
            } catch (GeneralSecurityException uee) {
                throw new SaslException(uee.getMessage(), uee);
            }
            nonces.put(nonce, new ArrayList());
            nonces.get(nonce).add(Integer.valueOf(1));
            challenge.append("nonce=\"" + nonce + "\"");
            challenge.append(",");
            challenge.append("qop=\"" + configurationManager.getSaslQOP() + "\"");
            challenge.append(",");
            challenge.append("charset=\"utf-8\"");
            challenge.append(",");
            challenge.append("algorithm=\"md5-sess\"");
            if (configurationManager.getSaslQOP().indexOf("auth-conf") != -1) {
                challenge.append(",");
                challenge.append("cipher-opts=\"" + configurationManager.getDigestMD5Ciphers() + "\"");
            }
            try {
                return Base64.encodeBase64(challenge.toString().getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException uee) {
                throw new SaslException(uee.getMessage(), uee);
            }
        } else {
            String nonce = null;
            if (!Base64.isArrayByteBase64(responseBytes)) {
                throw new SaslException("Can not decode Base64 Content", new MalformedBase64ContentException());
            }
            responseBytes = Base64.decodeBase64(responseBytes);
            List<byte[]> splittedBytes = splitByteArray(responseBytes, (byte) 0x3d);
            int tokenCountMinus1 = splittedBytes.size() - 1, lastCommaPos;
            Map rawDirectives = new HashMap();
            String key = null;
            Map<String, String> directives;
            try {
                key = new String(splittedBytes.get(0), "US-ASCII");
                for (int i = 1; i < tokenCountMinus1; i++) {
                    key = responseTokenProcessor(splittedBytes, rawDirectives, key, i, tokenCountMinus1);
                }
                responseTokenProcessor(splittedBytes, rawDirectives, key, tokenCountMinus1, tokenCountMinus1);
                if (rawDirectives.containsKey("charset")) {
                    String value = new String((byte[]) rawDirectives.get("charset"), "US-ASCII").toLowerCase(locale);
                    if (value.equals("utf-8")) {
                        encoding = "UTF-8";
                    }
                }
                if (encoding.equals("ISO-8859-1")) {
                    decodeAllAs8859(rawDirectives);
                } else {
                    decodeMixed(rawDirectives);
                }
                directives = rawDirectives;
            } catch (UnsupportedEncodingException uee) {
                throw new SaslException(uee.getMessage());
            }
            if (!directives.containsKey("username") || !directives.containsKey("nonce") || !directives.containsKey("nc") || !directives.containsKey("cnonce") || !directives.containsKey("response")) {
                throw new SaslException("Digest-Response lacks at least one neccesery key-value pair");
            }
            if (directives.get("username").indexOf('@') != -1) {
                throw new SaslException("digest-response username field must not include domain name", new AuthenticationException());
            }
            if (!directives.containsKey("qop")) {
                directives.put("qop", QOP_AUTH);
            }
            if (!directives.containsKey("realm") || ((String) directives.get("realm")).equals("")) {
                directives.put("realm", "null");
            }
            nonce = (String) directives.get("nonce");
            if (!nonces.containsKey(nonce)) {
                throw new SaslException("Illegal nonce value");
            }
            List<Integer> nonceListInMap = nonces.get(nonce);
            int nc = Integer.parseInt((String) directives.get("nc"), 16);
            if (nonceListInMap.get(nonceListInMap.size() - 1).equals(Integer.valueOf(nc))) {
                nonceListInMap.add(Integer.valueOf(++nc));
            } else {
                throw new SaslException("Illegal nc value");
            }
            nonceListInMap = null;
            if (directives.get("qop").equals(QOP_AUTH_INT)) integrity = true; else if (directives.get("qop").equals(QOP_AUTH_CONF)) privacy = true;
            if (privacy) {
                if (!directives.containsKey("cipher")) {
                    throw new SaslException("Message confidentially required but cipher entry is missing");
                }
                sessionCipher = directives.get("cipher").toLowerCase(locale);
                if ("3des,des,rc4-40,rc4,rc4-56".indexOf(sessionCipher) == -1) {
                    throw new SaslException("Unsupported cipher for message confidentiality");
                }
            }
            String realm = directives.get("realm").toLowerCase(Locale.getDefault());
            String username = directives.get("username").toLowerCase(locale);
            if (username.indexOf('@') == -1) {
                if (!directives.get("realm").equals("null")) {
                    username += directives.get("realm").substring(directives.get("realm").indexOf('@'));
                } else if (directives.get("authzid").indexOf('@') != -1) {
                    username += directives.get("authzid").substring(directives.get("authzid").indexOf('@'));
                }
            }
            DomainWithPassword domainWithPassword = configurationManager.getRealmPassword(realm, username);
            if (domainWithPassword == null || domainWithPassword.getPassword() == null) {
                log.warn("The supplied username and/or realm do(es) not match a registered entry");
                return null;
            }
            if (realm.equals("null") && username.indexOf('@') == -1) {
                username += "@" + domainWithPassword.getDomain();
            }
            byte[] HA1 = toByteArray(domainWithPassword.getPassword());
            for (int i = domainWithPassword.getPassword().length - 1; i >= 0; i--) {
                domainWithPassword.getPassword()[i] = 0xff;
            }
            domainWithPassword = null;
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (GeneralSecurityException gse) {
                throw new SaslException(gse.getMessage());
            }
            md.update(HA1);
            md.update(":".getBytes());
            md.update((directives.get("nonce")).getBytes());
            md.update(":".getBytes());
            md.update((directives.get("cnonce")).getBytes());
            if (directives.containsKey("authzid")) {
                md.update(":".getBytes());
                md.update((directives.get("authzid")).getBytes());
            }
            MD5DigestSessionKey = HA1 = md.digest();
            String MD5DigestSessionKeyToHex = toHex(HA1, HA1.length);
            md.update("AUTHENTICATE".getBytes());
            md.update(":".getBytes());
            md.update((directives.get("digest-uri")).getBytes());
            if (!directives.get("qop").equals(QOP_AUTH)) {
                md.update(":".getBytes());
                md.update("00000000000000000000000000000000".getBytes());
            }
            byte[] HA2 = md.digest();
            String HA2HEX = toHex(HA2, HA2.length);
            md.update(MD5DigestSessionKeyToHex.getBytes());
            md.update(":".getBytes());
            md.update((directives.get("nonce")).getBytes());
            md.update(":".getBytes());
            md.update((directives.get("nc")).getBytes());
            md.update(":".getBytes());
            md.update((directives.get("cnonce")).getBytes());
            md.update(":".getBytes());
            md.update((directives.get("qop")).getBytes());
            md.update(":".getBytes());
            md.update(HA2HEX.getBytes());
            byte[] responseHash = md.digest();
            String HexResponseHash = toHex(responseHash, responseHash.length);
            if (HexResponseHash.equals(directives.get("response"))) {
                md.update(":".getBytes());
                md.update((directives.get("digest-uri")).getBytes());
                if (!directives.get("qop").equals(QOP_AUTH)) {
                    md.update(":".getBytes());
                    md.update("00000000000000000000000000000000".getBytes());
                }
                HA2 = md.digest();
                HA2HEX = toHex(HA2, HA2.length);
                md.update(MD5DigestSessionKeyToHex.getBytes());
                md.update(":".getBytes());
                md.update((directives.get("nonce")).getBytes());
                md.update(":".getBytes());
                md.update((directives.get("nc")).getBytes());
                md.update(":".getBytes());
                md.update((directives.get("cnonce")).getBytes());
                md.update(":".getBytes());
                md.update((directives.get("qop")).getBytes());
                md.update(":".getBytes());
                md.update(HA2HEX.getBytes());
                responseHash = md.digest();
                return finalizeAuthentication.finalize(responseHash, username);
            } else {
                log.warn("Improper credentials");
                return null;
            }
        }
    }

    public boolean isDomainNeeded() {
        return domainNeeded;
    }

    public boolean isUserMBLocked() {
        return userMBLocked;
    }

    private abstract class FinalizeAuthentication {

        public abstract byte[] finalize(byte[] responseHash, String username) throws SaslException;
    }

    private class FinalizeAuthenticationSMTP extends FinalizeAuthentication {

        public byte[] finalize(byte[] responseHash, String username) throws SaslException {
            completed = true;
            if (integrity) {
                wrapper = new Wrapper();
            } else if (privacy) {
                wrapper = new EncryptedWrapper();
            }
            if (log.isInfoEnabled()) log.info("User: " + username + " logged in successfully.");
            try {
                return Base64.encodeBase64(("rspauth=" + toHex(responseHash, responseHash.length)).getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException uee) {
                completed = false;
                throw new SaslException(uee.getMessage());
            }
        }
    }

    private class FinalizeAuthenticationPOP3 extends FinalizeAuthentication {

        public byte[] finalize(byte[] responseHash, String username) throws SaslException {
            DeliveryService deliveryService = DeliveryService.getDeliveryService();
            int index = username.indexOf("@");
            if (index == -1) {
                domainNeeded = true;
                return null;
            } else {
                EmailAddress aUserAddress = null;
                try {
                    aUserAddress = new EmailAddress(username.substring(0, index), username.substring(index + 1));
                } catch (InvalidAddressException iae) {
                    throw new SaslException(iae.getMessage());
                }
                user = configurationManager.getUser(aUserAddress);
                if (deliveryService.isMailboxLocked(aUserAddress)) {
                    userMBLocked = true;
                    user = null;
                } else {
                    deliveryService.ipAuthenticated(clientIp);
                    deliveryService.lockMailbox(aUserAddress);
                    if (log.isInfoEnabled()) log.info("User: " + username + " logged in successfully.");
                    completed = true;
                    if (integrity) {
                        wrapper = new Wrapper();
                    } else if (privacy) {
                        wrapper = new EncryptedWrapper();
                    }
                    try {
                        return Base64.encodeBase64(("rspauth=" + toHex(responseHash, responseHash.length)).getBytes("US-ASCII"));
                    } catch (UnsupportedEncodingException uee) {
                        completed = false;
                        throw new SaslException(uee.getMessage());
                    }
                }
                return null;
            }
        }
    }

    public boolean isProtected() {
        return integrity || privacy;
    }

    public User getUser() {
        return user;
    }

    public void conclude() {
        clientIp = null;
        Iterator<List<Integer>> noncesEntries = nonces.values().iterator();
        while (noncesEntries.hasNext()) {
            noncesEntries.next().clear();
        }
        nonces.clear();
        nonces = null;
        finalizeAuthentication = null;
        user = null;
    }

    private byte[] toByteArray(char[] array) {
        int length = array.length;
        byte[] number = new byte[length / 2];
        int i, j, radix = 16;
        for (i = 0; i < length; i += 2) {
            j = java.lang.Character.digit(array[i], radix) * radix + java.lang.Character.digit(array[i + 1], radix);
            number[i / 2] = (byte) (j & 0xff);
        }
        return number;
    }

    private byte[] MD5Digest(String input) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes());
        return md.digest();
    }

    private String toHex(byte[] b, int len) {
        if (b == null) return "";
        StringBuilder s = new StringBuilder("");
        int i;
        for (i = 0; i < len; i++) s.append(toHex(b[i]));
        return s.toString();
    }

    private String toHex(byte b) {
        Integer I = new Integer((((int) b) << 24) >>> 24);
        int i = I.intValue();
        if (i < (byte) 16) return "0" + Integer.toString(i, 16); else return Integer.toString(i, 16);
    }

    private static final String QOP_AUTH = "auth";

    private static final String QOP_AUTH_INT = "auth-int";

    private static final String QOP_AUTH_CONF = "auth-conf";

    private static final String[] CIPHERSUITS = { "rc4-40", "rc4-56", "rc4", "des", "3des" };

    private static final int RC4_40 = 0;

    private static final int RC4_56 = 1;

    private static final int RC4 = 2;

    private static final int DES = 3;

    private static final int DES3 = 4;

    private static final byte[] EMPTY = new byte[0];

    public String getMechanismName() {
        return "DIGEST-MD5";
    }

    public byte[] unwrap(byte[] incoming, int offset, int length) throws SaslException {
        if (wrapper == null) {
            throw new SaslException("Neither integrity nor privacy was negotiated");
        }
        if (!completed) {
            throw new SaslException("Authentication not completed");
        }
        return (wrapper.unwrap(incoming, offset, length));
    }

    public byte[] wrap(byte[] outgoing, int offset, int length) throws SaslException {
        if (wrapper == null) {
            throw new SaslException("Neither integrity nor privacy was negotiated");
        }
        if (!completed) {
            throw new SaslException("DIGEST-MD5 authentication not completed");
        }
        return (wrapper.wrap(outgoing, offset, length));
    }

    public void dispose() throws SaslException {
        wrapper = null;
    }

    public Object getNegotiatedProperty(String propName) {
        return null;
    }

    public String getAuthorizationID() {
        return null;
    }

    public boolean isComplete() {
        return completed;
    }

    private class Wrapper {

        protected byte[] kis, kic;

        protected int serverSeqNum, clientSeqNum;

        protected final byte[] sequenceNum = new byte[4];

        protected final byte[] messageTypeNBO = new byte[2];

        private Wrapper() throws SaslException {
            try {
                byte[] serverSalt = "Digest session key to server-to-client signing key magic constant".getBytes("ISO-8859-1");
                byte[] clientSalt = "Digest session key to client-to-server signing key magic constant".getBytes("ISO-8859-1");
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] temp = new byte[MD5DigestSessionKey.length + clientSalt.length];
                System.arraycopy(MD5DigestSessionKey, 0, temp, 0, MD5DigestSessionKey.length);
                System.arraycopy(serverSalt, 0, temp, MD5DigestSessionKey.length, serverSalt.length);
                kis = md5.digest(temp);
                System.arraycopy(clientSalt, 0, temp, MD5DigestSessionKey.length, clientSalt.length);
                kic = md5.digest(temp);
                ByteUtils.getNetworkByteOrderFromInt(1, messageTypeNBO, 0, 2);
            } catch (UnsupportedEncodingException e) {
                throw new SaslException(e.getMessage(), e);
            } catch (GeneralSecurityException e) {
                throw new SaslException(e.getMessage(), e);
            }
        }

        public byte[] wrap(byte[] outgoing, int offset, int length) throws SaslException {
            if (length == 0) {
                return EMPTY;
            }
            byte[] wrapped = new byte[length + 16];
            System.arraycopy(outgoing, offset, wrapped, 0, length);
            ByteUtils.getNetworkByteOrderFromInt(serverSeqNum++, sequenceNum, 0, 4);
            byte[] mac = computeHMAC(kis, sequenceNum, outgoing, offset, length);
            System.arraycopy(mac, 0, wrapped, length, 10);
            System.arraycopy(messageTypeNBO, 0, wrapped, length + 10, 2);
            System.arraycopy(sequenceNum, 0, wrapped, length + 12, 4);
            return wrapped;
        }

        public byte[] unwrap(byte[] incoming, int offset, int length) throws SaslException {
            if (length == 0) {
                return EMPTY;
            }
            int messageSize = length - 16;
            byte[] message = new byte[messageSize];
            byte[] seqNum = new byte[4];
            System.arraycopy(incoming, offset, message, 0, messageSize);
            System.arraycopy(incoming, offset + messageSize + 12, seqNum, 0, 4);
            int messageType = ByteUtils.getIntegerFromNetworkByteOrder(incoming, offset + messageSize + 10, 2);
            if (messageType != 1) {
                throw new SaslException("Invalid message type: " + messageType);
            }
            int clientSeqNum = ByteUtils.getIntegerFromNetworkByteOrder(seqNum, 0, 4);
            if (clientSeqNum != this.clientSeqNum) {
                throw new SaslException("A message segment was received out of order. Expected: " + this.clientSeqNum + " Received: " + clientSeqNum);
            }
            this.clientSeqNum++;
            byte[] mac = new byte[10];
            System.arraycopy(incoming, offset + messageSize, mac, 0, 10);
            byte[] expectedMac = computeHMAC(kic, seqNum, message, 0, messageSize);
            if (!Arrays.equals(mac, expectedMac)) {
                return EMPTY;
            }
            return message;
        }

        protected byte[] computeHMAC(byte[] kic, byte[] seqNum, byte[] message, int offset, int length) throws SaslException {
            byte[] completeMessage = new byte[4 + length];
            System.arraycopy(seqNum, 0, completeMessage, 0, 4);
            System.arraycopy(message, offset, completeMessage, 4, length);
            try {
                SecretKey key = new SecretKeySpec(kic, "HmacMD5");
                Mac m = Mac.getInstance("HmacMD5");
                m.init(key);
                byte[] hmac = m.doFinal(completeMessage);
                byte mac[] = new byte[10];
                System.arraycopy(hmac, 0, mac, 0, 10);
                return mac;
            } catch (InvalidKeyException e) {
                throw new SaslException(e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException(e.getMessage(), e);
            }
        }
    }

    private final class EncryptedWrapper extends Wrapper {

        private Cipher encodingCipher, decodingCipher;

        private EncryptedWrapper() throws SaslException {
            super();
            try {
                byte[] serverSalt = "Digest H(A1) to server-to-client sealing key magic constant".getBytes("ISO-8859-1");
                byte[] clientSalt = "Digest H(A1) to client-to-server sealing key magic constant".getBytes("ISO-8859-1");
                int n;
                if (sessionCipher.equals(CIPHERSUITS[RC4_40])) {
                    n = 5;
                } else if (sessionCipher.equals(CIPHERSUITS[RC4_56])) {
                    n = 7;
                } else {
                    n = 16;
                }
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                byte[] temp = new byte[n + serverSalt.length];
                System.arraycopy(MD5DigestSessionKey, 0, temp, 0, n);
                System.arraycopy(serverSalt, 0, temp, n, serverSalt.length);
                byte[] kcs = messageDigest.digest(temp);
                System.arraycopy(clientSalt, 0, temp, n, clientSalt.length);
                byte[] kcc = messageDigest.digest(temp);
                SecretKey encodingKey;
                SecretKey decodingKey;
                if ((sessionCipher.equals(CIPHERSUITS[DES])) || (sessionCipher.equals(CIPHERSUITS[DES3]))) {
                    String cipherName;
                    if (sessionCipher.equals(CIPHERSUITS[DES])) {
                        cipherName = "DES/CBC/NoPadding";
                        encodingKey = createDesKey(kcs);
                        decodingKey = createDesKey(kcc);
                    } else {
                        cipherName = "DESede/CBC/NoPadding";
                        encodingKey = createDesedeKey(kcs);
                        decodingKey = createDesedeKey(kcc);
                    }
                    encodingCipher = Cipher.getInstance(cipherName);
                    IvParameterSpec encodingIV = new IvParameterSpec(kcs, 8, 8);
                    encodingCipher.init(Cipher.ENCRYPT_MODE, encodingKey, encodingIV);
                    decodingCipher = Cipher.getInstance(cipherName);
                    IvParameterSpec decodingIV = new IvParameterSpec(kcc, 8, 8);
                    decodingCipher.init(Cipher.DECRYPT_MODE, decodingKey, decodingIV);
                } else {
                    encodingCipher = Cipher.getInstance("RC4");
                    encodingKey = new SecretKeySpec(kcs, "RC4");
                    encodingCipher.init(Cipher.ENCRYPT_MODE, encodingKey);
                    decodingCipher = Cipher.getInstance("RC4");
                    decodingKey = new SecretKeySpec(kcc, "RC4");
                    decodingCipher.init(Cipher.DECRYPT_MODE, decodingKey);
                }
            } catch (UnsupportedEncodingException e) {
                throw new SaslException(e.getMessage());
            } catch (GeneralSecurityException e) {
                throw new SaslException(e.getMessage());
            }
        }

        public byte[] wrap(byte[] outgoing, int offset, int length) throws SaslException {
            if (length == 0) {
                return EMPTY;
            }
            ByteUtils.getNetworkByteOrderFromInt(serverSeqNum++, sequenceNum, 0, 4);
            byte[] mac = computeHMAC(kis, sequenceNum, outgoing, offset, length);
            int blockSize = encodingCipher.getBlockSize();
            byte paddingSize = blockSize == 1 ? 0 : (byte) ((blockSize - ((length + 10) % blockSize)) & 0xff);
            byte[] toBeEncrypted = new byte[length + paddingSize + 10];
            System.arraycopy(outgoing, offset, toBeEncrypted, 0, length);
            for (int i = 0; i < paddingSize; i++) {
                toBeEncrypted[length + i] = paddingSize;
            }
            System.arraycopy(mac, 0, toBeEncrypted, length + paddingSize, 10);
            byte[] encryptedMessage = encodingCipher.update(toBeEncrypted);
            if (encryptedMessage == null) {
                throw new SaslException("Error encrypting outgoing message");
            }
            int encryptedMessageSize = encryptedMessage.length;
            byte[] wrapped = new byte[encryptedMessageSize + 6];
            System.arraycopy(encryptedMessage, 0, wrapped, 0, encryptedMessageSize);
            System.arraycopy(messageTypeNBO, 0, wrapped, encryptedMessageSize, 2);
            System.arraycopy(sequenceNum, 0, wrapped, encryptedMessageSize + 2, 4);
            return wrapped;
        }

        public byte[] unwrap(byte[] incoming, int offset, int length) throws SaslException {
            if (length == 0) {
                return EMPTY;
            }
            int toBeDecryptedSize = length - 6;
            byte[] toBeDecrypted = new byte[toBeDecryptedSize];
            byte[] seqNum = new byte[4];
            System.arraycopy(incoming, offset, toBeDecrypted, 0, toBeDecryptedSize);
            System.arraycopy(incoming, offset + toBeDecryptedSize + 2, seqNum, 0, 4);
            int messageType = ByteUtils.getIntegerFromNetworkByteOrder(incoming, offset + toBeDecryptedSize + 10, 2);
            if (messageType != 1) {
                throw new SaslException("Invalid message type: " + messageType);
            }
            int clientSeqNum = ByteUtils.getIntegerFromNetworkByteOrder(seqNum, 0, 4);
            if (clientSeqNum != this.clientSeqNum) {
                throw new SaslException("A message segment was received out of order. Expected: " + this.clientSeqNum + " Received: " + clientSeqNum);
            }
            this.clientSeqNum++;
            byte[] decryptedMessage = decodingCipher.update(toBeDecrypted);
            if (decryptedMessage == null) {
                throw new SaslException("Error decrypting incoming message");
            }
            int paddedMessageSize = decryptedMessage.length - 10;
            byte[] paddedMessage = new byte[paddedMessageSize];
            System.arraycopy(decryptedMessage, 0, paddedMessage, 0, paddedMessageSize);
            byte[] mac = new byte[10];
            System.arraycopy(decryptedMessage, paddedMessageSize, mac, 0, 10);
            int blockSize = decodingCipher.getBlockSize();
            if (blockSize > 1) {
                paddedMessageSize -= paddedMessage[paddedMessageSize - 1];
                if (paddedMessageSize < 0) {
                    return EMPTY;
                }
            }
            byte[] expectedMac = computeHMAC(kic, seqNum, paddedMessage, 0, paddedMessageSize);
            if (!Arrays.equals(mac, expectedMac)) {
                return EMPTY;
            }
            if (paddedMessageSize == paddedMessage.length) {
                return paddedMessage;
            } else {
                byte[] message = new byte[paddedMessageSize];
                System.arraycopy(paddedMessage, 0, message, 0, paddedMessageSize);
                return message;
            }
        }
    }

    private static final SecretKey createDesKey(byte[] rawKey) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        byte[] edeKeyBaseBytes = ByteUtils.convert8bitTo7bit(rawKey, 0, true);
        ByteUtils.computeAndSetParityBit(edeKeyBaseBytes);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("des");
        KeySpec keySpec = new DESKeySpec(edeKeyBaseBytes, 0);
        return secretKeyFactory.generateSecret(keySpec);
    }

    private static final SecretKey createDesedeKey(byte[] rawKey) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        byte[] edeKeyBaseBytes = ByteUtils.convert8bitTo7bit(rawKey, 0, true);
        ByteUtils.computeAndSetParityBit(edeKeyBaseBytes);
        byte[] edeKeyBaseBytes2 = ByteUtils.convert8bitTo7bit(rawKey, 7, true);
        ByteUtils.computeAndSetParityBit(edeKeyBaseBytes2);
        byte[] desedeKeyBaseBytes = new byte[24];
        System.arraycopy(edeKeyBaseBytes, 0, desedeKeyBaseBytes, 0, 8);
        System.arraycopy(edeKeyBaseBytes2, 0, desedeKeyBaseBytes, 8, 8);
        System.arraycopy(edeKeyBaseBytes, 0, desedeKeyBaseBytes, 16, 8);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("desede");
        KeySpec keySpec = new DESedeKeySpec(desedeKeyBaseBytes, 0);
        return secretKeyFactory.generateSecret(keySpec);
    }
}
