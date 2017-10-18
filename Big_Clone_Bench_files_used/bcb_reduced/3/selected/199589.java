package ru.stdio.sRemote;

import java.nio.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.LinkedList;
import java.util.List;

public class udpHid {

    static final int UDPHID_MAX_PACKET = 256;

    static final int UDPHID_DEFAULT_TIMEOUT = 1000;

    static final int UDPHID_DEFAULT_ATTEMPTS = 3;

    static final String UDPHID_NAME = "UDP HID Java";

    static final int UDPHID_PROTO = 0x0100;

    static final int REQ_CHALLENGE = 0x01;

    static final int REQ_RESPONSE = 0x02;

    static final int REQ_SUCCESS = 0x03;

    static final int REQ_FAILURE = 0x04;

    static final int REQ_DESCRIPTOR = 0x05;

    static final int REQ_REPORT = 0x06;

    static final int ANS_CHALLENGE = 0x01;

    static final int ANS_SUCCESS = 0x03;

    static final int ANS_FAILURE = 0x04;

    static final int ANS_REQAUTH = 0x05;

    static final int ANS_REQDESCRIPTOR = 0x06;

    static final int ANS_REPORT = 0x07;

    private ByteBuffer buffer;

    InetAddress addr;

    int port;

    private byte[] sid;

    private boolean authorized;

    private int seqNumber;

    private String name;

    private String login, password;

    private byte[] descriptor;

    private List<byte[]> queueReport = new LinkedList<byte[]>();

    private byte[] htons(short sValue) {
        byte[] baValue = new byte[2];
        ByteBuffer buf = ByteBuffer.wrap(baValue);
        return buf.putShort(sValue).array();
    }

    private byte[] htonl(int sValue) {
        byte[] baValue = new byte[4];
        ByteBuffer buf = ByteBuffer.wrap(baValue);
        return buf.putInt(sValue).array();
    }

    private void auth() throws IOException {
        authorized = false;
        seqNumber = 0;
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(UDPHID_DEFAULT_TIMEOUT);
        ds.connect(addr, port);
        DatagramPacket p = new DatagramPacket(buffer.array(), buffer.capacity());
        for (int i = 0; i < UDPHID_DEFAULT_ATTEMPTS; i++) {
            buffer.clear();
            buffer.put((byte) REQ_CHALLENGE);
            buffer.put(htons((short) UDPHID_PROTO));
            buffer.put(name.getBytes());
            ds.send(new DatagramPacket(buffer.array(), buffer.position()));
            buffer.clear();
            try {
                ds.receive(p);
            } catch (SocketTimeoutException e) {
                continue;
            }
            switch(buffer.get()) {
                case ANS_CHALLENGE:
                    break;
                case ANS_FAILURE:
                    throw new IOException("REQ_FAILURE");
                default:
                    throw new IOException("invalid packet");
            }
            byte challenge_id = buffer.get();
            int challenge_len = (int) buffer.get();
            byte[] challenge = new byte[challenge_len];
            buffer.get(challenge, 0, p.getLength() - buffer.position());
            byte[] response;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(challenge_id);
                md.update(password.getBytes(), 0, password.length());
                md.update(challenge, 0, challenge.length);
                response = md.digest();
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("NoSuchAlgorithmException: " + e.toString());
            }
            buffer.clear();
            buffer.put((byte) REQ_RESPONSE);
            buffer.put(challenge_id);
            buffer.put((byte) response.length);
            buffer.put(response);
            buffer.put(login.getBytes());
            ds.send(new DatagramPacket(buffer.array(), buffer.position()));
            buffer.clear();
            try {
                ds.receive(p);
            } catch (SocketTimeoutException e) {
                continue;
            }
            switch(buffer.get()) {
                case ANS_SUCCESS:
                    int sidLength = buffer.get();
                    sid = new byte[sidLength];
                    buffer.get(sid, 0, sidLength);
                    authorized = true;
                    return;
                case ANS_FAILURE:
                    throw new IOException("access deny");
                default:
                    throw new IOException("invalid packet");
            }
        }
        throw new IOException("operation time out");
    }

    private void setDescriptor() throws IOException {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(UDPHID_DEFAULT_TIMEOUT);
        ds.connect(addr, port);
        DatagramPacket p = new DatagramPacket(buffer.array(), buffer.capacity());
        for (int i = 0; i < UDPHID_DEFAULT_ATTEMPTS; i++) {
            buffer.clear();
            buffer.put((byte) REQ_DESCRIPTOR);
            buffer.put((byte) sid.length);
            buffer.put(sid);
            buffer.put((byte) descriptor.length);
            buffer.put(descriptor);
            ds.send(new DatagramPacket(buffer.array(), buffer.position()));
            buffer.clear();
            try {
                ds.receive(p);
            } catch (SocketTimeoutException e) {
                continue;
            }
            switch(buffer.get()) {
                case ANS_SUCCESS:
                    int sidLength = buffer.get();
                    sid = new byte[sidLength];
                    buffer.get(sid, 0, sidLength);
                    return;
                case ANS_FAILURE:
                    throw new IOException("ANS_FAILURE");
                case ANS_REQAUTH:
                    auth();
                    break;
                default:
                    throw new IOException("invalid packet");
            }
        }
        throw new IOException("operation time out");
    }

    public udpHid(String hostname, int port) throws UnknownHostException, SocketException {
        name = UDPHID_NAME;
        authorized = false;
        addr = InetAddress.getByName(hostname);
        this.port = port;
        buffer = ByteBuffer.allocate(UDPHID_MAX_PACKET);
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void setName(String newName) {
        name = newName;
    }

    public void auth(String newLogin, String newPassword) throws IOException {
        login = newLogin;
        password = newPassword;
        auth();
    }

    public void setDescriptor(byte[] newDescriptor) throws IOException {
        descriptor = newDescriptor;
        setDescriptor();
    }

    public void addReport(byte[] report) {
        queueReport.add(report);
    }

    public void sendReport() throws IOException {
        seqNumber++;
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(UDPHID_DEFAULT_TIMEOUT);
        ds.connect(addr, port);
        DatagramPacket p = new DatagramPacket(buffer.array(), buffer.capacity());
        for (int i = 0; i < UDPHID_DEFAULT_ATTEMPTS; i++) {
            buffer.clear();
            buffer.put((byte) REQ_REPORT);
            buffer.put((byte) sid.length);
            buffer.put(sid);
            buffer.put(htonl(seqNumber));
            for (byte report[] : queueReport) {
                buffer.put((byte) report.length);
                buffer.put(report);
            }
            ds.send(new DatagramPacket(buffer.array(), buffer.position()));
            buffer.clear();
            try {
                ds.receive(p);
            } catch (SocketTimeoutException e) {
                continue;
            }
            switch(buffer.get()) {
                case ANS_SUCCESS:
                    queueReport.clear();
                    return;
                case ANS_FAILURE:
                    queueReport.clear();
                    throw new IOException("ANS_FAILURE");
                case ANS_REQAUTH:
                    auth();
                case ANS_REQDESCRIPTOR:
                    setDescriptor();
                    break;
                default:
                    throw new IOException("invalid packet");
            }
        }
        queueReport.clear();
        authorized = false;
        throw new IOException("operation time out");
    }

    public void sendReport(byte[] report) throws IOException {
        addReport(report);
        sendReport();
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
