package neoAtlantis.utilidades.accessController.cipher;

import java.security.MessageDigest;
import neoAtlantis.utilidades.accessController.cipher.interfaces.DataCipher;

/**
 *
 * @author Hiryu (aslhiryu@gmail.com)
 */
public class CipherSha1 implements DataCipher {

    public String cifra(String str) throws Exception {
        byte[] digest, buffer = str.getBytes();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        String hash = "";
        md.update(buffer);
        digest = md.digest();
        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) hash += "0";
            hash += Integer.toHexString(b);
        }
        return hash;
    }

    public String descifra(String str) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
