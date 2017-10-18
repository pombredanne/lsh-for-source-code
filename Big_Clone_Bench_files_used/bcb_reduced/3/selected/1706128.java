package br.net.woodstock.rockframework.security.timestamp.impl;

import java.math.BigInteger;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import br.net.woodstock.rockframework.security.digest.DigestType;
import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.security.digest.impl.BasicDigester;
import br.net.woodstock.rockframework.security.timestamp.TimeStamp;
import br.net.woodstock.rockframework.security.timestamp.TimeStampClient;
import br.net.woodstock.rockframework.security.timestamp.TimeStampException;
import br.net.woodstock.rockframework.security.timestamp.TimeStampProcessor;
import br.net.woodstock.rockframework.security.util.BouncyCastleProviderHelper;
import br.net.woodstock.rockframework.util.Assert;

public abstract class BouncyCastleTimeStampClient implements TimeStampClient {

    public static final String PROVIDER_NAME = BouncyCastleProviderHelper.PROVIDER_NAME;

    public static final String RSA_OID = OIWObjectIdentifiers.idSHA1.getId();

    private TimeStampProcessor processor;

    private boolean debug;

    public BouncyCastleTimeStampClient(final TimeStampProcessor processor) {
        super();
        Assert.notNull(processor, "processor");
        this.processor = processor;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return this.debug;
    }

    @Override
    public TimeStamp getTimeStamp(final byte[] data) {
        try {
            TimeStampRequest request = this.getTimeStampRequest(data);
            byte[] response = this.processor.getBinaryResponse(request.getEncoded());
            TimeStampResponse timeStampResponse = new TimeStampResponse(response);
            TimeStampToken timeStampToken = timeStampResponse.getTimeStampToken();
            if (timeStampToken == null) {
                throw new IllegalStateException("TimeStampToken not found in response");
            }
            return BouncyCastleTimeStampHelper.toTimeStamp(timeStampToken);
        } catch (Exception e) {
            throw new TimeStampException(e);
        }
    }

    protected TimeStampRequest getTimeStampRequest(final byte[] imprint) {
        Digester digester = new BasicDigester(DigestType.valueOf(DigestType.SHA1.getAlgorithm()));
        byte[] digest = digester.digest(imprint);
        TimeStampRequestGenerator generator = new TimeStampRequestGenerator();
        generator.setCertReq(true);
        BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
        TimeStampRequest request = generator.generate(BouncyCastleTimeStampClient.RSA_OID, digest, nonce);
        return request;
    }
}
