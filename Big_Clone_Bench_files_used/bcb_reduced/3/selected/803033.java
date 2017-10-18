package org.jpedal.objects.acroforms.gui;

import org.jpedal.objects.acroforms.gui.certificates.CertificateHolder;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.PdfDictionary;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Summary extends javax.swing.JPanel {

    private JDialog frame;

    private PdfObject sigObject;

    public void setValues(String signName, String reason, String signDate, String location) {
        signedByBox.setText(signName);
        reasonBox.setText(reason);
        String rawDate = sigObject.getTextStreamValue(PdfDictionary.M);
        StringBuffer date = new StringBuffer(rawDate);
        date.delete(0, 2);
        date.insert(4, '/');
        date.insert(7, '/');
        date.insert(10, ' ');
        date.insert(13, ':');
        date.insert(16, ':');
        date.insert(19, ' ');
        dateBox.setText(date.toString());
        locationBox.setText(location);
    }

    /**
     * Creates new form Signatures
     *
     * @param frame
     * @param sig
     */
    public Summary(JDialog frame, PdfObject sig) {
        this.frame = frame;
        this.sigObject = sig;
        initComponents();
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        locationBox = new javax.swing.JTextField();
        showCertificateButton = new javax.swing.JButton();
        signedByBox = new javax.swing.JTextField();
        reasonBox = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        dateBox = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        setLayout(null);
        jLabel1.setText("Location:");
        add(jLabel1);
        jLabel1.setBounds(310, 70, 70, 20);
        jLabel2.setText("Signed by:");
        add(jLabel2);
        jLabel2.setBounds(10, 10, 70, 20);
        jLabel3.setText("Reason:");
        add(jLabel3);
        jLabel3.setBounds(10, 40, 70, 20);
        locationBox.setEditable(false);
        add(locationBox);
        locationBox.setBounds(360, 70, 170, 20);
        showCertificateButton.setText("Show Certificate...");
        showCertificateButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCertificate(evt);
            }
        });
        add(showCertificateButton);
        showCertificateButton.setBounds(380, 10, 150, 23);
        signedByBox.setEditable(false);
        add(signedByBox);
        signedByBox.setBounds(70, 10, 300, 20);
        reasonBox.setEditable(false);
        add(reasonBox);
        reasonBox.setBounds(70, 40, 460, 20);
        jLabel4.setText("Date:");
        add(jLabel4);
        jLabel4.setBounds(10, 70, 70, 20);
        dateBox.setEditable(false);
        add(dateBox);
        dateBox.setBounds(70, 70, 230, 20);
        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close(evt);
            }
        });
        add(jButton1);
        jButton1.setBounds(433, 140, 90, 23);
    }

    private void close(java.awt.event.ActionEvent evt) {
        frame.setVisible(false);
    }

    private void showCertificate(java.awt.event.ActionEvent evt) {
        JDialog frame = new JDialog((JFrame) null, "Certificate Viewer", true);
        CertificateHolder ch = new CertificateHolder(frame);
        try {
            byte[] bytes = sigObject.getTextStreamValueAsByte(PdfDictionary.Cert);
            InputStream bais = new ByteArrayInputStream(bytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate signingCertificate = (X509Certificate) cf.generateCertificate(bais);
            bais.close();
            DateFormat format1 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
            Date notBefore = signingCertificate.getNotBefore();
            Date notAfter = signingCertificate.getNotAfter();
            String publicKey = byteToHex(signingCertificate.getPublicKey().getEncoded());
            String x509Data = byteToHex(signingCertificate.getEncoded());
            String sha1Digest = byteToHex(getDigest(bytes, "SHA1"));
            String md5Digest = byteToHex(getDigest(bytes, "MD5"));
            String keyDescription = signingCertificate.getPublicKey().toString();
            int keyDescriptionEnd = keyDescription.indexOf('\n');
            if (keyDescriptionEnd != -1) keyDescription = keyDescription.substring(0, keyDescriptionEnd);
            ch.setValues(sigObject.getTextStreamValue(PdfDictionary.Name), signingCertificate.getVersion(), signingCertificate.getSigAlgName(), signingCertificate.getSubjectX500Principal().toString(), signingCertificate.getIssuerX500Principal().toString(), signingCertificate.getSerialNumber(), format1.format(notBefore), format1.format(notAfter), keyDescription, publicKey, x509Data, sha1Digest, md5Digest);
            frame.getContentPane().add(ch);
            frame.setSize(440, 450);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (Exception e) {
        }
    }

    /**
     * @param bytes
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getDigest(byte[] bytes, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance(algorithm);
        sha1.update(bytes);
        byte[] sha1Res = sha1.digest();
        return sha1Res;
    }

    /**
     * @return
     */
    private static String byteToHex(byte[] bytes) {
        String hex = "";
        for (int i = 0; i < bytes.length; i++) {
            String singleByte = Integer.toHexString(bytes[i]);
            if (singleByte.startsWith("ffffff")) {
                singleByte = singleByte.substring(6, singleByte.length());
            } else if (singleByte.length() == 1) {
                singleByte = '0' + singleByte;
            }
            singleByte = singleByte.toUpperCase();
            hex += singleByte + ' ';
        }
        return hex;
    }

    private javax.swing.JTextField dateBox;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JTextField locationBox;

    private javax.swing.JTextField reasonBox;

    private javax.swing.JButton showCertificateButton;

    private javax.swing.JTextField signedByBox;
}
