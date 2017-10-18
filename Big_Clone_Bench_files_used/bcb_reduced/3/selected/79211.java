package Link;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Toolkit {

    /**
     * Convert md5 bytes into hex values
     * @param data Byte data to be hex'ed
     * @return Returns the hex representation of the md5sum
     */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = aData & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Creates an MD5 sum for a text string
     * @param text The string you want to sum
     * @return The md5sum
     * @throws NoSuchAlgorithmException If md5 isn't available
     * @throws UnsupportedEncodingException If the character encoding isn't available
     */
    public static String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash;
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }

    /**
     * Cut off the MD5sum, limit to 6 characters. Will still result in 16777216 possible combinations, 2^128 isn't needed
     * @param text The sum you want shortened
     * @return The shortened md5sum
     */
    public static String ShortMD5(String text) {
        try {
            return MD5(text).substring(0, ShortlinkProperties.getLengthURL());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This will sort the URL parameter by parameter. A URL can be:
     *   http://host/arg1=test&arg2=test
     *  or
     *   http://host/arg2=test&arg1=test
     *  ...which should preferably render the same md5 sum
     * @param url The URL we want to sort
     * @return Returns the sorted URL
     */
    public static String sortURL(String url) {
        String[] urlArgs = url.split("&");
        java.util.Arrays.sort(urlArgs);
        String ret = "";
        for (String urlArg : urlArgs) {
            ret = ret + urlArg;
        }
        return ret;
    }
}
