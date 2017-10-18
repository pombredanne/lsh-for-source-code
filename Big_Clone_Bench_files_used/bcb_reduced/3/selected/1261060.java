package org.infoeng.ofbiz.ltans.ers;

import java.security.MessageDigest;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import java.io.ByteArrayOutputStream;
import org.bouncycastle.util.encoders.Base64;
import org.infoeng.ofbiz.ltans.util.LtansUtils;
import org.infoeng.ofbiz.ltans.LTANSObject;

/** 
 *
 *   <pre>
 *     PartialHashtree ::= SEQUENCE OF OCTET STRING
 *   </pre>
 *
 */
public class PartialHashtree extends LTANSObject implements DEREncodable {

    private ASN1EncodableVector vector;

    public PartialHashtree() {
        vector = new ASN1EncodableVector();
    }

    public PartialHashtree(Object obj) {
        vector = new ASN1EncodableVector();
        if (obj instanceof ASN1Sequence) {
            ASN1Sequence asn1Seq = (ASN1Sequence) obj;
            for (int x = 0; x < asn1Seq.size(); x++) {
                DEROctetString derOctStr = (DEROctetString) DEROctetString.getInstance(asn1Seq.getObjectAt(x));
                vector.add(derOctStr);
            }
        } else {
            throw new IllegalArgumentException("Object (class " + obj.getClass().getName() + ")");
        }
    }

    public static PartialHashtree getInstance(Object obj) {
        return new PartialHashtree(obj);
    }

    public void addOctetString(DEROctetString dos) {
        vector.add(dos);
    }

    public byte[] verifyLeaf(byte[] leafDigestValue, int position, String algorithm) {
        byte[] retDigestBytes = null;
        byte[] interimDigestBytes = LtansUtils.duplicateByteArray(leafDigestValue);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
        }
        int tmpPosition = position;
        int nodeNum = vector.size();
        if (nodeNum < 2) return null;
        for (int x = 0; x < nodeNum; x++) {
            DEROctetString derStr = (DEROctetString) vector.get(x);
            byte[] nodeBytes = derStr.getOctets();
            if ((tmpPosition % 2) == 0) {
                md.reset();
                md.update(interimDigestBytes);
                md.update(nodeBytes);
                interimDigestBytes = md.digest();
            } else if ((tmpPosition % 2) == 1) {
                md.reset();
                md.update(nodeBytes);
                md.update(interimDigestBytes);
                interimDigestBytes = md.digest();
            }
            tmpPosition = (int) Math.floor((double) (tmpPosition / 2));
        }
        return interimDigestBytes;
    }

    public byte[] verifyLeaf(byte[] leafDigestValue, int position) {
        return verifyLeaf(leafDigestValue, position, LtansUtils.DIGEST_ALGORITHM);
    }

    public DERObject getDERObject() {
        return new DERSequence(vector);
    }

    public String toString() {
        DERSequence derSeq = (DERSequence) getDERObject();
        int seqSize = vector.size();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new String("PartialHashtree with " + seqSize + " elements.\n").getBytes());
        } catch (Exception e) {
        }
        for (int x = 0; x < seqSize; x++) {
            DEROctetString derOctets = (DEROctetString) vector.get(x);
            byte[] derBytes = derOctets.getOctets();
            String derString = new String(Base64.encode(derBytes));
            try {
                baos.write(new String("String " + x + ": " + derString + "\n").getBytes());
            } catch (Exception e) {
            }
        }
        return baos.toString();
    }
}
