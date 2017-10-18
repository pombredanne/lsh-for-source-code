package com.martiansoftware.nailgun.examples;

import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;
import com.martiansoftware.nailgun.NGContext;

/**
 * Hashes the client's stdin to the client's stdout in the form of
 * a hexadecimal string.  Command line requires one parameter: either the name
 * of the algorithm to use (e.g., "MD5"), or "?" to request a list of
 * available algorithms.
 * 
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 */
public class Hash {

    private static final char[] HEXCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * Provides a list of algorithms for the specified service (which, for
	 * our purposes, is "MessageDigest".
	 * 
	 * This method was only very slightly adapted (to use a TreeSet) from
	 * the Java Almanac at http://javaalmanac.com/egs/java.security/ListServices.html 
	 * @param serviceType The name of the service we're looking for.  It's "MessageDigest"
	 */
    private static Set getCryptoImpls(String serviceType) {
        Set result = new java.util.TreeSet();
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            Set keys = providers[i].keySet();
            for (Iterator it = keys.iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                key = key.split(" ")[0];
                if (key.startsWith(serviceType + ".")) {
                    result.add(key.substring(serviceType.length() + 1));
                } else if (key.startsWith("Alg.Alias." + serviceType + ".")) {
                    result.add(key.substring(serviceType.length() + 11));
                }
            }
        }
        return (result);
    }

    /**
     * Hashes client stdin, displays hash result to client stdout.
     * Requires one command line parameter, either the name of the hash
     * algorithm to use (e.g., "MD5") or "?" to request a list of
     * available algorithms.  Any exceptions become the problem of the user.
     */
    public static void nailMain(NGContext context) throws java.security.NoSuchAlgorithmException, java.io.IOException {
        String[] args = context.getArgs();
        if (args.length == 0) {
            Set algs = getCryptoImpls("MessageDigest");
            for (Iterator i = algs.iterator(); i.hasNext(); ) {
                context.out.println(i.next());
            }
        } else {
            MessageDigest md = MessageDigest.getInstance(args[0]);
            byte[] b = new byte[1024];
            int bytesRead = context.in.read(b);
            while (bytesRead != -1) {
                md.update(b, 0, bytesRead);
                bytesRead = System.in.read(b);
            }
            byte[] result = md.digest();
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < result.length; ++i) {
                buf.append(HEXCHARS[(result[i] >> 4) & 0x0f]);
                buf.append(HEXCHARS[result[i] & 0x0f]);
            }
            context.out.println(buf);
        }
    }
}
