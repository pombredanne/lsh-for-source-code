package com.mattgarner.jaddas.cli.net;

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
import com.mattgarner.jaddas.cli.ClientConfigurationManager;

public class Client {

    private static Client instance;

    private Socket socket;

    private ClientConfigurationManager clientConfig;

    private String sAddress;

    private int sPort;

    private int messageCounter;

    private int reconnectCounter = 0;

    private InputStream in;

    private OutputStream out;

    private DataInputStream din;

    private DataOutputStream dout;

    public Client(String serverAddress, int serverPort) {
        sAddress = serverAddress;
        sPort = serverPort;
        clientConfig = ClientConfigurationManager.getInstance();
        instance = this;
    }

    public Client() {
    }

    public static Client getInstance() {
        return instance;
    }

    public final void connect() {
        try {
            System.out.println(">> Connecting to JaddasNode...");
            socket = new Socket(sAddress, sPort);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            din = new DataInputStream(in);
            dout = new DataOutputStream(out);
            Message welcomeMsg = readMessage();
            System.out.println("<< " + welcomeMsg.getMessageString());
            System.out.println(">> Authenticating...");
            if (authenticate()) {
                System.out.println("<< Authentication successful.");
            } else {
                System.out.println("<< Pre-shared key authentication failed.");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.out.println("Unable to connect to host: " + e.getMessage());
            System.exit(-1);
        }
    }

    public final Message readMessage() {
        try {
            byte messageFlag = din.readByte();
            int messageID = din.readInt();
            int messageLength = din.readInt();
            byte[] inBuffer = null;
            if (messageFlag == Protocol.ENC_COMPRESSED_GZIP) {
                messageFlag = din.readByte();
                int decompressedLength = din.readInt();
                int readLength = decompressedLength;
                if (messageLength > decompressedLength) {
                    readLength = messageLength;
                }
                byte[] gzBuffer = new byte[readLength];
                GZIPInputStream gzin = new GZIPInputStream(din);
                gzin.read(gzBuffer, 0, readLength);
                inBuffer = new byte[decompressedLength];
                System.arraycopy(gzBuffer, 0, inBuffer, 0, decompressedLength);
                if (clientConfig.getCompressionMode() != ClientConfigurationManager.COMPRESSION_GZIP) {
                    clientConfig.setCompressionMode(ClientConfigurationManager.COMPRESSION_GZIP);
                }
            } else if (messageFlag == Protocol.ENC_COMPRESSED_LZIP) {
                messageFlag = din.readByte();
                int decompressedLength = din.readInt();
                int readLength = decompressedLength;
                if (messageLength > decompressedLength) {
                    readLength = messageLength;
                }
                byte[] gzBuffer = new byte[readLength];
                InflaterInputStream lzin = new InflaterInputStream(din);
                lzin.read(gzBuffer, 0, readLength);
                inBuffer = new byte[decompressedLength];
                System.arraycopy(gzBuffer, 0, inBuffer, 0, decompressedLength);
                if (clientConfig.getCompressionMode() != ClientConfigurationManager.COMPRESSION_LZIP) {
                    clientConfig.setCompressionMode(ClientConfigurationManager.COMPRESSION_LZIP);
                }
            } else {
                inBuffer = new byte[messageLength];
                din.read(inBuffer, 0, messageLength);
                if (clientConfig.getCompressionMode() != ClientConfigurationManager.COMPRESSION_NONE) {
                    clientConfig.setCompressionMode(ClientConfigurationManager.COMPRESSION_NONE);
                }
            }
            return new Message(messageFlag, messageID, messageLength, inBuffer);
        } catch (Exception e) {
            System.out.println("Unable to read: " + e.getMessage());
        }
        return null;
    }

    public final void sendMessage(Message message) {
        try {
            if (clientConfig.getCompressionMode() == ClientConfigurationManager.COMPRESSION_GZIP) {
                dout.writeByte(Protocol.ENC_COMPRESSED_GZIP);
                dout.writeInt(messageCounter++);
                ByteArrayOutputStream gzBuffer = new ByteArrayOutputStream();
                GZIPOutputStream gzout = new GZIPOutputStream(gzBuffer);
                gzout.write(message.getMessageBytes());
                gzout.close();
                gzBuffer.close();
                dout.writeInt(gzBuffer.toByteArray().length);
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(message.getMessageLength());
                dout.write(gzBuffer.toByteArray());
            } else if (clientConfig.getCompressionMode() == ClientConfigurationManager.COMPRESSION_LZIP) {
                dout.writeByte(Protocol.ENC_COMPRESSED_LZIP);
                dout.writeInt(messageCounter++);
                ByteArrayOutputStream lzBuffer = new ByteArrayOutputStream();
                DeflaterOutputStream lzout = new DeflaterOutputStream(lzBuffer);
                lzout.write(message.getMessageBytes());
                lzout.close();
                lzBuffer.close();
                dout.writeInt(lzBuffer.toByteArray().length);
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(message.getMessageLength());
                dout.write(lzBuffer.toByteArray());
            } else {
                dout.writeByte(message.getMessageFlag());
                dout.writeInt(messageCounter++);
                dout.writeInt(message.getMessageLength());
                dout.write(message.getMessageBytes());
            }
            dout.flush();
        } catch (IOException e) {
            if (e.getMessage().contains("Connection reset")) {
                if (reconnectCounter < clientConfig.getMaxReconnectAttempts()) {
                    System.out.println("** Server has gone away. Attempting to reconnect...");
                    reconnectCounter++;
                    connect();
                    sendMessage(message);
                } else {
                    System.out.println("** Server has gone away. Max reconnect attempts (" + clientConfig.getMaxReconnectAttempts() + ") exceeded.");
                    System.exit(-1);
                }
            } else {
                System.out.println("Unable to write: " + e.getMessage());
            }
        }
    }

    private final boolean authenticate() {
        Message authMsg = readMessage();
        byte[] authSharedKey = new byte[0];
        try {
            authSharedKey = clientConfig.getAuthSharedKey().getBytes(clientConfig.getStringEncoding());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Authentication error: " + e.getMessage());
        }
        byte[] authChallenge = new byte[authMsg.getMessageLength() + authSharedKey.length];
        System.arraycopy(authMsg.getMessageBytes(), 0, authChallenge, 0, authMsg.getMessageLength());
        System.arraycopy(authSharedKey, 0, authChallenge, authMsg.getMessageLength(), authSharedKey.length);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Authentication error: " + e.getMessage());
        }
        byte[] authResponse = md.digest(authChallenge);
        sendMessage(new Message(Protocol.MSG_AUTHCHALLENGE_RESP, messageCounter++, authResponse.length, authResponse));
        Message authServerResponse = readMessage();
        if (authServerResponse.getMessageFlag() == Protocol.MSG_OK) {
            return true;
        } else {
            return false;
        }
    }
}
