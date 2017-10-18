package networkcontroller.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import networkcontroller.NetworkConfiguration;
import networkcontroller.xml.XML;

/**
 * class for all packets send on network
 * 
 * @author maciek
 * 
 */
public abstract class Packet {

    /**
	 * size of size field in packet
	 */
    public static final int SIZE_SIZE = 8;

    /**
	 * size of message type in packet
	 */
    public static final int MESSEGE_TYPE_SIZE = 1;

    /**
	 * size of md5sum in packet
	 */
    public static final int MD5SUM_SIZE = 16;

    /**
	 * code for search packet
	 */
    public static final byte SEARCH_CODE = 0;

    /**
	 * code for get packet
	 */
    public static final byte GET_CODE = 1;

    /**
	 * code for result packet
	 */
    public static final byte RESULT_CODE = 2;

    /**
	 * code for segment packet
	 */
    public static final byte SEGMENT_CODE = 3;

    /**
	 * code for ask packet
	 */
    public static final byte ASK_CODE = 4;

    /**
	 * code for alive packet
	 */
    public static final byte ALIVE_CODE = 5;

    /**
	 * code for repeat packet
	 */
    public static final byte REPEAT_CODE = 6;

    /**
	 * handles xml data of packet
	 */
    protected XML xml;

    /**
	 * type of packet
	 */
    protected int type;

    /**
	 * packet which came from network
	 */
    private DatagramPacket packet;

    /**
	 * is this a packet send over tcp connection
	 */
    private boolean tcpPacket;

    /**
	 * is reply needed to this packet in the same tcp connection
	 */
    private boolean needReply;

    /**
	 * data of packet without size and checksum
	 */
    protected byte[] packetData;

    /**
	 * construct packet
	 * 
	 * @param tcpPacket
	 *            is packet over tcp connection
	 * @param needReply
	 *            is reply needed in same tcp connection
	 */
    public Packet(boolean tcpPacket, boolean needReply) {
        this.tcpPacket = tcpPacket;
        this.needReply = needReply;
    }

    /**
	 * creates packet using data conatained in xml
	 */
    protected void createPacket() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] xmlData = xml.getXMLByteData();
            ByteArrayOutputStream baos;
            long size;
            byte[] md5Sum = null;
            if (tcpPacket == false) {
                digest.update(xmlData);
                md5Sum = digest.digest();
                size = MESSEGE_TYPE_SIZE + MD5SUM_SIZE + xmlData.length;
                baos = new ByteArrayOutputStream(SIZE_SIZE + MESSEGE_TYPE_SIZE + MD5SUM_SIZE + xmlData.length);
            } else {
                size = MESSEGE_TYPE_SIZE + xmlData.length;
                baos = new ByteArrayOutputStream(SIZE_SIZE + MESSEGE_TYPE_SIZE + xmlData.length);
            }
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeLong(size);
            dos.writeByte(type);
            if (tcpPacket == false) dos.write(md5Sum);
            dos.write(xmlData);
            packetData = baos.toByteArray();
            if (tcpPacket == false) packet = new DatagramPacket(packetData, packetData.length, NetworkConfiguration.getInstance().getMulticastAddress(), NetworkConfiguration.getInstance().getPort());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * send packet to network
	 */
    protected void sendPacket() {
        System.out.println("sending packet");
        try {
            MulticastSocket socket = new MulticastSocket();
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * get packet data
	 * 
	 * @return packet data
	 */
    public byte[] getPacketData() {
        return packetData;
    }

    /**
	 * is reply needed in same tcp connection
	 * 
	 * @return reply needed?
	 */
    public boolean isNeedReply() {
        return needReply;
    }
}
