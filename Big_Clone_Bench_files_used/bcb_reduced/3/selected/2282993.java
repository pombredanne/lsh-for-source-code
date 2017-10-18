package org.mndacs.protocol;

import java.io.*;
import java.util.Arrays;
import org.mndacs.datatobjects.CommandSet;
import org.apache.commons.codec.binary.*;
import org.apache.log4j.Logger;
import java.security.*;
import java.util.zip.*;
import org.mndacs.kernel.CONFIG;

/**
 * class to implement protocol for network communication:
 *
 * data aquisition
 * data exchange
 * commands
 *
 * @author christopherwagner
 */
public class MNDACSNetworkProtocolBinaryCompressed implements ProtocolInterface {

    private static Logger logger = Logger.getLogger(MNDACSNetworkProtocolBinaryCompressed.class);

    private String header = "<MNDACS-BNPGZ-1.1>";

    public String encode(CommandSet cmd) {
        String returnString = "";
        try {
            returnString = header;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(cmd);
            out.close();
            byte[] buf = bos.toByteArray();
            Base64 base64 = new Base64();
            returnString += base64.encodeToString(compress(buf));
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash_buf = new byte[buf.length + CONFIG.Network_SALT.getBytes().length];
            System.arraycopy(buf, 0, hash_buf, 0, buf.length);
            System.arraycopy(CONFIG.Network_SALT.getBytes(), 0, hash_buf, buf.length, CONFIG.Network_SALT.getBytes().length);
            sha.reset();
            sha.update(hash_buf);
            byte[] checksum = sha.digest();
            returnString += "<sha256>";
            returnString += base64.encodeToString(checksum);
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
        }
        logger.debug("size encoded Message: " + returnString.length());
        return returnString;
    }

    public CommandSet decode(String line) {
        String nline;
        nline = line.trim();
        CommandSet returnSet = null;
        logger.debug("size Message to decode: " + line.length());
        logger.debug(line);
        try {
            String substring;
            if (nline.length() < header.length()) {
                logger.error("string to decode to short");
                return null;
            } else substring = nline.substring(0, header.length());
            if (!substring.equals(header)) return null;
            substring = nline.substring(header.length(), nline.indexOf("<sha256>"));
            Base64 base64 = new Base64();
            byte[] buf = uncompress(base64.decode(substring));
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash_buf = new byte[buf.length + CONFIG.Network_SALT.getBytes().length];
            System.arraycopy(buf, 0, hash_buf, 0, buf.length);
            System.arraycopy(CONFIG.Network_SALT.getBytes(), 0, hash_buf, buf.length, CONFIG.Network_SALT.getBytes().length);
            sha.reset();
            sha.update(hash_buf);
            byte[] checksum = sha.digest();
            substring = nline.substring(nline.indexOf("<sha256>") + 8);
            byte[] decodedChecksum = base64.decode(substring);
            if (!Arrays.equals(checksum, decodedChecksum)) {
                logger.error("checksum failed");
                return null;
            }
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf));
            returnSet = (CommandSet) in.readObject();
            logger.debug(returnSet.getCommandDescription());
            in.close();
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return returnSet;
    }

    private byte[] compress(byte[] byt) throws IOException {
        int size = 1024;
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(byt));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        byte[] buffer = new byte[size];
        int len;
        while ((len = bis.read(buffer, 0, size)) != -1) {
            gzip.write(buffer, 0, len);
        }
        gzip.finish();
        bis.close();
        gzip.close();
        return baos.toByteArray();
    }

    private byte[] uncompress(byte[] data) throws IOException {
        int size = 1024;
        GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[size];
        int len;
        while ((len = gzip.read(buffer, 0, size)) != -1) {
            baos.write(buffer, 0, len);
        }
        gzip.close();
        baos.close();
        return baos.toByteArray();
    }

    public String getHeader() {
        return header;
    }
}
