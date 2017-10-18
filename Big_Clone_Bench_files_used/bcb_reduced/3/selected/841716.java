package chatclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * The application's main frame.
 */
public class ChatClientView extends FrameView {

    public ChatClientView(SingleFrameApplication app) throws IOException {
        super(app);
        initComponents();
        showSettingsBox();
        readSettings();
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = ChatClientApp.getApplication().getMainFrame();
            aboutBox = new ChatClientAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        ChatClientApp.getApplication().show(aboutBox);
    }

    public String sendMessageToServer(String code, String message) throws FileNotFoundException, IOException {
        String query;
        String reply = null;
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(hostname, port);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            query = code + ":" + message;
            outToServer.writeBytes(query + '\n');
            reply = inFromServer.readLine();
        } catch (UnknownHostException ex) {
            javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "Unknown hostname.", "Error!", javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "Could not connect to host.", "Error!", javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        try {
            clientSocket.close();
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "Could not close socket.", "Error!", javax.swing.JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        return reply;
    }

    @Action
    public void showSettingsBox() throws IOException {
        if (settingsBox == null) {
            JFrame mainFrame = ChatClientApp.getApplication().getMainFrame();
            settingsBox = new ChatClientSettingsBox(mainFrame);
            settingsBox.setLocationRelativeTo(mainFrame);
            return;
        }
        ChatClientApp.getApplication().show(settingsBox);
    }

    private String MD5Hash(String Input) {
        String pass = Input;
        StringBuffer hexString = new StringBuffer();
        byte[] defaultBytes = pass.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            pass = hexString + "";
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Coulnd't encrypt password... ");
        }
        return hexString.toString();
    }

    public static void readSettings() {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader("settings.ini"));
            hostname = inputStream.readLine();
            port = Integer.parseInt(inputStream.readLine());
            timeStamps = Boolean.parseBoolean(inputStream.readLine());
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Invalid settings file...", "Invalid settings!", javax.swing.JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Invalid settings file... NumberFormatException was raised! ", "Invalid settings!", javax.swing.JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void addMessage(String newText) {
        addMessage(newText, false);
    }

    public static void addMessage(String newText, boolean noStamps) {
        java.util.Date now = new java.util.Date();
        int hours = now.getHours();
        int minutes = now.getMinutes();
        int seconds = now.getSeconds();
        if (!timeStamps || noStamps) txtMessages.setText(txtMessages.getText() + newText + "\n"); else txtMessages.setText(txtMessages.getText() + hours + ":" + minutes + ":" + seconds + ":" + " " + newText + "\n");
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        mainPanel = new javax.swing.JPanel();
        lblUserLogin = new javax.swing.JLabel();
        lblPassLogin = new javax.swing.JLabel();
        tfUsernameLogin = new javax.swing.JTextField();
        lblExUser = new javax.swing.JLabel();
        btntLogin = new javax.swing.JButton();
        btntNewAcc = new javax.swing.JButton();
        btntGuest = new javax.swing.JButton();
        tfPassLogin = new javax.swing.JPasswordField();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        PanNewUser = new javax.swing.JPanel();
        lblUsername = new javax.swing.JLabel();
        tfUserName = new javax.swing.JTextField();
        lblUnameCheck = new javax.swing.JLabel();
        lblPass = new javax.swing.JLabel();
        tfPassword = new javax.swing.JPasswordField();
        lblRePass = new javax.swing.JLabel();
        tfPassword2 = new javax.swing.JPasswordField();
        lblPassCheck = new javax.swing.JLabel();
        lblEmail = new javax.swing.JLabel();
        tfEmail = new javax.swing.JTextField();
        lblEmailCheck = new javax.swing.JLabel();
        lblNickName = new javax.swing.JLabel();
        tfNickName = new javax.swing.JTextField();
        lblNickCheck = new javax.swing.JLabel();
        lblFName = new javax.swing.JLabel();
        tfFName = new javax.swing.JTextField();
        lblFNameCheck = new javax.swing.JLabel();
        lblLName = new javax.swing.JLabel();
        tfLName = new javax.swing.JTextField();
        lblLNameCheck = new javax.swing.JLabel();
        btnBack = new javax.swing.JButton();
        btnSubmit = new javax.swing.JButton();
        PanChat = new javax.swing.JPanel();
        lblWelcome = new javax.swing.JLabel();
        tfSend = new javax.swing.JTextField();
        scrollUsers = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        tbsChan = new javax.swing.JTabbedPane();
        scrollChans = new javax.swing.JScrollPane();
        txtMessages = new javax.swing.JTextPane();
        mainPanel.setName("mainPanel");
        lblUserLogin.setDisplayedMnemonic('u');
        lblUserLogin.setLabelFor(tfUsernameLogin);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(chatclient.ChatClientApp.class).getContext().getResourceMap(ChatClientView.class);
        lblUserLogin.setText(resourceMap.getString("lblUserLogin.text"));
        lblUserLogin.setName("lblUserLogin");
        lblPassLogin.setDisplayedMnemonic('p');
        lblPassLogin.setLabelFor(tfPassLogin);
        lblPassLogin.setText(resourceMap.getString("lblPassLogin.text"));
        lblPassLogin.setName("lblPassLogin");
        tfUsernameLogin.setText(resourceMap.getString("tfUsernameLogin.text"));
        tfUsernameLogin.setName("tfUsernameLogin");
        lblExUser.setText(resourceMap.getString("lblExUser.text"));
        lblExUser.setName("lblExUser");
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(chatclient.ChatClientApp.class).getContext().getActionMap(ChatClientView.class, this);
        btntLogin.setAction(actionMap.get("showChatBox"));
        btntLogin.setMnemonic('g');
        btntLogin.setText(resourceMap.getString("btntLogin.text"));
        btntLogin.setName("btntLogin");
        btntLogin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btntLoginActionPerformed(evt);
            }
        });
        btntNewAcc.setMnemonic('n');
        btntNewAcc.setText(resourceMap.getString("btntNewAcc.text"));
        btntNewAcc.setName("btntNewAcc");
        btntNewAcc.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btntNewAccActionPerformed(evt);
            }
        });
        btntGuest.setMnemonic('s');
        btntGuest.setText(resourceMap.getString("btntGuest.text"));
        btntGuest.setEnabled(false);
        btntGuest.setName("btntGuest");
        tfPassLogin.setText(resourceMap.getString("tfPassLogin.text"));
        tfPassLogin.setName("tfPassLogin");
        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGap(30, 30, 30).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblPassLogin).addComponent(lblUserLogin)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblExUser).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(tfPassLogin, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE).addComponent(tfUsernameLogin, javax.swing.GroupLayout.Alignment.LEADING).addComponent(btntLogin)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(btntNewAcc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btntGuest)))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGap(47, 47, 47).addComponent(lblExUser).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblUserLogin).addComponent(tfUsernameLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btntNewAcc)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblPassLogin).addComponent(tfPassLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(btntGuest)).addGap(9, 9, 9).addComponent(btntLogin).addContainerGap(115, Short.MAX_VALUE)));
        menuBar.setName("menuBar");
        fileMenu.setMnemonic('f');
        fileMenu.setText(resourceMap.getString("fileMenu.text"));
        fileMenu.setName("fileMenu");
        settingsMenuItem.setAction(actionMap.get("showSettingsBox"));
        settingsMenuItem.setMnemonic('s');
        settingsMenuItem.setText(resourceMap.getString("settingsMenuItem.text"));
        settingsMenuItem.setName("settingsMenuItem");
        settingsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(settingsMenuItem);
        exitMenuItem.setAction(actionMap.get("quit"));
        exitMenuItem.setName("exitMenuItem");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        helpMenu.setMnemonic('h');
        helpMenu.setText(resourceMap.getString("helpMenu.text"));
        helpMenu.setName("helpMenu");
        aboutMenuItem.setAction(actionMap.get("showAboutBox"));
        aboutMenuItem.setName("aboutMenuItem");
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        statusPanel.setName("statusPanel");
        statusPanelSeparator.setName("statusPanelSeparator");
        statusMessageLabel.setName("statusMessageLabel");
        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel");
        progressBar.setName("progressBar");
        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE).addGroup(statusPanelLayout.createSequentialGroup().addContainerGap().addComponent(statusMessageLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 157, Short.MAX_VALUE).addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(statusAnimationLabel).addContainerGap()));
        statusPanelLayout.setVerticalGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(statusPanelLayout.createSequentialGroup().addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(statusMessageLabel).addComponent(statusAnimationLabel).addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(3, 3, 3)));
        PanNewUser.setName("PanNewUser");
        PanNewUser.setLayout(new java.awt.GridBagLayout());
        lblUsername.setDisplayedMnemonic('u');
        lblUsername.setLabelFor(tfUserName);
        lblUsername.setText(resourceMap.getString("lblUsername.text"));
        lblUsername.setName("lblUsername");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblUsername, gridBagConstraints);
        tfUserName.setText(resourceMap.getString("tfUserName.text"));
        tfUserName.setName("tfUserName");
        tfUserName.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfUserNameFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfUserName, gridBagConstraints);
        lblUnameCheck.setIcon(resourceMap.getIcon("lblUnameCheck.icon"));
        lblUnameCheck.setText(resourceMap.getString("lblUnameCheck.text"));
        lblUnameCheck.setName("lblUnameCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblUnameCheck, gridBagConstraints);
        lblPass.setDisplayedMnemonic('p');
        lblPass.setLabelFor(tfPassword);
        lblPass.setText(resourceMap.getString("lblPass.text"));
        lblPass.setName("lblPass");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblPass, gridBagConstraints);
        tfPassword.setText(resourceMap.getString("tfPassword.text"));
        tfPassword.setName("tfPassword");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfPassword, gridBagConstraints);
        lblRePass.setDisplayedMnemonic('a');
        lblRePass.setLabelFor(tfPassword2);
        lblRePass.setText(resourceMap.getString("lblRePass.text"));
        lblRePass.setName("lblRePass");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblRePass, gridBagConstraints);
        tfPassword2.setText(resourceMap.getString("tfPassword2.text"));
        tfPassword2.setName("tfPassword2");
        tfPassword2.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfPassword2FocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfPassword2, gridBagConstraints);
        lblPassCheck.setIcon(resourceMap.getIcon("lblPassCheck.icon"));
        lblPassCheck.setName("lblPassCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblPassCheck, gridBagConstraints);
        lblEmail.setDisplayedMnemonic('e');
        lblEmail.setLabelFor(tfEmail);
        lblEmail.setText(resourceMap.getString("lblEmail.text"));
        lblEmail.setName("lblEmail");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblEmail, gridBagConstraints);
        tfEmail.setText(resourceMap.getString("tfEmail.text"));
        tfEmail.setName("tfEmail");
        tfEmail.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfEmailFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfEmail, gridBagConstraints);
        lblEmailCheck.setIcon(resourceMap.getIcon("lblEmailCheck.icon"));
        lblEmailCheck.setName("lblEmailCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblEmailCheck, gridBagConstraints);
        lblNickName.setDisplayedMnemonic('n');
        lblNickName.setLabelFor(tfNickName);
        lblNickName.setText(resourceMap.getString("lblNickName.text"));
        lblNickName.setName("lblNickName");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblNickName, gridBagConstraints);
        tfNickName.setText(resourceMap.getString("tfNickName.text"));
        tfNickName.setName("tfNickName");
        tfNickName.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfNickNameFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfNickName, gridBagConstraints);
        lblNickCheck.setIcon(resourceMap.getIcon("lblNickCheck.icon"));
        lblNickCheck.setName("lblNickCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblNickCheck, gridBagConstraints);
        lblFName.setDisplayedMnemonic('i');
        lblFName.setLabelFor(tfFName);
        lblFName.setText(resourceMap.getString("lblFName.text"));
        lblFName.setName("lblFName");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblFName, gridBagConstraints);
        tfFName.setText(resourceMap.getString("tfFName.text"));
        tfFName.setName("tfFName");
        tfFName.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfFNameFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfFName, gridBagConstraints);
        lblFNameCheck.setIcon(resourceMap.getIcon("lblFNameCheck.icon"));
        lblFNameCheck.setName("lblFNameCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblFNameCheck, gridBagConstraints);
        lblLName.setDisplayedMnemonic('l');
        lblLName.setLabelFor(tfLName);
        lblLName.setText(resourceMap.getString("lblLName.text"));
        lblLName.setName("lblLName");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        PanNewUser.add(lblLName, gridBagConstraints);
        tfLName.setText(resourceMap.getString("tfLName.text"));
        tfLName.setName("tfLName");
        tfLName.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tfLNameFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        PanNewUser.add(tfLName, gridBagConstraints);
        lblLNameCheck.setIcon(resourceMap.getIcon("lblLNameCheck.icon"));
        lblLNameCheck.setName("lblLNameCheck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        PanNewUser.add(lblLNameCheck, gridBagConstraints);
        btnBack.setMnemonic('b');
        btnBack.setText(resourceMap.getString("btnBack.text"));
        btnBack.setMargin(new java.awt.Insets(1, 14, 2, 14));
        btnBack.setName("btnBack");
        btnBack.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 8);
        PanNewUser.add(btnBack, gridBagConstraints);
        btnSubmit.setMnemonic('r');
        btnSubmit.setText(resourceMap.getString("btnSubmit.text"));
        btnSubmit.setMargin(new java.awt.Insets(1, 18, 2, 18));
        btnSubmit.setName("btnSubmit");
        btnSubmit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 4, 0, 0);
        PanNewUser.add(btnSubmit, gridBagConstraints);
        PanChat.setName("PanChat");
        lblWelcome.setDisplayedMnemonic('s');
        lblWelcome.setLabelFor(tfSend);
        lblWelcome.setText(resourceMap.getString("lblWelcome.text"));
        lblWelcome.setName("lblWelcome");
        tfSend.setText(resourceMap.getString("tfSend.text"));
        tfSend.setName("tfSend");
        tfSend.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfSendActionPerformed(evt);
            }
        });
        tfSend.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                tfSendKeyTyped(evt);
            }
        });
        scrollUsers.setName("scrollUsers");
        tblUsers.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Image", "UserList" }) {

            Class[] types = new Class[] { java.lang.Object.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tblUsers.setName("tblUsers");
        tblUsers.setShowHorizontalLines(false);
        tblUsers.setShowVerticalLines(false);
        scrollUsers.setViewportView(tblUsers);
        tblUsers.getColumnModel().getColumn(0).setResizable(false);
        tblUsers.getColumnModel().getColumn(0).setPreferredWidth(10);
        tbsChan.setAutoscrolls(true);
        tbsChan.setName("tbsChan");
        scrollChans.setAutoscrolls(true);
        scrollChans.setName("scrollChans");
        txtMessages.setEditable(false);
        txtMessages.setName("txtMessages");
        scrollChans.setViewportView(txtMessages);
        tbsChan.addTab(resourceMap.getString("scrollChans.TabConstraints.tabTitle"), scrollChans);
        javax.swing.GroupLayout PanChatLayout = new javax.swing.GroupLayout(PanChat);
        PanChat.setLayout(PanChatLayout);
        PanChatLayout.setHorizontalGroup(PanChatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(PanChatLayout.createSequentialGroup().addContainerGap().addGroup(PanChatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(PanChatLayout.createSequentialGroup().addGroup(PanChatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tfSend, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).addComponent(tbsChan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(lblWelcome)).addContainerGap()));
        PanChatLayout.setVerticalGroup(PanChatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(PanChatLayout.createSequentialGroup().addContainerGap().addComponent(lblWelcome).addGap(18, 18, 18).addGroup(PanChatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollUsers, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE).addGroup(PanChatLayout.createSequentialGroup().addComponent(tbsChan, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(tfSend, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }

    private void btntNewAccActionPerformed(java.awt.event.ActionEvent evt) {
        mainPanel.setVisible(false);
        super.setComponent(PanNewUser);
        clearNewUserFields();
        PanNewUser.setVisible(true);
    }

    private boolean checkFields() {
        if (!bEmail || !bPass || !bNick || !bUname || !bFName || !bLName) {
            return false;
        }
        return true;
    }

    private void btnSubmitActionPerformed(java.awt.event.ActionEvent evt) {
        if (tfUserName.getText().isEmpty() || tfEmail.getText().isEmpty() || tfFName.getText().isEmpty() || tfLName.getText().isEmpty() || tfNickName.getText().isEmpty() || String.valueOf(tfPassword.getPassword()).isEmpty() || String.valueOf(tfPassword2.getPassword()).isEmpty()) javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "All fields are required. ", "Error!", javax.swing.JOptionPane.ERROR_MESSAGE); else if (checkFields()) {
            try {
                sendMessageToServer("NEWA", tfUserName.getText() + "," + tfEmail.getText() + "," + tfFName.getText() + "," + tfLName.getText() + "," + MD5Hash(String.valueOf(tfPassword.getPassword())) + "," + tfNickName.getText());
                int choice = javax.swing.JOptionPane.showConfirmDialog(super.getFrame(), "User " + tfUserName.getText() + " created.\nDo you want to login?", "Success!", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
                if (choice == 0) {
                    try {
                        String reply = sendMessageToServer("LOGN", tfUserName.getText() + "," + MD5Hash(String.valueOf(tfPassword.getPassword())));
                        if (reply.equals("ERR1")) {
                            javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "User already logged in...");
                            PanNewUser.setVisible(false);
                            super.setComponent(mainPanel);
                            mainPanel.setVisible(true);
                        } else if (reply.equals("ERR2")) {
                            javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "Invalid username/password...");
                            PanNewUser.setVisible(false);
                            super.setComponent(mainPanel);
                            mainPanel.setVisible(true);
                        } else if (reply.equals("SUCC")) {
                            PanNewUser.setVisible(false);
                            lblWelcome.setText("Welcome" + tfUserName.getText() + "!");
                            super.setComponent(PanChat);
                            PanChat.setVisible(true);
                            ChatClientChatHandler.connect(tfUserName.getText());
                        } else {
                            System.out.println("Invalid error code...");
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    PanNewUser.setVisible(false);
                    super.setComponent(mainPanel);
                    mainPanel.setVisible(true);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(null, "Fix the errors (red X)", "Error!", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tfPassword2FocusLost(java.awt.event.FocusEvent evt) {
        if (!String.valueOf(tfPassword.getPassword()).equals(String.valueOf(tfPassword2.getPassword()))) {
            lblPassCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            lblPassCheck.setToolTipText("Passwords do not match");
            bPass = false;
        } else if (tfPassword.getPassword().length < 6) {
            lblPassCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            lblPassCheck.setToolTipText("Password must be at least 6 characters long.");
            bPass = false;
        } else {
            lblPassCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
            lblPassCheck.setToolTipText(null);
            bPass = true;
        }
        lblPassCheck.setVisible(true);
    }

    private void tfUserNameFocusLost(java.awt.event.FocusEvent evt) {
        String allowableCharacters = "[A-Za-z0-9]*";
        Pattern regex = Pattern.compile(allowableCharacters);
        if (regex.matcher(tfUserName.getText()).matches()) {
            try {
                if (tfUserName.getText().isEmpty()) {
                    lblUnameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
                    lblUnameCheck.setToolTipText("Username cannot be emtpy");
                    bUname = false;
                } else {
                    if (sendMessageToServer("CHEK", "username:" + tfUserName.getText()).equals("Available")) {
                        lblUnameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
                        lblUnameCheck.setToolTipText(null);
                        bUname = true;
                    } else {
                        lblUnameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
                        lblUnameCheck.setToolTipText("Username already exists");
                        bUname = false;
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            lblUnameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            lblUnameCheck.setToolTipText("Illegal characters inputted");
            bUname = false;
        }
        lblUnameCheck.setVisible(true);
    }

    private void settingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void tfEmailFocusLost(java.awt.event.FocusEvent evt) {
        Pattern regex = Pattern.compile("^(([A-Za-z0-9]+_+)|([A-Za-z0-9]+\\-+)|([A-Za-z0-9]+\\.+)|([A-Za-z0-9]+\\++))*[A-Za-z0-9]+@((\\w+\\-+)|(\\w+\\.))*\\w{1,63}\\.[a-zA-Z]{2,6}$");
        if (regex.matcher(tfEmail.getText()).matches()) {
            lblEmailCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
            lblEmailCheck.setToolTipText(null);
            bEmail = true;
        } else {
            lblEmailCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            lblEmailCheck.setToolTipText("Invalid email address");
            bEmail = false;
        }
        lblEmailCheck.setVisible(true);
    }

    private void tfNickNameFocusLost(java.awt.event.FocusEvent evt) {
        String allowableCharacters = "[A-Za-z0-9]*";
        Pattern regex = Pattern.compile(allowableCharacters);
        if (regex.matcher(tfNickName.getText()).matches()) {
            try {
                if (tfNickName.getText().isEmpty()) {
                    lblNickCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
                    lblNickCheck.setToolTipText("Nickname cannot be empty");
                    bNick = false;
                } else {
                    if (sendMessageToServer("CHEK", "nickname:" + tfNickName.getText()).equals("Available")) {
                        lblNickCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
                        lblNickCheck.setToolTipText(null);
                        bNick = true;
                    } else {
                        lblNickCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
                        lblNickCheck.setToolTipText("Nickname already in use");
                        bNick = false;
                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            lblNickCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            lblNickCheck.setToolTipText("Illegal characters inputted");
            bNick = false;
        }
        lblNickCheck.setVisible(true);
    }

    private void tfFNameFocusLost(java.awt.event.FocusEvent evt) {
        String allowableCharacters = "[A-Za-z0-9]*";
        Pattern regex = Pattern.compile(allowableCharacters);
        if (!regex.matcher(tfFName.getText()).matches() || tfFName.getText().isEmpty()) {
            lblFNameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            if (tfFName.getText().isEmpty()) lblFNameCheck.setToolTipText("First name cannot be empty"); else lblFNameCheck.setToolTipText("Illegal characters inputted");
            bFName = false;
        } else {
            lblFNameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
            lblFNameCheck.setToolTipText(null);
            bFName = true;
        }
        lblFNameCheck.setVisible(true);
    }

    private void tfLNameFocusLost(java.awt.event.FocusEvent evt) {
        String allowableCharacters = "[A-Za-z0-9]*";
        Pattern regex = Pattern.compile(allowableCharacters);
        if (!regex.matcher(tfLName.getText()).matches() || tfLName.getText().isEmpty()) {
            lblLNameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/redX.png")));
            if (tfLName.getText().isEmpty()) lblLNameCheck.setToolTipText("Last name cannot be empty"); else lblLNameCheck.setToolTipText("Illegal characters inputted");
            bLName = false;
        } else {
            lblLNameCheck.setIcon(new ImageIcon(getClass().getClassLoader().getResource("chatclient/resources/icons/greenTick.png")));
            lblLNameCheck.setToolTipText(null);
            bLName = true;
        }
        lblFNameCheck.setVisible(true);
    }

    private void btntLoginActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String reply = sendMessageToServer("LOGN", tfUsernameLogin.getText() + "," + MD5Hash(String.valueOf(tfPassLogin.getPassword())));
            if (reply.equals("ERR1")) {
                javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "User already logged in...");
            } else if (reply.equals("ERR2")) {
                javax.swing.JOptionPane.showMessageDialog(super.getFrame(), "Invalid username/password...");
            } else if (reply.equals("SUCC")) {
                mainPanel.setVisible(false);
                lblWelcome.setText("Welcome " + tfUsernameLogin.getText() + "!");
                super.setComponent(PanChat);
                PanChat.setVisible(true);
                ChatClientChatHandler.connect(tfUsernameLogin.getText());
            } else {
                System.out.println("Invalid error code...");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearNewUserFields() {
        String empty = "";
        tfUserName.setText(empty);
        tfPassword.setText(empty);
        tfPassword2.setText(empty);
        tfNickName.setText(empty);
        tfEmail.setText(empty);
        tfFName.setText(empty);
        tfLName.setText(empty);
        lblUnameCheck.setIcon(null);
        lblPassCheck.setIcon(null);
        lblEmailCheck.setIcon(null);
        lblNickCheck.setIcon(null);
        lblFNameCheck.setIcon(null);
        lblLNameCheck.setIcon(null);
    }

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {
        PanNewUser.setVisible(false);
        super.setComponent(mainPanel);
        mainPanel.setVisible(true);
    }

    private void tfSendActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String toSend = tfSend.getText();
            if (toSend.isEmpty()) return;
            if (toSend.startsWith("//")) {
                toSend = '-' + toSend.substring(1);
                ChatClientChatHandler.send(toSend + '\n');
            } else if (!toSend.startsWith("/")) {
                toSend = '-' + toSend;
                ChatClientChatHandler.send(toSend + '\n');
            } else {
                if (toSend.equalsIgnoreCase("/DISC")) {
                    PanChat.setVisible(false);
                    super.setComponent(mainPanel);
                    mainPanel.setVisible(true);
                    ChatClientChatHandler.disconnect();
                } else if (toSend.toUpperCase().startsWith("/SEND ") || toSend.toUpperCase().startsWith("/NICK ") || toSend.toUpperCase().startsWith("/WHOIS ") || toSend.toUpperCase().startsWith("/MSG ") || toSend.toUpperCase().startsWith("/TIME") || toSend.toUpperCase().startsWith("/TIM0")) {
                    ChatClientChatHandler.send(toSend + '\n');
                } else if (toSend.equalsIgnoreCase("/IGNORE")) {
                    int rows[] = ChatClientView.tblUsers.getSelectedRows();
                    if (rows.length == 0) {
                        addMessage("You have not selected anyone");
                        return;
                    }
                    PrintWriter outputStream = new PrintWriter(new FileWriter("ignore.ini"));
                    for (int i = 0; i < rows.length; i++) {
                        outputStream.println(tblUsers.getValueAt(rows[i], 1));
                    }
                    outputStream.flush();
                    outputStream.close();
                } else if (toSend.equalsIgnoreCase("/UNIGNORE")) {
                    int rows[] = ChatClientView.tblUsers.getSelectedRows();
                    if (rows.length == 0) {
                        addMessage("You have not selected anyone");
                        return;
                    }
                    PrintWriter outputStream = new PrintWriter(new FileWriter("ignoreTemp.ini"));
                    for (int i = 0; i < rows.length; i++) {
                        BufferedReader inputStream = new BufferedReader(new FileReader("ignore.ini"));
                        String entry = null;
                        while ((entry = inputStream.readLine()) != null) if (!entry.equals(tblUsers.getValueAt(rows[i], 1))) outputStream.println(entry);
                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        File deleteAndRename = new File("ignore.ini");
                        deleteAndRename.delete();
                        deleteAndRename = new File("ignoreTemp.ini");
                        deleteAndRename.renameTo(new File("ignore.ini"));
                    }
                } else {
                    addMessage("Invalid Command");
                }
            }
            tfSend.setText("");
        } catch (IOException ex) {
            Logger.getLogger(ChatClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void tfSendKeyTyped(java.awt.event.KeyEvent evt) {
        if (tfSend.getText().length() >= 1024) {
            tfSendActionPerformed(null);
        }
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ChatClientChatHandler.disconnect();
        System.exit(0);
    }

    private javax.swing.JPanel PanChat;

    private javax.swing.JPanel PanNewUser;

    private javax.swing.JButton btnBack;

    private javax.swing.JButton btnSubmit;

    private javax.swing.JButton btntGuest;

    private javax.swing.JButton btntLogin;

    private javax.swing.JButton btntNewAcc;

    private javax.swing.JLabel lblEmail;

    private javax.swing.JLabel lblEmailCheck;

    private javax.swing.JLabel lblExUser;

    private javax.swing.JLabel lblFName;

    private javax.swing.JLabel lblFNameCheck;

    private javax.swing.JLabel lblLName;

    private javax.swing.JLabel lblLNameCheck;

    private javax.swing.JLabel lblNickCheck;

    private javax.swing.JLabel lblNickName;

    private javax.swing.JLabel lblPass;

    private javax.swing.JLabel lblPassCheck;

    private javax.swing.JLabel lblPassLogin;

    private javax.swing.JLabel lblRePass;

    private javax.swing.JLabel lblUnameCheck;

    private javax.swing.JLabel lblUserLogin;

    private javax.swing.JLabel lblUsername;

    private javax.swing.JLabel lblWelcome;

    private javax.swing.JPanel mainPanel;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JProgressBar progressBar;

    private javax.swing.JScrollPane scrollChans;

    private javax.swing.JScrollPane scrollUsers;

    private javax.swing.JMenuItem settingsMenuItem;

    private javax.swing.JLabel statusAnimationLabel;

    private javax.swing.JLabel statusMessageLabel;

    private javax.swing.JPanel statusPanel;

    public static javax.swing.JTable tblUsers;

    private javax.swing.JTabbedPane tbsChan;

    private javax.swing.JTextField tfEmail;

    private javax.swing.JTextField tfFName;

    private javax.swing.JTextField tfLName;

    private javax.swing.JTextField tfNickName;

    private javax.swing.JPasswordField tfPassLogin;

    private javax.swing.JPasswordField tfPassword;

    private javax.swing.JPasswordField tfPassword2;

    private javax.swing.JTextField tfSend;

    private javax.swing.JTextField tfUserName;

    public static javax.swing.JTextField tfUsernameLogin;

    public static javax.swing.JTextPane txtMessages;

    private final Timer messageTimer;

    private final Timer busyIconTimer;

    private final Icon idleIcon;

    private final Icon[] busyIcons = new Icon[15];

    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private JDialog settingsBox;

    private static String hostname;

    private static int port;

    private boolean bEmail = false, bUname = false, bPass = false, bNick = false, bFName = false, bLName = false;

    private static boolean timeStamps = false;
}
