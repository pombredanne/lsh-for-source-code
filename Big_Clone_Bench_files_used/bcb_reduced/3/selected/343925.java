package com.gite.jxta;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

public class MyPropagatedPipeServer implements PipeMsgListener {

    private InputPipe inputPipe;

    private PeerGroup peerGroup;

    private MyPropagatedPipeInterface parent;

    private PipeAdvertisement propagatedPipeAdv;

    private Object finishLock = new Object();

    public MyPropagatedPipeServer(MyPropagatedPipeInterface parent, PeerGroup peerGroup, String digestString) {
        this.parent = parent;
        this.peerGroup = peerGroup;
        setupPipe(digestString);
    }

    public PipeAdvertisement getPipeAdvertisement() {
        return propagatedPipeAdv;
    }

    private void setupPipe(String digestString) {
        propagatedPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        try {
            byte[] bid = MessageDigest.getInstance("MD5").digest(digestString.getBytes("ISO-8859-1"));
            PipeID pipeID = IDFactory.newPipeID(peerGroup.getPeerGroupID(), bid);
            propagatedPipeAdv.setPipeID(pipeID);
            propagatedPipeAdv.setType(PipeService.PropagateType);
            propagatedPipeAdv.setName("A chattering propagate pipe");
            propagatedPipeAdv.setDescription("verbose description");
            PipeService pipeService = peerGroup.getPipeService();
            inputPipe = pipeService.createInputPipe(propagatedPipeAdv, this);
            System.out.println("Propagate pipes and listeners created");
            System.out.println("Propagate PipeID: " + pipeID.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pipeMsgEvent(PipeMsgEvent msg) {
        System.out.println("Message received!");
        parent.pipeEvent(msg);
    }

    /**
     * Keep running
     */
    public void waitForever() {
        try {
            System.out.println("Waiting for Messages.");
            synchronized (finishLock) {
                finishLock.wait();
            }
            inputPipe.close();
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Stop() {
        finishLock.notify();
    }
}
