import java.io.*;
import java.util.*;
import java.security.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class DataGather {

    public static void main(String[] args) {
        JFrame frame = new DataGatherFrame();
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.show();
    }
}

class DataGatherFrame extends JFrame {

    private static final int WIDTH = 600;

    private static final int HEIGHT = 350;

    private static final String INTEGRITY_CHECKER = "integritycheck.pl";

    public static final String CONFIG_FILE = "config.cfg";

    private static final String ARCHIVE_DIR = "./programs/";

    public static final String TMP_DIR = "tmp";

    public static String VTMP_DIR = "tmp";

    public static String ARCH_DIR = TMP_DIR + "/" + ARCHIVE_DIR;

    private JCheckBox[] cb;

    private String[] cbString;

    private String s = "";

    private String config_content = "";

    private JTextField fileNameEntry, fileLocationEntry;

    private JLabel fileNameLabel, fileLocationLabel;

    private JTextArea textEcho;

    private JScrollPane textArea;

    private TitledBorder title;

    public DataGatherFrame() {
        setTitle("sfick DataGather v1.0: Gathering Verification Data");
        setSize(WIDTH, HEIGHT);
        final MakeDirectory temp = new MakeDirectory();
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                makeConfigFile();
                temp.finish();
            }
        };
        ActionListener listen = new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                getAddedFiles();
            }
        };
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        cbString = new String[20];
        cbString[0] = "ls";
        cbString[1] = "cp";
        cbString[2] = "rm";
        cbString[3] = "mv";
        cbString[4] = "netstat";
        cbString[5] = "mount";
        cbString[6] = "ps";
        cbString[7] = "chmod";
        cbString[8] = "chown";
        cbString[9] = "su";
        cbString[10] = "grep";
        cbString[11] = "sh";
        cbString[12] = "kill";
        cbString[13] = "login";
        cbString[14] = "passwd";
        cbString[15] = "patch";
        cbString[16] = "less";
        cbString[17] = "find";
        cbString[18] = "who";
        cbString[19] = "ifconfig";
        cb = new JCheckBox[20];
        for (int i = 0; i < cb.length; i++) {
            cb[i] = new JCheckBox(cbString[i], true);
        }
        File tmp = null;
        for (int i = 0; i <= 13; i++) {
            tmp = new File("/bin/" + cbString[i]);
            if ((!tmp.exists()) || (!tmp.canRead())) {
                cb[i].setSelected(false);
                cb[i].setEnabled(false);
            }
        }
        for (int i = 14; i <= 18; i++) {
            tmp = new File("/usr/bin/" + cbString[i]);
            if ((!tmp.exists()) || (!tmp.canRead())) {
                cb[i].setSelected(false);
                cb[i].setEnabled(false);
            }
        }
        tmp = new File("/sbin/" + cbString[19]);
        if ((!tmp.exists()) || (!tmp.canRead())) {
            cb[19].setSelected(false);
            cb[19].setEnabled(false);
        }
        title = BorderFactory.createTitledBorder("Recommended Files  ( greyed files do not exist or lack sufficient permissions )");
        JPanel checkBoxPanel = new JPanel(new GridLayout(5, 4));
        checkBoxPanel.setBorder(title);
        for (int i = 0; i < cb.length; i++) checkBoxPanel.add(cb[i]);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(checkBoxPanel);
        JPanel fileListPanel = new JPanel();
        fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
        title = BorderFactory.createTitledBorder("Files Added");
        fileListPanel.setBorder(title);
        textEcho = new JTextArea(10, 40);
        textEcho.setLineWrap(true);
        textArea = new JScrollPane(textEcho);
        fileListPanel.add(textArea);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(fileListPanel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(Box.createVerticalGlue());
        JPanel addFilesPanel = new JPanel();
        addFilesPanel.setLayout(new BoxLayout(addFilesPanel, BoxLayout.X_AXIS));
        title = BorderFactory.createTitledBorder("Add Files");
        addFilesPanel.setBorder(title);
        fileLocationEntry = new JTextField(64);
        fileLocationEntry.setMaximumSize(fileLocationEntry.getPreferredSize());
        fileLocationLabel = new JLabel("Absolute Path to File: ");
        JButton addFile = new JButton("Add");
        addFile.addActionListener(listen);
        addFilesPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        addFilesPanel.add(fileLocationLabel);
        addFilesPanel.add(fileLocationEntry);
        addFilesPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        addFilesPanel.add(addFile);
        addFilesPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        textPanel.add(addFilesPanel);
        JButton finish = new JButton("Finish");
        finish.addActionListener(listener);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(finish);
        Container contentPane = getContentPane();
        contentPane.add(checkBoxPanel, BorderLayout.NORTH);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        contentPane.add(textPanel, BorderLayout.CENTER);
    }

    private void makeConfigFile() {
        int i = 0;
        try {
            VTMP_DIR = TMP_DIR + "/";
            File tempDir = new File(VTMP_DIR);
            while (tempDir.exists()) {
                ++i;
                VTMP_DIR = TMP_DIR + i + "/";
                tempDir = new File(VTMP_DIR);
            }
            ARCH_DIR = VTMP_DIR + "/" + ARCHIVE_DIR;
            tempDir.mkdir();
            File archDir = new File(ARCH_DIR);
            archDir.mkdir();
            PrintWriter conf = new PrintWriter(new FileWriter(VTMP_DIR + CONFIG_FILE));
            for (int j = 0; j < 14; j++) {
                if (cb[j].isSelected()) conf.println(cbString[j] + ":/bin/" + cbString[j] + ":" + ARCHIVE_DIR + cbString[j]);
            }
            for (int j = 14; j < cb.length - 1; j++) {
                if (cb[j].isSelected()) conf.println(cbString[j] + ":/usr/bin/" + cbString[j] + ":" + ARCHIVE_DIR + cbString[j]);
            }
            if (cb[19].isSelected()) conf.println(cbString[19] + ":/sbin/" + cbString[19] + ":" + ARCHIVE_DIR + cbString[19]);
            if (config_content.length() > 0) conf.print(config_content);
            conf.flush();
            conf.close();
            BufferedReader fin = new BufferedReader(new FileReader(INTEGRITY_CHECKER));
            PrintWriter fout = new PrintWriter(new FileWriter(VTMP_DIR + INTEGRITY_CHECKER));
            String line;
            while ((line = fin.readLine()) != null) {
                fout.println(line);
            }
            fin.close();
            fout.close();
        } catch (IOException e) {
            System.out.println("IOException caught:\n");
            e.printStackTrace();
        }
    }

    private void getAddedFiles() {
        String filePath = fileLocationEntry.getText();
        int x = filePath.lastIndexOf('/');
        String fileTitle = filePath.substring(x + 1, filePath.length());
        File tmp = new File(filePath);
        if ((!tmp.exists()) || (!tmp.canRead())) {
            MessagePrompt e = new MessagePrompt("Error", "File does not exist or the user lacks sufficient permissions.");
            e.show();
        } else {
            config_content = config_content + fileTitle + ":" + filePath + ":" + ARCHIVE_DIR + fileTitle + "\n";
            s = s + filePath + "\n";
            fileLocationEntry.setText("");
            textEcho.setText(s);
        }
    }
}

class MessagePrompt extends JFrame {

    private static final int WIDTH = 415;

    private static final int HEIGHT = 120;

    private JButton closeButton;

    MessagePrompt(String messageType, String message) {
        setTitle(messageType);
        setSize(WIDTH, HEIGHT);
        ActionListener listenClose = new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        };
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.X_AXIS));
        TitledBorder title = BorderFactory.createTitledBorder("Message");
        msgPanel.setBorder(title);
        JLabel msgLabel = new JLabel(message);
        msgPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        msgPanel.add(msgLabel);
        msgPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        closeButton = new JButton("Close");
        closeButton.addActionListener(listenClose);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        Container contentPane = getContentPane();
        contentPane.add(msgPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }
}

class MakeDirectory {

    private static final String CONFIG_FILE = DataGatherFrame.CONFIG_FILE;

    private static final String TMP_DIR = DataGatherFrame.TMP_DIR;

    public static String VTMP_DIR = DataGatherFrame.VTMP_DIR;

    private static String ARCH_DIR = DataGatherFrame.ARCH_DIR;

    private MessageDigest hashSum;

    public MakeDirectory() {
    }

    public void finish() {
        String checksum = "";
        String tmp = "";
        int imp;
        ARCH_DIR = DataGatherFrame.ARCH_DIR;
        String VTMP_DIR = DataGatherFrame.VTMP_DIR;
        try {
            BufferedReader in = new BufferedReader(new FileReader(VTMP_DIR + CONFIG_FILE));
            String line;
            String name;
            String path;
            String[] configField = new String[3];
            int x;
            int y;
            byte b;
            byte[] sum;
            try {
                hashSum = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("NoSuchAlgorithmException caught... nothing done about it.");
            }
            while ((line = in.readLine()) != null) {
                if (!(line.equals(""))) {
                    x = line.indexOf(':');
                    configField[0] = line.substring(0, x);
                    y = line.lastIndexOf(':');
                    configField[1] = line.substring(x + 1, y);
                    configField[2] = line.substring(y + 1, line.length());
                    for (int j = 0; j < configField.length; j++) System.out.println("configField[" + j + "]: " + configField[j]);
                    File nameFile = new File(ARCH_DIR + configField[0]);
                    nameFile.mkdir();
                    File f = new File(configField[1]);
                    FileInputStream fin = new FileInputStream(f);
                    DataInputStream din = new DataInputStream(fin);
                    File o = new File(ARCH_DIR + configField[0] + "/" + configField[0]);
                    FileOutputStream fout = new FileOutputStream(o);
                    DataOutputStream dout = new DataOutputStream(fout);
                    hashSum.reset();
                    try {
                        while (true) {
                            b = din.readByte();
                            dout.write(b);
                            hashSum.update(b);
                        }
                    } catch (EOFException e) {
                        fout.close();
                        fin.close();
                        dout.close();
                        din.close();
                        sum = hashSum.digest();
                        PrintWriter hashFile = new PrintWriter(new FileWriter(ARCH_DIR + configField[0] + "/" + configField[0] + ".digest"));
                        System.out.println("Hash: ");
                        for (int i = 0; i < sum.length; i++) {
                            tmp = Byte.toString(sum[i]);
                            imp = Byte.decode(tmp).intValue();
                            if (imp < 0) {
                                tmp = Integer.toHexString(imp);
                                String tmp1 = tmp.substring(6, tmp.length());
                                tmp = tmp1;
                            } else {
                                tmp = Integer.toHexString(imp);
                                if (tmp.length() == 1) tmp = "0" + tmp;
                            }
                            checksum += tmp;
                        }
                        hashFile.println(checksum);
                        System.out.println(checksum);
                        hashFile.close();
                        checksum = "";
                    } catch (IOException e) {
                        fout.close();
                        fin.close();
                        dout.close();
                        din.close();
                        System.out.println("An IOException occurred:\n");
                        e.printStackTrace();
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("An IOException occurred:\n");
            e.printStackTrace();
        }
        MessagePrompt p = new MessagePrompt("Operation Complete", "Files in \"" + VTMP_DIR + "\" are ready for archiving.");
        p.show();
    }
}
