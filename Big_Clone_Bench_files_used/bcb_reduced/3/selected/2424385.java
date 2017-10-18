package wtanaka.praya.gale;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Random;
import wtanaka.debug.Debug;

/**
 * Represents a public key.
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
public class GalePublicKey implements PublicKey {

    public static final int DA_MD2 = 3;

    public static final int DA_MD5 = 5;

    private static final long RSA_F4 = 0x10001L;

    public static final int MIN_RSA_MODULUS_BITS = 508;

    public static final int MAX_RSA_MODULUS_BITS = 1024;

    public static final int MAX_RSA_MODULUS_LEN = ((MAX_RSA_MODULUS_BITS + 7) / 8);

    public static final int MAX_RSA_PRIME_BITS = ((MAX_RSA_MODULUS_BITS + 1) / 2);

    public static final int MAX_RSA_PRIME_LEN = ((MAX_RSA_PRIME_BITS + 7) / 8);

    public static final int MAX_DIGEST_LEN = 16;

    public static final byte[] DIGEST_INFO_A = { 0x30, 0x20, 0x30, 0x0c, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x02 };

    public static final int DIGEST_INFO_A_LEN = DIGEST_INFO_A.length;

    public static byte[] DIGEST_INFO_B = { 0x05, 0x00, 0x04, 0x10 };

    public static final int DIGEST_INFO_B_LEN = DIGEST_INFO_B.length;

    public static final int DIGEST_INFO_LEN = DIGEST_INFO_A.length + 1 + DIGEST_INFO_B.length + 16;

    public static final int MAX_SIGNATURE_LEN = MAX_RSA_MODULUS_LEN;

    String m_id;

    int m_bits;

    String m_owner;

    BigInteger m_rsaExponent;

    BigInteger m_rsaModulus;

    Timestamp m_signedDate;

    Timestamp m_expireDate;

    FragmentList m_fragments;

    byte[] m_signature;

    int m_originalStreamVersion = -1;

    /**
    * Convenience Constructor which reads in from a file
    **/
    public GalePublicKey(File f) throws IOException {
        this(new GaleInputStream(new FileInputStream(f)));
    }

    /**
    * Constructor which reads in from a file
    **/
    public GalePublicKey(GaleInputStream in) throws IOException {
        byte[] magic = new byte[4];
        if (4 != in.read(magic)) {
            throw new StreamCorruptedException("Expecting Magic");
        }
        if (magic[0] == 0x47 && magic[1] == 0x41 && magic[2] == 0x4C && magic[3] == 0x45) {
            m_originalStreamVersion = 3;
            byte[] version = new byte[2];
            in.read(version);
            if (version[0] == 0x00 && version[1] == 0x01) {
                m_id = in.readCountedUnicode();
                m_fragments = new FragmentList(new FragmentInterface[0]);
                while (in.available() > 0) {
                    FragmentInterface fragment = in.readFragment();
                    m_fragments = m_fragments.unionWith(fragment);
                }
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
            if (in.available() > 0) throw new StreamCorruptedException("Extra data at end of stream");
        } else if (magic[0] == 0x68 && magic[1] == 0x13 && magic[2] == 0x00 && magic[3] == 0x02) {
            m_originalStreamVersion = 2;
            m_id = in.readCountedUnicode();
            if (in.available() > 0) {
                m_owner = in.readCountedUnicode();
                m_bits = in.readInt();
                m_rsaModulus = in.readRLE(MAX_RSA_MODULUS_LEN);
                m_rsaExponent = in.readRLE(MAX_RSA_MODULUS_LEN);
            }
            if (in.available() > 0) {
                m_signedDate = in.readTime();
                m_expireDate = in.readTime();
                m_signature = new byte[in.available()];
                in.read(m_signature);
            }
        } else if (magic[0] == 0x68 && magic[1] == 0x13 && magic[2] == 0x00 && magic[3] == 0x00) {
            m_originalStreamVersion = 1;
            m_id = in.readNullTerminated();
            if (in.available() > 0) {
                m_owner = in.readNullTerminated();
                m_bits = in.readInt();
                m_rsaModulus = in.readRLE(MAX_RSA_MODULUS_LEN);
                m_rsaExponent = in.readRLE(MAX_RSA_MODULUS_LEN);
            }
            if (in.available() > 0) {
                m_signature = new byte[in.available()];
                in.read(m_signature);
                Debug.println("gale.pubkey", "ctr: version 1 signature");
                Debug.println("gale.pubkey", m_signature);
            }
        } else {
            throw new StreamCorruptedException("Unknown Private Key Format");
        }
    }

    /**
    * package visible constructor which constructs a key suitable for
    * stub export.
    **/
    GalePublicKey(String id) {
        m_id = id;
    }

    /**
    * Constructor for a newly generated key.
    **/
    public GalePublicKey(String owner, String id, int bits, BigInteger rsaModulus, BigInteger rsaExponent) {
        m_originalStreamVersion = 3;
        m_id = id;
        m_fragments = new FragmentList(new FragmentInterface[0]);
        FragmentInterface fragment;
        m_rsaModulus = rsaModulus;
        fragment = new InMemoryDataFragment("rsa.modulus", rsaModulus.toByteArray());
        m_fragments = m_fragments.unionWith(fragment);
        m_rsaExponent = rsaExponent;
        fragment = new InMemoryDataFragment("rsa.exponent", rsaExponent.toByteArray());
        m_fragments = m_fragments.unionWith(fragment);
        m_bits = bits;
        fragment = new InMemoryNumberFragment("rsa.bits", bits);
        m_fragments = m_fragments.unionWith(fragment);
        m_owner = "";
        m_owner = owner;
        fragment = new InMemoryTextFragment("key.owner", owner);
        m_fragments = m_fragments.unionWith(fragment);
    }

    public GalePublicKey sign(GalePrivateKey priv) throws NoSuchAlgorithmException {
        GalePublicKey toReturn = new GalePublicKey(m_owner, m_id, m_bits, m_rsaModulus, m_rsaExponent);
        toReturn.m_fragments = new FragmentList(new FragmentInterface[] { new SignatureFragment(toReturn.m_fragments, priv) });
        return toReturn;
    }

    /**
    * @deprecated use getBackwardID instead
    **/
    public String getID() {
        return getBackwardID();
    }

    /**
    * Gets the ID of this key in reversed order (most specific key
    * part first).
    **/
    public String getBackwardID() {
        return m_id;
    }

    /**
    * exports EXPORT_STUB format, for the given version
    *
    * Stub lengths:
    * <UL>
    * <LI> * Version 2:<br>
    * 4 // magic<br>
    * + 4 // id length<br>
    * + m_id.length()*2 // id length
    * </UL>
    *
    * Locks the output stream with synchronized (gout)
    * @param gout the output stream on which to export the stub key.
    * @param version should be "2"
    * @exception IllegalArgumentException if version != 2
    **/
    public void exportStub(GaleOutputStream gout, int version) throws IOException {
        switch(version) {
            case 2:
                synchronized (gout) {
                    gout.writeInt(0x68130002);
                    gout.writeUCS2WithLengthInChars(m_id);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown stub export version " + version);
        }
    }

    public int getStreamLength(int version) {
        ByteArrayOutputStream bOut;
        GaleOutputStream gOut;
        switch(version) {
            case 1:
                bOut = new ByteArrayOutputStream();
                gOut = new GaleOutputStream(bOut);
                try {
                    gOut.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaModulus);
                    gOut.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaExponent);
                } catch (IOException e) {
                    Debug.assrt(false);
                }
                return 4 + new NulTerminatedString(m_id).getStreamLength() + new NulTerminatedString(m_owner).getStreamLength() + 4 + bOut.toByteArray().length + m_signature.length;
            case 2:
                bOut = new ByteArrayOutputStream();
                gOut = new GaleOutputStream(bOut);
                try {
                    gOut.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaModulus);
                    gOut.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaExponent);
                } catch (IOException e) {
                    Debug.assrt(false);
                }
                return 4 + new CountedUnicodeString(m_id).getStreamLength() + new CountedUnicodeString(m_owner).getStreamLength() + 4 + bOut.toByteArray().length + 16 + 16 + m_signature.length;
            case 3:
                return 4 + 2 + 4 + m_id.length() * 2 + m_fragments.getStreamLength();
            default:
                throw new IllegalArgumentException("Unknown output version " + version);
        }
    }

    /**
    * Writes the public key to the stream with the given stream
    * version.  Locks the stream with synchronized (gout).
    * @param gout the output stream to write to
    * @param version the key file format version to use when writing
    * this key
    * @exception NotEnoughInfoException if this is a stub key.  Use
    * exportStub instead.
    * @exception IOException if any write to the stream throws
    * IOException
    **/
    public void write(GaleOutputStream gout, int version) throws IOException, NotEnoughInfoException {
        switch(version) {
            case 1:
                synchronized (gout) {
                    gout.writeInt(0x68130000);
                    new NulTerminatedString(m_id).write(gout);
                    if (m_owner == null || m_rsaModulus == null || m_rsaExponent == null) {
                        throw new NotEnoughInfoException("Trying to write a stub key");
                    }
                    new NulTerminatedString(m_owner).write(gout);
                    gout.writeInt(m_bits);
                    gout.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaModulus);
                    gout.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaExponent);
                    if (m_signature == null) {
                        throw new NotEnoughInfoException("Trying to write an unsigned key");
                    }
                    Debug.println("gale.pubkey", "write: version 1 signature");
                    Debug.println("gale.pubkey", m_signature);
                    gout.write(m_signature);
                }
                break;
            case 2:
                synchronized (gout) {
                    gout.writeInt(0x68130002);
                    new CountedUnicodeString(m_id).write(gout);
                    if (m_owner == null || m_rsaModulus == null || m_rsaExponent == null) {
                        throw new NotEnoughInfoException("Trying to write a stub key");
                    }
                    gout.writeCountedUnicode(m_owner);
                    gout.writeInt(m_bits);
                    gout.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaModulus);
                    gout.writeRLE(MAX_RSA_MODULUS_LEN, m_rsaExponent);
                    if (m_signedDate == null || m_expireDate == null || m_signedDate == null) {
                        throw new NotEnoughInfoException("Trying to write an unsigned key");
                    }
                    gout.writeTime(m_signedDate);
                    gout.writeTime(m_expireDate);
                    gout.write(m_signature);
                }
                break;
            case 3:
                synchronized (gout) {
                    gout.writeInt(0x47414c45);
                    gout.writeByte(0x00);
                    gout.writeByte(0x01);
                    gout.writeUCS2WithLengthInChars(m_id);
                    m_fragments.write(gout);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown output version " + version);
        }
    }

    /**
    * Convenience method which gets the stream length for this public
    * key using the stream version this was originally constructed
    * with.
    **/
    public int getStreamLength() {
        return getStreamLength(m_originalStreamVersion);
    }

    /**
    * Convenience method which writes this public key to the given
    * stream using the stream version this was originally constructed
    * with.
    * @param gout the output stream to write to
    * @exception NotEnoughInfoException if this is a stub key.  Use
    * exportStub instead.
    * @exception IOException if any write to the stream throws
    * IOException
    **/
    public void write(GaleOutputStream gout) throws IOException, NotEnoughInfoException {
        write(gout, m_originalStreamVersion);
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

    /**
    * Verify that the given message, when signed with the private key
    * corresponding to this key, produces the given signature.
    *
    * @see GalePrivateKey#sign
    **/
    public void verify(byte[] signature, byte[] message) throws AuthException, InvalidKeyFormatException {
        if (signature.length > MAX_SIGNATURE_LEN) throw new AuthException("Signature expected to be at most " + MAX_SIGNATURE_LEN + " long");
        Debug.println("gale.verify", "PublicKey.verify() message");
        Debug.println("gale.verify", message);
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Debug.assrt(false);
        }
        byte[] digest = messageDigest.digest(message);
        byte[] digestInfo = encodeDigestInfo(digest, DA_MD5);
        Debug.println("gale.verify", "PublicKey.verify() calculated digest");
        Debug.println("gale.verify", digestInfo);
        try {
            byte[] originalDigestInfo = publicDecrypt(signature).toByteArray();
            Debug.println("gale.verify", "PublicKey.verify() attached digest");
            Debug.println("gale.verify", originalDigestInfo);
            if (digestInfo.length != originalDigestInfo.length) throw new AuthException("Original Digest not the same size as " + " calculated digest"); else {
                for (int i = 0; i < digestInfo.length; ++i) {
                    if (digestInfo[i] != originalDigestInfo[i]) throw new AuthException("Mismatch in byte " + i);
                }
            }
        } catch (InvalidKeyFormatException e) {
            throw new AuthException(e.getMessage());
        }
    }

    private static byte[] encodeDigestInfo(byte[] digest, int digestAlgorithm) {
        byte[] digestInfo = new byte[DIGEST_INFO_LEN];
        System.arraycopy(DIGEST_INFO_A, 0, digestInfo, 0, DIGEST_INFO_A_LEN);
        digestInfo[DIGEST_INFO_A_LEN] = (digestAlgorithm == DA_MD2) ? (byte) 2 : (byte) 5;
        System.arraycopy(DIGEST_INFO_B, 0, digestInfo, DIGEST_INFO_A_LEN + 1, DIGEST_INFO_B_LEN);
        System.arraycopy(digest, 0, digestInfo, DIGEST_INFO_A_LEN + 1 + DIGEST_INFO_B_LEN, 16);
        return digestInfo;
    }

    private byte[] publicBlock(byte[] input) {
        byte[] pkcsBlock = new byte[(getBits() + 7) / 8];
        byte[] shortData = new BigInteger(1, input).modPow(getPublicExponent(), getModulus()).toByteArray();
        if (shortData.length > pkcsBlock.length) {
            for (int i = 0; i < shortData.length - pkcsBlock.length; ++i) {
                Debug.assrt(shortData[i] == 0, "shortData[" + i + "] is " + shortData[i]);
            }
            System.arraycopy(shortData, shortData.length - pkcsBlock.length, pkcsBlock, 0, pkcsBlock.length);
        } else {
            System.arraycopy(shortData, 0, pkcsBlock, pkcsBlock.length - shortData.length, shortData.length);
        }
        return pkcsBlock;
    }

    byte[] publicEncrypt(byte[] input) throws InvalidKeyFormatException {
        int i;
        Random random = new SecureRandom();
        int modulusLen = (m_bits + 7) / 8;
        byte[] pkcsBlock = new byte[modulusLen];
        if (input.length + 11 > modulusLen) throw new InvalidKeyFormatException("input length must be at most " + modulusLen);
        pkcsBlock[0] = 0;
        pkcsBlock[1] = 2;
        for (i = 2; i < modulusLen - input.length - 1; i++) {
            byte b;
            do {
                b = (byte) random.nextInt();
            } while (b == 0);
            pkcsBlock[i] = b;
        }
        pkcsBlock[i++] = 0;
        System.arraycopy(input, 0, pkcsBlock, i, input.length);
        byte[] output = publicBlock(pkcsBlock);
        for (i = 0; i < pkcsBlock.length; ++i) pkcsBlock[i] = 0;
        return output;
    }

    /**
    * public decrypt
    **/
    public BigInteger publicDecrypt(byte[] input) throws InvalidKeyFormatException, AuthException {
        int i;
        int modulusLen = (getBits() + 7) / 8;
        if (input.length > modulusLen) throw new InvalidKeyFormatException("input length must be sufficiently smaller than " + modulusLen);
        byte[] pkcsBlock = publicBlock(input);
        if (pkcsBlock.length != modulusLen) throw new InvalidKeyFormatException("Expecting pkcs block length (" + pkcsBlock.length + ") to be the same as the modulus length (" + modulusLen + ")");
        if ((pkcsBlock[0] != 0) || (pkcsBlock[1] != 1)) {
            throw new AuthException("Decryption Failed");
        }
        for (i = 2; i < modulusLen - 1; i++) if (pkcsBlock[i] != (byte) 0xff) break;
        if (pkcsBlock[i++] != 0) throw new InvalidKeyFormatException("Expecting pkcs separator");
        int outputLen = modulusLen - i;
        if (outputLen + 11 > modulusLen) throw new InvalidKeyFormatException("output length must be sufficiently smaller than " + "modulust length");
        byte[] output = new byte[outputLen];
        System.arraycopy(pkcsBlock, i, output, 0, outputLen);
        for (i = 0; i < pkcsBlock.length; ++i) pkcsBlock[i] = 0;
        pkcsBlock = null;
        return new BigInteger(1, output);
    }

    public FragmentList getFragmentList() {
        return m_fragments;
    }

    public int getBits() {
        return m_bits;
    }

    /**
    * If the key.redirect field exists, this method returns the merged
    * location that this key points at.  If the key.redirect field
    * does not exist, return null.
    **/
    public Location getRedirectTarget() {
        if (m_fragments == null) return null;
        FragmentInterface redirect = m_fragments.find(FragmentInterface.KEY_REDIRECT, true);
        if (redirect == null) return null;
        if (!(redirect instanceof InMemoryTextFragment)) return null;
        String value = ((InMemoryTextFragment) redirect).getValue();
        return new Location(value, "bug.461937");
    }

    public Location getLocation() {
        return new Location(new Location(this.getID()).getReversedString());
    }

    /**
    * If the key.member field exists, this method returns the content
    * of that fragment.  If the key.member field does not exist,
    * return null
    **/
    public String getKeyMember() {
        if (m_fragments == null) return null;
        FragmentInterface redirect = m_fragments.find(FragmentInterface.KEY_MEMBER, true);
        if (redirect == null) return null;
        if (!(redirect instanceof InMemoryTextFragment)) return null;
        String value = ((InMemoryTextFragment) redirect).getValue();
        return value;
    }

    public String toString() {
        return "Public Key: " + m_owner + " <" + m_id + ">" + " (" + getBits() + " bits)" + " " + (m_signedDate == null ? "Unsigned" : ("Signed " + m_signedDate)) + "," + " " + (m_expireDate == null ? "No Expire Date" : ("Expires " + m_expireDate)) + "\n" + "exponent = " + m_rsaExponent + "\n" + "modulus = " + m_rsaModulus;
    }

    public BigInteger getModulus() {
        return m_rsaModulus;
    }

    public BigInteger getPublicExponent() {
        return m_rsaExponent;
    }

    /**
    * Gets the owner of this key ("comment" in v1 format, 
    * "comment" in v2 format, "key.owner" in v3 format)
    **/
    public String getOwner() {
        return m_owner;
    }
}
