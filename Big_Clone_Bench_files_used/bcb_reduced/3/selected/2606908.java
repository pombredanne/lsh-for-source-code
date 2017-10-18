package net.sourceforge.herald;

import java.net.ProtocolException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.event.EventListenerList;
import net.sourceforge.util.*;

public class Authenticator {

    private EventListenerList listenerList = new EventListenerList();

    private MessengerClient client;

    private String userHandle;

    private char[] password;

    private String securityPolicy;

    private MessageLogSingleton log = MessageLogSingleton.instance();

    public Authenticator(MessengerClient client) {
        this.client = client;
    }

    public void authenticate(String userHandle, char[] password) {
        this.userHandle = userHandle;
        this.password = password;
        PolicyPacket clientPacket = new PolicyPacket();
        client.addPacketListener(clientPacket.getTransactionID(), new GetAuthProtocols());
        client.sendMessage(clientPacket);
    }

    public void addAuthenticationListener(AuthenticationListener l) {
        listenerList.add(AuthenticationListener.class, l);
    }

    public void removeAuthenticationListener(AuthenticationListener l) {
        listenerList.remove(AuthenticationListener.class, l);
    }

    protected void fireAuthenticationSuccessful(String userHandle, String friendlyName) {
        Object[] listeners = listenerList.getListenerList();
        AuthenticationEvent ev = new AuthenticationEvent(this, userHandle, friendlyName);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AuthenticationListener.class) {
                AuthenticationListener l = (AuthenticationListener) listeners[i + 1];
                l.authenticationSuccessful(ev);
            }
        }
    }

    protected void fireAuthenticationFailed() {
        Object[] listeners = listenerList.getListenerList();
        AuthenticationEvent ev = new AuthenticationEvent(this, null, null);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AuthenticationListener.class) {
                AuthenticationListener l = (AuthenticationListener) listeners[i + 1];
                l.authenticationFailed(ev);
            }
        }
    }

    protected void selectPolicy(String[] policies) {
        for (int i = 0; i < policies.length; i++) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance(policies[i]);
            } catch (NoSuchAlgorithmException ex) {
                md = null;
            }
            if (md != null) {
                securityPolicy = policies[i];
                break;
            }
        }
    }

    protected String calcAuthResponse(String challenge) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(securityPolicy);
        md.update(challenge.getBytes());
        for (int i = 0, n = password.length; i < n; i++) {
            md.update((byte) password[i]);
        }
        byte[] digest = md.digest();
        StringBuffer digestText = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            int v = (digest[i] < 0) ? digest[i] + 256 : digest[i];
            String hex = Integer.toHexString(v);
            if (hex.length() == 1) {
                digestText.append("0");
            }
            digestText.append(hex);
        }
        return digestText.toString();
    }

    class GetAuthProtocols implements PacketListener {

        public void packetReceived(MessengerPacket p) {
            client.removePacketListener(p.getTransactionID());
            if (p instanceof PolicyPacket) {
                PolicyPacket pp = (PolicyPacket) p;
                selectPolicy(pp.getPolicies());
                AuthPacket clientPacket = null;
                try {
                    clientPacket = new AuthPacket(securityPolicy, AuthPacket.INITIATE_INFO, userHandle);
                    client.addPacketListener(clientPacket.getTransactionID(), new GetAuthChallenge());
                    client.sendMessage(clientPacket);
                } catch (ProtocolException ex) {
                    fireAuthenticationFailed();
                }
            }
        }
    }

    class GetAuthChallenge implements PacketListener {

        public void packetReceived(MessengerPacket p) {
            client.removePacketListener(p.getTransactionID());
            if (p instanceof AuthPacket) {
                try {
                    AuthPacket ap = (AuthPacket) p;
                    String challenge = ap.getAuthInfo();
                    String response = calcAuthResponse(challenge);
                    AuthPacket clientPacket = null;
                    clientPacket = new AuthPacket(securityPolicy, AuthPacket.RESPONSE_INFO, response);
                    client.addPacketListener(clientPacket.getTransactionID(), new GetAuthResponse());
                    client.sendMessage(clientPacket);
                } catch (Exception ex) {
                    log.append("Unable to respond to authentication request: " + ex.getMessage());
                }
            }
        }
    }

    class GetAuthResponse implements PacketListener {

        public void packetReceived(MessengerPacket p) {
            client.removePacketListener(p.getTransactionID());
            boolean success = false;
            String friendlyName = null;
            try {
                if (p instanceof AuthPacket) {
                    AuthPacket ap = (AuthPacket) p;
                    try {
                        friendlyName = URLDecoder.decode(ap.authInfo);
                    } catch (Exception ex) {
                        log.append("Unable to decode friendly name: " + ex.getMessage());
                        friendlyName = ap.authInfo;
                    }
                    success = true;
                }
            } finally {
                if (success) {
                    fireAuthenticationSuccessful(userHandle, friendlyName);
                } else {
                    fireAuthenticationFailed();
                }
            }
        }
    }
}
