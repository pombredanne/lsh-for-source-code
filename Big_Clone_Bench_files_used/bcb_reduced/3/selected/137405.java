package com.generatescape.filehandling;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*******************************************************************************
 * Copyright (c) 2005, 2007 GenerateScape and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/copyleft/gpl.html
 * @author kentgibson : http://www.bigblogzoo.com
 *
 *******************************************************************************/
public class NamingService {

    MessageDigest md;

    /**
   * 
   */
    public NamingService() {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
   * Converts a 4 byte array of unsigned bytes to an long
   * 
   * @param b
   *          an array of 4 unsigned bytes
   * @return a long representing the unsigned int
   */
    private static final long unsignedIntToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        l <<= 8;
        l |= b[4] & 0xFF;
        l <<= 8;
        l |= b[5] & 0xFF;
        l <<= 8;
        l |= b[6] & 0xFF;
        l <<= 8;
        l |= b[7] & 0xFF;
        return l;
    }

    /**
   * We create a SHA1 digest, then take the first 8 bytes and convert to a long
   * 
   * @param bytes
   * @return
   */
    public long getDigest(byte[] bytes) {
        md.reset();
        md.update(bytes);
        byte[] toDigest = md.digest();
        long sum = unsignedIntToLong(toDigest);
        return sum;
    }
}
