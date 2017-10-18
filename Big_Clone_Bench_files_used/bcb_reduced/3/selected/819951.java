package org.ugue.bittorrent;

import java.util.*;
import java.net.*;
import java.io.*;
import org.ugue.bittorrent.utils.bencoding.*;

public class Tracker implements Runnable {

    private Torrent torrent;

    private Client client;

    private boolean running;

    private Thread thread;

    private Map<String, Object> parameters;

    private List<String> parameters_order;

    public Tracker(Torrent torrent, Client client) {
        this.torrent = torrent;
        this.client = client;
        parameters = new HashMap<String, Object>();
        parameters_order = new LinkedList<String>();
        this.running = false;
        BEncSHA1 bencsha1 = new BEncSHA1();
        addParameter("info_hash", bencsha1.digest(torrent.getInfo()).getSHA1());
        addParameter("peer_id", client.getPeerID());
        addParameter("port", 8000);
        addParameter("uploaded", 0);
        addParameter("downloaded", 0);
        addParameter("left", 0);
        addParameter("compact", "disabled");
        addParameter("no_peer_id", false);
        addParameter("event", "");
    }

    public synchronized void start(boolean hasStarted) {
        this.running = true;
        if (hasStarted == false) {
        }
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop(boolean hasCompleted) {
        this.running = false;
        thread.interrupt();
    }

    public void run() {
        while (running) {
        }
    }

    private void updateTracker() {
    }

    private void addParameter(String key, Object o) {
        parameters.put(key, o);
        parameters_order.add(key);
    }

    public String generateQuery() {
        StringBuffer query = new StringBuffer();
        for (String key : parameters_order) {
            if (parameters.containsKey(key)) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(key);
                query.append('=');
                query.append(escapeObject(parameters.get(key)));
            }
        }
        return query.toString();
    }

    private String escapeObject(Object o) {
        if (o instanceof String) {
            return escapeString((String) o);
        } else if (o instanceof byte[]) {
            return byteArrayToURLString((byte[]) o);
        } else if (o instanceof Number) {
            return ((Number) o).toString();
        } else if (o instanceof Boolean) {
            return (((Boolean) o).booleanValue()) ? "true" : "false";
        } else {
            return o.toString();
        }
    }

    private String escapeString(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
	 * 
	 * Copied from Java Bittorrent API by Baptiste Dubuis (Original Version)
	 * @param in
	 * @return
	 */
    private String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        if (in == null || in.length <= 0) return "";
        final char pseudo[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuffer out = new StringBuffer(in.length * 2);
        for (int i = 0; i < in.length; i++) {
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z') || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$' || in[i] == '-' || in[i] == '_' || in[i] == '.' || in[i] == '+' || in[i] == '!') {
                out.append((char) in[i]);
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0);
                ch = (byte) (ch >>> 4);
                ch = (byte) (ch & 0x0F);
                out.append(pseudo[(int) ch]);
                ch = (byte) (in[i] & 0x0F);
                out.append(pseudo[(int) ch]);
            }
        }
        return new String(out);
    }

    public static void main(String[] args) {
        try {
            Tracker t = new Tracker(new Torrent(new File(args[0])), new Client());
            System.out.println(t.generateQuery());
        } catch (Exception e) {
            System.err.println("Uh oh...");
        }
    }
}
