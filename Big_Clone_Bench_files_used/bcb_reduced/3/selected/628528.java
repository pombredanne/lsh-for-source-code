package org.infoeng.ofbiz.ltans.ltap;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.*;
import org.bouncycastle.asn1.x509.*;
import java.security.*;
import org.infoeng.ofbiz.ltans.LTANSObject;
import org.infoeng.ofbiz.ltans.util.*;

/**
 *
   <pre>
     MessageDigest ::= SEQUENCE {
       digestAlgorithm DigestMethodType,
       digestValue     DigestValueType 
     }
 
     DigestMethodType ::= AlgorithmIdentifier
     DigestValueType ::= OCTET STRING
   </pre>
   *
   */
public class LTAPMessageDigest extends LTANSObject implements DEREncodable {

    private DEROctetString digestValue;

    private AlgorithmIdentifier digestAlgorithm;

    private MessageDigest messageDigest;

    public LTAPMessageDigest(AlgorithmIdentifier algId, MessageDigest messageDigestObj) {
        this.digestAlgorithm = algId;
        this.messageDigest = messageDigestObj;
    }

    public LTAPMessageDigest(DEROctetString derStr, AlgorithmIdentifier algId) {
        this.digestValue = derStr;
        this.digestAlgorithm = algId;
    }

    public LTAPMessageDigest(Object obj) {
        if (obj instanceof ASN1Sequence) {
            ASN1Sequence seq = (ASN1Sequence) obj;
            this.digestAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
            this.digestValue = (DEROctetString) seq.getObjectAt(1);
        } else {
            throw new IllegalArgumentException("Object class: " + obj.getClass().getName() + " cannot be used as constructor.");
        }
    }

    public LTAPMessageDigest(AlgorithmIdentifier algId, MessageDigest md, DEREncodable rawData) throws Exception {
        this.digestAlgorithm = algId;
        this.digestValue = digestRawData(rawData);
    }

    public DEROctetString digestRawData(DEREncodable rd) throws Exception {
        byte[] dataBytes = LtansUtils.getDERObjectBytes(rd);
        byte[] digestBytes = MessageDigest.getInstance(LtansUtils.DIGEST_ALGORITHM).digest(dataBytes);
        return new DEROctetString(digestBytes);
    }

    public static LTAPMessageDigest digest(DEREncodable rd) throws Exception {
        byte[] dataBytes = LtansUtils.getDERObjectBytes(rd);
        byte[] digestBytes = MessageDigest.getInstance(LtansUtils.DIGEST_ALGORITHM).digest(dataBytes);
        return new LTAPMessageDigest(new DEROctetString(digestBytes), new AlgorithmIdentifier(LtansUtils.defaultDigestOID));
    }

    public static LTAPMessageDigest getInstance(Object obj) {
        return new LTAPMessageDigest(obj);
    }

    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return digestAlgorithm;
    }

    public DEROctetString getDigestValue() {
        return digestValue;
    }

    public DERObject getDERObject() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(digestAlgorithm);
        v.add(digestValue);
        return new DERSequence(v);
    }
}
