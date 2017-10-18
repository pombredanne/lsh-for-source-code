package poor.signature;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import poor.key.PoorPrivateKey;
import poor.utils.PoorSettings;

public class PoorSHA1withRSA extends SignatureSpi {

    private static final int MODE_UNINITIALIZED = 0;

    private static final int MODE_SIGN = 1;

    private static final int MODE_VERIFY = 2;

    private int mode = MODE_UNINITIALIZED;

    /**
	 * SHA-1 with RSA OID.
	 */
    private static final String SIGNATURE_OID = "1.2.840.113549.1.1.5";

    /**
	 * CryptoAPI HCERTSTORE handle associated with the private key, which was passed
	 * to initSign method.
	 */
    private int hStore;

    /**
	 * Used for computing the SHA-1 hash which is then signed in native code.
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
	 * SunJSSE SHA1withRSA signature class used for verification only.
	 */
    private Signature signature;

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
    public PoorSHA1withRSA() {
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
                        md = MessageDigest.getInstance("SHA-1");
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
        try {
            signature = Signature.getInstance("SHA1withRSA", "SunJSSE");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (signature != null) {
            signature.initVerify(publicKey);
        }
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
            signature.update(b);
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
            signature.update(b, off, len);
        }
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        mode = MODE_UNINITIALIZED;
        return signature.verify(sigBytes);
    }
}
