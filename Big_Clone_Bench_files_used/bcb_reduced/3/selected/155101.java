package net.sourceforge.epoint.pgp;

import java.io.*;
import java.security.*;
import java.math.BigInteger;
import java.util.Date;
import net.sourceforge.epoint.io.PacketHeader;
import net.sourceforge.epoint.io.MPI;

/**
 * class for generating OpenPGP signatures
 *
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class PGPSignature {

    private java.security.Signature sig;

    private MessageDigest md;

    private byte[] keyID;

    private int pkAlgo;

    /**
     * OpenPGP signature
     * @param key private signatgure key
     * @param id long OpenPGP key ID
     * @param hash message digest algorithm
     */
    public PGPSignature(java.security.PrivateKey key, byte[] id, String hash) throws NoSuchAlgorithmException, InvalidKeyException {
        md = MessageDigest.getInstance(hash);
        String algo = key.getAlgorithm();
        sig = java.security.Signature.getInstance(hash + "with" + algo);
        sig.initSign(key);
        keyID = id;
        if (algo.equals("RSA")) pkAlgo = Algo.RSA_S; else if (algo.equals("DSA")) pkAlgo = Algo.DSA_S; else throw new NoSuchAlgorithmException("Invalid algorithm: " + algo);
    }

    /**
     * OpenPGP signature
     * @param key private signature key
     * @param hash message digest algorithm
     */
    public PGPSignature(SECKEYPacket key, String hash) throws NoSuchAlgorithmException, InvalidKeyException {
        PrivateKey pk = key.getPrivateKey();
        md = MessageDigest.getInstance(hash);
        sig = java.security.Signature.getInstance(hash + "with" + pk.getAlgorithm());
        sig.initSign(pk);
        keyID = key.getKeyID();
        pkAlgo = key.getAlgorithm();
    }

    /**
     * OpenPGP signature using SHA1 digest
     * @param key private signature key
     */
    public PGPSignature(SECKEYPacket key) throws NoSuchAlgorithmException, InvalidKeyException {
        this(key, "SHA1");
    }

    public PGPSignature update(byte[] buf, int off, int len) throws SignatureException {
        md.update(buf, off, len);
        sig.update(buf, off, len);
        return this;
    }

    public PGPSignature update(byte[] buf) throws SignatureException {
        md.update(buf);
        sig.update(buf);
        return this;
    }

    public PGPSignature update(byte b) throws SignatureException {
        md.update(b);
        sig.update(b);
        return this;
    }

    /**
     * Output Signature in OpenPGP V3 format
     * @param type signature type
     * @param time signature creation time
     * @param out output Stream
     */
    public void write(int type, Date time, OutputStream out) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] hashed = new byte[5];
        byte[] num;
        long date = time.getTime() / 1000l;
        hashed[0] = (byte) type;
        hashed[1] = (byte) ((date >> 24) & 0xFF);
        hashed[2] = (byte) ((date >> 16) & 0xFF);
        hashed[3] = (byte) ((date >> 8) & 0xFF);
        hashed[4] = (byte) (date & 0xFF);
        md.update(hashed);
        sig.update(hashed);
        b.write(3);
        b.write(5);
        b.write(hashed);
        b.write(keyID);
        b.write(pkAlgo);
        b.write(Algo.SHA1);
        b.write(md.digest(), 0, 2);
        hashed = sig.sign();
        switch(pkAlgo) {
            case Algo.RSA_ES:
            case Algo.RSA_S:
                MPI.write(new BigInteger(hashed), b);
                break;
            case Algo.DSA_S:
                num = new byte[hashed[3] & 0xFF];
                System.arraycopy(hashed, 4, num, 0, num.length);
                MPI.write(new BigInteger(num), b);
                num = new byte[hashed[(hashed[3] & 0xFF) + 5] & 0xFF];
                System.arraycopy(hashed, (hashed[3] & 0xFF) + 6, num, 0, num.length);
                MPI.write(new BigInteger(num), b);
        }
        hashed = b.toByteArray();
        PacketHeader.write(false, (byte) Packet.SIGNATURE, hashed.length, out);
        out.write(hashed);
    }

    /**
     * Output Signature in OpenPGP V4 format
     * @param type signature type
     * @param hashed subpackets
     * @param unhashed subpackets
     * @param out output Stream
     */
    public void write(int type, byte[] hashed, byte[] unhashed, OutputStream out) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] num = new byte[2];
        byte[] sb = { (byte) 4, (byte) type, (byte) pkAlgo, (byte) Algo.SHA1, (byte) ((hashed.length >> 8) & 0xFF), (byte) (hashed.length & 0xFF) };
        int len = 6 + hashed.length;
        md.update(sb);
        sig.update(sb);
        b.write(sb);
        md.update(hashed);
        sig.update(hashed);
        b.write(hashed);
        sb = new byte[6];
        sb[0] = (byte) 4;
        sb[1] = (byte) 0xFF;
        sb[2] = (byte) ((len >> 24) & 0xFF);
        sb[3] = (byte) ((len >> 16) & 0xFF);
        sb[4] = (byte) ((len >> 8) & 0xFF);
        sb[5] = (byte) (len & 0xFF);
        md.update(sb);
        sig.update(sb);
        num[0] = (byte) ((unhashed.length >> 8) & 0xFF);
        num[1] = (byte) (unhashed.length & 0xFF);
        b.write(num);
        b.write(unhashed);
        b.write(md.digest(), 0, 2);
        sb = sig.sign();
        switch(pkAlgo) {
            case Algo.RSA_ES:
            case Algo.RSA_S:
                MPI.write(new BigInteger(sb), b);
                break;
            case Algo.DSA_S:
                num = new byte[sb[3] & 0xFF];
                System.arraycopy(sb, 4, num, 0, num.length);
                MPI.write(new BigInteger(num), b);
                num = new byte[sb[(sb[3] & 0xFF) + 5] & 0xFF];
                System.arraycopy(sb, (sb[3] & 0xFF) + 6, num, 0, num.length);
                MPI.write(new BigInteger(num), b);
        }
        sb = b.toByteArray();
        PacketHeader.write(false, (byte) Packet.SIGNATURE, sb.length, out);
        out.write(sb);
    }

    public int getAlgorithm() {
        return pkAlgo;
    }

    public byte[] getID() {
        return keyID;
    }
}
