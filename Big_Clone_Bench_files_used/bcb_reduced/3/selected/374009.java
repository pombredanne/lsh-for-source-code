package org.isodl.gui.reader;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import net.sourceforge.scuba.smartcards.APDUEvent;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;
import org.isodl.gui.common.DriverDataPanel;
import org.isodl.gui.common.FieldFormat;
import org.isodl.gui.common.FieldGroupSpec;
import org.isodl.gui.common.InputFieldSpec;
import org.isodl.gui.common.PicturePane;
import org.isodl.gui.common.Util;
import org.isodl.gui.common.ViewWindow;
import org.isodl.service.COMFile;
import org.isodl.service.DG11File;
import org.isodl.service.DG13File;
import org.isodl.service.DG14File;
import org.isodl.service.DG1File;
import org.isodl.service.DG2File;
import org.isodl.service.DG3File;
import org.isodl.service.DG4File;
import org.isodl.service.DG5File;
import org.isodl.service.DG6File;
import org.isodl.service.DriverDemographicInfo;
import org.isodl.service.DrivingLicense;
import org.isodl.service.DrivingLicenseFile;
import org.isodl.service.DrivingLicenseService;
import org.isodl.service.FaceInfo;
import org.isodl.service.FacePortrait;
import org.isodl.service.SODFile;
import org.isodl.service.SecurityObjectIndicator;
import org.isodl.util.Files;
import javax.smartcardio.*;

/**
 * A simple GUI application to read out ISO18013 driving licenses. Note: mainly
 * designed to read our ISO18013 applet implementation, which has the following
 * characteristics: DG1, DG2 (EAP protected), DG3 (EAP protected), DG4, DG5,
 * DG13 (Active Authentication), DG14 (Extended Access Protection). It is
 * probably not too useful for a general driving license card (have none to
 * test).
 * 
 * Sorry for poor GUI quality (feel free to write a better one ;))
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class LicenseReaderFrame extends JFrame implements ActionListener, APDUListener {

    private static final String SAVEPICTURE = "savepicture";

    private static final String SAVEDL = "savedl";

    private static final String VIEWDOCCERT = "viewdoccert";

    private static final String VIEWAAKEY = "viewaakey";

    private static final String VIEWEAPKEYS = "vieweapkeys";

    private static final String NONE = "<NONE>";

    private DriverDataPanel driverDataPanel = null;

    private JTabbedPane picturesPane = null;

    private JTextArea comContents;

    private JMenuItem savePicture;

    private JMenuItem saveDrivingLicense;

    private JMenuItem viewDocCert;

    private JMenuItem viewAAKey;

    private JMenuItem viewEAPKeys;

    private SecurityStatusBar statusBar;

    private DG1File dg1file = null;

    private DG2File dg2file = null;

    private DG3File dg3file = null;

    private DG4File dg4file = null;

    private DG5File dg5file = null;

    private DG6File dg6file = null;

    private DG11File dg11file = null;

    private DG13File dg13file = null;

    private DG14File dg14file = null;

    private SODFile sodFile = null;

    private COMFile comFile = null;

    private DrivingLicense dl = null;

    private boolean debug = true;

    /**
     * Log the APDU exchanges.
     */
    public void exchangedAPDU(APDUEvent apduEvent) {
        CommandAPDU c = apduEvent.getCommandAPDU();
        ResponseAPDU r = apduEvent.getResponseAPDU();
        if (debug) {
            System.out.println("C: " + Hex.bytesToHexString(c.getBytes()));
            System.out.println("R: " + Hex.bytesToHexString(r.getBytes()));
        }
    }

    /**
     * Construct the main GUI frame.
     */
    public LicenseReaderFrame() {
        super("Driving License");
        setLayout(new BorderLayout());
        Vector<InputFieldSpec> inputs = new Vector<InputFieldSpec>();
        inputs.add(new InputFieldSpec("family", "Family Name", new FieldFormat(FieldFormat.LETTERS | FieldFormat.SYMBOL, 0, 36), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("names", "Given Names", new FieldFormat(FieldFormat.LETTERS | FieldFormat.SYMBOL, 0, 36), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("birth", "Birth Date", new FieldFormat(FieldFormat.DIGITS, 8, 8), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("issue", "Issue Date", new FieldFormat(FieldFormat.DIGITS, 8, 8), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("expiry", "Expiry Date", new FieldFormat(FieldFormat.DIGITS, 8, 8), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("country", "Issuing Country", new FieldFormat(FieldFormat.LETTERS, 3, 3), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("authority", "Issuing Authority", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 65), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("number", "License Number", new FieldFormat(FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 25), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("categories", "Categories", new FieldFormat(true), FieldGroupSpec.driverData));
        inputs.add(new InputFieldSpec("gender", "Gender", new FieldFormat(FieldFormat.DIGITS, 1, 1), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("height", "Height", new FieldFormat(FieldFormat.DIGITS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("weight", "Weight", new FieldFormat(FieldFormat.DIGITS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("eye", "Eye Color", new FieldFormat(FieldFormat.LETTERS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("hair", "Hair Color", new FieldFormat(FieldFormat.LETTERS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("birthplace", "Birth Place", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 35), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("residenceplace", "Residence Place", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 113), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("adminnumber", "Admin. Number", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 25), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("documentdisc", "Document Disc.", new FieldFormat(FieldFormat.DIGITS, 2, 2), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("datadisc", "Data Disc.", new FieldFormat(FieldFormat.DIGITS, 2, 2), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("isoid", "ISO Issuer ID", new FieldFormat(FieldFormat.DIGITS, 6, 6), FieldGroupSpec.extraAuthorityData));
        InputFieldSpec[] ins = new InputFieldSpec[inputs.size()];
        int i = 0;
        Iterator<InputFieldSpec> it = inputs.iterator();
        while (it.hasNext()) {
            ins[i++] = it.next();
        }
        driverDataPanel = new DriverDataPanel(this, ins, false);
        picturesPane = new JTabbedPane();
        JPanel picPanel = new JPanel();
        picPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        picPanel.add(picturesPane, c);
        JPanel driverPanel = new JPanel();
        driverPanel.setLayout(new GridBagLayout());
        GridBagConstraints ccc = new GridBagConstraints();
        ccc.anchor = GridBagConstraints.NORTH;
        ccc.gridx = 0;
        ccc.gridy = 0;
        driverPanel.add(picPanel, ccc);
        ccc.gridx++;
        driverPanel.add(driverDataPanel, ccc);
        ccc.gridx = 0;
        ccc.gridy++;
        ccc.gridwidth = 2;
        ccc.anchor = GridBagConstraints.CENTER;
        comContents = new JTextArea();
        comContents.setEditable(false);
        comContents.setText(NONE);
        JPanel p = new JPanel();
        p.add(comContents);
        p.setBorder(BorderFactory.createTitledBorder("COM"));
        driverPanel.add(p, ccc);
        add(driverPanel, BorderLayout.CENTER);
        statusBar = new SecurityStatusBar();
        add(statusBar, BorderLayout.SOUTH);
        JMenuBar menu = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        saveDrivingLicense = new JMenuItem("Save as...");
        saveDrivingLicense.setActionCommand(SAVEDL);
        saveDrivingLicense.addActionListener(this);
        saveDrivingLicense.setEnabled(false);
        fileMenu.add(saveDrivingLicense);
        savePicture = new JMenuItem("Save Picture...");
        savePicture.setActionCommand(SAVEPICTURE);
        savePicture.addActionListener(this);
        savePicture.setEnabled(false);
        fileMenu.add(savePicture);
        menu.add(fileMenu);
        JMenu viewMenu = new JMenu("View");
        viewDocCert = new JMenuItem("Doc. Certificate...");
        viewDocCert.setActionCommand(VIEWDOCCERT);
        viewDocCert.addActionListener(this);
        viewDocCert.setEnabled(false);
        viewMenu.add(viewDocCert);
        viewAAKey = new JMenuItem("AA pub. key...");
        viewAAKey.setActionCommand(VIEWAAKEY);
        viewAAKey.addActionListener(this);
        viewAAKey.setEnabled(false);
        viewMenu.add(viewAAKey);
        viewEAPKeys = new JMenuItem("EAP card keys...");
        viewEAPKeys.setActionCommand(VIEWEAPKEYS);
        viewEAPKeys.addActionListener(this);
        viewEAPKeys.setEnabled(false);
        viewMenu.add(viewEAPKeys);
        menu.add(viewMenu);
        setJMenuBar(menu);
        addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                System.out.println("Exiting...");
                System.exit(0);
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }
        });
        setSize(600, 550);
    }

    public void actionPerformed(ActionEvent e) {
        if (SAVEPICTURE.equals(e.getActionCommand())) {
            byte[] data = ((PicturePane) picturesPane.getSelectedComponent()).getImage();
            if (data == null) {
                return;
            }
            File f = Util.getFile(this, "Save file", true);
            if (f != null) {
                try {
                    Files.writeFile(f, data);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (SAVEDL.equals(e.getActionCommand())) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(net.sourceforge.scuba.util.Files.ZIP_FILE_FILTER);
            int choice = fileChooser.showSaveDialog(getContentPane());
            switch(choice) {
                case JFileChooser.APPROVE_OPTION:
                    try {
                        File file = fileChooser.getSelectedFile();
                        FileOutputStream fileOut = new FileOutputStream(file);
                        ZipOutputStream zipOut = new ZipOutputStream(fileOut);
                        for (short fid : dl.getFileList()) {
                            String entryName = Hex.shortToHexString(fid) + ".bin";
                            InputStream dg = dl.getInputStream(fid);
                            zipOut.putNextEntry(new ZipEntry(entryName));
                            int bytesRead;
                            byte[] dgBytes = new byte[1024];
                            while ((bytesRead = dg.read(dgBytes)) > 0) {
                                zipOut.write(dgBytes, 0, bytesRead);
                            }
                            zipOut.closeEntry();
                        }
                        zipOut.finish();
                        zipOut.close();
                        fileOut.flush();
                        fileOut.close();
                        break;
                    } catch (IOException fnfe) {
                        fnfe.printStackTrace();
                    }
                default:
                    break;
            }
        } else if (VIEWDOCCERT.equals(e.getActionCommand())) {
            try {
                X509Certificate c = sodFile.getDocSigningCertificate();
                viewData(c.toString(), c.getEncoded());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (VIEWAAKEY.equals(e.getActionCommand())) {
            PublicKey k = dg13file.getPublicKey();
            viewData(k.toString(), k.getEncoded());
        } else if (VIEWEAPKEYS.equals(e.getActionCommand())) {
            String s = "";
            int count = 0;
            Set<Integer> ids = dg14file.getIds();
            List<byte[]> keys = new ArrayList<byte[]>();
            for (Integer id : ids) {
                if (count != 0) {
                    s += "\n";
                }
                if (id != -1) {
                    s += "Key identifier: " + id + "\n";
                }
                PublicKey k = dg14file.getKey(id);
                s += k.toString();
                keys.add(k.getEncoded());
                count++;
            }
            viewData(s, keys);
        }
    }

    private void viewData(String s, byte[] data) {
        List<byte[]> l = new ArrayList<byte[]>();
        l.add(data);
        new ViewWindow(this, "View", s, l);
    }

    private void viewData(String s, List<byte[]> data) {
        new ViewWindow(this, "View", s, data);
    }

    private void addPicture(String title, byte[] image, String mimeType, String date, String gender, String eyeColor) {
        PicturePane picture = new PicturePane(title, image, mimeType, date, gender, eyeColor);
        picturesPane.addTab(picture.getTitle(), picture);
    }

    void readLicenseData() {
        List<Short> files = dl.getFileList();
        InputStream in = null;
        Short fid = DrivingLicenseService.EF_COM;
        files.remove(fid);
        try {
            fid = DrivingLicenseService.EF_DG1;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg1file = new DG1File(in);
                DriverDemographicInfo di = dg1file.getDriverInfo();
                driverDataPanel.setValue("family", di.familyName);
                driverDataPanel.setValue("names", di.givenNames);
                driverDataPanel.setValue("birth", di.dob);
                driverDataPanel.setValue("issue", di.doi);
                driverDataPanel.setValue("expiry", di.doe);
                driverDataPanel.setValue("country", di.country);
                driverDataPanel.setValue("authority", di.authority);
                driverDataPanel.setValue("number", di.number);
                driverDataPanel.setCategories(dg1file.getCategories(), null);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG2;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg2file = new DG2File(in);
                driverDataPanel.setValue("gender", "" + dg2file.gender);
                driverDataPanel.setValue("height", "" + dg2file.height);
                driverDataPanel.setValue("weight", "" + dg2file.weight);
                driverDataPanel.setValue("eye", dg2file.eye);
                driverDataPanel.setValue("hair", dg2file.hair);
                driverDataPanel.setValue("birthplace", dg2file.pob);
                driverDataPanel.setValue("residenceplace", dg2file.por);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG3;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg3file = new DG3File(in);
                driverDataPanel.setValue("adminnumber", dg3file.adminNumber);
                driverDataPanel.setValue("documentdisc", "" + dg3file.documentDisc);
                driverDataPanel.setValue("datadisc", "" + dg3file.dataDisc);
                driverDataPanel.setValue("isoid", Hex.bytesToHexString(dg3file.idNumber));
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG6;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg6file = new DG6File(in);
                List<FaceInfo> faces = dg6file.getFaces();
                int i = 0;
                for (FaceInfo face : faces) {
                    addPicture("DG6." + (++i), face.getRawImage(), face.getMimeType(), null, face.getEyeColor().toString(), face.getGender().toString());
                }
                savePicture.setEnabled(true);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG4;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg4file = new DG4File(in);
                FacePortrait[] faces = dg4file.getFaces();
                int i = 0;
                for (FacePortrait face : faces) {
                    addPicture("DG4." + (++i), face.getImage(), face.getMimeType(), face.getDate(), null, null);
                }
                savePicture.setEnabled(true);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG5;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg5file = new DG5File(in);
                addPicture("DG5", dg5file.getImage(), dg5file.getMimeType(), null, null, null);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG11;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg11file = new DG11File(in);
                driverDataPanel.setCategories(null, dg11file.getCategories());
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG13;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg13file = new DG13File(in);
                viewAAKey.setEnabled(true);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG14;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                dg14file = new DG14File(in);
                viewEAPKeys.setEnabled(true);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_SOD;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                sodFile = new SODFile(in);
                viewDocCert.setEnabled(true);
                files.remove(fid);
            }
            for (Short f : files) {
                System.out.println("Don't know how to handle file ID: " + Hex.shortToHexString(f));
            }
            saveDrivingLicense.setEnabled(true);
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    void verifySecurity(DrivingLicenseService service) {
        if (dg13file != null) {
            PublicKey k = dg13file.getPublicKey();
            try {
                boolean result = service.doAA(k);
                if (result) {
                    statusBar.setAAOK();
                } else {
                    statusBar.setAAFail("wrong signature");
                }
            } catch (CardServiceException cse) {
                statusBar.setAAFail(cse.getMessage());
            }
        } else {
            statusBar.setAANotChecked();
        }
        List<Integer> comDGList = new ArrayList<Integer>();
        for (Integer tag : comFile.getTagList()) {
            comDGList.add(DrivingLicenseFile.lookupDataGroupNumberByTag(tag));
        }
        Collections.sort(comDGList);
        Map<Integer, byte[]> hashes = sodFile.getDataGroupHashes();
        List<Integer> tagsOfHashes = new ArrayList<Integer>();
        tagsOfHashes.addAll(hashes.keySet());
        Collections.sort(tagsOfHashes);
        if (!tagsOfHashes.equals(comDGList)) {
            statusBar.setDIFail("\"Jeroen van Beek sanity check\" failed!");
        } else {
            try {
                String digestAlgorithm = sodFile.getDigestAlgorithm();
                MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
                for (int dgNumber : hashes.keySet()) {
                    short fid = DrivingLicenseFile.lookupFIDByTag(DrivingLicenseFile.lookupTagByDataGroupNumber(dgNumber));
                    byte[] storedHash = hashes.get(dgNumber);
                    digest.reset();
                    InputStream dgIn = null;
                    Exception exc = null;
                    try {
                        dgIn = dl.getInputStream(fid);
                    } catch (Exception ex) {
                        exc = ex;
                    }
                    if (dgIn == null && dl.hasEAP() && !dl.wasEAPPerformed() && dl.getEAPFiles().contains(fid)) {
                        continue;
                    } else {
                        if (exc != null) throw exc;
                    }
                    byte[] buf = new byte[4096];
                    while (true) {
                        int bytesRead = dgIn.read(buf);
                        if (bytesRead < 0) {
                            break;
                        }
                        digest.update(buf, 0, bytesRead);
                    }
                    byte[] computedHash = digest.digest();
                    if (!Arrays.equals(storedHash, computedHash)) {
                        statusBar.setDIFail("Authentication of DG" + dgNumber + " failed");
                    }
                }
                statusBar.setDIOK("Hash alg. " + digestAlgorithm);
            } catch (Exception e) {
                statusBar.setDIFail(e.getMessage());
            }
        }
        try {
            X509Certificate docSigningCert = sodFile.getDocSigningCertificate();
            if (sodFile.checkDocSignature(docSigningCert)) {
                statusBar.setDSOK("sig. alg. " + sodFile.getDigestEncryptionAlgorithm());
            } else {
                statusBar.setDSFail("DS Signature incorrect");
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusBar.setDSFail(e.getMessage());
        }
    }

    private String formatComFile() {
        if (comFile == null) return NONE;
        List<Integer> list = comFile.getDGNumbers();
        String result = "Data groups:";
        for (Integer i : list) {
            result += " DG" + i.toString();
        }
        result += "\n";
        SecurityObjectIndicator[] sois = comFile.getSOIArray();
        if (sois.length > 0) {
            result += "Security Object Indicators:\n";
            for (SecurityObjectIndicator soi : sois) {
                result += "  " + soi.toString() + "\n";
            }
        }
        result = result.substring(0, result.length() - 1);
        return result;
    }

    public JTextArea getCOMContentsField() {
        return comContents;
    }

    public SecurityStatusBar getStatusBar() {
        return statusBar;
    }

    public DrivingLicense getDrivingLicense() {
        return dl;
    }

    public void setDrivingLicense(DrivingLicense dl) {
        this.dl = dl;
    }

    public COMFile getCOMFile() {
        return comFile;
    }

    public void setCOMFile(COMFile comFile) {
        this.comFile = comFile;
        comContents.setText(formatComFile());
    }
}
