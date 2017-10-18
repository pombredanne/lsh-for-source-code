package wow;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.*;
import gnu.java.math.BigInteger;
import gnu.java.security.hash.*;

public class WoWauth {

    private static final int NO_COMMAND = 0xff;

    private static final int LOGON_CHALLENGE = 0x00;

    private static final int LOGON_PROOF = 0x01;

    private static final int REALM_LIST = 0x10;

    private SocketConnection socket;

    private InputStream iStream;

    private OutputStream oStream;

    private String errorStr;

    private String account;

    private byte m_v0;

    private byte m_v1;

    private byte m_v2;

    private int m_v3;

    private byte[] userName;

    private byte[] userHash;

    private byte[] authHash;

    private byte[] authM2;

    private byte[] sessKey;

    private Vector realmList;

    private int lastCommand;

    private boolean authOk;

    private boolean realmsEof;

    public WoWauth(final String addr, byte v0, byte v1, byte v2, int v3) {
        m_v0 = v0;
        m_v1 = v1;
        m_v2 = v2;
        m_v3 = v3;
        cleanup();
        if (addr.length() == 0) return;
        try {
            socket = (SocketConnection) Connector.open("socket://" + addr, Connector.READ_WRITE, true);
            iStream = socket.openInputStream();
            oStream = socket.openOutputStream();
        } catch (Exception e) {
            socket = null;
            iStream = null;
            oStream = null;
            errorStr = e.getMessage();
            return;
        }
    }

    public void disconnect() {
        iStream = null;
        oStream = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
            socket = null;
        }
        lastCommand = NO_COMMAND;
        userName = null;
        userHash = null;
        authHash = null;
        authM2 = null;
    }

    public void cleanup() {
        disconnect();
        errorStr = "";
        account = null;
        realmList = new Vector(0, 1);
        authOk = false;
        realmsEof = false;
        sessKey = null;
    }

    public boolean isConnected() {
        return (socket != null);
    }

    public boolean isAuthenticated() {
        return authOk;
    }

    public boolean doLogin(final String user, final String passwd) {
        account = user.toUpperCase();
        Sha160 h = new Sha160();
        if (h == null) return false;
        try {
            userName = account.getBytes("UTF-8");
            h.update(userName);
            userHash = h.digest();
            h.reset();
            h.update((account + ":" + passwd.toUpperCase()).getBytes("UTF-8"));
            authHash = h.digest();
        } catch (Exception e) {
            errorStr = e.getMessage();
            return false;
        }
        if (!askChallenge()) return false;
        while (!authOk) {
            if (!readReply()) return false;
        }
        return authOk;
    }

    public boolean doRealms() {
        if (oStream == null || !authOk) return false;
        byte b[] = new byte[5];
        b[0] = REALM_LIST;
        b[1] = 0;
        b[2] = 0;
        b[3] = 0;
        b[4] = 0;
        if (!writeByte(b)) return false;
        realmsEof = false;
        while (!realmsEof) {
            if (!readReply()) return false;
        }
        return true;
    }

    public String getError() {
        return errorStr;
    }

    public Vector realms() {
        return realmList;
    }

    public String getAccount() {
        return account;
    }

    public byte[] sessionKey() {
        return sessKey;
    }

    public int buildNo() {
        return m_v3;
    }

    private boolean setError(int code) {
        switch(code) {
            case 0:
                return false;
            case 3:
                errorStr = "Account banned";
                break;
            case 4:
            case 5:
                errorStr = "Invalid access information";
                break;
            case 6:
                errorStr = "Account in use";
                break;
            case 7:
                errorStr = "Prepaid time used";
                break;
            case 8:
                errorStr = "Server currently full";
                break;
            case 9:
                errorStr = "Wrong game build number";
                break;
            case 10:
                errorStr = "Client needs updating";
                break;
            case 12:
                errorStr = "Account temporarily suspended";
                break;
            case 13:
                errorStr = "Blocked by parental controls";
                break;
            default:
                errorStr = "Logon failure code: " + code;
        }
        return true;
    }

    private boolean readReply() {
        if (iStream == null || lastCommand == NO_COMMAND) {
            errorStr = "Internal error";
            return false;
        }
        try {
            int c = readByte();
            if (c != lastCommand) {
                errorStr = "Command/Reply mismatch";
                return false;
            }
            lastCommand = NO_COMMAND;
            switch(c) {
                case LOGON_CHALLENGE:
                    return gotChallenge();
                case LOGON_PROOF:
                    return gotProof();
                case REALM_LIST:
                    return gotRealms();
                default:
                    errorStr = "Unknown Command/Reply code";
            }
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        return false;
    }

    private int readByte() throws IOException {
        int c = iStream.read();
        if (c < 0) throw new IOException("Server closed connection");
        return c;
    }

    private byte[] readByte(int len, int maxlen) throws IOException {
        if (len <= 0 || len > maxlen) throw new IOException("Invalid read length: " + len);
        byte b[] = new byte[len];
        int c = iStream.read(b);
        if (c < len) throw new IOException("Server closed connection");
        return b;
    }

    private byte[] readByte(int len) throws IOException {
        return readByte(len, 32);
    }

    private int readWord() throws IOException {
        return readByte() | (readByte() << 8);
    }

    private String readStr() throws IOException {
        String s = new String();
        int c;
        while ((c = readByte()) != 0) s += String.valueOf((char) c);
        return s;
    }

    private boolean writeByte(byte[] buf) {
        System.err.println("writeByte() " + buf.length);
        try {
            oStream.write(buf);
            lastCommand = buf[0] & 0xff;
            oStream.flush();
            return true;
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        return false;
    }

    private void copyBytes(byte[] dest, byte[] src, int len, int offs1, int offs2) {
        while (len-- > 0) dest[offs1++] = src[offs2++];
    }

    private void copyBytes(byte[] dest, BigInteger src, int len, int offs) {
        byte[] ba = src.toByteArray();
        if (len == 0) len = ba.length;
        byte s = (src.signum() < 0) ? (byte) 255 : 0;
        for (int i = 0; i < len; i++) {
            int idx = ba.length - i - 1;
            dest[offs++] = (idx >= 0) ? ba[idx] : s;
        }
    }

    private BigInteger getBigInteger(byte[] b) {
        byte[] r = new byte[b.length];
        for (int i = 0; i < b.length; i++) r[i] = b[b.length - i - 1];
        return new BigInteger(1, r);
    }

    private byte[] getBytesOf(BigInteger bi) {
        byte[] b = bi.toByteArray();
        byte[] r = new byte[b.length];
        for (int i = 0; i < b.length; i++) r[i] = b[b.length - i - 1];
        return r;
    }

    private boolean askChallenge() {
        System.err.println("askChallenge()");
        if (oStream == null || userName.length == 0 || userName.length > 32) return false;
        int tzo = 120;
        int len = 30 + userName.length;
        byte b[] = new byte[len + 4];
        b[0] = LOGON_CHALLENGE;
        b[1] = 8;
        b[2] = (byte) len;
        b[3] = (byte) (len >> 8);
        b[4] = 'W';
        b[5] = 'o';
        b[6] = 'W';
        b[7] = 0;
        b[8] = m_v0;
        b[9] = m_v1;
        b[10] = m_v2;
        b[11] = (byte) m_v3;
        b[12] = (byte) (m_v3 >> 8);
        b[13] = 'a';
        b[14] = 'v';
        b[15] = 'a';
        b[16] = 'J';
        b[17] = 'P';
        b[18] = 'D';
        b[19] = 'I';
        b[20] = 'M';
        b[21] = 'B';
        b[22] = 'G';
        b[23] = 'n';
        b[24] = 'e';
        b[25] = (byte) tzo;
        b[26] = (byte) (tzo >> 8);
        b[27] = (byte) (tzo >> 16);
        b[28] = (byte) (tzo >> 24);
        b[29] = (byte) 127;
        b[30] = (byte) 0;
        b[31] = (byte) 0;
        b[32] = (byte) 1;
        try {
            String tmp = socket.getLocalAddress().trim();
            System.err.println("Local Address: " + tmp);
            for (int i = 0; i < 4; i++) {
                int p = tmp.indexOf('.');
                if (p <= 0) break;
                int val = Integer.parseInt(tmp.substring(0, p - 1));
                if (val < 0 || val > 255) break;
                tmp = tmp.substring(p + 1);
                b[29 + i] = (byte) val;
            }
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        b[33] = (byte) userName.length;
        for (len = 0; len < userName.length; len++) b[34 + len] = userName[len];
        return writeByte(b);
    }

    private boolean askRealms() {
        System.err.println("askRealms()");
        if (!authOk) {
            errorStr = "Not logged in";
            return false;
        }
        byte b[] = new byte[5];
        b[0] = REALM_LIST;
        b[1] = 0;
        b[2] = 0;
        b[3] = 0;
        b[4] = 0;
        return writeByte(b);
    }

    private boolean gotChallenge() throws IOException {
        System.err.println("gotChallenge()");
        if (setError(readByte())) return false;
        if (setError(readByte())) return false;
        BigInteger B = getBigInteger(readByte(32));
        byte[] gb = readByte(readByte());
        BigInteger g = getBigInteger(gb);
        byte[] Nb = readByte(readByte());
        BigInteger N = getBigInteger(Nb);
        byte[] salt = readByte(32);
        BigInteger sb = getBigInteger(salt);
        byte unk3[] = readByte(16);
        int unk12 = readByte();
        if (sb.signum() == 0) {
            errorStr = "Account unknown by server";
            return false;
        }
        System.err.println("Got challenge, B=" + B.toString(16) + " g=" + g.intValue() + " N=" + N.toString(16) + " salt=" + sb.toString(16));
        Sha160 h = new Sha160();
        h.update(salt);
        h.update(authHash);
        BigInteger x = getBigInteger(h.digest());
        System.err.println("Computed x=" + x.toString(16));
        BigInteger v = g.modPow(x, N);
        System.err.println("Computed v=" + v.toString(16));
        BigInteger a = BigInteger.ONE.add(new BigInteger(128, new Random()));
        BigInteger A = g.modPow(a, N);
        System.err.println("Computed a=" + a.toString(16) + " A=" + A.toString(16));
        h.reset();
        h.update(getBytesOf(A));
        h.update(getBytesOf(B));
        BigInteger u = getBigInteger(h.digest());
        System.err.println("Computed u=" + u.toString(16));
        BigInteger k = new BigInteger("3");
        BigInteger S = B.subtract(k.multiply(v)).modPow(a.add(u.multiply(x)), N);
        System.err.println("Computed S=" + S.toString(16));
        byte[] s = new byte[32];
        copyBytes(s, S, 32, 0);
        byte[] s1 = new byte[16];
        byte[] s2 = new byte[16];
        for (int i = 0; i < 16; i++) {
            s1[i] = s[i * 2];
            s2[i] = s[i * 2 + 1];
        }
        h.reset();
        h.update(s1);
        s1 = h.digest();
        h.reset();
        h.update(s2);
        s2 = h.digest();
        s = new byte[40];
        for (int i = 0; i < 20; i++) {
            s[i * 2] = s1[i];
            s[i * 2 + 1] = s2[i];
        }
        sessKey = s;
        System.err.println("Session key=" + getBigInteger(sessKey).toString(16));
        h.reset();
        h.update(Nb);
        byte[] ngh = h.digest();
        h.reset();
        h.update(gb);
        byte[] gh = h.digest();
        for (int i = 0; i < 20; i++) ngh[i] ^= gh[i];
        h.reset();
        h.update(ngh);
        h.update(userHash);
        h.update(salt);
        h.update(getBytesOf(A));
        h.update(getBytesOf(B));
        h.update(sessKey);
        byte[] m1 = h.digest();
        System.err.println("M1=" + getBigInteger(m1).toString(16));
        h.reset();
        h.update(getBytesOf(A));
        h.update(m1);
        h.update(sessKey);
        authM2 = h.digest();
        System.err.println("M2=" + getBigInteger(authM2).toString(16));
        byte b[] = new byte[75];
        b[0] = LOGON_PROOF;
        copyBytes(b, A, 32, 1);
        copyBytes(b, m1, 20, 33, 0);
        b[73] = 0;
        b[74] = (byte) unk12;
        return writeByte(b);
    }

    private boolean gotProof() throws IOException {
        System.err.println("gotProof()");
        if (setError(readByte())) return false;
        byte M2[] = readByte(20);
        readByte(10);
        for (int i = 0; i < 20; i++) {
            if (M2[i] != authM2[i]) {
                errorStr = "Failed authenticating the server";
                return false;
            }
        }
        authOk = true;
        return authOk;
    }

    private boolean gotRealms() throws IOException {
        int len = readWord();
        readByte(4);
        int cnt = readWord();
        System.err.println("Realms len=" + len + ", count=" + cnt);
        for (int i = 0; i < cnt; i++) {
            int icon = readByte();
            int locked = readByte();
            int color = readByte();
            String name = readStr();
            String addr = readStr();
            readByte(4);
            int chars = readByte();
            int tz = readByte();
            readByte();
            System.err.println("Realm '" + name + "' at '" + addr + "' icon=" + icon + ", locked=" + locked + ", color=" + color + ", chars=" + chars + ", tz=" + tz);
            if (chars > 0) realmList.addElement(new WoWrealm(name, addr));
        }
        readWord();
        realmsEof = true;
        return true;
    }
}
