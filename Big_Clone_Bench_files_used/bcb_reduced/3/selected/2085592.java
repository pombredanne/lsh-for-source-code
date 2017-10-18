package es.caib.signatura.provider.impl.mscryptoapi.mscrypto;

import java.security.SignatureSpi;
import java.security.Signature;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import es.caib.signatura.impl.SigDebug;

public class MSRSASignFactoryImpl extends SignatureSpi {

    static MSCryptoFunctions MSF = new MSCryptoFunctions();

    static RSAPublicKey rsaPublicKey;

    static boolean signOpInProgress = false;

    static boolean verifyOpInProgress = false;

    static Signature jsse;

    static MessageDigest MD;

    static String MessageDigestType;

    static String certAlias = null;

    static String pin = null;

    protected void setMessageDigestType(String MDType) {
        try {
            MD = MessageDigest.getInstance(MDType);
            MessageDigestType = MDType;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (SigDebug.isActive()) SigDebug.write("MSRSASignFactoryImpl:setMessageDigestType " + MDType);
    }

    protected Object engineGetParameter(String param) {
        System.out.println("MSSHARSASignFactoryImpl: engineGetParameter: not implemented");
        return null;
    }

    protected void engineSetParameter(AlgorithmParameterSpec params) {
        System.out.println("MSSHARSASignFactoryImpl: engineSetParameter: not implemented");
    }

    protected void engineSetParameter(String param, Object value) {
        System.out.println("MSSHARSASignFactoryImpl: engineSetParameter: not implemented");
    }

    protected void engineInitSign(PrivateKey privateKey) {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineInitSign: entered");
        try {
            signOpInProgress = true;
            verifyOpInProgress = false;
            String[] keyInfo = (new String(privateKey.getEncoded(), "UTF-8")).split(Character.toString('\0'));
            pin = keyInfo[0];
            certAlias = keyInfo[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected byte[] engineSign() {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineSign: entered");
        if (!signOpInProgress) {
            System.out.println("MSSHARSASignFactoryImpl: error - throw exception");
            return null;
        }
        byte[] hash = MD.digest();
        byte[] mssig = MSF.MSrsaSignHash(hash, (byte[]) null, MessageDigestType, certAlias, pin);
        signOpInProgress = false;
        return mssig;
    }

    protected int engineSign(byte[] outbuf, int offset, int len) {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineSign: entered");
        if (!signOpInProgress) {
            System.out.println("MSSHARSASignFactoryImpl: error - throw exception");
            return 0;
        }
        byte[] hash = MD.digest();
        byte[] mssig = MSF.MSrsaSignHash(hash, (byte[]) null, MessageDigestType, certAlias, pin);
        java.lang.System.arraycopy((Object) mssig, 0, (Object) outbuf, offset, mssig.length);
        signOpInProgress = false;
        return mssig.length;
    }

    protected void engineUpdate(byte b) {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineUpdate: entered");
        try {
            if (signOpInProgress) {
                MD.update(b);
            } else if (verifyOpInProgress) {
                jsse.update(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void engineUpdate(byte[] data, int off, int len) {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineUpdate: entered");
        try {
            if (signOpInProgress) {
                MD.update(data, off, len);
            } else if (verifyOpInProgress) {
                jsse.update(data, off, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void engineInitVerify(PublicKey publicKey) {
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineInitVerify: entered");
        try {
            String SignatureAlg = MessageDigestType + "withRSA";
            jsse = Signature.getInstance(SignatureAlg, "SunJSSE");
            jsse.initVerify(publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        signOpInProgress = false;
        verifyOpInProgress = true;
    }

    protected boolean engineVerify(byte[] sigBytes) {
        boolean verifyresult = false;
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineVerify: entered");
        if (!verifyOpInProgress) {
            System.out.println("MSSHARSASignFactoryImpl: error - throw exception");
            return false;
        }
        try {
            verifyresult = jsse.verify(sigBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verifyresult;
    }

    protected boolean engineVerify(byte[] sig, int off, int len) {
        boolean verifyresult = false;
        if (SigDebug.isActive()) SigDebug.write("MSSHARSASignFactoryImpl: engineVerify: entered");
        if (!verifyOpInProgress) {
            System.out.println("MSSHARSASignFactoryImpl: error - throw exception");
            return false;
        }
        try {
            verifyresult = jsse.verify(sig, off, len);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verifyresult;
    }
}
