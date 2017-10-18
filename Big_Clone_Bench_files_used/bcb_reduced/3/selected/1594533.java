package org.xmldap.xmldsig;

import nu.xom.Document;
import nu.xom.Element;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.xml.Canonicalizable;
import org.xmldap.xml.XmlUtils;
import junit.framework.TestCase;

public class SignedInfoTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSignedInfo() throws SerializationException {
        Element data = new Element("element");
        Reference reference = new Reference(data, "test1", null, "SHA");
        String signingAlgorithm = "SHA1withRSA";
        SignedInfo si = new SignedInfo(reference, signingAlgorithm);
        String signedInfoXml = si.toXML();
        assertEquals("<dsig:SignedInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /><dsig:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><dsig:Reference URI=\"#test1\"><dsig:Transforms><dsig:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><dsig:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></dsig:Transforms><dsig:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><dsig:DigestValue>iNgcKrAWdbb1H6FuoDemS5DGqsE=</dsig:DigestValue></dsig:Reference></dsig:SignedInfo>", signedInfoXml);
    }

    public void testSignedInfoPrefix() throws SerializationException {
        Element data = new Element("element");
        Reference reference = new Reference(data, "test1", null, "SHA");
        String signingAlgorithm = "SHA1withRSA";
        SignedInfo si = new SignedInfo(reference, signingAlgorithm);
        String inclusiveNamespacePrefixes = "dsig";
        si.setInclusiveNamespacePrefixList(inclusiveNamespacePrefixes);
        String signedInfoXml = si.toXML();
        assertEquals("<dsig:SignedInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"dsig\" /></dsig:CanonicalizationMethod><dsig:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><dsig:Reference URI=\"#test1\"><dsig:Transforms><dsig:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><dsig:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></dsig:Transforms><dsig:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><dsig:DigestValue>iNgcKrAWdbb1H6FuoDemS5DGqsE=</dsig:DigestValue></dsig:Reference></dsig:SignedInfo>", signedInfoXml);
    }

    public void testSignedInfoString() throws Exception {
        String signedInfoStr = "<dsig:SignedInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /><dsig:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><dsig:Reference URI=\"#uuid-7B20C5C0-9B85-35D1-590A-D1B3093451CF\"><dsig:Transforms><dsig:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><dsig:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></dsig:Transforms><dsig:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><dsig:DigestValue>P834/zjB6jZbz80UPkCJQ+IGoqk=</dsig:DigestValue></dsig:Reference></dsig:SignedInfo>";
        Document signedInfoDoc = XmlUtils.parse(signedInfoStr);
        Element signedInfo = signedInfoDoc.getRootElement();
        byte[] bytes = XmlUtils.canonicalize(signedInfo, Canonicalizable.EXCLUSIVE_CANONICAL_XML);
        try {
            String b64EncodedDigest = CryptoUtils.digest(bytes, "SHA");
            String expected = "kP5B9dvJTnb+sSLDdMkgj+UYjJM=";
            assertEquals(expected, b64EncodedDigest);
        } catch (CryptoException e) {
            throw new CryptoException(e);
        }
    }
}
