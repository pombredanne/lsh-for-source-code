package edu.harvard.mcz.imagecapture.utility;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** HashUtility provides a convenience method for working with java.security.MessageDigest 
 * that takes a string and returns the SHA1 hash of it as a string.  Intended as a convenience utility
 * for obtaining SHA1 hashes of relatively short strings without having to deal with conversion to and 
 * from arrays of bytes.  Not intended as a general purpose wrapper for SHA1 digests, use the
 * MessageDigest class to obtain hashes for use as checksums of files and for purposes other than
 * obtaining SHA1 hashes of Strings.   
 * 
 * @author Paul J. Morris
 *
 */
public class HashUtility {

    /**
	 * Compute the SHA1 digest of a string, and return it as a string.  Returns an empty string if the SHA-1
	 * MessageDigest is not available, or if the utf-8 encoding is not supported.    
	 * 
	 * @param stringToHash the string on which an SHA1 hash is to be computed, this string is assumed to be
	 * in UTF-8 encoding.
	 * @return a string containing a hexidecimal encoding of the SHA1 message digest of the stringToHash note 
	 * this will be an empty string if an error is encountered in computing the hash.  
	 */
    public static String getSHA1Hash(String stringToHash) {
        String result = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.update(stringToHash.getBytes("utf-8"));
            byte[] hash = md.digest();
            StringBuffer hashString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                int halfByte = (hash[i] >>> 4) & 0x0F;
                int twoHalves = 0;
                do {
                    if ((0 <= halfByte) && (halfByte <= 9)) {
                        hashString.append((char) ('0' + halfByte));
                    } else {
                        hashString.append((char) ('a' + (halfByte - 10)));
                    }
                    halfByte = hash[i] & 0x0F;
                } while (twoHalves++ < 1);
            }
            result = hashString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
