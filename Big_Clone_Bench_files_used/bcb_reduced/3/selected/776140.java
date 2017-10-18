package org.msgroad.cmpp.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;
import org.msgroad.ByteUtil;
import org.msgroad.cmpp.ProtocolCommandID;
import org.msgroad.cmpp.CMPPConfig;

/**
 * @author Jason
 * 
 * This class support CMPP 2.0, CMPP 3.0, SMIAS 1.2.
 */
public class CMPPConnectRespMessage extends CMPPAbstractMessage {

    private static final long serialVersionUID = (long) ProtocolCommandID.CMPP_CONNECT_RESP;

    private static Logger logger = Logger.getLogger(CMPPConnectRespMessage.class);

    private byte[] authenticatorISMG = null;

    private byte[] clientAuthenticatorSource = null;

    private byte version = 0x30;

    private int status;

    private String sharedSecret = "654321";

    public CMPPConnectRespMessage() {
        commandId = ProtocolCommandID.CMPP_CONNECT_RESP;
    }

    /**
     * @return Returns the authenticatorSource.
     */
    public void computeAuthenticatorISMG() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String toMd5 = status + sharedSecret;
            authenticatorISMG = md.digest(toMd5.getBytes());
            logger.info("authenticatorSource: " + ByteUtil.toHexForLog(clientAuthenticatorSource));
        } catch (NoSuchAlgorithmException e) {
            logger.error("I don't know how to compute MD5!");
            System.exit(1);
        }
    }

    /**
     * @return Returns the timestamp.
     */
    public int getTimestamp() {
        return status;
    }

    /**
     * @param timestamp
     *            The timestamp to set.
     */
    public void setTimestamp(int timestamp) {
        this.status = timestamp;
    }

    /**
     * @return Returns the version.
     */
    public byte getVersion() {
        return version;
    }

    /**
     * @param version
     *            The version to set.
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    public void encodeBody(ByteBuffer buf) {
        logger.error("Not supported yet");
    }

    public void decodeBody(byte[] body) {
        if (!CMPPConfig.isSMIAS()) {
            int LEN_STATUS = 4;
            if (body.length == 18) {
                LEN_STATUS = 1;
                status = body[0];
            } else {
                status = ByteUtil.byte2Int(body, 0);
            }
            authenticatorISMG = new byte[16];
            System.arraycopy(body, LEN_STATUS, authenticatorISMG, 0, 16);
            version = body[LEN_STATUS + 16];
        } else {
            status = commandStatus;
            if (commandStatus == 0) {
            }
        }
    }

    public String toString() {
        return super.toString() + " TotalLength: (" + totalLength + ')' + " AuthenticatorSource:(" + authenticatorISMG + ')' + " Version(" + version + ')' + " Status(" + status + ")";
    }

    /**
     * @param authenticatorSource
     *            The authenticatorSource to set.
     */
    public void setClientAuthenticatorSource(byte[] authenticatorSource) {
        this.clientAuthenticatorSource = authenticatorSource;
    }

    /**
     * @return Returns the status.
     */
    public int getStatus() {
        return status;
    }
}
