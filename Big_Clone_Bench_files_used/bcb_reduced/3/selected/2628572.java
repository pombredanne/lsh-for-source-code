package org.jwebsocket.kit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import javolution.util.FastMap;

/**
 * Utility class for all the handshaking related request/response.
 * @author aschulze
 * @version $Id:$
 */
public final class WebSocketHandshake {

    /**
	 *
	 */
    public static int MAX_HEADER_SIZE = 16834;

    private String mKey1 = null;

    private String mKey2 = null;

    private byte[] mKey3 = null;

    private byte[] mExpectedServerResponse = null;

    private URI mURL = null;

    private String mOrigin = null;

    private String mProtocol = null;

    private String mDraft = null;

    /**
	 *
	 * @param aURL
	 */
    public WebSocketHandshake(URI aURL) {
        this(aURL, null, null);
    }

    /**
	 *
	 * @param aURL
	 * @param aProtocol
	 */
    public WebSocketHandshake(URI aURL, String aProtocol) {
        this(aURL, aProtocol, null);
    }

    /**
	 *
	 * @param aURL
	 * @param aProtocol
	 * @param aDraft
	 */
    public WebSocketHandshake(URI aURL, String aProtocol, String aDraft) {
        this.mURL = aURL;
        this.mProtocol = aProtocol;
        this.mDraft = aDraft;
        generateKeys();
    }

    public static byte[] generateC2SRequest(String aHost, String aPath) {
        String lOrigin = "http://" + aHost;
        String lHandshake = "GET " + aPath + " HTTP/1.1\r\n" + "Upgrade: WebSocket\r\n" + "Connection: Upgrade\r\n" + "Host: " + aHost + "\r\n" + "Origin: " + lOrigin + "\r\n" + "\r\n";
        byte[] lBA = null;
        try {
            lBA = lHandshake.getBytes("US-ASCII");
        } catch (Exception lEx) {
        }
        return lBA;
    }

    private static long calcSecKeyNum(String aKey) {
        StringBuilder lSB = new StringBuilder();
        int lSpaces = 0;
        for (int lIdx = 0; lIdx < aKey.length(); lIdx++) {
            char lC = aKey.charAt(lIdx);
            if (lC == ' ') {
                lSpaces++;
            } else if (lC >= '0' && lC <= '9') {
                lSB.append(lC);
            }
        }
        long lRes = -1;
        if (lSpaces > 0) {
            try {
                lRes = Long.parseLong(lSB.toString()) / lSpaces;
            } catch (NumberFormatException lEx) {
            }
        }
        return lRes;
    }

    /**
	 * Parses the response from the client on an initial client's handshake
	 * request. This is always performed on the server only when a client -
	 * irrespective of if it is a Java Client or Browser Client - initiates a
	 * connection.
	 *
	 * @param aReq
	 * @return
	 */
    public static Map parseC2SRequest(byte[] aReq) {
        String lHost = null;
        String lOrigin = null;
        String lLocation = null;
        String lPath = null;
        String lSubProt = null;
        String lDraft = null;
        String lSecKey1 = null;
        String lSecKey2 = null;
        byte[] lSecKey3 = new byte[8];
        Boolean lIsSecure = false;
        Long lSecNum1 = null;
        Long lSecNum2 = null;
        byte[] lSecKeyResp = new byte[8];
        Map lRes = new FastMap();
        int lReqLen = aReq.length;
        String lRequest = "";
        try {
            lRequest = new String(aReq, "US-ASCII");
        } catch (Exception lEx) {
        }
        if (lRequest.indexOf("policy-file-request") >= 0) {
            lRes.put("policy-file-request", lRequest);
            return lRes;
        }
        lIsSecure = (lRequest.indexOf("Sec-WebSocket") > 0);
        if (lIsSecure) {
            lReqLen -= 8;
            for (int lIdx = 0; lIdx < 8; lIdx++) {
                lSecKey3[lIdx] = aReq[lReqLen + lIdx];
            }
        }
        int lPos = lRequest.indexOf("Host:");
        lPos += 6;
        lHost = lRequest.substring(lPos);
        lPos = lHost.indexOf("\r\n");
        lHost = lHost.substring(0, lPos);
        lPos = lRequest.indexOf("Origin:");
        lPos += 8;
        lOrigin = lRequest.substring(lPos);
        lPos = lOrigin.indexOf("\r\n");
        lOrigin = lOrigin.substring(0, lPos);
        lPos = lRequest.indexOf("GET");
        lPos += 4;
        lPath = lRequest.substring(lPos);
        lPos = lPath.indexOf("HTTP");
        lPath = lPath.substring(0, lPos - 1);
        lLocation = "ws://" + lHost + lPath;
        lPos = lRequest.indexOf("WebSocket-Protocol:");
        if (lPos > 0) {
            lPos += 20;
            lSubProt = lRequest.substring(lPos);
            lPos = lSubProt.indexOf("\r\n");
            lSubProt = lSubProt.substring(0, lPos);
        }
        lPos = lRequest.indexOf("Sec-WebSocket-Draft:");
        if (lPos > 0) {
            lPos += 21;
            lDraft = lRequest.substring(lPos);
            lPos = lDraft.indexOf("\r\n");
            lDraft = lDraft.substring(0, lPos);
        }
        lPos = lRequest.indexOf("Sec-WebSocket-Key1:");
        if (lPos > 0) {
            lPos += 20;
            lSecKey1 = lRequest.substring(lPos);
            lPos = lSecKey1.indexOf("\r\n");
            lSecKey1 = lSecKey1.substring(0, lPos);
            lSecNum1 = calcSecKeyNum(lSecKey1);
        }
        lPos = lRequest.indexOf("Sec-WebSocket-Key2:");
        if (lPos > 0) {
            lPos += 20;
            lSecKey2 = lRequest.substring(lPos);
            lPos = lSecKey2.indexOf("\r\n");
            lSecKey2 = lSecKey2.substring(0, lPos);
            lSecNum2 = calcSecKeyNum(lSecKey2);
        }
        if (lSecNum1 != null && lSecNum2 != null) {
            BigInteger lSec1 = new BigInteger(lSecNum1.toString());
            BigInteger lSec2 = new BigInteger(lSecNum2.toString());
            byte[] l128Bit = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            byte[] lTmp;
            int lOfs;
            lTmp = lSec1.toByteArray();
            int lIdx = lTmp.length;
            int lCnt = 0;
            while (lIdx > 0 && lCnt < 4) {
                lIdx--;
                lCnt++;
                l128Bit[4 - lCnt] = lTmp[lIdx];
            }
            lTmp = lSec2.toByteArray();
            lIdx = lTmp.length;
            lCnt = 0;
            while (lIdx > 0 && lCnt < 4) {
                lIdx--;
                lCnt++;
                l128Bit[8 - lCnt] = lTmp[lIdx];
            }
            lTmp = lSecKey3;
            System.arraycopy(lSecKey3, 0, l128Bit, 8, 8);
            try {
                MessageDigest lMD = MessageDigest.getInstance("MD5");
                lSecKeyResp = lMD.digest(l128Bit);
            } catch (Exception lEx) {
            }
        }
        lRes.put(RequestHeader.WS_PATH, lPath);
        lRes.put(RequestHeader.WS_HOST, lHost);
        lRes.put(RequestHeader.WS_ORIGIN, lOrigin);
        lRes.put(RequestHeader.WS_LOCATION, lLocation);
        lRes.put(RequestHeader.WS_PROTOCOL, lSubProt);
        lRes.put(RequestHeader.WS_SECKEY1, lSecKey1);
        lRes.put(RequestHeader.WS_SECKEY2, lSecKey2);
        lRes.put(RequestHeader.WS_DRAFT, lDraft);
        lRes.put("isSecure", lIsSecure);
        lRes.put("secKeyResponse", lSecKeyResp);
        return lRes;
    }

    /**
	 * Generates the response for the server to answer an initial client
	 * request. This is performed on the server only as an answer to a client's
	 * request - irrespective of if it is a Java or Browser Client.
	 *
	 * @param aRequest
	 * @return
	 */
    public static byte[] generateS2CResponse(Map aRequest) {
        String lPolicyFileRequest = (String) aRequest.get("policy-file-request");
        if (lPolicyFileRequest != null) {
            byte[] lBA;
            try {
                lBA = ("<cross-domain-policy>" + "<allow-access-from domain=\"*\" to-ports=\"*\" />" + "</cross-domain-policy>\n").getBytes("US-ASCII");
            } catch (UnsupportedEncodingException lEx) {
                lBA = null;
            }
            return lBA;
        }
        Boolean lIsSecure = (Boolean) aRequest.get("isSecure");
        String lOrigin = (String) aRequest.get(RequestHeader.WS_ORIGIN);
        String lLocation = (String) aRequest.get(RequestHeader.WS_LOCATION);
        String lSubProt = (String) aRequest.get(RequestHeader.WS_PROTOCOL);
        String lRes = "HTTP/1.1 101 Web" + (lIsSecure ? "" : " ") + "Socket Protocol Handshake\r\n" + "Upgrade: WebSocket\r\n" + "Connection: Upgrade\r\n" + (lSubProt != null ? (lIsSecure ? "Sec-" : "") + "WebSocket-Protocol: " + lSubProt + "\r\n" : "") + (lIsSecure ? "Sec-" : "") + "WebSocket-Origin: " + lOrigin + "\r\n" + (lIsSecure ? "Sec-" : "") + "WebSocket-Location: " + lLocation + "\r\n" + "\r\n";
        byte[] lBA;
        try {
            lBA = lRes.getBytes("US-ASCII");
            if (lIsSecure) {
                byte[] lSecKey = (byte[]) aRequest.get("secKeyResponse");
                byte[] lResult = new byte[lBA.length + lSecKey.length];
                System.arraycopy(lBA, 0, lResult, 0, lBA.length);
                System.arraycopy(lSecKey, 0, lResult, lBA.length, lSecKey.length);
                return lResult;
            } else {
                return lBA;
            }
        } catch (UnsupportedEncodingException lEx) {
            return null;
        }
    }

    /**
	 * Reads the handshake response from the server into an byte array. This is
	 * used on clients only. The browser client implement that internally.
	 *
	 * @param aIS
	 * @return
	 */
    public static byte[] readS2CResponse(InputStream aIS) {
        byte[] lBuff = new byte[MAX_HEADER_SIZE];
        boolean lContinue = true;
        int lIdx = 0;
        int lB1 = 0, lB2 = 0, lB3 = 0, lB4 = 0;
        while (lContinue && lIdx < MAX_HEADER_SIZE) {
            int lIn;
            try {
                lIn = aIS.read();
                if (lIn < 0) {
                    return null;
                }
            } catch (IOException lIOEx) {
                return null;
            }
            lB1 = lB2;
            lB2 = lB3;
            lB3 = lB4;
            lB4 = lIn;
            lContinue = !(lB1 == 13 && lB2 == 10 && lB3 == 13 && lB4 == 10);
            lBuff[lIdx] = (byte) lIn;
            lIdx++;
        }
        byte[] lRes = new byte[lIdx];
        System.arraycopy(lBuff, 0, lRes, 0, lIdx);
        return lRes;
    }

    /**
	 *
	 * @param aResp
	 * @return
	 */
    public static Map parseS2CResponse(byte[] aResp) {
        Map lRes = new FastMap();
        String lResp = null;
        try {
            lResp = new String(aResp, "US-ASCII");
        } catch (Exception lEx) {
        }
        return lRes;
    }

    /**
	 *
	 * @return
	 */
    public byte[] getHandshake() {
        String lPath = mURL.getPath();
        String lHost = mURL.getHost();
        mOrigin = "http://" + lHost;
        if ("".equals(lPath)) {
            lPath = "/";
        }
        String lHandshake = "GET " + lPath + " HTTP/1.1\r\n" + "Host: " + lHost + "\r\n" + "Connection: Upgrade\r\n" + "Sec-WebSocket-Key2: " + mKey2 + "\r\n";
        if (mProtocol != null) {
            lHandshake += "Sec-WebSocket-Protocol: " + mProtocol + "\r\n";
        }
        if (mDraft != null) {
            lHandshake += "Sec-WebSocket-Draft: " + mDraft + "\r\n";
        }
        lHandshake += "Upgrade: WebSocket\r\n" + "Sec-WebSocket-Key1: " + mKey1 + "\r\n" + "Origin: " + mOrigin + "\r\n" + "\r\n";
        byte[] lHandshakeBytes = new byte[lHandshake.getBytes().length + 8];
        System.arraycopy(lHandshake.getBytes(), 0, lHandshakeBytes, 0, lHandshake.getBytes().length);
        System.arraycopy(mKey3, 0, lHandshakeBytes, lHandshake.getBytes().length, 8);
        return lHandshakeBytes;
    }

    /**
	 *
	 * @param aBytes
	 * @throws WebSocketException
	 */
    public void verifyServerResponse(byte[] aBytes) throws WebSocketException {
        if (!Arrays.equals(aBytes, mExpectedServerResponse)) {
            throw new WebSocketException("not a WebSocket Server");
        }
    }

    /**
	 *
	 * @param aStatusLine
	 * @throws WebSocketException
	 */
    public void verifyServerStatusLine(String aStatusLine) throws WebSocketException {
        int lStatusCode = Integer.valueOf(aStatusLine.substring(9, 12));
        if (lStatusCode == 407) {
            throw new WebSocketException("connection failed: proxy authentication not supported");
        } else if (lStatusCode == 404) {
            throw new WebSocketException("connection failed: 404 not found");
        } else if (lStatusCode != 101) {
            throw new WebSocketException("connection failed: unknown status code " + lStatusCode);
        }
    }

    /**
	 *
	 * @param aHeaders
	 * @throws WebSocketException
	 */
    public void verifyServerHandshakeHeaders(Map<String, String> aHeaders) throws WebSocketException {
        if (!aHeaders.get("Upgrade").equals("WebSocket")) {
            throw new WebSocketException("connection failed: missing header field in server handshake: Upgrade");
        } else if (!aHeaders.get("Connection").equals("Upgrade")) {
            throw new WebSocketException("connection failed: missing header field in server handshake: Connection");
        } else if (!aHeaders.get("Sec-WebSocket-Origin").equals(mOrigin)) {
            throw new WebSocketException("connection failed: missing header field in server handshake: Sec-WebSocket-Origin");
        } else if (aHeaders.containsKey("Sec-WebSocket-Protocol") && (mProtocol.indexOf(aHeaders.get("Sec-WebSocket-Protocol")) == -1)) {
            throw new WebSocketException("connection failed: invalid header field in server handshake: Sec-WebSocket-Protocol," + " expected one of : " + mProtocol + ", but got: " + aHeaders.get("Sec-WebSocket-Protocol"));
        }
    }

    private void generateKeys() {
        int lSpaces1 = rand(1, 12);
        int lSpaces2 = rand(1, 12);
        int lMax1 = Integer.MAX_VALUE / lSpaces1;
        int lMax2 = Integer.MAX_VALUE / lSpaces2;
        int lNumber1 = rand(0, lMax1);
        int lNumber2 = rand(0, lMax2);
        int lProduct1 = lNumber1 * lSpaces1;
        int lProduct2 = lNumber2 * lSpaces2;
        mKey1 = Integer.toString(lProduct1);
        mKey2 = Integer.toString(lProduct2);
        mKey1 = insertRandomCharacters(mKey1);
        mKey2 = insertRandomCharacters(mKey2);
        mKey1 = insertSpaces(mKey1, lSpaces1);
        mKey2 = insertSpaces(mKey2, lSpaces2);
        mKey3 = createRandomBytes();
        ByteBuffer lBuffer = ByteBuffer.allocate(4);
        lBuffer.putInt(lNumber1);
        byte[] lNumber1Array = lBuffer.array();
        lBuffer = ByteBuffer.allocate(4);
        lBuffer.putInt(lNumber2);
        byte[] lNumber2Array = lBuffer.array();
        byte[] lChallenge = new byte[16];
        System.arraycopy(lNumber1Array, 0, lChallenge, 0, 4);
        System.arraycopy(lNumber2Array, 0, lChallenge, 4, 4);
        System.arraycopy(mKey3, 0, lChallenge, 8, 8);
        mExpectedServerResponse = md5(lChallenge);
    }

    private String insertRandomCharacters(String aKey) {
        int lCount = rand(1, 12);
        char[] lRandomChars = new char[lCount];
        int lRandCount = 0;
        while (lRandCount < lCount) {
            int lRand = (int) (Math.random() * 0x7e + 0x21);
            if (((0x21 < lRand) && (lRand < 0x2f)) || ((0x3a < lRand) && (lRand < 0x7e))) {
                lRandomChars[lRandCount] = (char) lRand;
                lRandCount += 1;
            }
        }
        for (int lIdx = 0; lIdx < lCount; lIdx++) {
            int lSplit = rand(1, aKey.length() - 1);
            String lPart1 = aKey.substring(0, lSplit);
            String lPart2 = aKey.substring(lSplit);
            aKey = lPart1 + lRandomChars[lIdx] + lPart2;
        }
        return aKey;
    }

    private String insertSpaces(String aKey, int aSpaces) {
        for (int lIdx = 0; lIdx < aSpaces; lIdx++) {
            int lSplit = rand(1, aKey.length() - 1);
            String lPart1 = aKey.substring(0, lSplit);
            String lPart2 = aKey.substring(lSplit);
            aKey = lPart1 + " " + lPart2;
        }
        return aKey;
    }

    private byte[] createRandomBytes() {
        byte[] lBytes = new byte[8];
        for (int lIdx = 0; lIdx < 8; lIdx++) {
            lBytes[lIdx] = (byte) rand(0, 255);
        }
        return lBytes;
    }

    private byte[] md5(byte[] aBytes) {
        try {
            MessageDigest lMD = MessageDigest.getInstance("MD5");
            return lMD.digest(aBytes);
        } catch (NoSuchAlgorithmException lEx) {
            return null;
        }
    }

    private int rand(int aMin, int aMax) {
        int lRand = (int) (Math.random() * aMax + aMin);
        return lRand;
    }
}
