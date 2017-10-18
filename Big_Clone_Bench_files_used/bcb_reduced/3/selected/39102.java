package de.excrawler.server.Server.Security.Encryption;

import de.excrawler.server.Logging.Log;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 *
 * @author Yves Hoppe <info at yves-hoppe.de>
 * @author Karpouzas George <www.webnetsoft.gr>
 */
public class MD5 {

    public String Hash(String plain) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(plain.getBytes(), 0, plain.length());
            return new BigInteger(1, md5.digest()).toString(16);
        } catch (Exception ex) {
            Log.serverlogger.warn("No such Hash algorithm", ex);
            return "";
        }
    }
}
