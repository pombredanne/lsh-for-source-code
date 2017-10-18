package net.sourceforge.epoint.pgp;

import java.io.*;
import java.security.*;
import java.math.*;
import java.util.Date;
import net.sourceforge.epoint.util.*;
import net.sourceforge.epoint.io.PacketHeader;

/**
 * OpenPGP Public or Private Key packet
 * 
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class KEYPacket extends Packet {

    public static final String ERROR_ALGO = "Unsupported public key algorithm ";

    /**
     * version
     */
    private int version;

    /**
     * algorithm
     */
    private int algorithm;

    /**
     * creation time
     */
    private int creationTime;

    /**
     * expiration
     */
    private int expirationDays;

    /**
     * key material
     */
    private BigInteger[] material;

    /**
     * public key length
     */
    private int pkLength;

    public KEYPacket(InputStream in) throws IOException, SyntaxException, NoSuchAlgorithmException {
        super(in, true);
        if ((getTag() != PUBKEY) && (getTag() != SECKEY) && (getTag() != SUBKEY) && (getTag() != SECSUBKEY)) throw new SyntaxException(TAG_ERROR + " " + Integer.toString(getTag()));
        init();
    }

    public KEYPacket(Packet p) throws IOException, SyntaxException, NoSuchAlgorithmException {
        super(p);
        save();
        if ((getTag() != PUBKEY) && (getTag() != SECKEY) && (getTag() != SUBKEY) && (getTag() != SECSUBKEY)) throw new SyntaxException(TAG_ERROR + " " + Integer.toString(getTag()));
        init();
    }

    private void init() throws IOException, SyntaxException, NoSuchAlgorithmException {
        switch(version = integer(1)) {
            case 3:
                creationTime = integer(4);
                expirationDays = integer(2);
                switch(algorithm = integer(1)) {
                    case Algo.RSA_ES:
                    case Algo.RSA_E:
                    case Algo.RSA_S:
                        material = mpis(2);
                        break;
                    default:
                        throw new NoSuchAlgorithmException(ERROR_ALGO + algorithm);
                }
                break;
            case 4:
                creationTime = integer(4);
                switch(algorithm = integer(1)) {
                    case Algo.RSA_ES:
                    case Algo.RSA_E:
                    case Algo.RSA_S:
                        material = mpis(2);
                        break;
                    case Algo.DSA_S:
                        material = mpis(4);
                        break;
                    case Algo.ELGAMAL_E:
                    case Algo.ELGAMAL_ES:
                        material = mpis(3);
                        break;
                    default:
                        throw new NoSuchAlgorithmException(ERROR_ALGO + algorithm);
                }
                break;
            default:
                throw new NoSuchAlgorithmException(ERROR_VERSION + version);
        }
        pkLength = getPayloadLength() - getTail();
    }

    /**
     * @return public cryptographic key
     */
    public PublicKey getPublicKey() throws NoSuchAlgorithmException {
        switch(algorithm) {
            case Algo.RSA_ES:
            case Algo.RSA_E:
            case Algo.RSA_S:
                return new RSAPublicKey(this);
            case Algo.DSA_S:
                return new DSAPublicKey(this);
            case Algo.ELGAMAL_E:
            case Algo.ELGAMAL_ES:
                return new ElGamalPublicKey(this);
            default:
                throw new NoSuchAlgorithmException(ERROR_ALGO);
        }
    }

    /**
     * @param i material index
     * @return key material
     */
    public BigInteger getMaterial(int i) {
        return new BigInteger(material[i].toByteArray());
    }

    /**
     * @return public key algorithm
     */
    public int getAlgorithm() {
        return algorithm;
    }

    /**
     * @return version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return creation date
     */
    public Date getDate() {
        return new Date(1000l * (long) creationTime);
    }

    /**
     * @return Public Key Packet Data
     */
    public byte[] toByteArrayPUBKEY() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int t = getTag();
        out.write(0x99);
        out.write(pkLength >> 8);
        out.write(pkLength);
        out.write(getPayload(), 0, pkLength);
        return out.toByteArray();
    }

    /**
     * Fingerprint
     * @return key fingerprint or <code>null</code> on error
     * @see #getFingerprintText
     */
    public byte[] getFingerprint() {
        try {
            MessageDigest md;
            switch(version) {
                case 3:
                    md = MessageDigest.getInstance("MD5");
                    md.update(material[0].toByteArray());
                    md.update(material[1].toByteArray());
                    return md.digest();
                case 4:
                    md = MessageDigest.getInstance("SHA1");
                    byte[] head = { (byte) 0x99, (byte) (pkLength >> 8), (byte) pkLength };
                    md.update(head);
                    md.update(getPayload(), 0, pkLength);
                    return md.digest();
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Key ID
     * @return 8-octet key ID or <code>null</code> on error
     */
    public byte[] getKeyID() {
        try {
            int i;
            byte[] id = new byte[8], fp;
            switch(version) {
                case 3:
                    fp = material[0].toByteArray();
                    break;
                case 4:
                    fp = getFingerprint();
                    break;
                default:
                    return null;
            }
            System.arraycopy(fp, fp.length - 8, id, 0, 8);
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fingerprint <code>String</code> representation in hexadecimal
     * delimited by spaces or other non-alphanumeric caracters
     * for easier reading and visual verification.
     * This representation should <b>NOT</b> be used for verification
     * purposes, as its delimitation may change across versions.
     * @return key fingerprint in formatted text or empty string on error
     * @see #getFingerprint
     */
    public String getFingerprintText() {
        byte[] fp = getFingerprint();
        if (fp == null) return "";
        if (fp.length == 16) return Splitter.group(Base16.encode(fp), 2, 8);
        return Splitter.group(Base16.encode(fp), 4, 5);
    }

    /**
     * for debugging purposes
     */
    public String toString() {
        try {
            return getPublicKey().toString();
        } catch (Exception e) {
            return e.toString();
        }
    }
}
