package org.infoeng.ofbiz.ltans.test.ers;

import junit.framework.TestCase;
import org.infoeng.ofbiz.ltans.ers.*;
import org.infoeng.ofbiz.ltans.LTANSObject;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;

public class EvidenceRecordTest extends TestCase {

    /**
     *       0512125.pdf                  draft-ietf-ltans-ers-13.txt       draft-ietf-ltans-ltap-04.txt      draft-nourse-scep-14.txt  rfc3280.txt  rfc4210.txt
     *       draft-ietf-ltans-ers-12.txt  draft-ietf-ltans-ers-scvp-02.txt  draft-ietf-ltans-validate-01.txt  rfc2560.txt               rfc3852.txt
     */
    private static final String[] inputFiles = { "rfc2560.txt", "rfc3280.txt", "rfc3852.txt", "rfc4210.txt" };

    private AlgorithmIdentifier[] algIds;

    private Attribute[] cryptoInfos;

    private EncryptionInfo encInfo;

    private MessageDigest digest;

    public EvidenceRecordTest() {
    }

    public void testEvidenceRecordTest() {
        for (int x = 0; x < inputFiles.length; x++) {
            InputStream inIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(inputFiles[x]);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                while (true) {
                    int m = inIS.read();
                    if (m == -1) break;
                    baos.write(m);
                }
            } catch (Exception e) {
            }
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
            }
            byte[] digestBytes = digest.digest(baos.toByteArray());
            String digestString = new String(Base64.encodeBase64(digestBytes));
            System.out.println(digestString);
        }
    }
}
