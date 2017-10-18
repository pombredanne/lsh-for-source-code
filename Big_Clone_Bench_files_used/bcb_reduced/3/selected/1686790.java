package com.thegreatchina.im.msn.backend.cmd.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import com.thegreatchina.im.IMException;
import com.thegreatchina.im.msn.backend.cmd.ClientCommand;
import com.thegreatchina.im.msn.backend.cmd.Payload;
import com.thegreatchina.im.msn.backend.cmd.ServerCommand;
import com.thegreatchina.im.msn.backend.cmd.WithTrId;

/**
 * When you receive a challenge, you must send a QRY command to the server 
 * within about 50 seconds or you'll be disconnected. QRY is a payload command 
 * with a TrID and two parameters: a client identification string and the number 
 * of bytes in the payload. As with all payload commands, the length is be followed 
 * by a newline, then the payload, which is not followed by a newline. The payload 
 * of the QRY command is the "MD5 digest" of the challenge string and a client 
 * identification code. MD5 digests are always 32 bytes in length.
 *
 * If you sent an invalid email address or incorrect digest, the server will send 
 * error 540 and close the connection. If you get the payload wrong, the results 
 * will be hard to predict and not good. Otherwise, the server will send a QRY 
 * response with the same TrID and no parameters.
 * 
 * @author patrick_jiang
 *
 */
public class QRY extends ClientCommand implements WithTrId, Payload {

    public static final String CLIENT_ID_STRING = "PROD0038W!61ZTF9";

    public static final String CLIENT_ID_CODE = "VT6PX?UQTM4WM%YR";

    private static Logger logger = Logger.getLogger(QRY.class);

    private String challengeString;

    public QRY(int trId, String challenge) {
        super("QRY", trId, new String[] { CLIENT_ID_STRING, "32" });
        this.challengeString = challenge;
    }

    public byte[] getBytes() throws IMException {
        String string = this.challengeString + CLIENT_ID_CODE;
        byte[] md5 = this.getMD5(string);
        try {
            byte[] cmd = (super.toString() + ENTER).getBytes("utf-8");
            return this.copy(cmd, md5);
        } catch (UnsupportedEncodingException ue) {
            logger.error(ue);
        }
        return null;
    }

    @Override
    public ServerCommand getResponse() {
        return new com.thegreatchina.im.msn.backend.cmd.server.QRY(this.getTrId());
    }

    /**
	 * MD 5 generator
	 * @param string
	 * @return
	 * @throws IMException
	 */
    private byte[] getMD5(String string) throws IMException {
        byte[] buffer = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(string.getBytes("utf-8"));
            buffer = md.digest();
            buffer = getHexString(buffer);
        } catch (NoSuchAlgorithmException e) {
            throw new IMException(e);
        } catch (UnsupportedEncodingException ue) {
            throw new IMException(ue);
        }
        return buffer;
    }

    private byte[] getHexString(byte[] buffer) {
        byte[] bb = new byte[32];
        for (int i = 0; i < buffer.length; i++) {
            int x = buffer[i];
            if (x < 0) {
                x = 256 + x;
            }
            int y = x / 16;
            int z = x - y * 16;
            bb[i * 2] = getHexChar(y);
            bb[i * 2 + 1] = getHexChar(z);
        }
        return bb;
    }

    private byte getHexChar(int x) {
        switch(x) {
            case 10:
                return 'A';
            case 11:
                return 'B';
            case 12:
                return 'C';
            case 13:
                return 'D';
            case 14:
                return 'E';
            case 15:
                return 'F';
            default:
                return (byte) ('0' + x);
        }
    }
}
