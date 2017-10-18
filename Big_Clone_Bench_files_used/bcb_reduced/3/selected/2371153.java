package com.liferay.portal.security.ntlm;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Encdec;

/**
 * <a href="NtlmManager.java.html"><b><i>View Source</i></b></a>
 *
 * @author Marcellus Tavares
 */
public class NtlmManager {

    public NtlmManager() {
        _domain = PropsValues.NTLM_DOMAIN;
        _ntlmServiceAccount = new NtlmServiceAccount(PropsValues.NTLM_SERVICE_ACCOUNT, null);
    }

    public NtlmUserAccount authenticate(byte[] material) {
        try {
            Type3Message type3Message = new Type3Message(material);
            if (type3Message.getFlag(_NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY) && (type3Message.getNTResponse().length == 24)) {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                byte[] bytes = new byte[16];
                System.arraycopy(_serverChallenge, 0, bytes, 0, 8);
                System.arraycopy(type3Message.getLMResponse(), 0, bytes, 8, 8);
                messageDigest.update(bytes);
                _serverChallenge = messageDigest.digest();
            }
            return NetlogonUtil.logon(type3Message.getDomain(), type3Message.getUser(), type3Message.getWorkstation(), _serverChallenge, type3Message.getNTResponse(), type3Message.getLMResponse());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getServerChallenge() {
        return _serverChallenge;
    }

    public byte[] negotiate(byte[] material) {
        try {
            Type1Message type1Message = new Type1Message(material);
            _secureRandom.nextBytes(_serverChallenge);
            Type2Message type2Message = new Type2Message(type1Message.getFlags(), _serverChallenge, _domain);
            if (type2Message.getFlag(_NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY)) {
                type2Message.setFlag(NtlmFlags.NTLMSSP_NEGOTIATE_LM_KEY, false);
                type2Message.setFlag(NtlmFlags.NTLMSSP_NEGOTIATE_TARGET_INFO, true);
                type2Message.setTargetInformation(getTargetInformation());
            }
            return type2Message.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setServerChallenge(byte[] serverChallenge) {
        _serverChallenge = serverChallenge;
    }

    protected byte[] getAVPairBytes(int avId, String value) throws UnsupportedEncodingException {
        byte[] valueBytes = value.getBytes("UTF-16LE");
        byte[] avPairBytes = new byte[4 + valueBytes.length];
        Encdec.enc_uint16le((short) avId, avPairBytes, 0);
        Encdec.enc_uint16le((short) valueBytes.length, avPairBytes, 2);
        System.arraycopy(valueBytes, 0, avPairBytes, 4, valueBytes.length);
        return avPairBytes;
    }

    protected byte[] getTargetInformation() throws UnsupportedEncodingException {
        byte[] computerName = getAVPairBytes(1, _ntlmServiceAccount.getComputerName());
        byte[] domainName = getAVPairBytes(2, _domain);
        byte[] targetInformation = append(computerName, domainName);
        byte[] eol = getAVPairBytes(0, "");
        targetInformation = append(targetInformation, eol);
        return targetInformation;
    }

    private static final int _NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY = 0x00080000;

    private String _domain;

    private NtlmServiceAccount _ntlmServiceAccount;

    private SecureRandom _secureRandom = new SecureRandom();

    private byte[] _serverChallenge = new byte[8];

    /**
         * From package com.liferay.portal.kernel.util.ArrayUtil.java
         * @param arrays
         * @return
         */
    public static byte[] append(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] newArray = new byte[length];
        int previousLength = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, newArray, previousLength, array.length);
            previousLength += array.length;
        }
        return newArray;
    }
}
