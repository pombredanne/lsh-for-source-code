package org.isodl.service;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import net.sourceforge.scuba.util.Hex;
import java.io.IOException;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DEREnumerated;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DERUnknownTag;

/**
 * Some static helper functions. Mostly dealing with low-level crypto.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @author Ronny Wichers Schreur (ronny@cs.ru.nl)
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
class Util {

    public static final int ENC_MODE = 1;

    public static final int MAC_MODE = 2;

    private Util() {
    }

    /**
     * Derives the ENC or MAC key from the keySeed.
     * 
     * @param keySeed
     *            the key seed.
     * @param mode
     *            either <code>ENC_MODE</code> or <code>MAC_MODE</code>.
     * 
     * @return the key.
     */
    public static SecretKey deriveKey(byte[] keySeed, int mode) throws GeneralSecurityException {
        MessageDigest shaDigest = MessageDigest.getInstance("SHA1");
        shaDigest.update(keySeed);
        byte[] c = { 0x00, 0x00, 0x00, (byte) mode };
        shaDigest.update(c);
        byte[] hash = shaDigest.digest();
        byte[] key = new byte[24];
        System.arraycopy(hash, 0, key, 0, 8);
        System.arraycopy(hash, 8, key, 8, 8);
        System.arraycopy(hash, 0, key, 16, 8);
        SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
        return desKeyFactory.generateSecret(new DESedeKeySpec(key));
    }

    public static long computeSendSequenceCounter(byte[] rndICC, byte[] rndIFD) {
        if (rndICC == null || rndICC.length != 8 || rndIFD == null || rndIFD.length != 8) {
            throw new IllegalStateException("Wrong length input");
        }
        long ssc = 0;
        for (int i = 4; i < 8; i++) {
            ssc <<= 8;
            ssc += (long) (rndICC[i] & 0x000000FF);
        }
        for (int i = 4; i < 8; i++) {
            ssc <<= 8;
            ssc += (long) (rndIFD[i] & 0x000000FF);
        }
        return ssc;
    }

    /**
     * Pads the input <code>in</code> according to ISO9797-1 padding method 2.
     * 
     * @param in
     *            input
     * 
     * @return padded output
     */
    public static byte[] pad(byte[] in) {
        return pad(in, 0, in.length);
    }

    public static byte[] pad(byte[] in, int offset, int length) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(in, offset, length);
        out.write((byte) 0x80);
        while (out.size() % 8 != 0) {
            out.write((byte) 0x00);
        }
        return out.toByteArray();
    }

    public static byte[] unpad(byte[] in) {
        int i = in.length - 1;
        while (i >= 0 && in[i] == 0x00) {
            i--;
        }
        if ((in[i] & 0x000000FF) != 0x00000080) {
            throw new IllegalStateException("unpad expected constant 0x80, found 0x" + Integer.toHexString((in[i] & 0x000000FF)) + "\nDEBUG: in = " + Hex.bytesToHexString(in) + ", index = " + i);
        }
        byte[] out = new byte[i];
        System.arraycopy(in, 0, out, 0, i);
        return out;
    }

    /**
     * Recovers the M1 part of the message sent back by the AA protocol
     * (INTERNAL AUTHENTICATE command). The algorithm is described in ISO
     * 9796-2:2002 9.3.
     * 
     * Based on code by Ronny (ronny@cs.ru.nl) who presumably ripped this from
     * Bouncy Castle.
     * 
     * @param digestLength
     *            should be 20
     * @param plaintext
     *            response from card, already 'decrypted' (using the AA public
     *            key)
     * 
     * @return the m1 part of the message
     */
    public static byte[] recoverMessage(int digestLength, byte[] plaintext) {
        if (plaintext == null || plaintext.length < 1) {
            throw new IllegalArgumentException("Plaintext too short to recover message");
        }
        if (((plaintext[0] & 0xC0) ^ 0x40) != 0) {
            throw new NumberFormatException("Could not get M1");
        }
        if (((plaintext[plaintext.length - 1] & 0xF) ^ 0xC) != 0) {
            throw new NumberFormatException("Could not get M1");
        }
        int delta = 0;
        if (((plaintext[plaintext.length - 1] & 0xFF) ^ 0xBC) == 0) {
            delta = 1;
        } else {
            throw new NumberFormatException("Could not get M1");
        }
        int paddingLength = 0;
        for (; paddingLength < plaintext.length; paddingLength++) {
            if (((plaintext[paddingLength] & 0x0F) ^ 0x0A) == 0) {
                break;
            }
        }
        int messageOffset = paddingLength + 1;
        int paddedMessageLength = plaintext.length - delta - digestLength;
        int messageLength = paddedMessageLength - messageOffset;
        if (messageLength <= 0) {
            throw new NumberFormatException("Could not get M1");
        }
        if ((plaintext[0] & 0x20) == 0) {
            throw new NumberFormatException("Could not get M1");
        } else {
            byte[] recoveredMessage = new byte[messageLength];
            System.arraycopy(plaintext, messageOffset, recoveredMessage, 0, messageLength);
            return recoveredMessage;
        }
    }

    public static String printDERObject(byte[] derBytes) throws IOException {
        ASN1InputStream asn1 = new ASN1InputStream(derBytes);
        return printDERObject(asn1.readObject());
    }

    public static String printDERObject(DERObject derObj) throws IOException {
        if (derObj instanceof DERSequence) return printDERObject((DERSequence) derObj);
        if (derObj instanceof DERSet) return printDERObject((DERSet) derObj);
        if (derObj instanceof DERTaggedObject) return printDERObject((DERTaggedObject) derObj);
        if (derObj instanceof DERNull) return printDERObject((DERNull) derObj);
        if (derObj instanceof DERUnknownTag) return printDERObject((DERUnknownTag) derObj);
        if (derObj instanceof DERObjectIdentifier) return printDERObject((DERObjectIdentifier) derObj);
        if (derObj instanceof DERString) return printDERObject((DERString) derObj);
        if (derObj instanceof DEROctetString) return printDERObject((DEROctetString) derObj);
        if (derObj instanceof DERUTCTime) return printDERObject((DERUTCTime) derObj);
        if (derObj instanceof DERGeneralizedTime) return printDERObject((DERGeneralizedTime) derObj);
        if (derObj instanceof DEREnumerated) return printDERObject((DEREnumerated) derObj);
        if (derObj instanceof DERInteger) return printDERObject((DERInteger) derObj);
        if (derObj instanceof DERBoolean) return printDERObject((DERBoolean) derObj);
        if (derObj instanceof DERApplicationSpecific) return printDERObject((DERApplicationSpecific) derObj);
        return derObj.getClass().getSimpleName() + "?";
    }

    public static String printDERObject(DERSequence derSeq) throws IOException {
        String r = "DERSequence:";
        for (Enumeration<DERObject> e = derSeq.getObjects(); e.hasMoreElements(); ) {
            r = r + "\n  " + printDERObject(e.nextElement()).replaceAll("\n", "\n  ");
        }
        return r;
    }

    public static String printDERObject(DERSet derSet) throws IOException {
        String r = "DERSet:";
        for (Enumeration<DERObject> e = derSet.getObjects(); e.hasMoreElements(); ) {
            r = r + "\n  " + printDERObject(e.nextElement()).replaceAll("\n", "\n  ");
        }
        return r;
    }

    public static String printDERObject(DERTaggedObject derTaggedObject) throws IOException {
        String r = "DERTaggedObject:";
        r = r + "\n  TagNum: " + derTaggedObject.getTagNo();
        r = r + "\n  Object: " + printDERObject(derTaggedObject.getObject()).replaceAll("\n", "\n  ");
        return r;
    }

    public static String printDERObject(DERNull derNull) throws IOException {
        return "DERNull" + Hex.bytesToHexString(derNull.getEncoded());
    }

    public static String printDERObject(DERUnknownTag derUnknownTag) {
        return "DERUnknownTag: " + derUnknownTag.getTag();
    }

    public static String printDERObject(DERObjectIdentifier derObjectIdentifier) {
        return "DERObjectIdentifier: " + derObjectIdentifier.getId();
    }

    public static String printDERObject(DEROctetString derOctetString) {
        return "DEROctetString: " + Hex.bytesToHexString(derOctetString.getOctets());
    }

    public static String printDERObject(DERString derString) {
        return derString.getClass().getSimpleName() + ": " + derString.getString();
    }

    public static String printDERObject(DERUTCTime derUTCTime) {
        return "DERUTCTime: " + derUTCTime.getAdjustedTime();
    }

    public static String printDERObject(DERGeneralizedTime derGeneralizedTime) {
        return "DERGeneralizedTime: " + derGeneralizedTime.getTime();
    }

    public static String printDERObject(DEREnumerated derEnumerated) {
        return "DEREnumerated: " + derEnumerated.getValue();
    }

    public static String printDERObject(DERBoolean derBoolean) {
        return "DERBoolean: " + derBoolean.isTrue();
    }

    public static String printDERObject(DERInteger derInteger) {
        return "DERInteger: " + derInteger.getValue();
    }

    public static String printDERObject(DERApplicationSpecific derApplicationSpecific) {
        String r = "DERApplicationSpecific:";
        r = r + "\n  Application Tag: " + derApplicationSpecific.getApplicationTag();
        r = r + "\n  Contents:        " + Hex.bytesToHexString(derApplicationSpecific.getContents());
        return r;
    }
}
