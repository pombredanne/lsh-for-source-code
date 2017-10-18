import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;

public class LogonDialog extends JFrame implements ActionListener, DocumentListener {

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private JComboBox combo;

    private JTextField editor;

    private boolean connected = false;

    private boolean localConnection = false;

    private boolean allowEnter = true;

    private ServerInterface si = null;

    private Properties properties = new Properties();

    JPanel parent = new JPanel(new BorderLayout());

    private JComponent oldComponent = null;

    JPanel p;

    JTextField usernameField = new JTextField();

    JPasswordField passwordField = new JPasswordField();

    JPasswordField connectionPasswordField = new JPasswordField();

    JLabel connectionLabel = null;

    JLabel progressLabel;

    String connectionPassword = null;

    String username = null;

    String password = null;

    String address = null;

    int port = -1;

    int phase = 0;

    Box xx;

    HelpButton helpButton;

    public class HelpButton extends JButton {

        ImageIcon icon;

        ImageIcon icon2;

        public HelpButton(ImageIcon _icon, ImageIcon _icon2) {
            super(_icon);
            icon = _icon;
            icon2 = _icon2;
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            addMouseListener(new MouseAdapter() {

                public void mouseEntered(MouseEvent me) {
                    setIcon(icon2);
                }

                public void mouseExited(MouseEvent me) {
                    setIcon(icon);
                }
            });
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == cancelButton) {
            if (connected == true) si.disconnect();
            System.exit(0);
        } else if (event.getSource() == helpButton) {
            new LogonHelpDialog(this);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                enterValue();
            }
        });
    }

    public void enterValue() {
        if (!allowEnter) {
            return;
        }
        if (phase == 0) {
            String value = (String) combo.getSelectedItem();
            if (value.equals("this computer")) {
                localConnection = true;
                port = -1;
                boolean isProtected = isProtected();
                System.out.println("Is protected: " + isProtected + "\r\n");
                if (!isProtected) {
                    username = "Administrator";
                    password = "password";
                    allowEnter = false;
                    new ConnectionThread(si, this, false);
                    return;
                }
                connectionLabel.setText("Username:");
                usernameField.setText("");
                okButton.setEnabled(false);
                allowEnter = false;
                replaceComponent(usernameField);
                phase = 2;
                return;
            } else {
                localConnection = false;
                String arr[] = value.split(":");
                if (arr.length == 2) {
                    address = arr[0];
                    port = Integer.parseInt(arr[1]);
                } else {
                    showErrorMessage(this, "Enter also port number, separated by :");
                    return;
                }
            }
            connectionLabel.setText("Connection password:");
            connectionPasswordField.setText("");
            okButton.setEnabled(false);
            allowEnter = false;
            replaceComponent(connectionPasswordField);
            phase++;
            return;
        } else if (phase == 1) {
            connectionPassword = connectionPasswordField.getText();
            connectionLabel.setText("Username:");
            usernameField.setText("");
            okButton.setEnabled(false);
            allowEnter = false;
            replaceComponent(usernameField);
            phase++;
            return;
        } else if (phase == 2) {
            username = usernameField.getText();
            usernameField.setText("");
            okButton.setEnabled(false);
            connectionLabel.setText("Password:");
            passwordField.setText("");
            replaceComponent(passwordField);
            phase++;
            return;
        }
        password = passwordField.getText();
        passwordField.setText("");
        okButton.setEnabled(false);
        cancelButton.setEnabled(false);
        passwordField.setEnabled(false);
        connectionLabel.setEnabled(false);
        allowEnter = false;
        if (localConnection) {
            new ConnectionThread(si, this, false);
        } else {
            new ConnectionThread(address, port, si, this, false);
        }
        return;
    }

    public String callPDManager(String[] args) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.directory(new File(".." + File.separator + "bin"));
            Process process = processBuilder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String result = br.readLine();
            System.out.println("result: " + result + "\r\n");
            return result;
        } catch (IOException e) {
            System.out.println("ERROR: " + e + "\r\n");
        }
        return "";
    }

    public void showErrorMessage(Component parent, String message) {
        Object[] options = { "OK" };
        JOptionPane.showOptionDialog(parent, message, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, null);
    }

    public void saveData(String address, int port) {
        if (properties.getProperty(address) == null) {
            String item = address + ":" + port;
            combo.addItem(item);
            properties.setProperty(item, "");
            try {
                FileOutputStream file = new FileOutputStream("client.dat");
                properties.store(file, "Pronetha Desktop");
                file.close();
            } catch (IOException exception) {
            }
        }
    }

    public LogonDialog(ServerInterface sis) {
        si = sis;
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(d.width / 2 - 195, d.height / 2 - 120, 390, 240);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowListener wl = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (connected == true) si.disconnect();
                System.exit(0);
            }
        };
        addWindowListener(wl);
        Box buttonpanel = Box.createHorizontalBox();
        buttonpanel.add(Box.createGlue());
        buttonpanel.add(okButton);
        buttonpanel.add(Box.createHorizontalStrut(8));
        buttonpanel.add(cancelButton);
        okButton.setPreferredSize(new Dimension(92, 30));
        cancelButton.setPreferredSize(new Dimension(92, 30));
        okButton.setFocusPainted(false);
        cancelButton.setFocusPainted(false);
        combo = new JComboBox();
        if (localConnectionPossible()) {
            properties.setProperty("this computer", "");
        }
        try {
            FileInputStream file = new FileInputStream("client.dat");
            properties.load(file);
            file.close();
        } catch (IOException exception) {
        }
        combo.setEditable(true);
        Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            combo.addItem(name);
        }
        connectionLabel = new JLabel("Connect to:");
        combo.setBackground(Color.white);
        Box tekstit = Box.createVerticalBox();
        tekstit.add(Box.createVerticalStrut(20));
        xx = Box.createHorizontalBox();
        xx.add(connectionLabel);
        xx.add(Box.createGlue());
        URL url1 = ClassLoader.getSystemResource("help1.png");
        URL url2 = ClassLoader.getSystemResource("help2.png");
        helpButton = new HelpButton(new ImageIcon(url1), new ImageIcon(url2));
        helpButton.addActionListener(this);
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream("help.html");
            if (in != null) {
                StringBuffer buffer = new StringBuffer();
                byte[] b = new byte[1024];
                for (int n; (n = in.read(b)) != -1; ) {
                    buffer.append(new String(b, 0, n));
                }
                helpButton.setToolTipText(buffer.toString());
                in.close();
            }
        } catch (IOException exception) {
        }
        xx.add(Box.createHorizontalStrut(2));
        xx.setMinimumSize(new Dimension(0, 24));
        xx.setMaximumSize(new Dimension(390, 24));
        xx.setPreferredSize(new Dimension(390, 24));
        tekstit.add(xx);
        tekstit.add(Box.createVerticalStrut(2));
        tekstit.add(parent);
        parent.add(combo);
        oldComponent = combo;
        tekstit.add(Box.createGlue());
        parent.setMinimumSize(new Dimension(0, 24));
        parent.setMaximumSize(new Dimension(390, 24));
        parent.setPreferredSize(new Dimension(390, 24));
        p = new JPanel(new BorderLayout());
        p.add(buttonpanel, BorderLayout.SOUTH);
        p.add(tekstit, BorderLayout.CENTER);
        p.setBounds(0, 0, 390, 240);
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(80, 180, 10, 18));
        URL myURL = ClassLoader.getSystemResource("pronetha.png");
        ImageIcon myImage = new ImageIcon(myURL);
        JLabel label = new JLabel(myImage);
        label.setBounds(0, 0, 390, 240);
        JLayeredPane desktop = new JLayeredPane();
        desktop.add(label, JLayeredPane.DEFAULT_LAYER);
        desktop.add(p, JLayeredPane.PALETTE_LAYER);
        JPanel pp = new JPanel(new BorderLayout());
        pp.add(desktop);
        getContentPane().add(pp);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        editor = ((JTextField) combo.getEditor().getEditorComponent());
        editor.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent evt) {
            }

            public void keyTyped(KeyEvent evt) {
            }

            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            enterValue();
                        }
                    });
                }
            }
        });
        editor.setCaretPosition(editor.getText().length());
        usernameField.addActionListener(this);
        passwordField.addActionListener(this);
        connectionPasswordField.addActionListener(this);
        usernameField.getDocument().addDocumentListener(this);
        passwordField.getDocument().addDocumentListener(this);
        connectionPasswordField.getDocument().addDocumentListener(this);
        editor.getDocument().addDocumentListener(this);
        setUndecorated(true);
        setVisible(true);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                editor.selectAll();
                combo.requestFocus();
            }
        });
    }

    public boolean localConnectionPossible() {
        File f = new File(".." + File.separator + "bin" + File.separator + "pdmanager.exe");
        return f.exists();
    }

    public int readLocalPort() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("id.txt"));
            String line = in.readLine();
            int localPort = Integer.parseInt(line);
            in.close();
            return localPort;
        } catch (Exception exception) {
        }
        return -1;
    }

    public boolean isProtected() {
        File file = new File("protected");
        return file.exists();
    }

    public void hideProgressInfo() {
        setVisible(false);
        restore();
    }

    public void connected(String address, int port) {
        connected = true;
        try {
            if (localConnection) {
                byte key[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                si.setEncryptionKey(key);
            } else {
                saveData(address, port);
                MessageDigest mds = MessageDigest.getInstance("SHA");
                mds.update(connectionPassword.getBytes("UTF-8"));
                si.setEncryptionKey(mds.digest());
            }
            if (!si.login(username, password)) {
                si.disconnect();
                connected = false;
                showErrorMessage(this, "Authentication Failure");
                restore();
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    connectionLabel.setText("");
                    progressLabel = new JLabel("Loading... Please wait.");
                    progressLabel.setOpaque(true);
                    progressLabel.setBackground(Color.white);
                    replaceComponent(progressLabel);
                    cancelButton.setEnabled(true);
                    xx.remove(helpButton);
                }
            });
        } catch (Exception e) {
            System.out.println("connected: Exception: " + e + "\r\n");
        }
        ;
    }

    public void connectionFailed(String address, int port) {
        showErrorMessage(this, "Connection to " + address + " (port " + port + ") failed.");
        restore();
    }

    public void restore() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                replaceComponent(combo);
                connectionLabel.setText("Connect to:");
                okButton.setEnabled(true);
                cancelButton.setEnabled(true);
                connectionLabel.setEnabled(true);
                passwordField.setEnabled(true);
                allowEnter = true;
                phase = 0;
            }
        });
    }

    public void replaceComponent(JComponent newComponent) {
        if (oldComponent != null) parent.remove(oldComponent);
        parent.add(newComponent);
        oldComponent = newComponent;
        parent.revalidate();
        parent.repaint();
        newComponent.requestFocus();
    }

    public byte[] convertHexStringToBytes(String data) {
        byte[] arr = new byte[20];
        int i = 0;
        StringTokenizer st = new StringTokenizer(data);
        while (st.hasMoreTokens()) {
            String item = st.nextToken();
            arr[i] = (byte) Integer.parseInt(item, 16);
            i++;
        }
        return arr;
    }

    public void insertUpdate(DocumentEvent event) {
        handleFieldEnabling(event);
    }

    public void removeUpdate(DocumentEvent event) {
        handleFieldEnabling(event);
    }

    public void changedUpdate(DocumentEvent event) {
    }

    public void handleFieldEnabling(DocumentEvent event) {
        int length = event.getDocument().getLength();
        if (length > 0) {
            allowEnter = true;
            okButton.setEnabled(true);
        } else {
            allowEnter = false;
            okButton.setEnabled(false);
        }
    }

    public void connectionLost() {
        System.out.println("LogonDialog, connectionLost\r\n");
        setVisible(true);
    }

    private class ConnectionThread implements Runnable {

        LogonDialog dialog;

        ServerInterface si;

        String address;

        int port;

        boolean localConnection;

        boolean reconnecting;

        ConnectionThread(String address, int port, ServerInterface si, LogonDialog dialog, boolean reconnecting) {
            this.address = address;
            this.dialog = dialog;
            this.port = port;
            this.si = si;
            this.reconnecting = reconnecting;
            localConnection = false;
            Thread thread = new Thread(this);
            thread.start();
        }

        ConnectionThread(ServerInterface si, LogonDialog dialog, boolean reconnecting) {
            this.address = "0.0.0.0";
            this.dialog = dialog;
            this.port = -1;
            this.si = si;
            this.reconnecting = reconnecting;
            localConnection = true;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void run() {
            String[] args = new String[2];
            if (localConnection && !reconnecting) {
                args[0] = "../bin/pdmanager.exe";
                args[1] = "-status";
                if (!callPDManager(args).equals("Running") && !callPDManager(args).equals("Starting")) {
                    args[0] = "../bin/pdmanager.exe";
                    args[1] = "-start";
                    callPDManager(args);
                }
            }
            if (localConnection) {
                args[0] = "../bin/pdmanager.exe";
                args[1] = "-status";
                while (!callPDManager(args).equals("Running")) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
                port = readLocalPort();
                if (port == -1) {
                }
                address = "127.0.0.1";
            }
            if (reconnecting) {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                }
            }
            tryToConnect();
        }

        public void tryToConnect() {
            boolean connected = si.Connect(address, port);
            if (!connected) {
                dialog.connectionFailed(address, port);
                return;
            }
            dialog.connected(address, port);
        }
    }
}
