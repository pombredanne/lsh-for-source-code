package jkex.net;

import java.io.*;
import java.net.*;
import java.util.*;
import jkex.server.GameServer;
import jkex.server.GameConnection;

/**
 * Die Server-Klasse implementiert einen nicht blockierenden Server.
 * Auf dem in der Klasse Options festgelegten Port wartet der
 * Server auf eingehende Verbindungen. Sobald eine neue Verbindung
 * hergestellt wurde, wird eine neue Instanz von GameConnection
 * erstellt und gestartet.
 *
 * @version	$Date: 2006/07/24 20:45:38 $
 * @author	Markus Dolze
 */
public class Server extends Thread implements MessageListener {

    /**
	 * Socket, auf dem der Server auf eingehende Verbindungen wartet
	 */
    private ServerSocket listenSock;

    /**
	 * Gibt an, ob der Thread aktiv ist oder nicht.
	 */
    private boolean tactive;

    /**
	 * Anzahl der aktiven Verbindungen
	 */
    private int activeSessions;

    /**
	 * Instanz des GameServers, der das Server-Objekt erzeugt hat
	 */
    private GameServer owner;

    /**
	 * Legt eine neue Server-Instanz an.
	 *
	 * @param owner	Erzeuger-Objekt
	 */
    public Server(GameServer owner) {
        activeSessions = 0;
        this.owner = owner;
    }

    /**
	 * Enth�lt die Hauptschleife des Servers. Am in den Optionen
	 * angegebenen Port wird auf eingehende Verbindungen gewartet.
	 * Dabei l�uft auch eine Time-Out mit, damit der Thread auch
	 * beendet werden kann. Beim Aufbau einer neuen Verbindung wird
	 * der Server als MessageListener eingetragen und die Verbindung
	 * zur weiteren Bearbeitung an den GameServer �bertragen.
	 */
    public void run() {
        tactive = true;
        Connection temp;
        try {
            listenSock = new ServerSocket(Options.port);
            listenSock.setSoTimeout(1000);
            System.out.println("Server listening on port " + Options.port);
        } catch (IOException e) {
            System.err.println("Port " + Options.port + " already in use - change defaults.properties");
            System.exit(1);
        }
        while (tactive) {
            try {
                Socket acceptSock = listenSock.accept();
                temp = new GameConnection(acceptSock);
                temp.addMessageListener(this);
                temp.start();
                activeSessions++;
                owner.newSession(temp);
                System.out.println("new socket from " + acceptSock.getInetAddress().toString() + " accepted");
            } catch (InterruptedIOException e) {
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
        try {
            listenSock.close();
        } catch (IOException e) {
        }
        System.out.println("Server stopped");
    }

    /**
	 * H�lt den Thread an.
	 */
    public synchronized void tStop() {
        tactive = false;
        interrupt();
    }

    /**
	 * Implementiert das MessageListener-Interface. Es werden die
	 * Nachrichten QUIT und SHUTDOWN_SERVER bearbeitet. Wenn eine
	 * Connection �ber beendet wird, wird der Verbindungsz�hler um
	 * eins erniedrigt. Wird eine Nachricht SHUTDOWN_SERVER empfangen,
	 * wird der Verbindungsz�hler ebenfalls erniedrigt und wenn
	 * danach keine aktiven Verbindungen mehr existieren der Server
	 * angehalten.
	 *
	 * @param e	MessageEvent der empfangenen Nachricht
	 */
    public synchronized void receive(MessageEvent e) {
        switch(e.message.getType()) {
            case MessageTypes.QUIT:
                activeSessions--;
                break;
            case MessageTypes.SHUTDOWN_SERVER:
                activeSessions--;
                if (Options.password.trim().equals("")) {
                    System.err.println("No default password set. Shutdown not allowed.");
                    break;
                }
                if (e.message.get("pwhash") == null) {
                    System.err.println("Shutdown message without password received. Shutdown not allowed.");
                    break;
                }
                try {
                    java.security.MessageDigest hash = java.security.MessageDigest.getInstance("SHA-1");
                    hash.update(Options.password.getBytes("UTF-8"));
                    if (!java.security.MessageDigest.isEqual(hash.digest(), (byte[]) e.message.get("pwhash"))) {
                        System.err.println("Wrong shutdown password. Shutdown not allowed.");
                        break;
                    } else {
                        System.out.println("Valid shutdown password received.");
                    }
                } catch (java.security.NoSuchAlgorithmException ex) {
                    System.err.println("Password hash algorithm SHA-1 not supported by runtime.");
                    break;
                } catch (UnsupportedEncodingException ex) {
                    System.err.println("Password character encoding not supported.");
                    break;
                } catch (Exception ex) {
                    System.err.println("Unhandled exception occured. Shutdown aborted. Details:");
                    ex.printStackTrace(System.err);
                    break;
                }
                if (activeSessions == 0) tStop(); else System.err.println("there are other active sessions - shutdown failed");
                break;
            default:
        }
    }
}
