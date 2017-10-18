package org.afekete.filemetadatacreator.checksum;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import org.afekete.filemetadatacreator.utils.StringUtils;

/**
 * Class for implementing checksum calculation.
 * 
 * @author afekete
 *
 */
public class SHA1Calculator {

    /**
	 * Creates checksum for a file.
	 * 
	 * @param file file to create checksum for
	 * @return checksum bytes
	 * @throws Exception
	 */
    public static byte[] createChecksum(File file) throws Exception {
        InputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("SHA1");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    /**
	* Executes checksum creation and gets string representation of the byte array result.
	*  
	* @param file file to create checksum for
	* @return checksum string
	* @throws Exception
	*/
    public static String getSHA1Checksum(File file) throws Exception {
        byte[] b = createChecksum(file);
        return StringUtils.getHexString(b);
    }
}
