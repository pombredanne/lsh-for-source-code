package org.dpes.compare.md5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class SimpleMD5GenImpl implements MD5Gen {

    public static int BUFF_LENGTH = 1024;

    @Override
    public String generateMD5Hash(File file) {
        final Formatter formatter = new Formatter();
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            final FileInputStream inputStream = new FileInputStream(file);
            final byte[] buffer = new byte[BUFF_LENGTH];
            int bufferLength;
            while ((bufferLength = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bufferLength);
            }
            final byte[] digest = messageDigest.digest();
            for (final byte digestByte : digest) {
                formatter.format("%02x", digestByte);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return formatter.toString();
    }
}
