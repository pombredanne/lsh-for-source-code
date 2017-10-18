package es.caib.signatura.provider.impl.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import es.caib.signatura.api.SignatureTimestampException;
import es.caib.signatura.impl.SigDebug;
import es.caib.signatura.impl.SignaturaProperties;
import es.caib.signatura.provider.impl.common.SHA1Util;

public class TimeStampManager {

    private SignaturaProperties properties = null;

    private static Random r = new Random(System.currentTimeMillis());

    private TimeStampToken lastTimeStampTokenGenerated = null;

    public TimeStampToken getLastTimeStampTokenGenerated() {
        return lastTimeStampTokenGenerated;
    }

    private URL getURLPrincipal(X509Certificate cert) throws IOException, SignatureTimestampException {
        if (SigDebug.isActive()) {
            SigDebug.write("Trying to get the timestamp provider for " + cert.getIssuerX500Principal().getName());
        }
        String proveidor = properties.getTimestampService(cert.getIssuerX500Principal().getName());
        if (proveidor == null || proveidor.length() == 0) {
            proveidor = properties.getTimestampService("default");
        }
        if (proveidor == null || proveidor.length() == 0) {
            throw new SignatureTimestampException("There is not any timestamp server configured for " + cert.getIssuerX500Principal().getName());
        }
        if (SigDebug.isActive()) {
            SigDebug.write("Found a timestamp provider for " + cert.getIssuerX500Principal().getName() + ", is: " + proveidor);
        }
        return new URL(proveidor);
    }

    public TimeStampManager() {
        super();
        try {
            properties = new SignaturaProperties();
        } catch (Exception e) {
            throw new Error("Unable to get the configuration properties of the API.", e);
        }
    }

    /**
	 * Generates a timestamp token.
	 * 
	 * @param cert signer certificate.
	 * @param digest digest of the signed content.
	 * @param digestAlgorithm digest algorithm.
	 * 
	 * @return A timestamp token.
	 * 
	 * @throws IOException
	 * @throws TSPException
	 * @throws SignatureTimestampException
	 */
    private TimeStampToken generateTimeStamp(X509Certificate cert, byte digest[], String digestAlgorithm) throws IOException, TSPException, SignatureTimestampException {
        try {
            TimeStampRequestGenerator generator = new TimeStampRequestGenerator();
            generator.setCertReq(true);
            byte[] nonce = new byte[16];
            r.nextBytes(nonce);
            TimeStampRequest req = generator.generate(digestAlgorithm, digest, new BigInteger(nonce));
            byte encoded[] = req.getEncoded();
            URL url = getURLPrincipal(cert);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/timestamp-query");
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            out.write(encoded);
            out.flush();
            out.close();
            InputStream in = conn.getInputStream();
            TimeStampResponse response = new TimeStampResponse(in);
            in.close();
            response.validate(req);
            TimeStampToken token = response.getTimeStampToken();
            byte[] data = token.toCMSSignedData().getEncoded();
            TimeStampTokenInfo info = token.getTimeStampInfo();
            AttributeTable atable = token.getSignedAttributes();
            Attribute a = atable.get(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"));
            return response.getTimeStampToken();
        } catch (Exception e) {
            throw new SignatureTimestampException(e);
        }
    }

    /**
	 * Adds a timestamp to signed data.
	 * 
	 * @param cert signer certificate.
	 * @param signedData signed content without timestamp.
	 * @param digest digest of the signed content.
	 * @param digestAlgorithm digest algorithm.
	 * 
	 * @return The signed data with timestamp if it has been obtained; otherwise return null.
	 * 
	 * @throws IOException
	 * @throws TSPException
	 * @throws SignatureTimestampException
	 * 
	 */
    public CMSSignedData addTimestamp(X509Certificate cert, CMSSignedData signedData, byte digest[], String algorithm) throws IOException, TSPException, SignatureTimestampException {
        try {
            Collection ss = signedData.getSignerInfos().getSigners();
            SignerInformation si = (SignerInformation) ss.iterator().next();
            TimeStampToken tok = generateTimeStamp(cert, digest, algorithm);
            ASN1InputStream asn1InputStream = new ASN1InputStream(tok.getEncoded());
            DERObject tstDER = asn1InputStream.readObject();
            DERSet ds = new DERSet(tstDER);
            Attribute a = new Attribute(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"), ds);
            DEREncodableVector dv = new DEREncodableVector();
            dv.add(a);
            AttributeTable at = new AttributeTable(dv);
            si = SignerInformation.replaceUnsignedAttributes(si, at);
            ss.clear();
            ss.add(si);
            SignerInformationStore sis = new SignerInformationStore(ss);
            signedData = CMSSignedData.replaceSigners(signedData, sis);
            this.lastTimeStampTokenGenerated = tok;
        } catch (Exception e) {
            return null;
        }
        return signedData;
    }

    /**
	 * Adds a timestamp to signed data.
	 * 
	 * @param cert signer certificate.
	 * @param signedData signed content without timestamp.
	 * 
	 * @return The signed data with timestamp if it has been obtained; otherwise return null.
	 * 
	 * @throws IOException
	 * @throws TSPException
	 * @throws SignatureTimestampException
	 */
    public CMSSignedData addTimestamp(X509Certificate cert, CMSSignedData signedData) throws IOException, TSPException, SignatureTimestampException {
        Collection ss = signedData.getSignerInfos().getSigners();
        SignerInformation si = (SignerInformation) ss.iterator().next();
        byte digest[];
        try {
            digest = SHA1Util.digest(si.getSignature());
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureTimestampException(e);
        } catch (NoSuchProviderException e) {
            throw new SignatureTimestampException(e);
        }
        return addTimestamp(cert, signedData, digest, TSPAlgorithms.SHA1);
    }

    public CMSSignedData addWrongTimestamp(X509Certificate cert, CMSSignedData signedData, byte digest[], String digestAlogrithm) throws IOException, TSPException, SignatureTimestampException {
        return signedData;
    }

    public Date getTimeStamp(X509Certificate cert) throws IOException, TSPException, SignatureTimestampException {
        TimeStampRequestGenerator generator = new TimeStampRequestGenerator();
        generator.setCertReq(true);
        byte[] nonce = new byte[16];
        r.nextBytes(nonce);
        byte[] emptyBuffer = new byte[new SHA1Digest().getDigestSize()];
        TimeStampRequest req = generator.generate(TSPAlgorithms.SHA1, emptyBuffer, new BigInteger(nonce));
        byte encoded[] = req.getEncoded();
        URL url = getURLPrincipal(cert);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Host", url.getHost() + ":" + url.getPort());
        conn.setRequestProperty("Content-type", "application/timestamp-query");
        conn.setRequestProperty("Content-length", Integer.toString(encoded.length));
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStream out = conn.getOutputStream();
        out.write(encoded);
        out.flush();
        out.close();
        InputStream in = conn.getInputStream();
        TimeStampResponse response = new TimeStampResponse(in);
        in.close();
        response.validate(req);
        return response.getTimeStampToken().getTimeStampInfo().getGenTime();
    }
}
