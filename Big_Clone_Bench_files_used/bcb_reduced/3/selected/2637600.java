package org.javasign.cards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.sql.Date;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.javasign.interfaces.CardSigner;
import org.javasign.util.ApduData;
import com.jaccal.Atr;
import com.jaccal.CardException;
import com.jaccal.CardResponse;
import com.jaccal.Session;
import com.jaccal.SessionFactory;
import com.jaccal.command.ApduCmd;

public class SiemensSigner implements CardSigner {

    static byte[] OK = { (byte) 0x90, (byte) 0x00 };

    public static void main(String[] args) {
        SessionFactory factory;
        Session session;
        Atr atr = null;
        ;
        ApduCmd cmd = null;
        CardResponse response = null;
        byte[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        String MF = "3F00";
        try {
            Security.addProvider(new BouncyCastleProvider());
            factory = SessionFactory.getInstance();
            session = factory.createSession("default");
            System.out.println("Power on");
            atr = session.open();
            System.out.println(atr.toString());
            String cerID = "C111";
            String BsoID = "B1";
            String pubKeyObjID = "A1";
            String seID = "32";
            String PIN = "12345678";
            String pinID = "45";
            SiemensSigner s = new SiemensSigner();
            byte[] content = { 'l', 'a', 'r', 'a' };
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            hash.update(content);
            byte[] digest = hash.digest();
            RSAPublicKeySpec pubKeySpec = s.generateBSO(PIN, pinID, BsoID, pubKeyObjID, session);
            if (pubKeySpec == null) return;
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            X509Certificate cert = s.generateX509Certificate(PIN, pinID, pubKey, BsoID, seID, session);
            FileOutputStream efoss = new FileOutputStream("card.cer");
            efoss.write(cert.getEncoded());
            efoss.close();
            s.storeX509Certificate(cerID, cert, session);
            s.sign(PIN, pinID, seID, BsoID, digest, session);
            System.out.println("Power off");
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RSAPublicKeySpec generateBSO(String PIN, String pinID, String bsoID, String pubID, Session session) throws CardException, IOException {
        ApduCmd cmd;
        CardResponse response;
        String tlvID = "E1E1";
        String apdu = "";
        String hPIN = ApduData.toHexString(PIN.getBytes());
        String pp = "830200" + pinID + "8508020F870FFFFF0008" + "860700000000000000" + "8F" + ApduData.hexLength(hPIN) + hPIN;
        apdu = "00DA016E" + ApduData.hexLength(pp) + pp;
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "00DA016E6D830220" + bsoID + "8508220F0C0FFFFF00008607" + pinID + "0000000000008B10FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF8F424100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "00DA016E6D830221" + bsoID + "8508020F0C0FFFFF00008607" + pinID + "0000000000008B10FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF8F424100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "00E00000396F378102008C820305FF008302" + tlvID + "850301000086090000000000000000008B18FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "004600000820" + bsoID + tlvID + "00100018";
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("00A4090C02" + tlvID);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("00B2100000");
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        String BSO_N = ApduData.toHexString(response.getData());
        BSO_N = BSO_N.substring(2);
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("00B2110000");
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        String BSO_E = ApduData.toHexString(response.getData());
        BSO_E = BSO_E.substring(2);
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "00DA016E6D830220" + pubID + "8508210F0C0FFFFF000086070001FF000000018B10FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF8F" + BSO_N;
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        apdu = "00DA016E30830221" + pubID + "8508010F0C0FFFFF000086070001FF000000018B10FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF8F" + BSO_E;
        cmd = new ApduCmd(apdu);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("00E4000002" + tlvID);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        String nn = BSO_N.substring(4);
        String ee = BSO_E.substring(4);
        System.out.println("N: " + nn);
        System.out.println("E: " + ee);
        return new RSAPublicKeySpec(new BigInteger(nn, 16), new BigInteger(ee, 16));
    }

    public byte[] sign(String PIN, String pinID, String seID, String bsoID, byte[] digest, Session session) throws CardException, IOException {
        ApduCmd cmd;
        CardResponse response;
        String hPIN = ApduData.toHexString(PIN.getBytes());
        cmd = new ApduCmd("002000" + pinID + ApduData.hexLength(hPIN) + hPIN);
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("00DA016D0F8301" + seID + "860200008F06000000" + bsoID + "0000");
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("0022F3" + seID + "00");
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response, false);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        cmd = new ApduCmd("002A8086" + "41000001FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00" + "30213009" + "06052b0e03021a" + "0500" + "0414" + ApduData.toHexString(digest) + "00");
        System.out.println(cmd.toString());
        response = session.execute(cmd);
        System.out.println(response.toString());
        String encrypted = ApduData.toHexString(response.getData());
        System.out.println("ENC: " + encrypted);
        ApduData.printResponse(response);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        return ApduData.parse(encrypted);
    }

    public byte[] generateSignature(String PIN, String pinID, String bsoId, String seID, byte[] contentbytes, Session session) throws CardException, IOException, NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest hash = MessageDigest.getInstance("SHA1");
        hash.update(contentbytes);
        byte[] digest = hash.digest();
        return sign(PIN, pinID, seID, bsoId, digest, session);
    }

    public byte[] extractArray(byte[] src, int start, int end) {
        int len = end - start;
        if (len < 0) return null;
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            ret[i] = src[start + i];
        }
        return ret;
    }

    public X509Certificate generateX509Certificate(String PIN, String pinID, PublicKey pkey, String bso_id, String se_id, Session session) throws NoSuchProviderException, SecurityException, SignatureException, InvalidKeyException, IOException {
        byte[] signature = null;
        V3TBSCertificateGenerator tbsGen = new V3TBSCertificateGenerator();
        tbsGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));
        Hashtable sAttrs = new Hashtable();
        Vector sOrder = new Vector();
        sAttrs.put(X509Principal.C, "IT");
        sAttrs.put(X509Principal.O, "Tecnes Milano");
        sAttrs.put(X509Principal.OU, "Tecnes Milano");
        sAttrs.put(X509Principal.EmailAddress, "tecnes@tecnes.org");
        sOrder.addElement(X509Principal.C);
        sOrder.addElement(X509Principal.O);
        sOrder.addElement(X509Principal.OU);
        sOrder.addElement(X509Principal.EmailAddress);
        Hashtable attrs = new Hashtable();
        Vector order = new Vector();
        attrs.put(X509Principal.C, "IT");
        attrs.put(X509Principal.O, "Tecnes Milano");
        attrs.put(X509Principal.L, "Milano");
        attrs.put(X509Principal.CN, "Raffaello Bindi");
        attrs.put(X509Principal.EmailAddress, "tecnes@tecnes.org");
        order.addElement(X509Principal.C);
        order.addElement(X509Principal.O);
        order.addElement(X509Principal.L);
        order.addElement(X509Principal.CN);
        order.addElement(X509Principal.EmailAddress);
        tbsGen.setIssuer(new X509Principal(sOrder, sAttrs));
        tbsGen.setStartDate(new Time(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30)));
        tbsGen.setEndDate(new Time(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)));
        tbsGen.setSubject(new X509Principal(order, attrs));
        tbsGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(pkey.getEncoded())).readObject()));
        Signature sig = null;
        DERObjectIdentifier sigOID = PKCSObjectIdentifiers.sha1WithRSAEncryption;
        if (sigOID == null) {
            throw new IllegalStateException("no signature algorithm specified");
        }
        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
        tbsGen.setSignature(sigAlgId);
        Hashtable extensions = new Hashtable();
        Vector extOrdering = new Vector();
        extensions.put(X509Extensions.BasicConstraints, new X509Extension(true, new DEROctetString(getBytes(new BasicConstraints(false)))));
        extOrdering.addElement(X509Extensions.BasicConstraints);
        extensions.put(X509Extensions.KeyUsage, new X509Extension(true, new DEROctetString(getBytes(new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)))));
        extOrdering.addElement(X509Extensions.KeyUsage);
        extensions.put(X509Extensions.ExtendedKeyUsage, new X509Extension(true, new DEROctetString(getBytes(new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)))));
        extOrdering.addElement(X509Extensions.ExtendedKeyUsage);
        extensions.put(X509Extensions.SubjectAlternativeName, new X509Extension(false, new DEROctetString(getBytes(new GeneralNames(new GeneralName(GeneralName.rfc822Name, "test@test.test"))))));
        extOrdering.addElement(X509Extensions.SubjectAlternativeName);
        if (extensions != null) {
            tbsGen.setExtensions(new X509Extensions(extOrdering, extensions));
        }
        TBSCertificateStructure tbsCert = tbsGen.generateTBSCertificate();
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            DEROutputStream dOut = new DEROutputStream(bOut);
            dOut.writeObject(tbsCert);
            signature = generateSignature(PIN, pinID, bso_id, se_id, bOut.toByteArray(), session);
        } catch (Exception e) {
            throw new SecurityException("exception encoding TBS cert - " + e);
        }
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(tbsCert);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));
        return new X509CertificateObject(new X509CertificateStructure(new DERSequence(v)));
    }

    public void storeX509Certificate(String cerID, X509Certificate cert, Session session) throws CardException, IOException, CertificateEncodingException {
        byte[] certData = cert.getEncoded();
        String SIZE = ApduData.fillZeros(Integer.toString(certData.length, 16).toUpperCase(), 4);
        String TYPE = "01";
        String FV = "8102" + SIZE + "8203" + TYPE + "FF00" + "8302" + cerID + "8503010000" + "8609000000000000000000" + "8B18FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        String FF = "6F" + ApduData.hexLength(FV) + FV;
        ApduCmd cmd = new ApduCmd("00E00000" + ApduData.hexLength(FF) + FF);
        System.out.println(cmd.toString());
        CardResponse response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response, false);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return;
        int BLOCK_SIZE = 255;
        byte[] buffer = new byte[BLOCK_SIZE];
        int blocks = certData.length / BLOCK_SIZE;
        if (certData.length % BLOCK_SIZE > 0) blocks++;
        int offset = 0;
        for (int i = 0; i < blocks; i++) {
            int len = BLOCK_SIZE;
            if ((i + 1) == blocks) if (certData.length % BLOCK_SIZE > 0) len = certData.length % BLOCK_SIZE;
            System.arraycopy(certData, BLOCK_SIZE * i, buffer, 0, len);
            String p1p2 = Integer.toString(offset, 16);
            p1p2 = ApduData.fillZeros(p1p2, 4);
            String dataBuff = ApduData.toHexString(buffer, 0, len);
            cmd = new ApduCmd("00D6" + p1p2 + ApduData.hexLength(dataBuff) + dataBuff);
            System.out.println(cmd.toString());
            response = session.execute(cmd);
            System.out.println(response.toString());
            ApduData.printResponse(response, false);
            if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return;
            offset += BLOCK_SIZE;
        }
    }

    public X509Certificate loadX509Certificate(String cerID, Session session) throws CardException, IOException, CertificateException {
        X509Certificate cert = null;
        ApduCmd cmd = new ApduCmd("00A4090002" + cerID + "00");
        System.out.println(cmd.toString());
        CardResponse response = session.execute(cmd);
        System.out.println(response.toString());
        ApduData.printResponse(response, false);
        if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
        byte[] data = response.getData();
        byte[] len = null;
        len = ApduData.getTaggedValue(data, 0x81, 2);
        if (len == null) len = ApduData.getTaggedValue(data, 0x80, 2);
        String hLen = ApduData.toHexString(len);
        int length = Integer.parseInt(hLen, 16);
        String certData = "";
        int offset = 0;
        while (offset < length) {
            String p1p2 = Integer.toString(offset, 16);
            if (p1p2.length() < 4) {
                String zeros = "";
                for (int i = 0; i < (4 - p1p2.length()); i++) {
                    zeros += "0";
                }
                p1p2 = zeros + p1p2;
            }
            cmd = new ApduCmd("00B0" + p1p2 + "ff");
            System.out.println(cmd.toString());
            response = session.execute(cmd);
            System.out.println(response.toString());
            ApduData.printResponse(response, false);
            if (!Arrays.equals(response.getStatusWord().getBytes(), OK)) return null;
            certData += ApduData.toHexString(response.getData());
            offset += 255;
        }
        ByteArrayInputStream creader = new ByteArrayInputStream(ApduData.parse(certData));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate) cf.generateCertificate(creader);
        return cert;
    }

    private byte[] getBytes(org.bouncycastle.asn1.DEREncodable value) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        try {
            dOut.writeObject(value);
        } catch (IOException e) {
            throw new IllegalArgumentException("error encoding value: " + e);
        }
        return bOut.toByteArray();
    }
}
