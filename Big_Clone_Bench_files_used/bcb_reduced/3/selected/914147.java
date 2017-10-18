package ch.comtools.jsch;

public class DHG1 extends KeyExchange {

    static final byte[] g = { 2 };

    static final byte[] p = { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2, (byte) 0x21, (byte) 0x68, (byte) 0xC2, (byte) 0x34, (byte) 0xC4, (byte) 0xC6, (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC, (byte) 0x1C, (byte) 0xD1, (byte) 0x29, (byte) 0x02, (byte) 0x4E, (byte) 0x08, (byte) 0x8A, (byte) 0x67, (byte) 0xCC, (byte) 0x74, (byte) 0x02, (byte) 0x0B, (byte) 0xBE, (byte) 0xA6, (byte) 0x3B, (byte) 0x13, (byte) 0x9B, (byte) 0x22, (byte) 0x51, (byte) 0x4A, (byte) 0x08, (byte) 0x79, (byte) 0x8E, (byte) 0x34, (byte) 0x04, (byte) 0xDD, (byte) 0xEF, (byte) 0x95, (byte) 0x19, (byte) 0xB3, (byte) 0xCD, (byte) 0x3A, (byte) 0x43, (byte) 0x1B, (byte) 0x30, (byte) 0x2B, (byte) 0x0A, (byte) 0x6D, (byte) 0xF2, (byte) 0x5F, (byte) 0x14, (byte) 0x37, (byte) 0x4F, (byte) 0xE1, (byte) 0x35, (byte) 0x6D, (byte) 0x6D, (byte) 0x51, (byte) 0xC2, (byte) 0x45, (byte) 0xE4, (byte) 0x85, (byte) 0xB5, (byte) 0x76, (byte) 0x62, (byte) 0x5E, (byte) 0x7E, (byte) 0xC6, (byte) 0xF4, (byte) 0x4C, (byte) 0x42, (byte) 0xE9, (byte) 0xA6, (byte) 0x37, (byte) 0xED, (byte) 0x6B, (byte) 0x0B, (byte) 0xFF, (byte) 0x5C, (byte) 0xB6, (byte) 0xF4, (byte) 0x06, (byte) 0xB7, (byte) 0xED, (byte) 0xEE, (byte) 0x38, (byte) 0x6B, (byte) 0xFB, (byte) 0x5A, (byte) 0x89, (byte) 0x9F, (byte) 0xA5, (byte) 0xAE, (byte) 0x9F, (byte) 0x24, (byte) 0x11, (byte) 0x7C, (byte) 0x4B, (byte) 0x1F, (byte) 0xE6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xEC, (byte) 0xE6, (byte) 0x53, (byte) 0x81, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    private static final int SSH_MSG_KEXDH_INIT = 30;

    private static final int SSH_MSG_KEXDH_REPLY = 31;

    static final int RSA = 0;

    static final int DSS = 1;

    private int type = 0;

    private int state;

    DH dh;

    byte[] V_S;

    byte[] V_C;

    byte[] I_S;

    byte[] I_C;

    byte[] e;

    private Buffer buf;

    private Packet packet;

    public void init(Session session, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws Exception {
        this.session = session;
        this.V_S = V_S;
        this.V_C = V_C;
        this.I_S = I_S;
        this.I_C = I_C;
        try {
            Class c = Class.forName(session.getConfig("sha-1"));
            sha = (HASH) (c.newInstance());
            sha.init();
        } catch (Exception e) {
            System.err.println(e);
        }
        buf = new Buffer();
        packet = new Packet(buf);
        try {
            Class c = Class.forName(session.getConfig("dh"));
            dh = (DH) (c.newInstance());
            dh.init();
        } catch (Exception e) {
            throw e;
        }
        dh.setP(p);
        dh.setG(g);
        e = dh.getE();
        packet.reset();
        buf.putByte((byte) SSH_MSG_KEXDH_INIT);
        buf.putMPInt(e);
        session.write(packet);
        if (JSch.getLogger().isEnabled(Logger.INFO)) {
            JSch.getLogger().log(Logger.INFO, "SSH_MSG_KEXDH_INIT sent");
            JSch.getLogger().log(Logger.INFO, "expecting SSH_MSG_KEXDH_REPLY");
        }
        state = SSH_MSG_KEXDH_REPLY;
    }

    public boolean next(Buffer _buf) throws Exception {
        int i, j;
        switch(state) {
            case SSH_MSG_KEXDH_REPLY:
                j = _buf.getInt();
                j = _buf.getByte();
                j = _buf.getByte();
                if (j != 31) {
                    System.err.println("type: must be 31 " + j);
                    return false;
                }
                K_S = _buf.getString();
                byte[] f = _buf.getMPInt();
                byte[] sig_of_H = _buf.getString();
                dh.setF(f);
                K = dh.getK();
                buf.reset();
                buf.putString(V_C);
                buf.putString(V_S);
                buf.putString(I_C);
                buf.putString(I_S);
                buf.putString(K_S);
                buf.putMPInt(e);
                buf.putMPInt(f);
                buf.putMPInt(K);
                byte[] foo = new byte[buf.getLength()];
                buf.getByte(foo);
                sha.update(foo, 0, foo.length);
                H = sha.digest();
                i = 0;
                j = 0;
                j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                String alg = new String(K_S, i, j);
                i += j;
                boolean result = false;
                if (alg.equals("ssh-rsa")) {
                    byte[] tmp;
                    byte[] ee;
                    byte[] n;
                    type = RSA;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    ee = tmp;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    n = tmp;
                    SignatureRSA sig = null;
                    try {
                        Class c = Class.forName(session.getConfig("signature.rsa"));
                        sig = (SignatureRSA) (c.newInstance());
                        sig.init();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                    sig.setPubKey(ee, n);
                    sig.update(H);
                    result = sig.verify(sig_of_H);
                    if (JSch.getLogger().isEnabled(Logger.INFO)) {
                        JSch.getLogger().log(Logger.INFO, "ssh_rsa_verify: signature " + result);
                    }
                } else if (alg.equals("ssh-dss")) {
                    byte[] q = null;
                    byte[] tmp;
                    byte[] p;
                    byte[] g;
                    type = DSS;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    p = tmp;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    q = tmp;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    g = tmp;
                    j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00) | ((K_S[i++]) & 0x000000ff);
                    tmp = new byte[j];
                    System.arraycopy(K_S, i, tmp, 0, j);
                    i += j;
                    f = tmp;
                    SignatureDSA sig = null;
                    try {
                        Class c = Class.forName(session.getConfig("signature.dss"));
                        sig = (SignatureDSA) (c.newInstance());
                        sig.init();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                    sig.setPubKey(f, p, q, g);
                    sig.update(H);
                    result = sig.verify(sig_of_H);
                    if (JSch.getLogger().isEnabled(Logger.INFO)) {
                        JSch.getLogger().log(Logger.INFO, "ssh_dss_verify: signature " + result);
                    }
                } else {
                    System.err.println("unknown alg");
                }
                state = STATE_END;
                return result;
        }
        return false;
    }

    public String getKeyType() {
        if (type == DSS) return "DSA";
        return "RSA";
    }

    public int getState() {
        return state;
    }
}
