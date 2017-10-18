package net.javasource.net.tacacs;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * @author Jay Colson, Copyright 2000
 * @version v0.10, 06/13/2000
 */
public class Header {

    public static byte TYPE_AUTHENTIC = (byte) 0x01;

    public static byte TYPE_AUTHORIZE = (byte) 0x02;

    public static byte TYPE_ACCOUNTIN = (byte) 0x03;

    static byte[] crypt(byte versionNumber, byte sequenceNumber, byte[] body, byte headerFlags, byte[] sessionID, byte[] secretkey) throws IOException, NoSuchAlgorithmException {
        if (headerFlags == Tacacs.HEADERFLAG_UNENCRYPT) return body;
        MessageDigest md = null;
        md = MessageDigest.getInstance("MD5");
        byte[] pad = null;
        byte[] lastPad = null;
        boolean keepLoop = true;
        while (keepLoop) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(sessionID);
            baos.write(secretkey);
            baos.write(versionNumber);
            baos.write(sequenceNumber);
            if (lastPad != null) baos.write(lastPad);
            lastPad = md.digest(baos.toByteArray());
            baos.reset();
            if (pad != null) baos.write(pad);
            baos.write(lastPad);
            pad = baos.toByteArray();
            if (pad.length > body.length) keepLoop = false;
        }
        byte[] realBody = new byte[body.length];
        for (int i = 0; i < body.length; i++) {
            realBody[i] = Bytes.InttoByte(Bytes.BytetoInt(body[i]) ^ Bytes.BytetoInt(pad[i]));
        }
        return realBody;
    }

    static byte[] makeHeader(byte[] body, Byte version, byte type, Integer tacacsSequence, byte headerFlags, byte[] sessionID) throws IOException {
        byte[] Body_Len = Bytes.InttoBytes(body.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(version.byteValue());
        baos.write(type);
        baos.write(tacacsSequence.byteValue());
        baos.write(headerFlags);
        baos.write(sessionID);
        baos.write(Body_Len);
        return baos.toByteArray();
    }

    static byte extractVersionNumber(byte[] Header) {
        return (Header[0]);
    }

    static byte extractFlags(byte[] Header) {
        return (Header[3]);
    }

    static byte extractSeqNum(byte[] Header) {
        return (Header[2]);
    }

    static int extractSessionID(byte[] Header) {
        byte[] sessionID = new byte[4];
        sessionID[0] = Header[4];
        sessionID[1] = Header[5];
        sessionID[2] = Header[6];
        sessionID[3] = Header[7];
        int sessID = Bytes.IntBytetoInt(sessionID);
        return sessID;
    }

    static int extractBodyLen(byte[] header) {
        byte[] length = new byte[4];
        length[0] = header[8];
        length[1] = header[9];
        length[2] = header[10];
        length[3] = header[11];
        int bodyLen = Bytes.IntBytetoInt(length);
        return bodyLen;
    }

    static byte[] generateSessionID() {
        Random ran = new Random(System.currentTimeMillis());
        return Bytes.InttoBytes(ran.nextInt());
    }
}
