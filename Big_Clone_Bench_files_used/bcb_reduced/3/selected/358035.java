package knet.net.chord;

import knet.net.*;
import knet.crypt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import knet.util.*;
import java.security.*;

public class ChordSocket implements KSocket, Constants, Task {

    ChordInterface iface;

    ChordProto proto;

    int task;

    ChordAddress target;

    int targetTask;

    byte[] replyBuffer;

    int replyLen;

    byte[] buffer;

    static final boolean isDebugPair = false;

    static final boolean isDebugTask = false;

    static final boolean isDebugData = false;

    int pairReplicas = 2;

    PairTask pairTask;

    ChordSocket(ChordInterface anIface) {
        iface = anIface;
        proto = iface.getProto();
        buffer = new byte[Constants.MAX_PACKETLEN + 2];
        replyBuffer = new byte[Constants.MAX_PACKETLEN + 2];
        pairTask = new PairTask();
        task = iface.newTask(this);
    }

    public void connect(KAddress peer, int port) throws IOException {
        target = (ChordAddress) peer;
        targetTask = port;
    }

    public synchronized void disconnect() {
        if (task != 0) {
            iface.deleteTask(task);
            task = 0;
        }
    }

    protected void finalize() {
        disconnect();
    }

    public int getLocalPort() {
        return task;
    }

    public void setTimeout(int millis) {
    }

    public int getTimeout() {
        return 0;
    }

    public synchronized void send(KPacket aPacket) throws IOException {
        int curoff = proto.makeDataPacket(task, target, targetTask, buffer, aPacket);
        if (isDebugData) System.out.println(iface.getAddress() + ".send  " + Tests.bufferToString(buffer, curoff));
        proto.outputNoWait(buffer, 0, curoff, target);
    }

    public synchronized void sendTo(KPacket aPacket, KDestination dest) throws IOException {
        ChordAddress caddress = (ChordAddress) dest.getAddress();
        int curoff = proto.makeDataPacket(task, caddress, dest.getPort(), buffer, aPacket);
        if (isDebugData) System.out.println(iface.getAddress() + ".send  " + Tests.bufferToString(buffer, curoff));
        proto.outputNoWait(buffer, 0, curoff, caddress);
    }

    public synchronized void receive(KPacket aPacket) throws IOException {
        if (replyLen == 0) {
            try {
                proto.waitInput(this, 0);
            } catch (InterruptedException e) {
                throw new InterruptedIOException("input wait interrupted");
            }
        }
        int off = 2;
        int len = replyLen - 2;
        off += proto.getLinkHeaderLen();
        len -= proto.getLinkHeaderLen();
        int vlen = proto.deserializeWord(replyBuffer, off);
        off += 2;
        len -= 2;
        if (isDebugData) System.out.println(iface.getAddress() + ".recv   " + Tests.bufferToString(replyBuffer, replyLen, off));
        aPacket.copy(replyBuffer, off, len);
        if (isDebugData) System.out.println(iface.getAddress() + ".recv   " + new String(replyBuffer, off, len));
        replyLen = 0;
    }

    public void setPairReplicas(int aReplicas) {
        pairReplicas = aReplicas;
    }

    public synchronized void handlePacket(byte[] buffer, int off, int len) {
        replyLen = off + len;
        System.arraycopy(buffer, 0, replyBuffer, 0, replyLen);
        if (isDebugTask) System.out.println("Notify task " + this + " with " + buffer);
        notify();
    }

    synchronized void putPair(SignedPair pair) {
        byte[] key = pair.getKey();
        MessageDigest digest = Crypt.getDigest();
        byte[] curHash = new byte[digest.getDigestLength()];
        pairTask.replyLen = 0;
        int localTask = iface.newTask(pairTask);
        try {
            for (int replica = 0; replica < pairReplicas; replica++) {
                if (replica == 0) {
                    digest.update(key, 0, key.length);
                } else {
                    digest.update(curHash, 0, curHash.length);
                }
                digest.digest(curHash, 0, curHash.length);
                ChordAddress address = new ChordAddress(curHash, 0);
                if (isDebugPair) System.out.println(iface.getAddress() + ".putPair(" + address + ")");
                try {
                    if (iface.routesToMe(address)) {
                        iface.realPut(pair);
                        if (isDebugPair) System.out.println(iface.getAddress() + ".putPair(" + ")=myself @" + address);
                    } else {
                        int curoff = proto.makePutPairPacket(address, buffer, localTask, pair);
                        boolean success = proto.outputAndWait(pairTask, buffer, 0, curoff, address);
                        if (!success) {
                            if (isDebugPair) System.out.println(iface.getAddress() + ".putPair(" + ") failed @" + address);
                        }
                        pairTask.replyLen = 0;
                    }
                } catch (InterruptedException ie) {
                    System.err.println("ChordProto: putPair timeout @" + address);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.err.println("ChordProto: putPair IO exception @" + address);
                }
            }
        } catch (DigestException e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString());
        } finally {
            iface.deleteTask(localTask);
        }
        return;
    }

    synchronized SignedPair getPair(byte[] key) {
        MessageDigest digest = Crypt.getDigest();
        byte[] curHash = new byte[digest.getDigestLength()];
        int localTask = iface.newTask(pairTask);
        try {
            for (int replica = 0; replica < pairReplicas; replica++) {
                if (replica == 0) {
                    digest.update(key, 0, key.length);
                } else {
                    digest.update(curHash, 0, curHash.length);
                }
                digest.digest(curHash, 0, curHash.length);
                ChordAddress address = new ChordAddress(curHash, 0);
                if (isDebugPair) System.out.println(iface.getAddress() + ".getPair(" + address + ")");
                try {
                    if (iface.routesToMe(address)) {
                        return (SignedPair) iface.realGet(key);
                    }
                    int curoff = proto.makeGetPairPacket(address, buffer, localTask, key);
                    boolean success = proto.outputAndWait(pairTask, buffer, 0, curoff, address);
                    if (!success) {
                        if (isDebugPair) System.out.println(iface.getAddress() + ".getPair(" + ") failed @" + address);
                    } else {
                        if (pairTask.replyLen == 0) throw new RuntimeException("replyBuffer is empty");
                        int[] offp = new int[1];
                        offp[0] += proto.getLinkHeaderLen() + 2;
                        SignedPair pair = SignedPair.deserialize(pairTask.replyBuffer, offp);
                        if (offp[0] > pairTask.replyLen) {
                            System.out.println(iface.getAddress() + ".get " + pairTask.replyBuffer + "," + Tests.bufferToString(pairTask.replyBuffer, pairTask.replyLen, offp[0]) + " length exceeded " + offp[0] + " > " + pairTask.replyLen);
                            throw new RuntimeException("Malformed packet - length exceeded");
                        }
                        if (isDebugPair) System.out.println(iface.getAddress() + ".get " + pairTask.replyBuffer + "," + Tests.bufferToString(pairTask.replyBuffer, pairTask.replyLen, offp[0]));
                        pairTask.replyLen = 0;
                        if (pair == null) continue;
                        return pair;
                    }
                } catch (InterruptedException ie) {
                    System.err.println("ChordProto: getPair timeout @" + address);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.err.println("ChordProto: getPair IO exception @" + address);
                }
            }
        } catch (DigestException e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString());
        } finally {
            iface.deleteTask(localTask);
        }
        return null;
    }

    public SignedPair get(byte[] key) {
        return getPair(key);
    }

    public void put(SignedPair pair) {
        putPair(pair);
    }

    static class PairTask implements Task {

        byte[] replyBuffer;

        int replyLen;

        PairTask() {
            replyBuffer = new byte[Constants.MAX_PACKETLEN + 2];
        }

        public synchronized void handlePacket(byte[] buffer, int off, int len) {
            replyLen = off + len;
            System.arraycopy(buffer, 0, replyBuffer, 0, replyLen);
            if (isDebugTask) System.out.println("Notify pair task " + this + " with " + buffer);
            notify();
        }
    }

    public int serialize(byte[] buffer, int offset) {
        return ChordProto.serializeEndpoint(buffer, offset, task, iface.getAddress());
    }

    public int deserialize(byte[] buffer, int offset, KDestination dest) {
        int len = buffer.length - offset;
        ChordAddress address = new ChordAddress(buffer, offset);
        offset += Constants.ADDRESS_BYTES;
        int port = ChordProto.deserializeWord(buffer, offset);
        offset += 2;
        dest.setAddress(address);
        dest.setPort(port);
        return offset;
    }
}
