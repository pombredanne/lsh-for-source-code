package edu.indiana.cs.b534.torrent;

import edu.indiana.cs.b534.torrent.context.TorrentContext;
import edu.indiana.cs.b534.torrent.impl.PeerInstance;
import edu.indiana.cs.b534.torrent.impl.TorrentManager;
import edu.indiana.cs.b534.torrent.message.PeerDictionary;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

public class Utils {

    private static Logger log = Logger.getLogger(TorrentManager.TORRENT_MANAGER_NAME);

    static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static byte[] computeHash(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        return digest.digest(input);
    }

    public static byte[] intToByteArray(final int integer) {
        int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;
        byte[] byteArray = new byte[4];
        for (int n = 0; n < byteNum; n++) byteArray[3 - n] = (byte) (integer >>> (n * 8));
        return (byteArray);
    }

    public static String getStrVal(TDictionary dictionary, TString key, boolean opional) throws TorrentException {
        TString val = (TString) dictionary.getDictionary().get(key);
        if (val != null) {
            return val.getValue();
        } else {
            if (opional) {
                return null;
            } else {
                throw new TorrentException("Key " + key + "does not present in dictionary " + dictionary);
            }
        }
    }

    /**
     * Returns the ip address of the machine running this code
     * CAUTION:
     * This will go through all the available network interfaces and will try to return an ip address.
     * First this will try to get the first IP which is not loopback address (127.0.0.1). If none is found
     * then this will return this will return 127.0.0.1.
     * This will <b>not<b> consider IPv6 addresses.
     * <p/>
     * TODO:
     * - Improve this logic to genaralize it a bit more
     * - Obtain the ip to be used here from the Call API
     *
     * @return Returns String.
     * @throws java.net.SocketException
     */
    public static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";
        while (e.hasMoreElements()) {
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration addresses = netface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }

    private static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

    public static String nicePrint(byte[] data, boolean tight, int max_length) {
        if (data == null) {
            return "";
        }
        int dataLength = data.length;
        if (dataLength > max_length) {
            dataLength = max_length;
        }
        int size = dataLength * 2;
        if (!tight) {
            size += (dataLength - 1) / 4;
        }
        char[] out = new char[size];
        try {
            int pos = 0;
            for (int i = 0; i < dataLength; i++) {
                if ((!tight) && (i % 4 == 0) && i > 0) {
                    out[pos++] = ' ';
                }
                out[pos++] = HEXDIGITS[(byte) ((data[i] >> 4) & 0xF)];
                out[pos++] = HEXDIGITS[(byte) (data[i] & 0xF)];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return new String(out) + (data.length > max_length ? "..." : "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static final int byteArrayToInt(byte[] b, int start) {
        int l = 0;
        l |= b[start] & 0xFF;
        l <<= 8;
        l |= b[start + 1] & 0xFF;
        l <<= 8;
        l |= b[start + 2] & 0xFF;
        l <<= 8;
        l |= b[start + 3] & 0xFF;
        return l;
    }

    public static boolean[] bitsToBooleanArray(byte[] bytes) {
        boolean[] results = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                results[8 * i + j] = (bytes[i] & 1 << (7 - j)) > 0;
            }
        }
        return results;
    }

    public static int readGivenNumberOfBytes(InputStream in, int readCount, byte[] buf) throws TorrentException {
        try {
            if (readCount < 0) {
                throw new TorrentException("Read count must not be Null");
            }
            int read = 0;
            int lastReadcount = 0;
            while (read < readCount) {
                lastReadcount = in.read(buf, read, readCount - read);
                if (lastReadcount < 0) {
                    throw new TorrentException("input stream closed");
                } else {
                    read = read + lastReadcount;
                }
            }
            if (read != readCount) {
                throw new RuntimeException("Utility method has read too much");
            }
            return read;
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                byte[] buf1 = new byte[1024];
                int read = in.read(buf1);
                System.out.println(read);
                for (int i = 0; i < read; i++) {
                    System.out.print(buf1[i]);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw new TorrentException(e);
        } catch (Exception e) {
            throw new TorrentException(e);
        }
    }

    public static byte[] calucateHash(File file, int length) throws TorrentException {
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buf = new byte[length];
            Utils.readGivenNumberOfBytes(in, length, buf);
            return Utils.computeHash(buf);
        } catch (Exception e) {
            throw new TorrentException(e);
        }
    }

    public static boolean compareByteArrays(byte[] hash1, byte[] hash2) {
        return ByteBuffer.wrap(hash1).compareTo(ByteBuffer.wrap(hash2)) == 0;
    }

    public static byte[] getAsBytes(String string, int expectedSize) throws TorrentException {
        ByteBuffer byteBuffer = TStruct.CHARSET.encode(string);
        if (byteBuffer.array().length != expectedSize) {
            throw new TorrentException("Created byte array size is different from the expected size of the buffer. Check the string *" + string + "*");
        }
        return byteBuffer.array();
    }

    public static byte[] convertBooleanArrayToByteArray(boolean[] pieceAvailability) {
        double size = pieceAvailability.length;
        int payloadsize = (int) Math.ceil(size / 8);
        byte[] bytes = new byte[payloadsize];
        for (int i = 0; i < payloadsize; i++) {
            int val = 0;
            for (int j = 0; j < 8; j++) {
                int index = 8 * i + j;
                val = val << 1;
                if (index < pieceAvailability.length && pieceAvailability[index]) {
                    val++;
                }
                if (j == 7) {
                    byte b = (byte) val;
                    bytes[i] = b;
                }
            }
        }
        return bytes;
    }

    public static PeerInstance findPeerInstance(TorrentContext torrentContext, String peerID) {
        PeerInstance peerInstance = torrentContext.getIncomingPeerInformation().get(peerID);
        if (peerInstance == null) {
            peerInstance = torrentContext.getOutgoingPeerInformation().get(peerID);
        }
        return peerInstance;
    }
}
