package crypto.rsacomp.common;

import java.security.MessageDigest;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

public class Util {

    public static void printRegisteredProviders() {
        Provider[] currentProviders = Security.getProviders();
        Print.println("Current providers:  ");
        for (int index = 0; index < currentProviders.length; index++) {
            Print.println(currentProviders[index].getName() + " " + currentProviders[index].getVersion());
            Print.println("  " + currentProviders[index].getInfo());
        }
        Print.println();
    }

    /**
     * Adds the JsafeJCE provider if it has not been added already.
     *
     * @throws Exception On failure.
     */
    public static synchronized void addJsafeJCE() throws Exception {
        Security.removeProvider("JsafeJCE");
        Provider jsafeProvider = new com.rsa.jsafe.provider.JsafeJCE();
        int position = Security.insertProviderAt(jsafeProvider, 1);
        if (position != 1) {
            throw new RuntimeException("Failed to insert provider at first position");
        }
    }

    public static byte[] generateSeed() {
        SecureRandom seeder = new SecureRandom();
        return seeder.generateSeed(20);
    }

    /** Trucate the hash into nrBytes byte[], XORing the whole hash for better security. */
    public static byte[] reduceHash(MessageDigest md, int nrBytes) {
        byte[] digest = md.digest();
        byte[] res = new byte[nrBytes];
        for (int dx = 0; dx < digest.length; dx++) res[dx % nrBytes] ^= digest[dx];
        return res;
    }
}
