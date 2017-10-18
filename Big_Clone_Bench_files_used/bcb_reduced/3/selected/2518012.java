package issrg.acm;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.Map;
import java.util.Hashtable;
import java.util.Date;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import org.w3c.dom.*;
import issrg.ac.AttributeCertificate;
import issrg.acm.extensions.LDAPSavingUtility;
import issrg.security.DefaultSecurity;
import issrg.utils.EnvironmentalVariables;
import issrg.utils.gui.repository.WebDAV_DIT;
import issrg.utils.gui.xml.XMLChangeEvent;
import issrg.utils.gui.xml.XMLChangeListener;
import issrg.utils.gui.xml.XMLChangeEvent;
import issrg.utils.webdav.HTTPMessageException;
import issrg.utils.webdav.WebDAVSocket;
import issrg.utils.webdav.WebDAVSocketHTTP;
import issrg.utils.webdav.WebDAVSocketHTTPS;

public class ConfigurationDialog extends JDialog implements XMLChangeListener {

    JTextField jHolderName;

    JTextField jValidityPeriod;

    JTextField jACType;

    JTextField jProviderURL;

    JTextField jProviderLogin;

    JPasswordField jProviderPassword;

    JTextField jDefaultProfile;

    JTextField jWebDAVHost;

    JTextField jWebDAVPort;

    JTextField jWebDAVSSL;

    JTextField jWebDAVRev;

    JTextField jWebDAVRevLoc;

    JTextField jWebDavRevCert;

    JCheckBox jWebDAVHttps;

    JButton jWebDAVSelectP12;

    JTextField jWebDAVP12Filename;

    JPasswordField jWebDAVP12Password;

    JCheckBox jCHEntrust;

    JButton jBProfile, jBHelper;

    JCheckBox jDIS;

    JTextField jDISAddress;

    JButton accept, cancel;

    JButton connect;

    JButton connectWebDAV;

    JButton addWebDAVSSL;

    JFrame jf;

    Map utils;

    Map vars;

    String file = "acm.cfg";

    String serialNumber = "";

    String RevLocation = "";

    String CertLocation = "";

    JRadioButton[] jaaia = new JRadioButton[2];

    JRadioButton[] jnorev = new JRadioButton[2];

    JRadioButton[] jdavrev = new JRadioButton[2];

    ButtonGroup group = new ButtonGroup();

    ButtonGroup group2 = new ButtonGroup();

    ButtonGroup group3 = new ButtonGroup();

    static String HOLDER_UTILITY = "";

    static String VALIDITY_UTILITY = "";

    static String WEBDAV_HOLDER_UTILITY = "";

    static String WEBDAV_REVOCATION_LOCATION = "";

    static String WEBDAV_CERTIFICATE_LOCATION = "";

    String webdavssl = "";

    private static boolean check = true;

    private static boolean davCloseCheck = false;

    public ConfigurationDialog(JFrame jf) {
        super(jf, "Preferences");
        this.jf = jf;
        Manager.acmConfigComponent.addXMLChangeListener(this);
        this.setResizable(false);
        JPanel pVariables, pUtilities, pLabels, pTexts;
        JPanel pSigning;
        JPanel pButt;
        JPanel pExtensions;
        JTabbedPane pData;
        pData = new JTabbedPane();
        pVariables = new JPanel();
        pVariables.setBorder(new TitledBorder("Attribute certificate default contents"));
        GridLayout gl = new GridLayout(4, 1);
        pLabels = new JPanel();
        pLabels.setLayout(gl);
        pTexts = new JPanel();
        pTexts.setLayout(gl);
        pLabels.add(new JLabel("Holder name"));
        pTexts.add(jHolderName = new JTextField("32"));
        pLabels.add(new JLabel("Validity period"));
        pTexts.add(jValidityPeriod = new JTextField(32));
        pLabels.add(new JLabel(""));
        pTexts.add(jBHelper = new JButton("Choose dates..."));
        jBHelper.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                Hashtable ht = new Hashtable();
                ht.put(EnvironmentalVariables.VALIDITY_PERIOD_STRING, jValidityPeriod.getText());
                ValidityEditor ve = new ValidityEditor();
                try {
                    ve.edit(null, null, ht);
                    if (ht.containsKey(EnvironmentalVariables.VALIDITY_PERIOD_STRING)) {
                        jValidityPeriod.setText((String) ht.get(EnvironmentalVariables.VALIDITY_PERIOD_STRING));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        pVariables.add(pLabels);
        pVariables.add(pTexts);
        pData.add("Variables", pVariables);
        pExtensions = new JPanel();
        pExtensions.setLayout(new BorderLayout());
        pExtensions.setBorder(new TitledBorder("Attribute Certificate Extensions"));
        JPanel extRight = new JPanel();
        extRight.setLayout(new GridBagLayout());
        jaaia[0] = new JRadioButton();
        jaaia[0].setActionCommand("jaaia0");
        jaaia[1] = new JRadioButton();
        jaaia[1].setActionCommand("jaaia1");
        jnorev[0] = new JRadioButton();
        jnorev[0].setActionCommand("jnorev0");
        jnorev[1] = new JRadioButton();
        jnorev[1].setActionCommand("jnorev1");
        jdavrev[0] = new JRadioButton();
        jdavrev[0].setActionCommand("jdavrev0");
        jdavrev[1] = new JRadioButton();
        jdavrev[0].setActionCommand("jdavrev1");
        group.add(jaaia[0]);
        group.add(jaaia[1]);
        group2.add(jnorev[0]);
        group2.add(jnorev[1]);
        group3.add(jdavrev[0]);
        group3.add(jdavrev[1]);
        jaaia[1].setSelected(true);
        jnorev[0].addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jaaia[1].setSelected(false);
                jdavrev[0].setEnabled(false);
                jdavrev[1].setEnabled(false);
                jdavrev[1].setSelected(false);
            }
        });
        jnorev[1].addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jdavrev[0].setEnabled(true);
                jdavrev[1].setEnabled(true);
            }
        });
        jnorev[1].setSelected(true);
        jdavrev[0].addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jnorev[0].setEnabled(false);
                jnorev[1].setEnabled(false);
                jnorev[1].setSelected(true);
            }
        });
        jdavrev[1].setSelected(true);
        jdavrev[1].addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jnorev[0].setEnabled(true);
                jnorev[1].setEnabled(true);
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        extRight.add(new JLabel("Insert AAIA extension into issued ACs                        "), c);
        c.gridx = 1;
        c.gridy = 0;
        extRight.add(new JLabel("Yes"), c);
        c.gridx = 2;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 20);
        extRight.add(jaaia[0], c);
        c.gridx = 3;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        extRight.add(new JLabel("No"), c);
        c.gridx = 4;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);
        extRight.add(jaaia[1], c);
        c.gridx = 0;
        c.gridy = 1;
        extRight.add(new JLabel("Indicate no revocation information will be available        "), c);
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        extRight.add(new JLabel("Yes"), c);
        c.gridx = 2;
        c.gridy = 1;
        c.insets = new Insets(0, 10, 0, 20);
        extRight.add(jnorev[0], c);
        c.gridx = 3;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        extRight.add(new JLabel("No"), c);
        c.gridx = 4;
        c.gridy = 1;
        c.insets = new Insets(0, 10, 0, 0);
        extRight.add(jnorev[1], c);
        pExtensions.add(extRight, BorderLayout.NORTH);
        c.gridx = 0;
        c.gridy = 2;
        extRight.add(new JLabel("Indicate WebDAV instant revocation is supported         "), c);
        c.gridx = 1;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 0);
        extRight.add(new JLabel("Yes"), c);
        c.gridx = 2;
        c.gridy = 2;
        c.insets = new Insets(0, 10, 0, 20);
        extRight.add(jdavrev[0], c);
        c.gridx = 3;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 0);
        extRight.add(new JLabel("No"), c);
        c.gridx = 4;
        c.gridy = 2;
        c.insets = new Insets(0, 10, 0, 0);
        extRight.add(jdavrev[1], c);
        pExtensions.add(extRight, BorderLayout.NORTH);
        pData.add("Extensions", pExtensions);
        pButt = new JPanel();
        pButt.setLayout(new FlowLayout(FlowLayout.CENTER));
        Container cnt = this.getContentPane();
        cnt.setLayout(new java.awt.BorderLayout());
        accept = new JButton("Save and Exit");
        cancel = new JButton("Quit without Saving");
        connect = new JButton("Test LDAP connection");
        connectWebDAV = new JButton("Test WebDAV connection");
        accept.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                saveChanges();
                if (davCloseCheck) {
                } else {
                    setVisible(false);
                }
            }
        });
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setVisible(false);
            }
        });
        connect.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                String why = tryConnection();
                if (why == null) {
                    showOK();
                } else {
                    showFalse(why);
                }
            }
        });
        connectWebDAV.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                String why = tryConnectionWebDAV();
                if (why == null) {
                    showOK();
                } else {
                    showFalse(why);
                }
            }
        });
        pButt.add(accept);
        pButt.add(cancel);
        pUtilities = new JPanel();
        pUtilities.setBorder(new TitledBorder("Additional functionality"));
        pUtilities.setLayout(new BorderLayout());
        SelectFileListener sfl = new SelectFileListener(jf);
        pSigning = new JPanel();
        JPanel epl = new JPanel();
        JPanel epr = new JPanel();
        epl.setLayout(new GridLayout(3, 1));
        epr.setLayout(new GridLayout(3, 1));
        pSigning.setLayout(new BorderLayout());
        epl.add(jDIS = new JCheckBox("Use Delegation Issuing Service", false));
        jDISAddress = new JTextField();
        jDISAddress.setEnabled(false);
        jDIS.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                boolean status = jDIS.isSelected();
                jDISAddress.setEnabled(status);
                if (status) {
                    jCHEntrust.setSelected(status);
                    jCHEntrust.setEnabled(false);
                    jDefaultProfile.setEnabled(status);
                    jBProfile.setEnabled(status);
                } else {
                    jCHEntrust.setEnabled(true);
                }
            }
        });
        epl.add(jCHEntrust = new JCheckBox("Digitally sign attribute certificates", true));
        jBProfile = new JButton("Select Signing Key...");
        jBProfile.addActionListener(sfl);
        jBProfile.setActionCommand("ENTRUST");
        epr.add(jDISAddress);
        epr.add(jBProfile);
        epl.add(new JLabel("Default Signing Key"));
        epr.add(jDefaultProfile = new JTextField(32));
        jCHEntrust.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                boolean status = jCHEntrust.isSelected();
                jDefaultProfile.setEnabled(status);
                jBProfile.setEnabled(status);
            }
        });
        pSigning.add("West", epl);
        pSigning.add("East", epr);
        JPanel ldap = new JPanel();
        ldap.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel pChecks = new JPanel();
        pChecks.setLayout(new GridLayout(2, 1));
        pChecks.add(ldap);
        JPanel pLDAP = new JPanel();
        pLDAP.setLayout(new BorderLayout());
        JPanel lpl = new JPanel();
        JPanel lpr = new JPanel();
        lpl.setLayout(new GridLayout(15, 1));
        lpr.setLayout(new GridLayout(15, 1));
        lpl.add(new JLabel("LDAP URL"));
        lpr.add(jProviderURL = new JTextField(24));
        lpl.add(new JLabel("LDAP Login"));
        lpr.add(jProviderLogin = new JTextField(10));
        lpl.add(new JLabel("LDAP Password"));
        lpr.add(jProviderPassword = new JPasswordField(10));
        lpr.add(connect);
        lpl.add(new JLabel(""));
        lpl.add(new JLabel("AC LDAP type"));
        lpr.add(jACType = new JTextField(32));
        JLabel lLine = new JLabel();
        JLabel rLine = new JLabel();
        lpl.add(lLine);
        lpr.add(rLine);
        lpl.add(new JLabel("WebDAV Host"));
        lpr.add(jWebDAVHost = new JTextField(32));
        lpl.add(new JLabel("WebDAV Port"));
        lpr.add(jWebDAVPort = new JTextField(5));
        lpl.add(new JLabel(""));
        lpr.add(connectWebDAV);
        lpl.add(jWebDAVHttps = new JCheckBox("HTTPS"));
        lpr.add(jWebDAVSelectP12 = new JButton("Select Signing Key..."));
        lpl.add(new JLabel("SSL Client Certificate and Signing Key  "));
        lpr.add(jWebDAVP12Filename = new JTextField());
        lpl.add(new JLabel("Password"));
        lpr.add(jWebDAVP12Password = new JPasswordField());
        lpl.add(new JLabel(""));
        lpl.add(new JLabel("WebDAV Server Certificate."));
        lpr.add(addWebDAVSSL = new JButton("Select Server Certificate"));
        lpr.add(jWebDAVSSL = new JTextField());
        jWebDAVSelectP12.addActionListener(sfl);
        jWebDAVSelectP12.setActionCommand("WEBDAVP12FILENAME");
        jWebDAVHttps.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                boolean status;
                if (status = jWebDAVHttps.isSelected()) {
                    jWebDAVPort.setText("443");
                } else {
                    jWebDAVPort.setText("80");
                }
                jWebDAVSelectP12.setEnabled(status);
                jWebDAVP12Filename.setEnabled(status);
                jWebDAVP12Password.setEnabled(status);
                jWebDAVSSL.setEnabled(status);
                addWebDAVSSL.setEnabled(status);
            }
        });
        addWebDAVSSL.addActionListener(sfl);
        addWebDAVSSL.setActionCommand("WEBDAVSSLFILENAME");
        jWebDAVSSL.addActionListener(sfl);
        pLDAP.add("West", lpl);
        pLDAP.add("East", lpr);
        pUtilities.add("North", pSigning);
        pUtilities.add("Center", pChecks);
        pUtilities.add("South", pLDAP);
        pData.add("Utilities", pUtilities);
        cnt.add("North", pData);
        cnt.add("South", pButt);
    }

    class SelectFileListener implements ActionListener {

        protected JFrame jf;

        SelectFileListener(JFrame jf) {
            this.jf = jf;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fd = new JFileChooser("Select file");
            fd.showOpenDialog(jf);
            if (fd.getSelectedFile() != null) {
                String file = fd.getSelectedFile().getAbsolutePath();
                if (e.getActionCommand().equals("ENTRUST")) jDefaultProfile.setText(file);
                if (e.getActionCommand().equals("WEBDAVP12FILENAME")) jWebDAVP12Filename.setText(file);
                if (e.getActionCommand().equals("WEBDAVSSLFILENAME")) {
                    File certpath = new File(file);
                    File store = new File("webdavtruststore.jks");
                    String password = "changeme";
                    char[] pass = password.toCharArray();
                    Certificate cert = getCertFromFile(certpath);
                    if (cert == null) {
                        removeKeyStore(store, pass, "WebDAV");
                    } else {
                        removeKeyStore(store, pass, "WebDAV");
                        addToKeyStore(store, pass, "WebDAV", cert);
                        jWebDAVSSL.setText(file);
                    }
                }
            }
        }
    }

    public Certificate getCertFromFile(File file) {
        try {
            try {
                FileInputStream fis;
                fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                CertificateFactory cf;
                cf = CertificateFactory.getInstance("X.509");
                while (bis.available() > 0) {
                    Certificate cert = cf.generateCertificate(bis);
                    System.out.println(cert.toString());
                    return cert;
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void addToKeyStore(File keystoreFile, char[] keystorePassword, String alias, java.security.cert.Certificate cert) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream in = new FileInputStream(keystoreFile);
            keystore.load(in, keystorePassword);
            in.close();
            keystore.setCertificateEntry(alias, cert);
            FileOutputStream out = new FileOutputStream(keystoreFile);
            keystore.store(out, keystorePassword);
            out.close();
        } catch (java.security.cert.CertificateException e) {
            System.err.println("CertificateException");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("NoSuchAlgorithmException Exception");
        } catch (FileNotFoundException e) {
            System.err.println("File not found Exception");
        } catch (KeyStoreException e) {
            System.err.println("keystore Exception");
        } catch (IOException e) {
            System.err.println("IO Exception");
        }
    }

    public static void removeKeyStore(File keystoreFile, char[] keystorePassword, String alias) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream in = new FileInputStream(keystoreFile);
            keystore.load(in, keystorePassword);
            in.close();
            if (keystore.containsAlias(alias)) {
                System.out.println("WebDAV certificate exists removing it now");
                keystore.deleteEntry(alias);
            }
            FileOutputStream out = new FileOutputStream(keystoreFile);
            keystore.store(out, keystorePassword);
            out.close();
        } catch (java.security.cert.CertificateException e) {
            System.err.println("CertificateException");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("NoSuchAlgorithmException Exception");
        } catch (FileNotFoundException e) {
            System.err.println("File not found Exception");
        } catch (KeyStoreException e) {
            System.err.println("keystore Exception");
        } catch (IOException e) {
            System.err.println("IO Exception");
        }
    }

    public void activateDialog() {
        pack();
        centerDialog();
        setModal(true);
        show();
    }

    public boolean initialiseDialog(Map environment) {
        vars = (Hashtable) environment.get(EnvironmentalVariables.VARIABLES_COLLECTION);
        utils = (Hashtable) environment.get(EnvironmentalVariables.UTILITIES_COLLECTION);
        boolean exists;
        if (vars != null && vars.size() > 0 && utils != null && utils.size() > 0) {
            exists = true;
            try {
                this.HOLDER_UTILITY = (String) utils.get(EnvironmentalVariables.LDAP_HOLDER_EDITOR_UTILITY);
                this.VALIDITY_UTILITY = (String) utils.get(EnvironmentalVariables.VALIDITY_PERIOD_EDITOR_UTILITY);
                this.WEBDAV_HOLDER_UTILITY = (String) utils.get(EnvironmentalVariables.WEBDAV_HOLDER_EDITOR_UTILITY);
                this.WEBDAV_REVOCATION_LOCATION = (String) vars.get("WEBDAV_REVOCATION_LOCATION");
                this.WEBDAV_CERTIFICATE_LOCATION = (String) vars.get("WEBDAV_CERTIFICATE_LOCATION");
            } catch (NullPointerException e) {
                this.HOLDER_UTILITY = "issrg.acm.ACMLDAPBrowser";
                this.VALIDITY_UTILITY = "issrg.acm.ValidityEditor";
                this.WEBDAV_HOLDER_UTILITY = "issrg.acm.ACMWebDAVBrowser";
                this.WEBDAV_REVOCATION_LOCATION = "Webdav.revocation.location";
                this.WEBDAV_CERTIFICATE_LOCATION = "Webdav.certificate.location";
            }
        } else exists = false;
        modifyDialog(exists);
        return true;
    }

    private void modifyDialog(boolean fileExists) {
        if (fileExists) {
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_REVOCATION_LOCATION)) {
                RevLocation = ((String) vars.get(EnvironmentalVariables.WEBDAV_REVOCATION_LOCATION));
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_CERTIFICATE_LOCATION)) {
                CertLocation = ((String) vars.get(EnvironmentalVariables.WEBDAV_CERTIFICATE_LOCATION));
            }
            if (vars.containsKey(EnvironmentalVariables.HOLDER_NAME_STRING)) {
                jHolderName.setText((String) vars.get(EnvironmentalVariables.HOLDER_NAME_STRING));
            } else jHolderName.setText("<EMPTY>");
            if (vars.containsKey(EnvironmentalVariables.LDAP_HOLDER_EDITOR_UTILITY)) {
                if (vars.containsKey(EnvironmentalVariables.HOLDER_EDITOR_UTILITY_SERVER)) {
                    jProviderURL.setText((String) vars.get(EnvironmentalVariables.HOLDER_EDITOR_UTILITY_SERVER));
                }
            }
            if (vars.containsKey(EnvironmentalVariables.SERIAL_NUMBER_STRING)) {
                serialNumber = (String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING);
            } else serialNumber = "<EMPTY>";
            if (vars.containsKey(EnvironmentalVariables.VALIDITY_PERIOD_STRING)) {
                jValidityPeriod.setText((String) vars.get(EnvironmentalVariables.VALIDITY_PERIOD_STRING));
            } else jValidityPeriod.setText("<EMPTY>");
            if (vars.containsKey(LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE)) {
                String acType = (String) vars.get(LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE);
                if ((!acType.equals("")) && (!acType.equals("<EMPTY>"))) jACType.setText((String) vars.get(LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE)); else jACType.setText("attributeCertificateAttribute");
            }
            if (utils.containsKey("issrg.acm.extensions.SimpleSigningUtility")) {
                if (vars.containsKey(DefaultSecurity.DEFAULT_FILE_STRING)) {
                    jDefaultProfile.setText((String) vars.get(DefaultSecurity.DEFAULT_FILE_STRING));
                } else jDefaultProfile.setText("<EMPTY>");
                jCHEntrust.setSelected(true);
            } else {
                jCHEntrust.setSelected(false);
                jDefaultProfile.setEnabled(false);
            }
            if (utils.containsKey("issrg.acm.extensions.ACMDISSigningUtility")) {
                if (vars.containsKey("DefaultDIS")) {
                    jDISAddress.setText((String) vars.get("DefaultDIS"));
                } else jDISAddress.setText("<EMPTY>");
                jDIS.setSelected(true);
                jCHEntrust.setSelected(true);
                jDefaultProfile.setEnabled(true);
                if (vars.containsKey(DefaultSecurity.DEFAULT_FILE_STRING)) {
                    jDefaultProfile.setText((String) vars.get(DefaultSecurity.DEFAULT_FILE_STRING));
                } else jDefaultProfile.setText("permis.p12");
            } else {
                jDIS.setSelected(false);
                jDISAddress.setEnabled(false);
            }
            if (vars.containsKey(EnvironmentalVariables.AAIA_LOCATION)) {
                jaaia[0].setSelected(true);
            }
            if (vars.containsKey(EnvironmentalVariables.NOREV_LOCATION)) {
                jnorev[0].setSelected(true);
                jdavrev[0].setEnabled(false);
                jdavrev[1].setEnabled(false);
                jdavrev[1].setSelected(false);
            }
            if (vars.containsKey(EnvironmentalVariables.DAVREV_LOCATION)) {
                jdavrev[0].setSelected(true);
                jnorev[0].setEnabled(false);
                jnorev[1].setEnabled(false);
                jnorev[1].setSelected(true);
            }
            if (vars.containsKey("LDAPSavingUtility.ProviderURI")) {
                jProviderURL.setText((String) vars.get("LDAPSavingUtility.ProviderURI"));
            } else jProviderURL.setText("<EMPTY>");
            if (vars.containsKey("LDAPSavingUtility.Login")) {
                jProviderLogin.setText((String) vars.get("LDAPSavingUtility.Login"));
            } else jProviderLogin.setText("<EMPTY>");
            if (vars.containsKey("LDAPSavingUtility.Password")) {
                jProviderPassword.setText((String) vars.get("LDAPSavingUtility.Password"));
            } else jProviderPassword.setText("<EMPTY>");
            if ((!vars.containsKey(EnvironmentalVariables.TRUSTSTORE)) || (((String) vars.get(EnvironmentalVariables.TRUSTSTORE)).equals(""))) {
                vars.put(EnvironmentalVariables.TRUSTSTORE, "truststorefile");
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_HOST)) {
                jWebDAVHost.setText((String) vars.get(EnvironmentalVariables.WEBDAV_HOST));
            } else {
                jWebDAVHost.setText("<EMPTY>");
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_PORT)) {
                jWebDAVPort.setText((String) vars.get(EnvironmentalVariables.WEBDAV_PORT));
            } else {
                jWebDAVPort.setText("<EMPTY>");
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_PROTOCOL)) {
                if (vars.get(EnvironmentalVariables.WEBDAV_PROTOCOL).equals("HTTPS")) {
                    jWebDAVHttps.setSelected(true);
                    jWebDAVSelectP12.setEnabled(true);
                    jWebDAVP12Filename.setEnabled(true);
                    jWebDAVP12Password.setEnabled(true);
                    jWebDAVSSL.setEnabled(true);
                    addWebDAVSSL.setEnabled(true);
                } else {
                    jWebDAVHttps.setSelected(false);
                    jWebDAVSelectP12.setEnabled(false);
                    jWebDAVP12Filename.setEnabled(false);
                    jWebDAVP12Password.setEnabled(false);
                    jWebDAVSSL.setEnabled(false);
                    addWebDAVSSL.setEnabled(false);
                }
            } else {
                jWebDAVHttps.setSelected(false);
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_P12FILENAME)) {
                jWebDAVP12Filename.setText((String) vars.get(EnvironmentalVariables.WEBDAV_P12FILENAME));
            } else {
                jWebDAVP12Filename.setText("<EMPTY>");
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_P12PASSWORD)) {
                jWebDAVP12Password.setText((String) vars.get(EnvironmentalVariables.WEBDAV_P12PASSWORD));
            } else {
                jWebDAVP12Password.setText("<EMPTY>");
            }
            if (vars.containsKey(EnvironmentalVariables.WEBDAV_SSLCERTIFICATE)) {
                jWebDAVSSL.setText((String) vars.get(EnvironmentalVariables.WEBDAV_SSLCERTIFICATE));
            } else {
                jWebDAVSSL.setText("<EMPTY>");
            }
        } else {
            jHolderName.setText("cn=A Permis Test User, o=PERMIS, c=gb");
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(new Date().toString().getBytes());
                byte[] result = md.digest();
                BigInteger bi = new BigInteger(result);
                bi = bi.abs();
                serialNumber = bi.toString(16);
            } catch (Exception e) {
                serialNumber = "<EMPTY>";
            }
            jValidityPeriod.setText("<EMPTY>");
            jDefaultProfile.setText("permis.p12");
            jCHEntrust.setSelected(true);
            jProviderURL.setText("ldap://sec.cs.kent.ac.uk/c=gb");
            jProviderLogin.setText("");
            jProviderPassword.setText("");
            jWebDAVHost.setText("");
            jWebDAVPort.setText("443");
            jWebDAVP12Filename.setText("");
            jACType.setText("attributeCertificateAttribute");
            vars.put(EnvironmentalVariables.TRUSTSTORE, "truststorefile");
            saveChanges();
        }
    }

    private String tryConnection() {
        try {
            issrg.utils.gui.repository.LDAP_DIT.connectTo(jProviderURL.getText());
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String tryConnectionWebDAV() {
        File certpath = new File(jWebDAVSSL.getText());
        File store = new File("webdavtruststore.jks");
        String password = "changeme";
        char[] pass = password.toCharArray();
        Certificate cert = getCertFromFile(certpath);
        if (cert == null) {
            removeKeyStore(store, pass, "WebDAV");
        } else {
            removeKeyStore(store, pass, "WebDAV");
            addToKeyStore(store, pass, "WebDAV", cert);
        }
        try {
            WebDAVSocket socket;
            if (jWebDAVHttps.isSelected()) {
                socket = new WebDAVSocketHTTPS(jWebDAVP12Filename.getText(), new String(jWebDAVP12Password.getPassword()));
            } else {
                socket = new WebDAVSocketHTTP();
            }
            WebDAV_DIT testWebDAV = new WebDAV_DIT(socket, jWebDAVHost.getText(), Integer.parseInt(jWebDAVPort.getText()));
            testWebDAV.testConnection("/");
        } catch (HTTPMessageException e) {
            return e.getErrorMessage();
        } catch (NumberFormatException e) {
            return "Invalid WebDAV Port Number: " + jWebDAVPort.getText();
        }
        return null;
    }

    private void showOK() {
        JOptionPane.showMessageDialog(jf, "Connection succesful");
    }

    private void showFalse(String why) {
        JOptionPane.showMessageDialog(jf, "Connection failed: " + why, "Error", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveChanges() {
        if (jWebDAVHttps.isSelected()) {
            File certpath = new File(jWebDAVSSL.getText());
            File store = new File("webdavtruststore.jks");
            String password = "changeme";
            char[] pass = password.toCharArray();
            Certificate cert = getCertFromFile(certpath);
            if (cert == null) {
                removeKeyStore(store, pass, "WebDAV");
            } else {
                removeKeyStore(store, pass, "WebDAV");
                addToKeyStore(store, pass, "WebDAV", cert);
            }
        }
        try {
            Node variables = Manager.acmConfigComponent.DOM.getElementsByTagName("Variables").item(0);
            NodeList variableList = Manager.acmConfigComponent.DOM.getElementsByTagName("Variable");
            if (vars.containsKey(EnvironmentalVariables.AAIA_LOCATION)) {
                if (jaaia[1].isSelected()) {
                    delItem(variableList, EnvironmentalVariables.AAIA_LOCATION);
                    vars.remove(EnvironmentalVariables.AAIA_LOCATION);
                }
            } else {
                if (jaaia[0].isSelected()) {
                    addItem("Variable", (Element) variables, EnvironmentalVariables.AAIA_LOCATION, EnvironmentalVariables.AAIA_LOCATION);
                    vars.put(EnvironmentalVariables.AAIA_LOCATION, EnvironmentalVariables.AAIA_LOCATION);
                }
            }
            if (vars.containsKey(EnvironmentalVariables.NOREV_LOCATION)) {
                if (jnorev[1].isSelected()) {
                    delItem(variableList, EnvironmentalVariables.NOREV_LOCATION);
                    vars.remove(EnvironmentalVariables.NOREV_LOCATION);
                }
            } else {
                if (jnorev[0].isSelected()) {
                    addItem("Variable", (Element) variables, EnvironmentalVariables.NOREV_LOCATION, EnvironmentalVariables.NOREV_LOCATION);
                    vars.put(EnvironmentalVariables.NOREV_LOCATION, EnvironmentalVariables.NOREV_LOCATION);
                    if (vars.containsKey(EnvironmentalVariables.DAVREV_LOCATION)) {
                        delItem(variableList, EnvironmentalVariables.DAVREV_LOCATION);
                        vars.remove(EnvironmentalVariables.DAVREV_LOCATION);
                    }
                }
            }
            if (vars.containsKey(EnvironmentalVariables.DAVREV_LOCATION)) {
                if (jdavrev[1].isSelected()) {
                    delItem(variableList, EnvironmentalVariables.DAVREV_LOCATION);
                    vars.remove(EnvironmentalVariables.DAVREV_LOCATION);
                }
            } else {
                if (jdavrev[0].isSelected()) {
                    addItem("Variable", (Element) variables, EnvironmentalVariables.DAVREV_LOCATION, EnvironmentalVariables.DAVREV_LOCATION);
                    vars.put(EnvironmentalVariables.DAVREV_LOCATION, EnvironmentalVariables.DAVREV_LOCATION);
                    if (vars.containsKey(EnvironmentalVariables.NOREV_LOCATION)) {
                        delItem(variableList, EnvironmentalVariables.NOREV_LOCATION);
                        vars.remove(EnvironmentalVariables.NOREV_LOCATION);
                    }
                }
            }
            if (variableList != null & variableList.getLength() > 0) {
                for (int i = 0; i < variableList.getLength(); i++) {
                    NamedNodeMap attributes = variableList.item(i).getAttributes();
                    String varName = attributes.getNamedItem("Name").getNodeValue();
                    if (varName.equals(EnvironmentalVariables.HOLDER_NAME_STRING)) {
                        if ((jHolderName.getText().intern() != "<EMPTY>") && (!jHolderName.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.HOLDER_NAME_STRING, jHolderName.getText());
                            issrg.acm.Manager.holder = jHolderName.getText();
                        }
                    } else if (varName.equals(EnvironmentalVariables.LDAP_HOLDER_EDITOR_UTILITY)) {
                        setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.LDAP_HOLDER_EDITOR_UTILITY, HOLDER_UTILITY);
                    } else if (varName.equals(EnvironmentalVariables.SERIAL_NUMBER_STRING)) {
                        if ((serialNumber.intern() != "<EMPTY>") && (serialNumber.trim() != "")) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.SERIAL_NUMBER_STRING, serialNumber);
                        }
                    } else if (varName.equals(EnvironmentalVariables.VALIDITY_PERIOD_STRING)) {
                        if ((jValidityPeriod.getText().intern() != "<EMPTY>") && (!jValidityPeriod.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.VALIDITY_PERIOD_STRING, jValidityPeriod.getText());
                        }
                    } else if (varName.equals(EnvironmentalVariables.VALIDITY_PERIOD_EDITOR_UTILITY)) {
                        setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.VALIDITY_PERIOD_EDITOR_UTILITY, VALIDITY_UTILITY);
                    } else if (varName.equals(EnvironmentalVariables.VERSION_STRING)) {
                        setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.VERSION_STRING, "2");
                    } else if (varName.equals(EnvironmentalVariables.AAIA_LOCATION)) {
                        if ((jProviderURL.getText().intern() != "<EMPTY>") && (!jProviderURL.getText().trim().equals("")) && jCHEntrust.isSelected() && jaaia[0].isSelected()) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.AAIA_LOCATION, "1");
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_HOST)) {
                        if ((jWebDAVHost.getText().intern() != "<EMPTY>") && (!jWebDAVHost.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_HOST, jWebDAVHost.getText());
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_PORT)) {
                        if ((jWebDAVPort.getText().intern() != "<EMPTY>") && (!jWebDAVPort.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_PORT, jWebDAVPort.getText());
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_PROTOCOL)) {
                        if (jWebDAVHttps.isSelected()) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_PROTOCOL, "HTTPS");
                        } else {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_PROTOCOL, "HTTP");
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_P12FILENAME)) {
                        if ((jWebDAVP12Filename.getText().intern() != "<EMPTY>") && (!jWebDAVP12Filename.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_P12FILENAME, jWebDAVP12Filename.getText());
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_P12PASSWORD)) {
                        if ((new String(jWebDAVP12Password.getPassword()).intern() != "<EMPTY>")) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_P12PASSWORD, new String(jWebDAVP12Password.getPassword()));
                        }
                    } else if (varName.equals(EnvironmentalVariables.WEBDAV_SSLCERTIFICATE)) {
                        if ((jWebDAVSSL.getText().intern() != "<EMPTY>")) {
                            setAttributeValue((Element) variableList.item(i), EnvironmentalVariables.WEBDAV_SSLCERTIFICATE, jWebDAVSSL.getText());
                        }
                    } else if (varName.equals(LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE)) {
                        if ((jACType.getText().intern() != "<EMPTY>") && (!jACType.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE, jACType.getText());
                        } else {
                            setAttributeValue((Element) variableList.item(i), LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE, "attributeCertificateAttribute");
                        }
                    } else if (varName.equals("LDAPSavingUtility.ProviderURI")) {
                        if ((jProviderURL.getText().intern() != "<EMPTY>") && (!jProviderURL.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), "LDAPSavingUtility.ProviderURI", jProviderURL.getText());
                        }
                    } else if (varName.equals("LDAPSavingUtility.Login")) {
                        if ((jProviderLogin.getText().intern() != "<EMPTY>")) {
                            setAttributeValue((Element) variableList.item(i), "LDAPSavingUtility.Login", jProviderLogin.getText());
                        }
                    } else if (varName.equals("LDAPSavingUtility.Password")) {
                        String providerPassword = new String(jProviderPassword.getPassword());
                        if (!providerPassword.trim().equals("<EMPTY>")) {
                            setAttributeValue((Element) variableList.item(i), "LDAPSavingUtility.Password", providerPassword);
                        }
                    } else if (varName.equals("PrintableStringEditor.Config")) {
                        setAttributeValue((Element) variableList.item(i), "PrintableStringEditor.Config", "printableString.oid");
                    } else if (varName.equals(DefaultSecurity.DEFAULT_FILE_STRING)) {
                        if (jCHEntrust.isSelected() && (jDefaultProfile.getText().intern() != "<EMPTY>") && (!jDefaultProfile.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), DefaultSecurity.DEFAULT_FILE_STRING, jDefaultProfile.getText());
                        }
                    } else if (varName.equals("DefaultDIS")) {
                        if (jDIS.isSelected() && (jDISAddress.getText().intern() != "<EMPTY>") && (!jDISAddress.getText().trim().equals(""))) {
                            setAttributeValue((Element) variableList.item(i), "DefaultDIS", jDISAddress.getText());
                        }
                    } else if (varName.equals("PMIXMLPolicyEditor.Validate")) {
                        setAttributeValue((Element) variableList.item(i), "PMIXMLPolicyEditor.Validate", " ");
                    } else if (varName.equals("Webdav.revocation.location")) {
                        setAttributeValue((Element) variableList.item(i), "Webdav.revocation.location", RevLocation);
                    } else if (varName.equals("Webdav.certificate.location")) {
                        setAttributeValue((Element) variableList.item(i), "Webdav.certificate.location", CertLocation);
                    }
                }
            }
            Node utilities = Manager.acmConfigComponent.DOM.getElementsByTagName("Utilities").item(0);
            if (utils.get("PRINTABLE_STRING_UTILITY") == null) {
                addItem("Utility", (Element) utilities, "PRINTABLE_STRING_UTILITY", "issrg.acm.extensions.PrintableStringEditor");
                utils.put("PRINTABLE_STRING_UTILITY", "issrg.acm.extensions.PrintableStringEditor");
            }
            if (jCHEntrust.isSelected() && !jDIS.isSelected()) {
                if (utils.get("SIMPLE_SIGNING_UTILITY") == null) {
                    addItem("Utility", (Element) utilities, "SIMPLE_SIGNING_UTILITY", "issrg.acm.extensions.SimpleSigningUtility");
                    utils.put("SIMPLE_SIGNING_UTILITY", "issrg.acm.extensions.SimpleSigningUtility");
                }
            }
            if (jDIS.isSelected()) {
                if (utils.get("ACM_DIS_SIGNING_UTILITY") == null) {
                    addItem("Utility", (Element) utilities, "ACM_DIS_SIGNING_UTILITY", "issrg.acm.extensions.ACMDISSigningUtility");
                    utils.put("ACM_DIS_SIGNING_UTILITY", "issrg.acm.extensions.ACMDISSigningUtility");
                }
            }
            if (utils.get("PERMIS_XML_POLICY_UTILITY") == null) {
                addItem("Utility", (Element) utilities, "PERMIS_XML_POLICY_UTILITY", "issrg.acm.extensions.PMIXMLPolicyEditor");
                utils.put("PERMIS_XML_POLICY_UTILITY", "issrg.acm.extensions.PMIXMLPolicyEditor");
            }
            if ((jProviderURL.getText().intern() != "<EMPTY>") && (!jProviderURL.getText().trim().equals(""))) {
                if (utils.get("HOLDER_UTILITY") == null) {
                    addItem("Utility", (Element) utilities, "HOLDER_UTILITY", HOLDER_UTILITY);
                    utils.put("HOLDER_UTILITY", HOLDER_UTILITY);
                }
            }
            if (utils.get("WEBDAV_HOLDER_UTILITY") == null) {
                addItem("Utility", (Element) utilities, "WEBDAV_HOLDER_UTILITY", WEBDAV_HOLDER_UTILITY);
                utils.put("WEBDAV_HOLDER_UTILITY", WEBDAV_HOLDER_UTILITY);
            }
            if (utils.get("VALIDITY_UTILITY") == null) {
                addItem("Utility", (Element) utilities, "VALIDITY_UTILITY", VALIDITY_UTILITY);
                utils.put("VALIDITY_UTILITY", VALIDITY_UTILITY);
            }
            Manager.acmConfigComponent.saveConfiguration();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(jf, e.getMessage(), "Error in saving the configuration file", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(jf, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setAttributeValue(Element e, String varName, String varValue) {
        String attrs[] = { "Name", "Value" };
        String values[] = { varName, varValue };
        Manager.acmConfigComponent.setAttributeValue(e, attrs, values);
    }

    private void delItem(NodeList variableList, String varName) {
        if (variableList != null & variableList.getLength() > 0) {
            for (int i = 0; i < variableList.getLength(); i++) {
                NamedNodeMap attributes = variableList.item(i).getAttributes();
                String s = attributes.getNamedItem("Name").getNodeValue();
                if (s.equals(varName)) {
                    Manager.acmConfigComponent.deleteItem((Element) variableList.item(i), (Element) variableList.item(i).getParentNode());
                }
            }
        }
    }

    private void addItem(String itemId, Element parentNode, String varName, String varValue) {
        Element childNode = Manager.acmConfigComponent.DOM.createElement(itemId);
        childNode.setAttribute("Name", varName);
        childNode.setAttribute("Value", varValue);
        Manager.acmConfigComponent.addItem(childNode, (Element) parentNode, parentNode.getChildNodes().getLength() + 1);
        vars.put(varName, varValue);
    }

    private void parseConfig(String configFile) throws IOException {
        vars = new Hashtable();
        utils = new Hashtable();
        InputStream is;
        is = new FileInputStream(configFile);
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String s = "";
        final int NOTHING = 0;
        final int VARIABLES = 1;
        final int UTILITIES = 2;
        int state = NOTHING;
        while (in.ready()) {
            s = in.readLine();
            if (s == null) break;
            s = s.trim();
            if (s.intern() == "" || s.startsWith(KernelApplication.CFG_COMMENT1) || s.startsWith(KernelApplication.CFG_COMMENT2)) {
                continue;
            }
            if (s.intern() == KernelApplication.CFG_UTILITIES) {
                state = UTILITIES;
                continue;
            }
            if (s.intern() == KernelApplication.CFG_VARIABLES) {
                state = VARIABLES;
                continue;
            }
            if (s.startsWith("[") && s.endsWith("]")) {
                state = NOTHING;
                continue;
            }
            if (state == VARIABLES) {
                int i = s.indexOf('=');
                String value = "";
                if (i != -1) {
                    value = s.substring(i + 1).trim();
                    s = s.substring(0, i);
                }
                vars.put(s, value);
                continue;
            }
            if (state == UTILITIES) {
                utils.put(s, "ACTIVATED");
                continue;
            }
        }
        is.close();
    }

    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height / 2;
        screenSize.width = screenSize.width / 2;
        size.height = size.height / 2;
        size.width = size.width / 2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x, y);
    }

    public void XMLChanged(XMLChangeEvent ev) {
        ((Manager) jf).parseConfig();
    }
}
