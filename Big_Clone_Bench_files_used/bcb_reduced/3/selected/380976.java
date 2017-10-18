package de.uni_bremen.informatik.sopra.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Mailserver {

    private String server;

    private int port;

    private String user;

    private String passwort;

    private Socket socket;

    private BufferedReader in;

    private PrintStream out;

    public Mailserver(final String server, final int port, final String user, final String passwort) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.passwort = passwort;
    }

    private void socket_send(final String s) {
        System.out.println("> " + s);
        out.print(s);
    }

    /**
	 * hier ist eine methode die lieste eine ganz zeile von socket und giebt ihm
	 * zurug
	 */
    private String socket_read() {
        try {
            final String s = in.readLine();
            System.out.println("< " + s);
            return s;
        } catch (final IOException e) {
            System.err.println(e);
        }
        return null;
    }

    /**
	 * methode zum senden von smtp befehlen. gibt die smtp antwortenummer
	 * zuruke.
	 *  
	 */
    private int smtpSend(final String cmd) {
        int resulta = -1;
        if (socket.isConnected()) {
            socket_send(cmd + "\n");
            final String reponse = socket_read();
            if (reponse != null) {
                try {
                    resulta = Integer.parseInt(reponse.substring(0, 3));
                } catch (final NumberFormatException e) {
                }
            }
        }
        return resulta;
    }

    private ArrayList smtpSendMulti(final String cmd, final int ok_code) {
        final ArrayList resulta = new ArrayList();
        if (socket.isConnected()) {
            socket_send(cmd + "\n");
            while (true) {
                final String line = socket_read();
                int k = -1;
                try {
                    k = Integer.parseInt(line.substring(0, 3));
                } catch (final NumberFormatException e) {
                }
                if (k > 0) {
                    if (line.charAt(3) == ' ') {
                        if (k != ok_code) {
                            return null;
                        }
                        resulta.add(line.substring(4));
                        break;
                    } else if (line.charAt(3) == '-') {
                        resulta.add(line.substring(4));
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
        return resulta;
    }

    /**
	 * verbendung zum mailserver aufbauen und leser und schreiber erzeugen
	 */
    public boolean connect() {
        try {
            try {
                socket = new Socket(server, port);
            } catch (final UnknownHostException e) {
                System.err.println(e);
                return false;
            } catch (final NoRouteToHostException e) {
                System.err.println(e);
                return false;
            }
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
            final String reponse = socket_read();
            if ((reponse == null) || (!(reponse.startsWith("220 ")))) {
                return false;
            }
            final ArrayList ehlo = smtpSendMulti("EHLO " + socket.getLocalAddress().toString(), 250);
            if (ehlo == null) {
                if (smtpSend("HELO " + socket.getLocalAddress().toString()) != 250) {
                    disconnect();
                    return false;
                }
            } else {
                String s;
                for (int i = 0; i < ehlo.size(); i++) {
                    s = (String) ehlo.get(i);
                    if (s.toUpperCase().startsWith("AUTH")) {
                        SMTPAuth(s);
                        break;
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    /**
	 * diese methode macht die verbendung zum serve aus
	 */
    public void disconnect() {
        smtpSend("QUIT");
        try {
            socket.close();
        } catch (final IOException e) {
            System.err.println(e);
        }
    }

    /**
	 * diese methode sendet eine E-mail, m�chte als paramete eine objekt von
	 * type message, in dem objekt die e-mail geschpeichert
	 */
    public boolean sendMessage(final Message message) {
        disconnect();
        if (smtpSend("MAIL FROM: " + message.getSender()) != 250) {
            return false;
        }
        for (int i = 0; i < message.getTo().length; i++) {
            if (smtpSend("RCPT TO: " + message.getTo()[i]) != 250) {
                return false;
            }
        }
        for (int i = 0; i < message.getCc().length; i++) {
            if (smtpSend("RCPT TO: " + message.getCc()[i]) != 250) {
                return false;
            }
        }
        for (int i = 0; i < message.getBcc().length; i++) {
            if (smtpSend("RCPT TO: " + message.getBcc()[i]) != 250) {
                return false;
            }
        }
        if (smtpSend("DATA") != 354) {
            return false;
        }
        socket_send("From: " + message.getSender() + "\n");
        socket_send("To: " + message.getTo()[0] + "\n");
        for (int i = 1; i < message.getTo().length; i++) {
            socket_send("\t" + message.getTo()[i] + "\n");
        }
        if (message.getCc().length > 0) {
            socket_send("CC: " + message.getCc()[0] + "\n");
            for (int i = 1; i < message.getCc().length; i++) {
                socket_send("\t" + message.getCc()[i] + "\n");
            }
        }
        socket_send("Subject: " + message.getSubject() + "\n");
        socket_send("Date: " + message.getDate() + "\n");
        socket_send("\n");
        socket_send(message.getText() + "\n");
        if (smtpSend(".") != 250) {
            return false;
        }
        return true;
    }

    public boolean send(final Message message) {
        if (!connect()) {
            return false;
        }
        if (!sendMessage(message)) {
            return false;
        }
        disconnect();
        return true;
    }

    private boolean SMTPAuth(final String auth) {
        boolean md5 = false;
        boolean plain = false;
        for (int i = 0; i < auth.length(); i++) {
            if (!plain && (i + 5 <= auth.length())) {
                plain = (auth.substring(i, i + 5).toUpperCase().equals("PLAIN"));
            }
            if (!md5 && (i + 8 <= auth.length())) {
                md5 = (auth.substring(i, i + 8).toUpperCase().equals("CRAM-MD5"));
            }
        }
        boolean result = false;
        if (md5) {
            result = SMTPAuth_cramMD5();
        }
        if ((!result) && plain) {
            result = SMTPAuth_plain();
        }
        return result;
    }

    private boolean SMTPAuth_plain() {
        final Base64 b64 = new Base64();
        final String authstr = "\0" + user + "\0" + passwort;
        if (smtpSend("AUTH PLAIN " + b64.encode(authstr)) != 235) {
            return false;
        }
        return true;
    }

    private boolean SMTPAuth_cramMD5() {
        final Base64 b64 = new Base64();
        socket_send("AUTH CRAM-MD5\n");
        final String res = socket_read();
        if (!res.startsWith("334 ")) {
            return false;
        }
        final String challenge = b64.decode(res.substring(4));
        final String digest = cramMD5(challenge, passwort);
        System.out.println(":" + md5string("test") + ":");
        final String reply = b64.encode(user + " " + digest);
        if (smtpSend(reply) != 235) {
            return false;
        }
        return true;
    }

    public String md5string(final String password) {
        final String digits = "0123456789abcdef";
        final StringBuffer rValue = new StringBuffer();
        byte[] b;
        try {
            final java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            b = md.digest(password.getBytes());
        } catch (final java.security.NoSuchAlgorithmException e) {
            b = "".getBytes();
        }
        for (byte i = 0; i < b.length; i++) {
            rValue.append(digits.charAt((b[i] >> 4) & 0x0f));
            rValue.append(digits.charAt(b[i] & 0x0f));
        }
        return rValue.toString();
    }

    private String cramMD5(final String challenge, String key) {
        String md5i;
        char cp;
        if (key.length() > 64) {
            key = md5string(key);
        }
        final char[] ipad = new char[64];
        final char[] opad = new char[64];
        for (int i = 0; i < 64; i++) {
            if (i < key.length()) {
                cp = key.charAt(i);
            } else {
                cp = 0;
            }
            ipad[i] = (char) (cp ^ 0x36);
            opad[i] = (char) (cp ^ 0x5c);
        }
        String ipad_str = new String(ipad);
        ipad_str = ipad_str + challenge;
        final String opad_str = new String(opad);
        md5i = md5string(ipad_str);
        return md5string(opad_str + md5i);
    }
}
