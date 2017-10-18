package com.ketralnis.isUpApp;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFileChooser;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.sun.java.SpringUtilities;

/**
	An isUpFactory that generates an isUp that checks to see that a file is unchanged.
*/
public class isUpFactoryChkSum extends isUpFactory {

    private final String myName = "Checksum";

    public JPanel configPane() {
        return new configPaneChkSum();
    }

    public JPanel configPane(isUp in) {
        return new configPaneChkSum((isUpChkSum) (in));
    }

    public isUp isUpFromConfigPane(JPanel in) {
        configPaneChkSum temp = (configPaneChkSum) (in);
        return retNew(temp.getFile());
    }

    public isUp retNew(Object in) {
        return new isUpChkSum((File) (in));
    }

    private class configPaneChkSum extends JPanel implements ActionListener {

        private JTextField textFile = null;

        private JTextField textCurrentSum = null;

        public configPaneChkSum() {
            init();
        }

        public configPaneChkSum(isUpChkSum in) {
            init();
            textFile.setText(in.getFile().toString());
            textCurrentSum.setText(byteaToString(in.getChkSum()));
        }

        public void init() {
            setLayout(new SpringLayout());
            JLabel labelFile = new JLabel("File name", JLabel.TRAILING);
            {
            }
            add(labelFile);
            textFile = new JTextField(30);
            {
                textFile.setText("");
                labelFile.setLabelFor(textFile);
            }
            add(textFile);
            JButton buttonBrowse = new JButton("Browse...");
            {
                String temp = "Browses for the file to monitor";
                buttonBrowse.setMnemonic(KeyEvent.VK_B);
                buttonBrowse.getAccessibleContext().setAccessibleDescription(temp);
                buttonBrowse.setToolTipText(temp);
                buttonBrowse.setActionCommand("browse");
                buttonBrowse.addActionListener(this);
            }
            add(buttonBrowse);
            JLabel labelCurrentSum = new JLabel("Sum");
            {
            }
            add(labelCurrentSum);
            textCurrentSum = new JTextField(16);
            {
                textCurrentSum.setEditable(false);
                textCurrentSum.setText("");
            }
            add(textCurrentSum);
            JButton buttonCalcSum = new JButton("Calculate");
            {
                String temp = "Calculates and displays the sum of the currently selected file";
                buttonCalcSum.setMnemonic(KeyEvent.VK_C);
                buttonCalcSum.getAccessibleContext().setAccessibleDescription(temp);
                buttonCalcSum.setToolTipText(temp);
                buttonCalcSum.setActionCommand("calcsum");
                buttonCalcSum.addActionListener(this);
            }
            add(buttonCalcSum);
            SpringUtilities.makeCompactGrid(this, 2, 3, 6, 6, 6, 6);
        }

        public void actionPerformed(ActionEvent e) {
            if ("browse".equals(e.getActionCommand())) {
                final JFileChooser fc = new JFileChooser(getFile());
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    textFile.setText(fc.getSelectedFile().toString());
                    actionPerformed(new ActionEvent(this, 0, "calcsum"));
                }
            } else if ("calcsum".equals(e.getActionCommand())) {
                new Thread() {

                    public void run() {
                        textCurrentSum.setText("Calculating MD5 sum...");
                        textCurrentSum.validate();
                        try {
                            textCurrentSum.setText(createChecksumString(getFile()));
                        } catch (IOException error) {
                            textCurrentSum.setText(error.toString());
                        } catch (NullPointerException error) {
                        }
                    }
                }.start();
            }
        }

        public File getFile() {
            return new File(textFile.getText());
        }
    }

    private class isUpChkSum extends isUp {

        private File m_file = null;

        private byte[] first_sum = null;

        protected isUpChkSum() {
        }

        public Object clone() {
            isUpChkSum ret = new isUpChkSum(new File(m_file.toString()));
            return ret;
        }

        /**
			Constructor
			@param in		The file to check
		*/
        public isUpChkSum(File in) {
            m_file = in;
            try {
                first_sum = createChecksum(m_file);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        public File getFile() {
            return m_file;
        }

        public byte[] getChkSum() {
            return first_sum;
        }

        public String getName() {
            return myName;
        }

        public boolean query() {
            try {
                return (new String(createChecksum(m_file)).equals(new String(first_sum)));
            } catch (IOException e) {
                System.err.println(e);
                return false;
            }
        }
    }

    private static byte[] createChecksum(File filename) throws IOException {
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(filename));
        byte[] buffer = new byte[1024];
        MessageDigest complete = null;
        try {
            complete = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e);
        }
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static String createChecksumString(File filename) throws IOException {
        byte[] digest = createChecksum(filename);
        return byteaToString(digest);
    }

    private static String byteaToString(byte[] digest) {
        String s = "";
        for (int i = 0; i < digest.length; i++) {
            String temp = "";
            temp = Integer.toHexString(digest[i] & 0xFF);
            s += (temp.length() == 1) ? "0" + temp : temp;
        }
        return s;
    }

    /**
		Checksum
	*/
    public String getName() {
        return myName;
    }

    /**
		An isUp that checks to see whether the MD5 sum of a file has changed (will return FALSE if file does not exist)
	*/
    public String getLongDescription() {
        return "An isUp that checks to see whether the MD5 sum of a file has changed (will return FALSE if file does not exist)";
    }
}
