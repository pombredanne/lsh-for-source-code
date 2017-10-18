package org.msgroad.cmpp.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;
import org.msgroad.BindType;
import org.msgroad.ByteUtil;
import org.msgroad.cmpp.ProtocolCommandID;
import org.msgroad.cmpp.CMPPConfig;

/**
 * This class support CMPP 2.0, CMPP 3.0, SMIAS 1.2.
 * 
 */
public class CMPPConnectMessage extends CMPPAbstractMessage {

    private static final long serialVersionUID = (long) ProtocolCommandID.CMPP_CONNECT;

    private static Logger logger = Logger.getLogger(CMPPConnectMessage.class);

    private byte[] authenticatorSource = null;

    private byte bindType = BindType.BIND_TRX;

    private byte version;

    private int timestamp;

    private String authenticatorSA;

    private String sharedSecret;

    public CMPPConnectMessage(byte bindType) {
        this.authenticatorSA = CMPPConfig.getAuthenticatorSA();
        this.sharedSecret = CMPPConfig.getSharedSecret();
        this.bindType = bindType;
        commandId = ProtocolCommandID.CMPP_CONNECT;
        String strNow = null;
        if (CMPPConfig.isSMIAS()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");
            Date now = new Date();
            strNow = dateFormat.format(now);
            timestamp = (int) (new Date().getTime() / 1000);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");
            Date now = new Date();
            strNow = dateFormat.format(now);
            timestamp = new Integer(strNow).intValue();
        }
        byte[] b0 = new byte[9];
        for (int i = 0; i < 9; i++) {
            b0[i] = 0;
        }
        try {
            ByteBuffer bb = ByteBuffer.allocate(32);
            bb.put(authenticatorSA.getBytes());
            bb.put(b0);
            bb.put(sharedSecret.getBytes());
            if (CMPPConfig.isSMIAS()) {
                bb.putInt(timestamp);
            } else {
                bb.put(strNow.getBytes());
            }
            bb.flip();
            byte[] toMd5 = new byte[bb.limit()];
            bb.get(toMd5);
            MessageDigest md = MessageDigest.getInstance("MD5");
            authenticatorSource = md.digest(toMd5);
            logger.info("authenticatorSource: " + ByteUtil.toHexForLog(authenticatorSource));
        } catch (NoSuchAlgorithmException e) {
            logger.error("I don't know how to compute MD5!");
            System.exit(1);
        }
    }

    /**
     * @return Returns the authenticatorSource.
     */
    public byte[] getAuthenticatorSource() {
        return authenticatorSource;
    }

    /**
     * @param authenticatorSource
     *            The authenticatorSource to set.
     */
    public void setAuthenticatorSource(byte[] authenticatorSource) {
        this.authenticatorSource = authenticatorSource;
    }

    /**
     * @return Returns the timestamp.
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp
     *            The timestamp to set.
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
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
    public void setVersion(byte v) {
        version = v;
    }

    public void encodeBody(ByteBuffer buf) {
        if (!CMPPConfig.isSMIAS()) {
            buf.put(authenticatorSA.getBytes());
            buf.put(authenticatorSource);
            if (bindType == BindType.BIND_TRX) {
                this.version = CMPPConfig.getSubVersion();
            } else {
                this.version = bindType;
            }
            buf.put(version);
            buf.putInt(timestamp);
        } else {
            buf.put(ByteUtil.getCOctetBytes(authenticatorSA));
            buf.put(authenticatorSource);
            buf.put(bindType);
            this.version = CMPPConfig.getSubVersion();
            buf.put(version);
            buf.putInt(timestamp);
        }
    }

    /**
     * For SP, it's not supported yet.
     */
    public void decodeBody(byte[] body) {
        logger.error("NOT Supported!");
    }

    public String toString() {
        return super.toString() + " TotalLength: (" + totalLength + ')' + " AuthenticatorSource:(" + authenticatorSource + ')' + " Version(" + version + ')' + " Timestamp(" + timestamp + ")";
    }
}
