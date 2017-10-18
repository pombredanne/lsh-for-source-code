package org.pz.net;

import org.pz.net.shared.Command;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.pz.net.util.Log.d;
import static org.pz.net.util.BufferHelper.*;

/**
 *
 * @author Jannek
 */
public class UserLink {

    private int id;

    private int key;

    private String name;

    private SocketAddress masterServerAddress;

    private SocketChannel channel;

    private ByteBuffer buffer;

    private Client client;

    private UserLink(int id, int key, String name, SocketAddress masterServerAddress) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.masterServerAddress = masterServerAddress;
    }

    private UserLink(String name, SocketAddress masterServerAddress) throws IOException {
        id = -1;
        key = 0;
        this.name = name;
        this.masterServerAddress = masterServerAddress;
        channel = SocketChannel.open(masterServerAddress);
        buffer = ByteBuffer.allocate(1000);
    }

    private static byte[] baseHash(String name, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(name.toLowerCase().getBytes());
            digest.update(password.getBytes());
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            d("MD5 algorithm not found!");
            throw new RuntimeException("MD5 algorithm not found! Unable to authenticate");
        }
    }

    private static byte[] loginHash(String name, String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(salt);
            digest.update(baseHash(name, password));
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            d("MD5 algorithm not found!");
            throw new RuntimeException("MD5 algorithm not found! Unable to authenticate");
        }
    }

    public static UserLink GetUser(String name, String password, SocketAddress masterServerAddress) throws IOException {
        UserLink link = new UserLink(name, masterServerAddress);
        link.buffer.clear();
        put(Command.GET_SALT, link.buffer);
        link.sendAndReceive();
        byte[] salt = getBytes(link.buffer);
        link.buffer.clear();
        put(Command.GET_USER, link.buffer);
        put(name, link.buffer);
        put(loginHash(name, password, salt), link.buffer);
        link.sendAndReceive();
        switch(getCommand(link.buffer)) {
            case LOGIN_SUCCEEDED:
                link.id = link.buffer.getInt();
                link.key = link.buffer.getInt();
                break;
            case LOGIN_FAILED:
                return null;
        }
        return link;
    }

    public static Command CreateUser(String name, String password, SocketAddress masterServerAddress) throws IOException {
        UserLink link = new UserLink(name, masterServerAddress);
        link.buffer.clear();
        put(Command.CREATE_CLIENT, link.buffer);
        put(name, link.buffer);
        put(baseHash(name, password), link.buffer);
        link.sendAndReceive();
        return getCommand(link.buffer);
    }

    private void sendAndReceive() throws IOException {
        if (!channel.isOpen()) channel = SocketChannel.open(masterServerAddress);
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
    }

    public Connection connectToServer(int serverId) {
    }

    public void getMasterUpdates(ByteBuffer data) {
    }
}
