package com.jcompressor.digest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.jcompressor.exceptions.JcompressorException;

public class Checksum {

    public static Boolean create(final InputStream stream) {
        try {
            final MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int number = stream.read(buffer);
            int count = 1;
            int length = 0;
            while (number > 0) {
                digester.update(buffer, 0, buffer.length);
                length = (length + buffer.length);
                count++;
                number = stream.read(buffer);
            }
            final BigInteger sum = new BigInteger(1, digester.digest());
            final String output = sum.toString(16);
            System.out.println("MD5: " + output);
            return Boolean.TRUE;
        } catch (IOException e) {
            throw new JcompressorException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new JcompressorException(e);
        }
    }
}
