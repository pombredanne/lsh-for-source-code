package ordo;

import java.io.*;
import java.util.Map;
import Freenet.crypt.*;
import java.math.*;

public class TicketResponse implements Serializable {

    public int grantedBy, ip;

    public short port;

    public transient int grantedTo;

    public BigInteger R, C;

    public DSASignature sig;

    public TicketResponse(String s) {
        NumericMap m = new NumericMap(s);
        Map h = m.getMap();
        Map sh = m.getStringMap();
        C = (BigInteger) h.get("c");
        sig = new DSASignature((BigInteger) h.get("sr"), (BigInteger) h.get("ss"));
        grantedBy = Integer.parseInt((String) sh.get("gb"));
        ip = Integer.parseInt((String) sh.get("ip"));
        port = Short.parseShort((String) sh.get("pt"));
    }

    public String asText() {
        NumericMap nm = new NumericMap();
        nm.addField("sr", sig.getR());
        nm.addField("ss", sig.getS());
        nm.addField("gb", Integer.toString(grantedBy()));
        nm.addField("ip", Integer.toString(ip));
        nm.addField("pt", Short.toString(port));
        nm.addField("c", C);
        return nm.toString();
    }

    public TicketResponse(int rb, int rt, int ip, short prt, BigInteger R, BigInteger C, DSASignature sig) {
        this.grantedBy = rb;
        this.grantedTo = rt;
        this.ip = ip;
        this.port = prt;
        this.R = R;
        this.C = C;
        this.sig = sig;
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

    public boolean verify(int rt, long io, User from, BigInteger Ca) {
        SHA1 ctx = new SHA1();
        updint(ctx, rt);
        byte[] cab = Util.MPIbytes(Ca);
        ctx.update(cab, 0, cab.length);
        updint(ctx, (int) (io >> 32));
        updint(ctx, (int) io);
        updint(ctx, grantedBy);
        byte[] cbb = Util.MPIbytes(C);
        ctx.update(cbb, 0, cbb.length);
        updint(ctx, ip);
        updsrt(ctx, port);
        byte[] dig = ctx.digest();
        BigInteger M = Util.byteArrayToMPI(dig);
        return DSA.verify(from.getSignaturePublicKey(), sig, M);
    }

    public String getIPAddress() {
        StringBuffer sb = new StringBuffer();
        sb.append((ip >> 24) & 0xff).append('.');
        sb.append((ip >> 16) & 0xff).append('.');
        sb.append((ip >> 8) & 0xff).append('.');
        sb.append(ip & 0xff);
        return sb.toString();
    }
}
