package updater;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.util.ArrayDeque;

/**
 * The application's main frame.
 */
public class UpdaterView extends FrameView {

    static final String BASE_URL = "http://myopa.sourceforge.net/";

    private int byteCount = 10000;

    private String md5Hash;

    private String upgradeURL = "";

    private double currentDBVersion = 0;

    private double currentVersion = 0;

    private double latestDBVersion = 0;

    private double latestVersionNum = 0;

    private boolean dbAdmin = false;

    private boolean schemaUpdatesNeeded = false;

    private ArrayDeque<Double> schemaChanges = new ArrayDeque<Double>();

    public UpdaterView(SingleFrameApplication app, String[] args) {
        super(app);
        if (args.length != 3) {
            System.out.println("Args must be passed.");
            System.exit(1);
        } else {
            currentVersion = Double.parseDouble(args[0]);
            currentDBVersion = Double.parseDouble(args[1]);
            dbAdmin = args[2].equals("true") ? true : false;
        }
        initComponents();
        try {
            URL url = new URL(BASE_URL + "version.txt");
            InputStream in = url.openStream();
            BufferedInputStream buffIn = new BufferedInputStream(in);
            String tmp = "";
            int data = buffIn.read();
            while (data != -1) {
                tmp = tmp.concat(Character.toString((char) data));
                data = buffIn.read();
            }
            String[] versionEntries = tmp.split("\n");
            if (versionEntries.length > 0) {
                String[] components = versionEntries[0].split(":");
                if (dbAdmin || Double.parseDouble(components[4]) == currentDBVersion) {
                    byteCount = Integer.parseInt(components[2]);
                    lblCurrent.setText(new Double(currentVersion).toString());
                    lblLatest.setText(components[0]);
                    latestVersionNum = Double.parseDouble(components[0]);
                    lblNotes.setText("<html>" + components[1]);
                    md5Hash = components[3];
                    latestDBVersion = Double.parseDouble(components[4]);
                    upgradeURL = components[5];
                    progressBar.setMaximum(byteCount);
                    if (dbAdmin && Double.parseDouble(components[4]) > currentDBVersion) {
                        schemaUpdatesNeeded = true;
                        schemaChanges.addFirst(latestDBVersion);
                        double lastVersion = latestDBVersion;
                        for (int i = 1; i < versionEntries.length; i++) {
                            components = versionEntries[i].split(":");
                            double nextVers = Double.parseDouble(components[4]);
                            if (nextVers != currentDBVersion) {
                                if (lastVersion != nextVers) {
                                    schemaChanges.addFirst(nextVers);
                                    lastVersion = nextVers;
                                }
                            } else {
                                schemaChanges.addFirst(currentDBVersion);
                                break;
                            }
                        }
                    }
                } else {
                    for (int i = 1; i < versionEntries.length; i++) {
                        components = versionEntries[i].split(":");
                        if (Double.parseDouble(components[4]) == currentDBVersion) {
                            byteCount = Integer.parseInt(components[2]);
                            lblCurrent.setText(new Double(currentVersion).toString());
                            lblLatest.setText(components[0]);
                            latestVersionNum = Double.parseDouble(components[0]);
                            lblNotes.setText("<html>" + components[1]);
                            md5Hash = components[3];
                            latestDBVersion = Double.parseDouble(components[4]);
                            upgradeURL = components[5];
                            progressBar.setMaximum(byteCount);
                            schemaUpdatesNeeded = true;
                            break;
                        }
                    }
                }
            } else {
                throw new InvalidUpdateFileFormatException("File Format is Wrong.");
            }
            if (latestVersionNum == currentVersion) {
                if (schemaUpdatesNeeded) {
                    javax.swing.JOptionPane.showMessageDialog(super.getComponent(), "Updates are available but they require database changes.  Please contact your system administrator to perform the upgrade.", "Myopa Updater", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(super.getComponent(), "No Updates are available - your software is up to date!", "Myopa Updater", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
                System.exit(0);
            } else {
                jButton1.setEnabled(true);
            }
        } catch (InvalidUpdateFileFormatException e) {
        } catch (MalformedURLException e) {
            System.out.println("EXCP " + e);
        } catch (IOException io) {
            System.out.println("IO" + io);
        }
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
            JFrame mainFrame = UpdaterApp.getApplication().getMainFrame();
            aboutBox = new UpdaterAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        UpdaterApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        lblCurrent = new javax.swing.JLabel();
        lblLatest = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lblNotes = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        mainPanel.setName("mainPanel");
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(updater.UpdaterApp.class).getContext().getResourceMap(UpdaterView.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        jButton1.setIcon(resourceMap.getIcon("jButton1.icon"));
        jButton1.setText(resourceMap.getString("jButton1.text"));
        jButton1.setEnabled(false);
        jButton1.setName("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jLabel3.setText(resourceMap.getString("jLabel3.text"));
        jLabel3.setName("jLabel3");
        lblCurrent.setText(resourceMap.getString("lblCurrent.text"));
        lblCurrent.setName("lblCurrent");
        lblLatest.setText(resourceMap.getString("lblAvailable.text"));
        lblLatest.setName("lblAvailable");
        jScrollPane1.setName("jScrollPane1");
        lblNotes.setText(resourceMap.getString("lblNotes.text"));
        lblNotes.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblNotes.setName("lblNotes");
        lblNotes.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jScrollPane1.setViewportView(lblNotes);
        jLabel4.setIcon(resourceMap.getIcon("jLabel4.icon"));
        jLabel4.setText(resourceMap.getString("jLabel4.text"));
        jLabel4.setName("jLabel4");
        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 405, Short.MAX_VALUE)).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addComponent(jLabel4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel2).addComponent(jLabel1))).addGroup(mainPanelLayout.createSequentialGroup().addContainerGap().addComponent(jLabel3))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblLatest).addComponent(lblCurrent).addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))).addContainerGap()));
        mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(mainPanelLayout.createSequentialGroup().addComponent(jLabel4).addGap(25, 25, 25).addComponent(jLabel3)).addGroup(mainPanelLayout.createSequentialGroup().addGap(28, 28, 28).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(lblCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel2).addComponent(lblLatest)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jButton1))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(7, Short.MAX_VALUE)));
        menuBar.setName("menuBar");
        fileMenu.setText(resourceMap.getString("fileMenu.text"));
        fileMenu.setName("fileMenu");
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(updater.UpdaterApp.class).getContext().getActionMap(UpdaterView.class, this);
        exitMenuItem.setAction(actionMap.get("quit"));
        exitMenuItem.setName("exitMenuItem");
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
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
        statusPanelLayout.setHorizontalGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE).addGroup(statusPanelLayout.createSequentialGroup().addContainerGap().addComponent(statusMessageLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 243, Short.MAX_VALUE).addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(statusAnimationLabel).addContainerGap()));
        statusPanelLayout.setVerticalGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(statusPanelLayout.createSequentialGroup().addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(statusMessageLabel).addComponent(statusAnimationLabel).addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(3, 3, 3)));
        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            URL url = new URL(upgradeURL);
            InputStream in = url.openStream();
            BufferedInputStream buffIn = new BufferedInputStream(in);
            FileOutputStream out = new FileOutputStream("");
            String bytes = "";
            int data = buffIn.read();
            int downloadedByteCount = 1;
            while (data != -1) {
                out.write(data);
                bytes.concat(Character.toString((char) data));
                buffIn.read();
                downloadedByteCount++;
                updateProgressBar(downloadedByteCount);
            }
            out.close();
            buffIn.close();
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(bytes.getBytes());
            String hash = m.digest().toString();
            if (hash.length() == 31) {
                hash = "0" + hash;
            }
            if (!hash.equalsIgnoreCase(md5Hash)) {
            }
        } catch (MalformedURLException e) {
        } catch (IOException io) {
        } catch (NoSuchAlgorithmException a) {
        }
    }

    private void updateProgressBar(int downloadedByteCount) {
        progressBar.setValue(downloadedByteCount);
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lblCurrent;

    private javax.swing.JLabel lblLatest;

    private javax.swing.JLabel lblNotes;

    private javax.swing.JPanel mainPanel;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JProgressBar progressBar;

    private javax.swing.JLabel statusAnimationLabel;

    private javax.swing.JLabel statusMessageLabel;

    private javax.swing.JPanel statusPanel;

    private final Timer messageTimer;

    private final Timer busyIconTimer;

    private final Icon idleIcon;

    private final Icon[] busyIcons = new Icon[15];

    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
