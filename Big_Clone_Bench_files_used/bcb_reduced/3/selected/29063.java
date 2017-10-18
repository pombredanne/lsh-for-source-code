package it.trento.comune.j4sign.examples;

import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import it.trento.comune.j4sign.pcsc.CardInfo;
import it.trento.comune.j4sign.pcsc.PCSCHelper;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.StringTokenizer;
import netscape.javascript.*;

/**
 * @author resolir
 * 
 */
public class PKCS11SignApplet extends JApplet implements java.awt.event.ActionListener {

    private JTextArea logArea = null;

    private JTextArea dataArea = null;

    private JButton enc = null;

    private JPasswordField pwd = null;

    private DigestSignTask dsTask = null;

    private FindCertTask certTask = null;

    private Timer findTimer = null;

    private JTextArea certArea = null;

    private JTextField signingTimeGMT = null;

    private Timer timer = null;

    private JButton v = null;

    private JButton ld = null;

    private JButton led = null;

    private JButton sd = null;

    private JProgressBar progressBar = null;

    boolean debug = false;

    boolean submitAfterSigning = false;

    private String encodedDigest = null;

    private byte[] encryptedDigest;

    private java.io.PrintStream log = null;

    public static final int ONE_SECOND = 1000;

    public static final String VERSION = "0.0.0.1";

    private java.lang.String cryptokiLib = null;

    private java.lang.String signerLabel = null;

    private java.lang.String digestPath = null;

    private byte[] certificate = null;

    private boolean makeDigestOnToken = false;

    private static final String DIGEST_MD5 = "1.2.840.113549.2.5";

    private static final String DIGEST_SHA1 = "1.3.14.3.2.26";

    private static final String DIGEST_SHA256 = "2.16.840.1.101.3.4.2.1";

    private static final String ENCRYPTION_RSA = "1.2.840.113549.1.1.1";

    private String digestAlg = DIGEST_SHA256;

    private String encAlg = ENCRYPTION_RSA;

    /**
	 * Initializes the applet, building the GUI according to the
	 * <code>debug</code> applet parameter.<br>
	 * Note that it is possible to force the cryptoki library to load, using the<br>
	 * <code>cryptokilib</code> applet parameter.<br>
	 * There is also the possibility to select a specific couple of keys on the
	 * card, using the <code>signerlabel </code> applet parameter.<br>
	 * This method loads also the digest from the form, calling
	 * {@link #retriveEncodedDigestFromForm()}
	 * 
	 * 
	 * @see #start
	 * @see #stop
	 * @see #destroy
	 */
    public void init() {
        super.init();
        System.out.println("Initializing PKCS11SignApplet ...");
        if (getParameter("debug") != null) debug = Boolean.valueOf(getParameter("debug")).booleanValue();
        if (getParameter("submit") != null) submitAfterSigning = Boolean.valueOf(getParameter("submit")).booleanValue();
        if (getParameter("digestPath") != null) setDigestPath(getParameter("digestPath"));
        if (getParameter("cryptokilib") != null) setCryptokiLib(getParameter("cryptokilib"));
        if (getParameter("signerlabel") != null) setSignerLabel(getParameter("signerlabel"));
        System.out.println("\nUsing cryptoki:\t" + getCryptokiLib());
        System.out.println("Using signer:\t" + getSignerLabel() + "\n");
        getContentPane().setLayout(new BorderLayout());
        if (!debug) log = System.out; else {
            logArea = new JTextArea();
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            log = new PrintStream(new JTextAreaOutputStream(logArea), true);
            dataArea = new JTextArea();
            enc = new JButton("Set Digest");
            ld = new JButton("Load Digest");
            led = new JButton("Load Enc Digest");
            sd = new JButton("Send Cert");
            v = new JButton("Verify");
            enc.addActionListener(this);
            ld.addActionListener(this);
            led.addActionListener(this);
            sd.addActionListener(this);
            v.addActionListener(this);
            JScrollPane logScrollPane = new JScrollPane(logArea);
            JScrollPane dataScrollPane = new JScrollPane(dataArea);
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logScrollPane, dataScrollPane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);
            splitPane.setPreferredSize(new Dimension(400, 200));
            getContentPane().add(splitPane, BorderLayout.CENTER);
        }
        pwd = new JPasswordField();
        pwd.setPreferredSize(new Dimension(50, 20));
        pwd.addActionListener(this);
        pwd.setEnabled(false);
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        JPanel controlsPanel = new JPanel();
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        JPanel certPanel = new JPanel();
        certPanel.setLayout(new BoxLayout(certPanel, BoxLayout.X_AXIS));
        certArea = new JTextArea();
        certArea.setPreferredSize(new Dimension(100, 40));
        certArea.setEditable(false);
        certArea.setLineWrap(true);
        certArea.setFont(new Font("Sans-serif", Font.BOLD, 12));
        certArea.setForeground(Color.BLUE);
        JPanel datePanel = new JPanel();
        datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.X_AXIS));
        signingTimeGMT = new JTextField();
        signingTimeGMT.setEditable(false);
        signingTimeGMT.setFont(new Font("Sans-serif", Font.BOLD, 12));
        signingTimeGMT.setForeground(Color.BLUE);
        datePanel.add(new JLabel("Ora della firma (sarà firmata): "));
        datePanel.add(signingTimeGMT);
        sd = new JButton("Aggiorna");
        sd.addActionListener(this);
        datePanel.add(sd);
        certPanel.add(new JLabel("Certificato: "));
        certPanel.add(certArea);
        controlsPanel.add(pwd);
        JPanel digestPanel = new JPanel();
        digestPanel.setLayout(new BoxLayout(digestPanel, BoxLayout.Y_AXIS));
        if (debug) {
            controlsPanel.add(enc);
            digestPanel.add(ld);
            digestPanel.add(led);
            controlsPanel.add(digestPanel);
            controlsPanel.add(v);
            v.setEnabled(false);
            led.setEnabled(false);
            ld.setEnabled(false);
        }
        progressBar = new JProgressBar();
        progressBar.setStringPainted(false);
        progressBar.setStringPainted(true);
        statusPanel.add(progressBar);
        southPanel.add(controlsPanel);
        southPanel.add(datePanel);
        southPanel.add(certPanel);
        southPanel.add(statusPanel);
        getContentPane().add(southPanel, debug ? BorderLayout.SOUTH : BorderLayout.CENTER);
        findCert();
    }

    public java.lang.String getDigestPath() {
        return digestPath;
    }

    public void setDigestPath(java.lang.String digestPath) {
        this.digestPath = digestPath;
    }

    /**
	 * GUI event management<br>
	 * The most important source of events is the pwd field, that triggers the
	 * creation of the signing task. The task is a {@link DigestSignTask}
	 * carried in a separate thread, avoiding to lock the gui; a
	 * <code>Timer</code> is used to refresh a progress bar every second,
	 * querying the task status.
	 * 
	 * @param e
	 *            The event to deal with.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        try {
            setStatus(DigestSignTask.RESET, "");
            if (e.getSource() == sd) if (retriveEncodedDigestFromServer()) setStatus(DigestSignTask.RESET, "Inserire il pin e battere INVIO per firmare.");
            if (e.getSource() == pwd) {
                initStatus(0, DigestSignTask.SIGN_MAXIMUM);
                if (detectCardAndCriptoki()) {
                    dsTask = new DigestSignTask(getCryptokiLib(), getSignerLabel(), log);
                    timer = new Timer(ONE_SECOND, new java.awt.event.ActionListener() {

                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            setStatus(dsTask.getCurrent(), dsTask.getMessage());
                            if (dsTask.done()) {
                                timer.stop();
                                progressBar.setValue(progressBar.getMinimum());
                                if (dsTask.getCurrent() == DigestSignTask.SIGN_DONE) {
                                    Toolkit.getDefaultToolkit().beep();
                                    setEncryptedDigest(dsTask.getEncryptedDigest());
                                    returnEncryptedDigestToForm();
                                    setCertificate(dsTask.getCertificate());
                                    returnCertificateToForm();
                                    if (getSubmitAfterSigning()) {
                                        submitForm();
                                    }
                                }
                                enableControls(true);
                            }
                        }
                    });
                    sign();
                }
            }
            if (e.getSource() == enc) {
                log.println("\nCalculating digest ...\n");
                java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
                md5.update(dataArea.getText().getBytes("UTF8"));
                byte[] digest = md5.digest();
                log.println("digest:\n" + formatAsHexString(digest));
                log.println("Done.");
                setEncodedDigest(encodeFromBytes(digest));
                returnDigestToForm();
            }
            if (e.getSource() == ld) retriveEncodedDigestFromForm();
            if (e.getSource() == led) retriveEncryptedDigestFromForm();
            if (e.getSource() == v) {
                verify();
            }
        } catch (Exception ex) {
            log.println(ex.toString());
        } finally {
            pwd.setText("");
        }
    }

    /**
	 * <code>Base64</code> String to String decoding function. Relies on
	 * {@link #decodeToBytes(String)} method.
	 * 
	 * @param s
	 *            The <code>Base64</code> string to decode.
	 * @return The decoded string.
	 */
    public String decode(String s) {
        try {
            byte[] bytes = decodeToBytes(s);
            if (bytes != null) return new String(bytes, "UTF8");
        } catch (java.io.UnsupportedEncodingException e) {
            log.println("Errore di encoding: " + e);
        }
        return null;
    }

    /**
	 * <code>Base64</code> String to byte[] decoding function. Warning: this
	 * method relies on <code>sun.misc.BASE64Decoder</code>
	 * 
	 * @param s
	 *            The <code>Base64</code> string to decode.
	 * @return the decoded string as a <code>byte[]</code>
	 */
    public byte[] decodeToBytes(String s) {
        byte[] stringBytes = null;
        try {
            sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
            stringBytes = decoder.decodeBuffer(s);
        } catch (java.io.IOException e) {
            log.println("Errore di io: " + e);
        }
        return stringBytes;
    }

    /**
	 * Cleans up whatever resources are being held. If the applet is active it
	 * is stopped.
	 * 
	 * @see #init
	 * @see #start
	 * @see #stop
	 */
    public void destroy() {
        super.destroy();
        System.out.println("Destroying applet and garbage collecting...");
        dsTask = null;
        System.gc();
        System.out.println("Garbage collection done.");
    }

    /**
	 * Enables GUI controls (depending on debug mode).
	 * 
	 * @param enable
	 *            if <code>true</code>, controls will be enabled.
	 */
    private void enableControls(boolean enable) {
        pwd.setEnabled(enable);
        if (isDebugMode()) {
            led.setEnabled(enable);
            ld.setEnabled(enable);
            v.setEnabled(enable);
        }
    }

    /**
	 * <code>Base64</code> String to String encoding function. Relies on
	 * {@link #encodeToBytes(String)} method.
	 * 
	 * @param s
	 *            The string to encode.
	 * @return The <code>Base64</code> encoded string.
	 */
    public String encode(String s) {
        try {
            return encodeFromBytes(s.getBytes("UTF8"));
        } catch (java.io.UnsupportedEncodingException e) {
            log.println("Errore di encoding: " + e);
        }
        return null;
    }

    /**
	 * <code>Base64</code> <code>byte[]</code> to <code>String</code> encoding
	 * function. Warning: this method relies on
	 * <code>sun.misc.BASE64Encoder</code>
	 * 
	 * @param The
	 *            <code>byte[]</code> to encode.
	 * @return The <code>Base64</code> encoded string.
	 */
    public String encodeFromBytes(byte[] bytes) {
        String encString = null;
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        encString = encoder.encode(bytes);
        return encString;
    }

    /**
	 * Returns information about this applet.
	 * 
	 * @return a string of information about this applet
	 */
    public String getAppletInfo() {
        return "PKCS11SignApplet\n" + "\n" + "This applet is based on http://j4sign.sf.net project.\n";
    }

    /**
	 * Gets the <code>x509 </code>certificate as a <code>byte[]</code>.
	 * 
	 * @return the <code>x509 </code>certificate as a <code>byte[]</code>.
	 */
    public byte[] getCertificate() {
        return certificate;
    }

    /**
	 * Gets name of the cryptoki library in use
	 * 
	 * @return a <code>String</code> specifiyng the cryptoki library name.
	 */
    private java.lang.String getCryptokiLib() {
        return cryptokiLib;
    }

    /**
	 * Gets the digest <code>Base64</code> encoded.
	 * 
	 * @return <code>Base64</code> encoding of the digest.
	 */
    public String getEncodedDigest() {
        return this.encodedDigest;
    }

    /**
	 * Gets the raw encryptedDigest-
	 * 
	 * @return the encrypted digest as <code>byte[]</code>.
	 */
    public byte[] getEncryptedDigest() {
        return encryptedDigest;
    }

    /**
	 * Gets the signer label (if specified in the applet parameter)
	 * 
	 * @return a String containing the label identifying the signer to use on
	 *         the card.
	 */
    private java.lang.String getSignerLabel() {
        return signerLabel;
    }

    /**
	 * Is the form inside the embedding page to be submitted after signing?
	 * 
	 * @return <code>true</code> if the applet has to submit the form.
	 */
    private boolean getSubmitAfterSigning() {
        return submitAfterSigning;
    }

    /**
	 * Initializes minimum and maximum status values for progress bar
	 * 
	 * @param min
	 * @param max
	 */
    private void initStatus(int min, int max) {
        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
        setStatus(min, "");
    }

    /**
	 * Is the applet in debug mode?
	 * 
	 * @return true if debug mode is on, false otherwise
	 */
    private boolean isDebugMode() {
        return debug;
    }

    /**
	 * Interfaces via javascript with the embedding page for loading the
	 * server-generated digest field on the form.
	 */
    public void retriveEncodedDigestFromForm() {
        try {
            log.println("Loading data from form ...");
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            setEncodedDigest((String) doc.eval("getDigest()"));
            log.println("Digest loaded.");
        } catch (netscape.javascript.JSException e) {
            log.println("Errore JSO: " + e);
            setStatus(DigestSignTask.ERROR, "Errore JavaScript recuperando il digest da firmare");
        }
    }

    /**
	 * Interfaces via javascript with the embedding page for loading the
	 * encrypted digest field on the form.
	 */
    public void retriveEncryptedDigestFromForm() {
        try {
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            String encodedEncryptedDigest = (String) doc.eval("getDigestFirmato()");
            setEncryptedDigest(decodeToBytes(encodedEncryptedDigest));
            log.println("Encrypted digest loaded.");
        } catch (netscape.javascript.JSException e) {
            log.println("Errore JSO: " + e);
            setStatus(DigestSignTask.ERROR, "Errore JavaScript estraendo i dati firmati ");
        } catch (Exception e) {
            log.println("I/O error decoding signed digest");
            log.println(e);
            setStatus(DigestSignTask.ERROR, "Errore i/o estraendo il digest firmato");
        }
    }

    /**
	 * Interfaces via javascript with the embedding page for setting the
	 * certificate field on the form.
	 */
    public void returnCertificateToForm() {
        try {
            String encodedCert = encodeFromBytes(getCertificate());
            BufferedReader br = new BufferedReader(new StringReader(encodedCert));
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            String aBlock = null;
            StringBuffer allText = new StringBuffer();
            while ((aBlock = br.readLine()) != null) allText.append(aBlock);
            doc.eval("setCertificato('" + allText + "')");
        } catch (netscape.javascript.JSException e) {
            log.println("Errore JSO: " + e);
            setStatus(DigestSignTask.ERROR, "Errore JavaScript");
        } catch (IOException ioe) {
            log.println("Errore restituendo il certificato alla form");
            log.println(ioe);
            setStatus(DigestSignTask.ERROR, "Errore restituendo il certificato alla form");
        }
    }

    /**
	 * Interfaces via javascript with the embedding page for setting the
	 * encrypted digest field on the form.
	 */
    public void returnEncryptedDigestToForm() {
        try {
            String encodedEncryptedDigest = encodeFromBytes(getEncryptedDigest());
            BufferedReader br = new BufferedReader(new StringReader(encodedEncryptedDigest));
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            String aBlock = null;
            StringBuffer allText = new StringBuffer();
            while ((aBlock = br.readLine()) != null) allText.append(aBlock);
            doc.eval("setDigestFirmato('" + allText + "')");
        } catch (netscape.javascript.JSException e) {
            log.println("Errore JSO: " + e);
            setStatus(DigestSignTask.ERROR, "Errore JavaScript");
        } catch (IOException ioe) {
            log.println("Errore restituendo encryptedDigest alla form");
            log.println(ioe);
            setStatus(DigestSignTask.ERROR, "Errore restituendo encryptedDigest alla form");
        }
    }

    /**
	 * Interfaces via javascript with the embedding page for setting the digest
	 * field on the form (useful only for testing purpose in debug mode).
	 */
    public void returnDigestToForm() {
        try {
            BufferedReader br = new BufferedReader(new StringReader(getEncodedDigest()));
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            String aBlock = null;
            StringBuffer allText = new StringBuffer();
            while ((aBlock = br.readLine()) != null) allText.append(aBlock);
            doc.eval("setDigest('" + allText + "')");
        } catch (netscape.javascript.JSException e) {
            log.println("Errore JSO: " + e);
            setStatus(DigestSignTask.ERROR, "Errore JavaScript");
        } catch (IOException ioe) {
            log.println("Errore restituendo digest alla form");
            log.println(ioe);
            setStatus(DigestSignTask.ERROR, "Errore restituendo digest alla form");
        }
    }

    /**
	 * Start verification of the encrypted digest (useful only for testing
	 * purpose in debug mode).
	 */
    private void verify() {
        String key = null;
        boolean verified = false;
        java.security.cert.CertificateFactory cf;
        try {
            cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.io.ByteArrayInputStream bais1 = new java.io.ByteArrayInputStream(getCertificate());
            java.security.cert.X509Certificate javaCert = (java.security.cert.X509Certificate) cf.generateCertificate(bais1);
            PublicKey pubKey = javaCert.getPublicKey();
            try {
                log.println("\nDecrypting ...");
                Cipher c = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                c.init(Cipher.DECRYPT_MODE, pubKey);
                byte[] decBytes = c.doFinal(getEncryptedDigest());
                byte[] digestBytes = decodeToBytes(getEncodedDigest());
                log.println("NOTE: decrypted digest can include digestInfo bytes as header!");
                log.println("decrypted:\t" + formatAsHexString(decBytes));
                log.println("original:\t" + formatAsHexString(digestBytes));
                int displacementInSignerInfo = decBytes.length - digestBytes.length;
                verified = displacementInSignerInfo >= 0;
                for (int i = 0; (i < digestBytes.length) && verified; i++) {
                    verified = decBytes[i + displacementInSignerInfo] == digestBytes[i];
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            } catch (IllegalStateException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            } catch (BadPaddingException e) {
                e.printStackTrace();
                log.println(e.getMessage());
            }
        } catch (CertificateException e) {
            e.printStackTrace();
            log.println(e.getMessage());
        }
        if (verified) log.println("Verifica OK"); else log.println("Verifica fallita");
    }

    /**
	 * Setter method
	 * 
	 * @param newCertificate
	 */
    private void setCertificate(byte[] newCertificate) {
        certificate = newCertificate;
    }

    /**
	 * Setter method
	 * 
	 * @param newCryptokiLib
	 */
    private void setCryptokiLib(java.lang.String newCryptokiLib) {
        cryptokiLib = newCryptokiLib;
    }

    /**
	 * Setter method
	 * 
	 * @param data
	 */
    public void setEncodedDigest(String data) {
        this.encodedDigest = data;
    }

    /**
	 * Setter method
	 * 
	 * @param newEncryptedDigest
	 */
    public void setEncryptedDigest(byte[] newEncryptedDigest) {
        encryptedDigest = newEncryptedDigest;
    }

    /**
	 * Setter method *
	 * 
	 * @param newSignerLabel
	 */
    private void setSignerLabel(java.lang.String newSignerLabel) {
        signerLabel = newSignerLabel;
    }

    /**
	 * Updates progress bar value and displays error alerts
	 * 
	 * @param code
	 *            The status value
	 * @param statusString
	 *            The status description
	 */
    void setStatus(int code, String statusString) {
        if (code == DigestSignTask.ERROR) {
            pwd.setText("");
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null, statusString, "Errore!", JOptionPane.ERROR_MESSAGE);
            code = 0;
        }
        progressBar.setValue(code);
        progressBar.setString(statusString);
    }

    /**
	 * Initializes and starts the sign task.
	 */
    public void sign() {
        if (getEncodedDigest() == null) setStatus(ERROR, "Digest non impostato"); else {
            long mechanism = -1L;
            if (ENCRYPTION_RSA.equals(this.encAlg)) if (this.makeDigestOnToken) {
                if (DIGEST_MD5.equals(this.digestAlg)) mechanism = PKCS11Constants.CKM_MD5_RSA_PKCS; else if (DIGEST_SHA1.equals(this.digestAlg)) mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS; else if (DIGEST_SHA256.equals(this.digestAlg)) mechanism = PKCS11Constants.CKM_SHA256_RSA_PKCS;
            } else mechanism = PKCS11Constants.CKM_RSA_PKCS;
            if (mechanism == -1L) setStatus(DigestSignTask.ERROR, "Impossibile determinare il meccanismo!"); else {
                dsTask.setMechanism(mechanism);
                enableControls(false);
                dsTask.setCertificate(getCertificate());
                dsTask.setDigest(decodeToBytes(getEncodedDigest()));
                dsTask.setPassword(pwd.getPassword());
                dsTask.go();
                timer.start();
            }
        }
    }

    /**
	 * Called to start the applet. You never need to call this method directly,
	 * it is called when the applet's document is visited.
	 * 
	 * @see #init
	 * @see #stop
	 * @see #destroy
	 */
    public void start() {
        super.start();
        System.out.println("Starting applet ...");
    }

    /**
	 * Called to stop the applet. It is called when the applet's document is no
	 * longer on the screen. It is guaranteed to be called before destroy() is
	 * called. You never need to call this method directly.
	 * 
	 * @see #init
	 * @see #start
	 * @see #destroy
	 */
    public void stop() {
        super.stop();
        System.out.println("stopping...");
    }

    /**
	 * Calls the javascript submit function on the embedding page.
	 * 
	 */
    private void submitForm() {
        try {
            JSObject win = JSObject.getWindow(this);
            JSObject doc = (JSObject) win.getMember("document");
            doc.eval("eseguiSubmit();");
            setStatus(DigestSignTask.VERIFY_DONE, "Invio dati ...");
        } catch (netscape.javascript.JSException e) {
            setStatus(DigestSignTask.ERROR, "Errore JSO: " + e);
        }
    }

    /**
	 * returns an hex dump of the supplied <code>byte[]</code>
	 * 
	 * @param bytes
	 *            the data to show
	 * @return a String containing the dump
	 */
    String formatAsHexString(byte[] bytes) {
        int n, x;
        String w = new String();
        String s = new String();
        for (n = 0; n < bytes.length; n++) {
            x = (int) (0x000000FF & bytes[n]);
            w = Integer.toHexString(x).toUpperCase();
            if (w.length() == 1) w = "0" + w;
            s = s + w + ((n + 1) % 16 == 0 ? "\n" : " ");
        }
        return s;
    }

    /**
	 * This triggers the PCSC wrapper stuff; a {@link PCSCHelper} class is used
	 * to detect reader and token presence, trying also to provide a candidate
	 * PKCS#11 cryptoki for it; detection is bypassed if an applet parameter
	 * forcing cryptoki selection is provided.
	 * 
	 * @return true if a token with corresponding candidate cryptoki was
	 *         detected.
	 * @throws IOException
	 */
    private boolean detectCardAndCriptoki() throws IOException {
        CardInfo ci = null;
        boolean cardPresent = false;
        log.println("\n\n========= DETECTING CARD ===========");
        log.println("Resetting cryptoki name");
        setCryptokiLib(null);
        if (getParameter("cryptokilib") != null) {
            log.println("Getting cryptoki name from Applet parameter 'cryptokilib': " + getParameter("cryptokilib"));
            setCryptokiLib(getParameter("cryptokilib"));
        } else {
            log.println("Trying to detect card via PCSC ...");
            PCSCHelper pcsc = new PCSCHelper(true);
            java.util.List cards = pcsc.findCards();
            cardPresent = !cards.isEmpty();
            if (cardPresent) {
                ci = (CardInfo) cards.get(0);
                log.println("\n\nFor signing we will use card: '" + ci.getProperty("description") + "' with criptoki '" + ci.getProperty("lib") + "'");
                setCryptokiLib(ci.getProperty("lib"));
            } else log.println("Sorry, no card detected!");
        }
        log.println("=================================");
        return ((ci != null) || (getCryptokiLib() != null));
    }

    private long algToMechanism(boolean digestOnToken, String digestAlg, String encryptionAlg) {
        long mechanism = -1L;
        if (ENCRYPTION_RSA.equals(encryptionAlg)) if (digestOnToken) {
            if (DIGEST_MD5.equals(digestAlg)) mechanism = PKCS11Constants.CKM_MD5_RSA_PKCS; else if (DIGEST_SHA1.equals(digestAlg)) mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS; else if (DIGEST_SHA256.equals(digestAlg)) mechanism = PKCS11Constants.CKM_SHA256_RSA_PKCS;
        } else mechanism = PKCS11Constants.CKM_RSA_PKCS;
        return mechanism;
    }

    public java.security.cert.X509Certificate getJavaCertificate() throws CertificateException {
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(getCertificate());
        java.security.cert.X509Certificate javaCert = (java.security.cert.X509Certificate) cf.generateCertificate(bais);
        return javaCert;
    }

    private void findCert() {
        long mechanism = algToMechanism(this.makeDigestOnToken, this.digestAlg, this.encAlg);
        if (mechanism == -1L) setStatus(DigestSignTask.ERROR, "Impossibile determinare il meccanismo!"); else {
            initStatus(0, FindCertTask.FIND_MAXIMUM);
            certTask = new FindCertTask(getCryptokiLib(), getSignerLabel(), log);
            findTimer = new Timer(ONE_SECOND, new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setStatus(certTask.getCurrent(), certTask.getMessage());
                    if (!certTask.isTokenPresent()) {
                        progressBar.setIndeterminate(true);
                        enableControls(false);
                    } else {
                        progressBar.setIndeterminate(false);
                    }
                    if (certTask.done()) {
                        findTimer.stop();
                        progressBar.setValue(progressBar.getMinimum());
                        if (certTask.getCurrent() == FindCertTask.FIND_DONE) {
                            Toolkit.getDefaultToolkit().beep();
                            setCertificate(certTask.getCertificate());
                            try {
                                certArea.setText(getJavaCertificate().getSubjectDN().toString());
                            } catch (CertificateException e) {
                                log.println("Error getting certificate Subject DN");
                            }
                        }
                        if (retriveEncodedDigestFromServer()) {
                            enableControls(true);
                            setStatus(DigestSignTask.RESET, "Inserire il pin e battere INVIO per firmare.");
                        }
                    }
                }
            });
        }
        certTask.setMechanism(mechanism);
        certTask.go();
        findTimer.start();
    }

    private boolean retriveEncodedDigestFromServer() {
        boolean retrieved = false;
        try {
            log.println("POSTing certificate and getting Digest...");
            String base64Certificate = encodeFromBytes(getCertificate());
            String data = URLEncoder.encode("certificate", "UTF-8") + "=" + URLEncoder.encode(base64Certificate, "UTF-8");
            URL url = new URL(this.getDocumentBase(), getDigestPath());
            log.println("POSTing to: " + url);
            String result = httpPOST(url, data);
            if (result == null) {
                log.println("No data received frome server");
                setStatus(DigestSignTask.ERROR, "Errore nell'invio dei dati al server!");
            } else {
                log.println("POST result: " + result);
                StringTokenizer st = new StringTokenizer(result, ",");
                if (st.hasMoreTokens()) {
                    String receivedEncoding = st.nextToken();
                    if (decode(receivedEncoding) == null) setStatus(DigestSignTask.ERROR, "Errore nella decodifica del digest ricevuto dal server!"); else {
                        setEncodedDigest(receivedEncoding);
                        if (st.hasMoreTokens()) {
                            String receivedTime = decode(st.nextToken());
                            if (receivedTime == null) setStatus(ERROR, "Errore nella decodifica dell'ora ricevuta dal server!"); else {
                                this.signingTimeGMT.setText(receivedTime);
                                retrieved = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.println("Error POSTing data: " + e);
            setStatus(DigestSignTask.ERROR, "Errore nell'invio dei dati al server!");
        }
        return retrieved;
    }

    private String httpPOST(URL url, String data) throws IOException {
        String result = null;
        URLConnection conn = null;
        if ("http".equals(url.getProtocol())) conn = (HttpURLConnection) url.openConnection();
        if ("https".equals(url.getProtocol())) conn = (HttpsURLConnection) url.openConnection();
        if (conn != null) {
            log.println("Connection opened.");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            log.println("Data sent.");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            if ((line = rd.readLine()) != null) result = line;
            wr.close();
            rd.close();
        }
        return result;
    }
}
