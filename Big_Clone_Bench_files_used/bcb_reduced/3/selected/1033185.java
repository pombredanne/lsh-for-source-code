package org.me.hello;

import java.security.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.lang.String;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import java.lang.*;
import java.util.Date;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.*;
import java.applet.Applet;

/**
 *
 * @author  davidg
 */
public class NewApplet extends java.applet.Applet {

    int loginRequest = 0;

    public int loginRefused = 1;

    public int loginAccepted = 2;

    public int messagePacket = 3;

    public int warningMessage = 4;

    public int errorMessage = 5;

    SSLSocket socket = null;

    SSLSocketFactory factory;

    InputStream inputstream;

    InputStreamReader inputstreamreader;

    BufferedReader bufferedreader;

    OutputStream outputstream;

    OutputStreamWriter outputstreamwriter;

    BufferedWriter bufferedwriter;

    Timer pingTimer = null;

    messageReceiver msgRec = null;

    String ipAddr;

    int port;

    int delay;

    /** Initializes the applet NewApplet */
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    initComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jLabelTitle = new javax.swing.JLabel();
        jPanelFooter = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLogin = new java.awt.Panel();
        jLabelUserName = new javax.swing.JLabel();
        jTextFieldUserName = new javax.swing.JTextField();
        jPasswordUserPassword = new javax.swing.JPasswordField();
        jLabelUserPassword = new javax.swing.JLabel();
        jButtonLogin = new javax.swing.JButton();
        jConnect = new java.awt.Panel();
        jLabel1 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        setLayout(new java.awt.BorderLayout());
        setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jLabelTitle.setFont(new java.awt.Font("Lucida Grande", 0, 24));
        jLabelTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelTitle.setText("Netwall Login");
        add(jLabelTitle, java.awt.BorderLayout.NORTH);
        jScrollPane1.setBackground(null);
        jTextArea1.setBackground(new java.awt.Color(204, 204, 204));
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setAutoscrolls(false);
        jTextArea1.setDragEnabled(false);
        jTextArea1.setFocusable(false);
        jTextArea1.setRequestFocusEnabled(false);
        jScrollPane1.setViewportView(jTextArea1);
        jPanelFooter.add(jScrollPane1);
        add(jPanelFooter, java.awt.BorderLayout.SOUTH);
        jLayeredPane1.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jLogin.setLayout(null);
        jLogin.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jLabelUserName.setText("User Name");
        jLogin.add(jLabelUserName);
        jLabelUserName.setBounds(80, 30, 68, 16);
        jTextFieldUserName.setFocusCycleRoot(true);
        jLogin.add(jTextFieldUserName);
        jTextFieldUserName.setBounds(180, 30, 100, 22);
        jPasswordUserPassword.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        jLogin.add(jPasswordUserPassword);
        jPasswordUserPassword.setBounds(180, 60, 100, 22);
        jLabelUserPassword.setText("Password");
        jLogin.add(jLabelUserPassword);
        jLabelUserPassword.setBounds(90, 60, 59, 16);
        jButtonLogin.setFocusTraversalPolicyProvider(true);
        jButtonLogin.setLabel("Login");
        jButtonLogin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                login(evt);
            }
        });
        jLogin.add(jButtonLogin);
        jButtonLogin.setBounds(140, 90, 75, 29);
        jLogin.setBounds(0, 0, 360, 130);
        jLayeredPane1.add(jLogin, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jConnect.setLayout(null);
        jConnect.setVisible(false);
        jLabel1.setText("You have been logged in successfully.");
        jConnect.add(jLabel1);
        jLabel1.setBounds(60, 30, 250, 16);
        jButton2.setText("Logout");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jConnect.add(jButton2);
        jButton2.setBounds(140, 80, 75, 29);
        jConnect.setBounds(0, 10, 360, 120);
        jLayeredPane1.add(jConnect, javax.swing.JLayeredPane.DEFAULT_LAYER);
        add(jLayeredPane1, java.awt.BorderLayout.CENTER);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        reset();
    }

    public void writeMessage(String msg) {
        String newmsg = msg + this.jTextArea1.toString();
        this.jTextArea1.removeAll();
        this.jTextArea1.append(msg + "\n");
    }

    public void reset() {
        this.jPasswordUserPassword.setText("");
        this.jTextFieldUserName.setText("");
        jLogin.setEnabled(true);
        jLogin.setVisible(true);
        jConnect.setVisible(false);
        try {
            if (pingTimer != null) {
                pingTimer.cancel();
                pingTimer.purge();
            }
            if (msgRec != null) {
                msgRec.interrupt();
                msgRec.stop();
                msgRec = null;
            }
            if (socket != null) {
                socket.getOutputStream().write(2);
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    private void login(java.awt.event.ActionEvent evt) {
        String ipAddress = "";
        int tcpPort = 0;
        String host;
        int port;
        int delay;
        char[] passphrase;
        int runloop = 2;
        ipAddress = this.getParameter("IP");
        tcpPort = Integer.parseInt(this.getParameter("PORT"));
        delay = Integer.parseInt(this.getParameter("DELAY"));
        while (runloop > 0) {
            try {
                host = ipAddress;
                port = tcpPort;
                String p = "changeit";
                passphrase = p.toCharArray();
                System.out.println("Passphrase is: " + passphrase);
                System.out.println("test");
                File file = new File("netwallcacerts");
                KeyStore ks;
                try {
                    System.out.println("Loading KeyStore " + file + "...");
                    InputStream in = new FileInputStream(file);
                    ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(in, passphrase);
                    in.close();
                } catch (Exception h) {
                    System.out.println("Makeing new store!");
                    ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(null, passphrase);
                }
                SSLContext context = SSLContext.getInstance("TLS");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
                SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
                context.init(null, new TrustManager[] { tm }, null);
                factory = context.getSocketFactory();
                System.out.println("Opening connection to " + host + ":" + port + "...");
                socket = (SSLSocket) factory.createSocket(host, port);
                socket.setSoTimeout(2000);
                socket.setKeepAlive(true);
                try {
                    System.out.println("Starting SSL handshake...");
                    socket.startHandshake();
                    System.out.println("Handshake successful.");
                    System.out.flush();
                    int i;
                    byte[] b = new byte[1];
                    byte[] tb;
                    try {
                        i = socket.getInputStream().read(b);
                        if ((int) b[0] == 0) {
                            String user = jTextFieldUserName.getText();
                            String pass = jPasswordUserPassword.getText();
                            b = new byte[3 + user.length() + pass.length()];
                            b[0] = 0;
                            b[1] = new Integer(user.length()).byteValue();
                            System.out.println("user length: " + user.length());
                            b[2] = new Integer(pass.length()).byteValue();
                            System.out.println("password length: " + pass.length());
                            tb = (user + pass).getBytes();
                            int k;
                            for (k = 0; k < tb.length; k++) {
                                b[3 + k] = tb[k];
                            }
                            socket.getOutputStream().write(b);
                            socket.getOutputStream().flush();
                        } else {
                            socket.close();
                            return;
                        }
                        b = new byte[1];
                        i = socket.getInputStream().read(b);
                        if ((int) b[0] == 2) {
                            socket.setSoTimeout(0);
                            jLogin.setVisible(false);
                            jConnect.setVisible(true);
                            jLogin.setEnabled(false);
                            System.out.println("Established connection.");
                            pingTimer = new Timer();
                            pingTimer.scheduleAtFixedRate(new pingTheServer(socket.getOutputStream()), 200, delay);
                            msgRec = new messageReceiver(socket.getInputStream(), this);
                            msgRec.start();
                            return;
                        } else {
                            socket.close();
                            writeMessage("Error: Username or Password is incorrect.");
                            return;
                        }
                    } catch (IOException e) {
                        System.out.println("IO Exception");
                        socket.close();
                        return;
                    }
                } catch (SSLException e) {
                }
                Frame f = new Frame();
                MessageDialog md = new MessageDialog(f, "Accept Netwall Certificate?", "Yes", "No");
                if ("Yes".equals(md.getUserAction())) {
                    X509Certificate[] chain = tm.chain;
                    if (chain == null) {
                        System.out.println("Could not obtain server certificate chain");
                        return;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println();
                    System.out.println("Server sent " + chain.length + " certificate(s):");
                    System.out.println();
                    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    for (int i = 0; i < chain.length; i++) {
                        X509Certificate cert = chain[i];
                        System.out.println(" " + (i + 1) + " Subject " + cert.getSubjectDN());
                        System.out.println("   Issuer  " + cert.getIssuerDN());
                        sha1.update(cert.getEncoded());
                        System.out.println("   sha1    " + toHexString(sha1.digest()));
                        md5.update(cert.getEncoded());
                        System.out.println("   md5     " + toHexString(md5.digest()));
                        System.out.println();
                    }
                    int k = 0;
                    X509Certificate cert = chain[k];
                    String alias = host + "-" + (k + 1);
                    ks.setCertificateEntry(alias, cert);
                    OutputStream out = new FileOutputStream("netwallcacerts");
                    ks.store(out, passphrase);
                    out.close();
                } else {
                    writeMessage("Error: Cannot connect to Netwall Server");
                    reset();
                    return;
                }
            } catch (Exception exception) {
            }
        }
        runloop--;
        writeMessage("Error: Failed to connect Netwall Server");
        reset();
    }

    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private class pingTheServer extends TimerTask {

        private OutputStream writer;

        pingTheServer(OutputStream write) {
            writer = write;
        }

        public void run() {
            try {
                writer.write(1);
                writer.flush();
            } catch (Exception e) {
                System.out.println("Got a ping exception!");
            }
        }
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;

        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButtonLogin;

    private java.awt.Panel jConnect;

    private java.awt.Panel jConnected;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabelTitle;

    private javax.swing.JLabel jLabelUserName;

    private javax.swing.JLabel jLabelUserPassword;

    private javax.swing.JLayeredPane jLayeredPane1;

    private java.awt.Panel jLogin;

    private javax.swing.JPanel jPanelFooter;

    private javax.swing.JPasswordField jPasswordUserPassword;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextFieldUserName;

    private java.awt.Panel panel1;
}

class MyHandshakeListener implements HandshakeCompletedListener {

    public void handshakeCompleted(HandshakeCompletedEvent e) {
        System.out.println("Handshake succesful!");
    }
}

class Pinger extends Thread {

    Thread t;

    SSLSocket mySocket;

    int pingInterval;

    public Pinger(SSLSocket socket, int ping) {
        this.mySocket = socket;
        this.pingInterval = ping;
    }

    public void run() {
        try {
            t.sleep(pingInterval);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}

class MessageDialog extends Dialog implements ActionListener {

    private String buttonClicked;

    public String getUserAction() {
        return buttonClicked;
    }

    public MessageDialog(Frame parent, String message) {
        this(parent, message, "OK", null, null);
    }

    public MessageDialog(Frame parent, String message, String button) {
        this(parent, message, button, null, null);
    }

    public MessageDialog(Frame parent, String message, String button1, String button2) {
        this(parent, message, button1, button2, null);
    }

    public MessageDialog(Frame parent, String message, String button1, String button2, String button3) {
        super(parent, null, true);
        setBackground(Color.white);
        add("Center", new MessageCanvas(message));
        Panel buttonBar = new Panel();
        buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        Button b1 = new Button((button1 == null) ? "OK" : button1);
        b1.addActionListener(this);
        buttonBar.add(b1);
        if (button2 != null) {
            Button b2 = new Button(button2);
            b2.addActionListener(this);
            buttonBar.add(b2);
        }
        if (button3 != null) {
            Button b3 = new Button(button3);
            b3.addActionListener(this);
            buttonBar.add(b3);
        }
        add("South", buttonBar);
        pack();
        setLocation(parent.getLocation().x + 50, parent.getLocation().y + 30);
        show();
    }

    public Insets getInsets() {
        Insets ins = (Insets) super.getInsets().clone();
        ins.left += 5;
        ins.right += 5;
        ins.bottom += 12;
        ins.top += 5;
        return ins;
    }

    public void actionPerformed(ActionEvent evt) {
        buttonClicked = evt.getActionCommand();
        dispose();
    }

    private static class MessageCanvas extends Canvas {

        private String message;

        private Vector messageStrings;

        private int messageWidth;

        private int messageHeight;

        private Font font;

        private int lineHeight;

        private int fontAscent;

        MessageCanvas(String message) {
            if (message == null) this.message = ""; else this.message = message;
        }

        public Dimension getPreferredSize() {
            if (messageStrings == null) makeStringList();
            return new Dimension(messageWidth + 20, messageHeight + 17);
        }

        public void paint(Graphics g) {
            if (messageStrings == null) makeStringList();
            int y = (getSize().height - messageHeight) / 2 + fontAscent;
            if (y < fontAscent) y = fontAscent;
            int x = (getSize().width - messageWidth) / 2;
            if (x < 0) x = 0;
            g.setFont(font);
            for (int i = 0; i < messageStrings.size(); i++) {
                g.drawString((String) messageStrings.elementAt(i), x, y);
                y += lineHeight;
            }
        }

        private void makeStringList() {
            messageStrings = new Vector();
            font = new Font("Dialog", Font.PLAIN, 12);
            FontMetrics fm = getFontMetrics(font);
            lineHeight = fm.getHeight() + 3;
            fontAscent = fm.getAscent();
            int totalWidth = fm.stringWidth(message);
            if (totalWidth <= 280) {
                messageStrings.addElement(message);
                messageWidth = 280;
                messageHeight = lineHeight;
            } else {
                if (totalWidth > 1800) messageWidth = Math.min(500, totalWidth / 6); else messageWidth = 300;
                int actualWidth = 0;
                String line = "    ";
                String word = "";
                message += " ";
                for (int i = 0; i < message.length(); i++) {
                    if (message.charAt(i) == ' ') {
                        if (fm.stringWidth(line + word) > messageWidth + 8) {
                            messageStrings.addElement(line);
                            actualWidth = Math.max(actualWidth, fm.stringWidth(line));
                            line = "";
                        }
                        line += word;
                        if (line.length() > 0) line += ' ';
                        word = "";
                    } else {
                        word += message.charAt(i);
                    }
                }
                if (line.length() > 0) {
                    messageStrings.addElement(line);
                    actualWidth = Math.max(actualWidth, fm.stringWidth(line));
                }
                messageHeight = lineHeight * messageStrings.size() - fm.getLeading();
                messageWidth = Math.max(280, actualWidth);
            }
        }
    }
}

class messageReceiver extends Thread {

    Thread t;

    InputStream stream;

    NewApplet app;

    public messageReceiver(InputStream stream, NewApplet app) {
        this.stream = stream;
        this.app = app;
    }

    public void run() {
        System.out.println("Running thread!");
        InputStreamReader bs = null;
        BufferedReader br = null;
        try {
            bs = new InputStreamReader(stream);
            br = new BufferedReader(bs);
        } catch (Exception e) {
        }
        while (true) {
            try {
                int err;
                char[] b = new char[1];
                err = br.read(b);
                if (err == -1) {
                    br.close();
                    bs.close();
                    app.reset();
                    return;
                }
                if ((int) b[0] >= 3 && (int) b[0] <= 6) {
                    char[] c = new char[2];
                    br.read(c);
                    int msgLength = (int) c[0] + ((int) c[1]) * 256 + 2;
                    int read;
                    char[] message = new char[msgLength];
                    read = br.read(message);
                    String msg = "";
                    for (int i = 0; i < msgLength; i++) {
                        msg += message[i];
                    }
                    if ((int) b[0] == 3) {
                        app.writeMessage("Message: " + msg);
                    }
                    if ((int) b[0] == 4) {
                        app.writeMessage("Warning: " + msg);
                    }
                    if ((int) b[0] == 5) {
                        app.writeMessage("Error: " + msg);
                    }
                    if ((int) b[0] == 6) {
                        app.writeMessage("Error: " + msg);
                    }
                }
            } catch (IOException e) {
                try {
                    br.close();
                    bs.close();
                    app.reset();
                    return;
                } catch (Exception f) {
                    app.reset();
                    return;
                }
            }
        }
    }
}
