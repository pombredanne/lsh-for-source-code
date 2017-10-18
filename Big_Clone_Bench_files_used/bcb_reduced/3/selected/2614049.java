package fishs.common.update;

import fishs.common.inter.UpdateHandler;
import fishs.common.util.FishsSSLSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.io.*;
import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class Update extends JDialog implements Runnable {

    private static final long serialVersionUID = 1L;

    private JPanel jContentPane = null;

    private JProgressBar jProgressBar = null;

    private JLabel jLabel = null;

    private JButton jButton = null;

    private UpdateHandler handler;

    private String addr;

    private int port;

    private FishsSSLSocket socket;

    private String workingDir;

    /**
	 * @param owner
	 */
    public Update(Frame owner, UpdateHandler handler) {
        super(owner);
        initialize();
        this.handler = handler;
        this.setAlwaysOnTop(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setIP(String addr) {
        this.addr = addr;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void run() {
        if (checkUpdate()) handler.doUpdateAvailable(this);
        this.dispose();
    }

    public boolean checkUpdate() {
        System.out.println("Checking for update...");
        socket = new FishsSSLSocket();
        socket.setIp(addr);
        socket.setPort(port);
        try {
            System.out.println("Trying to connect...");
            socket.connect();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            socket.close();
            return (false);
        }
        try {
            DataInputStream is = new DataInputStream(socket.getInputStream());
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            os.writeInt(0);
            String[] ver = Update.class.getPackage().getImplementationVersion().replaceAll("[0-9].+?:", "").split("\\.");
            System.out.println(ver.length);
            for (int i = 0; i < ver.length; i++) {
                os.writeInt(Integer.parseInt(ver[i]));
                System.out.println(i);
            }
            if (is.readInt() == 0) {
                System.out.println("No Update Available");
                socket.close();
                return (false);
            } else {
                System.out.println("Update Available!");
                socket.close();
                return (true);
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            return (false);
        }
    }

    public void getUpdate() {
        this.jProgressBar.setIndeterminate(true);
        this.jProgressBar.setValue(0);
        this.setLocationRelativeTo(this.getOwner());
        this.setVisible(true);
        socket = new FishsSSLSocket();
        socket.setIp(addr);
        socket.setPort(port);
        String pDir;
        try {
            socket.connect();
        } catch (Exception e) {
            jLabel.setText("Error connecting to update server");
            jButton.setText("OK");
        }
        try {
            DataInputStream is = new DataInputStream(socket.getInputStream());
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            os.writeInt(1);
            String[] ver = Update.class.getPackage().getImplementationVersion().replaceAll("[0-9].+?:", "").split("\\.");
            for (int i = 0; i < ver.length; i++) os.writeInt(Integer.parseInt(ver[i]));
            if (is.readInt() == 0) {
                socket.close();
                return;
            }
            long size = is.readLong();
            System.out.println("Size: " + size);
            jProgressBar.setMaximum((int) (size / (1024)));
            jProgressBar.setIndeterminate(false);
            int part;
            byte[] chunk = new byte[65535];
            long total = 0;
            if ((pDir = System.getProperty("fishs.directory")) == null) pDir = "";
            if (pDir.length() != 0 && !pDir.endsWith("/")) pDir += "/";
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pDir + "new.jar"));
            MessageDigest md = MessageDigest.getInstance("MD5");
            while (total < size) {
                part = is.readInt();
                System.out.println("Got: " + part);
                is.readFully(chunk, 0, part);
                bos.write(chunk, 0, part);
                md.update(chunk, 0, part);
                total += part;
                System.out.println("Total/Max" + (total / 1024) + "/" + (size / 1024));
                jProgressBar.setValue((int) (total / 1024));
            }
            bos.flush();
            bos.close();
            is.readInt();
            int hSize = is.readInt();
            byte[] hash = new byte[hSize];
            is.read(hash, 0, hSize);
            byte[] digest = md.digest();
            if (!md.isEqual(digest, hash)) {
                System.out.println("Hash didn't match!");
                File f = new File(pDir + "new.jar");
                f.delete();
            } else {
                File f = new File(pDir + "FIShSClient.jar");
                if (!f.delete()) System.exit(0);
                f = new File(pDir + "new.jar");
                f.renameTo(new File(pDir + "FIShSClient.jar"));
            }
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
        try {
            String minimize = "";
            String wd = "";
            if (System.getProperty("fishs.minimize") != null || !this.getOwner().isVisible()) minimize = "--minimize";
            if (pDir.length() > 0) wd = "-wd " + pDir;
            String cmd = "java -jar " + pDir + "FIShSClient.jar " + minimize + " " + wd;
            System.out.println(cmd);
            Runtime.getRuntime().exec(cmd);
            System.exit(0);
        } catch (Exception e) {
        }
    }

    private void forceShutdown() {
        this.socket.close();
        this.setVisible(false);
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(302, 130);
        this.setResizable(false);
        this.setTitle("FIShS Update");
        this.setContentPane(getJContentPane());
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.insets = new Insets(10, 5, 10, 0);
            gridBagConstraints2.gridy = 2;
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints1.insets = new Insets(15, 5, 10, 0);
            gridBagConstraints1.gridy = 0;
            jLabel = new JLabel();
            jLabel.setText("Please wait while we update FIShS...");
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.ipadx = 0;
            gridBagConstraints.ipady = 0;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0D;
            gridBagConstraints.insets = new Insets(0, 5, 0, 5);
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weighty = 1.0D;
            gridBagConstraints.gridy = 1;
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(getJProgressBar(), gridBagConstraints);
            jContentPane.add(jLabel, gridBagConstraints1);
            jContentPane.add(getJButton(), gridBagConstraints2);
        }
        return jContentPane;
    }

    /**
	 * This method initializes jProgressBar	
	 * 	
	 * @return javax.swing.JProgressBar	
	 */
    private JProgressBar getJProgressBar() {
        if (jProgressBar == null) {
            jProgressBar = new JProgressBar();
        }
        return jProgressBar;
    }

    /**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setAction(new AbstractAction() {

                public void actionPerformed(ActionEvent arg0) {
                    forceShutdown();
                }
            });
            jButton.setText("Cancel");
        }
        return jButton;
    }
}
