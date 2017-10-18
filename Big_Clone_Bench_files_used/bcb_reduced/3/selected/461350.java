package cz.cvut.fel.mvod.crypto;

import cz.cvut.fel.mvod.crypto.Base64;
import cz.cvut.fel.mvod.global.GlobalSettingsAndNotifier;
import cz.cvut.fel.mvod.gui.ErrorDialog;
import cz.cvut.fel.mvod.gui.PleaseWaitDialog;
import cz.cvut.fel.mvod.gui.settings.panels.PrologueSettingsPanel;
import cz.cvut.fel.mvod.prologueServer.PrologueServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import cz.cvut.fel.mvod.net.Server;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * This class handles certificate changes - loading/unloading, checks decrypting.
 * @author Radovan Murin
 */
public class CertManager {

    public static final String DefaultCertPath = "certs/";

    public static final int PROLOGUE = 0;

    public static final int VOTING = 1;

    private static String certPrologue = null;

    private static String hashPrologue = null;

    private static String certVoting = null;

    private static String hashVoting = null;

    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException ex) {
            new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("FATAL") + "\nCertManager\n" + ex.toString(), false).setVisible(true);
        }
    }

    private CertManager() {
    }

    /**
     * Starts a certificate change on a particular system.
     * @param system the system to have the certificate settings changed.
     * @throws FileNotFoundException thrown if the file cannot be found.
     */
    public static void changeCert(int system) throws FileNotFoundException {
        boolean loadOK = false;
        String passphrase = "11111";
        int tries = 3;
        JFileChooser fc = null;
        int returnVal = 0;
        File file;
        while (!loadOK) {
            if (tries == 3) {
                fc = new JFileChooser();
                returnVal = fc.showOpenDialog(null);
                file = null;
                tries = 0;
            }
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    file = fc.getSelectedFile();
                    KeyStore ks = KeyStore.getInstance("PKCS12");
                    ks.load(new FileInputStream(file.getAbsolutePath()), passphrase.toCharArray());
                    loadInfo(ks, system, passphrase, file);
                    loadOK = true;
                } catch (KeyStoreException ex) {
                    new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certFormErr") + "\nCertManager\n" + ex.toString()).setVisible(true);
                } catch (IOException ex) {
                    passphrase = JOptionPane.showInputDialog(null, GlobalSettingsAndNotifier.singleton.messages.getString("certPassChallLabel"), GlobalSettingsAndNotifier.singleton.messages.getString("certPassChallTitle"), JOptionPane.WARNING_MESSAGE);
                    tries++;
                } catch (NoSuchAlgorithmException ex) {
                    new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("FATAL") + "\nCertManager\n" + ex.toString(), false).setVisible(true);
                } catch (CertificateException ex) {
                    new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certFail") + "\nCertManager\n" + ex.toString(), false).setVisible(true);
                }
            }
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                break;
            }
        }
    }

    private static void loadInfo(KeyStore ks, int system, String passphrase, File file) {
        switch(system) {
            case PROLOGUE:
                {
                    try {
                        certPrologue = ks.getCertificate(ks.aliases().nextElement()).toString();
                        md.update(ks.getCertificate(ks.aliases().nextElement()).getEncoded());
                        hashPrologue = hexify(md.digest());
                        GlobalSettingsAndNotifier.singleton.modifySettings("Prologue_certpath", file.getAbsolutePath(), true);
                        PrologueServer.setCertPass(passphrase);
                        break;
                    } catch (CertificateEncodingException ex) {
                        new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certFail") + "\nCertManager\n" + ex.toString(), false).setVisible(true);
                    } catch (KeyStoreException ex) {
                        new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("FATAL") + "\nCertManager\n" + ex.toString(), true).setVisible(true);
                    }
                }
            case VOTING:
                {
                    try {
                        certVoting = ks.getCertificate(ks.aliases().nextElement()).toString();
                        md.update(ks.getCertificate(ks.aliases().nextElement()).getEncoded());
                        hashVoting = hexify(md.digest());
                        GlobalSettingsAndNotifier.singleton.modifySettings("Voting_certpath", file.getAbsolutePath(), true);
                        Server.setCertPass(passphrase);
                        break;
                    } catch (CertificateEncodingException ex) {
                        new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certFail") + "\nCertManager\n" + ex.toString(), false).setVisible(true);
                    } catch (KeyStoreException ex) {
                        new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("FATAL") + "\nCertManager\n" + ex.toString(), true).setVisible(true);
                    }
                }
        }
    }

    /**
     * Returns the Common name for a specific system
     * @param system the specific system
     * @return returns the CN
     */
    public static String getCN(int system) {
        Pattern mp = Pattern.compile("CN=[^,]*, ");
        String cert = "N/A";
        Matcher m = null;
        switch(system) {
            case -1:
                {
                    cert = getCertString(DefaultCertPath + "server.p12", "12345");
                    break;
                }
            case PROLOGUE:
                {
                    if (GlobalSettingsAndNotifier.singleton.getSetting("Prologue_USEDEFAULTCERT").equals("true")) {
                        if (certPrologue == null) {
                            return "N/A";
                        }
                        cert = certPrologue;
                        break;
                    } else {
                        cert = getCertString(DefaultCertPath + "server.p12", "12345");
                        break;
                    }
                }
            case VOTING:
                {
                    if (GlobalSettingsAndNotifier.singleton.getSetting("Voting_useEmbedded").equals("true")) {
                        if (certVoting == null) {
                            return "N/A";
                        }
                        cert = certVoting;
                        break;
                    } else {
                        cert = getCertString(DefaultCertPath + "server.p12", "12345");
                    }
                }
        }
        m = mp.matcher(cert);
        m.find();
        String match = null;
        try {
            match = m.group();
        } catch (Exception ex) {
            new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certFail"));
        }
        if (match == null) return "N/A";
        String res = match.replaceAll("Sign.*", "").replace("CN=", "").replace(",", "");
        return res;
    }

    /**
     * Returns the fingerprint of the certificate
     * @param system the system to which the certificate is attached to
     * @return the requested FP
     */
    public static String getFingerPrint(int system) {
        String ret = "";
        String HASH = "";
        switch(system) {
            case -1:
                {
                    byte[] crt = getCertBytes(DefaultCertPath + "server.p12", "12345");
                    md.update(crt);
                    HASH = hexify(md.digest());
                    break;
                }
            case PROLOGUE:
                {
                    if (GlobalSettingsAndNotifier.singleton.getSetting("Prologue_USEDEFAULTCERT").equals("true")) {
                        if (certPrologue == null) {
                            return "N/A";
                        }
                        HASH = hashPrologue;
                        break;
                    } else {
                        byte[] crt = getCertBytes(DefaultCertPath + "server.p12", "12345");
                        md.update(crt);
                        HASH = hexify(md.digest());
                        break;
                    }
                }
            case VOTING:
                {
                    if (GlobalSettingsAndNotifier.singleton.getSetting("Voting_useEmbedded").equals("true")) {
                        if (certVoting == null) {
                            return "N/A";
                        }
                        HASH = hashVoting;
                        break;
                    } else {
                        byte[] crt = getCertBytes(DefaultCertPath + "server.p12", "12345");
                        md.update(crt);
                        HASH = hexify(md.digest());
                    }
                }
        }
        return HASH;
    }

    public static void generateDefault() {
        File crtDir = new File(DefaultCertPath);
        Process process = null;
        crtDir.mkdir();
        File cert = new File(DefaultCertPath + "server.p12");
        if (!cert.exists()) {
            PleaseWaitDialog plw = new PleaseWaitDialog();
            plw.setVisible(true);
            try {
                if (System.getProperty("os.name").contains("Windows")) {
                    process = Runtime.getRuntime().exec("genCert.bat");
                } else if (System.getProperty("os.name").contains("Linux")) {
                    process = Runtime.getRuntime().exec("genCert.sh");
                } else {
                    System.out.println("Cannot generate a deafault certificate on this system, and cannot generat it. Certifocates are generated only on Linux and Winodws syustem" + "This is an unsupporte system. If you wish to continue, please create a PKCS12 certificate named server.p12, password:12345 and put it into certs directory in the application root." + "Where .jar file is.");
                    System.out.println("UNSUPPORTED OS");
                    System.exit(-1);
                }
                try {
                    process.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certGenFail") + "\n CertManager \n" + ex.toString(), false).setVisible(true);
            }
            plw.setVisible(false);
        }
        if (!cert.exists()) {
            new ErrorDialog(null, true, GlobalSettingsAndNotifier.singleton.messages.getString("certGenFail") + "\n CertManager \n", false).setVisible(true);
        }
    }

    /**
     *
     * Gets the hex code from a byte array
     * http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-
     * x509-certificates-thumbprint-in-java
     *
     * @param bytes
     * @return a string of hex data
     */
    public static String hexify(byte bytes[]) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
            if (i % 2 == 0) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    private static String getCertString(String path, String pass) {
        if (path == null || pass == null) return "CN=N/A,";
        Certificate c = getCertificate(path, pass);
        if (c == null) return "CN=N/A,";
        return c.toString();
    }

    private static byte[] getCertBytes(String path, String pass) {
        Certificate c = getCertificate(path, pass);
        if (c == null) return new byte[] { 1 };
        try {
            return c.getEncoded();
        } catch (CertificateEncodingException ex) {
            Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static Certificate getCertificate(String path, String pass) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(file.getAbsolutePath()), pass.toCharArray());
            Certificate ce = ks.getCertificate(ks.aliases().nextElement());
            return ce;
        } catch (KeyStoreException ex) {
            Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CertificateException ex) {
            Logger.getLogger(CertManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
