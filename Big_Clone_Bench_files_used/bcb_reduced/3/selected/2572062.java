package ordo;

import Freenet.crypt.*;
import Freenet.client.*;
import java.math.*;
import java.util.*;
import java.text.*;
import java.net.InetAddress;

public class Ticket implements java.io.Serializable {

    protected int grantedBy, grantedTo, ip;

    protected short port;

    protected long issuedOn;

    protected BigInteger C, R, E[];

    protected DSASignature sig;

    public Ticket(String s) {
        NumericMap m = new NumericMap(s);
        Map h = m.getMap();
        Map sh = m.getStringMap();
        E = new BigInteger[2];
        E[0] = (BigInteger) h.get("e0");
        E[1] = (BigInteger) h.get("e1");
        sig = new DSASignature((BigInteger) h.get("sr"), (BigInteger) h.get("ss"));
        grantedBy = Integer.parseInt((String) sh.get("gb"));
        grantedTo = Integer.parseInt((String) sh.get("gt"));
        ip = Integer.parseInt((String) sh.get("ip"));
        port = Short.parseShort((String) sh.get("pt"));
        issuedOn = Long.parseLong((String) sh.get("io"));
    }

    private Ticket(int gb, int gt, long io, int ip, short port, BigInteger R, BigInteger C, BigInteger[] E, DSASignature sig) {
        grantedBy = gb;
        grantedTo = gt;
        this.sig = sig;
        this.R = R;
        this.C = C;
        this.E = E;
        this.ip = ip;
        this.port = port;
        this.issuedOn = io;
    }

    public int grantedBy() {
        return grantedBy;
    }

    public int grantedTo() {
        return grantedTo;
    }

    protected static void updsrt(SHA1 ctx, short v) {
        ctx.update((byte) (v >> 8));
        ctx.update((byte) v);
    }

    protected static void updint(SHA1 ctx, int v) {
        ctx.update((byte) (v >> 24));
        ctx.update((byte) (v >> 16));
        ctx.update((byte) (v >> 8));
        ctx.update((byte) v);
    }

    public byte[] digest() {
        SHA1 ctx = new SHA1();
        updint(ctx, grantedBy());
        updint(ctx, grantedTo());
        byte[] e1b = Util.MPIbytes(E[0]);
        byte[] e2b = Util.MPIbytes(E[1]);
        ctx.update(e1b, 0, e1b.length);
        ctx.update(e2b, 0, e2b.length);
        updint(ctx, (int) (issuedOn >> 32));
        updint(ctx, (int) issuedOn);
        updint(ctx, ip);
        updsrt(ctx, port);
        byte[] m = ctx.digest();
        return m;
    }

    public static Ticket issue(User from, User to, InetAddress ipaddr, Random r) {
        int grantedBy = from.getEncryptionPublicKey().keyId();
        int grantedTo = to.getEncryptionPublicKey().keyId();
        long io = System.currentTimeMillis();
        BigInteger R = new BigInteger(256, r);
        BigInteger C = Global.DHgroupA.getG().modPow(R, Global.DHgroupA.getP());
        BigInteger[] E = ElGamal.encrypt(to.getEncryptionPublicKey(), C, r);
        byte[] e1b = Util.MPIbytes(E[0]);
        byte[] e2b = Util.MPIbytes(E[1]);
        byte[] ipt = ipaddr.getAddress();
        int ip = ((ipt[3] & 0xff) << 24) + ((ipt[2] & 0xff) << 16) + ((ipt[1] & 0xff) << 8) + (ipt[0] & 0xff);
        short port = (short) from.getPort();
        SHA1 ctx = new SHA1();
        updint(ctx, grantedBy);
        updint(ctx, grantedTo);
        ctx.update(e1b, 0, e1b.length);
        ctx.update(e2b, 0, e2b.length);
        updint(ctx, (int) (io >> 32));
        updint(ctx, (int) io);
        updint(ctx, ip);
        updsrt(ctx, port);
        byte[] m = ctx.digest();
        DSASignature sig = DSA.sign(from.getSignaturePublicKey().getGroup(), from.getSignaturePrivateKey(), Util.byteArrayToMPI(m), r);
        return new Ticket(grantedBy, grantedTo, io, ip, port, R, C, E, sig);
    }

    static int mbl(byte[] b, int sp) {
        int len = ((b[sp] & 0xff) << 8) + (b[sp + 1] & 0xff);
        return ((len + 7) >> 3);
    }

    public short getPort() {
        return port;
    }

    public String getIPAddress() {
        StringBuffer sb = new StringBuffer();
        sb.append((ip >> 24) & 0xff).append('.');
        sb.append((ip >> 16) & 0xff).append('.');
        sb.append((ip >> 8) & 0xff).append('.');
        sb.append(ip & 0xff);
        return sb.toString();
    }

    public String asText() {
        NumericMap m = new NumericMap();
        m.addField("gb", Integer.toString(grantedBy));
        m.addField("gt", Integer.toString(grantedTo));
        m.addField("ip", Integer.toString(ip));
        m.addField("io", Long.toString(issuedOn));
        m.addField("sr", sig.getR());
        m.addField("ss", sig.getS());
        m.addField("e0", E[0]);
        m.addField("e1", E[1]);
        m.addField("pt", Short.toString(port));
        return m.toString();
    }

    public String asMessage() {
        StringBuffer b = new StringBuffer(Version.header("TICKET"));
        String s = asText();
        while (s.length() > 0) {
            int cc = Math.min(64, s.length());
            b.append(s.substring(0, cc)).append('\n');
            s = s.substring(cc);
        }
        b.append(Version.trailer("TICKET"));
        return b.toString();
    }

    public boolean verify(User from) {
        return from.getEncryptionPublicKey().keyId() == grantedBy && DSA.verify(from.getSignaturePublicKey(), sig, Util.byteArrayToMPI(digest()));
    }

    public TicketResponse issueResponse(User from, InetAddress ipaddr, Random r) {
        SHA1 ctx = new SHA1();
        C = ElGamal.decrypt(from.getEncryptionPublicKey().getGroup(), from.getEncryptionPrivateKey(), E);
        System.err.println(C);
        updint(ctx, grantedBy);
        byte[] cab = Util.MPIbytes(C);
        ctx.update(cab, 0, cab.length);
        updint(ctx, (int) (issuedOn >> 32));
        updint(ctx, (int) issuedOn);
        updint(ctx, grantedTo);
        BigInteger Ra = new BigInteger(256, r);
        BigInteger Ca = Global.DHgroupA.getG().modPow(Ra, Global.DHgroupA.getP());
        byte[] cbb = Util.MPIbytes(Ca);
        ctx.update(cbb, 0, cbb.length);
        byte[] ipt = ipaddr.getAddress();
        int ip = ((ipt[3] & 0xff) << 24) + ((ipt[2] & 0xff) << 16) + ((ipt[1] & 0xff) << 8) + (ipt[0] & 0xff);
        updint(ctx, ip);
        short port = (short) from.getPort();
        updsrt(ctx, port);
        byte[] dig = ctx.digest();
        BigInteger M = Util.byteArrayToMPI(dig);
        DSASignature sig = DSA.sign(from.getSignaturePublicKey().getGroup(), from.getSignaturePrivateKey(), M, r);
        return new TicketResponse(grantedTo, grantedBy, ip, port, Ra, Ca, sig);
    }

    public long issueDate() {
        return issuedOn;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Ordo Chat ticket from keyId ").append(grantedBy);
        b.append(" issued to keyId ").append(grantedTo);
        b.append("\non ").append(DateFormat.getDateTimeInstance().format(new Date(issueDate())));
        return b.toString();
    }
}
