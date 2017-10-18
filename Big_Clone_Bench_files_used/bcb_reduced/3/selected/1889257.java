package com.pz.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import static com.pz.util.Log.d;
import static com.pz.net.Command.*;
import static com.pz.util.ByteBufferHelper.*;

/**
 * The MasterServerLink is NOT thread safe!
 * @author jannek
 */
public class MasterServerLink {

    private static final String MASTER_HOST = "localhost";

    private static final int MASTER_PORT = 13370;

    private final SocketAddress masterAddress;

    private final ByteBuffer buffer;

    private final ByteBuffer udpBuffer;

    private final MessageDigest digest;

    private boolean connected;

    private final String name;

    private final byte[] password;

    private byte[] md5;

    private final Client client;

    public MasterServerLink(String name, String password, boolean newClient) throws IOException, PZException {
        masterAddress = new InetSocketAddress(MASTER_HOST, MASTER_PORT);
        buffer = ByteBuffer.allocate(1000);
        udpBuffer = ByteBuffer.allocate(13);
        this.name = name;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            d(ex);
            throw new PZException("Unable to use MD5", ex);
        }
        this.password = digest.digest(("$@ö\\<!>" + password + "{ñ=?]£#").getBytes());
        if (newClient) {
            put(LINK_NEW_CLIENT, buffer);
            put(name, buffer);
            buffer.put(this.password);
            writeAndRead();
            switch(getCommand(buffer)) {
                case MASTER_CLIENT_CREATED:
                    client = new Client(buffer.getInt(), buffer.getInt(), name, this);
                    connected = true;
                    break;
                case MASTER_NAME_TAKEN:
                    throw new PZException("Name taken");
                case MASTER_FAILED_TO_PARSE:
                default:
                    throw new PZException("Failed to parse");
            }
        } else {
            generateMD5();
            put(LINK_LOGIN_WITH_NAME, buffer);
            put(name, buffer);
            buffer.put(md5);
            writeAndRead();
            switch(getCommand(buffer)) {
                case ACCEPTED:
                    client = new Client(buffer.getInt(), buffer.getInt(), name, this);
                    connected = true;
                    break;
                case DENIED:
                    throw new PZException("Incorrect name or password");
                case MASTER_FAILED_TO_PARSE:
                default:
                    throw new PZException("Failed to parse");
            }
        }
        int count = 0;
        do {
            if (count++ > 30) throw new PZException("Failed to confirm udp");
            clientUdp();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
            client.update();
        } while (!client.udpConfirmed());
        d("Client connected in " + count + (count == 1 ? "try." : " tries."));
    }

    private void generateMD5() throws PZException {
        buffer.clear();
        put(LINK_GET_MD5_SALT, buffer);
        writeAndRead();
        if (getCommand(buffer) != MASTER_MD5_SALT) throw new PZException("Unable to fetch MD5 salt");
        byte[] salt = getBytes(buffer);
        digest.reset();
        digest.update(salt);
        digest.update(password);
        this.md5 = digest.digest();
    }

    private void writeAndRead() throws PZException {
        buffer.flip();
        try {
            SocketChannel channel = SocketChannel.open(masterAddress);
            channel.write(buffer);
            buffer.clear();
            channel.read(buffer);
            channel.close();
        } catch (IOException ex) {
            d(ex);
            connected = false;
            throw new PZException(ex);
        }
        buffer.flip();
    }

    public void clientUdp() {
        buffer.clear();
        put(LINK_UDP_UPDATE_CLIENT, buffer);
        buffer.putInt(client.id);
        buffer.putInt(client.key);
        buffer.flip();
        try {
            client.udp.send(buffer, masterAddress);
        } catch (IOException ex) {
            d(ex);
        }
    }

    public void serverUdp(Server server) {
        buffer.clear();
        put(LINK_UDP_UPDATE_SERVER, buffer);
        buffer.putInt(client.id);
        buffer.putInt(client.key);
        buffer.putInt(server.info.id);
        buffer.flip();
        try {
            server.udp.send(buffer, masterAddress);
        } catch (IOException ex) {
            d(ex);
        }
    }

    public void reconnect() throws PZException {
        generateMD5();
        buffer.clear();
        put(LINK_LOGIN_WITH_ID, buffer);
        buffer.putInt(client.id);
        buffer.put(md5);
        writeAndRead();
        switch(getCommand(buffer)) {
            case ACCEPTED:
                client.key = buffer.getInt();
                connected = true;
                break;
            case DENIED:
                throw new PZException("Incorrect name or password");
            case MASTER_FAILED_TO_PARSE:
            default:
                throw new PZException("Failed to parse");
        }
    }

    public void disconnect() throws PZException {
        buffer.clear();
        put(DISCONNECT, buffer);
        buffer.putInt(client.id);
        buffer.putInt(client.key);
        writeAndRead();
        if (getCommand(buffer) != GOODBYE) throw new PZException("Failed to log out");
    }

    public List<ServerInfo> getServerList() throws PZException {
        List<ServerInfo> list = new LinkedList<ServerInfo>();
        buffer.clear();
        put(LINK_GET_SERVER_LIST, buffer);
        writeAndRead();
        switch(getCommand(buffer)) {
            case MASTER_SERVER_LIST:
                while (getCommand(buffer) == MORE) {
                    ServerInfo info = new ServerInfo(buffer.getInt(), getString(buffer), getString(buffer), getBytes(buffer), buffer.getShort(), buffer.getShort());
                    list.add(info);
                }
                return list;
            case MASTER_FAILED_TO_PARSE:
            default:
                throw new PZException("Failed to parse");
        }
    }

    public ServerInfo getServer(int serverId) throws PZException {
        buffer.clear();
        put(LINK_GET_SERVER, buffer);
        buffer.putInt(serverId);
        writeAndRead();
        switch(getCommand(buffer)) {
            case MASTER_SERVER_INFO:
                return new ServerInfo(serverId, getString(buffer), getString(buffer), getBytes(buffer), buffer.getShort(), buffer.getShort());
            case MASTER_NO_SUCH_SERVER:
                throw new PZException("No such server");
            case MASTER_SERVER_OFFLINE:
                throw new PZException("The server is offline");
            case MASTER_FAILED_TO_PARSE:
            default:
                throw new PZException("Failed to parse");
        }
    }

    public Client connectToServer(ServerInfo info, NetworkListener listener) throws PZException {
        buffer.clear();
        put(LINK_CONNECT_TO_SERVER, buffer);
        buffer.putInt(info.id);
        buffer.putInt(client.id);
        buffer.putInt(client.key);
        writeAndRead();
        Other server = null;
        switch(getCommand(buffer)) {
            case ACCEPTED:
                server = new Other(info.id, client.id, buffer.getInt(), info.name, getAddress(buffer));
                break;
            case MASTER_NO_SUCH_SERVER:
                throw new PZException("No such server");
            case MASTER_SERVER_OFFLINE:
                throw new PZException("The server is offline");
            case MASTER_SERVER_FULL:
                throw new PZException("The server is full");
            case DENIED:
                throw new PZException("Access denied by the server");
            case MASTER_UPDATE_UDP:
                throw new PZException("No or outdated udp socket");
            case MASTER_FAILED_TO_PARSE:
            default:
                throw new PZException("Failed to parse");
        }
        if (server == null) throw new PZException("Failed to parse");
        client.connect(server, listener);
        return client;
    }

    public Server createServer() throws PZException {
    }

    public List<Other> getNewPlayers() throws PZException {
    }
}
