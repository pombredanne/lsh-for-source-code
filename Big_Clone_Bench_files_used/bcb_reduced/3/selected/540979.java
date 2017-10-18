package org.hibnet.lune.ui.ssl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class X509CertificateComposite extends Composite {

    private Label hostNameLabel;

    private Label validFromLabel;

    private Label validToLabel;

    private Label fingerprintLabel;

    private Label issuerLabel;

    public X509CertificateComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));
        Label label = new Label(this, SWT.NONE);
        label.setText("Hostname:");
        hostNameLabel = new Label(this, SWT.NONE);
        label = new Label(this, SWT.NONE);
        label.setText("Valid from:");
        validFromLabel = new Label(this, SWT.NONE);
        label = new Label(this, SWT.NONE);
        label.setText("Valid to:");
        validToLabel = new Label(this, SWT.NONE);
        label = new Label(this, SWT.NONE);
        label.setText("Issuer:");
        issuerLabel = new Label(this, SWT.NONE);
        label = new Label(this, SWT.NONE);
        label.setText("Fingerprint:");
        fingerprintLabel = new Label(this, SWT.NONE);
    }

    public void show(X509Certificate cert) {
        hostNameLabel.setText(cert.getSubjectDN().getName());
        validFromLabel.setText(cert.getNotBefore().toString());
        validToLabel.setText(cert.getNotAfter().toString());
        issuerLabel.setText(cert.getIssuerDN().getName());
        String fingerprint = "N/A";
        try {
            fingerprint = getCertFingerPrint("SHA1", cert);
        } catch (CertificateEncodingException e) {
            MessageDialog.openError(getShell(), "Enable to get the certificate data", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            MessageDialog.openError(getShell(), "Enable to compoute the SHA1 digest", e.getMessage());
        }
        fingerprintLabel.setText(fingerprint);
    }

    private static String getCertFingerPrint(String algorithm, Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
        byte encoded[] = certificate.getEncoded();
        MessageDigest messagedigest = MessageDigest.getInstance(algorithm);
        byte digest[] = messagedigest.digest(encoded);
        return toHexString(digest);
    }

    private static String toHexString(byte encoded[]) {
        StringBuilder builder = new StringBuilder();
        int i = encoded.length;
        for (int j = 0; j < i; j++) {
            byte2hex(encoded[j], builder);
            if (j < i - 1) {
                builder.append(":");
            }
        }
        return builder.toString();
    }

    private static void byte2hex(byte b, StringBuilder builder) {
        char ac[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int i = (b & 0xf0) >> 4;
        int j = b & 0xf;
        builder.append(ac[i]);
        builder.append(ac[j]);
    }
}
