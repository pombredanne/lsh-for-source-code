package de.shandschuh.jaolt.gui.dialogs.certificate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import javax.swing.JLabel;
import com.jgoodies.forms.builder.PanelBuilder;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.gui.FormManager;

public class CertificateFormManager extends FormManager {

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private X509Certificate x509Certificate;

    private String[] subjectValues;

    private String[] issuerValues;

    private int issuerSkip;

    private int skip;

    public CertificateFormManager(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        subjectValues = x509Certificate.getSubjectDN().toString().split(",(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");
        issuerValues = x509Certificate.getIssuerDN().toString().split(",(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");
    }

    @Override
    protected void addPanelBuilderComponents(PanelBuilder panelBuilder) {
        panelBuilder.add(new JLabel(Language.translateStatic("QUESTION_ACCEPTCERTIFICATE")), getCellConstraints(1, 1, 6));
        panelBuilder.addSeparator(Language.translateStatic("CERTIFICATE"), getCellConstraints(1, 3, 7));
        panelBuilder.add(new JLabel("sha1"), getCellConstraints(2, 5));
        byte[] encoded = new byte[0];
        try {
            encoded = x509Certificate.getEncoded();
        } catch (CertificateEncodingException e) {
        }
        try {
            panelBuilder.add(new JLabel(toHexString(MessageDigest.getInstance("SHA1").digest(encoded))), getCellConstraints(4, 5));
        } catch (NoSuchAlgorithmException e) {
            panelBuilder.add(new JLabel(Language.translateStatic("ERROR_SHA1NOTFOUND")), getCellConstraints(4, 5));
        }
        panelBuilder.add(new JLabel("md5"), getCellConstraints(2, 7));
        try {
            panelBuilder.add(new JLabel(toHexString(MessageDigest.getInstance("MD5").digest(encoded))), getCellConstraints(4, 7));
        } catch (NoSuchAlgorithmException e) {
            panelBuilder.add(new JLabel(Language.translateStatic("ERROR_MD5NOTFOUND")), getCellConstraints(4, 7));
        }
        panelBuilder.addSeparator(Language.translateStatic("CERTIFICATE_OWNER"), getCellConstraints(1, 9, 7));
        if (subjectValues != null && subjectValues.length > 0) {
            for (int n = 0, i = subjectValues.length; n < i; n++) {
                int index = subjectValues[n].indexOf('=');
                panelBuilder.add(new JLabel(Language.translateStatic("CERTIFICATE_" + subjectValues[n].substring(0, index).trim())), getCellConstraints(2, 11 + 2 * n));
                panelBuilder.add(new JLabel(subjectValues[n].substring(index + 1).trim().replaceAll("\\\"", "")), getCellConstraints(4, 11 + 2 * n));
            }
        } else {
        }
        panelBuilder.addSeparator(Language.translateStatic("CERTIFICATE_ISSUER"), getCellConstraints(1, 11 + skip, 7));
        if (issuerValues != null && issuerValues.length > 0) {
            for (int n = 0, i = issuerValues.length; n < i; n++) {
                int index = issuerValues[n].indexOf('=');
                if (index > -1) {
                    panelBuilder.add(new JLabel(Language.translateStatic(("CERTIFICATE_" + issuerValues[n].substring(0, index).trim()).trim())), getCellConstraints(2, 13 + skip + 2 * n));
                    panelBuilder.add(new JLabel(issuerValues[n].substring(index + 1).trim().replaceAll("\\\"", "")), getCellConstraints(4, 13 + skip + 2 * n));
                }
            }
        } else {
        }
        panelBuilder.add(new JLabel(Language.translateStatic("HINT_CERTIFICATERESTART")), getCellConstraints(1, 15 + skip + issuerSkip, 6));
    }

    @Override
    protected String getColumnLayout() {
        return "5dlu, p, 4dlu, fill:p:grow, 4dlu, p, 4dlu";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected String getRowLayout() {
        StringBuffer stringBuffer = new StringBuffer();
        skip = 2 * getCount(subjectValues, stringBuffer);
        issuerSkip = 2 * getCount(issuerValues, stringBuffer);
        return "p, 10dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p" + stringBuffer.toString() + ", 10dlu, p";
    }

    @Override
    public boolean rebuildNeeded() {
        return false;
    }

    @Override
    protected void reloadLocal(boolean rebuild) {
    }

    @Override
    protected void saveLocal() throws Exception {
    }

    @Override
    protected void validateLocal() throws Exception {
    }

    private static int getCount(String[] array, StringBuffer stringBuffer) {
        int count = 0;
        if (array != null && array.length > 0) {
            for (int n = 0, i = array.length; n < i; n++) {
                if (array[n].indexOf('=') > -1) {
                    stringBuffer.append(", 3dlu, p");
                    count++;
                }
            }
        }
        return count;
    }

    private static String toHexString(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int n = 0, i = bytes.length; n < i; n++) {
            int b = bytes[n];
            b &= 0xff;
            stringBuffer.append(HEX_DIGITS[b >> 4]);
            stringBuffer.append(HEX_DIGITS[b & 15]);
            stringBuffer.append(' ');
        }
        return stringBuffer.toString();
    }
}
