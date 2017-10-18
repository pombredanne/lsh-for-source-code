package org.mndacs.protocol;

import java.io.*;
import java.util.Arrays;
import org.mndacs.datatobjects.CommandSet;
import org.apache.commons.codec.binary.*;
import org.apache.log4j.Logger;
import java.security.*;

/**
 * class to implement protocol for network communication:
 *
 * data aquisition
 * data exchange
 * commands
 *
 * @author christopherwagner
 */
public class MNDACSNetworkProtocolBinary implements ProtocolInterface {

    private static Logger logger = Logger.getLogger(MNDACSNetworkProtocolBinary.class);

    public String encode(CommandSet cmd) {
        String returnString = "";
        try {
            returnString = "<MNDACS-BNP-1.0>";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(cmd);
            out.close();
            byte[] buf = bos.toByteArray();
            Base64 base64 = new Base64();
            returnString += base64.encodeToString(buf);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.reset();
            sha.update(buf);
            byte[] checksum = sha.digest();
            returnString += "<sha256>";
            returnString += base64.encodeToString(checksum);
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return returnString;
    }

    public CommandSet decode(String line) {
        String nline;
        nline = line.trim();
        CommandSet returnSet = null;
        try {
            String substring;
            substring = nline.substring(0, 16);
            if (!substring.equals("<MNDACS-BNP-1.0>")) return null;
            substring = nline.substring(16, nline.indexOf("<sha256>"));
            Base64 base64 = new Base64();
            byte[] buf = base64.decode(substring);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.reset();
            sha.update(buf);
            byte[] checksum = sha.digest();
            substring = nline.substring(nline.indexOf("<sha256>") + 8);
            byte[] decodedChecksum = base64.decode(substring);
            if (!Arrays.equals(checksum, decodedChecksum)) return null;
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
            returnSet = (CommandSet) in.readObject();
            in.close();
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return returnSet;
    }

    public CommandSet test(CommandSet cmd) {
        CommandSet returnSet = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(cmd);
            byte[] buf = bos.toByteArray();
            Base64 base64 = new Base64();
            String coded;
            coded = base64.encodeToString(buf);
            buf = base64.decode(coded);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
            returnSet = (CommandSet) in.readObject();
            out.close();
            in.close();
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return returnSet;
    }

    public String getHeader() {
        return "<MNDACS-BNP-1.0>";
    }
}
