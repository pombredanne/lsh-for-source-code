package org.syrup.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a BLOB storage implementation by handling requests from a
 * BlobClient.
 * 
 * @author Robbert van Dalen
 */
public class BlobServer extends Thread {

    static final String COPYRIGHT = "Copyright 2005 Robbert van Dalen." + "At your option, you may copy, distribute, or make derivative works under " + "the terms of The Artistic License. This License may be found at " + "http://www.opensource.org/licenses/artistic-license.php. " + "THERE IS NO WARRANTY; USE THIS PRODUCT AT YOUR OWN RISK.";

    private static final String hextable[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    private final Socket s;

    private final Logger l;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    /**
     */
    public static void main(String[] args) throws Exception {
        int port = 6666;
        int max_connections = 1024 * 10;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length == 2) {
            max_connections = Integer.parseInt(args[1]);
        }
        ServerSocket server = new ServerSocket(port, max_connections);
        Logger logger = Logger.getLogger("org.syrup.services.BlobServer");
        logger.setLevel(Level.INFO);
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Starting BlobServer");
        }
        while (true) {
            try {
                new BlobServer(server.accept(), logger);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     */
    private BlobServer(Socket socket, Logger logger) {
        s = socket;
        l = logger;
        this.start();
    }

    /** Main processing method for the BlobServer object */
    public void run() {
        try {
            int m = s.getInputStream().read();
            if (m == 'R') {
                read(s);
            } else if (m == 'W') {
                write(s);
            } else if (m == 'G') {
                byte b[] = new byte[4];
                int l = read(s.getInputStream(), b, 0, 4);
                if (l == 4 && b[0] == 'E' && b[1] == 'T' && b[2] == ' ' && b[3] == '/') {
                    String s2 = "HTTP/1.1 200 OK\n\n";
                    s.getOutputStream().write(s2.getBytes());
                    read(s);
                } else {
                    throw new Exception("Malformed HTTP request");
                }
            } else {
                throw new Exception("Command " + (char) m + " not supported");
            }
        } catch (Throwable e) {
            l.log(Level.SEVERE, Thread.currentThread().toString(), e);
        } finally {
            try {
                s.close();
            } catch (Exception e) {
                l.log(Level.SEVERE, Thread.currentThread().toString(), e);
            }
        }
    }

    /**
     */
    private final void read(Socket s) throws Exception {
        if (l.isLoggable(Level.INFO)) {
            l.log(Level.INFO, Thread.currentThread().toString() + ": READ START");
        }
        InputStream si = null;
        OutputStream so = null;
        try {
            si = s.getInputStream();
            so = s.getOutputStream();
            byte b[] = new byte[80];
            byte bb = 0;
            int il = 0;
            while (il <= 80 && ((bb = (byte) si.read()) > ' ')) {
                b[il++] = bb;
            }
            if (il >= 80 && il <= 0) {
                throw new Exception("Client has requested identifier with illegal length: " + il);
            }
            String f = (new String(b, 0, il)).trim();
            if (il >= 0) {
                File ff = new File(f);
                f = ff.getName();
                if (l.isLoggable(Level.INFO)) {
                    l.log(Level.INFO, Thread.currentThread().toString() + ": READ " + f);
                }
                FileInputStream fi = null;
                try {
                    fi = new FileInputStream(f);
                    while ((il = fi.read(b)) >= 0) {
                        so.write(b, 0, il);
                    }
                } finally {
                    close(fi);
                }
            }
        } finally {
            close(si);
            close(so);
        }
        if (l.isLoggable(Level.INFO)) {
            l.log(Level.INFO, Thread.currentThread().toString() + ": READ END");
        }
    }

    /**
     */
    private final long getSequenceNumber() throws Exception {
        return (long) (Math.random() * (double) (1 << 62));
    }

    /**
     */
    private final void write(Socket s) throws Exception {
        long sequenceNumber = getSequenceNumber();
        MessageDigest mdigest = (MessageDigest) MessageDigest.getInstance("SHA").clone();
        if (l.isLoggable(Level.INFO)) {
            l.log(Level.INFO, Thread.currentThread().toString() + ": WRITE START");
        }
        InputStream si = null;
        OutputStream so = null;
        try {
            si = s.getInputStream();
            so = s.getOutputStream();
            byte b[] = new byte[8192];
            String f = formatter.format(new Date()) + "_" + sequenceNumber;
            int li = si.read(b);
            if (li >= 0) {
                if (l.isLoggable(Level.INFO)) {
                    l.log(Level.INFO, Thread.currentThread().toString() + ": WRITE " + f);
                }
                FileOutputStream of = null;
                try {
                    of = new FileOutputStream(f);
                    of.write(b, 0, li);
                    mdigest.update(b, 0, li);
                    while ((li = si.read(b)) >= 0) {
                        mdigest.update(b, 0, li);
                        of.write(b, 0, li);
                    }
                    of.flush();
                } finally {
                    close(of);
                }
                byte[] digest = mdigest.digest();
                String id = toHexID(digest);
                File destfile = new File(id);
                if (destfile.exists()) {
                    new File(f).delete();
                    if (l.isLoggable(Level.INFO)) {
                        l.log(Level.INFO, Thread.currentThread().toString() + ": REMOVED " + f + " because hash " + id + " was already existing");
                    }
                    s.shutdownInput();
                    returnID(id, so);
                } else {
                    boolean rename = (new File(f)).renameTo(destfile);
                    if (rename) {
                        s.shutdownInput();
                        returnID(id, so);
                    } else {
                        throw new Exception("Could not rename " + f + " to " + destfile);
                    }
                }
            }
        } finally {
            close(so);
        }
        if (l.isLoggable(Level.INFO)) {
            l.log(Level.INFO, Thread.currentThread().toString() + ": WRITE END");
        }
    }

    private static void returnID(String id, OutputStream so) throws Exception {
        so.write((id).getBytes());
    }

    /**
     */
    private static int read(InputStream i, byte[] b, int s, int l) throws IOException {
        int k = 0;
        int t = 0;
        while ((k = i.read(b, s, l)) >= 0 && s < l) {
            s += k;
            t += k;
            l -= k;
        }
        return t;
    }

    private static final void close(InputStream i) throws Exception {
        if (i != null) {
            i.close();
        }
    }

    private static final void close(OutputStream o) throws Exception {
        if (o != null) {
            o.close();
        }
    }

    private static String toHexID(byte array[]) {
        StringBuffer result = new StringBuffer(array.length * 2);
        result.append("HEXID_");
        for (int i = 0; i < array.length; i++) {
            int c = (array[i] & 0xF0) >>> 4;
            result.append(hextable[c]);
            c = (array[i] & 0x0F);
            result.append(hextable[c]);
        }
        return new String(result);
    }
}
