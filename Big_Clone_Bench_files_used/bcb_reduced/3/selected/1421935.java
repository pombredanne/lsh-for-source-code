package com.tylerhjones.boip.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JLabel;

/**
 *
 * @author tyler
 */
public class ServerCore implements Runnable {

    private static final String TAG = "ServerCore";

    private static JLabel lblLastClient = null;

    protected Settings SET = new Settings();

    private BufferedReader streamIn = null;

    private PrintStream streamOut = null;

    private Thread thread = null;

    private ServerSocket listener;

    private Socket socket;

    private boolean IsActive = true;

    private boolean runThread = false;

    private String input = "";

    private static final String R_DSEP = "\\|";

    private static final String R_SMC = ";$";

    private static final String DSEP = "||";

    private static final String SMC = ";";

    private static final String CHK = "CHECK";

    private static final String CHKOK = "CHECK_OK";

    private static final String NONE = "NONE";

    private static final String VER = "VERSION";

    private static final String ER9 = "ERR9";

    private static final String ER1 = "ERR1";

    private static final String ER2 = "ERR2";

    private static final String ER3 = "ERR3";

    private static final String THX = "THANKS\n";

    private static final String OK = "OK\n";

    private static final String NOPE = "NOPE\n";

    private static String server_hash = "NONE";

    KeypressEmulator KP = new KeypressEmulator();

    public ServerCore() {
        System.out.println(TAG + " -- Constructor was called!");
    }

    public void setInfoLabel(JLabel lbl) {
        lblLastClient = lbl;
    }

    public void run() {
        this.runThread = true;
        synchronized (this) {
            this.thread = Thread.currentThread();
        }
        if (!startListener()) {
            return;
        }
        while (runThread()) {
            if (!listener.isClosed()) {
                try {
                    pln(TAG + " -- Waiting for a client ...");
                    socket = listener.accept();
                    openStreams();
                    if (!IsActive) {
                        pln(TAG + " -- Receiving data from client while deactivated: Responding 'NOPE'");
                        streamOut.println(NOPE);
                    } else {
                        pln(TAG + " -- Client accepted: " + socket);
                        if (lblLastClient != null) {
                            lblLastClient.setText(this.socket.getInetAddress().toString() + " on port " + this.socket.getPort());
                        }
                        try {
                            input = streamIn.readLine();
                            pln(TAG + " -- Client sent data: " + input);
                            if (input != null) {
                                pln(TAG + " -- Rec'd data from " + this.socket.getInetAddress().toString() + ": '" + input + "'");
                                String res = ParseData(input.trim());
                                if (res.equals(CHKOK)) {
                                    pln(TAG + " -- Parser sent 'OK' to client");
                                    streamOut.println(OK);
                                } else if (res.startsWith("ERR")) {
                                    pln(TAG + " -- Parser sent data to client: " + res);
                                    streamOut.println(res + "\n");
                                } else if (res.equals(VER)) {
                                    pln(TAG + " -- Parser sent version info to client.");
                                    streamOut.println("BarcodeOverIP-Server v0.6.2 (Java) -- http://tylerhjones.me / http://boip.tylerjones.me");
                                    streamOut.print("\n*******************************************************************\nBarcodeOverIP-server " + SET.APP_INFO + " \nThis server is for use with mobile device applications.\nYou must have the right client to use it!\nPlease visit: https://code.google.com/p/barcodeoverip/ for more\ninformation on available clients.\n\nWritten by: Tyler H. Jones (me@tylerjones.me) (C) 2012\nGoogle Code Website: https://code.google.com/p/barcodeoverip/\n*******************************************************************\n\n");
                                } else if (res.length() > 0 && res != null) {
                                    pln(TAG + " -- Parser returned a barcode for system input: " + res);
                                    pln(TAG + " -- Sending keystrokes to system...");
                                    KP.typeString(res, SET.getAppendNL());
                                    pln(TAG + " -- Barcode was inputted. Sending 'THANKS' to client.");
                                    streamOut.println(THX);
                                } else {
                                    streamOut.println("ERR99\n");
                                    closeStreams();
                                    perr("\n***FATAL ERROR!!!*** -- this.ParseData returned NULL string that is supposed to be the barcode data.");
                                    return;
                                }
                            }
                        } catch (IOException ioe) {
                            perr(TAG + " -- IOException on socket listen: " + ioe);
                        }
                    }
                    closeStreams();
                } catch (IOException ie) {
                    perr(TAG + " -- Connection Acceptance Error: " + ie);
                }
            }
        }
        stopListener();
        pln(TAG + " -- The thread loop exited, exiting thread.");
    }

    public boolean startListener() {
        try {
            server_hash = "NONE";
            if (!SET.getPass().equals("")) {
                server_hash = SHA1(SET.getPass()).trim().toUpperCase();
            }
        } catch (NoSuchAlgorithmException e) {
            perr(TAG + " -- NoSuchAlgorithmException was caught in ConnectionHandler.run()! -- " + e.toString());
            return false;
        }
        try {
            pln(TAG + " -- Starting listener...");
            if (SET.getHost().equals("") || SET.getHost().equals("0.0.0.0")) {
                listener = new ServerSocket(SET.getPort());
            } else {
                listener = new ServerSocket(SET.getPort(), 2, InetAddress.getByName(SET.getHost()));
            }
            pln(TAG + " -- Server started: " + listener);
        } catch (IOException ioe) {
            perr(TAG + " --  IOException was caught! (Starting...) - " + ioe.toString());
            perr(TAG + " -- startListener failed!");
            runThread = false;
            return false;
        }
        return true;
    }

    public boolean stopListener() {
        try {
            pln(TAG + " -- Stopping listener...");
            listener.close();
        } catch (IOException ioe) {
            perr(TAG + " --  IOException was caught! (Stopping...) - " + ioe.toString());
            return false;
        }
        return true;
    }

    private synchronized boolean runThread() {
        return this.runThread;
    }

    public synchronized void stop() {
        this.runThread = true;
        this.stopListener();
    }

    public void activate() {
        if (thread == null) {
            thread.start();
        }
        IsActive = true;
    }

    public void deactivate() {
        if (thread == null) {
            thread.start();
        }
        IsActive = false;
    }

    private void openStreams() throws IOException {
        streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        streamOut = new PrintStream(socket.getOutputStream());
    }

    private void closeStreams() throws IOException {
        if (socket != null) socket.close();
        if (streamIn != null) streamIn.close();
        if (streamOut != null) streamOut.close();
    }

    private String ParseData(String data) {
        String begin, end;
        boolean chkd = false;
        data = data.toUpperCase();
        if (data.equals(VER)) {
            return VER;
        }
        if (!data.endsWith(SMC)) {
            pln(TAG + " -- Parser - Invalid data format and/or syntax! - Command does not end with '" + SMC + "'.");
            return ER1;
        } else if (data.indexOf(DSEP) < 2 || ((data.length() - 1) - data.indexOf(DSEP)) < 3) {
            pln(TAG + " -- Parser - Invalid data format and/or syntax! - Command does not seem contain the '" + DSEP + "' data separator.");
            return ER2;
        } else {
            data = data.split(R_SMC)[0];
            try {
                begin = data.split(R_DSEP)[0].trim();
                end = data.split(R_DSEP)[2].trim();
                pln(TAG + " -- Parser - Begin: '" + begin + "',  End: '" + end + "'");
            } catch (ArrayIndexOutOfBoundsException e) {
                perr(TAG + " -- Parser - Invalid data format and/or syntax! - Command does not seem to be assembled right. It cannot be parsed. - Exception: " + e.getMessage());
                return ER3;
            }
            if (begin.equals(CHK)) {
                chkd = true;
            }
            if (server_hash.equals(NONE) || (begin.equals(server_hash) ^ end.equals(server_hash))) {
                if (chkd) {
                    return CHKOK;
                }
                return end;
            } else {
                return ER9;
            }
        }
    }

    public static String convertToHex_better(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        int length = data.length;
        for (int i = 0; i < length; ++i) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (++two_halfs < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes());
        byte byteData[] = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public void pln(String s) {
        System.out.println(s);
    }

    public void perr(String s) {
        System.err.println(s);
    }
}
