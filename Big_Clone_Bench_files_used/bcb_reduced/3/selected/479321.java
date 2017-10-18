package com.gite.jxta;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaMulticastSocket;

public class MyMulticastSocketServer {

    private PeerGroup peerGroup;

    private MyMulticastSocketInterface parent;

    private JxtaMulticastSocket multicastSocketServer;

    private PipeAdvertisement propagatedPipeAdv;

    private Object finishLock = new Object();

    public MyMulticastSocketServer(MyMulticastSocketInterface parent, PeerGroup peerGroup, String digestString) {
        this.parent = parent;
        this.peerGroup = peerGroup;
        setupSocket(digestString);
    }

    public PipeAdvertisement getPipeAdvertisement() {
        return propagatedPipeAdv;
    }

    private void setupSocket(String digestString) {
        propagatedPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        try {
            byte[] bid = MessageDigest.getInstance("MD5").digest(digestString.getBytes("ISO-8859-1"));
            PipeID pipeID = IDFactory.newPipeID(peerGroup.getPeerGroupID(), bid);
            propagatedPipeAdv.setPipeID(pipeID);
            propagatedPipeAdv.setType(PipeService.PropagateType);
            propagatedPipeAdv.setName("The multicastsocket pipe");
            propagatedPipeAdv.setDescription("verbose description");
            System.out.println("Creating JxtaMulticastSocket");
            multicastSocketServer = new JxtaMulticastSocket(peerGroup, propagatedPipeAdv);
            multicastSocketServer.setSoTimeout(0);
            System.out.println("LocalAddress :" + multicastSocketServer.getLocalAddress());
            System.out.println("LocalSocketAddress :" + multicastSocketServer.getLocalSocketAddress());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitPacket() {
        byte[] buffer = new byte[16384];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            multicastSocketServer.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        parent.pipeEvent(packet);
    }

    public void Stop() {
        finishLock.notify();
    }
}
