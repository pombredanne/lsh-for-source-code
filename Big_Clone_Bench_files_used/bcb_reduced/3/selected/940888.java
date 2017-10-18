package com.sshtools.common.util;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.8 $
 */
public class UID {

    byte[] uid;

    private UID(String str) throws UIDException {
        if (str == null) {
            throw new UIDException("UID cannot be NULL");
        }
        try {
            uid = new byte[str.length() / 2];
            String tmp;
            int pos = 0;
            for (int i = 0; i < str.length(); i += 2) {
                tmp = str.substring(i, i + 2);
                uid[pos++] = (byte) Integer.parseInt(tmp, 16);
            }
        } catch (NumberFormatException ex) {
            throw new UIDException("Failed to parse UID String: " + ex.getMessage());
        }
    }

    private UID(byte[] uid) {
        this.uid = uid;
    }

    /**
*
*
* @param content
*
* @return
*
* @throws UIDException
*/
    public static UID generateUniqueId(byte[] content) throws UIDException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            if (content != null) {
                ByteArrayInputStream in = new ByteArrayInputStream(content);
                DigestInputStream dis = new DigestInputStream(in, messageDigest);
                while (dis.read() != -1) {
                    ;
                }
                dis.close();
                in.close();
            }
            byte[] noise = new byte[1024];
            ConfigurationLoader.getRND().nextBytes(noise);
            messageDigest.update(noise);
            UID uid = new UID(messageDigest.digest());
            return uid;
        } catch (Exception ex) {
            throw new UIDException("Failed to generate a unique identifier: " + ex.getMessage());
        }
    }

    /**
*
*
* @param uid
*
* @return
*
* @throws UIDException
*/
    public static UID fromString(String uid) throws UIDException {
        return new UID(uid);
    }

    /**
*
*
* @return
*/
    public String toString() {
        StringBuffer checksumSb = new StringBuffer();
        for (int i = 0; i < uid.length; i++) {
            String hexStr = Integer.toHexString(0x00ff & uid[i]);
            if (hexStr.length() < 2) {
                checksumSb.append("0");
            }
            checksumSb.append(hexStr);
        }
        return checksumSb.toString();
    }

    /**
*
*
* @param obj
*
* @return
*/
    public boolean equals(Object obj) {
        if ((obj != null) && obj instanceof UID) {
            return Arrays.equals(uid, ((UID) obj).uid);
        }
        return false;
    }

    /**
*
*
* @return
*/
    public int hashCode() {
        return toString().hashCode();
    }
}
