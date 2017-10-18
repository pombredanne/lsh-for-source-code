package jp.go.aist.sot.client.security;

import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import jp.go.aist.sot.client.common.SignOnException;
import jp.go.aist.sot.client.resources.MessageHandler;
import jp.go.aist.sot.client.security.KeyStore;

public class CertificatePrintUtil {

    private static final MessageHandler mh = new MessageHandler("cert_details");

    public static String printDetails(X509Certificate cert) throws SignOnException {
        if (cert == null) {
            throw new SignOnException("cert is null");
        }
        try {
            return mh.getMessage("details_format", new Object[] { cert.getSubjectDN().toString(), cert.getIssuerDN().toString(), cert.getSerialNumber().toString(16), String.valueOf(cert.getVersion()), cert.getNotBefore().toString(), cert.getNotAfter().toString(), getCertFingerPrint("MD5", cert), getCertFingerPrint("SHA1", cert) });
        } catch (Exception e) {
            throw new SignOnException(e);
        }
    }

    private static String getCertFingerPrint(String mdAlg, Certificate cert) throws Exception {
        byte[] encCertInfo = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance(mdAlg);
        byte[] digest = md.digest(encCertInfo);
        return toHexString(digest);
    }

    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
}
