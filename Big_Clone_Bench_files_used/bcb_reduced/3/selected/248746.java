package org.dcm4chex.archive.tools;

import java.security.MessageDigest;
import org.dcm4che.util.Base64;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: Pwd2Hash.java 3122 2007-02-15 17:35:26Z gunterze $
 * @since Feb 15, 2007
 */
public class Pwd2Hash {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("USAGE: Pwd2Hash <readable password>");
        } else {
            try {
                String hash = Base64.byteArrayToBase64(createHash(args[0]));
                System.out.println("Hashed password:" + hash);
            } catch (Exception x) {
                System.out.println("Cant create SHA1 hash" + x.getMessage());
                x.printStackTrace();
            }
        }
    }

    public static final byte[] createHash(String value) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA");
        byte[] hashBytes = digest.digest((value).getBytes("UTF-8"));
        return hashBytes;
    }
}
