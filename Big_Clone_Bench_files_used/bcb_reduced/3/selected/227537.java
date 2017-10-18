package org.gerhardb.jibs.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.gerhardb.jibs.Jibs;
import org.gerhardb.lib.swing.JFileChooserExtra;
import org.gerhardb.lib.swing.JPanelRows;
import org.gerhardb.lib.swing.SwingUtils;
import org.gerhardb.lib.util.Icons;

/**
 * REMOVED BECAUSE THE LIBRARY IN QUESTION HAS OPERATING SYSTEM DEPENDENT FILES...
 * Added a hash check sum checker because I could not easily find one and I hate looking.
	FireFox plug-in is in test status and I didn't feel like registering.
	Got Java code from http://www.twmacinta.com/myjava/fast_md5.php, but did not use
	the faster way there coded, just the old fashioned way shown.  Did use the hex converter...

 */
public class CheckSumChecker extends JFrame {

    private static final String LAST_FILE = "LastFile";

    private static final String ALGORITHM = "Algorithm";

    private static final Preferences clsPrefs = Preferences.userRoot().node("/org/gerhardb/jibs/util/CheckSumChecker");

    JTextField myFileName = new JTextField(60);

    JTextField myDownloadedHash = new JTextField(60);

    JLabel myComputedHash = new JLabel("     ");

    JLabel myResults = new JLabel("     ");

    JComboBox<String> myAlgorithm = new JComboBox<String>();

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public CheckSumChecker() {
        super("Check Sum Checker");
        layoutComponents();
        this.myFileName.setText(clsPrefs.get(LAST_FILE, null));
        this.setIconImage(Icons.getIcon(Icons.JIBS_16).getImage());
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                CheckSumChecker.this.pack();
                CheckSumChecker.this.setVisible(true);
                SwingUtils.centerOnScreen(CheckSumChecker.this);
            }
        });
    }

    private void layoutComponents() {
        this.setSize(new Dimension(600, 600));
        this.myAlgorithm.addItem("MD2");
        this.myAlgorithm.addItem("MD5");
        this.myAlgorithm.addItem("SHA-1");
        this.myAlgorithm.addItem("SHA-256");
        this.myAlgorithm.addItem("SHA-384");
        this.myAlgorithm.addItem("SHA-512");
        this.myAlgorithm.setEditable(false);
        this.myAlgorithm.setSelectedItem(clsPrefs.get(ALGORITHM, "MD2"));
        JButton goBtn = new JButton("Compute Hash");
        goBtn.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                go();
            }
        });
        JButton fileBtn = new JButton("...");
        fileBtn.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFile();
            }
        });
        JButton compareBtn = new JButton("Compare");
        compareBtn.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkHashes();
            }
        });
        JPanelRows topPanel = new JPanelRows(FlowLayout.CENTER);
        JPanel aRow = topPanel.topRow();
        aRow.add(new JLabel("File: "));
        aRow.add(this.myFileName);
        aRow.add(fileBtn);
        aRow = topPanel.nextRow();
        aRow.add(this.myAlgorithm);
        aRow.add(goBtn);
        aRow = topPanel.nextRow();
        aRow.add(this.myComputedHash);
        aRow = topPanel.nextRow();
        aRow.add(new JLabel("Hash to compare: "));
        aRow.add(this.myDownloadedHash);
        aRow = topPanel.nextRow();
        aRow.add(compareBtn);
        aRow = topPanel.nextRow();
        aRow.add(this.myResults);
        this.setContentPane(topPanel);
    }

    void selectFile() {
        JFileChooserExtra chooser = new JFileChooserExtra(clsPrefs.get(LAST_FILE, null));
        chooser.setDialogTitle(Jibs.getString("CheckSumChecker.15"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSaveName("CheckSumChecker", "DirectoryTreeList.txt");
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File picked = chooser.getSelectedFile();
            if (picked != null) {
                String fileName = picked.toString();
                if (!fileName.contains(".")) {
                    fileName = fileName + ".txt";
                }
                this.myFileName.setText(fileName);
                try {
                    clsPrefs.put(LAST_FILE, picked.toString());
                    clsPrefs.flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    void go() {
        String algorithm = this.myAlgorithm.getSelectedItem().toString();
        clsPrefs.put(ALGORITHM, this.myAlgorithm.getSelectedItem().toString());
        String fileName = this.myFileName.getText();
        File fileToHash = new File(fileName);
        if (!fileToHash.exists()) {
            JOptionPane.showMessageDialog(this, "Could not access file", "Problem", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.myResults.setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            int totalBytesRead = 0;
            byte[] input = new byte[1000000];
            InputStream is = new FileInputStream(fileToHash);
            int bytesRead = is.read(input);
            if (bytesRead < input.length) {
                totalBytesRead = +bytesRead;
            } else {
                totalBytesRead = totalBytesRead + bytesRead;
            }
            while (bytesRead == input.length) {
                md.update(input);
                bytesRead = is.read(input);
                totalBytesRead = totalBytesRead + bytesRead;
            }
            byte[] finalInput = new byte[bytesRead];
            for (int i = 0; i < bytesRead; i++) {
                finalInput[i] = input[i];
            }
            md.update(finalInput);
            is.close();
            System.out.println("fileToHash.length(): " + fileToHash.length());
            System.out.println("totalBytesRead:      " + totalBytesRead);
            if (fileToHash.length() != totalBytesRead) {
                JOptionPane.showMessageDialog(this, "Could not read entire file", "Problem Encountered", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] theHashAsBytes = md.digest();
            String hexHash = asHex(theHashAsBytes);
            this.myComputedHash.setText(hexHash);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Problem Encountered", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (this.myDownloadedHash.getText().trim().length() > 0) {
            checkHashes();
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    void checkHashes() {
        String computedHash = this.myComputedHash.getText().trim();
        this.myResults.setOpaque(true);
        if (computedHash.length() == 0) {
            go();
            computedHash = this.myComputedHash.getText().trim();
            if (computedHash.length() == 0) {
                this.myResults.setText("You need to compute a hash on a file first.");
                this.myResults.setBackground(Color.YELLOW);
                return;
            }
        }
        String downloadedHash = this.myDownloadedHash.getText();
        downloadedHash = downloadedHash.trim();
        if (downloadedHash.length() == 0) {
            this.myResults.setText("You need to enter a hash to compare against.");
            this.myResults.setBackground(Color.YELLOW);
            return;
        }
        if (computedHash.equals(downloadedHash)) {
            this.myResults.setText("The hashes match.");
            this.myResults.setBackground(Color.GREEN);
        } else {
            this.myResults.setText("The hashes do not match.");
            this.myResults.setBackground(Color.RED);
        }
    }

    /**
	* Turns array of bytes into string representing each byte as
	* unsigned hex number.
	* 
	* Originally from: http://www.twmacinta.com/myjava/fast_md5.php
	* 
	* @param hash Array of bytes to convert to hex-string
	* @return Generated hex string
	*/
    public static String asHex(byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

    public static void main(String[] args) {
        new CheckSumChecker();
    }
}
