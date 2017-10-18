package com.mattgarner.jaddas.node.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import com.mattgarner.jaddas.node.CurrentStateManager;
import com.mattgarner.jaddas.node.ErrorLogManager;
import com.mattgarner.jaddas.node.NodeConfigurationManager;
import com.mattgarner.jaddas.node.RemoteClient;
import com.mattgarner.jaddas.node.command.CommandParser;
import com.mattgarner.jaddas.node.command.CommandParserClient;

public class Worker implements Runnable {

    private static int threadCounter;

    private static int activeThreadCounter;

    private int threadID;

    private int messageID;

    private NodeConfigurationManager nodeConfig;

    private ErrorLogManager logManager;

    private CurrentStateManager stateManager;

    private Socket socket;

    private OutputStream out;

    private DataOutputStream dout;

    private InputStream in;

    private DataInputStream din;

    private RemoteClient remoteClient;

    private CommandParser commandParser;

    private long lastReceivedDataTime;

    public Worker(Socket clientSocket) {
        socket = clientSocket;
        nodeConfig = NodeConfigurationManager.getInstance();
        logManager = ErrorLogManager.getInstance();
        stateManager = CurrentStateManager.getInstance();
        synchronized (this) {
            threadID = threadCounter;
            threadCounter++;
            activeThreadCounter++;
        }
    }

    public final void run() {
        logManager.writeToLog(4, "NET", "[" + threadID + "] Accepted new connection from: " + socket.getInetAddress().toString().replaceAll("/", ""));
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            din = new DataInputStream(in);
            dout = new DataOutputStream(out);
            remoteClient = new RemoteClient(threadID, socket.getInetAddress(), this);
            stateManager.addRemoteClient(threadID, remoteClient);
            sendMessage(Protocol.MSG_COMMENT, "Welcome to JaddasNode v." + nodeConfig.getNodeVersion());
            if (!authenticateConnection()) {
                logManager.writeToLog(2, "NET", "[" + threadID + "] Authentication Failed.");
                sendMessage(Protocol.MSG_NAK, "Authentication failed.");
                close();
                return;
            }
            remoteClient.setClientStatus(RemoteClient.CONNECTED);
            commandParser = new CommandParserClient(this, remoteClient);
            while (true) {
                try {
                    Message message = readMessage();
                    lastReceivedDataTime = System.nanoTime();
                    remoteClient.setLastActivityTime(System.currentTimeMillis());
                    logManager.writeToLog(6, "NET", "[" + threadID + ":" + message.getMessageID() + "] << " + message.getMessageString());
                    commandParser.processCommand(message);
                } catch (IOException e) {
                    break;
                }
            }
        } catch (IOException e) {
            logManager.writeToLog(1, "NET", "[" + threadID + "] Socket error: " + e.getMessage());
        }
        close();
    }

    public static final int getActiveThreadCount() {
        return activeThreadCounter;
    }

    private final void close() {
        try {
            stateManager.removeRemoteClient(threadID);
            in.close();
            dout.close();
            out.close();
            socket.close();
            synchronized (this) {
                activeThreadCounter--;
            }
            logManager.writeToLog(4, "NET", "[" + threadID + "] Socket closed. (" + remoteClient.getIPAddressString() + ")");
        } catch (IOException e) {
            logManager.writeToLog(1, "NET", "[" + threadID + "] Socket close error: " + e.getMessage());
        }
    }

    private final void sendMessage(byte messageFlag, byte[] messageData) {
        sendMessage(new Message(messageFlag, messageData));
    }

    public final void sendMessage(byte messageFlag, String messageString) {
        try {
            sendMessage(new Message(messageFlag, messageString.getBytes(remoteClient.getStringEncoding())));
        } catch (UnsupportedEncodingException e) {
            logManager.writeToLog(1, "NET", "[" + threadID + "] Message Encoding Error: " + e.getMessage());
        }
    }

    public final Message readMessage() throws IOException {
        byte messageFlag = din.readByte();
        int messageID = din.readInt();
        int messageLength = din.readInt();
        byte[] inBuffer = null;
        if (messageFlag == Protocol.ENC_COMPRESSED_GZIP) {
            messageFlag = din.readByte();
            int decompressedLength = din.readInt();
            GZIPInputStream gzin = new GZIPInputStream(din);
            int readLength = decompressedLength;
            if (messageLength > decompressedLength) {
                readLength = messageLength;
            }
            byte[] gzBuffer = new byte[readLength];
            gzin.read(gzBuffer, 0, readLength);
            inBuffer = new byte[decompressedLength];
            System.arraycopy(gzBuffer, 0, inBuffer, 0, decompressedLength);
            logManager.writeToLog(6, "NET", "[" + threadID + "] Compression rate: " + decompressedLength + " -> " + messageLength);
        } else if (messageFlag == Protocol.ENC_COMPRESSED_LZIP) {
            messageFlag = din.readByte();
            int decompressedLength = din.readInt();
            InflaterInputStream lzin = new InflaterInputStream(din);
            int readLength = decompressedLength;
            if (messageLength > decompressedLength) {
                readLength = messageLength;
            }
            byte[] lzBuffer = new byte[readLength];
            lzin.read(lzBuffer, 0, readLength);
            inBuffer = new byte[decompressedLength];
            System.arraycopy(lzBuffer, 0, inBuffer, 0, decompressedLength);
            logManager.writeToLog(6, "NET", "[" + threadID + "] Compression rate: " + decompressedLength + " -> " + messageLength);
        } else {
            inBuffer = new byte[messageLength];
            din.read(inBuffer, 0, messageLength);
        }
        return new Message(messageFlag, messageID, messageLength, inBuffer);
    }

    public final void sendMessage(Message message) {
        try {
            if (remoteClient.getCompressionMode() == RemoteClient.COMPRESSION_GZIP) {
                dout.writeByte(Protocol.ENC_COMPRESSED_GZIP);
                dout.writeInt(messageID++);
                ByteArrayOutputStream gzBuffer = new ByteArrayOutputStream();
                GZIPOutputStream gzout = new GZIPOutputStream(gzBuffer);
                gzout.write(message.getMessageBytes());
                gzout.close();
                gzBuffer.close();
                dout.writeInt(gzBuffer.toByteArray().length);
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(message.getMessageLength());
                dout.write(gzBuffer.toByteArray());
                sleep(1L);
            } else if (remoteClient.getCompressionMode() == RemoteClient.COMPRESSION_LZIP) {
                dout.writeByte(Protocol.ENC_COMPRESSED_LZIP);
                dout.writeInt(messageID++);
                ByteArrayOutputStream lzBuffer = new ByteArrayOutputStream();
                DeflaterOutputStream lzout = new DeflaterOutputStream(lzBuffer);
                lzout.write(message.getMessageBytes());
                lzout.close();
                lzBuffer.close();
                dout.writeInt(lzBuffer.size());
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(message.getMessageLength());
                dout.write(lzBuffer.toByteArray());
                sleep(1L);
            } else {
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(messageID++);
                dout.writeInt(message.getMessageLength());
                dout.write(message.getMessageBytes());
            }
            dout.flush();
        } catch (IOException e) {
            logManager.writeToLog(1, "NET", "[" + threadID + "] Socket write error: " + e.getMessage());
        }
    }

    private final boolean authenticateConnection() {
        byte[] authChallengePhrase = generateChallengePhrase(this.toString());
        sendMessage(Protocol.MSG_AUTHCHALLENGE, authChallengePhrase);
        try {
            byte[] authSharedKey = new byte[0];
            try {
                authSharedKey = nodeConfig.getAuthSharedKeyClient().getBytes(remoteClient.getStringEncoding());
            } catch (UnsupportedEncodingException e) {
                logManager.writeToLog(1, "NET", "[" + threadID + "] Auth error: " + e.getMessage());
            }
            byte[] authCorrectPreDigest = new byte[authChallengePhrase.length + authSharedKey.length];
            System.arraycopy(authChallengePhrase, 0, authCorrectPreDigest, 0, authChallengePhrase.length);
            System.arraycopy(authSharedKey, 0, authCorrectPreDigest, authChallengePhrase.length, authSharedKey.length);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logManager.writeToLog(1, "NET", "[" + threadID + "] Auth error: " + e.getMessage());
            }
            byte[] authCorrectResponse = md.digest(authCorrectPreDigest);
            Message authResponse = readMessage();
            if (authResponse.getMessageFlag() != Protocol.MSG_AUTHCHALLENGE_RESP) {
                return false;
            }
            byte[] authResponseBytes = authResponse.getMessageBytes();
            if (authCorrectResponse.length != authResponseBytes.length) {
                return false;
            }
            for (int c = 0; c < authCorrectResponse.length; c++) {
                if (authCorrectResponse[c] != authResponseBytes[c]) {
                    return false;
                }
            }
            sendMessage(Protocol.MSG_OK, "Authentication accepted.");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private final byte[] generateChallengePhrase(String sharedKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(sharedKey.getBytes(remoteClient.getStringEncoding()));
        } catch (Exception e) {
            logManager.writeToLog(1, "NET", "[" + threadID + "] Authentication error: " + e.getMessage());
        }
        return null;
    }

    public final int getThreadID() {
        return threadID;
    }

    public final void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            return;
        }
    }
}
