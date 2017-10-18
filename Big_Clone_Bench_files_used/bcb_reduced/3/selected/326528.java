package net.sf.jradius.util;

import gnu.crypto.hash.HashFactory;
import gnu.crypto.hash.IMessageDigest;

/**
 * CHAP Utils. 
 * 
 * @author David Bird
 */
public final class CHAP {

    /**
     * @param id The packet identifier
     * @param Password The User's Password value in bytes
     * @param Challenge The 16 byte authentication challenge
     * @return Returns the CHAP-Password
     */
    public static byte[] chapMD5(byte id, byte[] Password, byte[] Challenge) {
        IMessageDigest md = HashFactory.getInstance("MD5");
        md.update(id);
        md.update(Password, 0, Password.length);
        md.update(Challenge, 0, Challenge.length);
        return md.digest();
    }

    /**
     * Do CHAP
     * 
     * @param id The packet identifier
     * @param Password The User's Password value in bytes
     * @param Challenge The 16 byte authentication challenge
     * @return Returns the CHAP-Password
     */
    public static byte[] chapResponse(byte id, byte[] Password, byte[] Challenge) {
        byte[] Response = new byte[17];
        Response[0] = id;
        System.arraycopy(chapMD5(id, Password, Challenge), 0, Response, 1, 16);
        return Response;
    }
}
