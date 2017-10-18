package com.gite.jxta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;

public class MySocketServer {

    private PeerGroup peerGroup;

    private MySocketInterface parent;

    private JxtaServerSocket serverSocket;

    private PipeAdvertisement socketAdv;

    private Object finishLock = new Object();

    public MySocketServer(MySocketInterface parent, PeerGroup peerGroup, String digestString) {
        this.parent = parent;
        this.peerGroup = peerGroup;
        setupSocket(digestString);
    }

    public PipeAdvertisement getSocketAdvertisement() {
        return socketAdv;
    }

    private void setupSocket(String digestString) {
        socketAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        try {
            byte[] bid = MessageDigest.getInstance("MD5").digest(digestString.getBytes("ISO-8859-1"));
            PipeID pipeID = IDFactory.newPipeID(peerGroup.getPeerGroupID(), bid);
            socketAdv.setPipeID(pipeID);
            socketAdv.setType(PipeService.UnicastType);
            socketAdv.setName("Socket Pipe");
            socketAdv.setDescription("verbose description");
            System.out.println("Creating JxtaServerSocket");
            serverSocket = new JxtaServerSocket(peerGroup, socketAdv);
            serverSocket.setSoTimeout(0);
            System.out.println("LocalAddress :" + serverSocket.getLocalSocketAddress());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitMessage() {
        try {
            Socket socket = serverSocket.accept();
            if (socket != null) {
                System.out.println("socket created");
                new Thread(new ConnectionHandler(parent, socket), "Connection Handler Thread").start();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Nothing so far...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Stop() {
        try {
            serverSocket.close();
            System.out.println("Connection closed");
            finishLock.notify();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ConnectionHandler implements Runnable {

        private Socket socket;

        private MySocketInterface parent;

        ConnectionHandler(MySocketInterface parent, Socket socket) {
            this.socket = socket;
            this.parent = parent;
        }

        private void handleData(Socket socket) {
            try {
                ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
                MySocketMessageInterface msg = null;
                while (msg == null) {
                    msg = (MySocketMessageInterface) objIn.readObject();
                }
                parent.receiveSocketMessage(msg);
            } catch (Exception ie) {
                ie.printStackTrace();
            }
        }

        public void run() {
            handleData(socket);
        }
    }
}
