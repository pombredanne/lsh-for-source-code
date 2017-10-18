package antiquity.client;

import antiquity.gw.api.*;
import antiquity.util.AntiquityUtils;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import ostore.util.ByteUtils;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;

/**
*** A set of common utility functions.
**/
public class ClientUtils {

    private static boolean init = false;

    private static MessageDigest md;

    private static KeyFactory key_factory;

    private static void init() {
        try {
            md = MessageDigest.getInstance("SHA1");
            key_factory = KeyFactory.getInstance("RSA");
        } catch (java.security.NoSuchAlgorithmException e) {
            String err_st = "Failed to initialize client utilities: " + e;
            throw new RuntimeException(err_st);
        }
        init = true;
        return;
    }

    /**
    *** Read an RSAPublicKey from a file.  Return <code>null</code> if
    *** the key cannot be constructed from the file.
    **/
    public static PublicKey readRsaPublicKey(String pkey_filename) {
        if (!init) {
            init();
        }
        return AntiquityUtils.readPublicKey(pkey_filename, key_factory);
    }

    /**
    *** Reconstruct an RSAPublicKey from its encoded form.  Return
    *** <code>null</code> if the conversion fails for any reason.
    **/
    public static PublicKey readRsaPublicKey(byte[] pkey_bytes) {
        if (!init) {
            init();
        }
        PublicKey pkey = null;
        try {
            pkey = AntiquityUtils.getPublicKey(pkey_bytes, key_factory);
        } catch (Exception e) {
        }
        return pkey;
    }

    /**
    *** Read an RSAPublicKey from a file.  Return <code>null</code> if
    *** the key cannot be constructed from the file.
    **/
    public static PrivateKey readRsaPrivateKey(String skey_filename) {
        if (!init) {
            init();
        }
        return AntiquityUtils.readPrivateKey(skey_filename, key_factory);
    }

    /**
    *** Reconstruct an RSAPrivateKey from its encoded form.  Return
    *** <code>null</code> if the conversion fails for any reason.
    **/
    public static PrivateKey readRsaPrivateKey(byte[] skey_bytes) {
        if (!init) {
            init();
        }
        PrivateKey skey = null;
        try {
            skey = AntiquityUtils.getPrivateKey(skey_bytes, key_factory);
        } catch (Exception e) {
        }
        return skey;
    }

    public static String printPkey(PublicKey pkey) {
        if (!init) {
            init();
        }
        md.update(pkey.getEncoded());
        byte[] hash = md.digest();
        return ByteUtils.print_bytes(hash, 0, AntiquityUtils.num_bytes_to_print);
    }

    public static String printSkey(PrivateKey skey) {
        if (!init) {
            init();
        }
        md.update(skey.getEncoded());
        byte[] hash = md.digest();
        return ByteUtils.print_bytes(hash, 0, AntiquityUtils.num_bytes_to_print);
    }

    public static PublicKey convertPublicKey(gw_public_key pkey) throws Exception {
        if (!init) {
            init();
        }
        return AntiquityUtils.getPublicKey(pkey, key_factory);
    }

    public static gw_public_key convertPublicKey(PublicKey pkey) {
        return AntiquityUtils.convertPublicKey(pkey);
    }

    public static gw_guid pkeyToGuid(gw_public_key pkey) {
        SecureHash pkey_hash = new SHA1Hash(pkey.value);
        gw_guid guid = new gw_guid();
        guid.value = pkey_hash.bytes();
        return guid;
    }

    public static gw_guid pkeyToGuid(PublicKey pkey) {
        gw_guid gw_guid = new gw_guid();
        gw_guid.value = (new SHA1Hash(pkey.getEncoded())).bytes();
        return gw_guid;
    }

    public static gw_guid hashToGuid(SecureHash hash) {
        return AntiquityUtils.secureHashToGuid(hash, gw_guid.class);
    }

    public static BigInteger hashToBigInt(SecureHash hash) {
        return AntiquityUtils.guidToBigInteger(hashToGuid(hash));
    }

    public static BigInteger guidToBigInt(gw_guid guid) {
        return AntiquityUtils.guidToBigInteger(guid);
    }

    public static String guidToString(gw_guid g) {
        return AntiquityUtils.guid_to_string(g);
    }

    public static String guidToString(BigInteger g) {
        return guidToString(bigIntToGuid(g));
    }

    public static gw_guid bigIntToGuid(BigInteger big_int) {
        return AntiquityUtils.bigIntegerToGuid(big_int, gw_guid.class);
    }

    public static gw_guid[] bigIntsToGuids(BigInteger[] big_ints) {
        gw_guid[] guids = new gw_guid[big_ints.length];
        for (int i = 0; i < big_ints.length; ++i) guids[i] = bigIntToGuid(big_ints[i]);
        return guids;
    }
}
