package org.javasign.operators;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.MechanismInfo;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.PrivateKey;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.javasign.util.ApduData;
import org.javasign.util.WrapperUtil;

/**
 * @author raffaello bindi, simone rastelli
 *
 */
public class CryptokiGenerator {

    File src = null;

    File dest = null;

    String driver = null;

    String KEY_LABEL = null;

    String PIN = null;

    PrintWriter output_;

    public CryptokiGenerator(String driver, File src, File dest, String KEY_LABEL, String PIN) {
        output_ = new PrintWriter(System.out, true);
        this.driver = driver;
        this.src = src;
        this.dest = dest;
        this.PIN = PIN;
        this.KEY_LABEL = KEY_LABEL;
    }

    public void generate() throws IOException, TokenException, NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertStoreException, CMSException {
        Module pkcs11Module = Module.getInstance(driver);
        pkcs11Module.initialize(null);
        Token token = WrapperUtil.selectToken(pkcs11Module, output_);
        if (token == null) {
            output_.flush();
            throw new NullPointerException("No token to proceed.");
        }
        List supportedMechanisms = Arrays.asList(token.getMechanismList());
        if (!supportedMechanisms.contains(Mechanism.RSA_PKCS)) {
            throw new NullPointerException("This token does not support raw RSA signing!");
        } else {
            MechanismInfo rsaMechanismInfo = token.getMechanismInfo(Mechanism.RSA_PKCS);
            if (!rsaMechanismInfo.isSign()) {
                throw new NullPointerException("This token does not support RSA signing according to PKCS!");
            }
        }
        Session session = WrapperUtil.openAuthorizedSession(token, Token.SessionReadWriteBehavior.RO_SESSION, output_, PIN);
        RSAPrivateKey privateSignatureKeyTemplate = new RSAPrivateKey();
        privateSignatureKeyTemplate.getSign().setBooleanValue(Boolean.TRUE);
        org.javasign.util.KeyAndCertificate selectedSignatureKeyAndCertificate = WrapperUtil.selectKeyAndCertificate(session, privateSignatureKeyTemplate, output_, KEY_LABEL);
        if (selectedSignatureKeyAndCertificate == null) {
            throw new NullPointerException("Nno signature key to proceed.");
        }
        PrivateKey selectedSignatureKey = (PrivateKey) selectedSignatureKeyAndCertificate.getKey();
        X509PublicKeyCertificate pkcs11SignerCertificate = selectedSignatureKeyAndCertificate.getCertificate();
        X509Certificate cert = (pkcs11SignerCertificate != null) ? WrapperUtil.loadX509Certificate(pkcs11SignerCertificate.getValue().getByteArrayValue()) : null;
        output_.println("signing data from file: " + src.toString());
        Security.addProvider(new BouncyCastleProvider());
        int sizecontent = ((int) src.length());
        byte[] contentbytes = new byte[sizecontent];
        FileInputStream freader = new FileInputStream(src);
        System.out.println("\nContent Bytes: " + freader.read(contentbytes, 0, sizecontent));
        freader.close();
        Signature2CMSSignedData fact = new Signature2CMSSignedData();
        CMSProcessableByteArray content = new CMSProcessableByteArray(contentbytes);
        String contentType = PKCSObjectIdentifiers.data.getId();
        session.signInit(Mechanism.RSA_PKCS, selectedSignatureKey);
        byte[] toEncrypt = buildBits(contentbytes);
        byte[] signature = session.sign(toEncrypt);
        ArrayList certList = new ArrayList();
        certList.add(cert);
        CertStore certs = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
        String algorithm = CMSSignedDataGenerator.DIGEST_SHA1;
        fact.addSignature(signature, cert, algorithm);
        fact.addCertificatesAndCRLs(certs);
        CMSSignedData envdata = fact.generate(content, true, "BC");
        byte[] enveloped = envdata.getEncoded();
        System.out.println("Got encoded pkcs7 bytes " + enveloped.length + " bytes");
        FileOutputStream efos = new FileOutputStream(dest);
        efos.write(enveloped);
        efos.close();
        session.closeSession();
        pkcs11Module.finalize(null);
    }

    public byte[] buildBits(byte[] contentbytes) throws NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance("SHA1");
        hash.update(contentbytes);
        byte[] digest = hash.digest();
        String hhStr = "30213009" + "06052b0e03021a" + "0500" + "0414" + ApduData.toHexString(digest);
        return ApduData.parse(hhStr);
    }
}
