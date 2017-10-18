package org.pointrel.pointrel20090201;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Standards {

    public static String timestampType = "ISO8601Timestamp";

    public static String makeISO8601Timestamp(long millisecondsSinceTheEpoch) {
        Timestamp timestamp = new Timestamp(millisecondsSinceTheEpoch);
        SimpleDateFormat ISO8601TimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ISO8601TimestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedTimestamp = ISO8601TimestampFormat.format(timestamp);
        return formattedTimestamp;
    }

    public static String getCurrentTimestamp() {
        return makeISO8601Timestamp(System.currentTimeMillis());
    }

    public static String makeTimestampSuitableForFileName(String timestamp) {
        String result = timestamp.replaceAll("-", "_");
        result = result.replaceAll(":", "_");
        result = result.replaceAll("\\.", "_");
        result = result.replaceAll("T", "_");
        return result;
    }

    public static String newUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getSHA1HashAsHexEncodedString(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] hashSHA1 = messageDigest.digest(bytes);
            return hexEncodedStringForByteArray(hashSHA1);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String XgetSHA1HashAsHexEncodedString(File file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            FileInputStream inputStream = new FileInputStream(file);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            byte[] hashSHA1 = messageDigest.digest();
            return hexEncodedStringForByteArray(hashSHA1);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getResourceFileReferenceWithSHA256HashAsHexEncodedString(byte[] bytes, String extension) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(getExtraBytes(bytes.length, extension));
            byte[] hashSHA256 = messageDigest.digest(bytes);
            String resourceFileReference = ResourceFileSupport.ResourceFilePrefix + hexEncodedStringForByteArray(hashSHA256) + getExtraString(bytes.length, extension);
            return resourceFileReference;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getExtraString(long size, String extension) {
        return "_" + size + extension;
    }

    public static byte[] getExtraBytes(long size, String extension) {
        String extraString = "_" + size + extension + "\n";
        byte[] extraBytes = extraString.getBytes();
        return extraBytes;
    }

    public static String getResourceFileReferenceWithSHA256HashAsHexEncodedString(File file) {
        String extension = ResourceFileSupport.getExtensionWithDotOrEmptyString(file.getName(), true);
        long size = file.length();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            FileInputStream inputStream = new FileInputStream(file);
            messageDigest.update(getExtraBytes(size, extension));
            try {
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            byte[] hashSHA256 = messageDigest.digest();
            String resourceFileReference = ResourceFileSupport.ResourceFilePrefix + hexEncodedStringForByteArray(hashSHA256) + getExtraString(size, extension);
            return resourceFileReference;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] deflateByteArrayUsingZLIBCompression(byte[] uncompressedBytes) {
        Deflater deflater = new Deflater();
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        try {
            deflaterOutputStream.write(uncompressedBytes, 0, uncompressedBytes.length);
            deflaterOutputStream.finish();
            deflaterOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] inflateByteArrayUsingZLIBCompression(byte[] compressedBytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
        try {
            ResourceFileSupport.copyInputStreamToOutputStream(inflaterInputStream, byteArrayOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static String stringForPoint(Point point) {
        return point.x + " " + point.y;
    }

    public static Point pointForString(String string) {
        String[] splitResult = string.split(" ");
        return new Point(Integer.parseInt(splitResult[0]), Integer.parseInt(splitResult[1]));
    }

    public static String hexEncodedStringForByteArray(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int theByte : bytes) {
            if (theByte < 0) {
                theByte = theByte + 256;
            }
            String hexString = Integer.toHexString(theByte);
            if (hexString.length() == 1) stringBuilder.append("0");
            stringBuilder.append(hexString);
        }
        return stringBuilder.toString();
    }

    public static byte[] byteArrayForHexEncodedString(String hexEncodedByteString) {
        if (hexEncodedByteString.length() % 2 != 0) {
            System.out.println("Hex encoded string must be of even length");
            return null;
        }
        byte[] bytes = new byte[hexEncodedByteString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexEncodedByteString.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
