package issrg.acm;

import java.awt.*;
import java.awt.event.*;
import javax.naming.NamingException;
import javax.swing.*;
import java.io.*;
import java.security.*;
import iaik.asn1.*;
import iaik.asn1.structures.*;
import issrg.ac.Extension;
import issrg.ac.ACCreationException;
import issrg.ac.Util;
import java.util.*;
import java.math.BigInteger;
import javax.swing.border.*;
import java.io.BufferedReader;
import issrg.acm.extensions.WebDAVSavingUtility;
import issrg.utils.EnvironmentalVariables;
import issrg.ac.AttCertValidityPeriod;
import issrg.utils.CreateAAIALocation;
import issrg.utils.gui.*;
import issrg.utils.gui.repository.RepositoryMultiBrowser;

/**
 * This is the GUI Frame of the Privilege Allocator. It understands how to use the Registry to obtain
 * extensions and editors.
 *
 * <p>Call initEnvironment to let the frame know who the Registry is, if that changes after calling
 * the constructor. Then call getAC to
 * let the Manager get the user input and return it as an AC.
 *
 * @author A.Otenko
 * @version 1.0
 */
public class KernelFrame extends JDialog {

    public static final String TIMES_SEPARATOR = ";";

    public static final String DIRECTORY_NAMES_SEPARATOR = ",";

    public static final String DIGEST_ALGORITHM_NAME = "SHA1";

    public static final String AC_USE_EXPLICIT = "AC.UseExplicitTagEncoding";

    public static final String SEPARATOR = "|";

    public static final String LOCATION_SEPARATOR = ";";

    public static boolean WEBDAVREV = false;

    public static boolean NOREV = false;

    public static boolean AAIA = false;

    Registry editors;

    Map environment;

    Map vars;

    String configFile;

    SigningUtility signingUtility = null;

    boolean existing = true;

    Vector attributeNamesList = null;

    public Vector extensionNamesList = null;

    JPanel contentPane;

    JDialog dialog;

    JTextField jHolderEntityNameTextField;

    JButton jHolderButton = new JButton();

    JButton jHolderButtonWebDAV = new JButton();

    String jACSerialNumberTextField = "";

    JLabel jLabel13 = new JLabel();

    JTextField jACVPNotBeforeTextField = new JTextField(15);

    JTextField jACVPNotAfterTextField = new JTextField(15);

    JLabel jLabel14 = new JLabel();

    JButton jCreateACButton = new JButton();

    JButton jExit = new JButton();

    JButton edit = new JButton();

    JScrollPane jatcp;

    private String ldapUsername, ldapPassword;

    private JTextField uid;

    private JPasswordField pass;

    private boolean doit = false;

    Object sleep = null;

    byte[] attribute_certificate = null;

    TitledBorder titledBorder1;

    JList jAttributesList = new JList();

    JButton jAttributesButton = new JButton();

    JButton jAttributeEditButton = new JButton();

    JButton jAttributeRemoveButton = new JButton();

    JButton jACValidityButton = new JButton();

    JCheckBox jIssuerNameCheckBox = new JCheckBox();

    JCheckBox jIssuerBCIDCheckBox = new JCheckBox();

    JCheckBox jExtensions = new JCheckBox();

    JCheckBox chkWebdav;

    JFrame parentFrame;

    public KernelFrame(Registry r, JFrame f, String file, boolean existing) {
        super(f);
        editors = r;
        parentFrame = f;
        this.existing = existing;
        configFile = file;
        initEnvironment();
        jHolderEntityNameTextField = new JTextField(40);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initEnvironment() {
        environment = ((Manager) parentFrame).getInitialEnvironment();
        if ((vars = (Hashtable) environment.get(EnvironmentalVariables.VARIABLES_COLLECTION)) == null) {
            vars = new Hashtable();
            environment.put(EnvironmentalVariables.VARIABLES_COLLECTION, vars);
        }
    }

    /**
     * Parses ac parameter and fills the Environment with according values.
     * The values read from ac override those that are possibly there in Environment already.
     * Returns true if it was actually possible to add anything. It is not always a case it added anything at all:
     * ac could be null.
     *
     * @param ac  the AttributeCertificate to parse; can be null.
     * @return    returns false if the pointer to VARIABLES collection is null.
     */
    protected boolean parseACIntoEnvironment(issrg.ac.AttributeCertificate ac) {
        if (vars == null) {
            return false;
        }
        if (ac == null) {
            return true;
        }
        vars.put(EnvironmentalVariables.SIGNATURE_VALUE_BIT_STRING, ac.getSignatureValue());
        if (ac.getACInfo() == null) {
            return true;
        }
        if (ac.getACInfo().getVersion() != null) {
            vars.put(EnvironmentalVariables.VERSION_STRING, ac.getACInfo().getVersion().getVersion().toString());
        }
        if (ac.getACInfo().getHolder() != null) {
            if (ac.getACInfo().getHolder().getEntityName() != null) vars.put(EnvironmentalVariables.HOLDER_NAME_STRING, issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getEntityName()));
            if (ac.getACInfo().getHolder().getBaseCertificateID() != null) vars.put(EnvironmentalVariables.HOLDER_BCID_STRING, ac.getACInfo().getHolder().getBaseCertificateID().getSerial().toString() + Util.SN_NAME_SEPARATOR + issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getBaseCertificateID().getIssuer()));
            if (ac.getACInfo().getHolder().getObjectDigestInfo() != null) vars.put(EnvironmentalVariables.HOLDER_OBJECT_DIGEST_INFO_OBJECTDIGESTINFO, ac.getACInfo().getHolder().getObjectDigestInfo());
        }
        if (ac.getACInfo().getIssuer() != null) {
            String iss = issrg.ac.Util.issuerToString(ac.getACInfo().getIssuer());
            if (iss != null) vars.put(EnvironmentalVariables.ISSUER_NAME_STRING, iss);
            if (ac.getACInfo().getIssuer().getV2Form() != null) {
                if (ac.getACInfo().getIssuer().getV2Form().getBaseCertificateID() != null) vars.put(EnvironmentalVariables.ISSUER_BCID_STRING, ac.getACInfo().getIssuer().getV2Form().getBaseCertificateID().getSerial().toString() + Util.SN_NAME_SEPARATOR + issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV2Form().getBaseCertificateID().getIssuer()));
                if (ac.getACInfo().getIssuer().getV2Form().getObjectDigestInfo() != null) vars.put(EnvironmentalVariables.ISSUER_OBJECT_DIGEST_INFO_OBJECTDIGESTINFO, ac.getACInfo().getIssuer().getV2Form().getObjectDigestInfo());
            }
        }
        if (ac.getACInfo().getIssuerUniqueID() != null) vars.put(EnvironmentalVariables.ISSUER_UID_STRING, new String((byte[]) ac.getACInfo().getIssuerUniqueID().getValue()));
        if (ac.getACInfo().getValidityPeriod() != null) {
            String vt = "";
            if (ac.getACInfo().getValidityPeriod().getNotBefore() != null) {
                vt = issrg.ac.Util.timeToString(ac.getACInfo().getValidityPeriod().getNotBefore().getTime());
            }
            vt += TIMES_SEPARATOR;
            if (ac.getACInfo().getValidityPeriod().getNotAfter() != null) {
                vt += issrg.ac.Util.timeToString(ac.getACInfo().getValidityPeriod().getNotAfter().getTime());
            }
            vars.put(EnvironmentalVariables.VALIDITY_PERIOD_STRING, vt);
        }
        vars.put(EnvironmentalVariables.ATTRIBUTES_VECTOR, ac.getACInfo().getAttributes());
        if (ac.getACInfo().getExtensions() != null) {
            vars.put(EnvironmentalVariables.EXTENSIONS_VECTOR, ac.getACInfo().getExtensions().getValues());
        }
        return true;
    }

    /**
     * This loads things from environment into the input boxes and the Attribute List.
     *
     * @return true if loaded OK
     */
    protected boolean loadFromEnvironment() {
        if (vars == null || editors == null) {
            return false;
        }
        String s = "";
        if (vars.containsKey(EnvironmentalVariables.AAIA_LOCATION)) {
            System.out.println("AAIA Attribute Configured");
            AAIA = true;
        }
        if (vars.containsKey(EnvironmentalVariables.NOREV_LOCATION)) {
            System.out.println("No Revocation Attribute Configured");
            NOREV = true;
        }
        if (vars.containsKey(EnvironmentalVariables.DAVREV_LOCATION)) {
            System.out.println("WebDAV Revocation Attribute Configured");
            WEBDAVREV = true;
        }
        if (NOREV) {
            System.out.println("Unenabling revocation and delegation check boxs");
            chkWebdav.setEnabled(false);
            jExtensions.setEnabled(false);
        }
        if (WEBDAVREV) {
            System.out.println("Ticking DAV delegation checkbox");
            chkWebdav.setSelected(true);
        }
        this.jHolderEntityNameTextField.setText(((s = (String) vars.get(EnvironmentalVariables.HOLDER_NAME_STRING)) == null) ? "" : s);
        s = (((s = (String) vars.get(EnvironmentalVariables.ISSUER_NAME_STRING)) == null) ? "" : s);
        if (s.intern() != "") {
            this.jIssuerNameCheckBox.setSelected(true);
        } else {
            this.jIssuerNameCheckBox.setSelected(false);
        }
        s = (((s = (String) vars.get(EnvironmentalVariables.ISSUER_BCID_STRING)) == null) ? "" : s);
        if (s.intern() != "") {
            this.jIssuerBCIDCheckBox.setSelected(true);
        } else {
            this.jIssuerBCIDCheckBox.setSelected(false);
        }
        if (!this.jIssuerBCIDCheckBox.isSelected() && !this.jIssuerNameCheckBox.isSelected()) {
            this.jIssuerNameCheckBox.setSelected(true);
        }
        this.jACSerialNumberTextField = (((s = (String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING)) == null) ? "" : s);
        this.jACVPNotBeforeTextField.setText("");
        this.jACVPNotAfterTextField.setText("");
        s = (String) vars.get(EnvironmentalVariables.VALIDITY_PERIOD_STRING);
        s = (String) vars.get(EnvironmentalVariables.VALIDITY_PERIOD_STRING);
        if (s != null) {
            int i = s.indexOf(this.TIMES_SEPARATOR);
            if (i > 0) {
                this.jACVPNotBeforeTextField.setText(s.substring(0, i));
            }
            i += this.TIMES_SEPARATOR.length();
            if (i < s.length()) {
                this.jACVPNotAfterTextField.setText(s.substring(i));
            }
        }
        Vector attributes = collapseAttributes((Vector) (vars.get(EnvironmentalVariables.ATTRIBUTES_VECTOR)));
        vars.put(EnvironmentalVariables.ATTRIBUTES_VECTOR, attributes);
        attributeNamesList = new Vector();
        Object[] o = attributes.toArray();
        for (int i = 0; i < o.length; i++) {
            String type = ((issrg.ac.Attribute) o[i]).getType();
            PrivilegeEditor pe = (PrivilegeEditor) (editors.getCollection(EnvironmentalVariables.ATTRIBUTE_EDITORS_COLLECTION).get(type));
            if (pe == null) {
                attributeNamesList.add(type + " : [No editor found]");
            } else {
                Vector v = (Vector) vars.get(EnvironmentalVariables.ATTRIBUTES_VECTOR);
                String str = createRoleValuesString(type, v);
                if (str == null) return false;
                if (str.equals("")) str = ""; else str = "=" + str;
                attributeNamesList.add(pe.getName() + str);
            }
        }
        Vector extensions = (Vector) (vars.get(EnvironmentalVariables.EXTENSIONS_VECTOR));
        extensionNamesList = new Vector();
        if (extensions != null) {
            extensionNamesList = new Vector(extensions);
            updateCheckBoxAndButton();
        }
        updateLists();
        return true;
    }

    private String createRoleValuesString(String type, Vector values) {
        StringBuffer roleValuesString = new StringBuffer();
        for (int i = 0; i < values.size(); i++) {
            issrg.ac.Attribute attr = (issrg.ac.Attribute) values.get(i);
            if (!attr.getType().equals(type)) continue;
            Vector v = attr.getValues();
            if (v.size() > 0) {
                for (int k = 0; k < v.size(); k++) {
                    try {
                        Object o = v.get(k);
                        issrg.ac.attributes.PermisRole av = null;
                        if (o instanceof issrg.ac.attributes.PermisRole) {
                            av = new issrg.ac.attributes.PermisRole((issrg.ac.attributes.PermisRole) o);
                        } else {
                            av = new issrg.ac.attributes.PermisRole((issrg.ac.AttributeValue) o);
                        }
                        if (!av.getRoleValue().startsWith("<?xml")) roleValuesString.append(av.getRoleValue()).append("+");
                    } catch (Exception e) {
                        return null;
                    }
                }
                if (roleValuesString.length() > 0) roleValuesString.deleteCharAt(roleValuesString.length() - 1);
            }
        }
        return roleValuesString.toString();
    }

    private String createRoleValuesString(Vector values) {
        StringBuffer buf = new StringBuffer();
        try {
            if (values.size() > 0) {
                for (int i = 0; i < values.size(); i++) {
                    issrg.ac.attributes.PermisRole av = (issrg.ac.attributes.PermisRole) values.get(i);
                    buf.append(av.getRoleValue()).append("+");
                }
                buf.deleteCharAt(buf.length() - 1);
            }
        } catch (java.lang.ClassCastException cce) {
        }
        return buf.toString();
    }

    /**
     * This saves things to environment from the input boxes and the Attribute List.
     *
     * @return true if saved OK
     */
    protected boolean saveToEnvironment() {
        if (vars == null || editors == null) {
            return false;
        }
        vars.put(EnvironmentalVariables.VERSION_STRING, issrg.ac.AttCertVersion.V2.toString());
        vars.put(EnvironmentalVariables.HOLDER_NAME_STRING, this.jHolderEntityNameTextField.getText());
        vars.put(EnvironmentalVariables.VALIDITY_PERIOD_STRING, this.jACVPNotBeforeTextField.getText() + this.TIMES_SEPARATOR + this.jACVPNotAfterTextField.getText());
        return true;
    }

    private void jbInit() throws Exception {
        JPanel pHolderN, pHolderS, pValidityN, pValidityS;
        JPanel pAttributesN, pAttributesS;
        JPanel pData, pFields, pButtons;
        pHolderN = new JPanel();
        pHolderS = new JPanel();
        pValidityN = new JPanel();
        pValidityS = new JPanel();
        pAttributesN = new JPanel();
        pAttributesS = new JPanel();
        pData = new JPanel();
        pFields = new JPanel();
        pButtons = new JPanel();
        JPanel pHolder = new JPanel();
        JPanel pIssuer = new JPanel();
        JPanel pValidity = new JPanel();
        JPanel pAttributes = new JPanel();
        pHolder.setLayout(new BorderLayout());
        pHolder.setBorder(new TitledBorder("Holder"));
        pIssuer.setLayout(new BorderLayout());
        pIssuer.setBorder(new TitledBorder("Information to include about the issuer"));
        pValidity.setLayout(new BorderLayout());
        pValidity.setBorder(new TitledBorder("Validity information"));
        pAttributes.setLayout(new BorderLayout());
        pAttributes.setBorder(new TitledBorder("Attributes"));
        pFields.setLayout(new BorderLayout());
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        this.setTitle("Management Tool");
        pHolderN.add(new JLabel("Holder Name"));
        pHolderN.add(jHolderEntityNameTextField);
        pHolder.add("North", pHolderN);
        Map editorMap = editors.getCollection(EnvironmentalVariables.UTILITIES_COLLECTION);
        if (editorMap != null) {
            JComponent contentPanel;
            Editor holderEditor = (Editor) editorMap.get(EnvironmentalVariables.HOLDER_EDITOR_UTILITY);
            Editor ldapEditor = (Editor) editorMap.get(EnvironmentalVariables.LDAP_HOLDER_EDITOR_UTILITY);
            Editor webDAVEditor = (Editor) editorMap.get(EnvironmentalVariables.WEBDAV_HOLDER_EDITOR_UTILITY);
            if (holderEditor != null) {
                if (ldapEditor != null) {
                    ((ACMRepositoryBrowserGUI) holderEditor).addBrowser((JPanel) ldapEditor.getContentPanel(), ((RepositoryMultiBrowser) ldapEditor).getBrowserName(), ((RepositoryMultiBrowser) ldapEditor).getBrowserLogo());
                }
                if (webDAVEditor != null) {
                    ((ACMRepositoryBrowserGUI) holderEditor).addBrowser((RepositoryMultiBrowser) webDAVEditor);
                }
                contentPanel = holderEditor.getContentPanel();
                contentPanel.setPreferredSize(new Dimension(70, 200));
                pHolder.add("South", contentPanel);
                saveToEnvironment();
                ldapEditor.edit(parentFrame, this, environment);
                webDAVEditor.edit(parentFrame, this, environment);
            }
        }
        jIssuerNameCheckBox.setSelected(true);
        jIssuerNameCheckBox.setText("Issuer Name");
        jIssuerBCIDCheckBox.setText("Issuer Base Certificate ID");
        pIssuer.add("North", jIssuerNameCheckBox);
        pIssuer.add("South", jIssuerBCIDCheckBox);
        pValidityN.add(new JLabel("Valid from "));
        pValidityN.add(jACVPNotBeforeTextField);
        pValidityN.add(new JLabel(" to "));
        pValidityN.add(jACVPNotAfterTextField);
        jACValidityButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jACValidityButton_actionPerformed(e);
            }
        });
        jACValidityButton.setText("View Calendar...");
        pValidityS.add(jACValidityButton);
        pValidity.add("North", pValidityN);
        pValidity.add("South", pValidityS);
        pFields.add("North", pHolder);
        pFields.add("Center", pIssuer);
        pFields.add("South", pValidity);
        pData.add("West", pFields);
        jAttributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jAttributesList.setFixedCellWidth(200);
        jAttributesList.setVisibleRowCount(12);
        jatcp = new JScrollPane(jAttributesList);
        pAttributesN.add(jatcp);
        jAttributesButton.setText("New...");
        jAttributesButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jAttributesButton_actionPerformed(e);
            }
        });
        jAttributeEditButton.setText("Edit...");
        jAttributeEditButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jAttributeEditButton_actionPerformed(e);
            }
        });
        jAttributeRemoveButton.setText("Remove");
        jAttributeRemoveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jAttributeRemoveButton_actionPerformed(e);
            }
        });
        pAttributesS.add(jAttributesButton);
        pAttributesS.add(jAttributeRemoveButton);
        pAttributesS.add(jAttributeEditButton);
        pAttributes.add("North", pAttributesN);
        pAttributes.add("Center", pAttributesS);
        JPanel pExtensions = new JPanel();
        pExtensions.setBorder(new TitledBorder("Extensions"));
        pExtensions.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JLabel extensionLabel1 = new JLabel("Can the holder delegate this(these)");
        JLabel extensionLabel2 = new JLabel("attribute(s) to someone else?");
        JLabel lblUuseWebdavExt = new JLabel("Use WebDAV as store and revocation?");
        chkWebdav = new JCheckBox();
        jExtensions.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jExtensions_stateChanged(e);
            }
        });
        edit = new JButton("Edit...");
        edit.setEnabled(false);
        edit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jExtensions_Edit(e);
            }
        });
        c.gridx = 0;
        c.gridy = 0;
        pExtensions.add(lblUuseWebdavExt, c);
        c.gridx = 1;
        pExtensions.add(chkWebdav, c);
        c.gridx = 0;
        c.gridy = 1;
        pExtensions.add(extensionLabel1, c);
        c.gridx = 1;
        c.gridy = 1;
        pExtensions.add(jExtensions, c);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pExtensions.add(extensionLabel2, c);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.CENTER;
        pExtensions.add(edit, c);
        pAttributes.add("South", pExtensions);
        pData.add("East", pAttributes);
        jCreateACButton.setText("Generate and save");
        jCreateACButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jCreateACButton_actionPerformed(e);
            }
        });
        jExit.setText("Cancel");
        jExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        pButtons.add(jCreateACButton);
        pButtons.add(jExit);
        contentPane.add("North", pData);
        contentPane.add("South", pButtons);
        pack();
        validate();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - this.getWidth()) / 2, (d.height - this.getHeight()) / 2);
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
        }
    }

    /**
     * It returns a byte array, representing the BER encoded signed AC.
     *
     * @param ac is the AttributeCertificate to edit; pass null if you want a new AC
     *
     * @return the byte array of the BER encoded signed AC
     */
    public byte[] getAC(issrg.ac.AttributeCertificate ac) {
        attribute_certificate = null;
        this.parseACIntoEnvironment(ac);
        if (this.vars.get(AC_USE_EXPLICIT) != null) {
            issrg.ac.AttributeCertificate.setImplicitEncoding(false);
        }
        this.loadFromEnvironment();
        this.vars.put(EnvironmentalVariables.VALIDITY_PERIOD_STRING, this.jACVPNotBeforeTextField.getText() + this.TIMES_SEPARATOR + this.jACVPNotAfterTextField.getText());
        return attribute_certificate;
    }

    private void updateLists() {
        jAttributesList.setListData(attributeNamesList);
    }

    protected void jCreateACButton_actionPerformed(ActionEvent e) {
        try {
            attribute_certificate = generateAC();
            if (attribute_certificate != null) {
                if (attribute_certificate.length > 0) {
                    if (chkWebdav.isSelected()) {
                        SavingUtility su = new WebDAVSavingUtility(null, true, vars);
                        boolean redo;
                        do {
                            redo = false;
                            try {
                                su.save(parentFrame, attribute_certificate, vars);
                            } catch (ACCreationException acce) {
                                issrg.utils.Util.bewail(acce.getMessage(), acce, this);
                                redo = javax.swing.JOptionPane.showConfirmDialog(this, "Try to save again?", "Confirm", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION;
                            }
                        } while (redo);
                    } else {
                        SavingUtility su;
                        su = (SavingUtility) (editors.getCollection(EnvironmentalVariables.UTILITIES_COLLECTION).get(EnvironmentalVariables.SAVING_UTILITY));
                        System.out.println("SU er:" + su.getClass());
                        if (vars == null || (su = (SavingUtility) (editors.getCollection(EnvironmentalVariables.UTILITIES_COLLECTION).get(EnvironmentalVariables.SAVING_UTILITY))) == null) {
                            su = new issrg.acm.extensions.MultiChoiceSavingUtility(editors);
                        }
                        boolean redo;
                        do {
                            redo = false;
                            try {
                                su.save(parentFrame, attribute_certificate, vars);
                            } catch (ACCreationException acce) {
                                issrg.utils.Util.bewail(acce.getMessage(), acce, this);
                                redo = javax.swing.JOptionPane.showConfirmDialog(this, "Try to save again?", "Confirm", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION;
                            }
                        } while (redo);
                    }
                    String nextHash = calculateNexthash((String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING));
                    vars.put(EnvironmentalVariables.SERIAL_NUMBER_STRING, nextHash);
                    storeSerialNumber();
                }
                if (!existing) {
                    if (javax.swing.JOptionPane.showConfirmDialog(this, new String("Create another AC?"), "Confirm", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE) == javax.swing.JOptionPane.YES_OPTION) {
                        initEnvironment();
                        getAC(null);
                        toFront();
                    } else setVisible(false);
                } else setVisible(false);
            }
        } catch (IllegalInputException iie) {
            issrg.utils.Util.bewail(iie.getMessage(), null, this);
        } catch (ACCreationException acce) {
            acce.printStackTrace();
            issrg.utils.Util.bewail(acce.getMessage(), acce, this);
        }
    }

    private void storeSerialNumber() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            StringWriter newFile = new StringWriter();
            BufferedWriter writer = new BufferedWriter(newFile);
            String line = reader.readLine();
            boolean saved = false;
            String newSerial = "AC.SerialNumber=" + (String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING);
            while (line != null) {
                if (line.intern().startsWith("AC.SerialNumber=")) {
                    line = newSerial;
                    saved = true;
                }
                writer.write(line);
                writer.newLine();
                line = reader.readLine();
            }
            if (!saved) {
                writer.write("[variables]");
                writer.newLine();
                writer.write(newSerial);
                writer.newLine();
            }
            reader.close();
            writer.flush();
            writer.close();
            BufferedWriter finalFile = new BufferedWriter(new FileWriter(configFile));
            finalFile.write(newFile.toString());
            finalFile.flush();
            finalFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String calculateNexthash(String string) {
        String s;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            BigInteger bi = new BigInteger(string, 16);
            md.update(bi.toByteArray());
            byte[] result = md.digest();
            BigInteger bir = new BigInteger(result);
            bir = bir.abs();
            s = bir.toString(16);
        } catch (Exception e) {
            s = "NULL";
        }
        return s;
    }

    /**
     * Generates an AC, getting data from the input fields and Environment.
     * Particularly, V2 AC will be generated, if V1orV2 radiobutton is selected,
     * and V2_FLAG is present in the environment, and its value as boolean is true.
     *
     * <p>If, however, Force V1 radio-button is selected, but some V2 features or V2_FLAG
     * are also present, a dialog box will pop-up, for user to confirm either use v2
     * or v1 AC.
     */
    protected byte[] generateAC() throws ACCreationException {
        iaik.utils.RFC2253NameParser.register("SERIALNUMBER", iaik.asn1.ObjectID.serialNumber);
        boolean v2allowed = true;
        Object o;
        boolean v2wanted = ((o = vars.get(EnvironmentalVariables.V2_FLAG)) == null) ? false : ((Boolean) o).booleanValue();
        java.math.BigInteger ACSerialNumber;
        try {
            ACSerialNumber = new java.math.BigInteger(((String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING)), 16);
        } catch (NumberFormatException nfe) {
            try {
                ACSerialNumber = new java.math.BigInteger((String) vars.get(EnvironmentalVariables.SERIAL_NUMBER_STRING));
            } catch (NumberFormatException e) {
                throw new IllegalInputException("AC Serial Number must be a valid Integer value");
            }
        }
        ACSerialNumber = ACSerialNumber.abs();
        if (this.jHolderEntityNameTextField.getText().equals("") && this.vars.get(EnvironmentalVariables.HOLDER_OBJECT_DIGEST_INFO_OBJECTDIGESTINFO) == null) {
            throw new IllegalInputException("Please specify one of the optional parameters for the Holder");
        }
        if ((!this.jIssuerNameCheckBox.isSelected() && !this.jIssuerBCIDCheckBox.isSelected()) && v2allowed) {
            throw new IllegalInputException("Please select one of the optional parameters for the Issuer for inclusion");
        }
        if (!this.jIssuerNameCheckBox.isSelected() && !v2allowed) {
            throw new IllegalInputException("Please select the name for the Issuer for inclusion:\nVersion 1 AC is enforced");
        }
        issrg.ac.AttCertValidityPeriod validity_period = getValidityPeriod();
        Vector attributes = collapseAttributes((Vector) (vars.get(EnvironmentalVariables.ATTRIBUTES_VECTOR)));
        if (attributes.size() == 0) {
            throw new IllegalInputException("Attribute set cannot be empty");
        }
        v2wanted = true;
        if (v2wanted && !v2allowed) {
            throw new IllegalInputException("Please explicitly unselect Version 2 features or enable creating V2 ACs");
        }
        iaik.asn1.structures.GeneralNames hn = jHolderEntityNameTextField.getText().intern() == "" ? null : issrg.ac.Util.buildGeneralNames(jHolderEntityNameTextField.getText());
        issrg.ac.Holder holder = new issrg.ac.Holder(null, hn, getHolderDigestInfo(v2wanted));
        issrg.ac.AttCertIssuer issuer;
        signingUtility = issrg.acm.Util.getSigningUtility(editors);
        try {
            signingUtility.login(parentFrame, vars);
            java.security.cert.X509Certificate signerPKC = signingUtility.getVerificationCertificate();
            String subjectDN;
            String issuerDN;
            if (signerPKC instanceof iaik.x509.X509Certificate) {
                try {
                    subjectDN = ((iaik.asn1.structures.Name) signerPKC.getSubjectDN()).getRFC2253String();
                    issuerDN = ((iaik.asn1.structures.Name) signerPKC.getIssuerDN()).getRFC2253String();
                } catch (iaik.utils.RFC2253NameParserException rnpe) {
                    throw new ACCreationException("Failed to decode DNs", rnpe);
                }
            } else {
                subjectDN = signerPKC.getSubjectDN().getName();
                issuerDN = signerPKC.getIssuerDN().getName();
            }
            issrg.ac.V2Form signer = new issrg.ac.V2Form(issrg.ac.Util.buildGeneralNames(subjectDN), new issrg.ac.IssuerSerial(issrg.ac.Util.buildGeneralNames(issuerDN), signerPKC.getSerialNumber(), null), null);
            Vector ext = (Vector) vars.get(EnvironmentalVariables.EXTENSIONS_VECTOR);
            issrg.ac.Extensions extensions;
            if (ext == null) {
                ext = new Vector();
            }
            if (NOREV) {
                System.out.println("NoRevocation extension being created");
                String location = (String) vars.get(EnvironmentalVariables.HOLDER_EDITOR_UTILITY_SERVER);
                issrg.ac.extensions.NoRevocation norev = new issrg.ac.extensions.NoRevocation(location, subjectDN, ACSerialNumber);
                ext.add(norev);
                extensions = new issrg.ac.Extensions(ext);
            } else {
                if (chkWebdav.isSelected()) {
                    System.out.println("Adding WebDAV revocation extension");
                    issrg.ac.extensions.AuthorityInformationAccess aia = new issrg.ac.extensions.AuthorityInformationAccess(vars, subjectDN, jHolderEntityNameTextField.getText().intern(), ACSerialNumber);
                    ext.add(aia);
                    extensions = new issrg.ac.Extensions(ext);
                } else {
                    System.out.println("Just AAIA extension");
                    if (ext != null && !ext.isEmpty()) {
                        for (Iterator i = ext.iterator(); i.hasNext(); ) {
                            Extension e = (Extension) i.next();
                            if (e instanceof issrg.ac.extensions.AttributeAuthorityInformationAccess) {
                                i.remove();
                                break;
                            }
                        }
                    }
                    String lFlag = (String) vars.get(EnvironmentalVariables.AAIA_LOCATION);
                    if ((lFlag != null) && (!lFlag.equals(""))) {
                        String location = (String) vars.get(EnvironmentalVariables.HOLDER_EDITOR_UTILITY_SERVER);
                        String aaiaLocation = CreateAAIALocation.createLocation(location, subjectDN);
                        if (aaiaLocation != null) {
                            issrg.ac.extensions.AttributeAuthorityInformationAccess e = new issrg.ac.extensions.AttributeAuthorityInformationAccess(new String[] { aaiaLocation });
                            if (ext == null) ext = new Vector();
                            ext.add(e);
                        }
                    }
                }
                if (ext == null) ext = new Vector();
                extensions = new issrg.ac.Extensions(ext);
            }
            if (!this.jIssuerNameCheckBox.isSelected()) signer.setIssuerName(null);
            if (!this.jIssuerBCIDCheckBox.isSelected()) signer.setBaseCertificateID(null);
            signer.setObjectDigestInfo(null);
            issuer = new issrg.ac.AttCertIssuer(v2wanted ? null : signer.getIssuerName(), v2wanted ? signer : null);
            byte[] bt = signerPKC.getSigAlgParams();
            ASN1Object algParams = bt == null ? null : iaik.asn1.DerCoder.decode(bt);
            AlgorithmID signatureAlg = new iaik.asn1.structures.AlgorithmID(new iaik.asn1.ObjectID(signingUtility.getSigningAlgorithmID()));
            issrg.ac.AttributeCertificateInfo aci = new issrg.ac.AttributeCertificateInfo(new issrg.ac.AttCertVersion(v2wanted ? issrg.ac.AttCertVersion.V2 : issrg.ac.AttCertVersion.DEFAULT), holder, issuer, signatureAlg, ACSerialNumber, validity_period, attributes, null, extensions);
            try {
                saveToEnvironment();
                byte[] b = aci.getEncoded();
                byte[] certificate = new issrg.ac.AttributeCertificate(aci, signatureAlg, new BIT_STRING(signingUtility.sign(b))).getEncoded();
                if (!vars.containsKey("DefaultDIS")) {
                    return certificate;
                } else {
                    String endpoint = ((String) vars.get("DefaultDIS")).trim();
                    Map utilities = editors.getCollection(EnvironmentalVariables.UTILITIES_COLLECTION);
                    if (utilities != null) {
                        issrg.acm.SSLConnection uti = (issrg.acm.SSLConnection) utilities.get(EnvironmentalVariables.SIGNING_UTILITY);
                        if (uti != null) {
                            String message = uti.sign(endpoint, certificate, vars);
                            if (message != null) {
                                if (message.indexOf(issrg.dis.Comm.PUBLISH, 0) == -1) {
                                    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
                                    return new byte[0];
                                } else {
                                    showReturnMessage(message);
                                    return new byte[0];
                                }
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(null, "Error - ACM can not load necessary utilities", "Error", JOptionPane.ERROR_MESSAGE);
                    return new byte[0];
                }
            } catch (Throwable e) {
                throw new ACCreationException(e.getMessage(), e);
            } finally {
                signingUtility.logout(parentFrame, vars);
            }
        } catch (iaik.asn1.CodingException ce) {
            throw new ACCreationException(ce.getMessage(), ce);
        } catch (issrg.security.SecurityException se) {
            throw new ACCreationException(se.getMessage(), se);
        }
    }

    protected void showReturnMessage(String msg) {
        Enumeration e = new java.util.StringTokenizer(msg, SEPARATOR);
        Vector returnArray = new Vector();
        while (e.hasMoreElements()) {
            returnArray.add(e.nextElement());
        }
        Object a = returnArray.remove(0);
        String roleTypesAndValues = (String) returnArray.elementAt(1);
        Enumeration ee = new java.util.StringTokenizer(roleTypesAndValues, "+");
        Vector roleTypes = new Vector();
        while (ee.hasMoreElements()) {
            roleTypes.add(ee.nextElement());
        }
        Vector roleValues = new Vector();
        int numberOfTypes = roleTypes.size();
        for (int i = 0; i < numberOfTypes; i++) {
            String type = (String) roleTypes.elementAt(i);
            int ii = type.indexOf(":");
            String values = type.substring(ii + 1).trim();
            roleValues.add(i, values);
            type = type.substring(0, ii);
            roleTypes.setElementAt(type, i);
        }
        final JDialog dialog = new JDialog(this, "Information received", true);
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 10);
        c.anchor = GridBagConstraints.LINE_START;
        contentPane.add(new JLabel("Holder"), c);
        c.gridx = 1;
        c.gridy = 0;
        JLabel holder = new JLabel((String) returnArray.elementAt(0));
        contentPane.add(holder, c);
        for (int i = 0; i < numberOfTypes; i++) {
            c.gridx = 0;
            c.gridy = i + 1;
            JLabel rt = new JLabel((String) roleTypes.elementAt(i));
            contentPane.add(rt, c);
            c.gridx = 1;
            c.gridy = i + 1;
            JLabel vl = new JLabel((String) roleValues.elementAt(i));
            contentPane.add(vl, c);
        }
        c.gridx = 0;
        c.gridy = numberOfTypes + 1;
        contentPane.add(new JLabel("Valid From "), c);
        c.gridx = 1;
        c.gridy = numberOfTypes + 1;
        JLabel from = new JLabel((String) returnArray.elementAt(2));
        contentPane.add(from, c);
        c.gridx = 0;
        c.gridy = numberOfTypes + 2;
        contentPane.add(new JLabel("Valid To "), c);
        c.gridx = 1;
        c.gridy = numberOfTypes + 2;
        JLabel to = new JLabel((String) returnArray.elementAt(3));
        contentPane.add(to, c);
        c.gridx = 0;
        c.gridy = numberOfTypes + 3;
        contentPane.add(new JLabel("Can Holder use this Attribute certificate? "), c);
        c.gridx = 1;
        c.gridy = numberOfTypes + 3;
        JLabel canUse = new JLabel((String) returnArray.elementAt(4));
        contentPane.add(canUse, c);
        c.gridx = 0;
        c.gridy = numberOfTypes + 4;
        contentPane.add(new JLabel("Delegation depth "), c);
        c.gridx = 1;
        c.gridy = numberOfTypes + 4;
        String depth = (String) returnArray.elementAt(5);
        JLabel canDelegate;
        if (depth.equals("-1")) {
            canDelegate = new JLabel("NOT ALLOWED");
        } else if (depth.equals("0")) {
            canDelegate = new JLabel("UNLIMITED");
        } else canDelegate = new JLabel(depth);
        contentPane.add(canDelegate, c);
        c.gridx = 0;
        c.gridy = numberOfTypes + 5;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 10, 0, 10);
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        contentPane.add(okButton, c);
        dialog.pack();
        dialog.validate();
        Dimension screenSize = parentFrame.getToolkit().getScreenSize();
        Dimension size = dialog.getSize();
        screenSize.height = screenSize.height / 2;
        screenSize.width = screenSize.width / 2;
        size.height = size.height / 2;
        size.width = size.width / 2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        dialog.setLocation(x, y);
        dialog.setVisible(true);
    }

    protected void showDISLoginDialog() {
        doit = false;
        dialog = new JDialog(this, "Enter your Username and password for the DIS service", true);
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 10);
        c.anchor = GridBagConstraints.LINE_START;
        contentPane.add(new JLabel("Your username for the DIS service: "), c);
        uid = new JTextField(15);
        c.gridx = 1;
        c.gridy = 0;
        contentPane.add(uid, c);
        c.gridx = 0;
        c.gridy = 1;
        contentPane.add(new JLabel("Your password for the DIS service: "), c);
        pass = new JPasswordField(15);
        c.gridx = 1;
        c.gridy = 1;
        contentPane.add(pass, c);
        c.gridx = 0;
        c.gridy = 2;
        contentPane.add(new JLabel("Store the AC automatically"), c);
        c.gridx = 0;
        c.gridy = 3;
        contentPane.add(new JLabel("Let me choose where to store the AC"), c);
        final JRadioButton defaultCheck = new JRadioButton();
        defaultCheck.setSelected(true);
        final JRadioButton manualCheck = new JRadioButton();
        ButtonGroup group = new ButtonGroup();
        group.add(defaultCheck);
        group.add(manualCheck);
        c.gridx = 1;
        c.gridy = 2;
        contentPane.add(defaultCheck, c);
        c.gridx = 1;
        c.gridy = 3;
        contentPane.add(manualCheck, c);
        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.CENTER;
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ldapUsername = uid.getText().intern();
                ldapPassword = new String(pass.getPassword());
                dialog.dispose();
                doit = true;
            }
        });
        contentPane.add(okButton, c);
        c.gridx = 1;
        c.gridy = 4;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                doit = false;
            }
        });
        contentPane.add(cancelButton, c);
        dialog.pack();
        dialog.validate();
        Dimension screenSize = parentFrame.getToolkit().getScreenSize();
        Dimension size = dialog.getSize();
        screenSize.height = screenSize.height / 2;
        screenSize.width = screenSize.width / 2;
        size.height = size.height / 2;
        size.width = size.width / 2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        dialog.setLocation(x, y);
        dialog.setVisible(true);
    }

    protected void jAttributesButton_actionPerformed(ActionEvent e) {
        createASN1(EnvironmentalVariables.ATTRIBUTE_EDITORS_COLLECTION, EnvironmentalVariables.ATTRIBUTES_VECTOR, attributeNamesList);
    }

    protected void jAttributeEditButton_actionPerformed(ActionEvent e) {
        int idx = 0;
        if ((idx = jAttributesList.getSelectedIndex()) >= 0) {
            Vector v = (Vector) vars.get(EnvironmentalVariables.ATTRIBUTES_VECTOR);
            issrg.ac.Attribute attr = (issrg.ac.Attribute) v.get(idx);
            PrivilegeEditor pe = (PrivilegeEditor) (editors.getCollection(EnvironmentalVariables.ATTRIBUTE_EDITORS_COLLECTION).get(attr.getType()));
            if (pe == null) {
                javax.swing.JOptionPane.showMessageDialog(this, new String("No appropriate editor found"), "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } else {
                try {
                    attr = (issrg.ac.Attribute) pe.run(parentFrame, vars, attr, editors);
                    if (attr != null) {
                        v.set(idx, attr);
                        Vector values = attr.getValues();
                        String str = createRoleValuesString(values);
                        if (str.equals("")) attributeNamesList.set(idx, pe.getName()); else attributeNamesList.set(idx, pe.getName().concat("=").concat(str));
                        updateLists();
                    }
                } catch (IllegalInputException iie) {
                    issrg.utils.Util.bewail(iie.getMessage(), null, this);
                } catch (ACCreationException acce) {
                    issrg.utils.Util.bewail(acce.getMessage(), acce, this);
                }
            }
        }
    }

    protected void jAttributeRemoveButton_actionPerformed(ActionEvent e) {
        removeASN1(jAttributesList, this.attributeNamesList, EnvironmentalVariables.ATTRIBUTES_VECTOR);
    }

    protected issrg.ac.ObjectDigestInfo getHolderDigestInfo(boolean v2) {
        if (!v2) {
            return null;
        }
        return (issrg.ac.ObjectDigestInfo) vars.get(EnvironmentalVariables.HOLDER_OBJECT_DIGEST_INFO_OBJECTDIGESTINFO);
    }

    protected issrg.ac.AttCertValidityPeriod getValidityPeriod() throws ACCreationException {
        return new issrg.ac.AttCertValidityPeriod(issrg.ac.Util.buildGeneralizedTime(this.jACVPNotBeforeTextField.getText()), issrg.ac.Util.buildGeneralizedTime(this.jACVPNotAfterTextField.getText()));
    }

    public Vector expandAttributes(Vector src) {
        if (src == null) {
            src = new Vector();
        }
        Object[] o = src.toArray();
        Vector result = new Vector();
        for (int i = 0; i < o.length; i++) {
            issrg.ac.Attribute attribute = (issrg.ac.Attribute) o[i];
            while (!attribute.getValues().isEmpty()) {
                Vector v = new Vector();
                v.add(attribute.getValues().firstElement());
                result.add(new issrg.ac.Attribute(attribute.getType(), v));
                attribute.getValues().remove(attribute.getValues().firstElement());
            }
        }
        return result;
    }

    public Vector collapseAttributes(Vector src) {
        if (src == null) {
            src = new Vector();
        }
        Object[] o = src.toArray();
        Vector result = new Vector();
        for (int i = 0; i < o.length; i++) {
            String a = ((issrg.ac.Attribute) o[i]).getType();
            Vector v = ((issrg.ac.Attribute) o[i]).getValues();
            for (int j = i; j-- > 0; ) {
                if (o[j] != null && (((issrg.ac.Attribute) o[j]).getType().equals(a))) {
                    ((issrg.ac.Attribute) o[j]).getValues().addAll(v);
                    o[i] = null;
                    break;
                }
            }
            if (o[i] != null) {
                result.add(o[i]);
            }
        }
        return result;
    }

    protected void createASN1(String editorsCollection, String things, Vector dataList) {
        try {
            Map edits = editors.getCollection(editorsCollection);
            if (edits == null) {
                throw new ACCreationException("No appropriate Editors found");
            }
            Object[] pes = edits.values().toArray();
            String[] lines = new String[pes.length];
            for (int i = 0; i < lines.length; i++) {
                lines[i] = ((PrivilegeEditor) pes[i]).getName();
            }
            final JDialog d = new JDialog(this, "Choose the attribute to assign:", true);
            d.setResizable(false);
            int w = 400;
            int h = 400;
            java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            d.setBounds((dim.width - w) / 2, (dim.height - h) / 2, w, h);
            java.awt.Container p = d.getContentPane();
            final JList list = new JList(lines);
            JScrollPane sp = new JScrollPane(list);
            JPanel panel = new JPanel(new java.awt.FlowLayout());
            sp.setBounds(0, 0, w, h - 60);
            panel.setBounds(0, h - 60, w, h - 15);
            JButton b = new JButton("OK");
            boolean isSelected = false;
            b.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    if (list.getSelectedIndex() >= 0) {
                        d.setVisible(false);
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(d, "Nothing is selected", "Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
            panel.add(b);
            b = new JButton("Cancel");
            b.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent ae) {
                    list.setEnabled(false);
                    d.setVisible(false);
                }
            });
            panel.add(b);
            p.setLayout(null);
            p.add(sp);
            p.add(panel);
            list.addMouseListener(new java.awt.event.MouseListener() {

                public void mouseClicked(java.awt.event.MouseEvent ae) {
                    if (ae.getClickCount() == 2 && list.getSelectedIndex() >= 0) {
                        d.setVisible(false);
                    }
                }

                public void mouseEntered(java.awt.event.MouseEvent ae) {
                }

                public void mouseExited(java.awt.event.MouseEvent ae) {
                }

                public void mousePressed(java.awt.event.MouseEvent ae) {
                }

                public void mouseReleased(java.awt.event.MouseEvent ae) {
                }
            });
            list.addKeyListener(new java.awt.event.KeyListener() {

                public void keyPressed(java.awt.event.KeyEvent ae) {
                }

                public void keyReleased(java.awt.event.KeyEvent ae) {
                }

                public void keyTyped(java.awt.event.KeyEvent ae) {
                    if (ae.getKeyChar() == '\n' && list.getSelectedIndex() >= 0) {
                        d.setVisible(false);
                    }
                }
            });
            d.setVisible(true);
            if (list.isEnabled()) {
                PrivilegeEditor pe = (PrivilegeEditor) pes[list.getSelectedIndex()];
                Vector collection = (Vector) vars.get(things);
                if (collection == null) {
                    vars.put(things, collection = new Vector());
                }
                int idx;
                if ((idx = dataList.indexOf(pe.getName())) >= 0) {
                    if (javax.swing.JOptionPane.showConfirmDialog(this, new String("Do you want to replace the attribute with a new one?\nPress Ok if you want to replace it.\nIf you want to add a new value to it, press Cancel and use Edit mode."), "Attribute already assigned: [" + pe.getName() + "]", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE) != javax.swing.JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                ASN1Type asn1 = pe.run(parentFrame, this.vars, null, this.editors);
                if (asn1 == null) return;
                if (idx >= 0) {
                    dataList.remove(idx);
                    collection.remove(idx);
                }
                collection.add(asn1);
                issrg.ac.Attribute attr = (issrg.ac.Attribute) asn1;
                Vector values = attr.getValues();
                String str = createRoleValuesString(values);
                if (str.equals("")) dataList.add(pe.getName()); else dataList.add(pe.getName().concat("=").concat(str));
                updateLists();
            }
        } catch (IllegalInputException iie) {
            issrg.utils.Util.bewail(iie.getMessage(), null, this);
        } catch (ACCreationException acce) {
            issrg.utils.Util.bewail(acce.getMessage(), acce, this);
        }
    }

    protected void removeASN1(JList jlist, Vector list, String things) {
        int idx = 0;
        if ((idx = jlist.getSelectedIndex()) >= 0) {
            if (javax.swing.JOptionPane.showConfirmDialog(this, new String("Are you sure you want to remove the selected object?"), "Confirm", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE) != javax.swing.JOptionPane.OK_OPTION) {
                return;
            }
            Vector v = (Vector) vars.get(things);
            v.remove(idx);
            list.remove(idx);
            updateLists();
        }
    }

    protected void runEditor(String name) {
        Map c = editors.getCollection(EnvironmentalVariables.UTILITIES_COLLECTION);
        if (c != null) {
            Editor e = (Editor) c.get(name);
            if (e != null) try {
                saveToEnvironment();
                e.edit(parentFrame, this, vars);
                loadFromEnvironment();
            } catch (ACCreationException ace) {
                issrg.utils.Util.bewail(ace.getMessage(), ace, this);
            }
        }
    }

    void jACValidityButton_actionPerformed(ActionEvent e) {
        runEditor(EnvironmentalVariables.VALIDITY_PERIOD_EDITOR_UTILITY);
    }

    void jExtensions_stateChanged(ActionEvent e) {
        boolean extensionExist;
        Object source = e.getSource();
        if (source == jExtensions) {
            if (jExtensions.isSelected()) {
                extensionExist = true;
                ExtensionsChooser extensionsChooser = new ExtensionsChooser(this, "Delegation Options", true, extensionExist);
                vars.put(EnvironmentalVariables.EXTENSIONS_VECTOR, extensionNamesList);
                updateCheckBoxAndButton();
            } else {
                extensionNamesList.clear();
                vars.put(EnvironmentalVariables.EXTENSIONS_VECTOR, extensionNamesList);
                updateCheckBoxAndButton();
            }
        }
    }

    void jExtensions_Edit(ActionEvent e) {
        boolean extensionExist;
        if (jExtensions.isSelected()) {
            extensionExist = true;
        } else extensionExist = false;
        ExtensionsChooser extensionsChooser = new ExtensionsChooser(this, "Delegation Options", true, extensionExist);
        vars.put(EnvironmentalVariables.EXTENSIONS_VECTOR, extensionNamesList);
        updateCheckBoxAndButton();
    }

    void updateCheckBoxAndButton() {
        int num = extensionNamesList.size();
        for (int i = 0; i < num; i++) {
            issrg.ac.Extension ext = (issrg.ac.Extension) extensionNamesList.elementAt(i);
            if ((ext instanceof issrg.ac.attributes.BasicAttConstraint) || (ext instanceof issrg.ac.attributes.NoAssertion)) {
                jExtensions.setSelected(true);
                edit.setEnabled(true);
                return;
            }
        }
        jExtensions.setSelected(false);
        edit.setEnabled(false);
        return;
    }
}
