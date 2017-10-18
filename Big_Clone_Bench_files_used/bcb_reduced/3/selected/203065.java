package javadata.encryption;

import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javadata.util.Globals;

/**
 * <p>
 * <b>Title: </b>Class that implements checksum utility methods.
 * </p>
 *
 * <p>
 * <b>Description: </b>Class that implements checksum utility methods.
 * Can be used to hold a computed checksum and do checksum comparisons.
 * </p>
 *
 * <p><b>Version: </b>1.0</p>
 * 
 * <p>
 * <b>Author: </b> Matthew Pearson, Copyright 2006, 2007
 * </p>
 * 
 * <p>
 * <b>License: </b>This file is part of JavaData.
 * </p>
 * <p>
 * JavaData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 * <p>
 * JavaData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with JavaData.  If not, see 
 * <a href="http://www.gnu.org/licenses/">GNU licenses</a>.
 * </p> 
 *
 */
public class Checksum {

    /**
	 * Method to compute the checksum of an array.
	 * Does not store the result.
	 * @param array Input array
	 * @return byte[]
	 */
    public byte[] doChecksum(byte[] array) {
        return this.computeChecksum(array);
    }

    /**
	 * Method to compute the checksum of an character array.
	 * Ignores multibyte character encoding.
	 * Does not store the result.
	 * @param array Input character array
	 * @return byte[]
	 */
    public byte[] doChecksum(char[] array) {
        byte byteArray[] = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            byteArray[i] = (byte) array[i];
        }
        return this.computeChecksum(byteArray);
    }

    /**
	 * Compare the input byte[] array with the one
	 * stored inside this object.
	 * @param checksum
	 * @return boolean
	 */
    public boolean sameChecksum(byte[] checksum) {
        return Arrays.equals(this.mChecksum, checksum);
    }

    /**
	 * Get the checksum.
	 * @return byte[]
	 */
    public byte[] getChecksum() {
        return this.mChecksum;
    }

    /**
	 * Set the checksum.
	 * @param checksum A checksum byte[] array.
	 */
    public void setChecksum(byte[] checksum) {
        this.mChecksum = checksum;
    }

    /**
	 * Method to compute the checksum of an array.
	 * Does not store the result.
	 * @param array Input array
	 * @returns byte[]
	 */
    private byte[] computeChecksum(byte[] array) {
        byte[] result = null;
        try {
            MessageDigest md = MessageDigest.getInstance(Globals.getCHECKSUM_MODE());
            md.update(array);
            result = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Storage location for a checksum.
	 */
    private byte[] mChecksum = null;

    ;
}
