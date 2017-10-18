package org.infoeng.ofbiz.ltans.ltap;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.infoeng.ofbiz.ltans.LTANSObject;
import org.infoeng.ofbiz.ltans.ltap.*;
import org.infoeng.ofbiz.ltans.util.*;
import java.io.*;
import java.security.*;
import java.util.*;

/**
 *  Long-term Archive Protocol Data class.
 *  @see <a href="http://tools.ietf.org/id/draft-ietf-ltans-ltap-04.txt">Long-term Archive Protocol (LTAP)</a>
 *
   <pre>
    Data ::= SEQUENCE {
        dataReference  DataOrTransaction,
        metaData       MetaData,
        messageImprint MessageDigest }
   </pre>
   <pre>
    DataOrTransaction ::= CHOICE {
    data      RawData,
    artifact  Artifact,
    reference IA5String   }
   </pre>
   <pre>
    Artifact ::= PrintableString
   </pre>
   <pre>
    RawData ::= CHOICE {
      opaque     OCTET STRING,
      string     UTF8String,
      structured MetaData }
   </pre>
 * 
 */
public class Data extends LTANSObject implements DEREncodable {

    private DEREncodable dataReference;

    private MetaData metaData;

    private LTAPMessageDigest messageImprint;

    public Data() {
    }

    public Data(DEREncodable dataReferenceIn, MetaData metaDataIn, LTAPMessageDigest messageDigest) {
        this.dataReference = dataReferenceIn;
        this.metaData = metaDataIn;
        this.messageImprint = messageDigest;
    }

    public Data(Object obj) {
        if (obj instanceof ASN1Sequence) {
            ASN1Sequence seq = (ASN1Sequence) obj;
            if (seq.size() == 3) {
                dataReference = seq.getObjectAt(0);
                metaData = MetaData.getInstance(seq.getObjectAt(1));
                messageImprint = LTAPMessageDigest.getInstance(seq.getObjectAt(2));
            }
        } else {
            throw new IllegalArgumentException("Illegal argument type: " + obj.getClass().getName() + "");
        }
    }

    public Data(InputStream dataIS, MetaData metaDataIn) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageDigest md = MessageDigest.getInstance(LtansUtils.DIGEST_ALGORITHM);
        int x = -1;
        while ((x = dataIS.read()) != -1) baos.write(x);
        this.dataReference = new DEROctetString(baos.toByteArray());
        this.metaData = metaDataIn;
        this.messageImprint = LTAPMessageDigest.digest(new DEROctetString(baos.toByteArray()));
    }

    public static Data getInstance(Object obj) {
        return new Data(obj);
    }

    public DEROctetString getOctetString() {
        return new DEROctetString(LtansUtils.getDERObjectBytes(dataReference));
    }

    public MetaData getMetaData() {
        return this.metaData;
    }

    public LTAPMessageDigest getMessageDigest() {
        return this.messageImprint;
    }

    public byte[] getDataBytes() {
        return LtansUtils.getDERObjectBytes(this);
    }

    public DERObject getDERObject() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(dataReference);
        v.add(metaData);
        v.add(messageImprint);
        return new DERSequence(v);
    }
}
