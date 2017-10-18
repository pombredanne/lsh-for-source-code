package wtanaka.praya.gale;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import wtanaka.debug.Debug;

/**
 * Private Key used by the Gale Protocol.
 * <pre>
 * How does RSA work?
 *
 *    We show a high-level though working description of RSA. Then, we give
 *    an example with easy to work with numbers.
 *
 * Description
 *
 *    To initialise RSA, follow the steps
 *
 *     1. Pick two large primes, p and q.
 *     2. Find N = p * q. N is the RSA modulus.
 *     3. Let e be a number relatively prime to (p-1)*(q-1).
 *     4. Find d, so that d*e = 1 mod (p-1)*(q-1) .
 *     5. The set (e, N) is the public key. Make it known to every one.
 *        The set (d, N) is the private key. Keep it private and safe.
 *
 *    To encrypt a message m,
 *
 *     1. Make sure m < N, otherwise chop m in suitably small pieces and
 *        perform RSA on each individual piece.
 *     2. Compute c = m ^ e mod N
 *     3. c is the encrypted message
 *
 *    To decrypt a ciphertext c,
 *
 *     1. Compute m = c ^ d mod N
 *     2. m is the original message
 *
 *    To sign message m,
 *
 *     1. Compute s = m ^ d mod N
 *     2. s is the digital signature. Send along with message m.
 *
 *    To verify signed message s,
 *
 *     1. Compute m = s ^ e mod N
 *     2. Check if m from above calculation is the same with message sent.
 * </pre>
 *
 * <p>
 * Return to <A href="http://sourceforge.net/projects/praya/">
 * <IMG src="http://sourceforge.net/sflogo.php?group_id=2302&type=1"
 *   alt="Sourceforge" width="88" height="31" border="0"></A>
 * or the <a href="http://praya.sourceforge.net/">Praya Homepage</a>
 *
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2003/12/17 01:25:17 $
 **/
public class GalePrivateKey implements PrivateKey {

    public static final int MIN_RSA_MODULUS_BITS = 508;

    public static final int MAX_RSA_MODULUS_BITS = 1024;

    public static final int MAX_RSA_MODULUS_LEN = ((MAX_RSA_MODULUS_BITS + 7) / 8);

    public static final int MAX_RSA_PRIME_BITS = ((MAX_RSA_MODULUS_BITS + 1) / 2);

    public static final int MAX_RSA_PRIME_LEN = ((MAX_RSA_PRIME_BITS + 7) / 8);

    public static final byte[] DIGEST_INFO_A = { 0x30, 0x20, 0x30, 0x0c, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x02 };

    public static final byte[] DIGEST_INFO_B = { 0x05, 0x00, 0x04, 0x10 };

    public static final int DIGEST_INFO_LEN = (DIGEST_INFO_A.length + 1 + DIGEST_INFO_B.length + 16);

    public static final int GALE_RSA_MODULUS_BITS = 1024;

    public static final int GALE_RSA_MODULUS_LEN = ((GALE_RSA_MODULUS_BITS + 7) / 8);

    public static final int GALE_RSA_PRIME_BITS = ((GALE_RSA_MODULUS_BITS + 1) / 2);

    public static final int GALE_RSA_PRIME_LEN = ((GALE_RSA_PRIME_BITS + 7) / 8);

    public static final int GALE_ENCRYPTED_KEY_LEN = GALE_RSA_MODULUS_LEN;

    public static final int GALE_SIGNATURE_LEN = GALE_RSA_MODULUS_LEN;

    String m_id;

    String m_owner;

    int m_bits;

    /**
    * N
    **/
    BigInteger m_rsaModulus;

    BigInteger m_rsaExponent;

    BigInteger m_rsaPrivateExponent;

    /**
    * p
    **/
    BigInteger m_rsaPrivatePrime1;

    /**
    * q
    **/
    BigInteger m_rsaPrivatePrime2;

    BigInteger m_rsaPrivatePrimeExponent1;

    BigInteger m_rsaPrivatePrimeExponent2;

    BigInteger m_rsaPrivateCoefficient;

    FragmentList m_fragments;

    int m_originalStreamVersion = -1;

    public GalePrivateKey(File f) throws IOException {
        this(new GaleInputStream(new FileInputStream(f)));
    }

    public GalePrivateKey(GaleInputStream in) throws IOException {
        byte[] magic = new byte[4];
        if (4 != in.read(magic)) {
            throw new StreamCorruptedException("Expecting Magic");
        }
        if (magic[0] == 0x47 && magic[1] == 0x41 && magic[2] == 0x4C && magic[3] == 0x45) {
            m_originalStreamVersion = 3;
            byte[] version = new byte[2];
            in.read(version);
            if (!(version[0] == 0x00 && version[1] == 0x02)) {
                throw new StreamCorruptedException("Unrecognized Magic");
            }
            m_id = in.readCountedUnicode();
            m_fragments = new FragmentList(new FragmentInterface[0]);
            while (in.available() > 0) {
                FragmentInterface fragment = in.readFragment();
                m_fragments = m_fragments.unionWith(fragment);
            }
            InMemoryDataFragment rsaModulus = ((InMemoryDataFragment) m_fragments.find("rsa.modulus", true));
            if (rsaModulus != null) m_rsaModulus = new BigInteger(1, rsaModulus.getByteArray());
            InMemoryDataFragment rsaExponent = ((InMemoryDataFragment) m_fragments.find("rsa.exponent", true));
            if (rsaExponent != null) m_rsaExponent = new BigInteger(1, rsaExponent.getByteArray());
            InMemoryNumberFragment rsaBits = ((InMemoryNumberFragment) m_fragments.find("rsa.bits", true));
            if (rsaBits != null) m_bits = rsaBits.getIntValue();
            {
                InMemoryTextFragment owner = ((InMemoryTextFragment) m_fragments.find("key.owner", true));
                if (owner != null) m_owner = owner.getValue(); else m_owner = "";
            }
            InMemoryDataFragment rsaPrivateExponent = ((InMemoryDataFragment) m_fragments.find("rsa.private.exponent"));
            if (rsaPrivateExponent != null) {
                GaleInputStream gin = new GaleInputStream(new ByteArrayInputStream(rsaPrivateExponent.getByteArray()));
                m_rsaPrivateExponent = new BigInteger(1, rsaPrivateExponent.getByteArray());
            }
            InMemoryDataFragment rsaPrivatePrime = ((InMemoryDataFragment) m_fragments.find("rsa.private.prime"));
            if (rsaPrivatePrime != null) {
                GaleInputStream gin = new GaleInputStream(new ByteArrayInputStream(rsaPrivatePrime.getByteArray()));
                byte[] orig = rsaPrivatePrime.getByteArray();
                byte[] array1 = new byte[orig.length / 2];
                byte[] array2 = new byte[orig.length / 2];
                System.arraycopy(orig, 0, array1, 0, array1.length);
                System.arraycopy(orig, array1.length, array2, 0, array2.length);
                m_rsaPrivatePrime1 = new BigInteger(1, array1);
                m_rsaPrivatePrime2 = new BigInteger(1, array2);
            }
            InMemoryDataFragment rsaPrivatePrimeExponent = ((InMemoryDataFragment) m_fragments.find("rsa.private.prime.exponent"));
            if (rsaPrivatePrimeExponent != null) {
                GaleInputStream gin = new GaleInputStream(new ByteArrayInputStream(rsaPrivatePrimeExponent.getByteArray()));
                byte[] orig = rsaPrivatePrimeExponent.getByteArray();
                byte[] array1 = new byte[orig.length / 2];
                byte[] array2 = new byte[orig.length / 2];
                System.arraycopy(orig, 0, array1, 0, array1.length);
                System.arraycopy(orig, array1.length, array2, 0, array2.length);
                m_rsaPrivatePrimeExponent1 = new BigInteger(1, array1);
                m_rsaPrivatePrimeExponent2 = new BigInteger(1, array2);
            }
            InMemoryDataFragment rsaPrivateCoefficient = ((InMemoryDataFragment) m_fragments.find("rsa.private.coefficient"));
            if (rsaPrivateCoefficient != null) {
                GaleInputStream gin = new GaleInputStream(new ByteArrayInputStream(rsaPrivateCoefficient.getByteArray()));
                m_rsaPrivateCoefficient = new BigInteger(1, rsaPrivateCoefficient.getByteArray());
            }
        } else if (magic[0] == 0x68 && magic[1] == 0x13 && magic[2] == 0x00 && magic[3] == 0x03) {
            m_originalStreamVersion = 2;
            m_id = in.readCountedUnicode();
            m_bits = in.readInt();
            m_rsaModulus = in.readRLE(MAX_RSA_MODULUS_LEN);
            m_rsaExponent = in.readRLE(MAX_RSA_MODULUS_LEN);
            m_rsaPrivateExponent = in.readRLE(MAX_RSA_MODULUS_LEN);
            m_rsaPrivatePrime1 = in.readRLE(MAX_RSA_PRIME_LEN);
            m_rsaPrivatePrime2 = in.readRLE(MAX_RSA_PRIME_LEN);
            m_rsaPrivatePrimeExponent1 = in.readRLE(MAX_RSA_PRIME_LEN);
            m_rsaPrivatePrimeExponent2 = in.readRLE(MAX_RSA_PRIME_LEN);
            m_rsaPrivateCoefficient = in.readRLE(MAX_RSA_PRIME_LEN);
        } else {
            throw new StreamCorruptedException("Unknown Private Key Format");
        }
        if (in.available() > 0) throw new StreamCorruptedException("Extra data at end of stream");
        if (m_bits < MIN_RSA_MODULUS_BITS || m_bits > MAX_RSA_MODULUS_BITS) {
            throw new StreamCorruptedException("Bad Priv Key Size");
        }
    }

    /**
    * Constructor for a newly generated key.
    **/
    public GalePrivateKey(String id, int bits, BigInteger rsaModulus, BigInteger rsaExponent, BigInteger rsaPrivateExponent, BigInteger rsaPrivatePrime1, BigInteger rsaPrivatePrime2, BigInteger rsaPrivatePrimeExponent1, BigInteger rsaPrivatePrimeExponent2, BigInteger rsaPrivateCoefficient) {
        m_id = id;
        m_bits = bits;
        m_rsaModulus = rsaModulus;
        m_rsaExponent = rsaExponent;
        m_rsaPrivateExponent = rsaPrivateExponent;
        m_rsaPrivatePrime1 = rsaPrivatePrime1;
        m_rsaPrivatePrime2 = rsaPrivatePrime2;
        m_rsaPrivatePrimeExponent1 = rsaPrivatePrimeExponent1;
        m_rsaPrivatePrimeExponent2 = rsaPrivatePrimeExponent2;
        m_rsaPrivateCoefficient = rsaPrivateCoefficient;
    }

    /**
    * Gets the EXPORT_STUB public key corresponding to this ID.  Used
    * for signing.
    **/
    public GalePublicKey toPublicStub() {
        return new GalePublicKey(m_id);
    }

    public String getID() {
        return m_id;
    }

    public String getAlgorithm() {
        return "RSA";
    }

    public String getFormat() {
        return "Gale Private Key v2";
    }

    public byte[] getEncoded() {
        throw new RuntimeException("nyi");
    }

    public BigInteger getModulus() {
        return m_rsaModulus;
    }

    public BigInteger getPrivateExponent() {
        return m_rsaPrivateExponent;
    }

    public BigInteger getPrivatePrime1() {
        return m_rsaPrivatePrime1;
    }

    public BigInteger getPrivatePrime2() {
        return m_rsaPrivatePrime2;
    }

    /**
    * @see GalePublicKey#verify
    **/
    public byte[] sign(byte[] message) {
        Debug.println("gale.sign", "GalePrivateKey.sign() message");
        Debug.println("gale.sign", message);
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Debug.assrt(false);
        }
        byte[] hash = digest.digest(message);
        byte[] digestInfo = new byte[DIGEST_INFO_LEN];
        System.arraycopy(DIGEST_INFO_A, 0, digestInfo, 0, DIGEST_INFO_A.length);
        digestInfo[DIGEST_INFO_A.length] = (byte) 5;
        System.arraycopy(DIGEST_INFO_B, 0, digestInfo, DIGEST_INFO_A.length + 1, DIGEST_INFO_B.length);
        System.arraycopy(hash, 0, digestInfo, DIGEST_INFO_A.length + 1 + DIGEST_INFO_B.length, hash.length);
        Debug.println("gale.sign", "sign() digestInfo");
        Debug.println("gale.sign", digestInfo);
        BigInteger signatureInt = privateEncrypt(digestInfo);
        Debug.assrt(signatureInt.signum() == 1);
        byte[] toReturn = signatureInt.toByteArray();
        if (toReturn[0] == 0) {
            byte[] newSigData = new byte[toReturn.length - 1];
            System.arraycopy(toReturn, 1, newSigData, 0, newSigData.length);
            toReturn = newSigData;
        }
        Debug.println("gale.sign", "sign() SIGNATURE");
        Debug.println("gale.sign", toReturn);
        return toReturn;
    }

    public BigInteger privateEncrypt(byte[] input) {
        int i;
        int modulusLen = (m_bits + 7) / 8;
        if (input.length + 11 > modulusLen) throw new IllegalArgumentException("input length must be sufficiently smaller than " + modulusLen);
        byte[] pkcsBlock = new byte[modulusLen];
        pkcsBlock[0] = 0;
        pkcsBlock[1] = 1;
        for (i = 2; i < modulusLen - input.length - 1; i++) pkcsBlock[i] = (byte) 0xff;
        pkcsBlock[i++] = 0;
        System.arraycopy(input, 0, pkcsBlock, i, input.length);
        BigInteger intInput = new BigInteger(1, pkcsBlock);
        BigInteger signature = intInput.modPow(getPrivateExponent(), getModulus());
        intInput = null;
        return signature;
    }

    /**
    * RSA private-key decryption, according to PKCS #1.
    **/
    public byte[] privateDecrypt(byte[] input) {
        int i;
        int modulusLen = (m_bits + 7) / 8;
        if (input.length > modulusLen) throw new IllegalArgumentException("input length must be at most " + modulusLen);
        BigInteger intInput = new BigInteger(1, input);
        byte[] pkcsBlockOrig = intInput.modPow(getPrivateExponent(), getModulus()).toByteArray();
        byte[] pkcsBlock = new byte[modulusLen];
        System.arraycopy(pkcsBlockOrig, 0, pkcsBlock, pkcsBlock.length - pkcsBlockOrig.length, pkcsBlockOrig.length);
        Debug.println("gale.decrypt", "POSTDECRYPT PKCSBLOCK");
        Debug.println("gale.decrypt", pkcsBlock);
        if ((pkcsBlock[0] != 0) || (pkcsBlock[1] != 2)) throw new IllegalArgumentException("Required block type 2");
        for (i = 2; i < modulusLen - 1; ++i) {
            if (pkcsBlock[i] == 0) break;
        }
        i++;
        if (i >= modulusLen) throw new IllegalArgumentException("Invalid data");
        byte[] output = new byte[modulusLen - i];
        if (output.length + 11 > modulusLen) throw new IllegalArgumentException("Output length should be " + "sufficiently smaller than modulus length");
        System.arraycopy(pkcsBlock, i, output, 0, output.length);
        for (i = 0; i < pkcsBlock.length; ++i) {
            pkcsBlock[i] = 0;
        }
        Debug.println("gale.decrypt", "PRIVATE DECRYPT");
        Debug.println("gale.decrypt", output);
        return output;
    }

    /**
    * Convenience method which writes this private key to the given
    * stream using the stream version this was originally constructed
    * with.
    * @param gout the output stream to write to
    * @exception NotEnoughInfoException if this is a stub key.  Use
    * exportStub instead.
    * @exception IOException if any write to the stream throws
    * IOException
    **/
    public void write(GaleOutputStream gout) throws IOException, NotEnoughInfoException {
        throw new NotEnoughInfoException("NYI");
    }

    public String toString() {
        return "Private Key: <" + m_id + "> (" + m_bits + " bits)";
    }
}
