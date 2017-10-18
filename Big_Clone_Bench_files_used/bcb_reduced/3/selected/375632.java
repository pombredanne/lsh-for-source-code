package wow;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.*;
import gnu.java.security.hash.*;
import gnu.javax.crypto.prng.*;
import gnu.javax.crypto.mac.*;

public class WoWconn {

    private static final byte[] decSeed = { (byte) 0xCC, (byte) 0x98, (byte) 0xAE, (byte) 0x04, (byte) 0xE8, (byte) 0x97, (byte) 0xEA, (byte) 0xCA, (byte) 0x12, (byte) 0xDD, (byte) 0xC0, (byte) 0x93, (byte) 0x42, (byte) 0x91, (byte) 0x53, (byte) 0x57 };

    private static final byte[] encSeed = { (byte) 0xC2, (byte) 0xB3, (byte) 0x72, (byte) 0x3C, (byte) 0xC6, (byte) 0xAE, (byte) 0xD9, (byte) 0xB5, (byte) 0x34, (byte) 0x3C, (byte) 0x53, (byte) 0xEE, (byte) 0x2F, (byte) 0x43, (byte) 0x67, (byte) 0xCE };

    private SocketConnection socket;

    private InputStream iStream;

    private OutputStream oStream;

    private String errorStr;

    private Thread m_reader;

    private Vector m_packets;

    private ARCFour dCipher;

    private ARCFour eCipher;

    private String m_account;

    private byte[] m_session;

    private int m_build;

    public WoWconn(final String addr) {
        m_reader = null;
        m_packets = new Vector(0, 1);
        m_account = null;
        m_session = null;
        m_build = 0;
        disconnect();
        errorStr = null;
        if (addr.length() == 0) return;
        dCipher = null;
        eCipher = null;
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
        stop();
        iStream = null;
        oStream = null;
        dCipher = null;
        eCipher = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
            socket = null;
        }
    }

    public void start() {
        if (m_reader == null) {
            m_reader = new Thread() {

                public void run() {
                    while (m_reader == this) {
                        if (!readPacketLoop()) break;
                    }
                    System.err.println("WoWconn exiting reader thread: " + errorStr);
                    if (m_reader == this) m_reader = null;
                }
            };
            m_reader.start();
        }
    }

    public void stop() {
        Thread tmp = m_reader;
        m_reader = null;
        if (tmp != null) tmp.interrupt();
    }

    public void setAuth(final String user, byte[] sessKey, int buildNo) {
        m_account = user.toUpperCase();
        m_session = sessKey;
        m_build = buildNo;
    }

    public static String dumpHex(byte[] bytes, int maxBytes) {
        String s = "";
        for (int i = 0; i < bytes.length; i++) {
            if (i >= maxBytes) {
                s += " ...";
                break;
            }
            String h = Integer.toHexString(bytes[i] & 0xff);
            if (h.length() < 2) h = "0" + h;
            if (s.length() != 0) s += " ";
            s += h;
        }
        return s;
    }

    public void startCrypt(byte[] key) {
        System.err.println("WoWconn.startCrypt() " + key.length);
        byte skip1k[] = new byte[1024];
        HMac hash = new HMac(new Sha160());
        hash.init(decSeed);
        hash.update(key, 0, key.length);
        dCipher = new ARCFour();
        dCipher.init(hash.digest());
        dCipher.nextBytes(skip1k);
        hash = new HMac(new Sha160());
        hash.init(encSeed);
        hash.update(key, 0, key.length);
        eCipher = new ARCFour();
        eCipher.init(hash.digest());
        eCipher.nextBytes(skip1k);
    }

    public boolean isConnected() {
        return (socket != null);
    }

    public String getError() {
        String tmp = errorStr;
        errorStr = null;
        return tmp;
    }

    private int readByte() throws IOException {
        int c = iStream.read();
        if (c < 0) throw new IOException("Server closed connection");
        if (dCipher == null) return c;
        byte pad[] = new byte[1];
        dCipher.nextBytes(pad);
        return c ^ (pad[0] & 0xff);
    }

    private byte[] readByte(int len) throws IOException {
        if (len <= 0) throw new IOException("Invalid read length: " + len);
        byte b[] = new byte[len];
        int pos = 0;
        while (pos < len) {
            int c = iStream.read(b, pos, len - pos);
            if (c < 0) throw new IOException("Server closed connection");
            pos += c;
        }
        return b;
    }

    private boolean writeByte(byte[] buf) {
        try {
            synchronized (oStream) {
                oStream.write(buf);
                oStream.flush();
            }
            return true;
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        return false;
    }

    private WoWpacket readPacketRaw() {
        try {
            int len = readByte();
            if ((len & 0x80) != 0) len = ((len & 0x7f) << 16) | (readByte() << 8) | readByte(); else len = (len << 8) | readByte();
            if (len < 2 || len > 0x10002) {
                errorStr = "Invalid packet length 0x" + Integer.toHexString(len) + " (" + len + ")";
                return null;
            }
            len -= 2;
            int cmd = readByte() | (readByte() << 8);
            byte[] buf = (len > 0) ? readByte(len) : null;
            return new WoWpacket(cmd, buf);
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        return null;
    }

    private static byte[] append(byte[] buf, byte[] data) {
        int l1 = (buf != null) ? buf.length : 0;
        byte[] tmp = new byte[l1 + data.length];
        int d = 0;
        int i;
        for (i = 0; i < l1; i++) tmp[d++] = buf[i];
        for (i = 0; i < data.length; i++) tmp[d++] = data[i];
        return tmp;
    }

    private WoWpacket readPacketAuth() {
        WoWpacket pkt = readPacketRaw();
        if (pkt == null || pkt.code() != WoWpacket.SMSG_AUTH_CHALLENGE) return pkt;
        System.err.println("Got auth challenge packet");
        if (m_account == null || m_session == null || m_build == 0 || pkt.data() == null || pkt.data().length < 8) return null;
        try {
            Sha160 h = new Sha160();
            byte[] unk = new byte[4];
            unk[0] = unk[1] = unk[2] = unk[3] = 0;
            Random rnd = new Random();
            byte[] seed = new byte[4];
            for (int i = 0; i < 4; i++) seed[i] = (byte) rnd.nextInt(256);
            byte[] acc = m_account.getBytes("UTF-8");
            h.update(acc);
            h.update(unk);
            h.update(seed);
            h.update(pkt.data(), 4, 4);
            h.update(m_session);
            byte auth[] = new byte[4];
            auth[0] = (byte) m_build;
            auth[1] = (byte) (m_build >> 8);
            auth[2] = (byte) (m_build >> 16);
            auth[3] = (byte) (m_build >> 24);
            auth = append(auth, unk);
            auth = append(auth, acc);
            auth = append(auth, new byte[1]);
            auth = append(auth, unk);
            auth = append(auth, seed);
            auth = append(auth, unk);
            auth = append(auth, unk);
            auth = append(auth, unk);
            auth = append(auth, unk);
            auth = append(auth, unk);
            auth = append(auth, h.digest());
            auth = append(auth, unk);
            if (!writePacket(WoWpacket.CMSG_AUTH_SESSION, auth)) return null;
            startCrypt(m_session);
            return pkt;
        } catch (Exception e) {
            errorStr = e.getMessage();
        }
        return null;
    }

    private boolean readPacketLoop() {
        WoWpacket pkt = readPacketAuth();
        if (pkt == null) return false;
        synchronized (m_packets) {
            m_packets.addElement(pkt);
            int size = m_packets.size();
            if (size > 0 && (size % 10) == 0) System.err.println("Queued received packets: " + size);
        }
        return true;
    }

    public WoWpacket readPacket() {
        synchronized (m_packets) {
            if (m_packets.isEmpty()) return null;
            WoWpacket pkt = (WoWpacket) m_packets.firstElement();
            m_packets.removeElementAt(0);
            return pkt;
        }
    }

    public boolean writePacket(int cmd, byte[] buf) {
        int len = (buf == null) ? 0 : buf.length;
        len += 4;
        byte hdr[] = new byte[6];
        hdr[0] = (byte) (len >> 8);
        hdr[1] = (byte) (len & 0xff);
        hdr[2] = (byte) (cmd & 0xff);
        hdr[3] = (byte) (cmd >> 8);
        hdr[4] = 0;
        hdr[5] = 0;
        if (eCipher != null) {
            byte[] pad = new byte[hdr.length];
            eCipher.nextBytes(pad);
            for (int i = 0; i < hdr.length; i++) hdr[i] ^= pad[i];
        }
        if (buf != null) hdr = append(hdr, buf);
        return writeByte(hdr);
    }

    public boolean writePacket(int cmd, int val) {
        byte buf[] = new byte[4];
        buf[0] = (byte) val;
        buf[1] = (byte) (val >> 8);
        buf[2] = (byte) (val >> 16);
        buf[3] = (byte) (val >> 24);
        return writePacket(cmd, buf);
    }

    public boolean writePacket(int cmd, long val) {
        byte buf[] = new byte[8];
        buf[0] = (byte) val;
        buf[1] = (byte) (val >> 8);
        buf[2] = (byte) (val >> 16);
        buf[3] = (byte) (val >> 24);
        buf[4] = (byte) (val >> 32);
        buf[5] = (byte) (val >> 40);
        buf[6] = (byte) (val >> 48);
        buf[7] = (byte) (val >> 56);
        return writePacket(cmd, buf);
    }

    public boolean writePacket(int cmd) {
        return writePacket(cmd, null);
    }

    public boolean writePacket(WoWpacket packet) {
        return writePacket(packet.code(), packet.data());
    }
}
