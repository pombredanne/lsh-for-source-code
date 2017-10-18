package org.isodl.gui.maker;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sourceforge.scuba.smartcards.APDUEvent;
import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.util.Hex;
import org.isodl.gui.common.DriverDataPanel;
import org.isodl.gui.common.FieldFormat;
import org.isodl.gui.common.FieldGroupSpec;
import org.isodl.gui.common.InputFieldSpec;
import org.isodl.gui.common.PicturePane;
import org.isodl.gui.common.Util;
import org.isodl.gui.common.ViewWindow;
import org.isodl.service.COMFile;
import org.isodl.service.CategoryInfo;
import org.isodl.service.DG11File;
import org.isodl.service.DG1File;
import org.isodl.service.DG2File;
import org.isodl.service.DG3File;
import org.isodl.service.DG4File;
import org.isodl.service.DG5File;
import org.isodl.service.DocumentSigner;
import org.isodl.service.DriverDemographicInfo;
import org.isodl.service.DrivingLicense;
import org.isodl.service.DrivingLicenseEvent;
import org.isodl.service.DrivingLicenseListener;
import org.isodl.service.DrivingLicenseManager;
import org.isodl.service.DrivingLicensePersoService;
import org.isodl.service.DrivingLicenseService;
import org.isodl.service.FacePortrait;
import org.isodl.service.SODFile;
import org.isodl.service.SecurityObjectIndicator;
import org.isodl.service.SecurityObjectIndicatorDG14;
import org.isodl.service.SimpleDocumentSigner;
import org.isodl.util.Files;
import javax.smartcardio.*;
import org.ejbca.cvc.CVCertificate;

/**
 * A simple GUI application for creating ISO18013 driving licenses. Sorry for
 * (a) brief documentation, (b) poor GUI quality (feel free to write a better
 * one ;))
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class LicenseMakerFrame extends JFrame implements DrivingLicenseListener, ActionListener, APDUListener, ChangeListener {

    private static final String UPLOAD = "upload";

    private static final String SAVE = "save";

    private static final String LOAD = "load";

    private static final String ADDPICTURE = "addpicture";

    private static final String REMOVEPICTURE = "removepicture";

    private static final String LOADCERT = "loadcert";

    private static final String CLEARCERT = "clearcert";

    private static final String VIEWCERT = "viewcert";

    private static final String LOADCVCERT = "loadcvcert";

    private static final String CLEARCVCERT = "clearcvcert";

    private static final String VIEWCVCERT = "viewcvcert";

    private static final String LOADKEY = "loadkey";

    private static final String CLEARKEY = "clearkey";

    private static final String VIEWKEY = "viewkey";

    private static final String NONE = "<NONE>";

    private DriverDataPanel driverDataPanel = null;

    private Vector<PicturePane> pictures = new Vector<PicturePane>();

    private JTabbedPane picturesPane = null;

    private PicturePane signature = null;

    private JTextField cert;

    private X509Certificate certificate = null;

    private JTextField cvCert;

    private CVCertificate cvCertificate = null;

    private JTextField key;

    private RSAPrivateKey privateKey = null;

    private JTextField keyseed;

    private JCheckBox bapSHA1;

    private JButton upload;

    private JCheckBox eapDG2;

    private JCheckBox eapDG3;

    private DrivingLicensePersoService persoService = null;

    private boolean debug = true;

    private DrivingLicense dl = null;

    /**
     * Log the exchanged APDU-s on the console
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
     * 
     */
    public LicenseMakerFrame() {
        super("ISO18013 Driving License Maker");
        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
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
        inputs.add(new InputFieldSpec("gender", "Gender", new FieldFormat(FieldFormat.DIGITS, 1, 1, 1, 2), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("height", "Height", new FieldFormat(FieldFormat.DIGITS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("weight", "Weight", new FieldFormat(FieldFormat.DIGITS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("eye", "Eye Color", new FieldFormat(FieldFormat.LETTERS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("hair", "Hair Color", new FieldFormat(FieldFormat.LETTERS, 3, 3), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("birthplace", "Birth Place", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 35, -1, -1, 2), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("residenceplace", "Residence Place", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 113, -1, -1, 5), FieldGroupSpec.extraDriverData));
        inputs.add(new InputFieldSpec("adminnumber", "Admin. Number", new FieldFormat(FieldFormat.SYMBOL | FieldFormat.LETTERS | FieldFormat.DIGITS, 0, 25), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("documentdisc", "Document Disc.", new FieldFormat(FieldFormat.DIGITS, 2, 2, 1, 1), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("datadisc", "Data Disc.", new FieldFormat(FieldFormat.DIGITS, 2, 2, 1, 1), FieldGroupSpec.extraAuthorityData));
        inputs.add(new InputFieldSpec("isoid", "ISO Issuer ID", new FieldFormat(FieldFormat.DIGITS, 6, 6), FieldGroupSpec.extraAuthorityData));
        InputFieldSpec[] ins = new InputFieldSpec[inputs.size()];
        int i = 0;
        Iterator<InputFieldSpec> it = inputs.iterator();
        while (it.hasNext()) {
            ins[i++] = it.next();
        }
        driverDataPanel = new DriverDataPanel(this, ins, true);
        picturesPane = new JTabbedPane();
        PicturePane picture1 = new PicturePane("DG4.1", true);
        pictures.add(picture1);
        picturesPane.add(picture1.getTitle(), picture1);
        signature = new PicturePane("DG5", true);
        picturesPane.add(signature.getTitle(), signature);
        JPanel picPanel = new JPanel();
        picPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        picPanel.add(picturesPane, c);
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.gridy++;
        JButton addPicture = new JButton("Add");
        addPicture.setActionCommand(ADDPICTURE);
        addPicture.addActionListener(this);
        picPanel.add(addPicture, c);
        JButton removePicture = new JButton("Remove");
        removePicture.setActionCommand(REMOVEPICTURE);
        removePicture.addActionListener(this);
        c.gridx++;
        picPanel.add(removePicture, c);
        JPanel driverPanel = new JPanel();
        driverPanel.setLayout(new GridBagLayout());
        GridBagConstraints ccc = new GridBagConstraints();
        ccc.anchor = GridBagConstraints.NORTH;
        ccc.gridx = 0;
        ccc.gridy = 0;
        driverPanel.add(picPanel, ccc);
        ccc.gridx++;
        driverPanel.add(driverDataPanel, ccc);
        tabbedPane.add("Driver Data", driverPanel);
        JPanel certPanel = new JPanel();
        certPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        certPanel.add(new JLabel("Certificate: "), c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx++;
        cert = new JTextField(10);
        cert.setText(NONE);
        cert.setEditable(false);
        certPanel.add(cert, c);
        c.gridx++;
        JButton button = new JButton("Load...");
        button.setActionCommand(LOADCERT);
        button.addActionListener(this);
        certPanel.add(button, c);
        c.gridx++;
        button = new JButton("Clear");
        button.setActionCommand(CLEARCERT);
        button.addActionListener(this);
        certPanel.add(button, c);
        c.gridx++;
        button = new JButton("View...");
        button.setActionCommand(VIEWCERT);
        button.addActionListener(this);
        certPanel.add(button, c);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        certPanel.add(new JLabel("Key: "), c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx++;
        key = new JTextField(10);
        key.setText(NONE);
        key.setEditable(false);
        certPanel.add(key, c);
        c.gridx++;
        button = new JButton("Load...");
        button.setActionCommand(LOADKEY);
        button.addActionListener(this);
        certPanel.add(button, c);
        c.gridx++;
        button = new JButton("Clear");
        button.setActionCommand(CLEARKEY);
        button.addActionListener(this);
        certPanel.add(button, c);
        c.gridx++;
        button = new JButton("View...");
        button.setActionCommand(VIEWKEY);
        button.addActionListener(this);
        certPanel.add(button, c);
        certPanel.setBorder(BorderFactory.createTitledBorder("Document Signature"));
        JPanel eapPanel = new JPanel();
        eapPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        eapPanel.add(new JLabel("Terminal cert: "), c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx++;
        cvCert = new JTextField(10);
        cvCert.setText(NONE);
        cvCert.setEditable(false);
        eapPanel.add(cvCert, c);
        c.gridx++;
        button = new JButton("Load...");
        button.setActionCommand(LOADCVCERT);
        button.addActionListener(this);
        eapPanel.add(button, c);
        c.gridx++;
        button = new JButton("Clear");
        button.setActionCommand(CLEARCVCERT);
        button.addActionListener(this);
        eapPanel.add(button, c);
        c.gridx++;
        button = new JButton("View...");
        button.setActionCommand(VIEWCVCERT);
        button.addActionListener(this);
        eapPanel.add(button, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(2, 2, 2, 2);
        JPanel checkBoxes = new JPanel();
        checkBoxes.setLayout(new GridBagLayout());
        eapDG2 = new JCheckBox("DG2", false);
        eapDG3 = new JCheckBox("DG3", false);
        eapDG2.setEnabled(false);
        eapDG3.setEnabled(false);
        checkBoxes.add(eapDG2, cc);
        checkBoxes.add(eapDG3, cc);
        eapPanel.add(checkBoxes, c);
        eapPanel.setBorder(BorderFactory.createTitledBorder("EAP"));
        JPanel bapPanel = new JPanel();
        bapPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        bapPanel.add(new JLabel("Key Seed: "), c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx++;
        keyseed = new JTextField(20);
        keyseed.setText("");
        keyseed.setEditable(true);
        bapPanel.add(keyseed, c);
        c.gridx++;
        bapSHA1 = new JCheckBox(" SHA1", true);
        bapPanel.add(bapSHA1);
        bapPanel.setBorder(BorderFactory.createTitledBorder("BAP"));
        JPanel secPanel = new JPanel();
        secPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        secPanel.add(bapPanel, c);
        c.gridy++;
        secPanel.add(certPanel, c);
        c.gridy++;
        secPanel.add(eapPanel, c);
        tabbedPane.add("Security", secPanel);
        add(tabbedPane, BorderLayout.CENTER);
        JPanel buttonsPane = new JPanel();
        buttonsPane.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridy = 0;
        c.gridx = 0;
        button = new JButton("Load...");
        button.setActionCommand(LOAD);
        button.addActionListener(this);
        buttonsPane.add(button, c);
        c.gridx++;
        button = new JButton("Save...");
        button.setActionCommand(SAVE);
        button.addActionListener(this);
        buttonsPane.add(button, c);
        c.gridx++;
        upload = new JButton("Upload");
        upload.setActionCommand(UPLOAD);
        upload.addActionListener(this);
        upload.setEnabled(false);
        buttonsPane.add(upload, c);
        add(buttonsPane, BorderLayout.SOUTH);
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
        setSize(600, 500);
        setVisible(true);
        try {
            dl = new DrivingLicense();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not create an empty DL, will exit. (" + e.getClass() + ")");
            System.exit(1);
        }
    }

    /**
     * Handles input events.
     */
    public void actionPerformed(ActionEvent e) {
        if (UPLOAD.equals(e.getActionCommand())) {
            uploadDrivingLicense();
        } else if (SAVE.equals(e.getActionCommand())) {
            saveDrivingLicense();
        } else if (LOAD.equals(e.getActionCommand())) {
            loadDrivingLicense();
        } else if (ADDPICTURE.equals(e.getActionCommand())) {
            addPicture();
        } else if (REMOVEPICTURE.equals(e.getActionCommand())) {
            removePicture();
        } else if (LOADCERT.equals(e.getActionCommand())) {
            loadCertificate();
        } else if (LOADCVCERT.equals(e.getActionCommand())) {
            loadCVCertificate();
        } else if (CLEARCERT.equals(e.getActionCommand())) {
            certificate = null;
            cert.setText(NONE);
        } else if (CLEARCVCERT.equals(e.getActionCommand())) {
            cvCertificate = null;
            updateEAPBoxesState();
            cvCert.setText(NONE);
        } else if (LOADKEY.equals(e.getActionCommand())) {
            loadKey();
        } else if (CLEARKEY.equals(e.getActionCommand())) {
            privateKey = null;
            key.setText(NONE);
        } else if (VIEWCERT.equals(e.getActionCommand())) {
            if (certificate != null) {
                try {
                    viewData(certificate.toString(), certificate.getEncoded());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (VIEWCVCERT.equals(e.getActionCommand())) {
            if (cvCertificate != null) {
                try {
                    viewData(cvCertificate.getAsText(), cvCertificate.getDEREncoded());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (VIEWKEY.equals(e.getActionCommand())) {
            if (privateKey != null) {
                viewData(privateKey.toString(), privateKey.getEncoded());
            }
        }
    }

    private void viewData(String s, byte[] data) {
        List<byte[]> l = new ArrayList<byte[]>();
        l.add(data);
        new ViewWindow(this, "View", s, l);
    }

    private void loadCertificate() {
        File f = Util.getFile(this, "Load Certificate", false);
        if (f == null) return;
        certificate = Files.readCertFromFile(f);
        if (certificate != null) {
            cert.setText(certificate.getIssuerDN().getName());
        }
    }

    private void loadCVCertificate() {
        try {
            File file = Util.getFile(this, "Load CV Certificate", false);
            cvCertificate = Files.readCVCertificateFromFile(file);
            if (cvCertificate != null) {
                cvCert.setText(cvCertificate.getCertificateBody().getHolderReference().getConcatenated());
            }
        } catch (Exception e) {
            cvCertificate = null;
            cert.setText(NONE);
            e.printStackTrace();
        }
        updateEAPBoxesState();
    }

    private void loadKey() {
        File f = Util.getFile(this, "Load Key", false);
        if (f == null) return;
        privateKey = (RSAPrivateKey) Files.readRSAPrivateKeyFromFile(f);
        if (privateKey != null) {
            key.setText(privateKey.getAlgorithm() + " " + privateKey.getFormat());
        }
    }

    private void addPicture() {
        int num = pictures.size();
        PicturePane picture = new PicturePane("DG4." + ((num) + 1), true);
        pictures.add(picture);
        picturesPane.insertTab(picture.getTitle(), null, picture, null, num);
    }

    private void removePicture() {
        Component c = picturesPane.getSelectedComponent();
        if (c == signature || c == pictures.elementAt(0)) return;
        picturesPane.remove(c);
        pictures.remove(c);
    }

    private void propagateDrivingLicenseData() {
        picturesPane.removeAll();
        pictures.clear();
        List<Short> files = dl.getFileList();
        InputStream in = null;
        Short fid = null;
        try {
            fid = DrivingLicenseService.EF_DG1;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                DG1File dg1file = new DG1File(in);
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
                DG2File dg2file = new DG2File(in);
                driverDataPanel.setValue("gender", "" + dg2file.gender);
                driverDataPanel.setValue("height", "" + dg2file.height);
                driverDataPanel.setValue("weight", "" + dg2file.weight);
                driverDataPanel.setValue("eye", dg2file.eye);
                driverDataPanel.setValue("hair", dg2file.hair);
                driverDataPanel.setValue("birthplace", dg2file.pob);
                driverDataPanel.setValue("residenceplace", dg2file.por);
                files.remove(fid);
            } else {
                driverDataPanel.setValue("gender", null);
                driverDataPanel.setValue("height", null);
                driverDataPanel.setValue("weight", null);
                driverDataPanel.setValue("eye", null);
                driverDataPanel.setValue("hair", null);
                driverDataPanel.setValue("birthplace", null);
                driverDataPanel.setValue("residenceplace", null);
            }
            fid = DrivingLicenseService.EF_DG3;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                DG3File dg3file = new DG3File(in);
                driverDataPanel.setValue("adminnumber", dg3file.adminNumber);
                driverDataPanel.setValue("documentdisc", "" + dg3file.documentDisc);
                driverDataPanel.setValue("datadisc", "" + dg3file.dataDisc);
                driverDataPanel.setValue("isoid", Hex.bytesToHexString(dg3file.idNumber));
                files.remove(fid);
            } else {
                driverDataPanel.setValue("adminnumber", null);
                driverDataPanel.setValue("documentdisc", null);
                driverDataPanel.setValue("datadisc", null);
                driverDataPanel.setValue("isoid", null);
            }
            fid = DrivingLicenseService.EF_DG4;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                DG4File dg4file = new DG4File(in);
                FacePortrait[] faces = dg4file.getFaces();
                int i = 0;
                for (FacePortrait face : faces) {
                    addPicture("DG4." + (++i), face.getImage(), face.getMimeType(), face.getDate());
                }
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG5;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                DG5File dg5file = new DG5File(in);
                addPicture("DG5", dg5file.getImage(), dg5file.getMimeType(), null);
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG11;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                DG11File dg11file = new DG11File(in);
                driverDataPanel.setCategories(null, dg11file.getCategories());
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG13;
            if (files.contains(fid)) {
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_DG14;
            if (files.contains(fid)) {
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_SOD;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                SODFile sodFile = new SODFile(in);
                certificate = sodFile.getDocSigningCertificate();
                cert.setText(certificate.getIssuerDN().getName());
                files.remove(fid);
            }
            fid = DrivingLicenseService.EF_COM;
            if (files.contains(fid)) {
                in = dl.getInputStream(fid);
                COMFile comFile = new COMFile(in);
                files.remove(fid);
                for (SecurityObjectIndicator soi : comFile.getSOIArray()) {
                    if (soi instanceof SecurityObjectIndicatorDG14) {
                        List<Integer> dgs = ((SecurityObjectIndicatorDG14) soi).getDataGroups();
                        cvCertificate = dl.getCVCertificate();
                        if (cvCertificate != null) {
                            eapDG2.setEnabled(true);
                            eapDG3.setEnabled(true);
                            eapDG2.setSelected(dgs.contains(2));
                            eapDG3.setSelected(dgs.contains(3));
                            cvCert.setText(cvCertificate.getCertificateBody().getHolderReference().getConcatenated());
                        }
                    }
                }
            }
            for (Short f : files) {
                System.out.println("Don't know how to handle file ID: " + Hex.shortToHexString(f));
            }
            if (dl.getKeySeed() != null) {
                keyseed.setText(new String(dl.getKeySeed()));
                bapSHA1.setSelected(false);
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    private void addPicture(String title, byte[] image, String mimeType, String date) {
        PicturePane picture = new PicturePane(title, image, mimeType, date, null, null, true);
        if (title.startsWith("DG4")) {
            pictures.add(picture);
        } else {
            signature = picture;
        }
        picturesPane.addTab(title, picture);
    }

    private void collectDrivingLicenseData() {
        DriverDemographicInfo ddi = new DriverDemographicInfo(driverDataPanel.getValue("family"), driverDataPanel.getValue("names"), driverDataPanel.getValue("birth"), driverDataPanel.getValue("issue"), driverDataPanel.getValue("expiry"), driverDataPanel.getValue("country"), driverDataPanel.getValue("authority"), driverDataPanel.getValue("number"));
        List<CategoryInfo> l = driverDataPanel.getCategories(false);
        dl.putFile(DrivingLicenseService.EF_DG1, new DG1File(ddi, l).getEncoded());
        l = driverDataPanel.getCategories(true);
        if (l.size() > 0) {
            dl.putFile(DrivingLicenseService.EF_DG11, new DG11File(l).getEncoded());
        }
        if (driverDataPanel.getValue("gender") != null) {
            try {
                int gender = Integer.parseInt(driverDataPanel.getValue("gender"));
                int height = Integer.parseInt(driverDataPanel.getValue("height"));
                int weight = Integer.parseInt(driverDataPanel.getValue("weight"));
                String eye = driverDataPanel.getValue("eye");
                String hair = driverDataPanel.getValue("hair");
                String birthplace = driverDataPanel.getValue("birthplace");
                String residenceplace = driverDataPanel.getValue("residenceplace");
                dl.putFile(DrivingLicenseService.EF_DG2, new DG2File(gender, height, weight, eye, hair, birthplace, residenceplace).getEncoded(), eapDG2.isEnabled() && eapDG2.isSelected());
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        if (driverDataPanel.getValue("adminnumber") != null) {
            try {
                int documentDisc = Integer.parseInt(driverDataPanel.getValue("documentdisc"));
                int dataDisc = Integer.parseInt(driverDataPanel.getValue("datadisc"));
                String adminNumber = driverDataPanel.getValue("adminnumber");
                byte[] idNumber = Hex.hexStringToBytes(driverDataPanel.getValue("isoid"));
                dl.putFile(DrivingLicenseService.EF_DG3, new DG3File(adminNumber, documentDisc, dataDisc, idNumber).getEncoded(), eapDG3.isEnabled() && eapDG3.isSelected());
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        Iterator<PicturePane> it = pictures.iterator();
        ArrayList<FacePortrait> faces = new ArrayList<FacePortrait>();
        while (it.hasNext()) {
            PicturePane p = it.next();
            if (p.getImage() == null) {
                continue;
            }
            FacePortrait f = new FacePortrait(p.getImage(), p.getMimeType(), p.getDate());
            faces.add(f);
        }
        if (faces.size() > 0) {
            dl.putFile(DrivingLicenseService.EF_DG4, new DG4File(faces).getEncoded());
        }
        if (signature.getImage() != null) {
            dl.putFile(DrivingLicenseService.EF_DG5, new DG5File(signature.getImage(), signature.getMimeType()).getEncoded());
        }
        try {
            Provider provider = Security.getProvider("BC");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
            generator.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4));
            dl.setAAKeys(generator.generateKeyPair());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not generate RSA keys for AA.");
        }
        if (cvCertificate != null) {
            KeyPair ecKeyPair = null;
            try {
                String preferredProvider = "BC";
                Provider provider = Security.getProvider(preferredProvider);
                KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", provider);
                generator.initialize(new ECGenParameterSpec(DrivingLicensePersoService.EC_CURVE_NAME));
                ecKeyPair = generator.generateKeyPair();
                dl.setEAPKeys(ecKeyPair);
                dl.setCVCertificate(cvCertificate);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Could not generate EC keys for EAP.");
            }
        }
        if (certificate != null) {
            dl.setDocSigningCertificate(certificate);
        }
        if (privateKey != null) {
            DocumentSigner signer = new SimpleDocumentSigner(privateKey);
            if (certificate != null) {
                signer.setCertificate(certificate);
            }
            dl.setSigner(signer);
        }
        byte[] ks = getKeySeed();
        if (ks != null) {
            dl.setKeySeed(ks);
        }
    }

    private byte[] getKeySeed() {
        byte[] ks = null;
        if (bapSHA1.isSelected()) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                byte[] t = md.digest(keyseed.getText().getBytes());
                ks = new byte[16];
                System.arraycopy(t, 0, ks, 0, 16);
            } catch (NoSuchAlgorithmException nsae) {
            }
        } else if (keyseed.getText().length() == 16) {
            ks = keyseed.getText().getBytes();
        }
        return ks;
    }

    /**
     * Upload the driving license based on the data in the GUI. Note: there is
     * very little checks done on the presence of the (possibly required) data.
     * 
     */
    private void uploadDrivingLicense() {
        collectDrivingLicenseData();
        try {
            long timeElapsed = System.currentTimeMillis();
            dl.upload(persoService, getKeySeed());
            timeElapsed = System.currentTimeMillis() - timeElapsed;
            System.out.println("Uploading time: " + (timeElapsed / 1000) + " s.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDrivingLicense() {
        collectDrivingLicenseData();
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
                        String eap = "";
                        if (fid == DrivingLicenseService.EF_DG2 && eapDG2.isEnabled() && eapDG2.isSelected()) {
                            eap = "eap";
                        }
                        if (fid == DrivingLicenseService.EF_DG3 && eapDG3.isEnabled() && eapDG3.isSelected()) {
                            eap = "eap";
                        }
                        String entryName = Hex.shortToHexString(fid) + eap + ".bin";
                        InputStream dg = dl.getInputStream(fid);
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        int bytesRead;
                        byte[] dgBytes = new byte[1024];
                        while ((bytesRead = dg.read(dgBytes)) > 0) {
                            zipOut.write(dgBytes, 0, bytesRead);
                        }
                        zipOut.closeEntry();
                    }
                    byte[] keySeed = dl.getKeySeed();
                    if (keySeed != null) {
                        String entryName = "keyseed.bin";
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        zipOut.write(keySeed);
                        zipOut.closeEntry();
                    }
                    PrivateKey aaPrivateKey = dl.getAAPrivateKey();
                    if (aaPrivateKey != null) {
                        String entryName = "aaprivatekey.der";
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        zipOut.write(aaPrivateKey.getEncoded());
                        zipOut.closeEntry();
                    }
                    PrivateKey caPrivateKey = dl.getEAPPrivateKey();
                    if (caPrivateKey != null) {
                        String entryName = "caprivatekey.der";
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        zipOut.write(caPrivateKey.getEncoded());
                        zipOut.closeEntry();
                    }
                    CVCertificate cvCert = dl.getCVCertificate();
                    if (cvCert != null) {
                        String entryName = "cacert.cvcert";
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        zipOut.write(cvCert.getDEREncoded());
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
    }

    private void loadDrivingLicense() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(net.sourceforge.scuba.util.Files.ZIP_FILE_FILTER);
        int choice = fileChooser.showOpenDialog(getContentPane());
        switch(choice) {
            case JFileChooser.APPROVE_OPTION:
                try {
                    if (privateKey != null) {
                        dl = new DrivingLicense(fileChooser.getSelectedFile(), true, new SimpleDocumentSigner(privateKey));
                    } else {
                        dl = new DrivingLicense(fileChooser.getSelectedFile());
                    }
                    propagateDrivingLicenseData();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            default:
                break;
        }
    }

    public void updateEAPBoxesState() {
        if (cvCertificate == null) {
            eapDG2.setEnabled(false);
            eapDG3.setEnabled(false);
        } else {
            Map<String, JCheckBox> map = driverDataPanel.getOptionalDataGroupIds();
            eapDG2.setEnabled(map.get("DG2").isSelected());
            eapDG3.setEnabled(map.get("DG3").isSelected());
        }
    }

    public void stateChanged(ChangeEvent e) {
        updateEAPBoxesState();
    }

    /**
     * Reacts to the license inserted event.
     */
    public void licenseInserted(DrivingLicenseEvent pe) {
        System.out.println("Inserted license card.");
        try {
            DrivingLicenseService s = pe.getService();
            s.addAPDUListener(this);
            persoService = new DrivingLicensePersoService(s);
            persoService.open();
        } catch (Exception e) {
            persoService = null;
        }
        if (persoService != null) {
            if (upload != null) upload.setEnabled(true);
        }
    }

    /**
     * Reacts to the license removed event.
     */
    public void licenseRemoved(DrivingLicenseEvent pe) {
        System.out.println("Removed license card.");
        persoService = null;
        if (upload != null) upload.setEnabled(false);
    }

    /**
     * Reacts to the card inserted event.
     */
    public void cardInserted(CardEvent ce) {
        System.out.println("Inserted card.");
    }

    /**
     * Reacts to the card removed event.
     */
    public void cardRemoved(CardEvent ce) {
        System.out.println("Removed card.");
    }

    /**
     * Build up the frame and start up the application.
     * 
     * @param args
     *            should be none (ignored)
     */
    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Locale.setDefault(Locale.ENGLISH);
        DrivingLicenseManager manager = DrivingLicenseManager.getInstance();
        manager.addDrivingLicenseListener(new LicenseMakerFrame());
        CardManager cm = CardManager.getInstance();
        for (CardTerminal t : cm.getTerminals()) {
            cm.startPolling(t);
        }
    }
}
