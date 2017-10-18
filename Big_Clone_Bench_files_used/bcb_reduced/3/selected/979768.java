package HTTPBrute;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import HTTPBrute.SettingsDialog;
import HTTPBrute.GlobalSettings;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class HTTPBruteMainFrame extends JFrame {

    public HTTPBruteMainFrame() {
        settings = new GlobalSettings();
        initComponents();
        GlobalSettings.setSetting("passChars", "0123456789abcdefghijklmnopqrstuvwxyz!~+-.,?/}{*'`@#$%^&");
        GlobalSettings.setSetting("upperCaseBox", "true");
        GlobalSettings.setSetting("maxCosecutive", "2");
        GlobalSettings.setSetting("maxSameChar", "4");
        GlobalSettings.setSetting("maxSpecialChar", "1");
        GlobalSettings.setSetting("maxUpperLimit", "2");
        GlobalSettings.setSetting("minPassLen", "2");
        GlobalSettings.setSetting("maxPassLen", "7");
    }

    public void initComponents() {
        getContentPane().setLayout(new MigLayout("fill"));
        JPanel panel = new JPanel(new MigLayout("fill, gapx 20, gapy 1px", "[fill, fill]"));
        panel.add(new JLabel("Username"));
        panel.add(new JLabel("Nonce"), "wrap");
        Username = new JTextField(32);
        Username.setEditable(true);
        panel.add(Username);
        Nonce = new JTextField(32);
        Nonce.setEditable(true);
        panel.add(Nonce, "wrap");
        panel.add(new JLabel("Password"), "gaptop 9");
        panel.add(new JLabel("Qop"), "gaptop 9, wrap");
        Password = new JTextField(32);
        Password.setEditable(true);
        panel.add(Password);
        Qop = new JTextField(32);
        Qop.setEditable(true);
        Qop.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                QopTextFieldKeyReleased(e);
            }
        });
        panel.add(Qop, "wrap");
        panel.add(new JLabel("Realm"), "gaptop 9");
        panel.add(new JLabel("NonceCount"), "gaptop 9, wrap");
        Realm = new JTextField(32);
        Realm.setEditable(true);
        panel.add(Realm);
        NonceCount = new JTextField(32);
        NonceCount.setEnabled(false);
        panel.add(NonceCount, "wrap");
        panel.add(new JLabel("Method"), "gaptop 9");
        panel.add(new JLabel("CNonce"), "gaptop 9, wrap");
        Method = new JTextField(32);
        Method.setEditable(true);
        panel.add(Method);
        Cnonce = new JTextField(32);
        Cnonce.setEnabled(false);
        panel.add(Cnonce, "wrap");
        panel.add(new JLabel("URI"), "gaptop 9");
        panel.add(new JLabel("EntityBody"), "gaptop 9, wrap");
        URI = new JTextField(32);
        URI.setEnabled(true);
        panel.add(URI);
        EntityBody = new JTextField(32);
        EntityBody.setEnabled(false);
        panel.add(EntityBody, "wrap");
        JPanel ResponsePanel = new JPanel(new MigLayout("fillx"));
        ResponsePanel.add(new JLabel("Response"), "gaptop 9, center, wrap");
        Response = new JTextField();
        Response.setEditable(true);
        ResponsePanel.add(Response, "growx");
        panel.add(ResponsePanel, "span, growx, center");
        JPanel LastLine = new JPanel(new MigLayout("fillx"));
        CalcButton = new JButton("Calculate");
        CalcButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                CalcButtonActionPerformed(evt);
            }
        });
        LastLine.add(CalcButton, "align left");
        BruteButton = new JButton("Crack");
        BruteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                BruteButtonActionPerformed(evt);
            }
        });
        LastLine.add(BruteButton, "align center");
        StopButton = new JButton("Stop");
        StopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                StopButtonActionPerformed(evt);
            }
        });
        LastLine.add(StopButton, "align center");
        CloseButton = new JButton("Close");
        CloseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                CloseButtonActionPerformed(evt);
            }
        });
        LastLine.add(CloseButton, "align right, wrap");
        panel.add(LastLine, "growx, spanx, wrap");
        ProgressBar = new JProgressBar();
        ProgressBar.setIndeterminate(true);
        ProgressBar.setVisible(false);
        panel.add(ProgressBar, "span, growx, center, wrap");
        TimeLabel = new JLabel("Time:");
        TimeLabel.setVisible(false);
        panel.add(TimeLabel, "split 2, right");
        elapsedTimeLabel = new JLabel("00:00:00");
        elapsedTimeLabel.setVisible(false);
        panel.add(elapsedTimeLabel, "left");
        cntLabel = new JLabel("Counter:");
        cntLabel.setVisible(false);
        panel.add(cntLabel, "split 2, right");
        passwordCounterLabel = new JLabel();
        passwordCounterLabel.setVisible(false);
        panel.add(passwordCounterLabel, "left");
        getContentPane().add(new JScrollPane(panel), "grow");
        setResizable(true);
        setIcon(this);
        setTitle("HTTPBrute");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        JMenuBar MainMenuBar = new JMenuBar();
        JMenu OptionsMenu = new JMenu("Options");
        MainMenuBar.add(OptionsMenu);
        JMenuItem SettingsMenuItem = new JMenuItem("Settings");
        SettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SettingsMenuItemActionPerformed(evt);
            }
        });
        OptionsMenu.add(SettingsMenuItem);
        MainMenuBar.add(OptionsMenu);
        JMenu HelpMenu = new JMenu("Help");
        JMenuItem AboutMenuItem = new JMenuItem("About");
        AboutMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutMenuActionPerformed(evt);
            }
        });
        HelpMenu.add(AboutMenuItem);
        MainMenuBar.add(HelpMenu);
        setJMenuBar(MainMenuBar);
        pack();
        setMinimumSize(getSize());
    }

    private void SettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        SettingsDialog settingsDialog = new SettingsDialog(this);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }

    private void CalcButtonActionPerformed(ActionEvent evt) {
        StringBuilder finalStr;
        StringBuilder A1, HA1;
        StringBuilder A2, HA2;
        A1 = new StringBuilder(Username.getText());
        A1.append(":").append(Realm.getText()).append(":").append(Password.getText());
        HA1 = new StringBuilder(calculateMD5Digest(A1.toString()));
        if (Qop.getText().equals("auth") == true) {
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1.append(":").append(Nonce.getText()).append(":"));
            finalStr.append(NonceCount.getText()).append(":").append(Cnonce.getText()).append(":");
            finalStr.append(Qop.getText()).append(":").append(HA2);
        } else if (Qop.getText().equals("auth-int") == true) {
            A2 = new StringBuilder(EntityBody.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText()).append(":").append(HA2);
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1);
            finalStr.append(":").append(Nonce.getText());
            finalStr.append(":").append(NonceCount.getText()).append(":");
            finalStr.append(Cnonce.getAlignmentX()).append(":").append(Qop.getText());
            finalStr.append(":").append(HA2);
        } else {
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1);
            finalStr.append(":").append(Nonce.getText()).append(":").append(HA2);
        }
        Response.setText(calculateMD5Digest(finalStr.toString()));
    }

    public boolean CalcForPassword(StringBuilder password) {
        StringBuilder finalStr;
        StringBuilder A1, HA1;
        StringBuilder A2, HA2;
        A1 = new StringBuilder(Username.getText());
        A1.append(":").append(Realm.getText()).append(":").append(password);
        HA1 = new StringBuilder(calculateMD5Digest(A1.toString()));
        if (Qop.getText().equals("auth") == true) {
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1.append(":").append(Nonce.getText()).append(":"));
            finalStr.append(NonceCount.getText()).append(":").append(Cnonce.getText()).append(":");
            finalStr.append(Qop.getText()).append(":").append(HA2);
        } else if (Qop.getText().equals("auth-int") == true) {
            A2 = new StringBuilder(EntityBody.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText()).append(":").append(HA2);
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1);
            finalStr.append(":").append(Nonce.getText());
            finalStr.append(":").append(NonceCount.getText()).append(":");
            finalStr.append(Cnonce.getAlignmentX()).append(":").append(Qop.getText());
            finalStr.append(":").append(HA2);
        } else {
            A2 = new StringBuilder(Method.getText());
            A2.append(":").append(URI.getText());
            HA2 = new StringBuilder(calculateMD5Digest(A2.toString()));
            finalStr = new StringBuilder(HA1);
            finalStr.append(":").append(Nonce.getText()).append(":").append(HA2);
        }
        return calculateMD5Digest(finalStr.toString()).equals(Response.getText());
    }

    private void CloseButtonActionPerformed(ActionEvent evt) {
        ControllerThread.setFoundFlag2True();
        timer.cancel();
        dispose();
    }

    private void StopButtonActionPerformed(ActionEvent evt) {
        ControllerThread.setFoundFlag2True();
        timer.cancel();
    }

    private void BruteButtonActionPerformed(ActionEvent evt) {
        BlockingQueue<Runnable> worksQueue = new ArrayBlockingQueue<Runnable>(5);
        RejectedExecutionHandler executionHandler = new MyRejectedExecutionHandelerImpl();
        executor = new ThreadPoolExecutor(numberThreads, numberThreads, 10, TimeUnit.SECONDS, worksQueue, executionHandler);
        executor.allowCoreThreadTimeOut(true);
        ProgressBar.setVisible(true);
        monitor = new Thread(new ControllerThread(this, executor));
        monitor.setDaemon(true);
        monitor.start();
        startTime = System.currentTimeMillis();
        class UpdateClock extends TimerTask {

            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                elapsedTime = elapsedTime / 1000;
                StringBuilder seconds = new StringBuilder(Integer.toString((int) (elapsedTime % 60)));
                StringBuilder minutes = new StringBuilder(Integer.toString((int) ((elapsedTime % 3600) / 60)));
                StringBuilder hours = new StringBuilder(Integer.toString((int) (elapsedTime / 3600)));
                if (seconds.length() < 2) seconds.insert(0, '0');
                if (minutes.length() < 2) minutes.insert(0, '0');
                if (hours.length() < 2) hours.insert(0, '0');
                elapsedTimeLabel.setText(hours.append(":").append(minutes).append(":").append(seconds).toString());
                updatePassCntLabel();
            }
        }
        timer = new Timer();
        timer.schedule(new UpdateClock(), 1000, 1000);
        passwordCounterLabel.setText("0");
        passwordCounterLabel.setVisible(true);
        TimeLabel.setVisible(true);
        elapsedTimeLabel.setVisible(true);
        cntLabel.setVisible(true);
        return;
    }

    private void QopTextFieldKeyReleased(KeyEvent evt) {
        if (Qop.getText().equals("auth") == true || Qop.getText().equals("auth-int") == true) {
            NonceCount.setEnabled(true);
            Cnonce.setEnabled(true);
            EntityBody.setEnabled(true);
        } else {
            NonceCount.setEnabled(false);
            Cnonce.setEnabled(false);
            EntityBody.setEnabled(false);
        }
    }

    private String calculateMD5Digest(String txt) {
        MessageDigest md;
        String result;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        byte[] Bytes = md.digest(txt.getBytes());
        BigInteger Big = new BigInteger(1, Bytes);
        result = Big.toString(16);
        while (result.length() < 32) {
            result = "0" + result;
        }
        return result;
    }

    public static void setIcon(Window window) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(window.getClass().getResource("/icons/HTTPBrute.png"));
        } catch (IOException exc) {
        }
        window.setIconImage(image);
    }

    public void setPasswordField(String password) {
        Password.setText(password);
    }

    private void AboutMenuActionPerformed(java.awt.event.ActionEvent evt) {
        AboutDialog aboutDialog = new AboutDialog(this);
        aboutDialog.setLocationRelativeTo(this);
        aboutDialog.setVisible(true);
    }

    public String getCharacters() {
        return GlobalSettings.getSetting("passChars");
    }

    public boolean getLowerUpperFlag() {
        String result;
        if ((result = GlobalSettings.getSetting("upperCaseBox")) != null && result.equals("true") == true) {
            return true;
        }
        return false;
    }

    public int getConsecLimit() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("maxCosecutive")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public int getSameCharLimit() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("maxSameChar")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public int getSpecCharLimit() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("maxSpecialChar")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public int getUpperLimit() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("maxUpperLimit")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public int getMinPassLen() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("minPassLen")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public int getMaxPassLen() {
        String result;
        int a = 1;
        if ((result = GlobalSettings.getSetting("maxPassLen")) != null) {
            a = Integer.parseInt(result);
            if (a == 0) {
                a = 1;
            }
        }
        return a;
    }

    public Timer getTimer() {
        return timer;
    }

    public JProgressBar getProgressBar() {
        return ProgressBar;
    }

    public void updatePassCntLabel() {
        passwordCounterLabel.setText(String.format("%,d", passwordCounter));
    }

    public void increasePasswordCounter() {
        passwordCounter++;
    }

    public void setPasswordCounter(long arg) {
        passwordCounter = arg;
    }

    public static String getVersion() {
        return "1.00";
    }

    private JTextField Username;

    private JTextField Password;

    private JTextField Realm;

    private JTextField Method;

    private JTextField URI;

    private JTextField Nonce;

    private JTextField NonceCount;

    private JTextField Qop;

    private JTextField Cnonce;

    private JTextField EntityBody;

    private JTextField Response;

    private JButton CloseButton;

    private JButton StopButton;

    private JButton CalcButton;

    private JButton BruteButton;

    private JProgressBar ProgressBar;

    private JLabel Progress;

    public final int numberThreads = 16;

    private GlobalSettings settings;

    private ThreadPoolExecutor executor;

    private Thread monitor;

    private JLabel elapsedTimeLabel;

    private JLabel TimeLabel;

    private Timer timer;

    private long startTime;

    private JLabel passwordCounterLabel;

    private long passwordCounter;

    private JLabel cntLabel;
}
