package pubweb.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import padrmi.Server;
import pubweb.IntegrityException;

public class GuidGenerator {

    private static final BigInteger MAX_VALUE_INTEGER = new BigInteger("2").pow(256);

    public static enum PeerType {

        CONSUMER, SUPERNODE, WORKER
    }

    public static String peerTypeToString(PeerType type) {
        switch(type) {
            case CONSUMER:
                return "Consumer";
            case SUPERNODE:
                return "Supernode";
            case WORKER:
                return "Worker";
            default:
                return "[error]";
        }
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String getGuid(InetAddress ip, int port, PeerType type) throws IntegrityException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
            md.update(ip.getAddress());
            md.update((byte) ((port & 0xff00) >> 8));
            md.update((byte) (port & 0xff));
            byte[] hash = md.digest(peerTypeToString(type).getBytes());
            BigInteger x = new BigInteger(hash);
            if (x.signum() == -1) {
                x = MAX_VALUE_INTEGER.add(x);
            }
            return x.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IntegrityException("error creating guid: SHA-256 algorithm not found", e);
        } catch (NoSuchProviderException e) {
            throw new IntegrityException("error creating guid: BouncyCastle provider not found", e);
        }
    }

    public static String getGuid(String ip, String port, PeerType type) throws IntegrityException {
        InetAddress theIp;
        int thePort;
        try {
            theIp = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IntegrityException("error creating guid: invalid PadRMI address", e);
        }
        try {
            thePort = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IntegrityException("error creating guid: invalid PadRMI port", e);
        }
        return getGuid(theIp, thePort, type);
    }

    public static String getGuid(PeerType type) throws IntegrityException {
        String ip = System.getProperty(Server.PROPERTY_ADDRESS);
        if (ip == null || ip.isEmpty()) {
            ip = System.getProperty(Server.PROPERTY_BIND_ADDRESS);
        }
        String port = System.getProperty(Server.PROPERTY_PORT);
        if (port == null || port.isEmpty()) {
            port = System.getProperty(Server.PROPERTY_BIND_PORT);
        }
        return getGuid(ip, port, type);
    }
}
