import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.beans.*;

public class JoinSplit {

    public static void main(String Args[]) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ProgramWindow app = new ProgramWindow();
                app.setVisible(true);
            }
        });
    }
}

class ShowHashDialog extends JDialog implements ActionListener {

    private ProgramWindow owner;

    private JLabel msg;

    private JTextField res;

    private JButton but;

    public ShowHashDialog(String title, String message, String result) {
        setTitle(title);
        msg = new JLabel(message);
        res = new JTextField(result);
        res.setEditable(false);
        but = new JButton("OK");
        but.addActionListener(this);
        setSize(400, 100);
        setLayout(new FlowLayout());
        Container c = getContentPane();
        c.add(msg);
        c.add(res);
        c.add(but);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == but) dispose();
    }
}

class ProgramWindow extends JFrame implements ActionListener, PropertyChangeListener {

    private JMenuBar barMenu;

    private JMenu menuFile, menuHash;

    private JMenuItem itemSplit, itemJoin, itemExit, itemMD5, itemSHA1;

    private JProgressBar progress;

    class ComputeHash extends SwingWorker<String, Void> {

        private boolean succeed;

        private File f;

        private String h;

        ComputeHash(File file, String hash) {
            f = file;
            h = hash;
            succeed = false;
        }

        public String doInBackground() {
            int k;
            Long l, s;
            FileInputStream fins;
            MessageDigest md;
            try {
                byte[] buf = new byte[128 * 1024];
                setProgress(0);
                md = MessageDigest.getInstance(h);
                fins = new FileInputStream(f);
                s = f.length();
                l = 0L;
                do {
                    k = fins.read(buf);
                    if (k > 0) {
                        md.update(buf, 0, k);
                        l += k;
                        setProgress((int) (l * 100 / s));
                    }
                } while (k > 0);
                fins.close();
                h = arrayBytetohexString(md.digest());
                succeed = true;
            } catch (Exception e) {
                h = e.getMessage();
            }
            return h;
        }

        public void done() {
            quitProgress();
            setCursor(null);
            if (succeed) new ShowHashDialog("JoinSplit", "File signature is:", h); else JOptionPane.showMessageDialog(null, "Error while computing file signature\n" + h, "JoinSplit", JOptionPane.ERROR_MESSAGE);
        }
    }

    public ProgramWindow() {
        barMenu = new JMenuBar();
        menuFile = new JMenu("Files");
        itemSplit = new JMenuItem("Split file");
        itemJoin = new JMenuItem("Join files");
        menuHash = new JMenu("Compute file signature");
        itemMD5 = new JMenuItem("MD5 hash");
        itemSHA1 = new JMenuItem("SHA1 hash");
        itemExit = new JMenuItem("Quit");
        itemSplit.addActionListener(this);
        itemJoin.addActionListener(this);
        itemMD5.addActionListener(this);
        itemSHA1.addActionListener(this);
        itemExit.addActionListener(this);
        menuFile.add(itemJoin);
        menuFile.add(itemSplit);
        menuFile.add(menuHash);
        menuHash.add(itemMD5);
        menuHash.add(itemSHA1);
        menuFile.add(itemExit);
        barMenu.add(menuFile);
        setJMenuBar(barMenu);
        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setVisible(false);
        getContentPane().add(progress);
        setTitle("Join Split");
        setSize(500, 150);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void showProgress(String actiontitle) {
        setTitle(actiontitle);
        menuFile.setEnabled(false);
        progress.setValue(0);
        progress.setVisible(true);
    }

    private void quitProgress() {
        setTitle("Join Split");
        menuFile.setEnabled(true);
        progress.setVisible(false);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) progress.setValue((Integer) evt.getNewValue());
    }

    public void actionPerformed(ActionEvent aev) {
        Object evsrc = aev.getSource();
        JFileChooser fc;
        if (evsrc == itemExit) {
            System.exit(0);
        } else if (evsrc == itemSplit) {
            fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                JDialog dialog = new SplitDialog(this, (fc.getSelectedFile()).getPath());
                dialog.setVisible(true);
                dialog.dispose();
            }
        } else if (evsrc == itemJoin) {
            fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                JDialog dialog = new JoinDialog(this, fc.getSelectedFiles());
                dialog.setVisible(true);
                dialog.dispose();
            }
        } else if ((evsrc == itemMD5) || (evsrc == itemSHA1)) {
            int k, l;
            fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                ComputeHash task;
                showProgress("Computing hash signature...");
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                task = new ComputeHash(fc.getSelectedFile(), (evsrc == itemMD5) ? ("MD5") : ("SHA"));
                task.addPropertyChangeListener(this);
                task.execute();
            }
        }
    }

    public static String arrayBytetohexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            if ((array[i] >= 0) && (array[i] < 16)) hexString.append("0");
            hexString.append(Integer.toHexString(array[i] & 0xFF));
        }
        return hexString.toString();
    }

    class SplitDialog extends JDialog implements ActionListener {

        private ProgramWindow owner;

        private JTextField filepath, inputPieceMaxSize;

        private JComboBox sizeMultiple;

        private JButton butConfirm, butCancel;

        private OptionalFile dialMD5, dialSHA1;

        class SplitFile extends SwingWorker<Integer, Void> {

            private File f;

            private String fileoutputname;

            private int inputbuffersize, quo, rem, errcode;

            private long limitSize;

            private boolean computeMD5, computeSHA1;

            private String errmessage;

            SplitFile(File inFile, String outFilename, long maxsizepart, boolean doMD5, boolean doSHA1) {
                f = inFile;
                fileoutputname = outFilename;
                limitSize = maxsizepart;
                computeMD5 = doMD5;
                computeSHA1 = doSHA1;
                errcode = 0;
                errmessage = null;
            }

            public Integer doInBackground() {
                int i, k;
                long j, numPieces;
                MessageDigest md5, mdSHA;
                FileInputStream fins;
                FileOutputStream fout;
                if (limitSize > 1024000) {
                    inputbuffersize = 1024000;
                    quo = (int) (limitSize / 1024000);
                    rem = (int) (limitSize % 1024000);
                } else {
                    inputbuffersize = (int) limitSize;
                    quo = 1;
                    rem = 0;
                }
                byte[] bufRaw = new byte[inputbuffersize];
                md5 = mdSHA = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                    mdSHA = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException e) {
                }
                try {
                    fins = new FileInputStream(f);
                    j = f.length();
                    numPieces = j / limitSize;
                    if (j > limitSize * numPieces) ++numPieces;
                    fileoutputname = fileoutputname + ".part" + numPieces + "_" + numPieces;
                    char[] ofname = fileoutputname.toCharArray();
                    setZeroFilename(ofname);
                    setProgress(0);
                    for (i = 1; i < numPieces; ++i) {
                        incrementFilename(ofname);
                        System.out.println(new String(ofname));
                        fout = new FileOutputStream(new String(ofname));
                        for (k = 0; k < quo; ++k) {
                            fins.read(bufRaw);
                            if (computeMD5) md5.update(bufRaw);
                            if (computeSHA1) mdSHA.update(bufRaw);
                            fout.write(bufRaw);
                        }
                        fins.read(bufRaw, 0, rem);
                        if (computeMD5) md5.update(bufRaw, 0, rem);
                        if (computeSHA1) mdSHA.update(bufRaw, 0, rem);
                        fout.write(bufRaw, 0, rem);
                        fout.close();
                        setProgress((int) (i * 100 / numPieces));
                    }
                    incrementFilename(ofname);
                    System.out.println(new String(ofname));
                    fout = new FileOutputStream(new String(ofname));
                    do {
                        k = fins.read(bufRaw);
                        if (k > 0) {
                            fout.write(bufRaw, 0, k);
                            if (computeMD5) md5.update(bufRaw, 0, k);
                            if (computeSHA1) mdSHA.update(bufRaw, 0, k);
                        }
                    } while (k > 0);
                    fout.close();
                    setProgress(100);
                    fins.close();
                } catch (FileNotFoundException e) {
                    errcode = 1;
                    errmessage = e.getMessage();
                } catch (IOException e) {
                    errcode = 2;
                    errmessage = e.getMessage();
                }
                if (computeMD5) {
                    byte dig[];
                    try {
                        dig = md5.digest();
                        DataOutputStream fmd5 = new DataOutputStream(new FileOutputStream(dialMD5.getFile()));
                        fmd5.writeBytes(ProgramWindow.arrayBytetohexString(dig));
                        fmd5.close();
                    } catch (IOException oe) {
                        errmessage = oe.getMessage();
                        errcode = 3;
                    }
                }
                if (computeSHA1) {
                    byte dig[];
                    try {
                        dig = mdSHA.digest();
                        DataOutputStream fsha = new DataOutputStream(new FileOutputStream(dialSHA1.getFile()));
                        fsha.writeBytes(ProgramWindow.arrayBytetohexString(dig));
                        fsha.close();
                    } catch (IOException oe) {
                        errmessage = oe.getMessage();
                        errcode = 4;
                    }
                }
                return errcode;
            }

            protected void done() {
                quitProgress();
                setCursor(null);
                switch(errcode) {
                    case 0:
                        JOptionPane.showMessageDialog(null, "File successfully splitted.", "JoinSplit", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case 1:
                        JOptionPane.showMessageDialog(null, errmessage + "\nIgnore and delete created files.", "File error", JOptionPane.WARNING_MESSAGE);
                        break;
                    case 2:
                        JOptionPane.showMessageDialog(null, errmessage + "\nIgnore and delete created files.", "IO error", JOptionPane.WARNING_MESSAGE);
                        break;
                    case 3:
                        JOptionPane.showMessageDialog(null, errmessage + "\nImpossible to save MD5 digest to file.", "IO error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case 4:
                        JOptionPane.showMessageDialog(null, errmessage + "\nImpossible to save SHA1 digest to file.", "IO error", JOptionPane.ERROR_MESSAGE);
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Unknown error.", "Split error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        public SplitDialog(ProgramWindow owner, String path) {
            super(owner, "Split file", true);
            this.owner = owner;
            Box col, row1, row2;
            col = Box.createVerticalBox();
            row1 = Box.createHorizontalBox();
            row2 = Box.createHorizontalBox();
            String multiples[] = { "byte(s)", "kilobyte(s)", "megabyte(s)", "gigabyte(s)" };
            filepath = new JTextField(path);
            filepath.setEditable(false);
            col.add(filepath);
            col.add(Box.createVerticalStrut(20));
            inputPieceMaxSize = new JTextField(10);
            row1.add(inputPieceMaxSize);
            row1.add(Box.createHorizontalStrut(10));
            sizeMultiple = new JComboBox(multiples);
            row1.add(sizeMultiple);
            col.add(row1);
            col.add(Box.createVerticalStrut(20));
            dialMD5 = new OptionalFile("Create MD5 hash");
            col.add(dialMD5);
            col.add(Box.createVerticalStrut(20));
            dialSHA1 = new OptionalFile("Create SHA1 hash");
            col.add(dialSHA1);
            col.add(Box.createVerticalStrut(20));
            butConfirm = new JButton("Split");
            butConfirm.addActionListener(this);
            row2.add(butConfirm);
            row2.add(Box.createHorizontalStrut(10));
            butCancel = new JButton("Cancel");
            butCancel.addActionListener(this);
            row2.add(butCancel);
            col.add(row2);
            getContentPane().add(col);
            setSize(450, 280);
        }

        private void setZeroFilename(char[] fname) {
            int i;
            i = fname.length - 1;
            while ((i >= 0) && (fname[i] != '_')) --i;
            --i;
            while ((i >= 0) && (fname[i] != 't')) fname[i--] = '0';
        }

        private void incrementFilename(char[] fname) {
            int i;
            i = fname.length - 1;
            while ((i >= 0) && (fname[i] != '_')) --i;
            --i;
            while ((i >= 0) && (fname[i] == '9')) fname[i--] = '0';
            if (i >= 0) fname[i] += (char) 1;
        }

        public void actionPerformed(ActionEvent ev) {
            Object src = ev.getSource();
            if (src == butConfirm) {
                int i;
                long limitSize;
                boolean computeMD5, computeSHA1;
                computeMD5 = dialMD5.activeOption();
                computeSHA1 = dialSHA1.activeOption();
                try {
                    limitSize = Long.parseLong(inputPieceMaxSize.getText());
                } catch (NumberFormatException e) {
                    limitSize = -1;
                }
                if (limitSize > 0) {
                    i = sizeMultiple.getSelectedIndex();
                    while (i-- > 0) limitSize *= 1024;
                    File f = new File(filepath.getText());
                    JFileChooser fc = new JFileChooser();
                    fc.setMultiSelectionEnabled(false);
                    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        SplitFile task;
                        showProgress("Splitting file...");
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        task = new SplitFile(f, fc.getSelectedFile().getPath(), limitSize, computeMD5, computeSHA1);
                        task.addPropertyChangeListener(owner);
                        task.execute();
                    } else {
                        JOptionPane.showMessageDialog(null, "Split file :\naction cancelled by user", "JoinSplit", JOptionPane.INFORMATION_MESSAGE);
                    }
                    setVisible(false);
                } else {
                    JOptionPane.showMessageDialog(null, "Action aborted :\nYou must enter a strictly positive number", "JoinSplit", JOptionPane.ERROR_MESSAGE);
                }
            } else if (src == butCancel) {
                setVisible(false);
            }
        }
    }

    class JoinDialog extends JDialog implements ActionListener {

        private File[] filepieces;

        private DefaultListModel listModel;

        private JList listfiles;

        private JButton butConfirm, butCancel, butAdd, butRemove, butUp, butDown;

        private OptionalFile dialMD5, dialSHA1;

        private ProgramWindow owner;

        class JoinParts extends SwingWorker<Integer, Void> {

            private File outFile;

            private boolean computeMD5, computeSHA1;

            private int errcode, md5result, sharesult;

            private String[] inFilesList;

            private String errmessage;

            private JoinParts(String[] inputFiles, File outputFile, boolean doMD5, boolean doSHA1) {
                inFilesList = inputFiles;
                outFile = outputFile;
                computeMD5 = doMD5;
                computeSHA1 = doSHA1;
                errcode = md5result = sharesult = 0;
                errmessage = null;
            }

            public Integer doInBackground() {
                MessageDigest md5, mdSHA;
                md5 = mdSHA = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                    mdSHA = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException e) {
                }
                try {
                    FileInputStream fins;
                    FileOutputStream fout = new FileOutputStream(outFile);
                    int n;
                    byte[] bufRaw = new byte[1024000];
                    setProgress(0);
                    for (int i = 0; i < inFilesList.length; ) {
                        System.out.println("Joining " + inFilesList[i]);
                        fins = new FileInputStream(inFilesList[i]);
                        do {
                            n = fins.read(bufRaw);
                            if (n > 0) {
                                if (computeMD5) md5.update(bufRaw, 0, n);
                                if (computeSHA1) mdSHA.update(bufRaw, 0, n);
                                fout.write(bufRaw, 0, n);
                            }
                        } while (n == bufRaw.length);
                        fins.close();
                        setProgress(++i * 100 / inFilesList.length);
                    }
                    fout.close();
                } catch (FileNotFoundException e) {
                    errcode = 1;
                    errmessage = e.getMessage();
                } catch (IOException e) {
                    errcode = 2;
                    errmessage = e.getMessage();
                }
                setProgress(100);
                if (computeMD5) {
                    byte dig[];
                    String oldH;
                    try {
                        dig = md5.digest();
                        BufferedReader fmd5 = new BufferedReader(new FileReader(dialMD5.getFile()));
                        oldH = fmd5.readLine();
                        fmd5.close();
                        if (oldH.compareToIgnoreCase(ProgramWindow.arrayBytetohexString(dig)) == 0) md5result = 1; else md5result = -1;
                    } catch (IOException oe) {
                        errcode = 3;
                        errmessage = oe.getMessage();
                    }
                }
                if (computeSHA1) {
                    byte dig[];
                    String oldH;
                    try {
                        dig = mdSHA.digest();
                        BufferedReader fsha = new BufferedReader(new FileReader(dialSHA1.getFile()));
                        oldH = fsha.readLine();
                        fsha.close();
                        if (oldH.compareToIgnoreCase(ProgramWindow.arrayBytetohexString(dig)) == 0) sharesult = 1; else sharesult = -1;
                    } catch (IOException oe) {
                        errcode = 4;
                        errmessage = oe.getMessage();
                    }
                }
                return errcode;
            }

            protected void done() {
                quitProgress();
                setCursor(null);
                switch(errcode) {
                    case 0:
                        JOptionPane.showMessageDialog(null, "All parts joined.", "JoinSplit", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case 1:
                        JOptionPane.showMessageDialog(null, errmessage + "\nIgnore and delete created file.", "File error", JOptionPane.WARNING_MESSAGE);
                        break;
                    case 2:
                        JOptionPane.showMessageDialog(null, errmessage + "\nIgnore and delete created file.", "IO error", JOptionPane.WARNING_MESSAGE);
                        break;
                    case 3:
                        JOptionPane.showMessageDialog(null, errmessage + "\nImpossible to read MD5 digest from file and verify reconstructed file.", "IO error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case 4:
                        JOptionPane.showMessageDialog(null, errmessage + "\nImpossible to read SHA digest from file and verify reconstructed file.", "IO error", JOptionPane.ERROR_MESSAGE);
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Unknown error.", "Join error", JOptionPane.ERROR_MESSAGE);
                }
                if (md5result > 0) JOptionPane.showMessageDialog(null, "Same hash found.\nReconstructed file must be valid.", "MD5 hash", JOptionPane.INFORMATION_MESSAGE); else if (md5result < 0) JOptionPane.showMessageDialog(null, "Different hash found.\nReconstructed file is not valid.", "MD5 hash", JOptionPane.ERROR_MESSAGE);
                if (sharesult > 0) JOptionPane.showMessageDialog(null, "Same hash found.\nReconstructed file must be valid.", "SHA hash", JOptionPane.INFORMATION_MESSAGE); else if (sharesult < 0) JOptionPane.showMessageDialog(null, "Different hash found.\nReconstructed file is not valid.", "SHA hash", JOptionPane.ERROR_MESSAGE);
            }
        }

        public JoinDialog(ProgramWindow owner, File[] selectedfiles) {
            super(owner, "Join files", true);
            this.owner = owner;
            Box col, row0, row, col2;
            listModel = new DefaultListModel();
            for (int i = 0; i < selectedfiles.length; ++i) listModel.addElement(selectedfiles[i].getPath());
            listfiles = new JList(listModel);
            filepieces = selectedfiles;
            col = Box.createVerticalBox();
            row0 = Box.createHorizontalBox();
            row = Box.createHorizontalBox();
            col2 = Box.createVerticalBox();
            JScrollPane asc = new JScrollPane(listfiles);
            row0.add(asc);
            row0.add(Box.createHorizontalStrut(10));
            butUp = new JButton("UP");
            butUp.addActionListener(this);
            col2.add(butUp);
            col2.add(Box.createVerticalStrut(10));
            butDown = new JButton("DOWN");
            butDown.addActionListener(this);
            col2.add(butDown);
            col2.add(Box.createVerticalStrut(10));
            butAdd = new JButton("+");
            butAdd.addActionListener(this);
            col2.add(butAdd);
            col2.add(Box.createVerticalStrut(10));
            butRemove = new JButton("-");
            butRemove.addActionListener(this);
            col2.add(butRemove);
            row0.add(col2);
            col.add(row0);
            col.add(Box.createVerticalStrut(20));
            dialMD5 = new OptionalFile("Check MD5 hash", true);
            col.add(dialMD5);
            col.add(Box.createVerticalStrut(20));
            dialSHA1 = new OptionalFile("Check SHA1 hash", true);
            col.add(dialSHA1);
            col.add(Box.createVerticalStrut(20));
            butConfirm = new JButton("Join");
            butConfirm.addActionListener(this);
            row.add(butConfirm);
            butCancel = new JButton("Cancel");
            butCancel.addActionListener(this);
            row.add(Box.createHorizontalStrut(10));
            row.add(butCancel);
            col.add(row);
            getContentPane().add(col);
            setSize(530, 340);
        }

        public void actionPerformed(ActionEvent ev) {
            Object src = ev.getSource();
            if (src == butConfirm) {
                boolean computeMD5, computeSHA1;
                computeMD5 = dialMD5.activeOption();
                computeSHA1 = dialSHA1.activeOption();
                JFileChooser fc = new JFileChooser();
                fc.setMultiSelectionEnabled(false);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    int i, j;
                    String inFiles[];
                    JoinParts task;
                    j = listModel.getSize();
                    inFiles = new String[j];
                    for (i = 0; i < j; ++i) inFiles[i] = (String) listModel.elementAt(i);
                    showProgress("Joining file pieces...");
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    task = new JoinParts(inFiles, fc.getSelectedFile(), computeMD5, computeSHA1);
                    task.addPropertyChangeListener(owner);
                    task.execute();
                } else {
                    JOptionPane.showMessageDialog(null, "Join files :\naction cancelled by user", "JoinSplit", JOptionPane.INFORMATION_MESSAGE);
                }
                setVisible(false);
            } else if (src == butCancel) {
                setVisible(false);
            } else if (src == butAdd) {
                JFileChooser fc = new JFileChooser();
                fc.setMultiSelectionEnabled(true);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File[] extraFiles = fc.getSelectedFiles();
                    for (int i = 0; i < extraFiles.length; ++i) listModel.addElement(extraFiles[i].getPath());
                }
            } else if (src == butRemove) {
                int removeInd[] = listfiles.getSelectedIndices();
                for (int ind = removeInd.length - 1; ind >= 0; --ind) listModel.remove(removeInd[ind]);
            } else if (src == butUp) {
                Object swaptemp;
                int upInd[] = listfiles.getSelectedIndices();
                if ((upInd.length > 0) && (upInd[0] > 0)) {
                    for (int ind = 0; ind < upInd.length; ++ind) {
                        swaptemp = listModel.elementAt(upInd[ind] - 1);
                        listModel.setElementAt(listModel.elementAt(upInd[ind]), upInd[ind] - 1);
                        listModel.setElementAt(swaptemp, upInd[ind]);
                        upInd[ind]--;
                    }
                }
                listfiles.setSelectedIndices(upInd);
            } else if (src == butDown) {
                Object swaptemp;
                int downInd[] = listfiles.getSelectedIndices();
                if ((downInd.length > 0) && (downInd[downInd.length - 1] < listModel.getSize() - 1)) {
                    for (int ind = downInd.length - 1; ind >= 0; --ind) {
                        swaptemp = listModel.elementAt(downInd[ind] + 1);
                        listModel.setElementAt(listModel.elementAt(downInd[ind]), downInd[ind] + 1);
                        listModel.setElementAt(swaptemp, downInd[ind]);
                        downInd[ind]++;
                    }
                }
                listfiles.setSelectedIndices(downInd);
            }
        }
    }

    class OptionalFile extends JPanel implements ActionListener {

        private JCheckBox optBox;

        private JTextField filepath;

        private JButton butFile;

        private boolean isOpenFileDialog;

        public OptionalFile(String descOption) {
            optBox = new JCheckBox(descOption);
            optBox.addActionListener(this);
            filepath = new JTextField(20);
            filepath.setEditable(false);
            butFile = new JButton("...");
            butFile.setEnabled(false);
            butFile.addActionListener(this);
            Box layout = Box.createHorizontalBox();
            layout.add(optBox);
            layout.add(Box.createHorizontalStrut(15));
            layout.add(filepath);
            layout.add(Box.createHorizontalStrut(15));
            layout.add(butFile);
            add(layout);
            isOpenFileDialog = false;
        }

        public OptionalFile(String descOption, boolean dialogOpenFile) {
            this(descOption);
            isOpenFileDialog = dialogOpenFile;
        }

        public void actionPerformed(ActionEvent ev) {
            Object s = ev.getSource();
            if (s == optBox) {
                if (optBox.isSelected()) {
                    filepath.setEditable(true);
                    butFile.setEnabled(true);
                } else {
                    filepath.setEditable(false);
                    butFile.setEnabled(false);
                }
            } else if (s == butFile) {
                JFileChooser file = new JFileChooser();
                int r;
                if (isOpenFileDialog) r = file.showOpenDialog(this); else r = file.showSaveDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) filepath.setText(file.getSelectedFile().getPath());
            }
        }

        public boolean activeOption() {
            return optBox.isSelected();
        }

        public String getFile() {
            if (optBox.isSelected()) return filepath.getText(); else return new String("");
        }
    }
}
