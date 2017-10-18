package org.infoeng.ofbiz.ltans.test.ers;

import org.infoeng.ofbiz.ltans.ers.MerkleTree;
import org.infoeng.ofbiz.ltans.ers.PartialHashtree;
import java.security.MessageDigest;
import junit.framework.TestCase;
import java.util.ArrayList;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.security.Security;
import org.apache.commons.codec.binary.Base64;

public class MerkleTreeTest extends TestCase {

    private String testCertPathPrefix = "data/cert-file-";

    private String testCertPathSuffix = ".pem";

    private int testCertPathInt = 27147;

    private MerkleTree mt;

    private BouncyCastleProvider bcp;

    public MerkleTreeTest(String name) {
        super(name);
        bcp = new BouncyCastleProvider();
        Security.addProvider(bcp);
    }

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
    }

    public void testCreateMerkleTree() {
        try {
            int incInt = testCertPathInt;
            ArrayList<X509CertificateObject> certList = new ArrayList<X509CertificateObject>();
            MessageDigest sha = null;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (Exception e) {
            }
            while (true) {
                String testCertPath = testCertPathPrefix + incInt + testCertPathSuffix;
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCertPath);
                if (is == null) {
                    break;
                } else if (is != null) {
                    incInt++;
                    PEMReader pemReader = new PEMReader(new InputStreamReader(is));
                    X509CertificateObject x509obj = (X509CertificateObject) pemReader.readObject();
                    certList.add(x509obj);
                }
            }
            ArrayList<byte[]> digestList = new ArrayList<byte[]>();
            int certSize = certList.size();
            for (int m = 0; m < certSize; m++) {
                X509CertificateObject obj = certList.get(m);
                byte[] objBytes = obj.getEncoded();
                String encCertStr = new String(Base64.encodeBase64(objBytes, false));
                byte[] digestBytes = sha.digest(objBytes);
                encCertStr = new String(Base64.encodeBase64(digestBytes, false));
                digestList.add(digestBytes);
            }
            MerkleTree mt = MerkleTree.buildTree(digestList);
            byte[] calculatedRootBytes = mt.getRootDigest();
            System.out.println("Root signature is (Base64-encoded): " + new String(Base64.encodeBase64(calculatedRootBytes, false)) + ".");
            MerkleTree.Node[] leafNodes = mt.getLevel(new Integer(0));
            PartialHashtree pt = mt.getAuthenticationPath(leafNodes[6]);
            byte[] rootVal = pt.verifyLeaf(leafNodes[6].getBytes(), 6);
            System.out.println("Calculated root value from MT:       " + new String(Base64.encodeBase64(calculatedRootBytes, false)) + ".");
            System.out.println("Calculated root value from AuthPath: " + new String(Base64.encodeBase64(rootVal, false)) + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
