package poor.signature;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import poor.key.PoorPrivateKey;
import poor.utils.PoorSettings;
import poor.utils.PoorUtils;

public class PoorSHA256withRSA extends SignatureSpi {

    private static final int MODE_UNINITIALIZED = 0;

    private static final int MODE_SIGN = 1;

    private static final int MODE_VERIFY = 2;

    private int mode = MODE_UNINITIALIZED;

    /**
	 * SHA-256 with RSA OID.
	 */
    private static final String SIGNATURE_OID = "1.2.840.113549.1.1.11";

    /**
	 * CryptoAPI HCERTSTORE handle associated with the private key, which was passed
	 * to initSign method.
	 */
    private int hStore;

    /**
	 * Used for computing the SHA-256 hash which is then signed in native code.
	 */
    private MessageDigest md;

    /**
	 * A buffer to store the data in the update methods.
	 */
    private ByteArrayOutputStream baos;

    /**
	 * A private key obtained in initSign method.
	 */
    private PrivateKey privateKey;

    /**
	 * A public key obtained in initVerify method.
	 */
    private PublicKey publicKey;

    /**
	 * Determines, whether the signature will be raw RSA or in PKCS#7 format.<br>
	 * Possible values are:<br>
	 * SIGNATURE_RAW_RSA<br>
	 * SIGNATURE_PKCS_7
	 */
    private int signatureType;

    /**
	 * Whether to include signer's certificate in resulting signed data.
	 */
    private boolean includeSigner;

    /**
	 * Whether the signature will be in detached form. Attached otherwise.
	 */
    private boolean detachedSignature;

    /**
	 * Public constructor.
	 */
    public PoorSHA256withRSA() {
        super();
        initSignature();
    }

    /**
	 * Initializes this intance's variables.
	 */
    private native void initSignature();

    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        mode = MODE_SIGN;
        try {
            switch(signatureType) {
                case PoorSettings.SIGNATURE_RAW_RSA:
                    {
                        md = MessageDigest.getInstance("SHA-256");
                    }
                    break;
                case PoorSettings.SIGNATURE_PKCS_7:
                    {
                        baos = new ByteArrayOutputStream();
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.privateKey = privateKey;
        if (privateKey instanceof PoorPrivateKey) {
            this.hStore = ((PoorPrivateKey) privateKey).getStore();
        }
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        mode = MODE_VERIFY;
        baos = new ByteArrayOutputStream();
        this.publicKey = publicKey;
    }

    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    protected byte[] engineSign() throws SignatureException {
        if (privateKey instanceof PoorPrivateKey) {
            byte[] privateKeyAliasBytes = privateKey.toString().getBytes();
            if (privateKeyAliasBytes == null) throw new SignatureException("Alias for the signature is null");
            byte[] signedDataB = null;
            switch(signatureType) {
                case PoorSettings.SIGNATURE_RAW_RSA:
                    {
                        signedDataB = PoorSignature.signRaw(md.digest(), new String(privateKeyAliasBytes), SIGNATURE_OID, hStore);
                    }
                    break;
                case PoorSettings.SIGNATURE_PKCS_7:
                    {
                        signedDataB = PoorSignature.signPkcs7(baos.toByteArray(), new String(privateKeyAliasBytes), SIGNATURE_OID, hStore, includeSigner, detachedSignature);
                    }
            }
            return signedDataB;
        }
        mode = MODE_UNINITIALIZED;
        return null;
    }

    protected void engineUpdate(byte b) throws SignatureException {
        if (mode == MODE_SIGN) {
            try {
                switch(signatureType) {
                    case PoorSettings.SIGNATURE_RAW_RSA:
                        {
                            md.update(b);
                        }
                        break;
                    case PoorSettings.SIGNATURE_PKCS_7:
                        {
                            baos.write(b);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (mode == MODE_VERIFY) {
            baos.write(b);
        }
    }

    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        if (mode == MODE_SIGN) {
            try {
                switch(signatureType) {
                    case PoorSettings.SIGNATURE_RAW_RSA:
                        {
                            md.update(b, off, len);
                        }
                        break;
                    case PoorSettings.SIGNATURE_PKCS_7:
                        {
                            baos.write(b, off, len);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (mode == MODE_VERIFY) {
            baos.write(b, off, len);
        }
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        mode = MODE_UNINITIALIZED;
        byte[] publicKeyBlob = PoorUtils.subjectpublickeyinfoToPublickeyblob(publicKey.getEncoded(), PoorUtils.AT_KEYEXCHANGE);
        return verify(baos.toByteArray(), sigBytes, publicKeyBlob);
    }

    private native boolean verify(byte[] data, byte[] signedData, byte[] publicKeyBlob);
}
