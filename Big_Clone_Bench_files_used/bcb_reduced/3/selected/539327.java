package com.ngranek.unsolved.client.consumers;

import java.security.MessageDigest;
import com.ngranek.unsolved.server.ClientThread;
import com.ngranek.unsolved.server.MessagesConsumer;
import com.ngranek.unsolved.server.messages.AbstractMessage;
import com.ngranek.unsolved.server.messages.ConnectionResponse;
import com.ngranek.unsolved.server.messages.LoginRequest;
import com.ngranek.unsolved.server.messages.LoginResponse;
import com.ngranek.unsolved.client.Main;
import com.ngranek.unsolved.client.states.PlayState;

public class ClientConnectionConsumer {

    public static void processConnectionResponse(MessagesConsumer server, ClientThread thread, AbstractMessage message) throws Exception {
        ConnectionResponse response = (ConnectionResponse) message;
        String salt = response.getSalt();
        LoginRequest request = new LoginRequest();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        request.setLogin(Main.getInstance().getNetworkManager().getLogin());
        String password = byteArrayToHexString(digest.digest((salt + Main.getInstance().getNetworkManager().getPassword()).getBytes()));
        server.getLogger().info("Sending password: '" + password + "'");
        request.setPassword(password);
        thread.sendMessage(request);
    }

    public static void processLoginResponse(MessagesConsumer server, ClientThread thread, AbstractMessage message) throws Exception {
        LoginResponse response = (LoginResponse) message;
        server.getLogger().info("Session id: '" + response.getSessionId() + "'");
        if (response.getSessionId() == -1) {
            Main.getInstance().getCurrentState().setInfoText("Login incorrect. Please try again");
        } else {
            Main.getInstance().getCurrentState().setInfoText("Login successful!!! Please wait ...");
            Main.getInstance().setState(PlayState.getInstance());
            PlayState.getInstance().setScene(response.getScene());
        }
    }

    public static String byteArrayToHexString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            out.append(pseudo[(int) ch]);
            ch = (byte) (in[i] & 0x0F);
            out.append(pseudo[(int) ch]);
            i++;
        }
        String rslt = new String(out);
        return rslt;
    }
}
