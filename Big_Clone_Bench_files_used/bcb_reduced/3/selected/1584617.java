package issrg.editor2;

import iaik.asn1.ASN1Object;
import iaik.asn1.BIT_STRING;
import iaik.asn1.structures.AlgorithmID;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.security.MessageDigest;
import java.math.BigInteger;
import issrg.utils.EnvironmentalVariables;
import issrg.utils.gui.repository.AttributeComboBox;
import issrg.utils.gui.xml.NodeVector;
import issrg.acm.extensions.SimpleSigningUtility;
import issrg.acm.extensions.LDAPSavingUtility;
import issrg.acm.extensions.WebDAVSavingUtility;
import issrg.ac.attributes.PMIXMLPolicy;
import issrg.ac.AttributeCertificate;
import issrg.ac.ACCreationException;
import issrg.ac.Util;
import issrg.utils.gui.timedate.DateDialog;
import issrg.security.DefaultSecurity;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by IntelliJ IDEA.
 * User: Administrador
 * Date: Jul 12, 2004
 * Time: 1:00:07 PM
 * To change this template use Options | File Templates.
 */
public class PolicySigningDialog extends JDialog {

    private JRadioButton jRBfile = null;

    private JRadioButton jRBLDAP = null;

    private JRadioButton jRBWebDAV = null;

    private AttributeComboBox availableConnections = null;

    private AttributeComboBox availableConnectionsWebDAV = null;

    private JTextField textBeforeDate = null;

    private JSpinner spinnerBeforeH = null;

    private JSpinner spinnerBeforeM = null;

    private JSpinner spinnerBeforeS = null;

    private JTextField textAfterDate = null;

    private JSpinner spinnerAfterH = null;

    private JSpinner spinnerAfterM = null;

    private JSpinner spinnerAfterS = null;

    private JCheckBox jCHBefore = null;

    private JCheckBox jCHAfter = null;

    private Map env;

    private JButton bBeforeChooser = null;

    private JButton bAfterChooser = null;

    private JFrame jf;

    private String xmlPolicy = "NULL";

    private String xmlPolicyName;

    public PolicySigningDialog(JFrame f) {
        super(f, "Signing and publishing of your policy");
        jf = f;
        env = new Hashtable();
        JPanel pData = new JPanel();
        pData.setLayout(new BorderLayout());
        JPanel pButtons = new JPanel();
        JPanel pPublication = new JPanel();
        pPublication.setBorder(new TitledBorder("My policy will be published"));
        pPublication.setLayout(new FlowLayout());
        jRBfile = new JRadioButton("as a file", true);
        jRBLDAP = new JRadioButton("in a LDAP server", false);
        jRBWebDAV = new JRadioButton("in a WebDAV server", false);
        availableConnections = new AttributeComboBox(PEApplication.configComponent, "LDAPConfiguration", "LDAPDirectory", "Name");
        availableConnections.setEnabled(false);
        availableConnectionsWebDAV = new AttributeComboBox(PEApplication.configComponent, "WebDAVConfiguration", "WebDAVRepository", "Name");
        availableConnectionsWebDAV.setEnabled(false);
        jRBfile.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (jRBfile.isSelected()) {
                    availableConnections.setEnabled(false);
                    availableConnectionsWebDAV.setEnabled(false);
                }
            }
        });
        jRBLDAP.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (jRBLDAP.isSelected()) {
                    availableConnections.setEnabled(true);
                    availableConnectionsWebDAV.setEnabled(false);
                }
            }
        });
        jRBWebDAV.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (jRBWebDAV.isSelected()) {
                    availableConnections.setEnabled(false);
                    availableConnectionsWebDAV.setEnabled(true);
                }
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(jRBfile);
        bg.add(jRBLDAP);
        bg.add(jRBWebDAV);
        pPublication.add(jRBfile);
        pPublication.add(jRBLDAP);
        pPublication.add(availableConnections);
        pPublication.add(jRBWebDAV);
        pPublication.add(availableConnectionsWebDAV);
        JPanel pBefore = new JPanel();
        pBefore.setBorder(new TitledBorder("My policy will be valid from"));
        pBefore.setLayout(new GridLayout(2, 1));
        JPanel pBeforeDate = new JPanel();
        JPanel pBeforeTime = new JPanel();
        pBeforeDate.add(new JLabel("Date"));
        if (textBeforeDate == null) textBeforeDate = new JTextField(10);
        textBeforeDate.setText(new SimpleDateFormat("yyyy.MM.dd").format(new Date()));
        pBeforeDate.add(textBeforeDate);
        bBeforeChooser = new JButton("Choose...");
        bBeforeChooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                DateDialog DATE_CHOOSER = new DateDialog(jf, "Select Date");
                Date d = DATE_CHOOSER.select();
                textBeforeDate.setText(new SimpleDateFormat("yyyy.MM.dd").format(d));
            }
        });
        pBeforeDate.add(bBeforeChooser);
        pBefore.add(pBeforeDate);
        SpinnerNumberModel modelH = new SpinnerNumberModel(0, 0, 23, 1);
        spinnerBeforeH = new JSpinner(modelH);
        JSpinner.NumberEditor editorBeforeH = new JSpinner.NumberEditor(spinnerBeforeH);
        spinnerBeforeH.setEditor(editorBeforeH);
        SpinnerNumberModel modelM = new SpinnerNumberModel(0, 0, 59, 1);
        spinnerBeforeM = new JSpinner(modelM);
        JSpinner.NumberEditor editorBeforeM = new JSpinner.NumberEditor(spinnerBeforeM);
        spinnerBeforeM.setEditor(editorBeforeM);
        SpinnerNumberModel modelS = new SpinnerNumberModel(0, 0, 59, 1);
        spinnerBeforeS = new JSpinner(modelS);
        JSpinner.NumberEditor editorBeforeS = new JSpinner.NumberEditor(spinnerBeforeS);
        spinnerBeforeS.setEditor(editorBeforeS);
        pBeforeTime.add(spinnerBeforeH);
        pBeforeTime.add(new JLabel("hours"));
        pBeforeTime.add(spinnerBeforeM);
        pBeforeTime.add(new JLabel("minutes"));
        pBeforeTime.add(spinnerBeforeS);
        pBeforeTime.add(new JLabel("seconds"));
        pBefore.add(pBeforeTime);
        if (jCHBefore == null) jCHBefore = new JCheckBox("From now");
        jCHBefore.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                boolean status = !jCHBefore.isSelected();
                textBeforeDate.setEnabled(status);
                spinnerBeforeH.setEnabled(status);
                spinnerBeforeM.setEnabled(status);
                spinnerBeforeS.setEnabled(status);
                bBeforeChooser.setEnabled(status);
            }
        });
        JPanel pAfter = new JPanel();
        pAfter.setBorder(new TitledBorder("My policy will be valid until"));
        pAfter.setLayout(new GridLayout(2, 1));
        JPanel pAfterDate = new JPanel();
        JPanel pAfterTime = new JPanel();
        pAfterDate.add(new JLabel("Date"));
        textAfterDate = new JTextField(10);
        textAfterDate.setText(new SimpleDateFormat("yyyy.MM.dd").format(new Date()));
        pAfterDate.add(textAfterDate);
        bAfterChooser = new JButton("Choose...");
        bAfterChooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                DateDialog DATE_CHOOSER = new DateDialog(jf, "Select Date");
                Date d = DATE_CHOOSER.select();
                textAfterDate.setText(new SimpleDateFormat("yyyy.MM.dd").format(d));
            }
        });
        pAfterDate.add(bAfterChooser);
        pAfter.add(pAfterDate);
        SpinnerNumberModel modelH2 = new SpinnerNumberModel(0, 0, 23, 1);
        spinnerAfterH = new JSpinner(modelH2);
        JSpinner.NumberEditor editorAfterH = new JSpinner.NumberEditor(spinnerAfterH);
        spinnerAfterH.setEditor(editorAfterH);
        SpinnerNumberModel modelM2 = new SpinnerNumberModel(0, 0, 59, 1);
        spinnerAfterM = new JSpinner(modelM2);
        JSpinner.NumberEditor editorAfterM = new JSpinner.NumberEditor(spinnerAfterM);
        spinnerAfterM.setEditor(editorAfterM);
        SpinnerNumberModel modelS2 = new SpinnerNumberModel();
        spinnerAfterS = new JSpinner(modelS2);
        JSpinner.NumberEditor editorAfterS = new JSpinner.NumberEditor(spinnerAfterS);
        spinnerAfterS.setEditor(editorAfterS);
        pAfterTime.add(spinnerAfterH);
        pAfterTime.add(new JLabel("hours"));
        pAfterTime.add(spinnerAfterM);
        pAfterTime.add(new JLabel("minutes"));
        pAfterTime.add(spinnerAfterS);
        pAfterTime.add(new JLabel("seconds"));
        pAfter.add(pAfterTime);
        jCHAfter = new JCheckBox("Forever");
        jCHAfter.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                boolean status = !jCHAfter.isSelected();
                textAfterDate.setEnabled(status);
                spinnerAfterH.setEnabled(status);
                spinnerAfterM.setEnabled(status);
                spinnerAfterS.setEnabled(status);
                bAfterChooser.setEnabled(status);
            }
        });
        pData.add("North", pPublication);
        pData.add("West", pBefore);
        pData.add("East", pAfter);
        JButton accept = new JButton("Sign and Publish");
        JButton cancel = new JButton("Cancel");
        accept.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (signAndPublish()) setVisible(false);
            }
        });
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setVisible(false);
            }
        });
        pButtons.add(accept);
        pButtons.add(cancel);
        Container cnt = this.getContentPane();
        cnt.setLayout(new java.awt.BorderLayout());
        cnt.add("North", pData);
        cnt.add("South", pButtons);
    }

    public void setPolicy(String p) {
        xmlPolicy = p;
    }

    public void setPolicyName(Document d) {
        NodeList nl = d.getElementsByTagName("X.509_PMI_RBAC_Policy");
        xmlPolicyName = ((Element) nl.item(0)).getAttribute("OID");
    }

    public void interact() {
        this.pack();
        this.setResizable(false);
        this.setModal(true);
        centerDialog();
        this.show();
    }

    private boolean signAndPublish() {
        String before = "";
        String after = "";
        try {
            if (!jCHBefore.isSelected()) new SimpleDateFormat("yyyy.MM.dd").parse(textBeforeDate.getText());
            if (!jCHAfter.isSelected()) new SimpleDateFormat("yyyy.MM.dd").parse(textAfterDate.getText());
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(jf, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!jCHBefore.isSelected()) {
            before = textBeforeDate.getText() + " " + spinnerBeforeH.getValue() + ":" + spinnerBeforeM.getValue() + ":" + spinnerBeforeS.getValue();
        }
        if (!jCHAfter.isSelected()) {
            after = after + textAfterDate.getText() + " " + spinnerAfterH.getValue() + ":" + spinnerAfterM.getValue() + ":" + spinnerAfterS.getValue();
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            BigInteger bi = new BigInteger(md.digest(new java.util.GregorianCalendar().getTime().toString().getBytes()));
            java.math.BigInteger ACSerialNumber = new java.math.BigInteger((bi.abs().toString(16)).substring(0, 14), 16);
            issrg.ac.Generalized_Time btt = null;
            issrg.ac.Generalized_Time att = null;
            if (!jCHBefore.isSelected()) btt = Util.buildGeneralizedTime(before);
            if (!jCHAfter.isSelected()) att = Util.buildGeneralizedTime(after);
            issrg.ac.AttCertValidityPeriod validity_period = new issrg.ac.AttCertValidityPeriod(btt, att);
            PMIXMLPolicy policy = new PMIXMLPolicy(xmlPolicy);
            issrg.ac.Attribute attr = new issrg.ac.Attribute(PMIXMLPolicy.PMI_XML_POLICY_ATTRIBUTE_OID, policy);
            Vector attributes = new Vector();
            attributes.add(attr);
            issrg.ac.Extensions extensions = new issrg.ac.Extensions(new Vector());
            issrg.ac.AttCertIssuer issuer;
            SimpleSigningUtility signingUtility = new SimpleSigningUtility();
            Node root = PEApplication.configComponent.DOM.getElementsByTagName("ApplicationSettings").item(0);
            Element signingKey = ((Element) NodeVector.getChildElements(root).item(0));
            try {
                env.remove(DefaultSecurity.DEFAULT_FILE_STRING);
                env.put(DefaultSecurity.DEFAULT_FILE_STRING, signingKey.getAttribute("FileName"));
                signingUtility.login(jf, env);
                java.security.cert.X509Certificate signerPKC = signingUtility.getVerificationCertificate();
                String subjectDN;
                if (signerPKC instanceof iaik.x509.X509Certificate) {
                    try {
                        subjectDN = ((iaik.asn1.structures.Name) signerPKC.getSubjectDN()).getRFC2253String();
                    } catch (iaik.utils.RFC2253NameParserException rnpe) {
                        throw new ACCreationException("Failed to decode DNs", rnpe);
                    }
                } else {
                    subjectDN = signerPKC.getSubjectDN().getName();
                }
                iaik.asn1.structures.GeneralNames hn = issrg.ac.Util.buildGeneralNames(subjectDN);
                issrg.ac.V2Form signer = new issrg.ac.V2Form(hn, null, null);
                issuer = new issrg.ac.AttCertIssuer(null, signer);
                issrg.ac.Holder holder = new issrg.ac.Holder(null, hn, null);
                byte[] bt = signerPKC.getSigAlgParams();
                ASN1Object algParams = bt == null ? null : iaik.asn1.DerCoder.decode(bt);
                AlgorithmID signatureAlg = new AlgorithmID(new iaik.asn1.ObjectID(signingUtility.getSigningAlgorithmID()));
                issrg.ac.AttributeCertificateInfo aci = new issrg.ac.AttributeCertificateInfo(new issrg.ac.AttCertVersion(issrg.ac.AttCertVersion.V2), holder, issuer, signatureAlg, ACSerialNumber, validity_period, attributes, null, extensions);
                AttributeCertificate cert = null;
                byte[] b = aci.getEncoded();
                try {
                    cert = new issrg.ac.AttributeCertificate(aci, signatureAlg, new BIT_STRING(signingUtility.sign(b)));
                } catch (Throwable e) {
                    throw new ACCreationException(e.getMessage(), e);
                } finally {
                    signingUtility.logout(null, env);
                }
                if (cert != null) {
                    issrg.acm.SavingUtility su = null;
                    if (jRBfile.isSelected()) {
                        su = new issrg.acm.DiskSavingUtility();
                    } else if (jRBWebDAV.isSelected()) {
                        Node webdavCon = PEApplication.configComponent.DOM.getElementsByTagName("WebDAVConfiguration").item(0);
                        Element selectedConnection = ((Element) NodeVector.getChildElements(webdavCon).item(availableConnectionsWebDAV.getSelectedIndex()));
                        String host = selectedConnection.getAttribute("Host");
                        String port = selectedConnection.getAttribute("Port");
                        String protocol = selectedConnection.getAttribute("Protocol");
                        String p12filename = selectedConnection.getAttribute("P12Filename");
                        String p12password = selectedConnection.getAttribute("P12Password");
                        setWebDAVParameters(host, port, protocol, p12filename, p12password);
                        su = new issrg.acm.extensions.WebDAVSavingUtility(WebDAVSavingUtility.policyPrefix + xmlPolicyName);
                    } else {
                        Node ldapCon = PEApplication.configComponent.DOM.getElementsByTagName("LDAPConfiguration").item(0);
                        Element selectedConnection = ((Element) NodeVector.getChildElements(ldapCon).item(availableConnections.getSelectedIndex()));
                        String host = "ldap://" + selectedConnection.getAttribute("Host");
                        if (selectedConnection.getAttribute("Port") != null) host += ":" + selectedConnection.getAttribute("Port");
                        if (selectedConnection.getAttribute("BaseDN") != null) host += "/" + selectedConnection.getAttribute("BaseDN");
                        if (selectedConnection.getAttribute("Login") == null || selectedConnection.getAttribute("Login").equals("")) setLDAPParameters(host, "", selectedConnection.getAttribute("ACType")); else setLDAPParameters(host, selectedConnection.getAttribute("Login"), selectedConnection.getAttribute("ACType"));
                        su = new issrg.acm.extensions.LDAPSavingUtility();
                    }
                    boolean redo;
                    do {
                        redo = false;
                        try {
                            env.put(EnvironmentalVariables.HOLDER_NAME_STRING, subjectDN);
                            su.save(jf, cert.getEncoded(), env);
                        } catch (ACCreationException acce) {
                            issrg.utils.Util.bewail(acce.getMessage(), acce, this);
                            redo = javax.swing.JOptionPane.showConfirmDialog(this, "Try to save again?", "Confirm", javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.OK_OPTION;
                        }
                    } while (redo);
                }
            } catch (iaik.asn1.CodingException ce) {
                throw new ACCreationException(ce.getMessage(), ce);
            } catch (issrg.security.SecurityException se) {
                se.printStackTrace();
                throw new ACCreationException(se.getMessage(), se);
            }
        } catch (Exception ge) {
            JOptionPane.showMessageDialog(jf, ge.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public void setLDAPParameters(String server, String login, String acType) {
        env.put(EnvironmentalVariables.LDAP_SAVING_UTILITY_LDAP_PROVIDER, server);
        env.put(LDAPSavingUtility.LDAP_SAVING_UTILITY_LOGIN, login);
        env.put(LDAPSavingUtility.LDAP_SAVING_UTILITY_AC_TYPE, acType);
    }

    public void setWebDAVParameters(String host, String port, String protocol, String p12filename, String p12password) {
        env.put(EnvironmentalVariables.WEBDAV_HOST, host);
        env.put(EnvironmentalVariables.WEBDAV_PORT, port);
        env.put(EnvironmentalVariables.WEBDAV_PROTOCOL, protocol);
        env.put(EnvironmentalVariables.WEBDAV_P12FILENAME, p12filename);
        env.put(EnvironmentalVariables.WEBDAV_P12PASSWORD, p12password);
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
}
