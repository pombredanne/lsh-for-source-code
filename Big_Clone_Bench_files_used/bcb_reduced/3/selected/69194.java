package networkcontroller;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import commands.nc.NCAliveInternal;
import commands.nc.NCSearchInternal;
import networkcontroller.packet.Packet;
import networkcontroller.xml.XMLAsk;
import networkcontroller.xml.XMLSearch;
import controller.Controller;

/**
 * 
 * @author maciek Class that listens for incoming multicast packets
 * 
 */
public class MulticastListener implements Runnable {

    /**
	 * controller reference
	 */
    Controller ctrl;

    /**
	 * network controller reference
	 */
    NetworkController netCtrl;

    /**
	 * constructor that sets controller and network controller and starts
	 * listener thread
	 * 
	 * @param ctrl
	 *            controller
	 * @param netCtrl
	 *            network controller
	 */
    MulticastListener(Controller ctrl, NetworkController netCtrl) {
        this.ctrl = ctrl;
        this.netCtrl = netCtrl;
        new Thread(this).start();
    }

    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(NetworkConfiguration.getInstance().getPort());
            socket.joinGroup(NetworkConfiguration.getInstance().getMulticastAddress());
            while (true) {
                System.out.println("waiting for packet");
                byte buffer[] = new byte[10240];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * handles incoming packet
	 * 
	 * @param packet
	 *            incoming packet
	 */
    private void handlePacket(DatagramPacket packet) {
        try {
            byte[] packetData = packet.getData();
            ByteArrayInputStream bais = new ByteArrayInputStream(packetData);
            DataInputStream dis = new DataInputStream(bais);
            long size = dis.readLong();
            byte messageType = dis.readByte();
            byte[] md5Sum = new byte[Packet.MD5SUM_SIZE];
            dis.readFully(md5Sum);
            byte[] data = new byte[(int) size - (Packet.MD5SUM_SIZE + Packet.MESSEGE_TYPE_SIZE)];
            dis.readFully(data);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            byte[] md5SumCalculated = digest.digest();
            if (Arrays.equals(md5Sum, md5SumCalculated) == false) {
                System.out.println("NC: md5sum error");
                for (int i = 0; i < 16; i++) System.out.print(((int) md5Sum[i] + 127) + " ");
                System.out.println("");
                for (int i = 0; i < 16; i++) System.out.print((int) md5SumCalculated[i] + 127);
                System.out.println("");
                return;
            }
            switch(messageType) {
                case Packet.SEARCH_CODE:
                    XMLSearch xmlSearch = new XMLSearch(data);
                    System.out.println("NC: Got search message");
                    System.out.println("NC: packet contents:\n" + new String(data));
                    System.out.println("NC: XML search:" + xmlSearch.getHost() + ":" + xmlSearch.getPort());
                    netCtrl.writeMsg(new NCSearchInternal(xmlSearch.getHost(), xmlSearch.getPort(), xmlSearch.getName(), xmlSearch.getSizeMin(), xmlSearch.getSizeMax(), xmlSearch.getSum(), xmlSearch.getId()));
                    ;
                    break;
                case Packet.ASK_CODE:
                    System.out.println("NC: Got ask message");
                    System.out.println("NC: packet contents:\n" + new String(data));
                    XMLAsk xmlAsk = new XMLAsk(data);
                    netCtrl.writeMsg(new NCAliveInternal(xmlAsk.getHost(), xmlAsk.getPort()));
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
