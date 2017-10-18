package org.infoeng.ofbiz.ltans.afm.document;

import javax.security.auth.x500.*;
import java.security.*;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.*;
import org.bouncycastle.asn1.x509.*;
import org.infoeng.ofbiz.ltans.util.*;

/**
 *
 <pre>
  KeyedDocumentDigest ::= SEQUENCE { 
    documentName Name,
    documentKey DocumentKey,
    documentDigests SEQUENCE OF DigestInfo
  }
 </pre>
 *
 */
public class KeyedDocumentDigest implements DEREncodable {

    private X500Principal documentName;

    private DEREncodable documentKey;

    private ASN1EncodableVector documentsDigestVector;

    public KeyedDocumentDigest(X500Principal p, DEREncodable k, ASN1EncodableVector v) {
        this.documentName = p;
        this.documentKey = k;
        this.documentsDigestVector = v;
    }

    public KeyedDocumentDigest(KeyedDocument keyedDoc, String digestAlg) throws Exception {
        this.documentName = keyedDoc.getDocumentName();
        this.documentKey = keyedDoc.getDocumentKey();
        this.documentsDigestVector = new ASN1EncodableVector();
        MessageDigest md = MessageDigest.getInstance(digestAlg);
        AlgorithmIdentifier algId = LtansUtils.getAlgorithmIdentifier(digestAlg);
        for (int x = 0; x < keyedDoc.getDocumentCount(); x++) {
            md.reset();
            byte[] digestVal = md.digest(keyedDoc.getDocument(x).getOctets());
            documentsDigestVector.add(new DigestInfo(algId, digestVal));
        }
    }

    public static KeyedDocumentDigest getInstance(Object obj) {
        if (obj instanceof ASN1Sequence) {
            ASN1Sequence seq = (ASN1Sequence) obj;
            ASN1EncodableVector v = new ASN1EncodableVector();
            ASN1Sequence seqTwo = DERSequence.getInstance(seq.getObjectAt(2));
            for (int x = 0; x < seqTwo.size(); x++) {
                v.add(seqTwo.getObjectAt(x));
            }
            return new KeyedDocumentDigest(new X500Principal(X509Name.getInstance(seq.getObjectAt(0)).toString()), (DEREncodable) seq.getObjectAt(1), v);
        } else return null;
    }

    public int getDigestInfoCount() {
        return documentsDigestVector.size();
    }

    public DigestInfo getDigestInfo(int x) {
        return (DigestInfo) documentsDigestVector.get(x);
    }

    public X500Principal getDocumentName() {
        return documentName;
    }

    public DEREncodable getDocumentKey() {
        return documentKey;
    }

    public DERObject getDERObject() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new X509Name(documentName.getName()));
        v.add(documentKey);
        v.add(new DERSequence(documentsDigestVector));
        return new DERSequence(v).getDERObject();
    }
}
