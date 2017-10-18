package net.exclaimindustries.fotobilder;

import java.security.*;
import net.exclaimindustries.tools.CharToByte;

/**
 * An <code>AbstractPictureData</code> object is needed to provide some sort of
 * common functionality to <code>PictureData</code> objects while still keeping
 * the latter as just an interface.
 *
 * @author Nicholas Killewald
 */
public abstract class AbstractPictureData implements PictureData {

    protected boolean cached;

    /**
     * Gets the MD5 hash from a given byte array.
     *
     * @param data byte array to get an MD5 hash from
     * @return an MD5 hash of the given byte array
     */
    protected synchronized String getMD5From(byte[] data) {
        String md5;
        try {
            MessageDigest digestol = MessageDigest.getInstance("MD5");
            digestol.update(data);
            md5 = CharToByte.bytesToString(digestol.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 couldn't be loaded due to a NoSuchAlgorithmException! (the hell?)");
        }
        return md5;
    }

    /**
     * Gets the Magic bytes from a given byte array.  Magic bytes, in this case,
     * meaning the first ten bytes of the data, unrolled into a hex String.  Which
     * also means this will return a 20-character String.
     *
     * @param data byte array to get the Magic bytes from
     * @return the Magic bytes of the given byte array
     */
    protected synchronized String getMagicFrom(byte[] data) {
        byte[] bytes = new byte[10];
        for (int i = 0; i <= 9; i++) {
            bytes[i] = data[i];
        }
        return CharToByte.bytesToString(bytes);
    }

    /**
     * Gets whether or not the picture data is cached in this object.  By
     * default, most PictureData objects won't cache their data for memory
     * conservation.  However, if the data has been cached, it can be called
     * quickly without, say, re-reading the file.
     *
     * @return true if cached, false otherwise
     */
    protected boolean isCached() {
        return cached;
    }

    /**
     * Sets the cached variable.
     *
     * @param flag value to set
     */
    protected void setCached(boolean flag) {
        cached = flag;
    }
}
