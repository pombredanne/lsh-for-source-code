package peer;

import condivisi.Descrittore;
import condivisi.ErrorException;
import condivisi.InterfacciaRMI;
import condivisi.Porte;
import gui.BitCreekGui;
import java.awt.Cursor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Task che si occupa di creare e pubblicare un creek
 * @author Bandettini Alberto
 * @author Lottarini Andrea
 * @version BitCreekPeer 1.0
 */
public class Crea implements Runnable {

    /** File associato al creek */
    private File sorgente;

    /** Peer */
    private BitCreekPeer peer;

    /** Gui */
    private BitCreekGui gui;

    /**
     * Costruttore
     * @param sorgente file da pubblicare
     * @param peer logica del peer
     * @param gui
     * @throws ErrorException se lameno un parametro è null
     */
    public Crea(File sorgente, BitCreekPeer peer, BitCreekGui gui) throws ErrorException {
        if (sorgente == null || peer == null || gui == null) {
            throw new ErrorException("Param null");
        }
        if (sorgente.length() == 0) {
            throw new ErrorException("Empty File");
        }
        this.sorgente = sorgente;
        this.peer = peer;
        this.gui = gui;
    }

    /**
     * Corpo del task
     */
    public void run() {
        gui.getRootPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        FileInputStream input = null;
        FileOutputStream output = null;
        long dimensione = sorgente.length();
        String nomefilesorgente = sorgente.getName();
        boolean problema = false, presenza = false;
        presenza = peer.presenza(nomefilesorgente);
        Creek c = null;
        if (!presenza) {
            try {
                input = new FileInputStream(sorgente);
                output = new FileOutputStream(new File("./FileCondivisi/" + nomefilesorgente));
                try {
                    copia(input, output);
                } catch (ErrorException ex) {
                    problema = true;
                }
                input.close();
                output.close();
            } catch (FileNotFoundException e) {
                problema = true;
            } catch (IOException e) {
                problema = true;
            }
            Descrittore descr = null;
            if (!problema) {
                try {
                    descr = new Descrittore(nomefilesorgente, dimensione, hash(), peer.getStubCb());
                } catch (ErrorException ex) {
                    problema = true;
                }
            }
            Porte p = null;
            InterfacciaRMI stub = peer.getStub();
            if (!problema && stub != null) {
                try {
                    p = stub.inviaDescr(descr, peer.getMioIp(), peer.getPortaRichieste());
                } catch (RemoteException ex) {
                    problema = true;
                }
            } else {
                problema = true;
            }
            if (!problema && p != null) {
                descr.setPortaTCP(p.getPortaTCP());
                descr.setPortaUDP(p.getPortaUDP());
                try {
                    c = new Creek(descr, false, p.getPubblicato());
                } catch (ErrorException ex) {
                    problema = true;
                }
                c.setId(p.getId());
            } else {
                problema = true;
            }
            if (!problema) {
                try {
                    peer.addCreek(c);
                } catch (ErrorException ex) {
                    problema = true;
                }
            }
        }
        if (problema) {
            try {
                if (peer != null) {
                    peer.deleteCreek(c.getName());
                }
            } catch (ErrorException ex) {
            }
            File f = new File("./FileCondivisi/" + nomefilesorgente);
            f.delete();
            gui.PrintInformation("Errore Server", gui.ERRORE);
        } else {
            peer.addTask(new UploadManager(peer, c));
        }
        gui.getRootPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Copia il  file input in output
     * @param input
     * @param output
     */
    private void copia(FileInputStream input, FileOutputStream output) throws ErrorException {
        if (input == null || output == null) {
            throw new ErrorException("Param null");
        }
        FileChannel inChannel = input.getChannel();
        FileChannel outChannel = output.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();
        } catch (IOException e) {
            throw new ErrorException("Casino nella copia del file");
        }
    }

    /**
     * Crea la stringa hash
     * @return hash
     * @throws condivisi.ErrorException
     */
    private byte[] hash() throws ErrorException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new ErrorException("No such Algorithm");
        }
        byte[] arraybyte = null;
        long dim = sorgente.length();
        if (dim < BitCreekPeer.DIMBLOCCO) {
            arraybyte = new byte[(int) dim];
        } else {
            arraybyte = new byte[BitCreekPeer.DIMBLOCCO];
        }
        for (int i = 0; i < arraybyte.length; i++) {
            arraybyte[i] = 0;
        }
        FileInputStream input = null;
        try {
            input = new FileInputStream(sorgente);
        } catch (FileNotFoundException ex) {
            throw new ErrorException("File not Found");
        }
        byte[] arrayris = null;
        int dimhash = 0;
        ArrayList<byte[]> array = new ArrayList<byte[]>();
        try {
            while (input.read(arraybyte) != -1) {
                dim -= arraybyte.length;
                md.update(arraybyte);
                arrayris = md.digest();
                dimhash += arrayris.length;
                array.add(arrayris);
                if (dim != 0) {
                    if (dim < BitCreekPeer.DIMBLOCCO) {
                        arraybyte = new byte[(int) dim];
                    } else {
                        arraybyte = new byte[BitCreekPeer.DIMBLOCCO];
                    }
                    for (int i = 0; i < arraybyte.length; i++) {
                        arraybyte[i] = 0;
                    }
                }
            }
        } catch (IOException ex) {
            throw new ErrorException("IO Problem");
        }
        byte[] hash = new byte[dimhash];
        int k = 0;
        for (int i = 0; i < array.size(); i++) {
            arrayris = array.get(i);
            for (int j = 0; j < arrayris.length; j++) {
                hash[k] = arrayris[j];
                k++;
            }
        }
        return hash;
    }
}
